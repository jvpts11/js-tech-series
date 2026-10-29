/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.platform;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A landing platform's terminal: the multiblock of its pads, a machine of J's Core's kit that spends nothing and makes
 * nothing, with its owner and their team, its id the flight computers know it by, and the stage it holds. Formed, it
 * is noted in the world's platforms; undone or broken, it is let go. It waits for a stage while it is free, and works
 * while it holds one.
 */
public final class LandingPlatformBlockEntity extends MachineBlockEntity {

    private final ValueField<UUID> platform;
    private final ValueField<Integer> shown;
    private final MachineStructure structure;

    public LandingPlatformBlockEntity(final BlockPos pos, final BlockState state) {
        super(LandingPlatform.ENTITY.get(), pos, state, 0, MachineEnergy.fe(0, 0, 0));
        this.platform = fields().value("Platform", UUIDUtil.STRING_CODEC, UUID.randomUUID()).save().toClient();
        this.shown = fields().value("Side", Codec.INT, 0).save().toClient();
        this.structure = MachineStructure.of(LandingPlatform.SHAPE);
        formsWith(this.structure);
        fields().whenBroken((level, at) -> LandingPlatforms.drop(level.getServer(), id()));
    }

    /** The id the flight computers know it by. */
    public UUID id() {
        final UUID id = this.platform.get();
        return id == null ? new UUID(0, 0) : id;
    }

    /** How wide it was built, in blocks; none while it is not formed. */
    public int side() {
        return this.structure.structure().map(formed -> Math.max(formed.sizeX(), formed.sizeZ())).orElse(0);
    }

    /** How wide it stands formed, in blocks, as its players' games are told; none while it is not formed. */
    public int shownSide() {
        final Integer side = this.shown.get();
        return side == null ? 0 : side;
    }

    /** Whether it holds a stage, as the world's platforms say. */
    public boolean holding(final ServerLevel level) {
        return LandingPlatforms.get(level.getServer(), id()).map(entry -> !entry.free()).orElse(false);
    }

    /* It spends no energy. */
    @Override
    public boolean takesEnergy() {
        return false;
    }

    /* It holds no items. */
    @Override
    public boolean inventoryOn(final int tab) {
        return false;
    }

    /* It shows nothing as it works. */
    @Override
    public void animate(final Level level, final BlockPos pos, final RandomSource random) {
    }

    /* Handed to another owner, the world's platforms hear whose it is now. */
    @Override
    public void own(@Nullable final Ownership to) {
        super.own(to);
        if (this.level instanceof ServerLevel level) {
            noteOrDrop(level);
        }
    }

    /* Opened to others or closed, the world's platforms hear who may land on it. */
    @Override
    public void setAccess(final Access to) {
        super.setAccess(to);
        if (this.level instanceof ServerLevel level) {
            noteOrDrop(level);
        }
    }

    /* Moved aboard a moving structure or back, it is the same platform, noted where it stands now. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        if (this.level instanceof ServerLevel level) {
            noteOrDrop(level);
        }
    }

    @Override
    protected MachineStatus work(final ServerLevel level) {
        return holding(level) ? MachineStatus.WORKING : MachineStatus.WAITING;
    }

    @Override
    protected void structureChecked(final ServerLevel level) {
        noteOrDrop(level);
    }

    @Override
    protected Set<MachineResource> resources() {
        return Set.of();
    }

    private void noteOrDrop(final ServerLevel level) {
        final int side = this.structure.formed() ? side() : 0;
        if (side != shownSide()) {
            this.shown.set(side);
            setChanged();
        }
        final Ownership owned = ownership();
        if (this.structure.formed() && owned != null) {
            LandingPlatforms.note(level, id(), this.worldPosition, side(), owned);
        } else if (!this.structure.formed()) {
            LandingPlatforms.drop(level.getServer(), id());
        }
    }
}
