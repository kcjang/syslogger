package com.kichang.syslog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class PropertyService {
	Log logger = LogFactory.getLog(PropertyService.class);
	
	List<String[]> listConfig = Collections.synchronizedList(new ArrayList<String[]>());
	private final String FILENAME = "./list.cfg";
	
	public PropertyService() {
		readAndUpdate();
	}
	
	@Scheduled(fixedDelay= 5 * 1000)
	public void monitorList() {
		readAndUpdate();
	}
	
	public List<String[]> getListConfig() {
		return listConfig;
	}
	
	private void readAndUpdate() {
		try {
			List<String> allLines = Files.readAllLines(Paths.get(FILENAME));
			List<String[]> lines = new ArrayList<String[]>();
			
			for(String line : allLines) {
				String[] parts = parseLine(line);
				if (parts == null) 
					continue;
				else {
					lines.add(parts);
				}
			}
			listConfig = lines;
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private String[] parseLine(String line) {
		if (line.trim().startsWith("#")) {
			return null;
		}
		
		String[] pars = line.trim().split(":");
		if (pars == null || (pars.length != 3 && pars.length != 4)) {
			logger.error("Invalid Config Line : " + line);
			return null;
		}
		if (!StringUtils.isNumeric(pars[2])) {
			logger.error("Invalid Config Line : " + line);
			return null;
		}
		if ( !(pars[0].equalsIgnoreCase("tcp") || pars[0].equalsIgnoreCase("udp"))) {
			logger.error("Invalid Config Line : " + line);
			return null;
		}
		return pars;
	}
	

	
	
	
	
	
	
}
