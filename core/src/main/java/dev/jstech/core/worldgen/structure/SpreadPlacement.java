/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * The structure placement {@code jscore:spread}: the game's random spread (one tried in each cell of {@code spacing}
 * chunks, never closer than {@code separation}, in that many of the cells as its {@code frequency} says), and besides:
 * none nearer the world's origin than {@code min_distance} blocks, and none within {@code clear_of} chunks of any other
 * structure of any mod, the game's included, so no two overlap.
 *
 * <pre>{@code
 * { "type": "jscore:spread", "spacing": 48, "separation": 20, "salt": 482919, "frequency": 0.6,
 *   "min_distance": 2000, "clear_of": 4 }
 * }</pre>
 */
public final class SpreadPlacement extends RandomSpreadStructurePlacement {

    private final int minDistance;
    private final int clearOf;

    private static final int FARTHEST = 30_000_000;
    private static final int WIDEST_CLEARANCE = 64;
    /* The game's placement fields, then a spread's, then the Core's own, ten in all: more than its codec joins. */
    public static final MapCodec<SpreadPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO)
                    .forGetter(SpreadPlacement::locateOffset),
            StructurePlacement.FrequencyReductionMethod.CODEC.optionalFieldOf("frequency_reduction_method",
                    StructurePlacement.FrequencyReductionMethod.DEFAULT)
                    .forGetter(SpreadPlacement::frequencyReductionMethod),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(SpreadPlacement::frequency),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(SpreadPlacement::salt),
            StructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone")
                    .forGetter(SpreadPlacement::exclusionZone),
            Codec.intRange(0, 4096).fieldOf("spacing").forGetter(SpreadPlacement::spacing),
            Codec.intRange(0, 4096).fieldOf("separation").forGetter(SpreadPlacement::separation),
            RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR)
                    .forGetter(SpreadPlacement::spreadType),
            Codec.intRange(0, FARTHEST).optionalFieldOf("min_distance", 0).forGetter(SpreadPlacement::minDistance),
            Codec.intRange(0, WIDEST_CLEARANCE).optionalFieldOf("clear_of", 0).forGetter(SpreadPlacement::clearOf))
            .apply(instance, SpreadPlacement::new));

    public SpreadPlacement(final Vec3i locateOffset, final StructurePlacement.FrequencyReductionMethod reduction,
                           final float frequency, final int salt,
                           final Optional<StructurePlacement.ExclusionZone> exclusionZone, final int spacing,
                           final int separation, final RandomSpreadType spreadType, final int minDistance,
                           final int clearOf) {
        super(locateOffset, reduction, frequency, salt, exclusionZone, spacing, separation, spreadType);
        this.minDistance = minDistance;
        this.clearOf = clearOf;
    }

    /** How near the world's origin none lies, in blocks. */
    public int minDistance() {
        return this.minDistance;
    }

    /** How many chunks from any other structure none lies; 0 to let them be near. */
    public int clearOf() {
        return this.clearOf;
    }

    @Override
    public StructurePlacementType<?> type() {
        return CoreStructures.SPREAD.get();
    }

    @Override
    protected boolean isPlacementChunk(final ChunkGeneratorStructureState state, final int x, final int z) {
        if (!spreadChunk(state, x, z)) {
            return false;
        }
        if (this.clearOf > 0) {
            for (final Holder<StructureSet> other : state.possibleStructureSets()) {
                if (other.value().placement() != this && near(state, other.value().placement(), x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    /* Whether the cell picks this chunk, far enough from the origin, as often as the frequency says. */
    private boolean spreadChunk(final ChunkGeneratorStructureState state, final int x, final int z) {
        if (!super.isPlacementChunk(state, x, z)) {
            return false;
        }
        final long blockX = x * 16L + 8;
        final long blockZ = z * 16L + 8;
        return blockX * blockX + blockZ * blockZ >= (long) this.minDistance * this.minDistance;
    }

    /*
     * Whether {@code other} places a structure within the clearance of {@code x, z}. Another spread of the Core's is
     * asked without its own clearance, so two never ask each other round and round; when both keep clear of others,
     * only the one of the greater salt yields, so one of the two still stands. Any other placement is asked as the
     * game asks it.
     */
    private boolean near(final ChunkGeneratorStructureState state, final StructurePlacement other, final int x,
                         final int z) {
        final boolean yields = !(other instanceof SpreadPlacement spread) || spread.clearOf() == 0
                || spread.salt() < salt();
        if (!yields) {
            return false;
        }
        for (int i = x - this.clearOf; i <= x + this.clearOf; i++) {
            for (int j = z - this.clearOf; j <= z + this.clearOf; j++) {
                if (other instanceof SpreadPlacement spread) {
                    if (spread.spreadChunk(state, i, j)
                            && spread.applyAdditionalChunkRestrictions(i, j, state.getLevelSeed())) {
                        return true;
                    }
                } else if (other.isStructureChunk(state, i, j)) {
                    return true;
                }
            }
        }
        return false;
    }
}
