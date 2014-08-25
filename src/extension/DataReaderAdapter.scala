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

import scala.reflect.ClassTag
import scala.collection.mutable
import java.lang.reflect.Method
import com.rti.dds.subscription.DataReader
import com.rti.dds.subscription.SampleInfoSeq
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy
import com.rti.dds.subscription.SampleStateKind
import com.rti.dds.subscription.ViewStateKind
import com.rti.dds.subscription.InstanceStateKind
import com.rti.dds.subscription.SampleInfo
import com.rti.dds.subscription.ReadCondition

object DataReaderAdapter {
	def apply[T : ClassTag] = new DataReaderAdapter[T]
}

class DataReaderAdapter[T : ClassTag] {

	val dataReader = "DataReader"
			val typeSupport = "TypeSupport"	
			val seq = "Seq"
			val domainParticipant = "com.rti.dds.domain.DomainParticipant"
			val infoSeq = "com.rti.dds.subscription.SampleInfoSeq"
			val getTypeName = "get_type_name"
			val registerType = "register_type"
			val read = "read"
			val take = "take"
			val readCondition = "read_w_condition"
			val takeCondition = "take_w_condition"
			val size = "size"
			val get = "get"

			val ddsDataType = implicitly[ClassTag[T]].runtimeClass
			val dataReaderString = ddsDataType.getName() + dataReader
			val dataTypeSupportString = ddsDataType.getName() + typeSupport
			val dataSeqString = ddsDataType.getName() + seq

			val getTypeSupport : Class[_] = Class.forName(dataTypeSupportString)

			lazy val returnLoanMethod : Method =  Class.forName(dataReaderString).getMethod("return_loan", dataSeq, classOf[SampleInfoSeq])

			val typeNameMethod : Method = getTypeSupport.getMethod(getTypeName)

			val registerMethod : Method = getTypeSupport.getMethod(registerType, Class.forName(domainParticipant), classOf[String])

			val getTypeDomainParticipant : Class[_] = Class.forName(domainParticipant)

			val dataSeq : Class[_] = Class.forName(dataSeqString)

			val dataSeqSize : Method = dataSeq.getMethod(size)

			val dataSeqGet : Method = dataSeq.getMethod(get, Integer.TYPE)

			val readMethod : Method = Class.forName(dataReaderString).getMethod(read,
					dataSeq,
					Class.forName(infoSeq),
					ResourceLimitsQosPolicy.LENGTH_UNLIMITED.getClass(),
					SampleStateKind.NOT_READ_SAMPLE_STATE.getClass(),
					ViewStateKind.ANY_VIEW_STATE.getClass(),
					InstanceStateKind.ANY_INSTANCE_STATE.getClass())

					val takeMethod : Method = Class.forName(dataReaderString).getMethod(take,
							dataSeq,
							Class.forName(infoSeq),
							ResourceLimitsQosPolicy.LENGTH_UNLIMITED.getClass(),
							SampleStateKind.NOT_READ_SAMPLE_STATE.getClass(),
							ViewStateKind.ANY_VIEW_STATE.getClass(),
							InstanceStateKind.ANY_INSTANCE_STATE.getClass())

							val readConditionMethod : Method = Class.forName(dataReaderString).getMethod(readCondition,
									dataSeq,
									Class.forName(infoSeq),
									ResourceLimitsQosPolicy.LENGTH_UNLIMITED.getClass(),
									classOf[ReadCondition])

									val takeConditionMethod : Method = Class.forName(dataReaderString).getMethod(takeCondition,
											dataSeq,
											Class.forName(infoSeq),
											ResourceLimitsQosPolicy.LENGTH_UNLIMITED.getClass(),
											classOf[ReadCondition])

}