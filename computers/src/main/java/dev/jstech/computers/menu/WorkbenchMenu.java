/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.WorkbenchBlock;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.gui.layout.WorkbenchLayout;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.MenuValue;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.menu.SyncedValue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An Electronics Workbench's window: its nine trays, the tray the finished part waits in, the player's inventory, and
 * what its screen shows of the work: how far the part under way has come and which part it is, whether energy reaches
 * the bench and how full its store is. Any era's bench opens it, so it stays open while the player stands at a bench.
 */
public final class WorkbenchMenu extends CoreMenu {

    private final Era era;
    private final MenuValue progress;
    private final MenuValue stored;
    private final MenuValue powered;
    private final SyncedValue<ItemStack> underWay;

    /* A share from 0 to 1 travels as thousandths. */
    private static final int WHOLE = 1_000;

    public WorkbenchMenu(final int containerId, final Inventory inventory, final WorkbenchBlockEntity bench) {
        super(ComputingMenus.WORKBENCH_MENU.get(), containerId, inventory,
                MenuValidity.block(bench.getLevel(), bench.getBlockPos(), WorkbenchBlock.class));
        this.era = bench.era();
        final GuiLayout layout = WorkbenchLayout.layout();
        final Slot[] trays = new Slot[WorkbenchBlockEntity.TRAYS];
        for (int tray = 0; tray < trays.length; tray++) {
            trays[tray] = slot(bench.trays(), tray, layout.slotAt("tray" + tray));
        }
        final SlotGroup trayGroup = slots(trays);
        final SlotGroup part = slots(outputSlot(bench.output(), 0, layout.slotAt("part")));
        final PlayerSlots player = playerInventory(inventory, layout.playerInventoryAt());
        shiftClick(part, player.all());
        shiftClick(trayGroup, player.all());
        shiftClick(player.all(), trayGroup);
        this.progress = value(() -> (int) Math.round(bench.progress() * WHOLE));
        this.stored = value(() -> (int) Math.round(bench.stored() * WHOLE));
        this.powered = flag(bench::powered);
        this.underWay = synced("under_way", ItemStack.OPTIONAL_STREAM_CODEC, ItemStack.EMPTY, bench::partUnderWay);
    }

    public WorkbenchMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, WorkbenchBlockEntity.class));
    }

    /** The era of the bench, whose look its screen takes. */
    public Era era() {
        return era;
    }

    /** How far the part under way has come, from 0 to 1. */
    public double progress() {
        return (double) progress.get() / WHOLE;
    }

    /** How full the bench's store of energy is, from 0 to 1. */
    public double stored() {
        return (double) stored.get() / WHOLE;
    }

    /** Whether energy reaches the bench, as its lamp shows. */
    public boolean powered() {
        return powered.isSet();
    }

    /** The part under way, as its recipe makes it, or an empty stack. */
    public ItemStack underWay() {
        return underWay.get();
    }
}
