/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * A creative tab of a mod, made of sections shown one after another. A block or an item joins a section where it is
 * declared, so the tab is never a second list to keep in step with the registrations: whatever is declared into a
 * section is in the tab, in the order it was declared.
 *
 * <p>A tab and each of its sections may also name item tags of the mod: every item declared into them is written into
 * those tags when the mod's data is generated. A tag that groups things the way the tab groups them (one era of
 * hardware, a kind of part) is then never a third list to keep in step, and a datapack or a script can ask for "any
 * of these" through it.
 */
public final class ContentTab {

    private final ResourceLocation id;
    private final TextKey title;
    private final Supplier<? extends ItemLike> icon;
    private final List<Section> sections = new ArrayList<>();
    private final List<TagKey<Item>> tags = new ArrayList<>();
    /** The tab of the same mod this one stands right after, or null for a mod's first tab. */
    private final @Nullable ContentTab after;

    ContentTab(final String modid, final String id, final String englishTitle, final Supplier<? extends ItemLike> icon,
               final @Nullable ContentTab after) {
        this.id = ResourceLocation.fromNamespaceAndPath(modid, id);
        this.title = new TextKey("itemGroup." + modid + "." + id, englishTitle);
        this.icon = icon;
        this.after = after;
    }

    /** A new section, shown after every section made before it. */
    public Section section() {
        final Section section = new Section(id.getNamespace());
        sections.add(section);
        return section;
    }

    /** Puts every item of every section of the tab into the mod's item tag at {@code path}. */
    public ContentTab tagging(final String path) {
        tags.add(itemTag(id.getNamespace(), path));
        return this;
    }

    /** The item tags every item of the tab is written into, besides those of its own section. */
    public List<TagKey<Item>> tags() {
        return Collections.unmodifiableList(tags);
    }

    /** The tab's sections, in the order it shows them. */
    public List<Section> sections() {
        return Collections.unmodifiableList(sections);
    }

    /** The tab's name, which the language generator writes. */
    public TextKey title() {
        return title;
    }

    /** The tab's registry name, the mod's namespace and the id it was declared with. */
    public ResourceLocation id() {
        return id;
    }

    CreativeModeTab build() {
        final CreativeModeTab.Builder builder = CreativeModeTab.builder()
                .title(GameText.component(title))
                .icon(() -> new ItemStack(icon.get()))
                .displayItems((parameters, output) -> sections.forEach(section -> section.fill(output)));
        // A tab with no order of its own goes anywhere after the game's tabs; a mod's tabs keep the order it gave.
        if (after != null) {
            builder.withTabsBefore(after.id);
        }
        return builder.build();
    }

    private static TagKey<Item> itemTag(final String modid, final String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(modid, path));
    }

    /**
     * A run of a tab: the blocks and items declared into it, in declaration order unless an order is given, then
     * whatever stacks it was asked to show besides (items made up at the moment, such as a disc with a program
     * written on it).
     */
    public static final class Section {

        private final String modid;
        private final List<Supplier<? extends ItemLike>> entries = new ArrayList<>();
        private final List<Consumer<CreativeModeTab.Output>> more = new ArrayList<>();
        private final List<TagKey<Item>> tags = new ArrayList<>();
        private @Nullable Comparator<? super Item> order;
        private Predicate<Item> shownIf = item -> true;

        private Section(final String modid) {
            this.modid = modid;
        }

        /**
         * Puts an item declared elsewhere into the section too: a tab that gathers what several places declare, such
         * as a tab of content many mods share.
         */
        public Section with(final Supplier<? extends ItemLike> entry) {
            entries.add(entry);
            return this;
        }

        /** Shows only the entries {@code shown} lets through, asked each time the tab is filled. */
        public Section shownIf(final Predicate<Item> shown) {
            this.shownIf = shown;
            return this;
        }

        /** The items the section shows now: its entries the section lets through, in its order. */
        public List<Item> shown() {
            final List<Item> items = new ArrayList<>(items());
            items.removeIf(shownIf.negate());
            if (order != null) {
                // A stable sort: entries the order ranks alike keep their declaration order.
                items.sort(order);
            }
            return items;
        }

        /** Shows these stacks too, after the declared entries. */
        public Section alsoShowing(final Consumer<CreativeModeTab.Output> stacks) {
            more.add(stacks);
            return this;
        }

        /** Shows the declared entries in this order rather than the order they were declared in. */
        public Section sortedBy(final Comparator<? super Item> itemOrder) {
            this.order = itemOrder;
            return this;
        }

        /**
         * Puts every item declared into the section into the mod's item tag at {@code path}. The stacks it only shows
         * besides are not declared items and stay out; their items are tagged where they are declared.
         */
        public Section tagging(final String path) {
            tags.add(itemTag(modid, path));
            return this;
        }

        /** The item tags the section's declared items are written into. */
        public List<TagKey<Item>> tags() {
            return Collections.unmodifiableList(tags);
        }

        /**
         * The items declared into the section, in declaration order. Read once the game's registries are filled, as
         * the data run does; before that, a declared item does not exist yet.
         */
        public List<Item> items() {
            final List<Item> items = new ArrayList<>(entries.size());
            entries.forEach(entry -> items.add(entry.get().asItem()));
            return items;
        }

        void add(final Supplier<? extends ItemLike> entry) {
            entries.add(entry);
        }

        private void fill(final CreativeModeTab.Output output) {
            shown().forEach(output::accept);
            more.forEach(stacks -> stacks.accept(output));
        }
    }
}
