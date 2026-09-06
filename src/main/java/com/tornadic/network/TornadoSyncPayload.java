package com.tornadic.network;

import com.tornadic.tornado.TornadoState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

	public static final StreamCodec<FriendlyByteBuf, TornadoSyncPayload> STREAM_CODEC = StreamCodec.of(
		TornadoSyncPayload::write, TornadoSyncPayload::read);

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(entityId);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(funnelRadius);
		buf.writeFloat(cloudRadius);
		buf.writeVarInt(ef);
		buf.writeFloat(windMs);
		buf.writeFloat(heading);
		buf.writeVarInt(groundTone);
	}

	private static TornadoSyncPayload read(FriendlyByteBuf buf) {
		return new TornadoSyncPayload(
			buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
			buf.readFloat(), buf.readFloat(),
			buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt()
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static TornadoSyncPayload of(TornadoState state, int entityId, int groundTone) {
		return new TornadoSyncPayload(entityId, state.x, state.y, state.z, state.funnelRadius(),
			state.cloudRadius(), state.currentEf(), state.windMs(), state.heading, groundTone);
	}
}
