package com.tornadic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.tornadic.tornado.TornadoState;

public record TornadoSyncPayload(
	int entityId,
	double x,
	double y,
	double z,
	float funnelRadius,
	float cloudRadius,
	int ef,
	float windMs,
	float heading,
	int groundTone
) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tornadic", "tornado_sync");
	public static final CustomPacketPayload.Type<TornadoSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, TornadoSyncPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public void encode(FriendlyByteBuf buf, TornadoSyncPayload p) {
			buf.writeVarInt(p.entityId());
			buf.writeDouble(p.x());
			buf.writeDouble(p.y());
			buf.writeDouble(p.z());
			buf.writeFloat(p.funnelRadius());
			buf.writeFloat(p.cloudRadius());
			buf.writeVarInt(p.ef());
			buf.writeFloat(p.windMs());
			buf.writeFloat(p.heading());
			buf.writeVarInt(p.groundTone());
		}

		@Override
		public TornadoSyncPayload decode(FriendlyByteBuf buf) {
			return new TornadoSyncPayload(
				buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
				buf.readFloat(), buf.readFloat(),
				buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt()
			);
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static TornadoSyncPayload of(TornadoState state, int entityId, int groundTone) {
		return new TornadoSyncPayload(entityId, state.x, state.y, state.z,
			state.funnelRadius(), state.cloudRadius(), state.currentEf(),
			state.windMs(), state.heading, groundTone);
	}
}
