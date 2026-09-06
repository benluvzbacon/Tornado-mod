package com.tornadic.storm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Storm organization stage. A storm develops along the path:
 * CUMULONIMBUS -&gt; THUNDERSTORM -&gt; STRONG_THUNDERSTORM -&gt; SUPERCELL -&gt; TORNADIC_SUPERCELL.
 */
public enum StormType {
	CUMULONIMBUS("Developing Thunderstorm", ChatFormatting.GRAY, 0.35F),
	THUNDERSTORM("Thunderstorm", ChatFormatting.WHITE, 0.55F),
	STRONG_THUNDERSTORM("Strong Thunderstorm", ChatFormatting.YELLOW, 0.7F),
	SUPERCELL("Supercell", ChatFormatting.GOLD, 0.85F),
	TORNADIC_SUPERCELL("Tornadic Supercell", ChatFormatting.RED, 1.0F);

	private final String displayName;
	private final ChatFormatting color;
	private final float intensityFactor;

	StormType(String displayName, ChatFormatting color, float intensityFactor) {
		this.displayName = displayName;
		this.color = color;
		this.intensityFactor = intensityFactor;
	}

	public String displayName() {
		return displayName;
	}

	public ChatFormatting color() {
		return color;
	}

	/** Relative strength of this storm type (drives rain, lightning, wind). */
	public float intensityFactor() {
		return intensityFactor;
	}

	public boolean canHaveHail() {
		return this == SUPERCELL || this == TORNADIC_SUPERCELL;
	}

	public boolean canSpawnTornado() {
		return this == TORNADIC_SUPERCELL;
	}

	public net.minecraft.network.chat.MutableComponent asComponent() {
		return Component.literal(displayName).withStyle(color);
	}

	public static StormType fromOrdinal(int ordinal) {
		if (ordinal < 0 || ordinal >= values().length) {
			return CUMULONIMBUS;
		}
		return values()[ordinal];
	}
}
