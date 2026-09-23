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
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The network's Mirror, as a machine on that network reaches it.
 *
 * <p>The Mirror is the network's, not the machine's: it is installed on the Mainframe, and every computer of the
 * network installs from that one. Besides what it serves, it keeps a shelf of the packages players have published
 * for anyone on the network to install.
 */
public final class MirrorService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The machine's drives, for the package file a player publishes. */
    private final FileService files;
    /** The network's own language, for the row its engine fills in the list of services. */
    private final IqlService iql;

    public MirrorService(final IComputerTerminalHost terminal, final ServerLevel level, final FileService files,
                         final IqlService iql) {
        this.terminal = terminal;
        this.level = level;
        this.files = files;
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
        return this.serving() != null;
    }

    /** The network's Mainframe while its Mirror is serving, else null. */
    @Nullable
    public MainframeBlockEntity serving() {
        final MainframeBlockEntity mainframe = this.mainframe();
        return mainframe != null && mainframe.isMirrorActive() ? mainframe : null;
    }

    /** The name the Mirror's Mainframe goes by in a package line, or the plain word when it has none. */
    public String hostname() {
        final MainframeBlockEntity mirror = this.serving();
        final String name = mirror == null ? "" : mirror.console() == null ? "" : mirror.console().computerName();
        return name == null || name.isBlank() ? "mainframe" : name;
    }

    /** Puts a built package on the network's Mirror, for anyone on the network to install. */
    public ICliComputer.OpResult publish(final String path) {
        final MainframeBlockEntity mirror = this.serving();
        if (mirror == null) {
            return ICliComputer.OpResult.fail("could not resolve mirror:// - connect this computer to a network whose"
                    + " Mainframe runs the Mirror service");
        }
        final ICliComputer.FsResult read = this.files.readFile(path);
        if (!read.ok()) {
            return ICliComputer.OpResult.fail(read.message());
        }
        final Packed packed = Packed.read(read.message());
        if (packed == null) {
            return ICliComputer.OpResult.fail(path + ": this is not a package (build one with 'sgpack build')");
        }
        final List<String> wrong = packed.problems();
        if (!wrong.isEmpty()) {
            return ICliComputer.OpResult.fail(path + ": " + wrong.getFirst());
        }
        final String name = packed.manifest().name();
        final boolean replacing = mirror.shelvedPackage(name) != null;
        if (!mirror.shelve(name, read.message())) {
            return ICliComputer.OpResult.fail("the Mirror is full ("
                    + MainframeBlockEntity.SHELF_MAX + " packages)");
        }
        return ICliComputer.OpResult.ok((replacing ? "replaced " : "published ") + packed.manifest().label()
                + " on the Mirror");
    }

    /** Takes one back off the Mirror. */
    public ICliComputer.OpResult unpublish(final String name) {
        final MainframeBlockEntity mirror = this.serving();
        if (mirror == null) {
            return ICliComputer.OpResult.fail("could not resolve mirror://");
        }
        if (!mirror.unshelve(name == null ? "" : name.trim())) {
            return ICliComputer.OpResult.fail("the Mirror is not serving " + name);
        }
        return ICliComputer.OpResult.ok("took " + name + " off the Mirror");
    }

    /**
     * What players on this network have published, each marked as theirs. A published package the machine cannot
     * read is left out rather than shown broken.
     */
    public List<ICliComputer.PackageInfo> shelved() {
        final MainframeBlockEntity mirror = this.serving();
        if (mirror == null) {
            return List.of();
        }
        final List<ICliComputer.PackageInfo> out = new ArrayList<>();
        for (final var shelved : mirror.shelvedPackages().entrySet()) {
            final Packed packed = Packed.read(shelved.getValue());
            if (packed == null) {
                continue;
            }
            final String about = packed.manifest().about();
            out.add(new ICliComputer.PackageInfo(shelved.getKey(),
                    (about.isBlank() ? packed.manifest().label() : about) + " - " + packed.manifest().house(),
                    false, true));
        }
        return out;
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
