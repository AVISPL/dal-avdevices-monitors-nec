/*
 * Copyright (c) 2022 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.nec.multisync;

import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.CMD_GET_INPUT;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.CMD_GET_POWER;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.CMD_GET_TEMP;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.CMD_SELF_DIAG;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.CMD_SET_INPUT;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.CMD_SET_POWER;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.CMD_SET_SENSOR;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.DIAG_RESULT_CODES;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.MSG_TYPE_CMD;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.MSG_TYPE_CMD_REPLY;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.MSG_TYPE_GET;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.MSG_TYPE_GET_REPLY;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.MSG_TYPE_SET;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.POWER_OFF;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.POWER_ON;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.REP_POWER_CONTROL_Codes;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.REP_POWER_STATUS_READ_Codes;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.REP_RESERVED_DATA;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.REP_RESULT_CODE_NO_ERROR;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.REP_RESULT_CODE_NO_UNSUPPORTED;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.REP_SELF_DIAG_Codes;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.SENSOR_1;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.controlProperties;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.diagResultNames;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.inputNames;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.inputs;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.powerStatus;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.responseValues;
import static com.avispl.symphony.dal.communicator.nec.multisync.NECMultisyncConstants.statisticsProperties;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.SocketCommunicator;
import com.avispl.symphony.dal.util.StringUtils;
/**
 * NECMultisyncDevice
 * Supported features are:
 * <ul>
 * <li> - diagnosis</li>
 * <li> - input</li>
 * <li> - power</li>
 * <li> - temperature</li>
 * </ul>
 * @author Harry / Symphony Dev Team<br>
 * Created on 2/21/2024
 * @since 1.2.0
 */
public class NECMultisyncDevice extends SocketCommunicator implements Controller, Monitorable {

