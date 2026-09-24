/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A GPU component item.
 */
@TextHolder
public class GpuItem extends SpecItem<GpuSpec> implements IExpansionCardItem {

    private static final TextKey CORES = TextKey.of("jsc.item.gpu.cores", "%s cores  -  %s MB VRAM");
    private static final TextKey QUEUE = TextKey.of("jsc.item.gpu.queue", "+1 parallel queue  -  %s W");
    private static final TextKey OLDER_SLOT = TextKey.of("jsc.item.gpu.older_slot", "%s  -  slower on an older slot");

    public GpuItem(final Properties properties, final GpuSpec spec) {
        super(properties, spec);
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final GpuSpec spec = spec();
        tooltip.add(GameText.component(CORES.with(spec.cores(), spec.vramMb())).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(QUEUE.with(spec.tdpWatts())).withStyle(ChatFormatting.DARK_GRAY));
        /*
         * Say what the card wants and what happens when it does not get it: the card still fits an
         * older board, so without this line the lost VRAM would look like a bug rather than a trade-off.
         */
        tooltip.add(GameText.component(OLDER_SLOT.with(spec.bus())).withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
