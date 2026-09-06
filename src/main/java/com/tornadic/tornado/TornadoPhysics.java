package com.tornadic.tornado;

import java.util.List;

import com.tornadic.config.TornadicConfig;
import com.tornadic.entity.ChaserVehicleEntity;
import com.tornadic.saveddata.TornadicSavedData;
import com.tornadic.weather.DailyForecast;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Budgeted per-tornado physics: wind on entities, wind/debris damage, block
 * destruction and debris flinging. Every expensive operation is rate limited and
 * chunk-aware; nothing here touches blocks every tick.
 */
public final class TornadoPhysics {
	private TornadoPhysics() {
	}

	public static void apply(ServerLevel world, TornadicSavedData data, TornadoState state, DailyForecast forecast) {
		int tick = (int) (world.getGameTime() % 20000L);
		if (tick % 2 == 0) {
			applyEntityEffects(world, state);
		}
		if (TornadicConfig.debrisEnabled && tick % 4 == 0) {
			swirlDebris(world, state);
		}
		if (TornadicConfig.worldDamageEnabled && tick % 4 == 0) {
			breakBlocks(world, data, state);
		}
		issueWarnings(world, data, state);
	}

	// ------------------------------------------------------------------
	// Wind on entities
	// ------------------------------------------------------------------

