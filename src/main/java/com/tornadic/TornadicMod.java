package com.tornadic;

import com.tornadic.command.TornadicCommands;
import com.tornadic.config.TornadicConfig;
import com.tornadic.entity.ChaserVehicleEntity;
import com.tornadic.item.TornadicItems;
import com.tornadic.network.TornadicPayloads;
import com.tornadic.saveddata.TornadicSavedData;
import com.tornadic.tornado.TornadoEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Tornadic - realistic tornadoes, supercells and severe weather.
 *
 * <p>The simulation is server-authoritative and lives in {@link TornadicSavedData};
 * clients only render synced state (particles, screens, HUD, sounds).
 */
public class TornadicMod implements ModInitializer {
	public static final String MOD_ID = "tornadic";
	public static final Logger LOGGER = LoggerFactory.getLogger("Tornadic");

	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister<EntityType<?>> ENTITY_TYPES =
		net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.create(
			net.minecraft.core.registries.Registries.ENTITY_TYPE, MOD_ID);
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<EntityType<TornadoEntity>> TORNADO_TYPE;
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<EntityType<ChaserVehicleEntity>> CHASER_VEHICLE_TYPE;

	@Override
	public void onInitialize() {
		TornadicConfig.load();
		registerEntities();
		TornadicPayloads.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			TornadicCommands.register(dispatcher);
		});

		// The whole simulation runs on the overworld tick, server side only.
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.isClientSide) {
				return;
			}
			if (world.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
				return;
			}
			try {
				TornadicSavedData.getOrLoad(world.getServer()).tick(world);
			} catch (Throwable t) {
				LOGGER.error("Tornadic simulation tick failed", t);
			}
		});

		// New players get the full weather state immediately.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			try {
				TornadicSavedData.getOrLoad(server).syncToPlayer(server.overworld(), player);
			} catch (Throwable t) {
				LOGGER.error("Tornadic player sync failed", t);
			}
		});

		LOGGER.info("Tornadic loaded - may the winds be in your favor.");
	}

	static {
		TORNADO_TYPE = ENTITY_TYPES.register("tornado", () ->
			EntityType.Builder.of(TornadoEntity::new, MobCategory.MISC)
				.sized(1.0F, 1.0F)
				.clientTrackingRange(12)
				.updateInterval(2)
				.fireImmune()
				.noSave()
				.build());
		CHASER_VEHICLE_TYPE = ENTITY_TYPES.register("chaser_vehicle", () ->
			EntityType.Builder.of(ChaserVehicleEntity::new, MobCategory.MISC)
				.sized(1.6F, 1.0F)
				.clientTrackingRange(8)
				.updateInterval(3)
				.fireImmune()
				.noSummon()
				.build());
	}
}
