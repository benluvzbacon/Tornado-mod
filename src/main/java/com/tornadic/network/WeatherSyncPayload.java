package com.tornadic.network;

import com.tornadic.weather.DailyForecast;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

	public static final StreamCodec<FriendlyByteBuf, WeatherSyncPayload> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public void encode(FriendlyByteBuf buf, WeatherSyncPayload p) {
			buf.writeVarInt(p.day());
			buf.writeFloat(p.tempF());
			buf.writeFloat(p.dewPointF());
			buf.writeVarInt(p.humidity());
			buf.writeFloat(p.pressureMb());
			buf.writeFloat(p.windMph());
			buf.writeFloat(p.windDir());
			buf.writeVarInt(p.cape());
			buf.writeFloat(p.shear());
			buf.writeVarInt(p.stormProbability());
			buf.writeVarInt(p.tornadoProbability());
			buf.writeVarInt(p.riskOrdinal());
			buf.writeFloat(p.rainLevel());
			buf.writeFloat(p.thunderLevel());
			buf.writeVarInt(p.stormsToday());
			buf.writeVarInt(p.tornadoesToday());
			buf.writeVarInt(p.maxEfToday());
		}

		@Override
		public WeatherSyncPayload decode(FriendlyByteBuf buf) {
			return new WeatherSyncPayload(
				buf.readVarInt(), buf.readFloat(), buf.readFloat(),
				buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
				buf.readVarInt(), buf.readFloat(),
				buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readFloat(), buf.readFloat(),
				buf.readVarInt(), buf.readVarInt(), buf.readVarInt()
			);
		}
	};

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
