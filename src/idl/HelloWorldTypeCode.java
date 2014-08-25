package idl;
/*
  WARNING: THIS FILE IS AUTO-GENERATED. DO NOT MODIFY.

  This file was generated from .idl using "rtiddsgen".
  The rtiddsgen tool is part of the RTI Connext distribution.
  For more information, type 'rtiddsgen -help' at a command shell
  or consult the RTI Connext manual.
*/
    
import com.rti.dds.typecode.*;


public class HelloWorldTypeCode {
    public static final TypeCode VALUE = getTypeCode();

    private static TypeCode getTypeCode() {
        TypeCode tc = null;
        int i=0;
        StructMember sm[] = new StructMember[3];

        sm[i]=new StructMember("prefix",false,(short)-1,false,(TypeCode)new TypeCode(TCKind.TK_STRING,(HELLODDS_MAX_STRING_SIZE.VALUE))); i++;
        sm[i]=new StructMember("sampleId",false,(short)-1,false,(TypeCode)TypeCode.TC_LONG); i++;
        sm[i]=new StructMember("payload",false,(short)-1,false,(TypeCode)new TypeCode((HELLODDS_MAX_PAYLOAD_SIZE.VALUE),TypeCode.TC_OCTET)); i++;

        tc = TypeCodeFactory.TheTypeCodeFactory.create_struct_tc("HelloWorld",ExtensibilityKind.EXTENSIBLE_EXTENSIBILITY,sm);
        return tc;
    }
}
