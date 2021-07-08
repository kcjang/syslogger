package com.kichang.syslog;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import com.cloudbees.syslog.sender.AbstractSyslogMessageSender;
import com.cloudbees.syslog.sender.SyslogMessageSender;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

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
				
	    		
	    		
				ret = true;
			} finally {
				
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
	
	private SyslogMessageSender getMessageSender(String cfg) throws InvalidateConfigFormatException {
		String[] pars = cfg.trim().split(":");
		if (pars == null || pars.length != 3) {
			throw new InvalidateConfigFormatException(cfg);
		}
		
		if (!StringUtils.isNumeric(pars[2])) {
			throw new InvalidateConfigFormatException(cfg); 
		}
		
		if (pars[0].equalsIgnoreCase("tcp")) {
			return getTcpSyslogMessageSender(pars[1], pars[2]);
		} else if (pars[0].equalsIgnoreCase("udp")) {
			return getUdpSyslogMessageSender(pars[1], pars[2]);
		} else {
			throw new InvalidateConfigFormatException(cfg); 
		}
		
	}
	
	private TcpSyslogMessageSender getTcpSyslogMessageSender(String ip, String port) {
		TcpSyslogMessageSender messageSender = new TcpSyslogMessageSender();
		messageSender.setDefaultMessageHostname("shellguard"); // some syslog cloud services may use this field to transmit a secret key
		messageSender.setDefaultAppName("shellguard");
		messageSender.setDefaultFacility(Facility.USER);
		messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
		messageSender.setSyslogServerHostname(ip);
		messageSender.setSyslogServerPort(Integer.parseInt(port));
		messageSender.setMessageFormat(MessageFormat.RFC_3164); // optional, default is RFC 3164
		return messageSender;
	}
	
	private UdpSyslogMessageSender getUdpSyslogMessageSender(String ip, String port) throws SyslogException, IOException {
		UdpSyslogMessageSender messageSender = new UdpSyslogMessageSender();
		messageSender.setDefaultMessageHostname("shellguard"); // some syslog cloud services may use this field to transmit a secret key
		messageSender.setDefaultAppName("shellguard");
		messageSender.setDefaultFacility(Facility.USER);
		messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
		messageSender.setSyslogServerHostname(ip);
		messageSender.setSyslogServerPort(Integer.parseInt(port));
		messageSender.setMessageFormat(MessageFormat.RFC_3164); // optional, default is RFC 3164
	}

}
