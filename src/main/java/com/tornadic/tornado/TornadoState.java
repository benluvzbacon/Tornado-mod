package com.tornadic.tornado;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * Authoritative server-side tornado state, owned by {@link com.tornadic.saveddata.TornadicSavedData}.
 *
 * <p>The {@link TornadoEntity} is purely a visual/tracking vehicle (noSave); the truth
 * lives here and survives world reloads.
 */
public class TornadoState {
	public long id;
	public double x;
	public double y;
	public double z;
	public float heading;          // radians, meander heading of the funnel
	public double offsetX;         // offset from parent storm
	public double offsetZ;
	public long stormId;           // parent storm, -1 if orphaned
	public int age;
	public int rampTicks;          // formation time
	public int holdTicks;          // time at peak intensity
	public float peakEf;           // 0..5, the intensity this tornado will reach
	public float intensity;        // 0..1 progress toward peak
	public boolean dissipating;
	public int blocksBroken;
	public int warnedEf;
	public double warnedX;
	public double warnedZ;
	public boolean warned;
	public final List<Double> pathX = new ArrayList<>();
	public final List<Double> pathZ = new ArrayList<>();

	/** Transient link to the live entity (recreated after world load). */
	public transient TornadoEntity entity;
	/** Transient count of item entities currently flung around this tornado (debris budget). */
	public transient int debrisCount;

	public TornadoState(long id, double x, double z, long stormId, float peakEf) {
		this(id, x, 0, z, stormId, peakEf);
	}

	public TornadoState(long id, double x, double y, double z, long stormId, float peakEf) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.z = z;
		this.stormId = stormId;
		this.peakEf = peakEf;
		this.rampTicks = 260 + (int) (Math.random() * 340);
		this.holdTicks = 500 + (int) (Math.random() * 1100);
	}

	public int currentEf() {
		int ef = (int) Math.floor(intensity * (peakEf + 1.0F) * 0.9999F);
		if (ef < 0) {
			ef = 0;
		}
		if (ef > peakEf) {
			ef = (int) peakEf;
		}
		return ef;
	}

	public TornadoIntensity intensityScale() {
		return TornadoIntensity.fromEf(currentEf());
	}

	public float funnelRadius() {
		float base = TornadoIntensity.fromEf(peakEf).funnelRadius();
		float progress = Math.max(0.25F, intensity);
		return base * (0.4F + 0.6F * progress);
	}

	public float cloudRadius() {
		return funnelRadius() * (3.2F + peakEf * 0.5F);
	}

	public float windMs() {
		return TornadoIntensity.fromEf(currentEf()).windMs() * intensity;
	}

	public void tickLifecycle() {
		age++;
		if (dissipating) {
			intensity -= 0.0035F;
			if (intensity <= 0.0F) {
				intensity = 0.0F;
			}
			return;
		}
		if (age < rampTicks) {
			intensity = age / (float) rampTicks;
		} else if (age < rampTicks + holdTicks) {
			// Breathing at peak.
			float jitter = 0.06F * (float) Math.sin(age * 0.02);
			intensity = Math.max(0.9F, Math.min(1.0F, 0.95F + jitter));
		} else {
			intensity -= 0.0028F;
			if (intensity <= 0.25F) {
				dissipating = true;
			}
		}
		if (intensity < 0.0F) {
			intensity = 0.0F;
		}
	}

	public boolean isDissipated() {
		return dissipating && intensity <= 0.0F;
	}

	public void recordPathPoint() {
		pathX.add(x);
		pathZ.add(z);
		// Keep the path memory bounded.
		if (pathX.size() > 400) {
			pathX.remove(0);
			pathZ.remove(0);
		}
	}

	public double pathLength() {
		double len = 0.0;
		for (int i = 1; i < pathX.size(); i++) {
			double dx = pathX.get(i) - pathX.get(i - 1);
			double dz = pathZ.get(i) - pathZ.get(i - 1);
			len += Math.sqrt(dx * dx + dz * dz);
		}
		return len;
	}

	public CompoundTag writeNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putLong("Id", id);
		tag.putDouble("X", x);
		tag.putDouble("Y", y);
		tag.putDouble("Z", z);
		tag.putFloat("Heading", heading);
		tag.putDouble("OffsetX", offsetX);
		tag.putDouble("OffsetZ", offsetZ);
		tag.putLong("StormId", stormId);
		tag.putInt("Age", age);
		tag.putInt("Ramp", rampTicks);
		tag.putInt("Hold", holdTicks);
		tag.putFloat("PeakEf", peakEf);
		tag.putFloat("Intensity", intensity);
		tag.putBoolean("Dissipating", dissipating);
		tag.putInt("BlocksBroken", blocksBroken);
		tag.putBoolean("Warned", warned);
		tag.putInt("WarnedEf", warnedEf);
		tag.putDouble("WarnedX", warnedX);
		tag.putDouble("WarnedZ", warnedZ);
		ListTag path = new ListTag();
		for (int i = 0; i < pathX.size(); i++) {
			CompoundTag point = new CompoundTag();
			point.putDouble("X", pathX.get(i));
			point.putDouble("Z", pathZ.get(i));
			path.add(point);
		}
		tag.put("Path", path);
		return tag;
	}

	public void readNbt(CompoundTag tag) {
		this.id = tag.getLong("Id");
		this.x = tag.getDouble("X");
		this.y = tag.getDouble("Y");
		this.z = tag.getDouble("Z");
		this.heading = tag.getFloat("Heading");
		this.offsetX = tag.getDouble("OffsetX");
		this.offsetZ = tag.getDouble("OffsetZ");
		this.stormId = tag.getLong("StormId");
		this.age = tag.getInt("Age");
		this.rampTicks = tag.getInt("Ramp");
		this.holdTicks = tag.getInt("Hold");
		this.peakEf = tag.getFloat("PeakEf");
		this.intensity = tag.getFloat("Intensity");
		this.dissipating = tag.getBoolean("Dissipating");
		this.blocksBroken = tag.getInt("BlocksBroken");
		this.warned = tag.getBoolean("Warned");
		this.warnedEf = tag.getInt("WarnedEf");
		this.warnedX = tag.getDouble("WarnedX");
		this.warnedZ = tag.getDouble("WarnedZ");
		pathX.clear();
		pathZ.clear();
		ListTag path = tag.getList("Path", 10); // 10 = TAG_COMPOUND
		for (int i = 0; i < path.size(); i++) {
			CompoundTag point = path.getCompound(i);
			pathX.add(point.getDouble("X"));
			pathZ.add(point.getDouble("Z"));
		}
	}
}
