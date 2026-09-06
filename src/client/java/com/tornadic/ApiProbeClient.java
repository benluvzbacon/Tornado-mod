package com.tornadic;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.particle.DustParticleOptions;
import net.minecraft.client.particle.ParticleTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.particles.DustColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tornadic.tornado.TornadoEntity;

/**
 * TEMPORARY client-side compile-time API probe. Delete once all probes pass.
 */
public final class ApiProbeClient {
	private ApiProbeClient() {
	}

	// C01: key mapping ctor
	static KeyMapping c01() {
		return new KeyMapping("key.tornadic.probe", 82, "category");
	}

	static class ProbeScreen extends Screen {
		ProbeScreen() {
			super(net.minecraft.network.chat.Component.literal("x"));
		}

		@Override
		protected void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		}
	}

	// C02: mc accessors
	static Object c02() {
		Minecraft mc = Minecraft.getInstance();
		mc.setScreen(new ProbeScreen());
		return mc.level != null && mc.player == null && mc.font != null;
	}

	// C03: gui drawing
	static void c03(GuiGraphics g) {
		Font font = Minecraft.getInstance().font;
		g.drawTextWithShadow(font, "text", 1, 2, 0xFF000000);
		g.drawCenteredTextWithShadow(font, "text", 1, 2, 0xFF000000);
		font.width("abc");
		g.fill(0, 0, 4, 4, 0xFF0000FF);
	}

	// C04: particles
	static void c04(Level level) {
		level.addParticle(new DustParticleOptions(new DustColor(10, 20, 30), 0.5f), 0, 1, 2, 0, 0, 0);
		level.addParticle(ParticleTypes.SPLASH, 0, 1, 2, 0, 0, 0);
		level.addParticle(ParticleTypes.SNOWFLAKE, 0, 1, 2, 0, 0, 0);
		level.addParticle(ParticleTypes.CRIT, 0, 1, 2, 0, 0, 0);
		level.addParticle(ParticleTypes.CIT, 0, 1, 2, 0, 0, 0);
		level.addParticle(ParticleTypes.LARGE_SMOKE, 0, 1, 2, 0, 0, 0);
	}

	// C05: local sound
	static void c05(Level level) {
		level.playLocalSound(0, 1, 2, SoundEvents.LIGHTNING_BOLT_THUNDER,
			net.minecraft.sounds.SoundSource.WEATHER, 1f, 1f, false);
		level.playSound(null, 0, 1, 2, SoundEvents.SNOWBALL_THROW,
			net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
		level.playSound(null, 0, 1, 2, SoundEvents.ITEM_BREAK,
			net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
		level.playSound(null, 0, 1, 2, SoundEvents.NOTE_BLOCK_PLING,
			net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
	}

	// C06: renderer factory shape
	static EntityRenderer<TornadoEntity> c06(EntityRendererProvider.Context ctx) {
		return new EntityRenderer<TornadoEntity>(ctx) {
			@Override
			public boolean shouldRender(TornadoEntity entity, Frustum frustum, double x, double y, double z) {
				return false;
			}

			@Override
			public void render(TornadoEntity entity, float yaw, float partialTick, PoseStack poseStack,
				MultiBufferSource vertexConsumers, int light) {
			}

			@Override
			public net.minecraft.resources.ResourceLocation getTextureLocation(TornadoEntity entity) {
				return null;
			}
		};
	}

	// C07: renderer registration
	static void c07() {
		EntityRenderers.register(TornadicMod.TORNADO_TYPE.get(), ApiProbeClient::c06);
	}

	// C08: level game time + entity list
	static int c08(net.minecraft.client.multiplayer.ClientLevel level) {
		int t = (int) level.getGameTime();
		return t + level.getEntities().getEntities(TornadoEntity.class).size();
	}
}
