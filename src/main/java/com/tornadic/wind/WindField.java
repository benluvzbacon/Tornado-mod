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
			double reach = t.funnelRadius() * 3.0 + 12.0;
			if (dist > reach) {
				continue;
			}
			double norm = dist / reach;
			double core = Math.min(1.0, dist / Math.max(1.0, t.funnelRadius() * 0.5));
			double v = t.windMs() * 0.055 * TornadicConfig.windStrength * (0.35 + 0.65 * core) * (1.0 - norm * 0.75);
			// Tangential (counter-clockwise).
			double tx = -dz / (dist + 0.001);
			double tz = dx / (dist + 0.001);
			wx += tx * v;
			wz += tz * v;
			// Inward suction toward the core.
			double inward = v * 0.22;
			wx -= dx / (dist + 0.001) * inward;
			wz -= dz / (dist + 0.001) * inward;
			// Lift near the core.
			if (dist < t.funnelRadius()) {
				wy += v * 0.35;
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
