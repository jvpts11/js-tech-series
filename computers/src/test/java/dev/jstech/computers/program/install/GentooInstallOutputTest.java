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
 * The Gentoo tools print what the real ones print.
 *
 * <p>The phases are what is held here, because the phases are what this distribution's tools are for: an
 * installation that says only that it is working tells nobody whether it is fetching, unpacking, configuring
 * or compiling, and those four are the whole difference between a build that is slow and one that is stuck.
 */
class GentooInstallOutputTest {

    private static final String HOST = "distfiles.mainframe";

    @Test
    void wget_reportsTheLengthAndSavesUnderTheNameItFetched() {
        final String out = String.join("\n", GentooInstallOutput.wget(HOST, 256, 11.2));
        assertTrue(out.contains("Resolving " + HOST), out);
        assertTrue(out.contains("HTTP request sent, awaiting response... 200 OK"), out);
        // 256 megabytes, in bytes, which is the figure the real one leads its length line with.
        assertTrue(out.contains("Length: 268435456 (256M) [application/x-xz]"), out);
        assertTrue(out.contains("Saving to: '" + GentooInstallOutput.STAGE3 + "'"), out);
        assertTrue(out.contains("saved [268435456/268435456]"), out);
    }

    /** A slower connection shows a lower rate and a longer wait, which is the machine showing through. */
    @Test
    void wget_reportsTheRateTheMachineReallyManages() {
        final String quick = String.join("\n", GentooInstallOutput.wget(HOST, 256, 32.0));
        final String slow = String.join("\n", GentooInstallOutput.wget(HOST, 256, 2.0));
        assertTrue(quick.contains("32.0MB/s"), quick);
        assertTrue(slow.contains("2.0MB/s"), slow);
        assertTrue(quick.contains("in 8s"), quick);
        assertTrue(slow.contains("in 128s"), slow);
    }

    @Test
    void unpack_namesEveryPathItLaysOutStartingAtTheRoot() {
        final List<String> out = GentooInstallOutput.unpack();
        assertEquals("./", out.get(0));
        assertTrue(out.size() > 40, "a whole system is more than a handful of paths");
        assertTrue(out.contains("./etc/portage/make.conf"), "the file the next step edits");
        assertTrue(out.contains("./usr/src/"), "where the kernel sources will go");
        for (final String path : out) {
            assertTrue(path.startsWith("./"), path + " is not a path inside the archive");
        }
    }

    /**
     * Fetching the tree as a snapshot and syncing it against the mirror are two different tools.
     *
     * <p>They are run for the same reason and print nothing alike, and printing one line for both would be
     * the tell that nobody involved had run either.
     */
    @Test
    void theTwoWaysOfGettingTheTree_doNotSoundAlike() {
        final String snapshot = String.join("\n", GentooInstallOutput.webrsync(HOST, 214_483));
        final String sync = String.join("\n", GentooInstallOutput.sync(HOST, 214_483));
        assertTrue(snapshot.startsWith("Fetching most recent snapshot ..."), snapshot);
        assertTrue(snapshot.contains("Checking signature ..."), snapshot);
        assertTrue(sync.startsWith(">>> Syncing repository 'gentoo'"), sync);
        assertTrue(sync.contains("Action: sync for repository 'gentoo', returned code = 0"), sync);
        assertTrue(!snapshot.equals(sync));
    }

    @Test
    void emerge_announcesEveryPhaseInTheOrderItRunsThem() {
        final List<String> out =
                GentooInstallOutput.emerge("app-editors/vim", "9.1.0764", "vim-9.1.0764.tar.xz", 4, false);
        final String[] phases = {
                "Calculating dependencies... done!",
                ">>> Verifying ebuild manifests",
                ">>> Emerging (1 of 1) app-editors/vim-9.1.0764::gentoo",
                ">>> Unpacking source...",
                ">>> Source unpacked in /var/tmp/portage/app-editors/vim-9.1.0764/work",
                ">>> Source prepared.",
                ">>> Source configured.",
                ">>> Source compiled.",
                ">>> Installing (1 of 1) app-editors/vim-9.1.0764::gentoo",
        };
        int at = -1;
        for (final String phase : phases) {
            final int found = out.indexOf(phase);
            assertTrue(found > at, phase + " is out of order in:\n" + String.join("\n", out));
            at = found;
        }
    }

    @Test
    void emerge_compilesWithAsManyJobsAsItWasGiven() {
        final String out = String.join("\n",
                GentooInstallOutput.emerge("app-editors/vim", "9.1.0764", "vim-9.1.0764.tar.xz", 6, false));
        assertTrue(out.contains("make -j6"), out);
    }

    /**
     * A package of kernel sources is unpacked and never compiled, because that is all it is.
     *
     * <p>The half hour everybody remembers belongs to the kernel builder, not to this: getting the sources
     * is getting a directory of source code, and a tool that claimed to compile it here would be claiming
     * the wrong step took the time.
     */
    @Test
    void emerge_kernelSources_areUnpackedAndNeverCompiled() {
        final String out = String.join("\n", GentooInstallOutput.emerge("sys-kernel/gentoo-sources",
                GentooInstallOutput.KERNEL, "linux-" + GentooInstallOutput.KERNEL + ".tar.xz", 4, true));
        assertTrue(out.contains(">>> Unpacking source..."), out);
        assertTrue(!out.contains(">>> Source compiled."), out);
        assertTrue(!out.contains("make -j"), out);
        assertTrue(out.contains(">>> Installing (1 of 1) sys-kernel/gentoo-sources-"), out);
    }

    @Test
    void genkernel_buildsTheKernelThenTheImageAndDisclaimsTheResult() {
        final List<String> out = GentooInstallOutput.genkernel("6.11.5", 4);
        final String all = String.join("\n", out);
        assertTrue(out.get(0).startsWith("* Gentoo Linux Genkernel"), all);
        assertTrue(all.contains("* Working with Linux kernel 6.11.5-gentoo for x86_64"), all);
        assertTrue(all.contains("*         >> Compiling 6.11.5-gentoo bzImage with 4 jobs ..."), all);
        assertTrue(all.contains("*         >> Compiling 6.11.5-gentoo modules ..."), all);
        assertTrue(all.contains("* initramfs: >> Initializing ..."), all);
        assertTrue(all.contains("* Kernel compiled successfully!"), all);
        assertTrue(all.contains("* Do NOT report kernel bugs as genkernel bugs"), all);
    }

    @Test
    void genkernel_aMachineWithOneCore_isToldSoInTheSingular() {
        final String out = String.join("\n", GentooInstallOutput.genkernel("6.11.5", 1));
        assertTrue(out.contains("bzImage with 1 job ..."), out);
    }
}
