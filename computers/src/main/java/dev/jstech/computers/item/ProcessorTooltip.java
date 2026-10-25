/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.EraStorage;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** What a processor's tooltip says: its cores and clock, its design, what it orchestrates, its socket and its ISA. */
@TextHolder
final class ProcessorTooltip {

    private static final TextKey CORES = TextKey.of("jsc.item.cpu.cores", "%s cores @ %s GHz");
    private static final TextKey THROUGHPUT = TextKey.of("jsc.item.cpu.throughput", "%s it/t  -  %s W");
    private static final TextKey SOCKET = TextKey.of("jsc.item.cpu.socket", "Socket %s");
    private static final TextKey GRAPHICS = TextKey.of("jsc.item.cpu.graphics",
            "%s on the die: a video output on the board, no graphics card needed");
    /* The instruction set, then what an item costs on the era's disks. */
    private static final TextKey WORD = TextKey.of("jsc.item.cpu.word", "%s  -  %s");

    private ProcessorTooltip() {
    }

    static void append(final CpuSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(CORES.with(spec.cores(),
                        String.format(Locale.ROOT, "%.2f", spec.freqMhz() / 1000.0)))
                .withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendDesign(tooltip, spec.design().label());
        if (spec.hasIntegratedGraphics()) {
            tooltip.add(GameText.component(GRAPHICS.with(spec.design().graphics().model()))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(GameText.component(THROUGHPUT.with(spec.orchestrationCapacity(), spec.tdpWatts()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(GameText.component(SOCKET.with(spec.socket().display())).withStyle(ChatFormatting.DARK_GRAY));
        /*
         * The instruction set is what decides which programs this chip will run, so it is named rather than left to be
         * guessed from the era. The word size beside it is what an item costs on that era's disks, so the player can
         * read the ladder off the chip.
         */
        tooltip.add(GameText.component(WORD.with(HardwareTooltip.isa(spec),
                        HardwareTooltip.MB_PER_ITEM.with(EraStorage.mbPerItem(spec.era()))))
                .withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
