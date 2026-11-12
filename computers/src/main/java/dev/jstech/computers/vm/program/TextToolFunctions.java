/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IPureContext;
import dev.jstech.computers.vm.system.IntrinsicRegistry;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.CRC32;

/**
 * The third version's tools for text, each the program's own work with nothing of the machine: building a text a piece
 * at a time (StringBuilder), reading and writing CSV, a text's SHA-256 and CRC-32, Base64, and taking paths apart and
 * putting them together (Path), whichever separator a path was written with.
 */
@TextHolder
final class TextToolFunctions {

    /** The type a program writes for a text built a piece at a time. */
    static final String BUILDER = "StringBuilder";
    /** What a builder holds: the pieces appended, in order, and how many characters they come to. */
    static final String PIECES = "Pieces";
    static final String LENGTH = "Length";
    /** What a builder weighs, its pieces aside: the header and its two values. */
    static final long BUILDER_BYTES = Heap.HEADER + 2L * Heap.REFERENCE;

    private static final String TEXT = "string";
    private static final String WHOLE = "long";
    private static final String TABLE = "List<List<string>>";
    private static final String PATH = "Path";
    private static final char QUOTE = '"';
    private static final char COMMA = ',';

    private static final TextKey NOT_BASE64 = TextKey.of("jsc.vm.text_tools.not_base64", "'%s' is not Base64");
    private static final TextKey NO_BUILDER = TextKey.of("jsc.vm.text_tools.no_builder",
            "there is no StringBuilder here");

    private TextToolFunctions() {
    }

    static void register(final IntrinsicRegistry.Builder registry) {
        builder(registry);
        registry.onType("Csv", "Parse", TABLE, (context, target, arguments, line) ->
                rows(context, CsvText.read(String.valueOf(arguments[0])), line), TEXT);
        registry.onType("Csv", "Write", TEXT, (context, target, arguments, line) ->
                context.text(CsvText.write(table(arguments[0])), line), TABLE);
        registry.onType("Hash", "Sha256", TEXT, (context, target, arguments, line) ->
                context.text(sha256(String.valueOf(arguments[0])), line), TEXT);
        registry.onType("Hash", "Crc32", WHOLE, (context, target, arguments, line) -> {
            final CRC32 crc = new CRC32();
            crc.update(String.valueOf(arguments[0]).getBytes(StandardCharsets.UTF_8));
            return crc.getValue();
        }, TEXT);
        registry.onType("Base64", "Encode", TEXT, (context, target, arguments, line) -> context.text(Base64.getEncoder()
                .encodeToString(String.valueOf(arguments[0]).getBytes(StandardCharsets.UTF_8)), line), TEXT);
        registry.onType("Base64", "Decode", TEXT, (context, target, arguments, line) -> {
            final String coded = String.valueOf(arguments[0]);
            try {
                return context.text(new String(Base64.getDecoder().decode(coded), StandardCharsets.UTF_8), line);
            } catch (final IllegalArgumentException wrong) {
                throw new Halt(Halt.Reason.BAD_CAST, line, NOT_BASE64.with(coded), Halt.Thrown.FORMAT);
            }
        }, TEXT);
        registry.onType(PATH, "Combine", TEXT, (context, target, arguments, line) ->
                context.text(PathText.combine(String.valueOf(arguments[0]), String.valueOf(arguments[1])), line),
                TEXT, TEXT);
        registry.onType(PATH, "FileName", TEXT, (context, target, arguments, line) ->
                context.text(PathText.fileName(String.valueOf(arguments[0])), line), TEXT);
        registry.onType(PATH, "Extension", TEXT, (context, target, arguments, line) ->
                context.text(PathText.extension(String.valueOf(arguments[0])), line), TEXT);
        registry.onType(PATH, "Folder", TEXT, (context, target, arguments, line) ->
                context.text(PathText.folder(String.valueOf(arguments[0])), line), TEXT);
    }

