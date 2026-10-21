/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * What happens to a moving structure, posted on the game's event bus on the server: it struck something hard, it
 * parted into pieces, or it docked to another structure or let go of one.
 *
 * <pre>{@code
 * NeoForge.EVENT_BUS.addListener((MovingStructureEvent.Collision event) -> {
 *     if (event.speed() > 10) {
 *         shake(event.structure(), event.at());
 *     }
 * });
 * }</pre>
 */
public abstract class MovingStructureEvent extends Event {

    private final MovingStructure structure;

    protected MovingStructureEvent(final MovingStructure structure) {
        this.structure = Objects.requireNonNull(structure, "structure");
    }

    /** The structure it happened to. */
    public MovingStructure structure() {
        return this.structure;
    }

    /**
     * A structure moving by its physics struck something, the world or another structure, at a speed worth telling:
     * the hardest blow of its tick.
     */
    public static final class Collision extends MovingStructureEvent {

        private final @Nullable IStructureBody other;
        private final BlockPos struck;
        private final Vec3 at;
        private final double speed;
        private final double energy;

        public Collision(final MovingStructure structure, @Nullable final IStructureBody other, final BlockPos struck,
                         final Vec3 at, final double speed, final double energy) {
            super(structure);
            this.other = other;
            this.struck = struck.immutable();
            this.at = at;
            this.speed = speed;
            this.energy = energy;
        }

        /** The structure it struck, or null for the world. */
        public @Nullable IStructureBody other() {
            return this.other;
        }

        /** The block it struck: of the world, or of the other structure, a place of its yard. */
        public BlockPos struck() {
            return this.struck;
        }

        /** Where they met, in the world. */
        public Vec3 at() {
            return this.at;
        }

        /** How fast they closed there, in metres a second. */
        public double speed() {
            return this.speed;
        }

        /** The energy of the blow, in joules. */
        public double energy() {
            return this.energy;
        }
    }

    /**
     * A structure broken in its middle parted: it keeps one piece, and the others are structures of their own, each
     * told as it goes its own way, once its place in the yard has loaded.
     */
    public static final class Parted extends MovingStructureEvent {

        private final List<MovingStructure> pieces;

        public Parted(final MovingStructure structure, final List<MovingStructure> pieces) {
            super(structure);
            this.pieces = List.copyOf(pieces);
        }

        /** The pieces gone their own way, each a structure of its own. */
        public List<MovingStructure> pieces() {
            return this.pieces;
        }
    }

    /** A structure docked to another: it is held by it, as one with it. */
    public static final class Docked extends MovingStructureEvent {

        private final MovingStructure host;

        public Docked(final MovingStructure visitor, final MovingStructure host) {
            super(visitor);
            this.host = Objects.requireNonNull(host, "host");
        }

        /** The structure docked to, which holds it. */
        public MovingStructure host() {
            return this.host;
        }
    }

    /** A docked structure was let go by the one it was docked to. */
    public static final class Undocked extends MovingStructureEvent {

        private final MovingStructure host;

        public Undocked(final MovingStructure visitor, final MovingStructure host) {
            super(visitor);
            this.host = Objects.requireNonNull(host, "host");
        }

        /** The structure it was docked to. */
        public MovingStructure host() {
            return this.host;
        }
    }
}
