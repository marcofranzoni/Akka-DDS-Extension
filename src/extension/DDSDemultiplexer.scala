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

import com.rti.dds.infrastructure.WaitSet
import com.rti.dds.infrastructure.StatusCondition
import com.rti.dds.subscription.ReadCondition
import com.rti.dds.infrastructure.Condition
import com.rti.dds.infrastructure.Duration_t
import com.rti.dds.infrastructure.ConditionSeq
import com.rti.dds.infrastructure.RETCODE_TIMEOUT
import com.rti.dds.infrastructure.RETCODE_NO_DATA
import com.rti.dds.infrastructure.RETCODE_ERROR
import com.rti.dds.subscription.DataReader
import java.lang.reflect.Method
import com.rti.dds.subscription.SampleInfoSeq
import com.rti.dds.subscription.SampleInfo
import akka.actor.Actor
import akka.actor.ActorRef
import scala.collection.mutable.{Map, HashMap, SynchronizedMap}
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy
import com.rti.dds.subscription.SampleStateKind
import com.rti.dds.subscription.ViewStateKind
import com.rti.dds.subscription.InstanceStateKind
import com.rti.dds.infrastructure.GuardCondition

class DDSDemultiplexer extends Runnable {

	val ws : WaitSet = new WaitSet
	private val timeout : Duration_t = new Duration_t(Duration_t.DURATION_INFINITE_SEC, Duration_t.DURATION_INFINITE_NSEC) 
	private var activeConditions : ConditionSeq = new ConditionSeq(1024) // list of active conditions
	private val releaseCondition = new GuardCondition
	private val readConditions = Map[ReadCondition, DDSNode[_]]()
	private val statusConditions = Map[StatusCondition, DDSNode[_]]()
	private val lock = new Object
	private val readingLock = new Object
	@volatile private var running = true
	
	attachCondition(releaseCondition)
	
	private def attachCondition(condition : Condition) = {
		lock.synchronized {
			ws.attach_condition(condition)
		}
	}
	
	private def detachCondition(condition : Condition) = {
		lock.synchronized {
			ws.detach_condition(condition)
		}
	}
	
	def addReadCondition(condition: ReadCondition, node : DDSNode[_]) = {
		lock.synchronized {
			attachCondition(condition)
			readConditions += (condition -> node)
		}
    }
	  
	def removeReadCondition(condition: ReadCondition) = {
		lock.synchronized {
			detachCondition(condition)
			readConditions.remove(condition)
		}
	}
	
	def addStatusCondition(condition: StatusCondition, node : DDSNode[_]) = {
		lock.synchronized {
			attachCondition(condition)
			statusConditions += (condition -> node)
		}
    }
	  
	def removeStatusCondition(condition: StatusCondition) = {
		lock.synchronized {
			detachCondition(condition)
			statusConditions.remove(condition)
		}
	}
	
	private def readData(dr : DataReader,
						 adapter : DataReaderAdapter[_],
						 infoSeq : SampleInfoSeq,
						 dataSeq : Any,
						 dataSeqClass : Class[_],
						 kind : String) = {

//	  	READ/TAKE

		if(kind == adapter.read)
		{
			adapter.readMethod.invoke(dr,
				dataSeq.asInstanceOf[Object],
				infoSeq.asInstanceOf[Object],
				ResourceLimitsQosPolicy.LENGTH_UNLIMITED.asInstanceOf[Object],
				SampleStateKind.NOT_READ_SAMPLE_STATE.asInstanceOf[Object],
				ViewStateKind.ANY_VIEW_STATE.asInstanceOf[Object],
				InstanceStateKind.ANY_INSTANCE_STATE.asInstanceOf[Object])
		}
		else
			adapter.takeMethod.invoke(dr,
				dataSeq.asInstanceOf[Object],
				infoSeq.asInstanceOf[Object],
				ResourceLimitsQosPolicy.LENGTH_UNLIMITED.asInstanceOf[Object],
				SampleStateKind.NOT_READ_SAMPLE_STATE.asInstanceOf[Object],
				ViewStateKind.ANY_VIEW_STATE.asInstanceOf[Object],
				InstanceStateKind.ANY_INSTANCE_STATE.asInstanceOf[Object])
		  
	}
	
