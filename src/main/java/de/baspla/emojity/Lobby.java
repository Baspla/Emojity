package de.baspla.emojity;

import de.baspla.emojity.db.PlayerInformation;
import org.telegram.telegrambots.api.objects.Message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.StringTokenizer;

public class Lobby {

    private int lobbyid;
    private int maxPlayercount = 8;
    private ArrayList<Player> playerlist;
    private Bot bot;
    private Status status;
    private Player master;
    private ArrayList<PlayerMessage> mastermessages;
    private ArrayList<PlayerMessage> playermessages;
    protected String word;
    private String[] words;

    public Lobby(Bot bot, Long chatId, int lobbyid, String username) {
        this.bot = bot;
        this.lobbyid = lobbyid;
        playerlist = new ArrayList<Player>();
        mastermessages = new ArrayList<PlayerMessage>();
        playermessages = new ArrayList<PlayerMessage>();
        status = Status.IDLE;
        loadwords("words.csv");
        join(chatId, username);
    }

    private void loadwords(String string) {
        try {
            File f = new File(string);
            f.createNewFile();
            StringTokenizer token = new StringTokenizer(new String(Files.readAllBytes(f.toPath())), ",");
            words = new String[token.countTokens()];
            for (int i = 0; i < words.length; i++) {
                words[i] = token.nextToken();
            }
        } catch (IOException e) {
            words = new String[]{"FEHLER"};
        }
    }

    public void textmessage(Message message) {
        if (isIngame() && master != null) {
            if (master.getChatId() == message.getChatId()) {
                if (Bot.hasOnlyEmoji(message.getText())) {
                    mastermessages.add(new PlayerMessage(message.getChatId(), message.getText()));
                } else {
                    bot.send(message.getChatId(), "Benutze nur Emojis.");
                }
            } else {
                playermessages.add(new PlayerMessage(message.getChatId(), message.getText()));
            }
        } else if (isIdle() || isPreparing()) {
            sendToAll(getPlayer(message.getChatId()).getUsername() + "<b>:</b> " + message.getText(),
                    message.getChatId());
        }
    }

    public int getPlayercount() {
        return playerlist.size();
    }

    public int getLobbyid() {
        return lobbyid;
    }

    public ArrayList<Player> getPlayerlist() {
        return playerlist;
    }

    public int getMaxPlayercount() {
        return maxPlayercount;
    }

    private int getMinPlayerCount() {
        return bot.testmode?2:3;
    }

    public void setMaxPlayercount(int maxPlayercount) {
        this.maxPlayercount = maxPlayercount;
    }

    public void join(Long chatId, String username) {
        if (isFull()) {
            bot.send(chatId, "Diese Lobby ist leider schon voll.");
            return;
        }
        if (isIngame()) {
            bot.send(chatId, "Diese Lobby ist leider schon im Spiel.");
            return;
        }
        if (isClosing()) {
            bot.send(chatId, "Diese Lobby ist grade am schließen.");
            return;
        }
        if (!isNameAvailable(username)) {
            bot.send(chatId, "Dieser Nutzername wird schon verwendet.");
            return;
        }
        if (username.length() > 16) {
            bot.send(chatId, "Dieser Nutzername ist zu lang.");
            return;
        }
        sendToAll(username + " ist der Lobby beigetreten.");
        playerlist.add(new Player(chatId, username));
        bot.send(chatId, "Du bist Lobby " + getLobbyid() + " beigetreten.");
        if (getPlayercount() >= getMinPlayerCount()) {
            startPreparing();
        }
    }

    private boolean isNameAvailable(String username) {
        for (int i = 0; i < playerlist.size(); i++) {
            if (playerlist.get(i).getUsername().equalsIgnoreCase(username))
                return false;
        }
        return true;
    }

    private void sendToAll(String string) {
        for (int i = 0; i < playerlist.size(); i++) {
            bot.send(playerlist.get(i).getChatId(), string);
        }
    }

    private void sendToAll(String string, Long chatId) {
        for (int i = 0; i < playerlist.size(); i++) {
            if (chatId != playerlist.get(i).getChatId())
                bot.send(playerlist.get(i).getChatId(), string);
        }
    }

    public void leave(Long chatId) {
        Player p = getPlayer(chatId);
        if (p != null) {
            playerlist.remove(p);
            sendToAll(p.getUsername() + " hat die Lobby verlassen.");
            bot.send(chatId, "Du hast Lobby " + getLobbyid() + " verlassen.");
        }
        if (getPlayercount() <= 0) {
            bot.closeLobby(getLobbyid());
            return;
        }
        switch (getStatus()) {
            case INGAME:
                if (getPlayercount() < getMinPlayerCount()) {
                    stopGame();
                }
                break;
            case PREPARING:
                if (getPlayercount() < getMinPlayerCount()) {
                    stopPreparing();
                }
                break;

            default:
                break;
        }

    }

