package com.kichang.syslog;

public class InvalidateConfigFormatException extends Exception {

	public InvalidateConfigFormatException(String cfg) {
		super(cfg);
	}

	public InvalidateConfigFormatException() {
		super();
	}

}
