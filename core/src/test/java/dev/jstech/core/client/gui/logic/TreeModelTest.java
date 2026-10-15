/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TreeModelTest {

    @Test
    void rows_showTheChildrenOfOpenNodesWithTheirGuides() {
        final TreeModel tree = new TreeModel(List.of(
                new TreeModel.Node("line", "Ore line", -1, List.of(
                        new TreeModel.Node("crush", "Crushing", -1, List.of(TreeModel.Node.leaf("c1", "Crusher #1"))),
                        TreeModel.Node.leaf("smelt", "Smelting"))),
                TreeModel.Node.leaf("power", "Power")));

        assertEquals(2, tree.rows().size());
        tree.toggle("line");
        tree.open("crush");
        final List<TreeModel.Row> rows = tree.rows();
        assertEquals(List.of("line", "crush", "c1", "smelt", "power"), rows.stream().map(r -> r.node().id()).toList());
        assertEquals(2, rows.get(2).depth());
        assertEquals(List.of(true, true), rows.get(2).runOn());
        assertTrue(rows.get(3).last());
    }

    @Test
    void toggle_closesAnOpenNode() {
        final TreeModel tree = new TreeModel(List.of(new TreeModel.Node("a", "A", 3,
                List.of(TreeModel.Node.leaf("b", "B")))));

        tree.toggle("a");
        assertEquals(2, tree.rows().size());
        tree.toggle("a");
        assertEquals(1, tree.rows().size());
    }
}
