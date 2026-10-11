/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * What a manual reads from its mod as it is laid out, so a figure in it is never out of date: a single value, such as
 * how many memory modules a Personal Computer takes, or the rows of a table, such as how far each era's cable reaches.
 *
 * <p>A mod registers each under an id of its namespace while the game loads, as code that answers when asked. A
 * sentence names a value by its id between double braces, {@code {{mymod:memory_slots}}}, where the figure stands in
 * it ({@link #token} writes that), and a translation keeps the braces where its own language puts the figure; the value
 * is written the reader's way as the page is laid out. A table names the source of its rows instead of listing them,
 * and they are asked for each time a manual is laid out, so a part a mod adds to a family, or a setting changed, is in
 * the next book opened.
 *
 * <pre>{@code
 * GuideFacts.value("mymod:memory_slots", () -> new GuideBlock.GuideValue.Amount(PcSpec.MEMORY_SLOTS, ""));
 * GuideFacts.rows("mymod:cable_reach", () -> CableTier.all().stream().map(tier -> new GuideBlock.TableRow(List.of(
 *         new GuideBlock.GuideValue.Words(tier.nameKey()), new GuideBlock.GuideValue.Amount(tier.reach(), "m"))))
 *         .toList());
 * }</pre>
 *
 * <p>The answers are asked for on the side that lays a manual out, the player's game; a value only the client knows is
 * registered from the client's setup. A value's words name no values of their own.
 */
public final class GuideFacts {

    private static final Map<String, Supplier<GuideBlock.GuideValue>> VALUES = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<List<GuideBlock.TableRow>>> ROWS = new ConcurrentHashMap<>();
    /* What went wrong goes to the JDK's own log, which needs no library, so the class stays pure. */
    private static final System.Logger LOGGER = System.getLogger(GuideFacts.class.getName());
    private static final String OPEN = "{{";
    private static final String CLOSE = "}}";

    private GuideFacts() {
    }

    /** Registers a value a sentence may name, {@code namespace:path}, answered each time a manual is laid out. */
    public static void value(final String id, final Supplier<GuideBlock.GuideValue> value) {
        Objects.requireNonNull(value, "value");
        if (VALUES.putIfAbsent(Objects.requireNonNull(id, "id"), value) != null) {
            throw new IllegalStateException("the manuals' value " + id + " is registered twice");
        }
    }

    /** Registers the rows a table may name as its source, answered each time a manual is laid out. */
    public static void rows(final String id, final Supplier<List<GuideBlock.TableRow>> rows) {
        Objects.requireNonNull(rows, "rows");
        if (ROWS.putIfAbsent(Objects.requireNonNull(id, "id"), rows) != null) {
            throw new IllegalStateException("the manuals' rows " + id + " are registered twice");
        }
    }

    /** How a sentence names a value: its id between double braces. */
    public static String token(final String id) {
        return OPEN + id + CLOSE;
    }

    /** The value registered under an id, answered now; empty when no mod registered it, or its answer failed. */
    public static Optional<GuideBlock.GuideValue> valueFor(final String id) {
        final Supplier<GuideBlock.GuideValue> value = VALUES.get(id);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(value.get());
        } catch (final RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "The manuals' value " + id + " could not be read", e);
            return Optional.empty();
        }
    }

    /**
     * The rows of a source, answered now, each with {@code columns} cells: a row of another count does not fit its
     * table and is left out, as is every row when the answer fails, with a line in the log either way.
     */
    public static List<GuideBlock.TableRow> rowsOf(final String source, final int columns) {
        final Supplier<List<GuideBlock.TableRow>> rows = ROWS.get(source);
        if (rows == null) {
            LOGGER.log(System.Logger.Level.WARNING, "No mod registered the manuals' rows " + source);
            return List.of();
        }
        final List<GuideBlock.TableRow> answered;
        try {
            answered = rows.get();
        } catch (final RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "The manuals' rows " + source + " could not be read", e);
            return List.of();
        }
        final List<GuideBlock.TableRow> fitting = new ArrayList<>();
        for (final GuideBlock.TableRow row : answered) {
            if (row.cells().size() == columns) {
                fitting.add(row);
            } else {
                LOGGER.log(System.Logger.Level.WARNING, "A row of the manuals' rows " + source + " has "
                        + row.cells().size() + " cells for a table of " + columns + " columns");
            }
        }
        return fitting;
    }

    /**
     * The sentence with every value it names written in through {@code text}, the reader's way. A name no mod
     * registered stays as it is written, braces and all, so it is seen on the page and fixed.
     */
    public static String fill(final String sentence, final IGuideText text) {
        if (!sentence.contains(OPEN)) {
            return sentence;
        }
        final StringBuilder out = new StringBuilder();
        int at = 0;
        while (true) {
            final int open = sentence.indexOf(OPEN, at);
            final int close = open < 0 ? -1 : sentence.indexOf(CLOSE, open + OPEN.length());
            if (close < 0) {
                break;
            }
            final Optional<GuideBlock.GuideValue> value = valueFor(sentence.substring(open + OPEN.length(), close));
            out.append(sentence, at, open);
            out.append(value.isPresent() ? text.valueText(value.get())
                    : sentence.substring(open, close + CLOSE.length()));
            at = close + CLOSE.length();
        }
        return out.append(sentence.substring(at)).toString();
    }
}
