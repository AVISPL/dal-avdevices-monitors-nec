/*
 * Copyright (c) 2026 AVI-SPL Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.nec.multisync;

import java.util.function.Function;

/**
 * Test double that replaces socket I/O by supplying scripted {@code send} responses.
 * Uses a public {@link #send(byte[])} override (widened from
 * {@link com.avispl.symphony.dal.communicator.SocketCommunicator}).
 */
final class TestableNECMultisyncDevice extends NECMultisyncDevice {

	private Function<byte[], byte[]> sendHandler = req -> new byte[] { 0x01, 0x0D };

	void setSendHandler(Function<byte[], byte[]> sendHandler) {
		this.sendHandler = sendHandler != null ? sendHandler : req -> new byte[] { 0x01, 0x0D };
	}

	@Override
	public byte[] send(byte[] bytes) throws Exception {
		return sendHandler.apply(bytes);
	}
}
