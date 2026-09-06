package com.tornadic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Schedules a local thunder rumble on the client. The delay emulates the distance
 * between the flash (instant) and the sound (speed of sound), so distant lightning
 * rumbles late and quiet.
 */
public record ThunderPayload(double x, double z, float volume, float pitch, int delayTicks)
	implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tornadic", "thunder");
	public static final CustomPacketPayload.Type<ThunderPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, ThunderPayload> STREAM_CODEC = StreamCodec.composite(
		FriendlyByteBuf::writeDouble, ThunderPayload::x,
		FriendlyByteBuf::writeDouble, ThunderPayload::z,
		FriendlyByteBuf::writeFloat, ThunderPayload::volume,
		FriendlyByteBuf::writeFloat, ThunderPayload::pitch,
		FriendlyByteBuf::writeVarInt, ThunderPayload::delayTicks,
		ThunderPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
