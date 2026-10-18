/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.fire.ExtinguishingAgent;
import dev.jstech.core.jstechseries.fire.CharredBlocks;
import dev.jstech.core.jstechseries.fire.ExtinguisherItem;
import dev.jstech.core.jstechseries.fire.FireDetectorBlock;
import dev.jstech.core.jstechseries.fire.FireSafety;
import dev.jstech.core.jstechseries.fire.SprinklerBlock;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What fire leaves and what fights it, as a player sees them: charred wood and ash drawn from the game's own oak and
 * snow as the resources load, a fire smoking under a sprinkler whose bulb burst and detectors whose lamps call, an
 * extinguisher in the hand, and its spray.
 */
public final class FireClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos FIRE = new BlockPos(1, 3, -4);
    private static final List<BlockPos> CEILING = List.of(new BlockPos(0, 5, -4), new BlockPos(1, 5, -4),
            new BlockPos(2, 5, -4));
    private static final List<BlockPos> PLACED = List.of(new BlockPos(-2, 2, -4), new BlockPos(-1, 2, -4),
            new BlockPos(0, 2, -4), new BlockPos(1, 2, -4), FIRE, new BlockPos(0, 4, -4), new BlockPos(1, 4, -4),
            new BlockPos(2, 4, -4));
    private static final int WAIT = 200;
    private static final int SETTLE = 10;
    private static final int LAND = 60;

    private FireClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void fire_leavesItsRemainsAndIsFoughtWithItsTools(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.give(0, ItemStack.EMPTY);
            for (final BlockPos pos : PLACED) {
                ctx.serverLevel().setBlockAndUpdate(ctx.abs(pos), Blocks.AIR.defaultBlockState());
            }
            for (final BlockPos pos : CEILING) {
                ctx.serverLevel().setBlockAndUpdate(ctx.abs(pos), Blocks.AIR.defaultBlockState());
            }
        }));
        ctx.thenServer(0, level -> {
            ctx.give(0, new ItemStack(FireSafety.WATER_EXTINGUISHER.get()));
            for (final BlockPos pos : CEILING) {
                level.setBlockAndUpdate(ctx.abs(pos), Blocks.STONE.defaultBlockState());
            }
            final BlockState log = CharredBlocks.CHARRED_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(-2, 2, -4)), log);
            level.setBlockAndUpdate(ctx.abs(new BlockPos(-1, 2, -4)),
                    CharredBlocks.CHARRED_PLANKS.get().defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 2, -4)), CharredBlocks.ASH.get().defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(FIRE.below()), Blocks.NETHERRACK.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(FIRE), Blocks.FIRE.defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(0, 4, -4)),
                    FireSafety.HEAT_DETECTOR.get().defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(1, 4, -4)), FireSafety.SPRINKLER.get().defaultBlockState());
            level.setBlockAndUpdate(ctx.abs(new BlockPos(2, 4, -4)),
                    FireSafety.SMOKE_DETECTOR.get().defaultBlockState());
        })
                .thenTeleport(0, STAND, Direction.NORTH)
                .then(0, () -> ctx.selectHotbar(0))
                .thenAwaitLandDrawn(WAIT)
                .thenAssert(SETTLE, () -> List.of("charred_log_side", "charred_log_top", "charred_planks", "ash")
                        .stream().allMatch(name -> Minecraft.getInstance().getResourceManager().getResource(
                                ResourceLocation.fromNamespaceAndPath("jscore", "textures/block/" + name + ".png"))
                                .isPresent()), "what fire leaves is drawn as the resources load")
                .thenWaitUntilServer(level -> level.getBlockState(ctx.abs(new BlockPos(1, 4, -4)))
                                .getValue(SprinklerBlock.OPEN)
                                && level.getBlockState(ctx.abs(new BlockPos(2, 4, -4)))
                                .getValue(FireDetectorBlock.ALARM), WAIT,
                        "the sprinkler's bulb to burst and the smoke detector to call",
                        level -> "the sprinkler is " + level.getBlockState(ctx.abs(new BlockPos(1, 4, -4)))
                                + ", the detector " + level.getBlockState(ctx.abs(new BlockPos(2, 4, -4))))
                .thenScreenshot(LAND, "fire_scene")
                .thenServer(0, level -> ExtinguisherItem.spray(level, ctx.serverPlayer(), ExtinguishingAgent.WATER,
                        true, false))
                .thenScreenshot(3, "extinguisher_spray");
    }
}
