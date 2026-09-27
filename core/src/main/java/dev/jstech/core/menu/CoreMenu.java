/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import dev.jstech.core.gui.layout.GuiLayout;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A menu declared once. Its slots are added in named groups, placed from the screen's layout; where a shift-click in
 * each group sends the stack is declared as a route; the buttons it answers are declared with what they do; and it
 * stays open while its validity holds. The shift-click, the buttons and the validity check are this class's.
 */
public abstract class CoreMenu extends AbstractContainerMenu {

    private final Predicate<Player> validity;
    private final Map<SlotGroup, List<SlotGroup>> shiftClicks = new LinkedHashMap<>();
    private final Map<Integer, Consumer<Player>> buttons = new HashMap<>();

    /** The gap between the top of the inventory grid and the hotbar, as the game lays it out. */
    private static final int HOTBAR_GAP = 58;
    /** The distance from one slot to the next. */
    private static final int PITCH = 18;

    protected CoreMenu(final MenuType<?> type, final int containerId, final Predicate<Player> validity) {
        super(type, containerId);
        this.validity = validity;
    }

    @Override
    public boolean stillValid(final Player player) {
        return validity.test(player);
    }

    /**
     * Sends the shift-clicked stack along the route declared for the slot's group, to each group of the route in
     * turn until it is all placed; a slot in a group with no route moves nothing.
     */
    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        final List<SlotGroup> route = routeFrom(index);
        if (route == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        for (final SlotGroup to : route) {
            moveItemStackTo(stack, to.start(), to.end(), to.playerSide());
            if (stack.isEmpty()) {
                break;
            }
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean clickMenuButton(final Player player, final int id) {
        final Consumer<Player> action = buttons.get(id);
        if (action == null) {
            return false;
        }
        action.accept(player);
        return true;
    }

    /** A slot of {@code handler} at a layout's position. */
    protected static Slot slot(final IItemHandler handler, final int index, final GuiLayout.SlotPosition at) {
        return new SlotItemHandler(handler, index, at.x(), at.y());
    }

    /** A slot the player only takes from, such as a machine's output. */
    protected static Slot outputSlot(final IItemHandler handler, final int index, final GuiLayout.SlotPosition at) {
        return new SlotItemHandler(handler, index, at.x(), at.y()) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return false;
            }
        };
    }

    /** Adds these slots, one after the other, as the menu's next group. */
    protected final SlotGroup slots(final Slot... added) {
        final int start = slots.size();
        for (final Slot slot : added) {
            addSlot(slot);
        }
        return new SlotGroup(start, slots.size(), false);
    }

    /** Adds the player's inventory with the top-left of its grid at {@code at}, the hotbar below it. */
    protected final PlayerSlots playerInventory(final Inventory inventory, final GuiLayout.SlotPosition at) {
        final int mainStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, at.x() + col * PITCH, at.y() + row * PITCH));
            }
        }
        final int hotbarStart = slots.size();
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, at.x() + col * PITCH, at.y() + HOTBAR_GAP));
        }
        return new PlayerSlots(new SlotGroup(mainStart, hotbarStart, true),
                new SlotGroup(hotbarStart, slots.size(), true));
    }

    /** Declares where a shift-click in {@code from} sends the stack: to {@code to}, in that order. */
    protected final void shiftClick(final SlotGroup from, final SlotGroup... to) {
        shiftClicks.put(from, List.of(to));
    }

    /** Carries {@code data}, such as the block entity's {@code fields().menuData()}, to the client's menu. */
    protected final void data(final ContainerData data) {
        addDataSlots(data);
    }

    /** Declares what the button of that id does when the player presses it. */
    protected final void button(final int id, final Consumer<Player> action) {
        buttons.put(id, action);
    }

    private List<SlotGroup> routeFrom(final int index) {
        for (final Map.Entry<SlotGroup, List<SlotGroup>> entry : shiftClicks.entrySet()) {
            if (entry.getKey().contains(index)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
