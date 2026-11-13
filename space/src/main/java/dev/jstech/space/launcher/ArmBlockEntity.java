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
import dev.jstech.space.launch.LaunchCount;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The end at the rocket of an arm out from the umbilical tower, as its block entity: it draws the whole arm once the
 * arm is formed, as the approved launcher draws it, so it keeps the arm's measure, worked out as the arm forms and
 * told to the players who see it; and the launcher whose launch count swings the arm away from the rocket and back.
 */
public final class ArmBlockEntity extends SyncedBlockEntity {

    private final ValueField<Arm> arm;
    private final ValueField<BlockPos> launcher;

    public ArmBlockEntity(final BlockPos pos, final BlockState state) {
        super(LauncherBlocks.ARM_ENTITY.get(), pos, state);
        this.arm = fields().nullable("Arm", Arm.CODEC).save().toClient();
        this.launcher = fields().nullable("Launcher", BlockPos.CODEC).save().toClient();
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

    /** Its arm is swung by the launch count of the launcher whose measuring arm stands at {@code arm}. */
    public void swungBy(final BlockPos arm) {
        this.launcher.set(arm.immutable());
        setChanged();
    }

    /** How far its arm is swung away from the rocket, from nought, against it, to one, turned clear of it. */
    public float swung() {
        final BlockPos at = this.launcher.get();
        return at == null || this.level == null ? 0 : LauncherDeckBlockEntity.countAt(this.level, at)
                .map(LaunchCount::armsAway).orElse(0F);
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
