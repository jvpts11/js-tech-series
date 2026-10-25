/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.audio.SoundHardwareTexts;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** What a motherboard's tooltip says: its form, what its slots take, its video and audio, and its era. */
@TextHolder
final class MotherboardTooltip {

    private static final TextKey FORM_FACTOR = TextKey.of("jsc.item.motherboard.form_factor", "%s form factor");
    /* CPU slots and socket, memory slots and generations, card slots and bus. */
    private static final TextKey SLOTS = TextKey.of("jsc.item.motherboard.slots", "%sx %s  |  %s RAM (%s)  |  %sx %s");
    private static final TextKey OWN_ERA =
            TextKey.of("jsc.item.motherboard.own_era", "Seats parts of its own era only");
    /* A board of two buses: the older bus its slots also take. */
    private static final TextKey OLDER_BUS =
            TextKey.of("jsc.item.motherboard.older_bus", "Its slots take %s cards too");
    /* Whether a screen can be plugged into the board itself, which decides whether a card slot goes to graphics. */
    private static final TextKey OWN_VIDEO =
            TextKey.of("jsc.item.motherboard.own_video", "Has a screen output of its own, for its console");
    private static final TextKey NO_VIDEO = TextKey.of("jsc.item.motherboard.no_video",
            "No screen output of its own: a screen needs a graphics card, or a processor with graphics");

    private MotherboardTooltip() {
    }

    static void append(final MotherboardSpec spec, final List<Component> tooltip) {
        tooltip.add(GameText.component(FORM_FACTOR.with(spec.formFactor().label())).withStyle(ChatFormatting.AQUA));
        // The generations in the order they came, the way the declared ones are kept.
        final List<RamGeneration> order = RamGeneration.all();
        final String ramTypes = spec.acceptedRam().stream()
                .sorted(Comparator.comparingInt(order::indexOf))
                .map(generation -> generation.label().english())
                .collect(Collectors.joining(" / "));
        /*
         * The board is where a build succeeds or fails, so it spells out exactly what its slots take:
         * the socket, the memory generations, and the bus version cards are held to, each as its sheet names it.
         */
        tooltip.add(GameText.component(SLOTS.with(spec.cpuSlots(), spec.socket().display(), spec.ramSlots(), ramTypes,
                spec.pcieSlots(), spec.pcieGeneration().label())).withStyle(ChatFormatting.GRAY));
        if (spec.olderBus() != null) {
            tooltip.add(GameText.component(OLDER_BUS.with(spec.olderBus().label())).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(GameText.component(spec.hasOwnVideo() ? OWN_VIDEO : NO_VIDEO).withStyle(ChatFormatting.GRAY));
        if (spec.hasOnBoardAudio()) {
            tooltip.add(GameText.component(SoundHardwareTexts.ON_BOARD_AUDIO).withStyle(ChatFormatting.GRAY));
        }
        HardwareTooltip.appendEra(tooltip, spec.era());
        tooltip.add(GameText.component(OWN_ERA).withStyle(ChatFormatting.DARK_GRAY));
    }
}
