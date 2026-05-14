/*
 * Copyright (c) 2026 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.Arrays;

/**
 * Test-only protocol helpers for building valid NEC reply frames.
 */
final class NECMultisyncProtocolTestData {
	private static final byte SOH = 0x01;
	private static final byte RESERVED = 0x30;
	private static final byte CTRL_ADDR = 0x30;
	private static final byte CARRIAGE_RETURN = 0x0D;

	private NECMultisyncProtocolTestData() {
	}

	/** Reply to {@code CMD_SET_POWER} shaped for {@code digestResponse(..., POWER_CONTROL)}. */
	static byte[] buildPowerControlReply(byte monitorId, char powerStatusDigit /* '1'..'4' */) {
		byte[] r = baseFrame(monitorId, NECMultisyncConstants.MSG_TYPE_CMD_REPLY, 22);
		put(r, 8, NECMultisyncConstants.REP_RESULT_CODE_NO_ERROR);
		put(r, 10, NECMultisyncConstants.REP_POWER_CONTROL_Codes);
		r[16] = 0x30;
		r[17] = 0x30;
		r[18] = 0x30;
		r[19] = (byte) powerStatusDigit;
		return withChecksum(r);
	}

	static byte[] buildPowerStatusReply(byte monitorId, char powerStatusDigit /* '1'..'4' */) {
		byte[] r = baseFrame(monitorId, NECMultisyncConstants.MSG_TYPE_CMD_REPLY, 26);
		// reserved data + no error + power status codes
		put(r, 8, NECMultisyncConstants.REP_RESERVED_DATA);
		put(r, 10, NECMultisyncConstants.REP_RESULT_CODE_NO_ERROR);
		put(r, 12, NECMultisyncConstants.REP_POWER_STATUS_READ_Codes);
		r[23] = (byte) powerStatusDigit;
		return withChecksum(r);
	}

	static byte[] buildSelfDiagReply(byte monitorId, byte[] diagCode2Bytes) {
		byte[] r = baseFrame(monitorId, NECMultisyncConstants.MSG_TYPE_CMD_REPLY, 20);
		put(r, 8, NECMultisyncConstants.REP_SELF_DIAG_Codes);
		put(r, 10, diagCode2Bytes);
		return withChecksum(r);
	}

	static byte[] buildInputGetReply(byte monitorId, byte[] inputCode4Bytes) {
		byte[] r = baseFrame(monitorId, NECMultisyncConstants.MSG_TYPE_GET_REPLY, 26);
		put(r, 8, NECMultisyncConstants.REP_RESULT_CODE_NO_ERROR);
		put(r, 10, NECMultisyncConstants.CMD_GET_INPUT);
		put(r, 20, inputCode4Bytes);
		return withChecksum(r);
	}

	static byte[] buildTemperatureGetReply(byte monitorId, String temperatureHex4 /* e.g. "0032" */) {
		if (temperatureHex4 == null || temperatureHex4.length() != 4) {
			throw new IllegalArgumentException("temperatureHex4 must be a 4-char hex string");
		}
		byte[] r = baseFrame(monitorId, NECMultisyncConstants.MSG_TYPE_GET_REPLY, 26);
		put(r, 8, NECMultisyncConstants.REP_RESULT_CODE_NO_ERROR);
		put(r, 10, NECMultisyncConstants.CMD_GET_TEMP);
		byte[] hexBytes = temperatureHex4.getBytes();
		r[20] = hexBytes[0];
		r[21] = hexBytes[1];
		r[22] = hexBytes[2];
		r[23] = hexBytes[3];
		return withChecksum(r);
	}

	static byte[] buildUnsupportedCmdReply(byte monitorId) {
		byte[] r = baseFrame(monitorId, NECMultisyncConstants.MSG_TYPE_CMD_REPLY, 20);
		put(r, 8, NECMultisyncConstants.REP_RESERVED_DATA);
		put(r, 10, NECMultisyncConstants.REP_RESULT_CODE_NO_UNSUPPORTED);
		return withChecksum(r);
	}

	/**
	 * Minimal frame for SET input acknowledge parsing ({@code getInputValueFromResponse} reads indices 20–23 only).
	 */
	static byte[] buildSetInputAckFrame(byte[] inputCode4Bytes) {
		byte[] r = new byte[24];
		Arrays.fill(r, (byte) 0x30);
		put(r, 20, inputCode4Bytes);
		return r;
	}

	static byte[] corruptChecksum(byte[] frame) {
		byte[] copy = Arrays.copyOf(frame, frame.length);
		copy[copy.length - 2] = (byte) (copy[copy.length - 2] ^ 0x7F);
		return copy;
	}

	private static byte[] baseFrame(byte monitorId, byte msgType, int length) {
		if (length < 6) {
			throw new IllegalArgumentException("length too small");
		}
		byte[] r = new byte[length];
		r[0] = SOH;
		r[1] = RESERVED;
		r[2] = monitorId;
		r[3] = CTRL_ADDR;
		r[4] = msgType;
		r[length - 1] = CARRIAGE_RETURN;
		return r;
	}

	private static void put(byte[] target, int offset, byte[] src) {
		System.arraycopy(src, 0, target, offset, src.length);
	}

	private static byte[] withChecksum(byte[] frame) {
		byte[] checksumBytes = Arrays.copyOfRange(frame, 1, frame.length - 2);
		frame[frame.length - 2] = NECMultisyncUtils.xor(checksumBytes);
		return frame;
	}
}

