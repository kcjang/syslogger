package com.kichang.syslog;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.stereotype.Component;


@Component
public class ConfigFileChangeListener implements FileChangeListener {

    private boolean isLocked(Path path) {
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.WRITE); FileLock lock = ch.tryLock()) {
            return lock == null;
        } catch (IOException e) {
            return true;
        }
    }

	@Override
	public void onChange(Set<ChangedFiles> changeSet) {
		for(ChangedFiles cfiles : changeSet) {
            for(ChangedFile cfile: cfiles.getFiles()) {
                if( /* (cfile.getType().equals(Type.MODIFY) 
                     || cfile.getType().equals(Type.ADD)  
                     || cfile.getType().equals(Type.DELETE) ) && */ !isLocked(cfile.getFile().toPath())) {
                    System.out.println("Operation: " + cfile.getType() 
                      + " On file: "+ cfile.getFile().getName() + " is done");
                }
            }
        }
		
	}

}