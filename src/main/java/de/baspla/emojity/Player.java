package de.baspla.emojity;

public class Player {

	private long chatId;
	private String username;
	private int points;
	private int rsm;

	public Player(Long chatId, String username) {
		this.setChatId(chatId);
		this.setUsername(username);
		rsm = 0;
	}

	public int getPoints() {
		return points;
	}

	public int roundsSinceMaster() {
		return rsm;
	}

	public void setRoundsSinceMaster(int i) {
		rsm = i;
	}

	public void addRoundsSinceMaster() {
		rsm++;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public long getChatId() {
		return chatId;
	}

	public void setChatId(long chatId) {
		this.chatId = chatId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
