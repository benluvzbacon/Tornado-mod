package com.tornadic.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic daily atmospheric generator.
 *
 * <p>The forecast for a day is a pure function of (world seed, day number), so the
 * weather never "changes its mind" on reload and /thermos future days are stable.
 *
 * <p>The generator produces believable meteorological variables and derives the
 * severe-weather risk from them the way a forecaster would: instability (CAPE) +
 * moisture + wind shear combine to produce the outlook, with the strongest setups
 * being intrinsically rare.
 */
public final class WeatherGenerator {
	private WeatherGenerator() {
	}

	/**
	 * @param worldSeed the overworld seed
	 * @param day       minecraft day index (0-based)
	 */
	public static DailyForecast generate(long worldSeed, int day) {
		// Mix the day into the world seed so each day differs but is stable.
		long mixed = worldSeed ^ (day * 0x9E3779B97F4A7C15L) ^ 0x517CC1B727220A95L;
		Random r = new Random(mixed);

		// Seasonal-ish base temperature so days don't jump wildly.
		float seasonal = 72.0F + 16.0F * (float) Math.sin((day % 30) / 30.0 * Math.PI * 2.0);
		float tempF = clamp(seasonal + (r.nextFloat() * 2.0F - 1.0F) * 10.0F, 38.0F, 108.0F);

		// Dew point: moisture content. Hot days can be very humid.
		float spread = 3.0F + r.nextFloat() * 22.0F;
		float dewPointF = clamp(tempF - spread, 30.0F, tempF - 1.0F);

		// Relative humidity from the temperature / dew point spread.
		int humidity = clamp(Math.round(100.0F - (tempF - dewPointF) * 2.6F), 22, 98);

		// Pressure: low pressure correlates with stormy days.
		float pressureMb = 1013.0F + (r.nextFloat() * 2.0F - 1.0F) * 14.0F;

		// Steering wind.
		float windMph = 5.0F + r.nextFloat() * r.nextFloat() * 40.0F; // skew low
		float windDir = r.nextFloat() * 360.0F;

		// Instability (CAPE-like). Skewed distribution: mostly weak, occasionally strong.
		int cape = Math.round((float) (Math.pow(r.nextDouble(), 2.2) * 3400.0));

		// Vertical wind shear (knots).
		float shear = (float) (Math.pow(r.nextDouble(), 1.6) * 48.0);

		// Low pressure boosts effective instability a little.
		float lowPressureBoost = Math.max(0.0F, 1010.0F - pressureMb) * 4.0F;
		int effectiveCape = Math.round(cape + lowPressureBoost);

		// --- Derived probabilities (these drive the actual simulation) ---
		// Thunderstorm potential: needs instability AND moisture.
		float instabilityFactor = clamp01(effectiveCape / 2800.0F);
		float moistureFactor = clamp01((humidity - 35.0F) / 55.0F);
		float shearFactor = clamp01(shear / 40.0F);
		float lowPressureFactor = clamp01((1012.0F - pressureMb) / 12.0F);

		float stormBase = 0.08F + 0.72F * (0.55F * instabilityFactor + 0.25F * moistureFactor + 0.20F * lowPressureFactor);
		int stormProbability = clamp(Math.round(stormBase * 100.0F), 3, 97);

		// Tornado potential: shear + instability + moisture, with an extra rarity gate.
		float tornadoBase = 0.02F + 0.55F * (0.45F * shearFactor + 0.35F * instabilityFactor + 0.20F * moistureFactor);
		if (r.nextDouble() < 0.75) {
			tornadoBase *= 0.55F;
		}
		int tornadoProbability = clamp(Math.round(tornadoBase * 100.0F), 0, 65);

		// --- Risk rating from a composite score ---
		float score = 0.38F * instabilityFactor + 0.27F * shearFactor + 0.25F * moistureFactor + 0.10F * lowPressureFactor;
		RiskRating risk;
		if (score < 0.16F) {
			risk = RiskRating.NONE;
		} else if (score < 0.30F) {
			risk = RiskRating.MARGINAL;
		} else if (score < 0.42F) {
			risk = RiskRating.SLIGHT;
		} else if (score < 0.52F) {
			risk = RiskRating.ENHANCED;
		} else {
			// Moderate+ setups already rare; HIGH requires a near-top composite
			// plus a 15% gate so HIGH days are extremely rare.
			if (score < 0.62F || r.nextDouble() > 0.15) {
				risk = RiskRating.MODERATE;
			} else {
				risk = RiskRating.HIGH;
			}
		}

		List<String> lines = buildForecastLines(effectiveCape, shear, humidity, stormProbability, tornadoProbability, risk);

		return new DailyForecast(day, tempF, dewPointF, humidity, pressureMb, windMph, windDir,
			effectiveCape, shear, stormProbability, tornadoProbability, risk, lines);
	}

	private static List<String> buildForecastLines(int cape, float shear, int humidity, int stormProb, int tornadoProb, RiskRating risk) {
		List<String> lines = new ArrayList<>(3);
		if (risk.ordinal() <= 1) {
			lines.add("Very little severe weather expected.");
			if (cape > 900) {
				lines.add("Isolated thunderstorms possible.");
			} else {
				lines.add("Scattered showers possible.");
			}
		} else if (risk == RiskRating.SLIGHT) {
			lines.add("Scattered thunderstorms, some strong.");
			if (tornadoProb >= 12) {
				lines.add("Isolated tornadoes cannot be ruled out.");
			} else {
				lines.add("Strong gusts likely.");
			}
		} else if (risk == RiskRating.ENHANCED) {
			lines.add("Numerous strong thunderstorms likely.");
			if (shear > 24.0F) {
				lines.add("Supercells likely.");
				if (tornadoProb >= 20) {
					lines.add("Tornadoes likely.");
				} else {
					lines.add("Tornadoes possible.");
				}
			} else {
				lines.add("Large hail and damaging winds possible.");
			}
		} else {
			lines.add("A significant severe thunderstorm outbreak is possible.");
			lines.add("Organized supercells expected.");
			if (tornadoProb >= 30) {
				lines.add("Numerous and potentially violent tornadoes expected.");
			} else {
				lines.add("Tornadoes possible, some strong.");
			}
			if (humidity > 70 && cape > 2200) {
				lines.add("Extremely large hail possible.");
			}
		}
		return lines;
	}

	private static float clamp01(float v) {
		return Math.max(0.0F, Math.min(1.0F, v));
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	private static float clamp(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}
}
