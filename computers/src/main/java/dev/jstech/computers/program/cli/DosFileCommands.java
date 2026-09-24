/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;


import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
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

    @TextHolder
    static final class Format implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.format.summary", "erase everything on a drive");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.format.usage", "<drive>: [/y]");
        private static final TextKey INVALID_DRIVE =
                TextKey.of("jsc.cli.dos.format.invalid_drive", "invalid drive: %s");
        private static final TextKey WARNING =
                TextKey.of("jsc.cli.dos.format.warning", "WARNING: ALL DATA ON DRIVE %s: WILL BE LOST!");
        private static final TextKey PROCEED =
                TextKey.of("jsc.cli.dos.format.proceed", "Run 'format %s: /y' to proceed.");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() {
            return "format";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public Text usage() {
            return USAGE.text();
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final String arg = ctx.arg(0).toUpperCase(Locale.ROOT);
            if (arg.isEmpty() || !Character.isLetter(arg.charAt(0))) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), INVALID_DRIVE.with(ctx.arg(0))));
                return;
            }
            final char drive = arg.charAt(0);
            // The real format asks before destroying a volume; a stateless shell asks for the /y flag.
            final boolean confirmed = ctx.argCount() > 1 && ctx.arg(1).equalsIgnoreCase("/y");
            if (!confirmed) {
                ctx.out().styled(WARNING.with(String.valueOf(drive)), CliStyle.ERROR);
                ctx.out().dim(PROCEED.with(String.valueOf(drive)));
                return;
            }
            final ICliComputer.OpResult result = ctx.computer().formatDrive(drive);
            ctx.out().styled(result.message(), result.ok() ? CliStyle.OK : CliStyle.ERROR);
        }
    }

    @TextHolder
    static final class Dir implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.dir.summary", "list the contents of a directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.dir.usage", "[directory]");
        private static final TextKey DIRECTORY_OF = TextKey.of("jsc.cli.dos.dir.directory_of", " Directory of %s");
        private static final TextKey NOT_FOUND = TextKey.of("jsc.cli.dos.dir.not_found", "File Not Found");
        private static final TextKey TOTALS =
                TextKey.of("jsc.cli.dos.dir.totals", "%s File(s), %s Dir(s), %s mB");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "dir"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            final String dir = ctx.hasArgs() ? ctx.rest(0) : "";
            final ICliComputer.FsResult result = ctx.computer().listDisk(dir);
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            ctx.out().accent(DIRECTORY_OF.with(DosPath.resolve(ctx.computer().currentLocation(), dir).dosPath()));
            ctx.out().blank();
            final List<ICliComputer.FsEntry> entries = result.entries();
            if (entries.isEmpty()) {
                ctx.out().dim(NOT_FOUND);
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
             *
             * Every column is data, the marks included: <DIR>, a unit and [RO] are what the listing has always
             * printed, in every language DOS was sold in.
             */
            for (final ICliComputer.FsEntry entry : entries) {
                final String stamp = CliText.pad(formatStamp(entry.modified()), STAMP_W);
                if (entry.isDir()) {
                    dirs++;
                    ctx.out().line(Text.literal(stamp + CliText.pad("<DIR>", SIZE_W) + entry.name()));
                } else {
                    files++;
                    bytes += entry.weightMbEq();
                    ctx.out().line(Text.literal(stamp + CliText.padLeft(CliText.group(entry.weightMbEq()) + " mB",
                            SIZE_W - 2) + "  " + entry.name() + (entry.readOnly() ? "  [RO]" : "")));
                }
            }
            ctx.out().blank();
            ctx.out().dim(TOTALS.with(CliText.group(files), CliText.group(dirs), CliText.group(bytes)));
        }
    }

    /**
     * Prints the content of a file on the system disk to the console.
     * Refuses to open {@code .dat} (read-only storage projections).
     */
    @TextHolder
    static final class Type implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.dos.type.summary", "print the content of a file");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.type.usage", "<file>");
        private static final TextKey EMPTY_FILE = TextKey.of("jsc.cli.dos.type.empty_file", "(empty file)");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "type"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().readFile(ctx.arg(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            // Print each line of the file content as a plain output line.
            final String content = result.message().english();
            if (content.isEmpty()) {
                ctx.out().dim(EMPTY_FILE);
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
    @TextHolder
    static final class Del implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.del.summary", "delete a file from the system disk");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.del.usage", "<file>");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "del"; }

        @Override public List<String> aliases() { return List.of("erase"); }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
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
    @TextHolder
    static final class Write implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.write.summary", "create or overwrite a file on the system disk");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.write.usage", "<file> <text...>");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "write"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public List<String> aliases() { return List.of("save"); }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 1) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
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
    @TextHolder
    static final class Run implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.run.summary", "execute an .iql script from the system disk");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.run.usage", "<file.iql>");

        @Override public CommandScope scope() {
            return DOS_FILES;
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
    @TextHolder
    static final class Cd implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.cd.summary", "show or change the current directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.cd.usage", "[directory]");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "cd"; }

        @Override public List<String> aliases() { return List.of("chdir"); }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

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
    @TextHolder
    static final class Mkdir implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.dos.mkdir.summary", "create a directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.mkdir.usage", "<directory>");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "mkdir"; }

        @Override public List<String> aliases() { return List.of("md"); }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().makeDir(ctx.rest(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
            }
        }
    }

    /** Removes an empty directory from the current drive. */
    @TextHolder
    static final class Rmdir implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.dos.rmdir.summary", "remove an empty directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.rmdir.usage", "<directory>");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "rmdir"; }

        @Override public List<String> aliases() { return List.of("rd"); }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().removeDir(ctx.rest(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
            }
        }
    }

    /** Copies a file (or directory subtree) to a new location. */
    @TextHolder
    static final class Copy implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.copy.summary", "copy a file to another location");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.copy.usage", "<source> <destination>");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "copy"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
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
    @TextHolder
    static final class Move implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.dos.move.summary", "move a file into another directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.move.usage", "<source> <directory>");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "move"; }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
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
    @TextHolder
    static final class Ren implements ICliCommand {

        private static final TextKey SUMMARY = TextKey.of("jsc.cli.dos.ren.summary", "rename a file or directory");
        private static final TextKey USAGE = TextKey.of("jsc.cli.dos.ren.usage", "<file> <new name>");

        @Override public CommandScope scope() {
            return DOS_FILES;
        }

        @Override public String name() { return "ren"; }

        @Override public List<String> aliases() { return List.of("rename"); }

        @Override public CommandGroup group() { return CommandGroup.FILES; }

        @Override public Text summary() { return SUMMARY.text(); }

        @Override public Text usage() { return USAGE.text(); }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
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
