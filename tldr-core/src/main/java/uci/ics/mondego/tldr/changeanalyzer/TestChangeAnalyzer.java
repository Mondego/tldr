package uci.ics.mondego.tldr.changeanalyzer;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.commons.lang3.StringUtils;
import uci.ics.mondego.tldr.TLDR;
import uci.ics.mondego.tldr.exception.DatabaseSyncException;
import uci.ics.mondego.tldr.exception.NullDbIdException;
import uci.ics.mondego.tldr.tool.AccessCodes;
import uci.ics.mondego.tldr.tool.DatabaseIDs;
import uci.ics.mondego.tldr.tool.StringProcessor;

public class TestChangeAnalyzer extends ChangeAnalyzer{

	private final ClassParser parser;
	private final JavaClass parsedClass;
	private Map<String, Integer> allPreviousTestCases;
	private List<String> allInterfaces;
	private String superClass;
	private Map<String, Method> allExtractedMethods;
	
	public TestChangeAnalyzer(String className) 
			throws IOException, DatabaseSyncException, EOFException {
		super(className);
		this.parser = new ClassParser(this.getEntityName());
		this.parsedClass = parser.parse();
		this.allPreviousTestCases = new HashMap<String, Integer>();
		this.allInterfaces = new ArrayList<String>();
		this.superClass = "";
		this.allExtractedMethods = new HashMap<String, Method>();
		Set<String> prevTst = redisHandler.getAllKeysByPattern
				(DatabaseIDs.TABLE_ID_ENTITY, parsedClass.getClassName()+".*");  
		
		for(String entity: prevTst){
			allPreviousTestCases.put(entity, 0);
		}
		
		this.parse();
		try {
			this.syncClassHierarchy();
			this.deleteDepreciatedTestCases();
			this.closeRedis();
		} 
		catch (NullDbIdException e1) {
			e1.printStackTrace();
		}
		
	}
	
	private void syncClassHierarchy() throws NullDbIdException {	
		this.parseInterface();
		this.parseSuperClass();
		
		Set<String> all_superclass_interface = redisHandler.getSet(
				DatabaseIDs.TABLE_ID_INTERFACE_SUPERCLASS, 
				parsedClass.getClassName());
				
		for( int i = 0; i < allInterfaces.size(); i++) {
			if(!all_superclass_interface.contains(allInterfaces.get(i))
				&& !(allInterfaces.get(i).startsWith("java.") 
				|| allInterfaces.get(i).startsWith("junit."))) {
				
				redisHandler.insertInSet(
						DatabaseIDs.TABLE_ID_INTERFACE_SUPERCLASS, 
						parsedClass.getClassName(), 
						allInterfaces.get(i));
				redisHandler.insertInSet(
						DatabaseIDs.TABLE_ID_SUBCLASS, 
						allInterfaces.get(i), 
						parsedClass.getClassName());
			}				
		}
		
		if(!all_superclass_interface.contains(this.superClass) 
				&& superClass != null 
				&& superClass.length() > 0 
				&& !superClass.startsWith("java") 
				&& !superClass.startsWith("junit")){
			
			redisHandler.insertInSet(
					DatabaseIDs.TABLE_ID_INTERFACE_SUPERCLASS, 
					parsedClass.getClassName(), 
					superClass);
			redisHandler.insertInSet(
					DatabaseIDs.TABLE_ID_SUBCLASS, 
					superClass, 
					parsedClass.getClassName());
		}				
	}
	
