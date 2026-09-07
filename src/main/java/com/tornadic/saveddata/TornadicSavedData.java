package com.tornadic.saveddata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import com.tornadic.TornadicMod;
import com.tornadic.config.TornadicConfig;
import com.tornadic.network.StormSyncPayload;
import com.tornadic.network.ThunderPayload;
import com.tornadic.network.TornadoSyncPayload;
import com.tornadic.network.WeatherSyncPayload;
import com.tornadic.storm.Storm;
import com.tornadic.storm.StormType;
import com.tornadic.tornado.TornadoState;
import com.tornadic.tornado.TornadoEntity;
import com.tornadic.tornado.TornadoPhysics;
import com.tornadic.weather.DailyForecast;
import com.tornadic.weather.WeatherGenerator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The server-authoritative severe-weather simulation, persisted per overworld.
 *
 * <p>Owns all storms and tornadoes, advances their lifecycles, applies their effects
 * (rain, lightning, hail, wind, world damage), broadcasts state to clients and issues
 * warnings. Saved via {@link SavedData} so the whole weather state survives reloads.
 */
public class TornadicSavedData extends SavedData {
	private static final String NAME = "tornadic_weather";

	public final List<Storm> storms = new ArrayList<>();
	public final List<TornadoState> tornadoes = new ArrayList<>();

	public int lastDay = -1;
	private long nextId = 1L;

	// Daily stats (shown in the storm notebook).
	public int stormsToday;
	public int tornadoesToday;
	public int maxEfToday = -1;
	public int lightningToday;
	public int hailEventsToday;

	private float lastRain = -1F;
	private float lastThunder = -1F;
	private int tickCounter;
	private boolean entitiesRecreated;

	// Transient.
	private ServerLevel world;

	public static TornadicSavedData getOrLoad(MinecraftServer server) {
		DimensionDataStorage storage = server.overworld().getDataStorage();
		return storage.computeIfAbsent(
			new SavedData.Factory<>(TornadicSavedData::new, TornadicSavedData::load, net.minecraft.util.datafix.DataFixTypes.LEVEL), NAME);
	}

	private static TornadicSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
		TornadicSavedData data = new TornadicSavedData();
		data.lastDay = tag.getInt("LastDay");
		data.nextId = tag.getLong("NextId");
		data.stormsToday = tag.getInt("StormsToday");
		data.tornadoesToday = tag.getInt("TornadoesToday");
		data.maxEfToday = tag.contains("MaxEfToday") ? tag.getInt("MaxEfToday") : -1;
		data.lightningToday = tag.getInt("LightningToday");
		data.hailEventsToday = tag.getInt("HailToday");

		ListTag stormList = tag.getList("Storms", 10);
		for (int i = 0; i < stormList.size(); i++) {
			Storm storm = new Storm(0, 0, 0, 0);
			storm.readNbt(stormList.getCompound(i));
			data.storms.add(storm);
		}
		ListTag tornadoList = tag.getList("Tornadoes", 10);
		for (int i = 0; i < tornadoList.size(); i++) {
			TornadoState state = new TornadoState(0, 0, 0, -1, 0);
			state.readNbt(tornadoList.getCompound(i));
			data.tornadoes.add(state);
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putInt("LastDay", lastDay);
		tag.putLong("NextId", nextId);
		tag.putInt("StormsToday", stormsToday);
		tag.putInt("TornadoesToday", tornadoesToday);
		tag.putInt("MaxEfToday", maxEfToday);
		tag.putInt("LightningToday", lightningToday);
		tag.putInt("HailToday", hailEventsToday);

		ListTag stormList = new ListTag();
		for (Storm storm : storms) {
			stormList.add(storm.writeNbt());
		}
		tag.put("Storms", stormList);

		ListTag tornadoList = new ListTag();
		for (TornadoState state : tornadoes) {
			tornadoList.add(state.writeNbt());
		}
		tag.put("Tornadoes", tornadoList);
		return tag;
	}

	public long newId() {
		return nextId++;
	}

	public DailyForecast currentForecast(ServerLevel world) {
		return WeatherGenerator.generate(worldSeed(world), currentDay(world));
	}

	public static int currentDay(ServerLevel world) {
		long dayTime = world.getLevelData().getDayTime();
		return (int) ((dayTime + 6000L) / 24000L);
	}

	private long worldSeed(ServerLevel world) {
		// The persisted world seed: stable across restarts and unique per world, so
		// daily forecasts are deterministic per world/day.
		long seed = world.getSeed();
		return seed == 0L ? 1L : seed;
	}

