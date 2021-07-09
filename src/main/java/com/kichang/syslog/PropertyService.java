package com.kichang.syslog;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Service
public class PropertyService {
	List<String> listConfig = Collections.synchronizedList(new ArrayList<String>());
	private final String FILENAME = "./list.cfg";
	
	public PropertyService() {
		readAndUpdate();
	}
	
	private void readAndUpdate() {
		try {
			List<String> allLines = Files.readAllLines(Paths.get(FILENAME));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	
	
	@Scheduled(fixedDelay= 5 * 1000)
	public void monitorList() {
		readAndUpdate();
	}
	
	
	
	
	
	
	
}
