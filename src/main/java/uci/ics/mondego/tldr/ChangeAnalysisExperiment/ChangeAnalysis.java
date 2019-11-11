package uci.ics.mondego.tldr.ChangeAnalysisExperiment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.opencsv.CSVWriter;
import com.rfksystems.blake2b.Blake2b;

import io.netty.util.internal.ConcurrentSet;
import redis.clients.jedis.exceptions.JedisConnectionException;
import uci.ics.mondego.tldr.App;
import uci.ics.mondego.tldr.exception.DatabaseSyncException;
import uci.ics.mondego.tldr.exception.NullDbIdException;
import uci.ics.mondego.tldr.indexer.RedisHandler;
import uci.ics.mondego.tldr.tool.AccessCodes;
import uci.ics.mondego.tldr.tool.Databases;
import uci.ics.mondego.tldr.tool.FindAllTestDirectory;
import uci.ics.mondego.tldr.tool.StringProcessor;

public class ChangeAnalysis {
	private static final RedisHandler redisHandler = new RedisHandler();
	private static MessageDigest md;
	
	private static ConcurrentMap<String, List<String>> newMethods = new ConcurrentHashMap<String, List<String>>();
	private static ConcurrentMap<String, List<String>> deletedMethods = new ConcurrentHashMap<String, List<String>>();
	private static ConcurrentMap<String, List<String>> ChangedMethods = new ConcurrentHashMap<String, List<String>>();
	
	private static FileData sourceClassData = new FileData();
	private static FileData testClassData = new FileData();
	private static FileData jarData = new FileData();
	private static FileData otherFileData = new FileData();
	
	private static int totalSourceClass = 0;
	private static int totalTestClass = 0;
	private static int totalJar = 0;
	private static int totalFile = 0;
	

	public static void main(String[] args) {
		System.out.println("here at start of main");

		// TODO Auto-generated method stub
		String projectDir = args[0];
		String name = args[1];
		String commit = args[2];
		FindAllTestDirectory find = new FindAllTestDirectory(projectDir);
	    Set<String> allTestDir = find.getAllTestDir();
	    Map<String, Set<String>> changedSourceEntities = new HashMap<String, Set<String>>();
	    
	    try {
	    	md = MessageDigest.getInstance("MD5");
			List<String> allSourceClass = scanClassFiles(projectDir, Optional.of(allTestDir));
		    totalFile+= allSourceClass.size();
			
			List<String> allTestClass = new ArrayList<String>();
		    for (String dir: allTestDir) {
		    	allTestClass.addAll(scanClassFiles(dir, Optional.<Set<String>>absent()));
		    }
		    
		    // find all changed source
		    for (int i = 0; i < allSourceClass.size(); i++) {
		    	String claz = allSourceClass.get(i);
		    	if (hasClassChanged (claz)) {
		    		changedSourceEntities.put(claz, getChangedEntities(claz));
		    	}
		    }
		    
		    // find all changed tests
		    /*for (int i = 0; i < allTestClass.size(); i++) {
		    	String claz = allTestClass.get(i);
		    	if (hasClassChanged (claz)) {
		    		changedSourceEntities.put(claz, getChangedEntities(claz));
		    	}
		    }*/
		    
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JedisConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullDbIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.out.println("here at finally block");
			print(name, commit, changedSourceEntities);
			redisHandler.close();
			System.out.println("here at the end");
			System.exit(0);
		}
	}
	
	private static void print(String name, String commit, Map<String, Set<String>> map) {
	  String csv ="/lv_scratch/scratch/mondego/local/Maruf/TLDR/"+name+".csv";
      try{
    	  FileWriter pw = new FileWriter(csv, true);
	      int entity = 0; 
    	  for(Map.Entry<String, Set<String>> entry : map.entrySet()) {
	    	  entity += entry.getValue().size();
		  }
	      pw.append(name);
          pw.append(",");
          pw.append(commit);
          pw.append(",");
          pw.append(totalFile+"");
          pw.append(",");
          pw.append(map.size()+"");
          pw.append(",");
          pw.append(entity+"");
          pw.append("\n");
	      pw.flush();
          pw.close();
      }
      catch (IOException e) {
    	  e.printStackTrace();
      } 
	}

	
	/**
	 * Returns true if the specified file has changed.
	 */
	private static boolean hasClassChanged(String file) 
			throws JedisConnectionException, 
			NullDbIdException, 
			IOException, 
			NoSuchAlgorithmException {
		String currentCheckSum = calculateChecksum(file);
		
		if(!redisHandler.exists(Databases.TABLE_ID_FILE, file)){	
			redisHandler.update(Databases.TABLE_ID_FILE, file, currentCheckSum);			
			return true;
		}
		
		String prevCheckSum = redisHandler.getValueByKey(Databases.TABLE_ID_FILE, file);

		if (!prevCheckSum.equals(currentCheckSum)) {
			redisHandler.update(Databases.TABLE_ID_FILE, file, currentCheckSum);		
			return true;
		}
		return false;	
	}
	
	/**
	 * Method to scan repository for .class files. This method can scan a  project's
	 * source and test repository and returns a list of class file. 
	 * @param directoryName The directory from where the scanning is started.
	 * @param exclude an optional set of directories which are excluded from the search. 
	 */
	private static List<String> scanClassFiles(String directoryName, Optional<Set<String>> exclude) 
			throws InstantiationException, 
			IllegalAccessException,
			IllegalArgumentException, 
			InvocationTargetException, 
			NoSuchMethodException, 
			SecurityException {	
		
		List<String> classFiles = new ArrayList<String>();
		File directory = new File(directoryName);
	    File[] fList = directory.listFiles();	    	
	    
	    if(fList != null)
	        for (File file : fList) {    	        	
	            if (file.isFile()) {
	            	String fileAbsolutePath = file.getAbsolutePath();	
	                if(fileAbsolutePath.endsWith(".class")){
	                	classFiles.add(fileAbsolutePath);
	                }	                	         
	            } 
	            else if (file.isDirectory()) {
	            	if (!exclude.isPresent() || !exclude.get().contains(file.getAbsolutePath().toString())) {
		            	classFiles.addAll(scanClassFiles(file.getAbsolutePath(), exclude));
	            	}
	            }
	        }
	    return classFiles;
	  }	
	
}
