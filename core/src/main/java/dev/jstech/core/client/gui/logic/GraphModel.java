/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * What a node graph editor edits: nodes with their ports, and the wires from an output to an input. A wire joins two
 * ports of the same kind (items to items, energy to energy), of two nodes, and an input takes one wire; joining an
 * input that has one moves it. The owner hears every change, so the graph it stands for follows.
 */
public final class GraphModel {

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final List<Wire> wires = new ArrayList<>();
    private Consumer<GraphModel> onChange = graph -> {
    };

    /** How tall a node's title is, and how far apart its ports are. */
    public static final int TITLE = 16;
    public static final int PORT_PITCH = 12;

    /** Adds {@code node}, or puts it in the place of the node of its id. */
    public GraphModel put(final Node node) {
        nodes.put(node.id(), node);
        changed();
        return this;
    }

    /** Takes away the node {@code id} and its wires. */
    public void remove(final String id) {
        if (nodes.remove(id) != null) {
            wires.removeIf(wire -> wire.fromNode().equals(id) || wire.toNode().equals(id));
            changed();
        }
    }

    /** Moves the node {@code id} to {@code (x, y)}. */
    public void move(final String id, final int x, final int y) {
        final Node node = nodes.get(id);
        if (node != null) {
            nodes.put(id, new Node(id, node.title(), x, y, node.width(), node.inputs(), node.outputs()));
            changed();
        }
    }

    /** Whether an output and an input may be joined: of two nodes, and of the same kind. */
    public boolean canConnect(final PortRef output, final PortRef input) {
        final Optional<Port> from = port(output, true);
        final Optional<Port> to = port(input, false);
        return from.isPresent() && to.isPresent() && !output.node().equals(input.node())
                && from.get().kind().equals(to.get().kind());
    }

    /** Joins an output to an input, moving a wire the input had; nothing when they cannot be joined. */
    public boolean connect(final PortRef output, final PortRef input) {
        if (!canConnect(output, input)) {
            return false;
        }
        wires.removeIf(wire -> wire.to().equals(input));
        wires.add(new Wire(output, input));
        changed();
        return true;
    }

    /** Takes away the wire into {@code input}. */
    public void disconnect(final PortRef input) {
        if (wires.removeIf(wire -> wire.to().equals(input))) {
            changed();
        }
    }

    /** Runs {@code action} after every change. */
    public void onChange(final Consumer<GraphModel> action) {
        onChange = Objects.requireNonNull(action, "action");
    }

    public List<Node> nodes() {
        return List.copyOf(nodes.values());
    }

    public Optional<Node> node(final String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public List<Wire> wires() {
        return List.copyOf(wires);
    }

    /** The node whose box holds {@code (x, y)}, the last drawn first, or none. */
    public Optional<Node> nodeAt(final int x, final int y) {
        final List<Node> all = nodes();
        for (int i = all.size() - 1; i >= 0; i--) {
            final Node node = all.get(i);
            if (x >= node.x() && x < node.x() + node.width() && y >= node.y() && y < node.y() + node.height()) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    /** The port within {@code reach} pixels of {@code (x, y)}, or none. */
    public Optional<PortRef> portAt(final int x, final int y, final int reach) {
        for (final Node node : nodes.values()) {
            for (int i = 0; i < node.inputs().size(); i++) {
                if (near(x, y, node.x(), node.portY(i), reach)) {
                    return Optional.of(new PortRef(node.id(), node.inputs().get(i).id(), false));
                }
            }
            for (int i = 0; i < node.outputs().size(); i++) {
                if (near(x, y, node.x() + node.width(), node.portY(i), reach)) {
                    return Optional.of(new PortRef(node.id(), node.outputs().get(i).id(), true));
                }
            }
        }
        return Optional.empty();
    }

    /** Where the port {@code ref} is drawn, {@code {x, y}}, or none when there is no such port. */
    public Optional<int[]> where(final PortRef ref) {
        final Node node = nodes.get(ref.node());
        if (node == null) {
            return Optional.empty();
        }
        final List<Port> ports = ref.output() ? node.outputs() : node.inputs();
        for (int i = 0; i < ports.size(); i++) {
            if (ports.get(i).id().equals(ref.port())) {
                return Optional.of(new int[] {ref.output() ? node.x() + node.width() : node.x(), node.portY(i)});
            }
        }
        return Optional.empty();
    }

    private Optional<Port> port(final PortRef ref, final boolean output) {
        if (ref.output() != output) {
            return Optional.empty();
        }
        final Node node = nodes.get(ref.node());
        if (node == null) {
            return Optional.empty();
        }
        for (final Port port : output ? node.outputs() : node.inputs()) {
            if (port.id().equals(ref.port())) {
                return Optional.of(port);
            }
        }
        return Optional.empty();
    }

    private void changed() {
        onChange.accept(this);
    }

    private static boolean near(final int x, final int y, final int px, final int py, final int reach) {
        return Math.abs(x - px) <= reach && Math.abs(y - py) <= reach;
    }

    /**
     * A node.
     *
     * @param id      what it is found by
     * @param title   what it is called
     * @param x       its left edge, in the editor's space
     * @param y       its top edge
     * @param width   how wide it is
     * @param inputs  its inputs, down its left side
     * @param outputs its outputs, down its right side
     */
    public record Node(String id, String title, int x, int y, int width, List<Port> inputs, List<Port> outputs) {

        public Node {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
        }

        /** How tall it is: its title and as many ports as its longer side has. */
        public int height() {
            return TITLE + PORT_PITCH * Math.max(1, Math.max(inputs.size(), outputs.size())) + 4;
        }

        /** Where the {@code index}th port of either side stands, down the node. */
        public int portY(final int index) {
            return y + TITLE + PORT_PITCH / 2 + index * PORT_PITCH;
        }
    }

    /**
     * A port of a node.
     *
     * @param id    what it is found by among its node's
     * @param label what it says
     * @param kind  what flows through it; only ports of one kind are joined
     */
    public record Port(String id, String label, String kind) {
    }

    /**
     * A port of a given node.
     *
     * @param node   the node's id
     * @param port   the port's id
     * @param output whether it is an output
     */
    public record PortRef(String node, String port, boolean output) {
    }

    /**
     * A wire.
     *
     * @param from the output it leaves
     * @param to   the input it enters
     */
    public record Wire(PortRef from, PortRef to) {

        public String fromNode() {
            return from.node();
        }

        public String toNode() {
            return to.node();
        }
    }
}
