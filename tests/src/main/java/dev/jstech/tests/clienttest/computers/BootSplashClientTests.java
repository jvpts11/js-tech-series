/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.BootSequenceScreen;
import dev.jstech.computers.client.SystemBootScreen;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * The pictures the machines come up behind, each photographed at the moments that make it: the self-test's maker's
 * mark, Frames 95's logo on its sky, Frames XP's logo over its trough of running blocks, Frames 7's lights and then
 * its Welcome, Frames 10's mark, its lock screen and its sign-in, and Frames 11's mark over its ring of dots. Nothing
 * else asks a boot screen what it drew, so these pictures are how it is looked at; on the plain grounds the logo is
 * also measured, and has to stand in the middle of the glass.
 */
public final class BootSplashClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_200;
    /** How far a logo's middle may stand from the glass's, in the interface's pixels. */
    private static final int CENTRE_SLACK = 2;
    /** How far apart two colours have to be in one channel for a pixel to be told from the ground. */
    private static final int INK_STEP = 40;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private BootSplashClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void post_putsTheMakersMarkInTheMiddleOfTheGlass(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_11", true)
                .thenAwaitScreen(BootSequenceScreen.class, BOOT_WAIT)
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(2, "post-uefi")
                .then(0, () -> assertTopmostCentred(ctx, "the maker's mark on the self-test"));
    }

    @ClientTest(timeoutTicks = 3600)
    public static void frames95_comesUpBehindItsLogoOnTheSky(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_95")
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 30, BOOT_WAIT, "the start to be a third of the way through")
                .thenScreenshot(0, "frames-95-sky");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void framesXp_runsItsBlocksThroughTheTroughUnderTheLogo(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_xp")
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 30, BOOT_WAIT, "the start to be a third of the way through")
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(2, "frames-xp-trough")
                .then(0, () -> assertTopmostCentred(ctx, "Frames XP's logo"));
    }

    @ClientTest(timeoutTicks = 3600)
    public static void frames7_closesItsLightsIntoTheFlagAndThenSaysWelcome(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_7")
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 45, BOOT_WAIT, "the lights to have closed into the flag")
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(2, "frames-7-flag")
                .then(0, () -> assertTopmostCentred(ctx, "Frames 7's flag"))
                .thenWaitUntil(() -> through(ctx) >= 80, BOOT_WAIT, "Welcome to take the glass")
                .thenScreenshot(0, "frames-7-welcome");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void frames10_saysHiOnItsFirstStart(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_10", false)
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 40, BOOT_WAIT, "the start to be under way")
                .thenScreenshot(0, "frames-10-hi");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void frames10_showsItsMarkThenTheLockScreenThenTheSignIn(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_10", true)
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 20, BOOT_WAIT, "the mark to be up")
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(2, "frames-10-mark")
                .then(0, () -> assertTopmostCentred(ctx, "Frames 10's mark"))
                .thenWaitUntil(() -> through(ctx) >= 65, BOOT_WAIT, "the lock screen")
                .thenScreenshot(0, "frames-10-lock")
                .thenWaitUntil(() -> through(ctx) >= 85, BOOT_WAIT, "the sign-in")
                .thenScreenshot(0, "frames-10-sign-in");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void frames11_comesUpBehindTheMakersMarkInTheMiddle(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_11", true)
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 30, BOOT_WAIT, "the start to be a third of the way through")
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(2, "frames-11-mark")
                .then(0, () -> assertTopmostCentred(ctx, "the maker's mark Frames 11 starts behind"));
    }

    /** How far the start on the glass is, or nought while no boot screen is up. */
    private static int through(final ClientTestContext ctx) {
        final SystemBootScreen screen = ctx.openScreen(SystemBootScreen.class);
        return screen == null ? 0 : screen.percentThrough();
    }

    /**
     * Fails unless the first thing down the glass from its top, the logo or mark on every screen this is asked of,
     * stands in the middle of the glass from side to side. The ground is read at the glass's top left corner, and the
     * logo is the run of rows, from the first one with anything on it, that each have something on them.
     */
    private static void assertTopmostCentred(final ClientTestContext ctx, final String what) {
        final AbstractContainerScreen<?> screen = ctx.screen(AbstractContainerScreen.class);
        final int left = screen.getGuiLeft();
        final int top = screen.getGuiTop();
        final int right = left + screen.getXSize() - 1;
        final int bottom = top + screen.getYSize() - 1;
        int from = Integer.MAX_VALUE;
        int to = Integer.MIN_VALUE;
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            final int ground = frame.pixel(left + 1, top + 1);
            boolean inLogo = false;
            for (int y = top + 2; y < bottom - 1; y++) {
                int rowFrom = Integer.MAX_VALUE;
                int rowTo = Integer.MIN_VALUE;
                for (int x = left + 2; x < right - 1; x++) {
                    if (standsApart(frame.pixel(x, y), ground)) {
                        rowFrom = Math.min(rowFrom, x);
                        rowTo = Math.max(rowTo, x);
                    }
                }
                if (rowTo < rowFrom) {
                    if (inLogo) {
                        break;
                    }
                    continue;
                }
                inLogo = true;
                from = Math.min(from, rowFrom);
                to = Math.max(to, rowTo);
            }
        }
        ctx.assertTrue(from <= to, what + " is on the glass");
        final int leftMargin = from - left;
        final int rightMargin = right - to;
        ctx.assertTrue(Math.abs(leftMargin - rightMargin) <= 2 * CENTRE_SLACK, what + " stands in the middle of the "
                + "glass, but has " + leftMargin + " pixels to its left and " + rightMargin + " to its right");
    }

    /** Whether a pixel is far enough from the ground, in any one channel, to be something drawn on it. */
    private static boolean standsApart(final int pixel, final int ground) {
        for (int shift = 0; shift <= 16; shift += 8) {
            if (Math.abs((pixel >> shift & 0xFF) - (ground >> shift & 0xFF)) > INK_STEP) {
                return true;
            }
        }
        return false;
    }

    /** A machine carrying {@code system}, just switched on, with the player at its monitor. */
    private static ClientTestContext switchedOn(final ClientTestContext ctx, final String system) {
        return switchedOn(ctx, system, false);
    }

    /** The same, on a machine whose system has greeted it before when {@code metBefore}, so it does not again. */
    private static ClientTestContext switchedOn(final ClientTestContext ctx, final String system,
                                                final boolean metBefore) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    TestWorldBuilder.installMainframeParts(machine, StorageTier.SSD, true);
                    machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    if (metBefore) {
                        machine.setSystemWelcome(machine.systemWelcome().met());
                    }
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR);
    }
}
