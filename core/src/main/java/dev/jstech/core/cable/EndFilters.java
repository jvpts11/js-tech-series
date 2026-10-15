/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.grid.GridKind;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterBridges;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * How the filter of a line's end is set from the stacks a player drops on it: a stack names what a wire of its line
 * carries of it (the item itself, the liquid in a bucket or a tank, the gas in a cylinder), and a rule goes round the
 * item, its tags and its mod.
 */
public final class EndFilters {

    private EndFilters() {
    }

    /**
     * The rule a stack dropped on a filter slot makes for a wire of {@code grid}, carrying {@code amount}: the item, or
     * what it holds of a liquid or a gas, by its id; none for a stack that holds nothing the line carries.
     */
    public static Optional<ItemFilter.Rule> ruleFor(final GridKind grid, final ItemStack stack, final long amount) {
        return idFor(grid, stack).map(id -> new ItemFilter.Fuzzy(id, amount));
    }

    /** The id of what a wire of {@code grid} carries of {@code stack}; none for a stack that holds nothing it carries. */
    public static Optional<String> idFor(final GridKind grid, final ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (grid == GridKind.ITEM) {
            return Optional.of(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        }
        final Optional<String> fluid = FluidUtil.getFluidContained(stack)
                .map(held -> BuiltInRegistries.FLUID.getKey(held.getFluid()).toString());
        if (grid == GridKind.FLUID) {
            return fluid;
        }
        for (final MatterState state : MatterState.values()) {
            // A capability that answers nothing maps to none: Optional.map drops a null.
            final Optional<String> held = MatterCapabilities.item(state)
                    .map(capability -> stack.getCapability(capability)).flatMap(EndFilters::firstHeld);
            if (held.isPresent()) {
                return held;
            }
        }
        // A gas kept as a fluid is the material it stands for, when one does.
        return fluid.map(id -> {
            final ResourceLocation key = ResourceLocation.parse(id);
            return MatterBridges.materialOfFluid(key).map(material -> material.id().toString()).orElse(id);
        });
    }

    /**
     * The rule after {@code rule} as a right-click goes round the kinds of rule a slot can hold for what
     * {@code origin} names (a dropped stack's id, or none for the one the rule itself names): the thing itself, each of
     * its tags in order, the mod that adds it, and back. The amount it carries stays.
     */
    public static ItemFilter.Rule next(final GridKind grid, final ItemFilter.Rule rule, final Optional<String> origin) {
        final Optional<String> reference = origin.or(() -> reference(grid, rule));
        if (reference.isEmpty()) {
            return rule;
        }
        final List<ItemFilter.Rule> round = kinds(grid, reference.get());
        final ItemFilter.Rule bare = rule.withAmount(0L);
        final int at = round.indexOf(bare);
        return round.get((at + 1) % round.size()).withAmount(rule.amount());
    }

    /** The kinds of rule a slot can hold for {@code id}: the thing itself, its tags in order, its mod. */
    public static List<ItemFilter.Rule> kinds(final GridKind grid, final String id) {
        final List<ItemFilter.Rule> kinds = new ArrayList<>();
        kinds.add(new ItemFilter.Fuzzy(id, 0L));
        final ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return kinds;
        }
        final List<String> tags = new ArrayList<>();
        if (grid == GridKind.ITEM) {
            BuiltInRegistries.ITEM.getHolder(key).ifPresent(holder -> holder.tags()
                    .forEach(tag -> tags.add(tag.location().toString())));
        } else {
            BuiltInRegistries.FLUID.getHolder(key).ifPresent(holder -> holder.tags()
                    .forEach(tag -> tags.add(tag.location().toString())));
        }
        tags.sort(Comparator.naturalOrder());
        for (final String tag : tags) {
            kinds.add(new ItemFilter.Tag(tag, 0L));
        }
        kinds.add(new ItemFilter.Mod(key.getNamespace(), 0L));
        return kinds;
    }

    /** The item a slot shows for a rule naming the tag {@code tag} of items, or of fluids: the tag's first. */
    public static Optional<ResourceLocation> firstOfTag(final GridKind grid, final String tag) {
        final ResourceLocation key = ResourceLocation.tryParse(tag);
        if (key == null) {
            return Optional.empty();
        }
        if (grid == GridKind.ITEM) {
            return BuiltInRegistries.ITEM.getTag(TagKey.create(BuiltInRegistries.ITEM.key(), key))
                    .flatMap(set -> set.stream().findFirst()).flatMap(Holder::unwrapKey)
                    .map(found -> found.location());
        }
        return BuiltInRegistries.FLUID.getTag(TagKey.create(BuiltInRegistries.FLUID.key(), key))
                .flatMap(set -> set.stream().findFirst()).flatMap(Holder::unwrapKey).map(found -> found.location());
    }

    /* The thing a rule names, to go round its kinds from: none for a mod's rule, which names no one thing. */
    private static Optional<String> reference(final GridKind grid, final ItemFilter.Rule rule) {
        return switch (rule) {
            case ItemFilter.Fuzzy fuzzy -> Optional.of(fuzzy.item());
            case ItemFilter.Exact exact -> Optional.of(exact.pattern().itemId());
            case ItemFilter.Tag tag -> firstOfTag(grid, tag.tag()).map(ResourceLocation::toString);
            case ItemFilter.Mod mod -> Optional.empty();
        };
    }

    /* The id of the first material a store of matter holds. */
    private static Optional<String> firstHeld(final IMatterHandler handler) {
        for (int tank = 0; tank < handler.tanks(); tank++) {
            final Optional<MatterStack> held = handler.stack(tank);
            if (held.isPresent()) {
                final Material material = held.get().material();
                return Optional.of(material.id().toString());
            }
        }
        return Optional.empty();
    }
}
