package com.tornadic.client;

import java.util.ArrayList;
import java.util.List;

import com.tornadic.config.TornadicConfig;
import com.tornadic.network.StormSyncPayload;
import com.tornadic.network.TornadoSyncPayload;
import com.tornadic.network.WeatherSyncPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * All client-side weather visuals: tornado funnels, storm rain, hail, wind
 * streaks and scheduled thunder. Everything is budgeted by maxSimParticles and
 * distance-culled; no per-block work.
 */
public final class WeatherVisuals {
	private WeatherVisuals() {
	}

	private static final RandomSource RNG = RandomSource.create();

	public static void tick(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		tickThunder(client);
		int budget = TornadicConfig.maxSimParticles;
		budget = Math.max(0, budget - spawnStormClouds(client, budget));
		budget = Math.max(0, budget - spawnTornadoFunnel(client, budget));
		budget = Math.max(0, budget - spawnStormRain(client, budget));
		spawnWindStreaks(client, Math.max(0, budget));
	}

	// ------------------------------------------------------------------
	// Thunder with distance-based delay
	// ------------------------------------------------------------------

	private static void tickThunder(Minecraft client) {
		List<ClientWeatherState.PendingThunder> due = new ArrayList<>();
		synchronized (ClientWeatherState.thunders) {
			for (int i = ClientWeatherState.thunders.size() - 1; i >= 0; i--) {
				ClientWeatherState.PendingThunder t = ClientWeatherState.thunders.get(i);
				t.delayTicks--;
				if (t.delayTicks <= 0) {
					due.add(t);
					ClientWeatherState.thunders.remove(i);
				}
			}
		}
		for (ClientWeatherState.PendingThunder t : due) {
			client.level.playLocalSound(t.x, 64, t.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
				SoundSource.WEATHER, t.volume, t.pitch, true);
		}
	}

	// ------------------------------------------------------------------
	// Volumetric storm clouds (particle-budgeted, server-positioned)
	// ------------------------------------------------------------------

