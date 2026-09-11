/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.GatewayManagerApp;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The Gateway Manager on a desktop: the rail lists the Gateway on the host, Rename and Identify act on
 * it, the Status tab shows its buffer and empties it, the Permissions tab moves the knobs, and the
 * Computers, Shares and Log tabs show what the Gateway knows.
 */
public final class GatewayManagerClientTests {

    private GatewayManagerClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    /** The Gateway south of the crafting computer, its back socket against it. */
    private static final BlockPos GATEWAY = new BlockPos(5, 2, 3);
    /** A second computer off the network's cable, the one that shares a folder. */
    private static final BlockPos SHARER_CABLE = new BlockPos(4, 2, 1);
    private static final BlockPos SHARER = new BlockPos(5, 2, 1);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    @Nullable
    private static GatewayManagerApp manager(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(GatewayManagerApp.TITLE);
        return window != null && window.app() instanceof GatewayManagerApp app ? app : null;
    }

    private static boolean ready(final ClientTestContext ctx) {
        final GatewayManagerApp app = manager(ctx);
        return app != null && app.hasState() && app.railNames().contains("gateway-1") && !app.selectedName().isEmpty();
    }

    private static ClientTestContext atTheManager(final ClientTestContext ctx) {
        return ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork wired = world.buildCraftingNetwork();
                    wired.cc().installOs(FRAMES_XP);
                    wired.cc().console().install(Programs.GATEWAY_MANAGER.toString());
                    wired.cc().console().setComputerName("desk");
                    world.placeMonitor(MONITOR, Direction.EAST);
                    world.setBlock(GATEWAY, ComputingModule.NETWORK_GATEWAY.get().defaultBlockState()
                            .setValue(NetworkGatewayBlock.FACING, Direction.SOUTH));
                    world.setBlock(SHARER_CABLE, ComputingModule.ETHERNET_CABLE.get());
                    final PersonalComputerBlockEntity sharer = world.placeRunningPersonalComputer(SHARER);
                    sharer.console().setComputerName("lab");
                    final ServerCliComputer lab = new ServerCliComputer(sharer, world.level());
                    lab.makeDir("C:\\pub");
                    lab.setConfig("share", "C:\\pub write");
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(GatewayManagerApp.TITLE))
                .thenWaitUntil(() -> ready(ctx), SCREEN_WAIT, "the Gateway Manager to list the Gateway on this computer");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gatewayManager_showsTheGatewayRenamesItAndEmptiesItsBuffer(final ClientTestContext ctx) {
        atTheManager(ctx)
                .thenServer(0, level -> {
                    if (level.getBlockEntity(ctx.abs(GATEWAY)) instanceof NetworkGatewayBlockEntity g) {
                        g.buffer().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 12));
                    }
                })
                .thenWaitUntil(() -> manager(ctx).bufferUsedShown() == 1, SCREEN_WAIT, "the buffer to show its stack")
                .thenScreenshot(2, "status")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).renameCenter()))
                .thenWaitUntil(() -> manager(ctx).renameOpen(), SCREEN_WAIT, "the rename dialog")
                .then(SETTLE, () -> ctx.type("cc-bridge"))
                .thenScreenshot(2, "rename")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).renameApplyCenter()))
                .thenWaitUntil(() -> "cc-bridge".equals(manager(ctx).selectedName()), SCREEN_WAIT,
                        "the rail and the pane to show the new name")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).identifyCenter()))
                .thenWaitUntil(() -> manager(ctx).statusLine().contains("blinking"), SCREEN_WAIT,
                        "Identify to answer on the status line")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).clearBufferCenter()))
                .thenWaitUntil(() -> manager(ctx).bufferUsedShown() == 0, SCREEN_WAIT,
                        "the buffer to empty into the network")
                .thenScreenshot(2, "cleared")
                .then(0, () -> ctx.assertTrue(manager(ctx).logWhats().contains("clear buffer to network"),
                        "the log keeps the move; got " + manager(ctx).logWhats()));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gatewayManager_movesThePermissionKnobs(final ClientTestContext ctx) {
        atTheManager(ctx)
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabCenter(1)))
                .thenWaitUntil(() -> manager(ctx).tab() == 1, SCREEN_WAIT, "the Permissions tab")
                .thenScreenshot(2, "permissions")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).readToggleCenter()))
                .thenWaitUntil(() -> manager(ctx).permissionsShown()[0] == 0, SCREEN_WAIT, "reads to be denied")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).filesChoiceCenter(2)))
                .thenWaitUntil(() -> manager(ctx).permissionsShown()[2] == 2, SCREEN_WAIT, "files to be read & write")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).ceilingChoiceCenter(0)))
                .thenWaitUntil(() -> manager(ctx).permissionsShown()[3] == 0, SCREEN_WAIT, "the ceiling to be low")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).capChoiceCenter(2)))
                .thenWaitUntil(() -> manager(ctx).permissionsShown()[4] == 2, SCREEN_WAIT, "the cap to be 16")
                .thenScreenshot(2, "permissions-moved")
                .then(0, () -> ctx.assertTrue(manager(ctx).permissionsShown()[1] == 1, "operations were left alone"));
    }

    @ClientTest(timeoutTicks = 2400)
    public static void gatewayManager_showsComputersSharesAndTheLog(final ClientTestContext ctx) {
        atTheManager(ctx)
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabCenter(2)))
                .thenWaitUntil(() -> manager(ctx).tab() == 2, SCREEN_WAIT, "the Computers tab")
                .thenScreenshot(2, "computers")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabCenter(3)))
                .thenWaitUntil(() -> manager(ctx).tab() == 3 && manager(ctx).shareComputers().contains("lab"), SCREEN_WAIT,
                        "the Shares tab to list the computer that shares")
                .thenScreenshot(2, "shares")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabCenter(4)))
                .thenWaitUntil(() -> manager(ctx).tab() == 4 && manager(ctx).logWhats().contains("link"), SCREEN_WAIT,
                        "the Log tab to show the link")
                .thenScreenshot(2, "log")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).tabCenter(0)))
                .thenWaitUntil(() -> manager(ctx).tab() == 0, SCREEN_WAIT, "back on Status")
                .then(SETTLE, () -> ctx.clickDesktop(manager(ctx).openLogCenter()))
                .thenWaitUntil(() -> manager(ctx).tab() == 4, SCREEN_WAIT, "Open the log to switch tabs");
    }
}
