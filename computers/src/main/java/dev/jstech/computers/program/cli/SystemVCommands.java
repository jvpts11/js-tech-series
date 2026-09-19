/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import java.util.List;

/**
 * The commands that are System V's own and no other family's.
 *
 * <p>UNIX takes its programs from media and from nowhere else, so where the others have a manager that asks a
 * network, it has the tool of its day: put the medium in the drive and say {@code installpkg}.
 */
public final class SystemVCommands {

    private SystemVCommands() {
    }

    public static List<ICliCommand> all() {
        return List.of(new InstallPkg());
    }

    /**
     * Installs the package on the medium in a drive.
     *
     * <p>With nothing said it takes what is in the drive, which is how the real one worked: the medium is the
     * question and the answer. With more than one medium in, it lists them and asks to be told which, by name,
     * rather than guessing.
     */
    static final class InstallPkg implements ICliCommand {

        @Override public String name() { return "installpkg"; }

        @Override public String summary() { return "install the package on the medium in a drive"; }

        @Override public String usage() { return "[package]"; }

        @Override public boolean available(final ICliComputer computer) {
            return computer.platform() == Platform.UNIX;
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.ProgramInfo> media = ctx.computer().programsOnMedia();
            if (media.isEmpty()) {
                ctx.out().error("installpkg: no package medium in a drive");
                ctx.out().dim("  Insert the medium the package came on and try again.");
                return;
            }
            final ICliComputer.ProgramInfo chosen = ctx.hasArgs() ? named(media, ctx.arg(0))
                    : media.size() == 1 ? media.getFirst() : null;
            if (chosen == null && ctx.hasArgs()) {
                ctx.out().error("installpkg: " + ctx.arg(0) + ": no such package on the media in the drives");
                return;
            }
            if (chosen == null) {
                ctx.out().line("The following packages are on the media in the drives:");
                for (final ICliComputer.ProgramInfo each : media) {
                    ctx.out().line("    " + each.name());
                }
                ctx.out().dim("Say which: installpkg <package>");
                return;
            }
            ctx.out().line("Installing the " + chosen.name() + " package.");
            final ICliComputer.OpResult result = ctx.computer().install(chosen.id());
            if (result.ok()) {
                ctx.out().ok(result.message());
            } else {
                ctx.out().error("installpkg: " + result.message());
            }
        }

        private static ICliComputer.ProgramInfo named(final List<ICliComputer.ProgramInfo> media, final String name) {
            for (final ICliComputer.ProgramInfo each : media) {
                if (each.name().equalsIgnoreCase(name)) {
                    return each;
                }
            }
            return null;
        }
    }
}
