/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.gametest.ScenarioBuilder;
import dev.jstech.core.integration.emi.CoreEmiPlugin;
import dev.jstech.core.integration.jei.CoreJeiPlugin;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.tests.TestParts;
import dev.jstech.tests.TestToolkit;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * Block spaces of parts as a player meets them, to set beside what they should look like: microblocks of several
 * blocks side by side, glass over stone, a hollow piece, a post and a corner, and a facade and a cover on a cable; the
 * game's torch, lever and button hung beside a cable against a wall; a part's blade turning and a part declared the
 * Core's way; and the microblocks' rules in the recipe viewer.
 */
public final class MultipartClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos LAYERED = new BlockPos(-2, 2, 1);
    private static final BlockPos SIDE_BY_SIDE = new BlockPos(-1, 2, 1);
    private static final BlockPos HOLLOWED = new BlockPos(0, 2, 1);
    private static final BlockPos POSTED = new BlockPos(1, 2, 1);
    private static final BlockPos DRESSED = new BlockPos(2, 2, 1);
    /* A cable running east to west in front of a wall, the small blocks hung on the wall behind it. */
    private static final int WALL_Z = 3;
    private static final int RUN_Z = 2;
    private static final int WAIT = 200;
    private static final int SETTLE = 4;
    private static final int TURNING = 5;

    private MultipartClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void microblocks_drawAsTheBlocksTheyAreCutFrom(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    final ScenarioBuilder build = ScenarioBuilder.at(level, ctx.abs(BlockPos.ZERO));
                    build.placeMicroblock(LAYERED, face(MicroShape.SLAB, Blocks.STONE, Direction.DOWN));
                    build.placeMicroblock(LAYERED, face(MicroShape.SLAB, Blocks.GLASS, Direction.UP));
                    build.placeMicroblock(SIDE_BY_SIDE, face(MicroShape.PANEL, Blocks.OAK_PLANKS, Direction.NORTH));
                    build.placeMicroblock(SIDE_BY_SIDE, face(MicroShape.COVER, Blocks.BRICKS, Direction.DOWN));
                    build.placeMicroblock(HOLLOWED, new Microblock(new Microblock.MicroblockPiece(MicroShape.HOLLOW,
                            MicroShape.PANEL, Blocks.COPPER_BLOCK), slot(Direction.NORTH), SpaceSlots.Y));
                    build.placeMicroblock(POSTED, new Microblock(new Microblock.MicroblockPiece(MicroShape.POST,
                            MicroShape.PANEL, Blocks.SPRUCE_PLANKS), SpaceSlots.CENTRE, SpaceSlots.Y));
                    build.placeMicroblock(POSTED, new Microblock(new Microblock.MicroblockPiece(MicroShape.CORNER,
                            MicroShape.PANEL, Blocks.GOLD_BLOCK), SpaceSlots.corner(false, false, false),
                            SpaceSlots.Y));
                    build.setBlock(DRESSED, SharedEnergy.INSULATED_COPPER_CABLE);
                    build.placeMicroblock(DRESSED, new Microblock(new Microblock.MicroblockPiece(MicroShape.FACADE,
                            MicroShape.COVER, Blocks.STONE_BRICKS), slot(Direction.NORTH), SpaceSlots.Y));
                    build.placeMicroblock(DRESSED, face(MicroShape.COVER, Blocks.SMOOTH_STONE, Direction.DOWN));
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(POSTED))
                        .is(CoreSpaces.BLOCK.get()), WAIT, "the spaces to reach the player")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(HOLLOWED))))
                .thenScreenshot(20, "microblocks")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void smallBlocks_hangBesideACableAgainstAWall(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    final ScenarioBuilder build = ScenarioBuilder.at(level, ctx.abs(BlockPos.ZERO));
                    for (int x = -2; x <= 2; x++) {
                        build.setBlock(new BlockPos(x, 2, WALL_Z), Blocks.STONE_BRICKS);
                        build.setBlock(new BlockPos(x, 3, WALL_Z), Blocks.STONE_BRICKS);
                        build.setBlock(new BlockPos(x, 2, RUN_Z), SharedEnergy.INSULATED_COPPER_CABLE);
                    }
                    build.hangSmall(new BlockPos(-1, 2, RUN_Z), Blocks.WALL_TORCH.defaultBlockState()
                            .setValue(WallTorchBlock.FACING, Direction.NORTH));
                    build.hangSmall(new BlockPos(0, 2, RUN_Z), onWall(Blocks.LEVER).setValue(LeverBlock.POWERED,
                            true));
                    build.hangSmall(new BlockPos(1, 2, RUN_Z), onWall(Blocks.STONE_BUTTON));
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(
                        new BlockPos(1, 2, RUN_Z))).is(CoreCables.BLOCK.get()), WAIT, "the cable to reach the player")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 2, RUN_Z)))))
                .thenScreenshot(20, "small-blocks")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void parts_turnTheirBladesAndWearTheirPlates(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    final ScenarioBuilder build = ScenarioBuilder.at(level, ctx.abs(BlockPos.ZERO));
                    build.setBlock(HOLLOWED, SharedEnergy.INSULATED_COPPER_CABLE);
                    build.mountPart(HOLLOWED, Direction.NORTH, TestToolkit.FAN_PART.get());
                    build.setBlock(POSTED, SharedEnergy.INSULATED_COPPER_CABLE);
                    build.mountPart(POSTED, Direction.NORTH, TestParts.PLATE.get());
                })
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(POSTED))
                        .is(CoreCables.BLOCK.get()), WAIT, "the cables to reach the player")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(HOLLOWED))))
                .thenScreenshot(20, "fan-and-plate")
                .thenScreenshot(TURNING, "fan-turned")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void recipeViewer_showsTheMicroblocksRulesAsOneEntryEach(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded("jei") && !ModList.get().isLoaded("emi")) {
            // Without a viewer there is nothing to show: the test passes empty, as the viewer steps elsewhere do.
            ctx.then(0, () -> { });
            return;
        }
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        final ItemStack microblock = new ItemStack(CoreSpaces.MICROBLOCK.get());
        ctx.then(SETTLE, () -> ctx.assertTrue(ModList.get().isLoaded("emi") ? CoreEmiPlugin.showRecipesOf(microblock)
                        : CoreJeiPlugin.showRecipesOf(microblock), "the viewer opens on what makes a microblock"))
                .thenScreenshot(20, "microblocks-viewer")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    private static Microblock face(final int size, final Block material, final Direction face) {
        return new Microblock(new Microblock.MicroblockPiece(MicroShape.FACE, size, material), slot(face),
                SpaceSlots.Y);
    }

    private static int slot(final Direction face) {
        return SpaceSlots.face(face.get3DDataValue());
    }

    /* A lever or a button hung on the wall to its south, facing the player to the north. */
    private static BlockState onWall(final Block block) {
        return block.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.WALL)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH);
    }

    /* Everything the tests place, back to air. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -3; x <= 3; x++) {
            for (int y = 2; y <= 4; y++) {
                for (int z = 0; z <= WALL_Z; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

}
