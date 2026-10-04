/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.menu.CraftingRouterMenu;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.tier.HardwareEra;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * A Crafting Input Router: on the crafting cable that leaves a Crafting Interface, against one input face of a machine
 * with several. The interface sends each input of a pattern through the router chosen for it, by default the one
 * whose filter takes it. A router is named, which is how the interface lists it and software finds it, and it moves
 * nothing on its own: the interface feeds through it, measured and accounted for.
 */
public final class CraftingRouterPart extends ExportBusPart {

    private UUID id = UUID.randomUUID();

    /* One design for every era: it only marks a face, and keeps the filter that routes it. */
    public CraftingRouterPart() {
        super(HardwareEra.STANDARD);
    }

    @Override
    public PartType<?> type() {
        return ComputingParts.ROUTER.get();
    }

    /** The id an interface's routes know it by; it never changes. */
    public UUID id() {
        return id;
    }

    @Override
    public void serverTick() {
        // Passive: moving on its own would race the interface's measured feeding (and its accounting).
        final ServerLevel level = serverLevel();
        if (level != null) {
            settleLamps(level.getGameTime());
        }
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                            final CableBlockEntity cable, final Direction mountedFace) {
        return CraftingRouterMenu.create(containerId, inventory, cable, mountedFace);
    }

    @Override
    public ItemStack partItem() {
        return new ItemStack(ComputingModule.CRAFTING_ROUTER_ITEM.get());
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.save(tag, registries);
        tag.putUUID("Id", id);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.load(tag, registries);
        if (tag.hasUUID("Id")) {
            id = tag.getUUID("Id");
        }
    }
}
