package com.tornadic.network;

import com.tornadic.storm.Storm;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-storm state, broadcast to players near the storm. The client uses these to
 * render localized rain/hail, draw the radar and compute local wind.
 */
public record StormSyncPayload(
	long id,
	double x,
	double z,
	float radius,
	int typeOrdinal,
	float intensity,
	float rotation,
	float rainIntensity,
	boolean hail,
	float hailSize,
	float dir,
	float speed,
	int tornadoEf
) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tornadic", "storm_sync");
	public static final CustomPacketPayload.Type<StormSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, StormSyncPayload> STREAM_CODEC = StreamCodec.composite(
		FriendlyByteBuf::writeVarLong, StormSyncPayload::id,
		FriendlyByteBuf::writeDouble, StormSyncPayload::x,
		FriendlyByteBuf::writeDouble, StormSyncPayload::z,
		FriendlyByteBuf::writeFloat, StormSyncPayload::radius,
		FriendlyByteBuf::writeVarInt, StormSyncPayload::typeOrdinal,
		FriendlyByteBuf::writeFloat, StormSyncPayload::intensity,
		FriendlyByteBuf::writeFloat, StormSyncPayload::rotation,
		FriendlyByteBuf::writeFloat, StormSyncPayload::rainIntensity,
		FriendlyByteBuf::writeBoolean, StormSyncPayload::hail,
		FriendlyByteBuf::writeFloat, StormSyncPayload::hailSize,
		FriendlyByteBuf::writeFloat, StormSyncPayload::dir,
		FriendlyByteBuf::writeFloat, StormSyncPayload::speed,
		FriendlyByteBuf::writeVarInt, StormSyncPayload::tornadoEf,
		StormSyncPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static StormSyncPayload of(Storm storm, int tornadoEf) {
		return new StormSyncPayload(storm.id, storm.x, storm.z, storm.radius, storm.type.ordinal(),
			storm.intensity, storm.rotation, storm.rainIntensity, storm.hail, storm.hailSize,
			storm.dir, storm.speed, tornadoEf);
	}
}
