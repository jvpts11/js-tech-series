/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.ServerRackStructure;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.plant.CoolingWater;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.ServerStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A Supercomputer Rack cooled by water (computers/docs/design/temperature.md): seen from above, its two ports lie in
 * its roof, the inlet at its back right and the outlet at its front left; with more heat than its air takes away and
 * cold water at its inlet, its window shows the blue drop of its liquid cooling before its state.
 */
public final class RackCoolingClientTests {

    private static final BlockPos RACK = new BlockPos(2, 2, 20);
    private static final int SETTLE = 6;
    private static final int SCREEN_WAIT = 60;
    private static final int TWO_SECONDS = 45;
    /* Ticks for the server's teleport to reach the player's game, which turns the head back to its facing. */
    private static final int TELEPORT_SETTLES = 20;
    /* How near a pixel's channels lie to the cold water's to count as the drop, and how many the drop has at least. */
    private static final int NEAR = 30;
    private static final int DROP_PIXELS = 6;

    private RackCoolingClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void supercomputerRack_showsItsPortsAndItsDrop(final ClientTestContext ctx) {
        ctx.thenGive(0).thenServer(0, level -> {
            final BlockPos origin = ctx.origin().offset(RACK);
            final Block block = ComputingModule.SUPERCOMPUTER_RACK.get();
            final BlockState state = block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                    Direction.SOUTH);
            level.setBlock(origin, state, Block.UPDATE_ALL);
            block.setPlacedBy(level, origin, state, null, ItemStack.EMPTY);
            if (!(level.getBlockEntity(origin) instanceof ServerRackBlockEntity rack)) {
                throw new ClientTestFailure("no rack at " + origin);
            }
            for (final int row : new int[] {0, 2, 4}) {
                rack.getServers().setStackInSlot(row, ServerStacks.defaultSupercomputerNode());
            }
            final BlockPos top = ServerRackStructure.at(origin, Direction.SOUTH, 0, ServerRackStructure.HEIGHT - 1, 1);
            final IFluidHandler inlet = level.getCapability(Capabilities.FluidHandler.BLOCK, top, Direction.UP);
            if (inlet == null || inlet.fill(new FluidStack(CoolingWater.COLD.get(), 500),
                    IFluidHandler.FluidAction.EXECUTE) != 500) {
                throw new ClientTestFailure("the rack's inlet took no cold water");
            }
        })
                .thenServer(SETTLE, level -> {
                    // Hanging over the rack's front, flying so it does not fall, to look down on its roof.
                    final ServerPlayer player = ctx.serverPlayer();
                    final Vec3 over = Vec3.atLowerCornerOf(ctx.origin().offset(RACK)).add(0.0, 4.5, 2.5);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, over.x, over.y, over.z, 180F, 60F);
                })
                .then(TELEPORT_SETTLES, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.origin().offset(RACK))
                        .add(0.0, 3.0, 0.0)))
                .thenScreenshot(SETTLE, "rack-top-ports")
                .thenTeleport(TWO_SECONDS, RACK.south(2), Direction.NORTH)
                .thenRightClick(SETTLE, RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                .thenScreenshot(SETTLE, "rack-cooled")
                .then(1, () -> {
                    final int drop = dropPixels(ctx);
                    if (drop < DROP_PIXELS) {
                        throw new ClientTestFailure("the window shows the blue drop of its liquid cooling: " + drop
                                + " pixels of the cold water's blue in its header");
                    }
                });
    }

    /* How many pixels of the cold water's blue the window's header row holds. */
    private static int dropPixels(final ClientTestContext ctx) {
        final ServerRackScreen screen = (ServerRackScreen) ctx.mc().screen;
        final int colour = CoolingWater.COLD_WATER.colour();
        final int y0 = screen.getGuiTop() + ServerRackLayout.TITLE_Y;
        int found = 0;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = y0; y < y0 + 8; y++) {
                for (int x = screen.getGuiLeft(); x < screen.getGuiLeft() + ServerRackLayout.HEADER_X
                        + ServerRackLayout.HEADER_W; x++) {
                    final int rgb = frame.pixel(x, y);
                    if (Math.abs((rgb >> 16 & 0xFF) - (colour >> 16 & 0xFF)) < NEAR
                            && Math.abs((rgb >> 8 & 0xFF) - (colour >> 8 & 0xFF)) < NEAR
                            && Math.abs((rgb & 0xFF) - (colour & 0xFF)) < NEAR) {
                        found++;
                    }
                }
            }
        }
        return found;
    }
}
