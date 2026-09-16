/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Carries a machine up: the power-on self-test, the wait at a boot manager, and the system coming up.
 *
 * <p>The three belong together because a machine passes through them in order and is only ever in one of
 * them, and they belong to the machine rather than to the screen, the way a copy onto a disk does
 * ({@link dev.jstech.computers.os.install.OsInstallRunner} beside this). Closing the monitor halfway through
 * no longer stops a machine coming up, opening it again shows how far it has got, and a machine nobody is
 * looking at comes up all the same, which is what a machine does.
 *
 * <p>Written over the machine rather than over one class of machine, because a machine in a rack is a
 * machine: it has the same phases, on the same clocks, and only differs in whether anybody can see them.
 * Nothing is pushed to a screen without asking the machine whether it is the one being looked at.
 */
public final class BootRunner {

    /** How long a boot manager waits before booting its first entry: the five seconds those menus always gave. */
    public static final int MENU_TICKS = 100;

    private BootRunner() {
    }

    /** One tick of wherever this machine is on its way up. */
    public static void tick(final IOsHost machine, final BootPhases phases, final ServerLevel level,
                            final BlockPos pos) {
        post(machine, phases, level, pos);
        menu(machine, phases, level);
        boot(machine, phases, level, pos);
    }

    /** How long this machine's system takes to come up: its size, over the disk it sits on and the era. */
    public static int bootLength(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return 0;
        }
        final ItemStack disk = machine.systemDisk();
        final int diskSpeed = disk.getItem() instanceof dev.jstech.computers.item.DiskItem drive
                ? drive.spec().tier().speedMultiplier() : 1;
        final HardwareEra era = machine.installedEra();
        return BootTiming.bootTicks(system.footprintMb(), diskSpeed, era,
                BootTiming.tightRam(machine.ramTotalMb(), system.ramMb()));
    }

    /**
     * Carries the self-test along: works out how long this machine's own takes the first tick after the power
     * goes on, and ends it when its time is up, booting whoever is watching.
     */
    private static void post(final IOsHost machine, final BootPhases phases, final ServerLevel level,
                             final BlockPos pos) {
        if (!machine.isRunning() || !phases.needsPost()) {
            return;
        }
        final long now = level.getGameTime();
        if (phases.postUntimed()) {
            phases.timePost(now + BootTiming.postTicks(memoryModules(machine), postDevices(machine),
                    machine.installedEra()));
            return;
        }
        if (!phases.postDone(now)) {
            return;
        }
        machine.setNeedsPost(false);
        /*
         * The self-test is the moment the machine settles what it is running, which is what makes a desktop
         * installed a moment ago wait for a restart instead of turning up on the next look at the monitor.
         */
        machine.setBootedDesktopId(machine.installedDesktopId());
        /*
         * A machine with nothing to boot ends its self-test on the era's own failure and stays there, the way
         * one does: whoever is watching reads what happened instead of being dropped into the setup.
         */
        if (!machine.hasOs() && !machine.hasBootableMedium()) {
            return;
        }
        /*
         * A system on a disk takes time to come up, and that time is the machine's too. A machine booting a
         * medium instead has no system of its own to load, so it hands over as it always did.
         */
        if (machine.hasOs()) {
            /*
             * The systems that bring a boot manager stop at it first, which is also how a player finds out that
             * the other disk has something on it.
             */
            final BootMenu list = BootLines.menuFor(machine, MENU_TICKS);
            if (!list.isEmpty() && dev.jstech.computers.config.ComputersServerConfig.showBootMenu()) {
                phases.beginMenu(now, MENU_TICKS);
                showMenu(machine, level, pos);
            } else {
                phases.beginBoot();
            }
            return;
        }
        if (machine.onScreen()) {
            ScreenSessions.bootWatchers(level, pos);
        }
    }

    /** Carries the wait at the boot menu along, and goes on by itself when nobody chooses. */
    private static void menu(final IOsHost machine, final BootPhases phases, final ServerLevel level) {
        if (!phases.atMenu()) {
            return;
        }
        if (!machine.isRunning()) {
            phases.endMenu();
            return;
        }
        if (phases.menuDone(level.getGameTime())) {
            phases.endMenu();
            phases.beginBoot();
        }
    }

    /**
     * Carries the system's own coming-up along, after the self-test and before the desktop or the prompt.
     *
     * <p>Built the same way as the self-test and for the same reason: how long a system takes is read off the
     * machine it is on, the screen only watches, and a machine nobody is looking at comes up all the same.
     */
    private static void boot(final IOsHost machine, final BootPhases phases, final ServerLevel level,
                             final BlockPos pos) {
        if (!phases.booting()) {
            return;
        }
        if (!machine.isRunning()) {
            phases.endBoot();
            return;
        }
        final long now = level.getGameTime();
        if (phases.bootUntimed()) {
            phases.timeBoot(now, bootLength(machine));
            /*
             * Whoever watched the self-test end is still looking at it, so they are shown the system coming up the
             * moment the machine knows how long that takes.
             */
            if (machine.onScreen()) {
                ScreenSessions.eachWatcher(level, pos, (player, monitor) ->
                        dev.jstech.computers.block.MonitorBlock.openSystemBoot(player, level, monitor, pos,
                                machine));
            }
            return;
        }
        if (!phases.bootDone(now)) {
            return;
        }
        phases.endBoot();
        greet(machine);
        if (machine.onScreen()) {
            ScreenSessions.bootWatchers(level, pos);
        }
    }

    /** Puts the boot menu in front of whoever is watching. */
    private static void showMenu(final IOsHost machine, final ServerLevel level, final BlockPos pos) {
        if (!machine.onScreen()) {
            return;
        }
        ScreenSessions.eachWatcher(level, pos, (player, monitor) ->
                dev.jstech.computers.block.MonitorBlock.openBootMenu(player, level, monitor, pos, machine));
    }

    /**
     * Puts the system's welcome on the desktop as the system finishes coming up, when it has one to put and is
     * owed the putting: the first time always, and after that only while it is still wanted.
     *
     * <p>The window is the machine's, like every other window on it, so a machine that came up with nobody
     * watching still has its welcome waiting when somebody opens the monitor. It is never put up twice.
     */
    private static void greet(final IOsHost machine) {
        if (!WelcomeFacts.greeter(machine) || !machine.systemWelcome().greets()) {
            return;
        }
        final List<OpenWindow> windows = new ArrayList<>(machine.openWindows());
        for (final OpenWindow open : windows) {
            if (open.key().equals(WelcomeFacts.WINDOW_KEY)) {
                return;
            }
        }
        windows.add(new OpenWindow(WelcomeFacts.WINDOW_KEY, 40, 30, 250, 136, false, false));
        machine.setOpenWindows(windows);
    }

    /** How many memory modules the self-test has to count. */
    private static int memoryModules(final IOsHost machine) {
        final ComputerBuild build = machine.currentBuild();
        return build == null ? 0 : build.rams().size();
    }

    /** How many devices the self-test has to find: everything seated that is not memory. */
    private static int postDevices(final IOsHost machine) {
        final ComputerBuild build = machine.currentBuild();
        return build == null ? 0 : build.disks().size() + build.pcieCards().size();
    }
}
