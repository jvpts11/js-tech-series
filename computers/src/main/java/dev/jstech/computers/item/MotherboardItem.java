/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.audio.SoundHardwareTexts;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A motherboard item, the chassis that bounds a build (socket and counts of CPU/RAM/PCIe slots).
 */
@TextHolder
public class MotherboardItem extends SpecItem<MotherboardSpec> {

    private static final TextKey FORM_FACTOR = TextKey.of("jsc.item.motherboard.form_factor", "%s form factor");
    /* CPU slots and socket, memory slots and generations, card slots and bus. */
    private static final TextKey SLOTS = TextKey.of("jsc.item.motherboard.slots", "%sx %s  |  %s RAM (%s)  |  %sx %s");
    private static final TextKey OWN_ERA =
            TextKey.of("jsc.item.motherboard.own_era", "Seats parts of its own era only");

    public MotherboardItem(final Properties properties, final MotherboardSpec spec) {
        super(properties, spec);
    }

    public static boolean fits(final ItemStack stack, final Set<FormFactor> acceptedFormFactors) {
        return stack.getItem() instanceof MotherboardItem board
                && acceptedFormFactors.contains(board.spec().formFactor());
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final MotherboardSpec spec = spec();
        tooltip.add(GameText.component(FORM_FACTOR.with(spec.formFactor().label())).withStyle(ChatFormatting.AQUA));
        final String ramTypes = spec.acceptedRam().stream()
                .sorted()
                .map(RamGeneration::name)
                .collect(Collectors.joining(" / "));
        /*
         * The board is where a build succeeds or fails, so it spells out exactly what its slots take:
         * the socket, the memory generations, and the bus version cards are held to.
         */
        tooltip.add(GameText.component(SLOTS.with(spec.cpuSlots(), spec.socket().display(), spec.ramSlots(), ramTypes,
                spec.pcieSlots(), spec.pcieGeneration())).withStyle(ChatFormatting.GRAY));
        if (spec.era() == HardwareEra.STANDARD) {
            tooltip.add(GameText.component(SoundHardwareTexts.ON_BOARD_AUDIO).withStyle(ChatFormatting.GRAY));
        }
        HardwareTooltip.appendEra(tooltip, spec.era());
        tooltip.add(GameText.component(OWN_ERA).withStyle(ChatFormatting.DARK_GRAY));
    }
}
