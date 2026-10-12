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
import dev.jstech.computers.client.CrashScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.testkit.EraParts;
import dev.jstech.tests.testkit.TestWorldBuilder;
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
 * Calculator again and again until a window no longer fits, and the machine goes down on its blue page. Nothing the
 * player types reaches anything while it stands; then the machine starts over from its self-test and comes back to an
 * empty session, which opens programs again. A player who walks away from the page finds the machine started over
 * all the same.
 */
public final class MemoryCrashClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    /** Long enough for a cold start's self-test and the system's start on a mechanical disk with tight memory. */
    private static final int BOOT_WAIT = 2_400;
    /** More Calculators than 128 MB can hold beside the system: a quarter of its 16 MB each. */
    private static final int MOST_WINDOWS = 40;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String CALCULATOR = "Calculator";
    /** The key the Calculator's windows go by, as the machine keeps its open windows. */
    private static final String CALCULATOR_KEY = "jsc:calculator";
    private static final ResourceLocation FRAMES_95 = ResourceLocation.fromNamespaceAndPath("jsc", "frames_95");

    private MemoryCrashClientTests() {
    }

    @ClientTest(timeoutTicks = 7000)
    public static void frames95_crashPageStandsAndTheMachineStartsOverEmpty(final ClientTestContext ctx) {
        atTheDesk(ctx);
        openUntilCrash(ctx, MOST_WINDOWS, () -> ctx
                .thenAssert(0, () -> crash(ctx).page().style() == CrashStyle.FRAMES_95 && !crash(ctx).page().red(),
                        "the machine should go down on Frames 95's blue page")
                .thenScreenshot(SETTLE, "crash")
                .thenServer(0, level -> ctx.assertTrue(computer(ctx, level).openWindows().isEmpty()
                                && computer(ctx, level).crashPage() != null,
                        "the machine's session should go down with it"))
                // The keyboard reaches nothing while the page stands: the page stays, the machine keeps its time.
                .then(SETTLE, () -> {
                    ctx.type("7+2=");
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenAssert(SETTLE, () -> ctx.mc().screen instanceof CrashScreen,
                        "keys on the crash page should do nothing")
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).launcherLabels().contains(CALCULATOR), SCREEN_WAIT,
                        "the desktop to come back after the self-test and the system's start")
                // Only what the system opens on every start is up, as Frames 95's welcome is: none of the Calculators.
                .thenAssert(SETTLE, () -> desktop(ctx).windowsFor(CALCULATOR).isEmpty(),
                        "the machine should come back to a fresh session, with none of the windows of before")
                .thenServer(0, level -> ctx.assertTrue(computer(ctx, level).openWindows().stream()
                                .noneMatch(window -> window.key().equals(CALCULATOR_KEY)),
                        "the machine should have none of its windows of before after it started over"))
                .thenScreenshot(SETTLE, "rebooted")
                .then(SETTLE, () -> DesktopSteps.launch(ctx, CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowFor(CALCULATOR) != null, SCREEN_WAIT,
                        "the desktop to open a program again"));
    }

    @ClientTest(timeoutTicks = 7000)
    public static void frames95_walkingAwayFromTheCrashDoesNotSkipTheReboot(final ClientTestContext ctx) {
        atTheDesk(ctx);
        openUntilCrash(ctx, MOST_WINDOWS, () -> ctx
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT)
                .thenServer(SETTLE, level -> ctx.assertTrue(computer(ctx, level).crashPage() != null,
                        "the page should still stand with nobody looking at it"))
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> desktop(ctx).launcherLabels().contains(CALCULATOR), SCREEN_WAIT,
                        "the desktop to come back")
                .thenScreenshot(SETTLE, "back-at-the-desk")
                .then(SETTLE, () -> ctx.assertTrue(desktop(ctx).windowsFor(CALCULATOR).isEmpty(),
                        "a machine that crashed should not keep its windows because the player left the desk; got "
                                + desktop(ctx).windowsFor(CALCULATOR).size() + " Calculators")));
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
     * Opens the Calculator from Start once more, until a window does not fit and the machine puts its crash page up,
     * and then queues what follows: each step queues the next, so what follows the crash has to wait for it to be
     * found. While the page is on its way from the machine the desktop is still up, and the step only looks again.
     */
    private static void openUntilCrash(final ClientTestContext ctx, final int left, final Runnable then) {
        ctx.then(SETTLE, () -> {
            if (ctx.mc().screen instanceof CrashScreen) {
                then.run();
                return;
            }
            if (left == 0) {
                throw new ClientTestFailure("Frames 95 never crashed: the memory holds " + desktop(ctx).memoryMeter()
                        + " with " + desktop(ctx).openWindowLabels().size() + " windows open");
            }
            if (ctx.mc().screen instanceof DesktopScreen) {
                DesktopSteps.launch(ctx, CALCULATOR);
            }
            openUntilCrash(ctx, left - 1, then);
        });
    }

    private static CrashScreen crash(final ClientTestContext ctx) {
        return ctx.screen(CrashScreen.class);
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
    }
}
