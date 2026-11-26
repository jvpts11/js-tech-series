/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.Optional;

/**
 * What an empty item inlet of a chain machine's screen says beside the items its recipes take, where the machine works
 * what goes in a way its face does not show: the Electrolytic Cell dissolves its salt into the brine of the tank above
 * its inlet, and a player read the inlet under the word BRINE as where the brine goes.
 */
@TextHolder
public final class ChainInletNotes {

    static final TextKey ELECTROLYTIC_CELL = TextKey.of("jsc.chain_machine.electrolytic_cell.inlet_note",
            "Brine is made in the tank above from the salt and the water");

    /* By the id of the machine each is said for. */
    private static final Map<String, TextKey> BY_MACHINE = Map.of("electrolytic_cell", ELECTROLYTIC_CELL);

    private ChainInletNotes() {
    }

    /** The note for the machine {@code id}, or none. */
    public static Optional<TextKey> of(final String id) {
        return Optional.ofNullable(BY_MACHINE.get(id));
    }
}
