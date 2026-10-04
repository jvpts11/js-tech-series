/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.computers.storage.IDataSink;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.ValueField;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The network's side of a computer's personal-use cards: where an item an UPDATE took out of the network waits while
 * a card works on it, and where what the network then had no room for waits for space on the card.
 *
 * <p>Its slots belong to no inventory anything else can reach: they are no capability, no query of the network sees
 * them and no craft reads them. They are saved with the computer and fall out of it when it is broken. One UPDATE
 * holds the drawer at a time, and nothing it held outlives it but what it left at the card's output, so a drawer
 * found holding work with no UPDATE behind it (the world was closed while one ran) hands that work to the output.
 *
 * <p>The card's output is the card's own slot in the Workshop, where the player takes things from it anyway: the
 * furnace's output, the enchanting slot, and the anvil's two. What that slot cannot take yet waits here, and moves
 * there as soon as it can.
 */
public final class UpdateDrawer {

    private final FieldItemHandler slots;
    private final ValueField<Integer> progress;
    private final ValueField<Integer> card;
    /* The UPDATE holding the drawer, which no save keeps: an UPDATE does not outlive the world being closed. */
    @Nullable
    private UUID holder;
    private boolean smelting;

    /** The item the network handed the card. */
    public static final int WORK = 0;
    /** The second item: what a repair mends with, or what a combine adds. */
    public static final int SECOND = 1;
    /** What the card made, on its way back to the network. */
    public static final int MADE = 2;
    /** The first of the slots where what the network had no room for waits, one per slot of the card's output. */
    public static final int OUT = 3;
    public static final int SLOTS = 7;
    /** The card's own slots in the Workshop that the waiting slots move into, in their order. */
    private static final int[] STATIONS = {Workshop.FURNACE_OUT, Workshop.ENCHANT_ITEM, Workshop.ANVIL_LEFT,
            Workshop.ANVIL_RIGHT};
    private static final int NO_CARD = -1;

    public UpdateDrawer(final FieldItemHandler slots, final ValueField<Integer> progress,
                        final ValueField<Integer> card) {
        this.slots = slots;
        this.progress = progress;
        this.card = card;
    }

    /** The item in slot {@code slot}. */
    public ItemStack slot(final int slot) {
        return slots.getStackInSlot(slot);
    }

