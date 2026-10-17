/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.jstech.core.audio.Occlusion;
import dev.jstech.core.audio.acoustics.MaterialSounds;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * What each block is made of, as sound hears it: how much of a sound it drinks, how much gets through one block of
 * it, and what its steps, blows, breaking, placing and falls play instead of the game's. Resource packs say it in
 * {@code assets/<namespace>/block_sounds/<path>.json}, an object whose keys are blocks' ids or tags written
 * {@code #namespace:path}, each with any of {@code step}, {@code hit}, {@code break}, {@code place}, {@code fall},
 * {@code absorption} and {@code through}; what no pack says follows from the block's sound in the game: wool drinks
 * and muffles, stone and metal answer back, metal carries.
 */
final class BlockAcoustics extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER = "block_sounds";
    private static final BlockAcoustics INSTANCE = new BlockAcoustics();
    private static final MaterialSounds TABLE = new MaterialSounds();
    private static final Map<Block, MaterialSounds.Material> RESOLVED = new Reference2ObjectOpenHashMap<>();
    /*
     * The game's sounds of soft things, of loose things, of wood and of metal; copied from lists, as the game names
     * some sounds twice.
     */
    private static final Set<SoundType> SOFT = Set.copyOf(List.of(SoundType.WOOL, SoundType.SNOW,
            SoundType.POWDER_SNOW, SoundType.MOSS, SoundType.MOSS_CARPET, SoundType.HONEY_BLOCK,
            SoundType.SLIME_BLOCK));
    private static final Set<SoundType> LOOSE = Set.copyOf(List.of(SoundType.GRASS, SoundType.SAND,
            SoundType.GRAVEL, SoundType.ROOTED_DIRT, SoundType.MUD, SoundType.AZALEA_LEAVES, SoundType.CHERRY_LEAVES,
            SoundType.SUSPICIOUS_SAND, SoundType.SUSPICIOUS_GRAVEL));
    private static final Set<SoundType> WOODEN = Set.copyOf(List.of(SoundType.WOOD, SoundType.BAMBOO_WOOD,
            SoundType.CHERRY_WOOD, SoundType.NETHER_WOOD, SoundType.LADDER, SoundType.SCAFFOLDING));
    private static final Set<SoundType> METAL = Set.copyOf(List.of(SoundType.METAL, SoundType.COPPER,
            SoundType.ANVIL, SoundType.CHAIN, SoundType.LANTERN, SoundType.NETHERITE_BLOCK, SoundType.COPPER_BULB,
            SoundType.COPPER_GRATE, SoundType.HEAVY_CORE, SoundType.VAULT, SoundType.TRIAL_SPAWNER));
    /* How much of a sound each kind drinks. */
    private static final double SOFT_DRINKS = 0.7;
    private static final double LOOSE_DRINKS = 0.3;
    private static final double WOOD_DRINKS = 0.15;
    private static final double GLASS_DRINKS = 0.04;
    private static final double METAL_DRINKS = 0.03;
    private static final double STONE_DRINKS = 0.06;
    /* How much of a sound one block of each kind lets through. */
    private static final double SOFT_LETS = 0.3;
    private static final double WOOD_LETS = 0.5;
    private static final double GLASS_LETS = 0.8;
    private static final double METAL_LETS = 0.75;

    private BlockAcoustics() {
        super(new Gson(), FOLDER);
    }

    /** The listener the game reads the packs' files with. */
    static BlockAcoustics listener() {
        return INSTANCE;
    }

    /** What {@code state} is made of, as the packs say and as its sound in the game tells. */
    static MaterialSounds.Material of(final BlockState state) {
        synchronized (RESOLVED) {
            return RESOLVED.computeIfAbsent(state.getBlock(), block -> TABLE.of(id(block), tags(block)));
        }
    }

    /** How much of a sound {@code state} drinks. */
    static double absorption(final BlockState state) {
        final OptionalDouble said = of(state).absorption();
        return said.isPresent() ? said.getAsDouble() : drinks(state.getSoundType());
    }

    /** How much of a sound gets through one block of {@code state}; all of it through what is not a solid block. */
    static double through(final BlockState state, final boolean solid) {
        final OptionalDouble said = of(state).through();
        if (said.isPresent()) {
            return said.getAsDouble();
        }
        return solid ? lets(state.getSoundType()) : 1.0;
    }

    /** Whether no pack says what any block's steps or blows play, so none is looked at. */
    static boolean silent() {
        return TABLE.isEmpty();
    }

    /** Forgets what each block was found to be, as the tags change. */
    static void forgetBlocks() {
        synchronized (RESOLVED) {
            RESOLVED.clear();
        }
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resources,
                         final ProfilerFiller profiler) {
        TABLE.clear();
        files.keySet().stream().sorted().forEach(file -> {
            try {
                read(files.get(file).getAsJsonObject());
            } catch (final RuntimeException wrong) {
                LOGGER.warn("The block sounds of {} could not be read and are left out: {}", file, wrong.getMessage());
            }
        });
        forgetBlocks();
    }

    private static void read(final JsonObject file) {
        for (final Map.Entry<String, JsonElement> entry : file.entrySet()) {
            final JsonObject said = entry.getValue().getAsJsonObject();
            final Map<MaterialSounds.Kind, String> sounds = new EnumMap<>(MaterialSounds.Kind.class);
            for (final MaterialSounds.Kind kind : MaterialSounds.Kind.values()) {
                final String key = kind.serializedName();
                if (said.has(key)) {
                    sounds.put(kind, ResourceLocation.parse(said.get(key).getAsString()).toString());
                }
            }
            TABLE.put(entry.getKey(), new MaterialSounds.Material(sounds, share(said, "absorption"),
                    share(said, "through")));
        }
    }

    private static OptionalDouble share(final JsonObject said, final String key) {
        return said.has(key) ? OptionalDouble.of(said.get(key).getAsDouble()) : OptionalDouble.empty();
    }

    private static String id(final Block block) {
        return block.builtInRegistryHolder().key().location().toString();
    }

    private static List<String> tags(final Block block) {
        return block.builtInRegistryHolder().tags().map(tag -> "#" + tag.location()).toList();
    }

    private static double drinks(final SoundType sound) {
        if (SOFT.contains(sound)) {
            return SOFT_DRINKS;
        }
        if (LOOSE.contains(sound)) {
            return LOOSE_DRINKS;
        }
        if (WOODEN.contains(sound)) {
            return WOOD_DRINKS;
        }
        if (sound == SoundType.GLASS) {
            return GLASS_DRINKS;
        }
        return METAL.contains(sound) ? METAL_DRINKS : STONE_DRINKS;
    }

    private static double lets(final SoundType sound) {
        if (SOFT.contains(sound)) {
            return SOFT_LETS;
        }
        if (WOODEN.contains(sound)) {
            return WOOD_LETS;
        }
        if (sound == SoundType.GLASS) {
            return GLASS_LETS;
        }
        return METAL.contains(sound) ? METAL_LETS : Occlusion.THROUGH_ONE_BLOCK;
    }
}
