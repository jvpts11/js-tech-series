/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * The kinds of rule that move a structure, by id, so a rule in the middle of its way is written with the structure
 * and read back after a restart. The Core's own are here from the start; a mod adds one of its own from its
 * constructor.
 *
 * <pre>{@code
 * StructureMotions.register(MyMotion.TYPE, MyMotion.CODEC);
 * }</pre>
 */
public final class StructureMotions {

    private static final Map<ResourceLocation, MapCodec<? extends IStructureMotion>> KINDS =
            new ConcurrentHashMap<>();
    /** Any rule, by the id of its kind under {@code type}. */
    public static final Codec<IStructureMotion> CODEC = ResourceLocation.CODEC.partialDispatch("type",
            motion -> DataResult.success(motion.type()),
            type -> {
                final MapCodec<? extends IStructureMotion> codec = KINDS.get(type);
                return codec == null ? DataResult.error(() -> "no kind of structure motion is called " + type)
                        : DataResult.success(codec);
            });

    static {
        register(TowardMotion.TYPE, TowardMotion.CODEC);
        register(SpinMotion.TYPE, SpinMotion.CODEC);
        register(TurnAboutMotion.TYPE, TurnAboutMotion.CODEC);
        register(PathMotion.TYPE, PathMotion.CODEC);
        register(FaceMotion.TYPE, FaceMotion.CODEC);
        register(RailsMotion.TYPE, RailsMotion.CODEC);
    }

    private StructureMotions() {
    }

    /**
     * Adds a kind of rule.
     *
     * @throws IllegalStateException when a kind of that id is there already
     */
    public static void register(final ResourceLocation type, final MapCodec<? extends IStructureMotion> codec) {
        Objects.requireNonNull(codec, "codec");
        if (KINDS.putIfAbsent(type, codec) != null) {
            throw new IllegalStateException("a kind of structure motion is called " + type + " already");
        }
    }

    /** Whether a kind of rule of that id is there. */
    public static boolean has(final ResourceLocation type) {
        return KINDS.containsKey(type);
    }
}
