/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Tags and registries: the tags the thing chosen's item is in, with where it is registered; what a tag chosen holds;
 * and which pack's file of the tag put each entry in it, the mod's own or a data pack's.
 */
final class TagsPage extends StudioPage {

    private final List<TagKey<Item>> tags = new ArrayList<>();
    private final Map<String, String> cameFrom = new LinkedHashMap<>();
    private @Nullable StudioThing thing;
    private int chosen;

    private static final int ROW = 13;
    private static final int SLOT = 20;
    private static final int NAME_ROW = 22;
    private static final String ITEM = "item";
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    TagsPage(final StudioScreen studio) {
        super(studio);
    }

    @Override
    TextKey tab() {
        return StudioTool.TAGS.title();
    }

    @Override
    List<String> crumbs() {
        return this.thing == null ? List.of(words(this.tab())) : List.of(words(this.tab()), this.thing.name()
                .getString());
    }

    @Override
    List<String> lines() {
        return this.tags.stream().map(tag -> tag.location().toString()).toList();
    }

    @Override
    void shown() {
        this.thing = this.studio.thing().orElse(null);
        this.tags.clear();
        this.chosen = 0;
        if (this.thing != null && this.thing.item() != null) {
            this.thing.item().builtInRegistryHolder().tags().sorted((a, b) -> a.location().compareTo(b.location()))
                    .forEach(this.tags::add);
        }
        this.ask();
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final List<StudioLayout.Rect> columns = StudioLayout.columns(this.regions.body(), 0.30, 0.38);
        final StudioLayout.Rect left = columns.get(0);
        StudioDraw.panel(g, font(), left, this.thing == null ? words(this.tab())
                : GameText.resolve(ToolTexts.IS_IN.with(this.thing.name().getString())));
        if (this.thing == null || this.thing.item() == null) {
            StudioDraw.small(g, font(), words(ToolTexts.CHOOSE_FIRST), left.x() + StudioDraw.INSET,
                    left.y() + StudioDraw.HEAD + 4, left.width() - 2 * StudioDraw.INSET, colours.dim());
            StudioDraw.panel(g, font(), columns.get(1), "");
            StudioDraw.panel(g, font(), columns.get(2), words(ToolTexts.CAME_FROM));
            return;
        }
        if (this.tags.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.IN_NO_TAG), left.x() + StudioDraw.INSET,
                    left.y() + StudioDraw.HEAD + 4, left.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        int y = left.y() + StudioDraw.HEAD + 4;
        for (int i = 0; i < this.tags.size() && y + ROW < left.y() + left.height() / 2; i++) {
            final boolean on = i == this.chosen;
            if (on) {
                g.fill(left.x() + 3, y - 2, left.right() - 3, y + ROW - 3, colours.selected());
                Draw.outline(g, left.x() + 3, y - 2, left.width() - 6, ROW - 1, colours.outline());
            }
            StudioDraw.text(g, font(), this.tags.get(i).location().toString(), left.x() + 8, y,
                    left.width() - 12, on ? colours.text() : colours.label());
            y += ROW;
        }
        this.registries(g, left, colours);
        this.holds(g, columns.get(1), colours);
        this.came(g, columns.get(2), colours);
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final StudioLayout.Rect left = StudioLayout.columns(this.regions.body(), 0.30, 0.38).get(0);
        if (!left.contains(mouseX, mouseY)) {
            return false;
        }
        final int row = (int) ((mouseY - left.y() - StudioDraw.HEAD - 2) / ROW);
        if (row >= 0 && row < this.tags.size()) {
            this.chosen = row;
            this.ask();
        }
        return true;
    }

    /* Where the item is registered: its id, its mod, how many make a stack, and its components. */
    private void registries(final GuiGraphics g, final StudioLayout.Rect left, final StudioPalette.Colours colours) {
        final Item item = this.thing.item();
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        int y = left.y() + left.height() / 2;
        StudioDraw.text(g, font(), words(ToolTexts.REGISTRIES), left.x() + StudioDraw.INSET, y, left.width(),
                colours.text());
        y += 14;
        final String mod = StudioThings.modName(id.getNamespace());
        StudioDraw.row(g, font(), left, y, words(ToolTexts.REGISTRY_ID), id.toString(), colours.label());
        StudioDraw.row(g, font(), left, y + StudioDraw.SMALL_ROW, words(ToolTexts.REGISTRY_FROM),
                mod.isEmpty() ? id.getNamespace() : mod, colours.label());
        StudioDraw.row(g, font(), left, y + 2 * StudioDraw.SMALL_ROW, words(ToolTexts.REGISTRY_STACK),
                String.valueOf(new ItemStack(item).getMaxStackSize()), colours.label());
        StudioDraw.row(g, font(), left, y + 3 * StudioDraw.SMALL_ROW, words(ToolTexts.REGISTRY_COMPONENTS),
                String.valueOf(item.components().size()), colours.label());
    }

