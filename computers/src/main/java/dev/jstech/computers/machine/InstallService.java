/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * What is installed on a machine, and the installing itself.
 *
 * <p>A program arrives one of three ways: it comes with the system, it is installed from a disc in a linked drive,
 * or a live medium walks the player through installing the system by hand. What a machine has is read the same way
 * whichever way it got there.
 */
public final class InstallService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The packages the machine installs over the network, which is what says whether it already has one. */
    private final PackageService packages;

    public InstallService(final IComputerTerminalHost terminal, final ServerLevel level,
                          final PackageService packages) {
        this.terminal = terminal;
        this.level = level;
        this.packages = packages;
    }

    /**
     * The programs on this machine: the ones its platform ships with, and the ones installed onto it.
     *
     * <p>The platform is what keeps an MC-DOS listing from showing the Frames desktop apps, which come with the
     * Frames platform and with nothing else.
     */
    public List<ICliComputer.ProgramInfo> programs() {
        final List<ICliComputer.ProgramInfo> out = new ArrayList<>();
        final Platform platform = this.platform();
        for (final ProgramSpec spec : Programs.installed()) {
            if (platform == null || spec.platforms().contains(platform)) {
                out.add(new ICliComputer.ProgramInfo(spec.commandName(), spec.id().toString()));
            }
        }
        final ComputerConsoleState console = this.terminal.console();
        if (console != null) {
            for (final String id : console.installed()) {
                final ProgramSpec program = Programs.get(ResourceLocation.tryParse(id));
                if (program != null && !program.preinstalled()) {
                    out.add(new ICliComputer.ProgramInfo(program.commandName(), program.id().toString()));
                }
            }
        }
        return out;
    }

    /** The platform of the system installed on the machine, or null when it cannot be told. */
    @Nullable
    public Platform platform() {
        if (this.terminal instanceof IOsHost host) {
            final OsDef os = OsRegistry.getOs(host.installedOsId());
            return os == null ? null : os.platform();
        }
        return null;
    }

    /** Whether the program with that id is on this machine, installed or as a service the Mainframe keeps. */
    public boolean has(@Nullable final ResourceLocation id) {
        final ProgramSpec spec = id == null ? null : OsRegistry.getProgram(id);
        return spec != null && this.packages.has(spec);
    }

    /** The world the machine is in, which the installing itself is timed against. */
    ServerLevel level() {
        return this.level;
    }
}
