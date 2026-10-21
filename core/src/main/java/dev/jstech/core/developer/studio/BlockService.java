/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.blockentity.FieldFlow;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.util.Loaded;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The block inspector's answer for a place: the block there, its block entity's fields as it saves them, what each
 * face offers (energy, items, fluid), and, for a block entity of the Core's, what has crossed its faces all told,
 * which the inspector reads each second to say what goes in and out.
 */
final class BlockService implements IStudioService {

    static final String NAME = "block";
    /** The most items a face names. */
    private static final int ITEMS_NAMED = 3;

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final ServerLevel level = player.serverLevel();
        final BlockPos pos = BlockPos.of(args.getLong(StudioKeys.POS));
        final CompoundTag out = new CompoundTag();
        if (!Loaded.at(level, pos)) {
            reply.accept(out);
            return;
        }
        out.putString(StudioKeys.BLOCK, BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
        final BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null) {
            out.put(StudioKeys.FIELDS, NbtLines.listed(NbtLines.of(entity.saveWithoutMetadata(
                    level.registryAccess()))));
        }
        final ListTag faces = new ListTag();
        for (final Direction side : Direction.values()) {
            faces.add(face(level, pos, side));
        }
        out.put(StudioKeys.FACES, faces);
        if (entity instanceof SyncedBlockEntity synced) {
            final FieldFlow.Totals totals = synced.fields().flow().totals();
            final CompoundTag flow = new CompoundTag();
            flow.putLong(StudioKeys.ENERGY_IN, totals.energyIn());
            flow.putLong(StudioKeys.ENERGY_OUT, totals.energyOut());
            flow.putLong(StudioKeys.ITEMS_IN, totals.itemsIn());
            flow.putLong(StudioKeys.ITEMS_OUT, totals.itemsOut());
            flow.putLong(StudioKeys.FLUID_IN, totals.fluidIn());
            flow.putLong(StudioKeys.FLUID_OUT, totals.fluidOut());
            out.put(StudioKeys.FLOW, flow);
        }
        reply.accept(out);
    }

    /* What one face offers, read through the capabilities as a pipe or a cable there would. */
    private static CompoundTag face(final ServerLevel level, final BlockPos pos, final Direction side) {
        final CompoundTag face = new CompoundTag();
        face.putString(StudioKeys.SIDE, side.getSerializedName());
        final @Nullable IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
        if (energy != null) {
            face.putInt(StudioKeys.ENERGY, energy.getEnergyStored());
            face.putInt(StudioKeys.CAPACITY, energy.getMaxEnergyStored());
        }
        final @Nullable IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (items != null) {
            face.putInt(StudioKeys.SLOTS, items.getSlots());
            final ListTag held = new ListTag();
            for (int slot = 0; slot < items.getSlots() && held.size() < ITEMS_NAMED; slot++) {
                final ItemStack stack = items.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    held.add(counted(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount()));
                }
            }
            face.put(StudioKeys.ITEMS, held);
        }
        final @Nullable IFluidHandler fluid = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        if (fluid != null) {
            final ListTag tanks = new ListTag();
            for (int tank = 0; tank < fluid.getTanks(); tank++) {
                final FluidStack stack = fluid.getFluidInTank(tank);
                final CompoundTag held = counted(stack.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(
                        stack.getFluid()).toString(), stack.getAmount());
                held.putInt(StudioKeys.CAPACITY, fluid.getTankCapacity(tank));
                tanks.add(held);
            }
            face.put(StudioKeys.TANKS, tanks);
        }
        return face;
    }

    private static CompoundTag counted(final String id, final int count) {
        final CompoundTag out = new CompoundTag();
        out.putString(StudioKeys.ID, id);
        out.putInt(StudioKeys.COUNT, count);
        return out;
    }
}
