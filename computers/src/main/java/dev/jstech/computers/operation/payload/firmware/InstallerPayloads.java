/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.firmware;

import dev.jstech.computers.block.IInstallerScreenOpener;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.os.IOsHost;
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
                        IInstallerScreenOpener.Holder.open(payload)));
        // The installer is one of the monitor's own sessions, like the setup and the self-test.
        ComputerAccess.accept(registrar, InstallerActionPayload.TYPE, InstallerActionPayload.STREAM_CODEC,
                ComputerAccess.screenAt(InstallerActionPayload::hostPos, InstallerActionPayload::monitorPos,
                        MonitorSessionMenu.Phase.INSTALLER),
                InstallerPayloads::handleAction);
    }

    /**
     * Chooses the disk the system goes on and re-quotes the copy to it, exactly what a real
     * {@link InstallerActionPayload#ACTION_SELECT_DISK} answer does. Exposed so a test can drive the same update
     * a disk choice causes without sending a payload of its own.
     */
    public static void selectDiskAndResize(final IOsHost machine, final InstallerFlow flow, final int slot) {
        flow.select(slot);
        resize(machine, flow);
    }

    /**
     * Chooses the desktop that comes with the system (or drops it) and re-quotes the copy to match, exactly what
     * a real {@link InstallerActionPayload#ACTION_DESKTOP} answer does. Exposed for the same reason as
     * {@link #selectDiskAndResize}.
     */
    public static void chooseDesktopAndResize(final IOsHost machine, final InstallerFlow flow, final int index) {
        flow.chooseDesktop(index);
        resize(machine, flow);
    }

    private static void handleAction(final InstallerActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost machine)) {
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
            case InstallerActionPayload.ACTION_SELECT_DISK -> selectDiskAndResize(machine, flow, payload.value());
            case InstallerActionPayload.ACTION_NAME -> flow.setComputerName(payload.text());
            case InstallerActionPayload.ACTION_DESKTOP -> chooseDesktopAndResize(machine, flow, payload.value());
            case InstallerActionPayload.ACTION_ERASE -> {
                flow.askErase(payload.value());
                flow.confirmErase();
                resize(machine, flow);
            }
            case InstallerActionPayload.ACTION_CANCEL_ERASE -> {
                flow.cancelErase();
                resize(machine, flow);
            }
            case InstallerActionPayload.ACTION_PORTS -> {
                flow.setPortsSelected(payload.value() != 0);
                resize(machine, flow);
            }
            case InstallerActionPayload.ACTION_MIRROR -> {
                flow.setUseMirror(payload.value() != 0);
                resize(machine, flow);
            }
            case InstallerActionPayload.ACTION_CRON -> flow.setCronEnabled(payload.value() != 0);
            case InstallerActionPayload.ACTION_SSHD -> flow.setSshdEnabled(payload.value() != 0);
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
        machine.markChanged();
        OsInstallRunner.show(level, machine, payload.hostPos(), flow);
    }

    /**
     * Any answer that changes how long the copy takes cuts the clock again to match: a desktop chosen from the
     * Mirror or the Mirror turned down after one was chosen, and a disk chosen, erased or an erase taken back,
     * since a faster or slower disk changes the rate the whole copy runs at.
     *
     * <p>What has already run stays run: a job already under way is not assumed to still be at its first tick.
     * The ticks already spent are kept exactly as they were and the ticks still to come are worked out fresh from
     * the installer's new total, so a shorter choice cannot leave more ticks to run than the job now has, and a
     * longer one is not shortened by what had already gone before it. Before the copy starts, the job is simply
     * re-quoted at the new total with nothing done, since it was already quoted the moment the installer opened.
     */
    private static void resize(final IOsHost machine, final InstallerFlow flow) {
        final OsInstallJob job = machine.installing();
        if (job == null) {
            return;
        }
        final int done = job.ticksTotal() - job.ticksLeft();
        final int newTotal = flow.ticksTotal();
        // A disk of the same speed re-times to the same total but is still another disk: the job must follow it.
        if (newTotal == job.ticksTotal() && flow.targetSlot() == job.targetSlot()) {
            return;
        }
        machine.setInstalling(new OsInstallJob(job.osId(), flow.targetSlot(), job.readerPos(), newTotal,
                Math.max(0, newTotal - done)));
    }

    /** Leaving with nothing written, which the pages before the work allow and the work itself does not. */
    private static void quit(final IOsHost machine, final ServerPlayer player,
                             final ServerLevel level, final InstallerActionPayload payload) {
        final InstallerFlow flow = machine.installer();
        if (flow == null || !flow.quittable()) {
            return;
        }
        machine.setInstaller(null);
        machine.setInstalling(null);
        player.closeContainer();
        MonitorBlock.openFirmware(player, level, payload.monitorPos(),
                payload.hostPos());
    }

    /** The last page's one action: restart into the system that was just written. */
    private static void reboot(final IOsHost machine, final ServerPlayer player,
                               final ServerLevel level, final InstallerActionPayload payload,
                               final InstallerFlow flow) {
        final int slot = flow.targetSlot();
        machine.setInstaller(null);
        machine.setInstalling(null);
        machine.setPendingInstallSlot(IOsHost.NO_PENDING_INSTALL);
        if (slot >= 0) {
            machine.setBootDiskSlot(slot);
        }
        machine.setNeedsPost(true);
        /*
         * A restart is a restart: the machine tests itself again and then brings the system up from the
         * beginning, exactly as a power-on does. Going straight to the boot target skipped the self-test
         * altogether, so the one moment a player most expects to see a machine start over was the one moment
         * it did not.
         */
        MonitorBlock.openSession(player, level, payload.monitorPos(), payload.hostPos());
    }
}
