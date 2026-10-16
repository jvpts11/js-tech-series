/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.landform;

import dev.jstech.core.worldgen.biome.BiomeClimates;
import java.util.List;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

/**
 * Shapes the ground by one landform, over a box of columns and nothing outside it: raising, cutting or covering each
 * column as the landform's shape says ({@link LandformShapes}), from the ground's own height there. The world's
 * generation paints a landform a chunk at a time ({@link LandformFeature}); anything else can paint one where it is
 * wanted, a test into its arena, a command into the world.
 */
public final class LandformPainter {

    /* How deep a volcano's summit is filled, how often a lava tube opens to the sky, and how a channel winds. */
    private static final int SUMMIT_POOL = 2;
    private static final double SKYLIGHT = 0.08;
    private static final int SKYLIGHT_SPACING = 4;
    private static final double MEANDER = 48.0;
    private static final int GEYSER_MOUND = 2;
    private static final double CRACK_SPACING = 7.0;
    private static final double CRACK = 0.07;
    private static final double EJECTA = 0.6;
    private static final double BOULDERS = 0.05;
    private static final int RAYS = 9;
    private static final double RAY_WIDTH = 0.96;
    private static final int MESA_EDGE = 4;
    private static final int LARGE_CRATER = 24;
    private static final double FADING = 0.3;
    private static final double BANK = 1.5;
    private static final int UPDATE = 2;
    private static final int MAGMA_TICK = 20;
    private static final long MIX_X = 341_873_128_712L;
    private static final long MIX_Z = 132_897_987_541L;

    private LandformPainter() {
    }

