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
import java.util.Optional;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The tags: those of its block, its item, its fluid or its kind of entity, as the server sent them, each by its id's
 * path, the first few named and the rest counted.
 */
final class TagReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.PAPER);
    private static final String SEPARATOR = ", ";
    /** How many tags are named before the rest are counted. */
    private static final int NAMED = 3;

    @Override
    @SuppressWarnings("deprecation")
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        if (thing.kind() == ThingKind.RESEARCH) {
            return;
        }
        // The holders the registries keep for each entry hold the tags the server sent.
        final List<TagKey<?>> held = new ArrayList<>();
        if (thing.block() != null) {
            thing.block().builtInRegistryHolder().tags().forEach(held::add);
        }
        if (thing.item() != null) {
            thing.item().builtInRegistryHolder().tags().forEach(held::add);
        }
        if (thing.fluid() != null) {
            thing.fluid().builtInRegistryHolder().tags().forEach(held::add);
        }
        if (thing.entity() != null) {
            thing.entity().builtInRegistryHolder().tags().forEach(held::add);
        }
        final List<String> tags = held.stream().map(tag -> tag.location().getPath()).distinct().sorted().toList();
        final String named = tags.isEmpty() ? GameText.resolve(StudioTexts.NONE) : tags.size() <= NAMED
                ? String.join(SEPARATOR, tags) : GameText.resolve(StudioTexts.AND_MORE.with(
                String.join(SEPARATOR, tags.subList(0, NAMED)), tags.size() - NAMED));
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.TAGS), named,
                tags.isEmpty() ? StudioSheet.Mark.NONE : StudioSheet.Mark.FINE, Optional.empty()));
    }
}
