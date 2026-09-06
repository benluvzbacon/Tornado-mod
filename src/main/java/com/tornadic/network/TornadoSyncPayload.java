package com.tornadic.network;

import com.tornadic.tornado.TornadoState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-tornado visual state, sent to players tracking the tornado entity.
 */
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

	public static final StreamCodec<FriendlyByteBuf, TornadoSyncPayload> STREAM_CODEC = StreamCodec.composite(
		FriendlyByteBuf::writeVarInt, TornadoSyncPayload::entityId,
		FriendlyByteBuf::writeDouble, TornadoSyncPayload::x,
		FriendlyByteBuf::writeDouble, TornadoSyncPayload::y,
		FriendlyByteBuf::writeDouble, TornadoSyncPayload::z,
		FriendlyByteBuf::writeFloat, TornadoSyncPayload::funnelRadius,
		FriendlyByteBuf::writeFloat, TornadoSyncPayload::cloudRadius,
		FriendlyByteBuf::writeVarInt, TornadoSyncPayload::ef,
		FriendlyByteBuf::writeFloat, TornadoSyncPayload::windMs,
		FriendlyByteBuf::writeFloat, TornadoSyncPayload::heading,
		FriendlyByteBuf::writeVarInt, TornadoSyncPayload::groundTone,
		TornadoSyncPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static TornadoSyncPayload of(TornadoState state, int entityId, int groundTone) {
		return new TornadoSyncPayload(entityId, state.x, state.y, state.z, state.funnelRadius(),
			state.cloudRadius(), state.currentEf(), state.windMs(), state.heading, groundTone);
	}
}
