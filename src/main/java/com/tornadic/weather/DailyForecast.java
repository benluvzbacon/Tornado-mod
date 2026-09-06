package com.tornadic.weather;

import java.util.List;

import net.minecraft.nbt.CompoundTag;

/**
 * The atmospheric state of a single Minecraft day, generated deterministically from
 * (world seed, day). Every value shown by /thermos comes from here, and the storm
 * simulation consumes these values directly.
 *
 * <p>Units follow the /thermos display: temperature in °F, wind in mph, CAPE in J/kg.
 */
public record DailyForecast(
		int day,
		float tempF,
		float dewPointF,
		int humidity,
		float pressureMb,
		float windMph,
		float windDir,
		int cape,
		float shear,
		int stormProbability,
		int tornadoProbability,
		RiskRating risk,
		List<String> forecastLines
) {
	public CompoundTag writeNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Day", day);
		tag.putFloat("TempF", tempF);
		tag.putFloat("DewPointF", dewPointF);
		tag.putInt("Humidity", humidity);
		tag.putFloat("PressureMb", pressureMb);
		tag.putFloat("WindMph", windMph);
		tag.putFloat("WindDir", windDir);
		tag.putInt("Cape", cape);
		tag.putFloat("Shear", shear);
		tag.putInt("StormProb", stormProbability);
		tag.putInt("TornadoProb", tornadoProbability);
		tag.putInt("Risk", risk.ordinal());
		CompoundTag lines = new CompoundTag();
		for (int i = 0; i < forecastLines.size(); i++) {
			lines.putString("L" + i, forecastLines.get(i));
		}
		tag.put("Lines", lines);
		return tag;
	}

	public static DailyForecast readNbt(CompoundTag tag) {
		java.util.ArrayList<String> lines = new java.util.ArrayList<>();
		CompoundTag l = tag.getCompound("Lines");
		for (int i = 0; l.contains("L" + i); i++) {
			lines.add(l.getString("L" + i));
		}
		return new DailyForecast(
			tag.getInt("Day"),
			tag.getFloat("TempF"),
			tag.getFloat("DewPointF"),
			tag.getInt("Humidity"),
			tag.getFloat("PressureMb"),
			tag.getFloat("WindMph"),
			tag.getFloat("WindDir"),
			tag.getInt("Cape"),
			tag.getFloat("Shear"),
			tag.getInt("StormProb"),
			tag.getInt("TornadoProb"),
			RiskRating.fromOrdinal(tag.getInt("Risk")),
			lines
		);
	}
}
