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

import java.util.List;

/**
 * Reads the simulated air temperature (daily forecast, with a light local dip under
 * active storm clouds).
 */
public class ThermometerItem extends Item {
	public ThermometerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		if (!world.isClientSide && player instanceof ServerPlayer sp) {
			TornadicSavedData data = TornadicSavedData.getOrLoad(sp.getServer());
			float temp = data.currentForecast(sp.level()).tempF();
			// Storms are cooler than their surroundings.
			double minStormDist = Double.MAX_VALUE;
			for (var storm : data.storms()) {
				double d = storm.distanceTo(sp.getX(), sp.getZ());
				if (d < storm.radius * 1.5) {
					minStormDist = Math.min(minStormDist, d);
				}
			}
			if (minStormDist < Double.MAX_VALUE) {
				float falloff = (float) Math.max(0.0, 1.0 - minStormDist / 700.0);
				temp -= 8.0f * falloff * 0.5f;
			}
			sp.sendSystemMessage(Component.literal("Temperature: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(String.format("%.0f°F", temp)).withStyle(ChatFormatting.RED))
				.append(Component.literal(String.format("  (%.0f°C)", (temp - 32.0f) * 5.0f / 9.0f)).withStyle(ChatFormatting.DARK_GRAY)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tornadic.thermometer").withStyle(ChatFormatting.GRAY));
	}
}
