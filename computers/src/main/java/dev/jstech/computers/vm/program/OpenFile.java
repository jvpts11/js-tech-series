/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;

/**
 * A file a program opened, as C's FILE is, held by the program as one of its own objects.
 *
 * <p>Opening it reads what it holds into the program, and everything after that is the program's own: a line, a
 * value or a character read from where it is up to, text written there, the place moved. Nothing reaches the disk
 * until the file is closed, or until the program ends with it still open, which is when what was written is written.
 * Being an object of the program's, it is kept with the program when the world is saved, open and all.
 *
 * <p>The way it was opened says what may be done with it, as C's modes do: {@code "r"} reads a file that has to be
 * there, {@code "w"} writes one from nothing, {@code "a"} adds at its end, and each with a {@code +} does both. The
 * letters C allows for binary and text files are read and mean nothing, since every file here is text.
 */
@TextHolder
public final class OpenFile {

    /** The type a file a program opened has, named as C names it. */
    public static final String TYPE = "FILE";

    private static final String PATH = "Path";
    private static final String MODE = "Mode";
    private static final String TEXT = "Text";
    private static final String AT = "At";
    private static final String CHANGED = "Changed";
    private static final String OPEN = "Open";

    private static final TextKey CLOSED = TextKey.of("jsc.vm.open_file.closed", "'%s' is closed");
    private static final TextKey NOT_READ = TextKey.of("jsc.vm.open_file.not_read",
            "'%s' was opened with \"%s\", which does not read");
    private static final TextKey NOT_WRITTEN = TextKey.of("jsc.vm.open_file.not_written",
            "'%s' was opened with \"%s\", which does not write");

    private OpenFile() {
    }

    /**
     * The mode a program asked for, as this runtime reads it: {@code r}, {@code w} or {@code a}, with a {@code +} or
     * without, the binary and text letters left out; null when it is none of those.
     */
    public static String mode(final String asked) {
        final String mode = asked == null ? "" : asked.replace("b", "").replace("t", "");
        return switch (mode) {
            case "r", "w", "a", "r+", "w+", "a+" -> mode;
            default -> null;
        };
    }

    /** Whether a file opened that way has to be there already. */
    public static boolean needsFile(final String mode) {
        return mode.startsWith("r");
    }

    /** Whether a file opened that way starts from nothing, whatever it held. */
    public static boolean startsEmpty(final String mode) {
        return mode.startsWith("w");
    }

    /**
     * A file as it is opened, holding {@code text}: where it is up to is its start, or its end for one opened to add
     * to it.
     */
    public static Values.Obj opened(final String path, final String mode, final String text) {
        final Values.Obj file = new Values.Obj(TYPE);
        file.set(PATH, path);
        file.set(MODE, mode);
        file.set(TEXT, text);
        file.set(AT, mode.startsWith("a") ? text.length() : 0);
        file.set(CHANGED, false);
        file.set(OPEN, true);
        return file;
    }

    /** Whether that is a file a program opened and has not closed. */
    public static boolean isOpen(final Object value) {
        return value instanceof Values.Obj file && TYPE.equals(file.type()) && Boolean.TRUE.equals(file.get(OPEN));
    }

    /** Where the file came from, which it goes back to when it is closed. */
    public static String path(final Values.Obj file) {
        return String.valueOf(file.get(PATH));
    }

    /** What the file holds now, written to and all. */
    public static String text(final Values.Obj file) {
        return String.valueOf(file.get(TEXT));
    }

    /** Whether anything was written to it since it was opened, and so has to be written back. */
    public static boolean changed(final Values.Obj file) {
        return Boolean.TRUE.equals(file.get(CHANGED)) || startsEmpty(mode(file));
    }

    /** Closes it: nothing more can be done with it, and a second close is nothing. */
    public static void close(final Values.Obj file) {
        file.set(OPEN, false);
    }

    /** The next line, without its line break, into {@code into}; false at the end, with nothing read. */
    static boolean readLine(final Process process, final Values.Obj file, final Object[] into, final int line) {
        final String text = readable(file, line);
        final int at = at(file);
        if (at >= text.length()) {
            into[0] = process.text("", line);
            return false;
        }
        final int end = text.indexOf('\n', at);
        into[0] = process.text(text.substring(at, end < 0 ? text.length() : end), line);
        file.set(AT, end < 0 ? text.length() : end + 1);
        return true;
    }

    /** The next character, or -1 at the end, as C's EOF is. */
    static int read(final Values.Obj file, final int line) {
        final String text = readable(file, line);
        final int at = at(file);
        if (at >= text.length()) {
            return -1;
        }
        file.set(AT, at + 1);
        return text.charAt(at);
    }

    /**
     * The next value of {@code kind}, read from where the file is up to as scanf reads one; null when what is there is
     * not one, and then the file is left where it was, as C leaves it.
     */
    static Object scan(final Process process, final Values.Obj file, final String kind, final int line) {
        final String text = readable(file, line);
        final int at = at(file);
        if (at >= text.length()) {
            return null;
        }
        final ScanReading.Read read = ScanReading.of(kind, text.substring(at));
        if (read.value() == null) {
            return null;
        }
        file.set(AT, text.length() - read.rest().length());
        return read.value() instanceof String word ? process.text(word, line) : read.value();
    }

    /**
     * Writes {@code piece} where the file is up to, over what was there, or at its end for one opened to add to it,
     * as C writes.
     */
    static void write(final Process process, final Values.Obj file, final String piece, final int line) {
        final String text = open(file, line);
        final String mode = mode(file);
        if ("r".equals(mode)) {
            throw new Halt(Halt.Reason.REFUSED, line, NOT_WRITTEN.with(path(file), mode));
        }
        final int at = mode.startsWith("a") ? text.length() : Math.min(at(file), text.length());
        final String after = text.substring(Math.min(text.length(), at + piece.length()));
        file.set(TEXT, process.text(text.substring(0, at) + piece + after, line));
        // The text it replaces is the runtime's own, which the program cannot reach, so its cost goes with it; the
        // heap holds things by identity, so only that very copy is let go of.
        process.heap0().release(text);
        file.set(AT, at + piece.length());
        file.set(CHANGED, true);
    }

    /** Moves where the file is up to, kept between its start and its end. */
    static void seek(final Values.Obj file, final int place, final int line) {
        final String text = open(file, line);
        file.set(AT, Math.max(0, Math.min(text.length(), place)));
    }

    /** Where the file is up to, in characters from its start. */
    static int position(final Values.Obj file, final int line) {
        open(file, line);
        return at(file);
    }

    /** Whether the file is up to its end, with nothing more to read. */
    static boolean atEnd(final Values.Obj file, final int line) {
        return at(file) >= open(file, line).length();
    }

    private static String readable(final Values.Obj file, final int line) {
        final String text = open(file, line);
        final String mode = mode(file);
        if ("w".equals(mode) || "a".equals(mode)) {
            throw new Halt(Halt.Reason.REFUSED, line, NOT_READ.with(path(file), mode));
        }
        return text;
    }

    /** What an open file holds, or a halt for one that is closed. */
    private static String open(final Values.Obj file, final int line) {
        if (!Boolean.TRUE.equals(file.get(OPEN))) {
            throw new Halt(Halt.Reason.REFUSED, line, CLOSED.with(path(file)));
        }
        return text(file);
    }

    private static String mode(final Values.Obj file) {
        return String.valueOf(file.get(MODE)).toLowerCase(Locale.ROOT);
    }

    private static int at(final Values.Obj file) {
        return Numbers.toInt(file.get(AT));
    }
}
