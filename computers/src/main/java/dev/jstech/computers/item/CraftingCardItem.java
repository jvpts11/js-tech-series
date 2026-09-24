/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Crafting Card component item: the PCIe card a Crafting Computer needs to execute recipes.
 */
@TextHolder
public class CraftingCardItem extends SpecItem<CraftingCardSpec> implements IExpansionCardItem {

    private static final TextKey PURPOSE = TextKey.of("jsc.item.crafting_card.purpose", "Crafting accelerator (FPGA)");
    private static final TextKey THREADS_ONE = TextKey.of("jsc.item.crafting_card.threads_one", "Threads  -  %s thread");
    private static final TextKey THREADS_MANY =
            TextKey.of("jsc.item.crafting_card.threads_many", "Threads  -  %s threads");
    private static final TextKey THROUGHPUT = TextKey.of("jsc.item.crafting_card.throughput", "Throughput  -  %sx CPU");

    public CraftingCardItem(final Properties properties, final CraftingCardSpec spec) {
        super(properties, spec);
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final CraftingCardSpec spec = spec();
        tooltip.add(GameText.component(PURPOSE).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component((spec.threads() == 1 ? THREADS_ONE : THREADS_MANY).with(spec.threads()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(THROUGHPUT.with(spec.cpuFactor())).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(HardwareTooltip.TIER_POWER_BUS.with(spec.tier(), spec.tdpWatts(), spec.bus()))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
