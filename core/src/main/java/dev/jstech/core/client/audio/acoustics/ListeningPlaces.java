/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.audio.acoustics.ListeningPlace;
import dev.jstech.core.audio.acoustics.SoundConditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Where the listener is, read from the world for the ambience and the music: the dimension and the biome, the weather,
 * the time of day, whether under a roof (no sky above and walls all round), how deep below the open sky, and how much
 * of each thing asked about is near, counted from a sample of the blocks round the listener rather than every one.
 */
final class ListeningPlaces {

    /* How many blocks round the listener are looked at to count each thing asked about. */
    private static final int SAMPLES = 160;
    /* How walled in a place must be to be under a roof. */
    private static final double ROOFED = 0.6;
    private static final Map<String, TagKey<Block>> TAGS = new HashMap<>();

    private static int round;

    private ListeningPlaces() {
    }

    /** The place at {@code eye}, counting what {@code asked} is near. */
    static ListeningPlace at(final Level level, final Vec3 eye, final Collection<SoundConditions.Near> asked) {
        final BlockPos at = BlockPos.containing(eye);
        final Holder<Biome> biome = level.getBiome(at);
        final String biomeId = biome.unwrapKey().map(key -> key.location().toString()).orElse("");
        final Set<String> tags = biome.tags().map(tag -> "#" + tag.location()).collect(Collectors.toSet());
        final ListeningPlace.Weather weather = level.isThundering() ? ListeningPlace.Weather.THUNDER
                : level.isRaining() && biome.value().hasPrecipitation() ? ListeningPlace.Weather.RAIN
                : ListeningPlace.Weather.CLEAR;
        final int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
        final boolean indoors = ListenerAir.inRoom()
                || !level.canSeeSky(at) && AcousticProbe.space().enclosure() >= ROOFED;
        round++;
        final Map<String, Integer> near = new HashMap<>();
        for (final SoundConditions.Near one : asked) {
            near.merge(one.what(), count(level, at, one), Math::max);
        }
        return new ListeningPlace(level.dimension().location().toString(), biomeId, tags, weather,
                (int) (level.getDayTime() % ListeningPlace.DAY), indoors, surface - at.getY(), near);
    }

    /* About how many blocks of what is asked stand within its radius, from a sample of them. */
    private static int count(final Level level, final BlockPos centre, final SoundConditions.Near near) {
        final int side = 2 * near.radius() + 1;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int found = 0;
        for (int i = 0; i < SAMPLES; i++) {
            // A different spread of the same number of samples each round, so a count settles over a few seconds.
            final long mixed = (i + 1L) * 0x9E3779B97F4A7C15L + round * 0xBF58476D1CE4E5B9L;
            at.set(centre.getX() + Math.floorMod(mixed, side) - near.radius(),
                    centre.getY() + Math.floorMod(mixed >>> 21, side) - near.radius(),
                    centre.getZ() + Math.floorMod(mixed >>> 42, side) - near.radius());
            if (level.isLoaded(at) && is(level.getBlockState(at), near.what())) {
                found++;
            }
        }
        return (int) Math.round((double) found * side * side * side / SAMPLES);
    }

    private static boolean is(final BlockState state, final String what) {
        if (what.startsWith("#")) {
            return state.is(TAGS.computeIfAbsent(what, tag -> TagKey.create(Registries.BLOCK,
                    ResourceLocation.parse(tag.substring(1)))));
        }
        return state.getBlock().builtInRegistryHolder().key().location().toString().equals(what);
    }
}
