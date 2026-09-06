package com.tornadic.item;

import com.tornadic.config.TornadicConfig;
import com.tornadic.saveddata.TornadicSavedData;
import com.tornadic.tornado.TornadoIntensity;
import com.tornadic.tornado.TornadoState;
import com.tornadic.weather.DailyForecast;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Receives the daily bulletin and live warnings - the chaser's first stop each morning.
 */
public class WeatherRadioItem extends Item {
	public WeatherRadioItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		if (!world.isClientSide && player instanceof ServerPlayer sp) {
			TornadicSavedData data = TornadicSavedData.getOrLoad(sp.getServer());
			DailyForecast forecast = data.currentForecast(sp.level());
			Component msg = Component.empty()
				.append(Component.literal("WEATHER RADIO - DAY ").withStyle(ChatFormatting.CYAN, ChatFormatting.BOLD))
				.append(Component.literal(String.valueOf(forecast.day() + 1)).withStyle(ChatFormatting.CYAN))
				.append(Component.literal("\nRisk: ").withStyle(ChatFormatting.GRAY))
				.append(forecast.risk().asComponent().withStyle(ChatFormatting.BOLD))
				.append(Component.literal("\n" + forecast.risk().description()).withStyle(ChatFormatting.GRAY));
			for (String line : forecast.forecastLines()) {
				msg = msg.append(Component.literal("\n" + line).withStyle(ChatFormatting.WHITE));
			}
			// Live warnings for active tornadoes.
			for (TornadoState t : data.tornadoes()) {
				if (t.isDissipated() || t.currentEf() < 1) {
					continue;
				}
				if (sp.distanceToSqr(new Vec3(t.x, sp.getY(), t.z)) <= (double) TornadicConfig.warningRadius * TornadicConfig.warningRadius) {
					TornadoIntensity scale = t.intensityScale();
					msg = msg.append(Component.literal("\nTORNADO WARNING in effect: ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
						.append(Component.literal(scale.label()).withStyle(ChatFormatting.RED))
						.append(Component.literal(" near X " + (int) t.x + ", Z " + (int) t.z).withStyle(ChatFormatting.RED));
				}
			}
			sp.sendSystemMessage(msg);
			sp.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING, SoundSource.NEUTRAL, 0.3f, 1.4f);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.tornadic.weather_radio").withStyle(ChatFormatting.GRAY));
	}
}