	private def readData(dr : DataReader,
						 adapter : DataReaderAdapter[_],
						 infoSeq : SampleInfoSeq,
						 dataSeq : Any,
						 dataSeqClass : Class[_],
						 condition : ReadCondition,
						 kind : String) = {

//		READ/TAKE WITH CONDITION

		if(kind == adapter.readCondition)
			adapter.readConditionMethod.invoke(dr,
				dataSeq.asInstanceOf[Object],
				infoSeq.asInstanceOf[Object],
				ResourceLimitsQosPolicy.LENGTH_UNLIMITED.asInstanceOf[Integer],
				condition)
		else
			adapter.takeConditionMethod.invoke(dr,
				dataSeq.asInstanceOf[Object],
				infoSeq.asInstanceOf[Object],
				ResourceLimitsQosPolicy.LENGTH_UNLIMITED.asInstanceOf[Integer],
				condition)
	}
	
	private def getSample(condition : ReadCondition, node : Option[DDSNode[_]]) = {
		readingLock.synchronized {
			val dataReader = node.get.dataReader
			val actor = node.get.actor
			val adapter = node.get.adapter
			val take = node.get.take						
			val dataSeq = adapter.dataSeq.newInstance()
			val infoSeq : SampleInfoSeq = new SampleInfoSeq
			
			condition match { 
				case condition : ReadCondition => {
					if(take)
						readData(dataReader, adapter, infoSeq, dataSeq, adapter.dataSeq, condition, adapter.takeCondition)
					else
						readData(dataReader, adapter, infoSeq, dataSeq, adapter.dataSeq, condition, adapter.readCondition)
				}
				case _ => {
					if(take)
						readData(dataReader, adapter, infoSeq, dataSeq, adapter.dataSeq, adapter.take)
					else
						readData(dataReader, adapter, infoSeq, dataSeq, adapter.dataSeq, adapter.read)
				}
			}
	
			val size: Int = adapter.dataSeqSize.invoke(dataSeq).asInstanceOf[Int]
			
	
			for (i <- 0 until size) {
				var info : SampleInfo = infoSeq.get(i).asInstanceOf[SampleInfo]
	
				if (info.valid_data) {
					val data = adapter.dataSeqGet.invoke(dataSeq,i.asInstanceOf[Object])
					actor ! data
				}
			}
			adapter.returnLoanMethod.invoke(dataReader, dataSeq.asInstanceOf[Object], infoSeq)
		}
	}
	
	def run() = {
		println("Thread: WaitSet " +ws)
		println("ConditionSeq: " +activeConditions.size()) // 0
		println("ConditionSeq (MAX):" +activeConditions.getMaximum())
		println("Alive: " +ws.is_alive())
		
		while (running) {	
			try {
				val retcode = ws.wait(activeConditions, timeout) // attesa sincrona
						
				var readKeys = readConditions.keys
				var statusKeys = statusConditions.keys
						
				for (i <- 0 until activeConditions.size()) {
				  
					if (activeConditions.get(i).isInstanceOf[ReadCondition]) {
						val cond = activeConditions.get(i).asInstanceOf[ReadCondition]
						val value = readConditions.get(cond)
						getSample(cond, value)
					}
					
					if (activeConditions.get(i).isInstanceOf[StatusCondition]) {
						val cond = activeConditions.get(i).asInstanceOf[StatusCondition]
						val value = statusConditions.get(cond)
						getSample(null, value)
					}
					
				}
					
			} catch {
						case timed_out : RETCODE_TIMEOUT => println("Timed out!" +timed_out)
						case no_data : RETCODE_NO_DATA => println("No data!" +no_data)
						case ex : RETCODE_ERROR => throw ex
			}
		}
		deleteAllConditions
	}
	
	private def deleteAllConditions = {
		lock.synchronized {
			for (readCondition <- readConditions.keySet) {
				removeReadCondition(readCondition) // detatch dal ws e dall'array
				val dataReader = readCondition.get_datareader()
				dataReader.delete_readcondition(readCondition)
			}
			
			for (statusCondition <- statusConditions.keySet)
				removeStatusCondition(statusCondition) // detatch dal ws e dall'array
		}
	}
	
	def stop() = {
		readingLock.synchronized {
			running = false
			releaseCondition.set_trigger_value(true) // sblocco il thread bloccato sulla wait
		}
	}
	
	
	}