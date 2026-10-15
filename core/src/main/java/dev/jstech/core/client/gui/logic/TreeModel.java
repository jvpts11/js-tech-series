/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * What a tree shows: its nodes, which of them are open, and the rows that makes, each with how deep it is and which
 * of the guides beside it run on down, so a tree draws its dotted lines without working them out again.
 */
public final class TreeModel {

    private final List<Node> roots;
    private final Set<String> open = new HashSet<>();
    private String selected = "";

    public TreeModel(final List<Node> roots) {
        this.roots = List.copyOf(roots);
    }

    /** Opens a node that is closed, and closes one that is open. */
    public void toggle(final String id) {
        if (!open.remove(id)) {
            open.add(id);
        }
    }

    /** Opens the node {@code id}. */
    public void open(final String id) {
        open.add(id);
    }

    public boolean isOpen(final String id) {
        return open.contains(id);
    }

    /** Chooses the node {@code id}. */
    public void select(final String id) {
        selected = Objects.requireNonNull(id, "id");
    }

    /** The chosen node's id, or empty. */
    public String selected() {
        return selected;
    }

    /** The rows shown: every root, and the children of each open node under it, depth first. */
    public List<Row> rows() {
        final List<Row> out = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            walk(roots.get(i), 0, i == roots.size() - 1, new ArrayList<>(), out);
        }
        return out;
    }

    private void walk(final Node node, final int depth, final boolean last, final List<Boolean> runOn,
                      final List<Row> out) {
        out.add(new Row(node, depth, last, List.copyOf(runOn)));
        if (node.children().isEmpty() || !open.contains(node.id())) {
            return;
        }
        final List<Boolean> deeper = new ArrayList<>(runOn);
        deeper.add(!last);
        for (int i = 0; i < node.children().size(); i++) {
            walk(node.children().get(i), depth + 1, i == node.children().size() - 1, deeper, out);
        }
    }

    /**
     * A node of the tree.
     *
     * @param id       what it is found by
     * @param label    what it says
     * @param count    a number shown at the right, or -1 for none
     * @param children what it holds
     */
    public record Node(String id, String label, int count, List<Node> children) {

        public Node {
            children = List.copyOf(children);
        }

        /** A node with nothing under it. */
        public static Node leaf(final String id, final String label) {
            return new Node(id, label, -1, List.of());
        }
    }

    /**
     * A row of the tree as drawn.
     *
     * @param node   its node
     * @param depth  how deep it is, 0 for a root
     * @param last   whether it is the last of its parent's
     * @param runOn  for each level above it, whether that level's guide runs on past it
     */
    public record Row(Node node, int depth, boolean last, List<Boolean> runOn) {

        public Row {
            runOn = List.copyOf(runOn);
        }
    }
}
