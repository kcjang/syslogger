package com.kichang.syslog;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.integration.syslog.DefaultMessageConverter;
import org.springframework.integration.syslog.MessageConverter;
import org.springframework.integration.syslog.RFC5424MessageConverter;
import org.springframework.integration.syslog.SyslogHeaders;
import org.springframework.integration.syslog.inbound.UdpSyslogReceivingChannelAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.AbstractSyslogMessageSender;
import com.cloudbees.syslog.sender.SyslogMessageSender;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;

@SpringBootApplication
@EnableScheduling
public class SysloggerApplication {
	Log logger = LogFactory.getLog(SysloggerApplication.class);
	@Autowired PropertyService propertyService;
	@Autowired Environment env;
	@Value("${tcp.maxretry}") private int maxRetryCount;

	public static void main(String[] args) {
	    for(String arg : args) {
	        if (arg.equals("-v") || arg.equals("--version")) {
	          InputStream is = SysloggerApplication.class.getClassLoader().getResourceAsStream("version.txt");
	          Properties prop = new Properties();
	          try {
	            prop.load(is);
	            String version = (String)prop.get("Version");
	            System.out.println(version);
	            System.exit(1);
	          } catch (IOException e) {
	            System.err.println("version.txt not found in classpath");
	            System.exit(1);
	          }
	        }
	      }
	      
		//SpringApplication.run(SysloggerApplication.class, args);
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SysloggerApplication.class)
		        .properties("spring.config.location=classpath:/application.yml," + 
		        			"file:./syslogger.yml").build().run(args);
	}
	
	@Bean
	public UdpSyslogReceivingChannelAdapter udpReceiver() {
	    final UdpSyslogReceivingChannelAdapter adapter = new UdpSyslogReceivingChannelAdapter();
	    adapter.setUdpAdapter(receiver());
	    adapter.setOutputChannel((message, timeout) -> {
	    			Map<String,?> map = (Map)message.getPayload();
	    			logger.debug(message);
	    	    	String msg = (String)map.get(SyslogHeaders.UNDECODED);
    	    		logger.debug("output channel msg : " + msg);
    	    		List<String[]> list = propertyService.getListConfig();	
    				for(String[] parts : list) {
    					try {
	    					SyslogMessageSender sender = getMessageSender(parts);
	    					logger.debug(String.format("send to : %s:%s:%s", parts[0], parts[1], parts[2]));
    					
							sender.sendMessage(msg);
						} catch (IOException e) {
							logger.error(e.getMessage());
						} catch (InvalidateConfigFormatException e) {
							logger.error(e.getMessage());
						}
    				}

	    	    	return true;
	    		});
	    RFC5424MessageConverter converter = new KRRFC5424MessageConverter();
	    adapter.setConverter(converter);
	    return adapter;
	}

	
	private SyslogMessageSender getMessageSender(String pars[]) throws InvalidateConfigFormatException {
		String protocol = "udp";
		String encoding = "utf-8";
		
		if ("tcp".equalsIgnoreCase(pars[0]) || "udp".equalsIgnoreCase(pars[0])) {
			protocol = pars[0];
		} else {
			throw new InvalidateConfigFormatException(pars.toString());
		}
		
		if (pars.length > 3) {
			encoding = pars[3];
		}
		
		return getSyslogMessageSender(protocol, encoding, pars[1], pars[2]);
		
	}
	

	private SyslogMessageSender getSyslogMessageSender(String protocol, String encoding, String ip, String port) {

		AbstractSyslogMessageSender messageSender = null;
		if (encoding == null)
			encoding = "utf-8";
		
		if ("tcp".equalsIgnoreCase(protocol)) {
			messageSender = new KRTcpSyslogMessageSender(encoding, maxRetryCount);
		} else if ("udp".equalsIgnoreCase(protocol)) {
			messageSender = new KRUdpSyslogMessageSender(encoding);
		}
		
		setSyslogMessageSender(messageSender, ip, port);
		return messageSender;
	}
	
	private UdpSyslogMessageSender getUdpSyslogMessageSender(String ip, String port) {
		UdpSyslogMessageSender messageSender = new UdpSyslogMessageSender();
		setSyslogMessageSender(messageSender, ip, port);
		return messageSender;
	}
	
	private void setSyslogMessageSender(AbstractSyslogMessageSender messageSender, String ip, String port) {
		messageSender.setDefaultMessageHostname("sgserver"); // some syslog cloud services may use this field to transmit a secret key
		messageSender.setDefaultAppName("shellguard");
		messageSender.setDefaultFacility(Facility.USER);
		messageSender.setDefaultSeverity(Severity.NOTICE);
		messageSender.setSyslogServerHostname(ip);
		messageSender.setSyslogServerPort(Integer.parseInt(port));
		messageSender.setMessageFormat(MessageFormat.RFC_3164); // optional, default is RFC 3164
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
	

	

}