    /** Puts {@code stack} in slot {@code slot} as it is. */
    public void put(final int slot, final ItemStack stack) {
        slots.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    /** Takes everything out of slot {@code slot}. */
    public ItemStack take(final int slot) {
        final ItemStack stack = slots.getStackInSlot(slot).copy();
        slots.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    /** Whether no UPDATE holds the drawer and it holds no work. */
    public boolean free() {
        return holder == null && slot(WORK).isEmpty() && slot(SECOND).isEmpty() && slot(MADE).isEmpty();
    }

    /** Whether the UPDATE {@code operation} holds the drawer. */
    public boolean heldBy(final UUID operation) {
        return operation.equals(holder);
    }

    /** The UPDATE {@code operation} takes the drawer for {@code worker}; with {@code smelt}, to the furnace. */
    public void hold(final UUID operation, final WorkshopCard worker, final boolean smelt) {
        holder = operation;
        card.set(worker.index());
        smelting = smelt;
        progress.set(0);
    }

    /** The UPDATE {@code operation} lets the drawer go; nothing when it does not hold it. */
    public void release(final UUID operation) {
        if (heldBy(operation)) {
            holder = null;
            smelting = false;
            progress.set(0);
            card.set(NO_CARD);
        }
    }

    /** Whether the network's smelting has the card's furnace now; the Workshop's own waits behind it. */
    public boolean smelting() {
        return holder != null && smelting && !slot(WORK).isEmpty();
    }

    /** How far the network's smelting is over the item now in the furnace, in ticks. */
    public int progressTicks() {
        final Integer ticks = progress.get();
        return ticks == null ? 0 : ticks;
    }

    /**
     * Where the network puts what it hands the card into slot {@code slot}: only items, and no more than a stack of
     * the same item.
     */
    public IDataSink sink(final int slot) {
        return (key, amount, simulate) -> {
            if (!key.isItem() || amount <= 0L) {
                return 0L;
            }
            final ItemStack here = slots.getStackInSlot(slot);
            final ItemStack one = key.stack(1);
            if (!here.isEmpty() && !ItemStack.isSameItemSameComponents(here, one)) {
                return 0L;
            }
            final int put = (int) Math.min(one.getMaxStackSize() - here.getCount(), amount);
            if (put <= 0) {
                return 0L;
            }
            if (!simulate) {
                slots.setStackInSlot(slot, here.isEmpty() ? key.stack(put) : here.copyWithCount(here.getCount() + put));
            }
            return put;
        };
    }

    /**
     * One tick of the network's smelting at {@code speed} times a furnace's pace, at the same rules as the Workshop's:
     * the item handed to the card smelts into what it made while there is room for it, and the experience it earns
     * goes to the card's furnace, for whoever next takes its output. Returns how many items were smelted this tick.
     */
    public int tickSmelt(final ServerLevel level, final int speed, final Workshop workshop) {
        if (!smelting() || speed <= 0) {
            return 0;
        }
        final ItemStack input = slot(WORK);
        final Workshop.Smelt smelt = Workshop.smelt(level, input);
        if (smelt == null) {
            return 0;
        }
        final ItemStack made = slot(MADE);
        if (!made.isEmpty() && (!ItemStack.isSameItemSameComponents(made, smelt.result())
                || made.getCount() + smelt.result().getCount() > made.getMaxStackSize())) {
            return 0;
        }
        final int now = progressTicks() + 1;
        if (now < WorkshopRates.ticksPerItem(smelt.cookTicks(), speed)) {
            progress.set(now);
            return 0;
        }
        progress.set(0);
        put(WORK, input.copyWithCount(input.getCount() - 1));
        put(MADE, made.isEmpty() ? smelt.result().copy() : made.copyWithCount(made.getCount()
                + smelt.result().getCount()));
        workshop.addExperience(smelt.experience());
        return 1;
    }

    /**
     * Leaves {@code stack} at {@code worker}'s output: on the card's own slot in the Workshop as far as it takes it,
     * the rest waiting here for it. {@code second} names the anvil's second slot, for what is left of a repair's
     * material. Gives back what found no place at all, for the computer to drop.
     */
    public ItemStack park(final Workshop workshop, final WorkshopCard worker, final boolean second,
                          final ItemStack stack) {
        final int station = stationOf(worker, second);
        if (station < 0 || stack.isEmpty()) {
            return stack;
        }
        final ItemStack left = workshop.offer(STATIONS[station], stack);
        if (left.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final int out = OUT + station;
        final ItemStack waiting = slot(out);
        if (waiting.isEmpty()) {
            put(out, left);
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(waiting, left)) {
            return left;
        }
        final int fits = Math.min(left.getCount(), waiting.getMaxStackSize() - waiting.getCount());
        put(out, waiting.copyWithCount(waiting.getCount() + fits));
        return fits == left.getCount() ? ItemStack.EMPTY : left.copyWithCount(left.getCount() - fits);
    }

    /** Moves what waits at the cards' output onto their own slots in the Workshop as those free up. */
    public void tickOut(final Workshop workshop) {
        for (int i = 0; i < STATIONS.length; i++) {
            final ItemStack waiting = slot(OUT + i);
            if (!waiting.isEmpty()) {
                put(OUT + i, workshop.offer(STATIONS[i], waiting));
            }
        }
    }

    /**
     * Hands work no UPDATE holds any more to the card's output, as when the world was closed while one ran; gives back
     * what found no place, for the computer to drop.
     */
    public List<ItemStack> recover(final Workshop workshop) {
        final List<ItemStack> lost = new ArrayList<>();
        if (holder != null || slot(WORK).isEmpty() && slot(SECOND).isEmpty() && slot(MADE).isEmpty()) {
            return lost;
        }
        final Integer index = card.get();
        final WorkshopCard worker = index == null ? null : WorkshopCard.byIndex(index);
        for (final int slot : new int[] {WORK, MADE, SECOND}) {
            final ItemStack stack = take(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final ItemStack left = worker == null ? stack : park(workshop, worker, slot == SECOND, stack);
            if (!left.isEmpty()) {
                lost.add(left);
            }
        }
        card.set(NO_CARD);
        return lost;
    }

    /** Everything the drawer holds, taken out of it, for the computer's drops. */
    public List<ItemStack> drops() {
        final List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            final ItemStack stack = take(i);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    /* The waiting slot, from zero, for {@code worker}'s output; -1 for a card with none. */
    private static int stationOf(final WorkshopCard worker, final boolean second) {
        return switch (worker) {
            case FURNACE -> 0;
            case ENCHANTING -> 1;
            case ANVIL -> second ? 3 : 2;
            case CRAFTING_TABLE -> -1;
        };
    }
}
