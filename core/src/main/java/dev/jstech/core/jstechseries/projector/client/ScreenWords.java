/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector.client;

import dev.jstech.core.client.multiblock.GhostBlocks;
import dev.jstech.core.jstechseries.projector.ProjectorTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/** The words the projector puts together on the player's side: a multiblock's mod, and a count of blocks by name. */
final class ScreenWords {

    private ScreenWords() {
    }

    /** The name of the mod a multiblock of {@code id} comes from, as the mod calls itself. */
    static Text modName(final ResourceLocation id) {
        return Text.literal(ModList.get().getModContainerById(id.getNamespace())
                .map(container -> container.getModInfo().getDisplayName()).orElse(id.getNamespace()));
    }

    /** The first {@code most} of {@code counts}, each block's name and how many: "Bricks 10, Glass 1". */
    static Text counts(final Map<String, Integer> counts, final int most) {
        Text out = Text.EMPTY;
        int named = 0;
        for (final Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (named++ == most) {
                break;
            }
            final Text piece = ProjectorTexts.COUNTED.with(GameText.of(GhostBlocks.nameOf(entry.getKey())),
                    entry.getValue());
            out = out.isEmpty() ? piece : ProjectorTexts.AND_MORE.with(out, piece);
        }
        return out;
    }
}
