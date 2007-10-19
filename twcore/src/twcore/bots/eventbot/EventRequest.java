package twcore.bots.eventbot;

import java.util.Date;

/**
 * POJO class for eventbot.
 * This Plain Old Java Object (POJO) stores a few properties of a event request.
 * 
 * @author Maverick
 */
public class EventRequest {
	private String requester;
	private String event;
	private String comments;
	private Date lastrequest;
	
	public EventRequest(String requester, String event) {
		this.requester = requester;
		this.event = event;
		this.comments = null;
		this.lastrequest = new Date();
	}
	
	public EventRequest(String requester, String event, String comments) {
		this.requester = requester;
		this.event = event;
		this.comments = comments;
		this.lastrequest = new Date();
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
	public String getRequester() {
		return requester;
	}

	/**
	 * @param requester the requester to set
	 */
	public void setRequester(String requester) {
		this.requester = requester;
	}

	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * @return the lastrequest
	 */
	public Date getLastrequest() {
		return lastrequest;
	}

	/**
	 * @param lastrequest the lastrequest to set
	 */
	public void setLastrequest(Date lastrequest) {
		this.lastrequest = lastrequest;
	}
	
	
}
