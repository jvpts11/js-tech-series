/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The manual page: the entry a manual gives the thing's item, as its tooltip offers to open. */
final class ManualReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.BOOK);

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        if (thing.icon().isEmpty() || thing.kind() == ThingKind.RESEARCH) {
            return;
        }
        final Optional<String> entry = GuideClient.entryOf(thing.icon());
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.MANUAL),
                entry.orElseGet(() -> GameText.resolve(StudioTexts.NONE)),
                entry.isPresent() ? StudioSheet.Mark.FINE : StudioSheet.Mark.NONE, Optional.empty()));
    }
}
