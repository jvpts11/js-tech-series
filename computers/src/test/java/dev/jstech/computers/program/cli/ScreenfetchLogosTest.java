/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ScreenfetchLogosTest {

    private static final List<String> SYSTEMS = List.of("ubuntu", "debian", "fedora", "arch", "gentoo", "freebsd",
            "frames_95", "frames_xp", "frames_11");

    @Test
    void of_everyLogoIsReadOutOfItsColourMarkers() {
        for (final String system : SYSTEMS) {
            for (final List<CliSpan> row : ScreenfetchLogos.of(system).rows()) {
                for (final CliSpan run : row) {
                    assertFalse(run.text().contains("${c"), system + " kept a marker in " + run.text());
                }
            }
        }
    }

    @Test
    void of_freebsdStaysInItsOneRed() {
        final ScreenfetchLogos.Logo freebsd = ScreenfetchLogos.of("freebsd");
        assertTrue(freebsd.oneColour());
        assertEquals(CliStyle.RED, freebsd.title());
        assertEquals(15, freebsd.rows().size());
        assertEquals("   ```                        `", freebsd.rows().getFirst().getFirst().text());
    }

    @Test
    void of_ubuntuHasItsLetteringInWhiteInsideTheRedRing() {
        final ScreenfetchLogos.Logo ubuntu = ScreenfetchLogos.of("ubuntu");
        assertEquals("            .-/+oossssoo+/-.", text(ubuntu.rows().getFirst()));
        assertTrue(ubuntu.rows().get(3).contains(new CliSpan("dMMMNy", CliStyle.BRIGHT)));
        assertTrue(ubuntu.rows().get(3).contains(new CliSpan("sssso.", CliStyle.RED)));
        assertEquals(40, ubuntu.width());
    }

    @Test
    void of_labelsTakeTheSecondColourUnlessItIsWhite() {
        assertEquals(CliStyle.RED, ScreenfetchLogos.of("debian").label());
        assertEquals(CliStyle.CYAN, ScreenfetchLogos.of("arch").label());
        assertEquals(CliStyle.MAGENTA, ScreenfetchLogos.of("gentoo").label());
        assertEquals(CliStyle.RED, ScreenfetchLogos.of("frames_xp").title());
        assertEquals(CliStyle.GREEN, ScreenfetchLogos.of("frames_xp").label());
    }

    @Test
    void of_debianHasNoStrayQuote() {
        assertEquals("  ,g$$P\"     \"\"\"Y$$.\".", text(ScreenfetchLogos.of("debian").rows().get(2)));
    }

    @Test
    void of_aSystemWithNoLogoGetsThePlainMark() {
        assertTrue(ScreenfetchLogos.of("unix").oneColour());
        assertEquals(CliStyle.ACCENT, ScreenfetchLogos.of("unix").title());
    }

    private static String text(final List<CliSpan> row) {
        final StringBuilder out = new StringBuilder();
        for (final CliSpan run : row) {
            out.append(run.text());
        }
        return out.toString();
    }
}
