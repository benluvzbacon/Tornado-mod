package com.tornadic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.MinecraftServer;

/**
 * TEMPORARY compile-time API probe, round 5. Delete when done.
 */
public final class ApiProbe {
	private ApiProbe() {
	}

	// R5A: deferred register + entry
	static Object r5a() {
		var dr = net.fabricmc.fabric.api.object.builder.v1.registry.DeferredRegister
			.create(Registries.ENTITY_TYPE, "tornadic");
		var entry = dr.register("probe", () -> EntityType.MINECART);
		return entry.get();
	}

	// R5B: save-folder id
	static Object r5b(MinecraftServer server) {
		return server.getServerStorageSource().getLevelId();
	}

	// R5C: data fix types package candidate
	static Object r5c() {
		return net.minecraft.data.worldgen.DataFixTypes.LEVEL;
	}

	// R5D: registry wrapAsHolder
	static Object r5d() {
		return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ITEM_BREAK);
	}
}
