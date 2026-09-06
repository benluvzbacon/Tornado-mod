package com.tornadic.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.tornadic.network.StormSyncPayload;
import com.tornadic.network.TornadoSyncPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Storm radar scope. Deliberately not omniscient:
 *  - range-limited (3200 blocks),
 *  - shows organized convective cells as returns, supercells with a rotation
 *    halo, tornado warnings only when the circulation is strong (EF2+),
 *  - sweep animation; returns are quantized to the scope resolution.
 */
public class RadarScreen extends Screen {
	private static final int SCOPE = 220;
	private static final int RANGE = com.tornadic.config.TornadicConfig.radarRange;

	public RadarScreen() {
		super(Component.translatable("screen.tornadic.radar"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	protected void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		renderBackground(g);
		int ox = (this.width - SCOPE) / 2;
		int oy = (this.height - SCOPE) / 2 + 10;
		int cx = ox + SCOPE / 2;
		int cy = oy + SCOPE / 2;

		// Scope panel.
		g.fill(ox - 8, oy - 8, ox + SCOPE + 8, oy + SCOPE + 8, 0xF005080A);
		g.fill(ox, oy, ox + SCOPE, oy + SCOPE, 0xFF0A120C);

		// Range rings.
		int ringColor = 0x6600FF00;
		drawRing(g, cx, cy, SCOPE / 4, ringColor);
		drawRing(g, cx, cy, SCOPE / 2, ringColor);
		drawRing(g, cx, cy, SCOPE * 3 / 4, ringColor);
		// Crosshair.
		g.fill(ox, cy, ox + SCOPE, cy + 1, ringColor);
		g.fill(cx, oy, cx + 1, oy + SCOPE, ringColor);

		// Rotating sweep.
		long t = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
		double angle = ((t % 200L) / 200.0) * Math.PI * 2;
		int sweepColor = 0xCC00FF00;
		for (int i = 0; i < 24; i++) {
			double a = angle - i * 0.02;
			int x1 = cx + (int) (Math.cos(a) * (SCOPE / 2 - 2));
			int z1 = cy + (int) (Math.sin(a) * (SCOPE / 2 - 2));
			int x2 = cx + (int) (Math.cos(a - 0.015) * (SCOPE / 2 - 2));
			int z2 = cy + (int) (Math.sin(a - 0.015) * (SCOPE / 2 - 2));
			g.fill(Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2) + 1, Math.max(z1, z2) + 1,
				i == 0 ? sweepColor : (int) (0x5500FF00 - i * 8));
		}

		// Returns.
		List<Blip> blips = new ArrayList<>();
		if (Minecraft.getInstance().player != null) {
			double px = Minecraft.getInstance().player.getX();
			double pz = Minecraft.getInstance().player.getZ();
			for (StormSyncPayload s : ClientWeatherState.storms.values()) {
				double dx = s.x() - px;
				double dz = s.z() - pz;
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > RANGE) {
					continue; // out of range
				}
				double scale = (SCOPE / 2.0 - 10) / (double) RANGE;
				// Quantize to the scope resolution (radar is imperfect).
				int qx = (int) ((int) (dx * scale / 8) * 8);
				int qz = (int) ((int) (dz * scale / 8) * 8);
				int color = 0xFFAAFF33; // ordinary cell
				if (com.tornadic.storm.StormType.fromOrdinal(s.typeOrdinal()).ordinal()
					>= com.tornadic.storm.StormType.SUPERCELL.ordinal()) {
					color = 0xFFFFAA33;
				}
				Blip b = new Blip(cx + qx, cy + qz, color, s.rotation() > 0.5, dist);
				blips.add(b);
			}
			for (TornadoSyncPayload t2 : ClientWeatherState.tornadoes.values()) {
				double dx = t2.x() - px;
				double dz = t2.z() - pz;
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > RANGE) {
					continue;
				}
				double scale = (SCOPE / 2.0 - 10) / (double) RANGE;
				int qx = (int) ((int) (dx * scale / 8) * 8);
				int qz = (int) ((int) (dz * scale / 8) * 8);
				// Only strong circulations show a tornado flag.
				if (t2.ef() >= 2) {
					Blip b = new Blip(cx + qx, cy + qz, 0xFFFF4444, false, dist);
					b.tornado = true;
					b.ef = t2.ef();
					blips.add(b);
				}
			}
		}
		for (Blip b : blips) {
			g.fill(b.x - 3, b.y - 3, b.x + 4, b.y + 4, b.color);
			if (b.rotating) {
				g.fill(b.x - 5, b.y - 5, b.x - 4, b.y - 4, b.color);
				g.fill(b.x + 4, b.y - 5, b.x + 5, b.y - 4, b.color);
				g.fill(b.x - 5, b.y + 4, b.x - 4, b.y + 5, b.color);
				g.fill(b.x + 4, b.y + 4, b.x + 5, b.y + 5, b.color);
			}
			if (b.tornado) {
				Font f = this.font;
				g.drawTextWithShadow(f, "EF" + b.ef, b.x + 6, b.y - 8, 0xFFFF6666);
			}
		}

		// Labels.
		Font font = this.font;
		g.drawCenteredTextWithShadow(font, this.title, this.width / 2, oy - 22, 0xFFDDDDDD);
		g.drawCenteredTextWithShadow(font,
			String.format(Locale.US, "range %.0f km | sweep active", RANGE / 1000.0),
			this.width / 2, oy + SCOPE + 14, 0xFF66FF66);
		g.drawCenteredTextWithShadow(font, "green=cell  orange=supercell  red=tornado",
			this.width / 2, oy + SCOPE + 24, 0xFF999999);
		g.drawCenteredTextWithShadow(font, "[ESC] close", this.width / 2, oy + SCOPE + 34, 0xFF777777);
	}

	private void drawRing(GuiGraphics g, int cx, int cy, int radius, int color) {
		// Hollow square approximation of a ring.
		g.fill(cx - radius, cy - 1, cx + radius + 1, cy, color);       // top
		g.fill(cx - radius, cy + radius, cx + radius + 1, cy + radius + 1, color); // bottom
		g.fill(cx - radius, cy - radius, cx - radius + 1, cy + radius + 1, color); // left
		g.fill(cx + radius, cy - radius, cx + radius + 1, cy + radius + 1, color); // right
	}

	private static final class Blip {
		int x;
		int y;
		int color;
		boolean rotating;
		boolean tornado;
		int ef;
		double dist;

		Blip(int x, int y, int color, boolean rotating, double dist) {
			this.x = x;
			this.y = y;
			this.color = color;
			this.rotating = rotating;
			this.dist = dist;
		}
	}
}
