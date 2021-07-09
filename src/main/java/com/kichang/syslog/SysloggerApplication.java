package com.kichang.syslog;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.syslog.RFC5424MessageConverter;
import org.springframework.integration.syslog.SyslogHeaders;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.SyslogMessageSender;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

@SpringBootApplication
@EnableScheduling
public class SysloggerApplication {
	Log logger = LogFactory.getLog(SysloggerApplication.class);
	@Autowired PropertyService propertyService;
	@Autowired Environment env;
	
	
	public static void main(String[] args) {
		//SpringApplication.run(SysloggerApplication.class, args);
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SysloggerApplication.class)
		        .properties("spring.config.location=classpath:/application.yml," + 
		        			"file:./syslogger.yml").build().run(args);
	}
	
	
	@Bean
	public CommandLineRunner run(@Autowired Environment env) {
		return (args) -> {
			var e=env.getProperty("server.error.include-stacktrace");
			//System.out.println("server.error.include-stacktrace: "+e);
		};
	}
	
	@Bean
	public UdpSyslogReceivingChannelAdapter udpReceiver() {
		//Log logger = LogFactory.getLog("udpReceiver");
	    final UdpSyslogReceivingChannelAdapter adapter = new UdpSyslogReceivingChannelAdapter();
	    adapter.setUdpAdapter(receiver());
	    adapter.setOutputChannel( (message, timeout) -> { 
	    	Map<String,?> map = (Map)message.getPayload();
	    	String msg = (String)map.get(SyslogHeaders.UNDECODED);
	    	boolean ret = false;
	    	try {
	    		logger.info("output channel : " + msg);
	    		
	    		
	    		List<String[]> list = propertyService.getListConfig();	
				for(String[] parts : list) {
					SyslogMessageSender sender = getMessageSender(parts);
					//System.out.println(String.format("send to : %s:%s:%s", parts[0], parts[1], parts[2]));
					sender.sendMessage(msg);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			} 
	    	return ret;
	    });
	    adapter.setConverter(new RFC5424MessageConverter());
	    return adapter;
	}

	@Bean
	public UnicastReceivingChannelAdapter receiver() {
		int port = 514;
		String p = env.getProperty("syslog.port");
		if (p != null && org.apache.commons.lang3.StringUtils.isNumeric(p)) {
			port = Integer.parseInt(p);
		}
		logger.info("Syslogger is listen on udp port " + port);
	    UnicastReceivingChannelAdapter adapter = new UnicastReceivingChannelAdapter(port);
	    adapter.setTaskExecutor(executor());
	    return adapter;
	}

	@Bean
	public TaskExecutor executor() {
	    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
	    exec.setCorePoolSize(5);
	    return exec;
	}
	
	private SyslogMessageSender getMessageSender(String pars[]) throws InvalidateConfigFormatException {
		if (pars[0].equalsIgnoreCase("tcp")) {
			return getTcpSyslogMessageSender(pars[1], pars[2]);
		} else if (pars[0].equalsIgnoreCase("udp")) {
			return getUdpSyslogMessageSender(pars[1], pars[2]);
		} else {
			throw new InvalidateConfigFormatException(pars.toString());
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
	
	private UdpSyslogMessageSender getUdpSyslogMessageSender(String ip, String port) {
		UdpSyslogMessageSender messageSender = new UdpSyslogMessageSender();
		messageSender.setDefaultMessageHostname("shellguard"); // some syslog cloud services may use this field to transmit a secret key
		messageSender.setDefaultAppName("shellguard");
		messageSender.setDefaultFacility(Facility.USER);
		messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
		messageSender.setSyslogServerHostname(ip);
		messageSender.setSyslogServerPort(Integer.parseInt(port));
		messageSender.setMessageFormat(MessageFormat.RFC_3164); // optional, default is RFC 3164
		
		return messageSender;
	}

}
