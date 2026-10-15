/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.gui.logic.GraphModel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * A node graph editor: nodes as small windows, their inputs down the left and outputs down the right, each port in
 * the colour of what flows through it, and the wires between them drawn square with a shadow. A node is dragged by
 * its title; a wire is drawn from an output and dropped on an input of the same kind; the right button on an input
 * takes its wire away, and on the empty ground asks the owner for a new node there. The ground is dragged to look
 * round.
 */
public final class GraphEditor extends UiComponent {

    private final GraphModel model;
    private final ToIntFunction<String> kindColour;
    private final BiConsumer<Integer, Integer> onAddNode;
    private int panX;
    private int panY;
    @Nullable
    private String draggingNode;
    @Nullable
    private GraphModel.PortRef wiringFrom;
    private boolean panning;
    private int grabX;
    private int grabY;
    private int pointerX;
    private int pointerY;
    @Nullable
    private String picked;

    /** How far from a port a click still takes it. */
    private static final int REACH = 4;
    private static final int GRID = 8;
    private static final int DASH = 3;
    private static final float SMALL = 0.75f;
    private static final int RIGHT_BUTTON = 1;

    /**
     * @param model      the graph
     * @param kindColour the colour of a port and its wires, by what flows through it
     * @param onAddNode  what is done when the player asks for a new node, at a point in the graph's own space
     */
    public GraphEditor(final GraphModel model, final ToIntFunction<String> kindColour,
                       final BiConsumer<Integer, Integer> onAddNode) {
        this.model = Objects.requireNonNull(model, "model");
        this.kindColour = Objects.requireNonNull(kindColour, "kindColour");
        this.onAddNode = Objects.requireNonNull(onAddNode, "onAddNode");
    }

