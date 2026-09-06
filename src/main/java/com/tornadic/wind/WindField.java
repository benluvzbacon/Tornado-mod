package com.tornadic.wind;

import java.util.List;

import com.tornadic.config.TornadicConfig;
import com.tornadic.storm.Storm;
import com.tornadic.tornado.TornadoState;
import com.tornadic.weather.DailyForecast;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The global atmospheric wind field: daily steering wind + local storm forcing +
 * tornado vortexes. Evaluated lazily and only where needed (entity pushes, debris,
 * anemometer readings) - never per block per tick.
 */
public final class WindField {
	private WindField() {
	}

	/**
	 * Wind vector at a point, in blocks/tick (multiply by 20 for blocks/second).
	 */
	public static Vec3 windAt(ServerLevel level, DailyForecast forecast, List<Storm> storms,
		List<TornadoState> tornadoes, double x, double y, double z) {
		// Steering wind from the daily forecast (mph -> blocks/tick, scaled down for gameplay).
		double scale = 0.011 * TornadicConfig.windStrength;
		float dirRad = (float) (forecast.windDir() * Math.PI / 180.0);
		double wx = Math.cos(dirRad) * forecast.windMph() * scale;
		double wz = Math.sin(dirRad) * forecast.windMph() * scale;
		double wy = 0.0;

		// Storm forcing: broad push ahead of the storm plus a swirl for rotating ones.
		for (Storm storm : storms) {
			double dx = x - storm.x;
			double dz = z - storm.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > storm.radius * 2.0) {
				continue;
			}
			float fall = (float) Math.max(0.0, 1.0 - dist / (storm.radius * 2.0));
			double sScale = 0.05 * fall * storm.intensity * TornadicConfig.windStrength;
			float sDirRad = storm.dir;
			wx += Math.cos(sDirRad) * sScale * 2.2;
			wz += Math.sin(sDirRad) * sScale * 2.2;
			// Mesocyclone swirl (cyclonic in the northern hemisphere).
			if (Math.abs(storm.rotation) > 0.05) {
				double tx = -dz / (dist + 1.0);
				double tz = dx / (dist + 1.0);
				double swirl = sScale * 2.5 * storm.rotation;
				wx += tx * swirl;
				wz += tz * swirl;
			}
		}

		// Tornado vortexes: Rankine-style tangential + slight inward pull.
		for (TornadoState t : tornadoes) {
			if (t.intensity <= 0.01) {
				continue;
			}
			double dx = x - t.x;
			double dz = z - t.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			double coreRadius = Math.max(3.0, t.funnelRadius());
			double reach = coreRadius * 8.0 + 80.0;
			if (dist > reach) continue;

			// Modified Rankine vortex: velocity rises toward the radius of maximum
			// wind, then decays through the broad outer circulation.
			double rankine = dist < coreRadius ? dist / coreRadius
				: Math.pow(coreRadius / Math.max(coreRadius, dist), 0.72);
			double edgeFade = Math.max(0.0, 1.0 - Math.pow(dist / reach, 3.0));
			double v = t.windMs() * 0.055 * TornadicConfig.windStrength * rankine * edgeFade;
			double tx = -dz / (dist + 0.001), tz = dx / (dist + 0.001);
			wx += tx * v;
			wz += tz * v;

			// Low-level pressure deficit draws air inward; rising air dominates inside
			// the condensation funnel, with weak compensating outflow aloft/at the edge.
			double inflow = v * (dist < coreRadius * 2.5 ? 0.30 : 0.16);
			wx -= dx / (dist + 0.001) * inflow;
			wz -= dz / (dist + 0.001) * inflow;
			if (dist < coreRadius * 1.25) wy += v * 0.38 * (1.0 - dist / (coreRadius * 1.25));

			// Uncommon EF3+ suction vortices create intermittent localized wind maxima.
			int ef = t.currentEf();
			boolean multi = ef >= 3 && Math.floorMod((int) t.id * 31, 100) < (ef == 3 ? 28 : ef == 4 ? 52 : 72);
			if (multi) {
				int count = ef >= 5 ? 3 : 2;
				for (int sv = 0; sv < count; sv++) {
					double a = level.getGameTime() * (0.045 + sv * 0.008) + sv * Math.PI * 2.0 / count + t.id;
					double orbit = coreRadius * 0.58;
					double sx = t.x + Math.cos(a) * orbit, sz = t.z + Math.sin(a) * orbit;
					double sdx = x - sx, sdz = z - sz, sd = Math.sqrt(sdx * sdx + sdz * sdz);
					double sr = Math.max(2.0, coreRadius * 0.22);
					if (sd < sr * 3.0) {
						double boost = v * 0.42 * Math.max(0.0, 1.0 - sd / (sr * 3.0));
						wx += -sdz / (sd + 0.001) * boost;
						wz += sdx / (sd + 0.001) * boost;
					}
				}
			}
		}

		return new Vec3(wx, wy, wz);
	}

	/**
	 * Applies ambient wind nudging to an entity (player/mob) once per effect tick.
	 */
	public static void applyToEntity(ServerLevel level, DailyForecast forecast, List<Storm> storms,
		List<TornadoState> tornadoes, Entity entity) {
		Vec3 wind = windAt(level, forecast, storms, tornadoes, entity.getX(), entity.getY(), entity.getZ());
		Vec3 vel = entity.getDeltaMovement();
		double push = entity instanceof net.minecraft.world.entity.player.Player ? 0.45 : 1.0;
		Vec3 target = vel.add(wind.multiply(0.16 * push, 0.16 * push, 0.16 * push));
		// Damp horizontal velocity a bit so wind can't launch entities to infinity.
		double speed = target.length();
		double max = 0.9;
		if (speed > max) {
			target = target.multiply(max / speed, max / speed, max / speed);
		}
		entity.setDeltaMovement(target);
	}
}
