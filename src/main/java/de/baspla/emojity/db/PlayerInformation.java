package de.baspla.emojity.db;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PlayerInformation {

	@Id
	private Long chatId;
	private int points;
	private String language;
	private boolean mute;
	private boolean admin;

	public PlayerInformation(Long chatId) {
		this.chatId = chatId;
		admin = false;
		mute = false;
		language = "DEUTSCH";
		points = 0;
	}

	public PlayerInformation() {
		super();
	}

	public Long getChatId() {
		return chatId;
	}

	public void setChatId(Long chatId) {
		this.chatId = chatId;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isMute() {
		return mute;
	}

	public void setMute(boolean mute) {
		this.mute = mute;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

}
