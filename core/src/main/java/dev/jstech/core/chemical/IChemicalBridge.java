/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.chemical;

import dev.jstech.core.material.MatterState;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * What an integration provides for a chemical mod's chemicals to be moved and stored like fluids: a port onto a
 * block's chemical capability, and the identity of a chemical (existence, name, colour) by registry id. Nothing else
 * names a chemical mod; a bridge is registered only when its mod is present.
 */
public interface IChemicalBridge {

    /** The chemical port of the block at {@code pos} as seen from {@code side}, if the block has one. */
    Optional<IChemicalPort> portFor(Level level, BlockPos pos, @Nullable Direction side);

    /**
     * The port of the block at {@code pos} as seen from {@code side}, asked again each time it is read but found
     * cheaply: what a grid keeps for each block it meets and reads every tick. The bridge of a mod whose capability
     * can be cached gives a cached one; the others find the port anew.
     */
    default Supplier<Optional<IChemicalPort>> portCache(final ServerLevel level, final BlockPos pos,
                                                        @Nullable final Direction side) {
        return () -> portFor(level, pos, side);
    }

    /** Whether {@code chemical} is a registered chemical of this bridge's mod. */
    boolean exists(ResourceLocation chemical);

    /**
     * The state of matter {@code chemical} is in, so a line of each state carries only its own: a gas, a slurry; none
     * for a chemical that is no state of matter of its own (a pigment, a dye), which no line carries.
     */
    default Optional<MatterState> stateOf(final ResourceLocation chemical) {
        return Optional.empty();
    }

    /** The chemical's display name, as its mod shows it. */
    Component displayName(ResourceLocation chemical);

    /** The chemical's colour (ARGB), for the GUIs' tinted swatch. */
    int tint(ResourceLocation chemical);

    /**
     * The chemical an item carries (a filled tank item, a hohlraum), if any: how a bus filter names a chemical,
     * the way a filled bucket names a fluid.
     */
    default Optional<ResourceLocation> chemicalOf(final ItemStack stack) {
        return Optional.empty();
    }

    /**
     * The chemical port of an item that carries chemicals (a tank item), if it has one. The port reads and
     * writes {@code stack} itself, so a caller hands it the very stack it means to empty or refill.
     */
    default Optional<IChemicalPort> itemPortFor(final ItemStack stack) {
        return Optional.empty();
    }

    /** A chemical and an amount in millibuckets, as a recipe viewer lists them. */
    record ChemicalAmount(ResourceLocation chemical, long amount) {
    }

    /**
     * The chemical a recipe viewer's ingredient object stands for (its mod's chemical stack type), if this
     * bridge recognises it, which is how a recipe transfer reads chemical ingredients without naming the mod.
     */
    default Optional<ChemicalAmount> chemicalIngredient(final Object ingredient) {
        return Optional.empty();
    }

    /**
     * Whether a recipe object meters its chemical input per tick of the operation rather than per operation,
     * so a transfer must multiply the displayed amount by the machine's duration.
     */
    default boolean perTickUsage(final Object recipe) {
        return false;
    }
}
