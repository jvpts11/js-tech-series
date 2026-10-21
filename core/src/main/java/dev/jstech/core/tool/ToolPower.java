/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.item.ItemState;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.vehicle.Fuel;
import dev.jstech.core.vehicle.Fuels;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * What a tool works on, spent each time it works (each block it breaks, each shot it fires): the energy it holds, a
 * fuel it burns from the fluid it holds, as a chainsaw on petrol, or a gas from its tank, as a pneumatic tool on
 * compressed air. Out of it, a tool does no more than a bare hand.
 */
public sealed interface ToolPower {

    /** {@code perUse} FE of the energy it holds a use. */
    static ToolPower energy(final int perUse) {
        return new Energy(perUse);
    }

    /** Burns one of {@code fuels}, by id ({@link Fuels}), from the fluid it holds, {@code joulesPerUse} a use. */
    static ToolPower fuel(final List<ResourceLocation> fuels, final double joulesPerUse) {
        return new LiquidFuel(fuels, joulesPerUse);
    }

    /** Spends {@code perUse} millibuckets a use of one of the gases {@code materials}, by id, from its tank. */
    static ToolPower gas(final List<ResourceLocation> materials, final long perUse) {
        return new Gas(materials, perUse);
    }

    /** Whether {@code stack} holds what it spends for a use, on the side {@code level} is on. */
    boolean has(Level level, ItemStack stack);

    /** Spends what a use of {@code stack} takes; nothing when it does not hold it. */
    void spend(Level level, ItemStack stack);

    /** Whether {@code state}, as an item is declared, holds what this spends. */
    boolean heldBy(ItemState state);

    /**
     * Energy, as the item holds it, its own rate of giving energy to others aside.
     *
     * @param perUse FE a use; above nothing
     */
    record Energy(int perUse) implements ToolPower {

        public Energy {
            if (perUse <= 0) {
                throw new IllegalArgumentException("a tool on energy spends some, not " + perUse);
            }
        }

        @Override
        public boolean has(final Level level, final ItemStack stack) {
            return storage(stack).stored() >= this.perUse;
        }

        @Override
        public void spend(final Level level, final ItemStack stack) {
            final ItemEnergyStorage storage = storage(stack);
            if (storage.stored() >= this.perUse) {
                storage.extractEnergy(this.perUse, false);
            }
        }

        @Override
        public boolean heldBy(final ItemState state) {
            return state.holdsEnergy();
        }

        /* The tool's own store, which it draws from at any rate. */
        private static ItemEnergyStorage storage(final ItemStack stack) {
            final ItemState state = ItemStates.of(stack);
            return new ItemEnergyStorage(stack, state.energyCapacity(), 0, Integer.MAX_VALUE);
        }
    }

    /**
     * A fuel burnt from the fluid the tool holds.
     *
     * @param fuels        the fuels it burns, by id
     * @param joulesPerUse what a use takes burnt, in joules; a use spends the millibuckets of the fuel that give it,
     *                     rounded up
     */
    record LiquidFuel(List<ResourceLocation> fuels, double joulesPerUse) implements ToolPower {

        public LiquidFuel {
            fuels = List.copyOf(fuels);
            if (fuels.isEmpty() || !(joulesPerUse > 0)) {
                throw new IllegalArgumentException("a tool on fuel burns one or more, some each use");
            }
        }

        @Override
        public boolean has(final Level level, final ItemStack stack) {
            final FluidStack held = held(stack);
            return !held.isEmpty() && millibuckets(level, held).filter(mb -> held.getAmount() >= mb).isPresent();
        }

        @Override
        public void spend(final Level level, final ItemStack stack) {
            final FluidStack held = held(stack);
            if (held.isEmpty()) {
                return;
            }
            final Optional<Integer> mb = millibuckets(level, held);
            if (mb.isPresent() && held.getAmount() >= mb.get()) {
                held.shrink(mb.get());
                stack.set(ItemStates.FLUID.get(), SimpleFluidContent.copyOf(held));
            }
        }

        @Override
        public boolean heldBy(final ItemState state) {
            return state.holdsFluid();
        }

        /* The millibuckets of {@code held} a use burns, when it is one of the fuels. */
        private Optional<Integer> millibuckets(final Level level, final FluidStack held) {
            return Fuels.liquidAmong(level, this.fuels, held.getFluid()).flatMap(id -> Fuels.get(level, id))
                    .map(Fuel::joulesPerMb)
                    .map(perMb -> (int) Math.min(Integer.MAX_VALUE, Math.ceil(this.joulesPerUse / perMb)));
        }

        private static FluidStack held(final ItemStack stack) {
            return stack.getOrDefault(ItemStates.FLUID.get(), SimpleFluidContent.EMPTY).copy();
        }
    }

    /**
     * A gas spent from the tool's tank.
     *
     * @param materials the gases it works on, by their materials' ids
     * @param perUse    millibuckets a use, at the pressure of the open air
     */
    record Gas(List<ResourceLocation> materials, long perUse) implements ToolPower {

        public Gas {
            materials = List.copyOf(materials);
            if (materials.isEmpty() || perUse <= 0) {
                throw new IllegalArgumentException("a tool on gas works on one or more, some each use");
            }
        }

        @Override
        public boolean has(final Level level, final ItemStack stack) {
            final MatterStack held = stack.get(ItemStates.GAS.get());
            return held != null && this.materials.contains(held.material().id()) && held.amount() >= this.perUse;
        }

        @Override
        public void spend(final Level level, final ItemStack stack) {
            if (!has(level, stack)) {
                return;
            }
            final MatterStack held = stack.get(ItemStates.GAS.get());
            if (held.amount() == this.perUse) {
                stack.remove(ItemStates.GAS.get());
            } else {
                stack.set(ItemStates.GAS.get(), held.withAmount(held.amount() - this.perUse));
            }
        }

        @Override
        public boolean heldBy(final ItemState state) {
            return state.holdsGas();
        }
    }
}
