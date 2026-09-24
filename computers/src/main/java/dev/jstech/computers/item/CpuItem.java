/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A CPU component item.
 */
@TextHolder
public class CpuItem extends SpecItem<CpuSpec> {

    private static final TextKey CORES = TextKey.of("jsc.item.cpu.cores", "%s cores @ %s GHz");
    private static final TextKey THROUGHPUT = TextKey.of("jsc.item.cpu.throughput", "%s it/t  -  %s W");
    private static final TextKey SOCKET = TextKey.of("jsc.item.cpu.socket", "Socket %s");
    /* The architecture, then what an item costs on the era's disks. */
    private static final TextKey WORD = TextKey.of("jsc.item.cpu.word", "%s  -  %s");

    public CpuItem(final Properties properties, final CpuSpec spec) {
        super(properties, spec);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final CpuSpec spec = spec();
        tooltip.add(GameText.component(CORES.with(spec.cores(), String.format("%.2f", spec.freqMhz() / 1000.0)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(THROUGHPUT.with(spec.orchestrationCapacity(), spec.tdpWatts()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(GameText.component(SOCKET.with(spec.socket().display())).withStyle(ChatFormatting.DARK_GRAY));
        /*
         * The architecture is what decides which programs this chip will run, so it is named rather than left to be
         * guessed from the era. The word size beside it is what an item costs on that era's disks, so the player can
         * read the ladder off the chip.
         */
        tooltip.add(GameText.component(WORD.with(HardwareTooltip.architecture(spec),
                HardwareTooltip.MB_PER_ITEM.with(spec.era().mbPerItem()))).withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
