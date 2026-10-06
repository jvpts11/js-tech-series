/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.OpenBootMenuPayload;
import dev.jstech.computers.operation.payload.OpenPostPayload;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.OsInstallProgressPayload;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.SshTerminal;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Works out what a monitor's face shows, from what its machine holds: the server's half of the screen in the world.
 *
 * <p>It asks the same question a player opening the monitor is answered by ({@link MonitorBlock#entryFor}) and sends
 * the same opening that player would be sent ({@link SessionOpenings}), so the face is drawn by the very screen the
 * session opens and the two can never show different things. Only reads: unlike opening a session on the monitor,
 * describing it never settles anything about the machine, so a monitor looked at from across the room cannot change
 * what happens when somebody sits down at it.
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
        final BlockPos at = monitor.getBlockPos();
        final long now = level.getGameTime();
        final HardwareEra era = host.displayEra();
        return switch (MonitorBlock.entryFor(host, false)) {
            case NO_POWER -> IMonitorPicture.DARK;
            case POST -> {
                final OpenPostPayload post = SessionOpenings.post(level, at, owner, host);
                // A machine standing at a failed self-test shows the end of it, which no clock runs out.
                yield new IMonitorPicture.Session(era, new OpenPostPayload(post.host(), post.monitorPos(),
                        post.firmwareKind(), post.name(), 0, post.halted(), post.complaint()),
                        state(level, host, owner), post.halted() ? 0L : now + post.remainingTicks());
            }
            case GOING_DOWN -> booting(era, SessionOpenings.systemDown(at, owner, host), now);
            case BOOTING -> booting(era, SessionOpenings.systemBoot(at, owner, host), now);
            case BOOT_MENU -> {
                final OpenBootMenuPayload menu = SessionOpenings.bootMenu(at, owner, host);
                yield new IMonitorPicture.Session(era,new OpenBootMenuPayload(menu.hostPos(), menu.monitorPos(),
                        menu.menu(), 0), null, menu.remainingTicks() > 0 ? now + menu.remainingTicks() : 0L);
            }
            case INSTALLING -> installing(level, host, owner, at, now);
            case INSTALLER -> new IMonitorPicture.Session(era,SessionOpenings.installDone(at, owner, host), null, 0L);
            case BOOT -> switch (BootController.targetForComputer(be)) {
                case FIRMWARE -> new IMonitorPicture.Session(era,SessionOpenings.firmware(level, at, owner, host),
                        state(level, host, owner), 0L);
                case FULL_DESKTOP -> desktop(owner, host);
                case TERMINAL_ONLY, NETWORK_GUI -> console(level, host, be);
            };
        };
    }

    /* A system coming up or going down, its clock stopped at nought and the time it ends beside it. */
    private static IMonitorPicture booting(@Nullable final HardwareEra era, final OpenSystemBootPayload boot,
                                           final long now) {
        return new IMonitorPicture.Session(era,new OpenSystemBootPayload(boot.hostPos(), boot.monitorPos(), 0,
                boot.totalTicks(), boot.sequence(), boot.endsDark(), boot.splash(), boot.who()), null,
                now + boot.remainingTicks());
    }

    /* An install: an installer's page, as it is and with the parts it reads, or a copy with its clock stopped. */
    private static IMonitorPicture installing(final ServerLevel level, final IOsHost host, final BlockPos owner,
                                              final BlockPos at, final long now) {
        final HardwareEra era = host.displayEra();
        final CustomPacketPayload opening = SessionOpenings.installProgress(at, owner, host);
        if (opening instanceof OsInstallProgressPayload copy) {
            return new IMonitorPicture.Session(era,new OsInstallProgressPayload(copy.hostPos(), copy.monitorPos(),
                    copy.firmwareKind(), copy.osName(), copy.targetLabel(), 0, copy.ticksTotal()), null,
                    now + copy.ticksLeft());
        }
        return opening == null ? IMonitorPicture.DARK
                : new IMonitorPicture.Session(era,opening, state(level, host, owner), 0L);
    }

    /* What the machine's firmware reads of its parts, which the self-test, the setup and the installer list. */
    private static FirmwareStatePayload state(final ServerLevel level, final IOsHost host, final BlockPos owner) {
        return FirmwarePayloads.buildFirmwareState(level, host, owner);
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

    private static IMonitorPicture console(final ServerLevel level, final IOsHost host, final BlockEntity be) {
        if (!(be instanceof IComputerTerminalHost terminal) || terminal.console() == null) {
            return IMonitorPicture.DARK;
        }
        final HardwareEra era = host.displayEra() == null ? HardwareEra.STANDARD : host.displayEra();
        final ServerCliComputer here = new ServerCliComputer(terminal, level);
        // A booted live medium is a Linux console, whatever is or is not installed under it.
        final Platform platform = terminal.console().liveInstall() != null ? Platform.LINUX
                : host.installedOs() == null ? null : host.installedOs().platform();
        return new IMonitorPicture.Console(era, terminal.console().glass(), SshTerminal.prompt(here, here),
                platform, terminal.console().settings().guiScale());
    }
}
