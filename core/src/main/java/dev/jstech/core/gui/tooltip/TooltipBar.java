/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A line of a tooltip that is a bar: how full something an item holds is, its energy, a fluid, a gas or its wear,
 * drawn as wide as a short word with what it says beside it, as a battery's "Energy: 27.5 kFE of 50 kFE".
 *
 * <pre>{@code
 * tooltip.bar(TooltipBar.of(stored, capacity, TooltipBar.Paint.ENERGY, Component.literal("Energy: ...")));
 * }</pre>
 *
 * @param fill  how full, from 0 to 1
 * @param paint what fills it
 * @param label what it says beside the bar
 */
public record TooltipBar(float fill, Paint paint, Component label) implements TooltipComponent {

    public TooltipBar {
        fill = Mth.clamp(fill, 0.0F, 1.0F);
    }

    /** A bar {@code held} of {@code most} full; empty when {@code most} is none. */
    public static TooltipBar of(final long held, final long most, final Paint paint, final Component label) {
        return new TooltipBar(most <= 0 ? 0.0F : (float) ((double) held / most), paint, label);
    }

    /** What fills a bar. */
    public sealed interface Paint {

        /** The red of energy. */
        Paint ENERGY = new Energy();
        /** The pale grey of a gas. */
        Paint GAS = new Gas();

        /** Energy, in the series' red. */
        record Energy() implements Paint {
        }

        /** A gas, in a pale grey. */
        record Gas() implements Paint {
        }

        /**
         * A fluid, in its own texture and tint, as it is seen in the world.
         *
         * @param fluid the fluid
         */
        record Fluid(FluidStack fluid) implements Paint {

            public Fluid {
                fluid = fluid.copy();
            }

            /** The fluid, a copy. */
            @Override
            public FluidStack fluid() {
                return fluid.copy();
            }
        }

        /**
         * A colour of the mod's, as the game's own green to red of wear ({@code ItemStack#getBarColor}).
         *
         * @param rgb the colour, red, green and blue
         */
        record Colour(int rgb) implements Paint {
        }
    }
}
