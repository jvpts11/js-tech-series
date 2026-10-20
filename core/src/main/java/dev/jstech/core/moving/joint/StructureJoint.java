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
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * A joint between two ends, each on a moving structure or on the world: what it holds ({@link JointKind}) and the
 * load it breaks at, if any. Kept with the world, by the dimension, and found by either end's structure
 * ({@link StructureJoints}).
 *
 * @param id       its name
 * @param a        its first end
 * @param b        its second end
 * @param kind     what it holds
 * @param breaksAt the load it breaks at, in newtons or newton metres, or none for a joint that never breaks
 */
public record StructureJoint(UUID id, JointEnd a, JointEnd b, JointKind kind, Optional<Double> breaksAt) {

    public static final Codec<StructureJoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(StructureJoint::id),
            JointEnd.CODEC.fieldOf("a").forGetter(StructureJoint::a),
            JointEnd.CODEC.fieldOf("b").forGetter(StructureJoint::b),
            JointKind.CODEC.fieldOf("kind").forGetter(StructureJoint::kind),
            Codec.doubleRange(0, Double.MAX_VALUE).optionalFieldOf("breaks_at").forGetter(StructureJoint::breaksAt)
    ).apply(instance, StructureJoint::new));

    /** Whether either end is on the structure {@code id}. */
    public boolean touches(final StructureUuid id) {
        return a.on(id) || b.on(id);
    }

    /** The end on the side {@code side}. */
    public JointEnd end(final JointSignal.Side side) {
        return side == JointSignal.Side.A ? a : b;
    }

    /** This joint holding {@code next} instead. */
    public StructureJoint holding(final JointKind next) {
        return new StructureJoint(id, a, b, next, breaksAt);
    }

    /** This joint with its ends {@code first} and {@code second}. */
    public StructureJoint between(final JointEnd first, final JointEnd second) {
        return new StructureJoint(id, first, second, kind, breaksAt);
    }

    /** A number naming it, for its rows from step to step. */
    public long key() {
        return id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17);
    }
}
