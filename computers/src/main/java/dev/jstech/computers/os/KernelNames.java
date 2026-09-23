/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.List;

/**
 * What the systems met at a Unix prompt call their kernel and the processor under it.
 *
 * <p>Each family has words of its own for the same machine, and they are the first thing that tells one from
 * another at a glance: a Linux says {@code x86_64} where FreeBSD and UNIX name the maker's architecture, which in
 * this world is Velocion's {@code vel64} for a 64-bit processor and the Integra Architecture for the narrower
 * ones, {@code IA-32} and {@code IA-16}. They are gathered here because a boot log, a login banner, {@code uname}
 * and a system report all have to agree, and four places writing them out was how they would stop agreeing.
 */
public final class KernelNames {

    /** The Linux kernel these machines run, named after the mod so it is plainly this world's own. */
    public static final String LINUX_VERSION = "6.8-jsc";

    /** The FreeBSD release these machines run. */
    public static final String FREEBSD_RELEASE = "14.1-RELEASE";

    /** The System V release these machines run, and the version of it, which that system reports apart. */
    public static final String SYSTEM_V_RELEASE = "3.2";
    public static final String SYSTEM_V_VERSION = "2";

    private KernelNames() {
    }

    /** The kernel's own name, which is what {@code uname} says with nothing asked of it. */
    public static String name(final Platform platform) {
        return switch (platform) {
            case FREEBSD -> "FreeBSD";
            case UNIX -> "UNIX";
            default -> "Linux";
        };
    }

    /** The version of it these machines run. */
    public static String release(final Platform platform) {
        return switch (platform) {
            case FREEBSD -> FREEBSD_RELEASE;
            case UNIX -> SYSTEM_V_RELEASE;
            default -> LINUX_VERSION;
        };
    }

    /**
     * The version a Frames system gives for itself, which is not a kernel with a name of its own the way a Unix
     * one is: the line a system report shows where a Unix system names its kernel.
     *
     * @param familyRank where the edition sits in the Frames line, counting from one
     */
    public static String frames(final int familyRank) {
        return switch (familyRank) {
            case 1 -> "Frames 4.00.950";
            case 2 -> "Frames NT 5.1";
            case 3 -> "Frames NT 10.0";
            default -> "Frames";
        };
    }

    /** What a system of that family calls the architecture of a processor that many bits wide. */
    public static String architecture(final Platform platform, final int bits) {
        if (platform != Platform.FREEBSD && platform != Platform.UNIX) {
            return bits >= 64 ? "x86_64" : "i686";
        }
        if (bits >= 64) {
            return "vel64";
        }
        // FreeBSD never ran on anything narrower than 32 bits, so that is the narrowest it has a word for.
        return bits >= 32 || platform == Platform.FREEBSD ? "IA-32" : "IA-16";
    }

    /** What a system of that family calls the first terminal on the machine's own screen. */
    public static String terminal(final Platform platform) {
        return switch (platform) {
            case FREEBSD -> "ttyv0";
            case UNIX -> "console";
            default -> "tty1";
        };
    }

    /**
     * What a medium of that family starts a machine from, by the names that family gives them: a kernel and its
     * first filesystem on a Linux, the loader and the kernel on FreeBSD, the one file System V keeps at its root.
     */
    public static List<String> bootFiles(final Platform platform) {
        return switch (platform) {
            case FREEBSD -> List.of("boot/loader", "boot/kernel/kernel");
            case UNIX -> List.of("unix");
            default -> List.of("boot/vmlinuz", "boot/initrd.img");
        };
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
        return switch (platform) {
            case FREEBSD -> "FreeBSD " + host + " " + FREEBSD_RELEASE + " FreeBSD " + FREEBSD_RELEASE
                    + " GENERIC " + arch;
            // System V gives the system, the node, the release, the version and the machine, and no more.
            case UNIX -> "UNIX " + host + " " + SYSTEM_V_RELEASE + " " + SYSTEM_V_VERSION + " " + arch;
            default -> "Linux " + host + " " + LINUX_VERSION + " #1 SMP " + arch + " GNU/Linux";
        };
    }
}
