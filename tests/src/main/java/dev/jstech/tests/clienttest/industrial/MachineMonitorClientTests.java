/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.industrial;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.peripheral.PeripheralReconciler;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.integration.computers.AlertKind;
import dev.jstech.industrial.integration.computers.FieldbusCards;
import dev.jstech.industrial.integration.computers.IndustrialControlCable;
import dev.jstech.industrial.integration.computers.IndustrialControllers;
import dev.jstech.industrial.integration.computers.MachineMonitor;
import dev.jstech.industrial.integration.computers.MachineMonitorLayout;
import dev.jstech.industrial.integration.computers.MachineMonitorPayloads;
import dev.jstech.industrial.integration.computers.MachineMonitorPayloads.Answer;
import dev.jstech.industrial.integration.computers.MachineMonitorPayloads.Row;
import dev.jstech.tests.TestHotMachine;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Machine Monitor in a player's hands, in both its looks: on a Legacy network's Frames XP desktop the classic one,
 * with its menus and the machine's panel under the list, and on a Standard network's Frames 10 the modern one, with
 * the search box and the panel beside the list. An Industrial Controller Computer on the network reads a machine past
 * its limit and a Compressor in a whiff of chlorine; the player opens the program on the network's computer, sees
 * both alerts, chooses the overheating to see its machine and acknowledges it, and the alert stays, dimmed.
 */
