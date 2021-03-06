package uci.ics.mondego.tldr.worker;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import uci.ics.mondego.tldr.TLDR;
import uci.ics.mondego.tldr.map.EntityToTestMap;

@SuppressWarnings("rawtypes")
public class EntityToTestMapWorker extends Worker {
	
	private final String entity;
	private static final Logger logger = LogManager.getLogger(EntityToTestMapWorker.class);
	
	public EntityToTestMapWorker(String entity){
		this.entity = entity;
	}

	public void run() {
		try {
			this.map();
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
		}
	}
	
	private void map() 
			throws InstantiationException, 
			IllegalAccessException, 
			IllegalArgumentException, 
			InvocationTargetException, 
			NoSuchMethodException, 
			SecurityException {
	   EntityToTestMap map = new EntityToTestMap();
	   Set<String> tests = map.getTests(entity);
 	   
	   for(String str: tests) {
		   TLDR.IntraTestTraversalPool.send(str);
 	   }  	   
	   map.closeRedis();
	}
}
