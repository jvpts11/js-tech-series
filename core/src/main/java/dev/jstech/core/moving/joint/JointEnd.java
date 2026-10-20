/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.joint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.uuid.StructureUuid;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/**
 * One end of a joint: on a moving structure, at a place of its yard and with ways in its yard's axes, so the end goes
 * where the structure goes; or on the world, at a place and with ways of the world, where it stays.
 *
 * @param structure the structure the end is on, or none for the world
 * @param place     where the end is: a place of the structure's yard, or of the world
 * @param axis      the joint's axis at this end, of unit length: a hinge's, a slider's
 * @param normal    a way at right angles to the axis, of unit length, from which a hinge's turn is measured
 */
public record JointEnd(Optional<StructureUuid> structure, Vec3 place, Vec3 axis, Vec3 normal) {

    public static final Codec<JointEnd> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StructureUuid.CODEC.optionalFieldOf("structure").forGetter(JointEnd::structure),
            Vec3.CODEC.fieldOf("place").forGetter(JointEnd::place),
            Vec3.CODEC.optionalFieldOf("axis", new Vec3(0, 1, 0)).forGetter(JointEnd::axis),
            Vec3.CODEC.optionalFieldOf("normal", new Vec3(1, 0, 0)).forGetter(JointEnd::normal)
    ).apply(instance, JointEnd::new));

    /** Whether the end is on the world rather than a structure. */
    public boolean onWorld() {
        return structure.isEmpty();
    }

    /** Whether the end is on the structure {@code id}. */
    public boolean on(final StructureUuid id) {
        return structure.isPresent() && structure.get().equals(id);
    }

    /** This end moved to {@code to}, on the same structure or world, its ways unchanged. */
    public JointEnd movedTo(final Vec3 to) {
        return new JointEnd(structure, to, axis, normal);
    }

    /** This end on {@code to} instead, at {@code at}, with the ways {@code turnedAxis} and {@code turnedNormal}. */
    public JointEnd onto(final Optional<StructureUuid> to, final Vec3 at, final Vec3 turnedAxis,
                         final Vec3 turnedNormal) {
        return new JointEnd(to, at, turnedAxis, turnedNormal);
    }
}
