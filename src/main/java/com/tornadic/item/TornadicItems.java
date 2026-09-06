package com.tornadic.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * Storm-chasing equipment. Items are simple, no-recipe tools - the point is the
 * information they provide, not the crafting.
 */
public final class TornadicItems {
	private TornadicItems() {
	}

	public static final Item WEATHER_RADIO;
	public static final Item ANEMOMETER;
	public static final Item THERMOMETER;
	public static final Item BAROMETER;
	public static final Item STORM_RADAR;
	public static final Item STORM_NOTEBOOK;
	public static final Item CHASER_VEHICLE;

	static {
		WEATHER_RADIO = Registry.register(BuiltInRegistries.ITEM, "tornadic:weather_radio",
			new WeatherRadioItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
		ANEMOMETER = Registry.register(BuiltInRegistries.ITEM, "tornadic:anemometer",
			new AnemometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
		THERMOMETER = Registry.register(BuiltInRegistries.ITEM, "tornadic:thermometer",
			new ThermometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
		BAROMETER = Registry.register(BuiltInRegistries.ITEM, "tornadic:barometer",
			new BarometerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
		STORM_RADAR = Registry.register(BuiltInRegistries.ITEM, "tornadic:storm_radar",
			new StormRadarItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
		STORM_NOTEBOOK = Registry.register(BuiltInRegistries.ITEM, "tornadic:storm_notebook",
			new StormNotebookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
		CHASER_VEHICLE = Registry.register(BuiltInRegistries.ITEM, "tornadic:chaser_vehicle",
			new ChaserVehicleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
	}
}
