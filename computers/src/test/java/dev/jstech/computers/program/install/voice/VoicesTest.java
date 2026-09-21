/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tools of a by-hand installation say what the real ones say, in the order they say it.
 *
 * <p>Each is played from its first tick to its last the way the machine plays it, a tick at a time with
 * somebody watching, and what came out is held to the real tool: its phases in order, its figures adding up,
 * and what it was run for done at the end and not a moment before.
 */
class VoicesTest {

    /** The disk of the real run the filesystem figures come from: 24,747,008 blocks of four kilobytes. */
    private static final long REAL_RUN_MB = 24_747_008L * 4096L / (1024L * 1024L);

    private static final WorldStamp NOON_ON_DAY_214 = WorldStamp.of(214L * 24_000L + 6_000L);

    /** Plays a script to its end, a tick at a time, answering yes to whatever it asks. */
    private static Glass played(final TtyScript script) {
        final Glass glass = new Glass();
        final TtyScriptProcess tool = new TtyScriptProcess(script);
        tool.begin(0);
        for (long tick = 0; !tool.over() && tick < 200_000; tick++) {
            tool.advance(tick, glass);
            if (tool.asking() != null) {
                tool.answer("y", tick, glass);
            }
        }
        assertTrue(tool.over(), "the tool came to an end");
        return glass;
    }

    private static void inOrder(final Glass glass, final String... phases) {
        int at = -1;
        for (final String phase : phases) {
            int found = -1;
            for (int i = at + 1; i < glass.shown.size(); i++) {
                if (glass.shown.get(i).contains(phase)) {
                    found = i;
                    break;
                }
            }
            assertTrue(found > at, "\"" + phase + "\" is missing or out of order in:\n" + glass.all());
            at = found;
        }
    }

    @Test
    void mke2fs_printsTheFiguresOfTheRealRun() {
        final Glass glass = played(DiskVoices.mke2fs("sda3", REAL_RUN_MB, 1, 140, () -> { }));
        inOrder(glass, "mke2fs 1.47.1 (20-May-2024)",
                "Creating filesystem with 24747008 4k blocks and 6193152 inodes",
                "Filesystem UUID: ", "Superblock backups stored on blocks:",
                "32768, 98304, 163840, 229376, 294912, 819200, 884736, 1605632,", "23887872",
                "Allocating group tables: done", "Writing inode tables: done",
                "Creating journal (131072 blocks): done",
                "Writing superblocks and filesystem accounting information: done");
    }

    /** The counters run on the line they started on, which is what makes formatting something to watch. */
    @Test
    void mke2fs_countsItsWayThroughTheGroupsInPlace() {
        final Glass glass = played(DiskVoices.mke2fs("sda3", REAL_RUN_MB, 1, 400, () -> { }));
        assertTrue(glass.redraws > 50, "the counts were redrawn many times: " + glass.redraws);
        assertTrue(glass.everShown.stream().anyMatch(l -> l.matches("Writing inode tables: +\\d+/756")),
                "a count on its way up was on the glass at some point");
        assertFalse(glass.all().contains("/756"), "and none of them is left there at the end:\n" + glass.all());
    }

    @Test
    void mke2fs_aTinyFilesystem_isMadeWithoutAJournal() {
        assertFalse(played(DiskVoices.mke2fs("sda", 4, 1, 40, () -> { })).all().contains("Creating journal"));
    }

    @Test
    void wget_namesTheMirrorAndNeverTheWeb() {
        final Glass glass = played(FetchVoice.wget("/gentoo/stage3-vel64-openrc.tar.xz", 256, 320,
                NOON_ON_DAY_214, () -> { }));
        inOrder(glass, "--Day 214 12:00:00--  mirror://mainframe/gentoo/stage3-vel64-openrc.tar.xz",
                "Resolving mainframe... done.", "Connecting to mainframe... connected.",
                "Mirror request sent, awaiting response... 200 OK",
                "Length: 268435456 (256M) [application/x-xz]", "Saving to: 'stage3-vel64-openrc.tar.xz'",
                "100%[==============]", "saved [268435456/268435456]");
        assertFalse(glass.all().contains("http"), glass.all());
    }

    /** The bar really fills: it was on the glass part way along before it was on the glass full. */
    @Test
    void wget_theBarFillsRatherThanArrivingFull() {
        final Glass glass = played(FetchVoice.wget("/gentoo/stage3.tar.xz", 256, 320, NOON_ON_DAY_214, () -> { }));
        assertTrue(glass.everShown.stream().anyMatch(l -> l.contains(" 50%[") && l.contains("eta ")),
                "half way along it said so, with the time left");
        assertTrue(glass.all().contains("in 16s"), "and at the end it said how long it had taken:\n" + glass.all());
        assertTrue(glass.all().contains("16.0MB/s"), glass.all());
    }

