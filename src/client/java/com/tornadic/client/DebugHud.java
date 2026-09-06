package com.tornadic.client;

import java.util.Locale;

import com.tornadic.config.TornadicConfig;
import com.tornadic.network.StormSyncPayload;
import com.tornadic.network.TornadoSyncPayload;
import com.tornadic.network.WeatherSyncPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Debug overlay (key V toggles; default OFF): live atmospheric values,
 * probabilities and storm/tornado positions.
 */
public final class DebugHud {
	private DebugHud() {
	}

	public static void render(GuiGraphics graphics) {
		if (!TornadicConfig.debugHud) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int y = 4;
		graphics.drawTextWithShadow(font, "TORNADIC DEBUG [V]", 4, y, 0xFF55FF55);
		y += 10;
		WeatherSyncPayload w = ClientWeatherState.weather;
		if (w == null) {
			graphics.drawTextWithShadow(font, "waiting for sync...", 4, y, 0xFFAAAAAA);
			return;
		}
		graphics.drawTextWithShadow(font, String.format(Locale.US,
			"Day %d | Risk: %s", w.day(),
			com.tornadic.weather.RiskRating.fromOrdinal(w.riskOrdinal()).name()), 4, y, 0xFFEEEEEE);
		y += 10;
		graphics.drawTextWithShadow(font, String.format(Locale.US,
			"T %.0fF | Dew %.0fF | RH %d%% | P %.0fhPa",
			w.tempF(), w.dewPointF(), w.humidity(), w.pressureMb()), 4, y, 0xFFEEEEEE);
		y += 10;
		graphics.drawTextWithShadow(font, String.format(Locale.US,
			"Wind %.0fmph @%.0fdeg | CAPE %d | Shear %dkt",
			w.windMph(), w.windDir(), w.cape(), w.shear()), 4, y, 0xFFEEEEEE);
		y += 10;
		graphics.drawTextWithShadow(font, String.format(Locale.US,
			"StormP %d%% | TornadoP %d%% | Rain %.0f%%",
			w.stormProbability(), w.tornadoProbability(), w.rainLevel() * 100), 4, y, 0xFFEEEEEE);
		y += 10;

		int x0 = 4;
		if (!ClientWeatherState.storms.isEmpty()) {
			graphics.drawTextWithShadow(font, "Storms:", x0, y, 0xFFFFAA55);
			y += 10;
			int shown = 0;
			for (StormSyncPayload s : ClientWeatherState.storms.values()) {
				if (shown++ >= 4) {
					break;
				}
			graphics.drawTextWithShadow(font, String.format(Locale.US,
				"  %s @ %d,%d  r=%dm dir=%.0f rot=%.2f%s",
				com.tornadic.storm.StormType.fromOrdinal(s.typeOrdinal()).displayName(),
				(int) s.x(), (int) s.z(), s.radius(),
				Math.toDegrees(s.dir()), s.rotation(), s.hail() ? " HAIL" : ""), x0, y, 0xFFDDDDDD);
				y += 10;
			}
		}
		if (!ClientWeatherState.tornadoes.isEmpty()) {
			graphics.drawTextWithShadow(font, "Tornadoes:", x0, y, 0xFFFF5555);
			y += 10;
			int shown = 0;
			for (TornadoSyncPayload t : ClientWeatherState.tornadoes.values()) {
				if (shown++ >= 4) {
					break;
				}
			graphics.drawTextWithShadow(font, String.format(Locale.US,
				"  EF%d @ %d,%d  wind=%.0fm/s funnel=%.0fm",
				t.ef(), (int) t.x(), (int) t.z(), t.windMs(), t.funnelRadius()), x0, y, 0xFFF0D0D0);
				y += 10;
			}
		}
		if (mc.player != null) {
			graphics.drawTextWithShadow(font, String.format(Locale.US,
				"Local wind: %.0f mph",
				ClientWeatherState.localWindMph(mc.player.getX(), mc.player.getY(), mc.player.getZ())), x0, y, 0xFF88FF88);
		}
	}
}
