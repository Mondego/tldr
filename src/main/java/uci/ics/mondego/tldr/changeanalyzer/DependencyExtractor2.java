package uci.ics.mondego.tldr.changeanalyzer;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.bcel.classfile.Method;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.functors.AllPredicate;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import uci.ics.mondego.tldr.exception.UnknownDBIdException;
import uci.ics.mondego.tldr.extractor.MethodParser;
import uci.ics.mondego.tldr.indexer.RedisHandler;
import uci.ics.mondego.tldr.tool.Databases;

public class DependencyExtractor2 {

	protected final Entry<String, Method> changedMethod;
	protected final RedisHandler rh;
	public static final Logger logger = LogManager.getLogger(ClassChangeAnalyzer.class);
	private final String dbId;
	private List<String> fieldValueChanged;
	private Map<String, Integer> previousDependencies;
	
	public DependencyExtractor2(Entry<String, Method> changedMethod) throws IOException {
		this.changedMethod = changedMethod;
		this.dbId = Databases.TABLE_ID_DEPENDENCY;
		this.fieldValueChanged = new ArrayList<String>();
		this.rh = new RedisHandler();
		this.previousDependencies = new HashMap<String, Integer>();	
		Set<String> prevDepInSet = rh.getSet(Databases.TABLE_ID_FORWARD_INDEX_DEPENDENCY, 
				changedMethod.getKey());
		
		for(String dependency: prevDepInSet){
			previousDependencies.put(dependency, 0);
		}
		
		this.resolute();
		rh.close();
	}
	
	public DependencyExtractor2(Entry<String, Method> changedMethod, boolean flag) throws IOException{
		this.changedMethod = changedMethod;
		this.dbId = Databases.TABLE_ID_TEST_DEPENDENCY;
		this.fieldValueChanged = new ArrayList<String>();
		this.rh = new RedisHandler();
		this.previousDependencies = new HashMap<String, Integer>();	
		Set<String> prevDepInSet = rh.getSet(Databases.TABLE_ID_FORWARD_INDEX_DEPENDENCY, 
				changedMethod.getKey());
		
		for(String dependency: prevDepInSet){
			previousDependencies.put(dependency, 0);
		}
		
		this.resolute();
		this.removeAllDepreciateDependency();
		rh.close();
	}
	
	 public void resolute() throws IOException{
		 		 	
			String dependent = changedMethod.getKey();
			Method m = changedMethod.getValue();
						
			MethodParser parser = new MethodParser(m);
			List<String> allVirtualDependency = parser.getAllVirtualDependency();
			List<String> allInterfaceDependency = parser.getAllInterfaceDependency();
			List<String> allStaticDependency = parser.getAllStaticDependency();
			List<String> allFinalDependency = parser.getAllFinalDependency();
			List<String> allSpecialDependency = parser.getAllSpecialDependency();
			List<String> allStaticFieldUpdated = parser.getAllStaticFieldUpdated();
			List<String> allOwnFieldUpdated = parser.getAllOwnFieldUpdated();
						
			
			this.fieldValueChanged.addAll(allStaticFieldUpdated);
			for(int i=0;i<allOwnFieldUpdated.size();i++){
				String pkg = allOwnFieldUpdated.get(i).substring(0,allOwnFieldUpdated.get(i).lastIndexOf('.'));
				if(pkg.equals(dependent.substring(0,dependent.lastIndexOf('.')))){
					this.fieldValueChanged.add(allOwnFieldUpdated.get(i));
				}
			}
						
			for(int i = 0 ;i<allVirtualDependency.size();i++){
				this.syncAllPossibleDependency(allVirtualDependency.get(i), dependent);
			}
			
			for(int i = 0;i<allInterfaceDependency.size();i++){
				this.syncAllPossibleDependency(allInterfaceDependency.get(i), dependent);
			}
			
			for(int i = 0;i<allStaticDependency.size();i++){
				this.syncSingleDependency(allStaticDependency.get(i), dependent);
			}
			
			for(int i = 0;i<allFinalDependency.size();i++){
				this.syncSingleDependency(allFinalDependency.get(i), dependent);
			}
			
			for(int i= 0; i<allSpecialDependency.size();i++){
				this.syncSingleDependency(allSpecialDependency.get(i), dependent);
			}
	}
	
