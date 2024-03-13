/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;

/**
 * Tests for NEC Multisync device
 *
 * @author Maksym Rossiitsev
 * @version 1.1
 * @since 1.1
 */
public class NECMultisyncDeviceTest {

	private ExtendedStatistics extendedStatistic;
	private NECMultisyncDevice necMultisyncDevice;

	@BeforeEach
	public void setUp() throws Exception {
		necMultisyncDevice = new NECMultisyncDevice();
		necMultisyncDevice.setHost("172.31.254.236");
		necMultisyncDevice.init();
		necMultisyncDevice.connect();
	}

	@AfterEach
	public void destroy() throws Exception {
		necMultisyncDevice.disconnect();
	}

	/**
	 * Test NECMultisyncDevice.getMultipleStatistics get Statistic and DynamicStatistic success
	 * Expected retrieve monitoring data and non-null temperature data
	 */
	@Tag("RealDevice")
	@Test
	public void testLgLCDDeviceGetStatistic() throws Exception {
		necMultisyncDevice.setHistoricalProperties("temperature");
		extendedStatistic = (ExtendedStatistics) necMultisyncDevice.getMultipleStatistics().get(0);
		Map<String, String> dynamicStatistic = extendedStatistic.getDynamicStatistics();
		Map<String, String> statistics = extendedStatistic.getStatistics();

		Assertions.assertNotNull(dynamicStatistic.get(NECMultisyncConstants.statisticsProperties.temperature.name()));
		Assertions.assertEquals("HDMI1_PC", statistics.get(NECMultisyncConstants.statisticsProperties.input.name()));
		Assertions.assertEquals("1", statistics.get(NECMultisyncConstants.statisticsProperties.power.name()));
	}
}
