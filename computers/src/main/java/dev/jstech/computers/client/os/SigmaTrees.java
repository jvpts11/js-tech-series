/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.UiWindowPayload;
import java.util.ArrayList;
import java.util.List;

/**
 * The rows a tree shows: every node in the order it was added under its parent, depth first, leaving out what lies
 * under a node the player folded. A node's parent comes over as its number, written beside it.
 */
final class SigmaTrees {

    /** How deep a tree is walked; a parent that loops back on itself cannot be added, so this is only a guard. */
    private static final int MOST_DEPTH = 32;

    private SigmaTrees() {
    }

    /**
     * One row of a tree as it is shown.
     *
     * @param node     the node's number, counted from one
     * @param depth    how deep it lies, zero at the top
     * @param children whether any node hangs from it
     */
    record Row(int node, int depth, boolean children) {
    }

    /** The rows shown, top to bottom; worked out in one pass over the nodes. */
    static List<Row> shown(final UiWindowPayload.Widget tree, final SigmaUiState ui) {
        final int count = tree.rows().size();
        final List<List<Integer>> under = new ArrayList<>(count + 1);
        for (int i = 0; i <= count; i++) {
            under.add(new ArrayList<>());
        }
        for (int node = 1; node <= count; node++) {
            final int parent = parentOf(tree, node);
            under.get(parent >= 0 && parent < node ? parent : 0).add(node);
        }
        final List<Row> rows = new ArrayList<>();
        walk(tree.id(), under, ui, 0, 0, rows);
        return rows;
    }

    private static void walk(final long tree, final List<List<Integer>> under, final SigmaUiState ui,
                             final int parent, final int depth, final List<Row> into) {
        if (depth > MOST_DEPTH) {
            return;
        }
        for (final int node : under.get(parent)) {
            final boolean children = !under.get(node).isEmpty();
            into.add(new Row(node, depth, children));
            if (children && !ui.isFolded(tree, node)) {
                walk(tree, under, ui, node, depth + 1, into);
            }
        }
    }

    private static int parentOf(final UiWindowPayload.Widget tree, final int node) {
        try {
            return node - 1 < tree.details().size() ? Integer.parseInt(tree.details().get(node - 1)) : 0;
        } catch (final NumberFormatException unreadable) {
            return 0;
        }
    }
}
