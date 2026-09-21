/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;
import java.util.Set;

/**
 * The OS platform (family) a computer runs, and the axis a program is gated on.
 *
 * <p>A program declares the set of platforms it supports; an OS declares the single platform it is.
 * A program installs and runs only on an OS whose platform is in that set. This is independent of the
 * hardware era: it is about binary/API compatibility (a program built for one platform family does not
 * run on another), not about how advanced the hardware is.
 *
 * <p>A program that runs on several lists them all, which is most of them: what a desktop program needs is a
 * desktop, and every family that has one can hold it.
 */
public enum Platform implements IStableName {

    /** The MC-DOS command-line platform. */
    MC_DOS("mc_dos", "MC-DOS"),

    /** The MC-NET single-screen network platform. */
    MC_NET("mc_net", "MC-NET"),

    /** The Frames graphical desktop platform (Frames 95 / XP / 11). */
    FRAMES("frames", "Frames"),

    /** The Linux platform: every distribution on the Linux kernel (TTY, or a desktop environment on top). */
    LINUX("linux", "Linux"),

    /**
     * FreeBSD, which is a system and a platform both: one kernel, one base system and one ports tree, made in
     * one place. What is built for it is not what is built for Linux, however alike the two look at a prompt.
     */
    FREEBSD("freebsd", "FreeBSD"),

    /**
     * UNIX System V, the oldest of the families met at a Unix prompt and the only one a machine of the first age
     * runs. It takes its programs from media and from nowhere else, so what is built for it is its own.
     */
    UNIX("unix", "UNIX");

    private final String serializedName;
    private final String label;

    Platform(final String serializedName, final String label) {
        this.serializedName = serializedName;
        this.label = label;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** A human-readable name for tooltips and UI. */
    public String label() {
        return label;
    }

    /**
     * Whether this is one of the families met at a Unix prompt, whose media carry a shell script where the
     * others carry a setup program, and whose files sit under one root rather than on lettered drives.
     */
    public boolean unixLike() {
        return this == LINUX || this == FREEBSD || this == UNIX;
    }

    /** Whether everything in that set is of those families, so what is made for it is made for them alone. */
    public static boolean onlyUnixLike(final Set<Platform> platforms) {
        return !platforms.isEmpty() && platforms.stream().allMatch(Platform::unixLike);
    }
}