    /**
     * Paints {@code one} over the columns from {@code x0, z0} to {@code x1, z1}, each column's ground read from
     * {@code ground}'s height there, {@code base} the height of the ground at the landform's middle.
     */
    public static void paint(final WorldGenLevel level, final LandformConfig config, final LandformField.Instance one,
                             final int base, final Heightmap.Types ground, final int x0, final int z0, final int x1,
                             final int z1) {
        final Column column = new Column(level, config, one, base);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                column.paint(x, z, level.getHeight(ground, x, z) - 1);
            }
        }
    }

    /* One landform as it is painted, a column at a time. */
    private static final class Column {

        private final WorldGenLevel level;
        private final LandformConfig config;
        private final LandformField.Instance one;
        private final int base;
        private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

        private Column(final WorldGenLevel level, final LandformConfig config, final LandformField.Instance one,
                       final int base) {
            this.level = level;
            this.config = config;
            this.one = one;
            this.base = base;
        }

        private void paint(final int x, final int z, final int ground) {
            final double d = this.one.distance(x, z);
            final double r = this.one.radius();
            final double tall = r * this.config.height();
            switch (this.config.kind()) {
                case CRATER -> crater(x, z, ground, d, r, tall);
                case SHIELD_VOLCANO, STRATOVOLCANO -> volcano(x, z, ground, d, r, tall);
                case MESA -> mesa(x, z, ground, d, r, tall);
                case DUNES -> {
                    final double rise = LandformShapes.dune(this.one.along(x, z), 12 + r / 10, tall)
                            * LandformShapes.fade(d, r, r * FADING);
                    raise(x, z, ground, ground + (int) Math.round(rise), this.config.surface());
                }
                case CANYON -> canyon(x, z, ground, r, tall);
                case RIVER -> river(x, z, ground, r, tall);
                case LAVA_TUBE -> tube(x, z, ground, r, tall);
                case ICE_SHEET -> {
                    final int thick = (int) Math.round(tall * LandformShapes.fade(d, r, r * FADING));
                    if (thick > 0) {
                        fill(x, z, ground + 1, ground + thick, this.config.special());
                        put(x, ground + thick + 1, z, this.config.surface());
                    }
                }
                case GEYSER -> geyser(x, z, ground, d, r, tall);
                case SINKHOLE -> {
                    if (d < r) {
                        final int bottom = ground - (int) Math.round(tall * 2 * (1 - FADING * d / r));
                        lower(x, z, ground, bottom, this.config.surface());
                        if (!this.config.special().isAir()) {
                            fill(x, z, bottom + 1, bottom + 2, this.config.special());
                        }
                    }
                }
                case SALT_FLAT -> saltFlat(x, z, ground, d, r);
                case LAKE -> lake(x, z, ground, d, r, tall);
            }
        }

        private void crater(final int x, final int z, final int ground, final double d, final double r,
                            final double tall) {
            final double peak = r > LARGE_CRATER ? tall * 0.4 : 0;
            final int top = ground + (int) Math.round(LandformShapes.crater(d, r, tall, tall * FADING, peak));
            if (top < ground) {
                lower(x, z, ground, top, this.config.surface());
            } else if (top > ground) {
                raise(x, z, ground, top, this.config.surface());
            }
            if (d >= r && d < r * 2) {
                // The ejecta lie as a blanket over the ground, thick at the rim and thinning outwards, with a few
                // boulders thrown on it and rays running out through it.
                final double angle = Math.atan2(z - this.one.z(), x - this.one.x()) + this.one.turn();
                final double thin = 1 - (d - r) / r;
                final double roll = chance(x, z);
                if (roll < thin * thin * thin * BOULDERS) {
                    put(x, top + 1, z, this.config.scatter());
                } else if (Math.cos(angle * RAYS) > RAY_WIDTH || roll < thin * thin * EJECTA) {
                    put(x, top, z, this.config.scatter());
                }
            }
        }

        private void volcano(final int x, final int z, final int ground, final double d, final double r,
                             final double tall) {
            if (d >= r) {
                return;
            }
            final boolean shield = this.config.kind() == LandformKind.SHIELD_VOLCANO;
            final int top = ground + (int) Math.round(LandformShapes.volcano(d, r, tall, shield));
            raise(x, z, ground, top, this.config.surface());
            if (LandformShapes.inSummit(d, r)) {
                fill(x, z, top - SUMMIT_POOL + 1, top, this.config.special());
            }
        }

        private void mesa(final int x, final int z, final int ground, final double d, final double r,
                          final double tall) {
            final double rise = LandformShapes.mesa(d, r, tall, MESA_EDGE);
            if (rise <= 0) {
                return;
            }
            final int top = Math.max(ground, this.base + (int) Math.round(rise));
            for (int y = ground + 1; y < top; y++) {
                put(x, y, z, band(y - this.base));
            }
            if (top > ground) {
                put(x, top, z, this.config.surface());
            }
        }

        private void canyon(final int x, final int z, final int ground, final double r, final double tall) {
            final double along = this.one.along(x, z);
            if (Math.abs(along) > r) {
                return;
            }
            final double width = Math.max(6, r / 4);
            final double across = winding(x, z, along, width);
            final double depth = tall * LandformShapes.fade(Math.abs(along), r, r * FADING);
            final int cut = (int) Math.round(LandformShapes.canyon(across, width, depth));
            if (cut > 0) {
                lower(x, z, ground, ground - cut, this.config.surface());
                if (!this.config.special().isAir() && cut > 2) {
                    put(x, ground - cut + 1, z, this.config.special());
                }
            }
        }

        /*
         * A river runs at the height of its middle where the land stands higher, cutting a gorge through it, and
         * follows the land a block down where it lies lower, so its water never stands above its banks. It is as deep
         * as its height says, at least a block.
         */
        private void river(final int x, final int z, final int ground, final double r, final double tall) {
            final double along = this.one.along(x, z);
            if (Math.abs(along) > r) {
                return;
            }
            final double width = Math.max(4, r / 12);
            final double across = winding(x, z, along, width);
            if (Math.abs(across) >= width / 2) {
                return;
            }
            final double depth = Math.max(1, tall) * LandformShapes.fade(Math.abs(along), r, r * FADING);
            final int cut = (int) Math.round(LandformShapes.canyon(across, width, depth));
            final int surface = Math.min(ground, this.base) - 1;
            final int floor = surface - cut;
            if (cut <= 0 || floor >= ground) {
                return;
            }
            lower(x, z, ground, floor, this.config.surface());
            liquid(x, z, floor + 1, surface);
        }

        /* A lava tube is as wide as its height says, at least a block either way, and lies as deep under the ground. */
        private void tube(final int x, final int z, final int ground, final double r, final double tall) {
            final double along = this.one.along(x, z);
            final double across = this.one.across(x, z);
            final int bore = Math.max(1, (int) Math.round(tall / 2));
            if (Math.abs(along) > r || Math.abs(across) > bore) {
                return;
            }
            final int middle = ground - 2 * bore;
            final int half = (int) Math.floor(Math.sqrt(bore * bore - across * across));
            if (half <= 0) {
                return;
            }
            fill(x, z, middle - half + 1, middle + half, Blocks.CAVE_AIR.defaultBlockState());
            put(x, middle - half, z, this.config.special());
            final int stretch = (int) Math.floor(along / SKYLIGHT_SPACING);
            if (Math.abs(across) < 1 && chance(stretch, 0) < SKYLIGHT) {
                fill(x, z, middle + half + 1, ground, Blocks.AIR.defaultBlockState());
            }
        }

        /* A geyser's vent goes down as deep as its height says, at least two blocks. */
        private void geyser(final int x, final int z, final int ground, final double d, final double r,
                            final double tall) {
            if (d >= r) {
                return;
            }
            final int top = ground + (int) Math.round(GEYSER_MOUND * (1 - d / r));
            raise(x, z, ground, top, this.config.surface());
            if (x == this.one.x() && z == this.one.z()) {
                final int vent = Math.max(2, (int) Math.round(tall));
                fill(x, z, top - vent + 1, top, this.config.special());
                put(x, top - vent, z, this.config.scatter());
                // The vent's floor bubbles once the world ticks it, as a magma block under water does when placed.
                this.level.scheduleTick(this.at.set(x, top - vent, z).immutable(), this.config.scatter().getBlock(),
                        MAGMA_TICK);
            }
        }

        private void saltFlat(final int x, final int z, final int ground, final double d, final double r) {
            final double share = LandformShapes.fade(d, r, r * 0.2);
            if (share <= 0) {
                return;
            }
            final int flat = (int) Math.round(ground + (this.base - ground) * share);
            final BlockState top = cracked(x, z) ? this.config.scatter() : this.config.special();
            if (flat < ground) {
                lower(x, z, ground, flat, top);
            } else if (flat > ground) {
                raise(x, z, ground, flat, top);
            } else {
                put(x, ground, z, top);
            }
        }

        /*
         * A lake's water stands a block under the ground at its middle; where the land round it lies lower than its
         * water, a bank is raised at its edge so the water stands in the bowl.
         */
        private void lake(final int x, final int z, final int ground, final double d, final double r,
                          final double tall) {
            if (d >= r) {
                return;
            }
            final double share = d / r;
            final int surface = this.base - 1;
            final int bottom = Math.min(ground, surface - (int) Math.round(tall * (1 - share * share)));
            if (d >= r - BANK && ground <= surface) {
                raise(x, z, ground, surface + 1, this.config.surface());
                return;
            }
            if (bottom < ground) {
                lower(x, z, ground, bottom, this.config.surface());
            }
            liquid(x, z, bottom + 1, surface);
        }

        /* Fills from {@code from} to {@code to} with the liquid: frozen on top where cold, none where it boils. */
        private void liquid(final int x, final int z, final int from, final int to) {
            if (to < from) {
                return;
            }
            final double kelvin = BiomeClimates.kelvinAsMade(this.level, this.at.set(x, to, z));
            if (kelvin >= this.config.boils()) {
                return;
            }
            fill(x, z, from, to, this.config.special());
            if (kelvin < this.config.freezes()) {
                put(x, to, z, this.config.scatter());
            }
        }

        /* Builds the column up from {@code ground} to {@code top}: the fill, {@code surface} on top. */
        private void raise(final int x, final int z, final int ground, final int top, final BlockState surface) {
            if (top <= ground) {
                return;
            }
            fill(x, z, ground + 1, top - 1, this.config.fill());
            put(x, top, z, surface);
        }

        /*
         * Cuts the column down from {@code ground} to {@code floor}, {@code floorBlock} at its bottom; under a sea
         * the cut fills with the sea, as the sea would flow into it.
         */
        private void lower(final int x, final int z, final int ground, final int floor, final BlockState floorBlock) {
            final FluidState above = this.level.getFluidState(this.at.set(x, ground + 1, z));
            final BlockState hollow = above.isSource() ? above.createLegacyBlock()
                    : Blocks.AIR.defaultBlockState();
            fill(x, z, floor + 1, ground, hollow);
            put(x, floor, z, floorBlock);
        }

        private void fill(final int x, final int z, final int from, final int to, final BlockState state) {
            for (int y = from; y <= to; y++) {
                put(x, y, z, state);
            }
        }

        private void put(final int x, final int y, final int z, final BlockState state) {
            if (y > this.level.getMinBuildHeight() && y < this.level.getMaxBuildHeight()) {
                this.level.setBlock(this.at.set(x, y, z), state, UPDATE);
            }
        }

        /* How far across a winding channel {@code x, z} lies from the line it winds about. */
        private double winding(final int x, final int z, final double along, final double width) {
            return this.one.across(x, z) + Math.sin(along / MEANDER * 2 * Math.PI) * width;
        }

        private BlockState band(final int above) {
            final List<BlockState> bands = this.config.bands();
            return bands.isEmpty() ? this.config.fill() : bands.get(Math.floorMod(above, bands.size()));
        }

        private boolean cracked(final int x, final int z) {
            final double a = this.one.along(x, z) / CRACK_SPACING;
            final double b = this.one.across(x, z) / CRACK_SPACING;
            return a - Math.floor(a) < CRACK || b - Math.floor(b) < CRACK;
        }

        /* A number from 0 to 1 of a column of the landform, the same each time it is asked. */
        private double chance(final int x, final int z) {
            return new SplittableRandom(this.one.seed() ^ x * MIX_X ^ z * MIX_Z).nextDouble();
        }
    }
}
