/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.computers.guide.ComputersGuide;
import dev.jstech.computers.os.InstallMode;
import dev.jstech.computers.os.Platform;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.guide.TextSize;
import java.util.List;
import org.junit.jupiter.api.Test;

class HelpShelfTest {

    @Test
    void bookFor_givesEachSystemTheBookWrittenForIt() {
        assertEquals(ComputersGuide.FRAMES_RESOURCE_KIT, HelpShelf.bookFor(Platform.MC_DOS, InstallMode.GUIDED));
        assertEquals(ComputersGuide.FRAMES_RESOURCE_KIT, HelpShelf.bookFor(Platform.FRAMES, InstallMode.GUIDED));
        assertEquals(ComputersGuide.LINUX_DESK_REFERENCE, HelpShelf.bookFor(Platform.LINUX, InstallMode.GUIDED));
        assertEquals(ComputersGuide.LINUX_BY_HAND, HelpShelf.bookFor(Platform.LINUX, InstallMode.LIVE_MANUAL));
        assertEquals(ComputersGuide.LINUX_BY_HAND, HelpShelf.bookFor(Platform.LINUX, InstallMode.SOURCE));
        assertEquals(ComputersGuide.DAEMON_HANDBOOK, HelpShelf.bookFor(Platform.FREEBSD, InstallMode.GUIDED));
        assertEquals(ComputersGuide.DAEMON_HANDBOOK, HelpShelf.bookFor(Platform.UNIX, InstallMode.GUIDED));
        assertEquals(ComputersGuide.GUIDE_TO_OPERATIONS, HelpShelf.bookFor(Platform.MC_NET, InstallMode.GUIDED));
    }

    @Test
    void first_putsTheSystemsBookFirstAndKeepsTheOthersInTheirOrder() {
        final List<ManualReader> manuals = List.of(reader("jscore:technical_reference"),
                reader(ComputersGuide.GUIDE_TO_OPERATIONS), reader(ComputersGuide.FRAMES_RESOURCE_KIT));

        assertEquals(List.of(ComputersGuide.FRAMES_RESOURCE_KIT, "jscore:technical_reference",
                        ComputersGuide.GUIDE_TO_OPERATIONS),
                HelpShelf.first(manuals, ComputersGuide.FRAMES_RESOURCE_KIT).stream().map(ManualReader::manualId)
                        .toList());
        assertEquals(manuals, HelpShelf.first(manuals, ComputersGuide.DAEMON_HANDBOOK),
                "a book the game does not hold leaves the order as it was");
    }

    private static ManualReader reader(final String id) {
        return new ManualReader(new GuideManual(id, id, List.of(), "", "", "", List.of(), List.of(), 0, ""),
                new GuideContents(List.of()), new Words());
    }

    /** Words that are their own keys. */
    private record Words() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            return key;
        }

        @Override
        public int width(final String text, final TextSize size) {
            return text.length() * 6;
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return 0;
        }

        @Override
        public String amount(final long value, final String unit) {
            return value + " " + unit;
        }
    }
}
