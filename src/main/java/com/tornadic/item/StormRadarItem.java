package com.tornadic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Opens the storm radar overlay (also bound to a key). The radar is deliberately
 * imperfect: it sees organized storms out to a few thousand blocks, hints at
 * rotation, and only flags tornadoes when the circulation is strong enough to
 * show on the scope.
 */
public class StormRadarItem extends Item {
	/** Installed by the client mod. */
	public static ScreenOpening screens;

	public StormRadarItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		if (world.isClientSide && screens != null) {
			screens.openRadar();
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tornadic.storm_radar").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Right-click or press R").withStyle(ChatFormatting.DARK_GRAY));
	}
}
