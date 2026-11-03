/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.space.gui.SuitLockerLayout;
import java.util.function.Predicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The suit locker's menu: its nine places where its screen draws them, each taking only its own thing, one of it but
 * in the kit box, and the player's inventory; a shift-click from the inventory puts a thing in its place, from the
 * locker back into the inventory.
 */
public final class SuitLockerMenu extends CoreMenu {

    private final Container locker;

    /** On the server, for {@code locker}. */
    public SuitLockerMenu(final int id, final Inventory inventory, final SuitLockerEntity locker) {
        this(id, inventory, locker, locker::stillValid);
    }

    /** On a player's game, as the server opened it. */
    public SuitLockerMenu(final int id, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(SuitLockerEntity.PLACES.size()) {
            @Override
            public boolean canPlaceItem(final int slot, final ItemStack stack) {
                return SuitLockerEntity.PLACES.get(slot).takes(inventory.player.level(), stack);
            }
        }, player -> true);
    }

    private SuitLockerMenu(final int id, final Inventory inventory, final Container locker,
                           final Predicate<Player> validity) {
        super(ShipKitBlocks.SUIT_LOCKER_MENU.get(), id, inventory, validity);
        this.locker = locker;
        final Slot[] places = new Slot[SuitLockerEntity.PLACES.size()];
        for (int k = 0; k < places.length; k++) {
            final int[] at = SuitLockerLayout.PLACES.get(k);
            places[k] = new LockerSlot(locker, k, at[0], at[1]);
        }
        final SlotGroup held = slots(places);
        final PlayerSlots player = playerInventory(inventory, new GuiLayout.SlotPosition(SuitLockerLayout.INVENTORY_X,
                SuitLockerLayout.INVENTORY_Y));
        shiftClick(held, player.main(), player.hotbar());
        shiftClick(player.main(), held, player.hotbar());
        shiftClick(player.hotbar(), held, player.main());
        locker.startOpen(inventory.player);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        this.locker.stopOpen(player);
    }

    /** What the place numbered {@code slot} takes, for its screen's hint. */
    public SuitLockerEntity.Place place(final int slot) {
        return SuitLockerEntity.PLACES.get(slot);
    }

    /* A place of the locker: it takes only its own thing, one of it but in the kit box. */
    private static final class LockerSlot extends Slot {

        LockerSlot(final Container locker, final int slot, final int x, final int y) {
            super(locker, slot, x, y);
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return this.container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return SuitLockerEntity.PLACES.get(getContainerSlot()) == SuitLockerEntity.Place.KIT
                    ? super.getMaxStackSize() : 1;
        }
    }
}
