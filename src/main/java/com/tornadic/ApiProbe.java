package com.tornadic;

import com.tornadic.entity.ChaserVehicleEntity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.LightningBolt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * TEMPORARY compile-time API probe. Each method exercises one uncertain 1.21.1
 * Mojang mapping; CI compiler errors on a line mean that API shape is wrong.
 * Delete this file once all probes pass.
 */
public final class ApiProbe {
	private ApiProbe() {
	}

	// P01: far-away removal name/signature
	static boolean p01(Entity e) {
		return e.shouldRemove(1.0, 2.0);
	}

	// P02: gravity setter
	static void p02(Entity e) {
		e.setNoGravity(true);
	}

	// P03: level -> server
	static MinecraftServer p03(Level l) {
		return l.getServer();
	}

	// P04: leveldata seed
	static long p04(LevelData d) {
		return d.getWorldGenSettings().getSeed();
	}

	// P05: registry register overloads
	static Object p05() {
		return BuiltInRegistries.ENTITY_TYPE.register(ResourceLocation.parse("tornadic:probe"), EntityType.MINECART);
	}

	static Object p05b() {
		ResourceKey<EntityType<?>> key = ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
			ResourceLocation.parse("tornadic:probe"));
		return BuiltInRegistries.ENTITY_TYPE.register(key, EntityType.MINECART);
	}

	// P06: bounded random
	static double p06(RandomSource r) {
		return r.nextDouble(0.5);
	}

	// P07: sound events
	static SoundEvent p07() {
		return SoundEvents.PLAYER_LEVELUP;
	}

	static SoundEvent p07b() {
		return SoundEvents.ENTITY_PLAYER_LEVELUP;
	}

	// P08: compound tag default int
	static int p08(CompoundTag t) {
		return t.getInt("x", 1);
	}

	// P09: component withStyle on the interface type
	static Component p09(Component c) {
		return c.withStyle(ChatFormatting.BOLD);
	}

	// P10: level players accessor
	static Object p10(Level l) {
		return l.players().size();
	}

	// P11: server level players list
	static Object p11(ServerLevel w) {
		return w.getServer().getPlayerList().getPlayers().size();
	}

	// P12: entity passengers / vehicle APIs
	static Object p12(Entity e, Player p) {
		e.startRiding(e);
		e.stopRiding();
		p.stopRiding();
		return e.getPassengers().isEmpty() && e.getVehicle() == null;
	}

	// P13: entity interact override shape
	static Object p13(ChaserVehicleEntity v, Player p) {
		return v.interact(p, InteractionHand.MAIN_HAND);
	}

	// P14: interaction results
	static Object p14(Level l) {
		return InteractionResult.sidedSuccess(l.isClientSide);
	}

	// P15: lightning bolt ctor
	static LightningBolt p15(ServerLevel w) {
		LightningBolt bolt = new LightningBolt(
			BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.withDefaultNamespace("lightning_bolt")), w);
		bolt.setPos(0, 64, 0);
		return bolt;
	}

	// P16: level entity queries
	static Object p16(Level l) {
		l.getEntities(EntityType.MINECART, new AABB(0, 0, 0, 8, 8, 8), ent -> true);
		l.getEntitiesOfClass(LivingEntity.class, new AABB(0, 0, 0, 8, 8, 8));
		return l.addFreshEntity(new ItemEntity(l, 0, 0, 0, ItemStack.EMPTY));
	}

	// P17: block ops
	static Object p17(Level l, BlockPos pos) {
		int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING, pos);
		BlockState st = l.getBlockState(pos);
		float speed = st.getDestroySpeed(l, pos);
		l.setBlock(pos, st, 3);
		l.destroyBlock(pos, false);
		return pos.relative(net.minecraft.core.Direction.UP) && l.isLoaded(pos) && speed > 0;
	}

	// P18: saved data factory + storage
	static Object p18(MinecraftServer server) {
		DimensionDataStorage storage = server.overworld().getDataStorage();
		storage.computeIfAbsent(new SavedData.Factory<>(
			com.tornadic.saveddata.TornadicSavedData::new,
			(tag, HolderLookup.Provider registries) -> null), "probe");
		return storage;
	}

	// P19: player server-only APIs
	static Object p19(ServerPlayer p, ServerLevel w) {
		p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GameEventId.RAIN_LEVEL_CHANGE, 1f));
		p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GameEventId.THUNDER_LEVEL_CHANGE, 0f));
		p.sendSystemMessage(Component.literal("x"));
		p.playSound(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 1f, 1f);
		p.swing(InteractionHand.MAIN_HAND);
		p.distanceToSqr(new Vec3(0, 0, 0));
		p.getUUID();
		w.damageSources().fall();
		w.getRandom().nextFloat(0.5f);
		w.getRandom().nextInt(10);
		w.getLevelData().getDayTime();
		return p.getServer();
	}

	// P20: commands
	static int p20(CommandSourceStack source) {
		source.hasPermission(2);
		source.sendSuccess(() -> Component.literal("ok"), false);
		source.sendFailure(Component.literal("no"));
		ServerPlayer pl = null;
		try {
			pl = source.getPlayerOrException();
		} catch (Exception e) {
			// expected
		}
		return pl == null ? 0 : 1;
	}
}
