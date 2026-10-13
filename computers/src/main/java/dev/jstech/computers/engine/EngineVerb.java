/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

/**
 * The engine level of the contract: what every Network Operations Engine answers.
 *
 * <p>The first five move what the network already holds, and a network with no engine still does them, straight
 * through the Operations core. The rest make plans, which only an engine does: with none running they are refused,
 * and what asked is told the service is unavailable.
 */
public enum EngineVerb {
    /** Takes from the network's storage into a place that asked for it. */
    PULL(true),
    /** Puts into the network's storage. */
    PUSH(true),
    /** Moves between the network's own servers. */
    MOVE(true),
    /** Sends out of the network, to an inventory outside it or to nothing at all. */
    EXPORT(true),
    /** Fills a container somebody holds from what the network stores. */
    FILL(true),
    /** Makes something, planning how. */
    CRAFT(false),
    /** Shows how something would be made, without making it. */
    PLAN(false),
    /** Runs a statement of the network's language, in the engine's dialect. */
    QUERY(false),
    /** Changes an item the network holds with a personal-use card of the computer that asks. */
    UPDATE(false);

    private final boolean transfer;

    EngineVerb(final boolean transfer) {
        this.transfer = transfer;
    }

    /** Whether the verb only moves what is there, which a network does even with no engine running. */
    public boolean transfer() {
        return transfer;
    }
}
