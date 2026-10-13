/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.PhiCoprocessorSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A crafting co-processor item for the Supercomputer.
 */
@TextHolder
public class PhiCoprocessorItem extends SpecItem<PhiCoprocessorSpec> implements IExpansionCardItem {

    private static final TextKey CORES = TextKey.of("jsc.item.phi_coprocessor.cores", "%s cores @ %s GHz");
    private static final TextKey SLOTS = TextKey.of("jsc.item.phi_coprocessor.slots", "Supercomputer slots 1-%s");

    public PhiCoprocessorItem(final Properties properties, final PhiCoprocessorSpec spec) {
        super(properties.stacksTo(16), spec);
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final PhiCoprocessorSpec spec = spec();
        final String gigahertz = String.format(Locale.ROOT, "%.2f", spec.mhz() / 1000.0);
        tooltip.add(GameText.component(CORES.with(spec.cores(), gigahertz)).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(SLOTS.with(spec.maxSlot())).withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
