package com.tornadic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.tornadic.storm.Storm;

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

	public static final StreamCodec<FriendlyByteBuf, StormSyncPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public void encode(FriendlyByteBuf buf, StormSyncPayload p) {
			buf.writeVarLong(p.id());
			buf.writeDouble(p.x());
			buf.writeDouble(p.z());
			buf.writeFloat(p.radius());
			buf.writeVarInt(p.typeOrdinal());
			buf.writeFloat(p.intensity());
			buf.writeFloat(p.rotation());
			buf.writeFloat(p.rainIntensity());
			buf.writeBoolean(p.hail());
			buf.writeFloat(p.hailSize());
			buf.writeFloat(p.dir());
			buf.writeFloat(p.speed());
			buf.writeVarInt(p.tornadoEf());
		}

		@Override
		public StormSyncPayload decode(FriendlyByteBuf buf) {
			return new StormSyncPayload(
				buf.readVarLong(), buf.readDouble(), buf.readDouble(),
				buf.readFloat(), buf.readVarInt(),
				buf.readFloat(), buf.readFloat(), buf.readFloat(),
				buf.readBoolean(), buf.readFloat(),
				buf.readFloat(), buf.readFloat(), buf.readVarInt()
			);
		}
	};

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
