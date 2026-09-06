package com.tornadic.item;

import com.tornadic.TornadicMod;
import com.tornadic.config.TornadicConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Deploys the storm chaser research vehicle on the ground.
 */
public class ChaserVehicleItem extends Item {
	public ChaserVehicleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide) {
			return InteractionResult.SUCCESS;
		}
		if (!TornadicConfig.chaserVehiclesEnabled) {
			if (context.getPlayer() instanceof ServerPlayer sp) {
				sp.sendSystemMessage(Component.literal("Vehicles are disabled on this server.")
					.withStyle(ChatFormatting.RED));
			}
			return InteractionResult.FAIL;
		}
		if (context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()) {
			return InteractionResult.CONSUME;
		}
		BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
		Level world = context.getLevel();
		BlockState state = world.getBlockState(pos);
		if (!state.canBeReplaced()) {
			return InteractionResult.FAIL;
		}
		if (world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).bounds().maxY > 0) {
			return InteractionResult.FAIL;
		}
		// Don't stack vehicles.
		for (net.minecraft.world.entity.Entity e : world.getEntities(TornadicMod.CHASER_VEHICLE_TYPE,
			new net.minecraft.world.phys.AABB(
				net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.world.phys.Vec3.atCenterOf(pos)).inflate(2),
			other -> !other.isRemoved())) {
			return InteractionResult.FAIL;
		}
		com.tornadic.entity.ChaserVehicleEntity vehicle =
			com.tornadic.entity.ChaserVehicleEntity.create(TornadicMod.CHASER_VEHICLE_TYPE, world,
				pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5);
		world.addFreshEntity(vehicle);
		context.getItemInHand().shrink(1);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tornadic.chaser_vehicle").withStyle(ChatFormatting.GRAY));
	}
}
