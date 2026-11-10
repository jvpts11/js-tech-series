/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What a wafer on its way to chips says of itself (computers/docs/design/survival.md, "How a chip is made"): the chip
 * its mask makes of it, the layer it is on of its layers, and the machine it goes to next.
 */
@TextHolder
public final class WaferTooltip {

    public static final TextKey CHIP = TextKey.of("jsc.wafer.chip", "Chip: %s");
    public static final TextKey LAYER = TextKey.of("jsc.wafer.layer", "Layer %s of %s");
    public static final TextKey NEXT = TextKey.of("jsc.wafer.next", "Next: %s");
    public static final TextKey LITHOGRAPHY = TextKey.of("jsc.wafer.step.lithography", "a Lithography Station");
    public static final TextKey ETCH = TextKey.of("jsc.wafer.step.etch", "the Plasma Etcher");
    public static final TextKey IMPLANT = TextKey.of("jsc.wafer.step.implant", "the Ion Implanter");
    public static final TextKey DEPOSITION = TextKey.of("jsc.wafer.step.deposition", "the Deposition Chamber");
    public static final TextKey PACKAGING = TextKey.of("jsc.wafer.step.packaging", "the Packaging Line");

    private WaferTooltip() {
    }

    /** Adds a wafer's lines under its name; the mod's listener on the game's event. */
    public static void onTooltip(final ItemTooltipEvent event) {
        final WaferWork work = event.getItemStack().get(ComputingComponents.WAFER_WORK.get());
        if (work == null) {
            return;
        }
        final List<Component> tooltip = event.getToolTip();
        final int at = Math.min(1, tooltip.size());
        // Each goes in at the same place, so they read in the order chip, layer, next.
        tooltip.add(at, GameText.component(NEXT.with(step(work.next()).text())).withStyle(ChatFormatting.GRAY));
        if (work.next() != WaferWork.Step.PACKAGING) {
            tooltip.add(at, GameText.component(LAYER.with(work.layer(), work.layers()))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(at, GameText.component(CHIP.with(GameText.of(BuiltInRegistries.ITEM.get(work.chip())
                .getDescription()))).withStyle(ChatFormatting.GRAY));
    }

    private static TextKey step(final WaferWork.Step step) {
        return switch (step) {
            case LITHOGRAPHY -> LITHOGRAPHY;
            case ETCH -> ETCH;
            case IMPLANT -> IMPLANT;
            case DEPOSITION -> DEPOSITION;
            case PACKAGING -> PACKAGING;
        };
    }
}
