package com.tornadic.client;

import com.tornadic.tornado.TornadoEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * The tornado entity itself draws nothing - WeatherVisuals spawns the funnel
 * particles from synced state. Keeping this no-op keeps the render pipeline
 * trivial and crash-safe.
 */
public class TornadoRenderer extends EntityRenderer<TornadoEntity> {
	public TornadoRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(TornadoEntity entity, Frustum frustum, double x, double y, double z) {
		return false;
	}

	@Override
	public void render(TornadoEntity entity, float yaw, float partialTick, PoseStack poseStack,
		MultiBufferSource vertexConsumers, int light) {
	}

	@Override
	public ResourceLocation getTextureLocation(TornadoEntity entity) {
		return null;
	}
}
