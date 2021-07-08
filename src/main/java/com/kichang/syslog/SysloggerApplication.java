package com.kichang.syslog;

import java.io.IOException;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.syslog.RFC5424MessageConverter;
import org.springframework.integration.syslog.SyslogHeaders;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;

@SpringBootApplication
public class SysloggerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SysloggerApplication.class, args);
	}
	
	
	@Bean
	public UdpSyslogReceivingChannelAdapter udpReceiver() {
	    final UdpSyslogReceivingChannelAdapter adapter = new UdpSyslogReceivingChannelAdapter();
	    adapter.setUdpAdapter(receiver());
	    adapter.setOutputChannel( (message, timeout) -> { 
	    	Map<String,?> map = (Map)message.getPayload();
	    	String msg = (String)map.get(SyslogHeaders.UNDECODED);
	    	boolean ret = false;
	    	try {
	    		System.out.println(msg);
				tcpSyslog("192.168.0.157", "1468", msg);
				ret = true;
			} catch (SyslogException e) {

			} catch (IOException e) {

			}
	    	return ret;
	    });
	    adapter.setConverter(new RFC5424MessageConverter());
	    return adapter;
	}

	@Bean
	public UnicastReceivingChannelAdapter receiver() {
	    UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(514);
	    adapter.setTaskExecutor(executor());
	    return adapter;
	}

	@Bean
	public TaskExecutor executor() {
	    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
	    exec.setCorePoolSize(5);
	    return exec;
	}
	
	private void tcpSyslog(String ip, String port, String msg) throws SyslogException, IOException {
		String charset = "utf-8";
		TcpSyslogMessageSender messageSender = null;		
		if ("euc-kr".equalsIgnoreCase(charset)) {
			messageSender = new MyTcpSyslogMessageSender();
		} else if ("euc_kr".equalsIgnoreCase(charset)) {
			messageSender = new MyTcpSyslogMessageSender();
		} else if ("utf-8".equalsIgnoreCase(charset)) {
			messageSender = new TcpSyslogMessageSender();
		} else if ("utf8".equalsIgnoreCase(charset)) {
			messageSender = new TcpSyslogMessageSender();
		} else {
			throw new SyslogException("Unsupported charset : " + charset);
		}		
		messageSender.setDefaultMessageHostname("shellguard"); // some syslog cloud services may use this field to transmit a secret key
		messageSender.setDefaultAppName("scm");
		messageSender.setDefaultFacility(Facility.USER);
		messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
		messageSender.setSyslogServerHostname(ip);
		messageSender.setSyslogServerPort(Integer.parseInt(port));
		messageSender.setMessageFormat(MessageFormat.RFC_3164); // optional, default is RFC 3164
		// send a Syslog message
		messageSender.sendMessage(msg);
	}
	
	

}