    @Test
    void unpack_namesEveryPathStartingAtTheRoot_orNothingWhenAskedToBeQuiet() {
        final Glass loud = played(ArchiveVoice.unpack(true, 120, () -> { }));
        assertEquals("./", loud.shown.getFirst());
        assertEquals(ArchiveVoice.pathCount(), loud.shown.size());
        assertTrue(loud.shown.size() > 900, "a whole system is a great many paths: " + loud.shown.size());
        assertTrue(loud.shown.contains("./etc/portage/make.conf"), "the file the next step edits");
        assertTrue(played(ArchiveVoice.unpack(false, 120, () -> { })).shown.isEmpty(),
                "asked to work quietly, it works quietly");
    }

    @Test
    void emerge_listsAsksAndTakesEachPackageThroughEveryPhase() {
        final List<PortageVoices.Merge> merges = List.of(
                new PortageVoices.Merge("app-shells/bash", "5.2_p37", "5.2_p26", "bash-5.2.tar.gz", 10.7,
                        "net nls readline", "-afs -plugins", 14, 120),
                new PortageVoices.Merge("app-editors/vim", "9.1.0794", "", "vim-9.1.0794.tar.gz", 16.8,
                        "acl crypt nls", "-X -lua", 20, 160));
        final Glass glass = played(PortageVoices.emerge(merges, true, 4, () -> { }));
        inOrder(glass, "These are the packages that would be merged, in order:",
                "Calculating dependencies... done!", "[ebuild     U  ] app-shells/bash-5.2_p37 [5.2_p26]",
                "[ebuild  N     ] app-editors/vim-9.1.0794", "Total: 2 packages (1 upgrade, 1 new)",
                "Would you like to merge these packages? [Yes/No] y", ">>> Verifying ebuild manifests",
                ">>> Emerging (1 of 2) app-shells/bash-5.2_p37::gentoo",
                ">>> Downloading 'mirror://mainframe/distfiles/bash-5.2.tar.gz'", "BLAKE2B SHA512 size ;-)",
                ">>> Unpacking source...", ">>> Source prepared.", "checking build system type",
                ">>> Source configured.", "make -j4", "x86_64-pc-linux-gnu-scc", ">>> Source compiled.",
                ">>> Installing (1 of 2)", ">>> app-shells/bash-5.2_p37 merged.",
                ">>> Emerging (2 of 2) app-editors/vim-9.1.0794::gentoo", ">>> Jobs: 2 of 2 complete");
    }

    @Test
    void emerge_answeredNo_quitsAndMergesNothing() {
        final boolean[] merged = {false};
        final TtyScriptProcess tool = new TtyScriptProcess(PortageVoices.emerge(List.of(
                new PortageVoices.Merge("app-editors/vim", "9.1", "", "vim-9.1.tar.gz", 16.8, "acl", "", 20, 160)),
                true, 4, () -> merged[0] = true));
        final Glass glass = new Glass();
        tool.begin(0);
        for (long tick = 0; tool.asking() == null && tick < 1_000; tick++) {
            tool.advance(tick, glass);
        }
        tool.answer("n", 1_000, glass);
        assertTrue(tool.over());
        assertFalse(merged[0]);
        assertTrue(glass.all().contains("Quitting."), glass.all());
    }

    /** Kernel sources are fetched, unpacked and patched, and never compiled: that is the next step's job. */
    @Test
    void emerge_aPackageWithNothingToBuild_skipsTheConfigureAndTheCompile() {
        final Glass glass = played(PortageVoices.emerge(List.of(new PortageVoices.Merge(
                "sys-kernel/gentoo-sources", "6.11.5", "", "linux-6.11.tar.xz", 140, "", "-symlink", 180, 0)),
                false, 4, () -> { }));
        inOrder(glass, ">>> Emerging (1 of 1) sys-kernel/gentoo-sources-6.11.5::gentoo", ">>> Unpacking source...",
                ">>> Source prepared.", ">>> Installing (1 of 1)");
        assertFalse(glass.all().contains("Source compiled"), glass.all());
        assertFalse(glass.all().contains("make -j"), glass.all());
    }

