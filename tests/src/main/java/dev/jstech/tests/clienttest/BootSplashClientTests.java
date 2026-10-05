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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.SystemBootScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The pictures the Frames editions come up behind, each photographed at the moments that make it: Frames XP's logo
 * over its trough of running blocks, Frames 7's lights and then its Welcome, Frames 10's mark, its lock screen and
 * its sign-in. Nothing else asks a boot screen what it drew, so these pictures are how it is looked at.
 */
public final class BootSplashClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_200;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private BootSplashClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void framesXp_runsItsBlocksThroughTheTroughUnderTheLogo(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_xp")
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 30, BOOT_WAIT, "the start to be a third of the way through")
                .thenScreenshot(0, "frames-xp-trough");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void frames7_closesItsLightsIntoTheFlagAndThenSaysWelcome(final ClientTestContext ctx) {
        switchedOn(ctx, "frames_7")
                .thenAwaitScreen(SystemBootScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> through(ctx) >= 45, BOOT_WAIT, "the lights to have closed into the flag")
                .thenScreenshot(0, "frames-7-flag")
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
                .thenScreenshot(0, "frames-10-mark")
                .thenWaitUntil(() -> through(ctx) >= 65, BOOT_WAIT, "the lock screen")
                .thenScreenshot(0, "frames-10-lock")
                .thenWaitUntil(() -> through(ctx) >= 85, BOOT_WAIT, "the sign-in")
                .thenScreenshot(0, "frames-10-sign-in");
    }

    /** How far the start on the glass is, or nought while no boot screen is up. */
    private static int through(final ClientTestContext ctx) {
        final SystemBootScreen screen = ctx.screen(SystemBootScreen.class);
        return screen == null ? 0 : screen.percentThrough();
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
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
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
