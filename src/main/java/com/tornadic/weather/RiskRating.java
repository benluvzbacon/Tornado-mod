package com.tornadic.weather;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;

/**
 * Daily severe-weather risk rating, inspired by real severe-weather outlooks
 * (SPC-style categorical outlooks).
 *
 * <p>Order matters: ordinal() is stored in saves/sync.
 */
public enum RiskRating {
	NONE(ChatFormatting.GRAY, "Very little severe weather expected."),
	MARGINAL(ChatFormatting.GREEN, "A few isolated severe storms possible."),
	SLIGHT(ChatFormatting.YELLOW, "Scattered severe storms possible."),
	ENHANCED(ChatFormatting.GOLD, "Numerous severe storms possible."),
	MODERATE(ChatFormatting.RED, "A significant severe-weather event is possible."),
	HIGH(ChatFormatting.DARK_RED, "Extremely dangerous widespread severe-weather setup.");

	private final ChatFormatting color;
	private final String description;

	RiskRating(ChatFormatting color, String description) {
		this.color = color;
		this.description = description;
	}

	public ChatFormatting color() {
		return color;
	}

	public String description() {
		return description;
	}

	public Component asComponent() {
		return Component.literal(name()).withStyle(color);
	}

	public FormattedCharSequence colored() {
		return Component.literal(name()).withStyle(color).getVisualOrderText();
	}

	/**
	 * Storm spawn chance multiplier for a day with this rating.
	 */
	public double stormRateMultiplier() {
		return switch (this) {
			case NONE -> 0.35;
			case MARGINAL -> 0.6;
			case SLIGHT -> 1.0;
			case ENHANCED -> 1.6;
			case MODERATE -> 2.4;
			case HIGH -> 3.5;
		};
	}

	/**
	 * Highest EF level a tornado can reach on a day with this rating.
	 */
	public int tornadoEfCap() {
		return switch (this) {
			case NONE -> 1;
			case MARGINAL -> 2;
			case SLIGHT -> 2;
			case ENHANCED -> 3;
			case MODERATE -> 4;
			case HIGH -> 5;
		};
	}

	public static RiskRating fromOrdinal(int ordinal) {
		if (ordinal < 0 || ordinal >= values().length) {
			return NONE;
		}
		return values()[ordinal];
	}
}
