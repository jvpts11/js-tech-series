/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** What a memory module's tooltip says: the items it buffers, its generation and what it draws. */
@TextHolder
final class MemoryTooltip {

    private static final TextKey BUFFER = TextKey.of("jsc.item.ram.buffer", "%s items buffer  -  %s");

    private MemoryTooltip() {
    }

    static void append(final RamSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(BUFFER.with(spec.bufferItems(), spec.generation().label()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(HardwareTooltip.WATTS.with(spec.tdpWatts()))
                .withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
