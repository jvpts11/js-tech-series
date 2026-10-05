/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.StableCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the non-controller parts of a multiblock structure. Each part remembers its controller's position
 * so a break or an interaction on the part can reach the controller, declared once here as a saved field for every
 * structure that uses it.
 */
public abstract class MultiblockPartBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockPos> controller = fields().nullable("Controller", BlockPos.CODEC).save();
    /** What the part opens onto when the structure's pattern makes it a port; null for a plain part. */
    private final ValueField<PortKind> port = fields().nullable("Port", StableCodecs.byName(PortKind.class)).save();

    protected MultiblockPartBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                        final BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public BlockPos controllerPos() {
        return controller.get();
    }

    public void setController(final BlockPos pos) {
        controller.set(pos.immutable());
    }

    /** What the part opens onto, or null when it is not a port. */
    @Nullable
    public PortKind port() {
        return port.get();
    }

    /** Makes the part a port of that kind, or a plain part for null, as the formed structure's match says. */
    public void setPort(@Nullable final PortKind kind) {
        port.set(kind);
    }
}
