package uci.ics.mondego.tldr.worker;

import java.io.EOFException;
import java.io.IOException;
import java.util.Map;

import org.apache.bcel.classfile.Method;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import uci.ics.mondego.tldr.TLDR;
import uci.ics.mondego.tldr.changeanalyzer.TestChangeAnalyzer;
import uci.ics.mondego.tldr.exception.DatabaseSyncException;

@SuppressWarnings("rawtypes")
public class TestChangeAnalyzerAndIndexerWorker extends Worker {
	
	private final String testClassName;
	private static final Logger logger = LogManager.getLogger(TestChangeAnalyzerAndIndexerWorker.class);
	
	public TestChangeAnalyzerAndIndexerWorker(String name){
		this.testClassName = name;
	}
	
	public void run(){
		try {
			//logger.debug(testClassName.substring(testClassName.lastIndexOf("/")+1)
			//		+"indexed, end of the pool parseindextest");
			TestChangeAnalyzer ts = new TestChangeAnalyzer(testClassName);
			Map<String, Method> allExtractedMethods = ts.getAllExtractedMethods();
			TLDR.allExtractedTestMethods.putAll(allExtractedMethods);
		} catch (EOFException e){
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DatabaseSyncException e) {
			e.printStackTrace();
		}	
	}
}
