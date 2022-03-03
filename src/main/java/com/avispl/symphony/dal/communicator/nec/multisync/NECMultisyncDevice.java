/*
 * Copyright (c) 2020 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.nec.multisync;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.SocketCommunicator;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.*;

public class NECMultisyncDevice extends SocketCommunicator implements Controller, Monitorable {

    private int monitorID;

    private final int STARTUP_SHUTDOWN_COOLDOWN = 3000;
    private long latestShutdownStartupTimestamp;

    private ExtendedStatistics localStatistics;

    private final ReentrantLock powerLock = new ReentrantLock();

    /**
     * Constructor set the TCP/IP port to be used as well the default monitor ID
     */
    public NECMultisyncDevice() {
        super();

        this.setPort(7142);
        this.monitorID = 1+64;

        // set list of command success strings (included at the end of response when command succeeds, typically ending with command prompt)
        this.setCommandSuccessList(Collections.singletonList("\r"));
        // set list of error response strings (included at the end of response when command fails, typically ending with command prompt)
        this.setCommandErrorList(Collections.singletonList("ERROR"));
    }


    /**
     * This method is recalled by Symphony to control specific property
     * @param controllableProperty This is the property to be controled
     */
    @Override
    public void controlProperty(ControllableProperty controllableProperty) throws Exception {
        powerLock.lock();
        try {
            if (controllableProperty.getProperty().equals(controlProperties.power.name())){
                if(controllableProperty.getValue().toString().equals("1")){
                    powerSwitch(POWER_ON);
                }else if(controllableProperty.getValue().toString().equals("0")){
                    powerSwitch(POWER_OFF);
                }
            }
        } finally {
            powerLock.unlock();
        }
    }

    /**
     * This method is recalled by Symphony to control a list of properties
     * @param controllableProperties This is the list of properties to be controlled
     * @return byte This returns the calculated xor checksum.
     */
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

    /**
     * This method is recalled by Symphony to get the list of statistics to be displayed
     * @return List<Statistics> This return the list of statistics.
     */
    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {

        ExtendedStatistics extendedStatistics = new ExtendedStatistics();
        powerLock.lock();
        try {
            if (isValidShutdownStartupCooldown() && localStatistics != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Device is occupied. Skipping statistics refresh call.");
                }
                extendedStatistics.setStatistics(localStatistics.getStatistics());
                extendedStatistics.setControllableProperties(localStatistics.getControllableProperties());
                return Collections.singletonList(extendedStatistics);
            }

            //controls
            List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();
            //statistics
            Map<String, String> statistics = new HashMap<>();

            //controllable properties
            AdvancedControllableProperty.Switch powerSwitch = new AdvancedControllableProperty.Switch();
            powerSwitch.setLabelOn("On");
            powerSwitch.setLabelOff("Off");

            //getting power status from device
            String power;

            try {
                statistics.put(statisticsProperties.power.name(), "");
                power = getPower().name();
                if (power.contains("ON")) {
                    advancedControllableProperties.add(new AdvancedControllableProperty(statisticsProperties.power.name(), new Date(), powerSwitch, "1"));
                } else {
                    advancedControllableProperties.add(new AdvancedControllableProperty(statisticsProperties.power.name(), new Date(), powerSwitch, "0"));
                }
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getPower", e);
                }
                throw e;
            }

            //getting diagnostic result from device
            try {
                statistics.put(statisticsProperties.diagnosis.name(), getDiagResult().name());
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getDiagResult", e);
                }
                throw e;
            }

            //getting current device input
            try {
                statistics.put(statisticsProperties.input.name(), getInput().name());
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getInput", e);
                }
                throw e;
            }

            //getting device temperature
            try {
                statistics.put(statisticsProperties.temperature.name(), String.valueOf(getTemperature()));
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getTemperature", e);
                }
                throw e;
            }

            extendedStatistics.setControllableProperties(advancedControllableProperties);
            extendedStatistics.setStatistics(statistics);

            localStatistics = extendedStatistics;
            //Displays the generated list of controllable and statistics properties for debugging purposes
            if (this.logger.isDebugEnabled()) {
                for (AdvancedControllableProperty controlProperty : advancedControllableProperties) {
                    this.logger.debug("controllable key: " + controlProperty.getName() + ",value: " + controlProperty.getValue());
                }
                for (String s : statistics.keySet()) {
                    this.logger.debug("statistics key: " + s + ",value: " + statistics.get(s));
                }
            }
        } finally {
            powerLock.unlock();
        }
        return Collections.singletonList(extendedStatistics);
    }

    /**
     * This method is recalled by Symphony to get the current monitor ID (Future purpose)
     * @return int This returns the current monitor ID.
     */
    public int getMonitorID() {
        return monitorID;
    }

    /**
     * This method is is used by Symphony to set the monitor ID (FUture purpose)
     * @param monitorID This is the monitor ID to be set
     */
    public void setMonitorID(int monitorID) {
        this.monitorID = monitorID+64;
    }

    /**
     * This method is used to get the current display temperature (from sensor 1)
     * @return int This returns the retreived temperature.
     */
    private int getTemperature() throws Exception{

        //setting the sensor to retreive the temperature from
        send(NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_SET,CMD_SET_SENSOR,SENSOR_1));

        //send the get temperature command
        byte[]  response = send(NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_GET,CMD_GET_TEMP));

        //digest the result and returns the temperature
        return (Integer) digestResponse(response,responseValues.GET_TEMPERATURE);
    }

    /**
     * This method is used to get the current display power status
     * @return powerStatus This returns the calculated xor checksum.
     */
    private powerStatus getPower() throws Exception{
        //sending the get power command
        byte[]  response = send(NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_CMD,CMD_GET_POWER));

        //digest the result
        powerStatus power= (powerStatus)digestResponse(response,responseValues.POWER_STATUS_READ);

        if(power == null)
        {
            throw new Exception();
        }else{
            return power;
        }
    }


    /**
     * This method is used to send the power ON/OFF command to the display
     */
    private void powerSwitch(byte[] command){
        byte[] toSend = NECMultisyncUtils.buildSendString((byte)monitorID,MSG_TYPE_CMD,CMD_SET_POWER,command);

        try {
            byte[] response = send(toSend);
            //digesting the response but voiding the result
            digestResponse(response,responseValues.POWER_CONTROL);

            updateShutdownStartupTimestamp();
            localStatistics.getStatistics().put(statisticsProperties.power.name(), Arrays.equals(command, POWER_ON) ? "1" : "0");
            localStatistics.getControllableProperties().stream().filter(acp -> acp.getName().equals(
                    statisticsProperties.power.name())).findFirst().ifPresent(acp -> {
                        acp.setTimestamp(new Date());
                        acp.setValue(Arrays.equals(command, POWER_ON) ? "1" : "0");
            });
        } catch (Exception e) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("error during power switch operation", e);
            }
        }
    }

    /**
     * This method is used to get the diagnostics results from the display
     * @return diagResultNames This returns the retrieved diagnostic results.
     */
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

    /**
     * This method is used to get the current display input
     * @return inputNames This returns the current input.
     */
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

    /**
     * This method is used to digest the response received from the device
     * @param response This is the response to be digested
     * @param expectedResponse This is the expected response type to be compared with received
     * @return Object This returns the result digested from the response.
     */
    private Object digestResponse(byte[] response, responseValues expectedResponse){

        byte responseMessageType = response[4];

        Arrays.copyOfRange(response,1,response.length-2);

        //checksum verification
        if(response[response.length-2] == NECMultisyncUtils.xor(Arrays.copyOfRange(response,1,response.length-2))){
            if(responseMessageType == MSG_TYPE_CMD_REPLY){
                if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_RESERVED_DATA)){
                    if(Arrays.equals(Arrays.copyOfRange(response,10,12),REP_RESULT_CODE_NO_ERROR)){
                        if(Arrays.equals(Arrays.copyOfRange(response,12,14),REP_POWER_STATUS_READ_Codes) && expectedResponse == responseValues.POWER_STATUS_READ){
                            powerStatus power = powerStatus.values()[Character.getNumericValue((char)response[23])-1];
                            return power;
                        }
                    }else if(Arrays.equals(Arrays.copyOfRange(response,10,12),REP_RESULT_CODE_NO_UNSUPPORTED)){
                        if (this.logger.isErrorEnabled()) {
                            this.logger.error("error: REP_RESULT_CODE_NO_UNSUPPORTED: " + this.host + " port: " + this.getPort());
                        }
                        throw new RuntimeException("REP_RESULT_CODE_NO_UNSUPPORTED");
                    }
                }else if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_RESULT_CODE_NO_ERROR)){

                    if(Arrays.equals(Arrays.copyOfRange(response,10,16),REP_POWER_CONTROL_Codes) && expectedResponse == responseValues.POWER_CONTROL) {
                        powerStatus power = powerStatus.values()[Character.getNumericValue((char)response[19])-1];
                        return power;
                    }
                }else if(Arrays.equals(Arrays.copyOfRange(response,8,10),REP_RESULT_CODE_NO_UNSUPPORTED)){
                    if (this.logger.isErrorEnabled()) {
                        this.logger.error("error: REP_RESULT_CODE_NO_UNSUPPORTED: " + this.host + " port: " + this.getPort());
                    }
                    throw new RuntimeException("REP_RESULT_CODE_NO_UNSUPPORTED");
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
                    }else if(Arrays.equals(Arrays.copyOfRange(response,10,14),CMD_GET_TEMP))
                    {
                        String value = new String(Arrays.copyOfRange(response,20,24));

                        return Integer.parseInt(value,16)/2;
                    }
                }else
                {
                    if (this.logger.isErrorEnabled()) {
                        this.logger.error("error: REP_RESULT_CODE_NO_ERROR: " + this.host + " port: " + this.getPort());
                    }
                    throw new RuntimeException("REP_RESULT_CODE_NO_ERROR");
                }
            }
        }else{//Wrong checksum
            if (this.logger.isErrorEnabled()) {
                this.logger.error("error: wrong checksum communicating with: " + this.host + " port: " + this.getPort());
            }
            throw new RuntimeException("wrong Checksum received");
        }
        return null;
    }

    /**
     * Update timestamp of the latest shutdown/startup operation
     * */
    private void updateShutdownStartupTimestamp(){
        latestShutdownStartupTimestamp = new Date().getTime();
    }

    /***
     * Check whether the cooldown period for startup/shutdown has ended
     * Emergency delivery is triggered automatically each time device is turned on/off
     * Even though this device has only one control and this shouldnt be an issue - it still is
     * since some sensors are not available right after the switch.
     * In order to fix this - previous statistics results are returned on emergency delivery.
     * @return boolean value indicating whether the cooldown has ended or not
     */
    private boolean isValidShutdownStartupCooldown(){
        return (new Date().getTime() - latestShutdownStartupTimestamp) < STARTUP_SHUTDOWN_COOLDOWN;
    }
}
