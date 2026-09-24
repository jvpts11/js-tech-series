/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The Network Interactor's own colours, which its window and its Operations list share:
 * {@code jsc:app/network_interactor}.
 */
@PaletteHolder
final class InteractorPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/network_interactor",
            new Colours(0xFF2E8B45, 0xFF9A4A4A, 0xFFE6A93A, 0xFF2F6AC6, 0xFFE0A800, 0xFFB23A3A,
                    0x334A90E2, 0xFF3CC75A, 0xFFD05050, 0x99000000, 0xFFB8860B, 0xB0000000,
                    0xFFB8860B, 0xFFB23A3A, 0xFF6A7280, 0x552F6AC6, 0x22000000, 0xFF49E07A, 0xFF8A93A4));

    private InteractorPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The Interactor's colours.
     *
     * @param online      a server that is up, a store with room, a craft that can run
     * @param offline     a server that is down
     * @param amber       what wants the eye without being wrong
     * @param link        a link a player can follow
     * @param star        a starred entry
     * @param shortage    what is short, full or failed
     * @param band        the rubber band a drag selects with
     * @param dotReady    the mark of a recipe that can be crafted now
     * @param dotMissing  the mark of a recipe that cannot
     * @param popupDim    what dims the window behind the request popup
     * @param feasible    the line that says how much of a craft can be made
     * @param craftDim    what dims the window behind the craft popup
     * @param opPending   an Operation that is partial, running, waiting or pending
     * @param opFailed    an Operation that failed, is locked or was discarded
     * @param opOther     an Operation in any other state
     * @param rowChosen   the chosen row in the Operations list
     * @param rowHover    a row under the cursor
     * @param live        the mark of an Operation that is still going
     * @param idle        the mark of one that is not
     */
    record Colours(int online, int offline, int amber, int link, int star, int shortage, int band, int dotReady,
                   int dotMissing, int popupDim, int feasible, int craftDim, int opPending, int opFailed,
                   int opOther, int rowChosen, int rowHover, int live, int idle) {
    }
}
