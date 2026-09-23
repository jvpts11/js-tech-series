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

    static final class Ls implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "ls"; }

        @Override public String summary() { return "list directory contents"; }

        @Override public String usage() { return "[-l] [directory]"; }

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
                ctx.out().error("ls: " + result.message());
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
                 */
                for (final ICliComputer.FsEntry e : entries) {
                    final String mode = (e.isDir() ? "d" : "-") + (e.readOnly() ? "r--r--r--" : "rw-r--r--");
                    ctx.out().line(mode + CliText.padLeft(
                            e.isDir() ? "" : String.format(Locale.ROOT, "%,d mB", e.weightMbEq()), SIZE_W)
                            + "  " + e.name() + (e.isDir() ? "/" : ""));
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

    static final class Pwd implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "pwd"; }

        @Override public String summary() { return "print the current directory"; }

        @Override public void run(final CliContext ctx) {
            ctx.out().line(PosixPath.render(ctx.computer().tree(), ctx.computer().currentLocation()));
        }
    }

    static final class Cd implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "cd"; }

        @Override public String summary() { return "change the current directory (home when no argument)"; }

        @Override public String usage() { return "[directory]"; }

        @Override public void run(final CliContext ctx) {
            final String target = ctx.hasArgs() ? ctx.rest(0) : "~";
            final ICliComputer.FsResult result = ctx.computer().changeDir(dos(ctx, target));
            if (!result.ok()) {
                ctx.out().error("cd: " + result.message());
            }
        }
    }

    static final class Cat implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "cat"; }

        @Override public String summary() { return "print the content of a file"; }

        @Override public String usage() { return "<file> [file...]"; }

        /**
         * Every file it was given, one after another, which is what the name is short for and what a word with
         * a star in it turns into by the time it gets here.
         */
        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: cat <file>");
                return;
            }
            for (final String named : ctx.args()) {
                final ICliComputer.FsResult result = ctx.computer().readFile(dos(ctx, named));
                if (!result.ok()) {
                    ctx.out().error("cat: " + result.message());
                    continue;
                }
                for (final String line : result.message().split("\n", -1)) {
                    ctx.out().line(line);
                }
            }
        }
    }

    static final class Rm implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "rm"; }

        @Override public String summary() { return "remove a file"; }

        @Override public String usage() { return "<file>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: rm <file>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().deleteFile(dos(ctx, ctx.arg(0)));
            if (!result.ok()) {
                ctx.out().error("rm: " + result.message());
            }
        }
    }

    static final class Mkdir implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "mkdir"; }

        @Override public String summary() { return "create a directory"; }

        @Override public String usage() { return "<directory>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: mkdir <directory>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().makeDir(dos(ctx, ctx.rest(0)));
            if (!result.ok()) {
                ctx.out().error("mkdir: " + result.message());
            }
        }
    }

    static final class Rmdir implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "rmdir"; }

        @Override public String summary() { return "remove an empty directory"; }

        @Override public String usage() { return "<directory>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: rmdir <directory>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().removeDir(dos(ctx, ctx.rest(0)));
            if (!result.ok()) {
                ctx.out().error("rmdir: " + result.message());
            }
        }
    }

    static final class Cp implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "cp"; }

        @Override public String summary() { return "copy a file to another location"; }

        @Override public String usage() { return "<source> <destination>"; }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: cp <source> <destination>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().copyPath(dos(ctx, ctx.arg(0)), dos(ctx, ctx.arg(1)));
            if (!result.ok()) {
                ctx.out().error("cp: " + result.message());
            }
        }
    }

    static final class Mv implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "mv"; }

        @Override public String summary() { return "move a file into a directory, or rename it"; }

        @Override public String usage() { return "<source> <directory|new-name>"; }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: mv <source> <directory|new-name>");
                return;
            }
            final String dest = ctx.arg(1);
            // A bare new name (no slash, no path form) is a rename; anything else moves into a directory.
            final boolean rename = !dest.contains("/") && !dest.startsWith("~") && !dest.equals(".") && !dest.equals("..");
            final ICliComputer.FsResult result = rename
                    ? ctx.computer().renamePath(dos(ctx, ctx.arg(0)), dest)
                    : ctx.computer().movePath(dos(ctx, ctx.arg(0)), dos(ctx, dest));
            if (!result.ok()) {
                ctx.out().error("mv: " + result.message());
            }
        }
    }

    static final class Touch implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "touch"; }

        @Override public String summary() { return "create an empty file"; }

        @Override public String usage() { return "<file>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: touch <file>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().writeFile(dos(ctx, ctx.arg(0)), "");
            if (!result.ok()) {
                ctx.out().error("touch: " + result.message());
            }
        }
    }

    static final class Write implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "write"; }

        @Override public String summary() { return "create or overwrite a file with the given text"; }

        @Override public String usage() { return "<file> <text...>"; }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: write <file> <text...>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().writeFile(dos(ctx, ctx.arg(0)), ctx.rest(1));
            if (!result.ok()) {
                ctx.out().error("write: " + result.message());
            } else if (!result.message().isEmpty()) {
                ctx.out().ok(result.message());
            }
        }
    }

    static final class Run implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "run"; }

        @Override public String summary() { return "execute an .iql script"; }

        @Override public String usage() { return "<file.iql>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: run <file.iql>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().runScript(dos(ctx, ctx.arg(0)));
            if (!result.ok()) {
                ctx.out().error("run: " + result.message());
                return;
            }
            if (result.opResult() != null) {
                if (result.opResult().ok()) {
                    ctx.out().ok(result.opResult().message());
                } else {
                    ctx.out().error(result.opResult().message());
                }
            } else if (!result.message().isEmpty()) {
                ctx.out().line(result.message());
            }
        }
    }

    static final class Clear implements ICliCommand, CliShell.IClearMarker {
        /** Clearing the glass needs no files, so it is the one here that asks nothing of the disk. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "clear"; }

        @Override public String summary() { return "clear the terminal"; }

        @Override public void run(final CliContext ctx) {
            // The shell clears the scrollback because this command is a ClearMarker; nothing to print.
        }
    }

    static final class Man implements ICliCommand {
        /** The manual is about commands, not about files, so a system with no disk still has it. */
        @Override public CommandScope scope() {
            return CommandScope.on(CommandScope.UNIX_SYSTEMS);
        }

        @Override public String name() { return "man"; }

        @Override public String summary() { return "show the manual entry for a command"; }

        @Override public String usage() { return "<command>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("What manual page do you want?");
                ctx.out().dim("For example, try 'man intro'.");
                return;
            }
            final ICliCommand command = ctx.shell().find(ctx.arg(0));
            if (command == null || !command.available(ctx.computer())) {
                ctx.out().error("No manual entry for " + ctx.arg(0));
                return;
            }
            ctx.out().accent(command.name().toUpperCase(Locale.ROOT) + "(1)");
            for (final String line : ManPage.lines(command, true)) {
                ctx.out().line(line);
            }
            ctx.computer().report(JscEvents.MAN_PAGE, command.name());
        }
    }

    static final class Df implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "df"; }

        @Override public String summary() { return "report filesystem space usage"; }

        @Override public void run(final CliContext ctx) {
            final List<ICliComputer.MountInfo> mounts = ctx.computer().mounts();
            if (mounts.isEmpty()) {
                ctx.out().error("df: no filesystems mounted");
                return;
            }
            /*
             * A table, the way df prints one, with where it is mounted in the last column: pushed to the
             * right edge instead, every row ran the width of the glass and folded in half on a window
             * narrower than a monitor.
             */
            ctx.out().header("Filesystem       Size   Used   Avail  Use%  Mounted on");
            for (final ICliComputer.MountInfo m : mounts) {
                final String mount = PosixPath.render(ctx.computer().tree(), DosPath.Location.root(m.drive()));
                if (!m.ready()) {
                    ctx.out().line(CliText.pad(String.format(Locale.ROOT, "/dev/%s%s", m.device(),
                            "  (no medium)"), MOUNT_AT) + mount);
                    continue;
                }
                final long used = Math.max(0L, m.capacityMbEq() - m.freeMbEq());
                final int pct = m.capacityMbEq() <= 0 ? 0 : (int) (used * 100 / m.capacityMbEq());
                ctx.out().line(CliText.pad(String.format(Locale.ROOT, "/dev/%-8s %6s %6s %6s %3d%%",
                        m.device(), size(m.capacityMbEq()), size(used), size(m.freeMbEq()), pct), MOUNT_AT)
                        + mount);
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

    static final class Mkfs implements ICliCommand {
        @Override public CommandScope scope() {
            return POSIX_FILES;
        }

        @Override public String name() { return "mkfs.ext4"; }

        @Override public List<String> aliases() { return List.of("mkfs"); }

        @Override public String summary() { return "build a filesystem on a device (erases it)"; }

        @Override public String usage() { return "/dev/<device>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: mkfs.ext4 /dev/<device>   (see df for the devices)");
                return;
            }
            final String raw = ctx.arg(0);
            final String device = raw.startsWith("/dev/") ? raw.substring(5) : raw;
            // Resolve the device name back to its drive: "sda" also matches the "sda1" system partition.
            for (final ICliComputer.MountInfo mount : ctx.computer().mounts()) {
                if (mount.device().equals(device) || mount.device().startsWith(device)) {
                    final ICliComputer.OpResult result = ctx.computer().formatDrive(mount.drive());
                    if (result.ok()) {
                        ctx.out().dim("mke2fs 1.47 (JSC)");
                        ctx.out().ok("Creating filesystem on /dev/" + device + " ... done");
                    } else {
                        ctx.out().error("mkfs.ext4: " + result.message());
                    }
                    return;
                }
            }
            ctx.out().error("mkfs.ext4: cannot open /dev/" + device + ": No such device");
        }
    }

    /** A path as it was typed, turned into the form the drives understand, by the tree that system keeps. */
    private static String dos(final CliContext ctx, final String posixPath) {
        return PosixPath.toDos(ctx.computer().tree(), posixPath);
    }
}
