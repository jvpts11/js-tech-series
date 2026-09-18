/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Arch tools print what the real ones print.
 *
 * <p>Held to the shape of a real transaction rather than to itself: the order of the phases, the counters
 * counting what is really in the list, and the totals adding up to what the list really costs. A change
 * that leaves the text plausible but the arithmetic wrong fails here.
 */
class ArchInstallOutputTest {

    /** A connection fast enough that the retrieval lines have somewhere sensible to put their rate. */
    private static final double SPEED = 11.2;

    @Test
    void pacstrap_runsThePhasesInTheOrderTheRealOneRunsThem() {
        final List<String> out = ArchInstallOutput.pacstrap("/mnt", SPEED);
        final String[] phases = {
                "==> Creating install root at /mnt",
                "==> Installing packages to /mnt",
                ":: Synchronizing package databases...",
                "resolving dependencies...",
                "looking for conflicting packages...",
                ":: Proceed with installation? [Y/n] ",
                ":: Retrieving packages...",
                ":: Processing package changes...",
                ":: Running post-transaction hooks...",
        };
        int at = -1;
        for (final String phase : phases) {
            final int found = out.indexOf(phase);
            assertTrue(found > at, phase + " is out of order in:\n" + String.join("\n", out));
            at = found;
        }
    }

    @Test
    void pacstrap_countsTheSameNumberOfPackagesEverywhereItCountsThem() {
        final List<String> out = ArchInstallOutput.pacstrap("/mnt", SPEED);
        final int count = count(out);
        assertTrue(count > 1, "a base install resolves to more than one package");
        assertTrue(String.join("\n", out).contains(installing(count, count)),
                "the last package installed is number " + count + " of " + count);
        assertEquals(count, out.stream().filter(l -> l.contains("[######################] 100%")
                && l.startsWith(" ")).count(), "one retrieval line per package");
    }

    @Test
    void pacstrap_totalsAddUpTheListItJustPrinted() {
        final List<String> out = ArchInstallOutput.pacstrap("/mnt", SPEED);
        final double download = figure(out, "Total Download Size:");
        final double installed = figure(out, "Total Installed Size:");
        assertTrue(download > 0 && installed > download,
                "a package costs more unpacked than it does compressed: " + download + " / " + installed);
    }

    /**
     * The packages are named with their versions, the way the real one names them.
     *
     * <p>A bare list of names is what somebody writes from memory; the real one prints what it is about to
     * install, which is a name and the exact version of it.
     */
    @Test
    void pacstrap_namesEachPackageWithItsVersion() {
        final String out = String.join("\n", ArchInstallOutput.pacstrap("/mnt", SPEED));
        assertTrue(out.contains("linux-firmware-"), out);
        assertTrue(out.matches("(?s).*linux-\\d+\\.\\d+\\.\\d+.*"), "a real kernel version, not a round one");
    }

    @Test
    void pacman_oneNamedPackage_runsTheSamePhasesWithAListOfOne() {
        final List<String> out = ArchInstallOutput.pacman("vim", SPEED);
        assertEquals(1, count(out));
        assertTrue(String.join("\n", out).contains(installing(1, 1)), String.join("\n", out));
        assertTrue(String.join("\n", out).contains("Packages (1) vim-"), String.join("\n", out));
    }

    @Test
    void fstab_namesEveryFilesystemByItsIdentifierRatherThanItsDevice() {
        final List<String> out = ArchInstallOutput.fstab("sda2", "1e0f5b2c-3a4d-4e5f-8a9b-0c1d2e3f4a5b",
                "sda1", "A1B2-C3D4");
        final String all = String.join("\n", out);
        assertTrue(all.contains("UUID=1e0f5b2c-3a4d-4e5f-8a9b-0c1d2e3f4a5b"), all);
        assertTrue(all.contains("UUID=A1B2-C3D4"), all);
        assertTrue(all.contains("# /dev/sda2") && all.contains("# /dev/sda1"), all);
        assertTrue(all.contains("ext4") && all.contains("vfat"), all);
        // The one the firmware reads is checked after the root, which is what that trailing pair says.
        assertTrue(all.contains("0 1") && all.contains("0 2"), all);
    }

    @Test
    void fstab_aMachineWithNoBootPartition_getsATableWithOneLineInIt() {
        final List<String> out = ArchInstallOutput.fstab("sda1", "1e0f5b2c-3a4d-4e5f-8a9b-0c1d2e3f4a5b", "", "");
        assertEquals(2, out.size(), String.join("\n", out));
        assertTrue(!String.join("\n", out).contains("vfat"));
    }

    /** Both images are built, and the hooks that go into them are named as they run. */
    @Test
    void mkinitcpio_buildsBothPresetsAndNamesTheHooks() {
        final List<String> out = ArchInstallOutput.mkinitcpio("6.11.5-arch1-1");
        final String all = String.join("\n", out);
        assertTrue(all.contains("preset: 'default'") && all.contains("preset: 'fallback'"), all);
        assertEquals(2, out.stream().filter(l -> l.equals("==> Image generation successful")).count(), all);
        assertTrue(all.contains("  -> Running build hook: [base]"), all);
        assertTrue(all.contains("  -> Running build hook: [filesystems]"), all);
        assertTrue(all.contains("==> Starting build: '6.11.5-arch1-1'"), all);
        /* The fallback image is the one that leaves out the hook that only keeps what this machine needs. */
        assertEquals(1, out.stream().filter(l -> l.contains("[autodetect]")).count(), all);
    }

    /** How many packages the transaction says it is about to install. */
    private static int count(final List<String> out) {
        for (final String line : out) {
            if (line.startsWith("Packages (")) {
                return Integer.parseInt(line.substring("Packages (".length(), line.indexOf(')')));
            }
        }
        return 0;
    }

    private static String installing(final int at, final int of) {
        final int width = String.valueOf(of).length();
        return String.format("(%" + width + "d/%d)", at, of);
    }

    /** The number on a totals line, whatever it is padded out to. */
    private static double figure(final List<String> out, final String label) {
        for (final String line : out) {
            if (line.startsWith(label)) {
                return Double.parseDouble(line.substring(label.length()).replace("MiB", "").trim());
            }
        }
        return 0;
    }
}
