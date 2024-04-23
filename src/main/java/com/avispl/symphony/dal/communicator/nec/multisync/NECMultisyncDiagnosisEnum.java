/*
 * Copyright (c) 2024 AVI-SPL Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration representing diagnostic states for NEC MultiSync monitors.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 2/21/2024
 * @since 1.2.0
 */
public enum NECMultisyncDiagnosisEnum {
	NORMAL("NORMAL", "Normal"),
	STB_POWER_3_3V_ABNORMALITY("STB_POWER_3_3V_ABNORMALITY", "Main-power +3.3V abnormality"),
	STB_POWER_5V_ABNORMALITY("STB_POWER_5V_ABNORMALITY", "Main-power +5V abnormality"),
	PANEL_POWER_12V_ABNORMALITY("PANEL_POWER_12V_ABNORMALITY", "Panel-power/FAN-power +12V abnormality"),
	INVERTER_POWER_24V_ABNORMALITY("INVERTER_POWER_24V_ABNORMALITY", "Audio-power/Converter-power +24V abnormality"),
	FAN1_ABNORMALITY("FAN1_ABNORMALITY", "Cooling fan-1 abnormality"),
	FAN2_ABNORMALITY("FAN2_ABNORMALITY", "Cooling fan-2 abnormality"),
	FAN3_ABNORMALITY("FAN3_ABNORMALITY", "Cooling fan-3 abnormality"),
	TEMP_ABNORMALITY_SHUTDOWN("TEMP_ABNORMALITY_SHUTDOWN", "Temperature abnormality shutdown"),
	TEMP_ABNORMALITY_HALF_BRIGHT("TEMP_ABNORMALITY_HALF_BRIGHT", " Temperature abnormality half brightness"),
	MAX_TEMP_REACHED("MAX_TEMP_REACHED", "SENSOR reached at the temperature that the user had specified"),
	NO_SIGNAL("NO_SIGNAL", "No signal"),
	LED_ABNORMALITY_1("LED_ABNORMALITY_1", "LED Backlight abnormality (CON ERR1)*short open detection"),
	LED_ABNORMALITY_2("LED_ABNORMALITY_2", "LED Backlight abnormality (CON ERR2)*short open detection"),
	EXTERNAL_REDUNDANT("EXTERNAL_REDUNDANT", "External redundant PSU error"),
	INTERNAL_MAIN("INTERNAL_MAIN", "Internal main PSU error"),
	COMPUTE_MODULE("COMPUTE_MODULE", "COMPUTE MODULE Cooling fan abnormality"),
	ERROR_LOG_BUFFER_REDUCTION("ERROR_LOG_BUFFER_REDUCTION", "Error log buffer reduction"),
	RTC_ERROR("RTC_ERROR", "RTC error"),
	EEPROM_ERROR("EEPROM_ERROR", "EEPROM error"),
	NFC_EEPROM_ERROR("NFC_EEPROM_ERROR", "NFC-EEPROM error"),
	CPLD_ERROR("CPLD_ERROR", "CPLD error"),
	HDMI_SW1_ERROR("HDMI_SW1_ERROR", "HDMI_SW1 error"),
	HDMI_SW2_ERROR("HDMI_SW2_ERROR", "HDMI_SW2 error"),
	SCALER_DP("SCALER_DP", "Scaler DP block abnormal"),
	GARNET_EEPROM_ERROR("GARNET_EEPROM_ERROR", "Garnet EEPROM error"),
	WRONG_ROTATION("WRONG_ROTATION", "Wrong rotation (G Sensor)"),
	STRONG_IMPACT("STRONG_IMPACT", "Strong impact (G Sensor)"),
	NONE("UNKNOWN", "Unknown"),
	;
	private final String name;
	private final String value;

	/**
	 * Constructor for NECMultisyncDiagnosisEnum.
	 *
	 * @param name  The name representing the diagnosis state.
	 * @param value The human-readable description of the diagnosis state.
	 */
	NECMultisyncDiagnosisEnum(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Retrieves the human-readable description of a diagnostic state based on its name.
	 *
	 * @param name The name of the diagnostic state.
	 * @return The human-readable description of the diagnostic state, or None if not found.
	 */
	public static String getValueByName(String name) {
		Optional<NECMultisyncDiagnosisEnum> property = Arrays.stream(NECMultisyncDiagnosisEnum.values()).filter(item -> item.getName().equalsIgnoreCase(name)).findFirst();
		return property.isPresent() ? property.get().getValue() : NECMultisyncConstants.NONE;
	}
}
