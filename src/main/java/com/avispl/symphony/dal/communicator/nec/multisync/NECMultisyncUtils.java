package com.avispl.symphony.dal.communicator.nec.multisync;

public class NECMultisyncUtils {
    private final static char SOH = 0x01;
    private final static char STX = 0x02;
    private final static char ETX = 0x03;
    private final static char RESERVED = 0x30;
    private final static char CTRL_ADDR = 0x30;

    //Calculate an xor checksum of a byte[], return a byte
    static byte xor(byte bytes[]){
        byte checkSum = 0;

        for( byte s : bytes){
            checkSum = (byte) (checkSum ^ s);
        }

        return checkSum;
    }

    //print a String in HEX format
    protected static String getHexString(String str){
        StringBuilder sBld = new StringBuilder();

        int j =0;
        sBld.append("[");
        for(byte b: str.getBytes()) {
            sBld.append(String.format("%02x",b));
            j++;
            if(j<str.length())
                sBld.append(", ");
        }
        sBld.append("]");

        return sBld.toString();
    }

    //build a string to be sent according to the NEC Protocol
    static String buildSendString(char monitorID, char messageType, String command){
        return buildSendString(monitorID,messageType,command,null);
    }

    static String buildSendString(char monitorID, char messageType, String command, String param){

        //build message first as it's size is required in header
        String message = new String();

        if(param != null) {
            message = message + STX + command + param + ETX;
        }else{
            message = message + STX + command + ETX;
        }

        //build header
        String header = new String();
        header = header + SOH + RESERVED + monitorID + CTRL_ADDR + messageType + String.format("%02x", message.length());

        return header + message + (char) xor((header.substring(1) + message).getBytes()) + '\r';
    }
}
