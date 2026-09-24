/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.core.text.Text;

/**
 * A request the Gateway would not carry out, and why: a permission the host has not granted, a name the
 * network does not know, a Mainframe that is not there. The ComputerCraft side turns it into the error its
 * program sees; nothing about it is a fault in the mod.
 *
 * <p>Why is a sentence. A ComputerCraft program is handed its English, the language one machine speaks to another in;
 * a program of this mod's own that asked is halted with it, and its player reads it in their own language.
 */
public final class GatewayRefusedException extends Exception {

    private final transient Text why;

    public GatewayRefusedException(final Text why) {
        super(why.english());
        this.why = why;
    }

    /** Why, as the player of a program that asked reads it. */
    public Text text() {
        return this.why;
    }
}
