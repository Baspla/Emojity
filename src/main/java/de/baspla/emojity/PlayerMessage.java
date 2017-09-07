package de.baspla.emojity;

public class PlayerMessage {

	private Long chatId;
	private String text;
	private boolean issent;

	public PlayerMessage(Long chatId, String text) {
		this.chatId = chatId;
		this.setText(text);
		this.issent = false;
	}

	public Long getChatId() {
		return chatId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isIssent() {
		return issent;
	}

	public void send() {
		this.issent = true;
	}

}
