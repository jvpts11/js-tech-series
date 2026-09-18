/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What the Arch tools really print: the package manager, the table writer and the image builder.
 *
 * <p>Anybody who has installed this distribution knows its package manager by sight, and knows it by its
 * shape rather than its words: the dependency resolution, the list of what is about to be installed, the two
 * totals, the question, the retrieval with a bar per package, the five checks, then the installation with a
 * bar per package again. A summary of that in three lines is not the tool, it is a description of the tool.
 *
 * <p>So the shape is here in full, and the figures in it are added up from the packages rather than written
 * down: the totals are the sum of the list, the counters count the list, and a list changed in one place
 * changes everything that reports on it.
 *
 * <p>Pure text and arithmetic, no Minecraft, so all of it can be held to the real thing in a test.
 */
public final class ArchInstallOutput {

    /** How wide the name column of a progress line is, which is what lines the bars up with each other. */
    private static final int NAME_COLUMN = 34;

    /** The bar a finished step draws, at the width a terminal this size gives it. */
    private static final String BAR = "######################";

    /**
     * What a base installation really pulls in, which is not what was asked for.
     *
     * <p>Asking for the base group, a kernel and its firmware resolves to a list, and it is the resolved
     * list that the tool prints, totals and counts. Each is named and versioned the way the real one names
     * and versions it, with what it costs to download and what it costs on the disk.
     */
    private static final Package[] BASE = {
            new Package("filesystem", "2024.04.07-1", 0.01, 0.01),
            new Package("glibc", "2.40-2", 6.86, 49.03),
            new Package("gcc-libs", "14.2.1-1", 37.40, 168.04),
            new Package("ncurses", "6.5-3", 1.05, 5.79),
            new Package("readline", "8.2.013-1", 0.32, 0.53),
            new Package("bash", "5.2.037-1", 1.80, 8.40),
            new Package("coreutils", "9.5-2", 2.85, 16.18),
            new Package("util-linux", "2.40.2-1", 1.21, 14.52),
            new Package("e2fsprogs", "1.47.1-3", 1.24, 5.02),
            new Package("systemd", "256.7-1", 8.21, 33.62),
            new Package("pacman", "6.1.0-3", 1.00, 5.30),
            new Package("grub", "2:2.12-3", 7.79, 39.14),
            new Package("linux-firmware", "20241017.3b11e15-1", 130.86, 700.68),
            new Package("linux", "6.11.5.arch1-1", 139.62, 149.29),
    };

    /** The hooks that run when the package changes are done, which is the last thing on the screen. */
    private static final String[] HOOKS = {
            "Creating system user accounts...",
            "Updating journal message catalog...",
            "Reloading system manager configuration...",
            "Arming ConditionNeedsUpdate...",
            "Updating the info directory file...",
    };

    /** The hooks a chroot install runs, which is fewer, because the system manager is already up. */
    private static final String[] LATER_HOOKS = {
            "Arming ConditionNeedsUpdate...",
            "Updating the info directory file...",
    };

    private ArchInstallOutput() {
    }

    /**
     * What installing the base system into a mounted root prints, from the install root to the last hook.
     *
     * @param root  where it is installing, which the first two lines name
     * @param speed how many megabytes a second this machine's connection manages, which sets the rates and
     *              the times the retrieval lines carry
     */
    public static List<String> pacstrap(final String root, final double speed) {
        final List<String> out = new ArrayList<>();
        out.add("==> Creating install root at " + root);
        out.add("==> Installing packages to " + root);
        out.add(":: Synchronizing package databases...");
        out.add(" core downloading...");
        out.add(" extra downloading...");
        out.addAll(transaction(BASE, speed, HOOKS));
        return List.copyOf(out);
    }

    /** What installing one named package inside the new system prints. */
    public static List<String> pacman(final String named, final double speed) {
        final Package one = new Package(named, "1.0-1", 1.78, 4.03);
        return transaction(new Package[]{one}, speed, LATER_HOOKS);
    }

    /**
     * The table of what to mount at boot, in the format the generator writes it in.
     *
     * <p>Every filesystem is named by its identifier rather than by its device, which is the whole reason
     * the tool is run with that flag: a disk moved to another port comes up all the same.
     *
     * @param root     the device the system is on
     * @param rootUuid that filesystem's identifier
     * @param boot     the device the firmware reads the bootloader from, or empty on a machine whose
     *                 firmware does not use one
     * @param bootUuid that filesystem's identifier, which is shorter because it is a different filesystem
     */
    public static List<String> fstab(final String root, final String rootUuid,
                                     final String boot, final String bootUuid) {
        final List<String> out = new ArrayList<>();
        out.add("# /dev/" + root);
        out.add("UUID=" + rootUuid + "\t/         \text4      \trw,relatime\t0 1");
        if (!boot.isEmpty()) {
            out.add("");
            out.add("# /dev/" + boot);
            out.add("UUID=" + bootUuid + "\t/boot     \tvfat      \trw,relatime,fmask=0022,dmask=0022,"
                    + "codepage=437,iocharset=ascii,shortname=mixed,utf8,errors=remount-ro\t0 2");
        }
        return List.copyOf(out);
    }

