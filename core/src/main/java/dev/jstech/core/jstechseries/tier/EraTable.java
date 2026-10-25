/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Values given era by era, for the eras that have one of their own: an era without its own takes the value of the
 * nearest era before it that has one. The series' Exa and Singularity follow that rule (they use the Advanced's
 * values until they have their own), and so does every era an add-on brings, so a rule written era by era always has
 * an answer, whatever eras a world has.
 *
 * <pre>{@code
 * private static final EraTable<Integer> HUB_PORTS = EraTable.of(2)
 *         .from(HardwareEra.LEGACY, 4)
 *         .from(HardwareEra.STANDARD, 7);
 * }</pre>
 *
 * <p>Once the eras' registry has closed, a lookup is a read of an array by the era's place in the order.
 *
 * @param <T> what the table gives; null is a value like any other
 */
public final class EraTable<T> {

    /* The values given, by the id of the era each starts at; the first era's is always there. */
    private final Map<String, T> given;
    /* Each era's value by its place in the order, filled the first time the table is read after the registry closes. */
    private volatile @Nullable Object[] settled;

    private EraTable(final Map<String, T> given) {
        this.given = given;
    }

    /** A table that gives {@code value} from the first era on. */
    public static <T> EraTable<T> of(final @Nullable T value) {
        final Map<String, T> given = new LinkedHashMap<>();
        given.put(HardwareEra.VINTAGE.id(), value);
        return new EraTable<>(given);
    }

    /**
     * A table of the five eras that have parts, the Vintage to the Advanced, in their order; every era after the
     * Advanced takes the Advanced's.
     */
    public static <T> EraTable<T> ofFive(final @Nullable T vintage, final @Nullable T legacy,
                                         final @Nullable T transition, final @Nullable T standard,
                                         final @Nullable T advanced) {
        return EraTable.of(vintage)
                .from(HardwareEra.LEGACY, legacy)
                .from(HardwareEra.TRANSITION, transition)
                .from(HardwareEra.STANDARD, standard)
                .from(HardwareEra.ADVANCED, advanced);
    }

    /** The same table, giving {@code value} from {@code era} on, until an era after it gives another. */
    public EraTable<T> from(final Era era, final @Nullable T value) {
        Objects.requireNonNull(era, "era");
        final Map<String, T> next = new LinkedHashMap<>(given);
        next.put(era.id(), value);
        return new EraTable<>(next);
    }

    /** The value for {@code era}: its own, or that of the nearest era before it that has one. */
    @SuppressWarnings("unchecked")
    public @Nullable T get(final Era era) {
        Objects.requireNonNull(era, "era");
        if (!Eras.settled()) {
            return walk(era);
        }
        Object[] values = settled;
        if (values == null) {
            values = settle();
        }
        final int position = era.level();
        return position < 0 ? walk(era) : (T) values[position];
    }

    /** Whether {@code era} gives a value of its own rather than taking the one of an era before it. */
    public boolean hasOwn(final Era era) {
        return given.containsKey(era.id());
    }

    private Object[] settle() {
        final List<Era> all = Eras.ordered();
        final Object[] values = new Object[all.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = walk(all.get(i));
        }
        settled = values;
        return values;
    }

    /* The era's own value, or the one of the nearest era before it with one; an era not declared takes the first's. */
    private @Nullable T walk(final Era era) {
        final List<Era> all = Eras.ordered();
        for (int i = all.indexOf(era); i >= 0; i--) {
            final String id = all.get(i).id();
            if (given.containsKey(id)) {
                return given.get(id);
            }
        }
        return given.get(HardwareEra.VINTAGE.id());
    }
}
