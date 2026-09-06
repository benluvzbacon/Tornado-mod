package com.tornadic.client;

import java.util.Locale;

import com.tornadic.network.WeatherSyncPayload;
import com.tornadic.weather.RiskRating;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Storm notebook: today's forecast summary plus the day's event log exactly as
 * recorded by the server simulation (storms, tornadoes, max EF observed).
 */
public class NotebookScreen extends Screen {
	public NotebookScreen() {
		super(Component.translatable("screen.tornadic.notebook"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		// Do not invoke Screen#renderBackground here. In 1.21.1 the game may have
		// already installed the world-screen blur pass for this frame; requesting it
		// again can throw and crash the client when the notebook opens. A lightweight
		// translucent backdrop is deterministic and compatible with other render mods.
		g.fill(0, 0, this.width, this.height, 0x88000000);
		int w = 250;
		int h = 190;
		int x0 = (this.width - w) / 2;
		int y0 = (this.height - h) / 2;
		g.fill(x0 - 6, y0 - 14, x0 + w + 6, y0 + h + 6, 0xF0201A12);
		g.fill(x0, y0, x0 + w, y0 + h, 0xFF101418);

		Font font = this.font;
		WeatherSyncPayload w2 = ClientWeatherState.weather;
		int y = y0 + 8;
		int x = x0 + 10;
		g.drawString(font, "STORM NOTEBOOK", x0 + w / 2 - font.width("STORM NOTEBOOK") / 2, y0 - 10, 0xFFFFD080, true);
		if (w2 == null) {
			g.drawString(font, "waiting for weather data...", x, y, 0xFF999999, true);
			y += 14;
			g.drawString(font, "stand by for a sync packet", x, y, 0xFF666666, true);
		} else {
			g.drawString(font, "Day " + w2.day(), x, y, 0xFFEEEEEE, true);
			y += 14;
			g.drawString(font, "Risk rating: " + RiskRating.fromOrdinal(w2.riskOrdinal()), x, y,
				w2.riskOrdinal() >= 4 ? 0xFFFF6644 : 0xFFCCDDCC, true);
			y += 16;
			g.drawString(font, String.format(Locale.US,
				"Temp %.0fF   Dew point %.0fF   Humidity %d%%", w2.tempF(), w2.dewPointF(), w2.humidity()), x, y, 0xFFD0D0D0, true);
			y += 12;
			g.drawString(font, String.format(Locale.US,
				"Pressure %.0f hPa   Wind %.0f mph @ %.0f deg", w2.pressureMb(), w2.windMph(), w2.windDir()), x, y, 0xFFD0D0D0, true);
			y += 12;
			g.drawString(font, String.format(Locale.US,
				"CAPE %d J/kg   Shear %d kt", w2.cape(), w2.shear()), x, y, 0xFFD0D0D0, true);
			y += 12;
			g.drawString(font, String.format(Locale.US,
				"Storm probability %d%%   Tornado probability %d%%",
				w2.stormProbability(), w2.tornadoProbability()), x, y, 0xFFD0D0D0, true);
			y += 16;
			g.drawString(font, "--- Today so far ---", x, y, 0xFF80A0FF, true);
			y += 12;
			g.drawString(font, String.format(Locale.US,
				"Storms: %d   Tornadoes: %d   Max EF: %s",
				w2.stormsToday(), w2.tornadoesToday(),
				w2.maxEfToday() < 0 ? "none" : "EF" + w2.maxEfToday()), x, y, 0xFFD0D0D0, true);
			y += 12;
			g.drawString(font, String.format(Locale.US,
				"Active storms in range: %d", ClientWeatherState.storms.size()), x, y, 0xFFB0C0B0, true);
		}
		g.drawCenteredString(font, "[ESC] close", this.width / 2, y0 + h + 12, 0xFF777777);
	}
}
