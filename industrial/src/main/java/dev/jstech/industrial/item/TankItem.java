/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.item;

import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.ItemTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.industrial.blockentity.TankBlockEntity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** The Tank as an item, which says what fluid it carries from the tank it was broken from. */
public class TankItem extends BlockItem {

    public TankItem(final Block block, final Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        final SimpleFluidContent fluid = stack.getOrDefault(ItemStates.FLUID.get(), SimpleFluidContent.EMPTY);
        tooltip.add(GameText.component(fluid.isEmpty()
                ? ItemTexts.FLUID_EMPTY.with(TankBlockEntity.CAPACITY_MB)
                : ItemTexts.FLUID.with(GameText.of(fluid.copy().getHoverName()), fluid.getAmount(),
                        TankBlockEntity.CAPACITY_MB)).withStyle(ChatFormatting.GRAY));
    }
}
