/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.blockentity.DataCableBlockEntity;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.MenuValue;
import dev.jstech.core.menu.PlayerSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Shared menu for the four bus parts (Import, Export, Input, Receiving): a single ghost filter slot, the
 * min/max stock steppers, the mode toggle, and the player inventory. Every bus exposes an identical
 * configuration surface, so the slot wiring, the stepper buttons, the synced values and the shift-click
 * transfer all live here; the subclasses only register their own {@link MenuType} and the create/fromNetwork
 * factories. The bus name rides in the open packet and is edited back to the server by a dedicated payload,
 * not through the synced values (which are ints).
 */
public abstract class AbstractBusMenu extends CoreMenu {

    private final AbstractBusPart part;
    private final BlockPos cablePos;
    private final Direction face;
    private final MenuValue minValue;
    private final MenuValue maxValue;
    private final MenuValue modeValue;
    private final MenuValue linkedFlag;
    private String busName;

    public static final int FILTER_SLOT = 0;

    // Stepper button ids: field + direction + step.
    public static final int BTN_MIN_DOWN1 = 0;
    public static final int BTN_MIN_UP1 = 1;
    public static final int BTN_MIN_DOWN16 = 2;
    public static final int BTN_MIN_UP16 = 3;
    public static final int BTN_MAX_DOWN1 = 4;
    public static final int BTN_MAX_UP1 = 5;
    public static final int BTN_MAX_DOWN16 = 6;
    public static final int BTN_MAX_UP16 = 7;
    public static final int BTN_MODE = 8;

    /** How far a player may stand from the cable and keep this menu open, in blocks. */
    private static final double REACH_BLOCKS = 8.0;

    protected AbstractBusMenu(final MenuType<?> type, final int containerId, final Inventory playerInventory,
                              final AbstractBusPart part, final Level level, final BlockPos cablePos,
                              final Direction face, final String busName) {
        super(type, containerId, playerInventory, validity(level, cablePos, face, part));
        this.part = part;
        this.cablePos = cablePos;
        this.face = face;
        this.busName = busName == null ? "" : busName;

        final GuiLayout layout = BusLayout.layout();
        slots(filterSlot(part.getFilterHandler(), layout.slotAt("filterSlot"), this::filterApplies));
        final PlayerSlots playerSlots = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(playerSlots.main(), playerSlots.hotbar());
        shiftClick(playerSlots.hotbar(), playerSlots.main());

        this.minValue = value(part::min);
        this.maxValue = value(part::max);
        this.modeValue = value(part::mode);
        this.linkedFlag = flag(part::linked);

        /*
         * The passive crafting buses (Input, Receiving) have no stock window, so these ids are registered only
         * where one exists: on a passive bus the server refuses them (clickMenuButton answers false) instead of
         * accepting a press that does nothing.
         */
        if (stockControlsApply()) {
            button(BTN_MIN_DOWN1, player -> part.adjustMin(-1));
            button(BTN_MIN_UP1, player -> part.adjustMin(1));
            button(BTN_MIN_DOWN16, player -> part.adjustMin(-16));
            button(BTN_MIN_UP16, player -> part.adjustMin(16));
            button(BTN_MAX_DOWN1, player -> part.adjustMax(-1));
            button(BTN_MAX_UP1, player -> part.adjustMax(1));
            button(BTN_MAX_DOWN16, player -> part.adjustMax(-16));
            button(BTN_MAX_UP16, player -> part.adjustMax(16));
            button(BTN_MODE, player -> part.toggleMode());
        }
    }

    /**
     * Whether this bus exposes a filter. Every bus does: on the autonomous Import/Export buses it selects what
     * to move, and on the passive crafting Input/Receiving buses it pins what the mounted face carries so the
     * crafting engine can route each ingredient (or each output) to the correct face. An empty filter means the
     * face carries anything, matching the raw machine face.
     */
    public boolean filterApplies() {
        return true;
    }

    /**
     * Whether the stock controls (min/max window and continuous/redstone mode) apply to this bus. Only the
     * autonomous Import and Export buses hold stock; the crafting Input and Receiving buses are demand-driven by
     * the crafting engine, so those controls hide and their buttons are refused server-side.
     */
    public boolean stockControlsApply() {
        return !ComputingParts.isCrafting(part.type());
    }

    public int min() {
        return minValue.get();
    }

    public int max() {
        return maxValue.get();
    }

    public int mode() {
        return modeValue.get();
    }

    public boolean linked() {
        return linkedFlag.isSet();
    }

    public ItemStack filterStack() {
        return getSlot(FILTER_SLOT).getItem();
    }

    public String busName() {
        return busName;
    }

    public BlockPos cablePos() {
        return cablePos;
    }

    public Direction face() {
        return face;
    }

    /** Updates the locally-known name after the server confirms an edit, so the field stays in sync. */
    public void setBusNameLocal(final String name) {
        this.busName = name == null ? "" : name;
    }

    @Override
    public void clicked(final int slotId, final int button, final ClickType type, final Player player) {
        /*
         * Clicking the filter slot sets it from the carried item (a copy), or clears
         * it with an empty cursor, so the player's item is never consumed.
         */
        if (slotId == FILTER_SLOT) {
            if (filterApplies()) {
                part.setFilter(getCarried());
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    /**
     * A slot the player only ever sets by clicking (never by dropping an item in), shown only while
     * {@code active} says this bus carries a filter.
     */
    private static Slot filterSlot(final ItemStackHandler handler, final GuiLayout.SlotPosition at,
                                   final BooleanSupplier active) {
        return new SlotItemHandler(handler, 0, at.x(), at.y()) {
            @Override
            public boolean mayPlace(final ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(final Player player) {
                return false;
            }

            @Override
            public boolean isActive() {
                return active.getAsBoolean();
            }
        };
    }

    /**
     * Valid while the cable at {@code cablePos} still stands within reach and the part this menu was opened on is
     * still mounted on {@code face} of it: a part swapped out (or picked off) from under an open menu closes it,
     * rather than going on editing a bus that is no longer there.
     */
    private static Predicate<Player> validity(final Level level, final BlockPos cablePos, final Direction face,
                                              final AbstractBusPart part) {
        return MenuValidity.near(level, cablePos, REACH_BLOCKS)
                .and(player -> level.getBlockState(cablePos).getBlock() instanceof DataCableBlock)
                .and(player -> level.getBlockEntity(cablePos) instanceof DataCableBlockEntity cable
                        && cable.getPart(face) == part);
    }
}
