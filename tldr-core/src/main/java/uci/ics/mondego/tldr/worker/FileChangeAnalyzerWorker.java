package uci.ics.mondego.tldr.worker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import uci.ics.mondego.tldr.TLDR;
import uci.ics.mondego.tldr.changeanalyzer.FileChangeAnalyzer;
import uci.ics.mondego.tldr.exception.DatabaseSyncException;

@SuppressWarnings("rawtypes")
public class FileChangeAnalyzerWorker extends Worker {

	private final String fileToAnalyze;
	private static final Logger logger = LogManager.getLogger(FileChangeAnalyzerWorker.class);
	
	public FileChangeAnalyzerWorker(String filePath){
		this.fileToAnalyze = filePath;
	}
	
	public FileChangeAnalyzerWorker(String workerName, String filePath){
		super(workerName);
		this.fileToAnalyze = filePath;
	}
	
	public void run() {
		try {			
            this.changeAnalyzer();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }		
	}
	
	public void changeAnalyzer(){
		FileChangeAnalyzer fc;
		try {
			fc = new FileChangeAnalyzer(fileToAnalyze);
			logger.debug(fileToAnalyze + " is being analyzed");
			if(fc.hasChanged()){
				logger.debug(fileToAnalyze + " has been changed");
				logger.debug(fileToAnalyze + " in being pushed to EntityChangeAnlysis pool");
				TLDR.EntityChangeAnalysisPool.send(fileToAnalyze);
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (DatabaseSyncException e) {
			e.printStackTrace();
		}
	}
}
