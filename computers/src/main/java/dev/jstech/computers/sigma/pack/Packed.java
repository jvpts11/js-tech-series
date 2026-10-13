/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.pack;

import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A built package: what it says about itself, and everything it is made of.
 *
 * <p>It is one piece of text, and deliberately so. A package travels to a Mirror and from there onto
 * other people's machines, and a player who is about to install one written by somebody else can open
 * it in the editor and read every line of it first. Nothing is compressed, nothing is encoded, and
 * there is nowhere for anything to hide.
 */
@TextHolder
public record Packed(Manifest manifest, Map<String, String> files) {

    /** What the first line of a built package says. */
    public static final String HEAD = ".pkg 1";

    /** The line that starts each file inside it. */
    private static final String MARK = "--- ";

    /**
     * What a line of a file is written with when it would otherwise be read as something else: a line that
     * starts like the one that starts each file, or like this. It is taken off again when the package is read,
     * so a file comes back exactly as it went in, and a player reading the package still reads every line.
     */
    private static final String ESCAPE = "\\";

    /**
     * What follows the name on a file's first line when its text does not end in a newline. Without it every
     * body would come back with a newline the file never had.
     */
    private static final String NO_FINAL_NEWLINE = " \\";

    /** The extension a built package is written under. */
    public static final String EXTENSION = ".cpk";

    /* A file the manifest and the package disagree about, on the manifest's files line, which goes in as data. */
    private static final TextKey NAMED_NOT_PACKED = TextKey.of("jsc.sigma.packed.named_not_packed",
            "%s: %s is named but not in the package");
    private static final TextKey PACKED_NOT_NAMED = TextKey.of("jsc.sigma.packed.packed_not_named",
            "%s: %s is in the package but not named");
    private static final TextKey UNSAFE_FILE_NAME = TextKey.of("jsc.sigma.packed.unsafe_file_name",
            "%s: %s is not a file name a package may carry; it must stay inside the package's own folder");

    public Packed {
        files = new LinkedHashMap<>(files);
    }

    /** What to call the file this is written to. */
    public String fileName() {
        return this.manifest.name() + "-" + this.manifest.version() + EXTENSION;
    }

    /** Writes the whole thing out. */
    public String write() {
        final StringBuilder text = new StringBuilder(HEAD).append('\n');
        text.append(this.manifest.write());
        for (final Map.Entry<String, String> file : this.files.entrySet()) {
            final boolean endsWithNewline = file.getValue().endsWith("\n");
            text.append(MARK).append(file.getKey()).append(endsWithNewline ? "" : NO_FINAL_NEWLINE).append('\n');
            text.append(escaped(file.getValue()));
            if (!endsWithNewline) {
                text.append('\n');
            }
        }
        return text.toString();
    }

    /** A file's text with every line that could be mistaken for the start of another file marked as its own. */
    private static String escaped(final String content) {
        final StringBuilder out = new StringBuilder(content.length());
        int start = 0;
        while (true) {
            final int end = content.indexOf('\n', start);
            final String line = content.substring(start, end < 0 ? content.length() : end);
            if (line.startsWith(MARK) || line.startsWith(ESCAPE)) {
                out.append(ESCAPE);
            }
            out.append(line);
            if (end < 0) {
                return out.toString();
            }
            out.append('\n');
            start = end + 1;
        }
    }

    /** Reads one back, or null when the text is not a package at all or names one file twice. */
    public static Packed read(final String text) {
        if (text == null) {
            return null;
        }
        final String[] lines = text.split("\n", -1);
        if (!lines[0].trim().equals(HEAD)) {
            return null;
        }
        /*
         * A text ending in a newline splits with an empty piece after it. That piece is the end of the
         * last line, not a blank line of its own, and counting it would grow every package by one line
         * each time it went through a Mirror.
         */
        final int last = text.endsWith("\n") ? lines.length - 1 : lines.length;
        final StringBuilder head = new StringBuilder();
        final Map<String, String> files = new LinkedHashMap<>();
        String name = null;
        boolean noFinalNewline = false;
        StringBuilder body = null;
        for (int i = 1; i < last; i++) {
            final String line = lines[i];
            if (line.startsWith(MARK)) {
                if (name != null && !finished(files, name, body, noFinalNewline)) {
                    return null;
                }
                String header = line.substring(MARK.length());
                noFinalNewline = header.endsWith(NO_FINAL_NEWLINE);
                if (noFinalNewline) {
                    header = header.substring(0, header.length() - NO_FINAL_NEWLINE.length());
                }
                name = header.trim();
                body = new StringBuilder();
                continue;
            }
            if (name == null) {
                head.append(line).append('\n');
            } else {
                body.append(line.startsWith(ESCAPE) ? line.substring(ESCAPE.length()) : line).append('\n');
            }
        }
        if (name != null && !finished(files, name, body, noFinalNewline)) {
            return null;
        }
        return new Packed(Manifest.read(head.toString()), files);
    }

    /** What is wrong with it, if anything, in English: the form it takes as data. */
    public List<String> problems() {
        return this.problemTexts().stream().map(Text::english).toList();
    }

    /**
     * What is wrong with it, if anything, as the player reads it.
     *
     * <p>A package is checked when it is built and again when it is installed, because the two can be
     * far apart: it may have crossed a Mirror and a world save in between.
     */
    public List<Text> problemTexts() {
        final List<Text> found = new ArrayList<>(this.manifest.problemTexts());
        for (final String named : this.manifest.files()) {
            if (!this.files.containsKey(named)) {
                found.add(NAMED_NOT_PACKED.with("files", named));
            }
        }
        for (final String held : this.files.keySet()) {
            if (!this.manifest.files().contains(held)) {
                found.add(PACKED_NOT_NAMED.with("files", held));
            }
            if (!isSafeFileName(held)) {
                found.add(UNSAFE_FILE_NAME.with("files", held));
            }
        }
        return found;
    }

    /**
     * Whether a file of a package may be written under the package's own folder: a relative path of plain names
     * that stays inside it. A name that climbs out ({@code ..}), starts at a root, or carries a drive or a
     * backslash would put the file where another program's files or the system's own live.
     */
    public static boolean isSafeFileName(final String name) {
        if (name == null || name.isEmpty() || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0) {
            return false;
        }
        for (final String segment : name.split("/", -1)) {
            if (segment.equals(".") || segment.equals("..") || segment.isBlank() || !FsPaths.isValidName(segment)) {
                return false;
            }
        }
        return true;
    }

    /** How much disk this takes, in the same characters the filesystem weighs. */
    public int size() {
        return this.write().length();
    }

    /** Files a finished body under its name, or says false when that name already has one. */
    private static boolean finished(final Map<String, String> files, final String name, final StringBuilder body,
                                    final boolean noFinalNewline) {
        final String text = noFinalNewline ? body.substring(0, body.length() - 1) : body.toString();
        return files.putIfAbsent(name, text) == null;
    }
}
