/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.QuinceBoards;
import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.jstechseries.tier.HardwareEra;

/**
 * How one of Quince's machines starts (computers/docs/design/quince.md): the firmware on its logic board, which puts
 * its own picture on the glass while the machine tests itself, the Sad Quince when the parts make no computer, and
 * the start-up choices a power-on while sneaking opens, as holding the key did. Any other machine starts as its era's
 * firmware does ({@link FirmwareKind}).
 *
 * <p>Free of Minecraft types, as the firmware kinds are.
 */
public enum QuinceStart implements IStableId {
    /** Not one of Quince's machines. */
    NONE(0, false),
    /** The Russet 128K's ROM: the Happy Quince, and no choices at all. */
    ROM(1, false),
    /** Open Firmware, the Legacy's: the Happy Quince on its grey, and its prompt for a choice. */
    OPEN_FIRMWARE(2, true),
    /** EFI, the Transition's and the Standard's: the fruit while it starts, and the Startup Manager. */
    EFI(3, true),
    /** The Quince chip's own start, the Advanced's: the fruit and its bar, and the Startup Options. */
    CHIP(4, true);

    private static final StableIds<QuinceStart> IDS = StableIds.of(QuinceStart.class);
    private static final EraTable<QuinceStart> BY_ERA = EraTable.of(ROM)
            .from(HardwareEra.LEGACY, OPEN_FIRMWARE)
            .from(HardwareEra.TRANSITION, EFI)
            .from(HardwareEra.ADVANCED, CHIP);

    private final int id;
    private final boolean choices;

    QuinceStart(final int id, final boolean choices) {
        this.id = id;
        this.choices = choices;
    }

    @Override
    public int id() {
        return id;
    }

    /** Whether a power-on while sneaking opens start-up choices: every one but the Russet 128K's ROM. */
    public boolean hasChoices() {
        return choices;
    }

    /** How a machine on a board of {@code form} of {@code era} starts: by its era when the board is Quince's. */
    public static QuinceStart of(final FormFactor form, final Era era) {
        return QuinceBoards.isQuince(form) ? BY_ERA.get(era) : NONE;
    }

    /** The start that declares {@code id}; an id no start declares reads as none. */
    public static QuinceStart byId(final int id) {
        return IDS.byId(id, NONE);
    }
}
