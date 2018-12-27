package uci.ics.mondego.tldr;


import uci.ics.mondego.tldr.indexer.RedisHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;

import redis.clients.jedis.exceptions.JedisConnectionException;
import uci.ics.mondego.tldr.changeanalyzer.ClassChangeAnalyzer;
import uci.ics.mondego.tldr.extractor.ByteCodeParser;
import uci.ics.mondego.tldr.extractor.ClassParserExample;
import uci.ics.mondego.tldr.model.SourceFile;



/**
 * Hello world!
 *
 */
public class App 
{
	private static String PROJ_DIR;
    public static void main( String[] args )
    {
    	BasicConfigurator.configure();

    	RedisHandler rh = null;
       try{
	       PROJ_DIR = "/Users/demigorgan/brigadier";
	       
	    	//Scan the repository - gets java, test, class, and jar files. 
	       RepoScanner rs = new RepoScanner(PROJ_DIR);
	       
	       // in memory database handler
	       rh = new RedisHandler();
	       
	       List<SourceFile> allClass = rs.get_all_class_files();
	       List<SourceFile> allTestClass = rs.get_all_test_class_files();
	       
	       List<SourceFile> changedFiles = new ArrayList<SourceFile>();
	       //ByteCodeParser bp = new ByteCodeParser(allClass.get(11));
	       
	       //ClassParserExample cpe = new ClassParserExample();
	       //cpe.parse("/Users/demigorgan/brigadier/build/class"
	       //		+ "es/java/main/com/mojang/brigadier/context/CommandContext.class");
	       
	       ClassChangeAnalyzer cha = new ClassChangeAnalyzer("/Users/demigorgan/brigadier/build/classes/java/main/com/mojang/brigadier/context/CommandContext.class");
	       
	       
	       for(int i=0;i<allClass.size();i++){
	    	   if(!rh.exists(allClass.get(i).getPath())){
	    		   
	    		   System.out.println("file inserted");
	    		   rh.insert(allClass.get(i).getPath(), allClass.get(i).getCurrentCheckSum());
	    	   }
	    	   else{
	    		   //System.out.println("file exists");
	    		   String currentCheckSum = allClass.get(i).getCurrentCheckSum();
	    		   String prevCheckSum = rh.getValue(allClass.get(i).getPath());
	    		   
	    		   if(!currentCheckSum.equals(prevCheckSum)){
	    		       ByteCodeParser bp = new ByteCodeParser(allClass.get(i));
	    		       
	    		       System.out.println("file changed "+allClass.get(i).getPath());
	        		   changedFiles.add(allClass.get(i));
	        		   rh.insert(allClass.get(i).getPath(), currentCheckSum);
	    		   }
	    	   }
	    	   
	       }
	       
       }
       
       catch( JedisConnectionException e){
    	   System.out.println("No Connection to Jedis Server");
    	   e.printStackTrace();   
       }
       
       catch(IOException e){
    	   System.out.println("extractor can't read the designated class");
    	   e.printStackTrace();
       }
       
       catch(ArrayIndexOutOfBoundsException e){
    	   System.out.println("the file path is too long");
    	   e.printStackTrace();
       }
       
       catch(NullPointerException e){
    	   System.out.println("extractor can't find the designated class");
    	   e.printStackTrace();
       }
       
       
       finally{
    	   rh.close();
       }
    	
    }
}
