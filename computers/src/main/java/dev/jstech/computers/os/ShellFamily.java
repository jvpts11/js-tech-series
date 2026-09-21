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
 * The command-line syntax family a kernel gives its operating systems. It decides which command set the
 * terminal speaks and how paths are written: DOS-family kernels use drive letters, backslashes and verbs
 * like {@code dir}/{@code type}; POSIX-family kernels use a single rooted tree, forward slashes, mounted
 * media and verbs like {@code ls}/{@code cat}. An OS inherits the family from its kernel, so every
 * distribution built on the Linux kernel speaks bash syntax without declaring anything itself.
 */
public enum ShellFamily implements IStableName {

    /** Drive letters and DOS verbs (MC-DOS, the Frames command prompt). */
    DOS("dos"),

    /** A single rooted filesystem, mounts under {@code /media}, and Unix verbs (the Linux kernel). */
    POSIX("posix"),

    /**
     * The network appliance's own words, on a disk that keeps files and no folders (the {@code net_min}
     * kernel).
     *
     * <p>It is neither of the others on purpose. There are no paths to write, only names, so there is no
     * drive letter and no tree; the verbs are said in whole words rather than in either family's
     * abbreviations, and they are lowercase, since a machine of this kind never had the DOS family's
     * tolerance for shouting.
     */
    NET("net");

    private final String serializedName;

    ShellFamily(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
