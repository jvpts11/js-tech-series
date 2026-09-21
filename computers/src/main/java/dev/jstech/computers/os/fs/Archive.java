/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Many files packed into one, and taken back out again.
 *
 * <p>The archive really weighs less than what went into it, which is the whole reason it exists: a disk in
 * this mod counts the bytes of what is on it, so packing a folder of logs away is how a small disk is made to
 * stretch. Text that repeats itself packs hardest, so a log collapses much further than a config does, and
 * the player learns that by watching the numbers rather than by being told.
 *
 * <p>The listing at the top is left as plain text on purpose. Opening an archive to see what is inside it
 * then costs nothing, because the names, the types and the original sizes are readable without unpacking
 * anything at all.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class Archive {

    /** What every archive starts with, so a file that is not one is recognised before it is read. */
    public static final String MAGIC = "JSARK1";

    /** The extension an archive is written under. */
    public static final String EXTENSION = "ark";

    /** How many files one archive may hold, so a bad read cannot ask for an unbounded list. */
    public static final int MAX_ENTRIES = 512;

    private static final String SEPARATOR = "\t";
    private static final char NEWLINE = '\n';

    /** One file inside an archive, as the listing at the top of it says. */
    public record Entry(String name, FileType type, int originalBytes) {

        public Entry {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("an archived file must have a name");
            }
            if (type == null) {
                throw new IllegalArgumentException("an archived file must have a type");
            }
            if (originalBytes < 0) {
                throw new IllegalArgumentException("an archived file cannot have negative size");
            }
        }
    }

    private Archive() {
    }

    /** Whether this content is an archive, judged by what it starts with. */
    public static boolean isArchive(final String content) {
        return content != null && content.startsWith(MAGIC);
    }

    /**
     * Packs files into one archive.
     *
     * <p>The contents are joined and compressed together rather than one at a time, which is what lets two
     * similar files cost barely more than one of them.
     */
    public static String pack(final List<StoredFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("an archive must hold at least one file");
        }
        if (files.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("an archive holds at most " + MAX_ENTRIES + " files");
        }
        final StringBuilder header = new StringBuilder(MAGIC).append(NEWLINE);
        final StringBuilder joined = new StringBuilder();
        final Set<String> names = new HashSet<>();
        for (final StoredFile file : files) {
            final String name = leaf(file.path());
            if (name.indexOf(SEPARATOR.charAt(0)) >= 0 || name.indexOf(NEWLINE) >= 0) {
                throw new IllegalArgumentException("a file name may not hold a tab or a newline: " + name);
            }
            /*
             * A file keeps only its own name inside, so two files of the same name from different folders
             * would become one on the way out. Refused rather than silently swallowed.
             */
            if (!names.add(name)) {
                throw new IllegalArgumentException("two files would be archived under the name " + name);
            }
            final int bytes = file.content().getBytes(StandardCharsets.UTF_8).length;
            header.append(name).append(SEPARATOR)
                    .append(file.type().extension()).append(SEPARATOR)
                    .append(bytes).append(NEWLINE);
            joined.append(file.content());
        }
        header.append(NEWLINE);
        header.append(encode(joined.toString()));
        return header.toString();
    }

    /**
     * What an archive says it holds, read from its listing alone.
     *
     * <p>Nothing is decompressed here, so opening a large archive to look at it is as cheap as opening a
     * short text file. A listing that does not parse gives an empty list rather than throwing, because a
     * damaged file on a disk is a thing a player can produce and should not be a crash.
     */
    public static List<Entry> entries(final String content) {
        final List<Entry> out = new ArrayList<>();
        if (!isArchive(content)) {
            return out;
        }
        final String[] lines = content.split("\n", -1);
        for (int i = 1; i < lines.length && out.size() < MAX_ENTRIES; i++) {
            if (lines[i].isEmpty()) {
                break; // the blank line ends the listing and the payload follows it
            }
            final String[] parts = lines[i].split(SEPARATOR, -1);
            if (parts.length != 3) {
                continue;
            }
            try {
                out.add(new Entry(parts[0], FileType.of(parts[1].toLowerCase(Locale.ROOT)),
                        Integer.parseInt(parts[2])));
            } catch (final IllegalArgumentException malformed) {
                // One unreadable line does not make the rest of the listing unreadable.
            }
        }
        return out;
    }

    /**
     * Takes every file back out of an archive.
     *
     * <p>Gives an empty list when the archive is not one, or when its payload does not match its listing,
     * which is the only honest answer for a file that has been damaged.
     */
    public static List<StoredFile> unpack(final String content) {
        final List<Entry> entries = entries(content);
        if (entries.isEmpty()) {
            return List.of();
        }
        final String payload = payloadOf(content);
        if (payload == null) {
            return List.of();
        }
        final String joined = decode(payload);
        if (joined == null) {
            return List.of();
        }
        final byte[] bytes = joined.getBytes(StandardCharsets.UTF_8);
        final List<StoredFile> out = new ArrayList<>(entries.size());
        int at = 0;
        for (final Entry entry : entries) {
            /*
             * Compared the other way round on purpose. A listing edited by hand can name a size near the
             * largest a number holds, and adding it to the position would wrap round to a negative one,
             * which passes a test written as a sum and then reads off the end of the payload.
             */
            if (entry.originalBytes() > bytes.length - at) {
                return List.of(); // the listing promises more than the payload holds
            }
            final int end = at + entry.originalBytes();
            out.add(new StoredFile(entry.name(), entry.type(),
                    new String(bytes, at, entry.originalBytes(), StandardCharsets.UTF_8)));
            at = end;
        }
        return out;
    }

    /** One file back out of an archive by name, or null when it is not in there. */
    public static StoredFile unpackOne(final String content, final String name) {
        for (final StoredFile file : unpack(content)) {
            if (file.path().equals(name)) {
                return file;
            }
        }
        return null;
    }

    /** What the files inside weighed before they were packed, in raw bytes. */
    public static int originalBytes(final String content) {
        int total = 0;
        for (final Entry entry : entries(content)) {
            total += entry.originalBytes();
        }
        return total;
    }

    /** The name a file keeps inside an archive, which is its own and not the folder it came from. */
    public static String leaf(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /** Everything after the blank line that ends the listing, or null when there is no such line. */
    private static String payloadOf(final String content) {
        final int split = content.indexOf("\n\n");
        return split < 0 ? null : content.substring(split + 2);
    }

    /** Compresses text and writes it as characters that cost one byte each on a disk. */
    private static String encode(final String text) {
        final byte[] raw = text.getBytes(StandardCharsets.UTF_8);
        final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(raw);
            deflater.finish();
            /*
             * Sized for the worst case deflate has, which is a little larger than the input, so a file of
             * random-looking text never silently loses its tail to a buffer that was too small.
             */
            final byte[] buffer = new byte[raw.length + raw.length / 8 + 64];
            final int size = deflater.deflate(buffer);
            final byte[] packed = new byte[size];
            System.arraycopy(buffer, 0, packed, 0, size);
            return Base64.getEncoder().encodeToString(packed);
        } finally {
            deflater.end();
        }
    }

    /** Takes the compressed text back, or null when it is not what it claims to be. */
    private static String decode(final String encoded) {
        final byte[] packed;
        try {
            packed = Base64.getDecoder().decode(encoded.strip());
        } catch (final IllegalArgumentException notBase64) {
            return null;
        }
        final Inflater inflater = new Inflater();
        try {
            inflater.setInput(packed);
            final byte[] buffer = new byte[4096];
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while (!inflater.finished()) {
                final int read = inflater.inflate(buffer);
                if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    return null; // truncated payload
                }
                bytes.write(buffer, 0, read);
            }
            return bytes.toString(StandardCharsets.UTF_8);
        } catch (final DataFormatException damaged) {
            return null;
        } finally {
            inflater.end();
        }
    }
}
