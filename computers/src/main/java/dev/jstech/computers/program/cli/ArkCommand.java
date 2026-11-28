/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.fs.Archive;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 67ark at the prompt, Vaultis's archiver as 7-Zip's command line has it: {@code a} packs files into an archive,
 * adding to one that is there, {@code x} takes them out into a folder, and {@code l} lists what one holds. The window
 * packs and unpacks the same archives; this is what a program that keeps a log of its own packs it away with.
 *
 * <p>An archive packs its files together, as a solid one does, so only the whole of it has a packed size: the listing
 * gives it beside the one file of an archive that holds one, and on the total line otherwise.
 */
@TextHolder
final class ArkCommand implements ICliCommand {

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.ark.summary",
            "pack files into an archive, take them out, or list one");
    private static final TextKey USAGE_LINE = TextKey.of("jsc.cli.ark.usage", "a|x|l <archive.ark> [files | folder]");
    private static final TextKey BANNER = TextKey.of("jsc.cli.ark.banner", "67ark %s  Vaultis");
    private static final TextKey ADDING = TextKey.of("jsc.cli.ark.adding", "Adding  %s");
    private static final TextKey EXTRACTING = TextKey.of("jsc.cli.ark.extracting", "Extracting  %s");
    private static final TextKey PACKED_ONE = TextKey.of("jsc.cli.ark.packed_one", "1 file, %s -> %s");
    private static final TextKey PACKED = TextKey.of("jsc.cli.ark.packed", "%s files, %s -> %s");
    private static final TextKey SIZE = TextKey.of("jsc.cli.ark.size", "Size");
    private static final TextKey PACKED_SIZE = TextKey.of("jsc.cli.ark.packed_size", "Packed");
    private static final TextKey NAME = TextKey.of("jsc.cli.ark.name", "Name");
    private static final TextKey FILES = TextKey.of("jsc.cli.ark.files", "%s files");
    private static final TextKey NOTHING_TO_PACK = TextKey.of("jsc.cli.ark.nothing_to_pack",
            "name the files to pack after the archive");
    private static final TextKey CANNOT_READ = TextKey.of("jsc.cli.ark.cannot_read", "%s: %s");
    private static final TextKey HOLDS_ITSELF = TextKey.of("jsc.cli.ark.holds_itself", "an archive cannot hold itself");
    private static final TextKey NOT_AN_ARCHIVE = TextKey.of("jsc.cli.ark.not_an_archive", "%s is not an archive");
    private static final TextKey DAMAGED = TextKey.of("jsc.cli.ark.damaged", "%s is damaged");
    private static final TextKey ALREADY_THERE = TextKey.of("jsc.cli.ark.already_there",
            "%s is already there, and is left as it is");
    private static final String PACKAGE = "jsc:ark";
    /* The widths of the listing's two size columns, as 7-Zip's listing lines its figures up. */
    private static final int COLUMN = 7;
    private static final long KB = 1024L;

    /*
     * A program a player installs, not a word of the system: 'command not found' until 67ark is on the machine. Its id
     * is written out, as the scope reads it, so the command list loads where the game's registries do not.
     */
    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().fromPackage(PACKAGE);
    }

    @Override
    public String name() {
        return "67ark";
    }

    @Override
    public Text summary() {
        return SUMMARY.text();
    }

    @Override
    public Text usage() {
        return USAGE_LINE.text();
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.FILES;
    }

    @Override
    public void run(final CliContext ctx) {
        final String verb = ctx.arg(0).toLowerCase(Locale.ROOT);
        final String archive = ctx.arg(1);
        if (archive.isEmpty() || !List.of("a", "x", "l").contains(verb)) {
            ctx.out().error(CliTexts.USAGE.with(name(), USAGE_LINE.text()));
            return;
        }
        ctx.out().line(BANNER.with(ProgramVersions.of(PACKAGE)));
        switch (verb) {
            case "a" -> add(ctx, archive, ctx.args().subList(2, ctx.argCount()));
            case "x" -> extract(ctx, archive, ctx.arg(2));
            default -> list(ctx, archive);
        }
    }

    /* Packs {@code paths} into {@code archive}, beside what it holds already, a file of the same name replaced. */
    private void add(final CliContext ctx, final String archive, final List<String> paths) {
        if (paths.isEmpty()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), NOTHING_TO_PACK.text()));
            return;
        }
        final Map<String, StoredFile> going = new LinkedHashMap<>();
        final ICliComputer.FsResult there = ctx.computer().readFile(archive);
        if (there.ok()) {
            final String held = there.message().english();
            if (!Archive.isArchive(held)) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), NOT_AN_ARCHIVE.with(archive)));
                return;
            }
            for (final StoredFile file : Archive.unpack(held)) {
                going.put(file.path(), file);
            }
        }
        final List<String> added = new ArrayList<>();
        for (final String path : paths) {
            if (path.equalsIgnoreCase(archive)) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), HOLDS_ITSELF.text()));
                return;
            }
            final ICliComputer.FsResult read = ctx.computer().readFile(path);
            if (!read.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), CANNOT_READ.with(path, read.message())));
                return;
            }
            final String leaf = Archive.leaf(path.replace('\\', '/'));
            going.put(leaf, new StoredFile(leaf, typeOf(leaf), read.message().english()));
            added.add(leaf);
        }
        final String packed;
        try {
            packed = Archive.pack(List.copyOf(going.values()));
        } catch (final Archive.Refused refused) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), refused.text()));
            return;
        }
        final ICliComputer.FsResult written = ctx.computer().writeMade(archive, packed);
        if (!written.ok()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), written.message()));
            return;
        }
        for (final String leaf : added) {
            ctx.out().line(ADDING.with(leaf));
        }
        final String before = size(Archive.originalBytes(packed));
        final String after = size(StoredFile.bytesOf(FileType.ARK, packed));
        ctx.out().ok(going.size() == 1 ? PACKED_ONE.with(before, after) : PACKED.with(going.size(), before, after));
    }

    /* Takes what {@code archive} holds out into {@code folder}, or where the prompt stands; a file there stays. */
    private void extract(final CliContext ctx, final String archive, final String folder) {
        final String held = archiveText(ctx, archive);
        if (held == null) {
            return;
        }
        final List<StoredFile> inside = Archive.unpack(held);
        if (inside.isEmpty()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), DAMAGED.with(archive)));
            return;
        }
        for (final StoredFile file : inside) {
            final String to = into(folder, file.path());
            if (ctx.computer().readFile(to).ok()) {
                ctx.out().warn(ALREADY_THERE.with(to));
                continue;
            }
            final ICliComputer.FsResult written = ctx.computer().writeMade(to, file.content());
            if (!written.ok()) {
                ctx.out().error(CliTexts.SAID_BY.with(name(), written.message()));
                return;
            }
            ctx.out().line(EXTRACTING.with(file.path()));
        }
    }

    /* Lists what {@code archive} holds: each file's size and name, and the packed size where it can be told. */
    private void list(final CliContext ctx, final String archive) {
        final String held = archiveText(ctx, archive);
        if (held == null) {
            return;
        }
        final List<Archive.Entry> entries = Archive.entries(held);
        final String packed = size(StoredFile.bytesOf(FileType.ARK, held));
        ctx.out().line(column(SIZE.text().english()) + " " + column(PACKED_SIZE.text().english()) + "  "
                + NAME.text().english());
        for (final Archive.Entry entry : entries) {
            ctx.out().line(column(size(entry.weighs())) + " " + column(entries.size() == 1 ? packed : "") + "  "
                    + entry.name());
        }
        if (entries.size() > 1) {
            ctx.out().line(column(size(Archive.originalBytes(held))) + " " + column(packed) + "  "
                    + FILES.with(entries.size()).english());
        }
    }

    /* The text of {@code archive}, or null with the reason said when it cannot be read as one. */
    private String archiveText(final CliContext ctx, final String archive) {
        final ICliComputer.FsResult read = ctx.computer().readFile(archive);
        if (!read.ok()) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), CANNOT_READ.with(archive, read.message())));
            return null;
        }
        final String held = read.message().english();
        if (!Archive.isArchive(held)) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), NOT_AN_ARCHIVE.with(archive)));
            return null;
        }
        return held;
    }

    /* {@code name} in {@code folder}, joined with the separator the folder is written with; alone without one. */
    private static String into(final String folder, final String name) {
        if (folder.isEmpty()) {
            return name;
        }
        if (folder.endsWith("\\") || folder.endsWith("/")) {
            return folder + name;
        }
        return folder + (folder.contains("\\") || folder.endsWith(":") ? "\\" : "/") + name;
    }

    /* The kind of file a name is, by its extension; a name with none is text. */
    private static FileType typeOf(final String name) {
        final int dot = name.lastIndexOf('.');
        return FileType.of(dot < 0 ? "" : name.substring(dot + 1));
    }

    /* A size as the listing writes it: bytes under a kilobyte, kilobytes after, as 7-Zip's summary rounds it. */
    private static String size(final long bytes) {
        return bytes < KB ? bytes + " B" : (bytes + KB / 2) / KB + " KB";
    }

    /* A figure set to the right of its column. */
    private static String column(final String text) {
        return " ".repeat(Math.max(0, COLUMN - text.length())) + text;
    }
}