    /** A builder with nothing in it yet, made for {@code new StringBuilder()}. */
    static Values.Obj newBuilder(final Heap heap, final int line) {
        final Values.Obj made = new Values.Obj(BUILDER);
        final Values.ListValue pieces = heap.allocate(new Values.ListValue(), Heap.HEADER, line);
        made.set(PIECES, pieces);
        made.set(LENGTH, 0);
        return heap.allocate(made, BUILDER_BYTES, line);
    }

    /* A piece is appended as a list grows by one, which is why appending costs the same however long the text is. */
    private static void builder(final IntrinsicRegistry.Builder registry) {
        registry.onObject(BUILDER, "Append", BUILDER, (context, target, arguments, line) ->
                append(context, target, String.valueOf(arguments[0]), line), TEXT);
        // A piece the builder holds is counted as its list grows, as Append's is: made as the runtime's own text too,
        // it would be counted twice and never let go, however often the builder is cleared.
        registry.onObject(BUILDER, "AppendLine", BUILDER, (context, target, arguments, line) ->
                append(context, target, arguments[0] + "\n", line), TEXT);
        registry.onObject(BUILDER, "Clear", BUILDER, (context, target, arguments, line) -> {
            final Values.Obj builder = builder(target, line);
            final Values.ListValue pieces = (Values.ListValue) builder.get(PIECES);
            pieces.items().clear();
            context.resize(pieces, pieces.bytes(), line);
            builder.set(LENGTH, 0);
            return builder;
        });
        registry.onObject(BUILDER, "ToString", TEXT, (context, target, arguments, line) -> {
            final Values.Obj builder = builder(target, line);
            context.fitsText(length(builder), line);
            final StringBuilder whole = new StringBuilder();
            for (final Object piece : ((Values.ListValue) builder.get(PIECES)).items()) {
                whole.append(piece);
            }
            return context.text(whole.toString(), line);
        });
    }

    /*
     * A piece costs the builder only a place in its list, the same piece appended twice being one text; so the builder
     * grows no longer than the program could ever make into a text, and stops it there.
     */
    private static Values.Obj append(final IPureContext context, final Object target, final String piece,
                                     final int line) {
        final Values.Obj builder = builder(target, line);
        final long after = (long) length(builder) + piece.length();
        context.fitsText(after, line);
        final Values.ListValue pieces = (Values.ListValue) builder.get(PIECES);
        pieces.items().add(piece);
        context.resize(pieces, pieces.bytes(), line);
        builder.set(LENGTH, (int) after);
        return builder;
    }

    /** How many characters {@code builder} holds. */
    static int length(final Values.Obj builder) {
        return builder.get(LENGTH) instanceof Number count ? count.intValue() : 0;
    }

