/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The packages a machine installs over its network's Mirror, and the Mirror itself.
 *
 * <p>The Mirror is the network's, not the machine's: it is installed on the Mainframe and every computer of the
 * network installs from that one. A machine that cannot reach a serving Mirror is told so in the words its own
 * package manager would use, because that is what a player reads at the prompt.
 */
public final class PackageService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The network's own language, for the row its engine fills in the list of services. */
    private final IqlService iql;

    public PackageService(final IComputerTerminalHost terminal, final ServerLevel level, final IqlService iql) {
        this.terminal = terminal;
        this.level = level;
        this.iql = iql;
    }

    /** Installs the Mirror on the network's Mainframe, or says how it stands. */
    public ICliComputer.OpResult control(final String action) {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null) {
            return ICliComputer.OpResult.fail("the network has no running Mainframe to host the Mirror");
        }
        return switch (action == null ? "" : action.toLowerCase(Locale.ROOT)) {
            case "install" -> mainframe.installMirror()
                    ? ICliComputer.OpResult.ok("Mirror installed on the Mainframe and serving packages")
                    : ICliComputer.OpResult.fail("the Mirror is already installed");
            case "status", "" -> ICliComputer.OpResult.ok("Mirror: " + this.state());
            default -> ICliComputer.OpResult.fail("usage: mirror install|status");
        };
    }

    /** How the Mirror stands on the network's Mainframe, in the words every view shows. */
    public String state() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || !mainframe.isMirrorInstalled()) {
            return "not installed";
        }
        return mainframe.isMirrorActive() ? "serving" : "installed (Mainframe off)";
    }

    /** Whether a Mirror is serving this machine right now. */
    public boolean reachable() {
        return this.mirrorMainframe() != null;
    }

    /** The network's Mainframe while its Mirror is serving, else null. */
    @Nullable
    public MainframeBlockEntity mirrorMainframe() {
        final MainframeBlockEntity mainframe = this.mainframe();
        return mainframe != null && mainframe.isMirrorActive() ? mainframe : null;
    }

    /** The name the Mirror's Mainframe goes by in a package line, or the plain word when it has none. */
    public String mirrorHostname() {
        final MainframeBlockEntity mirror = this.mirrorMainframe();
        final String name = mirror == null ? "" : mirror.console() == null ? "" : mirror.console().computerName();
        return name == null || name.isBlank() ? "mainframe" : name;
    }

    /**
     * The services the network offers and how each stands, each row answered by whoever keeps that service: the
     * engine's by the language, the Mirror's here. A machine on no network with a Mainframe has none to list.
     */
    public List<ICliComputer.ServiceStatus> services() {
        if (this.mainframe() == null) {
            return List.of();
        }
        return List.of(new ICliComputer.ServiceStatus("IQL Engine", this.iql.state()),
                new ICliComputer.ServiceStatus("Mirror", this.state()));
    }

    /** The Mainframe of the machine's network, or null when it is on none, or none is running. */
    @Nullable
    private MainframeBlockEntity mainframe() {
        final NetworkUuid net = this.terminal.networkUuid();
        if (net == null) {
            return null;
        }
        return NetworkSystem.get(this.level).mainframePositionOf(net)
                .map(pos -> this.level.getBlockEntity(BlockPos.of(pos)) instanceof MainframeBlockEntity mf ? mf : null)
                .orElse(null);
    }
}
