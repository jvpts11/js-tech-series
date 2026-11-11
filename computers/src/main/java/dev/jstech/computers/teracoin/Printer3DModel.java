/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import java.util.Locale;

/**
 * The 3D printers (computers/docs/design/teracoin.md, "The 3D Printer"): a peripheral of each era that prints the
 * items of the server's price list, each paid in TRC as it starts. It prints the real item a row at a time from the
 * bottom, then puts it in its output by itself: the Prosa off its sliding bed into the tray, the Rattan off its belt
 * into the slot. Its block is named for its model, as every printer of the mod is. Pure.
 *
 * @param era        the era it is of
 * @param name       its model's name, as its maker gives it
 * @param itemTicks  how long an item takes to print, in ticks
 * @param ejectTicks how long it takes to put a finished item in its output, in ticks
 */
public record Printer3DModel(Era era, String name, int itemTicks, int ejectTicks) {

    /** The Standard's open printer: an item in 30 seconds, its bed sliding the item off into the tray. */
    public static final Printer3DModel PROSA_I3_MK3 = new Printer3DModel(HardwareEra.STANDARD, "Prosa i3 MK3", 600,
            60);
    /** The Advanced's closed printer: an item in 15 seconds, its belt carrying the item into the slot. */
    public static final Printer3DModel RATTAN_LAB_X1 = new Printer3DModel(HardwareEra.ADVANCED, "Rattan Lab X1",
            300, 48);
    /** Every model, the Standard's first. */
    public static final List<Printer3DModel> ALL = List.of(PROSA_I3_MK3, RATTAN_LAB_X1);
    /* The ticks of a second. */
    private static final int TICKS_A_SECOND = 20;

    /** The model named {@code name}; the Standard's for a name that is none of them. */
    public static Printer3DModel byName(final String name) {
        for (final Printer3DModel model : ALL) {
            if (model.name().equals(name)) {
                return model;
            }
        }
        return PROSA_I3_MK3;
    }

    /** Its name as ids and files have it: {@code prosa_i3_mk3}. */
    public String id() {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /** How long an item takes, in seconds. */
    public int itemSeconds() {
        return itemTicks / TICKS_A_SECOND;
    }

    /** Whether its bed is a belt that carries the item off, rather than a bed it slides out from under the item. */
    public boolean belt() {
        return equals(RATTAN_LAB_X1);
    }
}
