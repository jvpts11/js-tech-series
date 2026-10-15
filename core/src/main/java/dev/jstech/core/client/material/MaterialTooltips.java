/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

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
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The lines a material's things show in their tooltips, the game's own iron ingot included: each property the material
 * has a value for, those shown always, and with Shift held those shown only then; then the marks it bears
 * ({@link MaterialMarks}). When some line waits for Shift, the tooltip says so.
 */
@TextHolder
public final class MaterialTooltips {

    public static final TextKey LINE = TextKey.of("jscore.material.tooltip.line", "%s: %s");
    public static final TextKey MORE = TextKey.of("jscore.material.tooltip.more", "Hold Shift for more");

    private MaterialTooltips() {
    }

    /** Listens for tooltips, once, from the Core's client constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, MaterialTooltips::onTooltip);
    }

    /** The lines {@code thing}'s tooltip adds, with Shift held or not. */
    public static List<Component> lines(final MaterialThing thing, final boolean shift) {
        final List<MaterialValues.Line> lines = MaterialValues.lines(thing.material(), true);
        final List<Component> out = new ArrayList<>();
        boolean waiting = false;
        for (final MaterialValues.Line line : lines) {
            if (line.shown() == MaterialProperty.Shown.SHIFT && !shift) {
                waiting = true;
                continue;
            }
            out.add(GameText.component(LINE.with(line.property().name().text(), line.value()))
                    .withStyle(ChatFormatting.GRAY));
        }
        for (final MaterialMarks.Mark mark : MaterialMarks.of(thing.material())) {
            out.add(GameText.component(mark.text()).withStyle(ChatFormatting.YELLOW));
        }
        if (waiting) {
            out.add(GameText.component(MORE).withStyle(ChatFormatting.DARK_GRAY));
        }
        return out;
    }

    private static void onTooltip(final ItemTooltipEvent event) {
        final Optional<MaterialThing> thing = Materials.of(event.getItemStack().getItem());
        thing.ifPresent(found -> event.getToolTip().addAll(lines(found, Screen.hasShiftDown())));
    }
}
