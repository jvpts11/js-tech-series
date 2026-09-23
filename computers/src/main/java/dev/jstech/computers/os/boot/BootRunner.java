/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.operation.payload.DesktopBalloonPayload;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Carries a machine up: the power-on self-test, the wait at a boot manager, and the system coming up.
 *
 * <p>The three belong together because a machine passes through them in order and is only ever in one of
 * them, and they belong to the machine rather than to the screen, the way a copy onto a disk does
 * ({@link OsInstallRunner} beside this). Closing the monitor halfway through
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

    /** Where in its family the one edition sits that greets with a notice instead of a window. */
    private static final int BALLOON_GREETER_RANK = 2;

    /** How often a machine standing at its failure asks again whether it has somewhere to go: once a second. */
    private static final int HALT_RECHECK_TICKS = 20;

    private BootRunner() {
    }

    /** One tick of wherever this machine is on its way up. */
    public static void tick(final IOsHost machine, final BootPhases phases, final ServerLevel level,
                            final BlockPos pos) {
        mediumTakenOut(machine, level, pos);
        down(machine, phases, level, pos);
        post(machine, phases, level, pos);
        halted(machine, phases, level, pos);
        menu(machine, phases, level);
        boot(machine, phases, level, pos);
    }

    /**
     * The medium a machine was started from has been taken out of the drive.
     *
     * <p>Which ends the session the way it ends on a real machine: there is nothing left to run, so the
     * machine starts again, finds nothing to start with, and shows its firmware. Anybody looking at it
     * watches that happen, instead of being left at a terminal that has quietly stopped answering, which is
     * what this used to do and what made a machine look broken rather than unplugged.
     */
    private static void mediumTakenOut(final IOsHost machine, final ServerLevel level, final BlockPos pos) {
        if (!machine.settleLiveInstall()) {
            return;
        }
        machine.restart();
        ScreenSessions.bootWatchers(level, pos);
    }

    /** How long this machine's system takes to come up: its size, over the disk it sits on and the era. */
    public static int bootLength(final IOsHost machine) {
        final OsDef system = machine.installedOs();
        if (system == null) {
            return 0;
        }
        final ItemStack disk = machine.systemDisk();
        final int diskSpeed = disk.getItem() instanceof DiskItem drive
                ? drive.spec().tier().speedMultiplier() : 1;
        final HardwareEra era = machine.installedEra();
        return BootTiming.bootTicks(system.footprintMb(), diskSpeed, era,
                BootTiming.tightRam(machine.ramTotalMb(), system.ramMb()));
    }

    /**
     * Carries a restart's closing-down along, and starts the self-test when the system has finished saying
     * goodbye.
     *
     * <p>A restart used to jump straight to the self-test, which meant the one screen a system of any age put
     * up on its way down was the one screen a player could never see: switching a machine off showed it and
     * restarting one did not, although restarting is the way anybody actually reboots a computer here.
     */
    private static void down(final IOsHost machine, final BootPhases phases, final ServerLevel level,
                             final BlockPos pos) {
        if (!phases.goingDown()) {
            return;
        }
        if (!machine.isRunning()) {
            phases.endDown();
            return;
        }
        if (!phases.downDone(level.getGameTime())) {
            return;
        }
        phases.endDown();
        machine.setNeedsPost(true);
        /*
         * Timed here rather than on the next tick, so the screen put in front of whoever is watching joins a
         * self-test of the length this machine's self-test actually has. Opened before it was timed, it drew
         * the fallback length instead and read out its lines over a test that had already ended.
         */
        timePost(machine, phases, level.getGameTime());
        if (machine.onScreen()) {
            ScreenSessions.eachWatcher(level, pos,
                    (player, monitor) -> MonitorBlock.openPost(player, level, monitor, pos));
        }
    }

    /** Works out how long this machine's own self-test takes and says when it ends. */
    private static void timePost(final IOsHost machine, final BootPhases phases, final long now) {
        phases.timePost(now + BootTiming.postTicks(memoryModules(machine), postDevices(machine),
                machine.installedEra()));
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
            timePost(machine, phases, now);
            return;
        }
        if (!phases.postDone(now)) {
            return;
        }
        machine.setNeedsPost(false);
        JscEvents.awardOperatorAt(level, pos, JscEvents.POST_PASSED);
        /*
         * The self-test is the moment the machine settles what it is running, which is what makes a desktop
         * installed a moment ago wait for a restart instead of turning up on the next look at the monitor.
         */
        machine.setBootedDesktopId(machine.installedDesktopId());
        /*
         * A machine with nothing to boot ends its self-test on the era's own failure and stays there, the way
         * one does: whoever is watching reads what happened instead of being dropped into the setup.
         */
        /*
         * What is on the disk is looked at before it is trusted. Nothing on these machines is protected, so a
         * player really can delete the system, and what that costs them is decided by what is missing: the
         * whole folder leaves nothing to find, which is the same as an empty disk; the loader alone leaves a
         * system that is found and will not start, and the machine says which file it wanted.
         */
        final SystemIntegrity.Result health = SystemIntegrity.check(machine);
        if (health.state() == SystemIntegrity.State.NO_LOADER && !machine.hasBootableMedium()) {
            phases.halt();
            return;
        }
        final boolean bootable = machine.hasOs() && health.state() != SystemIntegrity.State.NO_SYSTEM;
        if (!bootable && !machine.hasBootableMedium()) {
            /*
             * Written down rather than simply left: the machine is standing at its own failure, and a monitor
             * opened after the fact has to find it there. It used to be a moment only the player already
             * watching ever saw, so looking at such a machine later opened its setup with no word about why.
             */
            phases.halt();
            return;
        }
        /*
         * An installation the machine was restarted for comes before its own system. It is why the machine
         * started over at all, so booting what is already installed first, and stopping at a boot manager to
         * ask which of two systems that should be, is the machine answering a question nobody asked.
         */
        if (machine.installationWaiting()) {
            if (machine.onScreen()) {
                ScreenSessions.bootWatchers(level, pos);
            }
            return;
        }
        /*
         * A system on a disk takes time to come up, and that time is the machine's too. A machine booting a
         * medium instead has no system of its own to load, so it hands over as it always did.
         */
        if (bootable) {
            /*
             * The systems that bring a boot manager stop at it first, which is also how a player finds out that
             * the other disk has something on it.
             */
            final BootMenu list = BootLines.menuFor(machine, MENU_TICKS);
            if (!list.isEmpty() && ComputersServerConfig.showBootMenu()) {
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

    /**
     * A machine standing at a self-test that found nothing to boot, once something to boot turns up.
     *
     * <p>It is waiting for exactly one thing, so when that thing arrives it stops waiting. A machine is powered
     * the moment it is built and its self-test ends seconds later, which is usually before anybody has plugged a
     * drive into it: without this it would stand at a failure that stopped being true, and opening its monitor
     * would show that failure rather than the machine it had become.
     *
     * <p>Switching it off and on again would do the same, and still does. This only spares a player doing it for
     * a machine that is plainly ready.
     */
    private static void halted(final IOsHost machine, final BootPhases phases, final ServerLevel level,
                               final BlockPos pos) {
        if (!phases.halted() || !machine.isRunning()) {
            return;
        }
        /*
         * Nothing the answer below depends on changes by itself: a disk goes in, a medium goes into a drive the
         * machine reaches, a player installs over the system. So a machine standing here asks once a second
         * rather than every tick, which for a rack of servers with no system on them was half of what an idle
         * base cost. Each machine asks on its own tick of the second, taken from where it stands, so a whole
         * datacenter that halted together does not ask together again.
         */
        if (Math.floorMod(level.getGameTime() + Mth.getSeed(pos), HALT_RECHECK_TICKS) != 0) {
            return;
        }
        /*
         * Somewhere to go is a system that is there and whole, or a medium to start from instead. A machine
         * standing at a wrecked system stays there: what is on its disk is not whole, although the disk still
         * says a system is installed, which is the very reason it was found and refused. Putting a medium in is
         * what gives it somewhere to go, and that is how such a machine is repaired. The system is asked about
         * first because the medium means walking every drive the machine is cabled to.
         */
        if (!(machine.hasOs() && SystemIntegrity.check(machine).whole()) && !machine.hasBootableMedium()) {
            return;
        }
        phases.resume();
        if (machine.hasOs()) {
            final BootMenu list = BootLines.menuFor(machine, MENU_TICKS);
            if (!list.isEmpty() && ComputersServerConfig.showBootMenu()) {
                phases.beginMenu(level.getGameTime(), MENU_TICKS);
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
                        MonitorBlock.openSystemBoot(player, level, monitor, pos,
                                machine));
            }
            return;
        }
        if (!phases.bootDone(now)) {
            return;
        }
        phases.endBoot();
        final boolean balloon = greet(machine);
        if (machine.onScreen()) {
            ScreenSessions.bootWatchers(level, pos);
            /*
             * After the desktop is in front of them, never before: a notice raised at a screen that does not
             * exist yet is a notice nobody ever sees.
             */
            if (balloon) {
                sayHello(machine, level, pos);
            }
        }
    }

    /**
     * The notice one edition greeted its owner with, raised from the corner of the desktop it just opened.
     *
     * <p>A sentence and an offer rather than a window: that edition did not interrupt anybody on a first
     * start, it said the machine was ready and left the way to Welcome one click away for as long as the
     * notice was up.
     */
    private static void sayHello(final IOsHost machine, final ServerLevel level, final BlockPos pos) {
        final OsDef system = machine.installedOs();
        final String name = machine.customName().isEmpty() ? "This computer" : machine.customName();
        ScreenSessions.eachWatcher(level, pos, (player, monitor) ->
                PacketDistributor.sendToPlayer(player, new DesktopBalloonPayload(pos,
                        "Welcome to " + (system == null ? "this computer" : system.displayName()),
                        name + " is ready. Click here to see what is on this computer.",
                        WelcomeFacts.WINDOW_KEY)));
    }

    /** Puts the boot menu in front of whoever is watching. */
    private static void showMenu(final IOsHost machine, final ServerLevel level, final BlockPos pos) {
        if (!machine.onScreen()) {
            return;
        }
        ScreenSessions.eachWatcher(level, pos, (player, monitor) ->
                MonitorBlock.openBootMenu(player, level, monitor, pos, machine));
    }

    /**
     * Puts the system's welcome on the desktop as the system finishes coming up, when it has one to put and is
     * owed the putting: the first time always, and after that only while it is still wanted.
     *
     * <p>The window is the machine's, like every other window on it, so a machine that came up with nobody
     * watching still has its welcome waiting when somebody opens the monitor. It is never put up twice.
     *
     * <p>One edition greeted differently, and the difference is the whole of what that start felt like: it
     * raised a notice from the corner and waited to be asked. That one answers yes here, and the notice is
     * sent once the desktop it belongs to is open.
     *
     * @return whether this machine owes a notice rather than a window
     */
    private static boolean greet(final IOsHost machine) {
        if (!WelcomeFacts.greeter(machine) || !machine.systemWelcome().greets()) {
            return false;
        }
        final OsDef system = machine.installedOs();
        if (system != null && system.familyRank() == BALLOON_GREETER_RANK) {
            return true;
        }
        final List<OpenWindow> windows = new ArrayList<>(machine.openWindows());
        for (final OpenWindow open : windows) {
            if (open.key().equals(WelcomeFacts.WINDOW_KEY)) {
                return false;
            }
        }
        windows.add(new OpenWindow(WelcomeFacts.WINDOW_KEY, 40, 30, 250, 136, false, false));
        machine.setOpenWindows(windows);
        return false;
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
