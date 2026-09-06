package com.tornadic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import com.tornadic.TornadicMod;
import com.tornadic.config.TornadicConfig;
import com.tornadic.saveddata.TornadicSavedData;
import com.tornadic.storm.Storm;
import com.tornadic.storm.StormType;
import com.tornadic.tornado.TornadoState;
import com.tornadic.weather.DailyForecast;
import com.tornadic.weather.RiskRating;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * /thermos            - today's atmospheric conditions and risk
 * /thermos tomorrow   - (or: /thermos 3) future forecast days
 * /weatherstorm [type] - spawn a storm for testing (op)
 * /spawn_tornado ef0..ef5 - spawn a test tornado (op)
 * /tornado_info       - details about the nearest tornado
 * /storm_info         - details about the nearest storm
 * /thermos debug on|off - toggle the debug HUD (op)
 */
public final class TornadicCommands {
	private TornadicCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("thermos")
			.executes(ctx -> thermos(ctx.getSource(), 0))
			.then(Commands.literal("tomorrow").executes(ctx -> thermos(ctx.getSource(), 1)))
			.then(Commands.argument("day", IntegerArgumentType.integer(0, 30))
				.executes(ctx -> thermos(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "day"))))
			.then(Commands.literal("debug")
				.requires(src -> src.hasPermission(2))
				.executes(ctx -> toggleDebug(ctx.getSource()))
				.then(Commands.literal("on").executes(ctx -> setDebug(ctx.getSource(), true)))
				.then(Commands.literal("off").executes(ctx -> setDebug(ctx.getSource(), false)))
			.then(Commands.literal("simtick")
				.executes(ctx -> simTick(ctx.getSource(), 200))
				.then(Commands.argument("ticks", IntegerArgumentType.integer(1, 2000))
					.executes(ctx -> simTick(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "ticks")))))))
