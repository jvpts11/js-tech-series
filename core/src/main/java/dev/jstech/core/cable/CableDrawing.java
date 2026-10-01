/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * What a cable block's model draws of its wires: each wire, lane by lane, with the faces it crosses and the faces it
 * meets a device on. A cable block entity hands it to the model through its model data under {@link #PROPERTY}; two
 * blocks that hold the same are drawn from the same quads.
 *
 * @param wires the wires, lane by lane
 * @param links each wire's faces: the low six bits those it crosses, the six from bit 8 those it meets a device on
 */
public record CableDrawing(List<Wire> wires, List<Integer> links) {

    /** Where a cable block's model finds what it draws of the wires. */
    public static final ModelProperty<CableDrawing> PROPERTY = new ModelProperty<>();
    /** A block that holds no wire. */
    public static final CableDrawing EMPTY = new CableDrawing(List.of(), List.of());
    private static final int FACE_BITS = 0x3F;
    private static final int PLUG_SHIFT = 8;

    public CableDrawing {
        wires = List.copyOf(wires);
        links = List.copyOf(links);
        if (wires.size() != links.size()) {
            throw new IllegalArgumentException("each wire has its faces: " + wires.size() + " and " + links.size());
        }
    }

    /** How the wires lie. */
    public BundleShape shape() {
        final List<BundleShape.Strand> strands = new ArrayList<>(this.wires.size());
        for (int i = 0; i < this.wires.size(); i++) {
            final Wire wire = this.wires.get(i);
            final int linked = this.links.get(i);
            strands.add(new BundleShape.Strand(wire.slot(), wire.type().thickness(), linked & FACE_BITS,
                    linked >> PLUG_SHIFT & FACE_BITS, wire.colour().isPresent()));
        }
        return BundleShape.of(strands);
    }

    public boolean isEmpty() {
        return this.wires.isEmpty();
    }
}
