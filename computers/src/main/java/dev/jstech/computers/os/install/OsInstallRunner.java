/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
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
 */
public final class OsInstallRunner {

    private OsInstallRunner() {
    }

    /** One tick of whatever copy this machine is doing, if it is doing one. */
    public static void tick(final AbstractComputerBlockEntity machine, final ServerLevel level, final BlockPos pos) {
        final OsInstallJob job = machine.installing();
        if (job == null) {
            return;
        }
        final OsDef system = OsRegistry.getOs(ResourceLocation.tryParse(job.osId()));
        if (mediumGone(level, job)) {
            machine.setInstalling(null);
            tell(level, machine, pos, job, system, "The medium was taken out before the system was written.");
            return;
        }
        if (!job.tick()) {
            machine.setChanged();
            return;
        }
        machine.setInstalling(null);
        final ResourceLocation id = ResourceLocation.tryParse(job.osId());
        if (id != null && machine.installOs(id, job.targetSlot())) {
            /*
             * The files are on the disk, but the machine is still running the installer until it restarts:
             * remember that, so the monitor comes back to the reboot prompt rather than to the system.
             */
            machine.setPendingInstallSlot(job.targetSlot());
            tell(level, machine, pos, job, system, "");
            return;
        }
        tell(level, machine, pos, job, system, "The disk would not take the system.");
    }

    /** Whether what was being read has left the drive it was in. */
    private static boolean mediumGone(final ServerLevel level, final OsInstallJob job) {
        if (job.readerPos() < 0) {
            return false;
        }
        return !(level.getBlockEntity(BlockPos.of(job.readerPos())) instanceof MediaReaderBlockEntity reader)
                || reader.insertedKind() != MediaKind.OS_INSTALL
                || reader.insertedPayload() == null
                || !reader.insertedPayload().toString().equals(job.osId());
    }

    /** Puts every player watching this machine on the installer's last beat, finished or refused. */
    private static void tell(final ServerLevel level, final AbstractComputerBlockEntity machine, final BlockPos pos,
                             final OsInstallJob job, @Nullable final OsDef system, final String failure) {
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
