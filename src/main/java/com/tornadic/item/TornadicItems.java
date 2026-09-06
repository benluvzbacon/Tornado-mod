package com.tornadic.item;

import com.tornadic.TornadicMod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Storm-chasing equipment. Items are simple, no-recipe tools - the point is the
 * information they provide, not the crafting.
 */
public final class TornadicItems {
	private TornadicItems() {
	}

	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister<Item> ITEMS =
		net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.create(
			net.minecraft.core.registries.Registries.ITEM, TornadicMod.MOD_ID);

	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<Item> WEATHER_RADIO =
		ITEMS.register("weather_radio", () -> new WeatherRadioItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<Item> ANEMOMETER =
		ITEMS.register("anemometer", () -> new AnemometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<Item> THERMOMETER =
		ITEMS.register("thermometer", () -> new ThermometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<Item> BAROMETER =
		ITEMS.register("barometer", () -> new BarometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<Item> STORM_RADAR =
		ITEMS.register("storm_radar", () -> new StormRadarItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<Item> STORM_NOTEBOOK =
		ITEMS.register("storm_notebook", () -> new StormNotebookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister.DeferredEntry<Item> CHASER_VEHICLE =
		ITEMS.register("chaser_vehicle", () -> new ChaserVehicleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
}
