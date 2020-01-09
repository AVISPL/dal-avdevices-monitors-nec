package com.avispl.symphony.dal.communicator.nec.multisync;

import com.avispl.symphony.api.dal.dto.control.ConnectionState;
import com.avispl.symphony.api.dal.error.CommandFailureException;
import com.avispl.symphony.dal.BaseDevice;
import com.avispl.symphony.dal.communicator.Communicator;
import com.avispl.symphony.dal.communicator.ConnectionStatus;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SocketCommunicator extends BaseDevice implements Communicator {
    private Socket socket;
    private int port;

    private List<String> commandErrorList;
    private List<String> commandSuccessList;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConnectionStatus status = new ConnectionStatus();

    public SocketCommunicator() {
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        if (this.isInitialized()) {
            throw new IllegalStateException("Cannot change properties after init() was called");
        } else {
            this.port = port;
        }
    }

    public List<String> getCommandErrorList() {
        return this.commandErrorList;
    }

    protected void setCommandErrorList(List<String> commandErrorList) {
        if (this.isInitialized()) {
            throw new IllegalStateException("Cannot change properties after init() was called");
        } else {
            this.commandErrorList = commandErrorList;
        }
    }

    public List<String> getCommandSuccessList() {
        return this.commandSuccessList;
    }

    protected void setCommandSuccessList(List<String> commandSuccessList) {
        if (this.isInitialized()) {
            throw new IllegalStateException("Cannot change properties after init() was called");
        } else {
            this.commandSuccessList = commandSuccessList;
        }
    }

    @Override
    public void connect() throws Exception {
        if (!this.isInitialized()) {
            throw new IllegalStateException("ShellCommunicator cannot be used before init() is called");
        } else {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("Connecting to: " + this.host + " port: " + this.port);
            }

            Lock writeLock = this.lock.writeLock();
            writeLock.lock();

            try {
                if (!this.isChannelConnected()) {
                    this.createChannel();
                    this.status.setLastTimestamp(System.currentTimeMillis());
                    this.status.setConnectionState(ConnectionState.Connected);
                    this.status.setLastError(null);
                }
            } catch (Exception var6) {
                if (var6 instanceof InterruptedException) {
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("Interrupted while connecting to: " + this.host + " port: " + this.port);
                    }
                } else if (this.logger.isErrorEnabled()) {
                    this.logger.error("Error connecting to: " + this.host + " port: " + this.port, var6);
                }

                this.status.setLastError(var6);
                this.status.setConnectionState(ConnectionState.Failed);
                this.destroyChannel();
                throw var6;
            } finally {
                writeLock.unlock();
            }
        }
    }

    @Override
    public void disconnect() throws Exception {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Disconnecting from: " + this.host + " port: " + this.port);
        }

        Lock writeLock = this.lock.writeLock();
        writeLock.lock();

        try {
            this.destroyChannel();
            this.status.setConnectionState(ConnectionState.Disconnected);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        Lock readLock = this.lock.readLock();
        readLock.lock();

        ConnectionStatus var2;
        try {
            var2 = this.status.copyOf();
        } finally {
            readLock.unlock();
        }

        return var2;
    }

    private void createChannel() throws Exception {
        try {
            if (this.socket == null || this.socket.isClosed() || !this.socket.isConnected()) {
                this.socket = new Socket(this.host, this.port);
                this.socket.setSoTimeout(5000);
            }

        }catch (UnknownHostException ex) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Error connecting to: " + this.host + " port: " + this.port, ex);
            }
        } catch (IOException ex) {
            if (this.logger.isErrorEnabled()) {
                this.logger.error("Error connecting to: " + this.host + " port: " + this.port, ex);
            }
        }
    }

    private void destroyChannel() {
        if (null != this.socket) {
            try {
                if (this.socket.isConnected()) {
                    this.socket.close();
                }
            } catch (Exception var2) {
                if (this.logger.isWarnEnabled()) {
                    this.logger.warn("error seen on destroyChannel", var2);
                }
            }

            this.socket = null;
        }

    }

    private boolean isChannelConnected() {
        Socket client = this.socket;
        return null != client && client.isConnected();
    }

    protected byte[] send(byte[] data) throws Exception {
        if (!this.isInitialized()) {
            throw new IllegalStateException("ShellCommunicator cannot be used before init() is called");
        } else if (null == data) {
            throw new IllegalArgumentException("Send data is null");
        } else {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace("Sending command: " + data + " to: " + this.host + " port: " + this.port);
            }

            Lock writeLock = this.lock.writeLock();
            writeLock.lock();

            byte[] var3;
            try {
                var3 = this.send(data, true);
            } finally {
                writeLock.unlock();
            }

            return var3;
        }
    }

    protected static final char[] hexArray = "0123456789ABCDEF".toCharArray();


    public static String getHexByteString(byte[] bytes) throws IOException {
        return getHexByteString((CharSequence)null, ",", (CharSequence)null, bytes);
    }

    public static String getHexByteString(CharSequence prefix, CharSequence separator, CharSequence suffix, byte[] bytes) throws IOException {
        byte[] data = bytes;
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < data.length; ++i) {
            if (i > 0) {
                sb.append(separator);
            }

            int v = data[i] & 255;
            if (prefix != null) {
                sb.append(prefix);
            }

            sb.append(hexArray[v >> 4]);
            sb.append(hexArray[v & 15]);
            if (suffix != null) {
                sb.append(suffix);
            }
        }

        return sb.toString();
    }

    private byte[] send(byte[] data, boolean retryOnError) throws Exception {
        try {
            if (!this.isChannelConnected()) {
                this.createChannel();
                this.status.setLastTimestamp(System.currentTimeMillis());
                this.status.setConnectionState(ConnectionState.Connected);
                this.status.setLastError(null);
            }

            if(this.logger.isDebugEnabled()) {
                this.logger.debug("Sending: " + getHexByteString(data) + " to: " + this.host + " port: " + this.port);
            }

            byte response[] = this.internalSend(data);

            if(this.logger.isDebugEnabled()) {
                this.logger.debug("Received response: " + getHexByteString(response) + " from: " + this.host + " port: " + this.port);
            }

            if (this.logger.isTraceEnabled()) {
                this.logger.trace("Received response: " + response + " from: " + this.host + " port: " + this.port);
            }

            this.status.setLastTimestamp(System.currentTimeMillis());
            return response;
        } catch (CommandFailureException var4) {
            if (this.logger.isErrorEnabled()) {
                this.logger.error("Command failed " + data + " to: " + this.host + " port: " + this.port + " connection state: " + this.status.getConnectionState(), var4);
            }

            this.status.setLastTimestamp(System.currentTimeMillis());
            throw var4;
        }catch(SocketTimeoutException ex){
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Time out while sending command: " + data + " to: " + this.host + " port: " + this.port + " connection state: " + this.status.getConnectionState() + " error: ", ex);
            }

            //throw ex;
            this.status.setLastError(ex);
            this.status.setConnectionState(ConnectionState.Unknown);
            this.destroyChannel();
            if (retryOnError) {
                return this.send(data, true);
            } else {
                throw ex;
            }
        } catch(Exception var5) {
            if (var5 instanceof InterruptedException) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Interrupted while sending command: " + data + " to: " + this.host + " port: " + this.port + " connection state: " + this.status.getConnectionState() + " error: ", var5);
                }
            } else if (this.logger.isErrorEnabled()) {
                this.logger.error("Error sending command: " + data + " to: " + this.host + " port: " + this.port + " connection state: " + this.status.getConnectionState() + " error: ", var5);
            }

            this.status.setLastError(var5);
            this.status.setConnectionState(ConnectionState.Failed);
            this.destroyChannel();
            if (retryOnError) {
                return this.send(data, false);
            } else {
                throw var5;
            }
        }
    }

    private byte[] internalSend(byte[] outputData) throws Exception {
        this.write(outputData);
        return this.read(outputData, this.socket.getInputStream());
    }

    private void write(byte[] outputData) throws Exception {
        OutputStream os = this.socket.getOutputStream();

        os.write(outputData);
        os.flush();
    }

    private byte[] read(byte[] command, InputStream in) throws Exception {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("DEBUG - Socket Communicator reading after command text \"" + command + "\" was sent to host " + this.host);
        }

        BufferedInputStream reader = new BufferedInputStream(in);

        List<Byte> bytes = new ArrayList<>();

        do{
            bytes.add((byte)reader.read());
        }while (reader.available()>0);

        byte byteArray[] = new byte[bytes.size()];

        for(int i = 0;i<bytes.size();i++)
        {
            byteArray[i] = bytes.get(i);
        }

        return byteArray;
    }

    protected boolean doneReading(String command, String response) throws CommandFailureException {
        Iterator var3 = this.commandErrorList.iterator();

        String string;
        do {
            if (!var3.hasNext()) {
                var3 = this.commandSuccessList.iterator();

                do {
                    if (!var3.hasNext()) {
                        return false;
                    }

                    string = (String)var3.next();
                } while(!response.endsWith(string));

                if (this.logger.isTraceEnabled()) {
                    this.logger.trace("Done reading, found success string: " + string + " from: " + this.host + " port: " + this.port);
                }

                return true;
            }

            string = (String)var3.next();
        } while(!response.endsWith(string));

        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Done reading, found error string: " + string + " from: " + this.host + " port: " + this.port);
        }

        throw new CommandFailureException(this.host, command, response);
    }

    protected void internalDestroy() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace("Destroying communication channel to: " + this.host + " port: " + this.port);
        }

        this.destroyChannel();
        this.status.setConnectionState(ConnectionState.Disconnected);
        super.internalDestroy();
    }

    protected void internalInit() throws Exception {
        super.internalInit();

        if (null != this.socket) {
            this.destroyChannel();
        }

        if (this.port <= 0) {
            throw new IllegalStateException("Invalid port property: " + this.port + " (must be positive number)");
        }  else if (null != this.commandSuccessList && !this.commandSuccessList.isEmpty()) {
            if (null == this.commandErrorList || this.commandErrorList.isEmpty()) {
                throw new IllegalStateException("Invalid commandErrorList property (must be non-empty list)");
            }
        } else {
            throw new IllegalStateException("Invalid commandSuccessList property (must be non-empty list)");
        }
    }
}
