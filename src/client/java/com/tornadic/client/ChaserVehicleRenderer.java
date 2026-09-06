package com.tornadic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tornadic.entity.ChaserVehicleEntity;
import com.tornadic.item.TornadicItems;
import com.tornadic.TornadicMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Axis;

/** Renders the TIV 2 using Tornadic's custom three-dimensional vehicle model. */
public class ChaserVehicleRenderer extends EntityRenderer<ChaserVehicleEntity> {
	private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;
	private final ItemStack modelStack = new ItemStack(TornadicItems.CHASER_VEHICLE);

	public ChaserVehicleRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
		this.shadowRadius = 1.25f;
	}

	@Override
	public boolean shouldRender(ChaserVehicleEntity entity, Frustum frustum, double x, double y, double z) {
		return super.shouldRender(entity, frustum, x, y, z);
	}

	@Override
	public void render(ChaserVehicleEntity entity, float yaw, float partialTick, PoseStack pose,
		MultiBufferSource buffers, int light) {
		pose.pushPose();
		pose.translate(0.0, 0.72, 0.0);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.scale(2.4f, 2.4f, 2.4f);
		if (entity.isDeployed()) {
			// Lowered suspension/skirt posture when the hydraulic anchors are active.
			pose.translate(0.0, -0.10, 0.0);
		}
		itemRenderer.renderStatic(modelStack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
			pose, buffers, entity.level(), entity.getId());
		pose.popPose();
		super.render(entity, yaw, partialTick, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(ChaserVehicleEntity entity) {
		return ResourceLocation.fromNamespaceAndPath(TornadicMod.MOD_ID, "textures/item/chaser_vehicle.png");
	}
}
