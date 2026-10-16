/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.multiblock.ShapePreview;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One multiblock as both recipe viewers show it: laid out at its least size, what it is called, what it takes, the
 * choice of a place where more than one block will do, how it may stand, and its controller's item.
 *
 * @param id         the multiblock's id
 * @param preview    its places, standing facing north
 * @param name       what it is called
 * @param materials  what it takes, most first, its controller last
 * @param choice     the first place where more than one block will do, when there is one
 * @param turns      the lines that say how it may stand
 * @param controller its controller's item, or an empty stack when it is not known
 */
public record MultiblockEntry(ResourceLocation id, ShapePreview preview, Component name, List<Material> materials,
                              Optional<Choice> choice, List<Component> turns, ItemStack controller) {

    public MultiblockEntry {
        materials = List.copyOf(materials);
        turns = List.copyOf(turns);
    }

    /**
     * A material: the items that do, each as many as it takes.
     *
     * @param stacks     the items, any of which does
     * @param count      how many it takes
     * @param controller whether it is the controller
     */
    public record Material(List<ItemStack> stacks, int count, boolean controller) {

        public Material {
            stacks = List.copyOf(stacks);
        }
    }

    /**
     * What will do at some places.
     *
     * @param stacks the items, one for each block that does
     * @param places how many places take any of them
     */
    public record Choice(List<ItemStack> stacks, int places) {

        public Choice {
            stacks = List.copyOf(stacks);
        }
    }
}
