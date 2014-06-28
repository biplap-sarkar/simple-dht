package edu.buffalo.cse.cse486586.simpledht;

/**
 * This class represents the information available to one node.
 * @author biplap
 *
 */
public class Entry {
	private String peerId;	// Id of the node
	private String peerAddress;	// Address of the node
	private String successor;	// Successor of the node
	private String successorAddress;	// Address of the successor
	private String predecessor;	// Predecessor of the node
	private String predecessorAddress;	// Address of the predecessor
	
	/**
	 * @return successor of the node
	 */
	public String getSuccessor() {
		return successor;
	}
	
	/**
	 * Sets the successor of the node
	 * @param successor
	 */
	public void setSuccessor(String successor) {
		this.successor = successor;
	}
	
	/**
	 * @return predecessor of the node
	 */
	public String getPredecessor() {
		return predecessor;
	}
	
	/**
	 * Sets the predecessor of the node
	 * @param predecessor
	 */
	public void setPredecessor(String predecessor) {
		this.predecessor = predecessor;
	}
	
	/**
	 * @return the id of the node
	 */
	public String getPeerId() {
		return peerId;
	}
	
	/**
	 * Sets the id of the node
	 * @param peerId
	 */
	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}
	
	/**
	 * @return the address of the node
	 */
	public String getPeerAddress() {
		return peerAddress;
	}
	
	/**
	 * Sets the address of the node
	 * @param peerAddress
	 */
	public void setPeerAddress(String peerAddress) {
		this.peerAddress = peerAddress;
	}
	
	/**
	 * @return address of the successor
	 */
	public String getSuccessorAddress() {
		return successorAddress;
	}
	
	/**
	 * Sets the address of the successor
	 * @param successorAddress
	 */
	public void setSuccessorAddress(String successorAddress) {
		this.successorAddress = successorAddress;
	}
	
	/**
	 * @return address of the predecessor
	 */
	public String getPredecessorAddress() {
		return predecessorAddress;
	}
	
	/**
	 * Sets the address of the predecessor
	 * @param predecessorAddress
	 */
	public void setPredecessorAddress(String predecessorAddress) {
		this.predecessorAddress = predecessorAddress;
	}

}
