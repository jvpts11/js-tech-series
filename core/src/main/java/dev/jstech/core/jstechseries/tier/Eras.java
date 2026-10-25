/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The eras of hardware, in their order, as a registry rather than a closed list: the series' seven are declared by the
 * Core ({@link HardwareEra}), and an add-on declares its own after the last or right after any other, while the game
 * loads. Every rule that asks for "this era or later" follows the order ({@link Era#isAtLeast}). An era is saved by
 * its id, so an era an add-on puts between two moves no other.
 *
 * <p>Nothing comes before the Vintage: an era is declared at the end or after another, so the series' first era is
 * every world's first.
 *
 * <pre>{@code
 * public static final Era QUANTUM = Eras.declareAfter(
 *         new Era("myaddon:quantum", MyTexts.QUANTUM.text(), 0xFF00FF, 2034), HardwareEra.ADVANCED);
 * }</pre>
 */
public final class Eras {

    /** The mod a bare era name belongs to: a data file's {@code "legacy"} is {@code jscore:legacy}. */
    public static final String SERIES_NAMESPACE = "jscore";
    /* What an id may be written with, the same letters a game id takes. */
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");
    /* Each era, by id, with the era it was declared after, or none for one at the end; in the order declared. */
    private static final Map<String, Era> DECLARED = new LinkedHashMap<>();
    private static final Map<String, String> AFTER = new HashMap<>();
    /* Settled once the registry closes: the order, and each era's place in it. */
    private static @Nullable List<Era> ordered;
    private static @Nullable Map<Era, Integer> positions;

    static {
        /*
         * The series' seven are declared before anything can ask, whichever of the two classes is loaded first. A
         * call, not a read of a field: when the series' class is the one loading, its fields are not set yet.
         */
        HardwareEra.load();
    }

    private Eras() {
    }

    /** Declares {@code era} after every era declared so far, while the game loads. */
    public static synchronized Era declare(final Era era) {
        return put(era, null);
    }

    /** Declares {@code era} right after {@code after}, before any era that came after it, while the game loads. */
    public static synchronized Era declareAfter(final Era era, final Era after) {
        Objects.requireNonNull(after, "after");
        if (DECLARED.get(after.id()) != after) {
            throw DeclarationError.of(era.namespace(), "the era " + era.id(),
                    "is declared after " + after.id() + ", which is no era yet",
                    "declare it after an era declared before it, such as one of the series' seven");
        }
        return put(era, after);
    }

    /** The era of {@code id}, if one is declared. */
    public static synchronized Optional<Era> byId(final String id) {
        return Optional.ofNullable(DECLARED.get(id));
    }

    /**
     * The era a data file or a command names: by its id, or by its bare name for one of the series' seven
     * ({@code "legacy"}), so the files written before eras had ids still read.
     */
    public static Optional<Era> byName(final String name) {
        return byId(name.indexOf(':') < 0 ? SERIES_NAMESPACE + ":" + name : name);
    }

    /** Every era, earliest first. */
    public static synchronized List<Era> ordered() {
        return ordered != null ? ordered : order();
    }

    /** The era at {@code position} in the order, held to its ends: what a packet that sends a position reads. */
    public static Era at(final int position) {
        final List<Era> all = ordered();
        return all.get(Math.max(0, Math.min(all.size() - 1, position)));
    }

    /** Where {@code era} stands in the order, from 0 for the earliest; -1 for one not declared. */
    public static int position(final Era era) {
        final Map<Era, Integer> settled = positions;
        if (settled != null) {
            return settled.getOrDefault(era, -1);
        }
        return ordered().indexOf(era);
    }

    /** The earliest era, every world's first: the Vintage. */
    public static Era first() {
        return ordered().getFirst();
    }

    /** The latest era. */
    public static Era last() {
        return ordered().getLast();
    }

    /** Whether the registry has closed, settling the order. */
    public static boolean settled() {
        return positions != null;
    }

    /**
     * Refuses an era id that is not {@code namespace:path} in the letters a game id takes.
     *
     * @throws IllegalArgumentException when it is not
     */
    public static void requireId(final String id) {
        Objects.requireNonNull(id, "id");
        final int colon = id.indexOf(':');
        if (colon <= 0 || colon != id.lastIndexOf(':') || !NAMESPACE.matcher(id.substring(0, colon)).matches()
                || !PATH.matcher(id.substring(colon + 1)).matches()) {
            throw new IllegalArgumentException("an era's id is namespace:path in lower case: " + id);
        }
    }

    /** Closes the registry once the game has loaded, settling the order; from the Core. */
    @ApiStatus.Internal
    public static synchronized void freeze() {
        final List<Era> settled = order();
        final Map<Era, Integer> places = new IdentityHashMap<>();
        for (int i = 0; i < settled.size(); i++) {
            places.put(settled.get(i), i);
        }
        ordered = settled;
        positions = places;
    }

    private static Era put(final Era era, final @Nullable Era after) {
        Objects.requireNonNull(era, "era");
        if (ordered != null) {
            throw DeclarationError.of(era.namespace(), "the era " + era.id(),
                    "is declared after the game has loaded",
                    "declare it from the mod's constructor, or from a class the constructor loads");
        }
        if (DECLARED.putIfAbsent(era.id(), era) != null) {
            throw DeclarationError.of(era.namespace(), "the era " + era.id(), "is declared twice",
                    "give one of the two another id");
        }
        if (after != null) {
            AFTER.put(era.id(), after.id());
        }
        return era;
    }

    /*
     * The order: the eras declared at the end, in turn, each followed by the eras declared right after it, in the
     * order they were declared, each in turn followed by its own.
     */
    private static List<Era> order() {
        final List<Era> out = new ArrayList<>();
        for (final Era era : DECLARED.values()) {
            if (!AFTER.containsKey(era.id())) {
                place(era, out);
            }
        }
        return List.copyOf(out);
    }

    private static void place(final Era era, final List<Era> out) {
        out.add(era);
        for (final Era next : DECLARED.values()) {
            if (era.id().equals(AFTER.get(next.id()))) {
                place(next, out);
            }
        }
    }
}
