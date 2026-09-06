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
import net.minecraft.world.level.tooltip.TooltipContext;

import java.util.List;

/**
 * Opens the storm notebook: today's forecast summary and the day's event log
 * (storms, tornadoes, max EF) as recorded by the simulation.
 */
public class StormNotebookItem extends Item {
	/** Installed by the client mod. */
	public static ScreenOpening screens;

	public StormNotebookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		if (world.isClientSide && screens != null) {
			screens.openNotebook();
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tornadic.storm_notebook").withStyle(ChatFormatting.GRAY));
	}
}
