/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.operation.payload.OpenInstallDonePayload;
import dev.jstech.computers.operation.payload.ScreenSessions;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Carries a system being copied onto a disk along, one tick at a time, and writes it when the copy ends.
 *
 * <p>The copy belongs to the machine, the way a program's setup does ({@link SetupRunner} beside this). Nobody
 * has to be watching for it to finish, closing the monitor does not throw it away, and it is saved with the
 * machine, so one that was under way when the world went away carries on where it was. Two things end it early
 * and write nothing: the power going off, which drops it with the rest of the session, and the medium leaving
 * the drive it was being read from.
 *
 * <p>How far it may run is the installer's business, not the clock's: the work goes to the end of the page it is
 * on and waits there, so a question nobody has answered is never copied past.
 */
public final class OsInstallRunner {

    private OsInstallRunner() {
    }

    /** One tick of whatever copy this machine is doing, if it is doing one. */
    public static void tick(final IOsHost machine, final ServerLevel level, final BlockPos pos) {
        final OsInstallJob job = machine.installing();
        if (job == null) {
            return;
        }
        final OsDef system = OsRegistry.getOs(ResourceLocation.tryParse(job.osId()));
        if (mediumGone(level, job)) {
            machine.setInstalling(null);
            machine.setInstaller(null);
            tell(level, machine, pos, job, system, "The medium was taken out before the system was written.");
            return;
        }
        final InstallerFlow flow = machine.installer();
        if (flow != null && !carry(machine, level, pos, job, flow)) {
            return;
        }
        if (flow == null && !job.tick()) {
            machine.markChanged();
            return;
        }
        finish(machine, level, pos, job, flow, system);
    }

    /**
     * One tick of the work under an installer, answering whether the whole of it is now done.
     *
     * <p>The clock only runs inside what the page it is on has unlocked; when that page's steps are over, the
     * installer moves on by itself unless the next page is one that asks something.
     *
     * <p>A screen watching this is not told every tick: it holds the same installer and walks the same clock,
     * and is only sent the page again when the page really changes, which is the one thing it cannot work out.
     */
    private static boolean carry(final IOsHost machine, final ServerLevel level,
                                 final BlockPos pos, final OsInstallJob job, final InstallerFlow flow) {
        if (job.ticksTotal() - job.ticksLeft() < flow.ticksUnlocked()) {
            job.tick();
            machine.markChanged();
        }
        if (flow.advance(job.ticksTotal() - job.ticksLeft()) && !job.finished()) {
            show(level, machine, pos, flow);
        }
        return job.finished();
    }

    /** Writes the system, gives the machine the name it was asked for, and leaves the installer on its last page. */
    private static void finish(final IOsHost machine, final ServerLevel level,
                               final BlockPos pos, final OsInstallJob job, @Nullable final InstallerFlow flow,
                               @Nullable final OsDef system) {
        machine.setInstalling(null);
        final ResourceLocation id = ResourceLocation.tryParse(job.osId());
        final int slot = flow != null ? flow.targetSlot() : job.targetSlot();
        if (id == null || !machine.installOs(id, slot)) {
            machine.setInstaller(null);
            tell(level, machine, pos, job, system, "The disk would not take the system.");
            return;
        }
        /*
         * The files are on the disk, but the machine is still running the installer until it restarts:
         * remember that, so the monitor comes back to the reboot prompt rather than to the system.
         */
        machine.setPendingInstallSlot(slot);
        if (flow == null) {
            tell(level, machine, pos, job, system, "");
            return;
        }
        name(machine, flow);
        desktop(machine, flow);
        flow.goTo(InstallerPage.DONE);
        machine.markChanged();
        show(level, machine, pos, flow);
    }

    /** The name the installer asked for becomes the name the prompt and the network use. */
    private static void name(final IOsHost machine, final InstallerFlow flow) {
        if (machine.console() != null && !flow.computerName().isBlank()) {
            machine.console().setComputerName(flow.computerName());
        }
    }

    /** A desktop chosen from the Mirror is installed with the system, which is what its own step paid for. */
    private static void desktop(final IOsHost machine, final InstallerFlow flow) {
        final InstallerFlow.Desktop chosen = flow.desktop();
        if (chosen != null && machine.console() != null) {
            machine.console().install(chosen.id());
        }
    }

    /** Whether what was being read has left the drive it was in. */
    private static boolean mediumGone(final ServerLevel level, final OsInstallJob job) {
        if (!job.hasReader()) {
            return false;
        }
        return !(level.getBlockEntity(BlockPos.of(job.readerPos())) instanceof MediaReaderBlockEntity reader)
                || reader.insertedKind() != MediaKind.OS_INSTALL
                || reader.insertedPayload() == null
                || !reader.insertedPayload().toString().equals(job.osId());
    }

    /** Puts every player watching this machine on the installer's page, wherever it has got to. */
    public static void show(final ServerLevel level, final IOsHost machine, final BlockPos pos,
                            final InstallerFlow flow) {
        if (!machine.onScreen()) {
            /*
             * Every machine in a rack shares the rack's position and its monitor shows one of them at a time,
             * so a page for one of them must not land in front of somebody looking at another. The work goes
             * on regardless; whoever switches to this one is put where it has got to.
             */
            return;
        }
        final OsInstallJob job = machine.installing();
        final int done = job == null ? flow.ticksTotal() : job.ticksTotal() - job.ticksLeft();
        ScreenSessions.eachWatcher(level, pos, (player, monitor) -> {
            ScreenSessions.opened(player, monitor, pos);
            PacketDistributor.sendToPlayer(player,
                    dev.jstech.computers.operation.payload.OpenInstallerPayload.of(pos, monitor, flow, done));
        });
    }

    /** Puts every player watching this machine on the installer's last beat, finished or refused. */
    private static void tell(final ServerLevel level, final IOsHost machine, final BlockPos pos,
                             final OsInstallJob job, @Nullable final OsDef system, final String failure) {
        if (!machine.onScreen()) {
            return;
        }
        final HardwareEra era = machine.displayEra();
        final int kind = FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD).id();
        final String name = system != null ? system.displayName() : job.osId();
        final String target = job.targetSlot() < 0 ? "the default disk" : "Disk " + job.targetSlot();
        ScreenSessions.eachWatcher(level, pos, (player, monitor) -> {
            ScreenSessions.opened(player, monitor, pos);
            PacketDistributor.sendToPlayer(player, new OpenInstallDonePayload(pos, monitor, kind, name, target,
                    failure.isEmpty() ? job.targetSlot() : -1, failure));
        });
    }
}
