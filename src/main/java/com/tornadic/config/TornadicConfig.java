package com.tornadic.config;

import com.tornadic.TornadicMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Simple key=value configuration (config/tornadic.cfg).
 *
 * <p>The file is created on first run with all defaults, so server owners can edit it
 * and restart the server to apply changes.
 */
public final class TornadicConfig {
	private TornadicConfig() {
	}

	public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("tornadic.cfg");

	// Storms
	public static double stormFrequency = 1.0;
	public static int maxStorms = 6;
	public static int stormMinPlayerDistance = 300;
	public static int stormMaxPlayerDistance = 4000;

	// Tornadoes
	public static boolean allowTornadoes = true;
	public static double tornadoFrequency = 1.0;
	public static int maxTornadoIntensity = 5;
	public static int maxTornadoes = 2;

	// Hail
	public static double hailFrequency = 1.0;
	public static int maxHailHitsPerStormTick = 6;

	// World damage / debris
	public static boolean worldDamageEnabled = true;
	public static int maxBlocksBrokenPerTick = 4;
	public static int maxBlocksBrokenPerTornado = 4000;
	public static boolean debrisEnabled = true;
	public static int maxDebrisItems = 32;
	public static List<String> protectedBlocks = new ArrayList<>(Arrays.asList("minecraft:bedrock", "minecraft:obsidian"));

	// Wind / simulation
	public static double windStrength = 1.0;
	public static int simTickInterval = 1;
	public static int maxSimParticles = 160;

	// Warnings / info
	public static boolean warningsEnabled = true;
	public static int warningRadius = 2400;

	// Equipment
	public static boolean chaserVehiclesEnabled = true;
	public static int radarRange = 3200;

	// Debug
	public static boolean debugHud = false;

