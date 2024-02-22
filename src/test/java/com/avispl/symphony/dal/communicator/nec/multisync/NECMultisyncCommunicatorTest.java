/*
 *  Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;

/**
 * MiddleAtlanticUPSCommunicatorTest for unit test of MiddleAtlanticUPSCommunicator
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 30/10/2023
 * @since 1.0.0
 */
public class NECMultisyncCommunicatorTest {
	private NECMultisyncDevice necMultisyncDevice;
	static ExtendedStatistics extendedStatistic;

	@BeforeEach()
	public void setUp() throws Exception {
		necMultisyncDevice = new NECMultisyncDevice();
		necMultisyncDevice.setHost("172.31.254.248");
		necMultisyncDevice.setPort(7142);
		necMultisyncDevice.setLogin("");
		necMultisyncDevice.setPassword("");
		necMultisyncDevice.init();
		necMultisyncDevice.connect();
	}

	@AfterEach()
	public void destroy() throws Exception {
		necMultisyncDevice.disconnect();
	}

	/**
	 * Unit test to verify the functionality of the "getMultipleStatistics" method in the MiddleAtlanticUPSCommunicator class.
	 * This test ensures that the method correctly retrieves multiple statistics and verifies the expected size of the result.
	 *
	 * @throws Exception if an error occurs during the test execution.
	 */
	@Test
	void testGetMultipleStatistics() throws Exception {
		extendedStatistic = (ExtendedStatistics) necMultisyncDevice.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllablePropertyList = extendedStatistic.getControllableProperties();
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals(4, statistics.size());
	}

	@Test
	void testHistorical() throws Exception {
		necMultisyncDevice.setHistoricalProperties("Temperature(C)");
		extendedStatistic = (ExtendedStatistics) necMultisyncDevice.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllablePropertyList = extendedStatistic.getControllableProperties();
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Map<String, String> dynamicStatistics = extendedStatistic.getDynamicStatistics();
		Assert.assertEquals(3, statistics.size());
		Assert.assertEquals(1, dynamicStatistics.size());
	}

	@Test
	void testVideoInput() throws Exception {
		extendedStatistic = (ExtendedStatistics) necMultisyncDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		String property = "Input";
		String value = "HDMI3";
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		necMultisyncDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) necMultisyncDevice.getMultipleStatistics().get(0);
		List<AdvancedControllableProperty> advancedControllablePropertyList = extendedStatistic.getControllableProperties();
		Optional<AdvancedControllableProperty> advancedControllableProperty = advancedControllablePropertyList.stream().filter(item ->
				property.equals(item.getName())).findFirst();
		Assert.assertEquals(value, advancedControllableProperty.get().getValue());
	}
}
