/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermBufferTest {

    private static final int COLUMNS = 20;

    private TermBuffer glass;

    @BeforeEach
    void setUp() {
        this.glass = new TermBuffer(4, COLUMNS);
    }

    private List<String> shown() {
        return this.glass.rows().stream().map(TermRow::text).toList();
    }

    @Test
    void push_aLineThatFits_isOneRow() {
        this.glass.push(CliLine.plain("mount /dev/sda /mnt"));
        assertEquals(List.of("mount /dev/sda /mnt"), shown());
    }

    @Test
    void push_aLongLine_breaksAtASpaceNearTheEdge() {
        this.glass.push(CliLine.plain("Writing inode tables: done now"));
        assertEquals(List.of("Writing inode ", "tables: done now"), shown());
    }

    /** A path is one word, so it is cut where the row ends: there is nowhere better to cut it. */
    @Test
    void push_oneLongWord_isCutWhereTheRowEnds() {
        this.glass.push(CliLine.plain("/var/tmp/portage/sys-kernel/gentoo-sources"));
        assertEquals(List.of("/var/tmp/portage/sys", "-kernel/gentoo-sourc", "es"), shown());
    }

    @Test
    void push_noRowIsEverWiderThanTheGlass() {
        this.glass.push(CliLine.plain("x".repeat(95) + " and then some more words after it all"));
        for (final TermRow row : this.glass.rows()) {
            assertTrue(row.length() <= COLUMNS, row.text());
        }
    }

    /** The colours of a line survive being wrapped, each row keeping the runs that fell in it. */
    @Test
    void push_aLineInSeveralColours_keepsEachColourWhereItFell() {
        this.glass.push(CliLine.of(new CliSpan(">>> ", CliStyle.OK), new CliSpan("Emerging (1 of 3) vim", CliStyle.PLAIN)));
        final List<TermRow> rows = this.glass.rows();
        assertEquals(2, rows.size());
        assertEquals(CliStyle.OK, rows.get(0).runs().get(0).style());
        assertEquals(">>> ", rows.get(0).runs().get(0).text());
        assertEquals(CliStyle.PLAIN, rows.get(1).runs().get(0).style());
    }

    /** Two runs of the same colour side by side come out as one, so the glass draws them in one go. */
    @Test
    void push_neighbouringRunsOfOneColour_areJoined() {
        this.glass.push(CliLine.of(new CliSpan("ab", CliStyle.OK), new CliSpan("cd", CliStyle.OK)));
        assertEquals(1, this.glass.rows().get(0).runs().size());
        assertEquals("abcd", this.glass.rows().get(0).text());
    }

    /** A tab runs to the next stop of eight, which is what the tools that indent with one are counting on. */
    @Test
    void push_aTab_opensOutToTheNextStop() {
        this.glass.push(CliLine.plain("\t32768"));
        assertEquals(List.of("        32768"), shown());
        this.glass.clear();
        this.glass.push(CliLine.plain("ab\tc"));
        assertEquals(List.of("ab      c"), shown());
    }

    @Test
    void push_pastTheMostItKeeps_theOldestLineGoesWithAllItsRows() {
        this.glass.push(CliLine.plain("a long first line that takes two rows"));
        this.glass.push(CliLine.plain("two"));
        this.glass.push(CliLine.plain("three"));
        this.glass.push(CliLine.plain("four"));
        this.glass.push(CliLine.plain("five"));
        assertEquals(List.of("two", "three", "four", "five"), shown());
    }

    /** A bar grows where it stands: the line it redraws is replaced, not followed. */
    @Test
    void replaceLast_drawsOverTheLastLine() {
        this.glass.push(CliLine.plain("stage3  37%[===>  ]"));
        this.glass.replaceLast(CliLine.plain("stage3  38%[===>  ]"));
        assertEquals(List.of("stage3  38%[===>  ]"), shown());
    }

    /** However many rows the old line took, all of them go: a long line redrawn shorter leaves nothing behind. */
    @Test
    void replaceLast_aLineThatHadWrapped_takesEveryRowOfItAway() {
        this.glass.push(CliLine.plain("before"));
        this.glass.push(CliLine.plain("a line so long that it has to take several rows of the glass"));
        assertTrue(this.glass.rows().size() > 3, "the long line wrapped: " + shown());
        this.glass.replaceLast(CliLine.plain("short"));
        assertEquals(List.of("before", "short"), shown());
    }

    @Test
    void replaceLast_withNothingOnTheGlass_isSimplyTheFirstLine() {
        this.glass.replaceLast(CliLine.plain("first"));
        assertEquals(List.of("first"), shown());
    }

    /** A window dragged wider wraps everything again at the new width, from the lines as they arrived. */
    @Test
    void setColumns_wrapsEverythingAgain() {
        this.glass.push(CliLine.plain("Writing inode tables: done now"));
        assertEquals(2, this.glass.rows().size());
        this.glass.setColumns(40);
        assertEquals(List.of("Writing inode tables: done now"), shown());
        this.glass.setColumns(COLUMNS);
        assertEquals(List.of("Writing inode ", "tables: done now"), shown());
    }

    @Test
    void generation_changesWheneverTheGlassDoes() {
        final int before = this.glass.generation();
        this.glass.push(CliLine.plain("x"));
        final int pushed = this.glass.generation();
        assertTrue(pushed != before);
        this.glass.setColumns(COLUMNS);
        assertEquals(pushed, this.glass.generation(), "asking for the width it has changes nothing");
        this.glass.clear();
        assertTrue(this.glass.generation() != pushed);
    }

    @Test
    void lines_giveBackWhatArrivedRatherThanTheRows() {
        final CliLine said = CliLine.plain("a line so long that it has to take three rows of the glass");
        this.glass.push(said);
        assertEquals(List.of(said), this.glass.lines());
    }
}
