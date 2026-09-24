/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.program.cli.man.ManPage;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;

/**
 * The Unix systems' file commands, in the words the distributions, UNIX and FreeBSD use.
 *
 * <p>Moved here from PosixCommands, which keeps the lists that say which system gets which command.
 */
final class PosixFileCommands {

    /** The Unix systems' words for files: the distributions, UNIX and FreeBSD, which all keep files. */
    private static final CommandScope POSIX_FILES =
            CommandScope.on(CommandScope.UNIX_SYSTEMS).needing(CommandScope.Need.FILES);

    /** How wide the size column of a long listing is. */
    private static final int SIZE_W = 12;

    /** Which column {@code df} writes where a filesystem is mounted in. */
    private static final int MOUNT_AT = 44;

    private PosixFileCommands() {
    }

    @TextHolder
    static final class Ls implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.ls.summary", "list directory contents");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.ls.usage", "[-l] [directory]");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "ls"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            boolean longFormat = false;
            String dir = "";
            for (int i = 0; i < ctx.argCount(); i++) {
                final String a = ctx.arg(i);
                if (a.equals("-l") || a.equals("-la") || a.equals("-al")) {
                    longFormat = true;
                } else {
                    dir = a;
                }
            }
            final ICliComputer.FsResult result = ctx.computer().listDisk(dos(ctx, dir));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
                return;
            }
            final List<ICliComputer.FsEntry> entries = result.entries();
            if (entries.isEmpty()) {
                return;
            }
            if (longFormat) {
                /*
                 * Columns, the way ls -l prints them: the mode, the size, then the name. The name goes last
                 * because it is the one column nothing can plan a width for; pushed to the right edge with
                 * the size, every line ran the width of the glass and folded in half on a narrow window.
                 * Every column is data (a mode, a figure and its unit, a name), so the line is one.
                 */
                for (final ICliComputer.FsEntry e : entries) {
                    final String mode = (e.isDir() ? "d" : "-") + (e.readOnly() ? "r--r--r--" : "rw-r--r--");
                    ctx.out().line(Text.literal(mode + CliText.padLeft(
                            e.isDir() ? "" : String.format(Locale.ROOT, "%,d mB", e.weightMbEq()), SIZE_W)
                            + "  " + e.name() + (e.isDir() ? "/" : "")));
                }
                return;
            }
            final StringBuilder line = new StringBuilder();
            for (final ICliComputer.FsEntry e : entries) {
                if (line.length() > 0) {
                    line.append("  ");
                }
                line.append(e.name()).append(e.isDir() ? "/" : "");
            }
            ctx.out().line(line.toString());
        }
    }

    @TextHolder
    static final class Pwd implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.posix.pwd.summary", "print the current directory");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "pwd"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(PosixPath.render(ctx.computer().tree(), ctx.computer().currentLocation()));
        }
    }

    @TextHolder
    static final class Cd implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.cd.summary",
                "change the current directory (home when no argument)");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.cd.usage", "[directory]");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "cd"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            final String target = ctx.hasArgs() ? ctx.rest(0) : "~";
            final ICliComputer.FsResult result = ctx.computer().changeDir(dos(ctx, target));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            }
        }
    }

    @TextHolder
    static final class Cat implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.cat.summary", "print the content of a file");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.cat.usage", "<file> [file...]");
        private static final TextKey USAGE_ERROR = TextKey.of("jsc.cli.posix.cat.usage_error", "usage: cat <file>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "cat"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        /**
         * Every file it was given, one after another, which is what the name is short for and what a word with
         * a star in it turns into by the time it gets here.
         */
        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(USAGE_ERROR);
                return;
            }
            for (final String named : ctx.args()) {
                final ICliComputer.FsResult result = ctx.computer().readFile(dos(ctx, named));
                if (!result.ok()) {
                    ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
                    continue;
                }
                for (final String line : result.message().english().split("\n", -1)) {
                    ctx.out().line(line);
                }
            }
        }
    }

    @TextHolder
    static final class Rm implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.rm.summary", "remove a file");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.rm.usage", "<file>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "rm"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().deleteFile(dos(ctx, ctx.arg(0)));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            }
        }
    }

    @TextHolder
    static final class Mkdir implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.mkdir.summary", "create a directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.mkdir.usage", "<directory>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "mkdir"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().makeDir(dos(ctx, ctx.rest(0)));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            }
        }
    }

    @TextHolder
    static final class Rmdir implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.rmdir.summary", "remove an empty directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.rmdir.usage", "<directory>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "rmdir"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().removeDir(dos(ctx, ctx.rest(0)));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            }
        }
    }

    @TextHolder
    static final class Cp implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.posix.cp.summary", "copy a file to another location");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.cp.usage", "<source> <destination>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "cp"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().copyPath(dos(ctx, ctx.arg(0)), dos(ctx, ctx.arg(1)));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            }
        }
    }

    @TextHolder
    static final class Mv implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.posix.mv.summary", "move a file into a directory, or rename it");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.mv.usage", "<source> <directory|new-name>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "mv"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final String dest = ctx.arg(1);
            // A bare new name (no slash, no path form) is a rename; anything else moves into a directory.
            final boolean rename = !dest.contains("/") && !dest.startsWith("~") && !dest.equals(".")
                    && !dest.equals("..");
            final ICliComputer.FsResult result = rename
                    ? ctx.computer().renamePath(dos(ctx, ctx.arg(0)), dest)
                    : ctx.computer().movePath(dos(ctx, ctx.arg(0)), dos(ctx, dest));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            }
        }
    }

    @TextHolder
    static final class Touch implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.touch.summary", "create an empty file");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.touch.usage", "<file>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "touch"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().writeFile(dos(ctx, ctx.arg(0)), "");
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            }
        }
    }

    @TextHolder
    static final class Write implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.posix.write.summary", "create or overwrite a file with the given text");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.write.usage", "<file> <text...>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "write"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().writeFile(dos(ctx, ctx.arg(0)), ctx.rest(1));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
            } else if (!result.message().english().isEmpty()) {
                ctx.out().ok(result.message());
            }
        }
    }

    @TextHolder
    static final class Run implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.run.summary", "execute an .iql script");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.run.usage", "<file.iql>");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "run"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().runScript(dos(ctx, ctx.arg(0)));
            if (!result.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
                return;
            }
            if (result.opResult() != null) {
                if (result.opResult().ok()) {
                    ctx.out().ok(result.opResult().message());
                } else {
                    ctx.out().error(result.opResult().message());
                }
            } else if (!result.message().english().isEmpty()) {
                ctx.out().line(result.message());
            }
        }
    }

    @TextHolder
    static final class Clear implements ICliCommand, CliShell.IClearMarker {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.clear.summary", "clear the terminal");

        /** Clearing the glass needs no files, so it is the one here that asks nothing of the disk. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "clear"; }

        @Override public CommandGroup group() { return CommandGroup.MACHINE; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public void run(final CliContext ctx) {
            // The shell clears the scrollback because this command is a ClearMarker; nothing to print.
        }
    }

    @TextHolder
    static final class Man implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.posix.man.summary", "show the manual entry for a command");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.man.usage", "<command>");
        private static final TextKey WHICH_PAGE =
                TextKey.of("jsc.cli.posix.man.which_page", "What manual page do you want?");
        private static final TextKey TRY_INTRO =
                TextKey.of("jsc.cli.posix.man.try_intro", "For example, try 'man intro'.");
        private static final TextKey NO_ENTRY = TextKey.of("jsc.cli.posix.man.no_entry", "No manual entry for %s");

        /** The manual is about commands, not about files, so a system with no disk still has it. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "man"; }

        @Override public CommandGroup group() { return CommandGroup.HELP; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(WHICH_PAGE);
                ctx.out().dim(TRY_INTRO);
                return;
            }
            final ICliCommand command = ctx.shell().find(ctx.arg(0));
            if (command == null || !command.available(ctx.computer())) {
                ctx.out().error(NO_ENTRY.with(ctx.arg(0)));
                return;
            }
            ctx.out().accent(command.name().toUpperCase(Locale.ROOT) + "(1)");
            for (final CliLine line : ManPage.lines(command, true)) {
                ctx.out().line(line);
            }
            ctx.computer().report(JscEvents.MAN_PAGE, command.name());
        }
    }

    @TextHolder
    static final class Df implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.posix.df.summary", "report filesystem space usage");
        private static final TextKey NONE_MOUNTED =
                TextKey.of("jsc.cli.posix.df.none_mounted", "no filesystems mounted");
        /* The spacing of the heading lines up with the columns of the rows printed under it. */
        private static final TextKey HEADING = TextKey.of("jsc.cli.posix.df.heading",
                "Filesystem       Size   Used   Avail  Use%  Mounted on");
        /*
         * Said after where the drive is mounted, where its length cannot push anything out of line, since a
         * drive with nothing in it has no figures to line up.
         */
        private static final TextKey NO_MEDIUM = TextKey.of("jsc.cli.posix.df.no_medium", "%s  (no medium)");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "df"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.MountInfo> mounts = ctx.computer().mounts();
            if (mounts.isEmpty()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), NONE_MOUNTED));
                return;
            }
            /*
             * A table, the way df prints one, with where it is mounted in the last column: pushed to the
             * right edge instead, every row ran the width of the glass and folded in half on a window
             * narrower than a monitor.
             */
            ctx.out().header(HEADING);
            for (final ICliComputer.MountInfo m : mounts) {
                final String mount = PosixPath.render(ctx.computer().tree(), DosPath.Location.root(m.drive()));
                if (!m.ready()) {
                    ctx.out().line(NO_MEDIUM.with(Text.literal(CliText.pad("/dev/" + m.device(), MOUNT_AT) + mount)));
                    continue;
                }
                final long used = Math.max(0L, m.capacityMbEq() - m.freeMbEq());
                final int pct = m.capacityMbEq() <= 0 ? 0 : (int) (used * 100 / m.capacityMbEq());
                ctx.out().line(Text.literal(CliText.pad(String.format(Locale.ROOT, "/dev/%-8s %6s %6s %6s %3d%%",
                        m.device(), size(m.capacityMbEq()), size(used), size(m.freeMbEq()), pct), MOUNT_AT)
                        + mount));
            }
        }

        private static String size(final long mbEq) {
            if (mbEq >= 1_000_000L) {
                return String.format(Locale.ROOT, "%.1fT", mbEq / 1_000_000.0);
            }
            if (mbEq >= 1_000L) {
                return String.format(Locale.ROOT, "%.1fG", mbEq / 1_000.0);
            }
            return mbEq + "M";
        }
    }

    @TextHolder
    static final class Mkfs implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.posix.mkfs.summary", "build a filesystem on a device (erases it)");
        private static final TextKey USAGE = TextKey.of("jsc.cli.posix.mkfs.usage", "/dev/<device>");
        private static final TextKey USAGE_ERROR = TextKey.of("jsc.cli.posix.mkfs.usage_error",
                "usage: mkfs.ext4 /dev/<device>   (see df for the devices)");
        private static final TextKey CREATING =
                TextKey.of("jsc.cli.posix.mkfs.creating", "Creating filesystem on /dev/%s ... done");
        private static final TextKey NO_SUCH_DEVICE =
                TextKey.of("jsc.cli.posix.mkfs.no_such_device", "cannot open /dev/%s: No such device");

        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "mkfs.ext4"; }

        @Override public List<String> aliases() { return List.of("mkfs"); }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(USAGE_ERROR);
                return;
            }
            final String raw = ctx.arg(0);
            final String device = raw.startsWith("/dev/") ? raw.substring(5) : raw;
            // Resolve the device name back to its drive: "sda" also matches the "sda1" system partition.
            for (final ICliComputer.MountInfo mount : ctx.computer().mounts()) {
                if (mount.device().equals(device) || mount.device().startsWith(device)) {
                    final ICliComputer.OpResult result = ctx.computer().formatDrive(mount.drive());
                    if (result.ok()) {
                        // The tool's own banner, a name and a version, which read the same in every language.
                        ctx.out().dim(Text.literal("mke2fs 1.47 (JSC)"));
                        ctx.out().ok(CREATING.with(device));
                    } else {
                        ctx.out().error(CliTexts.SAID_BY.with(name(), result.message()));
                    }
                    return;
                }
            }
            ctx.out().error(CliTexts.SAID_BY.with(name(), NO_SUCH_DEVICE.with(device)));
        }
    }

    /** A path as it was typed, turned into the form the drives understand, by the tree that system keeps. */
    private static String dos(final CliContext ctx, final String posixPath) {
        return PosixPath.toDos(ctx.computer().tree(), posixPath);
    }
}