	private static void applyEntityEffects(ServerLevel world, TornadoState state) {
		if (state.intensity <= 0.05) {
			return;
		}
		float funnel = state.funnelRadius();
		double reach = funnel * 3.0 + 16.0;
		AABB area = new AABB(state.x - reach, state.y - 32, state.z - reach,
			state.x + reach, state.y + 96, state.z + reach);
		List<LivingEntity> victims = world.getEntitiesOfClass(LivingEntity.class, area);
		if (victims.isEmpty()) {
			return;
		}
		float windMs = state.windMs();
		float threshold = state.intensityScale().windDamageThreshold();
		int limit = Math.min(victims.size(), 24);
		for (int i = 0; i < limit; i++) {
			LivingEntity e = victims.get(i);
			double dx = e.getX() - state.x;
			double dz = e.getZ() - state.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > reach) {
				continue;
			}
			double norm = dist / reach;
			double core = Math.min(1.0, dist / Math.max(1.0, funnel * 0.5));
			// Local wind speed (m/s, scaled for gameplay).
			double localWind = windMs * TornadicConfig.windStrength * (0.3 + 0.7 * core) * (1.0 - norm * 0.7);
			double force = localWind * 0.055;

			// Tangential shove (counter-clockwise) + inward pull + core lift.
			double tx = -dz / (dist + 0.001);
			double tz = dx / (dist + 0.001);
			double ix = -dx / (dist + 0.001);
			double iz = -dz / (dist + 0.001);
			Vec3 vel = e.getDeltaMovement();
			double lift = 0.0;
			if (dist < funnel * 1.4) {
				lift = force * 0.5 * (1.0 - dist / (funnel * 1.4));
			}
			Vec3 target = vel.add(tx * force * 1.6, lift, tz * force * 1.6).add(ix * force * 0.5, 0, iz * force * 0.5);
			double speed = target.length();
			double maxSpeed = 1.1;
			if (speed > maxSpeed) {
				target = target.multiply(maxSpeed / speed, maxSpeed / speed, maxSpeed / speed);
			}
			e.setDeltaMovement(target);

			// Wind damage - never instant death: small chunks that add up.
			if (localWind > threshold) {
				double excess = (localWind - threshold) / threshold;
				double chance = Math.min(0.35, 0.05 + excess * 0.12);
				if (world.getRandom().nextFloat((float) chance)) {
					double amount = 0.5 + state.currentEf() * 0.45 + world.getRandom().nextDouble() * 1.2;
					// Riding a chaser vehicle halves wind damage (shelter).
					if (e instanceof Player p && p.getVehicle() instanceof ChaserVehicleEntity) {
						amount *= 0.5;
					}
					e.hurt(world.damageSources().fall(), (float) Math.min(4.5, amount));
				}
			}

			// Debris strike near the core: bigger hit + violent knockback.
			if (dist < funnel * 1.6 && state.currentEf() >= 1) {
				double chance = state.intensityScale().debrisChance() * 0.5;
				if (world.getRandom().nextFloat((float) chance)) {
					double amount = 1.5 + world.getRandom().nextDouble() * (1.0 + state.currentEf() * 0.6);
					if (e instanceof Player p && p.getVehicle() instanceof ChaserVehicleEntity) {
						amount *= 0.5;
					}
					e.hurt(world.damageSources().fall(), (float) Math.min(5.0, amount));
					e.setDeltaMovement(e.getDeltaMovement().add(
						ix * -0.8 - tx * 0.4, 0.45 + world.getRandom().nextDouble() * 0.3, iz * -0.8 - tz * 0.4));
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// Debris (item entities orbit the funnel)
	// ------------------------------------------------------------------

	public static int activeDebris(TornadoState state) {
		return state.debrisCount;
	}

	private static void swirlDebris(ServerLevel world, TornadoState state) {
		double reach = state.funnelRadius() * 2.0 + 10.0;
		AABB area = new AABB(state.x - reach, state.y - 4, state.z - reach,
			state.x + reach, state.y + 64, state.z + reach);
		List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, area);
		int budget = Math.max(0, TornadicConfig.maxDebrisItems - state.debrisCount);
		int done = 0;
		for (int i = 0; i < items.size() && done < 16 && budget >= 0; i++) {
			ItemEntity item = items.get(i);
			double dx = item.getX() - state.x;
			double dz = item.getZ() - state.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > reach || dist < 0.5) {
				continue;
			}
			double v = state.windMs() * 0.05 * TornadicConfig.windStrength * (1.0 - dist / reach + 0.4);
			double tx = -dz / (dist + 0.001);
			double tz = dx / (dist + 0.001);
			double lift = v * 0.9;
			Vec3 vel = item.getDeltaMovement();
			item.setDeltaMovement(vel.add(tx * v * 1.4, lift, tz * v * 1.4));
			if (budget > 0 && item.customAge > 5) {
				state.debrisCount++;
				budget--;
			}
			done++;
		}
	}

	// ------------------------------------------------------------------
	// Block destruction
	// ------------------------------------------------------------------

	private static void breakBlocks(ServerLevel world, TornadicSavedData data, TornadoState state) {
		if (state.intensity <= 0.15) {
			return;
		}
		int budget = TornadicConfig.maxBlocksBrokenPerTick;
		if (state.blocksBroken >= TornadicConfig.maxBlocksBrokenPerTornado) {
			return;
		}
		float limit = state.intensityScale().destroyHardnessLimit();
		int samples = 6 + state.currentEf() * 5;
		float funnel = state.funnelRadius();

		for (int i = 0; i < samples && budget > 0; i++) {
			double angle = world.getRandom().nextDouble() * Math.PI * 2.0;
			double r = funnel * (0.75 + world.getRandom().nextDouble() * 0.6);
			int bx = (int) (state.x + Math.cos(angle) * r);
			int bz = (int) (state.z + Math.sin(angle) * r);
			BlockPos base = new BlockPos(bx, 0, bz);
			if (!world.isLoaded(base)) {
				continue;
			}
			int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, base);
			boolean brokeOne = false;
			for (int dy = 1; dy >= -1 && !brokeOne; dy--) {
				BlockPos pos = new BlockPos(bx, groundY + dy, bz);
				BlockState st = world.getBlockState(pos);
				if (st.isAir()) {
					continue;
				}
				if (data.isProtected(st)) {
					continue;
				}
				float hardness = st.getDestroySpeed(world, pos);
				if (hardness < 0) {
					continue; // unbreakable (bedrock, obsidian...)
				}
				if (hardness > limit) {
					continue;
				}
				// Stronger tornadoes break more readily.
				double chance = 0.2 + state.intensity * 0.55;
				if (!world.getRandom().nextFloat((float) chance)) {
					continue;
				}
				world.destroyBlock(pos, false);
				state.blocksBroken++;
				budget--;
				brokeOne = true;

				// Flung debris (budgeted).
				if (TornadicConfig.debrisEnabled && state.debrisCount < TornadicConfig.maxDebrisItems
					&& world.getRandom().nextDouble() < 0.4) {
					ItemStack stack = new ItemStack(st.getBlock());
					if (!stack.isEmpty()) {
						ItemEntity debris = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
						double tx = -(bz - state.z);
						double tz = bx - state.x;
						double len = Math.sqrt(tx * tx + tz * tz) + 0.001;
						double v = 0.25 + world.getRandom().nextDouble() * 0.3 * (1 + state.currentEf() * 0.3);
						debris.setDeltaMovement(tx / len * v, 0.35 + world.getRandom().nextDouble() * 0.25, tz / len * v);
						world.addFreshEntity(debris);
						state.debrisCount++;
					}
				}

				// Tornado paths strip trees: nearby logs go down too.
				if (st.is(BlockTags.LOGS) && world.getRandom().nextFloat() < 0.5f && budget > 0) {
					for (int k = 0; k < 3 && budget > 0; k++) {
						BlockPos logPos = pos.offset(world.getRandom().nextInt(5) - 2,
							world.getRandom().nextInt(3), world.getRandom().nextInt(5) - 2);
						if (!world.isLoaded(logPos)) {
							continue;
						}
						BlockState logSt = world.getBlockState(logPos);
						if (logSt.is(BlockTags.LOGS) && !data.isProtected(logSt)) {
							float logHardness = logSt.getDestroySpeed(world, logPos);
							if (logHardness >= 0 && logHardness <= limit) {
								world.destroyBlock(logPos, false);
								state.blocksBroken++;
								budget--;
							}
						}
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------
	// Warnings
	// ------------------------------------------------------------------

	private static void issueWarnings(ServerLevel world, TornadicSavedData data, TornadoState state) {
		if (!TornadicConfig.warningsEnabled || state.currentEf() < 1 || state.intensity < 0.3) {
			return;
		}
		int ef = state.currentEf();
		double moved = Math.sqrt(
			(state.x - state.warnedX) * (state.x - state.warnedX) + (state.z - state.warnedZ) * (state.z - state.warnedZ));
		if (state.warned && ef < state.warnedEf + 2 && moved < 1500) {
			return;
		}
		state.warned = true;
		state.warnedEf = ef;
		state.warnedX = state.x;
		state.warnedZ = state.z;

		TornadoIntensity scale = state.intensityScale();
		String compass = compass(state.heading);
		// Forward speed: follow the parent storm, else a default drift.
		double forward = 0.25;
		if (state.stormId >= 0) {
			var storm = data.findStorm(state.stormId);
			if (storm != null) {
				forward = storm.speed;
			}
		}
		double speedKmh = forward * 20.0 * 3.6; // blocks/tick -> m/s -> km/h

		Component warning = Component.empty()
			.append(Component.literal("\nTORNADO WARNING\n").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
			.append(Component.literal("A tornado has been detected near:\n"))
			.append(Component.literal("X: " + (int) state.x + "  Z: " + (int) state.z + "\n"))
			.append(Component.literal("Estimated intensity: ").append(scale.asComponent()))
			.append(Component.literal("\nMovement: " + compass + "  Speed: " + (int) speedKmh + " km/h\n"))
			.append(Component.literal("Seek shelter immediately.").withStyle(ChatFormatting.YELLOW));

		for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) {
			if (p.distanceToSqr(new Vec3(state.x, p.getY(), state.z)) <= (double) TornadicConfig.warningRadius * TornadicConfig.warningRadius) {
				p.sendSystemMessage(warning);
				p.playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.WEATHER, 0.7f, 1.0f);
			}
		}
	}

	private static String compass(float headingRad) {
		float deg = (float) ((headingRad * 180.0 / Math.PI) % 360.0);
		if (deg < 0) {
			deg += 360.0f;
		}
		String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
		return dirs[((int) Math.round(deg / 45.0)) % 8];
	}
}
