package com.tornadic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;

/**
 * TEMPORARY compile-time API probe, round 2. Wrong-type calls make javac list
 * every candidate signature; failing lines are the answer. Delete when done.
 */
public final class ApiProbe {
	record TestRecord(int a) {
	}

	private ApiProbe() {
	}

	// Q01: registry register overloads
	static Object q01() {
		return BuiltInRegistries.ENTITY_TYPE.register("a", "b");
	}

	// Q02: registry getValue overloads
	static Object q02() {
		return BuiltInRegistries.ENTITY_TYPE.getValue("a");
	}

	// Q03: SavedData.Factory constructor arity
	static Object q03() {
		return new net.minecraft.world.level.saveddata.SavedData.Factory<>("a", "b", "c");
	}

	// Q04: how to send a custom payload to a player
	static Object q04(ServerPlayer p) {
		p.connection.sendCustomPayload("a");
		return p;
	}

	// Q05: ClientboundGameEventPacket ctor
	static Object q05() {
		return new ClientboundGameEventPacket("a", "b");
	}

	// Q06: far-removal, name candidate A
	static boolean q06(Entity e) {
		return e.removeWhenFarAway("a");
	}

	// Q07: far-removal, name candidate B
	static boolean q07(Entity e) {
		return e.shouldRemove("a");
	}

	// Q08: bounded nextFloat candidates
	static float q08(RandomSource r) {
		return r.nextFloat("a");
	}

	// Q09: ItemEntity age accessors
	static int q09(ItemEntity it) {
		return it.getAge();
	}

	static void q09b(ItemEntity it) {
		it.setAge(1);
	}

	// Q10: player playSound 4-arg overloads
	static void q10(ServerPlayer p) {
		p.playSound("a", "b", 1f, 1f);
	}

	// Q11: AABB ctor candidates
	static Object q11() {
		return new AABB(new BlockPos(0, 0, 0));
	}

	// Q12: ServerLevelData world gen settings
	static long q12(ServerLevelData d) {
		return d.getWorldGenSettings().getSeed();
	}

	// Q13: MinecraftServer world gen settings
	static long q13(MinecraftServer s) {
		return s.getWorldGenSettings().getSeed();
	}

	// Q14: crops age property
	static Object q14() {
		return net.minecraft.world.level.block.CropsBlock.AGE;
	}

	// Q15: sound source blocks
	static Object q15() {
		return net.minecraft.sounds.SoundSource.BLOCKS;
	}

	// Q16: sound events (1.21.1 naming)
	static SoundEvent q16() {
		return net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER;
	}

	static SoundEvent q16b() {
		return net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT;
	}

	static SoundEvent q16c() {
		return net.minecraft.sounds.SoundEvents.SNOWBALL_THROW;
	}

	// Q16d: interaction result holder factories
	static Object q16d() {
		return net.minecraft.world.InteractionResultHolder.sidedSuccess(net.minecraft.world.item.ItemStack.EMPTY, true);
	}

	// Q17: lightning bolt ctor candidates
	static Object q17(ServerLevel w) {
		return new LightningBolt("a");
	}

	// Q18: item entity ctor candidates
	static Object q18(ServerLevel w) {
		return new ItemEntity("a");
	}

	// Q19: level playLocalSound candidates
	static void q19(ServerLevel w) {
		w.playLocalSound("a", "b", "c", "d", "e", "f", "g");
	}

	// Q20: stream codec shape via anonymous class
	static Object q20() {
		return new net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, TestRecord>() {
			@Override
			public void encode(FriendlyByteBuf buf, TestRecord v) {
			}

			@Override
			public TestRecord decode(FriendlyByteBuf buf) {
				return null;
			}
		};
	}

	// Q21: byte buf codecs
	static Object q21() {
		return net.minecraft.network.protocol.ByteBufCodecs.DOUBLE;
	}
}
