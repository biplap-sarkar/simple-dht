package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;


import android.database.Cursor;
import android.database.MatrixCursor;

import android.util.Log;

/**
 * This is a singleton class which provides the abstraction for DHT level details
 * and provides DHT functionalities.
 * 
 * @author biplap
 *
 */
public class DHTOperation {

	static final String TAG = DHTOperation.class.getSimpleName();
	private String myId;
	private String myAddress;
	private String predecessor;
	private String predecessorAddress;
	private String successor;
	private String successorAddress;
	private final String BASE_PORT = "5554";
	
	private static DHTOperation dhtOperationInstance = null;
	
	/**
	 * Constructor which initializes the DHT node by performing node join.
	 * Initially it tries to send node join request to node 5554.
	 * If it does not get any response, it assumes that it is the only
	 * node in the DHT.
	 * 
	 * @param myPortStr Port number where avd for this instance is running
	 */
	private DHTOperation(String myPortStr){
		try {
			this.myId = genHash(myPortStr);
			Log.v(TAG, "Initializing DHT");
			this.myAddress = String.valueOf(Integer.parseInt(myPortStr)*2);
			String baseAddress = String.valueOf(Integer.parseInt(BASE_PORT)*2);
			if(myAddress.equals(baseAddress)){		// This is the base node 5554
				Log.v(TAG, "I am the base node");
			}
			else{
				Entry succEntry = findSuccessor(myId, baseAddress);	// This is not the base node
																	// Find the successor of this node
																	// in the DHT
				
				if(succEntry!=null){	// successor found, send requests to it and it's predecessor to update their DHT entries
					Log.v(TAG, "Successor "+succEntry.getPeerAddress()+" found");
					if(succEntry.getSuccessor()==null && succEntry.getPredecessor()==null){
						Entry toSinglePeer = new Entry();
	    				toSinglePeer.setSuccessor(myId);
	    				toSinglePeer.setSuccessorAddress(myAddress);
	    				toSinglePeer.setPredecessor(myId);
	    				toSinglePeer.setPredecessorAddress(myAddress);
	    				updateRemotePeerEntry(succEntry.getPeerAddress(), toSinglePeer);
	    				successor = succEntry.getPeerId();
	    				successorAddress = succEntry.getPeerAddress();
	    				predecessor = succEntry.getPeerId();
	    				predecessorAddress = succEntry.getPeerAddress();
					}
					else{
						Entry toSucc = new Entry();
	    				toSucc.setPredecessor(myId);
	    				toSucc.setPredecessorAddress(myAddress);
	    				updateRemotePeerEntry(succEntry.getPeerAddress(), toSucc);
	    				Entry toPred = new Entry();
	    				toPred.setSuccessor(myId);
	    				toPred.setSuccessorAddress(myAddress);
	    				updateRemotePeerEntry(succEntry.getPredecessorAddress(), toPred);
	    				
	    				predecessor = succEntry.getPredecessor();
	    				predecessorAddress = succEntry.getPredecessorAddress();
	    				successor = succEntry.getPeerId();
	    				successorAddress = succEntry.getPeerAddress();
					}
				}
				else{		// Successor not found, this is the only node in DHT
					Log.v(TAG, "No successor found, I am the only node here");
				}
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates and returns the object for this singleton class to handle DHT level details.
	 * @param myAddress
	 * @return
	 */
	public static DHTOperation createAndGetInstance(String myAddress){
		dhtOperationInstance = new DHTOperation(myAddress);
		return dhtOperationInstance;
	}
	
	/**
	 * Returns the single instance of this class
	 * @return
	 */
	public static DHTOperation getInstance(){
		return dhtOperationInstance;
	}
	
	/**
	 * Sends request to a remote peer to update it's peer entries
	 * @param remotePeerAddress
	 * @param newEntry
	 */
	private void updateRemotePeerEntry(String remotePeerAddress, Entry newEntry){
		try {
			int addr = Integer.parseInt(remotePeerAddress);
			Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					addr);
			Log.v(TAG, "Connected with "+addr);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Message msg = new Message();
			msg.setType(Message.UPDATE_ENTRY);
			msg.setEntry(newEntry);
			String msgStr = msg.toJson();
			bw.write(msgStr+"\n");
			bw.flush();
			br.readLine();
			Log.v(TAG, "Peer updated");
			socket.close();
			
			
		} catch (UnknownHostException e) {
			Log.e(TAG, "ClientTask UnknownHostException");
		} catch (IOException e) {
			Log.e(TAG, "ClientTask socket IOException");
		}
	}
	
	/**
	 * Sends delete request to a remote peer
	 * @param address of the remote peer
	 * @param key of the entry to be deleted
	 * @return number of rows deleted
	 */
	public int deleteRequest(String address, String key){
		int res = 0;
		try {
			int addr = Integer.valueOf(address);
			Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					addr);
			Log.v(TAG, "Connected with "+addr);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Message msg = new Message();
			msg.setType(Message.DELETE);
			msg.setKey(key);
			String msgStr = msg.toJson();
			bw.write(msgStr+"\n");
			bw.flush();
			String responseStr = br.readLine();
			Message response = Message.fromJson(responseStr);
			Log.v(TAG, "Record deleted");
			socket.close();
			res = response.getSqlResult();
			
		} catch (UnknownHostException e) {
			Log.e(TAG, "ClientTask UnknownHostException");
		} catch (IOException e) {
			Log.e(TAG, "ClientTask socket IOException");
		}
		return res;
	}
	
	/**
	 * Fetches all the key values pairs from the DHT
	 * @return Cursor containing all the key value pairs in the DHT
	 */
	public Cursor fetchAll(){
		Log.v(TAG, "fetch all");
		MatrixCursor matCursor = new MatrixCursor(new String[]{DBHelper.KEY_FIELD,DBHelper.VALUE_FIELD});
		matCursor.moveToFirst();
		
		Entry entry = findSuccessor(myId, myAddress);	// Find the successor of this node
		Log.v(TAG, "Entry returned for "+entry.getPeerAddress());
		if(entry.getPeerAddress().equals(myAddress)){	// There is no successor, so just return all the entries from this node
			Log.v(TAG, "no successor");
			return searchRequest(myAddress, "@");
		}
		else{											// Successor exists
			Cursor myCursor = searchRequest(myAddress,"@");		// Fetch all the entries from this node
			if(myCursor.moveToFirst()){
				do{
					String key = myCursor.getString(0);
					String val = myCursor.getString(1);
					matCursor.addRow(new String[]{key,val});
				}while(myCursor.moveToNext());
			}
		}
		while(entry.getPeerId().equals(myId)==false){			// Continue to move from one peer to it's successor till records are fetched from all the nodes.
			Log.v(TAG, "sending search request @ to "+entry.getPeerAddress());
			Cursor peerCursor = searchRequest(entry.getPeerAddress(),"@");
			if(peerCursor.moveToFirst()){
				do{
					String key = peerCursor.getString(0);
					String val = peerCursor.getString(1);
					matCursor.addRow(new String[]{key,val});
				}while(peerCursor.moveToNext());
			}
			entry = findSuccessor(entry.getPeerId(),entry.getPeerAddress());
		}
		return matCursor;
	}
	
	
	/**
	 * Deletes all the key value pairs in the DHT
	 * @return number of rows deleted
	 */
	public int deleteAll(){
		int res = 0;
		Entry entry = findSuccessor(myId, myAddress);		// Find the successor of this node
		if(entry.getPeerAddress().equals(myAddress)){		// There is no successor, so just delete all the rows from this node
			return deleteRequest(myAddress, "@");
		}
		else{												// Successor exists
			res = res + deleteRequest(myAddress, "@");		// Delete the rows from this node
		}
		while(entry.getPeerId().equals(myId)==false){		// Loop through all the nodes going from node to it's predecessor, deleting all the rows from each node
			int peerRes = deleteRequest(entry.getPeerAddress(),"@");
			res = res + peerRes;
			entry = findSuccessor(entry.getPeerId(),entry.getPeerAddress());
		}
		return res;
	}
	
	/**
	 * Searches a key value pair in a remote node
	 * @param address: address of the remote node
	 * @param key: key to be searched
	 * @return Cursor containing the search result
	 */
	public Cursor searchRequest(String address, String key){
		MatrixCursor matCursor = null;
		try {
			Log.v(TAG, "Initiating search request for key "+key+" to node "+address);
			int addr = Integer.valueOf(address);
			Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					addr);
			Log.v(TAG, "Connected with "+addr);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Message msg = new Message();
			msg.setType(Message.GET);
			msg.setKey(key);
			String msgStr = msg.toJson();
			bw.write(msgStr+"\n");
			bw.flush();
			String responseStr = br.readLine();
			Message response = Message.fromJson(responseStr);
			ArrayList<KeyVal> keyValList = response.getKeyValList();
			Log.v(TAG, keyValList.size()+" rows fetched");
			matCursor = new MatrixCursor(new String[]{DBHelper.KEY_FIELD,DBHelper.VALUE_FIELD});
			for(int i=0;i<keyValList.size();i++){
				matCursor.moveToFirst();
				matCursor.addRow(new String[]{keyValList.get(i).getKey(),keyValList.get(i).getVal()});
			}
			
			Log.v(TAG, "Record fetched");
			socket.close();
			
		} catch (UnknownHostException e) {
			Log.e(TAG, "ClientTask UnknownHostException");
		} catch (IOException e) {
			Log.e(TAG, "ClientTask socket IOException");
		}
		return matCursor;
	}
	
