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
 * What the Gentoo tools really print: the fetch, the unpack, the tree sync, the merges and the kernel build.
 *
 * <p>This distribution is installed by fetching an archive of a working system, unpacking it over the disk
 * and then building the rest, and each of those tools has a voice of its own that anybody who has done it
 * once recognises immediately. The fetcher counts the bytes down; the archiver names every path it lays
 * out; the package manager talks in arrows and says what it is doing to what, in order; the kernel builder
 * talks in asterisks and tells you twice that a kernel that will not boot is not its fault.
 *
 * <p>Pure text, no Minecraft, so all of it can be held to the real thing in a test.
 */
public final class GentooInstallOutput {

    /** The release this medium carries, which every line that names a version names. */
    public static final String STAGE3 = "stage3-amd64-openrc.tar.xz";

    /** The kernel the sources are of, and the release the built kernel is installed as. */
    public static final String KERNEL = "6.11.5";

    /** How wide the bar the fetcher draws is, in a terminal of this width. */
    private static final int BAR_WIDTH = 19;

    /**
     * The head of what unpacking an archive of a whole system lists.
     *
     * <p>The real archive holds a hundred thousand paths and the tool names every one of them; this is what
     * the first screenful of that looks like, which is all anybody ever reads of it before the rest scrolls
     * past. The order is the order the archive is in: the root, then its directories, then into the first
     * of them.
     */
    private static final String[] UNPACKED = {
            "./", "./bin", "./boot/", "./dev/", "./etc/", "./etc/DIR_COLORS", "./etc/bash/",
            "./etc/bash/bashrc", "./etc/conf.d/", "./etc/csh.cshrc", "./etc/env.d/", "./etc/environment",
            "./etc/fstab", "./etc/group", "./etc/hosts", "./etc/init.d/", "./etc/inittab", "./etc/issue",
            "./etc/locale.gen", "./etc/passwd", "./etc/portage/", "./etc/portage/make.conf",
            "./etc/profile", "./etc/shadow", "./etc/shells", "./home/", "./lib", "./lib64/", "./media/",
            "./mnt/", "./opt/", "./proc/", "./root/", "./run/", "./sbin", "./sys/", "./tmp/", "./usr/",
            "./usr/bin/", "./usr/lib/", "./usr/lib64/", "./usr/local/", "./usr/sbin/", "./usr/share/",
            "./usr/src/", "./var/", "./var/cache/", "./var/db/", "./var/empty/", "./var/lib/",
            "./var/log/", "./var/spool/", "./var/tmp/",
    };

    private GentooInstallOutput() {
    }

    /**
     * What the fetcher prints while it pulls the archive down.
     *
     * <p>Everything the real one prints except the two lines that stamp the wall clock on either end of the
     * transfer: this world has a sun but no calendar, and a made-up date would be the one thing on the
     * screen that is not true.
     *
     * @param host    where it is pulling from
     * @param sizeMb  how big the archive is
     * @param speed   how many megabytes a second this machine's connection manages
     */
    public static List<String> wget(final String host, final long sizeMb, final double speed) {
        final double rate = Math.max(0.1, speed);
        final int seconds = (int) Math.max(1, sizeMb / rate);
        final long bytes = sizeMb * 1024L * 1024L;
        return List.of(
                "--  https://" + host + "/releases/amd64/autobuilds/" + STAGE3,
                "Resolving " + host + "... done",
                "Connecting to " + host + "|10.0.0.1|:443... connected.",
                "HTTP request sent, awaiting response... 200 OK",
                "Length: " + bytes + " (" + sizeMb + "M) [application/x-xz]",
                "Saving to: '" + STAGE3 + "'",
                "",
                String.format(Locale.ROOT, "%-19s 100%%[%s>] %5dM %5.1fMB/s    in %ds",
                        STAGE3.substring(0, Math.min(19, STAGE3.length())),
                        "=".repeat(BAR_WIDTH), sizeMb, rate, seconds),
                "",
                "'" + STAGE3 + "' saved [" + bytes + "/" + bytes + "]");
    }

    /** What the archiver names while it lays a whole system out, which is every path in it. */
    public static List<String> unpack() {
        return List.of(UNPACKED);
    }

    /**
     * What fetching the whole package tree as one snapshot prints.
     *
     * <p>This is the way the handbook has it on a fresh medium, because pulling the tree file by file over
     * the network takes longer than pulling it in one piece and unpacking it.
     */
    public static List<String> webrsync(final String host, final int files) {
        return List.of(
                "Fetching most recent snapshot ...",
                "Trying to retrieve the latest snapshot from https://" + host + " ...",
                "Fetching file portage-latest.tar.xz.md5sum ...",
                "Fetching file portage-latest.tar.xz.gpgsig ...",
                "Fetching file portage-latest.tar.xz ...",
                "Checking signature ...",
                "gpg: Good signature from \"Gentoo ebuild repository signing key\"",
                "Getting snapshot timestamp ...",
                "Syncing local tree ...",
                "Number of files: " + files,
                "Number of created files: " + files,
                "Cleaning up ...");
    }

