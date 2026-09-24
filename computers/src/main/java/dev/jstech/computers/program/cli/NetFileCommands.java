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
    @TextHolder
    static final class ListFiles implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netfile.listfiles.summary", "everything on this machine's disk");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netfile.listfiles.about",
                "Lists every file the disk holds, with how big it is and when it was written.");
        private static final TextKey ABOUT_FLAT = TextKey.of("jsc.cli.netfile.listfiles.about.flat",
                "This disk keeps no folders, so there is nothing to list the contents of: what you see is the"
                        + " whole of it.");
        private static final TextKey EXAMPLE_ALL =
                TextKey.of("jsc.cli.netfile.listfiles.example.all", "everything the disk holds");
        private static final TextKey EMPTY = TextKey.of("jsc.cli.netfile.listfiles.empty", "the disk is empty");
        /* The headings of the listing; the spaces line them up over the columns of the rows below. */
        private static final TextKey HEADER = TextKey.of("jsc.cli.netfile.listfiles.header",
                "NAME                        SIZE  WRITTEN");
        private static final TextKey KEPT = TextKey.of("jsc.cli.netfile.listfiles.kept", "[kept]");
        private static final TextKey TOTAL_ONE =
                TextKey.of("jsc.cli.netfile.listfiles.total.one", "%s file, %s mB");
        private static final TextKey TOTAL_MANY =
                TextKey.of("jsc.cli.netfile.listfiles.total.many", "%s files, %s mB");

        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "listfiles";
        }

        @Override public CommandGroup group() {
            return CommandGroup.FILES;
        }

        @Override public Text summary() {
            return SUMMARY.text();
        }

        @Override public List<Text> description() {
            return List.of(ABOUT.text(), ABOUT_FLAT.text());
        }

        @Override public List<Example> examples() {
            return List.of(new Example("listfiles", EXAMPLE_ALL));
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
                ctx.out().dim(EMPTY);
                return;
            }
            ctx.out().header(HEADER);
            long weight = 0L;
            int files = 0;
            for (final ICliComputer.FsEntry entry : entries) {
                if (entry.isDir()) {
                    continue;
                }
                files++;
                weight += entry.weightMbEq();
                final CliLine.Builder row = CliLine.build().plain(Text.literal(CliText.pad(entry.name(), NAME_W)
                        + CliText.padLeft(CliText.group(entry.weightMbEq()) + " mB", SIZE_W)
                        + "  " + Stamps.of(entry.modified())));
                if (entry.readOnly()) {
                    row.plain("  ").plain(KEPT.text());
                }
                ctx.out().line(row.done());
            }
            ctx.out().dim((files == 1 ? TOTAL_ONE : TOTAL_MANY).with(CliText.group(files), CliText.group(weight)));
        }
    }

    /** What is in a file, put on the glass whole. */
    @TextHolder
    static final class SeeFile implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netfile.seefile.summary", "put a file on the glass");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netfile.seefile.usage", "<file>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netfile.seefile.about",
                "Writes the whole file to the glass at once. A file longer than the glass runs off the top of it;"
                        + " read shows a long one a page at a time instead.");
        private static final TextKey EMPTY = TextKey.of("jsc.cli.netfile.seefile.empty", "that file is empty");

        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "seefile";
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

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public List<String> seeAlso() {
            return List.of("read", "listfiles", "write");
        }

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
            final String content = result.message().english();
            if (content.isEmpty()) {
                ctx.out().dim(EMPTY);
                return;
            }
            for (final String line : content.split("\n", -1)) {
                ctx.out().line(line);
            }
        }
    }

    /** Takes a file off the disk. */
    @TextHolder
    static final class Delete implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netfile.delete.summary", "take a file off the disk");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netfile.delete.usage", "<file>");

        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "delete";
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

        @Override public List<String> seeAlso() {
            return List.of("listfiles", "copy", "rename");
        }

        @Override public void run(final CliContext ctx) {
            say(ctx, ctx.computer().deleteFile(ctx.hasArgs() ? ctx.arg(0) : ""),
                    CliTexts.USAGE.with(name(), usage()), !ctx.hasArgs());
        }
    }

    /** A second file with the same content under another name. */
    @TextHolder
    static final class Copy implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netfile.copy.summary", "a second file with the same content");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netfile.copy.usage", "<file> <new name>");

        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "copy";
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

        @Override public List<String> seeAlso() {
            return List.of("rename", "delete", "listfiles");
        }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            say(ctx, ctx.computer().copyPath(ctx.arg(0), ctx.arg(1)), Text.EMPTY, false);
        }
    }

    /** The same file under another name. */
    @TextHolder
    static final class Rename implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netfile.rename.summary", "give a file another name");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netfile.rename.usage", "<file> <new name>");

        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "rename";
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

        @Override public List<String> seeAlso() {
            return List.of("copy", "delete", "listfiles");
        }

        @Override public void run(final CliContext ctx) {
            if (ctx.argCount() < 2) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            say(ctx, ctx.computer().renamePath(ctx.arg(0), ctx.arg(1)), Text.EMPTY, false);
        }
    }

    /** A file made out of what was typed after it. */
    @TextHolder
    static final class Write implements ICliCommand {

        private static final TextKey SUMMARY =
                TextKey.of("jsc.cli.netfile.write.summary", "make a file out of what you type");
        private static final TextKey USAGE = TextKey.of("jsc.cli.netfile.write.usage", "<file> <text...>");
        private static final TextKey ABOUT = TextKey.of("jsc.cli.netfile.write.about",
                "Writes the text after the name into that file, replacing whatever was there. The kind of file is"
                        + " read from the end of its name.");
        private static final TextKey EXAMPLE_NOTES =
                TextKey.of("jsc.cli.netfile.write.example.notes", "a file with that line in it");

        @Override public CommandScope scope() {
            return NET_FILES;
        }

        @Override public String name() {
            return "write";
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

        @Override public List<Text> description() {
            return List.of(ABOUT.text());
        }

        @Override public List<Example> examples() {
            return List.of(new Example("write notes.txt the cable runs north", EXAMPLE_NOTES));
        }

        @Override public List<String> seeAlso() {
            return List.of("seefile", "listfiles", "delete");
        }

        @Override public void run(final CliContext ctx) {
            if (!ctx.hasArgs()) {
                ctx.out().error(CliTexts.USAGE.with(name(), usage()));
                return;
            }
            say(ctx, ctx.computer().writeFile(ctx.arg(0), ctx.rest(1)), Text.EMPTY, false);
        }
    }

    /** Prints what the machine made of it, or the usage when there was nothing to act on. */
    private static void say(final CliContext ctx, final ICliComputer.FsResult result,
                            final Text usage, final boolean missing) {
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
