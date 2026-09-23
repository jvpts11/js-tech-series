/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * The network appliance's words for the files it keeps.
 *
 * <p>MC-NET used to borrow the DOS family's verbs, which read oddly on a machine that is not a personal
 * computer and never had a folder in its life. These are said in whole words instead: what a thing does,
 * spelled out, with nothing abbreviated and nothing shouted.
 *
 * <p>There is no verb here for folders, and that is not an omission. This disk is flat: a name is a name and
 * there is nowhere to put it but the disk, so there is nothing to change into, nothing to make and nothing to
 * draw a tree of. The palette is smaller than either family's because the machine is simpler than either.
 *
 * <p>Each of these is a face over the same service the other families' verbs call, so a listing here and a
 * listing at a DOS prompt are the same listing said differently, never two implementations that drift.
 */
final class NetFileCommands {

    /** The appliance's own verbs, on a system that keeps files at all. */
    private static final CommandScope NET_FILES =
            CommandScope.on(CommandScope.NET_SYSTEMS).needing(CommandScope.Need.FILES);

    /** How wide the name column of a listing is, leaving room for the size and when it was written. */
    private static final int NAME_W = 22;

    /** And how wide the size column is. */
    private static final int SIZE_W = 10;

    private NetFileCommands() {
    }

    static List<ICliCommand> all() {
        return List.of(new ListFiles(), new SeeFile(), new Delete(), new Copy(), new Rename(), new Write());
    }

    /**
     * Everything on the disk.
     *
     * <p>No folder column and no folders in it: what is on a flat disk is every file there is, so the
     * listing is the disk.
     */
    static final class ListFiles implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "listfiles";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public String summary() {
            return "everything on this machine's disk";
        }

        @Override public List<String> description() {
            return List.of("Lists every file the disk holds, with how big it is and when it was written.",
                    "",
                    "This disk keeps no folders, so there is nothing to list the contents of: what you see",
                    "is the whole of it.");
        }

        @Override public List<Example> examples() {
            return List.of(new Example("listfiles", "everything the disk holds"));
        }

        @Override public List<String> seeAlso() {
            return List.of("seefile", "read", "delete");
        }

        @Override public void run(final CliContext ctx) {
            final ICliComputer.FsResult result = ctx.computer().listDisk("");
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            final List<ICliComputer.FsEntry> entries = result.entries();
            if (entries == null || entries.isEmpty()) {
                ctx.out().dim("the disk is empty");
                return;
            }
            ctx.out().header(CliText.pad("NAME", NAME_W) + CliText.padLeft("SIZE", SIZE_W) + "  WRITTEN");
            long weight = 0L;
            int files = 0;
            for (final ICliComputer.FsEntry entry : entries) {
                if (entry.isDir()) {
                    continue;
                }
                files++;
                weight += entry.weightMbEq();
                ctx.out().line(CliText.pad(entry.name(), NAME_W)
                        + CliText.padLeft(CliText.group(entry.weightMbEq()) + " mB", SIZE_W)
                        + "  " + Stamps.of(entry.modified())
                        + (entry.readOnly() ? "  [kept]" : ""));
            }
            ctx.out().dim(CliText.group(files) + (files == 1 ? " file, " : " files, ")
                    + CliText.group(weight) + " mB");
        }
    }

    /** What is in a file, put on the glass whole. */
    static final class SeeFile implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "seefile";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public String summary() {
            return "put a file on the glass";
        }

        @Override public String usage() {
            return "<file>";
        }

        @Override public List<String> description() {
            return List.of("Writes the whole file to the glass at once. A file longer than the glass runs",
                    "off the top of it; read shows a long one a page at a time instead.");
        }

        @Override public List<String> seeAlso() {
            return List.of("read", "listfiles", "write");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: seefile <file>");
                return;
            }
            final ICliComputer.FsResult result = ctx.computer().readFile(ctx.arg(0));
            if (!result.ok()) {
                ctx.out().error(result.message());
                return;
            }
            final String content = result.message();
            if (content.isEmpty()) {
                ctx.out().dim("that file is empty");
                return;
            }
            for (final String line : content.split("\n", -1)) {
                ctx.out().line(line);
            }
        }
    }

    /** Takes a file off the disk. */
    static final class Delete implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "delete";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public String summary() {
            return "take a file off the disk";
        }

        @Override public String usage() {
            return "<file>";
        }

        @Override public List<String> seeAlso() {
            return List.of("listfiles", "copy", "rename");
        }

        @Override public void run(final CliContext ctx) {
            say(ctx, ctx.computer().deleteFile(ctx.hasArgs() ? ctx.arg(0) : ""),
                    "usage: delete <file>", !ctx.hasArgs());
        }
    }

    /** A second file with the same content under another name. */
    static final class Copy implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "copy";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public String summary() {
            return "a second file with the same content";
        }

        @Override public String usage() {
            return "<file> <new name>";
        }

        @Override public List<String> seeAlso() {
            return List.of("rename", "delete", "listfiles");
        }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: copy <file> <new name>");
                return;
            }
            say(ctx, ctx.computer().copyPath(ctx.arg(0), ctx.arg(1)), "", false);
        }
    }

    /** The same file under another name. */
    static final class Rename implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "rename";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public String summary() {
            return "give a file another name";
        }

        @Override public String usage() {
            return "<file> <new name>";
        }

        @Override public List<String> seeAlso() {
            return List.of("copy", "delete", "listfiles");
        }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error("usage: rename <file> <new name>");
                return;
            }
            say(ctx, ctx.computer().renamePath(ctx.arg(0), ctx.arg(1)), "", false);
        }
    }

    /** A file made out of what was typed after it. */
    static final class Write implements ICliCommand {
        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "write";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public String summary() {
            return "make a file out of what you type";
        }

        @Override public String usage() {
            return "<file> <text...>";
        }

        @Override public List<String> description() {
            return List.of("Writes the text after the name into that file, replacing whatever was there.",
                    "The kind of file is read from the end of its name.");
        }

        @Override public List<Example> examples() {
            return List.of(new Example("write notes.txt the cable runs north",
                    "a file with that line in it"));
        }

        @Override public List<String> seeAlso() {
            return List.of("seefile", "listfiles", "delete");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error("usage: write <file> <text...>");
                return;
            }
            say(ctx, ctx.computer().writeFile(ctx.arg(0), ctx.rest(1)), "", false);
        }
    }

    /** Prints what the machine made of it, or the usage when there was nothing to act on. */
    private static void say(final CliContext ctx, final ICliComputer.FsResult result,
                            final String usage, final boolean missing) {
        if (missing) {
            ctx.out().error(usage);
            return;
        }
        if (!result.ok()) {
            ctx.out().error(result.message());
            return;
        }
        ctx.out().styled(result.message(), CliStyle.OK);
    }
}
