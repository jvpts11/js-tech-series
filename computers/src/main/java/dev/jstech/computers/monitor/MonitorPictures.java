/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.SshTerminal;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Works out what a monitor's face shows, from what its machine holds: the server's half of the screen in the world.
 *
 * <p>Only reads: unlike opening a session on the monitor, describing it never settles anything about the machine, so
 * a monitor looked at from across the room cannot change what happens when somebody sits down at it.
 */
public final class MonitorPictures {

    private MonitorPictures() {
    }

    /** What the face of {@code monitor} shows right now. */
    public static IMonitorPicture describe(final ServerLevel level, final MonitorBlockEntity monitor) {
        final BlockPos owner = monitor.remoteSession() != null ? monitor.remoteSession() : monitor.ownerPos();
        if (!monitor.lit() || owner == null) {
            return IMonitorPicture.DARK;
        }
        final BlockEntity be = Loaded.blockEntity(level, owner);
        if (!(be instanceof IOsHost host) || !host.isRunning()) {
            return IMonitorPicture.DARK;
        }
        final HardwareEra era = host.displayEra() == null ? HardwareEra.STANDARD : host.displayEra();
        if (host.needsPost() || host.haltedAtPost()) {
            return selfTest(level, owner, host, era);
        }
        if (host.goingDown()) {
            return steps(era, BootLines.shutdownFor(host, !host.poweringOff()), host.downTotal() - host.downRemaining(),
                    host.downTotal());
        }
        final OsInstallJob job = host.installing();
        if (job != null) {
            return copying(era, job);
        }
        if (host.installer() != null) {
            return new IMonitorPicture.Lines(era, MonitorPictureTexts.INSTALLER.text(), List.of());
        }
        if (host.atBootMenu()) {
            return menu(era, BootLines.menuFor(host, host.menuRemaining()));
        }
        if (host.booting()) {
            return steps(era, host.bootSequence(), host.bootTotal() - host.bootRemaining(), host.bootTotal());
        }
        return switch (BootController.targetForComputer(be)) {
            case FIRMWARE -> new IMonitorPicture.Lines(era, MonitorPictureTexts.FIRMWARE.text(),
                    List.of(IMonitorPicture.Line.of(machineName(level, owner), IMonitorPicture.Line.PLAIN),
                            IMonitorPicture.Line.of(MonitorPictureTexts.NO_SYSTEM.text(), IMonitorPicture.Line.DIM)));
            case FULL_DESKTOP -> desktop(owner, host);
            case TERMINAL_ONLY, NETWORK_GUI -> console(level, era, be);
        };
    }

    private static IMonitorPicture selfTest(final ServerLevel level, final BlockPos owner, final IOsHost host,
                                            final HardwareEra era) {
        final List<IMonitorPicture.Line> lines = new ArrayList<>();
        lines.add(IMonitorPicture.Line.of(machineName(level, owner), IMonitorPicture.Line.PLAIN));
        lines.add(new IMonitorPicture.Line(MonitorPictureTexts.MEMORY.text(),
                MonitorPictureTexts.MEGABYTES.with(host.ramTotalMb()), IMonitorPicture.Line.GOOD));
        lines.add(host.haltedAtPost()
                ? IMonitorPicture.Line.of(MonitorPictureTexts.NO_SYSTEM.text(), IMonitorPicture.Line.BAD)
                : IMonitorPicture.Line.of(MonitorPictureTexts.TESTING.text(), IMonitorPicture.Line.DIM));
        return new IMonitorPicture.Lines(era, MonitorPictureTexts.SELF_TEST.text(), lines);
    }

    /* A system coming up or going down: the steps reached by now. */
    private static IMonitorPicture steps(final HardwareEra era, final BootSequence sequence, final int done,
                                         final int total) {
        final List<IMonitorPicture.Line> lines = new ArrayList<>();
        if (!sequence.subtitle().isEmpty()) {
            lines.add(IMonitorPicture.Line.of(sequence.subtitle(), IMonitorPicture.Line.DIM));
        }
        final int shown = sequence.shownAt(done, total);
        for (int i = 0; i < shown; i++) {
            final BootSequence.Line step = sequence.lines().get(i);
            final Text label = step.mark().isEmpty() ? step.label()
                    : MonitorPictureTexts.MARKED.with(step.mark(), step.label());
            lines.add(new IMonitorPicture.Line(label, step.value(),
                    step.good() ? IMonitorPicture.Line.GOOD : IMonitorPicture.Line.PLAIN));
        }
        return new IMonitorPicture.Lines(era, sequence.title(), lines);
    }

    private static IMonitorPicture menu(final HardwareEra era, final BootMenu menu) {
        final List<IMonitorPicture.Line> lines = new ArrayList<>();
        for (int i = 0; i < menu.entries().size(); i++) {
            lines.add(IMonitorPicture.Line.of(menu.entries().get(i).label(),
                    i == menu.defaultIndex() ? IMonitorPicture.Line.PICKED : IMonitorPicture.Line.PLAIN));
        }
        return new IMonitorPicture.Lines(era, menu.title(), lines);
    }

    private static IMonitorPicture copying(final HardwareEra era, final OsInstallJob job) {
        final OsDef system = OsRegistry.getOs(ResourceLocation.tryParse(job.osId()));
        final String name = system == null ? job.osId() : system.displayName();
        final int total = Math.max(1, job.ticksTotal());
        final int percent = (int) ((long) (total - job.ticksLeft()) * 100 / total);
        return new IMonitorPicture.Lines(era, MonitorPictureTexts.INSTALLING.with(name),
                List.of(IMonitorPicture.Line.of(MonitorPictureTexts.DONE.with(percent), IMonitorPicture.Line.GOOD)));
    }

    private static IMonitorPicture desktop(final BlockPos owner, final IOsHost host) {
        final ResourceLocation system = host.installedOsId();
        if (system == null) {
            return IMonitorPicture.DARK;
        }
        final ResourceLocation desktop = host.bootedDesktopId() != null ? host.bootedDesktopId() : system;
        final List<DesktopWindowsPayload.WireWindow> windows = new ArrayList<>();
        for (final OpenWindow window : host.openWindows()) {
            windows.add(DesktopWindowsPayload.WireWindow.of(window));
        }
        return new IMonitorPicture.Desktop(owner, system, desktop, host.ramTotalMb(), host.ramReservedMb(), windows,
                host.desktopWorkspace());
    }

    private static IMonitorPicture console(final ServerLevel level, final HardwareEra era, final BlockEntity be) {
        if (!(be instanceof IComputerTerminalHost host) || host.console() == null) {
            return IMonitorPicture.DARK;
        }
        final ServerCliComputer here = new ServerCliComputer(host, level);
        return new IMonitorPicture.Console(era, host.console().glass(), SshTerminal.prompt(here, here));
    }

    private static Text machineName(final ServerLevel level, final BlockPos owner) {
        return Text.literal(level.getBlockState(owner).getBlock().getName().getString());
    }
}
