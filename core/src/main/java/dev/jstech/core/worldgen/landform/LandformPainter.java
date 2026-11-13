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

    /*
     * How deep a volcano's summit is filled; how thick a lava tube's wall of the lava that set round it is; where its
     * roof fell in, one in its middle and others every so many blocks along it by chance, how wide each skylight is
     * for the tube's bore, and the rubble heaped under it, how far out and how high for the bore; and how a channel
     * winds.
     */
    private static final int SUMMIT_POOL = 2;
    private static final int TUBE_WALL = 2;
    private static final double SKYLIGHT = 0.5;
    private static final int SKYLIGHT_EVERY = 36;
    private static final double SKYLIGHT_WIDTH = 0.5;
    private static final int SKYLIGHT_LEAST = 2;
    private static final double RUBBLE_REACH = 0.5;
    private static final double RUBBLE_HEIGHT = 0.4;
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
    /* How much more tightly a rille winds than a canyon, and how far it swings, as a share of its width. */
    private static final double RILLE_WINDING = 3.0;
    private static final double RILLE_SWING = 1.5;
    /* How wide a glacier's moraines are, and how far apart its crevasses lie down its length. */
    private static final int MORAINE = 2;
    private static final int CREVASSE_SPACING = 5;
    /* How far past a cryovolcano's foot what it threw out lies, as a share of its radius. */
    private static final double PLUME = 0.5;
    private static final int UPDATE = 2;
    /* How high over the ground what stands on it may reach: a stone, a plant, a plant two blocks tall. */
    private static final int STANDING = 2;
    /* How deep a terrace's pools are, how much of a chimney's middle is hollow and how deep, a layered mound's step. */
    private static final int TERRACE_STEP = 2;
    /*
     * How far a terraced apron's outline swells and shrinks round it, as a share of its radius, in a few lobes; and how
     * far its pools' edges wander in and out, in blocks, so no pool is a ring.
     */
    private static final double TERRACE_LOBE = 0.35;
    private static final int TERRACE_LOBES = 3;
    private static final double TERRACE_WANDER = 4.0;
    private static final double WANDER_WAVE = 0.11;
    /* A spider's fan lies in clumps of this many blocks a side, not grain by grain. */
    private static final int FAN_CLUMP = 2;
    /*
     * A cave: how many tunnels run out of its chamber, at least and at most more; in how many straight lengths each
     * winds; how far, in radians, each strays from an even spread round the chamber; how far it reaches at least, as a
     * share of the radius; how wide a tunnel is, at least and at most more; how far it wanders aside and up or down,
     * as shares of the radius and of the depth; how much flatter than round it is; its chamber, as a share of the
     * radius, at least so many blocks and at most a share of the radius, how high for its width and how much of it
     * lies under its middle; how deep it lies at least; its crack, how wide either way and how long for the chamber;
     * how a mouth rises as it nears its end; and how far over the world's floor nothing is hollowed.
     */
    private static final int CAVE_TUNNELS = 3;
    private static final int CAVE_MORE_TUNNELS = 3;
    private static final int CAVE_LENGTHS = 6;
    private static final double CAVE_SPREAD = 0.4;
    private static final double CAVE_LEAST_REACH = 0.6;
    private static final double CAVE_BORE = 1.8;
    private static final double CAVE_MORE_BORE = 1.4;
    private static final double CAVE_WANDER = 0.12;
    private static final double CAVE_DRIFT = 0.3;
    private static final double CAVE_FLAT = 0.8;
    private static final double CHAMBER_SHARE = 0.22;
    private static final int CHAMBER_LEAST = 4;
    private static final double CHAMBER_MOST = 0.6;
    private static final double CHAMBER_HEIGHT = 0.6;
    private static final double CHAMBER_FLOOR = 0.5;
    private static final int CAVE_LEAST_DEPTH = 8;
    private static final double CRACK_HALF = 1.0;
    private static final double CRACK_LENGTH = 0.7;
    private static final double MOUTH_RISE = 1.5;
    private static final double MOUTH_SINK = 0.4;
    private static final int FLOOR_KEPT = 4;
    /*
     * A pit's chamber, as wide for the shaft's radius and as high, at least so many blocks; how ragged its rim is, as
     * a share of the radius, and the step round it, how far out for the rim and how deep; and the rubble under the
     * shaft, as far out and as high for its radius.
     */
    private static final double PIT_ROOM = 2.5;
    private static final double PIT_ROOM_HEIGHT = 1.2;
    private static final int PIT_LEAST_ROOM_HEIGHT = 3;
    private static final double PIT_RAGGED = 0.3;
    private static final double PIT_STEP = 1.35;
    private static final int PIT_STEP_DEPTH = 2;
    private static final double PIT_RUBBLE_REACH = 1.5;
    private static final double PIT_RUBBLE_HEIGHT = 1.2;
    private static final double CHIMNEY_HOLLOW = 0.75;
    private static final int CHIMNEY_HOLLOW_DEPTH = 2;
    private static final int MOUND_STEP = 3;
    /*
     * How many legs a spider has, at least and at most more, how far each strays from an even spread, in radians; how
     * far its fan reaches downwind, as a share of its radius, how wide it is at the pit and how it widens, and how
     * much of it has thinned out by its tip.
     */
    private static final int SPIDER_LEGS = 8;
    private static final int SPIDER_MORE_LEGS = 6;
    private static final double SPIDER_STRAY = 0.2;
    private static final double SPIDER_FAN = 1.6;
    private static final double FAN_ROOT = 1.5;
    private static final double FAN_WIDENING = 0.32;
    private static final double FAN_THINNING = 0.8;
    /* How many ridges cross a field of them, at least and at most more; how wide one is for its height, at least. */
    private static final int RIDGES = 3;
    private static final int MORE_RIDGES = 3;
    private static final double RIDGE_WIDTH = 1.4;
    private static final int RIDGE_LEAST_WIDTH = 3;
    private static final double RIDGE_OFFSET = 0.4;
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
        /* A spider's legs, the angles they run out at; a field of ridges' lines, each its angle and its offset. */
        private final double[] legs;
        private final double[][] lines;
        /* A terraced apron's lobes, each its phase round it, and the phases its pools' edges wander by. */
        private final double[] lobes;
        private final double[] wander;
        /*
         * A cave's tunnels, each a line of points out from its middle, across and along as shares of its radius and up
         * or down as a share of its depth; how wide each is; and which way its crack runs.
         */
        private final double[][][] tunnels;
        private final double[] bores;
        private final double crack;

        private Column(final WorldGenLevel level, final LandformConfig config, final LandformField.Instance one,
                       final int base) {
            this.level = level;
            this.config = config;
            this.one = one;
            this.base = base;
            final SplittableRandom random = new SplittableRandom(one.seed());
            this.legs = new double[SPIDER_LEGS + random.nextInt(SPIDER_MORE_LEGS)];
            for (int k = 0; k < this.legs.length; k++) {
                this.legs[k] = 2 * Math.PI * k / this.legs.length + (random.nextDouble() * 2 - 1) * SPIDER_STRAY;
            }
            this.lines = new double[RIDGES + random.nextInt(MORE_RIDGES)][];
            for (int k = 0; k < this.lines.length; k++) {
                this.lines[k] = new double[] {random.nextDouble() * Math.PI,
                        (random.nextDouble() * 2 - 1) * RIDGE_OFFSET};
            }
            this.lobes = new double[TERRACE_LOBES];
            for (int k = 0; k < TERRACE_LOBES; k++) {
                this.lobes[k] = random.nextDouble() * 2 * Math.PI;
            }
            this.wander = new double[] {random.nextDouble() * 2 * Math.PI, random.nextDouble() * 2 * Math.PI};
            final int count = CAVE_TUNNELS + random.nextInt(CAVE_MORE_TUNNELS);
            this.tunnels = new double[count][CAVE_LENGTHS + 1][];
            this.bores = new double[count];
            // A tunnel's end stops its widest bore short of the radius, so no cave reaches past its kind's reach.
            final double inside = Math.max(0, 1 - (CAVE_BORE + CAVE_MORE_BORE) / one.radius());
            for (int k = 0; k < count; k++) {
                final double angle = 2 * Math.PI * k / count + (random.nextDouble() * 2 - 1) * CAVE_SPREAD;
                final double reach = (CAVE_LEAST_REACH + random.nextDouble() * (1 - CAVE_LEAST_REACH)) * inside;
                final double phase = random.nextDouble() * 2 * Math.PI;
                final double drift = (random.nextDouble() * 2 - 1) * CAVE_DRIFT;
                this.bores[k] = CAVE_BORE + random.nextDouble() * CAVE_MORE_BORE;
                for (int i = 0; i <= CAVE_LENGTHS; i++) {
                    final double t = (double) i / CAVE_LENGTHS;
                    final double out = reach * t;
                    final double aside = Math.sin(t * 2 * Math.PI + phase) * CAVE_WANDER * t;
                    this.tunnels[k][i] = new double[] {out * Math.cos(angle) - aside * Math.sin(angle),
                            out * Math.sin(angle) + aside * Math.cos(angle), drift * t};
                }
            }
            this.crack = random.nextDouble() * Math.PI;
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
                case RILLE -> rille(x, z, ground, r, tall);
                case GLACIER -> glacier(x, z, ground, r, tall);
                case CRYOVOLCANO -> cryovolcano(x, z, ground, d, r, tall);
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
                        // Only within the shaft: one too shallow to hold it leaves the ground above it bare.
                        if (!this.config.special().isAir() && bottom + 1 <= ground - 1) {
                            fill(x, z, bottom + 1, Math.min(bottom + 2, ground - 1), this.config.special());
                        }
                    }
                }
                case SALT_FLAT -> saltFlat(x, z, ground, d, r);
                case LAKE -> lake(x, z, ground, d, r, tall);
                case TERRACES -> terraces(x, z, ground, d, r, tall);
                case CHIMNEY -> chimney(x, z, ground, d, r, tall);
                case SPIDER -> spider(x, z, ground, d, r);
                case RIDGES -> ridges(x, z, ground, d, r, tall);
                case LAYERED_MOUND -> layeredMound(x, z, ground, d, r, tall);
                case CAVE -> cave(x, z, ground, d, r, tall);
                case GROTTO -> grotto(x, z, ground, r, tall);
                case PIT -> pit(x, z, ground, d, r, tall);
            }
            settle(x, z, ground);
        }

        /*
         * Whatever stood on the ground before it was painted and can no longer stand there goes, a stone or a plant
         * over ground cut away or opened under it: a landform is painted a chunk at a time, often after the stones and
         * plants of the chunk beside were laid on ground it then cuts, which would leave them hanging over nothing.
         * From the bottom up, so what stood on what went goes too.
         */
        private void settle(final int x, final int z, final int ground) {
            for (int y = ground + 1; y <= ground + STANDING; y++) {
                final BlockState state = this.level.getBlockState(this.at.set(x, y, z));
                if (!state.isAir() && !state.canSurvive(this.level, this.at)) {
                    put(x, y, z, Blocks.AIR.defaultBlockState());
                }
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

        /* A rille is narrower than a canyon and winds three times as tightly, a smooth trough rather than a gorge. */
        private void rille(final int x, final int z, final int ground, final double r, final double tall) {
            final double along = this.one.along(x, z);
            if (Math.abs(along) > r) {
                return;
            }
            final double width = Math.max(4, r / 6);
            final double across = this.one.across(x, z)
                    + Math.sin(along / (MEANDER / RILLE_WINDING) * 2 * Math.PI) * width * RILLE_SWING;
            final double depth = tall * LandformShapes.fade(Math.abs(along), r, r * FADING);
            final int cut = (int) Math.round(LandformShapes.rille(across, width, depth));
            if (cut > 0) {
                lower(x, z, ground, ground - cut, this.config.surface());
            }
        }

        /*
         * A glacier lies down its line from its head, as wide as a third of its length: its ice the special under the
         * surface, cut open by crevasses across its lower half, and moraines of the scatter heaped on the ground along
         * its edges and before its snout, where the ice has given out.
         */
        private void glacier(final int x, final int z, final int ground, final double r, final double tall) {
            final double along = this.one.along(x, z);
            final double across = this.one.across(x, z);
            if (Math.abs(along) > r) {
                return;
            }
            final double width = Math.max(6, r * 2 / 3);
            final int thick = (int) Math.round(LandformShapes.glacier(along, across, r, width, tall));
            if (thick <= 0) {
                final boolean beside = Math.abs(across) >= width / 2 && Math.abs(across) < width / 2 + MORAINE;
                final boolean before = along >= r - MORAINE && Math.abs(across) < width / 2;
                if (beside || before) {
                    put(x, ground + 1, z, this.config.scatter());
                }
                return;
            }
            fill(x, z, ground + 1, ground + thick, this.config.special());
            put(x, ground + thick + 1, z, this.config.surface());
            final int crack = (int) Math.floor(along);
            if (along > 0 && thick > 1 && Math.floorMod(crack, CREVASSE_SPACING) == 0
                    && Math.abs(across) < width / 4) {
                fill(x, z, ground + thick - thick / 2 + 1, ground + thick + 1, Blocks.AIR.defaultBlockState());
            }
        }

        /*
         * A cryovolcano stands as a broad dome, built of the fill under its surface, its summit holding the special,
         * and what it threw out lies past its foot, all of it near the foot and less outwards.
         */
        private void cryovolcano(final int x, final int z, final int ground, final double d, final double r,
                                 final double tall) {
            if (d < r) {
                final int top = ground + (int) Math.round(LandformShapes.volcano(d, r, tall, true));
                raise(x, z, ground, top, this.config.surface());
                if (LandformShapes.inSummit(d, r)) {
                    fill(x, z, top - SUMMIT_POOL + 1, top, this.config.special());
                }
                return;
            }
            final double thin = 1 - (d - r) / (r * PLUME);
            if (thin > 0 && chance(x, z) < thin) {
                put(x, ground + 1, z, this.config.scatter());
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

        /*
         * A lava tube is as wide as its height says, at least a block either way, and lies as deep under the ground,
         * walled all round in the special, the lava that set round it, whatever rock it runs through. Its roof has
         * fallen in at its middle, and every so many blocks along it by chance: a skylight open to the sky, its rubble
         * of the scatter heaped on the tube's floor under it, where the light falls.
         */
        private void tube(final int x, final int z, final int ground, final double r, final double tall) {
            final double along = this.one.along(x, z);
            final double across = this.one.across(x, z);
            final int bore = Math.max(1, (int) Math.round(tall / 2));
            final int wall = bore + TUBE_WALL;
            if (Math.abs(along) > r || Math.abs(across) > wall) {
                return;
            }
            final int middle = ground - 2 * bore;
            final int outer = (int) Math.floor(Math.sqrt(wall * wall - across * across));
            fill(x, z, middle - outer, Math.min(ground - 1, middle + outer), this.config.special());
            if (Math.abs(across) >= bore) {
                return;
            }
            final int half = (int) Math.floor(Math.sqrt(bore * bore - across * across));
            if (half <= 0) {
                return;
            }
            final int floor = middle - half;
            fill(x, z, floor + 1, middle + half, Blocks.CAVE_AIR.defaultBlockState());
            final int which = (int) Math.round(along / SKYLIGHT_EVERY);
            if (which != 0 && chance(which, 0) >= SKYLIGHT) {
                return;
            }
            final double fromLight = Math.hypot(along - which * SKYLIGHT_EVERY, across);
            if (fromLight < Math.max(SKYLIGHT_LEAST, bore * SKYLIGHT_WIDTH)) {
                fill(x, z, middle + half + 1, ground, Blocks.AIR.defaultBlockState());
            }
            final int heap = (int) Math.round(LandformShapes.heap(fromLight, bore * RUBBLE_REACH,
                    bore * RUBBLE_HEIGHT));
            if (heap > 0) {
                fill(x, z, floor + 1, floor + heap, this.config.scatter());
            }
        }

        /*
         * A cave lies under the ground at its middle, its chamber the deepest of it: a crack opens over the chamber to
         * the sky, and its tunnels run out, rising or falling a little, roofed by a block at least, coming out where
         * they meet a steep slope or a wall; the first rises as it goes and opens on the ground at its end, wherever
         * the ground stands there.
         */
        private void cave(final int x, final int z, final int ground, final double d, final double r,
                          final double tall) {
            final double depth = Math.max(CAVE_LEAST_DEPTH, tall);
            final double deep = this.base - depth;
            final double room = Math.min(r * CHAMBER_MOST, Math.max(CHAMBER_LEAST, r * CHAMBER_SHARE));
            final double roof = LandformShapes.chamber(d, room, room * CHAMBER_HEIGHT);
            if (roof > 0) {
                hollow(x, z, deep - roof * CHAMBER_FLOOR, deep + roof, ground - 1);
            }
            final double dx = x - this.one.x();
            final double dz = z - this.one.z();
            final double along = dx * Math.cos(this.crack) + dz * Math.sin(this.crack);
            final double across = -dx * Math.sin(this.crack) + dz * Math.cos(this.crack);
            if (Math.abs(across) <= CRACK_HALF && Math.abs(along) < room * CRACK_LENGTH) {
                hollow(x, z, deep, ground, ground);
            }
            for (int k = 0; k < this.tunnels.length; k++) {
                final double[] near = nearest(this.tunnels[k], dx / r, dz / r);
                final double bore = this.bores[k];
                final double half = LandformShapes.chamber(near[0] * r, bore, bore * CAVE_FLAT);
                if (half <= 0) {
                    continue;
                }
                final double level = deep + near[2] * depth;
                if (k == 0) {
                    final double rise = Math.pow(near[1], MOUTH_RISE);
                    final double centre = level + (ground - bore * CAVE_FLAT * MOUTH_SINK - level) * rise;
                    hollow(x, z, centre - half, centre + half, ground);
                } else {
                    hollow(x, z, level - half, level + half, ground - 1);
                }
            }
        }

        /*
         * Grottos run out from the middle a block over its ground, the cave's tunnels, each climbing as it goes by as
         * much as its drift says, up to the landform's height at its end; hollowed only where they run through rock,
         * roofed by a block at least, so each opens in a face it meets, a mesa's, a butte's, a mound's, at its foot or
         * up it, and nothing is hollowed under the open ground.
         */
        private void grotto(final int x, final int z, final int ground, final double r, final double tall) {
            final double dx = x - this.one.x();
            final double dz = z - this.one.z();
            for (int k = 0; k < this.tunnels.length; k++) {
                final double[] near = nearest(this.tunnels[k], dx / r, dz / r);
                final double bore = this.bores[k];
                final double half = LandformShapes.chamber(near[0] * r, bore, bore * CAVE_FLAT);
                if (half <= 0) {
                    continue;
                }
                final double level = this.base + 1 + bore * CAVE_FLAT + tall * Math.abs(near[2]) / CAVE_DRIFT;
                hollow(x, z, level - half, level + half, ground - 1);
            }
        }

        /*
         * A pit's shaft runs from the ground down into a domed chamber at its depth, wider than the shaft, its floor
         * level; its rim broke away as the roof fell, its outline ragged and a step round it; the rubble of the roof
         * lies heaped on the floor under the shaft, the special lining the floor where it is not air.
         */
        private void pit(final int x, final int z, final int ground, final double d, final double r,
                         final double tall) {
            final double room = r * PIT_ROOM;
            if (d >= room) {
                return;
            }
            final int floor = this.base - (int) Math.round(Math.max(PIT_LEAST_ROOM_HEIGHT * 2, tall));
            final double high = Math.max(PIT_LEAST_ROOM_HEIGHT, r * PIT_ROOM_HEIGHT);
            final double edge = r * (1 + PIT_RAGGED * swell(x, z));
            final boolean shaft = d < edge;
            hollow(x, z, floor, shaft ? ground : floor + LandformShapes.chamber(d, room, high),
                    shaft ? ground : ground - 1);
            if (!shaft && d < edge * PIT_STEP) {
                hollow(x, z, ground - PIT_STEP_DEPTH + 1, ground, ground);
            }
            if (!this.config.special().isAir()) {
                put(x, floor - 1, z, this.config.special());
            }
            final int heap = (int) Math.round(LandformShapes.heap(d, r * PIT_RUBBLE_REACH, r * PIT_RUBBLE_HEIGHT));
            if (heap > 0) {
                fill(x, z, floor, floor + heap - 1, this.config.scatter());
            }
        }

        /*
         * How far an outline swells out or in at the column's bearing from the middle, from -1 to 1: a few lobes of
         * the landform's own, as a spring's terraces grew where the water ran or a pit's rim broke away.
         */
        private double swell(final int x, final int z) {
            final double angle = Math.atan2(z - this.one.z(), x - this.one.x());
            double swell = 0;
            for (int k = 0; k < TERRACE_LOBES; k++) {
                swell += Math.sin((k + 2) * angle + this.lobes[k]) / TERRACE_LOBES;
            }
            return swell;
        }

        /*
         * Hollows the column with cave air from {@code from} up to {@code to}, no higher than {@code ceiling}, and
         * never into the few blocks over the world's floor.
         */
        private void hollow(final int x, final int z, final double from, final double to, final int ceiling) {
            final int low = Math.max(this.level.getMinBuildHeight() + FLOOR_KEPT, (int) Math.round(from));
            final int high = Math.min(ceiling, (int) Math.round(to));
            fill(x, z, low, high, Blocks.CAVE_AIR.defaultBlockState());
        }

        /*
         * The point of the line {@code points} nearest {@code px, pz}: how far it lies from it, how far along the line
         * it is, from 0 to 1, and the line's rise there.
         */
        private static double[] nearest(final double[][] points, final double px, final double pz) {
            double best = Double.MAX_VALUE;
            double along = 0;
            double rise = 0;
            final int lengths = points.length - 1;
            for (int i = 0; i < lengths; i++) {
                final double[] a = points[i];
                final double[] b = points[i + 1];
                final double ex = b[0] - a[0];
                final double ez = b[1] - a[1];
                final double span = ex * ex + ez * ez;
                final double u = span == 0 ? 0
                        : Math.max(0, Math.min(1, ((px - a[0]) * ex + (pz - a[1]) * ez) / span));
                final double far = Math.hypot(px - (a[0] + u * ex), pz - (a[1] + u * ez));
                if (far < best) {
                    best = far;
                    along = (i + u) / lengths;
                    rise = a[2] + u * (b[2] - a[2]);
                }
            }
            return new double[] {best, along, rise};
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

        /*
         * A terraced apron's pools lie level, each at its step's height over the ground at the apron's middle, whatever
         * the ground under it does; the rim at each pool's lower edge stands a block over the pool's floor. Its outline
         * swells in a few lobes and its pools' edges wander, as a spring's terraces grow where the water ran.
         */
        private void terraces(final int x, final int z, final int ground, final double d, final double r,
                              final double tall) {
            final double at = d / (1 + TERRACE_LOBE * swell(x, z)) + TERRACE_WANDER
                    * Math.sin(x * WANDER_WAVE + this.wander[0]) * Math.cos(z * WANDER_WAVE + this.wander[1]);
            if (at >= r || d >= r * (1 + TERRACE_LOBE)) {
                return;
            }
            final int floor = this.base + (int) Math.round(LandformShapes.terrace(Math.max(0, at), r, tall,
                    TERRACE_STEP));
            // The lowest pool lies level with the ground round it, which holds it: it needs no rim.
            final boolean rim = floor > this.base
                    && LandformShapes.terraceRim(Math.max(0, at), r, tall, TERRACE_STEP);
            final int top = rim ? floor + 1 : floor;
            final BlockState face = rim ? this.config.scatter() : this.config.surface();
            if (top > ground) {
                raise(x, z, ground, top, face);
            } else if (top < ground) {
                lower(x, z, ground, top, face);
            } else {
                put(x, top, z, face);
            }
        }

        /* A chimney stands on the ground where it is; one two blocks across or more is hollow down its middle. */
        private void chimney(final int x, final int z, final int ground, final double d, final double r,
                             final double tall) {
            final int top = ground + (int) Math.round(LandformShapes.chimney(d, r, tall));
            if (top <= ground) {
                return;
            }
            raise(x, z, ground, top, this.config.surface());
            if (d < CHIMNEY_HOLLOW && r >= 2) {
                fill(x, z, top - CHIMNEY_HOLLOW_DEPTH + 1, top, Blocks.AIR.defaultBlockState());
            }
        }

        /*
         * A spider's troughs are cut into the ground from its own height; its fan lies over the ground outside them,
         * downwind of the pit along its turn, widening and thinning out towards its tip.
         */
        private void spider(final int x, final int z, final int ground, final double d, final double r) {
            final double angle = Math.atan2(z - this.one.z(), x - this.one.x());
            final int cut = LandformShapes.spider(d, angle, r, this.legs);
            if (cut > 0) {
                lower(x, z, ground, ground - cut, this.config.surface());
                return;
            }
            final double along = this.one.along(x, z);
            final double length = r * SPIDER_FAN;
            final double clump = chance(Math.floorDiv(x, FAN_CLUMP), Math.floorDiv(z, FAN_CLUMP));
            if (along > 0 && along < length && Math.abs(this.one.across(x, z)) < FAN_ROOT + along * FAN_WIDENING
                    && clump > along / length * FAN_THINNING) {
                put(x, ground + 1, z, this.config.scatter());
            }
        }

        /* Each ridge crosses the field along its own line, the highest of them standing where they meet. */
        private void ridges(final int x, final int z, final int ground, final double d, final double r,
                            final double tall) {
            if (d >= r) {
                return;
            }
            final double width = Math.max(RIDGE_LEAST_WIDTH, tall * RIDGE_WIDTH);
            double rise = 0;
            for (final double[] line : this.lines) {
                final double across = -(x - this.one.x()) * Math.sin(line[0]) + (z - this.one.z()) * Math.cos(line[0])
                        - line[1] * r;
                rise = Math.max(rise, LandformShapes.ridge(across, width, tall));
            }
            rise *= LandformShapes.fade(d, r, r * FADING);
            raise(x, z, ground, ground + (int) Math.round(rise), this.config.surface());
        }

        /* A layered mound stands from the ground at its middle, its bands counted from there, as a mesa's are. */
        private void layeredMound(final int x, final int z, final int ground, final double d, final double r,
                                  final double tall) {
            final double rise = LandformShapes.layeredMound(d, r, tall, MOUND_STEP);
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
