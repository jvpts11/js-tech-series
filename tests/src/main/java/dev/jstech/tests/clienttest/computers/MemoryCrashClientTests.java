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
import dev.jstech.computers.client.CrashScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.os.crash.CrashStyle;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.ClientTestFailure;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
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
 * Calculator again and again until a window no longer fits, and the machine goes down on its blue page. Nothing the
 * player types reaches anything while it stands; then the machine starts over from its self-test and comes back to an
 * empty session, which opens programs again. A player who walks away from the page finds the machine started over
 * all the same. Frames XP, preemptive, warns first, past nine tenths, and then goes down on its own page.
 */
public final class MemoryCrashClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    /** Long enough for a cold start's self-test and the system's start on a mechanical disk with tight memory. */
    private static final int BOOT_WAIT = 2_400;
    /** More Calculators than 128 MB can hold beside the system, at 16 MB each on a Legacy machine. */
    private static final int MOST_WINDOWS = 40;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final String CALCULATOR = "Calculator";
    /** The key the Calculator's windows go by, as the machine keeps its open windows. */
    private static final String CALCULATOR_KEY = "jsc:calculator";
    private static final ResourceLocation FRAMES_95 = ResourceLocation.fromNamespaceAndPath("jsc", "frames_95");
    private static final ResourceLocation FRAMES_XP = ResourceLocation.fromNamespaceAndPath("jsc", "frames_xp");

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

    /**
     * Frames XP, preemptive, warns first: the window that takes the memory past nine tenths opens with a balloon
     * naming what holds the most, and the next one, which does not fit, brings the machine down on XP's own page.
     */
    @ClientTest(timeoutTicks = 7000)
    public static void framesXp_warnsPastNineTenthsAndThenGoesDown(final ClientTestContext ctx) {
        atTheDesk(ctx, FRAMES_XP, HardwareItems.RAM_SDRAM_256.get());
        // Whatever the system opened on its own start goes first, so only the Calculators hold memory.
        ctx.then(SETTLE, () -> {
            for (final String open : List.copyOf(desktop(ctx).openWindowLabels())) {
                final DesktopWindow window = desktop(ctx).windowFor(open);
                if (window != null) {
                    ctx.clickDesktop(window.buttonCentre(DesktopWindow.BUTTON_CLOSE));
                }
            }
        }).thenWaitUntil(() -> desktop(ctx).openWindowLabels().isEmpty(), SCREEN_WAIT, "the desktop to be bare");
        // 128 MB of 256 are the system's and each Calculator holds 16: six of them leave it under nine tenths.
        for (int i = 0; i < 6; i++) {
            ctx.then(SETTLE, () -> DesktopSteps.launch(ctx, CALCULATOR));
        }
        ctx.thenWaitUntil(() -> desktop(ctx).windowsFor(CALCULATOR).size() == 6, SCREEN_WAIT,
                        "six Calculators to open")
                .thenAssert(SETTLE, () -> desktop(ctx).balloonTitle().isEmpty(), "no warning under nine tenths")
                .then(SETTLE, () -> DesktopSteps.launch(ctx, CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowsFor(CALCULATOR).size() == 7
                        && desktop(ctx).balloonTitle().equals("Low on memory"), SCREEN_WAIT,
                        "the seventh, which takes it past nine tenths, to open with the warning")
                // A program is named once with all its windows hold, as the real warning listed programs.
                .then(0, () -> ctx.assertTrue(desktop(ctx).balloonBody().contains("Calculator (112 MB)"),
                        "the warning should name what holds the most; it says: " + desktop(ctx).balloonBody()))
                .thenScreenshot(SETTLE, "xp-warning")
                // The eighth fills the memory to the last megabyte, and the ninth does not fit.
                .then(SETTLE, () -> DesktopSteps.launch(ctx, CALCULATOR))
                .thenWaitUntil(() -> desktop(ctx).windowsFor(CALCULATOR).size() == 8, SCREEN_WAIT,
                        "the eighth to open")
                .then(SETTLE, () -> DesktopSteps.launch(ctx, CALCULATOR))
                .thenAwaitScreen(CrashScreen.class, SCREEN_WAIT)
                .thenAssert(0, () -> crash(ctx).page().style() == CrashStyle.FRAMES_XP && !crash(ctx).page().red()
                                && crash(ctx).page().stopCode().equals("MEMORY_MANAGEMENT"),
                        "the ninth should bring it down on XP's own page")
                .thenScreenshot(SETTLE, "xp-crash");
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

    /**
     * Its only stick of memory taken out from under a running Frames 95, the machine goes down on the red page of a
     * fault of the hardware before the player's eyes, and with no computer left behind it the monitor then goes dark.
     */
    @ClientTest(timeoutTicks = 4000)
    public static void frames95_memoryTakenOutPutsTheRedPageUpThenTheMonitorIsDark(final ClientTestContext ctx) {
        atTheDesk(ctx);
        ctx.thenServer(SETTLE, level -> computer(ctx, level).getHardware()
                        .setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, ItemStack.EMPTY))
                .thenAwaitScreen(CrashScreen.class, SCREEN_WAIT)
                .thenAssert(0, () -> crash(ctx).page().style() == CrashStyle.FRAMES_95 && crash(ctx).page().red(),
                        "the machine should go down on the red page of a fault of the hardware")
                .thenScreenshot(SETTLE, "red-page")
                .thenAwaitNoScreen(SCREEN_WAIT * 4)
                .thenServer(0, level -> ctx.assertTrue(!computer(ctx, level).isRunning()
                                && computer(ctx, level).crashPage() == null,
                        "the machine should be dark once its page has stood its time"));
    }

    /* The Legacy computer built, running Frames 95, with the player seated at its monitor. */
    private static void atTheDesk(final ClientTestContext ctx) {
        atTheDesk(ctx, FRAMES_95);
    }

    /* The Legacy computer built, running that system, with the player seated at its monitor. */
    private static void atTheDesk(final ClientTestContext ctx, final ResourceLocation system) {
        atTheDesk(ctx, system, HardwareItems.RAM_SDRAM_128.get());
    }

    /* The same, with one stick of that memory. */
    private static void atTheDesk(final ClientTestContext ctx, final ResourceLocation system, final Item memory) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
                    final PersonalComputerBlockEntity computer = world.blockEntity(COMPUTER,
                            PersonalComputerBlockEntity.class);
                    final ItemStackHandler hardware = computer.getHardware();
                    final Item board = HardwareItems.MOTHERBOARD_ATX_LEGACY_S370.get();
                    hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                            new ItemStack(EraParts.cpuFor(board)));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(memory));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                            new ItemStack(EraParts.psuFor(HardwareEra.LEGACY)));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                            new ItemStack(EraParts.cardFor(board, GpuItem.class)));
                    hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                            new ItemStack(EraParts.diskFor(HardwareEra.LEGACY)));
                    if (!computer.installOs(system)) {
                        throw new IllegalStateException("could not install " + system + " on the Legacy computer");
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
