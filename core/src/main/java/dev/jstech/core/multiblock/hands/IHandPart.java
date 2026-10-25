/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * A part of a formed structure's model a player works by hand, by the bone of the model it is: a door that opens, a
 * place a thing is set in, a control that is pressed. Pure.
 */
public sealed interface IHandPart permits IHandPart.Door, IHandPart.Place, IHandPart.Control {

    /** The bone of the model the part is. */
    String bone();

    /**
     * Whether the bone {@code name} is of this part: the part's own bone, or one named after it, as a place's bones,
     * one for each thing it can hold ({@code place_3_cooler} of {@code place_3}).
     */
    default boolean owns(final String name) {
        return name.equals(bone()) || name.startsWith(bone() + "_");
    }

    /**
     * A door: its bone turns on its hinge as it opens and closes, and what lies behind it is reached only while it is
     * open.
     *
     * @param bone  its bone
     * @param hinge where it turns and how far
     */
    record Door(String bone, Hinge hinge) implements IHandPart {

        public Door {
            Objects.requireNonNull(bone, "bone");
            Objects.requireNonNull(hinge, "hinge");
        }
    }

    /**
     * A place: what the terminal holds in the slot {@code slot} of its inventory is shown there; worked with a thing
     * that fits in the hand, it sets one there, and with an empty hand it takes it out.
     *
     * @param bone its bone, and the bones named after it
     * @param slot the slot of the terminal's inventory it is
     * @param door the door it lies behind, reached only while that is open; none for a place in the open
     */
    record Place(String bone, int slot, @Nullable String door) implements IHandPart {

        public Place {
            Objects.requireNonNull(bone, "bone");
            if (slot < 0) {
                throw new IllegalArgumentException("a place is a slot of the inventory, not " + slot);
            }
        }
    }

    /**
     * A control: pressed, it does what its action says, as a button on the terminal's screen would.
     *
     * @param bone   its bone
     * @param action which of the model's actions it does, by the order they were declared in
     */
    record Control(String bone, int action) implements IHandPart {

        public Control {
            Objects.requireNonNull(bone, "bone");
        }
    }
}