    /** What syncing an already-fetched tree against the mirror prints. */
    public static List<String> sync(final String host, final int files) {
        return List.of(
                ">>> Syncing repository 'gentoo' into '/var/db/repos/gentoo'...",
                " * Using keys from /usr/share/openpgp-keys/gentoo-release.asc",
                " * Refreshing keys from keyserver ...                            [ ok ]",
                ">>> Starting rsync with rsync://" + host + "/gentoo-portage",
                "Number of files: " + files,
                "Number of created files: 0",
                ">>> Checking server timestamp ...",
                ">>> Syncing repository 'gentoo' ... done",
                "",
                "Action: sync for repository 'gentoo', returned code = 0");
    }

    /**
     * What merging one package prints, from working out what it needs to saying the job is done.
     *
     * <p>Every phase the real one announces is announced, because those arrows are how anybody watching a
     * build knows whether it is fetching, unpacking, configuring or compiling, and a build that says only
     * that it is building is a build nobody can tell is stuck.
     *
     * @param atom    the package's category and name
     * @param version the version on its own, which the working directory is named after
     * @param archive the file it unpacks, which is not always named after the package that carries it
     * @param jobs    how many jobs it is compiling with, which the machine's cores decide
     * @param sources whether this is a package of kernel sources, which are unpacked but never compiled
     */
    public static List<String> emerge(final String atom, final String version, final String archive,
                                      final int jobs, final boolean sources) {
        final String work = "/var/tmp/portage/" + atom + "-" + version + "/work";
        final List<String> out = new ArrayList<>();
        out.add("Calculating dependencies... done!");
        out.add("");
        out.add(">>> Verifying ebuild manifests");
        out.add("");
        out.add(">>> Emerging (1 of 1) " + atom + "-" + version + "::gentoo");
        out.add(">>> Downloading 'https://distfiles.mainframe/distfiles/" + archive + "'");
        out.add(">>> Unpacking source...");
        out.add(">>> Unpacking " + archive + " to " + work);
        out.add(">>> Source unpacked in " + work);
        if (!sources) {
            out.add(">>> Preparing source in " + work + " ...");
            out.add(">>> Source prepared.");
            out.add(">>> Configuring source in " + work + " ...");
            out.add(">>> Source configured.");
            out.add(">>> Compiling source in " + work + " ...");
            out.add("make -j" + jobs);
            out.add(">>> Source compiled.");
        }
        out.add(">>> Install " + atom + "-" + version + " into " + work + "/image");
        out.add(">>> Completed installing " + atom + "-" + version + " into " + work + "/image");
        out.add("");
        out.add(">>> Installing (1 of 1) " + atom + "-" + version + "::gentoo");
        out.add("");
        out.add(">>> Jobs: 1 of 1 complete                           Load avg: 1.02, 0.85, 0.52");
        out.add(">>> Auto-cleaning packages...");
        out.add("");
        out.add(">>> No outdated packages were found on your system.");
        return List.copyOf(out);
    }

    /**
     * What the kernel builder prints, which talks in asterisks and ends by disclaiming responsibility.
     *
     * <p>The warning at the end is the real one's, near enough word for word, and it is there for the same
     * reason: the tool builds a kernel from a configuration it did not write, and the commonest thing that
     * goes wrong with a hand-built system is a kernel that cannot reach the disk it is on.
     */
    public static List<String> genkernel(final String release, final int jobs) {
        final List<String> out = new ArrayList<>();
        out.add("* Gentoo Linux Genkernel; Version 4.3.9");
        out.add("* Running with options: all");
        out.add("");
        out.add("* Working with Linux kernel " + release + "-gentoo for x86_64");
        out.add("* Using genkernel configuration from '/etc/genkernel.conf' ...");
        out.add("");
        out.add("* kernel: >> Initializing ...");
        out.add("*         >> Running 'make oldconfig' ...");
        out.add("*         >> Compiling " + release + "-gentoo bzImage with " + jobs
                + (jobs == 1 ? " job ..." : " jobs ..."));
        out.add("*         >> Compiling " + release + "-gentoo modules ...");
        out.add("*         >> Installing " + release + "-gentoo modules ...");
        out.add("* initramfs: >> Initializing ...");
        out.add("*         >> Appending busybox cpio data ...");
        out.add("*         >> Appending modules cpio data ...");
        out.add("*         >> Deduping cache ...");
        out.add("*         >> Pre-generating initramfs ...");
        out.add("");
        out.add("* Kernel compiled successfully!");
        out.add("");
        out.add("* Required Kernel Parameters:");
        out.add("*     root=/dev/$ROOT");
        out.add("*");
        out.add("* Do NOT report kernel bugs as genkernel bugs unless your bug");
        out.add("* is about the default genkernel configuration.");
        return List.copyOf(out);
    }
}
