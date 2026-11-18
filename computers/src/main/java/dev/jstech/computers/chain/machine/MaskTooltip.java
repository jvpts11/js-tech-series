/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.chain.ChainChips;
import dev.jstech.core.guide.TextTable;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What a Mask Blank and a mask say of themselves (computers/docs/design/survival.md, "Masks"): how a blank becomes a
 * mask, by photo-reduction from a Layout in the Vintage and by the Lithography program from the Legacy on, and that a
 * mask is never used up, every wafer of its chip exposed through it.
 */
@TextHolder
public final class MaskTooltip {

    public static final TextKey BLANK = TextKey.of("jsc.mask.blank", "Becomes a mask in a Lithography Station's mask"
            + " slot.");
    public static final TextKey BLANK_VINTAGE = TextKey.of("jsc.mask.blank.vintage",
            "Vintage: photo-reduced from a Layout.");
    public static final TextKey BLANK_LEGACY = TextKey.of("jsc.mask.blank.legacy",
            "Legacy on: written by the Lithography program on the station's computer.");
    public static final TextKey NEVER_USED_UP = TextKey.of("jsc.mask.never_used_up",
            "Never used up: every %s wafer of the %s is exposed through it in a station's mask slot.");
    public static final TextKey WRITTEN = TextKey.of("jsc.mask.written", "Written by the Lithography program.");
    public static final TextKey PHOTO_REDUCED = TextKey.of("jsc.mask.photo_reduced",
            "Photo-reduced from a Layout in the Vintage station.");
    /* How many letters a line of the tooltip holds, as the game's own long tooltips are kept narrow. */
    private static final int LINE = 38;

    private MaskTooltip() {
    }

    /** Adds a blank's or a mask's lines under its name; the mod's listener on the game's event. */
    public static void onTooltip(final ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        final List<Text> said = new ArrayList<>();
        if (stack.is(ChainChips.MASK_BLANK.get())) {
            said.add(BLANK.text());
            said.add(BLANK_VINTAGE.text());
            said.add(BLANK_LEGACY.text());
        } else {
            final Optional<MaskWriting.Offer> offer = MaskWriting.offerOf(stack.getItem());
            if (offer.isEmpty()) {
                return;
            }
            said.add(NEVER_USED_UP.with(offer.get().name(), offer.get().era().name()));
            said.add(offer.get().era() == HardwareEra.VINTAGE ? PHOTO_REDUCED.text() : WRITTEN.text());
        }
        final List<Component> lines = new ArrayList<>();
        for (final Text text : said) {
            for (final String line : TextTable.wrap(GameText.resolve(text), LINE, String::length)) {
                lines.add(GameText.component(Text.literal(line)).withStyle(ChatFormatting.GRAY));
            }
        }
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }
}
