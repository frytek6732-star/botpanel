package pl.botpanel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpClientSession;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.time.Instant;
import java.util.BitSet;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot {
	public enum State { OFFLINE, CONNECTING, ONLINE }

	private final String name;
	private final BotManager manager;
	private volatile State state = State.OFFLINE;
	private volatile String lastMessage = "";
	private volatile Session session;

	public Bot(String name, BotManager manager) {
		this.name = name;
		this.manager = manager;
	}

	public String name() { return name; }
	public State state() { return state; }
	public String lastMessage() { return lastMessage; }

	public synchronized void connect() {
		if (state != State.OFFLINE) return;
		state = State.CONNECTING;

		// Tryb offline (nick bez konta premium) - zwykle tak działają boty na anarchiach.
		MinecraftProtocol protocol = new MinecraftProtocol(name);
		TcpClientSession s = new TcpClientSession(BotManager.HOST, BotManager.PORT, protocol);
		s.addListener(new SessionAdapter() {
			@Override
			public void connected(ConnectedEvent event) {
				lastMessage = "połączono, logowanie...";
			}

			@Override
			public void packetReceived(Session session, Packet packet) {
				if (packet instanceof ClientboundLoginPacket) {
					state = State.ONLINE;
					lastMessage = "wszedł na serwer";
					modeSent = false;
					lastAuthAt = 0;
					// Jeśli serwer nie wyśle prośby o hasło (np. już zarejestrowany sesją), spróbuj mimo to po 6 s.
					if (manager.autoLogin) {
						SCHEDULER.schedule(() -> {
							if (!modeSent) command("login " + manager.password);
						}, 4, TimeUnit.SECONDS);
						SCHEDULER.schedule(() -> scheduleModeJoin(0), 7, TimeUnit.SECONDS);
					}
				} else if (packet instanceof ClientboundSystemChatPacket chat) {
					onChat(plain(chat.getContent()));
				}
			}

			@Override
			public void disconnected(DisconnectedEvent event) {
				state = State.OFFLINE;
				lastMessage = "rozłączony: " + plain(event.getReason());
			}
		});
		session = s;
		s.connect(false);
	}

	public synchronized void disconnect() {
		Session s = session;
		if (s != null) s.disconnect("bye");
		state = State.OFFLINE;
	}

	public void chat(String message) {
		Session s = session;
		if (s == null || state != State.ONLINE) return;
		s.send(new ServerboundChatPacket(message, Instant.now(), 0L, null, 0, new BitSet(20)));
	}

	public void command(String command) {
		Session s = session;
		if (s == null || state != State.ONLINE) return;
		if (command.startsWith("/")) command = command.substring(1);
		s.send(new ServerboundChatCommandPacket(command));
	}

	private volatile boolean modeSent = false;
	private volatile long lastAuthAt = 0;

	private void scheduleModeJoin(long delayMs) {
		if (!manager.autoJoinMode || modeSent) return;
		modeSent = true;
		SCHEDULER.schedule(() -> command(manager.modeCommand), delayMs, TimeUnit.MILLISECONDS);
	}

	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "botpanel-scheduler");
		t.setDaemon(true);
		return t;
	});

	private void onChat(String text) {
		lastMessage = text;
		String low = text.toLowerCase(Locale.ROOT);

		if (manager.autoLogin) {
			long now = System.currentTimeMillis();
			boolean needRegister = low.contains("/register") || low.contains("/reg ") || low.contains("zarejestruj");
			boolean needLogin = low.contains("/login") || low.contains("/l ") || low.contains("zaloguj");
			if ((needRegister || needLogin) && now - lastAuthAt > 2000) {
				lastAuthAt = now;
				String pw = manager.password;
				if (needRegister) command("register " + pw + " " + pw);
				else command("login " + pw);
				// po wysłaniu hasła poczekaj chwilę i wejdź na tryb
				scheduleModeJoin(2500);
				return;
			}
			if (low.contains("zalogowano") || low.contains("logged in") || low.contains("successful login")
					|| low.contains("zarejestrowano") || low.contains("registered")) {
				scheduleModeJoin(1500);
			}
		}

		if (!manager.autoAcceptRules) return;
		for (String t : manager.rulesTriggers) {
			if (low.contains(t)) {
				command(manager.rulesCommand);
				return;
			}
		}
	}

	private static String plain(Component c) {
		return c == null ? "" : PlainTextComponentSerializer.plainText().serialize(c);
	}
}
