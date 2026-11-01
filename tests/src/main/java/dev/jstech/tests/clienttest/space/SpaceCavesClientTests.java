/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.worldgen.landform.LandformConfig;
import dev.jstech.core.worldgen.landform.LandformField;
import dev.jstech.core.worldgen.landform.Landforms;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.worldgen.MarsWorld;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * The caves of the Moon and Mars on a player's game, each shown as its mock drew it, at noon where it lies: inside a
 * lava tube looking back at its skylight and the rubble under it, inside a cave, before a cave's mouth in a slope,
 * over a pit, before a grotto in a canyon's wall, and inside a tunnel of the caps' ice looking out at its mouth.
 */
public final class SpaceCavesClientTests {

    /* The real places the caves are looked for near, by longitude and latitude. */
    private static final double[] MARIUS_HILLS = {-56.8, 14.1};
    private static final double[] TRANQUILLITATIS_PIT = {33.2, 8.3};
    private static final double[] DESCARTES = {15.5, -9.0};
    private static final double[] ARSIA_MONS = {-120.5, -9.0};
    private static final double[] VALLES = {-75, -10};
    private static final double[] NORTH_CAP = {0, 86};
    private static final double[] JEZERO = {77.5, 18.4};
    /* Noon on the middle meridian and a day, in ticks; a whole turn, in degrees. */
    private static final long NOON = 6_000L;
    private static final long DAY = 24_000L;
    private static final double FULL_TURN = 360;
    /* How many of a landform's cells round a place it is looked for in, and how many are tried for a mouth. */
    private static final int CELLS = 8;
    private static final int TRIES = 6;
    /* How far along a tube from its middle skylight the player stands, and how far out before a mouth or a pit. */
    private static final int ALONG_TUBE = 26;
    private static final int BEFORE_MOUTH = 12;
    /* How high over the ground the player hovers before a mouth, and its eyes over its feet. */
    private static final double HOVER = 1;
    private static final double EYE = 1.62;
    private static final int INSIDE_MOUTH = 6;
    private static final int BEFORE_PIT = 5;
    private static final int OVER_PIT = 4;
    /* How much roof stands over a mouth for it to be one in a slope or a wall, not a hole in a plain. */
    private static final int ROOFED = 3;
    /* How deep under its column's top a mouth is looked for. */
    private static final int MOUTH_DEPTH = 48;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    private static final int SEEING = 20 * 60 * 5;
    /* The sky's light on the rubble under a skylight, at the least. */
    private static final int LIT = 14;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private SpaceCavesClientTests() {
    }

