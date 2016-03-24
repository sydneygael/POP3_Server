package popmail;

import popserver.Communication;

public class Mail {
	private String receiver;
	private String sender;
	private String content;
	private String subject;
	private int nbByte;
	private int id;
	private boolean toDelete;
	
	public Mail() {}
	
	public Mail(int id, String sender, String receiver, String subject, String content) {
		this.setId(id);
		this.setReceiver(receiver);
		this.setSender(sender);
		this.setSubject(subject);
		this.setContent(content);
		this.setNbByte(content.getBytes().length);
	}
	
	public String generateMail() {
		String endLine = Communication.END_OF_LINE;
		return this.getSender() + endLine + 
				this.getReceiver() + endLine + 
				this.getSubject() + endLine + 
				endLine +
				this.getContent() + endLine +
				"." + endLine;
	}
	
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public int getNbByte() {
		return nbByte;
	}
	public void setNbByte(int nbByte) {
		this.nbByte = nbByte;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public boolean getToDelete() {
		return this.toDelete;
	}
	public void setToDelete(boolean b) {
		this.toDelete = b;
	}
	
	public String toString() {
		return this.getId() + ";" + this.getSender() + ";" + this.getReceiver() + ";" + this.getSubject() + ";" + this.getContent();
	}
}
