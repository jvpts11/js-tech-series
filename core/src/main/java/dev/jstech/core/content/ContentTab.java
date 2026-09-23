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
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * A creative tab of a mod, made of sections shown one after another. A block or an item joins a section where it is
 * declared, so the tab is never a second list to keep in step with the registrations: whatever is declared into a
 * section is in the tab, in the order it was declared.
 */
public final class ContentTab {

    private final TextKey title;
    private final Supplier<? extends ItemLike> icon;
    private final List<Section> sections = new ArrayList<>();

    ContentTab(final String modid, final String id, final String englishTitle, final Supplier<? extends ItemLike> icon) {
        this.title = new TextKey("itemGroup." + modid + "." + id, englishTitle);
        this.icon = icon;
    }

    /** A new section, shown after every section made before it. */
    public Section section() {
        final Section section = new Section();
        sections.add(section);
        return section;
    }

    /** The tab's name, which the language generator writes. */
    public TextKey title() {
        return title;
    }

    CreativeModeTab build() {
        return CreativeModeTab.builder()
                .title(GameText.component(title))
                .icon(() -> new ItemStack(icon.get()))
                .displayItems((parameters, output) -> sections.forEach(section -> section.fill(output)))
                .build();
    }

    /**
     * A run of a tab: the blocks and items declared into it, in declaration order unless an order is given, then
     * whatever stacks it was asked to show besides (items made up at the moment, such as a disc with a program
     * written on it).
     */
    public static final class Section {

        private final List<Supplier<? extends ItemLike>> entries = new ArrayList<>();
        private final List<Consumer<CreativeModeTab.Output>> more = new ArrayList<>();
        private @Nullable Comparator<? super Item> order;

        private Section() {
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

        void add(final Supplier<? extends ItemLike> entry) {
            entries.add(entry);
        }

        private void fill(final CreativeModeTab.Output output) {
            final List<Item> items = new ArrayList<>(entries.size());
            entries.forEach(entry -> items.add(entry.get().asItem()));
            if (order != null) {
                // A stable sort: entries the order ranks alike keep their declaration order.
                items.sort(order);
            }
            items.forEach(output::accept);
            more.forEach(stacks -> stacks.accept(output));
        }
    }
}
