/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.tooltip.ItemTooltip;
import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.client.gui.tooltip.TooltipTexts;
import dev.jstech.core.material.MaterialMarks;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.MaterialThing;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The lines a material's things show in their tooltips, the game's own iron ingot included: each property the material
 * has a value for, those shown always, and in the part Shift shows those shown only then; then the marks it bears
 * ({@link MaterialMarks}). The tooltip says that Shift shows more, as for every part that waits for it.
 */
@TextHolder
public final class MaterialTooltips {

    public static final TextKey LINE = TextKey.of("jscore.material.tooltip.line", "%s: %s");

    private MaterialTooltips() {
    }

    /** Hands the Core's tooltips what a material's things say, once, from the Core's client constructor. */
    public static void listen() {
        ItemTooltips.add(JsCore.MODID, MaterialTooltips::describe);
    }

    /** The lines {@code thing}'s tooltip shows, with Shift held or not, the hint included when something waits. */
    public static List<Component> lines(final MaterialThing thing, final boolean shift) {
        final List<Component> always = new ArrayList<>();
        final List<Component> shifted = new ArrayList<>();
        collect(thing, always, shifted);
        final List<Component> out = new ArrayList<>(always);
        if (shift) {
            out.addAll(shifted);
        } else if (!shifted.isEmpty()) {
            out.add(GameText.component(TooltipTexts.MORE).copy().withStyle(ChatFormatting.DARK_GRAY));
        }
        return out;
    }

    private static void describe(final ItemStack stack, final ItemTooltip tooltip) {
        Materials.of(stack.getItem()).ifPresent(thing -> {
            final List<Component> always = new ArrayList<>();
            final List<Component> shifted = new ArrayList<>();
            collect(thing, always, shifted);
            always.forEach(tooltip::line);
            shifted.forEach(tooltip.shift()::line);
        });
    }

    /* The lines shown always, then the marks, into always; those that wait for Shift into shifted. */
    private static void collect(final MaterialThing thing, final List<Component> always,
                                final List<Component> shifted) {
        for (final MaterialValues.Line line : MaterialValues.lines(thing.material(), true)) {
            final Component said = GameText.component(LINE.with(line.property().name().text(), line.value()))
                    .copy().withStyle(ChatFormatting.GRAY);
            (line.shown() == MaterialProperty.Shown.SHIFT ? shifted : always).add(said);
        }
        for (final MaterialMarks.Mark mark : MaterialMarks.of(thing.material())) {
            always.add(GameText.component(mark.text()).copy().withStyle(ChatFormatting.YELLOW));
        }
    }
}