	private void parseInterface(){
		try {
			String[] interfaces = this.parsedClass.getInterfaceNames();
			for(String cls: interfaces){
				allInterfaces.add(cls);
			}
		} 
		catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void parseSuperClass(){
		try {
			superClass = !parsedClass.getSuperclassName().startsWith("java.") 
					? parsedClass.getSuperclassName(): "";			
		} 
		catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	protected void parse() throws IOException, DatabaseSyncException{
		
		Method [] allMethods= parsedClass.getMethods();
		for(Method m: allMethods){
						
			if(m.getModifiers() == AccessCodes.ABSTRACT || 
				m.getModifiers() == AccessCodes.FINAL ||
				m.getModifiers() == AccessCodes.INTERFACE || 
				m.getModifiers() == AccessCodes.NATIVE || 
				m.getModifiers() == AccessCodes.PRIVATE || 
				m.getModifiers() == AccessCodes.PROTECTED || 
				m.getModifiers() == AccessCodes.PUBLIC || 
				m.getModifiers() == AccessCodes.STATIC || 
				m.getModifiers() == AccessCodes.STATIC_INIT ||
				m.getModifiers() == AccessCodes.STRICT || 
				m.getModifiers() == AccessCodes.SYNCHRONIZED || 
				m.getModifiers() == AccessCodes.TRANSIENT || 
				m.getModifiers() == AccessCodes.VOLATILE ||
				m.getModifiers() == AccessCodes.DEFAULT_INIT ||
				m.getModifiers() == AccessCodes.PUBLIC2 ||
				m.getModifiers() == AccessCodes.INHERIT ||		
				m.getModifiers() == AccessCodes.PUBLIC3 ||			
				m.getModifiers() == AccessCodes.PUBLIC4 ||			
				m.getModifiers() == AccessCodes.PUBLIC5 ||				
				m.getModifiers() == AccessCodes.ABSTRACT2 ||				
				m.getModifiers() == AccessCodes.STATIC2 ||	
				m.getModifiers() == AccessCodes.STATIC3 ||	
				m.getModifiers() == AccessCodes.INNER ||	
				m.getModifiers() == AccessCodes.DEFAULT_INIT2 ||
				m.getModifiers() == AccessCodes.FINAL2 ||
				m.getModifiers() == AccessCodes.STATIC4 ||
				m.getModifiers() == AccessCodes.ABSTRACT3||
				m.getModifiers() == AccessCodes.FINAL3 ||
				m.getModifiers() == AccessCodes.STATIC5 ||
				m.getModifiers() == AccessCodes.STATIC6 ||
				m.getModifiers() == AccessCodes.STATIC7 ||
				m.getModifiers() == AccessCodes.STATIC8 ||
				m.getModifiers() == AccessCodes.FINAL4 ||
				m.getModifiers() == AccessCodes.PUBLIC6 ||
				m.getModifiers() == AccessCodes.PUBLIC7 ||
				m.getModifiers() == AccessCodes.PUBLIC8 ||
				m.getModifiers() == AccessCodes.INNER2 ||	
				m.getModifiers() == AccessCodes.FINAL5 ||
				m.getModifiers() == AccessCodes.PUBLIC9 ||
				m.getModifiers() == AccessCodes.INNER3 ||	
				m.getModifiers() == AccessCodes.INNER4 ||					
				m.getModifiers() == AccessCodes.ABSTRACT4 ||
				m.getModifiers() == AccessCodes.PUBLIC10 ||					
				m.getModifiers() == AccessCodes.PUBLIC11 ||
				m.getModifiers() == AccessCodes.PUBLIC12){
				
				String code =  m.getModifiers()+"\n"+ m.getName()+ 
				"\n"+m.getSignature()+"\n"+ m.getCode();
							
				String lineInfo = code.substring(code.indexOf("Attribute(s)") == -1
						? 0 : code.indexOf("Attribute(s)"), 
						code.indexOf("LocalVariable(") == -1?
						code.length() : code.indexOf("LocalVariable(")) ;
				
				// Changes in other function impacts line# of other functions
				//...so Linecount info of the code must be removed
				code = StringUtils.replace(code, lineInfo, ""); 
					
				// For some reason StackMapTable also change unwanted. WHY??
				code = code.substring(0, code.indexOf("StackMapTable") == -1
						? code.length() 
						: code.indexOf("StackMapTable"));  
				
				// For some reason StackMapTable also change unwanted. WHY??
				code = code.substring(0, code.indexOf("StackMap") == -1
						? code.length() 
						: code.indexOf("StackMap"));  
				
				String methodFqn = parsedClass.getClassName()+"."+m.getName();
	
				methodFqn += ("(");
				for(int i=0;i<m.getArgumentTypes().length;i++) {
					methodFqn += ("$"+m.getArgumentTypes()[i]);
				}
					
				methodFqn += (")");
				
				String currentHashCode = StringProcessor.CreateBLAKE(code);
				
				if (!allPreviousTestCases.containsKey(methodFqn)) {
					TLDR.allNewAndChangeTests.put(methodFqn, true);
					allExtractedMethods.put(methodFqn, m);
								
					boolean ret1 = sync(DatabaseIDs.TABLE_ID_ENTITY, methodFqn, currentHashCode);
				    
					boolean ret2 = sync(DatabaseIDs.TABLE_ID_TEST_ENTITY, methodFqn, "1");
					
					if (!ret1 && !ret2) {
						throw new DatabaseSyncException(methodFqn);
					}
				}
				
				else{
					allPreviousTestCases.put(methodFqn, allPreviousTestCases.get(methodFqn) + 1);
					String prevHashCode = getValue(DatabaseIDs.TABLE_ID_ENTITY, methodFqn);	
					
					if (!currentHashCode.equals(prevHashCode)) {
						TLDR.allNewAndChangeTests.put(methodFqn, true);						
						allExtractedMethods.put(methodFqn, m);
						boolean ret = sync(
								DatabaseIDs.TABLE_ID_ENTITY, methodFqn, currentHashCode);
						
						if(!ret) {
							throw new DatabaseSyncException(methodFqn);
						}
					}
				}
			}
		}		
	}
	
	private long deleteDepreciatedTestCases(){
		long count = 0;
		for ( Map.Entry<String, Integer> entry : allPreviousTestCases.entrySet()) {
		    Integer val = entry.getValue();
		    if(val == 0){
		    	count++;
		    	String key = entry.getKey();
		    	this.redisHandler.removeKey(DatabaseIDs.TABLE_ID_ENTITY, key);
		    	
		    	Set<String> allDependencies = this.redisHandler.getSet(
		    			DatabaseIDs.TABLE_ID_FORWARD_INDEX_TEST_DEPENDENCY, 
		    			key);
		    	
		    	redisHandler.removeKey(
		    			DatabaseIDs.TABLE_ID_FORWARD_INDEX_TEST_DEPENDENCY, key);
		    	for(String dep: allDependencies){
		    		redisHandler.removeFromSet(
		    				DatabaseIDs.TABLE_ID_TEST_DEPENDENCY, dep, key);
		    	}
		    	
		    	redisHandler.removeKey(DatabaseIDs.TABLE_ID_TEST_ENTITY, key);		    	
		    }  
		}
		return count;
	}
	
	public Map<String, Method> getAllExtractedMethods(){
		return allExtractedMethods;
	}
}
