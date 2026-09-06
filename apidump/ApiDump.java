package com.tornadic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * TEMPORARY CI diagnostic: dumps real 1.21.1 signatures via reflection
 * (classes are loaded without initialization). Run with: ./gradlew apiDump
 */
public final class ApiDump {
	private static final String[] CLASSES = {
		"net.minecraft.world.entity.Entity",
		"net.minecraft.core.registries.BuiltInRegistries",
		"net.minecraft.core.registries.Registries",
		"net.minecraft.world.level.storage.LevelData",
		"net.minecraft.world.level.storage.ServerLevelData",
		"net.minecraft.world.level.storage.WritableLevelData",
		"net.minecraft.world.level.storage.DimensionDataStorage",
		"net.minecraft.world.level.storage.LevelStorageSource",
		"net.minecraft.server.MinecraftServer",
		"net.minecraft.world.level.Level",
		"net.minecraft.server.level.ServerLevel",
		"net.minecraft.sounds.SoundEvent",
		"net.minecraft.world.level.saveddata.SavedData",
		"net.minecraft.world.level.saveddata.SavedData$Factory",
		"net.minecraft.network.protocol.game.ClientboundGameEventPacket",
		"net.minecraft.server.network.ServerGamePacketListenerImpl",
		"net.minecraft.network.protocol.Packet",
		"net.minecraft.world.entity.LightningBolt",
		"net.minecraft.world.entity.item.ItemEntity",
		"net.minecraft.world.entity.player.Player",
		"net.minecraft.world.entity.data.synched.SynchedEntityData",
		"net.minecraft.network.codec.StreamCodec",
		"net.minecraft.world.level.block.CropBlock",
		"net.minecraft.world.level.block.state.properties.BlockStateProperties",
		"net.minecraft.world.phys.shapes.VoxelShape",
		"net.minecraft.world.level.block.state.BlockState",
		"net.minecraft.util.datafix.DataFixTypes",
		"net.minecraft.world.entity.EntityType",
		"net.minecraft.world.entity.EntityType$Builder",
		"net.minecraft.client.Minecraft",
		"net.minecraft.client.gui.GuiGraphics",
		"net.minecraft.client.gui.screens.Screen",
		"net.minecraft.client.gui.Font",
		"net.minecraft.client.KeyMapping",
		"net.minecraft.client.renderer.entity.EntityRenderers",
		"net.minecraft.client.renderer.entity.EntityRenderer",
		"net.minecraft.client.multiplayer.ClientLevel",
		"net.minecraft.core.particles.DustParticleOptions",
		"net.minecraft.core.particles.ParticleTypes",
		"net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking",
		"net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents",
		"net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper",
		"net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback",
	};

	private ApiDump() {
	}

	public static void main(String[] args) throws Exception {
		scanJars();
		ClassLoader loader = ApiDump.class.getClassLoader();
		for (String name : CLASSES) {
			try {
				Class<?> c = Class.forName(name, false, loader);
				System.out.println("== " + name + " (super=" + (c.getSuperclass() == null ? "-" : c.getSuperclass().getName()) + ")");
				System.out.println("-- constructors");
				for (Constructor<?> ctor : c.getDeclaredConstructors()) {
					if (Modifier.isPrivate(ctor.getModifiers())) {
						continue;
					}
					System.out.println("  " + ctor.getName() + safeTypes(ctor.getParameterTypes()));
				}
				System.out.println("-- declared methods");
				for (Method m : c.getDeclaredMethods()) {
					if (Modifier.isPrivate(m.getModifiers())) {
						continue;
					}
					String ret = "?";
					try {
						ret = m.getReturnType().getName();
					} catch (Throwable ignored) {
					}
					System.out.println("  " + m.getName() + safeTypes(m.getParameterTypes()) + " : " + ret);
				}
				System.out.println("-- fields (static)");
				for (Field f : c.getDeclaredFields()) {
					if (Modifier.isStatic(f.getModifiers())) {
						String t = "?";
						try {
							t = f.getType().getName();
						} catch (Throwable ignored) {
						}
						System.out.println("  " + f.getName() + " : " + t);
					}
				}
			} catch (Throwable t) {
				System.out.println("== " + name + " MISSING: " + t);
			}
		}
	}

	private static void scanJars() {
		String[] filters = {
			"CustomPayload",
			"DefaultedRegistry",
			"BuiltInRegistries",
			"net/minecraft/registry/",
			"net/minecraft/util/Identifier",
			"ClientPlayNetworking",
			"KeyBindingHelper",
			"HudRenderCallback",
			"ClientTickEvents"
		};
		for (String part : System.getProperty("java.class.path").split(File.pathSeparator)) {
			if (!part.endsWith(".jar")) {
				continue;
			}
			try (JarFile zf = new JarFile(part)) {
				var entries = zf.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String n = entry.getName();
					if (!n.endsWith(".class") || n.contains("$")) {
						continue;
					}
					boolean hit = n.endsWith("/Registry.class");
					for (String f : filters) {
						if (n.contains(f)) {
							hit = true;
							break;
						}
					}
					if (hit) {
						System.out.println("JARCLASS " + n.substring(0, n.length() - 6).replace('/', '.'));
					}
				}
			} catch (Throwable t) {
				System.out.println("SCAN-FAIL " + part + " : " + t);
			}
		}
		System.out.println("== scan done");
	}

	private static String safeTypes(Class<?>[] types) {
		StringBuilder sb = new StringBuilder("(");
		for (int i = 0; i < types.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			try {
				sb.append(types[i].getName());
			} catch (Throwable t) {
				sb.append("<unloaded>");
			}
		}
		return sb.append(")").toString();
	}
}
