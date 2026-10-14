/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.os.MachineMemory;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.ClientTestFailure;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * A Unix kernel out of memory, in a player's hands. On a Linux desktop every window opens, and the kernel then ends
 * what holds the most: the player opens the Calculator again and again on a Legacy computer with the least memory its
 * board takes, and once one no longer fits the latest goes, with a balloon saying why. At a Linux console the kernel's
 * line is printed where the player is typing, and the program it ended in front of the prompt leaves the shell's
 * "Killed" behind it and the prompt back.
 */
public final class OutOfMemoryClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    /** Long enough for a cold start's self-test and the system's start on a mechanical disk with tight memory. */
    private static final int BOOT_WAIT = 2_400;
    /** More Calculators than 128 MB can hold beside Debian and CDE. */
    private static final int MOST_WINDOWS = 30;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String CALCULATOR_KEY = "jsc:calculator";
    private static final String OUT_OF_MEMORY = "Out of memory";
    /** A program that runs until something stops it. */
    private static final String COUNTER = """
            using System.*;
            using System.IO.*;
            namespace Programs;
            class Counter : IScript {
                int seen;
                public void OnInit() { seen = 0; }
                public void OnTick() { seen = seen + 1; Console.PrintLine("tick " + seen); }
                public void OnDestroy() { }
            }
            """;

    private OutOfMemoryClientTests() {
    }

    @ClientTest(timeoutTicks = 7000)
    public static void linuxDesktop_endsTheLatestWindowAndSaysWhy(final ClientTestContext ctx) {
        final int[] kept = new int[1];
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity computer = legacyDebian(world);
                    computer.console().install("jsc:cde");
                    computer.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST, ComputingModule.LEGACY_MONITOR.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT);
        openUntilKilled(ctx, MOST_WINDOWS, () -> ctx
                .thenAssert(0, () -> desktop(ctx).balloonBody().startsWith("The system ran out of memory and closed "),
                        "the balloon should say what was closed; it says: " + desktop(ctx).balloonBody())
                .thenScreenshot(SETTLE, "out-of-memory")
                .thenServer(SETTLE, level -> {
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    final RamLedger ledger = computer.ramLedger();
                    ctx.assertTrue(ledger.usedMb() <= ledger.totalMb() && computer.crashPage() == null,
                            "the machine should hold what it has and stay up; it holds " + ledger.usedMb() + " of "
                                    + ledger.totalMb() + " MB");
                    kept[0] = (int) computer.openWindows().stream()
                            .filter(window -> window.key().equals(CALCULATOR_KEY)).count();
                })
                .thenWaitUntil(() -> desktop(ctx).windowsFor(CALCULATOR_KEY).size() == kept[0], SCREEN_WAIT,
                        "the desktop to show the Calculators the machine kept"));
    }

    @ClientTest(timeoutTicks = 5000)
    public static void linuxConsole_printsTheKernelsLineAndTheShellsKilled(final ClientTestContext ctx) {
        final int[] counter = new int[1];
        ctx.thenBuild(0, world -> {
                    legacyDebian(world).togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST, ComputingModule.LEGACY_MONITOR.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenServer(SETTLE * 4, level -> {
                    // The counter in front of the prompt, holding the most a program may.
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    final MachinePrograms.Started started = computer.programs().start("counter.sgs", COUNTER,
                            MachinePrograms.MAX_HEAP_MB, computer);
                    ctx.assertTrue(started.ok(), "the counter should start: " + started.message());
                    counter[0] = started.id();
                    computer.programs().hold(started.id());
                })
                .thenWaitUntil(() -> said(ctx, "tick "), SCREEN_WAIT, "the counter to print at the prompt")
                .thenServer(SETTLE, level -> {
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    // Calculators up to a little past what is left beside the counter: the counter holds the most.
                    final int each = MachineMemory.windowRamMb(CALCULATOR_KEY, computer.displayEra(), spec -> false);
                    computer.setOpenWindows(calculators(computer.ramLedger().freeMb() / each + 1));
                    computer.weighMemory();
                    ctx.assertTrue(computer.programs().byId(counter[0]) == null, "the counter should be ended");
                })
                .thenWaitUntil(() -> said(ctx, "Out of memory: Killed process " + counter[0] + " (sigma)")
                        && saidExactly(ctx, "Killed"), SCREEN_WAIT, "the kernel's line and the shell's word")
                .thenScreenshot(SETTLE, "killed-at-the-console")
                .then(SETTLE, () -> ctx.type("echo back"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> saidExactly(ctx, "back"), SCREEN_WAIT, "the prompt to be the shell's again");
    }

    /* The Legacy computer with the least memory its board takes, 128 MB, with Debian on its disk. */
    private static PersonalComputerBlockEntity legacyDebian(final TestWorldBuilder world) {
        world.setBlock(COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        final Item board = HardwareItems.MOTHERBOARD_ATX_LEGACY_S370.get();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SDRAM_128.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(EraParts.psuFor(HardwareEra.LEGACY)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuItem.class)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(EraParts.diskFor(HardwareEra.LEGACY)));
        if (!computer.installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"))) {
            throw new IllegalStateException("could not install Debian on the Legacy computer");
        }
        return computer;
    }

    /*
     * Opens the Calculator once more, until the kernel ends a window and the desktop says so, and then queues what
     * follows: each step queues the next, so what follows has to wait for the balloon to be found.
     */
    private static void openUntilKilled(final ClientTestContext ctx, final int left, final Runnable then) {
        ctx.then(SETTLE, () -> {
            if (ctx.mc().screen instanceof DesktopScreen screen && screen.balloonTitle().equals(OUT_OF_MEMORY)) {
                then.run();
                return;
            }
            if (left == 0) {
                throw new ClientTestFailure("the kernel never ended a window, with "
                        + desktop(ctx).windowsFor(CALCULATOR_KEY).size() + " Calculators open");
            }
            DesktopScreen.requestOpen(CALCULATOR_KEY);
            openUntilKilled(ctx, left - 1, then);
        });
    }

    /* That many Calculator windows, as a desktop leaves them open on the machine. */
    private static List<OpenWindow> calculators(final int count) {
        final List<OpenWindow> windows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            windows.add(new OpenWindow(CALCULATOR_KEY, 10 + i, 10 + i, 120, 140, false, false, "", 1));
        }
        return windows;
    }

    private static boolean said(final ClientTestContext ctx, final String what) {
        return ctx.mc().screen instanceof CommandPromptScreen<?> screen
                && screen.scrollbackText().stream().anyMatch(line -> line.contains(what));
    }

    private static boolean saidExactly(final ClientTestContext ctx, final String what) {
        return ctx.mc().screen instanceof CommandPromptScreen<?> screen
                && screen.scrollbackText().stream().anyMatch(line -> line.strip().equals(what));
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
    }
}
