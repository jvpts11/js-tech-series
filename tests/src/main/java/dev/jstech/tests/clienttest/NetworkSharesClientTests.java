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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Another machine's shared folder, seen from the file explorer: the Network entry lists the hosts
 * that share something, a host lists its shares, and a share lists its files.
 */
public final class NetworkSharesClientTests {

    private NetworkSharesClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;

    /** Where the crafting network's computer stands, and its monitor beside it. */
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    /** A second ethernet branch off the network's cable, and the computer on it that shares. */
    private static final BlockPos SHARER_CABLE = new BlockPos(4, 2, 1);
    private static final BlockPos SHARER = new BlockPos(5, 2, 1);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    private static List<FilesApp> explorers(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return List.of();
        }
        return desktop.windowsFor("Files").stream()
                .map(DesktopWindow::app)
                .filter(FilesApp.class::isInstance)
                .map(FilesApp.class::cast)
                .toList();
    }

    private static boolean lists(final ClientTestContext ctx, final String name) {
        final List<FilesApp> open = explorers(ctx);
        return open.size() == 1 && open.get(0).names().contains(name);
    }

    @ClientTest(timeoutTicks = 2400)
    public static void files_browseAnotherMachinesShareUnderNetwork(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork wired = world.buildCraftingNetwork();
                    wired.cc().installOs(FRAMES_XP);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    /*
                     * A second computer on the same network, off the router's ethernet, is the one that
                     * shares: its whole disk, with a file on it to find.
                     */
                    world.setBlock(SHARER_CABLE, ComputingModule.ETHERNET_CABLE.get());
                    final PersonalComputerBlockEntity sharer = world.placeRunningPersonalComputer(SHARER);
                    sharer.console().setComputerName("main");
                    final ServerCliComputer main = new ServerCliComputer(sharer, world.level());
                    main.writeFile("C:\\note.txt", "hi");
                    main.setConfig("share", "C:\\ write");
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpenFiles("net:"))
                .thenWaitUntil(() -> lists(ctx, "main"), SCREEN_WAIT,
                        "the Network folder to list the host that shares something")
                .thenScreenshot(2, "network-hosts")
                .then(SETTLE, () -> explorers(ctx).get(0).openNamed("main"))
                .thenWaitUntil(() -> lists(ctx, "c"), SCREEN_WAIT, "the host to list its share")
                .then(SETTLE, () -> explorers(ctx).get(0).openNamed("c"))
                .thenWaitUntil(() -> lists(ctx, "note.txt"), SCREEN_WAIT, "the share to list its files")
                .thenScreenshot(2, "share-files")
                .then(0, () -> ctx.assertTrue("net:main/c".equals(explorers(ctx).get(0).currentDir()),
                        "the explorer stands on the share; at " + explorers(ctx).get(0).currentDir()));
    }
}
