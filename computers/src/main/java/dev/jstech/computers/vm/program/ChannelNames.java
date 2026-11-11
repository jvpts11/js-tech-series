/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * The names of the channels programs send on and hear (computers/docs/design/sigma.md): any text of a line, up to
 * {@link #MOST} characters, matched exactly, capitals and all, as a port's number is.
 */
@TextHolder
public final class ChannelNames {

    /** The longest name a channel may have. */
    public static final int MOST = 64;

    private static final TextKey NOT_A_CHANNEL = TextKey.of("jsc.vm.channel.not_a_channel",
            "'%s' is not a channel's name: one line of 1 to %s characters");

    private ChannelNames() {
    }

    /** {@code name} when it may name a channel, or the program stopped, saying why. */
    public static String checked(final String name, final int line) {
        if (name.isEmpty() || name.length() > MOST || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_CHANNEL.with(name, MOST));
        }
        return name;
    }
}