	/** Called every server tick from the END_WORLD_TICK event (overworld only). */
	public void tick(ServerLevel world) {
		this.world = world;
		if (world.isClientSide) {
			return;
		}
		if (world.getServer().getPlayerList().getPlayers().isEmpty()) {
			// Nothing to simulate for; keep vanilla behavior.
			return;
		}

			tickBody(world);
	}

	/** Runs the full simulation step (spawn storms, tick storms/tornadoes, broadcast). */
	public void tickBody(ServerLevel world) {
		// Recreate tornado entities after a world load.
		if (!entitiesRecreated) {
			entitiesRecreated = true;
			recreateTornadoEntities(world);
		}

		int day = currentDay(world);
		if (day != lastDay) {
			lastDay = day;
			stormsToday = 0;
			tornadoesToday = 0;
			maxEfToday = -1;
			lightningToday = 0;
			hailEventsToday = 0;
			setDirty();
		}

		tickCounter++;
		int interval = Math.max(1, TornadicConfig.simTickInterval);
		if (tickCounter % interval != 0) {
			return;
		}

		DailyForecast forecast = currentForecast(world);

		trySpawnStorms(world, forecast);
		tickStorms(world, forecast);
		tickTornadoes(world, forecast);
		broadcastWeather(world, forecast);

		setDirty();
	}

	/**
	 * Debug/test hook (permission gated): runs the simulation body N times right now,
	 * even without players online. Used to exercise the full storm/tornado pipeline.
	 */
	public void forceTick(ServerLevel world, int times) {
		for (int i = 0; i < Math.max(1, Math.min(times, 2000)); i++) {
			tickBody(world);
		}
	}

	private void recreateTornadoEntities(ServerLevel world) {
		for (TornadoState state : tornadoes) {
			state.entity = null;
			if (!state.isDissipated() && world.isLoaded(new BlockPos((int) state.x, 64, (int) state.z))) {
				TornadoEntity entity = TornadoEntity.create(TornadicMod.TORNADO_TYPE, world, state.id,
					state.x, state.y > 0 ? state.y : 64, state.z);
				world.addFreshEntity(entity);
				state.entity = entity;
			}
		}
	}

	// ------------------------------------------------------------------
	// Storms
	// ------------------------------------------------------------------

	private void trySpawnStorms(ServerLevel world, DailyForecast forecast) {
		if (storms.size() >= TornadicConfig.maxStorms) {
			return;
		}
		double perTick = 0.00055 * (forecast.stormProbability() / 100.0)
			* forecast.risk().stormRateMultiplier() * TornadicConfig.stormFrequency;
		// Ensure a new session demonstrates that Tornadic is alive without forcing
		// severe weather: after 30 seconds an empty simulation may form one ordinary
		// developing cumulonimbus. All later storms remain forecast-driven.
		boolean initialDevelopingCell = storms.isEmpty() && tickCounter == 600;
		if (!initialDevelopingCell && world.getRandom().nextFloat() >= (perTick * intervalFactor())) {
			return;
		}

		// Spawn relative to a random player, at a reasonable distance.
		// Without players (e.g. /thermos debug simtick on a bare server) anchor to
		// the world spawn instead.
		List<ServerPlayer> players = world.getServer().getPlayerList().getPlayers();
		double ax, az;
		if (players.isEmpty()) {
			BlockPos sp = world.getSharedSpawnPos();
			ax = sp.getX();
			az = sp.getZ();
		} else {
			ServerPlayer anchor = players.get(world.getRandom().nextInt(players.size()));
			ax = anchor.getX();
			az = anchor.getZ();
		}
		for (int attempt = 0; attempt < 6; attempt++) {
			double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
			// Storms are simulation objects, not block entities: they may safely form
			// outside loaded chunks. The old loaded-chunk check made natural spawning
			// mathematically impossible with the default 300-block minimum and common
			// 8-10 chunk view distances. Bias new cells into a visible chasing range.
			double visibleMax = Math.max(TornadicConfig.stormMinPlayerDistance + 80.0,
				Math.min(TornadicConfig.stormMaxPlayerDistance, 720.0));
			double dist = TornadicConfig.stormMinPlayerDistance
				+ world.getRandom().nextDouble() * (visibleMax - TornadicConfig.stormMinPlayerDistance);
			double x = ax + Math.cos(angle) * dist;
			double z = az + Math.sin(angle) * dist;
			// Keep fresh storms away from every player.
			boolean tooClose = false;
			for (ServerPlayer p : players) {
				if (p.distanceToSqr(new Vec3(x, p.getY(), z)) < 256 * 256) {
					tooClose = true;
					break;
				}
			}
			if (tooClose) {
				continue;
			}
			Storm storm = new Storm(newId(), x, z, (float) (forecast.windDir() * Math.PI / 180.0));
			storm.speed = 0.14f + world.getRandom().nextFloat() * 0.2f;
			storm.radius = 90f + world.getRandom().nextFloat() * 60f;
			storm.type = StormType.CUMULONIMBUS;
			storm.intensity = 0.2f;
			// Does this day favor organized convection (supercells)?
			float org = (float) ((forecast.cape() / 3400.0) * 0.5 + (forecast.shear() / 48.0) * 0.5);
			storm.rotation = world.getRandom().nextFloat() * (0.2f + 0.8f * org)
				* (world.getRandom().nextBoolean() ? 1 : -1) * (0.5f + org * 0.5f);
			storm.hail = storm.rotation * storm.rotation > 0.25 && world.getRandom().nextFloat()
				< 0.5f * TornadicConfig.hailFrequency;
			storm.hailSize = 0.3f + world.getRandom().nextFloat() * 0.8f * (0.5f + org);
			storms.add(storm);
			stormsToday++;
			return;
		}
	}

