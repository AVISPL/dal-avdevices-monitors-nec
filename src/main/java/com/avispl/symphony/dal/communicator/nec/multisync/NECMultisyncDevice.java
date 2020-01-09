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
            if(controllableProperty.getValue().toString().equals("1")){
                powerON();
            }else if(controllableProperty.getValue().toString().equals("0")){
                powerOFF();
            }
        }
    }

    @Override
    public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
        controllableProperties.stream().forEach(p -> {
            try {
                controlProperty(p);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getting statistics");
        }

        ExtendedStatistics extendedStatistics = new ExtendedStatistics();

        Map<String, String> controllable = new HashMap<String, String>(){{
            put(controlProperties.power.name(),"Toggle");
        }};

        Map<String, String> statistics = new HashMap<String, String>();

        String power;

        try {
            power = getPower().name();
            if(power.compareTo("ON") == 0) {
                statistics.put(statisticsProperties.power.name(), "1");
            }else if(power.compareTo("OFF") == 0)
            {
                statistics.put(statisticsProperties.power.name(), "0");
            }
            //statistics.put(statisticsProperties.power.name(), getPower().name());
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

        if(this.logger.isDebugEnabled()) {
            for (String s : controllable.keySet()) {
                this.logger.debug("controllable key: " + s + ",value: " + controllable.get(s));
            }
        }

        if(this.logger.isDebugEnabled()) {
            for (String s : statistics.keySet()) {
                this.logger.debug("statistics key: " + s + ",value: " + statistics.get(s));
            }
        }

        //return Collections.singletonList(extendedStatistics);
        return new ArrayList<Statistics>(Collections.singleton(extendedStatistics));
    }

    public int getMonitorID() {
        return monitorID;
    }

    public void setMonitorID(int monitorID) {
        this.monitorID = monitorID+64;
    }

    private powerStatus getPower() throws Exception{

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getting power");
        }

        byte[]  response = send(NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_CMD,CMD_GET_POWER));

        powerStatus power= (powerStatus)digestResponse(response,responseValues.POWER_STATUS_READ);

        if(power == null)
        {
            throw new Exception();
        }else{
            return power;
        }
    }

    private void powerON(){

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("powerON");
        }

        byte[] toSend = NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_CMD,CMD_SET_POWER,POWER_ON);

        try {
            byte[] response = send(toSend);

            digestResponse(response,responseValues.POWER_CONTROL);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during power ON send", e);
            }
        }
    }

    private void powerOFF(){

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("powerOFF");
        }

        byte[] toSend = NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_CMD,CMD_SET_POWER,POWER_OFF);

        try {
            byte[] response = send(toSend);

            digestResponse(response,responseValues.POWER_CONTROL);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during power OFF send", e);
            }
        }
    }

    private diagResultNames getDiagResult()throws Exception{

        byte[] response = send(NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_CMD,CMD_SELF_DIAG));

        diagResultNames diagResult = (diagResultNames)digestResponse(response,responseValues.SELF_DIAG);

        if(diagResult == null)
        {
            throw new Exception();
        }else{
            return diagResult;
        }
    }

    private inputNames getInput()throws  Exception{
        byte[] response = send(NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_GET,CMD_GET_INPUT));

        inputNames input = (inputNames)digestResponse(response,responseValues.INPUT_STATUS_READ);

        if(input == null)
        {
            throw new Exception();
        }else{
            return input;
        }
    }

    private void setInput(inputNames i){
        byte[] toSend = NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_SET,CMD_SET_INPUT,inputs.get(i));

        try {
            byte[] response = send(toSend);

            digestResponse(response,responseValues.INPUT_CONTROL);
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during setInput send", e);
            }
        }
    }

    private Enum digestResponse(byte[] response, responseValues expectedResponse){

        byte responseMessageType = response[4];

        //int responseMessageLength =  Integer.parseInt(response.substring(5,7),16);

        Arrays.copyOfRange(response,1,response.length-2);

        if(response[response.length-2] == NECMultisyncUtils.xor(Arrays.copyOfRange(response,1,response.length-2))){
            if(responseMessageType == MSG_TYPE_CMD_REPLY){
                if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_RESERVED_DATA)){
                    if(Arrays.equals(Arrays.copyOfRange(response,10,12),REP_RESULT_CODE_NO_ERROR)){
                        if(Arrays.equals(Arrays.copyOfRange(response,12,14),REP_POWER_STATUS_READ_Codes) && expectedResponse == responseValues.POWER_STATUS_READ){
                            powerStatus power = powerStatus.values()[Character.getNumericValue((char)response[23])-1];
                            return power;
                        }
                    }else if(Arrays.equals(Arrays.copyOfRange(response,10,12),REP_RESULT_CODE_NO_UNSUPPORTED)){
                        //error
                    }
                }else if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_RESULT_CODE_NO_ERROR)){

                    if(Arrays.equals(Arrays.copyOfRange(response,10,16),REP_POWER_CONTROL_Codes) && expectedResponse == responseValues.POWER_CONTROL) {
                        powerStatus power = powerStatus.values()[Character.getNumericValue((char)response[19])-1];
                        return power;
                    }
                }else if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_RESULT_CODE_NO_UNSUPPORTED)){
                    //error
                }else if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_SELF_DIAG_Codes) && expectedResponse == responseValues.SELF_DIAG){

                    for(Map.Entry<diagResultNames,byte[]> entry : DIAG_RESULT_CODES.entrySet()){
                        if(Arrays.equals(Arrays.copyOfRange(response,10,12),entry.getValue())){
                            diagResultNames diagResult = entry.getKey();
                            return diagResult;
                        }
                    }
                }
            }else if(responseMessageType == MSG_TYPE_GET_REPLY){
                if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_RESULT_CODE_NO_ERROR)){
                    if(Arrays.equals(Arrays.copyOfRange(response,10,14),CMD_GET_INPUT))
                    {
                        for(Map.Entry<inputNames,byte[]> entry: inputs.entrySet())
                        {
                            if(Arrays.equals(Arrays.copyOfRange(response,20,24),entry.getValue()))
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
