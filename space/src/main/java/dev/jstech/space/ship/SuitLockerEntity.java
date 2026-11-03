/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.text.GameText;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.life.OxygenBottleItem;
import dev.jstech.space.life.SuitModuleItem;
import dev.jstech.space.life.SuitPatchItem;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a suit locker holds, a place for each thing, as the approved locker draws it: a space suit's helmet, its suit,
 * its trousers and its boots on the rack, four oxygen bottles on the shelf over it, and the suit's modules and patches
 * in the box at its foot. Each place takes only its own thing, from a player or a hopper. The players who see it are
 * told what it shows, the suit's pieces on its rack and how many bottles, so the airlock draws them; what else it
 * holds goes to a player only by its menu.
 */
public final class SuitLockerEntity extends ShipContainerEntity {

    /* What it shows, as told its players: a bit for each of the suit's pieces, then how many bottles. */
    private int shown;
    /* The suit's pieces on its rack, as told its players: on theirs what they draw, on the server what was sent. */
    private final ItemStack[] rack = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    /** Its places, by their number: the suit's four pieces, the four bottles, the kit box. */
    public static final List<Place> PLACES = List.of(Place.HELMET, Place.SUIT, Place.TROUSERS, Place.BOOTS,
            Place.BOTTLE, Place.BOTTLE, Place.BOTTLE, Place.BOTTLE, Place.KIT);
    private static final String SHOWN = "Shown";
    private static final String RACK = "Rack";
    private static final int PIECE_BITS = 4;
    private static final int PIECES_MASK = (1 << PIECE_BITS) - 1;

    public SuitLockerEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.SUIT_LOCKER_ENTITY.get(), pos, state, PLACES.size());
    }

    /** Whether the place numbered {@code slot} takes {@code stack}. */
    @Override
    public boolean canPlaceItem(final int slot, final ItemStack stack) {
        return slot >= 0 && slot < PLACES.size() && PLACES.get(slot).takes(this.level, stack);
    }

    /** Whether it holds the suit's piece in {@code place}, one of the rack's four; on a player's game, as told. */
    public boolean holds(final Place place) {
        final int bit = PLACES.indexOf(place);
        return bit >= 0 && bit < PIECE_BITS && (this.shown & 1 << bit) != 0;
    }

    /** How many bottles it holds on its shelf: as its players are told, on theirs. */
    public int bottles() {
        return this.shown >> PIECE_BITS;
    }

    /**
     * The suit's piece on its rack in the place {@code place}, one of the rack's four; empty for none. On a player's
     * game, as they are told.
     */
    public ItemStack piece(final Place place) {
        final int slot = PLACES.indexOf(place);
        if (slot < 0 || slot >= PIECE_BITS) {
            return ItemStack.EMPTY;
        }
        return this.level != null && this.level.isClientSide() ? this.rack[slot] : getItem(slot);
    }

    /* Saved; told its players what it shows, where that changed. */
    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        boolean changed = shownNow() != this.shown;
        for (int slot = 0; slot < PIECE_BITS; slot++) {
            changed |= !ItemStack.matches(getItem(slot), this.rack[slot]);
        }
        if (changed) {
            this.shown = shownNow();
            for (int slot = 0; slot < PIECE_BITS; slot++) {
                this.rack[slot] = getItem(slot).copy();
            }
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(SHOWN, shownNow());
        final ListTag pieces = new ListTag();
        for (int slot = 0; slot < PIECE_BITS; slot++) {
            pieces.add(getItem(slot).saveOptional(registries));
        }
        tag.put(RACK, pieces);
        return tag;
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        told(tag, registries);
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Told its players by a block's update, on theirs; worked out from what it holds, loaded on the server.
        if (tag.contains(SHOWN)) {
            told(tag, registries);
        } else {
            this.shown = shownNow();
            for (int slot = 0; slot < PIECE_BITS; slot++) {
                this.rack[slot] = getItem(slot).copy();
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return GameText.component(ShipFixtureTexts.SUIT_LOCKER);
    }

    @Override
    protected AbstractContainerMenu createMenu(final int id, final Inventory inventory) {
        return new SuitLockerMenu(id, inventory, this);
    }

    /* What it shows, as {@code tag} tells its players: the bits of what it holds, and the suit's pieces on its rack. */
    private void told(final CompoundTag tag, final HolderLookup.Provider registries) {
        this.shown = tag.getInt(SHOWN);
        final ListTag pieces = tag.getList(RACK, Tag.TAG_COMPOUND);
        for (int slot = 0; slot < PIECE_BITS; slot++) {
            this.rack[slot] = slot < pieces.size() ? ItemStack.parseOptional(registries, pieces.getCompound(slot))
                    : ItemStack.EMPTY;
        }
    }

    /* What it shows from what it holds: a bit for each of the suit's pieces there, then how many bottles. */
    private int shownNow() {
        int out = 0;
        int bottles = 0;
        for (int slot = 0; slot < PLACES.size(); slot++) {
            if (getItem(slot).isEmpty()) {
                continue;
            }
            if (slot < PIECE_BITS) {
                out |= 1 << slot;
            } else if (PLACES.get(slot) == Place.BOTTLE) {
                bottles++;
            }
        }
        return out & PIECES_MASK | bottles << PIECE_BITS;
    }

    /** A place of the locker, and what it takes. */
    public enum Place {
        /** A space suit's helmet. */
        HELMET(SpaceSuit::helmet),
        /** A space suit's suit, worn on the chest. */
        SUIT(SpaceSuit::chest),
        /** A space suit's trousers. */
        TROUSERS(SpaceSuit::legs),
        /** A space suit's boots. */
        BOOTS(SpaceSuit::feet),
        /** An oxygen bottle. */
        BOTTLE(null),
        /** A suit's module or a patch. */
        KIT(null);

        private final @Nullable Function<SpaceSuit, ResourceLocation> piece;

        Place(final @Nullable Function<SpaceSuit, ResourceLocation> piece) {
            this.piece = piece;
        }

        /** Whether it takes {@code stack}, the suits known on {@code level}'s side, or the declared ones with none. */
        public boolean takes(final @Nullable Level level, final ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (this == BOTTLE) {
                return stack.getItem() instanceof OxygenBottleItem;
            }
            if (this == KIT) {
                return stack.getItem() instanceof SuitModuleItem || stack.getItem() instanceof SuitPatchItem;
            }
            final ResourceLocation item = BuiltInRegistries.ITEM.getKey(stack.getItem());
            final Collection<SpaceSuit> suits = level == null ? SpaceSuits.declared().values()
                    : SpaceSuits.all(level).values();
            return suits.stream().anyMatch(suit -> this.piece.apply(suit).equals(item));
        }
    }
}
