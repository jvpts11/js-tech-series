/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.KnotRepository;

/**
 * The KnotHub a machine at the prompt reaches, on its own network or across a federation, as {@code knot} works it.
 */
public interface ICliKnot {

    /** The repository the hub keeps. */
    KnotRepository repository();

    /** The machine running the hub, as the network names it. */
    String host();

    /** The hub's network as the federation names it; empty when it is the machine's own. */
    String network();

    /** Whether the machine may read the repository: look at it, pull from it, read its log. */
    boolean maySee();

    /** Whether the machine may send it a revision. */
    boolean maySend();

    /** Who a revision this machine pushes is written down as: the player at it, or the machine when a program asks. */
    String author();

    /** The world's tick now, which a revision is written down at. */
    long now();

    /** Writes what changed in the repository back onto the server that keeps it. */
    void changed();
}