    private void stopPreparing() {
        if (isPreparing()) {
            setStatus(Status.IDLE);
            sendToAll("Die Vorbereitungs-Phase wurde abgebrochen");
        }
    }

    private void startPreparing() {
        if (isIdle()) {
            setStatus(Status.PREPARING);
            new Thread(new Runnable() {

                @Override
                public void run() {
                    int i = 0;
                    while (i < 60) {
                        i++;
                        if (getStatus() != Status.PREPARING) {
                            return;
                        }
                        if (i % 10 == 0) {
                            sendToAll("Das Spiel beginnt in " + (60 - i) + " Sekunden.");
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    startGame();
                }
            }).start();
        }
    }

    protected void startGame() {
        setStatus(Status.INGAME);
        new Thread(new Runnable() {

            @Override
            public void run() {
                boolean running = true;
                for (int i = 0; i < playerlist.size(); i++) {
                    playerlist.get(i).setPoints(0);
                    playerlist.get(i).setRoundsSinceMaster(0);
                }
                int t = 5;
                Gamestate state = Gamestate.ROUND_BEGIN;
                while (running) {
                    if (getStatus() != Status.INGAME) {
                        return;
                    }
                    switch (state) {
                        case ROUND_BEGIN:
                            if (t == 5) {
                                sendToAll("Jede Runde wird ein Spieler zum Master und muss ein Wort nur duch Emojis beschreiben. Wer das Wort errät bekommt einen Punkt. Das Spiel endet wenn ein Spieler " + getWinPoints() + " hat.");
                            }
                            if (t <= 0) {
                                state = Gamestate.MASTER;
                            }
                            break;
                        case MASTER:
                            if (t <= -1) {
                                t = 30;
                                mastermessages.clear();
                                master = chooseMaster();
                                word = randomWord();
                                sendToAll(
                                        master.getUsername()
                                                + " ist Master und hat 30 Sekunden Vorbereitung.",
                                        master.getChatId());
                                bot.send(master.getChatId(), "Du bist Master. Beschreibe das Wort " + word
                                        + ". Du hast 30 Sekunden für deine erste Beschreibung.  Wenn du dieser Zeit nichts schreibst wird dir ein Punkt abgezogen.");
                            }

                            if (!mastermessages.isEmpty()) {
                                for (int i = 0; i < mastermessages.size(); i++) {
                                    sendToAll(master.getUsername() + "<b>:</b>" + mastermessages.get(i).getText(),
                                            master.getChatId());
                                    mastermessages.get(i).send();
                                }
                                state = Gamestate.GUESS;
                                t = 0;
                                break;
                            } else {
                                if (t == 20 || t == 10) {
                                    sendToAll("Der Master hat noch " + t + " Sekunden.");
                                }
                            }
                            if (t == 0) {
                                addRoundSinceMaster();
                                master.setRoundsSinceMaster(0);
                                sendToAll(master.getUsername()
                                        + " hat zu lange gebraucht um sein ersten Hinweis zu geben. Deshalb wurde ihm ein Punkt abgezogen.");
                                master.setPoints(master.getPoints() - 1);
                                state = Gamestate.WAIT;
                            }
                            break;
                        case GUESS:
                            if (t <= -1) {
                                t = 60;
                                playermessages.clear();
                                sendToAll("Du hast nun 60 Sekunden um das Wort anhand der Emojis zu erraten.",
                                        master.getChatId());
                                bot.send(master.getChatId(), "Du hast jetzt 60 Sekunden um das Wort weiter zu beschreiben.");
                            }
                            for (int i = 0; i < mastermessages.size(); i++) {
                                if (!mastermessages.get(i).isIssent()) {
                                    sendToAll(master.getUsername() + "<b>:</b>" + mastermessages.get(i).getText(),
                                            master.getChatId());
                                    mastermessages.get(i).send();
                                }
                            }
                            for (int i = 0; i < playermessages.size(); i++) {
                                if (!playermessages.get(i).isIssent()) {
                                    if (playermessages.get(i).getText().equalsIgnoreCase(word)) {
                                        wonpoint(getPlayer(playermessages.get(i).getChatId()));
                                        addRoundSinceMaster();
                                        master.setRoundsSinceMaster(0);
                                        state = Gamestate.WAIT;
                                        t = 0;
                                        break;
                                    }
                                    bot.send(master.getChatId(), getPlayer(playermessages.get(i).getChatId()).getUsername()
                                            + "<b>:</b>" + playermessages.get(i).getText());
                                    playermessages.get(i).send();
                                }
                            }
                            if (t == 0 && state == Gamestate.GUESS) {
                                addRoundSinceMaster();
                                master.setRoundsSinceMaster(0);
                                sendToAll("Keiner hat das Wort erraten. Es war: " + word);
                                state = Gamestate.WAIT;
                            }
                            break;
                        case WAIT:
                            if (t <= -1) {
                                t = 10;
                                if (checkforwin()) {
                                    running = false;
                                }
                            }
                            if (t == 0) {
                                state = Gamestate.MASTER;
                            }
                            break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        stopGame();
                        return;
                    }
                    t--;
                }
            }

            private boolean checkforwin() {
                for (int i = 0; i < playerlist.size(); i++) {
                    if (playerlist.get(i).getPoints() >= getWinPoints()) {
                        sendToAll(playerlist.get(i) + " hat gewonnen!");
                        PlayerInformation pi = bot.loadPlayerInformation(playerlist.get(i).getChatId());
                        pi.setPoints(pi.getPoints() + 1);
                        bot.update(pi);
                        return true;
                    }
                }
                return false;
            }

            private void wonpoint(Player player) {
                sendToAll(player.getUsername() + " hat das Wort erraten! Es war: " + word);
                player.setPoints(player.getPoints() + 1);
            }

            private void addRoundSinceMaster() {
                for (int i = 0; i < playerlist.size(); i++) {
                    playerlist.get(i).addRoundsSinceMaster();
                }
            }

            private Player chooseMaster() {
                int max = 0;
                Player maxplayer = null;
                for (int i = 0; i < playerlist.size(); i++) {
                    if (playerlist.get(i).roundsSinceMaster() >= max) {
                        maxplayer = playerlist.get(i);
                        max = playerlist.get(i).roundsSinceMaster();
                    }
                }
                return maxplayer;
            }
        }).start();
    }

    protected int getWinPoints() {
        return 5;
    }

    public String randomWord() {
        if (words.length <= 0) {
            return "@TimMorgner 'Die Liste ist leer'";
        }
        int i = new Random().nextInt(words.length);
        return words[i];
    }

    void stopGame() {
        if (isIngame()) {
            setStatus(Status.CLOSING);
            sendToAll("Die Lobby wurde geschlossen.");
            playerlist.clear();
            bot.closeLobby(getLobbyid());
        }
    }

    public boolean hasPlayer(Long chatId) {
        for (int i = 0; i < playerlist.size(); i++) {
            if (playerlist.get(i).getChatId() == chatId) {
                return true;
            }
        }
        return false;
    }

    public Player getPlayer(Long chatId) {
        for (int i = 0; i < playerlist.size(); i++) {
            if (playerlist.get(i).getChatId() == chatId) {
                return playerlist.get(i);
            }
        }
        return null;
    }

    public void showPoints(Long chatId) {
        @SuppressWarnings("unchecked")
        ArrayList<Player> players = (ArrayList<Player>) playerlist.clone();
        players.sort(new Comparator<Player>() {
            @Override
            public int compare(Player o1, Player o2) {
                if (o1.getPoints() < o2.getPoints())
                    return -1;
                if (o1.getPoints() > o2.getPoints())
                    return 1;
                return 0;
            }
        });
        String txt = "Punktetabelle:\n";
        for (int i = 0; i < players.size(); i++) {
            txt = txt + players.get(i).getUsername() + " - " + players.get(i).getPoints() + "\n";
        }
        bot.send(chatId, txt);
    }

    public boolean isFull() {
        return (getPlayercount() >= getMaxPlayercount());
    }

    private boolean isIngame() {
        return (getStatus() == Status.INGAME);
    }

    private boolean isIdle() {
        return (getStatus() == Status.IDLE);
    }

    private boolean isClosing() {
        return (getStatus() == Status.CLOSING);
    }

    private boolean isPreparing() {
        return (getStatus() == Status.PREPARING);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusText() {
        switch (getStatus()) {
            case IDLE:
                return "warten";
            case INGAME:
                return "am Spielen";
            case PREPARING:
                return "warten";
            case CLOSING:
                return "schließt";
            default:
                return "LIMBO? @TimMorgner anschreiben.";
        }
    }

}

enum Status {
    IDLE, PREPARING, INGAME, CLOSING
}

enum Gamestate {
    ROUND_BEGIN, MASTER, GUESS, WAIT
}
