/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
@TextHolder
public final class MirrorService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The machine's drives, for the package file a player publishes. */
    private final FileService files;
    /** The network's own language, for the row its engine fills in the list of services. */
    private final IqlService iql;

    /** What every way of installing from the Mirror says when there is none in reach, and what to do about it. */
    static final TextKey NO_MIRROR_SERVICE = TextKey.of("jsc.service.mirror.no_mirror_service",
            "could not resolve mirror:// - connect this computer to a network whose Mainframe runs the Mirror service");

    private static final TextKey NO_MAINFRAME =
            TextKey.of("jsc.service.mirror.no_mainframe", "the network has no running Mainframe to host the Mirror");
    private static final TextKey INSTALLED =
            TextKey.of("jsc.service.mirror.installed", "Mirror installed on the Mainframe and serving packages");
    private static final TextKey ALREADY = TextKey.of("jsc.service.mirror.already", "the Mirror is already installed");
    private static final TextKey STATUS = TextKey.of("jsc.service.mirror.status", "Mirror: %s");
    private static final TextKey NOT_INSTALLED = TextKey.of("jsc.service.mirror.not_installed", "not installed");
    private static final TextKey SERVING = TextKey.of("jsc.service.mirror.serving", "serving");
    private static final TextKey OFF = TextKey.of("jsc.service.mirror.off", "installed (Mainframe off)");
    private static final TextKey NOT_A_PACKAGE =
            TextKey.of("jsc.service.mirror.not_a_package", "this is not a package (build one with '%s')");
    private static final TextKey FULL = TextKey.of("jsc.service.mirror.full", "the Mirror is full (%s packages)");
    private static final TextKey PUBLISHED = TextKey.of("jsc.service.mirror.published", "published %s on the Mirror");
    private static final TextKey REPLACED = TextKey.of("jsc.service.mirror.replaced", "replaced %s on the Mirror");
    private static final TextKey NOT_SERVING =
            TextKey.of("jsc.service.mirror.not_serving", "the Mirror is not serving %s");
    private static final TextKey TOOK = TextKey.of("jsc.service.mirror.took", "took %s off the Mirror");

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
            return ICliComputer.OpResult.fail(NO_MAINFRAME);
        }
        return switch (action == null ? "" : action.toLowerCase(Locale.ROOT)) {
            case "install" -> mainframe.installMirror()
                    ? ICliComputer.OpResult.ok(INSTALLED)
                    : ICliComputer.OpResult.fail(ALREADY);
            case "status", "" -> ICliComputer.OpResult.ok(STATUS.with(this.state()));
            default -> ICliComputer.OpResult.fail(CliTexts.USAGE.with(Text.literal("mirror"),
                    Text.literal("install|status")));
        };
    }

    /** How the Mirror stands on the network's Mainframe, in the words every view shows. */
    public Text state() {
        final MainframeBlockEntity mainframe = this.mainframe();
        if (mainframe == null || !mainframe.isMirrorInstalled()) {
            return NOT_INSTALLED.text();
        }
        return mainframe.isMirrorActive() ? SERVING.text() : OFF.text();
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
            return ICliComputer.OpResult.fail(NO_MIRROR_SERVICE);
        }
        final ICliComputer.FsResult read = this.files.readFile(path);
        if (!read.ok()) {
            return ICliComputer.OpResult.fail(read.message());
        }
        final Packed packed = Packed.read(read.message().english());
        if (packed == null) {
            return ICliComputer.OpResult.fail(
                    CliTexts.SAID_BY.with(path, NOT_A_PACKAGE.with(Text.literal("sgpack build"))));
        }
        final List<String> wrong = packed.problems();
        if (!wrong.isEmpty()) {
            return ICliComputer.OpResult.fail(CliTexts.SAID_BY.with(path, wrong.getFirst()));
        }
        final String name = packed.manifest().name();
        final boolean replacing = mirror.shelvedPackage(name) != null;
        if (!mirror.shelve(name, read.message().english())) {
            return ICliComputer.OpResult.fail(FULL.with(MainframeBlockEntity.SHELF_MAX));
        }
        return ICliComputer.OpResult.ok((replacing ? REPLACED : PUBLISHED).with(packed.manifest().label()));
    }

    /** Takes one back off the Mirror. */
    public ICliComputer.OpResult unpublish(final String name) {
        final MainframeBlockEntity mirror = this.serving();
        if (mirror == null) {
            return ICliComputer.OpResult.fail(ICliPackages.NO_MIRROR);
        }
        if (!mirror.unshelve(name == null ? "" : name.trim())) {
            return ICliComputer.OpResult.fail(NOT_SERVING.with(String.valueOf(name)));
        }
        return ICliComputer.OpResult.ok(TOOK.with(name));
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
        // A service's state travels as the English the machine keeps until the listing carries words to translate.
        return List.of(new ICliComputer.ServiceStatus("IQL Engine", this.iql.state()),
                new ICliComputer.ServiceStatus("Mirror", this.state().english()));
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
