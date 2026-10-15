/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GraphModelTest {

    private static final GraphModel.PortRef ORES = new GraphModel.PortRef("store", "ores", true);
    private static final GraphModel.PortRef IN = new GraphModel.PortRef("crusher", "in", false);
    private static final GraphModel.PortRef POWER = new GraphModel.PortRef("crusher", "power", false);

    @Test
    void connect_joinsOnlyAnOutputToAnInputOfTheSameKind() {
        final GraphModel graph = graph();

        assertFalse(graph.canConnect(ORES, POWER));
        assertFalse(graph.canConnect(IN, ORES));
        assertTrue(graph.connect(ORES, IN));
        assertEquals(1, graph.wires().size());
    }

    @Test
    void portAt_findsAPortByWhereItIsDrawn() {
        final GraphModel graph = graph();

        assertEquals(IN, graph.portAt(100, graph.node("crusher").orElseThrow().portY(0), 3).orElseThrow());
        assertEquals(ORES, graph.portAt(60, graph.node("store").orElseThrow().portY(0), 3).orElseThrow());
    }

    @Test
    void remove_takesTheNodesWiresWithIt() {
        final GraphModel graph = graph();
        final int[] changes = new int[1];
        graph.onChange(g -> changes[0]++);
        graph.connect(ORES, IN);

        graph.remove("store");

        assertTrue(graph.wires().isEmpty());
        assertEquals(2, changes[0]);
    }

    private static GraphModel graph() {
        return new GraphModel()
                .put(new GraphModel.Node("store", "Storage", 0, 0, 60, List.of(),
                        List.of(new GraphModel.Port("ores", "Ores", "items"))))
                .put(new GraphModel.Node("crusher", "Crusher", 100, 0, 60,
                        List.of(new GraphModel.Port("in", "Ores", "items"), new GraphModel.Port("power", "Power",
                                "energy")), List.of()));
    }
}
