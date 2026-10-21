/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.connect.ConnectedPacks;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.core.stepaside.StepAside;
import dev.jstech.testkit.gametest.ScenarioBuilder;
import dev.jstech.tests.TestConnected;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The connected textures of resource packs as the Core draws them, from the test mod's own resources as a pack would
 * write them: OptiFine's and Continuity's full method picking the tile of how each face of a wall joins, their compact
 * method drawing a quarter at a time, both by block and by texture; CTM's metadata joining a quarter at a time from its
 * companion; CTM's override in a model drawing a pattern over its faces of one tint index; microblocks of a material
 * joining their like in the spaces beside them; and the Core keeping the packs' textures to itself while neither
 * Continuity nor CTM is installed.
 */
public final class ConnectedPacksClientTests {

    private static final BlockPos STAND = new BlockPos(0, 3, -3);
    /* A wall three by three facing the player, its south faces looked at from the north... */
    private static final int WALL_Z = 2;
    private static final int WAIT = 200;
    private static final long SEED = 42L;

    private ConnectedPacksClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void fullMethod_picksTheTileOfHowEachFaceJoins(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    wall(ctx, level, TestConnected.FULL.get());
                })
                .thenWaitUntil(() -> placed(ctx, new BlockPos(1, 4, WALL_Z), TestConnected.FULL.get()), WAIT,
                        "the wall to reach the player")
                .thenAssert(0, () -> sprites(ctx, new BlockPos(0, 3, WALL_Z), Direction.NORTH)
                        .equals(List.of(TestConnected.fullTile(26))), "the middle of the wall joins all round")
                .thenAssert(0, () -> sprites(ctx, new BlockPos(0, 4, WALL_Z), Direction.NORTH)
                        .equals(List.of(TestConnected.fullTile(14))),
                        "the top middle joins below, both sides and both corners below")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 3, WALL_Z)))))
                .thenScreenshot(10, "full-method-wall")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void compactMethod_drawsAQuarterAtATimeByTexture(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 3, WALL_Z)),
                            TestConnected.COMPACT.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(1, 3, WALL_Z)),
                            TestConnected.COMPACT.get().defaultBlockState());
                })
                .thenWaitUntil(() -> placed(ctx, new BlockPos(1, 3, WALL_Z), TestConnected.COMPACT.get()), WAIT,
                        "the pair to reach the player")
                .thenAssert(0, () -> {
                    // Seen from the north, east is to the left: the block's right is the one at x 0.
                    final List<String> faces = sprites(ctx, new BlockPos(1, 3, WALL_Z), Direction.NORTH);
                    return faces.equals(List.of(TestConnected.COMPACT_TILES[0], TestConnected.COMPACT_TILES[3],
                            TestConnected.COMPACT_TILES[3], TestConnected.COMPACT_TILES[0]));
                }, "its left quarters alone, its right quarters joined sideways")
                .thenScreenshot(4, "compact-method-pair")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void ctmMetadata_joinsAQuarterAtATimeFromItsCompanion(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    wall(ctx, level, TestConnected.FRAME.get());
                })
                .thenWaitUntil(() -> placed(ctx, new BlockPos(1, 4, WALL_Z), TestConnected.FRAME.get()), WAIT,
                        "the wall to reach the player")
                .thenAssert(0, () -> sprites(ctx, new BlockPos(0, 3, WALL_Z), Direction.NORTH).equals(List.of(
                        "jstests:block/test_ctm_frame-ctm", "jstests:block/test_ctm_frame-ctm",
                        "jstests:block/test_ctm_frame-ctm", "jstests:block/test_ctm_frame-ctm")),
                        "the middle draws every quarter from the companion")
                .thenAssert(0, () -> sprites(ctx, new BlockPos(1, 4, WALL_Z), Direction.NORTH)
                        .contains("jstests:block/test_ctm_frame"), "a corner keeps its own quarter where it is alone")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 3, WALL_Z)))))
                .thenScreenshot(10, "ctm-metadata-wall")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void modelOverrides_drawTheFacesOfTheirTintIndex(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    wall(ctx, level, TestConnected.OVERRIDE.get());
                })
                .thenWaitUntil(() -> placed(ctx, new BlockPos(1, 4, WALL_Z), TestConnected.OVERRIDE.get()), WAIT,
                        "the wall to reach the player")
                .thenAssert(0, () -> {
                    final List<BakedQuad> quads = quads(ctx, new BlockPos(0, 3, WALL_Z), Direction.NORTH);
                    if (quads.size() != 1) {
                        return false;
                    }
                    // A pattern two by two lays a quarter of its texture over each face.
                    final BakedQuad quad = quads.getFirst();
                    final float span = quad.getSprite().getU1() - quad.getSprite().getU0();
                    return Math.abs(uSpan(quad) - span / 2) < span * 0.01F;
                }, "each face draws a quarter of the pattern")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 3, WALL_Z)))))
                .thenScreenshot(10, "model-override-pattern")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void microblocks_joinTheirLikeInTheSpacesBeside(final ClientTestContext ctx) {
        final Microblock cover = new Microblock(new Microblock.MicroblockPiece(MicroShape.FACE, MicroShape.COVER,
                TestConnected.COMPACT.get()), SpaceSlots.face(Direction.NORTH.get3DDataValue()), SpaceSlots.Y);
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    final ScenarioBuilder build = ScenarioBuilder.at(level, ctx.abs(BlockPos.ZERO));
                    build.placeMicroblock(new BlockPos(0, 3, WALL_Z), cover);
                    build.placeMicroblock(new BlockPos(1, 3, WALL_Z), cover);
                })
                .thenWaitUntil(() -> ctx.mc().level != null && !ctx.mc().level.getBlockState(ctx.abs(
                        new BlockPos(1, 3, WALL_Z))).isAir(), WAIT, "the spaces to reach the player")
                .thenWaitUntil(() -> withData(ctx, new BlockPos(1, 3, WALL_Z), Direction.NORTH).stream()
                        .map(quad -> quad.getSprite().contents().name().toString())
                        .anyMatch(TestConnected.COMPACT_TILES[3]::equals), WAIT,
                        "the cover to join the cover beside it, a quarter joined sideways")
                .thenScreenshot(10, "microblocks-joined")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 200)
    public static void stepAside_keepsThePacksWhileNeitherModIsInstalled(final ClientTestContext ctx) {
        ctx.thenAssert(0, () -> StepAside.CONNECTED_TEXTURES.installed().isEmpty(),
                        "neither Continuity nor CTM is installed")
                .thenAssert(0, () -> !StepAside.CONNECTED_TEXTURES.steps(), "so the Core does not step aside")
                .thenAssert(0, () -> StepAside.CONNECTED_TEXTURES.mods().equals(List.of("continuity", "ctm")),
                        "the mods it would step aside for")
                .thenAssert(0, ConnectedPacks::any, "and draws the packs' connected textures");
    }

    /* The sprites of the quads the block's face at {@code at} is drawn with, as the world's mesh would draw them. */
    private static List<String> sprites(final ClientTestContext ctx, final BlockPos at, final Direction side) {
        return quads(ctx, at, side).stream().map(quad -> quad.getSprite().contents().name().toString()).toList();
    }

    private static List<BakedQuad> quads(final ClientTestContext ctx, final BlockPos at, final Direction side) {
        final BlockPos pos = ctx.abs(at);
        final BlockState state = ctx.mc().level.getBlockState(pos);
        final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
        final ModelData data = model.getModelData(ctx.mc().level, pos, state, ModelData.EMPTY);
        return model.getQuads(state, side, RandomSource.create(SEED), data, null);
    }

    /* The same for a block with data of its own, as a space of microblocks has: its block entity's data first. */
    private static List<BakedQuad> withData(final ClientTestContext ctx, final BlockPos at, final Direction side) {
        if (ctx.mc().level == null) {
            return List.of();
        }
        final BlockPos pos = ctx.abs(at);
        final BlockState state = ctx.mc().level.getBlockState(pos);
        final BakedModel model = ctx.mc().getBlockRenderer().getBlockModel(state);
        final ModelData data = model.getModelData(ctx.mc().level, pos, state, ctx.mc().level.getModelData(pos));
        return model.getQuads(state, side, RandomSource.create(SEED), data, null);
    }

    private static float uSpan(final BakedQuad quad) {
        final int[] data = quad.getVertices();
        float low = Float.MAX_VALUE;
        float high = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            final float u = Float.intBitsToFloat(data[i * 8 + 4]);
            low = Math.min(low, u);
            high = Math.max(high, u);
        }
        return high - low;
    }

    private static boolean placed(final ClientTestContext ctx, final BlockPos at, final Block block) {
        return ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(at)).is(block);
    }

    /* A wall of {@code block} three by three, from x -1 to 1 and y 2 to 4. */
    private static void wall(final ClientTestContext ctx, final ServerLevel level, final Block block) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 2; y <= 4; y++) {
                level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, WALL_Z)), block.defaultBlockState());
            }
        }
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -2; x <= 2; x++) {
            for (int y = 2; y <= 5; y++) {
                for (int z = 0; z <= 3; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }
}
