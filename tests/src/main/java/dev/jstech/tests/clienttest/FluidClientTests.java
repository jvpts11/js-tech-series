/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.tests.TestFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * A declared fluid on a player's game: drawn with the textures and tint it was declared with, see-through in a pool
 * beside the game's water, and its bucket in the player's hand.
 */
public final class FluidClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -1);
    private static final int FLOOR = 1;
    private static final float LOOK_DOWN = 40.0F;

    private FluidClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void liquid_isDrawnTintedAndSeeThrough(final ClientTestContext ctx) {
        ctx.thenAssert(0, () -> IClientFluidTypeExtensions.of(TestFluids.ACID.type()).getTintColor()
                        == TestFluids.ACID.look().tint(), "the acid is drawn with its own tint")
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (int x = -2; x <= 2; x++) {
                        for (int z = 1; z <= 3; z++) {
                            final BlockPos at = ctx.abs(new BlockPos(x, FLOOR, z));
                            level.setBlockAndUpdate(at, x <= 0 ? TestFluids.ACID.block().orElseThrow().get()
                                    .defaultBlockState() : Blocks.WATER.defaultBlockState());
                            level.setBlockAndUpdate(at.below(), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    level.getServer().getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,
                            new ItemStack(TestFluids.ACID.bucket().orElseThrow().get()));
                })
                .thenWaitUntil(() -> ctx.player().getMainHandItem().is(TestFluids.ACID.bucket().orElseThrow().get()),
                        40, "the bucket to reach the player's hand")
                .then(0, () -> ctx.player().setXRot(LOOK_DOWN))
                .thenScreenshot(10, "acid")
                .thenServer(0, level -> clear(ctx, level));
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -2; x <= 2; x++) {
            for (int z = 1; z <= 3; z++) {
                level.removeBlock(ctx.abs(new BlockPos(x, FLOOR, z)), false);
            }
        }
        level.getServer().getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND,
                ItemStack.EMPTY);
    }
}