    /** The node chosen last, or none. */
    public Optional<String> picked() {
        return Optional.ofNullable(picked);
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().chartGround(g, x(), y(), width(), height());
        Draw.pushScissor(g, x() + 1, y() + 1, right() - 1, bottom() - 1);
        try {
            for (int gx = x() + Math.floorMod(panX, GRID); gx < right(); gx += GRID) {
                for (int gy = y() + Math.floorMod(panY, GRID); gy < bottom(); gy += GRID) {
                    g.fill(gx, gy, gx + 1, gy + 1, ctx.skin().chartLine());
                }
            }
            for (final GraphModel.Wire wire : model.wires()) {
                final Optional<int[]> from = model.where(wire.from());
                final Optional<int[]> to = model.where(wire.to());
                if (from.isPresent() && to.isPresent()) {
                    wire(g, screen(from.get()), screen(to.get()), colourOf(wire.from()), false);
                }
            }
            if (wiringFrom != null) {
                model.where(wiringFrom).ifPresent(from -> wire(g, screen(from), new int[] {pointerX, pointerY},
                        colourOf(wiringFrom), true));
            }
            for (final GraphModel.Node node : model.nodes()) {
                drawNode(g, ctx, node);
            }
        } finally {
            Draw.popScissor(g);
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        final int gx = (int) mx - x() - panX;
        final int gy = (int) my - y() - panY;
        pointerX = (int) mx;
        pointerY = (int) my;
        final Optional<GraphModel.PortRef> port = model.portAt(gx, gy, REACH);
        if (button == RIGHT_BUTTON) {
            if (port.isPresent() && !port.get().output()) {
                model.disconnect(port.get());
            } else if (model.nodeAt(gx, gy).isEmpty()) {
                onAddNode.accept(gx, gy);
            }
            return true;
        }
        if (port.isPresent() && port.get().output()) {
            wiringFrom = port.get();
            return true;
        }
        final Optional<GraphModel.Node> node = model.nodeAt(gx, gy);
        if (node.isPresent()) {
            picked = node.get().id();
            if (gy < node.get().y() + GraphModel.TITLE) {
                draggingNode = node.get().id();
                grabX = gx - node.get().x();
                grabY = gy - node.get().y();
            }
            return true;
        }
        panning = true;
        grabX = (int) mx - panX;
        grabY = (int) my - panY;
        return true;
    }

    @Override
    public boolean mouseDragged(final double mx, final double my, final int button) {
        pointerX = (int) mx;
        pointerY = (int) my;
        if (draggingNode != null) {
            model.move(draggingNode, (int) mx - x() - panX - grabX, (int) my - y() - panY - grabY);
        } else if (panning) {
            panX = (int) mx - grabX;
            panY = (int) my - grabY;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        if (wiringFrom != null) {
            model.portAt((int) mx - x() - panX, (int) my - y() - panY, REACH)
                    .filter(port -> !port.output())
                    .ifPresent(input -> model.connect(wiringFrom, input));
        }
        wiringFrom = null;
        draggingNode = null;
        panning = false;
        return true;
    }

    private void drawNode(final GuiGraphics g, final UiContext ctx, final GraphModel.Node node) {
        final int nx = x() + panX + node.x();
        final int ny = y() + panY + node.y();
        if (node.id().equals(picked)) {
            Draw.outline(g, nx - 2, ny - 2, node.width() + 4, node.height() + 4, ctx.skin().accent());
        }
        ctx.skin().windowFrame(g, nx, ny, node.width(), node.height());
        ctx.skin().drawText(g, ctx.font(), node.title(), nx + 5, ny + 4, ctx.skin().text());
        ctx.skin().separator(g, nx + 4, ny + GraphModel.TITLE - 2, node.width() - 8);
        ports(g, ctx, node, node.inputs(), nx, ny, false);
        ports(g, ctx, node, node.outputs(), nx + node.width(), ny, true);
    }

    private void ports(final GuiGraphics g, final UiContext ctx, final GraphModel.Node node,
                       final List<GraphModel.Port> ports, final int px, final int ny, final boolean outputs) {
        for (int i = 0; i < ports.size(); i++) {
            final GraphModel.Port port = ports.get(i);
            final int py = ny - node.y() + node.portY(i);
            g.fill(px - 3, py - 3, px + 3, py + 3, ctx.skin().windowBg());
            g.fill(px - 2, py - 2, px + 2, py + 2, kindColour.applyAsInt(port.kind()));
            final int tw = Math.round(ctx.skin().textWidth(ctx.font(), port.label()) * SMALL);
            final int tx = outputs ? px - 6 - tw : px + 6;
            ctx.skin().drawText(g, ctx.font(), port.label(), tx, py - 3, ctx.skin().dim(), SMALL);
        }
    }

    private int colourOf(final GraphModel.PortRef ref) {
        final Optional<GraphModel.Node> node = model.node(ref.node());
        if (node.isEmpty()) {
            return 0;
        }
        for (final GraphModel.Port port : ref.output() ? node.get().outputs() : node.get().inputs()) {
            if (port.id().equals(ref.port())) {
                return kindColour.applyAsInt(port.kind());
            }
        }
        return 0;
    }

    private int[] screen(final int[] graph) {
        return new int[] {x() + panX + graph[0], y() + panY + graph[1]};
    }

    /* A wire out of an output to the right, across at the middle, and into the input from its left. */
    private static void wire(final GuiGraphics g, final int[] from, final int[] to, final int colour,
                             final boolean dashed) {
        final int middle = (from[0] + to[0]) / 2;
        segment(g, from[0], from[1], middle, from[1], colour, dashed);
        segment(g, middle, from[1], middle, to[1], colour, dashed);
        segment(g, middle, to[1], to[0], to[1], colour, dashed);
    }

    private static void segment(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                                final int colour, final boolean dashed) {
        // The shadow is the wire's own alpha in black, a pixel below and to the right.
        final int shadow = colour >>> 24 << 24;
        if (y0 == y1) {
            final int end = Math.max(x0, x1) + 1;
            for (int x = Math.min(x0, x1); x < end; x = dashed ? x + 2 * DASH : end) {
                final int stop = dashed ? Math.min(end, x + DASH) : end;
                g.fill(x, y0 + 1, stop, y0 + 2, shadow);
                g.fill(x, y0, stop, y0 + 1, colour);
            }
        } else {
            final int end = Math.max(y0, y1) + 1;
            for (int y = Math.min(y0, y1); y < end; y = dashed ? y + 2 * DASH : end) {
                final int stop = dashed ? Math.min(end, y + DASH) : end;
                g.fill(x0 + 1, y, x0 + 2, stop, shadow);
                g.fill(x0, y, x0 + 1, stop, colour);
            }
        }
    }
}
