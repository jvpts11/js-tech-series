/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.flex;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlexLayoutTest {

    /* A font whose every letter is 6 pixels wide and whose lines are 9 tall, as the game's is near enough. */
    private static final TextMeasure TEXT = new TextMeasure(String::length, 9, 1f, true);
    private static final TextMeasure WIDE_TEXT = new TextMeasure(text -> text.length() * 6, 9, 1f, true);

    @Test
    void layout_rowPlacesBoxesOneAfterTheOtherWithTheGap() {
        final FlexBox row = FlexBox.row("row").gap(4)
                .add(block("a", 10, 10)).add(block("b", 20, 10)).add(block("c", 5, 10));

        final Placed placed = FlexLayout.layout(row, 100, 10);

        assertEquals(0, at(placed, "a").x());
        assertEquals(14, at(placed, "b").x());
        assertEquals(38, at(placed, "c").x());
    }

    @Test
    void layout_columnPlacesBoxesOneUnderTheOther() {
        final FlexBox column = FlexBox.column("column").gap(2).padding(Insets.of(3))
                .add(block("a", 10, 10)).add(block("b", 10, 7));

        final Placed placed = FlexLayout.layout(column, 50, 50);

        assertEquals(3, at(placed, "a").y());
        assertEquals(15, at(placed, "b").y());
        assertEquals(3, at(placed, "b").x());
    }

    @Test
    void layout_growSharesTheRoomLeftByShares() {
        final FlexBox row = FlexBox.row("row")
                .add(block("fixed", 10, 10)).add(block("one", 0, 10).grow(1)).add(block("two", 0, 10).grow(2));

        final Placed placed = FlexLayout.layout(row, 100, 10);

        assertEquals(30, at(placed, "one").width());
        assertEquals(60, at(placed, "two").width());
        assertEquals(100, at(placed, "two").right());
    }

    @Test
    void layout_growStopsAtTheLargestSizeAndHandsTheRestOn() {
        final FlexBox row = FlexBox.row("row")
                .add(block("capped", 0, 10).grow(1).maxWidth(10)).add(block("open", 0, 10).grow(1));

        final Placed placed = FlexLayout.layout(row, 100, 10);

        assertEquals(10, at(placed, "capped").width());
        assertEquals(90, at(placed, "open").width());
    }

    @Test
    void layout_shrinkGivesRoomBackDownToTheSmallestSize() {
        final FlexBox row = FlexBox.row("row")
                .add(leaf("a", 60, 10).minWidth(50)).add(leaf("b", 60, 10));

        final Placed placed = FlexLayout.layout(row, 100, 10);

        assertEquals(100, at(placed, "a").width() + at(placed, "b").width());
        assertTrue(at(placed, "a").width() >= 50);
        assertFalse(placed.overflowing());
    }

    @Test
    void layout_marksARowThatCannotFitEvenAtItsSmallest() {
        final FlexBox row = FlexBox.row("row")
                .add(block("a", 60, 10).shrink(0)).add(block("b", 60, 10).shrink(0));

        final Placed placed = FlexLayout.layout(row, 100, 10);

        assertEquals(List.of("row"), placed.overflows());
    }

    @Test
    void layout_justifyPutsTheRoomLeftWhereItSays() {
        assertEquals(80, at(justified(Justify.END), "a").x());
        assertEquals(40, at(justified(Justify.CENTER), "a").x());
        assertEquals(90, at(justified(Justify.SPACE_BETWEEN), "b").x());
    }

    @Test
    void layout_alignPlacesBoxesAcrossTheRow() {
        final FlexBox row = FlexBox.row("row").align(Align.CENTER)
                .add(leaf("small", 10, 4)).add(leaf("stretched", 10, 4).alignSelf(Align.STRETCH));

        final Placed placed = FlexLayout.layout(row, 40, 20);

        assertEquals(8, at(placed, "small").y());
        assertEquals(4, at(placed, "small").height());
        assertEquals(20, at(placed, "stretched").height());
    }

    @Test
    void layout_marginKeepsRoomAroundABox() {
        final FlexBox row = FlexBox.row("row").align(Align.START)
                .add(block("a", 10, 10).margin(new Insets(2, 3, 0, 5))).add(block("b", 10, 10));

        final Placed placed = FlexLayout.layout(row, 100, 20);

        assertEquals(5, at(placed, "a").x());
        assertEquals(2, at(placed, "a").y());
        assertEquals(18, at(placed, "b").x());
    }

    @Test
    void layout_wrappingRowGoesOnInANewLine() {
        final FlexBox row = FlexBox.row("row").wrap(true).gap(2).align(Align.START)
                .add(block("a", 40, 10)).add(block("b", 40, 10)).add(block("c", 40, 10));

        final Placed placed = FlexLayout.layout(row, 90, 30);

        assertEquals(0, at(placed, "c").x());
        assertEquals(12, at(placed, "c").y());
        assertFalse(placed.overflowing());
    }

    @Test
    void layout_gridSharesFractionsAfterFixedAndAutoColumns() {
        final FlexBox grid = FlexBox.grid("grid", List.of(Track.fixed(20), Track.auto(), Track.fraction(1),
                        Track.fraction(3)))
                .add(block("a", 0, 10)).add(leaf("b", 15, 10)).add(block("c", 0, 10)).add(block("d", 0, 10));

        final Placed placed = FlexLayout.layout(grid, 115, 10);

        assertEquals(20, at(placed, "a").width());
        assertEquals(15, at(placed, "b").width());
        assertEquals(20, at(placed, "c").width());
        assertEquals(60, at(placed, "d").width());
        assertEquals(55, at(placed, "d").x());
    }

    @Test
    void layout_gridGoesToTheNextRowAndHonoursSpans() {
        final FlexBox grid = FlexBox.grid("grid", List.of(Track.fixed(18), Track.fixed(18), Track.fixed(18))).gap(0)
                .add(block("wide", 0, 18).span(2)).add(block("x", 0, 18)).add(block("y", 0, 18));

        final Placed placed = FlexLayout.layout(grid, 54, 36);

        assertEquals(36, at(placed, "wide").width());
        assertEquals(36, at(placed, "x").x());
        assertEquals(18, at(placed, "y").y());
    }

    @Test
    void layout_stackLaysBoxesOverEachOther() {
        final FlexBox stack = FlexBox.stack("stack").add(block("ground", 0, 0)).add(leaf("mark", 4, 4)
                .alignSelf(Align.CENTER));

        final Placed placed = FlexLayout.layout(stack, 20, 20);

        assertEquals(20, at(placed, "ground").width());
        assertEquals(8, at(placed, "mark").x());
    }

    @Test
    void preferred_textWrapsToTheRoomItGets() {
        final FlexBox label = FlexBox.leaf("label", WIDE_TEXT.of("one two three"));

        assertEquals(new Size(78, 8), FlexLayout.preferred(label, 200));
        assertEquals(new Size(30, 26), FlexLayout.preferred(label, 30));
    }

    @Test
    void preferred_columnIsAsTallAsWhatItHolds() {
        final FlexBox column = FlexBox.column("column").gap(3).padding(Insets.of(2))
                .add(block("a", 10, 10)).add(block("b", 30, 5));

        assertEquals(new Size(34, 22), FlexLayout.preferred(column, 300));
    }

    @Test
    void lines_breakAtWordsAndKeepALongWordWhole() {
        assertEquals(List.of("aa bb", "cccccccc", "d"), TEXT.lines("aa bb cccccccc d", 5));
    }

    @Test
    void share_cutsTheWholeByLargestRemainder() {
        assertArrayEquals(new int[] {4, 3, 3}, FlexLayout.share(10, new int[] {1, 1, 1}));
        assertArrayEquals(new int[] {0, 0}, FlexLayout.share(10, new int[] {0, 0}));
    }

    @Test
    void guiLayout_checksTheSolidsAndTextsOfTheLaidOutScreen() {
        final FlexBox clean = FlexBox.column("root").gap(2)
                .add(block("a", 10, 10).role(Role.SOLID)).add(block("b", 10, 10).role(Role.SOLID));
        final FlexBox overlapping = FlexBox.stack("root")
                .add(block("a", 10, 10).role(Role.SOLID)).add(block("b", 10, 10).role(Role.SOLID));

        final GuiLayout good = FlexLayout.layout(clean, 40, 40).guiLayout(40, 40);
        final GuiLayout bad = FlexLayout.layout(overlapping, 40, 40).guiLayout(40, 40);

        assertTrue(good.isClean());
        assertEquals(List.of("a x b"), bad.overlaps());
    }

    @Test
    void add_refusesAChildInALeaf() {
        assertThrows(IllegalStateException.class, () -> leaf("leaf", 1, 1).add(leaf("inner", 1, 1)));
    }

    private static Placed justified(final Justify justify) {
        return FlexLayout.layout(FlexBox.row("row").justify(justify)
                .add(block("a", 10, 10)).add(block("b", 10, 10)), 100, 10);
    }

    /* A box of a given size; a size of 0 leaves that side to the layout. */
    private static FlexBox block(final String name, final int width, final int height) {
        final FlexBox box = FlexBox.leaf(name, IMeasure.NOTHING);
        if (width > 0) {
            box.width(width);
        }
        if (height > 0) {
            box.height(height);
        }
        return box;
    }

    /* A box as big as what it shows says. */
    private static FlexBox leaf(final String name, final int width, final int height) {
        return FlexBox.leaf(name, IMeasure.fixed(width, height));
    }

    private static Placed at(final Placed root, final String name) {
        return root.find(name).orElseThrow();
    }
}
