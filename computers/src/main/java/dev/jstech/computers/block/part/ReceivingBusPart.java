/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.menu.ReceivingBusMenu;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * A Receiving Bus part: marks the machine face that crafting operations collect finished outputs from, the
 * way to drive a sided machine whose output face differs from the face its Crafting Switch touches. It shares
 * the Import Bus's identity plumbing (menu, filters) but is passive: the crafting engine pulls through it, it
 * never transfers on its own.
 */
public final class ReceivingBusPart extends ImportBusPart {

    /* One design for every era: it only marks a face, and keeps the filter that routes it. */
    public ReceivingBusPart() {
        super(HardwareEra.STANDARD);
    }

    @Override
    public PartType<?> type() {
        return ComputingParts.RECEIVING.get();
    }

    @Override
    public void serverTick() {
        /*
         * Passive: autonomous pulling would steal a craft's outputs into the bus buffer while the operation
         * is trying to collect and account for them. Only its lamps go out after the engine's last pull.
         */
        final ServerLevel level = serverLevel();
        if (level != null) {
            settleLamps(level.getGameTime());
        }
    }

    @Override
    public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory,
                                            final CableBlockEntity cable, final Direction mountedFace) {
        return ReceivingBusMenu.create(containerId, inventory, cable, mountedFace);
    }

    @Override
    public ItemStack partItem() {
        return new ItemStack(ComputingModule.RECEIVING_BUS_ITEM.get());
    }
}
