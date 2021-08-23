package com.kichang.syslog;

import org.springframework.integration.syslog.RFC5424MessageConverter;
import org.springframework.integration.syslog.RFC5424SyslogParser;

public class KRRFC5424MessageConverter extends RFC5424MessageConverter{

	public KRRFC5424MessageConverter() {
		super();
		this.setCharset("euc-kr");
	}

	
}
