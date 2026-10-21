/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The loot tester: a loot table rolled many times with the tool chosen, Fortune or Silk Touch on it, and how its
 * drops spread: in how many rolls each item came, how many each roll gave, and the fewest and the most.
 */
final class LootPage extends StudioPage {

    private final TextField table = this.panel.add(new TextField(LONGEST));
    private final Button tool;
    private final Button fortune;
    private final Button rolls;
    private final Button roll;
    private int toolAt = 3;
    private int fortuneAt;
    private int rollsAt = 1;
    private @Nullable CompoundTag answer;
    private boolean rolling;

    private static final int LONGEST = 128;
    private static final List<Item> TOOLS = List.of(Items.AIR, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE,
            Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL,
            Items.SHEARS);
    /** Fortune's levels, and Silk Touch after them. */
    private static final int SILK = 4;
    private static final List<Integer> ROLL_COUNTS = List.of(100, 1_000, 10_000);
    /** The game's own key of an enchantment's level, as its tooltips write it. */
    private static final String LEVEL = "enchantment.level.";
    private static final int ROW = 26;
    private static final double PERCENT = 100.0;

    LootPage(final StudioScreen studio) {
        super(studio);
        this.tool = this.panel.add(new Button(() -> GameText.resolve(ToolTexts.TOOL.with(this.toolName())),
                () -> this.toolAt = (this.toolAt + 1) % TOOLS.size()));
        this.fortune = this.panel.add(new Button(() -> this.fortuneAt == SILK ? words(ToolTexts.SILK)
                : GameText.resolve(ToolTexts.FORTUNE.with(this.fortuneAt == 0 ? words(ToolTexts.FORTUNE_NONE)
                        : Component.translatable(LEVEL + this.fortuneAt).getString())),
                () -> this.fortuneAt = (this.fortuneAt + 1) % (SILK + 1)));
        this.rolls = this.panel.add(new Button(() -> GameText.resolve(ToolTexts.ROLLS.with(GameLocale.count(
                ROLL_COUNTS.get(this.rollsAt)))), () -> this.rollsAt = (this.rollsAt + 1) % ROLL_COUNTS.size()));
        this.roll = this.panel.add(new Button(() -> words(ToolTexts.ROLL), this::roll).setPrimary(true));
    }

    @Override
    TextKey tab() {
        return StudioTool.LOOT.title();
    }

    @Override
    List<String> crumbs() {
        return this.table.edit().isBlank() ? List.of(words(this.tab())) : List.of(words(this.tab()),
                this.table.edit());
    }

    @Override
    List<String> lines() {
        if (this.answer == null) {
            return List.of();
        }
        final ListTag rows = this.answer.getList(StudioKeys.ROWS, Tag.TAG_COMPOUND);
        return rows.stream().map(row -> ((CompoundTag) row).getString(StudioKeys.ID)).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect body = laid.body();
        final int gap = StudioLayout.GAP / 2;
        final int buttons = 110 + 88 + 80 + 92 + 4 * gap;
        final int field = Math.max(80, body.width() - buttons);
        int x = body.x() + 2;
        place(this.table, new StudioLayout.Rect(x, body.y(), field, 16));
        x += field + gap;
        place(this.tool, new StudioLayout.Rect(x, body.y(), 110, 16));
        x += 110 + gap;
        place(this.fortune, new StudioLayout.Rect(x, body.y(), 88, 16));
        x += 88 + gap;
        place(this.rolls, new StudioLayout.Rect(x, body.y(), 80, 16));
        x += 80 + gap;
        place(this.roll, new StudioLayout.Rect(x, body.y(), body.right() - x, 16));
    }

