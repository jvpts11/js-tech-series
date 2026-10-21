/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.text.GameText;
import dev.jstech.core.worn.WornItems;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Keys that work an item wherever its player carries it, not only in the hand: a jetpack turned on while worn, a
 * magnet switched in the inventory. The press reaches the server, which finds the first stack the action takes, in
 * the hands, then in what is worn (armour, and the slots of Curios or Accessories), then in the inventory, and hands
 * it to the action:
 *
 * <pre>{@code
 * KeyAction MAGNET = ItemActions.declare(KeyAction.builder(id("magnet"), MAGNET_KEY),
 *         stack -> stack.is(MAGNET_ITEM), (player, stack, shift) -> ItemActions.toggle(player, stack));
 * }</pre>
 *
 * <p>The stack handed over is the carried one itself, so what the action writes into its components stays with it,
 * wherever it is carried. An item that turns on and off keeps it in {@link ItemStates#ON}; its tooltip says which.
 */
public final class ItemActions {

    private ItemActions() {
    }

    /**
     * Declares {@code key} as an action of the items {@code which} takes, done by {@code press} with the first of them
     * its player carries; a press while carrying none does nothing.
     */
    public static KeyAction declare(final KeyAction.Builder key, final Predicate<ItemStack> which,
                                    final IItemPress press) {
        Objects.requireNonNull(which, "which");
        Objects.requireNonNull(press, "press");
        return KeyActions.declare(key.onServer((player, shift) -> find(player, which)
                .ifPresent(stack -> press.pressed(player, stack, shift))));
    }

    /** The first stack {@code player} carries that {@code which} takes: in a hand, worn, then in the inventory. */
    public static Optional<ItemStack> find(final ServerPlayer player, final Predicate<ItemStack> which) {
        for (final ItemStack held : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (!held.isEmpty() && which.test(held)) {
                return Optional.of(held);
            }
        }
        final Optional<ItemStack> worn = WornItems.firstWorn(player, which);
        if (worn.isPresent()) {
            return worn;
        }
        for (final ItemStack carried : player.getInventory().items) {
            if (!carried.isEmpty() && which.test(carried)) {
                return Optional.of(carried);
            }
        }
        return Optional.empty();
    }

    /** Whether {@code stack} is on; an item never turned is off. */
    public static boolean isOn(final ItemStack stack) {
        return stack.getOrDefault(ItemStates.ON.get(), false);
    }

    /** Turns {@code stack} on or off. */
    public static void setOn(final ItemStack stack, final boolean on) {
        stack.set(ItemStates.ON.get(), on);
    }

    /** Turns {@code stack} the other way and tells {@code player} on the action bar; gives whether it is on now. */
    public static boolean toggle(final ServerPlayer player, final ItemStack stack) {
        final boolean on = !isOn(stack);
        setOn(stack, on);
        player.displayClientMessage(GameText.component(ItemTexts.SWITCHED.with(GameText.of(stack.getHoverName()),
                on ? ItemTexts.ON.text() : ItemTexts.OFF.text())), true);
        return on;
    }

    /** What an action does with the stack it found. */
    @FunctionalInterface
    public interface IItemPress {

        /**
         * @param player who pressed the key
         * @param stack  the stack found, the carried one itself
         * @param shift  whether shift was held
         */
        void pressed(ServerPlayer player, ItemStack stack, boolean shift);
    }
}
