/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.plant.CduBlockEntity;
import dev.jstech.computers.plant.CoolingPlant;
import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The CDU (computers/docs/design/temperature.md): placed whole, it stands drawn as its approved model, its four water
 * ports in its roof; used, it opens its own machine screen.
 */
public final class CduClientTests {

    private static final BlockPos CDU = new BlockPos(2, 2, 20);
    private static final int SETTLE = 6;
    private static final int SCREEN_WAIT = 60;
    /* Ticks for the server's teleport to reach the player's game, which turns the head back to its facing. */
    private static final int TELEPORT_SETTLES = 20;

    private CduClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void cdu_standsWithItsPortsInItsRoofAndOpensItsScreen(final ClientTestContext ctx) {
        ctx.thenGive(0).thenServer(0, level -> {
            final BlockPos origin = ctx.origin().offset(CDU);
            final Block block = CoolingPlant.CDU.get();
            final BlockState state = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                    Direction.SOUTH);
            level.setBlock(origin, state, Block.UPDATE_ALL);
            block.setPlacedBy(level, origin, state, null, ItemStack.EMPTY);
            if (!(level.getBlockEntity(origin) instanceof CduBlockEntity)) {
                throw new ClientTestFailure("no CDU at " + origin);
            }
        })
                .thenServer(SETTLE, level -> {
                    // Hanging over its front, flying so it does not fall, to look down on its roof.
                    final ServerPlayer player = ctx.serverPlayer();
                    final Vec3 over = Vec3.atLowerCornerOf(ctx.origin().offset(CDU)).add(0.0, 4.5, 2.5);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, over.x, over.y, over.z, 180F, 60F);
                })
                .then(TELEPORT_SETTLES, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.origin().offset(CDU))
                        .add(0.0, 3.0, 0.0)))
                .thenScreenshot(SETTLE, "cdu-roof-ports")
                .thenTeleport(SETTLE, CDU.south(2), Direction.NORTH)
                .thenScreenshot(TELEPORT_SETTLES, "cdu-front")
                .thenRightClick(SETTLE, CDU)
                .thenAwaitScreen(MachineScreen.class, SCREEN_WAIT)
                .thenScreenshot(SETTLE, "cdu-screen");
    }
}
