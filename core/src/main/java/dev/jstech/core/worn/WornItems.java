/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * What an entity wears, wherever it wears it: its armour, and the slots of whichever mods of worn things are
 * installed, Curios, Accessories, or a mod's own. A mod asks here whether a player wears its goggles or carries its
 * watch on a wrist, and works the same with any of those mods or none.
 *
 * <p>The stacks handed back are the worn ones themselves, to read: a mod that changes what is worn does it through the
 * slot's own mod.
 */
public final class WornItems {

    private static final List<IWornSource> SOURCES = new CopyOnWriteArrayList<>();

    private WornItems() {
    }

    /** Adds a source of worn things, from the constructor of the mod that brings it. */
    public static void addSource(final IWornSource source) {
        SOURCES.add(source);
    }

    /** Everything {@code entity} wears: its armour first, then the slots of every source, none of them empty. */
    public static List<ItemStack> worn(final LivingEntity entity) {
        final List<ItemStack> out = new ArrayList<>();
        for (final ItemStack armour : entity.getArmorSlots()) {
            if (!armour.isEmpty()) {
                out.add(armour);
            }
        }
        collectFromSources(entity, out);
        return out;
    }

    /** The first thing {@code entity} wears that {@code test} accepts, or none. */
    public static Optional<ItemStack> firstWorn(final LivingEntity entity, final Predicate<ItemStack> test) {
        /* Armour is tested before any source is asked, so a match there costs no list at all. */
        for (final ItemStack armour : entity.getArmorSlots()) {
            if (!armour.isEmpty() && test.test(armour)) {
                return Optional.of(armour);
            }
        }
        final List<ItemStack> fromSources = new ArrayList<>();
        collectFromSources(entity, fromSources);
        for (final ItemStack stack : fromSources) {
            if (test.test(stack)) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    /** Whether {@code entity} wears anything {@code test} accepts. */
    public static boolean wears(final LivingEntity entity, final Predicate<ItemStack> test) {
        return firstWorn(entity, test).isPresent();
    }

    /* Asks every source, then drops any empty stack one wrote, so the no-empty-stacks promise never rests on them. */
    private static void collectFromSources(final LivingEntity entity, final List<ItemStack> out) {
        final int before = out.size();
        for (final IWornSource source : SOURCES) {
            source.collect(entity, out);
        }
        for (int i = out.size() - 1; i >= before; i--) {
            if (out.get(i).isEmpty()) {
                out.remove(i);
            }
        }
    }
}
