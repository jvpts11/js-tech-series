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
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.moving.physics.BlockPhysics;
import net.minecraft.core.BlockPos;

/**
 * Where a joint reads the signal that drives it, a bearing's motor or a winch: a block at one of its ends, a block of
 * that end's structure or of the world, from a side of that block as the block looks.
 *
 * @param end   which end the block is at
 * @param block the block, a place of that end's structure's yard or of the world
 * @param from  the side of the block the signal reaches it from
 */
public record JointSignal(Side end, BlockPos block, BlockPhysics.SignalSide from) {

    public static final Codec<JointSignal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(Side.class).fieldOf("end").forGetter(JointSignal::end),
            BlockPos.CODEC.fieldOf("block").forGetter(JointSignal::block),
            BlockPhysics.SignalSide.CODEC.optionalFieldOf("from", BlockPhysics.SignalSide.ANY)
                    .forGetter(JointSignal::from)
    ).apply(instance, JointSignal::new));

    /** This signal read at {@code to} instead, a block of the same end moved there. */
    public JointSignal movedTo(final BlockPos to) {
        return new JointSignal(end, to, from);
    }

    /** One end of a joint or the other. */
    public enum Side implements IStableName {
        A("a"),
        B("b");

        private final String serializedName;

        Side(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