    private int monitorID;
    private final int STARTUP_SHUTDOWN_COOLDOWN = 3000;
    private long latestShutdownStartupTimestamp;
    private ExtendedStatistics localStatistics;
    private String temperatureValue;
    private Set<String> historicalProperties = new HashSet<>();
    private final ReentrantLock reentrantLock = new ReentrantLock();

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
     * Retrieves {@link #historicalProperties}
     *
     * @return value of {@link #historicalProperties}
     */
    public String getHistoricalProperties() {
        return String.join(",", this.historicalProperties);
    }

    /**
     * Sets {@link #historicalProperties} value
     *
     * @param historicalProperties new value of {@link #historicalProperties}
     */
    public void setHistoricalProperties(String historicalProperties) {
        this.historicalProperties.clear();
        Arrays.asList(historicalProperties.split(",")).forEach(propertyName -> {
            this.historicalProperties.add(propertyName.trim());
        });
    }

    /**
     * This method is recalled by Symphony to control specific property
     *
     * @param controllableProperty This is the property to be controlled
     */
    @Override
    public void controlProperty(ControllableProperty controllableProperty) throws Exception {
        reentrantLock.lock();
        try {
            String propertyName = controllableProperty.getProperty();
            String value = String.valueOf(controllableProperty.getValue());
            Map<String, String> stats = localStatistics.getStatistics();
            List<AdvancedControllableProperty> advancedControllableProperties = localStatistics.getControllableProperties();
            if (propertyName.equals(controlProperties.Power.name())) {
                String inputValue = stats.get(statisticsProperties.Input.name());
                if (value.equals(NECMultisyncConstants.NUMBER_ONE)) {
                    powerSwitch(POWER_ON);
                    addAdvancedControlProperties(advancedControllableProperties, stats, createDropdown(statisticsProperties.Input.name(), getInputNamesArray(), inputValue), inputValue);
                } else if (value.equals(NECMultisyncConstants.ZERO)) {
                    powerSwitch(POWER_OFF);
                    removeValueForTheControllableProperty(stats, advancedControllableProperties, statisticsProperties.Input.name());
                    stats.put(statisticsProperties.Input.name(), inputValue);
                }
                stats.put(statisticsProperties.Temperature.name() + "(C)", temperatureValue);
            } else if (propertyName.equals(controlProperties.Input.name())) {
                changeInputValue(value);
            }
        } finally {
            reentrantLock.unlock();
        }
    }

    /**
     * This method is recalled by Symphony to control a list of properties
     * @param controllableProperties This is the list of properties to be controlled
     */
    @Override
    public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
        controllableProperties.stream().forEach(p -> {
            try {
                controlProperty(p);
            } catch (Exception e) {
                logger.error("Unable to execute control properties.", e);
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
        reentrantLock.lock();
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
            Map<String, String> dynamicStatistics = new HashMap<>();

            //controllable properties
            AdvancedControllableProperty.Switch powerSwitch = new AdvancedControllableProperty.Switch();
            powerSwitch.setLabelOn("On");
            powerSwitch.setLabelOff("Off");

            //getting power status from device
            String power;

            try {
                power = getPower().name();
                if (power.contains("ON")) {
                    advancedControllableProperties.add(new AdvancedControllableProperty(statisticsProperties.Power.name(), new Date(), powerSwitch, NECMultisyncConstants.NUMBER_ONE));
                    statistics.put(statisticsProperties.Power.name(), NECMultisyncConstants.NUMBER_ONE);
                } else {
                    advancedControllableProperties.add(new AdvancedControllableProperty(statisticsProperties.Power.name(), new Date(), powerSwitch, NECMultisyncConstants.ZERO));
                    statistics.put(statisticsProperties.Power.name(), NECMultisyncConstants.ZERO);
                }
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getPower", e);
                }
                throw new ResourceNotReachableException(NECMultisyncConstants.MESSAGE_ERROR + e.getMessage());
            }

            //getting diagnostic result from device
            try {
                statistics.put(statisticsProperties.Diagnosis.name(), NECMultisyncDiagnosisEnum.getValueByName(getDiagResult().name()));
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getDiagResult", e);
                }
                throw new ResourceNotReachableException(NECMultisyncConstants.MESSAGE_ERROR + e.getMessage());
            }

            //getting current device input
            try {
                String value = getInput().name();
                if (NECMultisyncConstants.NUMBER_ONE.equalsIgnoreCase(statistics.get(statisticsProperties.Power.name())) && !inputNames.UNKNOWN.name().equalsIgnoreCase(value)) {
                    addAdvancedControlProperties(advancedControllableProperties, statistics, createDropdown(statisticsProperties.Input.name(), getInputNamesArray(), value), value);
                } else {
                    statistics.put(statisticsProperties.Input.name(), value);
                }
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getInput", e);
                }
                throw new ResourceNotReachableException(NECMultisyncConstants.MESSAGE_ERROR + e.getMessage());
            }

            //getting device temperature
            try {
                String temperatureParameter = statisticsProperties.Temperature.name() + "(C)";
                temperatureValue = String.valueOf(getTemperature());
                if (!historicalProperties.isEmpty() && historicalProperties.contains(temperatureParameter)) {
                    dynamicStatistics.put(temperatureParameter, temperatureValue);
                } else {
                    statistics.put(temperatureParameter, temperatureValue);
                }
            } catch (Exception e) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("error during getTemperature", e);
                }
                throw new ResourceNotReachableException(NECMultisyncConstants.MESSAGE_ERROR + e.getMessage());
            }

