/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.ComputerConsoleState;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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

    /** The system installed on the machine, or null when it has none, or is not a machine that takes one. */
    @Nullable
    public OsDef installedOs() {
        final BlockEntity machine = (BlockEntity) this.terminal;
        return machine instanceof IOsHost host ? host.installedOs() : null;
    }

    /** The installed system's package manager; {@code NONE} on the platforms that install from media instead. */
    public PackageManagerKind manager() {
        final OsDef os = this.installedOs();
        return os == null ? PackageManagerKind.NONE : os.packageManager();
    }

    /** Whether the named program is on this machine: installed at its console, or a service flag on a Mainframe. */
    public boolean has(final ProgramSpec spec) {
        final BlockEntity machine = (BlockEntity) this.terminal;
        if (machine instanceof MainframeBlockEntity mf) {
            switch (spec.id().getPath()) {
                case "iqlengine" -> {
                    return mf.isIqlEngineInstalled();
                }
                case "automation_engine" -> {
                    return mf.isAutomationEngineInstalled();
                }
                case "mirror" -> {
                    return mf.isMirrorInstalled();
                }
                default -> {
                }
            }
        }
        final ComputerConsoleState console = this.terminal.console();
        return console != null && console.isInstalled(spec.id().toString());
    }

    /**
     * The packages a manager on THIS machine can offer: everything installable that runs on the platform it is
     * running. Reading the platform (rather than assuming Linux) is what lets the Frames manager see the
     * Frames-only software the Mirror serves.
     */
    public List<ProgramSpec> offered() {
        final OsDef os = this.installedOs();
        final Platform platform = os == null ? Platform.LINUX : os.platform();
        final List<ProgramSpec> out = new ArrayList<>();
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.installable() && spec.platforms().contains(platform)) {
                out.add(spec);
            }
        }
        return out;
    }

    /**
     * What the Mirror has for this machine: first what it can serve, each saying whether the machine already has it
     * and whether it is being built right now, then whatever players on this network have published, marked as
     * theirs. A published package the machine cannot read is left out rather than shown broken.
     */
    public List<ICliComputer.PackageInfo> available() {
        this.settleBuilds();
        if (this.mirrorMainframe() == null) {
            return List.of();
        }
        final ComputerConsoleState console = this.terminal.console();
        final List<ICliComputer.PackageInfo> out = new ArrayList<>();
        for (final ProgramSpec spec : this.offered()) {
            final boolean building = console != null && console.pendingBuilds().containsKey(spec.id().toString());
            out.add(new ICliComputer.PackageInfo(spec.commandName(), spec.displayName()
                    + (spec.kind() == ProgramKind.SERVICE ? " (service)" : ""), this.has(spec), building));
        }
        final MainframeBlockEntity mirror = this.mirrorMainframe();
        if (mirror != null) {
            for (final var shelved : mirror.shelvedPackages().entrySet()) {
                final Packed packed = Packed.read(shelved.getValue());
                if (packed == null) {
                    continue;
                }
                final String about = packed.manifest().about();
                out.add(new ICliComputer.PackageInfo(shelved.getKey(),
                        (about.isBlank() ? packed.manifest().label() : about) + " - " + packed.manifest().house(),
                        false, false, true));
            }
        }
        return out;
    }

    /**
     * Moves finished source builds into the installed set.
     *
     * <p>It is done whenever the packages are touched, and it happens whether or not a Mirror is serving: a build
     * that finished while the machine was cut off still belongs to the machine.
     */
    public void settleBuilds() {
        final ComputerConsoleState console = this.terminal.console();
        if (console != null && !console.settleBuilds(this.level.getGameTime()).isEmpty()) {
            ((BlockEntity) this.terminal).setChanged();
        }
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
