package com.tornadic.network;

import com.tornadic.weather.DailyForecast;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Global atmospheric state broadcast to all players (rate limited, only when it changes).
 */
public record WeatherSyncPayload(
	int day,
	float tempF,
	float dewPointF,
	int humidity,
	float pressureMb,
	float windMph,
	float windDir,
	int cape,
	float shear,
	int stormProbability,
	int tornadoProbability,
	int riskOrdinal,
	float rainLevel,
	float thunderLevel,
	int stormsToday,
	int tornadoesToday,
	int maxEfToday
) implements CustomPacketPayload {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("tornadic", "weather_sync");
	public static final CustomPacketPayload.Type<WeatherSyncPayload> TYPE = new CustomPacketPayload.Type<>(ID);

	public static final StreamCodec<FriendlyByteBuf, WeatherSyncPayload> STREAM_CODEC = StreamCodec.composite(
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::day,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::tempF,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::dewPointF,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::humidity,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::pressureMb,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::windMph,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::windDir,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::cape,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::shear,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::stormProbability,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::tornadoProbability,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::riskOrdinal,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::rainLevel,
		FriendlyByteBuf::writeFloat, WeatherSyncPayload::thunderLevel,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::stormsToday,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::tornadoesToday,
		FriendlyByteBuf::writeVarInt, WeatherSyncPayload::maxEfToday,
		WeatherSyncPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static WeatherSyncPayload of(DailyForecast forecast, float rainLevel, float thunderLevel,
		int stormsToday, int tornadoesToday, int maxEfToday) {
		return new WeatherSyncPayload(
			forecast.day(), forecast.tempF(), forecast.dewPointF(), forecast.humidity(),
			forecast.pressureMb(), forecast.windMph(), forecast.windDir(), forecast.cape(),
			forecast.shear(), forecast.stormProbability(), forecast.tornadoProbability(),
			forecast.risk().ordinal(), rainLevel, thunderLevel,
			stormsToday, tornadoesToday, maxEfToday
		);
	}
}
