/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import java.util.List;

/**
 * What a sensor, or a machine, reads and says in redstone: its readings, and which of them its signal speaks. A block
 * offers it through {@link Signals#OUTPUT}, so anything can read every sensor alike: a comparator's worth of redstone,
 * J's Computers through its peripheral links.
 */
public interface ISignalOutput {

    /** What it reads, each with its span. */
    List<Reading> readings();

    /** Which of its readings its signal speaks; the first unless said. */
    default int chosen() {
        return 0;
    }

    /** The signal it gives now, 0 to 15: the strength of the reading it speaks; 0 for one that reads nothing. */
    default int strength() {
        final List<Reading> readings = readings();
        final int chosen = chosen();
        return chosen < 0 || chosen >= readings.size() ? 0 : readings.get(chosen).strength();
    }
}