    @ClientTest(timeoutTicks = 12000)
    public static void caves_standAsTheirMocksDrewThem(final ClientTestContext ctx) {
        final boolean[] daylight = new boolean[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight[0], ctx.server());
            final ServerPlayer player = ctx.serverPlayer();
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.removeEffect(MobEffects.NIGHT_VISION);
            final BlockPos origin = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F,
                    0F);
        }));
        ctx.thenServer(0, level -> {
                    final GameRules.BooleanValue rule = level.getServer().getGameRules()
                            .getRule(GameRules.RULE_DAYLIGHT);
                    daylight[0] = rule.get();
                    rule.set(false, level.getServer());
                    inTube(ctx, world(ctx, MoonWorld.world()), MoonWorld.LAVA_TUBES, MARIUS_HILLS);
                })
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.dimension().equals(MoonWorld.world().dimension()), ARRIVE,
                        "the player's game to reach the Moon")
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "moon_lava_tube_inside")
                .thenServer(0, level -> inCave(ctx, world(ctx, MoonWorld.world()), MoonWorld.CAVES, DESCARTES))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "moon_cave_inside")
                .thenServer(0, level -> beforeMouth(ctx, world(ctx, MoonWorld.world()), MoonWorld.CAVES, DESCARTES))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "moon_cave_mouth")
                .thenServer(0, level -> overPit(ctx, world(ctx, MoonWorld.world()), MoonWorld.MARIA_PITS,
                        TRANQUILLITATIS_PIT))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "moon_maria_pit")
                .thenServer(0, level -> inTube(ctx, world(ctx, MarsWorld.world()), MarsWorld.LAVA_TUBES, ARSIA_MONS))
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.dimension().equals(MarsWorld.world().dimension()), ARRIVE,
                        "the player's game to reach Mars")
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "mars_lava_tube_inside")
                .thenServer(0, level -> beforeMouth(ctx, world(ctx, MarsWorld.world()), MarsWorld.CANYON_GROTTOS,
                        VALLES))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "mars_canyon_grotto")
                .thenServer(0, level -> insideMouth(ctx, world(ctx, MarsWorld.world()), MarsWorld.ICE_CAVES,
                        NORTH_CAP))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "mars_ice_cave_inside")
                .thenServer(0, level -> beforeMouth(ctx, world(ctx, MarsWorld.world()), MarsWorld.CAVES, JEZERO))
                .thenAwaitLandDrawn(ARRIVE)
                .thenScreenshot(SETTLE, "mars_cave_mouth");
    }

    /* Stands the player on the floor of the tube of {@code spec} nearest {@code at}, along it, facing its skylight. */
    private static void inTube(final ClientTestContext ctx, final ServerLevel world, final Landforms.LandformSpec spec,
                               final double[] at) {
        final LandformField.Instance one = landforms(world, spec, at).get(0);
        final int x = (int) Math.round(one.x() + ALONG_TUBE * Math.cos(one.turn()));
        final int z = (int) Math.round(one.z() + ALONG_TUBE * Math.sin(one.turn()));
        final int floor = hollowFloor(world, x, z);
        ctx.assertTrue(world.getBlockState(new BlockPos(x, floor, z)).is(Blocks.CAVE_AIR), spec.id() + ": the player"
                + " stands in the tube at " + x + ", " + floor + ", " + z + "; its middle " + one + ", the column "
                + column(world, x, z) + ", the middle's " + column(world, one.x(), one.z()));
        final int open = free(world, Heightmap.Types.WORLD_SURFACE, one.x(), one.z());
        final BlockPos heap = new BlockPos(one.x(), open - 1, one.z());
        ctx.assertTrue(world.getBlockState(heap).is(spec.scatter().get()), spec.id() + ": the rubble lies under the"
                + " skylight at " + heap + ", not " + world.getBlockState(heap));
        ctx.assertTrue(world.getBrightness(LightLayer.SKY, heap.above()) >= LIT, spec.id() + ": the sky lights the"
                + " rubble through the skylight: " + world.getBrightness(LightLayer.SKY, heap.above()));
        arrive(ctx, world, at, x + 0.5, floor, z + 0.5, (float) Math.toDegrees(one.turn()) + 90F, -10F, false);
    }

    /* Stands the player on the chamber's floor of the cave of {@code spec} nearest {@code at}, seeing in the dark. */
    private static void inCave(final ClientTestContext ctx, final ServerLevel world, final Landforms.LandformSpec spec,
                               final double[] at) {
        final LandformField.Instance one = landforms(world, spec, at).get(0);
        final int floor = free(world, Heightmap.Types.WORLD_SURFACE, one.x(), one.z());
        arrive(ctx, world, at, one.x() + 0.5, floor, one.z() + 0.5, (float) Math.toDegrees(one.turn()), 0F, true);
    }

    /* Stands the player before a mouth in a slope or a wall of a cave of {@code spec} near {@code at}, facing it. */
    private static void beforeMouth(final ClientTestContext ctx, final ServerLevel world,
                                    final Landforms.LandformSpec spec, final double[] at) {
        final Optional<BlockPos[]> found = mouth(world, spec, at);
        if (found.isEmpty()) {
            overCrack(ctx, world, spec, at);
            return;
        }
        final BlockPos[] mouth = found.get();
        final int dx = mouth[1].getX() - mouth[0].getX();
        final int dz = mouth[1].getZ() - mouth[0].getZ();
        BlockPos stand = mouth[1];
        for (int step = 1; step <= BEFORE_MOUTH; step++) {
            final BlockPos next = mouth[1].offset(dx * step, 0, dz * step);
            if (!world.getBlockState(next).is(Blocks.AIR) || !world.getBlockState(next.above()).is(Blocks.AIR)) {
                break;
            }
            stand = next;
        }
        while (world.getBlockState(stand.below()).is(Blocks.AIR) && stand.getY() > world.getMinBuildHeight()) {
            stand = stand.below();
        }
        final double feet = stand.getY() + HOVER;
        final double far = Math.hypot(stand.getX() - mouth[0].getX(), stand.getZ() - mouth[0].getZ());
        final float pitch = (float) Math.toDegrees(Math.atan2(feet + EYE - mouth[0].getY() - 0.5, Math.max(1, far)));
        arrive(ctx, world, at, stand.getX() + 0.5, feet, stand.getZ() + 0.5, facing(-dx, -dz), pitch, false);
    }

    /* Stands the player south of the crack over the chamber of the cave of {@code spec} nearest {@code at}. */
    private static void overCrack(final ClientTestContext ctx, final ServerLevel world,
                                  final Landforms.LandformSpec spec, final double[] at) {
        final LandformField.Instance one = landforms(world, spec, at).get(0);
        final int z = one.z() + BEFORE_PIT * 2;
        final int ground = free(world, Heightmap.Types.MOTION_BLOCKING, one.x(), z);
        arrive(ctx, world, at, one.x() + 0.5, ground + OVER_PIT, z + 0.5, facing(0, -1), 40F, false);
    }

    /*
     * Stands the player in a tunnel of a cave of {@code spec} near {@code at}, just inside its mouth, facing it; in
     * its chamber when none of the caves near has a mouth in a slope or a wall.
     */
    private static void insideMouth(final ClientTestContext ctx, final ServerLevel world,
                                    final Landforms.LandformSpec spec, final double[] at) {
        final Optional<BlockPos[]> found = mouth(world, spec, at);
        if (found.isEmpty()) {
            final LandformField.Instance one = landforms(world, spec, at).get(0);
            arrive(ctx, world, at, one.x() + 0.5, free(world, Heightmap.Types.WORLD_SURFACE, one.x(), one.z()),
                    one.z() + 0.5, (float) Math.toDegrees(one.turn()), 0F, false);
            return;
        }
        final BlockPos[] mouth = found.get();
        final int dx = mouth[1].getX() - mouth[0].getX();
        final int dz = mouth[1].getZ() - mouth[0].getZ();
        BlockPos stand = mouth[0];
        for (int step = 1; step <= INSIDE_MOUTH; step++) {
            final BlockPos next = mouth[0].offset(-dx * step, 0, -dz * step);
            if (!world.getBlockState(next).is(Blocks.CAVE_AIR)) {
                break;
            }
            stand = next;
        }
        while (world.getBlockState(stand.below()).is(Blocks.CAVE_AIR)) {
            stand = stand.below();
        }
        arrive(ctx, world, at, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, facing(dx, dz), 0F, false);
    }

    /* Stands the player south of the pit of {@code spec} nearest {@code at}, over the ground, looking down into it. */
    private static void overPit(final ClientTestContext ctx, final ServerLevel world, final Landforms.LandformSpec spec,
                                final double[] at) {
        final LandformField.Instance one = landforms(world, spec, at).get(0);
        final int z = one.z() + one.radius() + BEFORE_PIT;
        final int ground = free(world, Heightmap.Types.MOTION_BLOCKING, one.x(), z);
        arrive(ctx, world, at, one.x() + 0.5, ground + OVER_PIT, z + 0.5, facing(0, -1), 40F, false);
    }

    /*
     * Sets the hour to noon at the place {@code at}, the clock held still, and puts the player at {@code x, y, z} of
     * {@code world}, flying, with {@code yaw} and {@code pitch}, seeing in the dark if {@code seeing}.
     */
    private static void arrive(final ClientTestContext ctx, final ServerLevel world, final double[] at, final double x,
                               final double y, final double z, final float yaw, final float pitch,
                               final boolean seeing) {
        world.setDayTime(Math.floorMod(NOON - Math.round(at[0] / FULL_TURN * DAY), DAY));
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        if (seeing) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, SEEING, 0, false, false));
        } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
        player.teleportTo(world, x, y, z, yaw, pitch);
    }

    /*
     * A mouth of a cave of {@code spec} near {@code at} in a slope or a wall: a block of cave air with a roof over it
     * beside the world's open air, and that block of open air, from the cave nearest the place that has one.
     */
    private static Optional<BlockPos[]> mouth(final ServerLevel world, final Landforms.LandformSpec spec,
                                              final double[] at) {
        for (final LandformField.Instance one : landforms(world, spec, at)) {
            final Optional<BlockPos[]> found = mouthOf(world, one);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /* The mouth with the most roof over it in the chunks a cave reaches, if it has one. */
    private static Optional<BlockPos[]> mouthOf(final ServerLevel world, final LandformField.Instance one) {
        final int reach = SectionPos.blockToSectionCoord(one.radius()) + 1;
        final int middleX = SectionPos.blockToSectionCoord(one.x());
        final int middleZ = SectionPos.blockToSectionCoord(one.z());
        BlockPos[] best = null;
        int bestRoof = ROOFED - 1;
        for (int cx = middleX - reach; cx <= middleX + reach; cx++) {
            for (int cz = middleZ - reach; cz <= middleZ + reach; cz++) {
                final LevelChunk chunk = world.getChunk(cx, cz);
                for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                    for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                        final int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                        for (int y = top - MOUTH_DEPTH; y < top - bestRoof; y++) {
                            final BlockPos here = new BlockPos(x, y, z);
                            if (!world.getBlockState(here).is(Blocks.CAVE_AIR)) {
                                continue;
                            }
                            for (final int[] side : SIDES) {
                                final BlockPos out = here.offset(side[0], 0, side[1]);
                                if (world.getBlockState(out).is(Blocks.AIR)
                                        && free(world, Heightmap.Types.WORLD_SURFACE, out.getX(), out.getZ()) <= y) {
                                    best = new BlockPos[] {here, out};
                                    bestRoof = top - y;
                                }
                            }
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /* The lowest block of the hollow found first going down a column from its top: where one stands in it. */
    private static int hollowFloor(final ServerLevel world, final int x, final int z) {
        int y = free(world, Heightmap.Types.WORLD_SURFACE, x, z);
        while (y > world.getMinBuildHeight() && !world.getBlockState(new BlockPos(x, y, z)).is(Blocks.CAVE_AIR)) {
            y--;
        }
        while (y > world.getMinBuildHeight() && world.getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
            y--;
        }
        return y;
    }

    /* The blocks of a column from its top down, run by run, for a failure to say what stands there. */
    private static String column(final ServerLevel world, final int x, final int z) {
        final StringBuilder out = new StringBuilder();
        final int top = free(world, Heightmap.Types.WORLD_SURFACE, x, z);
        String last = "";
        for (int y = top; y > top - MOUTH_DEPTH; y--) {
            final String name = world.getBlockState(new BlockPos(x, y, z)).getBlock().getDescriptionId();
            if (!name.equals(last)) {
                out.append(y).append('=').append(name.substring(name.lastIndexOf('.') + 1)).append(' ');
                last = name;
            }
        }
        return out.toString();
    }

    /*
     * The first free block over a column by {@code type}, its chunk made first: the level's own reading answers the
     * world's floor for a chunk not loaded yet.
     */
    private static int free(final ServerLevel world, final Heightmap.Types type, final int x, final int z) {
        return world.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(type,
                x & 15, z & 15) + 1;
    }

    /* The yaw of a player facing the way {@code dx, dz}. */
    private static float facing(final int dx, final int dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /*
     * The landforms of {@code spec} round the place {@code at}, nearest first, that the world lays: their middles in
     * the biomes their feature keeps to, read at the ground as the feature reads them, and in its map's region.
     */
    private static List<LandformField.Instance> landforms(final ServerLevel world, final Landforms.LandformSpec spec,
                                                          final double[] at) {
        final LandformConfig config = (LandformConfig) world.registryAccess().registryOrThrow(
                Registries.CONFIGURED_FEATURE).getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                spec.id())).config();
        final DimensionBorders.Bounds bounds = DimensionBorders.inForce(world.dimension()).orElseThrow();
        final double perDegree = (bounds.maxX() - bounds.minX()) / FULL_TURN;
        final int px = (int) Math.round(at[0] * perDegree);
        final int pz = (int) Math.round(-at[1] * perDegree);
        final int reach = spec.cell() * CELLS;
        final ChunkGenerator generator = world.getChunkSource().getGenerator();
        final RandomState random = world.getChunkSource().randomState();
        final List<LandformField.Instance> found = config.field().reaching(world.getSeed(), px - reach, pz - reach,
                        px + reach, pz + reach).stream()
                .sorted(Comparator.comparingDouble(each -> each.distance(px, pz)))
                .filter(each -> {
                    final int base = generator.getBaseHeight(each.x(), each.z(), Heightmap.Types.OCEAN_FLOOR_WG,
                            world, random) - 1;
                    return config.biomes().map(biomes -> biomes.contains(generator.getBiomeSource().getNoiseBiome(
                            QuartPos.fromBlock(each.x()), QuartPos.fromBlock(base), QuartPos.fromBlock(each.z()),
                            random.sampler()))).orElse(true) && config.mayLieAt(each.x(), each.z());
                })
                .limit(TRIES)
                .toList();
        if (found.isEmpty()) {
            throw new IllegalStateException("no " + spec.id() + " near " + px + ", " + pz);
        }
        return found;
    }

    private static ServerLevel world(final ClientTestContext ctx, final SpaceWorld world) {
        final ResourceKey<Level> key = world.dimension();
        return RuntimeDimensions.getOrCreate(ctx.server(), key, key.location());
    }
}
