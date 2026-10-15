/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * A recipe moved into a processing machine from a player's inventory, as a recipe viewer's transfer button asks: the
 * recipe picked among those its inputs make, and the first lane's input slots filled with its ingredients, each in the
 * slot of its place in the recipe, its catalysts after them, once or as many sets as a stack holds. What the slots held
 * that the recipe does not take goes back to the player; nothing is filled for a player the machine does not let set
 * it up, nor for a recipe of a kind the machine does not work.
 */
public final class MachineFill {

    private MachineFill() {
    }

    /**
     * Whether {@code machine} works the recipes of {@code kind}: its block is one of the kind's machines, named by
     * the block itself or by its item; a block with no item of its own is told apart by the block alone.
     */
    public static boolean works(final MachineBlockEntity machine, final ProcessingKind kind) {
        final Block block = machine.getBlockState().getBlock();
        final Item own = block.asItem();
        for (final ItemLike each : kind.machines()) {
            final boolean same = each instanceof DeferredBlock<?> declared ? declared.get() == block
                    : each instanceof Block named ? named == block : own != Items.AIR && each.asItem() == own;
            if (same) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the player's {@code inventory}, with what the machine's first lane already holds, has a set of
     * {@code recipe}'s ingredients, each counted in full.
     */
    public static boolean hasASet(final Inventory inventory, final ProcessingMachineBlockEntity machine,
                                  final ProcessingRecipe recipe) {
        final List<SizedIngredient> wanted = wanted(recipe);
        if (wanted.size() > machine.layout().inputs()) {
            return false;
        }
        final List<ItemStack> pool = new ArrayList<>();
        for (int i = 0; i < inventory.items.size(); i++) {
            pool.add(inventory.items.get(i).copy());
        }
        for (int i = 0; i < machine.layout().inputs(); i++) {
            pool.add(machine.getInventory().getStackInSlot(machine.layout().laneInput(0, i)).copy());
        }
        for (final SizedIngredient each : wanted) {
            if (take(pool, each, each.count()) < each.count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves {@code holder}'s ingredients from {@code player}'s inventory into {@code machine}'s first lane and picks
     * the recipe; how many sets it moved, none when the machine does not take it from that player or the player holds
     * too little for one.
     */
    public static int fill(final ServerPlayer player, final MachineBlockEntity machine,
                           final RecipeHolder<ProcessingRecipe> holder, final boolean all) {
        final ProcessingRecipe recipe = holder.value();
        if (!(machine instanceof ProcessingMachineBlockEntity processing) || !machine.maySetUp(player)
                || !works(machine, recipe.kind()) || !hasASet(player.getInventory(), processing, recipe)) {
            return 0;
        }
        final ProcessingMachineBlockEntity.Layout layout = processing.layout();
        final List<SizedIngredient> wanted = wanted(recipe);
        final IItemHandlerModifiable slots = machine.getInventory();
        for (int i = 0; i < layout.inputs(); i++) {
            final int slot = layout.laneInput(0, i);
            final ItemStack held = slots.getStackInSlot(slot);
            if (!held.isEmpty() && (i >= wanted.size() || !wanted.get(i).ingredient().test(held))) {
                slots.setStackInSlot(slot, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(held);
            }
        }
        final int sets = all ? sets(player.getInventory(), processing, wanted, recipe.inputs().size()) : 1;
        for (int i = 0; i < wanted.size(); i++) {
            final SizedIngredient each = wanted.get(i);
            final int slot = layout.laneInput(0, i);
            final int count = i < recipe.inputs().size() ? each.count() * sets : each.count();
            final int lacking = count - slots.getStackInSlot(slot).getCount();
            if (lacking > 0) {
                moveIn(player.getInventory(), slots, slot, each, lacking);
            }
        }
        processing.chooseRecipe(holder.id());
        return sets;
    }

    /* The recipe's ingredients in the order they take the slots: its inputs, then its catalysts. */
    private static List<SizedIngredient> wanted(final ProcessingRecipe recipe) {
        final List<SizedIngredient> wanted = new ArrayList<>(recipe.inputs());
        wanted.addAll(recipe.catalysts());
        return wanted;
    }

    /*
     * How many sets the player can move at once: as many as a stack of each input holds, and as the player and the
     * slots have between them; at least one.
     */
    private static int sets(final Inventory inventory, final ProcessingMachineBlockEntity machine,
                            final List<SizedIngredient> wanted, final int inputs) {
        int sets = Integer.MAX_VALUE;
        for (int i = 0; i < inputs; i++) {
            final SizedIngredient each = wanted.get(i);
            final ItemStack held = machine.getInventory().getStackInSlot(machine.layout().laneInput(0, i));
            int had = held.getCount();
            int most = held.isEmpty() ? Item.DEFAULT_MAX_STACK_SIZE : held.getMaxStackSize();
            for (final ItemStack stack : inventory.items) {
                if (!stack.isEmpty() && each.ingredient().test(stack) && (held.isEmpty()
                        || ItemStack.isSameItemSameComponents(stack, held))) {
                    had += stack.getCount();
                    most = Math.min(most, stack.getMaxStackSize());
                }
            }
            sets = Math.min(sets, Math.min(had, most) / Math.max(1, each.count()));
        }
        return Math.max(1, sets == Integer.MAX_VALUE ? 1 : sets);
    }

    /* Moves up to {@code count} of what {@code each} takes from the inventory into {@code slot}. */
    private static void moveIn(final Inventory inventory, final IItemHandlerModifiable slots, final int slot,
                               final SizedIngredient each, final int count) {
        int left = count;
        for (int i = 0; i < inventory.items.size() && left > 0; i++) {
            final ItemStack stack = inventory.items.get(i);
            final ItemStack held = slots.getStackInSlot(slot);
            if (stack.isEmpty() || !each.ingredient().test(stack)
                    || !held.isEmpty() && !ItemStack.isSameItemSameComponents(stack, held)) {
                continue;
            }
            final ItemStack moving = stack.copyWithCount(Math.min(left, stack.getCount()));
            final ItemStack refused = slots.insertItem(slot, moving, false);
            final int moved = moving.getCount() - refused.getCount();
            stack.shrink(moved);
            left -= moved;
            if (moved == 0) {
                return;
            }
        }
        inventory.setChanged();
    }

    /* Takes up to {@code count} of what {@code each} takes from {@code pool}; how much it took. */
    private static int take(final List<ItemStack> pool, final SizedIngredient each, final int count) {
        int taken = 0;
        for (final ItemStack stack : pool) {
            if (taken >= count) {
                break;
            }
            if (!stack.isEmpty() && each.ingredient().test(stack)) {
                final int here = Math.min(count - taken, stack.getCount());
                stack.shrink(here);
                taken += here;
            }
        }
        return taken;
    }
}
