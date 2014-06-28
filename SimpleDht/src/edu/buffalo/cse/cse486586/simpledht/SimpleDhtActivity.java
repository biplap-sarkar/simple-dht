package edu.buffalo.cse.cse486586.simpledht;

/******************************************************************************************/
/********************** General Overview of the Implementation ****************************/
/**																						 **/
/** This project implements a simple Chord DHT and provides search, searchAll, insert,   **/
/** delete and deleteAll operations in the DHT. The object of DHTOperation takes care of **/
/** all the DHT level details, i.e. determining the successor of a given dht key and     **/
/** sending TCP requests to carry out the DHT operation to the node responsible to handle**/
/** that key. A TCP ServerTask thread runs in each of the nodes listening to the requests**/
/** from others (and itself) and carries out the operations by inserting, searching,     **/
/** deleting in the database.                                                            **/
/** 																				     **/
/** For node join, a node expects the node 5554 to be already present and sends the join **/
/** request to it. In case it finds that node 5554 is absent, it assumes that DHT ring   **/
/** was not formed and hence assumes that it is the only node in the DHT now             **/
/**																					     **/
/** The messages are sent between the objects by serializing and deserializing the 		 **/
/** objects of Message class in it's JSON format.										 **/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {

	static final String TAG = SimpleDhtActivity.class.getSimpleName();
	private DBHelper dbHelper;
	private DHTOperation dhtOperation;
	private static String myPort;
	
	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";
	
	static final String BASE_PORT = "5554";
	private final int SERVER_PORT = 10000;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);
        
        // Initialize the dbHelper
        dbHelper = new DBHelper(getApplicationContext());
        
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		myPort = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		
		// Initialize the DHT entries in this node
		initPeerEntries();
		Log.v(TAG, "Node "+dhtOperation.getMyAddress()+" Initialized with successor "+dhtOperation.getSuccessorAddress()+" and predecessor "+dhtOperation.getPredecessorAddress());
        Log.v(TAG, "Id "+dhtOperation.getMyId()+" Initialized with successor "+dhtOperation.getSuccessor()+" and predecessor "+dhtOperation.getPredecessor());
		TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));
        
        try {
			/*
			 * Create a server socket as well as a thread (AsyncTask) that listens on the server
			 * port.
			 * 
			 * AsyncTask is a simplified thread construct that Android provides. Please make sure
			 * you know how it works by reading
			 * http://developer.android.com/reference/android/os/AsyncTask.html
			 */
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			/*
			 * Log is a good way to debug your code. LogCat prints out all the messages that
			 * Log class writes.
			 * 
			 * Please read http://developer.android.com/tools/debugging/debugging-projects.html
			 * and http://developer.android.com/tools/debugging/debugging-log.html
			 * for more information on debugging.
			 */
			Log.e(TAG, "Can't create a ServerSocket");
			return;
		}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }
    

    /**
     * Initializes the DHT entries for this node
     */
    public void initPeerEntries(){
    	try {
			dhtOperation = new InitDHTTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, myPort).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    /**
     * This class implements a server task to listen to incoming TCP requests.
     * This class listens to insert, search and delete operations from clients
     * and carry out those operations in DB.
     * @author biplap
     *
     */
	private class ServerTask extends AsyncTask<Object, String, Void> {
		
		
		
		@Override
		protected Void doInBackground(Object... params) {
			ServerSocket serverSocket = (ServerSocket) params[0];

			while(true){		// Keep listening to incoming requests
				try {
					Socket soc = serverSocket.accept();
					Log.v(TAG, "Client connected");
					BufferedReader br = new BufferedReader(new InputStreamReader(soc.getInputStream()));
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(soc.getOutputStream()));
					String rawMsg = br.readLine();
					Message inMsg = Message.fromJson(rawMsg);
					Log.v(TAG, "Message type "+inMsg.getType());
					
					// Handling lookup request
					if(inMsg.getType() == Message.LOOKUP_REQUEST){
						
						Entry entry = new Entry();
						entry.setPeerId(dhtOperation.getMyId());
						entry.setPeerAddress(dhtOperation.getMyAddress());
						entry.setPredecessor(dhtOperation.getPredecessor());
						entry.setPredecessorAddress(dhtOperation.getPredecessorAddress());
						entry.setSuccessor(dhtOperation.getSuccessor());
						entry.setSuccessorAddress(dhtOperation.getSuccessorAddress());
						
						
						Message reply = new Message();
						reply.setEntry(entry);
						reply.setType(Message.LOOKUP_RESPONSE);
						String replyStr = reply.toJson();
						bw.write(replyStr+"\n");
						bw.flush();
					}
					
					
					// handling request to update predecessor and successor
					else if(inMsg.getType() == Message.UPDATE_ENTRY){
						Entry entry = inMsg.getEntry();
						if(entry.getSuccessor()!=null){
							dhtOperation.setSuccessor(entry.getSuccessor());
							dhtOperation.setSuccessorAddress(entry.getSuccessorAddress());
							Log.v(TAG, "Node "+dhtOperation.getMyAddress()+" updated successor to "+dhtOperation.getSuccessorAddress());
							Log.v(TAG, "Node "+dhtOperation.getMyId()+" updated successor to "+dhtOperation.getSuccessor());
						}
						if(entry.getPredecessor()!=null){
							dhtOperation.setPredecessor(entry.getPredecessor());
							dhtOperation.setPredecessorAddress(entry.getPredecessorAddress());
							Log.v(TAG, "Node "+dhtOperation.getMyAddress()+" updated predecessor to "+dhtOperation.getPredecessorAddress());
							Log.v(TAG, "Node "+dhtOperation.getMyId()+" updated predecessor to "+dhtOperation.getPredecessor());
						}
						Log.v(TAG, "Entry updated in node "+dhtOperation.getMyId());
					}
					
					// handling request to insert a new key value
					else if(inMsg.getType() == Message.PUT){
						Log.v(TAG, "Insert request received at "+myPort);
						ContentValues values = new ContentValues();
						values.put(KEY_FIELD, inMsg.getKey());
						values.put(VALUE_FIELD, inMsg.getValue());
						dbHelper.insert(values);
					}
					
					// handling request to search key value
					else if(inMsg.getType() == Message.GET){
						Log.v(TAG, "Received query request for key "+inMsg.getKey());
						String querySelection = DBHelper.KEY_FIELD+"=?";
						String selection = inMsg.getKey();
				    	String []querySelectionArgs = new String[]{selection};
				    	if(selection.equals("@")){
				    		querySelection = null;
				    		querySelectionArgs = null;
				    	}
				    	Cursor res = dbHelper.query(null, querySelection, querySelectionArgs, null);
				        Log.v("query", selection);
				        ArrayList<KeyVal> keyValList = new ArrayList<KeyVal>();
				        if (res.moveToFirst()){
				        	do{
				        		KeyVal newKeyVal = new KeyVal();
				        		newKeyVal.setKey(res.getString(0));
				        		newKeyVal.setVal(res.getString(1));
				        		keyValList.add(newKeyVal);
				        	}while(res.moveToNext());
				        }
				        res.close();
				        Message response = new Message();
				        response.setKeyValList(keyValList);
				        String responseStr = response.toJson();
				        bw.write(responseStr+"\n");
				        bw.flush();
					}
					
					// handling request to delete key value
					else if(inMsg.getType() == Message.DELETE){
						String whereClause = DBHelper.KEY_FIELD+"=?";
						String selection = inMsg.getKey();
				    	String []whereArgs = new String[]{selection};
				    	if(selection.equals("@")){
				    		whereClause = "1";
				    		whereArgs = null;
				    	}
				    	int res = dbHelper.delete(whereClause, whereArgs);
				    	Message response = new Message();
				    	response.setSqlResult(res);
				    	String responseStr = response.toJson();
				    	bw.write(responseStr+"\n");
				    	bw.flush();
					}
					
					soc.close();
				} catch (IOException e) {
					Log.e(TAG, "ServerTask socket IOException");
				}
			}
		}

		protected void onProgressUpdate(String...strings) {
			/*
			 * The following code displays what is received in doInBackground().
			 */
			TextView tv = (TextView) findViewById(R.id.textView1);
			tv.setMovementMethod(new ScrollingMovementMethod());
			tv.append(strings[0]+"\n");
			return;
		}
	}
	
	/**
	 * Creates the single instance of DHTOperation object which provides the DHT
	 * level abstraction for the operations.
	 * 
	 * @author biplap
	 *
	 */
	private class InitDHTTask extends AsyncTask<String, Void, DHTOperation> {
		@Override
		protected DHTOperation doInBackground(String... arg) {
			String myAddress = arg[0];
			return DHTOperation.createAndGetInstance(myAddress);
		}
	}
}
