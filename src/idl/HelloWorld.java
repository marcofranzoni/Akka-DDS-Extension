package idl;
/*
  WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

  This file was generated from .idl using "rtiddsgen".
  The rtiddsgen tool is part of the RTI Connext distribution.
  For more information, type 'rtiddsgen -help' at a command shell
  or consult the RTI Connext manual.
*/
    

import com.rti.dds.infrastructure.*;
import com.rti.dds.infrastructure.Copyable;

import java.io.Serializable;
import com.rti.dds.cdr.CdrHelper;


public class HelloWorld implements Copyable, Serializable
{

    public String prefix = ""; /* maximum length = ((HELLODDS_MAX_STRING_SIZE.VALUE)) */
    public int sampleId;
    public ByteSeq payload = new ByteSeq(((HELLODDS_MAX_PAYLOAD_SIZE.VALUE)));


    public HelloWorld() {

    }


    public HelloWorld(HelloWorld other) {

        this();
        copy_from(other);
    }



    public static Object create() {
        return new HelloWorld();
    }

    public boolean equals(Object o) {
                
        if (o == null) {
            return false;
        }        
        
        

        if(getClass() != o.getClass()) {
            return false;
        }

        HelloWorld otherObj = (HelloWorld)o;



        if(!prefix.equals(otherObj.prefix)) {
            return false;
        }
            
        if(sampleId != otherObj.sampleId) {
            return false;
        }
            
        if(!payload.equals(otherObj.payload)) {
            return false;
        }
            
        return true;
    }

    public int hashCode() {
        int __result = 0;

        __result += prefix.hashCode();
                
        __result += (int)sampleId;
                
        __result += payload.hashCode();
                
        return __result;
    }
    

    /**
     * This is the implementation of the <code>Copyable</code> interface.
     * This method will perform a deep copy of <code>src</code>
     * This method could be placed into <code>HelloWorldTypeSupport</code>
     * rather than here by using the <code>-noCopyable</code> option
     * to rtiddsgen.
     * 
     * @param src The Object which contains the data to be copied.
     * @return Returns <code>this</code>.
     * @exception NullPointerException If <code>src</code> is null.
     * @exception ClassCastException If <code>src</code> is not the 
     * same type as <code>this</code>.
     * @see com.rti.dds.infrastructure.Copyable#copy_from(java.lang.Object)
     */
    public Object copy_from(Object src) {
        

        HelloWorld typedSrc = (HelloWorld) src;
        HelloWorld typedDst = this;

        typedDst.prefix = typedSrc.prefix;
            
        typedDst.sampleId = typedSrc.sampleId;
            
        typedDst.payload.copy_from(typedSrc.payload);
            
        return this;
    }


    
    public String toString(){
        return toString("", 0);
    }
        
    
    public String toString(String desc, int indent) {
        StringBuffer strBuffer = new StringBuffer();        
                        
        
        if (desc != null) {
            CdrHelper.printIndent(strBuffer, indent);
            strBuffer.append(desc).append(":\n");
        }
        
        
        CdrHelper.printIndent(strBuffer, indent+1);            
        strBuffer.append("prefix: ").append(prefix).append("\n");
            
        CdrHelper.printIndent(strBuffer, indent+1);            
        strBuffer.append("sampleId: ").append(sampleId).append("\n");
            
        CdrHelper.printIndent(strBuffer, indent+1);
        strBuffer.append("payload: ");
        for(int i__ = 0; i__ < payload.size(); ++i__) {
            if (i__!=0) strBuffer.append(", ");        
            strBuffer.append(payload.get(i__));
        }
        strBuffer.append("\n");
            
        return strBuffer.toString();
    }
    
}

