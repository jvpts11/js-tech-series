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
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

/**
 * A machine growing into another in its place without being broken, as a mod's installer makes it the next of its
 * family: its energy, tanks, settings, owner and the work under way kept, its items moved slot by slot into the slots
 * of the same use (each lane's inputs and outputs, the upgrades), and what the new machine has no room for given to
 * the player who grew it.
 */
public final class MachineGrowth {

    private static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final String INVENTORY = "Inventory";
    private static final String LOCKS = "Locks";

    private MachineGrowth() {
    }

    /**
     * Grows {@code machine} into {@code into} for {@code player}: false, leaving it, when the player may not break the
     * machine or {@code into} makes no machine.
     */
    public static boolean grow(final ServerPlayer player, final MachineBlockEntity machine, final Block into) {
        if (!(machine.getLevel() instanceof ServerLevel level) || !machine.mayBreak(player)) {
            return false;
        }
        final BlockPos pos = machine.getBlockPos();
        final HolderLookup.Provider registries = level.registryAccess();
        final CompoundTag saved = machine.saveWithoutMetadata(registries);
        saved.remove(INVENTORY);
        final List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
            held.add(machine.getInventory().getStackInSlot(slot).copy());
        }
        final ProcessingMachineBlockEntity.Layout before = machine instanceof ProcessingMachineBlockEntity processing
                ? processing.layout() : null;
        BlockState state = into.defaultBlockState();
        final BlockState was = machine.getBlockState();
        if (state.hasProperty(FACING) && was.hasProperty(FACING)) {
            state = state.setValue(FACING, was.getValue(FACING));
        }
        machine.fields().takenWhole();
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity grown)) {
            give(player, held);
            return true;
        }
        final ProcessingMachineBlockEntity.Layout after = grown instanceof ProcessingMachineBlockEntity processing
                ? processing.layout() : null;
        if (before == null || after == null || before.inputs() != after.inputs() || before.lanes() != after.lanes()) {
            // Locks name input slots by number; on another layout of inputs they would lock the wrong ones.
            saved.remove(LOCKS);
        }
        grown.loadCustomOnly(saved, registries);
        give(player, move(held, before, after, grown.getInventory()));
        grown.setChanged();
        return true;
    }

    /* Puts each held item into the slot of the same use, and gives back what found none. */
    private static List<ItemStack> move(final List<ItemStack> held,
                                        final ProcessingMachineBlockEntity.@Nullable Layout before,
                                        final ProcessingMachineBlockEntity.@Nullable Layout after,
                                        final IItemHandlerModifiable into) {
        final List<ItemStack> left = new ArrayList<>();
        final boolean[] placed = new boolean[held.size()];
        if (before != null && after != null) {
            for (int lane = 0; lane < Math.min(before.lanes(), after.lanes()); lane++) {
                for (int i = 0; i < Math.min(before.inputs(), after.inputs()); i++) {
                    put(held, placed, before.laneInput(lane, i), into, after.laneInput(lane, i));
                }
                for (int i = 0; i < Math.min(before.outputs(), after.outputs()); i++) {
                    put(held, placed, before.laneOutput(lane, i), into, after.laneOutput(lane, i));
                }
            }
            for (int i = 0; i < Math.min(before.upgrades(), after.upgrades()); i++) {
                put(held, placed, before.firstUpgrade() + i, into, after.firstUpgrade() + i);
            }
        } else {
            for (int slot = 0; slot < Math.min(held.size(), into.getSlots()); slot++) {
                put(held, placed, slot, into, slot);
            }
        }
        for (int slot = 0; slot < held.size(); slot++) {
            if (!placed[slot] && !held.get(slot).isEmpty()) {
                left.add(held.get(slot));
            }
        }
        return left;
    }

    private static void put(final List<ItemStack> held, final boolean[] placed, final int from,
                            final IItemHandlerModifiable into, final int to) {
        if (from < held.size() && to < into.getSlots()) {
            into.setStackInSlot(to, held.get(from));
            placed[from] = true;
        }
    }

    private static void give(final ServerPlayer player, final List<ItemStack> stacks) {
        for (final ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }
}
