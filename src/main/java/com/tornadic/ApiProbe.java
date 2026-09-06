package com.tornadic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * TEMPORARY compile-time API probe, round 3 (final). Delete when done.
 */
public final class ApiProbe {
	private ApiProbe() {
	}

	// R3A: registry value lookup via Map#get(ResourceKey)
	static EntityType<?> r3a() {
		return BuiltInRegistries.ENTITY_TYPE.get(ResourceKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.parse("tornadic:probe")));
	}

	// R3B: fabric registry helper registration
	static Object r3b() {
		return net.fabricmc.fabric.api.registry.v1.Registry.register(Registries.ENTITY_TYPE,
			"tornadic:probe", EntityType.MINECART);
	}

	// R3C: custom payload packet wrapping
	static Object r3c(ServerPlayer p) {
		p.connection.send(new ClientboundCustomPayloadPacket(new TornadoSyncProbe()));
		return p;
	}

	record TornadoSyncProbe() implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<TornadoSyncProbe> TYPE =
			new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
				ResourceLocation.parse("tornadic:probe2"));

		@Override
		public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
			return TYPE;
		}
	}

	// R3D: game event packet ctor candidates (wrong arity lists them all)
	static Object r3d() {
		return new ClientboundGameEventPacket("a", "b", "c");
	}

	// R3E: saved data factory third arg type
	static Object r3e() {
		return new net.minecraft.world.level.saveddata.SavedData.Factory<>(
			() -> null, (t, p) -> null, "a");
	}

	// R3F: server level world gen settings
	static long r3f(ServerLevel w) {
		return w.getWorldGenSettings().getSeed();
	}

	// R3G: crop block age property
	static Object r3g() {
		return net.minecraft.world.level.block.CropBlock.AGE;
	}

	// R3H: note block pling
	static Object r3h() {
		return net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING;
	}

	// R3I: style empty
	static Object r3i() {
		return net.minecraft.network.chat.Style.EMPTY.withBold(true);
	}

	// R3J: far-removal override candidates — four probe entities
	static class ProbeEntA extends Entity {
		ProbeEntA(EntityType<? extends Entity> t, Level l) {
			super(t, l);
		}

		@Override
		public boolean removeWhenFarAway(double d) {
			return false;
		}
	}

	static class ProbeEntB extends Entity {
		ProbeEntB(EntityType<? extends Entity> t, Level l) {
			super(t, l);
		}

		@Override
		public boolean shouldRemove(double x, double z) {
			return false;
		}
	}

	static class ProbeEntC extends Entity {
		ProbeEntC(EntityType<? extends Entity> t, Level l) {
			super(t, l);
		}

		@Override
		public boolean canRemove() {
			return false;
		}
	}

	static class ProbeEntD extends Entity {
		ProbeEntD(EntityType<? extends Entity> t, Level l) {
			super(t, l);
		}

		@Override
		public boolean isBeyondRemoveDistance(double d) {
			return false;
		}
	}
}
