package com.tornadic.client;

import com.tornadic.entity.ChaserVehicleEntity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * The chaser vehicle body is a particle silhouette (VehicleVisuals); the entity
 * renderer is a no-op.
 */
public class ChaserVehicleRenderer extends EntityRenderer<ChaserVehicleEntity> {
	public ChaserVehicleRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRender(ChaserVehicleEntity entity, Frustum frustum, double x, double y, double z) {
		return false;
	}

	@Override
	public void render(ChaserVehicleEntity entity, float yaw, float partialTick, PoseStack poseStack,
		MultiBufferSource vertexConsumers, int light) {
	}

	@Override
	public ResourceLocation getTextureLocation(ChaserVehicleEntity entity) {
		return null;
	}
}
