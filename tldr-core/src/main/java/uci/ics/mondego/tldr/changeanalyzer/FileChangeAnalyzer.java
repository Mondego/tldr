package uci.ics.mondego.tldr.changeanalyzer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import com.rfksystems.blake2b.Blake2b;
import com.rfksystems.blake2b.security.*;

import uci.ics.mondego.tldr.exception.DatabaseSyncException;
import uci.ics.mondego.tldr.tool.DatabaseIDs;

/**
 * Analyzes changes in file i.e. class level.
 * @author demigorgan
 *
 */
public class FileChangeAnalyzer extends ChangeAnalyzer{
	
	private final MessageDigest md;
	
	public FileChangeAnalyzer(String fileName) 
			throws IOException,
					NoSuchAlgorithmException, 
					DatabaseSyncException {
		super(fileName);
		Security.addProvider(new Blake2bProvider());
		md = MessageDigest.getInstance(Blake2b.BLAKE2_B_160);
		this.parse();
		this.closeRedis();
	}
	
	protected void parse() throws IOException, DatabaseSyncException {
		logger.debug("Change analysis : " + getEntityName());
		if( !exists(DatabaseIDs.TABLE_ID_FILE, getEntityName()) ){
			
			logger.debug("New entity  : " + getEntityName());
			String currentCheckSum = calculateChecksum();
			boolean ret = sync(DatabaseIDs.TABLE_ID_FILE, this.getEntityName(), currentCheckSum);
			
			// If it is not synced then throw an exception.
			if(!ret){
				throw new DatabaseSyncException(this.getEntityName());
			}
			setChanged(true);
		}
		
		else {
			String prevCheckSum = getValue(DatabaseIDs.TABLE_ID_FILE, this.getEntityName()); 
			String currentCheckSum = calculateChecksum();
			
			if (!prevCheckSum.equals(currentCheckSum)) {
				logger.debug("Changed entity  : " + getEntityName());

				boolean ret = this.sync(DatabaseIDs.TABLE_ID_FILE, this.getEntityName(), currentCheckSum);
				if (!ret) {
					throw new DatabaseSyncException(this.getEntityName());
				}
				setChanged(true);
			}
		}
	}
	
	/**
	 * Calculates the blake2b checksum of a given file.
	 * @return
	 * @throws IOException
	 */
	private String calculateChecksum() throws IOException {
		InputStream fis = new FileInputStream(getEntityName());
        byte[] buffer = new byte[1024];
        int nread;        
        while ((nread = fis.read(buffer)) != -1) {
            md.update(buffer, 0, nread);
        }
        
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest()) {
            result.append(String.format("%02x", b));
        }
        
        fis.close();
        return result.toString();
    }
}