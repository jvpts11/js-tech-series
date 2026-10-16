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
import dev.jstech.core.look.IDescribedAt;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The entity of a space of parts alone: its parts, saved and sent to the players who see it, who draw and shape the
 * space from them.
 */
public final class SpaceBlockEntity extends SyncedBlockEntity implements IPartHost, ISpaceHolder, IDescribedAt {

    private final SpaceParts parts = new SpaceParts(this);
    private final PartField partsField = fields().part(SpaceParts.KEY, this.parts).save().toClient();

    public SpaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(CoreSpaces.BLOCK_ENTITY.get(), pos, state);
    }

    /** Runs the parts' ticks, on the server, while any needs one. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final SpaceBlockEntity space) {
        space.parts.tick();
    }

    /* A space whose state does not show its parts, read from an older save, is set right on its first tick. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()
                && !PartStates.showing(this.level, this.worldPosition, this.parts)) {
            this.level.scheduleTick(this.worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public SpaceParts parts() {
        return this.parts;
    }

    /* The part a player looks at says what it is doing. */
    @Override
    public void describe(final List<Text> lines, final boolean details, final Player player) {
        final SpaceParts.Hit aimed = SpaceBlock.aimOf(this.level == null ? EmptyBlockGetter.INSTANCE : this.level,
                this.worldPosition, player);
        if (aimed != null) {
            this.parts.describe(aimed.slot(), lines, details);
        }
    }

    @Override
    public @Nullable Level partLevel() {
        return this.level;
    }

    @Override
    public BlockPos partPos() {
        return this.worldPosition;
    }

    /* The parts changed: they are saved and sent, and the state shows them again. */
    @Override
    public void partChanged() {
        this.partsField.changed();
        if (this.level != null && !this.level.isClientSide()) {
            PartStates.show(this.level, this.worldPosition, this.parts);
        }
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
