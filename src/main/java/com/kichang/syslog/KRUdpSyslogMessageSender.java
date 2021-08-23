package com.kichang.syslog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.Charset;
import java.util.logging.Level;

import com.cloudbees.syslog.SyslogMessage;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

public class KRUdpSyslogMessageSender extends UdpSyslogMessageSender{
	private DatagramSocket datagramSocket;
	
	protected Charset charset = Charset.forName("euc-kr");

    public KRUdpSyslogMessageSender(String encoding) {
        try {
            setSyslogServerHostname(DEFAULT_SYSLOG_HOST);
            datagramSocket = new DatagramSocket();
            charset = Charset.forName(encoding);
        } catch (IOException e) {
            throw new IllegalStateException("Exception initializing datagramSocket", e);
        }
    }
    
    @Override
    public void sendMessage(SyslogMessage message) throws IOException {
        sendCounter.incrementAndGet();
        long nanosBefore = System.nanoTime();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(baos, charset);
            message.toSyslogMessage(messageFormat, out);
            out.flush();

            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Send syslog message " + new String(baos.toByteArray(), charset));
            }
            byte[] bytes = baos.toByteArray();

            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, syslogServerHostnameReference.get(), syslogServerPort);
            datagramSocket.send(packet);
        } catch (IOException e) {
            sendErrorCounter.incrementAndGet();
            throw e;
        } catch (RuntimeException e) {
            sendErrorCounter.incrementAndGet();
            throw e;
        } finally {
            sendDurationInNanosCounter.addAndGet(System.nanoTime() - nanosBefore);
        }
    }
}
