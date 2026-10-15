/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import java.util.List;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * What a cable block's model draws inside its wires' windows: what each wire carries, in the order of the wires it
 * draws. A cable block entity hands it to the model through its model data under {@link #PROPERTY}.
 *
 * @param flows each wire's flow, in the order of {@link CableDrawing#wires()}
 */
public record CableFlowView(List<WireFlow> flows) {

    /** Where a cable block's model finds what its wires carry. */
    public static final ModelProperty<CableFlowView> PROPERTY = new ModelProperty<>();
    /** Nothing flows, or no wire shows it. */
    public static final CableFlowView NONE = new CableFlowView(List.of());

    public CableFlowView {
        flows = List.copyOf(flows);
    }

    /** What the wire drawn {@code index}th carries. */
    public WireFlow of(final int index) {
        return index < this.flows.size() ? this.flows.get(index) : WireFlow.EMPTY;
    }

    /** Whether nothing flows in any wire. */
    public boolean isEmpty() {
        return this.flows.stream().allMatch(WireFlow::isEmpty);
    }
}
