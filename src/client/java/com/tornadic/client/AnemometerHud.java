package com.tornadic.client;

import java.util.Locale;

import com.tornadic.item.TornadicItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Compact live instrument display shown while the anemometer is held. */
public final class AnemometerHud {
	private AnemometerHud() {}

	public static void render(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui) return;
		ItemStack main = mc.player.getMainHandItem();
		ItemStack off = mc.player.getOffhandItem();
		if (!main.is(TornadicItems.ANEMOMETER) && !off.is(TornadicItems.ANEMOMETER)) return;

		Vec3 wind = ClientWeatherState.windAt(mc.player.getX(), mc.player.getY(), mc.player.getZ());
		double mph = Math.sqrt(wind.x * wind.x + wind.z * wind.z) * 20.0 * 2.23694;
		String direction = direction(wind, mph);
		String condition = mph < 3 ? "CALM" : mph < 18 ? "BREEZY" : mph < 45 ? "STRONG" : mph < 90 ? "SEVERE" : "EXTREME";
		int color = mph < 18 ? 0xFF77DDFF : mph < 45 ? 0xFFFFDD66 : mph < 90 ? 0xFFFF9944 : 0xFFFF5555;

		int width = 142, height = 54;
		int x = graphics.guiWidth() - width - 8;
		int y = graphics.guiHeight() - height - 34;
		graphics.fill(x, y, x + width, y + height, 0xCC101820);
		graphics.fill(x, y, x + width, y + 1, 0xFF6CA6B8);
		graphics.drawString(mc.font, "ANEMOMETER", x + 8, y + 6, 0xFFE8F6FA, true);
		graphics.drawString(mc.font, String.format(Locale.US, "WIND  %5.1f mph", mph), x + 8, y + 19, color, true);
		graphics.drawString(mc.font, "DIR   " + direction, x + 8, y + 31, 0xFFE8F6FA, true);
		graphics.drawString(mc.font, condition, x + 73, y + 31, color, true);

		// A continuously rotating four-cup indicator. Its angular velocity is driven
		// directly by the same synchronized local wind value used by the readout.
		double angle = (System.currentTimeMillis() / 1000.0) * Math.min(35.0, mph * 0.35);
		int cx = x + 124, cy = y + 15;
		for (int i = 0; i < 4; i++) {
			double a = angle + i * Math.PI / 2.0;
			int ex = cx + (int) Math.round(Math.cos(a) * 7);
			int ey = cy + (int) Math.round(Math.sin(a) * 7);
			graphics.fill(Math.min(cx, ex), Math.min(cy, ey), Math.max(cx, ex) + 1, Math.max(cy, ey) + 1, 0xFFBDEAF4);
			graphics.fill(ex - 1, ey - 1, ex + 2, ey + 2, color);
		}
	}

	private static String direction(Vec3 wind, double mph) {
		if (mph < 0.5) return "--";
		double deg = Math.toDegrees(Math.atan2(wind.z, wind.x));
		if (deg < 0) deg += 360.0;
		String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
		return dirs[((int) Math.round(deg / 45.0)) & 7];
	}
}
