/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.worldgen.landform.LandformConfig;
import dev.jstech.core.worldgen.landform.LandformField;
import dev.jstech.core.worldgen.landform.LandformKind;
import dev.jstech.core.worldgen.landform.LandformPainter;
import dev.jstech.core.worldgen.landform.SeaConfig;
import dev.jstech.core.worldgen.landform.SeaFeature;
import dev.jstech.core.worldgen.strata.StrataConfig;
import dev.jstech.core.worldgen.strata.StrataFeature;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's landforms, rock in layers and seas, each laid into a test's arena over a floor of stone three blocks
 * thick: what each kind does to the ground, the liquids that freeze where it is cold and boil away where it is hot,
 * and the layers of rock by depth. The arenas are open to the sky, so the ground's height read from the world is the
 * floor's and not the barriers a closed arena is roofed with.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LandformGameTests {

    private static final String ARENA = "empty";
    /* The arena's floor of stone fills y 1 to 3, its top at 3; the arena is nine blocks a side. */
    private static final int TOP = 3;
    private static final int SIDE = 8;
    private static final int MIDDLE = 4;
    private static final double WATER_FREEZES = 273.15;
    private static final double WATER_BOILS = 373.15;
    /* Hotter and colder than any arena's air, to see a liquid freeze or boil whatever the place. */
    private static final double ABOVE_ANY_AIR = 400;
    private static final double BELOW_ANY_AIR = 200;

    private LandformGameTests() {
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void crater_digsABowlWithARaisedRim(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.CRATER, 0.5, Blocks.GRAVEL, Blocks.STONE, Blocks.WATER, Blocks.COBBLESTONE),
                3);
        helper.assertTrue(top(helper, MIDDLE, MIDDLE) < TOP, "the crater's middle is dug below the ground");
        helper.assertBlockState(new BlockPos(MIDDLE, top(helper, MIDDLE, MIDDLE), MIDDLE),
                state -> state.is(Blocks.GRAVEL), () -> "its floor is its surface");
        helper.assertTrue(top(helper, MIDDLE + 3, MIDDLE) >= TOP, "its rim stands at the ground or above it");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void volcano_raisesAConeWithLavaInItsSummit(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.STRATOVOLCANO, 0.5, Blocks.BASALT, Blocks.STONE, Blocks.LAVA,
                Blocks.MAGMA_BLOCK), 4);
        final int summit = topOfAnything(helper, MIDDLE, MIDDLE);
        helper.assertTrue(summit > TOP, "the cone rises over the ground");
        helper.assertBlockState(new BlockPos(MIDDLE, summit, MIDDLE), state -> state.is(Blocks.LAVA),
                () -> "and its summit holds lava");
        helper.assertBlockState(new BlockPos(MIDDLE + 2, top(helper, MIDDLE + 2, MIDDLE), MIDDLE),
                state -> state.is(Blocks.BASALT), () -> "its slopes are its surface");
        helper.assertTrue(top(helper, MIDDLE + 2, MIDDLE) < summit, "and fall away from its summit");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void mesa_standsInBandsUnderItsTop(final GameTestHelper helper) {
        floor(helper);
        paint(helper, new LandformConfig(LandformKind.MESA, 64, 1, 4, 4, 0, 0.5, state(Blocks.GRASS_BLOCK),
                state(Blocks.STONE), state(Blocks.WATER), state(Blocks.ICE),
                List.of(state(Blocks.RED_TERRACOTTA), state(Blocks.WHITE_TERRACOTTA)), WATER_FREEZES, WATER_BOILS,
                Optional.empty()), 4);
        helper.assertBlockState(new BlockPos(MIDDLE, TOP + 1, MIDDLE), state -> state.is(Blocks.WHITE_TERRACOTTA),
                () -> "the mesa's body is in its bands from its foot");
        helper.assertBlockState(new BlockPos(MIDDLE, TOP + 2, MIDDLE), state -> state.is(Blocks.GRASS_BLOCK),
                () -> "under its top of grass");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void dunes_raiseRidgesOfTheirFill(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.DUNES, 0.25, Blocks.SAND, Blocks.SAND, Blocks.WATER, Blocks.ICE), 8);
        int raised = 0;
        for (int x = 0; x <= SIDE; x++) {
            if (top(helper, x, MIDDLE) > TOP && helper.getBlockState(new BlockPos(x, top(helper, x, MIDDLE), MIDDLE))
                    .is(Blocks.SAND)) {
                raised++;
            }
        }
        helper.assertTrue(raised > 0, "the dunes raise ridges of sand across the ground");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void canyon_cutsAChannelThroughTheGround(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.CANYON, 0.5, Blocks.RED_SAND, Blocks.STONE, Blocks.AIR, Blocks.ICE), 4);
        int cut = 0;
        for (int z = 0; z <= SIDE; z++) {
            for (int x = 0; x <= SIDE; x++) {
                if (top(helper, x, z) < TOP) {
                    cut++;
                }
            }
        }
        helper.assertTrue(cut > 0 && cut < (SIDE + 1) * (SIDE + 1), "the canyon cuts a channel, not the whole land");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void river_runsItsLiquidInItsChannel(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.RIVER, 0.5, Blocks.GRAVEL, Blocks.STONE, Blocks.WATER, Blocks.ICE), 4);
        helper.assertTrue(count(helper, Blocks.WATER) > 0, "the river's channel holds water");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void lavaTube_hollowsATunnelUnderTheGround(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.LAVA_TUBE, 0.5, Blocks.STONE, Blocks.STONE, Blocks.BASALT, Blocks.ICE), 4);
        helper.assertBlockState(new BlockPos(MIDDLE, 1, MIDDLE), state -> state.is(Blocks.CAVE_AIR),
                () -> "the tube is hollow under the ground");
        helper.assertBlockState(new BlockPos(MIDDLE, 0, MIDDLE), state -> state.is(Blocks.BASALT),
                () -> "its floor is lined with its own block");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void iceSheet_coversTheLandInIceUnderSnow(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.ICE_SHEET, 0.25, Blocks.SNOW_BLOCK, Blocks.STONE, Blocks.PACKED_ICE,
                Blocks.ICE), 4);
        helper.assertBlockState(new BlockPos(MIDDLE, TOP + 1, MIDDLE), state -> state.is(Blocks.PACKED_ICE),
                () -> "the sheet lies on the ground in ice");
        helper.assertBlockState(new BlockPos(MIDDLE, TOP + 2, MIDDLE), state -> state.is(Blocks.SNOW_BLOCK),
                () -> "under snow");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void geyser_ventsWaterOverMagma(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.GEYSER, 1.0, Blocks.CALCITE, Blocks.CALCITE, Blocks.WATER,
                Blocks.MAGMA_BLOCK), 3);
        final int mound = TOP + 2;
        helper.assertBlockState(new BlockPos(MIDDLE, mound, MIDDLE), state -> state.is(Blocks.WATER),
                () -> "the vent is open at the top of the mound");
        helper.assertBlockState(new BlockPos(MIDDLE, mound - 3, MIDDLE), state -> state.is(Blocks.MAGMA_BLOCK),
                () -> "over magma at its foot");
        helper.assertBlockState(new BlockPos(MIDDLE + 1, top(helper, MIDDLE + 1, MIDDLE), MIDDLE),
                state -> state.is(Blocks.CALCITE), () -> "in a mound of sinter");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void sinkhole_dropsAShaftIntoTheGround(final GameTestHelper helper) {
        floor(helper);
        paint(helper, config(LandformKind.SINKHOLE, 0.5, Blocks.MOSSY_COBBLESTONE, Blocks.STONE, Blocks.AIR,
                Blocks.ICE), 3);
        helper.assertTrue(top(helper, MIDDLE, MIDDLE) <= 0, "the sinkhole drops through the floor of stone");
        helper.assertBlockState(new BlockPos(MIDDLE, TOP, MIDDLE), BlockState::isAir, () -> "and is open above");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void saltFlat_levelsTheLandToSalt(final GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(MIDDLE + 1, TOP + 1, MIDDLE), Blocks.STONE);
        paint(helper, config(LandformKind.SALT_FLAT, 0.5, Blocks.STONE, Blocks.STONE, Blocks.WHITE_CONCRETE_POWDER,
                Blocks.CALCITE), 8);
        helper.assertTrue(top(helper, MIDDLE + 1, MIDDLE) == TOP, "the knoll on the flat is levelled");
        helper.assertBlockState(new BlockPos(MIDDLE, TOP, MIDDLE),
                state -> state.is(Blocks.WHITE_CONCRETE_POWDER) || state.is(Blocks.CALCITE),
                () -> "its top is salt, or the cracks in it");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void lake_fillsItsBowlWithItsLiquid(final GameTestHelper helper) {
        floor(helper);
        paint(helper, lake(WATER_FREEZES, WATER_BOILS), 4);
        helper.assertBlockState(new BlockPos(MIDDLE, TOP - 1, MIDDLE), state -> state.is(Blocks.WATER),
                () -> "the lake's water stands a block under the ground");
        helper.assertBlockState(new BlockPos(MIDDLE, 0, MIDDLE), state -> state.is(Blocks.CLAY),
                () -> "on a bed of its surface");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void lake_freezesWhereItIsCold(final GameTestHelper helper) {
        floor(helper);
        paint(helper, lake(ABOVE_ANY_AIR, ABOVE_ANY_AIR + 1), 4);
        helper.assertBlockState(new BlockPos(MIDDLE, TOP - 1, MIDDLE), state -> state.is(Blocks.ICE),
                () -> "a lake colder than its liquid freezes is frozen on top");
        helper.assertBlockState(new BlockPos(MIDDLE, TOP - 2, MIDDLE), state -> state.is(Blocks.WATER),
                () -> "over its liquid");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void lake_boilsAwayWhereItIsHot(final GameTestHelper helper) {
        floor(helper);
        paint(helper, lake(BELOW_ANY_AIR - 1, BELOW_ANY_AIR), 4);
        helper.assertTrue(count(helper, Blocks.WATER) == 0, "a lake hotter than its liquid boils holds none of it");
        helper.assertTrue(top(helper, MIDDLE, MIDDLE) < TOP, "its bowl is left dry");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void sea_freezesWhereColdAndBoilsAwayWhereHot(final GameTestHelper helper) {
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
                helper.setBlock(new BlockPos(x, TOP, z), Blocks.WATER);
            }
        }
        settle(helper, new SeaConfig(state(Blocks.WATER), state(Blocks.PACKED_ICE), ABOVE_ANY_AIR,
                ABOVE_ANY_AIR + 1));
        helper.assertBlockState(new BlockPos(MIDDLE, TOP, MIDDLE), state -> state.is(Blocks.PACKED_ICE),
                () -> "a sea colder than its liquid freezes is frozen on top");
        helper.assertBlockState(new BlockPos(MIDDLE, 2, MIDDLE), state -> state.is(Blocks.WATER),
                () -> "over its water");
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                helper.setBlock(new BlockPos(x, TOP, z), Blocks.WATER);
            }
        }
        settle(helper, new SeaConfig(state(Blocks.WATER), state(Blocks.PACKED_ICE), BELOW_ANY_AIR - 1,
                BELOW_ANY_AIR));
        helper.assertTrue(count(helper, Blocks.WATER) == 0, "a sea hotter than its liquid boils is gone");
        helper.assertBlockState(new BlockPos(MIDDLE, 1, MIDDLE), state -> state.is(Blocks.STONE),
                () -> "down to its floor");
        helper.succeed();
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void strata_layTheirRockByDepth(final GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(MIDDLE + 1, TOP, MIDDLE), Blocks.DIRT);
        final StrataConfig config = new StrataConfig(List.of(List.of(new StrataConfig.Layer(state(Blocks.CALCITE), 1),
                new StrataConfig.Layer(state(Blocks.TUFF), 1))), 1024, 0, 0, BlockTags.BASE_STONE_OVERWORLD);
        final BlockPos low = helper.absolutePos(BlockPos.ZERO);
        final BlockPos high = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        StrataFeature.lay(helper.getLevel(), config, Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()),
                Math.max(low.getZ(), high.getZ()), Heightmap.Types.OCEAN_FLOOR);
        helper.assertBlockState(new BlockPos(MIDDLE, TOP, MIDDLE), state -> state.is(Blocks.CALCITE),
                () -> "the top layer is laid at the top of the ground");
        helper.assertBlockState(new BlockPos(MIDDLE, TOP - 1, MIDDLE), state -> state.is(Blocks.TUFF),
                () -> "the next under it");
        helper.assertBlockState(new BlockPos(MIDDLE, TOP - 2, MIDDLE), state -> state.is(Blocks.STONE),
                () -> "and the rock below the last layer is left as it was");
        helper.assertBlockState(new BlockPos(MIDDLE + 1, TOP, MIDDLE), state -> state.is(Blocks.DIRT),
                () -> "what is not the rock the layers replace is left alone");
        helper.succeed();
    }

    /* Lays the arena's floor of stone, its top at y 3. */
    private static void floor(final GameTestHelper helper) {
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                for (int y = 1; y <= TOP; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
    }

    /* Paints a landform of {@code radius} in the arena's middle, over the whole arena. */
    private static void paint(final GameTestHelper helper, final LandformConfig config, final int radius) {
        final BlockPos middle = helper.absolutePos(new BlockPos(MIDDLE, TOP, MIDDLE));
        final BlockPos low = helper.absolutePos(BlockPos.ZERO);
        final BlockPos high = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        LandformPainter.paint(helper.getLevel(), config, new LandformField.Instance(middle.getX(), middle.getZ(),
                        radius, 0, 1L), middle.getY(), Heightmap.Types.OCEAN_FLOOR, Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()),
                Math.max(low.getZ(), high.getZ()));
    }

    private static void settle(final GameTestHelper helper, final SeaConfig config) {
        final BlockPos low = helper.absolutePos(BlockPos.ZERO);
        final BlockPos high = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        SeaFeature.settle(helper.getLevel(), config, Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()),
                Math.max(low.getZ(), high.getZ()), Heightmap.Types.WORLD_SURFACE);
    }

    /* The highest solid block of a column of the arena, relative; -1 when the column is empty. */
    private static int top(final GameTestHelper helper, final int x, final int z) {
        for (int y = TOP + 2; y >= 0; y--) {
            final BlockState state = helper.getBlockState(new BlockPos(x, y, z));
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return y;
            }
        }
        return -1;
    }

    /* The highest block of anything, a liquid too, of a column of the arena, relative. */
    private static int topOfAnything(final GameTestHelper helper, final int x, final int z) {
        for (int y = TOP + 2; y >= 0; y--) {
            if (!helper.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return y;
            }
        }
        return -1;
    }

    private static int count(final GameTestHelper helper, final Block block) {
        int found = 0;
        for (int x = 0; x <= SIDE; x++) {
            for (int y = 0; y <= TOP + 2; y++) {
                for (int z = 0; z <= SIDE; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) {
                        found++;
                    }
                }
            }
        }
        return found;
    }

    private static LandformConfig config(final LandformKind kind, final double height, final Block surface,
                                         final Block fill, final Block special, final Block scatter) {
        return new LandformConfig(kind, 64, 1, 4, 4, 0, height, state(surface), state(fill), state(special),
                state(scatter), List.of(), WATER_FREEZES, WATER_BOILS, Optional.empty());
    }

    private static LandformConfig lake(final double freezes, final double boils) {
        return new LandformConfig(LandformKind.LAKE, 64, 1, 4, 4, 0, 0.5, state(Blocks.CLAY), state(Blocks.STONE),
                state(Blocks.WATER), state(Blocks.ICE), List.of(), freezes, boils, Optional.empty());
    }

    private static BlockState state(final Block block) {
        return block.defaultBlockState();
    }
}
