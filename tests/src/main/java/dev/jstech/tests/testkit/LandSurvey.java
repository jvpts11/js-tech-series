/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.core.worldgen.landform.LandformField;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.core.worldgen.vein.Veins;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Finds the landforms and veins a generated land holds, worked out from its seed as the generation works them out, so
 * a test knows where to look before a single chunk is made: a landform of a kind on the biome it wants, standing clear
 * of every other landform, so what lies at its middle is its own doing; a vein of a kind where it is found.
 */
public final class LandSurvey {

    /* Every landform of the land is looked for this many cells about the one tried, either way. */
    private static final int NEIGHBOURS = 1;
    /* How high the biome is read at: the land's biomes are the same all the way up. */
    private static final int BIOME_HEIGHT = 64;
    private static final double MARGIN = 2;
    /* How many blocks over the sea dry land stands, at the least. */
    private static final int DRY = 2;

    private LandSurvey() {
    }

    /**
     * A landform of {@code spec} in {@code land}, along the first {@code cells} cells of a row, whose middle lies in
     * {@code biome} and whose reach, twice its radius, touches no landform of {@code others}; null when none does.
     */
    public static LandformField.Instance clear(final ServerLevel land, final Landforms.LandformSpec spec,
                                               final Holder<Biome> biome, final List<Landforms.LandformSpec> others,
                                               final int cells) {
        return clear(land, spec, biome, others, cells, false);
    }

    /** As {@link #clear(ServerLevel, Landforms.LandformSpec, Holder, List, int)}, its middle on dry land when asked. */
    public static LandformField.Instance clear(final ServerLevel land, final Landforms.LandformSpec spec,
                                               final Holder<Biome> biome, final List<Landforms.LandformSpec> others,
                                               final int cells, final boolean dry) {
        final long seed = land.getSeed();
        final ChunkGenerator generator = land.getChunkSource().getGenerator();
        final RandomState random = land.getChunkSource().randomState();
        final LandformField field = field(spec);
        for (int cx = 0; cx < cells; cx++) {
            final LandformField.Instance one = field.in(seed, cx, 0);
            if (one != null && generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(one.x()),
                    QuartPos.fromBlock(BIOME_HEIGHT), QuartPos.fromBlock(one.z()), random.sampler()).equals(biome)
                    && apart(seed, one, cx, others) && (!dry || onLand(land, generator, random, one))) {
                return one;
            }
        }
        return null;
    }

    /** A vein of the kind {@code deposit} found where it lies in {@code land}, along the first {@code zones} zones. */
    public static VeinField.Vein vein(final ServerLevel land, final ResourceLocation deposit, final int zones) {
        final List<Deposits.Entry> kinds = Deposits.all();
        final VeinField field = Veins.field(kinds);
        for (int zx = 0; zx < zones; zx++) {
            final VeinField.Vein vein = field.in(land.getSeed(), zx, 0);
            if (vein != null && kinds.get(vein.kind()).id().equals(deposit) && Veins.found(
                    land.getChunkSource().getGenerator(), land.getChunkSource().randomState(), kinds.get(vein.kind()),
                    vein)) {
                return vein;
            }
        }
        return null;
    }

    /** A reservoir of the kind {@code kind} found where it lies in {@code land}, in its first {@code zones} zones. */
    public static Reservoirs.Reservoir reservoir(final ServerLevel land, final ResourceLocation kind, final int zones) {
        final List<Reservoirs.Kind> kinds = Reservoirs.all();
        final VeinField field = Reservoirs.field(kinds);
        for (int zx = 0; zx < zones; zx++) {
            final VeinField.Vein body = field.in(land.getSeed(), zx, 0);
            if (body != null && kinds.get(body.kind()).id().equals(kind)) {
                final Reservoirs.Kind found = kinds.get(body.kind());
                if (Veins.found(land.getChunkSource().getGenerator(), land.getChunkSource().randomState(),
                        found.type().biomes(), found.type().elsewhere(), body)) {
                    return new Reservoirs.Reservoir(found, body);
                }
            }
        }
        return null;
    }

    /** Where the landforms of {@code spec} lie. */
    public static LandformField field(final Landforms.LandformSpec spec) {
        return new LandformField(spec.cell(), spec.chance(), spec.smallest(), spec.largest(), spec.salt());
    }

    /* Whether the ground at a landform's middle and a little way round it stands above the land's sea. */
    private static boolean onLand(final ServerLevel land, final ChunkGenerator generator, final RandomState random,
                                  final LandformField.Instance one) {
        final int sea = generator.getSeaLevel();
        for (final int[] step : new int[][] {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            final int x = one.x() + step[0] * one.radius();
            final int z = one.z() + step[1] * one.radius();
            if (generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, land, random) <= sea + DRY) {
                return false;
            }
        }
        return true;
    }

    private static boolean apart(final long seed, final LandformField.Instance one, final int cx,
                                 final List<Landforms.LandformSpec> others) {
        for (final Landforms.LandformSpec other : others) {
            final LandformField field = field(other);
            for (int nx = cx - NEIGHBOURS; nx <= cx + NEIGHBOURS; nx++) {
                for (int nz = -NEIGHBOURS; nz <= NEIGHBOURS; nz++) {
                    final LandformField.Instance near = field.in(seed, nx, nz);
                    if (near != null && !near.equals(one) && one.distance(near.x(), near.z())
                            <= 2.0 * (one.radius() + near.radius()) + MARGIN) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
