/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity whose state is its declared {@link #fields()}. The saving, the loading, the update the players who
 * see the block are sent and the client's reading of it are all written here, from the declarations, so a block
 * entity only declares its fields and uses them.
 *
 * <p>The client reads only the fields declared {@code toClient()}: the rest of what it holds (a menu's values, its
 * own worked-out state) is never overwritten by an update. Updates are batched: however many fields change in a
 * tick, the players who see the block are sent one update at its end.
 */
public abstract class SyncedBlockEntity extends BlockEntity {

    private final BlockEntityFields fields = new BlockEntityFields(this);

    protected SyncedBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
    }

    /** The fields this block entity declares, and what is done with them. */
    public final BlockEntityFields fields() {
        return fields;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && fields.polls()) {
            ClientUpdates.watch(server, this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            ClientUpdates.forget(server, this);
        }
    }

    @Override
    public final CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        return fields.writeClient(registries);
    }

    @Override
    public final Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public final void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        receive(tag, registries);
    }

    @Override
    public final void onDataPacket(final Connection connection, final ClientboundBlockEntityDataPacket packet,
                                   final HolderLookup.Provider registries) {
        receive(packet.getTag(), registries);
    }

    @Override
    protected final void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        fields.save(tag, registries);
    }

    @Override
    protected final void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fields.load(tag, registries);
    }

    /** Runs on the client just before an update from the server is read, while the old values still stand. */
    protected void beforeClientUpdate() {
    }

    /** Runs on the client once an update from the server has been read. */
    protected void afterClientUpdate() {
    }

    private void receive(final CompoundTag tag, final HolderLookup.Provider registries) {
        beforeClientUpdate();
        fields.readClient(tag, registries);
        afterClientUpdate();
    }
}
