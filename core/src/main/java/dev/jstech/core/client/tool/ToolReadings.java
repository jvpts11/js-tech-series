/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.item.ItemState;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tool.ToolHeat;
import dev.jstech.core.tool.ToolPower;
import dev.jstech.core.tool.ToolSpec;
import dev.jstech.core.tool.ToolTexts;
import dev.jstech.core.tool.Tools;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * What an item shows of what it holds, in the order its icon stacks them: the energy, the fluid (a fuel when it is a
 * tool that burns one), the gas, and the heat of a tool that heats. Each with how much of how much, and its colour.
 */
final class ToolReadings {

    private ToolReadings() {
    }

    /** What {@code stack} shows, on the side {@code level} is on; nothing for an item that holds nothing. */
    static List<Reading> of(final Level level, final ItemStack stack) {
        final ItemState state = ItemStates.of(stack);
        final Optional<ToolSpec> spec = Tools.spec(stack);
        final ToolHudPalette.Colours colours = ToolHudPalette.get();
        final List<Reading> out = new ArrayList<>();
        if (state.holdsEnergy()) {
            final long stored = new ItemEnergyStorage(stack, state.energyCapacity(), 0, 0).stored();
            out.add(new Reading(stored, state.energyCapacity(), colours.energy(), ToolTexts.HUD_ENERGY));
        }
        if (state.holdsFluid()) {
            final FluidStack fluid = stack.getOrDefault(ItemStates.FLUID.get(), SimpleFluidContent.EMPTY).copy();
            final boolean fuel = spec.flatMap(ToolSpec::power).filter(ToolPower.LiquidFuel.class::isInstance)
                    .isPresent();
            final int colour = fuel || fluid.isEmpty() ? colours.fuel()
                    : FastColor.ARGB32.opaque(IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid));
            out.add(new Reading(fluid.getAmount(), state.fluidCapacity(), colour,
                    fuel ? ToolTexts.HUD_FUEL : ToolTexts.HUD_FLUID));
        }
        if (state.holdsGas()) {
            final MatterStack gas = stack.get(ItemStates.GAS.get());
            out.add(new Reading(gas == null ? 0 : gas.amount(), state.gasCapacity(), colours.gas(),
                    ToolTexts.HUD_GAS));
        }
        final Optional<ToolHeat> heat = spec.flatMap(ToolSpec::heat);
        if (heat.isPresent()) {
            final double now = heat.get().heat(level, stack);
            if (now > 0) {
                out.add(new Reading((long) Math.ceil(now), (long) Math.ceil(heat.get().most()), colours.heat(),
                        null));
            }
        }
        return out;
    }

    /** What {@code stack} in the hand shows, on the player's game. */
    static List<Reading> held(final ItemStack stack) {
        final Level level = Minecraft.getInstance().level;
        return level == null ? List.of() : of(level, stack);
    }

    /**
     * One thing an item holds, as it shows.
     *
     * @param amount   how much it holds
     * @param capacity how much it holds at most
     * @param colour   the colour of its bar
     * @param line     what the HUD writes of it, or null for one it writes nothing of (the heat, which has its bar)
     */
    record Reading(long amount, long capacity, int colour, TextKey line) {

        /** How full it is, from nothing to whole. */
        double fraction() {
            return this.capacity <= 0 ? 0.0 : Math.min(1.0, (double) this.amount / this.capacity);
        }

        /** What the HUD writes of it, or none. */
        Optional<Text> text() {
            if (this.line == null) {
                return Optional.empty();
            }
            final UnitFormatter format = UnitFormatter.forCurrentLocale();
            return Optional.of(this.line.with(format.decimal(this.amount), format.decimal(this.capacity)));
        }
    }
}
