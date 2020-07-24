/*
 * Copyright (c) 2020 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.ArrayList;
import java.util.List;

public class NECMultisyncUtils {

    //few constants used to build the commands
    private final static byte SOH = 0x01;
    private final static byte STX = 0x02;
    private final static byte ETX = 0x03;
    private final static byte RESERVED = 0x30;
    private final static byte CTRL_ADDR = 0x30;
    private final static byte CARRIAGE_RETURN = 0x0D;

    /**
     * This method is used to calculate the xor checksum of a byte array
     * @param bytes This is the list of bytes against which the checksum should be calculated
     * @return byte This returns the calculated xor checksum.
     */
    static byte xor(byte bytes[]){
        byte checkSum = 0;

        for( byte s : bytes){
            checkSum = (byte) (checkSum ^ s);
        }

        return checkSum;
    }

    /**
     * This method is used to convert a String in HEX format (for printing purposes)
     * @param str This is the String to be converted in Hex format
     * @return String This returns the converted String
     */
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

    /**
     * This method is used to build a string to be sent according to the NEC Protocol (See bellow)
     */
    static byte[] buildSendString(byte monitorID, byte messageType, byte[] command){
        return buildSendString(monitorID,messageType,command,null);
    }

    /**
     * This method is used to build a string to be sent according to the NEC Protocol
     * @param monitorID This is byte representing the monitro ID (1=41h -> 26=5Ah, ALL = 2Ah)
     * @param messageType This is byte representing the type of message to be sent (see NECMultisyncConstants)
     * @param command This is the byte array reprensenting the command to be sent (see NECMultisyncConstants)
     * @param param This is the byte array reprensenting the parameter values to be sent (see NECMultisyncConstants)
     * @return byte[] This returns the string to be sent to the NEC display
     */
    static byte[] buildSendString(byte monitorID, byte messageType, byte[] command, byte[] param){
        List<Byte> bytes = new ArrayList<>();

        List<Byte> header = new ArrayList<>();
        List<Byte> message = new ArrayList<>();


        //build message first as it's size is required in header

        message.add(STX);
        for(byte b:command){
            message.add(b);
        }
        if(param != null) {
            for(byte b:param){
                message.add(b);
            }
        }
        message.add(ETX);

        //build header

        header.add(SOH);
        header.add(RESERVED);
        header.add(monitorID);
        header.add(CTRL_ADDR);
        header.add(messageType);

        String messageSize =String.format("%02x", message.size());
        header.add(messageSize.getBytes()[0]);
        header.add(messageSize.getBytes()[1]);

        bytes.addAll(header);
        bytes.addAll(message);

        byte[] checksumBytes = new byte[header.size()-1+message.size()];
        for (int i = 1; i < header.size(); i++) {
            checksumBytes[i-1] = header.get(i);
        }
        for (int i = 0; i < message.size(); i++) {
            checksumBytes[i+header.size()-1] = message.get(i);
        }

        bytes.add(xor(checksumBytes));
        bytes.add(CARRIAGE_RETURN);

        byte[] byteArray = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            byteArray[i] = bytes.get(i);
        }

        return byteArray;
    }
}
