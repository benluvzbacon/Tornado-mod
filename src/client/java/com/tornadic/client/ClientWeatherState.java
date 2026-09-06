package com.tornadic.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tornadic.config.TornadicConfig;
import com.tornadic.network.StormSyncPayload;
import com.tornadic.network.TornadoSyncPayload;
import com.tornadic.network.WeatherSyncPayload;

import net.minecraft.world.phys.Vec3;

import com.tornadic.weather.DailyForecast;
import com.tornadic.weather.RiskRating;

/**
 * Client-side cache of the last synced weather state. All client visuals, HUD,
 * screens and instrument approximations read from here; nothing is simulated on
 * the client.
 */
public final class ClientWeatherState {
	private ClientWeatherState() {
	}

	public static volatile WeatherSyncPayload weather;

	/** storm id -> last payload. */
	public static final Map<Long, Stamp<StormSyncPayload>> storms = new ConcurrentHashMap<>();
	/** tornado id -> last payload. */
	public static final Map<Integer, Stamp<TornadoSyncPayload>> tornadoes = new ConcurrentHashMap<>();

	public static final List<PendingThunder> thunders = new ArrayList<>();

	public static class Stamp<T> {
		public final T value;
		public final long time;

		public Stamp(T value) {
			this.value = value;
			this.time = System.currentTimeMillis();
		}
	}

	public static class PendingThunder {
		public double x;
		public double z;
		public float volume;
		public float pitch;
		public int delayTicks;
	}

	public static void putStorm(StormSyncPayload p) {
		storms.put(p.id(), new Stamp<>(p));
	}

	public static void putTornado(TornadoSyncPayload p) {
		tornadoes.put(p.entityId(), new Stamp<>(p));
	}

	public static void scheduleThunder(double x, double z, float volume, float pitch, int delayTicks) {
		synchronized (thunders) {
			if (thunders.size() < 32) {
				PendingThunder t = new PendingThunder();
				t.x = x;
				t.z = z;
				t.volume = volume;
				t.pitch = pitch;
				t.delayTicks = Math.max(0, delayTicks);
				thunders.add(t);
			}
		}
	}

	/** Removes entries older than the given number of milliseconds. */
	public static void expire(long maxAgeMs) {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<Long, Stamp<StormSyncPayload>>> si = storms.entrySet().iterator();
		while (si.hasNext()) {
			if (now - si.next().getValue().time > maxAgeMs) {
				si.remove();
			}
		}
		Iterator<Map.Entry<Integer, Stamp<TornadoSyncPayload>>> ti = tornadoes.entrySet().iterator();
		while (ti.hasNext()) {
			if (now - ti.next().getValue().time > maxAgeMs) {
				ti.remove();
			}
		}
	}

	/**
	 * Client-side approximation of the wind field at a point (blocks/tick),
	 * mirroring the server logic with the synced data.
	 */
	public static Vec3 windAt(double x, double y, double z) {
		WeatherSyncPayload w = weather;
		double wx = 0;
		double wz = 0;
		double wy = 0;
		if (w != null) {
			double scale = 0.011 * TornadicConfig.windStrength;
			float dirRad = (float) (w.windDir() * Math.PI / 180.0);
			wx = Math.cos(dirRad) * w.windMph() * scale;
			wz = Math.sin(dirRad) * w.windMph() * scale;
		}
		for (Stamp<StormSyncPayload> s : storms.values()) {
			StormSyncPayload storm = s.value;
			double dx = x - storm.x();
			double dz = z - storm.z();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > storm.radius() * 2.0) {
				continue;
			}
			float fall = (float) Math.max(0.0, 1.0 - dist / (storm.radius() * 2.0));
			double sScale = 0.05 * fall * storm.intensity() * TornadicConfig.windStrength;
			wx += Math.cos(storm.dir()) * sScale * 2.2;
			wz += Math.sin(storm.dir()) * sScale * 2.2;
			if (Math.abs(storm.rotation()) > 0.05) {
				double tx = -dz / (dist + 1.0);
				double tz = dx / (dist + 1.0);
				double swirl = sScale * 2.5 * storm.rotation();
				wx += tx * swirl;
				wz += tz * swirl;
			}
		}
		for (Stamp<TornadoSyncPayload> t : tornadoes.values()) {
			TornadoSyncPayload t2 = t.value;
			double dx = x - t2.x();
			double dz = z - t2.z();
			double dist = Math.sqrt(dx * dx + dz * dz);
			double coreRadius = Math.max(3.0, t2.funnelRadius());
			double reach = coreRadius * 8.0 + 80.0;
			if (dist > reach) continue;
			double rankine = dist < coreRadius ? dist / coreRadius
				: Math.pow(coreRadius / Math.max(coreRadius, dist), 0.72);
			double edgeFade = Math.max(0.0, 1.0 - Math.pow(dist / reach, 3.0));
			double v = t2.windMs() * 0.055 * TornadicConfig.windStrength * rankine * edgeFade;
			double tx = -dz / (dist + 0.001), tz = dx / (dist + 0.001);
			wx += tx * v;
			wz += tz * v;
			double inflow = v * (dist < coreRadius * 2.5 ? 0.30 : 0.16);
			wx -= dx / (dist + 0.001) * inflow;
			wz -= dz / (dist + 0.001) * inflow;
			if (dist < coreRadius * 1.25) wy += v * 0.38 * (1.0 - dist / (coreRadius * 1.25));
		}
		return new Vec3(wx, wy, wz);
	}

	/**
	 * Local wind speed for instruments, in mph.
	 */
	public static double localWindMph(double x, double y, double z) {
		Vec3 w = windAt(x, y, z);
		return Math.sqrt(w.x * w.x + w.z * w.z) * 20.0 * 2.23694;
	}

	/**
	 * Builds a transient DailyForecast from the last synced weather (for client
	 * display helpers).
	 */
	public static DailyForecast snapshotForecast() {
		WeatherSyncPayload w = weather;
		if (w == null) {
			return null;
		}
		return new DailyForecast(w.day(), w.tempF(), w.dewPointF(), w.humidity(), w.pressureMb(),
			w.windMph(), w.windDir(), w.cape(), w.shear(), w.stormProbability(), w.tornadoProbability(),
			RiskRating.fromOrdinal(w.riskOrdinal()), java.util.List.of());
	}
}