	private float intervalFactor() {
		return (float) Math.max(1, TornadicConfig.simTickInterval);
	}

	private void tickStorms(ServerLevel world, DailyForecast forecast) {
		Iterator<Storm> it = storms.iterator();
		while (it.hasNext()) {
			Storm storm = it.next();
			storm.tickMovement();

			boolean anyoneNear = false;
			double minPlayerDist = Double.MAX_VALUE;
			for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
				double d = storm.distanceTo(p.getX(), p.getZ());
				minPlayerDist = Math.min(minPlayerDist, d);
				if (d < TornadicConfig.stormMaxPlayerDistance) {
					anyoneNear = true;
				}
			}
			if (!anyoneNear) {
				storm.farTicks++;
				if (storm.farTicks > 6000) {
					it.remove();
					continue;
				}
				// Far storms still evolve, just without local effects.
			} else {
				storm.farTicks = 0;
			}

			developStorm(world, forecast, storm);

			if (anyoneNear) {
				applyStormEffects(world, forecast, storm);
				// Tornado formation.
				if (TornadicConfig.allowTornadoes && storm.type == StormType.TORNADIC_SUPERCELL
					&& storm.intensity > 0.55 && countActiveTornadoes() < TornadicConfig.maxTornadoes) {
					double perSecond = (0.045 + 0.11 * storm.rotation * storm.rotation)
						* (0.35 + 0.65 * forecast.tornadoProbability() / 100.0)
						* TornadicConfig.tornadoFrequency;
					if (world.getRandom().nextFloat() < (perSecond / 20.0)) {
						trySpawnTornado(world, storm);
					}
				}
			}

			// Dissipation.
			int maxAge = 1800 + world.getRandom().nextInt(1400);
			if (storm.age > maxAge) {
				storm.intensity -= 0.004f;
			}
			if (storm.intensity <= 0.02f) {
				dissipateStorm(world, storm);
				it.remove();
			}
		}
	}

	private void developStorm(ServerLevel world, DailyForecast forecast, Storm storm) {
		// Slowly approach the intensity of its type.
		float target = storm.type.intensityFactor();
		storm.intensity += (target - storm.intensity) * 0.004f;
		storm.rainIntensity = storm.intensity * (0.75f + world.getRandom().nextFloat() * 0.25f);

		// Stage progression. Quality of the day gates organization.
		float quality = (float) ((forecast.cape() / 3400.0) * 0.5 + (forecast.shear() / 48.0) * 0.5);
		if (storm.age > 500) {
			double advanceChance = 0.0006 * (0.3 + 0.7 * quality);
			if (storm.type == StormType.TORNADIC_SUPERCELL) {
				advanceChance = 0.0;
			}
			if (world.getRandom().nextFloat() < advanceChance) {
				StormType next = StormType.values()[Math.min(storm.type.ordinal() + 1, StormType.values().length - 1)];
				// Weaker days rarely organize past strong thunderstorms.
				if (next.ordinal() >= StormType.SUPERCELL.ordinal() && quality < 0.28) {
					return;
				}
				storm.type = next;
				storm.radius += 12f + world.getRandom().nextFloat() * 14f;
				if (next.canHaveHail() && !storm.hail && world.getRandom().nextDouble()
					< 0.35 * TornadicConfig.hailFrequency) {
					storm.hail = true;
				}
			}
		}
	}

	private void applyStormEffects(ServerLevel world, DailyForecast forecast, Storm storm) {
		// Lightning: rare, with cooldown.
		storm.lightningCooldown--;
		float lightningRate = 0.0012f * storm.intensity * storm.type.intensityFactor();
		if (storm.type.ordinal() >= StormType.SUPERCELL.ordinal()) {
			lightningRate *= 1.8f;
		}
		if (storm.lightningCooldown <= 0 && world.getRandom().nextFloat() < lightningRate) {
			strikeLightning(world, storm);
		}

		// Hail impacts (supercells only).
		if (storm.hail && storm.type.canHaveHail() && tickCounter % 6 == 0) {
			applyHail(world, storm);
		}

		// Wind push on entities (rate limited).
		if (tickCounter % 4 == 0 && storm.intensity > 0.25) {
			double reach = storm.radius;
			AABB area = new AABB(storm.x - reach, 0, storm.z - reach, storm.x + reach, 256, storm.z + reach);
			List<LivingEntity> victims = world.getEntitiesOfClass(LivingEntity.class, area);
			for (int i = 0; i < Math.min(victims.size(), 24); i++) {
				LivingEntity e = victims.get(i);
				double dx = e.getX() - storm.x;
				double dz = e.getZ() - storm.z;
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist > reach) {
					continue;
				}
				float fall = (float) (1.0 - dist / reach);
				double push = 0.02 * fall * storm.intensity * TornadicConfig.windStrength
					* (e instanceof net.minecraft.world.entity.player.Player ? 0.5 : 1.0);
				float dir = storm.dir;
				Vec3 vel = e.getDeltaMovement();
				e.setDeltaMovement(vel.add(Math.cos(dir) * push * 2.0, 0, Math.sin(dir) * push * 2.0));
			}
		}
	}

	private void strikeLightning(ServerLevel world, Storm storm) {
		double dx = (world.getRandom().nextDouble() - 0.5) * storm.radius * 1.4;
		double dz = (world.getRandom().nextDouble() - 0.5) * storm.radius * 1.4;
		int lx = (int) (storm.x + dx);
		int lz = (int) (storm.z + dz);
		if (!world.isLoaded(new BlockPos(lx, 64, lz))) {
			return;
		}
		BlockPos pos = new BlockPos(lx, 0, lz);
		int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		LightningBolt bolt = new LightningBolt(
			(net.minecraft.world.entity.EntityType<? extends LightningBolt>) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(
				net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
					net.minecraft.resources.ResourceLocation.withDefaultNamespace("lightning_bolt"))),
			world);
		bolt.setPos(lx + 0.5, groundY + 8.0, lz + 0.5);
		world.addFreshEntity(bolt);
		storm.lightningCooldown = 24 + world.getRandom().nextInt(160);
		lightningToday++;

		// Thunder with distance-based delay for players.
		for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
			double dist = Math.sqrt(
				(p.getX() - lx) * (p.getX() - lx) + (p.getZ() - lz) * (p.getZ() - lz));
			if (dist > 1800) {
				continue;
			}
			float volume = (float) (1.4 * storm.intensity * (1.0 - dist / 2200.0));
			if (volume < 0.05f) {
				continue;
			}
			float pitch = 0.6f + world.getRandom().nextFloat() * 0.7f;
			int delay = (int) (dist / 16.0); // ~speed of sound in ticks
			p.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
				new ThunderPayload(lx, lz, Math.min(1.0f, volume), pitch, delay)));
		}
	}

	private void applyHail(ServerLevel world, Storm storm) {
		int hits = 1 + (int) (world.getRandom().nextFloat() * TornadicConfig.maxHailHitsPerStormTick * storm.intensity);
		for (int i = 0; i < hits; i++) {
			double dx = (world.getRandom().nextDouble() - 0.5) * storm.radius * 1.2;
			double dz = (world.getRandom().nextDouble() - 0.5) * storm.radius * 1.2;
			int hx = (int) (storm.x + dx);
			int hz = (int) (storm.z + dz);
			if (!world.isLoaded(new BlockPos(hx, 64, hz))) {
				continue;
			}
			// Only bother near players (relevance + performance).
			boolean nearPlayer = false;
			for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
				if (p.distanceToSqr(new Vec3(hx, p.getY(), hz)) < 96 * 96) {
					nearPlayer = true;
					break;
				}
			}
			if (!nearPlayer) {
				continue;
			}
			BlockPos pos = new BlockPos(hx, 0, hz);
			int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
			BlockState surface = world.getBlockState(new BlockPos(hx, groundY, hz));
			boolean broke = false;

			// Damage crops.
			if (surface.getBlock() instanceof net.minecraft.world.level.block.CropBlock
				&& surface.hasProperty(net.minecraft.world.level.block.CropBlock.AGE)) {
				world.setBlock(new BlockPos(hx, groundY, hz),
					surface.setValue(net.minecraft.world.level.block.CropBlock.AGE, 0),
					net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				broke = true;
				hailEventsToday++;
			}
			// Large hail shatters fragile blocks (glass, ice, snow...).
			if (!broke && storm.hailSize > 0.55f) {
				float hardness = surface.getDestroySpeed(world, new BlockPos(hx, groundY, hz));
				if (hardness > 0 && hardness < 0.5f && !surface.isAir()) {
					if (!isProtected(surface)) {
						world.destroyBlock(new BlockPos(hx, groundY, hz), false);
						broke = true;
						hailEventsToday++;
					}
				}
			}
			// Hurt entities on the impact point.
			if (storm.hailSize > 0.45f) {
				AABB area = new AABB(hx - 4, groundY - 2, hz - 4, hx + 4, groundY + 8, hz + 4);
				List<LivingEntity> victims = world.getEntitiesOfClass(LivingEntity.class, area);
				for (int v = 0; v < Math.min(victims.size(), 8); v++) {
					LivingEntity e = victims.get(v);
					if (world.getRandom().nextFloat() < 0.6f) {
						e.hurt(world.damageSources().fall(), (float) (1.0 + storm.hailSize * 3.0 * world.getRandom().nextFloat()));
						e.setDeltaMovement(e.getDeltaMovement().add(
							(world.getRandom().nextDouble() - 0.5) * 0.3, 0.35, (world.getRandom().nextDouble() - 0.5) * 0.3));
					}
				}
			}
			if (broke) {
				net.minecraft.sounds.SoundEvent hailSound =
					storm.hailSize > 0.7f ? SoundEvents.ITEM_BREAK : SoundEvents.SNOWBALL_THROW;
				world.playSound(null, hx, groundY + 1, hz,
					net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(hailSound),
					SoundSource.BLOCKS, 0.4f * storm.intensity, 0.8f + world.getRandom().nextFloat() * 0.4f);
			}
		}
	}

	private void dissipateStorm(ServerLevel world, Storm storm) {
		// Orphan any tornado still attached; it will dissipate on its own.
		for (TornadoState t : tornadoes) {
			if (t.stormId == storm.id) {
				t.stormId = -1;
			}
		}
	}

	public void removeStorm(Storm storm) {
		storms.remove(storm);
		dissipateStorm(world, storm);
		setDirty();
	}

	// ------------------------------------------------------------------
	// Tornadoes
	// ------------------------------------------------------------------

	private int countActiveTornadoes() {
		int n = 0;
		for (TornadoState t : tornadoes) {
			if (!t.isDissipated()) {
				n++;
			}
		}
		return n;
	}

	private void trySpawnTornado(ServerLevel world, Storm storm) {
		// Form on the periphery of the circulation, never on top of players.
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
			double dist = storm.radius * (0.2 + world.getRandom().nextDouble() * 0.4);
			double x = storm.x + Math.cos(angle) * dist;
			double z = storm.z + Math.sin(angle) * dist;
			boolean nearPlayer = false;
			for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
				if (p.distanceToSqr(new Vec3(x, p.getY(), z)) < 96 * 96) {
					nearPlayer = true;
					break;
				}
			}
			if (nearPlayer || !world.isLoaded(new BlockPos((int) x, 64, (int) z))) {
				continue;
			}
			int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
			DailyForecast forecast = currentForecast(world);
			double r = world.getRandom().nextDouble();
			int ef = r < 0.42 ? 0 : r < 0.72 ? 1 : r < 0.89 ? 2 : r < 0.965 ? 3 : r < 0.992 ? 4 : 5;
			ef = Math.min(ef, forecast.risk().tornadoEfCap());
			ef = Math.min(ef, TornadicConfig.maxTornadoIntensity);
			TornadoState state = new TornadoState(newId(), x, groundY, z, storm.id, ef);
			state.offsetX = x - storm.x;
			state.offsetZ = z - storm.z;
			state.heading = storm.dir;
			tornadoes.add(state);
			tornadoesToday++;
			maxEfToday = Math.max(maxEfToday, ef);
			TornadoEntity entity = TornadoEntity.create(TornadicMod.TORNADO_TYPE, world, state.id, x, groundY, z);
			world.addFreshEntity(entity);
			state.entity = entity;
			return;
		}
		// If every candidate is on/near a player, wait for next tick.
	}

	/** Creates a command-spawned tornado and its visible parent mesocyclone. */
	public boolean spawnTestTornado(ServerLevel world, double x, double z, int ef) {
		BlockPos pos = new BlockPos((int) x, 64, (int) z);
		if (!world.isLoaded(pos)) {
			return false;
		}
		int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
		int clampedEf = Math.max(0, Math.min(5, ef));

		// Test tornadoes used to be orphaned (stormId=-1). tickTornadoes immediately
		// marked every orphan as dissipating while its intensity was near zero, so it
		// vanished before the first client sync. Give it a real tornadic supercell.
		Storm parent = new Storm(newId(), x, z, world.getRandom().nextFloat() * (float) (Math.PI * 2.0));
		parent.type = StormType.TORNADIC_SUPERCELL;
		parent.intensity = 1.0f;
		parent.radius = 150.0f + clampedEf * 12.0f;
		parent.speed = 0.08f;
		parent.rotation = 0.85f;
		parent.rainIntensity = 1.0f;
		parent.hail = clampedEf >= 2;
		parent.hailSize = 0.55f + clampedEf * 0.08f;
		parent.lightningCooldown = 60;
		storms.add(parent);
		stormsToday++;

		TornadoState state = new TornadoState(newId(), x, groundY, z, parent.id, clampedEf);
		state.heading = parent.dir;
		// Start command tornadoes visibly condensed while preserving their normal
		// intensification, movement, physics and eventual dissipation lifecycle.
		state.age = Math.max(1, state.rampTicks / 3);
		state.intensity = state.age / (float) state.rampTicks;
		tornadoes.add(state);
		tornadoesToday++;
		maxEfToday = Math.max(maxEfToday, clampedEf);
		TornadoEntity entity = TornadoEntity.create(TornadicMod.TORNADO_TYPE, world, state.id, x, groundY, z);
		if (!world.addFreshEntity(entity)) {
			storms.remove(parent);
			tornadoes.remove(state);
			return false;
		}
		state.entity = entity;
		setDirty();
		// Do not wait for the periodic broadcaster: command feedback and visuals
		// should be observable on the very next client frame.
		syncToNearbyPlayers(world, parent, state);
		return true;
	}

	private void syncToNearbyPlayers(ServerLevel world, Storm storm, TornadoState state) {
		int groundTone = sampleGroundTone(world, state);
		for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
			if (player.distanceToSqr(new Vec3(state.x, player.getY(), state.z)) > 1600 * 1600) continue;
			player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
				StormSyncPayload.of(storm, state.currentEf())));
			player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
				TornadoSyncPayload.of(state, (int) state.id, groundTone)));
		}
	}

	private void tickTornadoes(ServerLevel world, DailyForecast forecast) {
		Iterator<TornadoState> it = tornadoes.iterator();
		while (it.hasNext()) {
			TornadoState state = it.next();
			state.tickLifecycle();

			if (state.isDissipated()) {
				finishTornado(world, state);
				it.remove();
				continue;
			}

			// Orphaned tornadoes drift and dissipate faster.
			Storm parent = findStorm(state.stormId);
			if (parent != null) {
				// Meander around the parent storm.
				state.heading += (float) (world.getRandom().nextDouble() - 0.5) * 0.09;
				// Ease the meander back toward the storm's motion.
				state.heading += (parent.dir - state.heading) * 0.02f;
				double meanderSpeed = parent.speed * 0.9;
				state.offsetX += Math.cos(state.heading) * meanderSpeed;
				state.offsetZ += Math.sin(state.heading) * meanderSpeed;
				double maxOffset = parent.radius * 0.55;
				double od = Math.sqrt(state.offsetX * state.offsetX + state.offsetZ * state.offsetZ);
				if (od > maxOffset) {
					state.offsetX *= maxOffset / od;
					state.offsetZ *= maxOffset / od;
				}
				state.x = parent.x + state.offsetX;
				state.z = parent.z + state.offsetZ;
			} else {
				state.dissipating = true;
				state.x += Math.cos(state.heading) * 0.15;
				state.z += Math.sin(state.heading) * 0.15;
			}

			int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) state.x, (int) state.z);
			state.y = groundY;

			// Move the tracking entity. Vanilla's default far-removal may have yanked the
		// visual entity while players were far away, so re-create it once someone is
		// close enough to see it (state stays authoritative regardless).
			if (state.entity == null || state.entity.isRemoved()) {
				BlockPos spawnPos = new BlockPos((int) state.x, (int) state.y, (int) state.z);
				boolean near = false;
				for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
					if (p.distanceToSqr(new Vec3(state.x, p.getY(), state.z)) < 400 * 400) {
						near = true;
						break;
					}
				}
				if (near && world.isLoaded(spawnPos)) {
					TornadoEntity entity = TornadoEntity.create(TornadicMod.TORNADO_TYPE, world, state.id,
						state.x, state.y, state.z);
					world.addFreshEntity(entity);
					state.entity = entity;
				} else {
					state.entity = null;
				}
			} else {
				state.entity.setPos(state.x, state.y, state.z);
				state.entity.setYRot((float) (state.heading * 180.0 / Math.PI));
			}

			if (tickCounter % 20 == 0) {
				state.recordPathPoint();
			}

			// Physics, damage, debris, warnings (all budgeted inside).
			TornadoPhysics.apply(world, this, state, forecast);

			// Client sync.
			if (tickCounter % 5 == 0) {
				int groundTone = sampleGroundTone(world, state);
				for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
					if (p.distanceToSqr(new Vec3(state.x, p.getY(), state.z)) < 1600 * 1600) {
						p.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
						TornadoSyncPayload.of(state, (int) state.id, groundTone)));
					}
				}
			}
		}
	}

	public Storm findStorm(long id) {
		if (id < 0) {
			return null;
		}
		for (Storm storm : storms) {
			if (storm.id == id) {
				return storm;
			}
		}
		return null;
	}

	public TornadoState findTornado(long id) {
		for (TornadoState t : tornadoes) {
			if (t.id == id) {
				return t;
			}
		}
		return null;
	}

	public TornadoState nearestTornado(double x, double z, double maxDist) {
		TornadoState best = null;
		double bestDist = maxDist * maxDist;
		for (TornadoState t : tornadoes) {
			if (t.isDissipated()) {
				continue;
			}
			double dx = t.x - x;
			double dz = t.z - z;
			double d = dx * dx + dz * dz;
			if (d < bestDist) {
				bestDist = d;
				best = t;
			}
		}
		return best;
	}

	public Storm nearestStorm(double x, double z, double maxDist) {
		Storm best = null;
		double bestDist = maxDist * maxDist;
		for (Storm storm : storms) {
			double dx = storm.x - x;
			double dz = storm.z - z;
			double d = dx * dx + dz * dz;
			if (d < bestDist) {
				bestDist = d;
				best = storm;
			}
		}
		return best;
	}

	private void finishTornado(ServerLevel world, TornadoState state) {
		// Leave visible evidence of the path.
		if (TornadicConfig.worldDamageEnabled && state.pathX.size() >= 4 && state.blocksBroken > 0) {
			int plantCount = Math.min(14, state.pathX.size() / 4);
			for (int i = 0; i < plantCount; i++) {
				int idx = world.getRandom().nextInt(state.pathX.size());
				double px = state.pathX.get(idx);
				double pz = state.pathZ.get(idx);
				BlockPos pos = new BlockPos((int) px, 0, (int) pz);
				if (!world.isLoaded(pos)) {
					continue;
				}
				int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
				BlockState surface = world.getBlockState(new BlockPos((int) px, y, (int) pz));
				if (surface.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
					world.setBlock(new BlockPos((int) px, y + 1, (int) pz),
						net.minecraft.world.level.block.Blocks.DEAD_BUSH.defaultBlockState(),
						net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
		}
		if (state.entity != null && !state.entity.isRemoved()) {
			state.entity.discard();
		}
		state.entity = null;
	}

	private int sampleGroundTone(ServerLevel world, TornadoState state) {
		BlockPos pos = new BlockPos((int) state.x, 0, (int) state.z);
		if (!world.isLoaded(pos)) {
			return 0;
		}
		int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		BlockState surface = world.getBlockState(new BlockPos((int) state.x, y, (int) state.z));
		BlockState above = world.getBlockState(new BlockPos((int) state.x, y + 1, (int) state.z));
		net.minecraft.world.level.block.Block block = surface.getBlock();
		if (block == net.minecraft.world.level.block.Blocks.SAND || block == net.minecraft.world.level.block.Blocks.SANDSTONE
			|| block == net.minecraft.world.level.block.Blocks.GRAVEL) {
			return 1; // desert dust
		}
		if (above.is(net.minecraft.world.level.block.Blocks.SNOW) || block == net.minecraft.world.level.block.Blocks.POWDER_SNOW
			|| block == net.minecraft.world.level.block.Blocks.SNOW_BLOCK || block == net.minecraft.world.level.block.Blocks.ICE) {
			return 2; // snowy
		}
		if (block == net.minecraft.world.level.block.Blocks.OAK_LEAVES || block == net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES
			|| block == net.minecraft.world.level.block.Blocks.BIRCH_LEAVES) {
			return 3; // forest
		}
		return 0; // default
	}

	// ------------------------------------------------------------------
	// Weather broadcast
	// ------------------------------------------------------------------

	private void broadcastWeather(ServerLevel world, DailyForecast forecast) {
		if (tickCounter % 20 != 0) {
			return;
		}
		float rain = 0.05f + forecast.stormProbability() / 100.0f * 0.2f;
		float thunder = 0f;
		for (Storm storm : storms) {
			double d = Double.MAX_VALUE;
			for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
				d = Math.min(d, storm.distanceTo(p.getX(), p.getZ()));
			}
			float prox = (float) Math.max(0.35, 1.0 - d / 2500.0);
			rain = Math.max(rain, storm.rainIntensity * prox);
			if (storm.type.ordinal() >= StormType.SUPERCELL.ordinal() && storm.intensity > 0.45f) {
				thunder = Math.max(thunder, storm.intensity * 0.6f);
			}
		}
		rain = Math.min(1.0f, rain);

		boolean changed = Math.abs(rain - lastRain) > 0.012f || Math.abs(thunder - lastThunder) > 0.02f;
		if (changed || tickCounter % 40 == 0) {
			lastRain = rain;
			lastThunder = thunder;
			ClientboundGameEventPacket rainPacket =
				new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rain);
			ClientboundGameEventPacket thunderPacket =
				new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunder);
			for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
				p.connection.send(rainPacket);
				p.connection.send(thunderPacket);
				if (tickCounter % 40 == 0) {
					p.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
						WeatherSyncPayload.of(forecast, rain, thunder, stormsToday, tornadoesToday, maxEfToday)));
				}
			}
		}

		// Per-storm sync.
		if (tickCounter % 10 == 0) {
			for (Storm storm : storms) {
				int tornadoEf = -1;
				for (TornadoState t : tornadoes) {
					if (t.stormId == storm.id && !t.isDissipated()) {
						tornadoEf = t.currentEf();
						break;
					}
				}
				StormSyncPayload payload = StormSyncPayload.of(storm, tornadoEf);
				for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
					if (p.distanceToSqr(new Vec3(storm.x, p.getY(), storm.z)) < 4200 * 4200) {
						p.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(payload));
					}
				}
			}
		}
	}

	/** Sends the full current state to one player (on join). */
	public void syncToPlayer(ServerLevel world, ServerPlayer player) {
		DailyForecast forecast = currentForecast(world);
		player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
			WeatherSyncPayload.of(forecast, lastRain < 0 ? 0 : lastRain,
			lastThunder < 0 ? 0 : lastThunder, stormsToday, tornadoesToday, maxEfToday)));
		for (Storm storm : storms) {
			int tornadoEf = -1;
			for (TornadoState t : tornadoes) {
				if (t.stormId == storm.id && !t.isDissipated()) {
					tornadoEf = t.currentEf();
					break;
				}
			}
			if (player.distanceToSqr(new Vec3(storm.x, player.getY(), storm.z)) < 4200 * 4200) {
				player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
				StormSyncPayload.of(storm, tornadoEf)));
			}
		}
		for (TornadoState t : tornadoes) {
			if (!t.isDissipated() && player.distanceToSqr(new Vec3(t.x, player.getY(), t.z)) < 1600 * 1600) {
				player.connection.send(new net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket(
				TornadoSyncPayload.of(t, (int) t.id, sampleGroundTone(world, t))));
			}
		}
	}

	// ------------------------------------------------------------------
	// Damage rules
	// ------------------------------------------------------------------

	public boolean isProtected(BlockState state) {
		return TornadicConfig.protectedBlocks.contains(
			net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
	}

	public List<Storm> storms() {
		return storms;
	}

	public List<TornadoState> tornadoes() {
		return tornadoes;
	}

	public int day() {
		return lastDay;
	}
}
