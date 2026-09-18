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
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * A system that builds what it installs, watched from its monitor: the package manager takes the glass, what
 * it prints arrives while it works, and the prompt comes back when the program is on the machine.
 */
public final class SourceBuildClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 600;
    private static final int BUILD_WAIT = 1_200;

    private static final BlockPos MACHINE = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final ResourceLocation GENTOO = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "gentoo");

    private SourceBuildClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void emerge_printsWhileItBuildsAndGivesThePromptBack(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    /*
                     * Built by hand rather than from the usual fixture, which puts the network's own system on
                     * the disk: a machine with two systems stops at its boot manager to be asked which.
                     */
                    world.setBlock(MACHINE, ComputingModule.MAINFRAME.get());
                    final MainframeBlockEntity machine = world.blockEntity(MACHINE, MainframeBlockEntity.class);
                    final ItemStackHandler parts = machine.getInventory();
                    parts.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
                    parts.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    parts.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    parts.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
                    parts.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    machine.togglePower();
                    machine.installOs(GENTOO);
                    machine.installMirror();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.type("emerge --ask mines"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                // The question itself stands where the prompt would, so what is waited for is the list above it.
                .thenWaitUntil(() -> said(ctx, "Total: 1 package"), SCREEN_WAIT * 2,
                        "the manager to list what it would merge and ask")
                .thenScreenshot(2, "emerge-asks")
                .then(1, () -> ctx.type("y"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> said(ctx, "Compiling source"), BUILD_WAIT, "the compile to be under way")
                .thenScreenshot(30, "emerge-compiling")
                .thenWaitUntilServer(level -> machine(ctx, level).console().isInstalled("jsc:minesweeper"),
                        BUILD_WAIT, "the program to be on the machine", level -> "still building")
                .thenScreenshot(10, "emerge-merged");
    }

    private static boolean said(final ClientTestContext ctx, final String what) {
        final CommandPromptScreen<?> screen = ctx.screen(CommandPromptScreen.class);
        return screen != null && screen.scrollbackText().stream().anyMatch(line -> line.contains(what));
    }

    private static MainframeBlockEntity machine(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(MACHINE, MainframeBlockEntity.class);
    }
}
