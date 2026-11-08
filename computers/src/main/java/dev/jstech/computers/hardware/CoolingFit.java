/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * What a computer's cooling comes to as its parts stand (computers/docs/design/temperature.md): the heat it can take
 * away, and what its screens say of it.
 *
 * @param budgetWatts    what the case, its coolers and its fans take away together, in watts
 * @param fans           the fans turning in its seats
 * @param fanSeats       the seats its case has for them, less those radiators take
 * @param bareProcessors the processors seated with no cooler over them
 */
public record CoolingFit(int budgetWatts, int fans, int fanSeats, int bareProcessors) {

    /** A computer cooled from outside it, which has none of this. */
    public static final CoolingFit NONE = new CoolingFit(0, 0, 0, 0);
}