public final class MachineMonitorClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 200;
    private static final int BOOT_WAIT = 2_400;
    private static final String LABEL = "Machine Monitor";
    /* The controller south of the network's computer, on the access line from the router. */
    private static final BlockPos CONTROLLER = EraNetwork.SECOND_COMPUTER;
    private static final List<BlockPos> CONTROL_CABLE = List.of(CONTROLLER.south(), CONTROLLER.south(2));
    private static final BlockPos HOT = CONTROLLER.south().east();
    private static final BlockPos COMPRESSOR = CONTROLLER.south(2).east();
    private static final double CHLORINE_MOLES = 0.2;

    private MachineMonitorClientTests() {
    }

    @ClientTest(timeoutTicks = 5000)
    public static void legacy_machineMonitorShowsTheAlertsInItsClassicLook(final ClientTestContext ctx) {
        monitor(ctx, EraKit.LEGACY, IndustrialControllers.LEGACY_INDUSTRIAL_CONTROLLER_COMPUTER,
                FieldbusCards.PROFIBUS, true);
    }

    @ClientTest(timeoutTicks = 5000)
    public static void standard_machineMonitorShowsTheAlertsInItsModernLook(final ClientTestContext ctx) {
        monitor(ctx, EraKit.STANDARD, IndustrialControllers.INDUSTRIAL_CONTROLLER_COMPUTER,
                FieldbusCards.ETHERNET_IP, false);
    }

    private static void monitor(final ClientTestContext ctx, final EraKit kit,
                                final Supplier<? extends Block> controller, final Supplier<? extends Item> card,
                                final boolean classic) {
        final String era = kit.name().toLowerCase(Locale.ROOT);
        ctx.thenGive(0)
                .thenBuild(0, world -> {
                    final EraNetwork network = EraNetwork.lay(world, kit);
                    network.computer().console().install(MachineMonitor.ID.toString());
                    controller(world, kit, controller.get(), card.get());
                    world.setBlock(HOT, TestHotMachine.BLOCK.get());
                    world.setBlock(COMPRESSOR, IndustrialModule.COMPRESSOR.get());
                })
                .thenServer(0, level -> {
                    for (final BlockPos at : CONTROL_CABLE) {
                        Cables.lay(level, ctx.abs(at), IndustrialControlCable.CABLE.get());
                    }
                })
                .thenServer(SETTLE, level -> {
                    if (level.getBlockEntity(ctx.abs(HOT)) instanceof TestHotMachine.HotMachineBlockEntity hot) {
                        hot.heatBody().set(TestHotMachine.LIMIT + 100.0);
                    }
                    // A whiff of chlorine at the Compressor: past its ten parts in a million, gone in seconds.
                    GasClouds.release(level, ctx.abs(COMPRESSOR), Elements.of(Element.CHLORINE), CHLORINE_MOLES);
                })
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(MachineMonitor.ID.toString()))
                .thenWaitUntil(() -> window(ctx) != null, WAIT, "the Machine Monitor to open")
                .thenWaitUntil(() -> has(ctx, AlertKind.OVERHEATING) && has(ctx, AlertKind.GAS_LEAK), WAIT,
                        "the Machine Monitor to show the overheating and the gas leak", () -> String.valueOf(
                                MachineMonitorPayloads.latest(ctx.abs(EraNetwork.COMPUTER))))
                .thenWaitUntil(() -> window(ctx).width() == (classic ? MachineMonitorLayout.CLASSIC_W
                                : MachineMonitorLayout.MODERN_W), WAIT,
                        "the window to open in its " + (classic ? "classic" : "modern") + " look",
                        () -> "it is " + window(ctx).width() + " wide")
                .then(SETTLE, () -> ctx.clickDesktop(rowPoint(ctx, classic, indexOf(ctx, AlertKind.OVERHEATING))))
                // The advancements' toasts would cover the window's corner.
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE * 3, "machine-monitor-" + era)
                .then(SETTLE, () -> ctx.clickDesktop(point(ctx, MachineMonitorLayout.ACK_W / 2,
                        MachineMonitorLayout.toolsY(classic) + MachineMonitorLayout.BUTTON_H / 2)))
                .thenWaitUntil(() -> row(ctx, AlertKind.OVERHEATING) != null
                                && row(ctx, AlertKind.OVERHEATING).acknowledged(), WAIT,
                        "Acknowledge to dim the overheating, kept in the list")
                // Past the computer's check of the devices it holds ports for, which a machine is not.
                .then((int) PeripheralReconciler.INTERVAL_TICKS, () -> { })
                .thenWaitUntil(() -> {
                    final Answer answer = MachineMonitorPayloads.latest(ctx.abs(EraNetwork.COMPUTER));
                    return answer != null && answer.machines() == 2;
                }, WAIT, "the controller to read both machines still", () -> String.valueOf(
                        MachineMonitorPayloads.latest(ctx.abs(EraNetwork.COMPUTER))))
                .then(0, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE * 3, "machine-monitor-" + era + "-acknowledged");
    }

    /* The era's controller where the second computer would stand, built of the era's parts and the card, on. */
    private static void controller(final TestWorldBuilder world, final EraKit kit, final Block block,
                                   final Item card) {
        for (int z = EraNetwork.ROUTER.getZ() + 1; z <= CONTROLLER.getZ(); z++) {
            world.setBlock(new BlockPos(EraNetwork.ROUTER.getX(), EraNetwork.ROUTER.getY(), z), kit.access());
        }
        world.setBlock(CONTROLLER.west(), kit.access());
        world.setBlock(CONTROLLER, block);
        world.faceRearTowardCable(CONTROLLER);
        final BlockEntity entity = world.blockEntity(CONTROLLER, BlockEntity.class);
        if (!(entity instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no controller at " + CONTROLLER);
        }
        final ItemStackHandler hardware = computer.getHardware();
        final Item board = kit.computerBoard();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(kit.era())));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuSpec.class)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1, new ItemStack(card));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(kit.era())));
        computer.installOs(kit.desktopOs());
        computer.togglePower();
    }

    private static @Nullable DesktopWindow window(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        return desktop == null ? null : desktop.windowFor(LABEL);
    }

    private static List<Row> rows(final ClientTestContext ctx) {
        final Answer answer = MachineMonitorPayloads.latest(ctx.abs(EraNetwork.COMPUTER));
        return answer == null ? List.of() : answer.rows();
    }

    private static boolean has(final ClientTestContext ctx, final AlertKind kind) {
        return row(ctx, kind) != null;
    }

    private static @Nullable Row row(final ClientTestContext ctx, final AlertKind kind) {
        for (final Row row : rows(ctx)) {
            if (row.kind() == kind) {
                return row;
            }
        }
        return null;
    }

    private static int indexOf(final ClientTestContext ctx, final AlertKind kind) {
        final List<Row> rows = rows(ctx);
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).kind() == kind) {
                return i;
            }
        }
        return 0;
    }

    /* The middle of the machine's cell in the list's row {@code index}. */
    private static int[] rowPoint(final ClientTestContext ctx, final boolean classic, final int index) {
        return point(ctx, MachineMonitorLayout.machineX() + MachineMonitorLayout.MACHINE_W / 2,
                MachineMonitorLayout.listY(classic) + index * MachineMonitorLayout.ROW_H
                        + MachineMonitorLayout.ROW_H / 2);
    }

    /* A point {@code x}, {@code y} into the program's room in the window, on the desktop. */
    private static int[] point(final ClientTestContext ctx, final int x, final int y) {
        final DesktopWindow window = window(ctx);
        return new int[] {window.x() + DesktopWindow.CONTENT_INSET + x,
                window.y() + DesktopWindow.TITLE_H + DesktopWindow.CONTENT_INSET + y};
    }
}
