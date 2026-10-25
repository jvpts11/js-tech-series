/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** What a graphics card's tooltip says: its cores and video memory, its design, its queue and its slot. */
@TextHolder
final class GraphicsCardTooltip {

    private static final TextKey CORES = TextKey.of("jsc.item.gpu.cores", "%s cores  -  %s MB VRAM");
    private static final TextKey QUEUE = TextKey.of("jsc.item.gpu.queue", "+1 parallel queue  -  %s W");
    private static final TextKey OLDER_SLOT = TextKey.of("jsc.item.gpu.older_slot", "%s  -  slower on an older slot");

    private GraphicsCardTooltip() {
    }

    static void append(final GpuSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(CORES.with(spec.cores(), spec.vramMb())).withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendDesign(tooltip, spec.designLabel());
        tooltip.add(GameText.component(QUEUE.with(spec.tdpWatts())).withStyle(ChatFormatting.DARK_GRAY));
        /*
         * Say what the card wants and what happens when it does not get it: the card still fits an
         * older board, so without this line the lost VRAM would look like a bug rather than a trade-off.
         */
        tooltip.add(GameText.component(OLDER_SLOT.with(spec.bus().label())).withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
