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
import dev.jstech.computers.os.install.SetupJob;
import dev.jstech.computers.os.install.SetupRunner;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    /** The network's own language, since its engine is installed on the Mainframe rather than here. */
    private final IqlService iql;

    public InstallService(final IComputerTerminalHost terminal, final ServerLevel level,
                          final PackageService packages, final IqlService iql) {
        this.terminal = terminal;
        this.level = level;
        this.packages = packages;
        this.iql = iql;
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

    /**
     * Installs a program from the disc in a linked drive.
     *
     * <p>Installing is something the machine does over time, from that disc. Whether it can, and why not, is decided
     * in one place for every way of asking, so the prompt says exactly what the Setup window on a desktop would.
     */
    public ICliComputer.OpResult install(final String programId) {
        final ResourceLocation location = ResourceLocation.tryParse(
                programId.contains(":") ? programId.toLowerCase(Locale.ROOT)
                        : "jsc:" + programId.toLowerCase(Locale.ROOT));
        final ProgramSpec program = location == null ? null : Programs.get(location);
        if (program == null) {
            return ICliComputer.OpResult.fail("no such program: " + programId);
        }
        if (program.id().equals(Programs.IQL_ENGINE)) {
            // The Engine is a service on the Mainframe, not a console-local app, so install it there.
            return this.iql.control("install");
        }
        final MediaFormat medium = this.installMediumFor(program.id());
        if (medium == null) {
            return ICliComputer.OpResult.fail(program.commandName() + " needs its install disc in a linked drive");
        }
        final IOsHost machine = this.osHost();
        if (machine == null) {
            return ICliComputer.OpResult.fail("this computer cannot store installed programs");
        }
        final Optional<String> refusal = SetupRunner.begin(machine, this.level,
                ((BlockEntity) this.terminal).getBlockPos(), program, medium, false, SetupJob.VIA_INSTALL);
        return refusal.map(ICliComputer.OpResult::fail)
                .orElseGet(() -> ICliComputer.OpResult.ok("Setting up " + program.commandName() + " from "
                        + driveName(medium) + " ..."));
    }

    /** The machine as the thing that installs programs, whichever of the two the prompt is held by. */
    @Nullable
    private IOsHost osHost() {
        if (this.terminal instanceof IOsHost fromHost) {
            return fromHost;
        }
        return (BlockEntity) this.terminal instanceof IOsHost fromBlock ? fromBlock : null;
    }

    /** What the disc a program comes from is called at a prompt. */
    private static String driveName(final MediaFormat medium) {
        return switch (medium) {
            case FLOPPY -> "the floppy";
            case CD -> "the CD";
            case DVD -> "the DVD";
            case USB -> "the USB drive";
        };
    }

    /** The format of the disc a program's installer sits on in a linked drive, or null when none does. */
    @Nullable
    private MediaFormat installMediumFor(final ResourceLocation programId) {
        if (!(this.terminal instanceof IOsHost computer)) {
            return null;
        }
        for (final long endpoint : computer.linkedEndpoints()) {
            if (this.level.getBlockEntity(BlockPos.of(endpoint)) instanceof MediaReaderBlockEntity reader
                    && reader.insertedKind() == MediaKind.PROGRAM_INSTALL
                    && programId.equals(reader.insertedPayload())) {
                return reader.insertedFormat();
            }
        }
        return null;
    }
}
