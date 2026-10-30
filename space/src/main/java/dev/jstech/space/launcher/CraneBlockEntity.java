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
 * The tip of a mobile launcher's crane boom, as its block entity: it draws the crane on the tower's top once the
 * tower is formed, as the approved launcher draws it, the house, the boom over it with the cable and hook hanging from
 * it, and the aviation lamps, so it keeps the crane's measure, worked out as the tower forms and told to the players
 * who see it.
 */
public final class CraneBlockEntity extends SyncedBlockEntity {

    private final ValueField<Crane> crane;

    public CraneBlockEntity(final BlockPos pos, final BlockState state) {
        super(LauncherBlocks.CRANE_ENTITY.get(), pos, state);
        this.crane = fields().nullable("Crane", Crane.CODEC).save().toClient();
    }

    /** The crane it draws, if the tower is formed. */
    public Optional<Crane> crane() {
        return Optional.ofNullable(this.crane.get());
    }

    /** Keeps {@code to} as the crane it draws, or none. */
    public void crane(final @Nullable Crane to) {
        this.crane.set(to);
        setChanged();
    }

    /**
     * A crane, as its boom's tip measured it.
     *
     * @param tower the way along the boom to the tower
     * @param near  how many blocks from the tip the tower's near wall stands under the boom
     * @param far   how many from it its far wall stands
     * @param back  how many from it the boom's other end is, past the tower
     */
    public record Crane(Direction tower, int near, int far, int back) {

        public static final Codec<Crane> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Direction.CODEC.fieldOf("tower").forGetter(Crane::tower),
                Codec.INT.fieldOf("near").forGetter(Crane::near),
                Codec.INT.fieldOf("far").forGetter(Crane::far),
                Codec.INT.fieldOf("back").forGetter(Crane::back)
        ).apply(instance, Crane::new));
    }
}
