/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.instancing;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import dev.jstech.core.model.DrawingSettings;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

/**
 * Drawing alike things many at once on the graphics card: the shaders that do it, and whether it may be done now. It
 * may while the player leaves it on ({@link DrawingSettings}), its shaders were built, and no shader pack draws the
 * world, for a pack draws what it knows and not these; otherwise everything is drawn one by one, as before.
 *
 * <p>A model's faces are drawn with the attributes {@code Position}, {@code UV0}, {@code Normal} and {@code Part} at
 * the places 0 to 3; a particle's square with {@code Corner} at 0.
 */
public final class Instancing {

    private static final Logger LOGGER = LogUtils.getLogger();
    /** The attributes a model's faces are drawn with, in their places. */
    public static final VertexFormat KIT_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("Part", VertexFormatElement.UV1)
            .build();
    /** The attribute a particle's square is drawn with. */
    public static final VertexFormat PARTICLE_FORMAT = VertexFormat.builder()
            .add("Corner", VertexFormatElement.UV0)
            .build();
    /* The texture unit the things' numbers are read from: past the three the game's own shaders sample. */
    static final int UNIT = 3;
    /* The fewest texels a graphics card holds in one texture buffer, and the most read at once here. */
    private static final int LEAST_TEXELS = 65_536;
    private static final int MOST_TEXELS = 1 << 22;
    private static final String IRIS_API = "net.irisshaders.iris.api.v0.IrisApi";
    private static final boolean SHADER_MODS = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
    /* Asks Iris (or Oculus, which keeps its interface) whether a pack is in use; none without either, or if it is
     * not found, in which case a pack is taken to be in use. */
    @Nullable
    private static final MethodHandle PACK_IN_USE = SHADER_MODS ? packInUse() : null;

    @Nullable
    private static ShaderInstance kit;
    @Nullable
    private static ShaderInstance particles;
    private static int mostTexels = -1;

    private Instancing() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterShadersEvent.class, event -> {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "kit_instanced"), KIT_FORMAT),
                        loaded -> kit = loaded);
                event.registerShader(new ShaderInstance(event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "particle_instanced"), PARTICLE_FORMAT),
                        loaded -> particles = loaded);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /** Whether alike things are drawn many at once now: the player leaves it on, and nothing stands in the way. */
    public static boolean on() {
        return DrawingSettings.instancing() && kit != null && particles != null && !shaderPack();
    }

    /** Whether a shader pack draws the world now. */
    public static boolean shaderPack() {
        if (!SHADER_MODS) {
            return false;
        }
        if (PACK_IN_USE == null) {
            return true;
        }
        try {
            return (boolean) PACK_IN_USE.invoke();
        } catch (final Throwable failed) {
            return true;
        }
    }

    /** The shader models are drawn with many at once; none until the resources are read. */
    public static @Nullable ShaderInstance kit() {
        return kit;
    }

    /** The shader particles are drawn with many at once; none until the resources are read. */
    public static @Nullable ShaderInstance particles() {
        return particles;
    }

    /** How many texels the graphics card reads in one go. On the render thread. */
    public static int mostTexels() {
        if (mostTexels < 0) {
            final int card = GL11.glGetInteger(GL31.GL_MAX_TEXTURE_BUFFER_SIZE);
            mostTexels = Math.min(MOST_TEXELS, Math.max(LEAST_TEXELS, card));
        }
        return mostTexels;
    }

    /* Iris's question, found once: its interface's instance, asked whether a pack is in use. */
    private static @Nullable MethodHandle packInUse() {
        try {
            final Class<?> api = Class.forName(IRIS_API);
            final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            final Object instance = lookup.findStatic(api, "getInstance", MethodType.methodType(api)).invoke();
            return lookup.findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class))
                    .bindTo(instance);
        } catch (final Throwable missing) {
            LOGGER.info("A shader mod is loaded without the interface to ask it about packs; alike things are drawn "
                    + "one by one");
            return null;
        }
    }
}