;

		dispatcher.register(Commands.literal("weatherstorm")
			.requires(src -> src.hasPermission(2))
			.executes(ctx -> spawnStorm(ctx.getSource(), StormType.THUNDERSTORM))
			.then(Commands.argument("type", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
					new String[]{"cumulonimbus", "thunderstorm", "strong_thunderstorm", "supercell", "tornadic"}, builder))
				.executes(ctx -> {
					StormType type = parseStormType(StringArgumentType.getString(ctx, "type"));
					return type == null ? 0 : spawnStorm(ctx.getSource(), type);
				})));

		dispatcher.register(Commands.literal("spawn_tornado")
			.requires(src -> src.hasPermission(2))
			.executes(ctx -> spawnTornado(ctx.getSource(), 2))
			.then(Commands.argument("ef", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
					new String[]{"ef0", "ef1", "ef2", "ef3", "ef4", "ef5"}, builder))
				.executes(ctx -> spawnTornado(ctx.getSource(), efFromArg(StringArgumentType.getString(ctx, "ef"))))));

		dispatcher.register(Commands.literal("tornado_info").executes(ctx -> tornadoInfo(ctx.getSource())));
		dispatcher.register(Commands.literal("storm_info").executes(ctx -> stormInfo(ctx.getSource())));
	}

	private static int simTick(CommandSourceStack source, int ticks) {
		if (source.getServer() == null || source.getServer().overworld() == null) {
			return 0;
		}
		com.tornadic.saveddata.TornadicSavedData data =
			com.tornadic.saveddata.TornadicSavedData.getOrLoad(source.getServer());
		int stormsBefore = data.storms().size();
		int tornadoesBefore = data.tornadoes().size();
		data.forceTick(source.getServer().overworld(), ticks);
		int storms = data.storms().size();
		int tornadoes = data.tornadoes().size();
		final int sb = stormsBefore, tb = tornadoesBefore;
		source.sendSuccess(() -> Component.literal(
			"Tornadic: ran " + ticks + " forced simulation ticks. Storms " + sb + " -> " + storms
				+ ", tornadoes " + tb + " -> " + tornadoes + ".")
			.withStyle(ChatFormatting.AQUA), true);
		return 1;
	}

	// ------------------------------------------------------------------

	private static int thermos(CommandSourceStack source, int dayOffset) {
		ServerLevel world = source.getServer().overworld();
		DailyForecast forecast = TornadicSavedData.getOrLoad(source.getServer()).currentForecast(world);
		int day = dayOffset == 0 ? forecast.day() : TornadicSavedData.currentDay(world) + dayOffset;
		DailyForecast shown = com.tornadic.weather.WeatherGenerator.generate(
			seedOf(world), day);
		if (dayOffset == 0) {
			shown = forecast;
		}

		RiskRating risk = shown.risk();
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("THERMOS — DAY " + (shown.day() + 1)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		lines.add(line("Temperature:", shown.tempF() + "°F"));
		lines.add(line("Dew Point:", shown.dewPointF() + "°F"));
		lines.add(line("Humidity:", shown.humidity() + "%"));
		lines.add(line("Pressure:", shown.pressureMb() + " hPa"));
		lines.add(line("Wind:", shown.windMph() + " mph " + compass(shown.windDir())));
		lines.add(instabilityLine(shown.cape()));
		lines.add(line("Wind Shear:", shown.shear() + " kt"));
		lines.add(line("Storm Probability:", shown.stormProbability() + "%"));
		lines.add(line("Tornado Probability:", shown.tornadoProbability() + "%"));
		lines.add(Component.empty());
		lines.add(Component.literal("SEVERE WEATHER RISK:").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
			.append(" ").append(risk.asComponent().withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true))));
		lines.add(Component.literal(risk.description()).withStyle(risk.color()));
		lines.add(Component.empty());
		lines.add(Component.literal("Forecast:").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		for (String l : shown.forecastLines()) {
			lines.add(Component.literal(l).withStyle(ChatFormatting.GRAY));
		}
		if (dayOffset > 0) {
			lines.add(Component.literal("(forecast for day +" + dayOffset + ")").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
		}
		for (Component c : lines) {
			source.sendSuccess(() -> c, false);
		}
		return 1;
	}

	private static long seedOf(ServerLevel world) {
		// Same deterministic derivation as TornadicSavedData.worldSeed: hash of the
		// save-folder id (stable across restarts, unique per world).
		String id = ((net.minecraft.world.level.storage.ServerLevelData) world.getLevelData()).getLevelName();
		long seed = 1125899906842597L;
		for (int i = 0; i < id.length(); i++) {
			seed = 31 * seed + id.charAt(i);
		}
		return seed == 0L ? 1L : seed;
	}

	private static Component line(String label, Object value) {
		String text = value instanceof Float f ? String.format("%.0f", f) : String.valueOf(value);
		return Component.literal(label + " ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(text).withStyle(ChatFormatting.WHITE));
	}

	private static Component instabilityLine(int cape) {
		ChatFormatting color;
		String text;
		if (cape < 500) {
			text = "LOW";
			color = ChatFormatting.GREEN;
		} else if (cape < 1200) {
			text = "MODERATE";
			color = ChatFormatting.YELLOW;
		} else if (cape < 2200) {
			text = "HIGH";
			color = ChatFormatting.GOLD;
		} else {
			text = "EXTREME";
			color = ChatFormatting.RED;
		}
		return Component.literal("Instability: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(text + " (" + cape + " J/kg)").withStyle(color));
	}

	private static String compass(float dirDeg) {
		float deg = dirDeg % 360f;
		if (deg < 0) {
			deg += 360f;
		}
		String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
		return dirs[((int) Math.round(deg / 45.0f)) % 8];
	}

	private static int toggleDebug(CommandSourceStack source) {
		TornadicConfig.debugHud = !TornadicConfig.debugHud;
		source.sendSuccess(() -> Component.literal("Debug HUD " + (TornadicConfig.debugHud ? "enabled" : "disabled") + ".")
			.withStyle(ChatFormatting.GRAY), true);
		return 1;
	}

	private static int setDebug(CommandSourceStack source, boolean on) {
		TornadicConfig.debugHud = on;
		source.sendSuccess(() -> Component.literal("Debug HUD " + (on ? "enabled" : "disabled") + ".")
			.withStyle(ChatFormatting.GRAY), true);
		return 1;
	}


	// ------------------------------------------------------------------

	private static StormType parseStormType(String raw) {
		return switch (raw.toLowerCase()) {
			case "cumulonimbus", "cbs" -> StormType.CUMULONIMBUS;
			case "thunderstorm", "ts" -> StormType.THUNDERSTORM;
			case "strong_thunderstorm", "strong", "s" -> StormType.STRONG_THUNDERSTORM;
			case "supercell", "sc" -> StormType.SUPERCELL;
			case "tornadic", "tornadic_supercell", "tsc" -> StormType.TORNADIC_SUPERCELL;
			default -> null;
		};
	}

	private static int spawnStorm(CommandSourceStack source, StormType type) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception e) {
			source.sendFailure(Component.literal("Requires a player."));
			return 0;
		}
		ServerLevel world = source.getServer().overworld();
		double angle = Math.random() * Math.PI * 2.0;
		double dist = 450 + Math.random() * 200;
		double x = player.getX() + Math.cos(angle) * dist;
		double z = player.getZ() + Math.sin(angle) * dist;
		com.tornadic.saveddata.TornadicSavedData data = TornadicSavedData.getOrLoad(source.getServer());
		Storm storm = new Storm(data.newId(), x, z, (float) (Math.random() * Math.PI * 2.0));
		storm.type = type;
		storm.intensity = 0.3f;
		storm.radius = 110f;
		storm.speed = 0.2f;
		storm.rotation = (float) ((Math.random() - 0.5) * 2.0) * (type.ordinal() >= StormType.STRONG_THUNDERSTORM.ordinal() ? 0.9f : 0.3f);
		storm.hail = type.canHaveHail() && Math.random() < 0.6;
		storm.hailSize = 0.5f + (float) Math.random() * 0.6f;
		data.storms().add(storm);
		data.setDirty();
		source.sendSuccess(() -> Component.literal("Spawned " + type.displayName() + " at " + (int) x + ", " + (int) z)
			.withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	private static int efFromArg(String raw) {
		String s = raw.toLowerCase();
		if (s.startsWith("ef")) {
			s = s.substring(2);
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static int spawnTornado(CommandSourceStack source, int ef) {
		if (ef < 0 || ef > 5) {
			source.sendFailure(Component.literal("Usage: /spawn_tornado ef0..ef5"));
			return 0;
		}
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception e) {
			source.sendFailure(Component.literal("Requires a player."));
			return 0;
		}
		ServerLevel world = source.getServer().overworld();
		double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
		// Keep the command tornado outside immediate contact, but inside the player's
		// guaranteed loaded/tracked area so the entity and first visual sync cannot fail.
		double x = player.getX() + Math.cos(angle) * 64;
		double z = player.getZ() + Math.sin(angle) * 64;
		boolean spawned = TornadicSavedData.getOrLoad(source.getServer()).spawnTestTornado(world, x, z, ef);
		if (!spawned) {
			source.sendFailure(Component.literal("Could not spawn tornado: target chunk is not loaded."));
			return 0;
		}
		source.sendSuccess(() -> Component.literal("Spawned " + com.tornadic.tornado.TornadoIntensity.fromEf(ef).label()
				+ " tornado with tornadic supercell at " + (int) x + ", " + (int) z)
			.withStyle(ChatFormatting.GREEN), false);
		return 1;
	}


	// ------------------------------------------------------------------

	private static int tornadoInfo(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception e) {
			source.sendFailure(Component.literal("Requires a player."));
			return 0;
		}
		TornadoState t = TornadicSavedData.getOrLoad(source.getServer())
			.nearestTornado(player.getX(), player.getZ(), 2048);
		if (t == null) {
			source.sendSuccess(() -> Component.literal("No tornado within 2048 blocks.").withStyle(ChatFormatting.GRAY), false);
			return 1;
		}
		String compass = compass((float) (t.heading * 180.0 / Math.PI));
		source.sendSuccess(() -> Component.empty()
			.append(Component.literal("TORNADO").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
			.append(Component.literal("\nIntensity: ").withStyle(ChatFormatting.GRAY))
			.append(t.intensityScale().asComponent())
			.append(Component.literal("  (peak " + com.tornadic.tornado.TornadoIntensity.fromEf((int) t.peakEf).label() + ")").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\nPosition: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal((int) t.x + ", " + (int) t.y + ", " + (int) t.z).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nMovement: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(compass).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nCore wind: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.0f m/s", t.windMs())).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nPressure deficit: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.0f hPa", t.pressureDeficitHpa())).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nFunnel radius: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.0f blocks", t.funnelRadius())).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nAge: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.0fs", t.age / 20.0)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nPath length: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.0f blocks", t.pathLength())).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nBlocks destroyed: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(t.blocksBroken)).withStyle(ChatFormatting.WHITE)), false);
		return 1;
	}

	private static int stormInfo(CommandSourceStack source) {
		ServerPlayer player;
		try {
			player = source.getPlayerOrException();
		} catch (Exception e) {
			source.sendFailure(Component.literal("Requires a player."));
			return 0;
		}
		Storm s = TornadicSavedData.getOrLoad(source.getServer())
			.nearestStorm(player.getX(), player.getZ(), 4096);
		if (s == null) {
			source.sendSuccess(() -> Component.literal("No storm within 4096 blocks.").withStyle(ChatFormatting.GRAY), false);
			return 1;
		}
		int tornadoEf = -1;
		var data = TornadicSavedData.getOrLoad(source.getServer());
		for (TornadoState t : data.tornadoes()) {
			if (t.stormId == s.id && !t.isDissipated()) {
				tornadoEf = t.currentEf();
				break;
			}
		}
		final int efSnapshot = tornadoEf;
		source.sendSuccess(() -> Component.empty()
			.append(s.type.asComponent().withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true)))
			.append(Component.literal("\nPosition: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal((int) s.x + ", " + (int) s.z).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nMovement: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(compass(s.dir)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nIntensity: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.0f%%", s.intensity * 100)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nRadius: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.0f blocks", s.radius)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nRotation (mesocyclone): ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.2f", s.rotation)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("\nHail: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(s.hail ? "yes (" + String.format("%.2f", s.hailSize) + " size)" : "no")
				.withStyle(s.hail ? ChatFormatting.AQUA : ChatFormatting.WHITE))
			.append(Component.literal("\nTornado: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(efSnapshot >= 0 ? com.tornadic.tornado.TornadoIntensity.fromEf(efSnapshot).label() : "none")
				.withStyle(efSnapshot >= 0 ? ChatFormatting.RED : ChatFormatting.WHITE)), false);
		return 1;
	}
}
