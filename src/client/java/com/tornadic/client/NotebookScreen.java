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
	protected void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		renderBackground(g);
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
		g.drawTextWithShadow(font, "STORM NOTEBOOK", x0 + w / 2 - font.width("STORM NOTEBOOK") / 2, y0 - 10, 0xFFFFD080);
		if (w2 == null) {
			g.drawTextWithShadow(font, "waiting for weather data...", x, y, 0xFF999999);
			y += 14;
			g.drawTextWithShadow(font, "stand by for a sync packet", x, y, 0xFF666666);
		} else {
			g.drawTextWithShadow(font, "Day " + w2.day(), x, y, 0xFFEEEEEE);
			y += 14;
			g.drawTextWithShadow(font, "Risk rating: " + RiskRating.fromOrdinal(w2.riskOrdinal()), x, y,
				w2.riskOrdinal() >= 4 ? 0xFFFF6644 : 0xFFCCDDCC);
			y += 16;
			g.drawTextWithShadow(font, String.format(Locale.US,
				"Temp %.0fF   Dew point %.0fF   Humidity %d%%", w2.tempF(), w2.dewPointF(), w2.humidity()), x, y, 0xFFD0D0D0);
			y += 12;
			g.drawTextWithShadow(font, String.format(Locale.US,
				"Pressure %.0f hPa   Wind %.0f mph @ %.0f deg", w2.pressureMb(), w2.windMph(), w2.windDir()), x, y, 0xFFD0D0D0);
			y += 12;
			g.drawTextWithShadow(font, String.format(Locale.US,
				"CAPE %d J/kg   Shear %d kt", w2.cape(), w2.shear()), x, y, 0xFFD0D0D0);
			y += 12;
			g.drawTextWithShadow(font, String.format(Locale.US,
				"Storm probability %d%%   Tornado probability %d%%",
				w2.stormProbability(), w2.tornadoProbability()), x, y, 0xFFD0D0D0);
			y += 16;
			g.drawTextWithShadow(font, "--- Today so far ---", x, y, 0xFF80A0FF);
			y += 12;
			g.drawTextWithShadow(font, String.format(Locale.US,
				"Storms: %d   Tornadoes: %d   Max EF: %s",
				w2.stormsToday(), w2.tornadoesToday(),
				w2.maxEfToday() < 0 ? "none" : "EF" + w2.maxEfToday()), x, y, 0xFFD0D0D0);
			y += 12;
			g.drawTextWithShadow(font, String.format(Locale.US,
				"Active storms in range: %d", ClientWeatherState.storms.size()), x, y, 0xFFB0C0B0);
		}
		g.drawCenteredTextWithShadow(font, "[ESC] close", this.width / 2, y0 + h + 12, 0xFF777777);
	}
}
