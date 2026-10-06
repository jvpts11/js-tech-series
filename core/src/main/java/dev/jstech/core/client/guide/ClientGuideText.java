/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.text.TextFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The manual's layout answered from the player's game: words in the loaded language, widths from the game's fonts in
 * the style's, recipes from the world the player is in, numbers written the player's way.
 */
public final class ClientGuideText implements IGuideText {

    private final Font font;
    private final GuideStyle style;
    private final GuideRecipes recipes;

    public ClientGuideText(final Font font, final GuideStyle style, final GuideRecipes recipes) {
        this.font = font;
        this.style = style;
        this.recipes = recipes;
    }

    @Override
    public String text(final String key, final Object... args) {
        final String pattern = Language.getInstance().getOrDefault(key, key);
        if (args.length == 0) {
            return pattern;
        }
        final List<String> written = new ArrayList<>();
        for (final Object arg : args) {
            written.add(String.valueOf(arg));
        }
        return TextFormat.apply(pattern, written);
    }

    @Override
    public int width(final String text, final TextSize size) {
        return GuideFonts.width(this.font, text, size, this.style);
    }

    @Override
    public int recipeCount(final String type, final String output) {
        return this.recipes.of(type, output).size();
    }

    @Override
    public String amount(final long value, final String unit) {
        final String number = GameLocale.count(value);
        return unit.isEmpty() ? number : number + " " + unit;
    }

    @Override
    public String itemName(final String item) {
        return itemNameOf(item);
    }

    /** An item's name in the loaded language, read from the game's items; its id's path when it has none. */
    public static String itemNameOf(final String item) {
        final ResourceLocation id = ResourceLocation.tryParse(item);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return GuideIds.path(item);
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName().getString();
    }
}
