/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The series' seven eras of hardware, from the Vintage to the Singularity, declared in the registry of eras
 * ({@link Eras}) in their order. An era is an {@link Era}, whoever declared it; these are the ones every world has.
 *
 * <p>The series' own blocks that keep an era in their state (the parts of a Mainframe and of a rack) exist only in
 * these seven, so they keep its place among them ({@link #seriesIndex}), which no era an add-on brings can move.
 */
@TextHolder
public final class HardwareEra {

    private static final TextKey VINTAGE_NAME = TextKey.of("jscore.era.vintage", "Vintage");
    private static final TextKey LEGACY_NAME = TextKey.of("jscore.era.legacy", "Legacy");
    private static final TextKey TRANSITION_NAME = TextKey.of("jscore.era.transition", "Transition");
    private static final TextKey STANDARD_NAME = TextKey.of("jscore.era.standard", "Standard");
    private static final TextKey ADVANCED_NAME = TextKey.of("jscore.era.advanced", "Advanced");
    private static final TextKey EXA_NAME = TextKey.of("jscore.era.exa", "Exa");
    private static final TextKey SINGULARITY_NAME = TextKey.of("jscore.era.singularity", "Singularity");

    /*
     * Each era's colour is the one its screens are remembered by: the green phosphor of a CRT terminal for Vintage, the
     * blue of the Legacy desktop's chrome, the sky blue of the glass desktops of the Transition, the accent blue of
     * the Standard desktop, and a colder cast for each generation past that. Its year is the one its machines were
     * built in, as a firmware banner prints it.
     */
    public static final Era VINTAGE = series("vintage", VINTAGE_NAME, 0x33FF33, 1987);
    public static final Era LEGACY = series("legacy", LEGACY_NAME, 0x245EDC, 1998);
    public static final Era TRANSITION = series("transition", TRANSITION_NAME, 0x3FA9E0, 2008);
    public static final Era STANDARD = series("standard", STANDARD_NAME, 0x0078D4, 2026);
    public static final Era ADVANCED = series("advanced", ADVANCED_NAME, 0x9B59FF, 2044);
    public static final Era EXA = series("exa", EXA_NAME, 0x00E5FF, 2071);
    public static final Era SINGULARITY = series("singularity", SINGULARITY_NAME, 0xF2F2F2, 2099);
    /** The seven, earliest first. */
    public static final List<Era> SERIES = List.of(VINTAGE, LEGACY, TRANSITION, STANDARD, ADVANCED, EXA, SINGULARITY);
    /** The five that have parts, the Vintage to the Advanced; the Exa's and the Singularity's are still to come. */
    public static final List<Era> WITH_PARTS = List.of(VINTAGE, LEGACY, TRANSITION, STANDARD, ADVANCED);

    private HardwareEra() {
    }

    /** Where {@code era} stands among the series' seven, from 0 for the Vintage; -1 for an era an add-on brings. */
    public static int seriesIndex(final Era era) {
        return SERIES.indexOf(era);
    }

    /**
     * The place among the seven of the latest of them at or before {@code era}: its own for one of the seven, that of
     * the series' era it comes after for one an add-on brings. What a saved number that only the series' eras have,
     * such as a cable's generation, keeps, so an era put between two moves no number already saved.
     */
    public static int seriesLevel(final Era era) {
        for (int i = SERIES.size() - 1; i > 0; i--) {
            if (SERIES.get(i).isAtMost(era)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * The latest of the five eras with parts at or before {@code era}: the one whose models and textures an era
     * wears until it has its own, as the Exa and the Singularity wear the Advanced's.
     */
    public static Era withParts(final Era era) {
        return WITH_PARTS.get(partsIndex(era));
    }

    /**
     * Where the era {@code era} is drawn as ({@link #withParts}) stands among the five with parts, from 0 for the
     * Vintage to 4 for the Advanced: what a palette that keeps one colour per era of parts picks by.
     */
    public static int partsIndex(final Era era) {
        return Math.min(seriesLevel(era), WITH_PARTS.size() - 1);
    }

    /**
     * The one of five values, the Vintage's to the Advanced's, that {@code era} is drawn with ({@link #withParts}):
     * what a palette that keeps one colour for each era of parts picks its colour by.
     */
    public static int pick(final Era era, final int vintage, final int legacy, final int transition,
                           final int standard, final int advanced) {
        return switch (partsIndex(era)) {
            case 0 -> vintage;
            case 1 -> legacy;
            case 2 -> transition;
            case 3 -> standard;
            default -> advanced;
        };
    }

    /**
     * The series' era at {@code index} among the seven, the inverse of {@link #seriesIndex}: what a block state of
     * the series' own blocks reads back.
     *
     * @throws IllegalArgumentException if no era of the series has that index
     */
    public static Era ofSeriesIndex(final int index) {
        final Era era = seriesAt(index);
        if (era == null) {
            throw new IllegalArgumentException("no era of the series at " + index);
        }
        return era;
    }

    /** The series' era at {@code index} among the seven, or null when there is none there: a save's "no era". */
    public static @Nullable Era seriesAt(final int index) {
        return index < 0 || index >= SERIES.size() ? null : SERIES.get(index);
    }

    /** The series' era of that bare name ({@code legacy}), or null when none of the seven has it. */
    public static @Nullable Era seriesNamed(final String path) {
        for (final Era era : SERIES) {
            if (era.path().equals(path)) {
                return era;
            }
        }
        return null;
    }

    /* Loads the class, which declares the seven; what the registry calls before anything can read it. */
    static void load() {
        // Loading the class is the whole of it.
    }

    private static Era series(final String path, final TextKey name, final int colour, final int year) {
        return Eras.declare(new Era(Eras.SERIES_NAMESPACE + ":" + path, name.text(), colour, year));
    }
}
