/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.computers.operation.payload.OpenBootMenuPayload;
import dev.jstech.computers.operation.payload.OpenComputerUiPayload;
import dev.jstech.computers.operation.payload.OpenInstallDonePayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.OsInstallProgressPayload;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.boot.BootIdentity;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootSplash;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine shows as each of its sessions opens: the self-test, the boot manager, a system coming up or going
 * down, the copy of an install, an installer's page, its last page, the firmware setup. One answer for the two that
 * ask: a player opening the monitor is sent it ahead of the session, and the monitor's face in the world is drawn
 * from it by the very screen that session opens, so the two never show different things.
 */
public final class SessionOpenings {

    private SessionOpenings() {
    }

    /** The self-test of the machine at {@code owner}, at the point it has reached, as shown on {@code monitorPos}. */
    public static OpenPostPayload post(final Level level, final BlockPos monitorPos, final BlockPos owner,
                                       @Nullable final IOsHost machine) {
        final String name = level.getBlockState(owner).getBlock().getName().getString();
        final HardwareEra era = machine == null ? null : machine.displayEra();
        final FirmwareKind kind = FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD);
        final int remaining = machine == null ? 0 : machine.postRemaining();
        // A machine standing at a failed self-test opens at the end of it, not at the start of another one.
        final boolean halted = machine != null && machine.haltedAtPost();
        /*
         * What the machine found wrong with its own disk, when that is why it stopped: a system whose loader has been
         * deleted is found and will not start, and the screen says which file it wanted.
         */
        final Text complaint = machine == null ? Text.EMPTY : SystemIntegrity.check(machine).complaint();
        return new OpenPostPayload(owner, monitorPos, kind.id(), name, remaining, halted, complaint);
    }

    /** The boot manager the machine is standing at, with what is left of its wait. */
    public static OpenBootMenuPayload bootMenu(final BlockPos monitorPos, final BlockPos owner,
                                               final IOsHost computer) {
        return new OpenBootMenuPayload(owner, monitorPos, BootLines.menuFor(computer, computer.menuRemaining()),
                computer.menuRemaining());
    }

    /** The system the machine is bringing up, at the point it has reached. */
    public static OpenSystemBootPayload systemBoot(final BlockPos monitorPos, final BlockPos owner,
                                                   final IOsHost computer) {
        return new OpenSystemBootPayload(owner, monitorPos, computer.bootRemaining(), computer.bootTotal(),
                computer.bootSequence(), false, splashOf(computer), identityOf(computer));
    }

    /**
     * The system the machine is closing down, at the point it has reached: on its way off the system says so and the
     * screen ends dark; on its way round it says it is restarting.
     */
    public static OpenSystemBootPayload systemDown(final BlockPos monitorPos, final BlockPos owner,
                                                   final IOsHost computer) {
        final boolean off = computer.poweringOff();
        return new OpenSystemBootPayload(owner, monitorPos, computer.downRemaining(), computer.downTotal(),
                BootLines.shutdownFor(computer, !off), off, splashOf(computer), identityOf(computer).goingDown());
    }

    /**
     * The install the machine is in the middle of: the installer's page it has reached, with the work it has done
     * behind it, or the copy alone where no installer runs it; null when it is installing nothing.
     */
    @Nullable
    public static CustomPacketPayload installProgress(final BlockPos monitorPos, final BlockPos owner,
                                                      final IOsHost computer) {
        final InstallerFlow flow = computer.installer();
        final OsInstallJob job = computer.installing();
        if (flow != null) {
            final int done = job == null ? flow.ticksTotal() : job.ticksTotal() - job.ticksLeft();
            return OpenInstallerPayload.of(owner, monitorPos, flow, done);
        }
        if (job == null) {
            return null;
        }
        final OsDef os = OsRegistry.getOs(ResourceLocation.tryParse(job.osId()));
        return new OsInstallProgressPayload(owner, monitorPos, firmwareOf(computer).id(),
                os != null ? os.displayName() : job.osId(), OsInstallRunner.targetLabel(job.targetSlot()),
                job.ticksLeft(), job.ticksTotal());
    }

    /** The finished installer's last page, the restart into the system it just put on the disk. */
    public static OpenInstallDonePayload installDone(final BlockPos monitorPos, final BlockPos owner,
                                                     final IOsHost computer) {
        final int slot = computer.pendingInstallSlot();
        final ResourceLocation osId = slot < 0 ? computer.installedOsId() : OsDisks.systemOn(computer.diskInSlot(slot));
        final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
        return new OpenInstallDonePayload(owner, monitorPos, firmwareOf(computer).id(),
                os != null ? os.displayName() : "", OsInstallRunner.targetLabel(slot), slot, Text.EMPTY);
    }

    /** The machine's firmware setup, in the firmware its era wears. */
    public static OpenComputerUiPayload firmware(final Level level, final BlockPos monitorPos, final BlockPos owner,
                                                 @Nullable final IOsHost computer) {
        final String name = level.getBlockState(owner).getBlock().getName().getString();
        final HardwareEra era = computer == null ? null : computer.displayEra();
        return new OpenComputerUiPayload(owner, monitorPos,
                FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD).id(), name);
    }

    /**
     * Who is coming up: the desktop, the system by the name it prints of itself, and the machine's host name, which a
     * desktop's own loading screen may name.
     *
     * <p>The desktop is what the machine booted with rather than what is installed on it: a desktop added a moment ago
     * waits for a restart, so the screen that shows a desktop coming up has to show the one that really is.
     */
    public static BootIdentity identityOf(final IOsHost computer) {
        final ResourceLocation desktop = computer.bootedDesktopId();
        final OsDef system = computer.installedOs();
        return new BootIdentity(desktop == null ? "" : desktop.getPath(),
                system == null ? "" : system.displayName(), Installers.hostName(computer),
                computer.console() == null ? "" : computer.console().desktop().cdeStyle().encoded());
    }

    /** The picture that machine's system comes up behind, or the plain one when it has no system. */
    public static BootSplash splashOf(final IOsHost computer) {
        final OsDef system = computer.installedOs();
        return system == null ? BootSplash.PLAIN : BootSplash.of(system.platform(), system.familyRank());
    }

    private static FirmwareKind firmwareOf(final IOsHost computer) {
        final HardwareEra era = computer.displayEra();
        return FirmwareKind.forEra(era != null ? era : HardwareEra.STANDARD);
    }
}
