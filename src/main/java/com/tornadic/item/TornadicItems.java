package com.tornadic.item;

import com.tornadic.TornadicMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Storm-chasing equipment. Items are simple, no-recipe tools - the point is the
 * information they provide, not the crafting.
 */
public final class TornadicItems {
	private TornadicItems() {
	}

	/** Forces class initialization (registers all items). */
	public static void ensureRegistered() {
	}

	public static final Item WEATHER_RADIO = register("weather_radio",
		new WeatherRadioItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final Item ANEMOMETER = register("anemometer",
		new AnemometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final Item THERMOMETER = register("thermometer",
		new ThermometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final Item BAROMETER = register("barometer",
		new BarometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final Item STORM_RADAR = register("storm_radar",
		new StormRadarItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	public static final Item STORM_NOTEBOOK = register("storm_notebook",
		new StormNotebookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
	public static final Item CHASER_VEHICLE = register("chaser_vehicle",
		new ChaserVehicleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

	private static Item register(String name, Item item) {
		return net.fabricmc.fabric.api.registry.v1.Registry.register(net.minecraft.core.registries.Registries.ITEM, TornadicMod.MOD_ID + ":" + name, item);
	}
}