	/**
	 * Inserts a key value pair in a remote node
	 * @param address: address of the remote node
	 * @param key: key to be inserted
	 * @param value: value to be inserted
	 */
	public void insertRequest(String address, String key, String value){
		try {
			String port = address;
			int addr = Integer.valueOf(port);
			Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					addr);
			Log.v(TAG, "Connected with "+addr);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			Message msg = new Message();
			msg.setType(Message.PUT);
			msg.setKey(key);
			msg.setValue(value);
			String msgStr = msg.toJson();
			bw.write(msgStr+"\n");
			bw.flush();
			Log.v(TAG, "Record inserted");
			socket.close();
			
		} catch (UnknownHostException e) {
			Log.e(TAG, "ClientTask UnknownHostException");
		} catch (IOException e) {
			Log.e(TAG, "ClientTask socket IOException");
		}
	}
	
	/**
	 * Generates sha1 hash of a string
	 * @param input
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        String hash = formatter.toString();
        formatter.close();
        return hash;
    }
	
	/**
	 * Finds successor node of a dht key.
	 * If the dht key belongs to a node, it returns the details of successor of the node 
	 * If the dht key belongs to a resource, it returns the node responsible for that key
	 * @param dhtKey
	 * @param baseNode
	 * @return
	 */
	public Entry findSuccessor(String dhtKey, String baseNode){
		try {
			Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
					Integer.parseInt(baseNode));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			Message msg = new Message();
			msg.setType(Message.LOOKUP_REQUEST);
			String msgStr = msg.toJson();
			bw.write(msgStr+"\n");
			bw.flush();
			String responseStr = br.readLine();
			Message response = Message.fromJson(responseStr);
			Entry nodeEntry = response.getEntry();
			socket.close();
			
			if(nodeEntry.getSuccessor()==null && nodeEntry.getPredecessor()==null){	//This is the only node in the ring, so this is 
																					//successor of everything
				return nodeEntry;
			}
			if(dhtKey.equals(nodeEntry.getPeerId())){	// dhtKey belongs to a node, so have to return the successor of that node
				
				socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
						Integer.parseInt(nodeEntry.getSuccessorAddress()));
				bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
				br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				msg = new Message();
				msg.setType(Message.LOOKUP_REQUEST);
				msgStr = msg.toJson();
				bw.write(msgStr+"\n");
				bw.flush();
				responseStr = br.readLine();
				response = Message.fromJson(responseStr);
				Entry succEntry = response.getEntry();
				socket.close();
				return succEntry;
			}
			
			else {		// dhtKey belongs to a resource
				if(isFallingBetween(dhtKey, nodeEntry.getPredecessor(), nodeEntry.getPeerId())==true){	// is this node successor of dhtKey ?
					return nodeEntry;
				}
				Log.v(TAG, "recursive");
				return findSuccessor(dhtKey, nodeEntry.getSuccessorAddress());	// search the successor of dhtKey in the successor peer of this node
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}
	
	/**
	 * Determines if a dht key falls between given predecessor and successor or not
	 * @param target: dht key to be compared
	 * @param pred: predecessor to be tested
	 * @param succ: successor to be tested
	 * @return true if dht key falls between the given predecessor and successor, false otherwise
	 */
	private boolean isFallingBetween(String target, String pred, String succ){
		Log.v(TAG, "Target- "+target+", Pred:- "+pred+", Succ:- "+succ);
		if(succ.compareTo(pred)>0){
			if(target.compareTo(pred)>0 && succ.compareTo(target)>0)
				return true;
			else
				return false;
		}
		else {
			if(target.compareTo(succ)>0 && pred.compareTo(target)>0)
				return false;
			else
				return true;
		}
	}
	
	/**
	 * Returns the predecessor of this node
	 * @return
	 */
	public String getPredecessor() {
		return predecessor;
	}
	
	/**
	 * Sets the predecessor of this node
	 * @param predecessor
	 */
	public void setPredecessor(String predecessor) {
		this.predecessor = predecessor;
	}

	/**
	 * Returns the address of predecessor for this node
	 * @return
	 */
	public String getPredecessorAddress() {
		return predecessorAddress;
	}

	/**
	 * Sets the address of the predecessor for this node
	 * @param predecessorAddress
	 */
	public void setPredecessorAddress(String predecessorAddress) {
		this.predecessorAddress = predecessorAddress;
	}

	/**
	 * Returns the successor of this node
	 * @return
	 */
	public String getSuccessor() {
		return successor;
	}

	/**
	 * Sets the successor of this node
	 * @param successor
	 */
	public void setSuccessor(String successor) {
		this.successor = successor;
	}

	/**
	 * Returns the address of the successor for this node
	 * @return
	 */
	public String getSuccessorAddress() {
		return successorAddress;
	}

	/**
	 * Sets the address of the successor for this node
	 * @param successorAddress
	 */
	public void setSuccessorAddress(String successorAddress) {
		this.successorAddress = successorAddress;
	}

	/**
	 * Returns the id of this node
	 * @return
	 */
	public String getMyId() {
		return myId;
	}

	/**
	 * Returns the address of this node
	 * @return
	 */
	public String getMyAddress() {
		return myAddress;
	}

}
