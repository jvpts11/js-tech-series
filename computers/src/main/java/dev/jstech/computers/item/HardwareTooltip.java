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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The lines every hardware component's tooltip shares. A board seats only parts of its own generation,
 * so the generation has to be readable on the part itself, because otherwise the only way to find out whether
 * two pieces go together is to try them in the slot and watch nothing happen.
 *
 * <p>One place, so the wording cannot drift between a CPU and the memory it has to match.
 */
@TextHolder
public final class HardwareTooltip {

    /** The small print of an expansion card: its tier, what it draws and the slot it takes. */
    public static final TextKey TIER_POWER_BUS =
            TextKey.of("jsc.item.hardware_tooltip.tier_power_bus", "%s  -  %s W  -  %s");

    /** What a part draws: "65 W". */
    public static final TextKey WATTS = TextKey.of("jsc.item.hardware_tooltip.watts", "%s W");

    /** How much an item weighs on a disk of the part's era: "16 MB per item". */
    public static final TextKey MB_PER_ITEM = TextKey.of("jsc.item.hardware_tooltip.mb_per_item", "%s MB per item");

    /** An architecture and the width of its word: "x86-64, 64-bit". */
    private static final TextKey ARCHITECTURE = TextKey.of("jsc.item.hardware_tooltip.architecture", "%s, %s-bit");

    private HardwareTooltip() {
    }

    /**
     * Appends the component's hardware generation, the axis that decides what fits with what, in the
     * colour of that generation's screens so the era reads before the word does.
     */
    public static void appendEra(final List<Component> tooltip, final HardwareEra era) {
        if (era == null) {
            return;
        }
        tooltip.add(eraName(era, era.named()));
    }

    /** {@code text} in the colour of {@code era}'s screens: the one styling every era mention shares. */
    public static Component eraName(final HardwareEra era, final Text text) {
        return GameText.component(text).withStyle(style -> style.withColor(era.screenColor()));
    }

    /**
     * How a processor's architecture reads wherever it is shown: "x86-64, 64-bit". The word size comes from the
     * architecture rather than from the era, since it is the architecture's own, and one place says it so a chip's
     * tooltip and a machine's screens cannot come to word it differently.
     */
    public static Text architecture(final CpuSpec cpu) {
        return ARCHITECTURE.with(cpu.architecture().name(), cpu.architecture().bits());
    }
}
