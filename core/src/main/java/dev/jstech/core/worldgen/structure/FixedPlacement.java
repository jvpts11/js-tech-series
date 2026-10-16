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
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * The structure placement {@code jscore:fixed}: the structure is placed at the chunks it names and nowhere else, as a
 * landmark is, one of a kind at a known place.
 *
 * <pre>{@code
 * { "type": "jscore:fixed", "salt": 1, "chunks": [[120, -45]] }
 * }</pre>
 */
public final class FixedPlacement extends StructurePlacement {

    private final List<ChunkPos> chunks;

    private static final Codec<ChunkPos> CHUNK = Codec.INT.listOf(2, 2).xmap(
            xz -> new ChunkPos(xz.get(0), xz.get(1)), chunk -> List.of(chunk.x, chunk.z));
    public static final MapCodec<FixedPlacement> CODEC = RecordCodecBuilder.mapCodec(instance ->
            placementCodec(instance).and(CHUNK.listOf().fieldOf("chunks").forGetter(FixedPlacement::chunks))
                    .apply(instance, FixedPlacement::new));

    public FixedPlacement(final Vec3i locateOffset, final StructurePlacement.FrequencyReductionMethod reduction,
                          final float frequency, final int salt,
                          final Optional<StructurePlacement.ExclusionZone> exclusionZone,
                          final List<ChunkPos> chunks) {
        super(locateOffset, reduction, frequency, salt, exclusionZone);
        this.chunks = List.copyOf(chunks);
    }

    /** At {@code chunks} only, always. */
    public FixedPlacement(final List<ChunkPos> chunks, final int salt) {
        this(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty(), chunks);
    }

    /** The chunks it is placed at. */
    public List<ChunkPos> chunks() {
        return this.chunks;
    }

    @Override
    public StructurePlacementType<?> type() {
        return CoreStructures.FIXED.get();
    }

    @Override
    protected boolean isPlacementChunk(final ChunkGeneratorStructureState state, final int x, final int z) {
        for (final ChunkPos chunk : this.chunks) {
            if (chunk.x == x && chunk.z == z) {
                return true;
            }
        }
        return false;
    }
}