    /** The load sits at the right-hand edge of the glass, and one column past it is a line broken in two. */
    @Test
    void emerge_theClosingCountOfJobs_reachesTheEdgeOfTheTerminalAndNoFurther() {
        for (final int jobs : new int[] {1, 4, 16}) {
            final Glass glass = played(PortageVoices.emerge(List.of(new PortageVoices.Merge(
                    "app-misc/mines", "5.1", "", "mines-5.1.tar.xz", 4.0, "nls", "-debug", 10, 40)),
                    false, jobs, () -> { }));
            final String line = glass.everShown.stream().filter(said -> said.contains("Jobs: 1 of 1 complete"))
                    .findFirst().orElse("");
            assertEquals(Bars.COLUMNS, line.length(), line);
            assertTrue(line.endsWith("2.84, 1.37"), line);
        }
    }

    @Test
    void genkernel_namesEachPhaseAsItStartsAndDisclaimsTheResult() {
        final Glass glass = played(KernelVoices.genkernel("6.11.5", 1_280, () -> { }));
        inOrder(glass, "Gentoo Linux Genkernel; Version", "Working with Linux kernel 6.11.5-gentoo for x86_64",
                ">> Running 'make oldconfig' ...", ">> Compiling 6.11.5-gentoo bzImage ...",
                ">> Compiling 6.11.5-gentoo modules ...", "initramfs: >> Initializing ...",
                "Kernel compiled successfully!", "Do NOT report kernel bugs as genkernel bugs");
        assertTrue(glass.shown.size() < 40, "it is the quiet one: " + glass.shown.size() + " lines");
    }

    @Test
    void make_scrollsEveryObjectThenLinksThenBuildsTheImage() {
        final Glass glass = played(KernelVoices.make("6.11.5", 4, 1_280, () -> { }));
        inOrder(glass, "  HOSTSCC scripts/basic/fixdep", "  SCC     ", "  AR      ", "  LD      vmlinux",
                "  BUILD   arch/x86/boot/bzImage", "Kernel: arch/x86/boot/bzImage is ready  (#1)",
                "  DEPMOD  /lib/modules/6.11.5-gentoo", "  INSTALL /boot");
        assertTrue(glass.shown.size() > 1_000, "it is the one that scrolls: " + glass.shown.size() + " lines");
        assertTrue(glass.all().contains("  SCC [M] "), "and some of what it builds is built as modules");
    }

    /**
     * The software of this world is written in Sigma and built by the Sigma compiler into the listings a
     * machine runs. There is no C in it, so no tool may name a C compiler, a C source, a header or an object.
     */
    @Test
    void noTool_namesACompilerOrAFileOfALanguageThisWorldDoesNotHave() {
        final List<PortageVoices.Merge> merges = List.of(new PortageVoices.Merge("app-editors/vim", "9.1", "",
                "vim-9.1.tar.gz", 16.8, "acl", "-X", 20, 400));
        final List<Glass> played = List.of(played(PortageVoices.emerge(merges, false, 4, () -> { })),
                played(KernelVoices.make("6.11.5", 4, 1_280, () -> { })),
                played(KernelVoices.genkernel("6.11.5", 1_280, () -> { })),
                played(ArchiveVoice.unpack(true, 200, () -> { })),
                played(PacmanVoices.pacstrap("/mnt", 400, 300, "6.11.5-arch1-1", () -> { })));
        final Pattern foreign = Pattern.compile(
                "\\bgcc\\b|\\bg\\+\\+|\\bglibc\\b|\\bCC\\b|\\bHOSTCC\\b|C compiler|GNU C\\b|[\\w-]\\.[cho]\\b");
        for (final Glass glass : played) {
            for (final String line : glass.everShown) {
                assertFalse(foreign.matcher(line).find(), "this is C: " + line);
            }
        }
        assertTrue(played.get(0).all().contains(".sg") && played.get(0).all().contains("-scc "),
                "what compiles is Sigma, by the Sigma compiler");
    }

    @Test
    void pacstrap_countsTheSameListEverywhereItCountsIt() {
        final Glass glass = played(PacmanVoices.pacstrap("/mnt", 400, 300, "6.11.5-arch1-1", () -> { }));
        inOrder(glass, "==> Creating install root at /mnt", "==> Installing packages to /mnt",
                ":: Synchronizing package databases...", "resolving dependencies...",
                "looking for conflicting packages...", "Packages (48) iana-etc-20240814-1",
                "Total Download Size:", "Total Installed Size:", ":: Proceed with installation? [Y/n]",
                ":: Retrieving packages...", "linux-firmware-2024", "(48/48) checking keys in keyring",
                "(48/48) checking available disk space", ":: Processing package changes...",
                "( 1/48) installing iana-etc", "(48/48) installing base", ":: Running post-transaction hooks...",
                "Updating linux initcpios...", "==> Building image from preset: /etc/mkinitcpio.d/linux.preset: "
                        + "'default'", "  -> Running build hook: [autodetect]", "'fallback'",
                "==> Initcpio image generation successful", "(11/11) Reloading system bus configuration...");
    }

