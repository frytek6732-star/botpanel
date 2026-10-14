package pl.botpanel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BotManager {
	public static final String HOST = "anarchia.gg";
	public static final int PORT = 25565;

	/** Komenda wysyłana przez bota, gdy serwer poprosi o akceptację regulaminu. Zmień pod swój serwer. */
	public volatile String rulesCommand = "regulamin akceptuj";
	/** Fragmenty czatu, po których bot uznaje, że ma zaakceptować regulamin. */
	public final List<String> rulesTriggers = new ArrayList<>(List.of("regulamin", "zaakceptuj", "akceptuj"));
	public volatile boolean autoAcceptRules = true;

	/** Hasło używane przez boty do /login i /register. */
	public volatile String password = "BotPass123!";
	public volatile boolean autoLogin = true;
	/** Komenda wejścia na tryb po zalogowaniu (bez ukośnika). Zmień jeśli na serwerze jest inna. */
	public volatile String modeCommand = "lifesteal";
	public volatile boolean autoJoinMode = true;

	private final List<Bot> bots = new CopyOnWriteArrayList<>();

	public List<Bot> bots() {
		return bots;
	}

	public Bot add(String name) {
		for (Bot b : bots) {
			if (b.name().equalsIgnoreCase(name)) return b;
		}
		Bot bot = new Bot(name, this);
		bots.add(bot);
		return bot;
	}

	public void remove(Bot bot) {
		bot.disconnect();
		bots.remove(bot);
	}

	public void connectAll() {
		for (Bot b : bots) b.connect();
	}

	public void disconnectAll() {
		for (Bot b : bots) b.disconnect();
	}

	/** Każdy bot wysyła /tpa do podanego gracza. */
	public void tpaAll(String target) {
		for (Bot b : bots) b.command("tpa " + target);
	}

	public void sayAll(String message) {
		for (Bot b : bots) b.chat(message);
	}

	public void commandAll(String command) {
		for (Bot b : bots) b.command(command);
	}
}
