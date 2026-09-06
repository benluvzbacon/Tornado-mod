package com.tornadic.item;

import com.tornadic.saveddata.TornadicSavedData;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.tooltip.TooltipContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads atmospheric pressure. Pressure falls as storms close in, and the trend
 * arrow (rising/falling/steady) is the classic "the weather is turning" signal.
 */
public class BarometerItem extends Item {
	private static final Map<UUID, float[]> LAST_READINGS = new HashMap<>();

	public BarometerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		if (!world.isClientSide && player instanceof ServerPlayer sp) {
			TornadicSavedData data = TornadicSavedData.getOrLoad(sp.getServer());
			float pressure = data.currentForecast(sp.level()).pressureMb();
			// Approaching storms depress the local pressure.
			for (var storm : data.storms()) {
				double d = storm.distanceTo(sp.getX(), sp.getZ());
				if (d < 2200) {
					float falloff = (float) Math.max(0.0, 1.0 - d / 2200.0);
					pressure -= 14.0f * falloff * storm.intensity;
				}
			}
			float[] last = LAST_READINGS.computeIfAbsent(sp.getUUID(), k -> new float[]{-1f, 0f});
			float trend;
			if (last[0] < 0) {
				trend = 0f;
			} else {
				trend = pressure - last[0];
			}
			last[0] = pressure;
			String trendText;
			ChatFormatting trendColor;
			if (Math.abs(trend) < 0.4f) {
				trendText = "steady";
				trendColor = ChatFormatting.GRAY;
			} else if (trend < 0) {
				trendText = "falling";
				trendColor = ChatFormatting.RED;
			} else {
				trendText = "rising";
				trendColor = ChatFormatting.GREEN;
			}
			sp.sendSystemMessage(Component.literal("Pressure: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(String.format("%.1f hPa", pressure)).withStyle(ChatFormatting.CYAN))
				.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.literal(trendText).withStyle(trendColor)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tornadic.barometer").withStyle(ChatFormatting.GRAY));
	}
}
