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

import akka.actor.Actor
import com.rti.dds.publication.DataWriter
import com.rti.dds.infrastructure.InstanceHandle_t
import java.lang.reflect.Method
import com.rti.dds.infrastructure.RETCODE_ERROR
import scala.reflect.ClassTag
import akka.actor.ActorLogging
import java.io.PrintWriter
import java.io.File
import java.sql.Timestamp
import idl.HelloWorld

class DWActor[T <: Object : ClassTag](dw : DataWriter) extends Actor with ActorLogging{
	val datawriter = dw
	var count = 0
	
	def receive = {
		case x : HelloWorld => {
			val dataWriterClass = datawriter.getClass
			val method : Method = dataWriterClass.getMethod("write", x.getClass, InstanceHandle_t.HANDLE_NIL.getClass())
			try {
				method.invoke(datawriter, x, InstanceHandle_t.HANDLE_NIL)
				count += 1
				if (count % 1000 == 0){
					log.info("Inviato messaggio {}", count)
				}
			} catch {
			case timed_out : RETCODE_ERROR => println("Write Error!" +timed_out)
			}
	  	}
				
		case _ => throw new Error("Not a known message")

	}
	
	override def postStop() {
		println("Inviati " +count +" messaggi")
		println("%s has stopped".format(self.path.name))

	}
}