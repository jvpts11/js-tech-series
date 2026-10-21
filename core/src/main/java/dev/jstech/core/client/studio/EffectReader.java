/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.effect.EffectFiles;
import dev.jstech.core.developer.ThingNames;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The effects: the effect files of the thing's mod named after it, as {@code compressor_steam} is the compressor's,
 * the first of them opened by Open. A thing no effect is named after has no row.
 */
final class EffectReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.BLAZE_POWDER);

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        final List<ResourceLocation> effects = EffectFiles.ids().stream()
                .filter(effect -> effect.getNamespace().equals(thing.id().getNamespace())
                        && ThingNames.names(effect.getPath(), thing.id().getPath()))
                .sorted().toList();
        if (effects.isEmpty()) {
            return;
        }
        final ResourceLocation first = effects.getFirst();
        final String path = EffectFiles.path(first);
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.EFFECTS), effects.size() == 1 ? path
                : GameText.resolve(StudioTexts.AND_MORE.with(path, effects.size() - 1)), StudioSheet.Mark.FINE,
                Optional.of(StudioFiles.asset(ResourceLocation.fromNamespaceAndPath(first.getNamespace(), path)))));
    }
}
