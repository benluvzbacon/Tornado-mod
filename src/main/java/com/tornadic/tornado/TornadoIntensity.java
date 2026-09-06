package com.tornadic.tornado;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Enhanced-Furneaux (EF) style tornado intensity scale.
 *
 * <p>Wind speeds are realistic EF-scale values in m/s; in-game forces are scaled by
 * the windStrength config so players can actually fight the wind.
 */
public enum TornadoIntensity {
	EF0("EF0", 30.0F, 7.0F, ChatFormatting.GREEN),
	EF1("EF1", 42.0F, 11.0F, ChatFormatting.YELLOW),
	EF2("EF2", 55.0F, 15.0F, ChatFormatting.GOLD),
	EF3("EF3", 68.0F, 21.0F, ChatFormatting.RED),
	EF4("EF4", 82.0F, 30.0F, ChatFormatting.LIGHT_PURPLE),
	EF5("EF5", 100.0F, 42.0F, ChatFormatting.DARK_RED);

	private final String label;
	private final float windMs;          // core wind speed, m/s (realistic EF scale)
	private final float funnelRadius;    // funnel radius at the ground, blocks
	private final ChatFormatting color;

	TornadoIntensity(String label, float windMs, float funnelRadius, ChatFormatting color) {
		this.label = label;
		this.windMs = windMs;
		this.funnelRadius = funnelRadius;
		this.color = color;
	}

	public String label() {
		return label;
	}

	public float windMs() {
		return windMs;
	}

	public float funnelRadius() {
		return funnelRadius;
	}

	public Component asComponent() {
		return Component.literal(label).withStyle(color);
	}

	/**
	 * Hardness threshold of blocks this intensity can break (world damage).
	 * Obsidian (1200) and bedrock (unbreakable) are safe at every level.
	 */
	public float destroyHardnessLimit() {
		return switch (this) {
			case EF0 -> 0.7F;   // plants, crops, snow, leaves
			case EF1 -> 1.6F;   // wood, dirt, glass, stone start
			case EF2 -> 3.2F;   // planks, cobblestone, many ores
			case EF3 -> 7.0F;   // iron, deepslate bricks
			case EF4 -> 15.0F;  // most player-built structures
			case EF5 -> 80.0F;  // catastrophic, still not obsidian
		};
	}

	/** Wind damage threshold in m/s: wind above this hurts. */
	public float windDamageThreshold() {
		return windMs() * 0.55F;
	}

	/** Probability (per effect tick) that the tornado flings one nearby item entity. */
	public float debrisChance() {
		return switch (this) {
			case EF0 -> 0.0F;
			case EF1 -> 0.02F;
			case EF2 -> 0.05F;
			case EF3 -> 0.09F;
			case EF4 -> 0.14F;
			case EF5 -> 0.2F;
		};
	}

	public static TornadoIntensity fromEf(int ef) {
		if (ef < 0) {
			return EF0;
		}
		if (ef >= values().length) {
			return EF5;
		}
		return values()[ef];
	}
}