            extendedStatistics.setControllableProperties(advancedControllableProperties);
            extendedStatistics.setStatistics(statistics);
            extendedStatistics.setDynamicStatistics(dynamicStatistics);

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
            reentrantLock.unlock();
        }
        return Collections.singletonList(extendedStatistics);
    }

    @Override
    protected byte[] send(byte[] data) throws Exception {
       return sendWithTimeout(data, 30000, TimeUnit.SECONDS);
    }

    /**
     * Async executor service wrapper for send() operation, so we'll react if there are connection issues with the request,
     * cancel the future and throw Socket timeout exception. This should prevent potential breaking memory leaks from occurring
     * on a larger infrastructure scales.
     *
     * @param data bytes to send
     * @param timeout timeout after which operation is canceled
     * @param unit time unit for the specified timeout
     * @throws Exception if there was an exception during command execution
     * */
    public byte[] sendWithTimeout(byte[] data, long timeout, TimeUnit unit) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
            try {
                return super.send(data);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);

        try {
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new SocketTimeoutException("Device operation timed out, please check device state and network accessibility.");
        } catch (ExecutionException e) {
            throw (Exception) e.getCause();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * This method is recalled by Symphony to get the current monitor ID (Future purpose)
     * @return int This returns the current monitor ID.
     */
    public int getMonitorID() {
        return monitorID;
    }

    /**
     * This method is used by Symphony to set the monitor ID (Future purpose)
     *
     * @param monitorID This is the monitor ID to be set
     */
    public void setMonitorID(int monitorID) {
        this.monitorID = monitorID+64;
    }

    /**
     * This method is used to get the current display temperature (from sensor 1)
     * @return int This returns the retrieved temperature.
     */
    private int getTemperature() throws Exception{

        //setting the sensor to retrieve the temperature from
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
            throw new RuntimeException("Unable to retrieve power status from the device.");
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
            digestResponse(response, responseValues.POWER_CONTROL);
            Thread.sleep(5000);
            response = send(NECMultisyncUtils.buildSendString((byte) monitorID, MSG_TYPE_CMD, CMD_GET_POWER));
            powerStatus power = (powerStatus) digestResponse(response, responseValues.POWER_STATUS_READ);
            if (power == null) {
                throw new IllegalArgumentException("Error during power switch operation");
            }
            if (!(Arrays.equals(command, POWER_ON) && power.name().contains("ON") || Arrays.equals(command, POWER_OFF) && power.name().contains("OFF"))) {
                throw new IllegalArgumentException("Error during power switch operation. The current power status is " + power.name());
            }

            updateShutdownStartupTimestamp();
            localStatistics.getStatistics().put(statisticsProperties.Power.name(), Arrays.equals(command, POWER_ON) ? NECMultisyncConstants.NUMBER_ONE : NECMultisyncConstants.ZERO);
            localStatistics.getControllableProperties().stream().filter(acp -> acp.getName().equals(
                statisticsProperties.Power.name())).findFirst().ifPresent(acp -> {
                acp.setTimestamp(new Date());
                acp.setValue(Arrays.equals(command, POWER_ON) ? NECMultisyncConstants.NUMBER_ONE : NECMultisyncConstants.ZERO);
            });
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Changes the input value of the monitor to the specified value.
     *
     * @param value The input value to set for the monitor.
     * @throws IllegalArgumentException If the input value is not supported with the model, or if an error occurs during the process.
     */
    private void changeInputValue(String value) {
        try {
            inputNames inputName = getInputNameFromString(value);
            if (inputName != null) {
                byte[] response = send(NECMultisyncUtils.buildSendString((byte) monitorID, MSG_TYPE_SET, CMD_SET_INPUT, inputs.get(inputName)));
                String controlValue = getInputValueFromResponse(response);
                //Sleep 1s to wait the device effect
                Thread.sleep(1000);
                response = send(NECMultisyncUtils.buildSendString((byte) monitorID, MSG_TYPE_GET, CMD_GET_INPUT));
                String currentValue = getInputValueFromResponse(response);
                if (!currentValue.equalsIgnoreCase(controlValue)) {
                    throw new IllegalArgumentException(String.format("The device does not support %s input command.", value));
                } else {
                    localStatistics.getStatistics().put(statisticsProperties.Input.name(), value);
                    localStatistics.getControllableProperties().stream().filter(acp -> acp.getName().equals(
                        statisticsProperties.Input.name())).findFirst().ifPresent(acp -> {
                        acp.setTimestamp(new Date());
                        acp.setValue(value);
                    });
                }
            } else {
                throw new IllegalArgumentException(String.format("Can't control Input with value is %s. Error because invalid input value", value));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Extracts the input value from a response byte array by comparing a portion of the response
     * with predefined byte arrays associated with input names.
     *
     * @param response The byte array representing the response from which to extract the input value.
     * @return The name of the input associated with the matching byte array, or "None" if no match is found.
     */
    private String getInputValueFromResponse(byte[] response) {
        for (Map.Entry<inputNames, byte[]> entry : inputs.entrySet()) {
            if (Arrays.equals(Arrays.copyOfRange(response, 20, 24), entry.getValue())) {
                inputNames input = entry.getKey();
                return input.name();
            }
        }
        return NECMultisyncConstants.NONE;
    }

    /**
     * Converts a string representation of an input name to the corresponding inputNames enum.
     *
     * @param inputNameString The string representation of the input name.
     * @return The inputNames enum value corresponding to the given string, or null if not found.
     */
    private inputNames getInputNameFromString(String inputNameString) {
        return Arrays.stream(inputNames.values())
            .filter(inputName -> inputName.name().equalsIgnoreCase(inputNameString))
            .findFirst()
            .orElse(null);
    }

    /**
     * This method is used to get the diagnostics results from the display
     *
     * @return diagResultNames This returns the retrieved diagnostic results.
     */
    private diagResultNames getDiagResult() throws Exception {
        byte[] response = send(NECMultisyncUtils.buildSendString((byte) monitorID, MSG_TYPE_CMD, CMD_SELF_DIAG));
        diagResultNames diagResult = (diagResultNames) digestResponse(response, responseValues.SELF_DIAG);

        if (diagResult == null) {
            return diagResultNames.UNKNOWN;
        } else {
            return diagResult;
        }
    }

    /**
     * This method is used to get the current display input
     *
     * @return inputNames This returns the current input.
     */
    private inputNames getInput() throws Exception {
        byte[] response = send(NECMultisyncUtils.buildSendString((byte) monitorID, MSG_TYPE_GET, CMD_GET_INPUT));
        inputNames input = (inputNames) digestResponse(response, responseValues.INPUT_STATUS_READ);

        if (input == null) {
            logger.error("Error while retrieve Input status");
            return inputNames.UNKNOWN;
        } else {
            return input;
        }
    }

    /**
     * This method is used to digest the response received from the device
     *
     * @param response This is the response to be digested
     * @param expectedResponse This is the expected response type to be compared with received
     * @return Object This returns the result digested from the response.
     */
    private Object digestResponse(byte[] response, responseValues expectedResponse) {
        if (response.length <= 4) {
            throw new ResourceNotReachableException("Error while retrieve data");
        }
        byte responseMessageType = response[4];

        Arrays.copyOfRange(response, 1, response.length - 2);

        //checksum verification
        if (response[response.length - 2] == NECMultisyncUtils.xor(Arrays.copyOfRange(response, 1, response.length - 2))) {
            if (responseMessageType == MSG_TYPE_CMD_REPLY) {
                if (Arrays.equals(Arrays.copyOfRange(response, 8, 10), REP_RESERVED_DATA)) {
                    if (Arrays.equals(Arrays.copyOfRange(response, 10, 12), REP_RESULT_CODE_NO_ERROR)) {
                        if (Arrays.equals(Arrays.copyOfRange(response, 12, 14), REP_POWER_STATUS_READ_Codes) && expectedResponse == responseValues.POWER_STATUS_READ) {
                            powerStatus power = powerStatus.values()[Character.getNumericValue((char) response[23]) - 1];
                            return power;
                        }
                    } else if (Arrays.equals(Arrays.copyOfRange(response, 10, 12), REP_RESULT_CODE_NO_UNSUPPORTED)) {
                        if (this.logger.isErrorEnabled()) {
                            this.logger.error("error: REP_RESULT_CODE_NO_UNSUPPORTED: " + this.host + " port: " + this.getPort());
                        }
                        throw new RuntimeException("REP_RESULT_CODE_NO_UNSUPPORTED");
                    }
                } else if (Arrays.equals(Arrays.copyOfRange(response, 8, 10), REP_RESULT_CODE_NO_ERROR)) {

                    if (Arrays.equals(Arrays.copyOfRange(response, 10, 16), REP_POWER_CONTROL_Codes) && expectedResponse == responseValues.POWER_CONTROL) {
                        powerStatus power = powerStatus.values()[Character.getNumericValue((char) response[19]) - 1];
                        return power;
                    }
                } else if (Arrays.equals(Arrays.copyOfRange(response, 8, 10), REP_RESULT_CODE_NO_UNSUPPORTED)) {
                    if (this.logger.isErrorEnabled()) {
                        this.logger.error("error: REP_RESULT_CODE_NO_UNSUPPORTED: " + this.host + " port: " + this.getPort());
                    }
                    throw new RuntimeException("REP_RESULT_CODE_NO_UNSUPPORTED");
                } else if (Arrays.equals(Arrays.copyOfRange(response, 8, 10), REP_SELF_DIAG_Codes) && expectedResponse == responseValues.SELF_DIAG) {

                    for (Map.Entry<diagResultNames, byte[]> entry : DIAG_RESULT_CODES.entrySet()) {
                        if (Arrays.equals(Arrays.copyOfRange(response, 10, 12), entry.getValue())) {
                            diagResultNames diagResult = entry.getKey();
                            return diagResult;
                        }
                    }
                }
            } else if (responseMessageType == MSG_TYPE_GET_REPLY) {
                if (Arrays.equals(Arrays.copyOfRange(response, 8, 10), REP_RESULT_CODE_NO_ERROR)) {
                    if (Arrays.equals(Arrays.copyOfRange(response, 10, 14), CMD_GET_INPUT)) {
                        for (Map.Entry<inputNames, byte[]> entry : inputs.entrySet()) {
                            if (Arrays.equals(Arrays.copyOfRange(response, 20, 24), entry.getValue())) {
                                inputNames input = entry.getKey();
                                return input;
                            }
                        }
                        return NECMultisyncConstants.NONE;
                    } else if (Arrays.equals(Arrays.copyOfRange(response, 10, 14), CMD_GET_TEMP)) {
                        String value = new String(Arrays.copyOfRange(response, 20, 24));

                        return Integer.parseInt(value, 16) / 2;
                    }
                } else {
                    if (this.logger.isErrorEnabled()) {
                        this.logger.error("error: REP_RESULT_CODE_NO_ERROR: " + this.host + " port: " + this.getPort());
                    }
                    throw new RuntimeException("REP_RESULT_CODE_NO_ERROR");
                }
            }
        } else {//Wrong checksum
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
     * Even though this device has only one control and this should not be an issue - it still is
     * since some sensors are not available right after the switch.
     * In order to fix this - previous statistics results are returned on emergency delivery.
     * @return boolean value indicating whether the cooldown has ended or not
     */
    private boolean isValidShutdownStartupCooldown() {
        return (new Date().getTime() - latestShutdownStartupTimestamp) < STARTUP_SHUTDOWN_COOLDOWN;
    }

    /**
     * Retrieves an array of input names as strings.
     *
     * @return An array of strings representing the names of the input names enum.
     */
    private static String[] getInputNamesArray() {
        return Arrays.stream(inputNames.values()).filter(input -> !input.equals(inputNames.UNKNOWN))
            .map(Enum::name)
            .toArray(String[]::new);
    }

    /***
     * Create dropdown advanced controllable property
     *
     * @param name the name of the control
     * @param initialValue initial value of the control
     * @return AdvancedControllableProperty dropdown instance
     */
    private AdvancedControllableProperty createDropdown(String name, String[] values, String initialValue) {
        AdvancedControllableProperty.DropDown dropDown = new AdvancedControllableProperty.DropDown();
        dropDown.setOptions(values);
        dropDown.setLabels(values);

        return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
    }

    /**
     * Removes a controllable property and its associated value from the provided statistics and advanced controllable properties lists.
     *
     * @param stats The statistics map containing property values.
     * @param advancedControllableProperties The list of advanced controllable properties.
     * @param name The name of the property to remove.
     */
    private void removeValueForTheControllableProperty(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String name) {
        stats.remove(name);
        advancedControllableProperties.removeIf(item -> item.getName().equalsIgnoreCase(name));
    }

    /**
     * Add addAdvancedControlProperties if advancedControllableProperties different empty
     *
     * @param advancedControllableProperties advancedControllableProperties is the list that store all controllable properties
     * @param stats store all statistics
     * @param property the property is item advancedControllableProperties
     * @throws IllegalStateException when exception occur
     */
    private void addAdvancedControlProperties(List<AdvancedControllableProperty> advancedControllableProperties, Map<String, String> stats, AdvancedControllableProperty property, String value) {
        if (property != null) {
            for (AdvancedControllableProperty controllableProperty : advancedControllableProperties) {
                if (controllableProperty.getName().equals(property.getName())) {
                    advancedControllableProperties.remove(controllableProperty);
                    break;
                }
            }
            if (StringUtils.isNotNullOrEmpty(value)) {
                stats.put(property.getName(), value);
            } else {
                stats.put(property.getName(), NECMultisyncConstants.EMPTY);
            }
            advancedControllableProperties.add(property);
        }
    }
}
