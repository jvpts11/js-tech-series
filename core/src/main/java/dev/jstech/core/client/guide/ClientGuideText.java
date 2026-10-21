/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.multiblock.GhostBlocks;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideCode;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.guide.GuideHighlighters;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.TextFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The manual's layout answered from the player's game: words in the loaded language with the values they name written
 * in, widths from the game's fonts in the style's, recipes from the world the player is in, numbers written the
 * player's way, multiblocks as the server has them.
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
            return GuideFacts.fill(pattern, this);
        }
        final List<String> written = new ArrayList<>();
        for (final Object arg : args) {
            written.add(String.valueOf(arg));
        }
        return GuideFacts.fill(TextFormat.apply(pattern, written), this);
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

    @Override
    public List<List<GuideCode.Run>> highlight(final String language, final List<String> lines) {
        return GuideHighlighters.colour(language, lines);
    }

    @Override
    public String elsewhere(final String target) {
        return GuideLibrary.loaded().manualForTarget(target).map(manual -> this.text(manual.titleKey())).orElse("");
    }

    @Override
    public String titleOf(final String target) {
        return GuideLibrary.loaded().titleKeyOf(target).map(key -> this.text(key)).orElse("");
    }

    /** The multiblock in force on the player's side, its controller in its place, each place its first block. */
    @Override
    public List<GuideBlock.SceneBlock> multiblock(final String id) {
        final ResourceLocation multiblock = ResourceLocation.tryParse(id);
        final Optional<ShapePreview> laid = multiblock == null ? Optional.empty()
                : Projectables.preview(multiblock, 0);
        return laid.map(preview -> GuideBlock.Multiblock.blocksOf(preview, first -> {
            final Block block = GhostBlocks.blockOf(first);
            return block == null ? "" : BuiltInRegistries.BLOCK.getKey(block).toString();
        })).orElse(List.of());
    }

    /** The structure saved in the mod's assets, read from its file. */
    @Override
    public List<GuideBlock.SceneBlock> structure(final String id) {
        return GuideStructures.blocksOf(id);
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
