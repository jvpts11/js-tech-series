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
import dev.jstech.computers.operation.payload.DesktopBalloonPayload;
import dev.jstech.computers.operation.payload.InstallerActionPayload;
import dev.jstech.computers.operation.payload.LiveInstallPayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.os.LiveOverlay;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.core.text.Text;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
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
        // The live desktop's Install Linux Mint, which puts the installer over the live session.
        ComputerAccess.accept(registrar, LiveInstallPayload.TYPE, LiveInstallPayload.STREAM_CODEC,
                ComputerAccess.machine(LiveInstallPayload::hostPos), InstallerPayloads::handleLiveInstall);
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

    /**
     * Takes OpenBSD's answers (X, xenodm and the sets) and re-quotes the copy to the sets that are left, exactly what
     * a real {@link InstallerActionPayload#ACTION_OPTIONS} answer does. Exposed for the same reason as
     * {@link #selectDiskAndResize}.
     */
    public static void answerOptionsAndResize(final IOsHost machine, final InstallerFlow flow, final int options) {
        flow.setOptions(options);
        resize(machine, flow);
    }

    /**
     * Starts the system's installer over a live session, from the medium the machine started from, and puts the one
     * who asked in front of it; one already under way is simply shown again. A machine that cannot take the system
     * says why on its desktop, in the corner, the way it says anything else.
     */
    private static void handleLiveInstall(final LiveInstallPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost machine) || !machine.liveSession()) {
            return;
        }
        if (machine.installer() == null) {
            // A live session the network brought copies from the Mirror it came from; one off a medium, from that.
            final OsDef live = machine.installedOs();
            final Text failure = machine.liveOverNetwork() && live != null
                    ? NetworkBoot.install(level, machine, live, -1)
                    : FirmwarePayloads.beginInstall(level, machine, FirmwarePayloads.ANY_READER, -1);
            if (failure != null) {
                final ProgramSpec installer = OsRegistry.getProgram(LiveOverlay.INSTALLER);
                PacketDistributor.sendToPlayer(player, new DesktopBalloonPayload(payload.hostPos(),
                        installer == null ? Text.EMPTY : installer.name().text(), failure, ""));
                return;
            }
        }
        MonitorBlock.openSession(player, level, payload.monitorPos(), payload.hostPos());
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
         * Nothing here erases a disk agreed to be erased: the copy does that as it starts and not a moment sooner
         * (OsInstallRunner), so a player who changes their mind on the pages before it walks away with everything.
         * Only the disk step's own changes to a table, each confirmed on the screen, are written at once.
         */
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
            case InstallerActionPayload.ACTION_OPTIONS -> answerOptionsAndResize(machine, flow, payload.value());
            case InstallerActionPayload.ACTION_HALT -> {
                halt(machine, player, flow);
                return;
            }
            case InstallerActionPayload.ACTION_CONTINUE_TESTING -> {
                continueTesting(machine, player, level, payload, flow);
                return;
            }
            case InstallerActionPayload.ACTION_PLACE -> {
                flow.select(payload.value());
                flow.place(payload.amount());
                resize(machine, flow);
            }
            case InstallerActionPayload.ACTION_FORMAT_AS -> flow.formatAs(payload.text());
            case InstallerActionPayload.ACTION_SHRINK -> flow.shrinkTo(payload.amount());
            case InstallerActionPayload.ACTION_MIRROR_DISK -> flow.mirrorOn(payload.value());
            case InstallerActionPayload.ACTION_DELETE_PARTITION ->
                    Installers.deletePartition(machine, flow, payload.value(), (int) payload.amount());
            case InstallerActionPayload.ACTION_NEW_PARTITION ->
                    Installers.newPartition(machine, flow, payload.value(), payload.amount());
            case InstallerActionPayload.ACTION_FORMAT_PARTITION ->
                    Installers.formatPartition(machine, flow, payload.value(), (int) payload.amount());
            case InstallerActionPayload.ACTION_EXTEND_PARTITION ->
                    Installers.extendPartition(machine, flow, payload.value(), (int) payload.amount());
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
        final OsInstallJob resized = new OsInstallJob(job.osId(), flow.targetSlot(), job.readerPos(),
                job.readerBay(), newTotal, Math.max(0, newTotal - done));
        // A copy begun from an installer found in the world is still that copy, re-timed.
        if (job.found()) {
            resized.markFound();
        }
        machine.setInstalling(resized);
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
        // Quitting an installer that ran on a live desktop goes back to that desktop, which is still running.
        if (machine.liveSession()) {
            MonitorBlock.openSession(player, level, payload.monitorPos(), payload.hostPos());
            return;
        }
        player.closeContainer();
        MonitorBlock.openFirmware(player, level, payload.monitorPos(),
                payload.hostPos());
    }

    /**
     * OpenBSD's other way out of its last page: halt, which leaves the system written and the machine switched off;
     * the next start boots what was installed.
     */
    private static void halt(final IOsHost machine, final ServerPlayer player, final InstallerFlow flow) {
        if (flow.page() != InstallerPage.DONE) {
            return;
        }
        finishOn(machine, flow);
        player.closeContainer();
        machine.setPowered(false);
    }

    /**
     * Linux Mint's Continue Testing: back to its live desktop, the system written and waiting on the disk for the
     * restart that boots it.
     */
    private static void continueTesting(final IOsHost machine, final ServerPlayer player, final ServerLevel level,
                                        final InstallerActionPayload payload, final InstallerFlow flow) {
        if (flow.page() != InstallerPage.DONE || !machine.liveSession()) {
            return;
        }
        finishOn(machine, flow);
        MonitorBlock.openSession(player, level, payload.monitorPos(), payload.hostPos());
    }

    /* What ending the installer leaves: no installer, the copy done, and the disk it wrote booted first. */
    private static void finishOn(final IOsHost machine, final InstallerFlow flow) {
        final int slot = flow.targetSlot();
        machine.setInstaller(null);
        machine.setInstalling(null);
        machine.setPendingInstallSlot(IOsHost.NO_PENDING_INSTALL);
        if (slot >= 0) {
            machine.setBootDiskSlot(slot);
        }
    }

    /** The last page's one action: restart into the system that was just written. */
    private static void reboot(final IOsHost machine, final ServerPlayer player,
                               final ServerLevel level, final InstallerActionPayload payload,
                               final InstallerFlow flow) {
        // Only from the last page, as halting: mid-copy it would boot a disk written half way.
        if (flow.page() != InstallerPage.DONE) {
            return;
        }
        finishOn(machine, flow);
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
