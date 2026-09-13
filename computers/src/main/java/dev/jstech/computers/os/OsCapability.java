/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;

/**
 * The capability tier of an operating system, what it can do for the player.
 *
 * <p>Declared from least to most capable; a tier implies every capability declared before it, so
 * {@code compareTo} gates: an OS can run a program whose minimum capability compares less than or
 * equal to its own.
 */
public enum OsCapability implements IStableName {
    /** CLI terminal only, no graphical applications. Used by MC-DOS. */
    TERMINAL_ONLY("terminal_only"),
    /** Full-screen network GUI (the Network Interactor) plus a terminal, but no application desktop. */
    NETWORK_GUI("network_gui"),
    /** A graphical desktop with a window manager, filesystem, and application runtime. */
    FULL_DESKTOP("full_desktop");

    private final String serializedName;

    OsCapability(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
