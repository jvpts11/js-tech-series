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
import dev.jstech.computers.client.BootMenuScreen;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * FreeBSD as a player meets it at a monitor: the loader it counts down at, and the console it comes up to.
 */
public final class FreeBsdClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 1_200;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    private static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");

    private FreeBsdClientTests() {
    }

    /**
     * The machine stops at the loader after its self-test, a key that chooses nothing only stops the count,
     * Enter boots, and the console that comes up greets and answers as FreeBSD.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void loader_countsDownHoldsOnAKeyAndBootsToTheConsole(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    final ItemStackHandler inv = machine.getInventory();
                    inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
                    inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    // A graphics card gives the machine peripheral ports, which is what the monitor links to.
                    inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.installOs(FREEBSD);
                    machine.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(BootMenuScreen.class, BOOT_WAIT)
                .thenScreenshot(2, "freebsd-loader")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_SPACE))
                .thenWaitUntilServer(level -> TestWorldBuilder.at(level, ctx.origin())
                                .blockEntity(MACHINE, MainframeBlockEntity.class).atBootMenu(), SCREEN_WAIT,
                        "the machine to still be standing at the loader", level -> "left the loader")
                .thenScreenshot(2, "freebsd-loader-paused")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> said(ctx, "Welcome to FreeBSD, player."), SCREEN_WAIT, "the console to greet")
                .thenAssert(1, () -> said(ctx, "FreeBSD/vel64 (freebsd) (ttyv0)"),
                        "the console names itself over Velocion's architecture")
                .then(SETTLE, () -> ctx.type("uname -a"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "GENERIC vel64"), SCREEN_WAIT, "uname to answer as FreeBSD")
                .thenScreenshot(2, "freebsd-console");
    }

    private static boolean said(final ClientTestContext ctx, final String words) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt.scrollbackText().stream().anyMatch(l -> l.contains(words));
    }
}
