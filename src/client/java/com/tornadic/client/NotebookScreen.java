package com.tornadic.client;

import java.util.Locale;
import com.tornadic.network.StormSyncPayload;
import com.tornadic.network.TornadoSyncPayload;
import com.tornadic.network.WeatherSyncPayload;
import com.tornadic.storm.StormType;
import com.tornadic.weather.RiskRating;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Crash-contained, data-rich storm notebook backed only by synchronized state. */
public class NotebookScreen extends Screen {
	private String renderError;

	public NotebookScreen() { super(Component.translatable("screen.tornadic.notebook")); }
	@Override public boolean isPauseScreen() { return false; }

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		g.fill(0, 0, width, height, 0xB0000000);
		try {
			renderNotebook(g);
			renderError = null;
		} catch (Throwable problem) {
			// A malformed/outdated sync packet must never take down the render thread.
			renderError = problem.getClass().getSimpleName();
			g.drawCenteredString(font, "Notebook data temporarily unavailable", width / 2, height / 2, 0xFFFF7777);
			g.drawCenteredString(font, renderError, width / 2, height / 2 + 13, 0xFFAAAAAA);
		}
	}

	private void renderNotebook(GuiGraphics g) {
		int panelW = Math.min(330, width - 24), panelH = Math.min(230, height - 24);
		int x0 = (width - panelW) / 2, y0 = (height - panelH) / 2;
		g.fill(x0, y0, x0 + panelW, y0 + panelH, 0xF0181D20);
		g.fill(x0, y0, x0 + panelW, y0 + 2, 0xFFFFC45C);
		int x = x0 + 10, y = y0 + 9;
		g.drawCenteredString(font, "TORNADIC STORM NOTEBOOK", width / 2, y, 0xFFFFD080); y += 16;
		WeatherSyncPayload w = ClientWeatherState.weather;
		if (w == null) {
			g.drawString(font, "Waiting for server weather synchronization...", x, y, 0xFFBBBBBB); return;
		}
		RiskRating risk = RiskRating.fromOrdinal(w.riskOrdinal());
		line(g, x, y, "DAY " + (w.day() + 1) + "   RISK: " + risk.name(), 0xFFFFFFFF); y += 12;
		line(g, x, y, String.format(Locale.US, "Temperature %.0f F   Dew point %.0f F   Humidity %d%%", w.tempF(), w.dewPointF(), w.humidity()), 0xFFD8E5E8); y += 11;
		line(g, x, y, String.format(Locale.US, "Pressure %.0f hPa   Wind %.0f mph at %.0f deg", w.pressureMb(), w.windMph(), w.windDir()), 0xFFD8E5E8); y += 11;
		line(g, x, y, String.format(Locale.US, "CAPE %d J/kg   Shear %d kt", w.cape(), w.shear()), 0xFFD8E5E8); y += 11;
		line(g, x, y, String.format(Locale.US, "Storm chance %d%%   Tornado chance %d%%", w.stormProbability(), w.tornadoProbability()), 0xFFFFD080); y += 14;
		line(g, x, y, String.format(Locale.US, "TODAY: %d storms, %d tornadoes, max %s", w.stormsToday(), w.tornadoesToday(), w.maxEfToday() < 0 ? "none" : "EF" + w.maxEfToday()), 0xFF9FC7FF); y += 15;
		line(g, x, y, "ACTIVE WEATHER", 0xFFFFC45C); y += 11;
		int shown = 0;
		for (ClientWeatherState.Stamp<StormSyncPayload> stamp : ClientWeatherState.storms.values()) {
			if (shown++ >= 4 || y > y0 + panelH - 35) break;
			StormSyncPayload s = stamp.value;
			line(g, x + 5, y, String.format(Locale.US, "%s  X%d Z%d  intensity %.0f%%  moving %s",
				StormType.fromOrdinal(s.typeOrdinal()).displayName(), (int)s.x(), (int)s.z(), s.intensity() * 100, compass(s.dir())), 0xFFE5E5E5); y += 11;
		}
		shown = 0;
		for (ClientWeatherState.Stamp<TornadoSyncPayload> stamp : ClientWeatherState.tornadoes.values()) {
			if (shown++ >= 3 || y > y0 + panelH - 24) break;
			TornadoSyncPayload t = stamp.value;
			line(g, x + 5, y, String.format(Locale.US, "TORNADO EF%d  X%d Z%d  core %.0f m/s", t.ef(), (int)t.x(), (int)t.z(), t.windMs()), 0xFFFF7777); y += 11;
		}
		if (ClientWeatherState.storms.isEmpty() && ClientWeatherState.tornadoes.isEmpty())
			line(g, x + 5, y, "No active storms currently detected.", 0xFF999999);
		g.drawCenteredString(font, "ESC to close", width / 2, y0 + panelH - 12, 0xFF888888);
	}

	private void line(GuiGraphics g, int x, int y, String text, int color) { g.drawString(font, text, x, y, color, false); }
	private static String compass(float radians) {
		double d = Math.toDegrees(radians); if (d < 0) d += 360;
		String[] dirs={"E","NE","N","NW","W","SW","S","SE"}; return dirs[((int)Math.round(d/45.0))&7];
	}
}
