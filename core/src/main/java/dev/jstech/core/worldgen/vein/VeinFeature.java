/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import java.util.List;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lays the veins on the chunk it is placed in, and only there, the feature {@code jscore:veins}: it finds every vein
 * that reaches the chunk ({@link VeinField}), keeps those found where they lie ({@link Veins#found}), and lays each
 * over the chunk's columns ({@link VeinPainter}). Nothing while the server's ores do not lie in veins. Placed once a
 * chunk in every biome, with the ores.
 */
public final class VeinFeature extends Feature<NoneFeatureConfiguration> {

    private static final int CHUNK = 16;
    /*
     * How many loose knots of a kind a chunk holds for each whole of its frequency, before the server's setting: one a
     * chunk of an ore of the first era, whose veins hold one zone in sixteen; and how large a knot is, in blocks across
     * and up and down, a handful of ore as the game's own small ores are.
     */
    private static final double KNOTS = 16;
    private static final double KNOT_RADIUS = 2;
    private static final double KNOT_THICKNESS = 1.5;

    public VeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!VeinBalance.enabled()) {
            return false;
        }
        final List<Deposits.Entry> kinds = Deposits.all();
        if (kinds.isEmpty()) {
            return false;
        }
        final WorldGenLevel level = context.level();
        final ChunkPos chunk = new ChunkPos(context.origin());
        final int x0 = chunk.getMinBlockX();
        final int z0 = chunk.getMinBlockZ();
        final RandomState random = level.getLevel().getChunkSource().randomState();
        final VeinField field = Veins.field(kinds);
        int laid = 0;
        for (final VeinField.Vein vein : field.reaching(level.getSeed(), x0, z0, x0 + CHUNK - 1, z0 + CHUNK - 1)) {
            laid += lay(context, random, kinds.get(vein.kind()), vein, x0, z0);
        }
        // A little of every kind lies loose between the veins, as the game's own ores lie scattered, so a player
        // digging anywhere comes on some of each; as many knots as the kind is common.
        for (final VeinField.Vein knot : field.knots(level.getSeed(), chunk.x, chunk.z,
                KNOTS * VeinBalance.loose(), KNOT_RADIUS, KNOT_THICKNESS)) {
            laid += lay(context, random, kinds.get(knot.kind()), knot, x0, z0);
        }
        return laid > 0;
    }

    /* Lays {@code vein} of {@code kind} over the chunk from {@code x0, z0}, where it is found; how many blocks. */
    private static int lay(final FeaturePlaceContext<NoneFeatureConfiguration> context, final RandomState random,
                           final Deposits.Entry kind, final VeinField.Vein vein, final int x0, final int z0) {
        if (!Veins.found(context.chunkGenerator(), random, kind, vein)) {
            return 0;
        }
        return VeinPainter.paint(context.level(), kind, vein, Veins.density(kind), x0, z0, x0 + CHUNK - 1,
                z0 + CHUNK - 1);
    }
}
