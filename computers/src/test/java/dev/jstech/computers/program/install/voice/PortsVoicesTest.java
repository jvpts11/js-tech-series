/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.ITtySink;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The ports say what the real ones say, a phase to a line, in the order they say it, and do their work where the
 * real ones do it.
 */
class PortsVoicesTest {

    private static final PortsVoices.Port SCREENFETCH = new PortsVoices.Port("screenfetch-3.9.1", "screenfetch",
            "GPLv3", "screenfetch-3.9.1.tar.gz", 1.0, 60, 100);

    private static final WorldStamp NOON_ON_DAY_214 = WorldStamp.of(214L * 24_000L + 6_000L);

    @Test
    void make_takesThePortThroughEveryPhaseInOrder() {
        final Glass glass = played(PortsVoices.make(SCREENFETCH, () -> { }, () -> { }, () -> { }));
        inOrder(glass, "===>  License GPLv3 accepted by the user",
                "=> screenfetch-3.9.1.tar.gz doesn't seem to exist in /usr/ports/distfiles/.",
                "=> Attempting to fetch from the Mirror.", "screenfetch-3.9.1.tar.gz ",
                "===>  Extracting for screenfetch-3.9.1", "=> SHA256 Checksum OK for screenfetch-3.9.1.tar.gz.",
                "===>  Configuring for screenfetch-3.9.1", "===>  Building for screenfetch-3.9.1",
                "scc -O2 -pipe -march=native -c -o ", "scc -O2 -pipe -march=native -o screenfetch ",
                "===>  Staging for screenfetch-3.9.1", "===>  Installing for screenfetch-3.9.1",
                "===>   Registering installation for screenfetch-3.9.1",
                "Built for this machine: 10% less memory, disk and processor than the package.",
                "===>  Cleaning for screenfetch-3.9.1");
    }

    @Test
    void make_buildsThenInstallsThenCleans_eachOnceTheOneBeforeIsDone() {
        final List<String> done = new ArrayList<>();
        final TtyScriptProcess tool = new TtyScriptProcess(PortsVoices.make(SCREENFETCH, () -> done.add("built"),
                () -> done.add("installed"), () -> done.add("cleaned")));
        final Glass glass = new Glass();
        tool.begin(0);
        tool.advance(100, glass);
        assertTrue(done.isEmpty(), "half way through the build nothing has happened yet");
        tool.advance(100_000, glass);
        assertEquals(List.of("built", "installed", "cleaned"), done);
    }

    @Test
    void make_withTheBuildDone_goesStraightToStaging() {
        final Glass glass = played(PortsVoices.make(SCREENFETCH, null, () -> { }, null));
        assertTrue(glass.shown.getFirst().contains("===>  Staging for screenfetch-3.9.1"), glass.all());
        assertFalse(glass.all().contains("License") || glass.all().contains("Building for")
                || glass.all().contains("Cleaning for"), glass.all());
    }

    @Test
    void make_aPortWithNoLicence_acceptsNone() {
        final PortsVoices.Port unlicensed = new PortsVoices.Port("mines-1.0", "mines", "", "mines-1.0.tar.gz", 4.0,
                60, 100);
        final Glass glass = played(PortsVoices.make(unlicensed, () -> { }, null, null));
        assertFalse(glass.all().contains("License"), glass.all());
        assertTrue(glass.all().contains("===>  Building for mines-1.0"), glass.all());
    }

    @Test
    void fetchThenExtract_laysOutEveryPortAndIndexesThem() {
        final boolean[] tagged = {false};
        final CliLine[] index = {PortsVoices.noSpace()};
        final TtyScript both = TtyScript.script()
                .then(PortsVoices.fetch(24.0, 60, NOON_ON_DAY_214, () -> tagged[0] = true))
                .then(PortsVoices.extract(false, List.of("sysutils/screenfetch", "editors/vim"), 20,
                        () -> index[0] = PortsVoices.indexed(2, "28 MB"), () -> index[0]))
                .done();
        final Glass glass = played(both);
        assertTrue(tagged[0], "the snapshot was kept");
        inOrder(glass, "Looking up the Mirror for the ports tree... found.",
                "Fetching snapshot tag from the Mirror... done.", "Fetching snapshot generated at Day 214 12:00:00:",
                "ports.tar.gz", "Extracting snapshot... done.", "Verifying snapshot integrity... done.",
                "/usr/ports/sysutils/screenfetch/", "/usr/ports/editors/vim/",
                "Building new INDEX files... done. 2 ports, 28 MB on this disk.");
    }

    @Test
    void extract_anUpdate_saysItRemovesTheOldAndNamesOnlyWhatChanged() {
        final Glass glass = played(PortsVoices.extract(true, List.of("editors/vim"), 20, () -> { },
                () -> PortsVoices.indexed(2, "28 MB")));
        inOrder(glass, "Removing old files and directories... done.", "Extracting new files:",
                "/usr/ports/editors/vim/", "Building new INDEX files... done.");
    }

    @Test
    void extract_onAFullDisk_saysSoWhereTheIndexWouldBe() {
        final Glass glass = played(PortsVoices.extract(false, List.of("editors/vim"), 20, () -> { },
                PortsVoices::noSpace));
        assertEquals("/usr/ports: No space left on device", glass.shown.getLast());
    }

    /** No line that redraws itself is wider than the terminal, since one that wrapped would redraw two rows. */
    @Test
    void everyRedrawnLine_fitsTheTerminal() {
        final List<Glass> all = List.of(played(PortsVoices.make(SCREENFETCH, () -> { }, null, null)),
                played(PortsVoices.fetch(12_000.0, 200, NOON_ON_DAY_214, () -> { })));
        for (final Glass glass : all) {
            assertFalse(glass.redrawn.isEmpty(), "something redrew itself");
            for (final String line : glass.redrawn) {
                assertTrue(line.length() <= Bars.COLUMNS, line.length() + " columns: " + line);
            }
        }
    }

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

    /** What a terminal would show, and every line that was drawn over another. */
    private static final class Glass implements ITtySink {

        private final List<String> shown = new ArrayList<>();
        private final List<String> redrawn = new ArrayList<>();

        @Override
        public void line(final CliLine line) {
            this.shown.add(line.text());
        }

        @Override
        public void redraw(final CliLine line) {
            this.shown.set(this.shown.size() - 1, line.text());
            this.redrawn.add(line.text());
        }

        private String all() {
            return String.join("\n", this.shown);
        }
    }
}
