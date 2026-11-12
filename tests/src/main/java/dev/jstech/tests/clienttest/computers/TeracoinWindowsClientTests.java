/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.CryptoWalletApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.MiningPanelApp;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Teracoin's windows on a Frames 10 desktop (computers/docs/design/teracoin.md), maximized as their mocks show them:
 * the Crypto Wallet, a wallet made in it and each of its tabs, and the Mining Panel with each of its own.
 */
public final class TeracoinWindowsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 160;
    private static final int BOOT_WAIT = 1_200;
    /* Long enough for a tab to ask the machine what it shows and draw it. */
    private static final int TAB_WAIT = 20;
    /* Long enough for a window's opening to play out. */
    private static final int OPENING = 40;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String WALLET = "jsc:crypto_wallet";
    private static final String PANEL = "jsc:mining_panel";
    private static final String[] WALLET_TABS = {"overview", "send", "receive", "transactions", "print", "wallets"};
    private static final String[] PANEL_TABS = {"pool", "miners", "blocks", "settings"};

    private TeracoinWindowsClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void cryptoWallet_makesAWalletAndShowsEachTab(final ClientTestContext ctx) {
        opened(ctx, WALLET)
                .then(SETTLE, () -> ((CryptoWalletApp) app(ctx, WALLET)).makeWallet("main"))
                .thenWaitUntil(() -> app(ctx, WALLET) instanceof CryptoWalletApp wallet && wallet.hasState()
                        && !wallet.address().isEmpty(), SCREEN_WAIT, "the wallet to be made and show its address");
        for (int tab = 0; tab < WALLET_TABS.length; tab++) {
            final int shown = tab;
            ctx.then(SETTLE, () -> ((CryptoWalletApp) app(ctx, WALLET)).showTab(shown))
                    .thenScreenshot(TAB_WAIT, "wallet-" + WALLET_TABS[tab]);
        }
    }

    @ClientTest(timeoutTicks = 2400)
    public static void miningPanel_showsEachTab(final ClientTestContext ctx) {
        opened(ctx, PANEL)
                .thenWaitUntil(() -> app(ctx, PANEL) instanceof MiningPanelApp panel && panel.hasState(),
                        SCREEN_WAIT, "the Mining Panel to be told how the machine mines");
        for (int tab = 0; tab < PANEL_TABS.length; tab++) {
            final int shown = tab;
            ctx.then(SETTLE, () -> ((MiningPanelApp) app(ctx, PANEL)).showTab(shown))
                    .thenScreenshot(TAB_WAIT, "mining-panel-" + PANEL_TABS[tab]);
        }
    }

    /* A running Frames 10 computer with {@code program} installed, opened on its desktop and maximized. */
    private static ClientTestContext opened(final ClientTestContext ctx, final String program) {
        return ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_10"));
                    pc.console().install(program);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(program))
                .thenWaitUntil(() -> window(ctx, program) != null, SCREEN_WAIT, program + " to open in a window")
                // Maximized as its mocks show it; the title bar's own button is the disk tools' tests' to press.
                .then(OPENING, () -> window(ctx, program).setMaximized(true))
                .thenWaitUntil(() -> window(ctx, program).maximized(), SCREEN_WAIT, program + " to be maximized");
    }

    @Nullable
    private static IDesktopApp app(final ClientTestContext ctx, final String program) {
        final DesktopWindow window = window(ctx, program);
        return window == null ? null : window.app();
    }

    @Nullable
    private static DesktopWindow window(final ClientTestContext ctx, final String program) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        return desktop == null ? null : desktop.windowFor(program);
    }
}
