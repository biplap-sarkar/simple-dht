package edu.buffalo.cse.cse486586.simpledht;

/**
 * Represents a key value pair of record.
 * A list of objects of this class is used to return search results
 * containing records with many key value pairs.
 * 
 * @author biplap
 *
 */
public class KeyVal {

	private String key;
	private String val;
	
	/**
	 * Returns the key
	 * @return
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Sets the key
	 * @param key
	 */
	public void setKey(String key) {
		this.key = key;
	}
	
	/**
	 * Returns the value
	 * @return
	 */
	public String getVal() {
		return val;
	}
	
	/**
	 * Sets the value
	 * @param val
	 */
	public void setVal(String val) {
		this.val = val;
	}
	

}
