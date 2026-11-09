/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import java.util.Locale;
import java.util.Optional;

/**
 * The part a Mainframe's network engine plays (computers/docs/design/computers.md): the orchestrator runs the network,
 * its index and its Operations; a Subframe lends the orchestrator part of its capacity and its graphics cards' queues
 * when both run the same engine, counts for no conflict, keeps no index, and takes the network over when the
 * orchestrator falls. Saved and sent by its id.
 */
public enum EngineRole {

    ORCHESTRATOR(0, "orchestrator"),
    SUBFRAME(1, "subframe");

    private final int id;
    private final String word;

    EngineRole(final int id, final String word) {
        this.id = id;
        this.word = word;
    }

    /** The role of {@code id}; an id no role has reads as the orchestrator, which every Mainframe is to begin with. */
    public static EngineRole byId(final int id) {
        return id == SUBFRAME.id ? SUBFRAME : ORCHESTRATOR;
    }

    /** The role a player typed at the prompt, in any case; empty for a word that names none. */
    public static Optional<EngineRole> byWord(final String typed) {
        final String word = typed.toLowerCase(Locale.ROOT);
        for (final EngineRole role : values()) {
            if (role.word.equals(word)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    public int id() {
        return id;
    }

    /** The word that names it at the prompt, the same in every language. */
    public String word() {
        return word;
    }
}
