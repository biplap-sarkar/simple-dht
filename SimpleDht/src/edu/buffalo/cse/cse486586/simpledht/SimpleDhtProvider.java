package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


/**
 * Implementation of the content provider for this project.
 * Implements insert, search and delete operations.
 * It does not interact with database directly, but uses object
 * of DHTOperation to perform these operations within the DHT
 * 
 * @author biplap
 *
 */
public class SimpleDhtProvider extends ContentProvider {
	static final String TAG = SimpleDhtProvider.class.getSimpleName();
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";
	DHTOperation dhtOperation;
	
	
    @Override
    /**
     * Provides the delete operation.
     * Delegates to object of DHTOperation to perform delete operation within DHT
     * 
     * returns the number of rows deleted
     */
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    	int result = 0;
    	String key = selection;
    	if(key.equals("*"))
    		return dhtOperation.deleteAll();
    	else if(key.equals("@"))
    		return dhtOperation.deleteRequest(dhtOperation.getMyAddress(), key);
    	try {
    		String dhtKey = dhtOperation.genHash(key);
    		Entry succEntry = dhtOperation.findSuccessor(dhtKey, dhtOperation.getMyAddress());
    		dhtOperation.deleteRequest(succEntry.getPeerAddress(), key);	        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.v(TAG, e.getLocalizedMessage());
		} 
    	return result;
    }

    /**
     * auto generated stub
     */
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    /**
     * Implements the insert request.
     * Delegates to object of DHTOperation to perform this operation within DHT
     */
    public Uri insert(Uri uri, ContentValues values) {
    	if(dhtOperation == null)
    		dhtOperation = DHTOperation.getInstance();
    	String key = values.getAsString(KEY_FIELD);
    	String val = values.getAsString(VALUE_FIELD);
    	try {
    		String dhtKey = dhtOperation.genHash(key);
    		Log.v(TAG, "Inserting key "+key+" with sha1 "+dhtKey+" from node"+dhtOperation.getMyAddress());
    		Entry succEntry = dhtOperation.findSuccessor(dhtKey, dhtOperation.getMyAddress());
    		Log.v(TAG, "Inserting key "+key+" at "+succEntry.getPeerAddress());
    		dhtOperation.insertRequest(succEntry.getPeerAddress(), key, val);
	        Log.v("insert", values.toString());
		} catch (Exception e) {
			Log.v(TAG, e.getLocalizedMessage());
		} 
        return uri;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    /**
     * Performs the query operation.
     * Delegates to object of DHTOperation to perform this operation within DHT
     * 
     * returns a Cursor of the result
     */
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
    	String key = selection;
    	if(key.equals("*"))
    		return dhtOperation.fetchAll();
    	else if(key.equals("@"))
    		return dhtOperation.searchRequest(dhtOperation.getMyAddress(), key);
    	Cursor response = null;
    	try {
    		String dhtKey = dhtOperation.genHash(key);
    		Log.v(TAG, "Finding successor of "+dhtKey);
    		Entry succEntry = dhtOperation.findSuccessor(dhtKey, dhtOperation.getMyAddress());
			response = dhtOperation.searchRequest(succEntry.getPeerAddress(), key);
	        
		} catch (Exception e) {
			e.printStackTrace();
		} 
    	return response;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
   
}
