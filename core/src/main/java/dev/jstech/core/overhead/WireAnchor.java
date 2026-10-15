/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import dev.jstech.core.id.IStableName;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * One insulator of a block, which an overhead wire is hung from: where on the block the wire is fastened, what the
 * insulator does with what the wire carries, the face of the block it meets, and how many wires it holds.
 *
 * @param at        where the wire is fastened, from the block's lowest corner, in blocks
 * @param role      what it does with what the wire carries
 * @param face      the face of its block it meets: where a connector hands what the wire carries to the block
 * @param mostWires the most wires it holds
 */
public record WireAnchor(Vec3 at, Role role, Direction face, int mostWires) {

    public WireAnchor {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(face, "face");
        if (mostWires < 1) {
            throw new IllegalArgumentException("an insulator holds a wire at least, not " + mostWires);
        }
    }

    /** What an insulator does with what its wires carry. */
    public enum Role implements IStableName {
        /** What the wire carries enters or leaves its block here: a machine's insulator, a substation's. */
        CONNECTOR("connector"),
        /** It only passes what the wire carries from one wire to the next: a pole's or a tower's insulator. */
        RELAY("relay"),
        /** It takes the wire through a wall one block thick: the insulators on either side of it are joined. */
        FEEDTHROUGH("feedthrough");

        private final String serializedName;

        Role(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
