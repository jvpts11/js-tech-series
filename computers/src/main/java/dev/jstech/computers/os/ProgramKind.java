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
 * Classification of a program by how it runs.
 *
 * <p>This is a description of the program's shape, not a gate: what a program can install and run on
 * is decided by the platforms and hardware minimums it declares (see {@link ProgramSpec} and
 * {@link OsGating}). Typically an {@link #APP} lists only desktop platforms while a {@link #SERVICE}
 * lists every platform, but that is the declaration's job, not this enum's.
 */
public enum ProgramKind implements IStableName {
    /** Foreground graphical application that opens windows (normally declared for desktop platforms). */
    APP("app"),
    /** Headless background service (normally declared for every platform). */
    SERVICE("service"),
    /** Headless core with an optional graphical panel where the platform provides a desktop. */
    HYBRID("hybrid"),
    /**
     * A desktop environment package (KDE Plasma, GNOME, Cinnamon): installing it turns a TTY-only Linux into
     * a graphical desktop. The matching {@link DesktopEnvironmentDef} describes the chrome it brings.
     */
    DESKTOP_ENVIRONMENT("desktop_environment");

    private final String serializedName;

    ProgramKind(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
