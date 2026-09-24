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
 * What a monitor says when it has nothing to show, and why: the line a player reads above the hotbar when they use
 * one that is not linked, or linked to a machine that cannot drive it.
 */
@TextHolder
final class MonitorTexts {

    static final TextKey UNLINKED = TextKey.of("block.jsc.monitor.unlinked", "No computer linked");
    static final TextKey NO_COMPUTER = TextKey.of("block.jsc.monitor.no_computer",
            "No computer found in range over a Peripheral Cable");
    static final TextKey NO_GPU = TextKey.of("block.jsc.monitor.no_gpu",
            "The computer has no GPU - install a GPU to host monitors (4 per GPU)");
    static final TextKey AT_CAPACITY = TextKey.of("block.jsc.monitor.at_capacity",
            "The computer's monitor outputs are all in use");
    static final TextKey NO_POWER = TextKey.of("block.jsc.monitor.no_power", "No signal - the computer is powered off");
    static final TextKey RACK_EMPTY = TextKey.of("block.jsc.monitor.rack_empty", "The rack holds no computer to show");
    static final TextKey NEEDS_KVM = TextKey.of("block.jsc.monitor.needs_kvm",
            "This rack holds several computers - mount a KVM Switch to pick one");
    static final TextKey IN_USE = TextKey.of("block.jsc.monitor.in_use", "%s is using this monitor");

    private MonitorTexts() {
    }
}
