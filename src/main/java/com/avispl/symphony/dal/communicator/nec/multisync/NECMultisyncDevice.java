package com.avispl.symphony.dal.communicator.nec.multisync;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;

import java.util.*;

import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.*;

public class NECMultisyncDevice extends SocketCommunicator implements Controller, Monitorable {

    private int monitorID;

    public NECMultisyncDevice() {
        super();

        this.setPort(7142);
        this.monitorID = 1+64;

        // set list of command success strings (included at the end of response when command succeeds, typically ending with command prompt)
        this.setCommandSuccessList(Collections.singletonList("\r"));
        // set list of error response strings (included at the end of response when command fails, typically ending with command prompt)
        this.setCommandErrorList(Collections.singletonList("ERROR"));
    }


    @Override
    public void controlProperty(ControllableProperty controllableProperty) throws Exception {
        if (controllableProperty.getProperty().equals(controlProperties.power.name())){
            if(controllableProperty.getValue().toString().equals(controlPowerValues.ON.name())){
                powerON();
            }else if(controllableProperty.getValue().toString().equals(controlPowerValues.OFF.name())){
                powerOFF();
            }
        }else if (controllableProperty.getProperty().equals(controlProperties.input.name())){
            setInput(inputNames.valueOf(controllableProperty.getValue().toString()));
        }
    }

    @Override
    public void controlProperties(List<ControllableProperty> list) throws Exception {

    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {

        ExtendedStatistics extendedStatistics = new ExtendedStatistics();

        Map<String, String> controllable = new HashMap<String, String>(){{
            put(controlProperties.power.name(),"[ON,OFF]");
            put(controlProperties.input.name(),inputs.keySet().toString());
        }};

        Map<String, String> statistics = new HashMap<String, String>();

        try {
            statistics.put(statisticsProperties.power.name(), getPower().name());
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getPower", e);
            }
            throw e;
        }

        try {
            statistics.put(statisticsProperties.diagnosis.name(), getDiagResult().name());
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getDiagResult", e);
            }
            throw e;
        }

        try {
            statistics.put(statisticsProperties.input.name(), getInput().name());
        }catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during getInput", e);
            }
            throw e;
        }

        extendedStatistics.setControl(controllable);
        extendedStatistics.setStatistics(statistics);

        return Collections.singletonList(extendedStatistics);
    }

    public int getMonitorID() {
        return monitorID;
    }

    public void setMonitorID(int monitorID) {
        this.monitorID = monitorID+64;
    }

    private powerStatus getPower() throws Exception{
        String response = send(NECMultisyncUtils.buildSendString((char)monitorID,MSG_TYPE_CMD,CMD_GET_POWER));

        powerStatus power= (powerStatus)digestResponse(response,responseValues.POWER_STATUS_READ);

        if(power == null)
        {
            throw new Exception();
        }else{
            return power;
        }
    }

    private void powerON(){
        String toSend = NECMultisyncUtils.buildSendString((char)monitorID,MSG_TYPE_CMD,CMD_SET_POWER,POWER_ON);

        try {
            String response = send(toSend);

            digestResponse(response,responseValues.POWER_CONTROL);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during power ON send", e);
            }
        }
    }

    private void powerOFF(){
        String toSend = NECMultisyncUtils.buildSendString((char)monitorID,MSG_TYPE_CMD,CMD_SET_POWER,POWER_OFF);

        try {
            String response = send(toSend);

            digestResponse(response,responseValues.POWER_CONTROL);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during power OFF send", e);
            }
        }
    }

    private diagResultNames getDiagResult()throws Exception{

        String response = send(NECMultisyncUtils.buildSendString((char)monitorID,MSG_TYPE_CMD,CMD_SELF_DIAG));

        diagResultNames diagResult = (diagResultNames)digestResponse(response,responseValues.SELF_DIAG);

        if(diagResult == null)
        {
            throw new Exception();
        }else{
            return diagResult;
        }
    }

    private inputNames getInput()throws  Exception{
        String response = send(NECMultisyncUtils.buildSendString((char)monitorID,MSG_TYPE_GET,CMD_GET_INPUT));

        inputNames input = (inputNames)digestResponse(response,responseValues.INPUT_STATUS_READ);

        if(input == null)
        {
            throw new Exception();
        }else{
            return input;
        }
    }

    private void setInput(inputNames i){
        String toSend = NECMultisyncUtils.buildSendString((char)monitorID,MSG_TYPE_SET,CMD_SET_INPUT,inputs.get(i));

        try {
            String response = send(toSend);

            digestResponse(response,responseValues.INPUT_CONTROL);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during setInput send", e);
            }
        }
    }

    private Enum digestResponse(String response, responseValues expectedResponse){

        int responseSourceID = response.charAt(3) - 64;

        char responseMessageType = response.charAt(4);

        //int responseMessageLength =  Integer.parseInt(response.substring(5,7),16);

        if(((byte) response.charAt(response.length()-2)) == NECMultisyncUtils.xor((response.substring(1,response.length()-2)).getBytes())){
            if(responseMessageType == MSG_TYPE_CMD_REPLY){
                if(Arrays.equals(response.substring(8,10).toCharArray(),REP_RESERVED_DATA)){
                    if(Arrays.equals(response.substring(10,12).toCharArray(),REP_RESULT_CODE_NO_ERROR)){
                        if(Arrays.equals(response.substring(12,14).toCharArray(),REP_POWER_STATUS_READ_Codes) && expectedResponse == responseValues.POWER_STATUS_READ){
                            powerStatus power = powerStatus.values()[Character.digit(response.charAt(23),16)-1];
                            return power;
                        }
                    }else if(Arrays.equals(response.substring(10,12).toCharArray(),REP_RESULT_CODE_NO_UNSUPPORTED)){
                        //error
                    }
                }else if(Arrays.equals(response.substring(8,10).toCharArray(),REP_RESULT_CODE_NO_ERROR)){

                    if(Arrays.equals(response.substring(10,16).toCharArray(),REP_POWER_CONTROL_Codes) && expectedResponse == responseValues.POWER_CONTROL) {
                        powerStatus power = powerStatus.values()[Character.digit(response.charAt(19),16)-1];
                        return power;
                    }
                }else if(Arrays.equals(response.substring(8,10).toCharArray(),REP_RESULT_CODE_NO_UNSUPPORTED)){
                    //error
                }else if(Arrays.equals(response.substring(8,10).toCharArray(),REP_SELF_DIAG_Codes) && expectedResponse == responseValues.SELF_DIAG){

                    for(Map.Entry<diagResultNames,String> entry : DIAG_RESULT_CODES.entrySet()){
                        if(Arrays.equals(response.substring(10,12).toCharArray(),entry.getValue().toCharArray())){
                            diagResultNames diagResult = entry.getKey();
                            return diagResult;
                        }
                    }
                }
            }else if(responseMessageType == MSG_TYPE_GET_REPLY){
                if(Arrays.equals(response.substring(8,10).toCharArray(),REP_RESULT_CODE_NO_ERROR)){
                    if(Arrays.equals(response.substring(10,14).toCharArray(),CMD_GET_INPUT.toCharArray()))
                    {
                        for(Map.Entry<inputNames,String> entry: inputs.entrySet())
                        {
                            if(Arrays.equals(response.substring(20,24).toCharArray(),entry.getValue().toCharArray()))
                            {
                                inputNames input = entry.getKey();
                                return input;
                            }
                        }
                    }

                }else
                {
                    //Error
                }
            }
        }else{//Wrong BCC
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error: wrong BCC");
            }
        }
        return null;
    }
}
