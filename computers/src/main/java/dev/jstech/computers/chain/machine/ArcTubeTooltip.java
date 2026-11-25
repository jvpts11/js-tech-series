/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.guide.TextTable;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What the Mercury Arc Tube says of itself (computers/docs/design/survival.md): the lamp of a Lithography Station's
 * mercury light, made as the real i-line lamp is, a bulb of fused quartz with tungsten electrodes and a bead of
 * mercury.
 */
@TextHolder
public final class ArcTubeTooltip {

    public static final TextKey MADE_OF = TextKey.of("jsc.mercury_arc_tube.made_of",
            "Fused quartz, tungsten electrodes and a bead of mercury");
    /* How many letters a line of the tooltip holds, the electrodes ending the first. */
    private static final int LINE = 34;

    private ArcTubeTooltip() {
    }

    /** Adds the tube's lines under its name; the mod's listener on the game's event. */
    public static void onTooltip(final ItemTooltipEvent event) {
        if (!event.getItemStack().is(ChainParts.MERCURY_ARC_TUBE.get())) {
            return;
        }
        final List<Component> lines = new ArrayList<>();
        for (final String line : TextTable.wrap(GameText.resolve(MADE_OF), LINE, String::length)) {
            lines.add(GameText.component(Text.literal(line)).withStyle(ChatFormatting.GRAY));
        }
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }
}
