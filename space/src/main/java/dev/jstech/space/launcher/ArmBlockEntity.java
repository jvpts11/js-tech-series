/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The end at the rocket of an arm out from the umbilical tower, as its block entity: it draws the whole arm once the
 * arm is formed, as the approved launcher draws it, so it keeps the arm's measure, worked out as the arm forms and
 * told to the players who see it.
 */
public final class ArmBlockEntity extends SyncedBlockEntity {

    private final ValueField<Arm> arm;

    public ArmBlockEntity(final BlockPos pos, final BlockState state) {
        super(LauncherBlocks.ARM_ENTITY.get(), pos, state);
        this.arm = fields().nullable("Arm", Arm.CODEC).save().toClient();
    }

    /** The arm it draws, if the arm is formed. */
    public Optional<Arm> arm() {
        return Optional.ofNullable(this.arm.get());
    }

    /** Keeps {@code to} as the arm it draws, or none. */
    public void arm(final @Nullable Arm to) {
        this.arm.set(to);
        setChanged();
    }

    /**
     * An arm, as its end measured it.
     *
     * @param out    the way it runs out from its tower to the rocket
     * @param length how many blocks it has, from its end here to the tower's face
     */
    public record Arm(Direction out, int length) {

        public static final Codec<Arm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.fieldOf("out").forGetter(Arm::out),
                Codec.INT.fieldOf("length").forGetter(Arm::length)
        ).apply(instance, Arm::new));
    }
}
