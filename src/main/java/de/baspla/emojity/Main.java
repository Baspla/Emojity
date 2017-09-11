package de.baspla.emojity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.*;
import java.util.Properties;

/**
 * Created by Mew on 22.05.2017.
 */
public class Main {

	private static Log LOG = LogFactory.getLog(Main.class.getName());

	public static void main(String[] args) {

		ApiContextInitializer.init();

		TelegramBotsApi botsApi = new TelegramBotsApi();

		Properties properties = new Properties();
		File file = new File("emojity.properties");

		// Einstellungen laden
		try {
			if (!file.exists()) {
				file.createNewFile();
				LOG.info("Einstellungs Datei neu erstellt");
			}
			BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));
			properties.load(stream);
			stream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Einstellungen laden oder zum ersten mal setzen
		String name = properties.getProperty("botname", "EmojityBot");
		String token = properties.getProperty("apitoken", "");
		properties.setProperty("botname", name);
		properties.setProperty("apitoken",token);
		// Einstellungen Speichern
		try {
			FileOutputStream fos = new FileOutputStream(file);
			properties.store(fos, "Einstellungen fuer Emojity");
			fos.flush();
			fos.close();
			LOG.info("Einstellungen geladen und gespeichert.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Bot registrieren
		try {
			Bot bot = new Bot(name, token);
			botsApi.registerBot(bot);
		} catch (TelegramApiException e) {
			LOG.error(e);
		}
	}
}
