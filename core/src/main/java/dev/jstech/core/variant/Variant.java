/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * One variant the game registered: of one kind, made of one material.
 *
 * @param kind     its kind
 * @param material its material
 * @param id       its id, the block's and the item's
 * @param block    its block
 * @param item     its item
 */
public record Variant(VariantKind kind, FoundMaterial material, ResourceLocation id, Block block, Item item) {

    public Variant {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(item, "item");
    }
}
