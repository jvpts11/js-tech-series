/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Where a structure may stand on the land, and how it sits there: the ground under it read at its corners and its
 * middle, {@code footprint} blocks out; none where the ground rises more than {@code slope} across that, lies outside
 * its heights, or, when it wants {@code dry} land, under water; and it is sunk {@code sunk} blocks into the ground,
 * half of its height for one half buried, all of it for one buried whole.
 *
 * <pre>{@code
 * { "footprint": 12, "slope": 6, "min_y": 60, "max_y": 160, "dry": true, "sunk": 0 }
 * }</pre>
 *
 * @param footprint how far from its middle the ground is read, in blocks
 * @param slope     how far the ground may rise and fall across it, in blocks; -1 for any land
 * @param minY      the lowest the ground under it may lie
 * @param maxY      the highest
 * @param dry       whether it stands only where there is no water over the ground
 * @param sunk      how many blocks into the ground it sits
 */
public record StructureFit(int footprint, int slope, int minY, int maxY, boolean dry, int sunk) {

    private static final int WIDEST = 128;
    private static final int LOWEST = -2048;
    private static final int HIGHEST = 2048;
    /** Any land, as the game stands its own structures. */
    public static final StructureFit ANYWHERE = new StructureFit(8, -1, LOWEST, HIGHEST, false, 0);
    public static final Codec<StructureFit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, WIDEST).optionalFieldOf("footprint", 8).forGetter(StructureFit::footprint),
            Codec.intRange(-1, HIGHEST).optionalFieldOf("slope", -1).forGetter(StructureFit::slope),
            Codec.intRange(LOWEST, HIGHEST).optionalFieldOf("min_y", LOWEST).forGetter(StructureFit::minY),
            Codec.intRange(LOWEST, HIGHEST).optionalFieldOf("max_y", HIGHEST).forGetter(StructureFit::maxY),
            Codec.BOOL.optionalFieldOf("dry", false).forGetter(StructureFit::dry),
            Codec.intRange(-WIDEST, WIDEST).optionalFieldOf("sunk", 0).forGetter(StructureFit::sunk))
            .apply(instance, StructureFit::new));

    /** Whether ground whose corners lie from {@code lowest} to {@code highest} suits it. */
    public boolean suits(final int lowest, final int highest) {
        return (this.slope < 0 || highest - lowest <= this.slope) && lowest >= this.minY && lowest <= this.maxY;
    }
}
