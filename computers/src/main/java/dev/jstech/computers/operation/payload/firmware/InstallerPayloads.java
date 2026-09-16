/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.InstallerActionPayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The installer's payloads: the page a machine is on, and the answers a player gives it.
 *
 * <p>Every answer is applied on the machine and the whole page is sent back, so two players at two monitors on
 * the same computer are looking at one installer rather than at two that happen to agree.
 */
public final class InstallerPayloads {

    private InstallerPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        registrar.playToClient(OpenInstallerPayload.TYPE, OpenInstallerPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) ->
                        dev.jstech.computers.block.IInstallerScreenOpener.Holder.open(payload)));
        // The installer is a plain screen, like the setup and the self-test, so it answers to the screen gate.
        ComputerAccess.accept(registrar, InstallerActionPayload.TYPE, InstallerActionPayload.STREAM_CODEC,
                ComputerAccess.screen(InstallerActionPayload::hostPos), InstallerPayloads::handleAction);
    }

    private static void handleAction(final InstallerActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof AbstractComputerBlockEntity machine)) {
            return;
        }
        final InstallerFlow flow = machine.installer();
        if (flow == null) {
            return;
        }
        /*
         * Whether the work had begun before this answer is the whole test for the one destructive act: a disk
         * agreed to be erased is erased when the copying starts and not a moment sooner, so a player who
         * changes their mind on the pages before it still walks away with everything they had.
         */
        final boolean wasStarted = flow.started();
        switch (payload.action()) {
            case InstallerActionPayload.ACTION_NEXT -> flow.next();
            case InstallerActionPayload.ACTION_BACK -> flow.back();
            case InstallerActionPayload.ACTION_SELECT_DISK -> flow.select(payload.value());
            case InstallerActionPayload.ACTION_NAME -> flow.setComputerName(payload.text());
            case InstallerActionPayload.ACTION_DESKTOP -> {
                flow.chooseDesktop(payload.value());
                resize(machine, flow);
            }
            case InstallerActionPayload.ACTION_ERASE -> {
                flow.askErase(payload.value());
                flow.confirmErase();
            }
            case InstallerActionPayload.ACTION_GO_TO -> flow.goToStage(payload.value());
            case InstallerActionPayload.ACTION_QUIT -> {
                quit(machine, player, level, payload);
                return;
            }
            case InstallerActionPayload.ACTION_REBOOT -> {
                reboot(machine, player, level, payload, flow);
                return;
            }
            default -> {
                return;
            }
        }
        if (!wasStarted && flow.started() && flow.eraseSlot() != InstallerFlow.NO_DISK) {
            machine.formatDisk(flow.eraseSlot());
        }
        machine.setChanged();
        OsInstallRunner.show(level, machine, payload.hostPos(), flow);
    }

    /**
     * A desktop chosen from the Mirror makes the whole installation longer, so the clock is cut again to match.
     *
     * <p>Only ever while nothing has been done: every installer that offers a desktop asks for it before it
     * copies anything, which is what makes this safe rather than a clock that jumps under a running bar.
     */
    private static void resize(final AbstractComputerBlockEntity machine, final InstallerFlow flow) {
        final OsInstallJob job = machine.installing();
        if (job == null || job.ticksLeft() != job.ticksTotal()) {
            return;
        }
        machine.setInstalling(new OsInstallJob(job.osId(), flow.targetSlot(), job.readerPos(),
                flow.ticksTotal(), flow.ticksTotal()));
    }

    /** Leaving with nothing written, which the pages before the work allow and the work itself does not. */
    private static void quit(final AbstractComputerBlockEntity machine, final ServerPlayer player,
                             final ServerLevel level, final InstallerActionPayload payload) {
        final InstallerFlow flow = machine.installer();
        if (flow == null || !flow.quittable()) {
            return;
        }
        machine.setInstaller(null);
        machine.setInstalling(null);
        player.closeContainer();
        dev.jstech.computers.block.MonitorBlock.openFirmware(player, level, payload.monitorPos(),
                payload.hostPos());
    }

    /** The last page's one action: restart into the system that was just written. */
    private static void reboot(final AbstractComputerBlockEntity machine, final ServerPlayer player,
                               final ServerLevel level, final InstallerActionPayload payload,
                               final InstallerFlow flow) {
        final int slot = flow.targetSlot();
        machine.setInstaller(null);
        machine.setInstalling(null);
        machine.setPendingInstallSlot(dev.jstech.computers.os.IOsHost.NO_PENDING_INSTALL);
        if (slot >= 0) {
            machine.setBootDiskSlot(slot);
        }
        machine.setNeedsPost(true);
        dev.jstech.computers.block.MonitorBlock.openBootTarget(player, level, payload.monitorPos(),
                payload.hostPos());
    }
}
