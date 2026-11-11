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
 * Blockwright's ASICs (computers/docs/design/teracoin.md): blocks made only to mine, one for each era from the
 * Standard on, each mining far past any rig at far more energy. It has no system: its window sets where it pays and the
 * pool it mines for. The heat and the fans are what its window shows of its hashboards; they cost nothing more. Pure.
 *
 * @param era       the era it is of
 * @param name      its model's name, as its maker gives it
 * @param hashrate  what it hashes a second
 * @param watts     what it draws while it mines
 * @param boards    its hashboards, which share its work
 * @param boardHeat the warmest hashboard's temperature while it mines, in degrees; the others a degree cooler each
 * @param fanRpm    its fans' speed while it mines
 */
public record AsicModel(Era era, String name, long hashrate, int watts, int boards, int boardHeat, int fanRpm) {

    /** The Standard's Forge S1: three hashboards, 1 MH/s at 1,400 W. */
    public static final AsicModel FORGE_S1 = new AsicModel(HardwareEra.STANDARD, "Forge S1", 1_000_000L, 1_400, 3,
            71, 4_200);
    /** The Advanced's Forge A2: four hashboards, 10 MH/s at 3,200 W. */
    public static final AsicModel FORGE_A2 = new AsicModel(HardwareEra.ADVANCED, "Forge A2", 10_000_000L, 3_200, 4,
            69, 5_600);

    /** Every model, the Standard's first. */
    public static final List<AsicModel> ALL = List.of(FORGE_S1, FORGE_A2);

    /** The model named {@code name}, as its maker names it; the Standard's for a name that is none of them. */
    public static AsicModel byName(final String name) {
        for (final AsicModel model : ALL) {
            if (model.name().equals(name)) {
                return model;
            }
        }
        return FORGE_S1;
    }

    /** Its name as ids and files have it: {@code forge_s1}. */
    public String id() {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /** What each hashboard hashes a second: its share of the whole. */
    public long boardHashrate() {
        return hashrate / boards;
    }

    /** The temperature of hashboard {@code board}, from 0, while it mines. */
    public int heatOf(final int board) {
        return boardHeat - (boards - 1 - board);
    }
}
