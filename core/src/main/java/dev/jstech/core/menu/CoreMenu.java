/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.util.DeclarationError;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A menu declared once. Its slots are added in named groups, placed from the screen's layout; where a shift-click in
 * each group sends the stack is declared as a route; the buttons it answers are declared with what they do; and it
 * stays open while its validity holds. The shift-click, the buttons and the validity check are this class's.
 */
public abstract class CoreMenu extends AbstractContainerMenu {

    private final Predicate<Player> validity;
    private final Player player;
    private final boolean onClient;
    private final Map<SlotGroup, List<SlotGroup>> shiftClicks = new LinkedHashMap<>();
    private final Map<Integer, Consumer<Player>> buttons = new HashMap<>();
    private final List<SyncedValue<?>> synced = new ArrayList<>();
    private final Map<String, Supplier<?>> bound = new LinkedHashMap<>();

    /** The gap between the top of the inventory grid and the hotbar, as the game lays it out. */
    private static final int HOTBAR_GAP = 58;
    /** The distance from one slot to the next. */
    private static final int PITCH = 18;

    /**
     * A menu of {@code type} for the player whose {@code inventory} it is, open while {@code validity} holds for them.
     * The inventory tells the menu whether it is the server's or the client's copy.
     */
    protected CoreMenu(final MenuType<?> type, final int containerId, final Inventory inventory,
                       final Predicate<Player> validity) {
        super(type, containerId);
        this.validity = validity;
        this.player = inventory.player;
        this.onClient = inventory.player.level().isClientSide();
    }

    /**
     * The value bound to {@code name}, declared with {@link #value(String, IntSupplier)}, {@link #flag(String,
     * BooleanSupplier)} or {@link #synced}: what a screen's file reads when a component says {@code "bind": name}.
     */
    public Optional<Supplier<?>> bound(final String name) {
        return Optional.ofNullable(bound.get(name));
    }

    /** The names values are bound under, in the order they were declared. */
    public List<String> boundNames() {
        return List.copyOf(bound.keySet());
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        sendSynced(false);
    }

    @Override
    public void broadcastFullState() {
        super.broadcastFullState();
        sendSynced(true);
    }

    @Override
    public void sendAllDataToRemote() {
        super.sendAllDataToRemote();
        sendSynced(true);
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
        final Map.Entry<SlotGroup, List<SlotGroup>> route = routeFrom(index);
        if (route == null) {
            return ItemStack.EMPTY;
        }
        final boolean fromPlayer = route.getKey().playerSide();
        final ItemStack stack = slot.getItem();
        final ItemStack original = stack.copy();
        for (final SlotGroup to : route.getValue()) {
            /*
             * From a machine into the player's inventory the game fills from the end, the hotbar first; between the
             * player's own groups it fills from the start, as the game's own menus do.
             */
            moveItemStackTo(stack, to.start(), to.end(), to.playerSide() && !fromPlayer);
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
        return playerInventory(inventory, at, () -> true);
    }

    /**
     * The same, shown only while {@code shown} says so: a screen with pages, only some of which have the inventory
     * under them, hides it on the others, where its slots are neither drawn nor clicked.
     */
    protected final PlayerSlots playerInventory(final Inventory inventory, final GuiLayout.SlotPosition at,
                                                final BooleanSupplier shown) {
        final int mainStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(inventorySlot(inventory, col + row * 9 + 9, at.x() + col * PITCH, at.y() + row * PITCH,
                        shown));
            }
        }
        final int hotbarStart = slots.size();
        for (int col = 0; col < 9; col++) {
            addSlot(inventorySlot(inventory, col, at.x() + col * PITCH, at.y() + HOTBAR_GAP, shown));
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

    /**
     * Declares an int the menu shows that the server works out with {@code onServer}, carried to the client's copy
     * of the menu as it changes. On the client {@code onServer} is never asked, so it may read things only the server
     * has.
     */
    protected final MenuValue value(final IntSupplier onServer) {
        final MenuValue value = new MenuValue(onServer, onClient);
        addDataSlot(value);
        return value;
    }

    /** The same, for a yes or no, carried as 1 or 0. */
    protected final MenuValue flag(final BooleanSupplier onServer) {
        return value(() -> onServer.getAsBoolean() ? 1 : 0);
    }

    /** An int as {@link #value(IntSupplier)} declares one, bound to {@code name} for a screen's file to read. */
    protected final MenuValue value(final String name, final IntSupplier onServer) {
        final MenuValue value = value(onServer);
        bind(name, value::get);
        return value;
    }

    /** A yes or no as {@link #flag(BooleanSupplier)} declares one, bound to {@code name}. */
    protected final MenuValue flag(final String name, final BooleanSupplier onServer) {
        final MenuValue value = flag(onServer);
        bind(name, value::isSet);
        return value;
    }

    /**
     * Declares a value of any kind the menu shows, bound to {@code name}: worked out on the server with
     * {@code onServer}, written by {@code codec}, and carried to the client's copy of the menu whenever it changes,
     * {@code initial} until it first arrives. No packet is written for it: every synced value of every menu travels in
     * one payload of the Core's.
     */
    protected final <T> SyncedValue<T> synced(final String name,
                                              final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                              final T initial, final Supplier<T> onServer) {
        final SyncedValue<T> value = new SyncedValue<>(name, codec, initial, onServer, onClient);
        synced.add(value);
        bind(name, value::get);
        return value;
    }

    /** Whether this is the client's copy of the menu. */
    protected final boolean onClient() {
        return onClient;
    }

    /* Writes the synced values into the client's copy of the menu: those that changed, or all of them. */
    void receive(final MenuSyncPayload payload, final RegistryAccess access) {
        for (final MenuSyncPayload.Entry entry : payload.entries()) {
            if (entry.index() >= 0 && entry.index() < synced.size()) {
                synced.get(entry.index()).receive(entry.data(), access);
            }
        }
    }

    private void bind(final String name, final Supplier<?> reader) {
        if (bound.putIfAbsent(name, reader) != null) {
            throw DeclarationError.of(null, "the menu value " + name + " of " + getClass().getSimpleName(),
                    "is bound twice", "give each value the menu shows a name of its own");
        }
    }

    /* Sends the client the synced values that changed since the last time, or all of them when {@code all}. */
    private void sendSynced(final boolean all) {
        if (synced.isEmpty() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        final List<MenuSyncPayload.Entry> entries = new ArrayList<>();
        for (int i = 0; i < synced.size(); i++) {
            final byte[] data = synced.get(i).pending(serverPlayer.registryAccess(), all);
            if (data != null) {
                entries.add(new MenuSyncPayload.Entry(i, data));
            }
        }
        if (!entries.isEmpty()) {
            PacketDistributor.sendToPlayer(serverPlayer, new MenuSyncPayload(containerId, entries));
        }
    }

    /* The group the slot belongs to, with the route declared for it; null when its group has none. */
    private Map.Entry<SlotGroup, List<SlotGroup>> routeFrom(final int index) {
        for (final Map.Entry<SlotGroup, List<SlotGroup>> entry : shiftClicks.entrySet()) {
            if (entry.getKey().contains(index)) {
                return entry;
            }
        }
        return null;
    }

    /* A slot of the player's inventory that is there only while {@code shown} says so. */
    private static Slot inventorySlot(final Inventory inventory, final int index, final int x, final int y,
                                      final BooleanSupplier shown) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean isActive() {
                return shown.getAsBoolean();
            }
        };
    }
}
