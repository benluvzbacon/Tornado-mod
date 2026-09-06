package com.tornadic.client;

import com.tornadic.TornadicMod;
import com.tornadic.item.ScreenOpening;
import com.tornadic.item.StormNotebookItem;
import com.tornadic.item.StormRadarItem;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Client entry point: renderers, key bindings, payload receivers, screens and
 * the per-frame weather visuals.
 */
public class TornadicClientMod implements ClientModInitializer {
	public static KeyMapping RADAR_KEY;
	public static KeyMapping DEBUG_KEY;

	@Override
	public void onInitializeClient() {
		// Renderers (both are particle-visual shells; the entity itself is invisible).
		EntityRendererRegistry.register(TornadicMod.TORNADO_TYPE, TornadoRenderer::new);
		EntityRendererRegistry.register(TornadicMod.CHASER_VEHICLE_TYPE, ChaserVehicleRenderer::new);

		// Screen hooks for the common item classes.
		ScreenOpening screens = new ScreenOpening() {
			@Override
			public void openRadar() {
				Minecraft client = Minecraft.getInstance();
				client.execute(() -> client.setScreen(new RadarScreen()));
			}

			@Override
			public void openNotebook() {
				Minecraft client = Minecraft.getInstance();
				client.execute(() -> client.setScreen(new NotebookScreen()));
			}
		};
		StormRadarItem.screens = screens;
		StormNotebookItem.screens = screens;

		// Key bindings.
		RADAR_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.tornadic.radar", GLFW.GLFW_KEY_R, "key.category.tornadic"));
		DEBUG_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.tornadic.debug", GLFW.GLFW_KEY_V, "key.category.tornadic"));

		// Payload receivers.
		ClientPlayNetworking.registerGlobalReceiver(com.tornadic.network.WeatherSyncPayload.TYPE, (payload, context) -> {
			ClientWeatherState.weather = payload;
		});
		ClientPlayNetworking.registerGlobalReceiver(com.tornadic.network.StormSyncPayload.TYPE, (payload, context) -> {
			ClientWeatherState.putStorm(payload);
		});
		ClientPlayNetworking.registerGlobalReceiver(com.tornadic.network.TornadoSyncPayload.TYPE, (payload, context) -> {
			ClientWeatherState.putTornado(payload);
		});
		ClientPlayNetworking.registerGlobalReceiver(com.tornadic.network.ThunderPayload.TYPE, (payload, context) -> {
			ClientWeatherState.scheduleThunder(payload.x(), payload.z(), payload.volume(), payload.pitch(), payload.delayTicks());
		});

		// Per-frame client work.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) {
				return;
			}
			while (RADAR_KEY.consumeClick()) {
				client.setScreen(new RadarScreen());
			}
			if (DEBUG_KEY.consumeClick()) {
				com.tornadic.config.TornadicConfig.debugHud = !com.tornadic.config.TornadicConfig.debugHud;
			}
			ClientWeatherState.expire(4000);
			WeatherVisuals.tick(client);
			VehicleVisuals.tick(client);
		});

		HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
			AnemometerHud.render(graphics);
			DebugHud.render(graphics);
		});
	}
}
