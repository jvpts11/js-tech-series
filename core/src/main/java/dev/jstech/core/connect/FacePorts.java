/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ports of a block: on which of its faces it takes which lines, declared once as a constant and asked by whatever
 * reaches a face (a cable deciding whether to show a connection, the device deciding whether it is on a network), so
 * the two never disagree.
 *
 * <pre>{@code
 * FacePorts PORTS = FacePorts.builder()
 *         .port(FaceRule.BACK, ACCESS, BACKBONE)
 *         .port(FaceRule.EVERY, CRAFTING)
 *         .build();
 * }</pre>
 */
public final class FacePorts {

    private final List<Port> ports;
    private final Set<ResourceLocation> lines;

    private FacePorts(final List<Port> ports) {
        this.ports = List.copyOf(ports);
        final Set<ResourceLocation> taken = new LinkedHashSet<>();
        for (final Port port : this.ports) {
            for (final Connection connection : port.takes()) {
                taken.add(connection.line());
            }
        }
        this.lines = Set.copyOf(taken);
    }

    /** Starts declaring a block's ports. */
    public static Builder builder() {
        return new Builder();
    }

    /** One port on every face, taking each of {@code takes}. */
    public static FacePorts everyFace(final Connection... takes) {
        return builder().port(FaceRule.EVERY, takes).build();
    }

    /** Whether the world's {@code face} of a block in {@code state} takes {@code offered}. */
    public boolean accepts(final BlockState state, final Direction face, final Connection offered) {
        for (final Port port : this.ports) {
            if (port.faces().matches(state, face) && port.takes(offered)) {
                return true;
            }
        }
        return false;
    }

    /** Every line some face takes. */
    public Set<ResourceLocation> lines() {
        return this.lines;
    }

    public List<Port> ports() {
        return this.ports;
    }

    /**
     * One port: the faces it is on, and what it takes there.
     *
     * @param faces the faces
     * @param takes each line it takes, at the newest generation it takes; earlier generations are taken too
     */
    public record Port(FaceRule faces, List<Connection> takes) {

        public Port {
            Objects.requireNonNull(faces, "faces");
            takes = List.copyOf(takes);
            if (takes.isEmpty()) {
                throw new IllegalArgumentException("a port takes at least one line");
            }
        }

        /** Whether this port takes {@code offered}. */
        public boolean takes(final Connection offered) {
            for (final Connection connection : this.takes) {
                if (connection.takes(offered)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Declares ports one at a time. */
    public static final class Builder {

        private final List<Port> ports = new ArrayList<>();

        private Builder() {
        }

        /** A port on {@code faces}, taking each of {@code takes}. */
        public Builder port(final FaceRule faces, final Connection... takes) {
            this.ports.add(new Port(faces, List.of(takes)));
            return this;
        }

        public FacePorts build() {
            return new FacePorts(this.ports);
        }
    }
}