	private static int spawnStormClouds(Minecraft client, int budget) {
		int spawned = 0;
		double px = client.player.getX();
		double pz = client.player.getZ();
		long time = client.level.getGameTime();
		for (var stamp : ClientWeatherState.storms.values()) {
			StormSyncPayload s = stamp.value;
			if (s.typeOrdinal() < 1 || spawned >= budget) continue;
			double dx = s.x() - px, dz = s.z() - pz;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > 900.0) continue;

			boolean supercell = s.typeOrdinal() >= 3;
			boolean tornadic = s.typeOrdinal() >= 4;
			int count = Math.min(budget - spawned, supercell ? 12 : 7);
			// Broad dark deck plus an anvil canopy. Particles are long-lived and only a
			// handful are emitted each frame, producing depth without cloud entities.
			for (int i = 0; i < count; i++) {
				double phase = time * (supercell ? 0.018 : 0.006) * Math.signum(s.rotation() == 0 ? 1 : s.rotation());
				double a = phase + RNG.nextDouble() * Math.PI * 2.0;
				double radial = Math.sqrt(RNG.nextDouble()) * s.radius() * (supercell ? 0.72 : 0.58);
				double cx = s.x() + Math.cos(a) * radial;
				double cz = s.z() + Math.sin(a) * radial;
				double baseY = Math.max(client.level.getSeaLevel() + 58, client.player.getY() + 45);
				double cy = baseY + RNG.nextDouble() * (supercell ? 20 : 12);
				client.level.addParticle(supercell ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
					cx, cy, cz, Math.cos(s.dir()) * s.speed() * 0.12, supercell ? 0.005 : 0.012,
					Math.sin(s.dir()) * s.speed() * 0.12);
				spawned++;
			}
			// Rotating lowered wall cloud directly above a tornadic circulation. This
			// visually joins cloud base to the funnel produced below.
			if (tornadic && spawned < budget) {
				double a = time * 0.07 * Math.signum(s.rotation() == 0 ? 1 : s.rotation());
				double r = Math.max(12.0, s.radius() * 0.16);
				client.level.addParticle(ParticleTypes.LARGE_SMOKE,
					s.x() + Math.cos(a) * r, Math.max(client.level.getSeaLevel() + 42, client.player.getY() + 32),
					s.z() + Math.sin(a) * r, 0, -0.015, 0);
				spawned++;
			}
		}
		return spawned;
	}

	// ------------------------------------------------------------------
	// Tornado funnel
	// ------------------------------------------------------------------

	private static int spawnTornadoFunnel(Minecraft client, int budget) {
		if (budget <= 0) {
			return 0;
		}
		int spawned = 0;
		long time = System.currentTimeMillis();
		double px = client.player.getX();
		double pz = client.player.getZ();
		List<TornadoSyncPayload> sorted = new ArrayList<>();
		for (var entry : ClientWeatherState.tornadoes.values()) {
			sorted.add(entry.value);
		}
		sorted.sort((a, b) -> Double.compare(
			(a.x() - px) * (a.x() - px) + (a.z() - pz) * (a.z() - pz),
			(b.x() - px) * (b.x() - px) + (b.z() - pz) * (b.z() - pz)));
		for (TornadoSyncPayload t : sorted) {
			double dx = t.x() - px;
			double dz = t.z() - pz;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > 2048) {
				continue;
			}
			// Closer = more detail.
			float detail = (float) Math.max(0.25, 1.0 - dist / 2048.0);
			if (RNG.nextInt(100) > Math.max(5, (int) (detail * 60))) {
				continue; // frame skip for far tornadoes
			}
			float funnel = Math.max(2f, t.funnelRadius());
			float cloud = Math.max(funnel * 2f, t.cloudRadius());
			float height = 48f + t.ef() * 8f;
			// Ground tone: 0 default, 1 desert, 2 snow, 3 forest.
			float r = 0.42f, g = 0.42f, b = 0.44f;
			if (t.groundTone() == 1) {
				r = 0.72f; g = 0.62f; b = 0.42f; // desert dust
			} else if (t.groundTone() == 2) {
				r = 0.85f; g = 0.88f; b = 0.92f; // snow
			} else if (t.groundTone() == 3) {
				r = 0.45f; g = 0.48f; b = 0.36f; // forest
			}
			int rotBase = (int) (time / (90f - t.ef() * 8f)); // faster rotation for stronger EFs
			int rings = 3 + t.ef();
			for (int i = 0; i < rings && spawned < budget; i++) {
				float fr = i / (float) rings;
				float radius = funnel + (cloud - funnel) * (float) Math.pow(fr, 0.75);
				float y = (float) (t.y() + fr * height);
				float angle = (rotBase + i * 37) * 0.35f;
				double ax = t.x() + Math.cos(angle) * radius;
				double az = t.z() + Math.sin(angle) * radius;
				// Funnel body.
				client.level.addParticle(
					new DustParticleOptions(new org.joml.Vector3f(r, g, b),
						0.6f + fr * 1.6f),
					ax, y, az,
					0, 0.02f + RNG.nextFloat() * 0.02f, 0);
				spawned++;
				// Ground debris cloud near the base.
				if (fr < 0.25f && spawned < budget && RNG.nextInt(3) == 0) {
					float da = RNG.nextFloat() * (float) Math.PI * 2;
					client.level.addParticle(
						new DustParticleOptions(new org.joml.Vector3f(r * 0.78F, g * 0.78F, b * 0.78F),
							1.4f),
						t.x() + Math.cos(da) * funnel * (0.8f + RNG.nextFloat()),
						t.y() + 0.5 + RNG.nextFloat() * 2.5f,
						t.z() + Math.sin(da) * funnel * (0.8f + RNG.nextFloat()),
						0, 0.05f, 0);
					spawned++;
				}
				// Rain wrapped around the funnel.
				if (t.ef() >= 1 && fr > 0.2f && fr < 0.8f && spawned < budget && RNG.nextInt(4) == 0) {
					float ra = RNG.nextFloat() * (float) Math.PI * 2;
					float rr = radius + 4f + RNG.nextFloat() * 8f;
					client.level.addParticle(ParticleTypes.SPLASH,
						t.x() + Math.cos(ra) * rr, y + RNG.nextFloat() * 10f, t.z() + Math.sin(ra) * rr,
						0, -0.25, 0);
					spawned++;
				}
			}
			// Debris sparks on the outer edge.
			if (t.ef() >= 1 && spawned < budget && RNG.nextInt(3) == 0) {
				float da = RNG.nextFloat() * (float) Math.PI * 2;
				client.level.addParticle(ParticleTypes.CRIT,
					t.x() + Math.cos(da) * (funnel + 2), t.y() + 1 + RNG.nextFloat() * 3,
					t.z() + Math.sin(da) * (funnel + 2), 0, 0.08f, 0);
				spawned++;
			}
			if (spawned >= budget || sorted.size() <= 2) {
				continue;
			}
			if (RNG.nextInt(4) == 0) {
				continue; // distant second tornado rendered sparsely
			}
		}
		return spawned;
	}

	// ------------------------------------------------------------------
	// Storm rain / hail / wind streaks
	// ------------------------------------------------------------------

	private static int spawnStormRain(Minecraft client, int budget) {
		int spawned = 0;
		double px = client.player.getX();
		double pz = client.player.getZ();
		Vec3 wind = ClientWeatherState.windAt(px, client.player.getY(), pz);
		for (var entry : ClientWeatherState.storms.values()) {
			StormSyncPayload s = entry.value;
			double dx = s.x() - px;
			double dz = s.z() - pz;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > s.radius() + 90) {
				continue;
			}
			float intensity = s.rainIntensity();
			int drops = Math.min(budget - spawned, (int) (6 * intensity));
			for (int i = 0; i < drops && spawned < budget; i++) {
				double rx = px + (RNG.nextFloat() * 2 - 1) * 40;
				double rz = pz + (RNG.nextFloat() * 2 - 1) * 40;
				double ry = client.player.getY() + 12 + RNG.nextFloat() * 8;
				client.level.addParticle(ParticleTypes.SPLASH,
					rx, ry, rz,
					wind.x * 3.0, -0.6 - RNG.nextFloat() * 0.4, wind.z * 3.0);
				spawned++;
			}
			// Hail: larger falling flakes with impact sound.
			if (s.hail()) {
				int flakes = Math.min(budget - spawned, 2 + (int) (3 * s.hailSize() * intensity));
				for (int i = 0; i < flakes && spawned < budget; i++) {
					double rx = px + (RNG.nextFloat() * 2 - 1) * 40;
					double rz = pz + (RNG.nextFloat() * 2 - 1) * 40;
					client.level.addParticle(ParticleTypes.SNOWFLAKE,
						rx, client.player.getY() + 14 + RNG.nextFloat() * 6, rz,
						wind.x * 2.5, -0.55 - s.hailSize() * 0.3, wind.z * 2.5);
					spawned++;
				}
				if (s.hailSize() > 0.5 && RNG.nextInt(24) == 0) {
					client.level.playLocalSound(px, client.player.getY(), pz,
						s.hailSize() > 0.8 ? SoundEvents.ITEM_BREAK : SoundEvents.SNOWBALL_THROW,
						SoundSource.BLOCKS, 0.3f * intensity, 0.8f + RNG.nextFloat() * 0.4f, false);
				}
			}
			break; // only the most relevant storm adds local rain
		}
		return spawned;
	}

	private static void spawnWindStreaks(Minecraft client, int budget) {
		WeatherSyncPayload w = ClientWeatherState.weather;
		if (w == null || budget <= 0) {
			return;
		}
		double px = client.player.getX();
		double pz = client.player.getZ();
		Vec3 wind = ClientWeatherState.windAt(px, client.player.getY(), pz);
		double speed = Math.sqrt(wind.x * wind.x + wind.z * wind.z) * 20.0; // blocks/sec
		if (speed < 3.0) {
			return;
		}
		int streaks = Math.min(budget, 1 + (int) (speed / 8.0));
		for (int i = 0; i < streaks; i++) {
			if (RNG.nextInt(10) != 0) {
				continue;
			}
			double sx = px + (RNG.nextFloat() * 2 - 1) * 24;
			double sz = pz + (RNG.nextFloat() * 2 - 1) * 24;
			double sy = client.player.getY() + 1 + RNG.nextFloat() * 3;
			client.level.addParticle(
				new DustParticleOptions(new org.joml.Vector3f(235 / 255.0F, 235 / 255.0F, 235 / 255.0F), 0.35f),
				sx, sy, sz,
				wind.x * 4.0, 0.02, wind.z * 4.0);
		}
	}
}
