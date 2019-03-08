package uci.ics.mondego.tldr.worker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.apache.bcel.classfile.Method;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import uci.ics.mondego.tldr.App;
import uci.ics.mondego.tldr.changeanalyzer.ClassChangeAnalyzer;
import uci.ics.mondego.tldr.exception.DatabaseSyncException;

public class ClassChangeAnalyzerWorker extends Worker{

	private final String className;
	private static final Logger logger = LogManager.getLogger(ClassChangeAnalyzerWorker.class);
	
	public ClassChangeAnalyzerWorker( String className){
		this.className = className;
	}
	
	public ClassChangeAnalyzerWorker(String name, String className){
		super(name);
		this.className = className;
	}
	
	public void run() {
		try {
            this.changeAnalyzer();
        } 
		catch (NoSuchElementException e) {
            e.printStackTrace();
        } 
		catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }	
	}
	
	private void changeAnalyzer(){
	    ClassChangeAnalyzer cc;
		try {
			cc = new ClassChangeAnalyzer(className);
			HashMap<String, Method> m = cc.getextractedFunctions();	 
	 		if(m.size() > 0){
	 			logger.debug(className.substring(className.lastIndexOf("/"))
	 					+" -- Some method changed and sent to DependencyExtractionPool");
	 			App.DependencyExtractionPool.send(m);	
	 		}
		} 
		catch (IOException e1) {
			e1.printStackTrace();
		} 
		catch (InstantiationException e1) {
			e1.printStackTrace();
		} 
		catch (IllegalAccessException e1) {
			e1.printStackTrace();
		} 
		catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} 
		catch (InvocationTargetException e1) {
			e1.printStackTrace();
		} 
		catch (NoSuchMethodException e1) {
			e1.printStackTrace();
		} 
		catch (SecurityException e1) {
			e1.printStackTrace();
		} 
		catch (DatabaseSyncException e) {
			e.printStackTrace();
		} 	 	      
	}	
}
