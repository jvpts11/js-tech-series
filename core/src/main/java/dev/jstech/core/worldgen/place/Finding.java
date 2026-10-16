/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.place;

import com.mojang.datafixers.util.Pair;
import dev.jstech.core.worldgen.structure.FixedPlacement;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.core.worldgen.vein.Veins;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Finding the nearest thing of a kind (a structure, a biome, a landmark, a vein), answered with a level of precision,
 * so an instrument can point roughly at first and exactly later: at precision 0 the answer is a place within 512
 * blocks of the thing, each level halves that, and at {@link #EXACT} it is the thing's own place. The rough answer is
 * the same each time it is asked, so the needle does not wander, and is never more than its stated distance off.
 *
 * <pre>{@code
 * Optional<Finding.Found> near = Finding.find(level, player.blockPosition(),
 *         new Finding.StructureTarget(MyTags.RUINS), 2);    // within 128 blocks
 * }</pre>
 */
public final class Finding {

    /** The precision of an exact answer. */
    public static final int EXACT = 9;

    private static final int ROUGHEST = 512;
    private static final int STRUCTURE_SEARCH_CHUNKS = 100;
    private static final int BIOME_SEARCH = 6400;
    private static final int BIOME_STEP = 32;
    private static final int BIOME_STEP_UP = 64;
    private static final int VEIN_SEARCH = 1024;
    private static final long MIX = -7_046_029_254_386_353_131L;

    private Finding() {
    }

    /** How far off an answer at {@code precision} may be, in blocks. */
    public static int within(final int precision) {
        return precision >= EXACT ? 0 : ROUGHEST >> Math.max(0, precision);
    }

    /** The nearest thing {@code target} names from {@code from}, at {@code precision}, or none within reach. */
    public static Optional<Found> find(final ServerLevel level, final BlockPos from, final Target target,
                                       final int precision) {
        final Optional<BlockPos> exact = switch (target) {
            case StructureTarget structures -> structure(level, from, structures.structures());
            // The game's biome search looks only from a height inside the world, so one outside is brought in.
            case BiomeTarget biomes -> Optional.ofNullable(level.findClosestBiome3d(biome -> biome.is(biomes.biomes()),
                    from.atY(Mth.clamp(from.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1)),
                    BIOME_SEARCH, BIOME_STEP, BIOME_STEP_UP)).map(Pair::getFirst);
            case LandmarkTarget landmark -> Landmarks.get(level, landmark.id())
                    .filter(found -> found.dimension().equals(level.dimension())).map(Landmark::pos);
            case VeinTarget vein -> vein(level, from, vein.deposit());
        };
        return exact.map(place -> blur(place, precision));
    }

    /** The answer for the thing at {@code place}, at {@code precision}: off by no more than it says, always alike. */
    public static Found blur(final BlockPos place, final int precision) {
        final int off = within(precision);
        if (off == 0) {
            return new Found(place, 0);
        }
        final SplittableRandom random = new SplittableRandom(place.asLong() * MIX ^ precision);
        final double angle = random.nextDouble() * 2 * Math.PI;
        // A block short of the distance stated, so rounding to a block never carries the answer past it.
        final double reach = Math.sqrt(random.nextDouble()) * (off - 1);
        return new Found(place.offset((int) Math.round(Math.cos(angle) * reach), 0,
                (int) Math.round(Math.sin(angle) * reach)), off);
    }

    private static Optional<BlockPos> structure(final ServerLevel level, final BlockPos from,
                                                final TagKey<Structure> tag) {
        final Optional<HolderSet.Named<Structure>> structures = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE).getTag(tag);
        if (structures.isEmpty()) {
            return Optional.empty();
        }
        final Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, structures.get(), from, STRUCTURE_SEARCH_CHUNKS, false);
        BlockPos best = found == null ? null : found.getFirst();
        // The game's search passes over structures at fixed places, as a landmark's is; they are few, each looked at.
        for (final Holder<StructureSet> set : level.getChunkSource().getGeneratorState().possibleStructureSets()) {
            if (!(set.value().placement() instanceof FixedPlacement fixed)) {
                continue;
            }
            for (final StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                if (!structures.get().contains(entry.structure())) {
                    continue;
                }
                for (final ChunkPos chunk : fixed.chunks()) {
                    final StructureStart start = level.getChunk(chunk.x, chunk.z, ChunkStatus.STRUCTURE_STARTS)
                            .getStartForStructure(entry.structure().value());
                    final BlockPos at = fixed.getLocatePos(chunk);
                    if (start != null && start.isValid() && (best == null || flat(at, from) < flat(best, from))) {
                        best = at;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static long flat(final BlockPos a, final BlockPos b) {
        final long x = a.getX() - b.getX();
        final long z = a.getZ() - b.getZ();
        return x * x + z * z;
    }

    private static Optional<BlockPos> vein(final ServerLevel level, final BlockPos from, final ResourceLocation id) {
        final List<Deposits.Entry> kinds = Deposits.all();
        for (final VeinField.Vein vein : Veins.field(kinds).near(level.getSeed(), from.getX(), from.getZ(),
                VEIN_SEARCH)) {
            final Deposits.Entry kind = kinds.get(vein.kind());
            if (kind.id().equals(id) && Veins.found(level.getChunkSource().getGenerator(),
                    level.getChunkSource().randomState(), kind, vein)) {
                return Optional.of(new BlockPos(vein.x(), vein.y(), vein.z()));
            }
        }
        return Optional.empty();
    }

    /** What is looked for. */
    public sealed interface Target permits StructureTarget, BiomeTarget, LandmarkTarget, VeinTarget {
    }

    /**
     * The nearest structure of a tag.
     *
     * @param structures the tag
     */
    public record StructureTarget(TagKey<Structure> structures) implements Target {
    }

    /**
     * The nearest biome of a tag.
     *
     * @param biomes the tag
     */
    public record BiomeTarget(TagKey<Biome> biomes) implements Target {
    }

    /**
     * A landmark, in the dimension it is in.
     *
     * @param id the landmark
     */
    public record LandmarkTarget(ResourceLocation id) implements Target {
    }

    /**
     * The nearest vein of a kind.
     *
     * @param deposit the kind
     */
    public record VeinTarget(ResourceLocation deposit) implements Target {
    }

    /**
     * An answer.
     *
     * @param near   a place near the thing
     * @param within how far from it the thing may be, in blocks; 0 for its own place
     */
    public record Found(BlockPos near, int within) {
    }
}
