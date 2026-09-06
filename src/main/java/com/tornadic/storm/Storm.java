package com.tornadic.storm;

import net.minecraft.nbt.CompoundTag;

/**
 * A server-side thunderstorm system. Storms are not entities: they are data objects
 * owned by {@link StormSystem} which moves them across the map, evolves them through
 * {@link StormType} stages and applies their effects (rain, lightning, hail, wind,
 * tornado formation) around them.
 */
public class Storm {
	public long id;
	public double x;
	public double z;
	public float dir;          // radians, movement direction
	public float speed;        // blocks per tick
	public StormType type;
	public int age;            // ticks
	public float intensity;    // 0..1, evolves with development
	public float radius;       // footprint radius in blocks
	public float rotation;     // mesocyclone rotation -1..1 (positive = cyclonic)
	public boolean hail;
	public float hailSize;     // 0.25..1.5, 1 = basketball-ish
	public float rainIntensity; // 0..1
	public int lightningCooldown;
	public boolean warned;     // whether the warning for its tornado has been issued
	public int warnedEf;
	public double warnedX;
	public double warnedZ;
	public int farTicks;       // ticks spent far from every player

	public Storm(long id, double x, double z, float dir) {
		this.id = id;
		this.x = x;
		this.z = z;
		this.dir = dir;
	}

	public void tickMovement() {
		// Steady drift with slow meandering.
		dir += (float) (Math.random() - 0.5) * 0.01;
		x += Math.cos(dir) * speed;
		z += Math.sin(dir) * speed;
		age++;
	}

	public CompoundTag writeNbt() {
		CompoundTag tag = new CompoundTag();
		tag.putLong("Id", id);
		tag.putDouble("X", x);
		tag.putDouble("Z", z);
		tag.putFloat("Dir", dir);
		tag.putFloat("Speed", speed);
		tag.putInt("Type", type.ordinal());
		tag.putInt("Age", age);
		tag.putFloat("Intensity", intensity);
		tag.putFloat("Radius", radius);
		tag.putFloat("Rotation", rotation);
		tag.putBoolean("Hail", hail);
		tag.putFloat("HailSize", hailSize);
		tag.putFloat("Rain", rainIntensity);
		tag.putBoolean("Warned", warned);
		tag.putInt("WarnedEf", warnedEf);
		tag.putDouble("WarnedX", warnedX);
		tag.putDouble("WarnedZ", warnedZ);
		tag.putInt("FarTicks", farTicks);
		return tag;
	}

	public void readNbt(CompoundTag tag) {
		this.id = tag.getLong("Id");
		this.x = tag.getDouble("X");
		this.z = tag.getDouble("Z");
		this.dir = tag.getFloat("Dir");
		this.speed = tag.getFloat("Speed");
		this.type = StormType.fromOrdinal(tag.getInt("Type"));
		this.age = tag.getInt("Age");
		this.intensity = tag.getFloat("Intensity");
		this.radius = tag.getFloat("Radius");
		this.rotation = tag.getFloat("Rotation");
		this.hail = tag.getBoolean("Hail");
		this.hailSize = tag.getFloat("HailSize");
		this.rainIntensity = tag.getFloat("Rain");
		this.warned = tag.getBoolean("Warned");
		this.warnedEf = tag.getInt("WarnedEf");
		this.warnedX = tag.getDouble("WarnedX");
		this.warnedZ = tag.getDouble("WarnedZ");
		this.farTicks = tag.getInt("FarTicks");
	}

	public float distanceTo(double px, double pz) {
		double dx = x - px;
		double dz = z - pz;
		return (float) Math.sqrt(dx * dx + dz * dz);
	}
}
