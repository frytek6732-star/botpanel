package pl.botpanel;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BotPanelScreen extends Screen {
	private TextFieldWidget nameField;
	private TextFieldWidget textField;
	private final BotManager m = BotPanelClient.MANAGER;

	public BotPanelScreen() {
		super(Text.literal("Bot Panel - " + BotManager.HOST));
	}

	@Override
	protected void init() {
		int x = 10;
		nameField = new TextFieldWidget(textRenderer, x, 30, 120, 18, Text.literal("nick"));
		nameField.setPlaceholder(Text.literal("nick bota"));
		addDrawableChild(nameField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj bota"), b -> {
			String n = nameField.getText().trim();
			if (!n.isEmpty()) { m.add(n); nameField.setText(""); }
		}).dimensions(x + 125, 29, 70, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("Połącz wszystkie"), b -> m.connectAll())
				.dimensions(x, 55, 95, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Rozłącz wszystkie"), b -> m.disconnectAll())
				.dimensions(x + 100, 55, 95, 20).build());

		addDrawableChild(ButtonWidget.builder(Text.literal("TPA do mnie"), b -> {
			if (client != null && client.player != null) m.tpaAll(client.player.getGameProfile().getName());
		}).dimensions(x, 80, 95, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Regulamin: " + (m.autoAcceptRules ? "ON" : "OFF")), b -> {
			m.autoAcceptRules = !m.autoAcceptRules;
			b.setMessage(Text.literal("Regulamin: " + (m.autoAcceptRules ? "ON" : "OFF")));
		}).dimensions(x + 100, 80, 95, 20).build());

		TextFieldWidget pwField = new TextFieldWidget(textRenderer, x + 205, 30, 110, 18, Text.literal("hasło"));
		pwField.setText(m.password);
		pwField.setChangedListener(s -> { if (!s.isEmpty()) m.password = s; });
		addDrawableChild(pwField);
		TextFieldWidget modeField = new TextFieldWidget(textRenderer, x + 205, 55, 110, 18, Text.literal("tryb"));
		modeField.setText(m.modeCommand);
		modeField.setChangedListener(s -> { if (!s.isEmpty()) m.modeCommand = s.startsWith("/") ? s.substring(1) : s; });
		addDrawableChild(modeField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Auto-login+tryb: " + (m.autoLogin ? "ON" : "OFF")), b -> {
			m.autoLogin = !m.autoLogin;
			m.autoJoinMode = m.autoLogin;
			b.setMessage(Text.literal("Auto-login+tryb: " + (m.autoLogin ? "ON" : "OFF")));
		}).dimensions(x + 205, 80, 110, 20).build());

		textField = new TextFieldWidget(textRenderer, x, 108, 195, 18, Text.literal("tekst"));
		textField.setMaxLength(256);
		textField.setPlaceholder(Text.literal("wiadomość lub /komenda"));
		addDrawableChild(textField);
		addDrawableChild(ButtonWidget.builder(Text.literal("Wyślij wszystkim"), b -> {
			String t = textField.getText();
			if (t.isEmpty()) return;
			if (t.startsWith("/")) m.commandAll(t); else m.sayAll(t);
			textField.setText("");
		}).dimensions(x, 130, 195, 20).build());
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawTextWithShadow(textRenderer, title, 10, 12, 0xFFFFFF);

		int y = 160;
		for (Bot bot : m.bots()) {
			int color = switch (bot.state()) {
				case ONLINE -> 0x55FF55;
				case CONNECTING -> 0xFFFF55;
				case OFFLINE -> 0xAAAAAA;
			};
			String line = bot.name() + " [" + bot.state() + "] " + trim(bot.lastMessage(), 60);
			ctx.drawTextWithShadow(textRenderer, line, 10, y, color);
			y += 11;
		}
	}

	private static String trim(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
