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
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a suit locker holds: a row of nine places, for the suit's four pieces and what goes with them. It keeps too the
 * block of its part's formed model it stands for, sent to the players, so it is aimed at and outlined as the model
 * draws it.
 */
public final class SuitLockerEntity extends BaseContainerBlockEntity implements IFixtureEntity,
        IMovesWithStructures {

    private NonNullList<ItemStack> items = NonNullList.withSize(PLACES, ItemStack.EMPTY);
    private @Nullable FormedPlace place;
    private static final int PLACES = 9;
    private static final String PLACE = "FixturePlace";

    public SuitLockerEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.SUIT_LOCKER_ENTITY.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return PLACES;
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
        writePlace(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.place = readPlace(tag);
    }

    @Override
    protected Component getDefaultName() {
        return GameText.component(ShipFixtureTexts.SUIT_LOCKER);
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
    protected AbstractContainerMenu createMenu(final int id, final Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, this, 1);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(PLACES, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.place = readPlace(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        writePlace(tag);
    }

    /* Saved, and sent to the players who see it. */
    private void told() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void writePlace(final CompoundTag tag) {
        if (this.place != null) {
            FormedPlace.CODEC.encodeStart(NbtOps.INSTANCE, this.place).result().ifPresent(at -> tag.put(PLACE, at));
        }
    }

    private static @Nullable FormedPlace readPlace(final CompoundTag tag) {
        return tag.contains(PLACE) ? FormedPlace.CODEC.parse(NbtOps.INSTANCE, tag.get(PLACE)).result().orElse(null)
                : null;
    }
}
