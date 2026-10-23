/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * The item of a block that keeps its fluid in its item when it is broken, as a tank does: its block entity writes the
 * fluid into {@link ItemStates#FLUID}, the block drops itself with its contents, and this item's tooltip says what it
 * carries, "Water: 4000 of 16000 mB", or that it is empty.
 */
public class FluidBlockItem extends BlockItem {

    private final int capacity;

    public FluidBlockItem(final Block block, final Properties properties, final int capacity) {
        super(block, properties);
        if (capacity <= 0) {
            throw new IllegalArgumentException("a block that keeps a fluid holds some, not " + capacity + " mB");
        }
        this.capacity = capacity;
    }

    /** What its block holds, in millibuckets. */
    public int capacity() {
        return this.capacity;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        final SimpleFluidContent fluid = stack.getOrDefault(ItemStates.FLUID.get(), SimpleFluidContent.EMPTY);
        lines.add(GameText.component(fluid.isEmpty() ? ItemTexts.FLUID_EMPTY.with(this.capacity)
                : ItemTexts.FLUID.with(GameText.of(fluid.copy().getHoverName()), fluid.getAmount(), this.capacity))
                .withStyle(ChatFormatting.GRAY));
    }
}
