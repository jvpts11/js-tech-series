/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.region.SpatialIndex;
import dev.jstech.core.worldgen.reservoir.ReservoirPainter;
import dev.jstech.core.worldgen.reservoir.ReservoirType;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.testkit.LandSurvey;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's reservoirs: one laid into a test's arena is a lens of soaked rock under a cap, flooding nothing; the kinds
 * travel through their files; and in a generated land, a reservoir lies where its field says, each block holds what
 * its kind says, a well drains it a block at a time, the block turns back into plain rock, and what was drawn is kept
 * in the dimension's data over areas for every mod to read.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ReservoirGameTests {

    private static final String ARENA = "empty";
    private static final int SIDE = 8;
    private static final int MIDDLE = 4;
    private static final int TOP = 5;
    private static final int ZONES_SEARCHED = 32;

    private ReservoirGameTests() {
    }

    @GameTest(template = ARENA, skyAccess = true)
    public static void reservoir_laysALensOfSoakedRockUnderACapAndFloodsNothing(final GameTestHelper helper) {
        for (int x = 0; x <= SIDE; x++) {
            for (int z = 0; z <= SIDE; z++) {
                for (int y = 1; y <= TOP; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                }
            }
        }
        helper.setBlock(new BlockPos(MIDDLE + 1, 2, MIDDLE), Blocks.AIR);
        helper.setBlock(new BlockPos(MIDDLE - 1, 2, MIDDLE), Blocks.WATER);
        final Reservoirs.Kind brine = Reservoirs.all().stream()
                .filter(kind -> kind.id().equals(TestWorldGen.BRINES)).findFirst().orElseThrow();
        final BlockPos middle = helper.absolutePos(new BlockPos(MIDDLE, 2, MIDDLE));
        final Reservoirs.Reservoir reservoir = new Reservoirs.Reservoir(brine, new VeinField.Vein(0, middle.getX(),
                middle.getY(), middle.getZ(), 4, 1.5, 1L));
        final BlockPos low = helper.absolutePos(BlockPos.ZERO);
        final BlockPos high = helper.absolutePos(new BlockPos(SIDE, 0, SIDE));
        final int soaked = ReservoirPainter.paint(helper.getLevel(), reservoir, Math.min(low.getX(), high.getX()),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()),
                Math.max(low.getZ(), high.getZ()));
        helper.assertTrue(soaked > 0, "the lens is laid");
        helper.assertBlockState(new BlockPos(MIDDLE, 2, MIDDLE), state -> state.is(Blocks.MUD),
                () -> "its middle is the soaked rock");
        helper.assertBlockState(new BlockPos(MIDDLE, 4, MIDDLE), state -> state.is(Blocks.SMOOTH_BASALT),
                () -> "under its cap rock");
        helper.assertBlockState(new BlockPos(MIDDLE + 1, 2, MIDDLE), state -> state.isAir(),
                () -> "a cave in it is left a cave");
        helper.assertBlockState(new BlockPos(MIDDLE - 1, 2, MIDDLE), state -> state.is(Blocks.WATER),
                () -> "and water left water");
        helper.assertBlockState(new BlockPos(0, 2, 0), state -> state.is(Blocks.STONE),
                () -> "the rock beyond its edge is left as it was");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void reservoirs_comeFromTheirFilesAndTravelThroughThem(final GameTestHelper helper) {
        helper.assertTrue(Reservoirs.FILES.get(TestWorldGen.BRINES).isPresent()
                        && Reservoirs.FILES.get(TestWorldGen.BRINES).get()
                        .equals(Reservoirs.declared().get(TestWorldGen.BRINES)),
                "the test brine is read from the file the data generation wrote, as declared");
        final ReservoirType type = Reservoirs.declared().get(TestWorldGen.BRINES);
        final JsonElement written = ReservoirType.CODEC.encodeStart(JsonOps.INSTANCE, type).getOrThrow();
        helper.assertTrue(ReservoirType.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow().equals(type),
                "a kind of reservoir reads back from its file");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void testLand_drainsAReservoirABlockAtATime(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ResourceKey<Level> copy = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "brine_" + UUID.randomUUID().toString()
                        .substring(0, 8)));
        final ServerLevel land = RuntimeDimensions.getOrCreate(server, copy, TestWorldGen.TEST_LAND.id());
        try {
            final Reservoirs.Reservoir reservoir = LandSurvey.reservoir(land, TestWorldGen.BRINES, ZONES_SEARCHED);
            helper.assertTrue(reservoir != null, "the test land holds a reservoir of its brine");
            final BlockPos middle = new BlockPos(reservoir.body().x(), reservoir.body().y(), reservoir.body().z());
            land.getChunk(middle.getX() >> 4, middle.getZ() >> 4);
            helper.assertTrue(land.getBlockState(middle).is(Blocks.MUD), "its middle is soaked rock in the land, not "
                    + land.getBlockState(middle));
            helper.assertTrue(land.getBlockState(middle.above((int) Math.ceil(reservoir.body().thickness()) + 1))
                    .is(Blocks.SMOOTH_BASALT), "under its cap");
            helper.assertTrue(reservoir.fluid() == Fluids.WATER, "it holds water");
            final Optional<Reservoirs.Held> full = Reservoirs.at(land, middle);
            helper.assertTrue(full.isPresent() && full.get().left() == 1000, "a block of it holds a bucket");
            helper.assertTrue(Reservoirs.drain(land, middle, 400, true) == 400 && Reservoirs.at(land, middle).get()
                    .left() == 1000, "drawing only to see takes nothing");
            helper.assertTrue(Reservoirs.drain(land, middle, 400, false) == 400, "a well draws what it asks");
            helper.assertTrue(Reservoirs.at(land, middle).get().left() == 600, "and the block holds the rest");
            final SpatialIndex.Entry<String, Reservoirs.Drawn> drawn = Reservoirs.DRAWN.get(land, reservoir.key());
            helper.assertTrue(drawn != null && drawn.value().total() == 400 && drawn.box().contains(middle.getX(),
                    middle.getY(), middle.getZ()), "what was drawn is kept over the reservoir's area");
            helper.assertTrue(Reservoirs.drain(land, middle, 1000, false) == 600,
                    "a block gives no more than it holds");
            helper.assertTrue(land.getBlockState(middle).is(Blocks.PACKED_MUD), "and turns back into plain rock, not "
                    + land.getBlockState(middle));
            helper.assertTrue(Reservoirs.at(land, middle).isEmpty() && Reservoirs.drain(land, middle, 100, false) == 0,
                    "which holds nothing");
            helper.assertTrue(Reservoirs.left(land, reservoir) == reservoir.held() - 1000,
                    "the reservoir holds less by what was drawn");
            final Optional<BlockPos> next = Reservoirs.nearestHolding(land, reservoir, middle);
            helper.assertTrue(next.isPresent() && !next.get().equals(middle) && next.get().distSqr(middle) <= 2,
                    "a well finds the next block beside the one it drained, not " + next);
            helper.assertTrue(Reservoirs.near(land, middle.east(40), 64).stream()
                    .anyMatch(near -> near.key().equals(reservoir.key())), "and every mod finds the reservoir near");
        } finally {
            RuntimeDimensions.remove(server, copy);
        }
        helper.succeed();
    }
}
