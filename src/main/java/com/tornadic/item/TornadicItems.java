package com.tornadic.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.CreativeModeTabs;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

/**
 * Storm-chasing equipment. Items are simple, no-recipe tools - the point is the
 * information they provide, not the crafting.
 */
public final class TornadicItems {
	private TornadicItems() {
	}

	public static Item WEATHER_RADIO;
	public static Item ANEMOMETER;
	public static Item THERMOMETER;
	public static Item BAROMETER;
	public static Item STORM_RADAR;
	public static Item STORM_NOTEBOOK;
	public static Item CHASER_VEHICLE;

	/** Called from the mod initializer, before any world loads. */
	public static void register() {
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

		// Make every instrument genuinely obtainable from the vanilla creative UI.
		// The anemometer also has a survival crafting recipe under data/tornadic/recipe.
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
			entries.accept(ANEMOMETER);
			entries.accept(THERMOMETER);
			entries.accept(BAROMETER);
			entries.accept(WEATHER_RADIO);
			entries.accept(STORM_RADAR);
			entries.accept(STORM_NOTEBOOK);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries ->
			entries.accept(CHASER_VEHICLE));
	}
}
