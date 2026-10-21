/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The names: the languages of the thing's mod that name it. Wrong for each language the mod names things in and not
 * this one, which opens that language's file.
 */
final class NameReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.NAME_TAG);
    private static final String SEPARATOR = ", ";

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        final String key = thing.nameKey();
        final Map<String, Set<String>> languages = context.languages(thing.modid());
        final List<String> naming = new ArrayList<>();
        final List<String> silent = new ArrayList<>();
        languages.forEach((code, keys) -> (keys.contains(key) ? naming : silent).add(code));
        final boolean wrong = naming.isEmpty() || !silent.isEmpty();
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.NAMES),
                naming.isEmpty() ? GameText.resolve(StudioTexts.NONE) : String.join(SEPARATOR, naming),
                wrong ? StudioSheet.Mark.ERROR : StudioSheet.Mark.FINE, Optional.empty()));
        if (languages.isEmpty()) {
            sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR, GameText.resolve(StudioTexts.NO_LANGUAGES),
                    GameText.resolve(StudioTexts.NAMED_NOWHERE.with(key)), Optional.empty()));
        }
        for (final String code : silent) {
            final String detail = naming.isEmpty() ? GameText.resolve(StudioTexts.NAMED_NOWHERE.with(key))
                    : GameText.resolve(StudioTexts.ONLY_IN.with(key, String.join(SEPARATOR, naming)));
            sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR, GameText.resolve(StudioTexts.NO_NAME_IN
                    .with(code)), detail, Optional.of(StudioFiles.asset(ResourceLocation.fromNamespaceAndPath(
                    thing.modid(), "lang/" + code + ".json")))));
        }
    }
}
