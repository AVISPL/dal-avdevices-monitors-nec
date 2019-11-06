package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class NECMultisyncConstants {

    final static char MSG_TYPE_CMD = 0x41;
    final static char MSG_TYPE_CMD_REPLY = 0x42;
    final static char MSG_TYPE_GET = 0x43;
    final static char MSG_TYPE_GET_REPLY = 0x44;
    final static char MSG_TYPE_SET = 0x45;
    final static char MSG_TYPE_SET_REPLY = 0x46;

    final static String CMD_GET_POWER = "01D6";
    final static String CMD_SET_POWER = "C203D6";
    final static String CMD_SELF_DIAG = "B1";
    final static String CMD_SET_INPUT = "0060";
    final static String CMD_GET_INPUT = "0060";

    final static char[] REP_RESERVED_DATA = {0x30,0x32};
    final static char[] REP_RESULT_CODE_NO_ERROR = {0x30,0x30};
    final static char[] REP_RESULT_CODE_NO_UNSUPPORTED = {0x30,0x31};

    final static char[] REP_POWER_STATUS_READ_Codes = {0x44,0x36};
    final static char[] REP_POWER_CONTROL_Codes = {0x43,0x32,0x30,0x44,0x36};
    final static char[] REP_SELF_DIAG_Codes = {0x41,0x31};

    final static String POWER_ON = "0001";
    final static String POWER_OFF = "0004";

    enum powerStatus {ON,STANDBY,SUSPEND,OFF}
    enum responseValues {POWER_STATUS_READ,POWER_CONTROL,SELF_DIAG,INPUT_STATUS_READ,INPUT_CONTROL}

    enum diagResultNames {NORMAL,STB_POWER_3_3V_ABNORMALITY,STB_POWER_5V_ABNORMALITY,PANEL_POWER_12V_ABNORMALITY,INVERTER_POWER_24V_ABNORMALITY,
        FAN1_ABNORMALITY,FAN2_ABNORMALITY,FAN3_ABNORMALITY,LED_ABNORMALITY,TEMP_ABNORMALITY_SHUTDOWN,TEMP_ABNORMALITY_HALF_BRIGHT,MAX_TEMP_REACHED,NO_SIGNAL}

    final static LinkedHashMap<diagResultNames, String> DIAG_RESULT_CODES = new LinkedHashMap<diagResultNames,String>() {{
        put(diagResultNames.NORMAL, new String(new byte[] {0x30,0x30}));
        put(diagResultNames.STB_POWER_3_3V_ABNORMALITY, new String(new byte[] {0x37,0x30}));
        put(diagResultNames.STB_POWER_5V_ABNORMALITY, new String(new byte[] {0x37,0x31}));
        put(diagResultNames.PANEL_POWER_12V_ABNORMALITY, new String(new byte[] {0x37,0x32}));
        put(diagResultNames.INVERTER_POWER_24V_ABNORMALITY, new String(new byte[] {0x37,0x38}));
        put(diagResultNames.FAN1_ABNORMALITY, new String(new byte[] {0x38,0x30}));
        put(diagResultNames.FAN2_ABNORMALITY, new String(new byte[] {0x38,0x31}));
        put(diagResultNames.FAN3_ABNORMALITY, new String(new byte[] {0x38,0x32}));
        put(diagResultNames.LED_ABNORMALITY, new String(new byte[] {0x39,0x31}));
        put(diagResultNames.TEMP_ABNORMALITY_SHUTDOWN, new String(new byte[] {0x41,0x30}));
        put(diagResultNames.TEMP_ABNORMALITY_HALF_BRIGHT, new String(new byte[] {0x41,0x31}));
        put(diagResultNames.MAX_TEMP_REACHED, new String(new byte[] {0x41,0x32}));
        put(diagResultNames.NO_SIGNAL, new String(new byte[] {0x42,0x30}));
    }};

    enum inputNames {NOSOURCE,VGA,VGAHV,DVI,VIDEO1,VIDEO2,SVIDEO,COMP1,COMP2,TV,HDMI1,HDMI2,HDMI3,DPORT}
    final static Map<inputNames, String> inputs = new HashMap<inputNames, String>() {{
        put(inputNames.NOSOURCE, new String(new byte[] {0x30,0x30,0x30,0x30}));
        put(inputNames.VGA, new String(new byte[] {0x30,0x30,0x30,0x31}));
        put(inputNames.VGAHV, new String(new byte[] {0x30,0x30,0x30,0x32}));
        put(inputNames.DVI, new String(new byte[] {0x30,0x30,0x30,0x33}));
        put(inputNames.VIDEO1, new String(new byte[] {0x30,0x30,0x30,0x35}));
        put(inputNames.VIDEO2, new String(new byte[] {0x30,0x30,0x30,0x36}));
        put(inputNames.SVIDEO, new String(new byte[] {0x30,0x30,0x30,0x37}));
        put(inputNames.COMP1, new String(new byte[] {0x30,0x30,0x30,0x43}));
        put(inputNames.COMP2, new String(new byte[] {0x30,0x30,0x30,0x34}));
        put(inputNames.TV, new String(new byte[] {0x30,0x30,0x30,0x41}));
        put(inputNames.HDMI1, new String(new byte[] {0x30,0x30,0x31,0x31}));
        put(inputNames.HDMI2, new String(new byte[] {0x30,0x30,0x31,0x32}));
        put(inputNames.HDMI3, new String(new byte[] {0x30,0x30,0x31,0x33}));
        put(inputNames.DPORT, new String(new byte[] {0x30,0x30,0x30,0x46}));
    }};

    enum controlProperties {power,input}
    enum controlPowerValues {ON,OFF}
    enum statisticsProperties {power,diagnosis,input}
}
