/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A variant's item, named as its block is, whatever block its kind made. */
public class VariantBlockItem extends BlockItem {

    private final Text name;

    public VariantBlockItem(final Block block, final Properties properties, final Text name) {
        super(block, properties);
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public Component getName(final ItemStack stack) {
        return GameText.component(this.name);
    }
}
