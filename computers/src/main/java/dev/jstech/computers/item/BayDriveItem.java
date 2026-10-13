/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A part for a case's 5.25-inch bay, named in its tooltip by what it does: the discs an internal drive reads, or the
 * disk the hot-swap bay takes from the front, that it fits any bay, and its era.
 */
@TextHolder
public class BayDriveItem extends SpecItem<BayDriveSpec> {

    private static final TextKey READS = TextKey.of("jsc.item.bay_drive.reads", "Reads %s");
    private static final TextKey HOT_SWAP = TextKey.of("jsc.item.bay_drive.hot_swap",
            "Takes a disk at the front, put in or taken out with the computer on or off");
    private static final TextKey FITS = TextKey.of("jsc.item.bay_drive.fits", "Fits any 5.25-inch bay");

    public BayDriveItem(final Properties properties, final BayDriveSpec spec) {
        super(properties, spec);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final MediaDriveType reads = spec().kind().reads();
        final Text what = reads == null ? HOT_SWAP.text() : READS.with(formats(reads));
        tooltip.add(GameText.component(what).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(FITS.text()).withStyle(ChatFormatting.GRAY));
        HardwareTooltip.appendEra(tooltip, spec().era());
    }

    /* The discs a drive reads by their formats' names, newest first: "Blu-ray, DVD, CD". */
    private static Text formats(final MediaDriveType drive) {
        final List<Text> names = new ArrayList<>();
        for (final MediaFormat format : MediaFormat.values()) {
            if (format.onTray() && drive.accepts(format)) {
                // The formats run oldest first, so each one read goes ahead of those before it.
                names.addFirst(format.text());
            }
        }
        return TextLists.join(", ", names);
    }
}
