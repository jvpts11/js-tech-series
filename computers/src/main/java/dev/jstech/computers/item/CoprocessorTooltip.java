/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.PhiCoprocessorSpec;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** What a Supercomputer co-processor's tooltip says: its cores and clock, and the slots it seats in. */
@TextHolder
final class CoprocessorTooltip {

    private static final TextKey CORES = TextKey.of("jsc.item.phi_coprocessor.cores", "%s cores @ %s GHz");
    private static final TextKey SLOTS = TextKey.of("jsc.item.phi_coprocessor.slots", "Supercomputer slots 1-%s");

    private CoprocessorTooltip() {
    }

    static void append(final PhiCoprocessorSpec spec, final List<Component> tooltip) {
        final String gigahertz = String.format(Locale.ROOT, "%.2f", spec.mhz() / 1000.0);
        tooltip.add(GameText.component(CORES.with(spec.cores(), gigahertz)).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(SLOTS.with(spec.maxSlot())).withStyle(ChatFormatting.GOLD));
    }
}