	public static void load() {
		try {
			if (!Files.exists(CONFIG_PATH)) {
				Files.createDirectories(CONFIG_PATH.getParent());
				writeDefaults();
			}
			for (String line : Files.readAllLines(CONFIG_PATH)) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				int eq = trimmed.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				String key = trimmed.substring(0, eq).trim();
				String value = trimmed.substring(eq + 1).trim();
				apply(key, value);
			}
		} catch (IOException e) {
			TornadicMod.LOGGER.error("Could not read tornadic config, using defaults", e);
		}
	}

	public static void writeDefaults() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("# Tornadic configuration. Edit values and restart the server.\n");
			sb.append("#\n# --- Storms ---\n");
			sb.append("stormFrequency=").append(stormFrequency).append("   # multiplier for storm spawn chance\n");
			sb.append("maxStorms=").append(maxStorms).append("   # maximum simultaneous storms\n");
			sb.append("stormMinPlayerDistance=").append(stormMinPlayerDistance).append("   # storms never spawn closer to a player (blocks)\n");
			sb.append("stormMaxPlayerDistance=").append(stormMaxPlayerDistance).append("   # storms only spawn within this distance of a player (blocks)\n");
			sb.append("#\n# --- Tornadoes ---\n");
			sb.append("allowTornadoes=").append(allowTornadoes).append("   # whether tornadoes can form naturally\n");
			sb.append("tornadoFrequency=").append(tornadoFrequency).append("   # multiplier for tornado formation chance\n");
			sb.append("maxTornadoIntensity=").append(maxTornadoIntensity).append("   # absolute EF cap (0-5)\n");
			sb.append("maxTornadoes=").append(maxTornadoes).append("   # maximum simultaneous tornadoes\n");
			sb.append("#\n# --- Hail ---\n");
			sb.append("hailFrequency=").append(hailFrequency).append("   # multiplier for hail chance in supercells\n");
			sb.append("maxHailHitsPerStormTick=").append(maxHailHitsPerStormTick).append("   # hail impact budget per storm per effect tick\n");
			sb.append("#\n# --- World damage / debris ---\n");
			sb.append("worldDamageEnabled=").append(worldDamageEnabled).append("   # tornadoes may break blocks\n");
			sb.append("maxBlocksBrokenPerTick=").append(maxBlocksBrokenPerTick).append("   # hard per-tick block destruction budget per tornado\n");
			sb.append("maxBlocksBrokenPerTornado=").append(maxBlocksBrokenPerTornado).append("   # total block destruction budget per tornado\n");
			sb.append("debrisEnabled=").append(debrisEnabled).append("   # tornadoes may pick up and fling items\n");
			sb.append("maxDebrisItems=").append(maxDebrisItems).append("   # maximum debris item entities flung around by one tornado\n");
			sb.append("protectedBlocks=").append(String.join(",", protectedBlocks)).append("   # comma separated block ids never broken\n");
			sb.append("#\n# --- Wind / simulation ---\n");
			sb.append("windStrength=").append(windStrength).append("   # global wind force multiplier\n");
			sb.append("simTickInterval=").append(simTickInterval).append("   # run the simulation every N server ticks (1 = every tick)\n");
			sb.append("maxSimParticles=").append(maxSimParticles).append("   # per-frame particle budget for weather visuals\n");
			sb.append("#\n# --- Warnings / info ---\n");
			sb.append("warningsEnabled=").append(warningsEnabled).append("   # issue tornado warnings in chat\n");
			sb.append("warningRadius=").append(warningRadius).append("   # how far players are warned (blocks)\n");
			sb.append("#\n# --- Equipment ---\n");
			sb.append("chaserVehiclesEnabled=").append(chaserVehiclesEnabled).append("   # allow the storm chaser vehicle\n");
			sb.append("radarRange=").append(radarRange).append("   # storm radar range (blocks)\n");
			sb.append("#\n# --- Debug ---\n");
			sb.append("debugHud=").append(debugHud).append("   # show the weather debug HUD (also toggleable in-game)\n");
			Files.writeString(CONFIG_PATH, sb.toString());
		} catch (IOException e) {
			TornadicMod.LOGGER.error("Could not write tornadic config defaults", e);
		}
	}

	private static void apply(String key, String value) {
		try {
			switch (key) {
				case "stormFrequency" -> stormFrequency = Double.parseDouble(value);
				case "maxStorms" -> maxStorms = clampInt(Integer.parseInt(value), 1, 16);
				case "stormMinPlayerDistance" -> stormMinPlayerDistance = clampInt(Integer.parseInt(value), 0, 4000);
				case "stormMaxPlayerDistance" -> stormMaxPlayerDistance = clampInt(Integer.parseInt(value), 100, 12000);
				case "allowTornadoes" -> allowTornadoes = Boolean.parseBoolean(value);
				case "tornadoFrequency" -> tornadoFrequency = Double.parseDouble(value);
				case "maxTornadoIntensity" -> maxTornadoIntensity = clampInt(Integer.parseInt(value), 0, 5);
				case "maxTornadoes" -> maxTornadoes = clampInt(Integer.parseInt(value), 0, 8);
				case "hailFrequency" -> hailFrequency = Double.parseDouble(value);
				case "maxHailHitsPerStormTick" -> maxHailHitsPerStormTick = clampInt(Integer.parseInt(value), 0, 32);
				case "worldDamageEnabled" -> worldDamageEnabled = Boolean.parseBoolean(value);
				case "maxBlocksBrokenPerTick" -> maxBlocksBrokenPerTick = clampInt(Integer.parseInt(value), 0, 32);
				case "maxBlocksBrokenPerTornado" -> maxBlocksBrokenPerTornado = clampInt(Integer.parseInt(value), 0, 100000);
				case "debrisEnabled" -> debrisEnabled = Boolean.parseBoolean(value);
				case "maxDebrisItems" -> maxDebrisItems = clampInt(Integer.parseInt(value), 0, 256);
				case "protectedBlocks" -> protectedBlocks = Arrays.asList(value.split(","));
				case "windStrength" -> windStrength = Double.parseDouble(value);
				case "simTickInterval" -> simTickInterval = clampInt(Integer.parseInt(value), 1, 20);
				case "maxSimParticles" -> maxSimParticles = clampInt(Integer.parseInt(value), 0, 1000);
				case "warningsEnabled" -> warningsEnabled = Boolean.parseBoolean(value);
				case "warningRadius" -> warningRadius = clampInt(Integer.parseInt(value), 256, 12000);
				case "chaserVehiclesEnabled" -> chaserVehiclesEnabled = Boolean.parseBoolean(value);
				case "radarRange" -> radarRange = clampInt(Integer.parseInt(value), 256, 8000);
				case "debugHud" -> debugHud = Boolean.parseBoolean(value);
				default -> TornadicMod.LOGGER.warn("Unknown tornadic config key: {}", key);
			}
		} catch (NumberFormatException e) {
			TornadicMod.LOGGER.warn("Invalid tornadic config value for {}: {}", key, value);
		}
	}

	private static int clampInt(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}
