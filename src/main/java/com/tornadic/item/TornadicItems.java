package com.tornadic.item;

import com.tornadic.block.AnemometerBlock;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registers Tornadic's weather instruments and dedicated creative tab. */
public final class TornadicItems {
	private TornadicItems() {}

	public static Block ANEMOMETER_BLOCK;
	public static Item WEATHER_RADIO;
	public static Item ANEMOMETER;
	public static Item THERMOMETER;
	public static Item BAROMETER;
	public static Item STORM_RADAR;
	public static Item STORM_NOTEBOOK;
	public static Item CHASER_VEHICLE;
	public static CreativeModeTab WEATHER_TAB;

	public static void register() {
		ANEMOMETER_BLOCK = Registry.register(BuiltInRegistries.BLOCK, "tornadic:anemometer",
			new AnemometerBlock(BlockBehaviour.Properties.of().strength(0.8f).noOcclusion()));
		WEATHER_RADIO = Registry.register(BuiltInRegistries.ITEM, "tornadic:weather_radio",
			new WeatherRadioItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
		ANEMOMETER = Registry.register(BuiltInRegistries.ITEM, "tornadic:anemometer",
			new AnemometerItem(ANEMOMETER_BLOCK, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
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

		WEATHER_TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
			ResourceLocation.fromNamespaceAndPath("tornadic", "weather"),
			FabricItemGroup.builder()
				.title(Component.translatable("itemGroup.tornadic.weather"))
				.icon(() -> new ItemStack(ANEMOMETER))
				.displayItems((parameters, output) -> {
					output.accept(ANEMOMETER);
					output.accept(THERMOMETER);
					output.accept(BAROMETER);
					output.accept(WEATHER_RADIO);
					output.accept(STORM_RADAR);
					output.accept(STORM_NOTEBOOK);
					output.accept(CHASER_VEHICLE);
				})
				.build());
	}
}
