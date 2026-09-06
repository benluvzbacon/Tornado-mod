package com.tornadic.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The storm chaser research vehicle.
 *
 * <p>A lightweight rideable platform: faster than a player on foot, and it shelters
 * the driver from part of the wind/debris damage. It is deliberately not a tank -
 * tornadoes still destroy it (and fling the driver). Visuals are particle based
 * (see VehicleVisuals on the client) to keep rendering cheap.
 */
public class ChaserVehicleEntity extends Entity {
	private static final EntityDataAccessor<Boolean> DEPLOYED =
		SynchedEntityData.defineId(ChaserVehicleEntity.class, EntityDataSerializers.BOOLEAN);

	public ChaserVehicleEntity(EntityType<? extends ChaserVehicleEntity> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
	}

	public static ChaserVehicleEntity create(EntityType<? extends ChaserVehicleEntity> type, Level level,
		double x, double y, double z) {
		ChaserVehicleEntity entity = new ChaserVehicleEntity(type, level);
		entity.setPos(x, y, z);
		return entity;
	}

	@Override
	public boolean canRide(Entity entity) {
		return entity instanceof Player && getPassengers().isEmpty();
	}

	public boolean isDeployed() {
		return entityData.get(DEPLOYED);
	}

	public void setDeployed(boolean deployed) {
		entityData.set(DEPLOYED, deployed);
		if (deployed) setDeltaMovement(Vec3.ZERO);
	}

	@Override
	public net.minecraft.world.InteractionResult interact(Player player, net.minecraft.world.InteractionHand hand) {
		if (!level().isClientSide) {
			if (player.isSecondaryUseActive()) {
				setDeployed(!isDeployed());
				player.sendSystemMessage(Component.literal(isDeployed()
					? "TIV 2 deployed: skirts lowered and hydraulic spikes anchored."
					: "TIV 2 deployment retracted.").withStyle(isDeployed() ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
				player.swing(hand);
			} else if (isDeployed()) {
				player.sendSystemMessage(Component.literal("Retract deployment with sneak + right-click before driving.")
					.withStyle(ChatFormatting.YELLOW));
			} else if (!getPassengers().isEmpty()) {
				player.stopRiding();
			} else {
				player.startRiding(this);
				player.swing(hand);
			}
		}
		return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide) {
			return;
		}
		if (isDeployed()) {
			setDeltaMovement(Vec3.ZERO);
		} else if (!getPassengers().isEmpty()) {
			Player driver = (Player) getControllingPassenger();
		// Research vehicle handling: steady forward force, a little speed, no superpowers.
		float yaw = (float) (driver.getYRot() * Math.PI / 180.0);
		float sprintFactor = driver.isSprinting() ? 1.35f : 1.0f;
		double forward = 0.06 * sprintFactor;
		// Vanilla forward vector at a given yaw: (-sin, cos).
		Vec3 vel = getDeltaMovement();
		setDeltaMovement(new Vec3(
			vel.x * 0.85 - Math.sin(yaw) * forward,
			vel.y,
			vel.z * 0.85 + Math.cos(yaw) * forward
		));
	} else {
			// Coasting friction.
			Vec3 vel = getDeltaMovement();
			setDeltaMovement(vel.multiply(0.9, 0.9, 0.9));
		}
		// Discard if it falls far out of the world.
		if (getY() < (level() instanceof ServerLevel sl ? sl.getMinBuildHeight() : 0) - 64) {
			discard();
		}
	}


	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		setDeployed(tag.getBoolean("Deployed"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putBoolean("Deployed", isDeployed());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DEPLOYED, false);
	}
}