    private static Values.Obj builder(final Object target, final int line) {
        if (!(target instanceof Values.Obj builder) || !BUILDER.equals(builder.type())
                || !(builder.get(PIECES) instanceof Values.ListValue)) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, NO_BUILDER.text());
        }
        return builder;
    }

    /* The rows read, as the program's lists of texts. */
    private static Values.ListValue rows(final IPureContext context, final List<List<String>> read, final int line) {
        final Values.ListValue rows = new Values.ListValue();
        for (final List<String> fields : read) {
            final Values.ListValue row = new Values.ListValue();
            for (final String field : fields) {
                row.items().add(context.text(field, line));
            }
            rows.items().add(context.allocate(row, row.bytes(), line));
        }
        return context.allocate(rows, rows.bytes(), line);
    }

    /* The program's rows as texts, a row that is no list counting as empty. */
    private static List<List<String>> table(final Object rows) {
        if (!(rows instanceof Values.ListValue list)) {
            return List.of();
        }
        return list.items().stream().map(row -> row instanceof Values.ListValue fields
                ? fields.items().stream().map(String::valueOf).toList() : List.<String>of()).toList();
    }

    private static String sha256(final String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException missing) {
            // Every Java runtime has SHA-256; one that does not cannot run the game either.
            throw new IllegalStateException(missing);
        }
    }

    /** CSV as RFC 4180 writes it: fields parted by commas, quoted where they hold a comma, a quote or a line break. */
    static final class CsvText {

        private CsvText() {
        }

        /** The rows of {@code text}, each a list of its fields; a blank line is a row of one empty field. */
        static List<List<String>> read(final String text) {
            final List<List<String>> rows = new ArrayList<>();
            List<String> row = new ArrayList<>();
            final StringBuilder field = new StringBuilder();
            boolean quoted = false;
            boolean anything = false;
            for (int i = 0; i < text.length(); i++) {
                final char c = text.charAt(i);
                anything = true;
                if (quoted) {
                    if (c == QUOTE && i + 1 < text.length() && text.charAt(i + 1) == QUOTE) {
                        field.append(QUOTE);
                        i++;
                    } else if (c == QUOTE) {
                        quoted = false;
                    } else {
                        field.append(c);
                    }
                } else if (c == QUOTE) {
                    quoted = true;
                } else if (c == COMMA) {
                    row.add(field.toString());
                    field.setLength(0);
                } else if (c == '\n' || c == '\r') {
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    row.add(field.toString());
                    field.setLength(0);
                    rows.add(row);
                    row = new ArrayList<>();
                    anything = false;
                } else {
                    field.append(c);
                }
            }
            if (anything) {
                row.add(field.toString());
                rows.add(row);
            }
            return rows;
        }

        /** {@code rows} as CSV, a line each, every line ending with a line break. */
        static String write(final List<List<String>> rows) {
            final StringBuilder out = new StringBuilder();
            for (final List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) {
                        out.append(COMMA);
                    }
                    final String field = row.get(i);
                    if (field.indexOf(COMMA) >= 0 || field.indexOf(QUOTE) >= 0 || field.indexOf('\n') >= 0
                            || field.indexOf('\r') >= 0) {
                        out.append(QUOTE).append(field.replace("\"", "\"\"")).append(QUOTE);
                    } else {
                        out.append(field);
                    }
                }
                out.append('\n');
            }
            return out.toString();
        }
    }

    /**
     * Paths taken apart and put together. A path written with a backslash or a drive letter is Frames' and MC-DOS's,
     * any other a Unix one, so each keeps the separator it was written with.
     */
    static final class PathText {

        private PathText() {
        }

        /** {@code tail} under {@code head}; a tail that starts at a root is a whole path of its own. */
        static String combine(final String head, final String tail) {
            if (head.isEmpty() || rooted(tail)) {
                return tail;
            }
            if (tail.isEmpty()) {
                return head;
            }
            final char separator = separatorOf(head);
            final char last = head.charAt(head.length() - 1);
            return last == '/' || last == '\\' ? head + tail : head + separator + tail;
        }

        /** The name at the end of a path, its folders left out. */
        static String fileName(final String path) {
            return path.substring(lastSeparator(path) + 1);
        }

        /** The ending of the name at the end of a path, its dot included: {@code .log}; empty for a name with none. */
        static String extension(final String path) {
            final String name = fileName(path);
            final int dot = name.lastIndexOf('.');
            return dot <= 0 ? "" : name.substring(dot);
        }

        /** The folder a path is in: a root keeps its separator, and a bare name has none. */
        static String folder(final String path) {
            final int last = lastSeparator(path);
            if (last < 0) {
                return path.length() == 2 && path.charAt(1) == ':' ? path : "";
            }
            final boolean root = last == 0 || (last == 2 && path.charAt(1) == ':');
            return path.substring(0, root ? last + 1 : last);
        }

        private static boolean rooted(final String path) {
            return path.startsWith("/") || path.startsWith("\\") || (path.length() >= 2 && path.charAt(1) == ':');
        }

        private static char separatorOf(final String path) {
            return path.indexOf('\\') >= 0 || (path.length() >= 2 && path.charAt(1) == ':') ? '\\' : '/';
        }

        private static int lastSeparator(final String path) {
            return Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        }
    }
}
