/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Cluster Management Computer says about a bulk install: why one did not start, what it is doing, how the
 * last one went, and why a node was left out of it. A disc's name and a node's name are data.
 */
@TextHolder
final class ClusterJobTexts {

    // Why a job did not start.
    static final TextKey NO_CARD = TextKey.of("jsc.cluster.job.no_card", "no cluster interface card");
    static final TextKey ALREADY_RUNNING = TextKey.of("jsc.cluster.job.already_running", "a job is already running");
    static final TextKey OUT_OF_REACH =
            TextKey.of("jsc.cluster.job.out_of_reach", "this card does not reach that cluster");
    static final TextKey NO_SYSTEM_DISC =
            TextKey.of("jsc.cluster.job.no_system_disc", "no system disc in a linked reader");
    static final TextKey NO_PROGRAM_DISC =
            TextKey.of("jsc.cluster.job.no_program_disc", "no program disc in a linked reader");
    static final TextKey INSTALLS_BY_HAND =
            TextKey.of("jsc.cluster.job.installs_by_hand", "that system installs by hand from its own shell");
    static final TextKey NOTHING_TO_INSTALL =
            TextKey.of("jsc.cluster.job.nothing_to_install", "that disc carries nothing to install");
    static final TextKey NO_NODES = TextKey.of("jsc.cluster.job.no_nodes", "no nodes in that cluster");

    // What a job is doing, and how the last one went.
    static final TextKey INSTALLING = TextKey.of("jsc.cluster.job.installing", "installing %s on %s");
    static final TextKey ONE_NODE = TextKey.of("jsc.cluster.job.one_node", "%s node");
    static final TextKey NODES = TextKey.of("jsc.cluster.job.nodes", "%s nodes");
    static final TextKey DONE_ON = TextKey.of("jsc.cluster.job.done_on", "done: %s on %s");
    static final TextKey CANCELLED_ON = TextKey.of("jsc.cluster.job.cancelled_on", "cancelled: %s on %s");
    static final TextKey WITH_SKIPPED = TextKey.of("jsc.cluster.job.with_skipped", "%s, %s skipped");

    // Why a node was left out, after its name.
    static final TextKey SKIPPED = TextKey.of("jsc.cluster.job.skipped", "%s (%s)");
    static final TextKey MISSING_RACK = TextKey.of("jsc.cluster.job.missing_rack", "missing rack");
    static final TextKey CHANGED = TextKey.of("jsc.cluster.job.changed", "changed while writing");
    static final TextKey BAY_OFF = TextKey.of("jsc.cluster.job.bay_off", "bay off");
    static final TextKey ALREADY_INSTALLED = TextKey.of("jsc.cluster.job.already_installed", "already installed");
    static final TextKey UNKNOWN_PROGRAM = TextKey.of("jsc.cluster.job.unknown_program", "unknown program");
    static final TextKey NO_SYSTEM = TextKey.of("jsc.cluster.job.no_system", "no system");
    static final TextKey REQUIREMENTS =
            TextKey.of("jsc.cluster.job.requirements", "does not meet the program's requirements");
    static final TextKey TOO_OLD = TextKey.of("jsc.cluster.job.too_old", "too old for the program");

    private ClusterJobTexts() {
    }
}
