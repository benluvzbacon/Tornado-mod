package com.tornadic.block;

import com.tornadic.saveddata.TornadicSavedData;
import com.tornadic.wind.WindField;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Placeable server-driven cup anemometer. Its block model physically rotates. */
public class AnemometerBlock extends Block {
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

	public AnemometerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (level instanceof ServerLevel server) server.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		double mph = windMph(level, pos);
		if (mph > 0.35) {
			level.setBlock(pos, state.cycle(ROTATION), Block.UPDATE_CLIENTS);
		}
		// Calm cups barely move; severe/tornadic wind visibly accelerates them.
		int delay = mph < 1 ? 20 : mph < 10 ? 10 : mph < 30 ? 6 : mph < 70 ? 3 : 1;
		level.scheduleTick(pos, this, delay);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// Restarts animation for instruments loaded from an older save/chunk.
		if (!level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 1);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
		Player player, BlockHitResult hit) {
		if (!level.isClientSide && level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
			double mph = windMph(server, pos);
			Vec3 wind = wind(server, pos);
			double deg = Math.toDegrees(Math.atan2(wind.z, wind.x));
			if (deg < 0) deg += 360.0;
			String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
			String direction = mph < 0.5 ? "--" : dirs[((int) Math.round(deg / 45.0)) & 7];
			String condition = mph < 3 ? "CALM" : mph < 18 ? "BREEZY" : mph < 45 ? "STRONG" : mph < 90 ? "SEVERE" : "EXTREME";
			sp.sendSystemMessage(Component.literal("ANEMOMETER  ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
				.append(Component.literal(String.format("%.1f mph  %s  %s", mph, direction, condition)).withStyle(ChatFormatting.WHITE)));
			server.scheduleTick(pos, this, 1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private Vec3 wind(ServerLevel level, BlockPos pos) {
		TornadicSavedData data = TornadicSavedData.getOrLoad(level.getServer());
		return WindField.windAt(level, data.currentForecast(level), data.storms(), data.tornadoes(),
			pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
	}

	private double windMph(ServerLevel level, BlockPos pos) {
		Vec3 wind = wind(level, pos);
		return Math.sqrt(wind.x * wind.x + wind.z * wind.z) * 20.0 * 2.23694;
	}
}
