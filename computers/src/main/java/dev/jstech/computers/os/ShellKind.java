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
 * The interactive shell a system ships.
 *
 * <p>The command syntax comes from the kernel's shell family; the shell only flavours how the prompt looks, and
 * each has a shape of its own: zsh ends in a percent sign with spaces about the path, bash runs the path up
 * against its dollar sign, and System V's sh leaves a space before the dollar. The shape is said here, with the
 * shell, so a system that ships a new one brings its prompt along instead of falling to somebody else's.
 */
public enum ShellKind implements IStableName {

    /**
     * The DOS family's command interpreter. Its own prompt is a drive and a path, drawn by that family; asked
     * for a Unix one, it gives the plain shape.
     */
    CMD("cmd", ":", "$"),

    /** The Bourne shell, which System V and FreeBSD ship. */
    SH("sh", ":", " $"),

    /** bash, which most Linux distributions ship. */
    BASH("bash", ":", "$"),

    /** zsh, which Arch ships. */
    ZSH("zsh", " ", " %");

    private final String serializedName;
    /** What stands between the machine's name and the path. */
    private final String beforePath;
    /** What closes the prompt after the path. */
    private final String end;

    ShellKind(final String serializedName, final String beforePath, final String end) {
        this.serializedName = serializedName;
        this.beforePath = beforePath;
        this.end = end;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** The prompt this shell stands at in {@code cwd}, for the player logged in on {@code hostname}. */
    public String prompt(final String hostname, final String cwd) {
        return "player@" + hostname + this.beforePath + cwd + this.end;
    }
}
