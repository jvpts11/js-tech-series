/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NextgreStudioLayoutTest {

    @Test
    void layout_isCleanAtEverySize() {
        assertTrue(NextgreStudioLayout.layout().isClean(), () -> NextgreStudioLayout.layout().overlaps().toString());
        assertTrue(NextgreStudioLayout.layout(NextgreStudioLayout.MIN_W, NextgreStudioLayout.MIN_H).isClean());
        assertTrue(NextgreStudioLayout.layout(640, 400).isClean());
    }

    @Test
    void tree_putsAParentOverTheMiddleOfItsChildren() {
        final List<int[]> boxes = NextgreStudioLayout.tree(List.of(-1, 0, 0, 0));

        assertEquals((boxes.get(1)[0] + boxes.get(3)[0]) / 2, boxes.get(0)[0]);
        assertTrue(boxes.get(1)[1] > boxes.get(0)[1]);
    }

    @Test
    void tree_neverPutsTwoBoxesOfOneRowOverEachOther() {
        // A root with a leaf, a step of three parts, a leaf, and a step of one part with one of its own.
        final List<Integer> parents = List.of(-1, 0, 0, 2, 2, 2, 0, 0, 7, 8);
        final List<int[]> boxes = NextgreStudioLayout.tree(parents);

        for (int a = 0; a < boxes.size(); a++) {
            for (int b = a + 1; b < boxes.size(); b++) {
                if (boxes.get(a)[1] == boxes.get(b)[1]) {
                    final int gap = Math.abs(boxes.get(a)[0] - boxes.get(b)[0]);
                    assertTrue(gap >= NextgreStudioLayout.BOX_W + NextgreStudioLayout.BOX_GAP,
                            "boxes " + a + " and " + b + " are " + gap + " apart");
                }
            }
        }
    }

    @Test
    void treeSize_coversEveryBoxWithItsMargin() {
        final List<int[]> boxes = NextgreStudioLayout.tree(List.of(-1, 0, 0));
        final int[] size = NextgreStudioLayout.treeSize(boxes);

        assertEquals(boxes.get(2)[0] + NextgreStudioLayout.BOX_W + NextgreStudioLayout.TREE_PAD, size[0]);
        assertEquals(boxes.get(1)[1] + NextgreStudioLayout.BOX_H + NextgreStudioLayout.TREE_PAD, size[1]);
    }

    @Test
    void tree_ofOneBoxSitsAtTheMargin() {
        final List<int[]> boxes = NextgreStudioLayout.tree(List.of(-1));

        assertEquals(NextgreStudioLayout.TREE_PAD, boxes.get(0)[0]);
        assertEquals(NextgreStudioLayout.TREE_PAD, boxes.get(0)[1]);
    }
}
