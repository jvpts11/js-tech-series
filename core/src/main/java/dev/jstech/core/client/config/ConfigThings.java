/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.config.ConfigControl;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The game's things a setting can name, an item, a block or a fluid, as a settings screen shows them: each by its id,
 * its name in the player's language and a picture, the thing's item or, for a block, its item, and for a fluid, its
 * bucket. A thing no mod installed has is shown by its id alone.
 */
final class ConfigThings {

    private ConfigThings() {
    }

    /** What a list or a single id of {@code control}'s kind names: items, blocks or fluids. */
    enum Kind {
        ITEM,
        BLOCK,
        FLUID;

        /** The kind {@code control} picks, or items for any other. */
        static Kind of(final ConfigControl control) {
            return switch (control) {
                case BLOCK, BLOCKS -> BLOCK;
                case FLUID, FLUIDS -> FLUID;
                default -> ITEM;
            };
        }
    }

    /**
     * One thing.
     *
     * @param id   its id, as the setting writes it
     * @param name its name in the player's language
     * @param icon its picture, empty for none
     */
    record Thing(String id, Component name, ItemStack icon) {
    }

    /** Every thing of {@code kind} the installed mods have, by name. */
    static List<Thing> all(final Kind kind) {
        final List<Thing> out = new ArrayList<>();
        switch (kind) {
            case ITEM -> BuiltInRegistries.ITEM.keySet().forEach(id -> {
                if (!id.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
                    out.add(of(kind, id.toString()));
                }
            });
            case BLOCK -> BuiltInRegistries.BLOCK.keySet().forEach(id -> {
                if (!id.equals(BuiltInRegistries.BLOCK.getDefaultKey())) {
                    out.add(of(kind, id.toString()));
                }
            });
            case FLUID -> BuiltInRegistries.FLUID.entrySet().forEach(entry -> {
                // A fluid that flows is the same liquid as its source: only the source is a choice.
                final Fluid fluid = entry.getValue();
                if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState())) {
                    out.add(of(kind, entry.getKey().location().toString()));
                }
            });
        }
        out.sort(Comparator.comparing((Thing thing) -> thing.name().getString().toLowerCase(Locale.ROOT))
                .thenComparing(Thing::id));
        return out;
    }

    /** The thing of {@code kind} called {@code id}: its name and picture, or its id alone when nothing has it. */
    static Thing of(final Kind kind, final String id) {
        final ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return new Thing(id, Component.literal(id), ItemStack.EMPTY);
        }
        return switch (kind) {
            case ITEM -> BuiltInRegistries.ITEM.getOptional(location)
                    .map(item -> new Thing(id, item.getDescription(), new ItemStack(item)))
                    .orElseGet(() -> new Thing(id, Component.literal(id), ItemStack.EMPTY));
            case BLOCK -> BuiltInRegistries.BLOCK.getOptional(location)
                    .map(block -> new Thing(id, block.getName(), iconOf(block)))
                    .orElseGet(() -> new Thing(id, Component.literal(id), ItemStack.EMPTY));
            case FLUID -> BuiltInRegistries.FLUID.getOptional(location)
                    .map(fluid -> new Thing(id, fluid.getFluidType().getDescription(),
                            new ItemStack(fluid.getBucket())))
                    .orElseGet(() -> new Thing(id, Component.literal(id), ItemStack.EMPTY));
        };
    }

    /** Whether {@code thing} answers a search's words, by its name or its id. */
    static boolean matches(final Thing thing, final String query) {
        final String words = query.trim().toLowerCase(Locale.ROOT);
        return words.isEmpty() || thing.name().getString().toLowerCase(Locale.ROOT).contains(words)
                || thing.id().contains(words);
    }

    private static ItemStack iconOf(final Block block) {
        final ItemStack stack = new ItemStack(block.asItem());
        return stack.is(Items.AIR) ? ItemStack.EMPTY : stack;
    }
}
