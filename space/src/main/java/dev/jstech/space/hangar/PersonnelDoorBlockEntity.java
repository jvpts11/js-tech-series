/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a block of a personnel door of the Rocket Hangar keeps: when its leaf last began to swing, for players' games
 * to draw it turning about its hinge ({@link PersonnelDoorSwing}), open or shut as its block stands.
 */
public final class PersonnelDoorBlockEntity extends SyncedBlockEntity {

    private final ValueField<Long> swungAt;

    /* Never swung: so long ago its leaf stands at rest. */
    private static final long NEVER = Long.MIN_VALUE / 2;

    public PersonnelDoorBlockEntity(final BlockPos pos, final BlockState state) {
        super(HangarBlocks.PERSONNEL_DOOR_ENTITY.get(), pos, state);
        this.swungAt = fields().value("SwungAt", Codec.LONG, NEVER).save().toClient();
    }

    /** The game's time its leaf last began to swing. */
    public long swungAt() {
        return this.swungAt.get();
    }

    /** Its leaf begins to swing at the game's time {@code time}, the way its block now stands. */
    public void swung(final long time) {
        this.swungAt.set(time);
        setChanged();
    }

    /** How far its leaf stands turned from shut, in degrees, {@code partialTick} into this tick. */
    public double angle(final float partialTick) {
        if (this.level == null) {
            return 0;
        }
        return PersonnelDoorSwing.angle(getBlockState().getValue(PersonnelDoorBlock.OPEN),
                this.level.getGameTime() + partialTick - this.swungAt.get());
    }
}
