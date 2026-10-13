/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a Redstone Interface's screen says. */
@TextHolder
final class RedstoneInterfaceTexts {

    static final TextKey TITLE = TextKey.of("jsc.redstone_interface.title", "REDSTONE INTERFACE");
    static final TextKey NAME = TextKey.of("jsc.redstone_interface.name", "NAME");
    static final TextKey NAME_FIELD = TextKey.of("jsc.redstone_interface.name_field", "Redstone Interface name");
    static final TextKey HINT = TextKey.of("jsc.redstone_interface.hint", "Programs find this interface by its name");
    static final TextKey CLASH = TextKey.of("jsc.redstone_interface.clash",
            "Another interface already has this name");
    static final TextKey COMPUTER = TextKey.of("jsc.redstone_interface.computer", "COMPUTER");
    static final TextKey NO_COMPUTER = TextKey.of("jsc.redstone_interface.no_computer", "None");
    static final TextKey SIGNAL = TextKey.of("jsc.redstone_interface.signal", "SIGNAL");
    static final TextKey READS = TextKey.of("jsc.redstone_interface.reads", "Reads %s");
    static final TextKey EMITS = TextKey.of("jsc.redstone_interface.emits", "Emits %s");
    static final TextKey NO_POWER = TextKey.of("jsc.redstone_interface.no_power", "Off, no power");
    static final TextKey MODE = TextKey.of("jsc.redstone_interface.mode", "MODE");
    /** What a program set is marked with, the program's name after it. */
    static final TextKey SET_BY = TextKey.of("jsc.redstone_interface.set_by", "Set by %s");
    /** The two modes, by the words IQL writes them in, each with what it does. */
    static final TextKey IN = TextKey.of("jsc.redstone_interface.in", "IN");
    static final TextKey OUT = TextKey.of("jsc.redstone_interface.out", "OUT");
    static final TextKey IN_DOES = TextKey.of("jsc.redstone_interface.in_does", "reads");
    static final TextKey OUT_DOES = TextKey.of("jsc.redstone_interface.out_does", "emits");
    static final TextKey STRENGTH = TextKey.of("jsc.redstone_interface.strength", "STRENGTH");
    static final TextKey SOFTWARE = TextKey.of("jsc.redstone_interface.software", "SOFTWARE");

    private RedstoneInterfaceTexts() {
    }
}
