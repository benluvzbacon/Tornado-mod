package com.tornadic.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers all Tornadic custom payloads on both sides.
 */
public final class TornadicPayloads {
	private TornadicPayloads() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(WeatherSyncPayload.TYPE, WeatherSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(StormSyncPayload.TYPE, StormSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(TornadoSyncPayload.TYPE, TornadoSyncPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ThunderPayload.TYPE, ThunderPayload.STREAM_CODEC);
	}
}
