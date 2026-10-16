/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.blockentity.PartField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The entity of a space of parts alone: its parts, saved and sent to the players who see it, who draw and shape the
 * space from them.
 */
public final class SpaceBlockEntity extends SyncedBlockEntity implements IPartHost, ISpaceHolder {

    private final SpaceParts parts = new SpaceParts(this);
    private final PartField partsField = fields().part(SpaceParts.KEY, this.parts).save().toClient();

    public SpaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(CoreSpaces.BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public SpaceParts parts() {
        return this.parts;
    }

    @Override
    public @Nullable Level partLevel() {
        return this.level;
    }

    @Override
    public BlockPos partPos() {
        return this.worldPosition;
    }

    @Override
    public void partChanged() {
        this.partsField.changed();
    }

    /* The space's model draws its parts from this, into the world's mesh. */
    @Override
    public ModelData getModelData() {
        return MicroblockLayout.modelData(this.parts);
    }

    /* The parts changed on a player's game: the mesh and the shape follow them. */
    @Override
    protected void afterClientUpdate() {
        requestModelDataUpdate();
        if (this.level != null) {
            final BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
