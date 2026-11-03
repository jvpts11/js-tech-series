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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a starship's furnace keeps: the game's furnace's own slots, fuel and smelting, and the block of its part's
 * formed model it stands for, sent to the players, so it is aimed at and outlined as the model draws it.
 */
public final class ShipFurnaceEntity extends AbstractFurnaceBlockEntity implements IFixtureEntity,
        IMovesWithStructures {

    private @Nullable FormedPlace place;

    public ShipFurnaceEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.SHIP_FURNACE_ENTITY.get(), pos, state, RecipeType.SMELTING);
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
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        if (this.place != null && move.rotation() != Rotation.NONE) {
            this.place = this.place.turned(move.rotation());
        }
    }

    /* The players who see it are told only the block of the model it stands for; the rest goes by its menu. */
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
    protected Component getDefaultName() {
        return GameText.component(ShipFixtureTexts.FURNACE);
    }

    @Override
    protected AbstractContainerMenu createMenu(final int id, final Inventory inventory) {
        return new FurnaceMenu(id, inventory, this, this.dataAccess);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.place = FixturePlace.read(tag);
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        FixturePlace.write(tag, this.place);
    }
}
