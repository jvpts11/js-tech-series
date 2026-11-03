/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.moving.IMovesWithStructures;
import dev.jstech.core.multiblock.shape.FormedPlace;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A starship's fixture that holds things, a suit locker, a barrel: its places, and the block of its part's formed model
 * it stands for, sent to the players, so it is aimed at and outlined as the model draws it. What it holds goes to a
 * player only by its menu.
 */
public abstract class ShipContainerEntity extends BaseContainerBlockEntity implements IFixtureEntity,
        IMovesWithStructures {

    private final int size;
    private NonNullList<ItemStack> items;
    private @Nullable FormedPlace place;

    protected ShipContainerEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                  final int size) {
        super(type, pos, state);
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public List<FormedPlace> places() {
        return this.place == null ? List.of() : List.of(this.place);
    }

    @Override
    public void place(final @Nullable FormedPlace place) {
        if (Objects.equals(this.place, place)) {
            return;
        }
        this.place = place;
        told();
    }

    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        if (this.place != null && move.rotation() != Rotation.NONE) {
            this.place = this.place.turned(move.rotation());
        }
    }

    /* The players who see it are told only the block of the model it stands for; what it holds goes by its menu. */
    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = new CompoundTag();
        FixturePlace.write(tag, this.place);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.place = FixturePlace.read(tag);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(final NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.size, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.place = FixturePlace.read(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        FixturePlace.write(tag, this.place);
    }

    /* Saved, and sent to the players who see it. */
    private void told() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
