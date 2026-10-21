/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;

/** Whether the thing's item is in a creative tab: a warning when a player in creative cannot find it. */
final class TabReader implements IStudioReader {

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        if (thing.item() != null && !context.inATab(thing.item())) {
            sheet.check(new StudioSheet.Check(StudioSheet.Mark.WARNING, GameText.resolve(StudioTexts.IN_NO_TAB),
                    GameText.resolve(StudioTexts.IN_NO_TAB_DETAIL.with(BuiltInRegistries.ITEM.getKey(thing.item())
                            .toString())), Optional.empty()));
        }
    }
}
