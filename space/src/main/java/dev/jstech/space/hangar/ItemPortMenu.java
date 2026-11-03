/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.menu.SyncedValue;
import dev.jstech.space.gui.ItemPortLayout;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The Item Port's menu: its hold's eighteen places where its screen draws them, each taking only a block the ship
 * its dock builds still lacks, and the player's inventory; and what the build still lacks, sent to its screen as it
 * changes. A shift-click from the inventory puts a block in the hold, from the hold back into the inventory.
 */
public final class ItemPortMenu extends CoreMenu {

    private final SyncedValue<ItemPortView> view;

    /** On the server, for {@code port}. */
    public ItemPortMenu(final int id, final Inventory inventory, final ItemPortBlockEntity port) {
        this(id, inventory, port, port.hold());
    }

    /** On a player's game, as the server opened it. */
    public ItemPortMenu(final int id, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(id, inventory, MenuOpening.blockEntity(inventory, buf, ItemPortBlockEntity.class));
    }

    private ItemPortMenu(final int id, final Inventory inventory, final ItemPortBlockEntity port,
                         final IItemHandler hold) {
        super(HangarBlocks.ITEM_PORT_MENU.get(), id, inventory, MenuValidity.blockEntity(port));
        this.view = synced("view", ItemPortView.STREAM_CODEC, ItemPortView.NONE, port::view);
        // On a player's game the hold's places take what the build lacks as the server last said.
        final IItemHandler places = onClient() ? new ClientHold(this) : hold;
        final Slot[] slots = new Slot[ItemPortBlockEntity.PLACES];
        for (int k = 0; k < slots.length; k++) {
            final int[] at = ItemPortLayout.place(k);
            slots[k] = new SlotItemHandler(places, k, at[0], at[1]);
        }
        final SlotGroup held = slots(slots);
        final PlayerSlots player = playerInventory(inventory, new GuiLayout.SlotPosition(ItemPortLayout.INVENTORY_X,
                ItemPortLayout.INVENTORY_Y));
        shiftClick(held, player.main(), player.hotbar());
        shiftClick(player.main(), held, player.hotbar());
        shiftClick(player.hotbar(), held, player.main());
    }

    /** What the build still lacks, as the server last said. */
    public ItemPortView view() {
        return this.view.get();
    }

    /* The hold as a player's game keeps it while its screen is open: its places take what the view says it lacks. */
    private static final class ClientHold extends ItemStackHandler {

        private final ItemPortMenu menu;

        private ClientHold(final ItemPortMenu menu) {
            super(ItemPortBlockEntity.PLACES);
            this.menu = menu;
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            return !stack.isEmpty() && this.menu.view().takes(stack.getItem());
        }
    }
}
