package com.tornadic.item;

import com.tornadic.saveddata.TornadicSavedData;
import com.tornadic.wind.WindField;

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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Measures the local wind: speed (mph) and direction from the full wind field
 * (daily wind + storms + tornado vortexes).
 */
public class AnemometerItem extends Item {
	public AnemometerItem(Properties properties) {
		super(properties);
	}

	@Override
	public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(
		Level world, Player player, InteractionHand hand) {
		if (!world.isClientSide && player instanceof ServerPlayer sp) {
			TornadicSavedData data = TornadicSavedData.getOrLoad(sp.getServer());
			Vec3 wind = WindField.windAt(sp.serverLevel(), data.currentForecast(sp.serverLevel()),
				data.storms(), data.tornadoes(), sp.getX(), sp.getY(), sp.getZ());
			double blocksPerSec = Math.sqrt(wind.x * wind.x + wind.z * wind.z) * 20.0;
			double mph = blocksPerSec * 2.23694;
			String dir = "--";
			if (mph > 0.5) {
				double deg = Math.toDegrees(Math.atan2(wind.z, wind.x));
				if (deg < 0) deg += 360;
				String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
				dir = dirs[((int) Math.round(deg / 45.0)) & 7];
			}
			String condition = mph < 3 ? "Calm" : mph < 18 ? "Breezy" : mph < 45 ? "Strong" : mph < 90 ? "Severe" : "Extreme";
			sp.sendSystemMessage(Component.literal("ANEMOMETER  ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
				.append(Component.literal(String.format("Wind %.1f mph  |  Direction %s  |  %s", mph, dir, condition))
					.withStyle(ChatFormatting.WHITE)));
		}
		return net.minecraft.world.InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tornadic.anemometer").withStyle(ChatFormatting.GRAY));
	}
}
