/*

Copyright (c) 2014, Marco Franzoni, Università  di Bologna
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list
	of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this
	list of conditions and the following disclaimer in the documentation and/or
	other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may
	be used to endorse or promote products derived from this software without specific
	prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package extension

import akka.actor._
import java.util.concurrent.TimeUnit
import scala.collection.mutable.Map
import com.rti.dds.domain.DomainParticipant
import com.rti.dds.domain.DomainParticipantFactory
import com.rti.dds.infrastructure.InstanceHandle_t
import com.rti.dds.infrastructure.RETCODE_ERROR
import com.rti.dds.infrastructure.StatusKind
import com.rti.dds.publication.Publisher
import com.rti.dds.topic.Topic
import com.rti.dds.domain.DomainParticipantQos
import com.rti.dds.topic.TopicQos
import com.rti.dds.publication.DataWriterQos
import akka.dispatch.RequiresMessageQueue
import akka.dispatch.UnboundedMessageQueueSemantics
import com.rti.dds.`type`.builtin.StringDataReader
import com.rti.dds.subscription.DataReaderQos
import com.rti.dds.infrastructure.Condition
import com.rti.dds.infrastructure.WaitSet
import com.rti.dds.infrastructure.ConditionSeq
import com.rti.dds.infrastructure.Duration_t
import com.rti.dds.infrastructure.RETCODE_TIMEOUT
import com.rti.dds.infrastructure.StatusCondition
import com.rti.dds.subscription.SampleStateKind
import com.rti.dds.subscription.ViewStateKind
import com.rti.dds.subscription.InstanceStateKind
import com.rti.dds.subscription.ReadCondition
import com.rti.dds.infrastructure.StringSeq
import com.rti.dds.subscription.SampleInfoSeq
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy
import com.rti.dds.subscription.SampleInfo
import com.rti.dds.publication.DataWriter
import com.rti.dds.subscription.Subscriber
import com.rti.dds.dynamicdata.DynamicDataTypeSupport
import com.rti.dds.typecode.TypeCode
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import scala.xml.Null
import com.rti.dds.subscription.DataReaderImpl
import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._
import com.rti.dds.dynamicdata.DynamicDataSeq
import com.rti.dds.infrastructure.RETCODE_NO_DATA
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy
import com.rti.dds.topic.TypeSupportImpl
import java.lang.reflect.Constructor
import scala.reflect.api.Mirror
import com.rti.dds.publication.DataWriterImpl
import com.rti.dds.subscription.DataReader
import scala.collection.mutable.ListBuffer
import com.rti.dds.publication.PublisherQos
import com.rti.dds.subscription.SubscriberQos
import com.rti.dds.infrastructure.ReliabilityQosPolicyKind
import com.rti.dds.infrastructure.HistoryQosPolicyKind
import com.rti.dds.infrastructure.DurabilityQosPolicyKind
import com.rti.dds.infrastructure.PublishModeQosPolicyKind
import com.rti.dds.publication.FlowController
import com.rti.dds.infrastructure.TransportMulticastSettings_t
import java.net.InetAddress
import com.rti.dds.infrastructure.TransportMulticastQosPolicyKind

object DDSExtension extends ExtensionId[DDSExtension] with ExtensionIdProvider {
  
	override def get(system: ActorSystem): DDSExtension = super.get(system)
	def lookup() = this
	override def createExtension(system: ExtendedActorSystem) = new DDSExtension(system)
	
}

class DDSExtension(system: ActorSystem) extends Extension {
	
	private val domains = Map[Int, DDSDomain]()
	private val dataWriters = ListBuffer[DataWriter]()
	private val dataReaders = ListBuffer[DataReader]()
	private val actors = ListBuffer[ActorRef]()
	private val lock = new Object
  
	def getOrCreateDomainParticipant(domainID : Int, domainQos : DomainParticipantQos) : DDSDomain = {
		lock.synchronized {
			domains.getOrElseUpdate(domainID, new DDSDomain(domainID, domainQos))
		}
	}
	
	// T = Topic Type
	def newDataWriter[T : ClassTag](domainID : Int = 0, topicName : String, actorName : String) : ActorRef = 
		newDataWriter[T](Publisher.DATAWRITER_QOS_DEFAULT, domainID,
			DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, topicName, DomainParticipant.TOPIC_QOS_DEFAULT, actorName, false)

	def newDataWriter[T : ClassTag](qos : DataWriterQos, domainID : Int = 0, domainQos : DomainParticipantQos, topicName: String, topicQos : TopicQos, actorName : String, flow : Boolean) : ActorRef = {
	  
		val ddsDomain : DDSDomain = getOrCreateDomainParticipant(domainID, domainQos)
		val participant = ddsDomain.domainParticipant
		
		val topicType = implicitly[ClassTag[T]].runtimeClass
		val dataWriterAdapter : DataWriterAdapter[T] = DataWriterAdapter.apply[T]
		
		val typeName: String = dataWriterAdapter.typeNameMethod.invoke(null).asInstanceOf[String]
		println("TypeName: " +typeName)
		
		dataWriterAdapter.registerMethod.invoke(null, participant, typeName)
		
		println("DataWriter successfully created domain participant:\n" +participant)
		
		val topic = ddsDomain.getTopic(topicName, typeName, topicQos)
		println("Topic: " +topic)
			
		participant.get_implicit_publisher().get_default_datawriter_qos(qos)
//		qos.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS
		qos.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS
	  	qos.publish_mode.kind = PublishModeQosPolicyKind.ASYNCHRONOUS_PUBLISH_MODE_QOS

		qos.reliability.kind = ReliabilityQosPolicyKind.BEST_EFFORT_RELIABILITY_QOS
	  	
//	  	qos.resource_limits.max_samples = 5000 // default = ResourceLimitsQosPolicy.LENGTH_UNLIMITED
//		qos.resource_limits.max_instances = 5000  // default = ResourceLimitsQosPolicy.LENGTH_UNLIMITED
//		qos.resource_limits.initial_samples = 5000 // default = 32
//		qos.resource_limits.initial_instances = 5000 // default = 32
//		
//	  	qos.reliability.max_blocking_time.sec = 10
//	  	qos.reliability.max_blocking_time.nanosec = 0
//  	
//	  	qos.protocol.rtps_reliable_writer.min_nack_response_delay.nanosec = 0
//		qos.protocol.rtps_reliable_writer.min_nack_response_delay.sec = 0
//		qos.protocol.rtps_reliable_writer.max_nack_response_delay.nanosec = 0
//		qos.protocol.rtps_reliable_writer.max_nack_response_delay.sec = 0
		
		// test throughput
//		qos.protocol.rtps_reliable_writer.min_send_window_size = 50
//	  	qos.protocol.rtps_reliable_writer.max_send_window_size = 50
//	  	qos.protocol.rtps_reliable_writer.fast_heartbeat_period.sec = 0; 
//		qos.protocol.rtps_reliable_writer.fast_heartbeat_period.nanosec = 150000000; //150 ms
		
		// test latency
//		qos.protocol.rtps_reliable_writer.fast_heartbeat_period.sec = 0
//		qos.protocol.rtps_reliable_writer.fast_heartbeat_period.nanosec = 250000000; // 250 ms
//		qos.protocol.rtps_reliable_writer.heartbeat_period.sec = 3600 * 24 * 7; // 1 week
		
		// FlowController for big data
		if(flow){
				try {
					val controller : FlowController = participant.lookup_flowcontroller(FlowController.DEFAULT_FLOW_CONTROLLER_NAME)
					qos.publish_mode.flow_controller_name = FlowController.DEFAULT_FLOW_CONTROLLER_NAME
				} catch {
				  case err : RETCODE_ERROR => throw new RuntimeException("Unable to create FlowController")
				}
		}
		
		val dataWriter = participant.create_datawriter(
			topic, 
			qos,
			null, // listener
			StatusKind.STATUS_MASK_NONE)
		dataWriter match {
			case dataWriter : DataWriter => dataWriter
			case _ => throw new RuntimeException("Unable to create data writer")
		}
		
		println("DataWriter: " +dataWriter)
		dataWriters+=dataWriter
		println("DataWriters: " +dataWriters)
			
		val props = Props(classOf[DWActor[T]], dataWriter, implicitly[ClassTag[T]])
		val dw = system.actorOf(props, actorName)
		println("DataWriter: " +dw)
		return dw
	}

	// T = Topic Type
	def newDataReaderWS[T : ClassTag](domainID : Int, topicName: String, actor : ActorRef, take : Boolean) : Unit =
		newDataReaderWS[T](Subscriber.DATAREADER_QOS_DEFAULT, domainID,
						   DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT,
						   topicName,
						   DomainParticipant.TOPIC_QOS_DEFAULT,
						   actor,
						   take,
						   false)

	def newDataReaderWS[T : ClassTag](qos : DataReaderQos,
									  domainID : Int,
									  domainQos : DomainParticipantQos,
									  topicName: String,
									  topicQos : TopicQos,
									  actor : ActorRef,
									  take : Boolean,
									  multi : Boolean) : Unit = {

		val ddsDomain : DDSDomain = getOrCreateDomainParticipant(domainID, domainQos)
		val participant = ddsDomain.domainParticipant
		
		val topicType = implicitly[ClassTag[T]].runtimeClass		
		val dataReaderAdapter : DataReaderAdapter[T] = DataReaderAdapter.apply[T]
		
		val typeName: String = dataReaderAdapter.typeNameMethod.invoke(null).asInstanceOf[String]
		println("TypeName: " +typeName) // ok
		
		dataReaderAdapter.registerMethod.invoke(null, participant, typeName)
		
		println("DataReader: " +participant)

		val topic = ddsDomain.getTopic(topicName, typeName, topicQos)
		println("DataReader: " +topic)
		
		participant.get_implicit_subscriber().get_default_datareader_qos(qos)
//		qos.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS
//	  	qos.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS
		qos.reliability.kind = ReliabilityQosPolicyKind.BEST_EFFORT_RELIABILITY_QOS
		qos.history.kind = HistoryQosPolicyKind.KEEP_LAST_HISTORY_QOS;

	  	qos.protocol.rtps_reliable_reader.min_heartbeat_response_delay.nanosec = 0; 
		qos.protocol.rtps_reliable_reader.min_heartbeat_response_delay.sec = 0; 
		qos.protocol.rtps_reliable_reader.max_heartbeat_response_delay.nanosec = 0; 
		qos.protocol.rtps_reliable_reader.max_heartbeat_response_delay.sec = 0;
		
		// MULTICAST
		if(multi) {
			val ms : TransportMulticastSettings_t = new TransportMulticastSettings_t()
			ms.receive_address = InetAddress.getByName("224.0.0.44")
			qos.multicast.value.add(ms)
			qos.multicast.kind = TransportMulticastQosPolicyKind.AUTOMATIC_TRANSPORT_MULTICAST_QOS
		}

		val dataReader = participant.create_datareader(
			topic, 
			qos,
			null,         // Listener
			StatusKind.STATUS_MASK_ALL)
		dataReader match {
			case dataReader: DataReader => dataReader
			case _ => throw new RuntimeException("Unable to create data reader")
		}
		
		println("DataReader: " +dataReader)
		dataReaders+=dataReader
		println("DataReaders: " +dataReaders)
		
		val statusCondition : StatusCondition = dataReader.get_statuscondition()
		statusCondition.set_enabled_statuses(StatusKind.DATA_AVAILABLE_STATUS)

		val ws = ddsDomain.ws
		val node = new DDSNode[T](dataReader, actor, dataReaderAdapter, take)
		ws.addStatusCondition(statusCondition, node)

//		val readCondition : ReadCondition = dataReader.create_readcondition(SampleStateKind.NOT_READ_SAMPLE_STATE,
//																			ViewStateKind.ANY_VIEW_STATE,
//																			InstanceStateKind.ANY_INSTANCE_STATE)
//
//		val ws = ddsDomain.ws
//		val node = new DDSNode[T](dataReader, actor, dataReaderAdapter, take)
//		ws.addReadCondition(readCondition, node)
		
	}
	
	private def closeReader(domain : DomainParticipant, dr : DataReader) = {
		lock.synchronized {
			println("CLOSE READER " +dr)
			println("get_subscriber pre delete: " +dr.get_subscriber())
			domain.get_implicit_subscriber().delete_datareader(dr)
			println("Post delete: " +dr)
			dataReaders -= dr
		}
	}
	
	private def closeWriter(domain : DomainParticipant, dw : DataWriter) = {
		lock.synchronized {
			println("CLOSE WRITER " +dw)
			println("get_publisher pre delete: " +dw.get_publisher())
			domain.get_implicit_publisher().delete_datawriter(dw)
			println("Post delete: " +dw)
			dataWriters -= dw
		}
	}
	
	def closeAll() = {
		lock.synchronized {
			domains.keys.foreach {i => {
				val domain = domains.get(i).get.domainParticipant
				val wsThread = domains.get(i).get.waitSetThread
				println("closeAll domain: ID " +domain.get_domain_id() +" "+domain)
				val ddsWaitSet = domains.get(i).get.ws
				ddsWaitSet.stop
				wsThread.join()
				dataWriters.foreach(dw => closeWriter(domain, dw))
				dataReaders.foreach(dr => closeReader(domain, dr))
				println("Thread state: " +wsThread.getState())
				domain.delete_contained_entities()
				DomainParticipantFactory.get_instance().delete_participant(domain);
				domains.remove(i)
				}
			}
		}
	}

}
