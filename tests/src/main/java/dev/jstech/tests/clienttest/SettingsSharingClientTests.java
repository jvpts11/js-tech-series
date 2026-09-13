/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.SettingsApp;
import dev.jstech.computers.program.ServerCliComputer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The Network page of Settings shares a folder, changes its mode, removes it, and says whether other
 * computers may start programs here; each change goes through the same setting the prompt's config sets.
 */
public final class SettingsSharingClientTests {

    private SettingsSharingClientTests() {
    }

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final int NETWORK_PAGE = 2;
    private static final String SETTINGS = "Settings";

    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation FRAMES_XP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp");

    @Nullable
    private static SettingsApp settings(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        if (desktop == null) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(SETTINGS);
        return window != null && window.app() instanceof SettingsApp app ? app : null;
    }

    private static boolean onNetworkPage(final ClientTestContext ctx) {
        final SettingsApp app = settings(ctx);
        return app != null && app.page() == NETWORK_PAGE && app.shareForWritingCenter()[0] > 0;
    }

    @ClientTest(timeoutTicks = 2400)
    public static void settings_sharesAFolderChangesItsModeAndRemovesIt(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    final CraftingComputerBlockEntity computer = world.placeRunningCraftingComputer(COMPUTER);
                    computer.installOs(FRAMES_XP);
                    computer.console().setComputerName("desk");
                    new ServerCliComputer(computer, world.level()).makeDir("C:\\pub");
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(SETTINGS))
                .thenWaitUntil(() -> settings(ctx) != null && settings(ctx).navCenter(NETWORK_PAGE)[0] > 0, SCREEN_WAIT,
                        "the Settings window")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).navCenter(NETWORK_PAGE)))
                .thenWaitUntil(() -> onNetworkPage(ctx), SCREEN_WAIT, "the Network page with its sharing controls")
                .thenScreenshot(2, "network-page")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).shareFieldCenter()))
                .then(SETTLE, () -> ctx.type("C:\\pub"))
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).shareForWritingCenter()))
                .thenWaitUntil(() -> settings(ctx).sharesShown().contains("C:\\pub write"), SCREEN_WAIT,
                        "the folder to be listed as shared for writing")
                .thenScreenshot(2, "shared")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).shareRowReadCenter(0)))
                .thenWaitUntil(() -> settings(ctx).sharesShown().contains("C:\\pub read"), SCREEN_WAIT,
                        "the share to turn read-only")
                .thenServer(0, level -> {
                    if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof CraftingComputerBlockEntity computer) {
                        final var shares = computer.console().settings().shares();
                        ctx.assertTrue(shares.size() == 1 && !shares.get(0).writable(),
                                "the machine keeps the share read-only; got " + shares);
                    }
                })
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).remoteRefusedCenter()))
                .thenWaitUntil(() -> !settings(ctx).remoteAllowedShown(), SCREEN_WAIT, "programs from other computers to be refused")
                .thenServer(0, level -> {
                    if (level.getBlockEntity(ctx.abs(COMPUTER)) instanceof CraftingComputerBlockEntity computer) {
                        ctx.assertTrue(!computer.console().settings().remoteAllowed(), "the machine says no to remote programs");
                    }
                })
                .thenScreenshot(2, "refused")
                .then(SETTLE, () -> ctx.clickDesktop(settings(ctx).shareRowRemoveCenter(0)))
                .thenWaitUntil(() -> settings(ctx).sharesShown().isEmpty(), SCREEN_WAIT, "the share to be removed")
                .thenScreenshot(2, "removed");
    }
}
