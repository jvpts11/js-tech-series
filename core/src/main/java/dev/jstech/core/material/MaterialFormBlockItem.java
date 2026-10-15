/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.GameText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** The item of a material's block, named as the block is. */
public class MaterialFormBlockItem extends BlockItem {

    private final MaterialFormBlock block;

    public MaterialFormBlockItem(final MaterialFormBlock block, final Properties properties) {
        super(block, properties);
        this.block = block;
    }

    /** The material and the form it is. */
    public MaterialThing thing() {
        return this.block.thing();
    }

    @Override
    public Component getName(final ItemStack stack) {
        return GameText.component(this.block.displayName());
    }
}
