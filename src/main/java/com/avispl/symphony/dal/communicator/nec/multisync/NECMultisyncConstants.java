/*
 * Copyright (c) 2020 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constants class for NEC MultiSync monitor properties.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 2/21/2024
 * @since 1.2.0
 */
class NECMultisyncConstants {
    final static String NUMBER_ONE = "1";
    final static String ZERO = "0";
    final static String NONE = "None";
    final static String EMPTY = "";
    final static String MESSAGE_ERROR = "Can't retrieve the data. ";

    //Command types HEX codes
    final static byte MSG_TYPE_CMD = 0x41;
    final static byte MSG_TYPE_CMD_REPLY = 0x42;
    final static byte MSG_TYPE_GET = 0x43;
    final static byte MSG_TYPE_GET_REPLY = 0x44;
    final static byte MSG_TYPE_SET = 0x45;
    final static byte MSG_TYPE_SET_REPLY = 0x46;

    //Commands HEX codes
    final static byte[] CMD_GET_POWER = {0x30,0x31,0x44,0x36};
    final static byte[] CMD_SET_POWER = {0x43,0x32,0x30,0x33,0x44,0x36};
    final static byte[] CMD_SELF_DIAG = { 0x42, 0x31 };
    final static byte[] CMD_SET_INPUT = { 0x31, 0x31, 0x30, 0x36 };
    final static byte[] CMD_GET_INPUT = { 0x30, 0x30, 0x36, 0x30 };
    final static byte[] CMD_SET_SENSOR = {0x30,0x32,0x37,0x38};
    final static byte[] CMD_GET_TEMP= {0x30,0x32,0x37,0x39};

    //Commands parameters HEX codes
    final static byte[] SENSOR_1 = {0x30,0x30,0x30,0x31};

    final static byte[] POWER_ON = {0x30,0x30,0x30,0x31};
    final static byte[] POWER_OFF = {0x30,0x30,0x30,0x34};

    //Replies Type HEX codes
    final static byte[] REP_RESERVED_DATA = {0x30,0x32};
    final static byte[] REP_RESULT_CODE_NO_ERROR = {0x30,0x30};
    final static byte[] REP_RESULT_CODE_NO_UNSUPPORTED = {0x30,0x31};

    //Replies HEX codes
    final static byte[] REP_POWER_STATUS_READ_Codes = {0x44,0x36};
    final static byte[] REP_POWER_CONTROL_Codes = {0x43,0x32,0x30,0x44,0x36};
    final static byte[] REP_SELF_DIAG_Codes = {0x41,0x31};

    //Power status values
    enum powerStatus {ON,STANDBY,SUSPEND,OFF}

    //Expected replies values
    enum responseValues {POWER_STATUS_READ,POWER_CONTROL,SELF_DIAG,INPUT_STATUS_READ,INPUT_CONTROL,GET_TEMPERATURE}

    //Diagnostics values
    enum diagResultNames {NORMAL,STB_POWER_3_3V_ABNORMALITY,STB_POWER_5V_ABNORMALITY,PANEL_POWER_12V_ABNORMALITY,INVERTER_POWER_24V_ABNORMALITY,
        FAN1_ABNORMALITY,FAN2_ABNORMALITY,FAN3_ABNORMALITY,LED_ABNORMALITY,TEMP_ABNORMALITY_SHUTDOWN,TEMP_ABNORMALITY_HALF_BRIGHT,MAX_TEMP_REACHED,NO_SIGNAL}

    //Diagnostics HEX codes map
    final static LinkedHashMap<diagResultNames, byte[]> DIAG_RESULT_CODES = new LinkedHashMap<diagResultNames,byte[]>() {{
        put(diagResultNames.NORMAL, new byte[] {0x30,0x30});
        put(diagResultNames.STB_POWER_3_3V_ABNORMALITY, new byte[] {0x37,0x30});
        put(diagResultNames.STB_POWER_5V_ABNORMALITY, new byte[] {0x37,0x31});
        put(diagResultNames.PANEL_POWER_12V_ABNORMALITY, new byte[] {0x37,0x32});
        put(diagResultNames.INVERTER_POWER_24V_ABNORMALITY, new byte[] {0x37,0x38});
        put(diagResultNames.FAN1_ABNORMALITY, new byte[] {0x38,0x30});
        put(diagResultNames.FAN2_ABNORMALITY, new byte[] {0x38,0x31});
        put(diagResultNames.FAN3_ABNORMALITY, new byte[] {0x38,0x32});
        put(diagResultNames.LED_ABNORMALITY, new byte[] {0x39,0x31});
        put(diagResultNames.TEMP_ABNORMALITY_SHUTDOWN, new byte[] {0x41,0x30});
        put(diagResultNames.TEMP_ABNORMALITY_HALF_BRIGHT, new byte[] {0x41,0x31});
        put(diagResultNames.MAX_TEMP_REACHED, new byte[] {0x41,0x32});
        put(diagResultNames.NO_SIGNAL, new byte[] {0x42,0x30});
    }};

    //Input Values
    enum inputNames {COMPUTE_MODULE, DVI1, DVI2, DPORT1, DPORT2, HDMI1, HDMI2, HDMI3, NO_SOURCE, OPTION, VIDEO1, VIDEO2, SVIDEO}

    //Inputs HEX codes map
    final static Map<inputNames, byte[]> inputs = new HashMap<inputNames, byte[]>() {{
        put(inputNames.NO_SOURCE, new byte[] { 0x30, 0x30, 0x30, 0x30 });
        put(inputNames.DVI1, new byte[] { 0x30, 0x30, 0x30, 0x33 });
        put(inputNames.DVI2, new byte[] { 0x30, 0x30, 0x30, 0x34 });
        put(inputNames.VIDEO1, new byte[] { 0x30, 0x30, 0x30, 0x35 });
        put(inputNames.VIDEO2, new byte[] { 0x30, 0x30, 0x30, 0x36 });
        put(inputNames.SVIDEO, new byte[] { 0x30, 0x30, 0x30, 0x37 });
        put(inputNames.HDMI1, new byte[] { 0x30, 0x30, 0x31, 0x31 });
        put(inputNames.HDMI2, new byte[] { 0x30, 0x30, 0x31, 0x32 });
        put(inputNames.HDMI3, new byte[] { 0x30, 0x30, 0x31, 0x33 });
        put(inputNames.DPORT1, new byte[] { 0x30, 0x30, 0x30, 0x46 });
        put(inputNames.DPORT2, new byte[] { 0x30, 0x30, 0x31, 0x30 });
        put(inputNames.OPTION, new byte[] { 0x30, 0x30, 0x30, 0x44 });
        put(inputNames.COMPUTE_MODULE, new byte[] { 0x30, 0x30, 0x38, 0x38 });
    }};

    enum controlProperties {Power, Input}
    enum statisticsProperties {Power, Diagnosis, Input, Temperature}
}
