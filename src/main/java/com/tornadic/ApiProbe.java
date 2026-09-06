package com.tornadic;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * TEMPORARY compile-time API probe, round 4 (last). Delete when done.
 */
public final class ApiProbe {
	private ApiProbe() {
	}

	// R4A: vanilla static registration API
	static Object r4a() {
		return net.minecraft.registry.Registry.register(Registries.ENTITY_TYPE,
			net.minecraft.util.Identifier.of("tornadic", "probe"), EntityType.MINECART);
	}

	// R4B: custom payload packet class (common.custom package)
	static Object r4b(ServerPlayer p) {
		p.connection.send(new net.minecraft.network.protocol.common.custom.ClientboundCustomPayloadPacket(
			new PayloadProbe()));
		return p;
	}

	record PayloadProbe() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<PayloadProbe> TYPE =
			new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
				net.minecraft.resources.ResourceLocation.parse("tornadic:probe3"));

		@Override
		public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// R4C: data fix types
	static Object r4c() {
		return net.minecraft.data.fixes.DataFixTypes.LEVEL;
	}

	// R4D: game event packet constants (statics on the packet class)
	static Object r4d() {
		return new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1f);
	}

	static Object r4d2() {
		return new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0f);
	}

	// R4E: far-removal override candidates, batch 2
	static class ProbeEntE extends Entity {
		ProbeEntE(EntityType<? extends Entity> t, Level l) {
			super(t, l);
		}

		@Override
		public boolean shouldBeRemoved() {
			return false;
		}

		@Override
		protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		}

		@Override
		protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		}
	}

	static class ProbeEntF extends Entity {
		ProbeEntF(EntityType<? extends Entity> t, Level l) {
			super(t, l);
		}

		@Override
		public boolean canBeRemoved() {
			return false;
		}

		@Override
		protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		}

		@Override
		protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
		}
	}
}
