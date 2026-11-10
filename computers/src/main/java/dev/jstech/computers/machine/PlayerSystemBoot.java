/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.api.language.ILanguageProcess;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.PlayerPanic;
import dev.jstech.computers.os.player.PlayerBoot;
import dev.jstech.computers.os.player.PlayerSystem;
import dev.jstech.computers.os.player.PlayerSystems;
import dev.jstech.computers.os.player.SystemRun;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.operation.payload.desktop.PlayerScreenPayloads;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Running an operating system a player wrote (computers/docs/design/player-systems.md): the machine boots it as its
 * process 1 from the listing on its disk, it comes up as its {@code Boot} calls say until it calls {@code Done}, and
 * a fault it does not catch, a {@code Panic.Halt}, or its simply ending brings the machine down on its own page.
 */
@TextHolder
public final class PlayerSystemBoot {

    /** How long the machine waits for a player's system to say it is up: as long as it takes. */
    public static final int BOOT_TICKS = Integer.MAX_VALUE / 4;
    private static final TextKey NO_LISTING = TextKey.of("jsc.machine.player_system.no_listing",
            "the system's listing, %s, is not on its disk");
    private static final TextKey WENT_DOWN = TextKey.of("jsc.machine.player_system.went_down", "%s went down.");
    private static final TextKey ENDED = TextKey.of("jsc.machine.player_system.ended",
            "the system's process ended, and nothing was left to run the machine");

    private PlayerSystemBoot() {
    }

    /** Whether the system the machine runs is one a player wrote. */
    public static boolean isPlayers(final IOsHost machine) {
        final OsDef os = machine.installedOs();
        return os != null && PlayerSystems.os(os.id()) != null && machine.systemRun() != null;
    }

    /** Boots the system as process 1, told the machine is coming up; one that cannot start comes down on its page. */
    public static void start(final IOsHost machine) {
        final SystemRun run = machine.systemRun();
        final MachinePrograms programs = machine.programs();
        final OsDef os = machine.installedOs();
        if (run == null || programs == null || os == null || !(machine instanceof BlockEntity block)) {
            return;
        }
        run.reset();
        final PlayerSystem system = PlayerSystems.withFiles(os.id());
        // From the disk's root: a POSIX prompt starts in its home, and the system's folder is not there.
        final String listing = "/" + (system == null ? "" : system.folder() + "/") + PlayerSystem.LISTING;
        final FileService files = machine.services() == null ? null : machine.services().files();
        final ICliComputer.FsResult read = files == null ? null : files.readFile(listing);
        if (read == null || !read.ok()) {
            panic(machine, os, WENT_DOWN.with(nameOf(os)).english(), List.of(NO_LISTING.with(listing).english()));
            return;
        }
        final int heapMb = (int) Math.max(1, Math.min(Integer.MAX_VALUE, machine.ramTotalMb() / 4));
        final MachinePrograms.Started started = programs.boot(os.displayName(), read.message().english(), heapMb,
                block);
        if (!started.ok()) {
            panic(machine, os, WENT_DOWN.with(nameOf(os)).english(), List.of(started.message()));
        }
    }

    /**
     * As the system comes up: the services it registered start, in the order it registered them, and the desktop it
     * started comes up with it, or its prompt or its own screen when it started none.
     */
    public static void cameUp(final IOsHost machine) {
        final SystemRun run = machine.systemRun();
        if (!isPlayers(machine) || machine.services() == null) {
            return;
        }
        machine.services().system().startRegistered();
        machine.setBootedDesktopId(run.desktop());
    }

    /**
     * How the machine hands its credits out to its programs: as the kernel of the player's system it runs says, the
     * system choosing each slice when it answers {@code OnSchedule}; as the mod's systems do on any other machine.
     */
    public static SchedulePolicy policy(final IOsHost machine) {
        final OsDef os = machine.installedOs();
        final PlayerSystem system = os == null ? null : PlayerSystems.withFiles(os.id());
        if (system == null || !system.facts().kernel().own()) {
            return SchedulePolicy.MOD;
        }
        final MachinePrograms programs = machine.programs();
        final SigmaProgram running = programs == null ? null : programs.system();
        return SchedulePolicy.of(system.facts().kernel(),
                running != null && running.answers(SchedulePolicy.ON_SCHEDULE, 1));
    }

    /** Whether the system said it is up, which ends its coming up. */
    public static boolean done(final IOsHost machine) {
        final SystemRun run = machine.systemRun();
        return run != null && run.done();
    }

    /**
     * One tick of a machine running a player's system: a system that faulted or ended comes down on its page, and the
     * picture of it coming up goes to whoever watches when it has changed.
     */
    public static void tick(final IOsHost machine, final ServerLevel level, final BlockPos pos) {
        if (!machine.isRunning() || !isPlayers(machine)) {
            return;
        }
        final SystemRun run = machine.systemRun();
        final OsDef os = machine.installedOs();
        final MachinePrograms programs = machine.programs();
        final SigmaProgram system = programs == null ? null : programs.system();
        if (system != null && run.halted().isEmpty() && (system.state() == ILanguageProcess.State.HALTED
                || system.state() == ILanguageProcess.State.FINISHED)) {
            final boolean faulted = system.state() == ILanguageProcess.State.HALTED;
            panic(machine, os, WENT_DOWN.with(nameOf(os)).english(),
                    List.of(faulted ? system.message().english() : ENDED.english()));
            return;
        }
        if (run.takePictureChange() && !run.done()) {
            final PlayerBoot picture = pictureOf(pos, os, run);
            ScreenSessions.eachWatcher(level, pos, (player, monitor) ->
                    PacketDistributor.sendToPlayer(player, picture));
        }
        if (run.takeScreenChange()) {
            final PlayerScreenPayloads.Screen screen = PlayerScreenPayloads.screenOf(pos, run);
            ScreenSessions.eachWatcher(level, pos, (player, monitor) ->
                    PacketDistributor.sendToPlayer(player, screen));
        }
    }

    /** The picture of the system coming up on that machine, as its calls set it. */
    public static PlayerBoot pictureOf(final BlockPos pos, final OsDef os, final SystemRun run) {
        return new PlayerBoot(pos, os.displayName(), run.logo(), run.line(), run.progress(), run.background(),
                run.text());
    }

    /**
     * Brings the machine down on the system's own page, with {@code message} at the top and {@code trace} under it;
     * the process is stopped, and the page stands for the reboot the system asked for.
     */
    public static void panic(final IOsHost machine, @Nullable final OsDef os, final String message,
                             final List<String> trace) {
        final SystemRun run = machine.systemRun();
        if (run == null || os == null) {
            return;
        }
        run.halt(message, trace);
        final MachinePrograms programs = machine.programs();
        final SigmaProgram system = programs == null ? null : programs.system();
        final int heldMb = system == null ? 0 : (int) (system.heldBytes() >> 20);
        if (programs != null) {
            programs.stop(MachinePrograms.SYSTEM_PID);
        }
        final PlayerSystem built = PlayerSystems.withFiles(os.id());
        final String author = built == null ? os.house().name() : built.facts().author();
        machine.panic(CrashPage.of(new PlayerPanic(os.displayName(), author, message, trace,
                MachinePrograms.SYSTEM_PID, Math.max(heldMb, os.ramMb()), run.background(), run.text(), run.logo(),
                run.rebootAfter())));
    }

    /* The system's name without its version, as its page names it: "Lantern OS". */
    private static String nameOf(final OsDef os) {
        final PlayerSystem built = PlayerSystems.withFiles(os.id());
        return built == null ? os.displayName() : built.facts().name();
    }
}
