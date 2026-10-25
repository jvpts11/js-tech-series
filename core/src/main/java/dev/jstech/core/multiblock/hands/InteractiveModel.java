/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import org.jetbrains.annotations.Nullable;

/**
 * What of a formed structure's model a player works by hand, declared by the model's bones: doors that open, places a
 * thing is set in, controls that are pressed. The structure's terminal keeps whether each door is open, saved with it;
 * a player looking at a part sees it outlined, not the block, and works it with a click, checked again on the server.
 * Who may work it is who may open the terminal's screen.
 *
 * <pre>{@code
 * public static final InteractiveModel HANDS = InteractiveModel.of(MyContent.MODEL)
 *         .door("door_0", Hinge.vertical(-23, 92).turns(-90).inTicks(8))
 *         .places(9, place -> "place_" + place, place -> place, place -> "door_0")
 *         .control("button_start", IHandControl.START);
 * formsWith(MachineStructure.of(MyContent.TANK_SHAPE).merges().handsOn(HANDS));   // in the constructor
 * }</pre>
 */
public final class InteractiveModel {

    private final DeclaredModel model;
    private final List<IHandPart> parts = new ArrayList<>();
    private final List<IHandControl> actions = new ArrayList<>();
    private @Nullable HandGeometry geometry;

    private InteractiveModel(final DeclaredModel model) {
        this.model = model;
    }

    /** What of {@code model}, a structure's formed model, is worked by hand: nothing until declared. */
    public static InteractiveModel of(final DeclaredModel model) {
        return new InteractiveModel(Objects.requireNonNull(model, "model"));
    }

    /** A door, the bone {@code bone}, turning on {@code hinge}. */
    public InteractiveModel door(final String bone, final Hinge hinge) {
        this.parts.add(new IHandPart.Door(bone, hinge));
        return this;
    }

    /**
     * A place, the bone {@code bone} and the bones named after it, showing the slot {@code slot} of the terminal's
     * inventory, behind the door {@code door}, or none.
     */
    public InteractiveModel place(final String bone, final int slot, @Nullable final String door) {
        this.parts.add(new IHandPart.Place(bone, slot, door));
        return this;
    }

    /** {@code count} places, the {@code n}th of the bone, the slot and the door those say of it. */
    public InteractiveModel places(final int count, final IntFunction<String> bone, final IntUnaryOperator slot,
                                   final IntFunction<String> door) {
        for (int place = 0; place < count; place++) {
            place(bone.apply(place), slot.applyAsInt(place), door.apply(place));
        }
        return this;
    }

    /** A control, the bone {@code bone}, doing {@code action} when pressed. */
    public InteractiveModel control(final String bone, final IHandControl action) {
        this.parts.add(new IHandPart.Control(bone, this.actions.size()));
        this.actions.add(Objects.requireNonNull(action, "action"));
        return this;
    }

    /** The model it works. */
    public DeclaredModel model() {
        return this.model;
    }

    /** Its parts, in the order they were declared. */
    public List<IHandPart> parts() {
        return List.copyOf(this.parts);
    }

    /** What the control of action {@code action} does. */
    public IHandControl action(final int action) {
        return this.actions.get(action);
    }

    /**
     * Where its parts stand in its model, read from the model's file once, the same on the server and on the
     * player's game.
     *
     * @throws DeclarationError when the model's file does not read, or has no bone a part names
     */
    public synchronized HandGeometry geometry() {
        if (this.geometry == null) {
            try {
                this.geometry = new HandGeometry(HandModels.read(this.model), this.parts);
            } catch (final IllegalArgumentException wrong) {
                throw DeclarationError.of(this.model.id().getNamespace(), "the parts worked by hand of the model "
                        + this.model.id(), wrong.getMessage(), "name the bones its file has, and doors among the "
                        + "parts declared");
            }
        }
        return this.geometry;
    }
}