    /* What the tag chosen holds: each item as a slot, and its name under its id. */
    private void holds(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        final TagKey<Item> tag = this.tag();
        final List<Item> items = new ArrayList<>();
        if (tag != null) {
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> items.add(holder.value()));
        }
        StudioDraw.panel(g, font(), area, tag == null ? "" : GameText.resolve(ToolTexts.HOLDS.with(
                tag.location().toString(), items.size())));
        final int perRow = Math.max(1, (area.width() - 2 * StudioDraw.INSET) / SLOT);
        for (int i = 0; i < items.size() && i < perRow * 2; i++) {
            final int x = area.x() + StudioDraw.INSET + i % perRow * SLOT;
            final int y = area.y() + StudioDraw.HEAD + 4 + i / perRow * SLOT;
            g.blitSprite(SLOT_SPRITE, x, y, 18, 18);
            g.renderItem(new ItemStack(items.get(i)), x + 1, y + 1);
        }
        int y = area.y() + StudioDraw.HEAD + 8 + 2 * SLOT;
        for (final Item item : items) {
            if (y + NAME_ROW > area.bottom()) {
                break;
            }
            StudioDraw.text(g, font(), item.getDescription().getString(), area.x() + StudioDraw.INSET, y,
                    area.width() - 2 * StudioDraw.INSET, colours.label());
            StudioDraw.small(g, font(), BuiltInRegistries.ITEM.getKey(item).toString(), area.x() + StudioDraw.INSET,
                    y + 9, area.width() - 2 * StudioDraw.INSET, colours.dim());
            y += NAME_ROW;
        }
    }

    private void came(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.CAME_FROM));
        int y = area.y() + StudioDraw.HEAD + 4;
        for (final Map.Entry<String, String> entry : this.cameFrom.entrySet()) {
            if (y + 24 > area.bottom()) {
                break;
            }
            StudioDraw.text(g, font(), entry.getKey(), area.x() + StudioDraw.INSET, y,
                    area.width() - 2 * StudioDraw.INSET, colours.label());
            final boolean fromPack = !entry.getValue().contains("mod/") && !entry.getValue().contains("vanilla");
            StudioDraw.small(g, font(), entry.getValue(), area.x() + StudioDraw.INSET, y + 10,
                    area.width() - 2 * StudioDraw.INSET, fromPack ? colours.warning() : colours.dim());
            y += 24;
        }
    }

    private @Nullable TagKey<Item> tag() {
        return this.chosen < this.tags.size() ? this.tags.get(this.chosen) : null;
    }

    /* Asks the server which pack's file of the tag put in each of its entries. */
    private void ask() {
        this.cameFrom.clear();
        final TagKey<Item> tag = this.tag();
        if (tag == null || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        final CompoundTag args = new CompoundTag();
        args.putString(StudioKeys.REGISTRY, ITEM);
        args.putString(StudioKeys.TAG, tag.location().toString());
        StudioAnswers.ask(StudioServices.SOURCES, args, answered -> {
            this.cameFrom.clear();
            final ListTag packs = answered.getList(StudioKeys.PACKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < packs.size(); i++) {
                final CompoundTag pack = packs.getCompound(i);
                final String from = GameText.resolve((pack.getBoolean(StudioKeys.REPLACE) ? ToolTexts.REPLACED_BEFORE
                        : ToolTexts.TAG_FILE).with(pack.getString(StudioKeys.PACK)));
                final ListTag entries = pack.getList(StudioKeys.ENTRIES, Tag.TAG_STRING);
                for (int j = 0; j < entries.size(); j++) {
                    this.cameFrom.putIfAbsent(entries.getString(j), from);
                }
            }
        });
    }
}
