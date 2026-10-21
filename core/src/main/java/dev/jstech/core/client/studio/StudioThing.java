/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.content.BlockEntry;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * A thing of the game, as the studio lists it: what a mod or a content pack put in the game, under its mod and its
 * kind, with what it is in the game's registries. Only what the thing is has a value: a block has its block and,
 * when it has one, its item; a piece of research has none of them.
 *
 * @param modid   the mod or the content pack it belongs to
 * @param kind    what kind of thing it is
 * @param id      its id
 * @param nameKey the key of its name
 * @param icon    what stands for it in the tree and on the bench, or an empty stack
 * @param entry   the block as its mod declared it, for a block
 * @param block   its block, for a block or a fluid that has one
 * @param item    its item, for an item or a block that has one
 * @param fluid   its fluid, for a fluid
 * @param entity  its kind of entity, for an entity
 */
public record StudioThing(String modid, ThingKind kind, ResourceLocation id, String nameKey, ItemStack icon,
                          @Nullable BlockEntry<?> entry, @Nullable Block block, @Nullable Item item,
                          @Nullable Fluid fluid, @Nullable EntityType<?> entity) {

    public StudioThing {
        Objects.requireNonNull(modid, "modid");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nameKey, "nameKey");
        Objects.requireNonNull(icon, "icon");
    }

    /** What it is called, in the player's language. */
    public Component name() {
        return Component.translatable(this.nameKey);
    }

    /** Whether the word typed in the search is in its name or its id. */
    public boolean matches(final String typed, final String name) {
        final String word = typed.toLowerCase(Locale.ROOT);
        return name.toLowerCase(Locale.ROOT).contains(word) || this.id.toString().contains(word);
    }
}
