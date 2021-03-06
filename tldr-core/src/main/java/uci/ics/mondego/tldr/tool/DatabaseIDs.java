package uci.ics.mondego.tldr.tool;

/**
 * ID of Redis tables.
 * @author demigorgan
 *
 */
public interface DatabaseIDs {
	
	public static final String TABLE_ID_PROJECT = "0";
	
	public static final String TABLE_ID_FILE = "1";
	
	public static final String TABLE_ID_ENTITY = "2";
	
	public static final String TABLE_ID_DEPENDENCY = "3";
	
	public static final String TABLE_ID_TEST_DEPENDENCY = "4";
	
	public static final String TABLE_ID_SUBCLASS = "5";
	
	public static final String TABLE_ID_INTERFACE_SUPERCLASS = "6";
	
	public static final String TABLE_ID_FORWARD_INDEX_DEPENDENCY = "7";
	
	public static final String TABLE_ID_FORWARD_INDEX_TEST_DEPENDENCY = "8";
	
	public static final String TABLE_ID_TEST_ENTITY = "9";
}
