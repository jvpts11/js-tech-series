/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.backup;

import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

import dev.jstech.computers.backup.BackupArchive;
import dev.jstech.computers.backup.BackupJob;
import dev.jstech.computers.backup.BackupJobs;
import dev.jstech.computers.backup.BackupPart;
import dev.jstech.computers.backup.BackupSchedule;
import dev.jstech.computers.backup.BackupService;
import dev.jstech.computers.backup.BackupTexts;
import dev.jstech.computers.backup.TapeContents;
import dev.jstech.computers.backup.TapeLibrary;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.client.os.BackupConsoleApp;
import dev.jstech.computers.operation.payload.BackupActionPayload;
import dev.jstech.computers.operation.payload.BackupStatePayload;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Backup Console's conversation with the network's Backup Service: every action the console sends is carried out
 * on the service and answered with the console's whole state, as the Messenger's are (computers/docs/design/
 * programs.md). The service lives on a server in a rack; the console only sets its jobs and restores its backups.
 */
public final class BackupPayloads {

    private BackupPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, BackupActionPayload.TYPE, BackupActionPayload.STREAM_CODEC,
                ComputerAccess.machine(BackupActionPayload::hostPos), BackupPayloads::handle);
        registrar.playToClient(BackupStatePayload.TYPE, BackupStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((state, player) -> BackupConsoleApp.accept(state)));
    }

    /**
     * What {@code player} asked of the Backup Service from the console at {@code payload}'s computer, carried out, and
     * the console's state sent back with how it went.
     */
    public static void handle(final BackupActionPayload payload, final ServerPlayer player, final ServerLevel level) {
        final IComputerTerminalHost terminal = niHost(player, level, payload.hostPos(), payload.monitorPos());
        final NetworkUuid network = terminal == null ? null : terminal.networkUuid();
        final ServerServices.Host host = BackupService.serviceOf(level, network);
        Text note = Text.EMPTY;
        if (network != null && host != null) {
            note = switch (payload.action()) {
                case BackupActionPayload.RUN -> BackupService.run(level, network, host, payload.job()).said();
                case BackupActionPayload.SAVE -> save(level, network, host, payload);
                case BackupActionPayload.DELETE -> delete(level, network, host, payload.job());
                case BackupActionPayload.RESTORE -> BackupService.restore(level, network, payload.backup(),
                        BackupPart.of(payload.parts()), Set.copyOf(payload.images()));
                case BackupActionPayload.LABEL, BackupActionPayload.ERASE, BackupActionPayload.EJECT ->
                        tape(level, network, payload);
                default -> Text.EMPTY;
            };
        }
        PacketDistributor.sendToPlayer(player,
                BackupConsoleState.of(level, network, host, payload.target(), payload.backup(), note));
    }

    /*
     * A job made or changed: its name one word, something chosen to keep, and its Automation Engine job brought in
     * line, so a timed one fires at its hour and one made by hand fires never.
     */
    private static Text save(final ServerLevel level, final NetworkUuid network, final ServerServices.Host host,
                             final BackupActionPayload payload) {
        final String name = payload.job().strip();
        if (!BackupJob.validName(name)) {
            return BackupTexts.BAD_NAME.with(BackupJob.MOST_NAME);
        }
        final Set<BackupPart> parts = BackupPart.of(payload.parts());
        if (parts.isEmpty()) {
            return BackupTexts.NOTHING_CHOSEN.text();
        }
        final BackupJobs jobs = host.rack().backupsAt(host.slot());
        if (jobs == null) {
            return BackupTexts.NO_SERVICE.text();
        }
        final BackupJob before = payload.was().isEmpty() ? null : jobs.named(payload.was());
        final BackupJob taken = jobs.named(name);
        if (taken != null && taken != before) {
            return BackupTexts.NAME_TAKEN.with(name);
        }
        final BackupJob after = new BackupJob(name, parts, payload.target(), payload.when(),
                before == null ? -1L : before.lastRun(), before == null ? BackupJob.RunState.NEVER : before.result());
        if (before != null) {
            jobs.remove(before.name());
        }
        if (!jobs.put(after)) {
            return BackupTexts.FULL.with(BackupJobs.MOST);
        }
        host.changed();
        final boolean engine = BackupSchedule.sync(level, network, before, after);
        return after.when().timed() && !engine ? BackupTexts.NO_ENGINE.with(name) : BackupTexts.SAVED.with(name);
    }

    private static Text delete(final ServerLevel level, final NetworkUuid network, final ServerServices.Host host,
                               final String name) {
        final BackupJobs jobs = host.rack().backupsAt(host.slot());
        final BackupJob job = jobs == null ? null : jobs.named(name);
        if (job == null) {
            return BackupTexts.NO_JOB.with(name);
        }
        jobs.remove(job.name());
        host.changed();
        BackupSchedule.sync(level, network, job, null);
        return BackupTexts.DELETED.with(job.name());
    }

    /*
     * A tape labelled, erased or taken out of its library. The tape in the drive is left alone while the drive works
     * on it; an erased tape lets the backups on it go, and an ejected one drops out in front of its rack.
     */
    private static Text tape(final ServerLevel level, final NetworkUuid network, final BackupActionPayload payload) {
        final TapeLibrary library = TapeLibrary.find(level, network, payload.target().rack(), payload.target().row());
        final int n = payload.tape();
        if (library == null || library.tape(n).isEmpty()) {
            return BackupTexts.NO_TAPE.with(n + 1);
        }
        if (library.drive().busy() && library.drive().tape() == n) {
            return BackupTexts.TAPE_IN_DRIVE.with(n + 1);
        }
        final TapeContents on = library.contents(n);
        return switch (payload.action()) {
            case BackupActionPayload.LABEL -> {
                final String label = payload.text().strip();
                library.write(n, on.labelled(label));
                yield BackupTexts.LABELLED.with(n + 1, label);
            }
            case BackupActionPayload.ERASE -> {
                final BackupArchive archive = BackupArchive.of(level);
                for (final TapeContents.Spot spot : on.spots()) {
                    archive.remove(spot.backup());
                }
                library.write(n, on.erased());
                yield BackupTexts.ERASED.with(n + 1);
            }
            default -> {
                final ItemStack taken = library.take(n);
                Block.popResource(level, library.rack().getBlockPos(), taken);
                yield BackupTexts.EJECTED.with(n + 1);
            }
        };
    }
}
