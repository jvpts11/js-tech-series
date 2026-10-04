/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.item.CraftingCardItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Where a Crafting Computer keeps what it can make, in the order the Crafting Manager lists them: the ROM of each of
 * its Crafting Cards, in slot order, with the bench recipes; then each Crafting Interface it drives, in the order the
 * cable reaches them, with the processing and pipeline recipes. A recipe in a window is named by its place's number
 * in this list and its number in the place, packed in one {@link #ref}.
 */
public final class CraftPlaces {

    private final List<Place> places;

    /** How many entries one place may hold, which packs a place and an entry into one number. */
    public static final int ENTRIES = 256;

    private CraftPlaces(final List<Place> places) {
        this.places = places;
    }

    /** The places of {@code computer} as they stand. */
    public static CraftPlaces of(final CraftingComputerBlockEntity computer, final ServerLevel level) {
        final List<Place> places = new ArrayList<>();
        for (final int slot : computer.cardSlots()) {
            places.add(new Place(computer, slot, null, null));
        }
        for (final CraftingFloor.Site site : computer.drivenInterfaces()) {
            final CraftingInterfacePart part = site.part(level, CraftingInterfacePart.class);
            if (part != null) {
                places.add(new Place(computer, -1, site, part));
            }
        }
        return new CraftPlaces(places);
    }

    /** The one number a recipe goes by: its place's number and its own in the place. */
    public static int ref(final int place, final int entry) {
        return place * ENTRIES + entry;
    }

    /** The place a ref names. */
    public static int placeOf(final int ref) {
        return ref / ENTRIES;
    }

    /** The entry a ref names in its place. */
    public static int entryOf(final int ref) {
        return ref % ENTRIES;
    }

    /** Every place, cards first. */
    public List<Place> all() {
        return places;
    }

    /** The place numbered {@code index}, or null. */
    @Nullable
    public Place at(final int index) {
        return index >= 0 && index < places.size() ? places.get(index) : null;
    }

    /** The first card whose ROM has room for another recipe, or null. */
    @Nullable
    public Place cardWithRoom() {
        for (final Place place : places) {
            if (place.isCard() && place.used() < place.capacity()) {
                return place;
            }
        }
        return null;
    }

    /** The first interface with room for another recipe, or null. */
    @Nullable
    public Place interfaceWithRoom() {
        for (final Place place : places) {
            if (!place.isCard() && place.used() < place.capacity()) {
                return place;
            }
        }
        return null;
    }

    /**
     * One place: a Crafting Card's ROM in a hardware slot, or a Crafting Interface the computer drives.
     *
     * @param computer the computer
     * @param slot     the card's hardware slot, or -1 for an interface
     * @param site     where the interface is, or null for a card
     * @param part     the interface, or null for a card
     */
    public record Place(CraftingComputerBlockEntity computer, int slot, @Nullable CraftingFloor.Site site,
                        @Nullable CraftingInterfacePart part) {

        public boolean isCard() {
            return part == null;
        }

        /** The card, for a card's place. */
        public ItemStack card() {
            return computer.cardIn(slot);
        }

        /** What it holds, each as a recipe. */
        public List<NetworkRecipe> recipes() {
            final List<NetworkRecipe> out = new ArrayList<>();
            if (part != null) {
                part.patterns().forEach(held -> out.add(held.recipe()));
            } else {
                CraftingCardItem.rom(card()).forEach(pattern -> out.add(NetworkRecipe.ofBench(pattern)));
            }
            return out;
        }

        public int used() {
            return part != null ? part.patterns().size() : CraftingCardItem.rom(card()).size();
        }

        public int capacity() {
            return part != null ? part.capacity() : CraftingCardItem.romSize(card());
        }

        /** Puts {@code recipe} here: a bench recipe in a card, a processing or pipeline recipe in an interface. */
        public boolean put(final NetworkRecipe recipe) {
            if (part != null) {
                return part.place(recipe);
            }
            return recipe.bench().isPresent() && computer.loadPatternInto(slot, recipe.bench().get());
        }

        /** Takes out the recipe at {@code entry}; null when there is none. */
        @Nullable
        public NetworkRecipe take(final int entry) {
            if (part != null) {
                return part.take(entry);
            }
            final List<CraftingPattern> rom = CraftingCardItem.rom(card());
            if (entry < 0 || entry >= rom.size()) {
                return null;
            }
            final CraftingPattern taken = rom.get(entry);
            if (CraftingCardItem.remove(card(), entry)) {
                computer.setChanged();
                return NetworkRecipe.ofBench(taken);
            }
            return null;
        }
    }
}