    @Override
    void shown() {
        this.studio.thing().filter(thing -> thing.block() != null).ifPresent(thing -> this.table.set(
                thing.block().getLootTable().location().toString()));
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final StudioLayout.Rect area = StudioLayout.below(this.regions.body(), 22);
        final int asked = this.answer == null ? ROLL_COUNTS.get(this.rollsAt) : this.answer.getInt(StudioKeys.ROLLS);
        StudioDraw.panel(g, font(), area, GameText.resolve(ToolTexts.WHAT_ROLLS_GAVE.with(GameLocale.count(asked))));
        if (this.answer == null) {
            StudioDraw.small(g, font(), words(this.rolling ? ToolTexts.ASKING : ToolTexts.PRESS_ROLL),
                    area.x() + StudioDraw.INSET, area.y() + StudioDraw.HEAD + 4, area.width(), colours.dim());
            return;
        }
        if (this.answer.contains(StudioServices.REFUSED) || this.answer.contains(StudioKeys.PROBLEM)) {
            final String why = this.answer.contains(StudioKeys.PROBLEM) ? GameText.resolve(ToolTexts.NO_TABLE.with(
                    this.answer.getString(StudioKeys.PROBLEM))) : this.answer.getString(StudioServices.REFUSED);
            StudioDraw.small(g, font(), why, area.x() + StudioDraw.INSET, area.y() + StudioDraw.HEAD + 4,
                    area.width(), colours.error());
            return;
        }
        final int[] at = {area.x() + 30, area.x() + area.width() * 34 / 100, area.x() + area.width() * 47 / 100,
            area.x() + area.width() * 61 / 100, area.x() + area.width() * 76 / 100};
        final List<TextKey> heads = List.of(ToolTexts.ITEM, ToolTexts.IN_ROLLS, ToolTexts.EACH_ROLL,
                ToolTexts.FEWEST_MOST);
        for (int i = 0; i < heads.size(); i++) {
            StudioDraw.small(g, font(), words(heads.get(i)), at[i], area.y() + StudioDraw.HEAD + 2, area.width() / 5,
                    colours.text());
        }
        final ListTag rows = this.answer.getList(StudioKeys.ROWS, Tag.TAG_COMPOUND);
        final int all = Math.max(1, this.answer.getInt(StudioKeys.ROLLS));
        double perRoll = 0;
        for (int i = 0; i < rows.size(); i++) {
            final CompoundTag row = rows.getCompound(i);
            perRoll += row.getDouble(StudioKeys.PER_ROLL);
            final int y = area.y() + StudioDraw.HEAD + 14 + i * ROW;
            if (y + ROW > area.bottom() - 14) {
                continue;
            }
            final ResourceLocation id = ResourceLocation.tryParse(row.getString(StudioKeys.ID));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(id)), area.x() + 8, y);
            }
            StudioDraw.text(g, font(), BlockPage.nameOf(row.getString(StudioKeys.ID)), at[0], y + 4,
                    at[1] - at[0] - 4, colours.label());
            final double share = row.getInt(StudioKeys.IN_ROLLS) * PERCENT / all;
            StudioDraw.text(g, font(), String.format(GameLocale.locale(), "%.1f %%", share), at[1], y + 4,
                    at[2] - at[1] - 4, colours.text());
            StudioDraw.text(g, font(), String.format(GameLocale.locale(), "%.3g", row.getDouble(StudioKeys.PER_ROLL)),
                    at[2], y + 4, at[3] - at[2] - 4, colours.label());
            StudioDraw.text(g, font(), row.getInt(StudioKeys.FEWEST) + ", " + row.getInt(StudioKeys.MOST), at[3],
                    y + 4, at[4] - at[3] - 4, colours.label());
            g.fill(at[4], y + 4, at[4] + (int) ((area.right() - 8 - at[4]) * share / PERCENT), y + 12,
                    colours.bar());
        }
        StudioDraw.small(g, font(), GameText.resolve(ToolTexts.LOOT_SUMMARY.with(String.format(GameLocale.locale(),
                "%.2f", perRoll))), area.x() + StudioDraw.INSET, area.bottom() - 12, area.width(), colours.dim());
    }

    private String toolName() {
        final Item item = TOOLS.get(this.toolAt);
        return item == Items.AIR ? words(ToolTexts.HAND) : item.getDescription().getString();
    }

    private void roll() {
        if (this.rolling || Minecraft.getInstance().getConnection() == null
                || ResourceLocation.tryParse(this.table.edit().trim()) == null) {
            return;
        }
        this.rolling = true;
        this.answer = null;
        final CompoundTag args = new CompoundTag();
        args.putString(StudioKeys.TABLE, this.table.edit().trim());
        args.putInt(StudioKeys.ROLLS, ROLL_COUNTS.get(this.rollsAt));
        final Item item = TOOLS.get(this.toolAt);
        args.putString(StudioKeys.TOOL, item == Items.AIR ? "" : BuiltInRegistries.ITEM.getKey(item).toString());
        args.putInt(StudioKeys.FORTUNE, this.fortuneAt == SILK ? 0 : this.fortuneAt);
        args.putBoolean(StudioKeys.SILK, this.fortuneAt == SILK);
        StudioAnswers.ask(StudioServices.LOOT, args, answered -> {
            this.rolling = false;
            this.answer = answered;
        });
    }
}
