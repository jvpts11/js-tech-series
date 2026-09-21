/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InteracScreenTest {

    private static final int WIDE = 80;
    private static final int TALL = 19;

    /** The view as it opens, on the glass these tests read. */
    private static final InteracState OPENING = InteracState.OPENING.on(WIDE, TALL);

    private static InteracScreen.Data data(final int rows) {
        final List<InteracScreen.Row> list = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            list.add(new InteracScreen.Row("Item " + i, String.valueOf(i * 10), "storage-1"));
        }
        return new InteracScreen.Data("4f1c9b2e  Mainframe ok  2 ops", list,
                List.of("Cobblestone", "minecraft:cobblestone", "", "storage-1   8,000"), "", "");
    }

    private static boolean says(final List<String> screen, final String text) {
        return screen.stream().anyMatch(line -> line.contains(text));
    }

    @Test
    void render_drawsEveryRowToTheWidthItWasGiven() {
        final List<String> screen = InteracScreen.render(OPENING, data(4));

        for (final String line : screen) {
            assertEquals(WIDE, line.length(), "a row is the width of the glass: [" + line + "]");
        }
    }

    @Test
    void render_drawsAsManyRowsAsTheGlassHolds() {
        assertEquals(TALL, InteracScreen.render(OPENING, data(4)).size());
        assertEquals(30, InteracScreen.render(OPENING.on(WIDE, 30), data(4)).size(),
                "a taller glass shows more of the list, not a taller list");
        assertEquals(InteracScreen.HEAD_ROWS + InteracScreen.LEAST_BODY + InteracScreen.FOOT_ROWS,
                InteracScreen.render(OPENING.on(WIDE, 6), data(4)).size(),
                "and a glass too short for a list still gets the few rows that make one");
    }

    @Test
    void render_hasTheBarTheTabsTheListAndTheKeys() {
        final List<String> screen = InteracScreen.render(OPENING, data(4));

        assertTrue(says(screen, "Network Interactor"), "the bar names the program");
        assertTrue(says(screen, "Mainframe ok"), "and says how the network is");
        assertTrue(says(screen, "[Network]"), "the tab that is up is marked");
        assertTrue(says(screen, "Starred"), "and the others are there to go to");
        assertTrue(says(screen, "Item 0") && says(screen, "storage-1"), "the rows are drawn");
        assertTrue(says(screen, "minecraft:cobblestone"), "the panel beside them says what is picked");
        assertTrue(says(screen, "2Get") && says(screen, "10Quit"), "and the keys are along the foot");
    }

    @Test
    void render_marksThePickedRow() {
        final List<String> screen = InteracScreen.render(OPENING.picking(2), data(4));
        final String picked = screen.stream().filter(line -> line.contains("Item 2")).findFirst().orElse("");

        assertTrue(picked.startsWith(">"), "the picked row is marked: [" + picked + "]");
        assertFalse(screen.stream().filter(line -> line.contains("Item 1")).findFirst().orElse("")
                .startsWith(">"), "and no other row is");
    }

    @Test
    void render_scrollsSoThePickedRowIsAlwaysOnTheGlass() {
        final List<String> screen = InteracScreen.render(OPENING.picking(40), data(60));

        assertTrue(says(screen, "Item 40"), "the picked row is shown even far down a long list");
        assertFalse(says(screen, "Item 0"), "and the top of the list has scrolled away");
    }

    @Test
    void render_putsAQuestionWhereTheMessageGoes() {
        final InteracScreen.Data asking = new InteracScreen.Data("net", data(2).rows(), List.of("x"),
                "42 Cobblestone queued", "Get how many Cobblestone? 42_");
        final List<String> screen = InteracScreen.render(OPENING, asking);

        assertTrue(says(screen, "Get how many Cobblestone? 42_"), "the question stands at the foot");
        assertFalse(says(screen, "42 Cobblestone queued"), "and takes the place of what was said before");
    }

    @Test
    void render_showsTheSearchBeingTyped() {
        final List<String> screen = InteracScreen.render(OPENING.searchingFor("stone"), data(3));

        assertTrue(says(screen, "Search: stone_"), "what is being looked for is on the glass");
    }

    @Test
    void rowsSaid_readsTheCountBackOffAScreenThatWasDrawn() {
        assertEquals(4, InteracScreen.rowsSaid(InteracScreen.render(OPENING, data(4))));
        assertEquals(1, InteracScreen.rowsSaid(InteracScreen.render(OPENING, data(1))),
                "one row is a row, not rows");
        assertEquals(0, InteracScreen.rowsSaid(InteracScreen.render(OPENING, data(0))));
        assertEquals(0, InteracScreen.rowsSaid(List.of()), "and nothing drawn says nothing");
    }

    @Test
    void rowsSaid_holdsUpWhenTheSearchItselfHasSpacesInIt() {
        final List<String> screen = InteracScreen.render(OPENING.searchingFor("oak log"), data(2));

        assertEquals(2, InteracScreen.rowsSaid(screen));
    }

    @Test
    void tabAt_findsTheHeadingAColumnFallsOn() {
        assertEquals(0, InteracScreen.tabAt(2), "a column on the first heading is the first heading");
        assertEquals(InteracState.TABS.length - 1,
                InteracScreen.tabAt(InteracScreen.render(OPENING, data(1))
                        .get(InteracScreen.TAB_ROW).indexOf("Starred")),
                "and the last is where it is drawn");
        assertEquals(-1, InteracScreen.tabAt(WIDE - 1), "past them all there is no heading");
    }

    @Test
    void keyAt_findsTheKeyAlongTheFootAColumnFallsOn() {
        final String foot = InteracScreen.render(OPENING, data(1))
                .get(InteracScreen.screenRows(OPENING) - 1);

        assertEquals(0, InteracScreen.keyAt(foot.indexOf("1Help")));
        assertEquals(1, InteracScreen.keyAt(foot.indexOf("2Get")));
        assertEquals(InteracScreen.KEYS.length - 1, InteracScreen.keyAt(foot.indexOf("10Quit")));
        assertEquals(-1, InteracScreen.keyAt(WIDE - 1), "past them all there is no key");
    }

    @Test
    void render_holdsUpOnANarrowGlassToo() {
        for (final String line : InteracScreen.render(OPENING.on(40, TALL), data(3))) {
            assertEquals(40, line.length(), "[" + line + "]");
        }
    }
}
