package com.tornadic.block;

import com.tornadic.saveddata.TornadicSavedData;
import com.tornadic.wind.WindField;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** A placeable, visibly spinning weather station that reads the server wind field. */
public class AnemometerBlock extends Block {
	public AnemometerBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
		Player player, BlockHitResult hit) {
		if (!level.isClientSide && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
			TornadicSavedData data = TornadicSavedData.getOrLoad(serverLevel.getServer());
			Vec3 wind = WindField.windAt(serverLevel, data.currentForecast(serverLevel), data.storms(), data.tornadoes(),
				pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
			double mph = Math.sqrt(wind.x * wind.x + wind.z * wind.z) * 20.0 * 2.23694;
			double deg = Math.toDegrees(Math.atan2(wind.z, wind.x));
			if (deg < 0) deg += 360.0;
			String[] dirs = {"E", "NE", "N", "NW", "W", "SW", "S", "SE"};
			String direction = mph < 0.5 ? "--" : dirs[((int) Math.round(deg / 45.0)) & 7];
			String condition = mph < 3 ? "CALM" : mph < 18 ? "BREEZY" : mph < 45 ? "STRONG" : mph < 90 ? "SEVERE" : "EXTREME";
			sp.sendSystemMessage(Component.literal("ANEMOMETER  ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
				.append(Component.literal(String.format("%.1f mph  %s  %s", mph, direction, condition)).withStyle(ChatFormatting.WHITE)));
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		// A tight orbit around the cup assembly makes its motion visible from every
		// direction. Weather increases apparent speed without adding entities.
		double speed = level.isThundering() ? 0.48 : level.isRaining() ? 0.30 : 0.16;
		double angle = level.getGameTime() * speed + (pos.asLong() & 255) * 0.07;
		double radius = 0.42;
		level.addParticle(new DustParticleOptions(new Vector3f(0.45f, 0.85f, 0.95f), 0.45f),
			pos.getX() + 0.5 + Math.cos(angle) * radius, pos.getY() + 0.72,
			pos.getZ() + 0.5 + Math.sin(angle) * radius,
			-Math.sin(angle) * 0.025, 0.0, Math.cos(angle) * 0.025);
	}
}
