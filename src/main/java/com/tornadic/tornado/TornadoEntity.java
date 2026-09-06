package com.tornadic.tornado;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Visual/tracking vehicle for a {@link TornadoState}.
 *
 * <p>Not saved to disk (the authoritative state lives in TornadicSavedData); the
 * system recreates one after world load. The client renderer is a no-op - all
 * visuals are particles spawned on the client from synced state, which keeps
 * rendering cheap and safe.
 */
public class TornadoEntity extends Entity {
	private long tornadoId;

	public TornadoEntity(EntityType<? extends TornadoEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.noGravity = true;
	}

	public static TornadoEntity create(EntityType<? extends TornadoEntity> type, Level level, long tornadoId,
		double x, double y, double z) {
		TornadoEntity entity = new TornadoEntity(type, level);
		entity.tornadoId = tornadoId;
		entity.setPos(x, y, z);
		return entity;
	}

	public long tornadoId() {
		return tornadoId;
	}

	public void setTornadoId(long id) {
		this.tornadoId = id;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.tornadoId = tag.getLong("TornadoId");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putLong("TornadoId", tornadoId);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}
}
