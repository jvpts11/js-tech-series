/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import com.mojang.serialization.Codec;
import dev.jstech.space.SpaceContent;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * What a suit's pieces carry: the air left in the tanks, on the piece worn on the chest, which carries them, in
 * seconds of breathing (full where it is not said), and the holes in the suit there, until each is patched; the
 * modules fitted to each piece, by their ids; and the air in an oxygen bottle, in seconds of breathing.
 */
public final class SuitData {

    /* The most modules a piece takes. */
    private static final int MOST_MODULES = 8;

    /** The seconds of breathing left in the tanks; a piece that never had it said holds them full. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AIR =
            SpaceContent.CONTENT.component("suit_air", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);
    /** The modules fitted to the piece, by their ids. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ResourceLocation>>> MODULES =
            SpaceContent.CONTENT.component("suit_modules", ResourceLocation.CODEC.listOf(),
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_MODULES)));
    /** The holes in the suit, on the piece worn on the chest, each leaking until it is patched. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HOLES =
            SpaceContent.CONTENT.component("suit_holes", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);
    /** The seconds of breathing an oxygen bottle holds; one that never had it said is empty. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BOTTLE_AIR =
            SpaceContent.CONTENT.component("bottle_air", Codec.intRange(0, Integer.MAX_VALUE), ByteBufCodecs.VAR_INT);

    private SuitData() {
    }

    /** Declares the components, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }
}
