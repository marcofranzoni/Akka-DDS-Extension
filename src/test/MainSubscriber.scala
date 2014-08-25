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

package test

import akka.actor.Actor
import com.rti.dds.publication.Publisher
import com.rti.dds.domain.DomainParticipantFactory
import com.rti.dds.domain.DomainParticipant
import akka.event.Logging
import com.rti.dds.subscription.Subscriber
import akka.actor.Props
import idl.HelloWorld
import com.rti.dds.typecode.TypeCode
import idl.HelloWorldTypeCode
import idl.HelloWorldDataWriter
import idl.HelloWorldDataReader
import idl.HelloWorldTypeSupport
import idl.HelloWorldSeq
import akka.actor.ActorSystem
import com.rti.dds.subscription.DataReaderQos
import com.rti.dds.infrastructure.ReliabilityQosPolicyKind
import com.rti.dds.infrastructure.DurabilityQosPolicyKind
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy
import com.rti.dds.infrastructure.DataReaderResourceLimitsQosPolicy
import com.typesafe.config.ConfigFactory
import com.rti.dds.topic.TopicQos
import com.rti.dds.infrastructure.HistoryQosPolicyKind
import com.rti.dds.domain.DomainParticipantQos
import extension.DDSExtension

object MainSubscriber {
  def main(args:Array[String]) {
	  	val system = ActorSystem("DDSAkkaSystem")
		println(system)
		val dds = new DDSExtension(system)
		println(dds)
				
		val actorDR = system.actorOf(Props[DRActor], "dataReaderActor")
		println(actorDR)
		
		val qosDataReader : DataReaderQos = new DataReaderQos
		val qosTopic : TopicQos = new TopicQos
		qosTopic.reliability.kind = ReliabilityQosPolicyKind.BEST_EFFORT_RELIABILITY_QOS
		qosTopic.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS
		
		dds.newDataReaderWS[HelloWorld](qosDataReader , 0, 
		DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, "Test Topic", qosTopic, actorDR, take = false, false)
		
		val input = readLine("Press any key to continue\n")
		println("Closing the system")
		
		dds.closeAll
		system.shutdown
	}

}