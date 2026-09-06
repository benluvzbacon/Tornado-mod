package com.tornadic.client;

import java.util.List;

import com.tornadic.entity.ChaserVehicleEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.DustParticleOptions;
import net.minecraft.client.particle.ParticleTypes;
import net.minecraft.util.RandomSource;

/**
 * Particle body for chaser vehicles: a dark research-suv silhouette made of
 * soft dust puffs plus a blinking roof light. Cheap: a handful of particles
 * every few ticks per vehicle in view.
 */
public final class VehicleVisuals {
	private VehicleVisuals() {
	}

	private static final RandomSource RNG = RandomSource.create();

	public static void tick(Minecraft client) {
		if (client.player == null || client.level == null) {
			return;
		}
		if (client.level.getGameTime() % 4 != 0) {
			return;
		}
		List<ChaserVehicleEntity> vehicles =
			client.level.getEntitiesOfClass(ChaserVehicleEntity.class,
				client.player.getBoundingBox().inflate(64.0D));
		for (ChaserVehicleEntity v : vehicles) {
			if (v.distanceTo(client.player) > 64) {
				continue;
			}
			float yaw = (float) v.getYRot() * (float) Math.PI / 180.0;
			double fx = -Math.sin(yaw);
			double fz = Math.cos(yaw);
			double rx = fz;
			double rz = -fx;
			// Body: ~2x1.6 footprint, rotated into world space.
			for (int i = 0; i < 6; i++) {
				double lf = RNG.nextFloat() * 1.6 - 0.8; // along forward
				double lr = RNG.nextFloat() * 1.0 - 0.5; // along right
				double wx = v.getX() + lf * fx + lr * rx;
				double wz = v.getZ() + lf * fz + lr * rz;
				double wy = v.getY() + 0.2 + RNG.nextFloat() * 0.8;
				client.level.addParticle(
					new DustParticleOptions(new org.joml.Vector3f(58 / 255.0F, 64 / 255.0F, 72 / 255.0F), 0.55f),
					wx, wy, wz, 0, 0, 0);
			}
			// Blinking research light on the roof.
			if ((v.level().getGameTime() / 10) % 2 == 0) {
				client.level.addParticle(ParticleTypes.ENCHANT,
					v.getX(), v.getY() + 1.25, v.getZ(), 0, 0.02, 0);
			}
		}
	}
}
