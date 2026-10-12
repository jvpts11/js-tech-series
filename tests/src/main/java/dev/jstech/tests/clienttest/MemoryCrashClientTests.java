/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.CalculatorApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.testkit.EraParts;
import dev.jstech.tests.testkit.TestWorldBuilder;
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
 * The crash of a system with no memory protection, in a player's hands. A Legacy computer with the least memory its
 * board takes runs Frames 95, whose cooperative kernel shares one memory with every program: the player opens the
 * Calculator again and again until a window no longer fits, and the desktop gives way to the crash page. While it is
 * up, neither a click nor a key reaches anything behind it; once it has stood its time the machine comes back to an
 * empty session, which opens programs again. A player who walks away from the crash page finds the machine rebooted
 * all the same.
 */
public final class MemoryCrashClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    /** Long enough for a cold start's self-test and the system's start on a mechanical disk with tight memory. */
    private static final int BOOT_WAIT = 2_400;
    /** The crash page stands 4.2 seconds; this is that with room to spare. */
    private static final int CRASH_WAIT = 200;
    /** More Calculators than 128 MB can hold beside the system: a quarter of its 16 MB each. */
    private static final int MOST_WINDOWS = 40;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String CALCULATOR = "Calculator";
    private static final ResourceLocation FRAMES_95 = ResourceLocation.fromNamespaceAndPath("jsc", "frames_95");

    private MemoryCrashClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void frames95_crashPageHoldsUntilAnEmptySessionComesBack(final ClientTestContext ctx) {
        final String[] before = new String[1];
        atTheDesk(ctx);
        openUntilCrash(ctx, MOST_WINDOWS, () -> ctx
                .thenScreenshot(SETTLE, "crash")
                .thenServer(0, level -> ctx.assertTrue(!computer(ctx, level).openWindows().isEmpty(),
                        "the machine should have the windows that were open when it crashed"))
                // A click on Start opens nothing while the page is up.
                .then(SETTLE, () -> ctx.click(desktop(ctx).startButtonX(), desktop(ctx).startButtonY()))
                .thenAssert(SETTLE, () -> desktop(ctx).crashing() && !desktop(ctx).isStartOpen(),
                        "a click on Start should do nothing while the crash page is up")
                // Nor does the keyboard reach the Calculator in front, behind the page.
                .then(0, () -> before[0] = frontCalculator(ctx).result())
                .then(SETTLE, () -> ctx.type("7+2="))
                .then(SETTLE, () -> ctx.assertTrue(desktop(ctx).crashing()
                                && frontCalculator(ctx).result().equals(before[0]),
                        "keys should not reach the program behind the crash page; the Calculator now shows "
                                + frontCalculator(ctx).result()))
                .thenWaitUntil(() -> !desktop(ctx).crashing(), CRASH_WAIT, "the crash page to give way to a reboot")
                .thenAssert(SETTLE, () -> desktop(ctx).openWindowLabels().isEmpty(),
                        "the machine should come back to an empty session")
                .thenWaitUntilServer(level -> computer(ctx, level).openWindows().isEmpty(), SCREEN_WAIT,
                        "the machine to have no window open after the reboot",
                        level -> "open=" + computer(ctx, level).openWindows().size())
                .thenScreenshot(SETTLE, "rebooted")
                .then(SETTLE, () -> DesktopSteps.launch(ctx, CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null && !desktop(ctx).crashing(),
                        SCREEN_WAIT, "the rebooted desktop to open a program again"));
    }

    @ClientTest(timeoutTicks = 6000)
    public static void frames95_walkingAwayFromTheCrashDoesNotSkipTheReboot(final ClientTestContext ctx) {
        atTheDesk(ctx);
        openUntilCrash(ctx, MOST_WINDOWS, () -> ctx
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, SCREEN_WAIT)
                .thenWaitUntil(() -> !desktop(ctx).crashing(), CRASH_WAIT, "any crash page left to give way")
                .thenScreenshot(SETTLE, "back-at-the-desk")
                .then(SETTLE, () -> ctx.assertTrue(desktop(ctx).openWindowLabels().isEmpty(),
                        "a machine that crashed should not keep its windows because the player left the desk; got "
                                + desktop(ctx).openWindowLabels().size() + " windows")));
    }

    /* The Legacy computer built, running Frames 95, with the player seated at its monitor. */
    private static void atTheDesk(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
                    final PersonalComputerBlockEntity computer = world.blockEntity(COMPUTER,
                            PersonalComputerBlockEntity.class);
                    final ItemStackHandler hardware = computer.getHardware();
                    final Item board = HardwareItems.MOTHERBOARD_ATX_LEGACY_S370.get();
                    hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                            new ItemStack(EraParts.cpuFor(board)));
                    // The least memory the board takes: one stick of 128 MB.
                    hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                            new ItemStack(HardwareItems.RAM_SDRAM_128.get()));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                            new ItemStack(EraParts.psuFor(HardwareEra.LEGACY)));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                            new ItemStack(EraParts.cardFor(board, GpuItem.class)));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                            new ItemStack(EraParts.diskFor(HardwareEra.LEGACY)));
                    if (!computer.installOs(FRAMES_95)) {
                        throw new IllegalStateException("could not install Frames 95 on the Legacy computer");
                    }
                    computer.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST, ComputingModule.LEGACY_MONITOR.get());
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).launcherLabels().contains(CALCULATOR), SCREEN_WAIT,
                        "the desktop to list the Calculator");
    }

    /*
     * Opens the Calculator from Start once more, until a window does not fit and the crash page is up, and then queues
     * what follows: each step queues the next, so what follows the crash has to wait for it to be found.
     */
    private static void openUntilCrash(final ClientTestContext ctx, final int left, final Runnable then) {
        ctx.then(SETTLE, () -> {
            final DesktopScreen desktop = desktop(ctx);
            if (desktop.crashing()) {
                then.run();
                return;
            }
            if (left == 0) {
                throw new ClientTestFailure("Frames 95 never crashed: the memory holds " + desktop.memoryMeter()
                        + " with " + desktop.openWindowLabels().size() + " windows open");
            }
            DesktopSteps.launch(ctx, CALCULATOR);
            openUntilCrash(ctx, left - 1, then);
        });
    }

    /* The Calculator in front: the last of its windows, which has the keyboard. */
    private static CalculatorApp frontCalculator(final ClientTestContext ctx) {
        final List<DesktopWindow> windows = desktop(ctx).windowsFor(CALCULATOR);
        if (windows.isEmpty() || !(windows.getLast().app() instanceof CalculatorApp app)) {
            throw new ClientTestFailure("no Calculator window is open");
        }
        return app;
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
    }
}
