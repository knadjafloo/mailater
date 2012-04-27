package com.threebars.mailater.model;

import android.text.format.Time;

public class DelayedEmail {

	private long id;
	private String receipients;
	private String sender;
	private String subject;
	private String body;
	private String attachments;
	private Time time;
	private String status;
	//---------------------------------------------
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Time getTime() {
		return time;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public void setTime(Time time) {
		this.time = time;
	}
	public String getReceipients() {
		return receipients;
	}
	public void setReceipients(String receipients) {
		this.receipients = receipients;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public String getAttachments() {
		return attachments;
	}
	public void setAttachments(String attachments) {
		this.attachments = attachments;
	}

	// Will be used by the ArrayAdapter in the ListView
	@Override
	public String toString() {
		return subject;
	}
}
