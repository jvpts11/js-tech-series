/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

/**
 * What the systems met at a Unix prompt call their kernel and the processor under it.
 *
 * <p>Each family has words of its own for the same machine, and they are the first thing that tells one from
 * another at a glance: a Linux says {@code x86_64} where FreeBSD names the maker's architecture, which in this
 * world is Velocion's {@code vel64} for a 64-bit processor and the Integra Architecture, {@code IA-32}, for a
 * 32-bit one. They are gathered here because a boot log, a login banner, {@code uname} and a system report all
 * have to agree, and four places writing them out was how they would stop agreeing.
 */
public final class KernelNames {

    /** The Linux kernel these machines run, named after the mod so it is plainly this world's own. */
    public static final String LINUX_VERSION = "6.8-jsc";

    /** The FreeBSD release these machines run. */
    public static final String FREEBSD_RELEASE = "14.1-RELEASE";

    private KernelNames() {
    }

    /** The kernel's own name, which is what {@code uname} says with nothing asked of it. */
    public static String name(final Platform platform) {
        return platform == Platform.FREEBSD ? "FreeBSD" : "Linux";
    }

    /** The version of it these machines run. */
    public static String release(final Platform platform) {
        return platform == Platform.FREEBSD ? FREEBSD_RELEASE : LINUX_VERSION;
    }

    /** What a system of that family calls the architecture of a processor that many bits wide. */
    public static String architecture(final Platform platform, final int bits) {
        if (platform == Platform.FREEBSD) {
            return bits >= 64 ? "vel64" : "IA-32";
        }
        return bits >= 64 ? "x86_64" : "i686";
    }

    /** The three together, as a system report gives them: the name, the release, the architecture. */
    public static String kernel(final Platform platform, final int bits) {
        return name(platform) + " " + release(platform) + " " + architecture(platform, bits);
    }

    /**
     * Everything {@code uname -a} says, in that family's own order.
     *
     * @param host the name the machine answers to
     */
    public static String everything(final Platform platform, final String host, final int bits) {
        final String arch = architecture(platform, bits);
        if (platform == Platform.FREEBSD) {
            return "FreeBSD " + host + " " + FREEBSD_RELEASE + " FreeBSD " + FREEBSD_RELEASE + " GENERIC " + arch;
        }
        return "Linux " + host + " " + LINUX_VERSION + " #1 SMP " + arch + " GNU/Linux";
    }
}