    /** A bar per package that really fills: the big ones were on the glass part way before they were full. */
    @Test
    void pacstrap_theBigPackagesAreWatchedArriving() {
        final Glass glass = played(PacmanVoices.pacstrap("/mnt", 800, 300, "6.11.5-arch1-1", () -> { }));
        assertTrue(glass.everShown.stream().anyMatch(l -> l.contains("linux-firmware") && l.contains(" 50%")),
                "the firmware was half way at some point");
    }

    @Test
    void pacman_insideTheNewSystem_reallyAsks() {
        final TtyScriptProcess tool = new TtyScriptProcess(PacmanVoices.install(
                List.of(new PacmanVoices.Package("grub-2:2.12-3", 7.8)), 20, 20, () -> { }));
        tool.begin(0);
        final Glass glass = new Glass();
        for (long tick = 0; tick < 200 && tool.asking() == null; tick++) {
            tool.advance(tick, glass);
        }
        assertTrue(tool.asking() != null && tool.asking().text().text().contains("Proceed with installation?"),
                glass.all());
    }

    /**
     * No line that redraws itself is wider than the terminal it was laid out for, since one that wrapped would
     * be redrawing two rows. The one exception is the filesystem maker's last count, whose label alone is
     * nearly the width of the glass and is the real tool's word for word; the glass takes a wrapped line away
     * whole when it is drawn over, so that one is safe to let run on.
     */
    @Test
    void everyBarAndCounter_fitsTheTerminal() {
        final List<Glass> all = List.of(
                played(DiskVoices.mke2fs("sda3", REAL_RUN_MB, 1, 200, () -> { })),
                played(FetchVoice.wget("/gentoo/stage3-vel64-openrc.tar.xz", 256, 320, NOON_ON_DAY_214, () -> { })),
                played(PacmanVoices.pacstrap("/mnt", 400, 300, "6.11.5-arch1-1", () -> { })),
                played(KernelVoices.genkernel("6.11.5", 600, () -> { })));
        for (final Glass glass : all) {
            for (final String line : glass.redrawn) {
                final boolean runsOn = line.startsWith("Writing superblocks");
                assertTrue(runsOn || line.length() <= Bars.COLUMNS, line.length() + " columns: " + line);
            }
        }
    }

    /** What a tool was run for is done at its end, so one stopped half way has done nothing. */
    @Test
    void everyTool_doesItsWorkAtTheEndAndNotBefore() {
        final boolean[] done = {false};
        final TtyScriptProcess tool = new TtyScriptProcess(
                DiskVoices.mke2fs("sda3", REAL_RUN_MB, 1, 200, () -> done[0] = true));
        final Glass glass = new Glass();
        tool.begin(0);
        tool.advance(150, glass);
        assertFalse(done[0], "three quarters of the way through, no filesystem has been made");
        tool.advance(10_000, glass);
        assertTrue(done[0]);
    }

    @Test
    void worldStamp_readsTheDayAndTheTimeOffTheWorld() {
        assertEquals("Day 0 06:00:00", WorldStamp.of(0).dated());
        assertEquals("Day 0 12:00:00", WorldStamp.of(6_000).dated());
        assertEquals("Day 1 00:00:00", WorldStamp.of(18_000).dated());
        assertEquals("Day 214 12:00:00", NOON_ON_DAY_214.dated());
        assertEquals("Day 214 12:19:12", NOON_ON_DAY_214.after(320).dated(), "sixteen seconds of play is 19 minutes");
    }

    /** What a terminal would show, kept the way a terminal keeps it, with everything that was ever on it. */
    private static final class Glass implements ITtySink {

        private final List<String> shown = new ArrayList<>();
        private final List<String> everShown = new ArrayList<>();
        private final List<String> redrawn = new ArrayList<>();
        private int redraws;

        @Override
        public void line(final CliLine line) {
            this.shown.add(line.text());
            this.everShown.add(line.text());
        }

        @Override
        public void redraw(final CliLine line) {
            this.redraws++;
            this.shown.set(this.shown.size() - 1, line.text());
            this.everShown.add(line.text());
            this.redrawn.add(line.text());
        }

        private String all() {
            return String.join("\n", this.shown);
        }
    }
}
