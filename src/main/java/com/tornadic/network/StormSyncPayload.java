package com.tornadic.network;

import com.tornadic.storm.Storm;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

	public static final StreamCodec<FriendlyByteBuf, StormSyncPayload> STREAM_CODEC = StreamCodec.of(
		StormSyncPayload::write, StormSyncPayload::read);

	private void write(FriendlyByteBuf buf) {
		buf.writeVarLong(id);
		buf.writeDouble(x);
		buf.writeDouble(z);
		buf.writeFloat(radius);
		buf.writeVarInt(typeOrdinal);
		buf.writeFloat(intensity);
		buf.writeFloat(rotation);
		buf.writeFloat(rainIntensity);
		buf.writeBoolean(hail);
		buf.writeFloat(hailSize);
		buf.writeFloat(dir);
		buf.writeFloat(speed);
		buf.writeVarInt(tornadoEf);
	}

	private static StormSyncPayload read(FriendlyByteBuf buf) {
		return new StormSyncPayload(
			buf.readVarLong(), buf.readDouble(), buf.readDouble(),
			buf.readFloat(), buf.readVarInt(),
			buf.readFloat(), buf.readFloat(), buf.readFloat(),
			buf.readBoolean(), buf.readFloat(),
			buf.readFloat(), buf.readFloat(), buf.readVarInt()
		);
	}

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
