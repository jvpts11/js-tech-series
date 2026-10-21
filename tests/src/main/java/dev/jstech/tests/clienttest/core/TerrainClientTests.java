/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.worldgen.landform.LandformField;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.vein.VeinField;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestWorldGen;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.LandSurvey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * How a land made by the Core's terrain, biomes and landforms looks to a player: a crater on the steppe, dug into the
 * sand with its gravel floor, its rim and its ejecta, the land round it seen from high above, and a lake on the
 * frost, frozen over, under the snow of the frost's own surface; a vein of iron and tin cut through, dense at its
 * middle and thinning outwards; and a reservoir of brine cut through, its soaked rock under its cap. The player is
 * taken to a copy of the test land and back.
 */
public final class TerrainClientTests {

    private static final ResourceKey<Level> LAND = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "land_view"));
    private static final int CELLS = 64;
    private static final int ARRIVE = 600;
    /* Ticks after the land is drawn before the picture, for a frame of it to be shown. */
    private static final int SHOOT = 5;
    private static final long NOON = 6_000L;
    /* Where the player stands to see a landform: off its edge, above it, and clear of the ground under it. */
    private static final double OFF = 1.6;
    private static final double UP = 0.9;
    private static final int LEAST_UP = 8;
    private static final int CLEARANCE = 4;
    /* How high over a crater, and how far back, the land is seen whole. */
    private static final int HIGH = 48;
    /* How far the land is cut away beside a vein, and how far below its middle. */
    private static final int CUT = 20;
    private static final int RESERVOIR_CUT = 32;
    private static final int DEEP = 8;
    private static final int UPDATE = 2;

    private TerrainClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void testLand_showsACraterAndAFrozenLake(final ClientTestContext ctx) {
        final LandformField.Instance[] found = new LandformField.Instance[2];
        final BlockPos[] tops = new BlockPos[2];
        final Vec3[] stands = new Vec3[2];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final ServerPlayer player = ctx.serverPlayer();
            final BlockPos home = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), LAND);
        }));
        ctx.thenServer(0, level -> {
            final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), LAND,
                    TestWorldGen.TEST_LAND.id());
            level.getServer().overworld().setDayTime(NOON);
            final Registry<Biome> biomes = land.registryAccess().registryOrThrow(Registries.BIOME);
            final Holder<Biome> steppe = biomes.getHolderOrThrow(TestWorldGen.STEPPE.key());
            final Holder<Biome> frost = biomes.getHolderOrThrow(TestWorldGen.FROST.key());
            final List<Landforms.LandformSpec> all = List.of(TestWorldGen.CRATERS, TestWorldGen.LAKES);
            found[0] = LandSurvey.clear(land, TestWorldGen.CRATERS, steppe, all, CELLS, true);
            found[1] = LandSurvey.clear(land, TestWorldGen.LAKES, frost, all, CELLS, true);
            for (int i = 0; i < found.length; i++) {
                if (found[i] != null) {
                    tops[i] = top(land, found[i].x(), found[i].z());
                    stands[i] = stand(land, found[i], tops[i]);
                }
            }
            if (found[0] != null) {
                go(ctx, land, stands[0]);
            }
        })
                .thenAssert(1, () -> found[0] != null && found[1] != null,
                        "the test land holds a clear crater on the steppe and a clear lake on the frost")
                .thenWaitUntil(() -> arrived(ctx, stands[0], tops[0]), ARRIVE, "the player's game to reach the crater")
                .then(1, () -> ctx.lookAt(Vec3.atCenterOf(tops[0])))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "crater_on_the_steppe")
                .then(1, () -> expect(ctx, tops[0], "the crater's floor", Blocks.GRAVEL))
                .thenServer(1, level -> go(ctx, level.getServer().getLevel(LAND), Vec3.atCenterOf(tops[0])
                        .add(0, HIGH, HIGH)))
                .thenWaitUntil(() -> arrived(ctx, Vec3.atCenterOf(tops[0]).add(0, HIGH, HIGH), tops[0]), ARRIVE,
                        "the player's game to rise over the land")
                .then(1, () -> ctx.lookAt(Vec3.atCenterOf(tops[0])))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "the_land_from_above")
                .thenServer(1, level -> go(ctx, level.getServer().getLevel(LAND), stands[1]))
                .thenWaitUntil(() -> arrived(ctx, stands[1], tops[1]), ARRIVE, "the player's game to reach the lake")
                .then(1, () -> ctx.lookAt(Vec3.atCenterOf(tops[1])))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "frozen_lake_on_the_frost")
                .then(1, () -> expect(ctx, tops[1], "the frost's lake", Blocks.ICE, Blocks.PACKED_ICE));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void testLand_showsAVeinCutOpen(final ClientTestContext ctx) {
        final VeinField.Vein[] found = new VeinField.Vein[1];
        final Vec3[] stand = new Vec3[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final BlockPos home = ctx.origin().above(2);
            ctx.serverPlayer().teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(),
                    home.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), LAND);
        }));
        ctx.thenServer(0, level -> {
            final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), LAND,
                    TestWorldGen.TEST_LAND.id());
            level.getServer().overworld().setDayTime(NOON);
            found[0] = LandSurvey.vein(land, TestWorldGen.IRON_VEINS, CELLS);
            if (found[0] == null) {
                return;
            }
            stand[0] = cutOpen(ctx, land, new BlockPos(found[0].x(), found[0].y(), found[0].z()), CUT);
        })
                .thenAssert(1, () -> found[0] != null, "the test land holds a vein of its iron")
                .thenWaitUntil(() -> arrived(ctx, stand[0], new BlockPos(found[0].x(), found[0].y(), found[0].z())),
                        ARRIVE, "the player's game to reach the cut")
                .then(1, () -> ctx.lookAt(new Vec3(found[0].x(), found[0].y() + 0.5, found[0].z() + 0.5)))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "iron_vein_cut_open")
                .then(1, () -> {
                    final VeinField.Vein vein = found[0];
                    int ores = 0;
                    for (int z = vein.z() - 4; z <= vein.z() + 4; z++) {
                        for (int y = vein.y() - 2; y <= vein.y() + 2; y++) {
                            final BlockState face = ctx.mc().level.getBlockState(new BlockPos(vein.x(), y, z));
                            ores += face.isAir() || face.is(Blocks.STONE) || face.is(Blocks.TUFF)
                                    || face.is(Blocks.CALCITE) ? 0 : 1;
                        }
                    }
                    if (ores == 0) {
                        throw new ClientTestFailure("the vein's cut face shows no ore on the player's game");
                    }
                });
    }

    @ClientTest(timeoutTicks = 2400)
    public static void testLand_showsAReservoirCutOpen(final ClientTestContext ctx) {
        final Reservoirs.Reservoir[] found = new Reservoirs.Reservoir[1];
        final Vec3[] stand = new Vec3[1];
        final BlockPos[] middle = new BlockPos[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            final BlockPos home = ctx.origin().above(2);
            ctx.serverPlayer().teleportTo(ctx.server().overworld(), home.getX() + 0.5, home.getY(),
                    home.getZ() + 0.5, 0F, 0F);
            RuntimeDimensions.remove(ctx.server(), LAND);
        }));
        ctx.thenServer(0, level -> {
            final ServerLevel land = RuntimeDimensions.getOrCreate(level.getServer(), LAND,
                    TestWorldGen.TEST_LAND.id());
            level.getServer().overworld().setDayTime(NOON);
            found[0] = LandSurvey.reservoir(land, TestWorldGen.BRINES, CELLS);
            if (found[0] != null) {
                middle[0] = new BlockPos(found[0].body().x(), found[0].body().y(), found[0].body().z());
                stand[0] = cutOpen(ctx, land, middle[0], RESERVOIR_CUT);
            }
        })
                .thenAssert(1, () -> found[0] != null, "the test land holds a reservoir of its brine")
                .thenWaitUntil(() -> arrived(ctx, stand[0], middle[0]), ARRIVE, "the player's game to reach the cut")
                .then(1, () -> ctx.lookAt(Vec3.atCenterOf(middle[0])))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SHOOT, "brine_reservoir_cut_open")
                .then(1, () -> {
                    expect(ctx, middle[0], "the reservoir's middle", Blocks.MUD);
                    expect(ctx, middle[0].above((int) Math.ceil(found[0].body().thickness()) + 1), "its cap",
                            Blocks.SMOOTH_BASALT);
                });
    }

    /*
     * Takes away the land east of {@code middle}, from below it up to the sky, so what lies there shows cut through,
     * and takes the player into the cut, looking west at it; where the player stands.
     */
    private static Vec3 cutOpen(final ClientTestContext ctx, final ServerLevel land, final BlockPos middle,
                                final int cut) {
        for (int x = middle.getX() + 1; x <= middle.getX() + cut; x++) {
            for (int z = middle.getZ() - cut; z <= middle.getZ() + cut; z++) {
                final int topY = top(land, x, z).getY();
                for (int y = middle.getY() - DEEP; y <= topY; y++) {
                    land.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), UPDATE);
                }
            }
        }
        final Vec3 stand = new Vec3(middle.getX() + cut - 2 + 0.5, middle.getY() + 2, middle.getZ() + 0.5);
        go(ctx, land, stand);
        return stand;
    }

    /* The highest block of a column of the land, making its chunk first: a chunk not made yet has no height. */
    private static BlockPos top(final ServerLevel land, final int x, final int z) {
        land.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, land.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
    }

    /* Off a landform's edge, above its middle, and above the ground where the player hangs. */
    private static Vec3 stand(final ServerLevel land, final LandformField.Instance one, final BlockPos top) {
        final int x = (int) Math.round(top.getX() + one.radius() * OFF);
        final int over = top(land, x, top.getZ()).getY() + CLEARANCE;
        final double y = Math.max(over, top.getY() + Math.max(LEAST_UP, one.radius() * UP));
        return new Vec3(x + 0.5, y, top.getZ() + 0.5);
    }

    private static void go(final ClientTestContext ctx, final ServerLevel land, final Vec3 at) {
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(land, at.x, at.y, at.z, 90F, 35F);
    }

    private static boolean arrived(final ClientTestContext ctx, final Vec3 at, final BlockPos top) {
        return ctx.mc().level != null && ctx.mc().level.dimension().equals(LAND) && ctx.player() != null
                && ctx.player().position().distanceTo(at) < 2.0
                && !ctx.mc().level.getChunk(top.getX() >> 4, top.getZ() >> 4).isEmpty();
    }

    private static void expect(final ClientTestContext ctx, final BlockPos at, final String what,
                               final Block... blocks) {
        final BlockState there = ctx.mc().level.getBlockState(at);
        for (final Block block : blocks) {
            if (there.is(block)) {
                return;
            }
        }
        throw new ClientTestFailure(what + " at " + at + " is " + there + " on the player's game");
    }
}
