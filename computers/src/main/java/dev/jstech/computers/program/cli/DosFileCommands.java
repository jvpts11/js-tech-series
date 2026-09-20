/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import java.util.List;
import java.util.Locale;

/**
 * The DOS family's file commands, in the words MC-DOS and the Frames editions use.
 *
 * <p>Moved here from BuiltinCommands, which keeps the lists that say which system gets which command.
 */
final class DosFileCommands {

    /** The DOS family's words for files, on a system that keeps files at all: MC-NET keeps none. */
    private static final CommandScope DOS_FILES =
            CommandScope.on(CommandScope.DOS_SYSTEMS).needing(CommandScope.Need.FILES);

    /**
     * How wide the column saying when a file was last written is, in a listing.
     *
     * <p>As narrow as the longest stamp and no narrower: every column these two take is a column the name
     * does not have, and the name is the part a player has to be able to read and type back.
     */
    private static final int STAMP_W = 14;

    /** And how wide the one saying how big it is, which {@code <DIR>} stands in for. */
    private static final int SIZE_W = 10;

    private DosFileCommands() {
    }

    static final class Format implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() {
            return "format";
        }

        @Override public String summary() {
            return "erase everything on a drive";
        }

        @Override public String usage() {
            return "<drive>: [/y]";
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: format <drive>: [/y]");
                return;
            }
            final String arg = ctx.arg(0).toUpperCase(Locale.ROOT);
            if (arg.isEmpty() || !Character.isLetter(arg.charAt(0))) {
                ctx.out().error("format: invalid drive: " + ctx.arg(0));
                return;
            }
            final char drive = arg.charAt(0);
            // The real format asks before destroying a volume; a stateless shell asks for the /y flag.
            final boolean confirmed = ctx.argCount() > 1 && ctx.arg(1).equalsIgnoreCase("/y");
            if (!confirmed) {
                ctx.out().styled("WARNING: ALL DATA ON DRIVE " + drive + ": WILL BE LOST!", CliStyle.ERROR);
                ctx.out().dim("Run 'format " + drive + ": /y' to proceed.");
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().formatDrive(drive);
            for (final String line : result.message().split("\n", -1)) {
                ctx.out().styled(line, result.ok() ? CliStyle.OK : CliStyle.ERROR);
            }
        }
    }

    static final class Dir implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "dir"; }

        @Override public String summary() { return "list the contents of a directory"; }

        @Override public String usage() { return "[directory]"; }

        @Override public void run(final CliContext ctx) {
            final String dir = ctx.hasArgs() ? ctx.rest(0) : "";
            final ICliComputer.FsResult result = ctx.computer().listDisk(dir);
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            ctx.out().accent(" Directory of "
                    + DosPath.resolve(ctx.computer().currentLocation(), dir).dosPath());
            ctx.out().blank();
            final List<ICliComputer.FsEntry> entries = result.entries();
            if (entries.isEmpty()) {
                ctx.out().dim("File Not Found");
                return;
            }
            int dirs = 0;
            int files = 0;
            long bytes = 0L;
            /*
             * Columns, the way DIR has always printed them: when it was last written, how big it is, and
             * then the name. The name goes last on purpose, because it is the one thing that has no width
             * anybody can plan for, and a listing whose last column runs long is still a listing that lines
             * up. Written as a settings row instead, every line ran the whole width of the glass and folded
             * in half on any window narrower than a monitor.
             */
            for (final ICliComputer.FsEntry entry : entries) {
                final String stamp = CliText.pad(formatStamp(entry.modified()), STAMP_W);
                if (entry.isDir()) {
                    dirs++;
                    ctx.out().line(stamp + CliText.pad("<DIR>", SIZE_W) + entry.name());
                } else {
                    files++;
                    bytes += entry.weightMbEq();
                    ctx.out().line(stamp + CliText.padLeft(CliText.group(entry.weightMbEq()) + " mB",
                            SIZE_W - 2) + "  " + entry.name() + (entry.readOnly() ? "  [RO]" : ""));
                }
            }
            ctx.out().blank();
            ctx.out().dim(CliText.group(files) + " File(s), " + CliText.group(dirs) + " Dir(s), "
                    + CliText.group(bytes) + " mB");
        }
    }

    /**
     * Prints the content of a file on the system disk to the console.
     * Refuses to open {@code .dat} (read-only storage projections).
     */
    static final class Type implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "type"; }

        @Override public String summary() { return "print the content of a file"; }

        @Override public String usage() { return "<file>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: type <file>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().readFile(ctx.arg(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            // Print each line of the file content as a plain output line.
            final String content = result.message();
            if (content.isEmpty()) {
                ctx.out().dim("(empty file)");
                return;
            }
            for (final String line : content.split("\n", -1)) {
                ctx.out().line(line);
            }
        }
    }

    /**
     * Deletes a file from the system disk. Refuses to delete {@code .dat} storage projections;
     * use the Network Interactor to move items out of disk storage.
     */
    static final class Del implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "del"; }

        @Override public List<String> aliases() { return List.of("erase"); }

        @Override public String summary() { return "delete a file from the system disk"; }

        @Override public String usage() { return "<file>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: del <file>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().deleteFile(ctx.arg(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            ctx.out().styled(result.message(), CliStyle.OK);
        }
    }

    /**
     * Creates or overwrites a file on the system disk with the given text. The file type is inferred
     * from the extension; non-editable types ({@code .dat}, {@code .log}) are refused.
     */
    static final class Write implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "write"; }

        @Override public List<String> aliases() { return List.of("save"); }

        @Override public String summary() { return "create or overwrite a file on the system disk"; }

        @Override public String usage() { return "<file> <text...>"; }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 1) {
                ctx.out().error("usage: write <file> <text...>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().writeFile(ctx.arg(0), ctx.rest(1));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            ctx.out().styled(result.message(), CliStyle.OK);
        }
    }

    /**
     * Reads a {@code .iql} file from the system disk and executes it as an IQL statement, routing
     * through the same dispatch path as the {@code operation} command.
     */
    static final class Run implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "run"; }

        @Override public String summary() { return "execute an .iql script from the system disk"; }

        @Override public String usage() { return "<file.iql>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: run <file.iql>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().runScript(ctx.arg(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            // Forward the underlying OpResult style: OK in green, fail in red.
            final ICliComputer.OpResult op = result.opResult();
            if (op != null) {
                ctx.out().styled(op.message(), op.ok() ? CliStyle.OK : CliStyle.ERROR);
            } else {
                ctx.out().styled(result.message(), CliStyle.OK);
            }
        }
    }

    /**
     * Shows or changes the current directory. With no argument it prints the current path (DOS
     * behaviour); with a path it changes to that directory relative to the current one.
     */
    static final class Cd implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "cd"; }

        @Override public List<String> aliases() { return List.of("chdir"); }

        @Override public String summary() { return "show or change the current directory"; }

        @Override public String usage() { return "[directory]"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().line(ctx.computer().currentLocation().dosPath());
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().changeDir(ctx.rest(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
            }
        }
    }

    /** Creates a directory on the current drive. */
    static final class Mkdir implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "mkdir"; }

        @Override public List<String> aliases() { return List.of("md"); }

        @Override public String summary() { return "create a directory"; }

        @Override public String usage() { return "<directory>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: mkdir <directory>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().makeDir(ctx.rest(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
            }
        }
    }

    /** Removes an empty directory from the current drive. */
    static final class Rmdir implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "rmdir"; }

        @Override public List<String> aliases() { return List.of("rd"); }

        @Override public String summary() { return "remove an empty directory"; }

        @Override public String usage() { return "<directory>"; }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: rmdir <directory>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().removeDir(ctx.rest(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
            }
        }
    }

    /** Copies a file (or directory subtree) to a new location. */
    static final class Copy implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "copy"; }

        @Override public String summary() { return "copy a file to another location"; }

        @Override public String usage() { return "<source> <destination>"; }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: copy <source> <destination>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().copyPath(ctx.arg(0), ctx.arg(1));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            ctx.out().styled(result.message(), CliStyle.OK);
        }
    }

    /** Moves a file (or directory subtree) into another directory. */
    static final class Move implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "move"; }

        @Override public String summary() { return "move a file into another directory"; }

        @Override public String usage() { return "<source> <directory>"; }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: move <source> <directory>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().movePath(ctx.arg(0), ctx.arg(1));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            ctx.out().styled(result.message(), CliStyle.OK);
        }
    }

    /** Renames a file or directory in place. */
    static final class Ren implements ICliCommand {
        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "ren"; }

        @Override public List<String> aliases() { return List.of("rename"); }

        @Override public String summary() { return "rename a file or directory"; }

        @Override public String usage() { return "<file> <new name>"; }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: ren <file> <new name>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().renamePath(ctx.arg(0), ctx.arg(1));
            if (!result.ok()) {
                ctx.out().error(result.message());
            }
        }
    }

    /**
     * When a file was last written.
     *
     * <p>Answered by {@link Stamps}, which every family's listing asks, so the same file read at two
     * prompts never gives two different hours.
     */
    private static String formatStamp(final long ticks) {
        return Stamps.of(ticks);
    }
}
