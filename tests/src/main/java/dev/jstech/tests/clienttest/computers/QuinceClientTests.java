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
import dev.jstech.computers.QuinceItems;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Quince's systems on its own computers (computers/docs/design/quince.md), each as its mock draws it: the Quince System
 * on the Russet 128K, Quince OS 9 on the qRusset G3, Quince OS X in Pulp on the white qRusset and gone flat on the slim
 * one, and quinceOS on the coloured qRusset, each machine showing its desktop on its own glass with the startup disk's
 * window open.
 */
public final class QuinceClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_600;
    private static final BlockPos MACHINE = new BlockPos(3, 2, 3);
    private static final BlockPos PLAYER = MACHINE.north(2);
    /* A point every Quince all-in-one has glass at, in sixty-fourths of its front: the middle of the 128K's. */
    private static final int ON_THE_GLASS_X = 32;
    private static final int ON_THE_GLASS_Y = 22;
    /* The scale Quince's desktops are designed at: a desktop pixel to a pixel of the game's interface. */
    private static final int DESIGNED_SCALE = 100;
    private static final int WINDOW_WAIT = 200;
    private static final String FILES = "jsc:files";
    /* The startup disk's root, as its icon opens it. */
    private static final String ROOT = "";

    private QuinceClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceSystem_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(), "quince_system")
                .thenScreenshot(SETTLE, "qs-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOs9_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_G3.getFirst().get(), QuinceItems.BOARD_QRUSSET_G3.get(), "quince_os_9")
                .thenScreenshot(SETTLE, "os9-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOsX_showsItsDesktopInPulp(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(), QuinceItems.BOARD_QRUSSET_WHITE.get(), "quince_os_x")
                .thenScreenshot(SETTLE, "osx-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOsX_showsItsDesktopGoneFlat(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_SLIM.get(), QuinceItems.BOARD_QRUSSET_THIN.get(), "quince_os_x")
                .thenScreenshot(SETTLE, "flat-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOs_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_COLOURS.getFirst().get(), QuinceItems.BOARD_QRUSSET_COLOUR.get(),
                "quinceos")
                .thenScreenshot(SETTLE, "qos-desktop");
    }

    /* The machine built on its board with that system, running, and its own glass open on the desktop. */
    private static ClientTestContext atDesktop(final ClientTestContext ctx, final Block block, final Item board,
                                               final String system) {
        return ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, block.defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    final PersonalComputerBlockEntity machine = world.blockEntity(MACHINE,
                            PersonalComputerBlockEntity.class);
                    build(machine, board);
                    if (!machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system))) {
                        throw new IllegalStateException(system + " does not install on " + block);
                    }
                    machine.console().settings().setGuiScale(DESIGNED_SCALE);
                    machine.togglePower();
                })
                .thenTeleport(SETTLE, PLAYER, Direction.SOUTH)
                // On the glass, where a click opens the desktop; on the case it opens the machine's parts.
                .thenRightClickFront(SETTLE, MACHINE, ON_THE_GLASS_X, ON_THE_GLASS_Y)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpenFiles(ROOT))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(FILES) != null, WINDOW_WAIT,
                        "the startup disk's window")
                .then(SETTLE * 10, () -> ctx.mc().getToasts().clear());
    }

    /*
     * The parts in the slots the machine's own map has for them: the board, the strongest processor of its era, a
     * module of its memory where it takes one, its era's supply and disk.
     */
    private static void build(final PersonalComputerBlockEntity machine, final Item board) {
        final MotherboardSpec spec = EraParts.board(board);
        final ItemStackHandler hardware = machine.getHardware();
        final ComputerHardwareLayout map = machine.hardwareLayout();
        hardware.setStackInSlot(map.motherboardSlot(), new ItemStack(board));
        hardware.setStackInSlot(map.cpuSlot(0), new ItemStack(EraParts.cpuFor(board)));
        if (spec.ramSlots() > 0) {
            hardware.setStackInSlot(map.ramSlot(0), new ItemStack(EraParts.ramFor(board)));
        }
        hardware.setStackInSlot(map.psuSlot(), new ItemStack(EraParts.psuFor(spec.era())));
        hardware.setStackInSlot(map.diskStart(), new ItemStack(EraParts.diskFor(spec.era())));
    }
}
