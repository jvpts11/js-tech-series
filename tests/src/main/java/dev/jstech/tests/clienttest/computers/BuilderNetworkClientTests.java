/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PortableItems;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.portable.Portables;
import dev.jstech.computers.wireless.AccessPointMount;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.builder.client.MultiblockBuilderScreen;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.tests.TestStructures;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * The Multiblock Builder building from the network's storage (computers/docs/design/wireless.md): with no portable on
 * the player the network is a source the builder cannot use and its page shows it dimmed; carrying a qPhone joined to
 * the network's access point, the source can be chosen, and the page to build counts what the network holds.
 */
public final class BuilderNetworkClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 200;
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    private static final BlockPos RACK = new BlockPos(2, 2, 1);
    private static final BlockPos ACCESS_POINT = new BlockPos(5, 2, 2);
    private static final BlockPos BAKER = new BlockPos(3, 3, 6);
    private static final BlockPos PLAYER = new BlockPos(3, 2, 8);
    private static final int IRON = 64;
    /* The builder's pages by their soft keys: the page to build first, the sources third. */
    private static final int BUILD_PAGE = 0;
    private static final int SOURCE_PAGE = 2;

    private BuilderNetworkClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void builder_takesTheNetworkAsASourceThroughACarriedPortable(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenBuild(0, world -> {
                    world.placeRunningMainframe(MAINFRAME);
                    world.setBlock(MAINFRAME.east(), ComputingModule.HBW_CABLE);
                    final ServerRackBlockEntity rack = world.placeSeededRack(RACK);
                    rack.getServerStorage(0).insert(Items.IRON_BLOCK, IRON);
                    world.setBlock(MAINFRAME.east(2), ComputingModule.PERSONAL_ROUTER.get());
                    world.setBlock(MAINFRAME.east(3), ComputingModule.ETHERNET_CABLE);
                    world.setBlock(ACCESS_POINT, ComputingModule.TRANSITION_ACCESS_POINT.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                            .setValue(AccessPointBlock.MOUNT, AccessPointMount.DESK));
                    world.setBlock(BAKER, TestStructures.BAKER.get().defaultBlockState());
                })
                .thenTeleport(SETTLE, PLAYER, Direction.NORTH)
                .thenGive(0, new ItemStack(MultiblockBuilder.ITEM.get()));
        sources(ctx).thenScreenshot(5, "network-dimmed")
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                // The builder again first, so it stays in the hand, then the phone beside it.
                .thenGive(0, new ItemStack(MultiblockBuilder.ITEM.get()), new ItemStack(PortableItems.QPHONE_5.get()))
                // Long enough for the phone to look for the access point and join it.
                .then(Portables.LOOK_EVERY * 3, () -> { });
        sources(ctx).thenScreenshot(5, "network-usable")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_DOWN))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(SETTLE, () -> press(ctx, ProbeLayout.softX(BUILD_PAGE) + 4, ProbeLayout.SOFT_Y + 4))
                .thenScreenshot(20, "network-counts")
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                .thenServer(0, level -> ctx.serverPlayer().getInventory().clearContent());
    }

    /* The builder in hand, used on the test machine, and its page of sources up. */
    private static ClientTestContext sources(final ClientTestContext ctx) {
        return ctx.then(2, () -> {
                    ctx.selectHotbar(0);
                    ctx.lookAt(Vec3.atCenterOf(ctx.abs(BAKER)));
                })
                .then(2, () -> ctx.rightClick(BAKER, Direction.SOUTH))
                .thenAwaitScreen(MultiblockBuilderScreen.class, WAIT)
                .then(SETTLE, () -> press(ctx, ProbeLayout.softX(SOURCE_PAGE) + 4, ProbeLayout.SOFT_Y + 4));
    }

    /* A press on the builder's face at ({@code x}, {@code y}) from its top left. */
    private static void press(final ClientTestContext ctx, final int x, final int y) {
        final int left = (ctx.mc().getWindow().getGuiScaledWidth() - ProbeLayout.WIDTH) / 2;
        final int top = (ctx.mc().getWindow().getGuiScaledHeight() - ProbeLayout.HEIGHT) / 2;
        ctx.click(left + x + 0.5, top + y + 0.5);
    }
}