    /**
     * What building the boot images from the presets prints, which is one build per preset.
     *
     * <p>Both are built, the ordinary one and the one that carries every module in case the ordinary one
     * left out the module this machine turns out to need, and the hooks that go into them are named as they
     * run because that list is how anybody finds out why an image came out unable to reach its own disk.
     */
    public static List<String> mkinitcpio(final String release) {
        final List<String> out = new ArrayList<>();
        out.addAll(image(release, "default", "/boot/initramfs-linux.img",
                new String[]{"base", "udev", "autodetect", "microcode", "modconf", "kms", "keyboard",
                        "keymap", "consolefont", "block", "filesystems", "fsck"}));
        out.addAll(image(release, "fallback", "/boot/initramfs-linux-fallback.img",
                new String[]{"base", "udev", "microcode", "modconf", "kms", "keyboard", "keymap",
                        "consolefont", "block", "filesystems", "fsck"}));
        return List.copyOf(out);
    }

    /** The whole of one transaction: resolve, list, total, ask, retrieve, check, install, run the hooks. */
    private static List<String> transaction(final Package[] packages, final double speed,
                                            final String[] hooks) {
        final List<String> out = new ArrayList<>();
        out.add("resolving dependencies...");
        out.add("looking for conflicting packages...");
        out.add("");
        out.add("Packages (" + packages.length + ") " + named(packages));
        out.add("");
        out.add(String.format(Locale.ROOT, "Total Download Size:   %8.2f MiB", total(packages, true)));
        out.add(String.format(Locale.ROOT, "Total Installed Size:  %8.2f MiB", total(packages, false)));
        out.add("");
        out.add(":: Proceed with installation? [Y/n] ");
        out.add(":: Retrieving packages...");
        for (final Package one : packages) {
            out.add(retrieving(one, speed));
        }
        for (final String check : new String[]{"checking keys in the keyring", "checking package integrity",
                "loading package files", "checking for file conflicts", "checking available disk space"}) {
            out.add(counted(packages.length, packages.length, check));
        }
        out.add(":: Processing package changes...");
        for (int i = 0; i < packages.length; i++) {
            out.add(counted(i + 1, packages.length, "installing " + packages[i].name()));
        }
        out.add(":: Running post-transaction hooks...");
        for (int i = 0; i < hooks.length; i++) {
            out.add("(" + (i + 1) + "/" + hooks.length + ") " + hooks[i]);
        }
        return out;
    }

    /** One build of one image, from the preset it was read out of to whether it worked. */
    private static List<String> image(final String release, final String preset, final String file,
                                      final String[] hooks) {
        final List<String> out = new ArrayList<>();
        out.add("==> Building image from preset: /etc/mkinitcpio.d/linux.preset: '" + preset + "'");
        out.add("  -> -k /boot/vmlinuz-linux -c /etc/mkinitcpio.conf -g " + file);
        out.add("==> Starting build: '" + release + "'");
        for (final String hook : hooks) {
            out.add("  -> Running build hook: [" + hook + "]");
        }
        out.add("==> Generating module dependencies");
        out.add("==> Creating zstd-compressed initcpio image: '" + file + "'");
        out.add("==> Image generation successful");
        return out;
    }

    /** The one-line list of what is about to be installed, each package at the version it is at. */
    private static String named(final Package[] packages) {
        final StringBuilder out = new StringBuilder();
        for (final Package one : packages) {
            if (out.length() > 0) {
                out.append("  ");
            }
            out.append(one.name()).append('-').append(one.version());
        }
        return out.toString();
    }

    private static double total(final Package[] packages, final boolean download) {
        double sum = 0;
        for (final Package one : packages) {
            sum += download ? one.downloadMib() : one.installedMib();
        }
        return sum;
    }

    /**
     * One package's retrieval line: what it is, how big, how fast it came and how long it took.
     *
     * <p>The time is the size over the rate, so a machine on a slow connection watches bigger numbers go by
     * for longer, which is the one place in this output where the machine the player built shows through.
     */
    private static String retrieving(final Package one, final double speed) {
        final double rate = Math.max(0.1, speed);
        final int seconds = (int) (one.downloadMib() / rate);
        return String.format(Locale.ROOT, " %-30s %7.1f MiB %6.2f MiB/s %02d:%02d [%s] 100%%",
                one.name() + "-" + one.version() + "-x86_64", one.downloadMib(), rate,
                seconds / 60, seconds % 60, BAR);
    }

    /** A counted line, whose counter is as wide as the total so the column below it stays straight. */
    private static String counted(final int at, final int of, final String what) {
        final int width = String.valueOf(of).length();
        final String counter = String.format(Locale.ROOT, "(%" + width + "d/%d)", at, of);
        return String.format(Locale.ROOT, "%s %-" + NAME_COLUMN + "s [%s] 100%%", counter, what, BAR);
    }

    /** One package as the tool knows it: what it is called, which one it is, and what it costs twice over. */
    private record Package(String name, String version, double downloadMib, double installedMib) {
    }
}
