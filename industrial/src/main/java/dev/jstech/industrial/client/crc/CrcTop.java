/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.industrial.crc.CrcLayout;

/** What the Research and Group tabs show along their top: two plates, and a lamp or two with their words. */
final class CrcTop {

    private CrcTop() {
    }

    /** The first plate in aluminium, {@code first}, and after it {@code second} on a plate of {@code ground}. */
    static void plates(final CrcView view, final String first, final String second, final int ground,
                       final int ink) {
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        final int w = MachineArt.plate(view.g(), view.font(), view.x(CrcLayout.PLATE_X), view.y(CrcLayout.PLATE_Y),
                MachineArt.fit(view.font(), first, CrcLayout.TOP_PLATE_MOST - CrcLayout.PLATE_PADDING),
                m.aluminium(), m.plateInk());
        final int x = CrcLayout.PLATE_X + w + CrcLayout.PLATE_GAP;
        MachineArt.plate(view.g(), view.font(), view.x(x), view.y(CrcLayout.PLATE_Y), MachineArt.fit(view.font(),
                second, CrcLayout.TOP_PLATES_END - x - CrcLayout.PLATE_PADDING), ground, ink);
    }

    /** A lamp at {@code x} on the top row, lit or not, and its {@code word} cut to {@code room}. */
    static void lamp(final CrcView view, final int x, final int colour, final boolean lit, final String word,
                     final int room) {
        MachineArt.lamp(view.g(), view.x(x), view.y(CrcLayout.TOP_LAMP_Y), colour, lit);
        CrcArt.engrave(view.g(), view.font(), word, view.x(x + CrcLayout.LAMP_WORD_GAP),
                view.y(CrcLayout.TOP_LAMP_Y + 1), room);
    }
}
