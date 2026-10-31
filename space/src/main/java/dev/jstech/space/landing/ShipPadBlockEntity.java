/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

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
 * A ship landing pad's terminal: the multiblock of its deck, its truss, its stair and its masts, a machine of J's
 * Core's kit that spends nothing and makes nothing, with its owner and their team, and its id the ships' autopilots
 * know it by. Formed, its blocks merge into the pad drawn whole, and it is noted in the world's landings, its ships
 * setting down on its deck's middle; undone or broken, it is let go. It waits while it stands bare, and works while a
 * ship stands on it.
 */
public final class ShipPadBlockEntity extends MachineBlockEntity {

    private final ValueField<UUID> pad;
    private final ValueField<Boolean> shown;
    private final MachineStructure structure;

    public ShipPadBlockEntity(final BlockPos pos, final BlockState state) {
        super(ShipLandingBlocks.ENTITY.get(), pos, state, 0, MachineEnergy.fe(0, 0, 0));
        this.pad = fields().value("Pad", UUIDUtil.STRING_CODEC, UUID.randomUUID()).save().toClient();
        this.shown = fields().value("Formed", Codec.BOOL, false).save().toClient();
        // Formed, its blocks merge into the pad drawn whole.
        this.structure = MachineStructure.of(ShipLandingBlocks.PAD).merges();
        formsWith(this.structure);
        fields().whenBroken((level, at) -> ShipLandings.dropPad(level.getServer(), id()));
    }

    /** The id the ships' autopilots know it by. */
    public UUID id() {
        final UUID id = this.pad.get();
        return id == null ? new UUID(0, 0) : id;
    }

    /** Whether it stands formed, as its players' games are told. */
    public boolean shownFormed() {
        return Boolean.TRUE.equals(this.shown.get());
    }

    /** Its deck's middle block, a ship setting down on top of it, as the pad stands formed; itself unformed. */
    public BlockPos middle() {
        return formedOrientation().map(turned -> ShipLandingBlocks.middle(this.worldPosition, turned))
                .orElse(this.worldPosition);
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

    /* Handed to another owner, the world's landings hear whose it is now. */
    @Override
    public void own(@Nullable final Ownership to) {
        super.own(to);
        if (this.level instanceof ServerLevel level) {
            noteOrDrop(level);
        }
    }

    /* Opened to others or closed, the world's landings hear who may set down on it. */
    @Override
    public void setAccess(final Access to) {
        super.setAccess(to);
        if (this.level instanceof ServerLevel level) {
            noteOrDrop(level);
        }
    }

    /* Moved aboard a moving structure or back, it is the same pad, noted where it stands now. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        if (this.level instanceof ServerLevel level) {
            noteOrDrop(level);
        }
    }

    @Override
    protected MachineStatus work(final ServerLevel level) {
        return this.structure.formed() && ShipLandings.taken(level, ShipLandings.over(level, middle()), null)
                ? MachineStatus.WORKING : MachineStatus.WAITING;
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
        final boolean formed = this.structure.formed();
        if (formed != shownFormed()) {
            this.shown.set(formed);
            setChanged();
        }
        final Ownership owned = ownership();
        if (formed && owned != null) {
            ShipLandings.notePad(level, id(), this.worldPosition, middle(), owned);
        } else if (!formed) {
            ShipLandings.dropPad(level.getServer(), id());
        }
    }
}
