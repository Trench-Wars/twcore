package twcore.bots.eventbot;

import java.util.Date;
import java.util.HashSet;

/**
 * POJO class for eventbot.
 * This Plain Old Java Object (POJO) stores a few properties of a event request.
 * 
 * @author Maverick
 */
public class EventRequest {
	private HashSet<String> requesters = new HashSet<String>();
	private String event;
	private HashSet<String> comments = new HashSet<String>();
	private Date lastrequest;
	
	public EventRequest(String requester, String event) {
		this.requesters.add(requester);
		this.event = event;
		this.comments = null;
		this.lastrequest = new Date();
	}
	
	public EventRequest(String requester, String event, String comments) {
		this.requesters.add(requester);
		this.event = event;
		this.comments.add(comments);
		this.lastrequest = new Date();
	}

	/**
	 * @return the comments
	 */
	public HashSet<String> getComments() {
		return comments;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(HashSet<String> comments) {
		this.comments = comments;
	}
	
	/**
	 * @param comments the comments to add
	 */
	public void addComments(String comments){
		this.comments.add(comments);
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return lastrequest;
	}

	/**
	 * @param date the date to set
	 */
	public void setDate(Date date) {
		this.lastrequest = date;
	}

	/**
	 * @return the event
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * @param event the event to set
	 */
	public void setEvent(String event) {
		this.event = event;
	}

	/**
	 * @return the requester
	 */
	public HashSet<String> getRequesters() {
		return requesters;
	}

	/**
	 * @param requester the requester to set
	 */
	public void setRequesters(HashSet<String> requesters) {
		this.requesters = requesters;
	}
	
	public void addRequester(String requester) {
		this.requesters.add(requester);
	}
	
	public int numberRequesters() {
		return this.requesters.size();
	}
	
	public int numberComments() {
		return this.comments.size();
	}
}