	protected void addDependentsInDb(String dependency, String dependents) throws UnknownDBIdException{
		
		if(this.dbId.length() == 0 || this.dbId == null){
			throw new UnknownDBIdException(dbId);
		}
		
		if(dependency.contains("java."))
			return;
		if(dependency.contains("junit."))
			return;
		if(dependency.contains("mockito."))
			return;
		if(dependency.contains("hamcrest."))
			return;		
		
		if(previousDependencies.containsKey(dependency)){
			// we increase the hashmap value which is used later to remove depreciated dependencies
			previousDependencies.put(dependency, previousDependencies.get(dependency) + 1);
			return;
		}
		else{
			this.rh.insertInSet(this.dbId, dependency, dependents);
			logger.debug(dependents+ " has been updated as "+dependency+" 's dependent");
		}
	}
	
	private long removeAllDepreciateDependency(){
		long count = 0;
		for ( Map.Entry<String, Integer> entry : previousDependencies.entrySet()) {
		    Integer val = entry.getValue();
		    if(val == 0){
		    	String key = entry.getKey();
		    	count += this.rh.removeFromSet(Databases.TABLE_ID_FORWARD_INDEX_DEPENDENCY, 
		    			changedMethod.getKey(), key);
		    	this.rh.removeFromSet(Databases.TABLE_ID_DEPENDENCY, key, changedMethod.getKey());
		    	logger.debug(changedMethod.getKey()+ " has been removed as "+key+" s dependency");
		    }  
		}
		return count;
	}
	
	protected List<String> traverseClassHierarchy(String claz, String pattern){
		
		List<String> toTest = new ArrayList<String>();

		Set<String> entity = this.rh.getAllKeys(Databases.TABLE_ID_ENTITY, claz+"."+pattern);
		
		for(String e: entity){
			toTest.add(e.substring(1));
		}		
		Set<String> allSubclass = this.rh.getSet(Databases.TABLE_ID_SUBCLASS, claz);
		
		for(String sub: allSubclass){
			List<String> t = traverseClassHierarchy(sub, pattern);
			
			if(!t.isEmpty() || t!= null)
				toTest.addAll(t);
		}

		return toTest;
	}
	
	protected void syncAllPossibleDependency(String dependency, String dependents){
		
		// JDK DEPENDENCY IGNORED
		if(dependency.contains("java/lang") || dependency.contains("java/util") || 
				dependency.contains("java/io")|| dependency.contains("java/net") || 
				dependency.contains("java/awt"))
			return;
		
		try{
			int index = dependency.indexOf("(");
			StringBuilder sb = new StringBuilder();
			sb.append(dependency.substring(index));
			for(int i = index - 1; i>=0; i--){
				if(dependency.charAt(i) == '.')
					break;
				sb.insert(0, dependency.charAt(i));
			}
			
			String pattern = sb.toString();
			String claz = dependency.substring(0, dependency.indexOf(pattern) >=0? 
					dependency.indexOf(pattern) - 1: dependency.length());
						
			List<String> keys = traverseClassHierarchy(claz, pattern);
			
			if(CollectionUtils.isEmpty(keys))
				return;
			for(String k: keys){
				addDependentsInDb(k, dependents); // because we have to remove table id
			}
		}
		
		catch(NullPointerException e){
			logger.error("Problem is syncing dependencies of changed entities"+e.getMessage());
		} 
		catch(StringIndexOutOfBoundsException e){
			e.printStackTrace();
		}
		catch (UnknownDBIdException e) {
			e.printStackTrace();
		}
	}
	
	
	protected void syncSingleDependency(String dependency, String dependents){

		try{
			addDependentsInDb(dependency, dependents);
		}
		catch(NullPointerException e){
			logger.error("Problem is syncing dependencies of changed entities"+e.getMessage());
		} catch (UnknownDBIdException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<String> getFieldValueChanged(){
		return fieldValueChanged;
	}
}
