/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.Platform;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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
    @TextHolder
    static final class InstallPkg implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.sysv.installpkg.summary", "install the package on the medium in a drive");
        private static final TextKey USAGE = TextKey.of("jsc.cli.sysv.installpkg.usage", "[package]");
        private static final TextKey NO_MEDIUM =
                TextKey.of("jsc.cli.sysv.installpkg.no_medium", "no package medium in a drive");
        private static final TextKey INSERT = TextKey.of("jsc.cli.sysv.installpkg.insert",
                "  Insert the medium the package came on and try again.");
        private static final TextKey NO_SUCH = TextKey.of("jsc.cli.sysv.installpkg.no_such",
                "%s: no such package on the media in the drives");
        private static final TextKey ON_MEDIA = TextKey.of("jsc.cli.sysv.installpkg.on_media",
                "The following packages are on the media in the drives:");
        private static final TextKey SAY_WHICH =
                TextKey.of("jsc.cli.sysv.installpkg.say_which", "Say which: installpkg <package>");
        private static final TextKey INSTALLING =
                TextKey.of("jsc.cli.sysv.installpkg.installing", "Installing the %s package.");

        @Override public String name() { return "installpkg"; }

        @Override public CommandGroup group() { return CommandGroup.SOFTWARE; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public CommandScope scope() {
            return CommandScope.on(Platform.UNIX);
        }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.ProgramInfo> media = ctx.computer().programsOnMedia();
            if (media.isEmpty()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), NO_MEDIUM));
                ctx.out().dim(INSERT);
                return;
            }
            final ICliComputer.ProgramInfo chosen = ctx.hasArgs() ? named(media, ctx.arg(0))
                    : media.size() == 1 ? media.getFirst() : null;
            if (chosen == null && ctx.hasArgs()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), NO_SUCH.with(ctx.arg(0))));
                return;
            }
            if (chosen == null) {
                ctx.out().line(ON_MEDIA);
                for (final ICliComputer.ProgramInfo each : media) {
                    ctx.out().line("    " + each.name());
                }
                ctx.out().dim(SAY_WHICH);
                return;
            }
            ctx.out().line(INSTALLING.with(chosen.name()));
            final ICliComputer.OpResult result = ctx.computer().install(chosen.id());
            if (result.ok()) {
                ctx.out().ok(result.message());
            } else {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
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
