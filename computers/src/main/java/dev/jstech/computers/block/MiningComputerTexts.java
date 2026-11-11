/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Mining Computer says in the world (computers/docs/design/teracoin.md): why a card does not go on a riser,
 * and what a look at a riser with a card in hand, or at a seated card with an empty hand, would do.
 */
@TextHolder
public final class MiningComputerTexts {

    public static final TextKey SWITCH_OFF_FIRST =
            TextKey.of("jsc.mining_computer.switch_off_first", "Switch the rig off first");
    public static final TextKey NO_RISER =
            TextKey.of("jsc.mining_computer.no_riser", "Put a riser here first, at the rig's screen");
    public static final TextKey RISER_TAKEN =
            TextKey.of("jsc.mining_computer.riser_taken", "A card stands on this riser already");
    public static final TextKey ONLY_GRAPHICS_CARDS =
            TextKey.of("jsc.mining_computer.only_graphics_cards", "Only a graphics card stands on a riser");
    public static final TextKey TOO_NEW_FOR_RISER =
            TextKey.of("jsc.mining_computer.too_new_for_riser", "This riser carries cards of its era or older");
    public static final TextKey BOARD_REFUSES =
            TextKey.of("jsc.mining_computer.board_refuses", "The rig's board does not take this card");
    /** The look at a free riser with a card in hand: the card, the riser's number from 1. */
    public static final TextKey SEAT_ON = TextKey.of("jsc.mining_computer.seat_on", "Seat the %s on riser %s");
    /** The look at a seated card with an empty hand: the riser's number from 1. */
    public static final TextKey TAKE_OFF =
            TextKey.of("jsc.mining_computer.take_off", "Sneak with an empty hand to take the card off riser %s");
    /** An ASIC's SET with an address that is not a Teracoin address. */
    public static final TextKey BAD_ADDRESS =
            TextKey.of("jsc.mining_computer.bad_address", "That is not a Teracoin address: trc1q and 30 letters");
    /** The tooltip of a rig two blocks wide. */
    public static final TextKey TWO_WIDE =
            TextKey.of("jsc.mining_computer.two_wide", "Stands two blocks wide when placed");

    private MiningComputerTexts() {
    }
}
