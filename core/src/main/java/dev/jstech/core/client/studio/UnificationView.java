/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What unification does to the world's recipes, or would do turned on, on the studio's Recipes page: each tag with the
 * item kept and those it replaces, and how many recipes that changes; beside it, one of those recipes with the item it
 * named before and the one it names after, what happens to drops, and whether it steps aside. Off, it can be turned
 * on from here, for a player who may change the server's settings.
 */
final class UnificationView {

    private final StudioScreen studio;
    private final Button turnOn;
    private final Button back;
    private @Nullable CompoundTag answer;
    private boolean open;
    private int chosen;
    private int scrolled;

    /* The pitch of a tag's row: its name, the item kept, those replaced. */
    private static final int ROW = 34;
    private static final int HEADER = 16;
    private static final int SLOT = 18;
    /* How far above the panel's foot the notes start: room for the drops' two lines and the line under them. */
    private static final int NOTES = 30;
    /* The room a button leaves round its words. */
    private static final int LABEL_ROOM = 16;
    private static final String JOIN = ", ";
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    UnificationView(final StudioScreen studio, final Panel panel) {
        this.studio = studio;
        this.turnOn = panel.add(new Button(() -> words(UnificationTexts.TURN_ON), this::turnOn).setPrimary(true));
        this.back = panel.add(new Button(() -> words(UnificationTexts.BACK), () -> this.open = false));
    }

    /** Whether the page shows unification rather than the recipe bench. */
    boolean isOpen() {
        return this.open;
    }

    /** Shows unification, asking the server what it does. */
    void open() {
        this.open = true;
        this.chosen = 0;
        this.scrolled = 0;
        this.ask(false);
    }

    /** Back to the recipe bench, as the page is shown again. */
    void close() {
        this.open = false;
    }

    /** The words after the page's name in the crumbs. */
    String crumb() {
        return words(this.isOn() ? UnificationTexts.CHANGED : UnificationTexts.WOULD_CHANGE);
    }

    /* Each tag with the item kept and how many recipes it changes: what a test reads. */
    List<String> lines() {
        final List<String> out = new ArrayList<>();
        final ListTag tags = this.tags();
        for (int i = 0; i < tags.size(); i++) {
            final CompoundTag tag = tags.getCompound(i);
            out.add(tag.getString(StudioKeys.TAG) + " " + tag.getString(StudioKeys.KEPT) + " "
                    + tag.getInt(StudioKeys.COUNT));
        }
        return out;
    }

    void layout(final StudioLayout.Rect body, final Font font) {
        final int turnWidth = font.width(words(UnificationTexts.TURN_ON)) + LABEL_ROOM;
        final int backWidth = font.width(words(UnificationTexts.BACK)) + LABEL_ROOM;
        StudioPage.place(this.turnOn, new StudioLayout.Rect(body.right() - turnWidth, body.y() - 1, turnWidth,
                HEADER));
        StudioPage.place(this.back, new StudioLayout.Rect(body.right() - turnWidth - StudioLayout.GAP - backWidth,
                body.y() - 1, backWidth, HEADER));
    }

    void render(final GuiGraphics g, final Font font, final StudioLayout.Rect body) {
        final boolean on = this.isOn();
        final boolean aside = this.answer != null && this.answer.getBoolean(StudioKeys.ASIDE);
        final boolean offered = this.open && this.answer != null && !on && !aside
                && !this.answer.contains(StudioServices.REFUSED);
        this.turnOn.setVisible(offered);
        this.back.setVisible(this.open);
        if (!this.open) {
            return;
        }
        // With nothing to turn on, Back takes the corner Turn it on would stand in.
        final int backRight = offered ? this.turnOn.x() - StudioLayout.GAP : body.right();
        StudioPage.place(this.back, new StudioLayout.Rect(backRight - this.back.width(), this.back.y(),
                this.back.width(), this.back.height()));
        final StudioPalette.Colours colours = StudioPalette.get();
        final TextKey head = on ? UnificationTexts.ON : aside ? UnificationTexts.ASIDE : UnificationTexts.OFF;
        final String said = this.answer == null ? words(ToolTexts.ASKING) : words(head);
        final int room = this.back.x() - body.x() - StudioLayout.GAP;
        final int colour = on ? colours.fine() : colours.warning();
        if (font.width(said) <= room) {
            StudioDraw.text(g, font, said, body.x() + 2, body.y() + 3, room, colour);
        } else {
            StudioDraw.smallWrapped(g, font, said, body.x() + 2, body.y(), room, colour);
        }
        final List<StudioLayout.Rect> columns = this.columns(body);
        final StudioLayout.Rect left = columns.get(0);
        final StudioLayout.Rect right = columns.get(1);
        StudioDraw.panel(g, font, left, words(UnificationTexts.BY_TAG));
        if (this.answer == null) {
            StudioDraw.panel(g, font, right, "");
            return;
        }
        if (this.answer.contains(StudioServices.REFUSED)) {
            StudioDraw.small(g, font, this.answer.getString(StudioServices.REFUSED), left.x() + StudioDraw.INSET,
                    left.y() + StudioDraw.HEAD + 4, left.width() - 2 * StudioDraw.INSET, colours.error());
            StudioDraw.panel(g, font, right, "");
            return;
        }
        this.drawTags(g, font, left, colours);
        this.drawChosen(g, font, right, colours);
    }

    boolean mouseClicked(final double mouseX, final double mouseY, final StudioLayout.Rect body) {
        final StudioLayout.Rect left = this.columns(body).get(0);
        if (!this.open || !left.contains(mouseX, mouseY)) {
            return false;
        }
        final int row = (int) ((mouseY - left.y() - StudioDraw.HEAD - 2) / ROW) + this.scrolled;
        if (row >= 0 && row < this.tags().size()) {
            this.chosen = row;
        }
        return true;
    }

    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY,
                          final StudioLayout.Rect body) {
        final StudioLayout.Rect left = this.columns(body).get(0);
        if (!this.open || !left.contains(mouseX, mouseY)) {
            return false;
        }
        final int most = Math.max(0, this.tags().size() - this.rowsShown(left));
        this.scrolled = Math.clamp(this.scrolled - (int) Math.signum(scrollY), 0, most);
        return true;
    }

    private void drawTags(final GuiGraphics g, final Font font, final StudioLayout.Rect left,
                          final StudioPalette.Colours colours) {
        final ListTag tags = this.tags();
        final int x = left.x() + StudioDraw.INSET + 2;
        final int room = left.width() - 2 * StudioDraw.INSET - 4;
        if (tags.isEmpty()) {
            StudioDraw.small(g, font, words(UnificationTexts.NOTHING), x, left.y() + StudioDraw.HEAD + 4, room,
                    colours.dim());
        }
        final int shown = this.rowsShown(left);
        for (int i = this.scrolled; i < Math.min(tags.size(), this.scrolled + shown); i++) {
            final CompoundTag tag = tags.getCompound(i);
            final int y = left.y() + StudioDraw.HEAD + 2 + (i - this.scrolled) * ROW;
            if (i == this.chosen) {
                g.fill(left.x() + 3, y, left.right() - 3, y + ROW - 2, colours.selected());
                Draw.outline(g, left.x() + 3, y, left.width() - 6, ROW - 2, colours.outline());
            }
            final String count = recipes(tag.getInt(StudioKeys.COUNT));
            final int countWidth = Math.round(font.width(count) * StudioDraw.SMALL);
            StudioDraw.small(g, font, count, left.right() - StudioDraw.INSET - 2 - countWidth, y + 4, countWidth,
                    colours.dim());
            StudioDraw.text(g, font, tag.getString(StudioKeys.TAG), x, y + 3, room - countWidth - 6, i == this.chosen
                    ? colours.text() : colours.label());
            StudioDraw.small(g, font, GameText.resolve(UnificationTexts.KEPT.with(tag.getString(StudioKeys.KEPT))),
                    x, y + 14, room, colours.fine());
            StudioDraw.small(g, font, GameText.resolve(UnificationTexts.REPLACED.with(String.join(JOIN,
                    strings(tag.getList(StudioKeys.REPLACED, Tag.TAG_STRING))))), x, y + 22, room, colours.text());
        }
        StudioDraw.small(g, font, GameText.resolve(UnificationTexts.ORDER.with(String.join(JOIN, strings(
                this.answer.getList(StudioKeys.PREFER, Tag.TAG_STRING))))), x, left.bottom() - 12, room,
                colours.dim());
    }

    /* The tag chosen: one of its recipes, the item it named before and the one after, and the notes under it. */
    private void drawChosen(final GuiGraphics g, final Font font, final StudioLayout.Rect right,
                            final StudioPalette.Colours colours) {
        final ListTag tags = this.tags();
        final int x = right.x() + StudioDraw.INSET + 2;
        final int room = right.width() - 2 * StudioDraw.INSET - 4;
        if (this.chosen >= tags.size()) {
            StudioDraw.panel(g, font, right, "");
            this.drawNotes(g, font, right, x, room, colours);
            return;
        }
        final CompoundTag tag = tags.getCompound(this.chosen);
        final int count = tag.getInt(StudioKeys.COUNT);
        StudioDraw.panel(g, font, right, count == 0 ? words(UnificationTexts.NO_RECIPE) : count == 1
                ? words(UnificationTexts.THE_ONE) : GameText.resolve(UnificationTexts.ONE_OF.with(count)));
        StudioDraw.small(g, font, tag.getString(StudioKeys.ID), x, right.y() + StudioDraw.HEAD + 2, room,
                colours.label());
        final String before = tag.getString(StudioKeys.BEFORE).isEmpty() ? first(tag.getList(StudioKeys.REPLACED,
                Tag.TAG_STRING)) : tag.getString(StudioKeys.BEFORE);
        final int labelWidth = Math.max(font.width(words(UnificationTexts.BEFORE)),
                font.width(words(UnificationTexts.AFTER))) + 12;
        this.drawItem(g, font, x, right.y() + 40, labelWidth, room, words(UnificationTexts.BEFORE), before,
                colours.label());
        this.drawItem(g, font, x, right.y() + 72, labelWidth, room, words(UnificationTexts.AFTER),
                tag.getString(StudioKeys.KEPT), colours.fine());
        this.drawNotes(g, font, right, x, room, colours);
    }

    private void drawItem(final GuiGraphics g, final Font font, final int x, final int y, final int labelWidth,
                          final int room, final String label, final String id, final int colour) {
        StudioDraw.text(g, font, label, x, y + 5, labelWidth, StudioPalette.get().text());
        final int slotX = x + labelWidth;
        g.blitSprite(SLOT_SPRITE, slotX, y, SLOT, SLOT);
        final ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed != null) {
            g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(parsed)), slotX + 1, y + 1);
        }
        StudioDraw.small(g, font, id, slotX + SLOT + 4, y + 6, room - labelWidth - SLOT - 4, colour);
    }

    private void drawNotes(final GuiGraphics g, final Font font, final StudioLayout.Rect right, final int x,
                           final int room, final StudioPalette.Colours colours) {
        final boolean aside = this.answer.getBoolean(StudioKeys.ASIDE);
        StudioDraw.smallWrapped(g, font, words(this.answer.getBoolean(StudioKeys.DROPS) ? UnificationTexts.DROPS
                : UnificationTexts.NO_DROPS), x, right.bottom() - NOTES, room, colours.dim());
        StudioDraw.small(g, font, words(aside ? UnificationTexts.STEPPED_ASIDE : UnificationTexts.STEPS_ASIDE), x,
                right.bottom() - 12, room, aside ? colours.warning() : colours.dim());
    }

    private List<StudioLayout.Rect> columns(final StudioLayout.Rect body) {
        return StudioLayout.columns(StudioLayout.below(body, HEADER + StudioLayout.GAP), 0.52);
    }

    private int rowsShown(final StudioLayout.Rect left) {
        return Math.max(1, (left.height() - StudioDraw.HEAD - 2 - 16) / ROW);
    }

    private boolean isOn() {
        return this.answer != null && this.answer.getBoolean(StudioKeys.ON);
    }

    private ListTag tags() {
        return this.answer == null || this.answer.contains(StudioServices.REFUSED) ? new ListTag()
                : this.answer.getList(StudioKeys.ENTRIES, Tag.TAG_COMPOUND);
    }

    private void turnOn() {
        this.ask(true);
    }

    private void ask(final boolean turningOn) {
        final CompoundTag args = new CompoundTag();
        if (turningOn) {
            args.putBoolean(StudioKeys.TURN_ON, true);
        }
        StudioAnswers.ask(StudioServices.UNIFICATION, args, answered -> {
            if (turningOn && answered.contains(StudioServices.REFUSED)) {
                this.studio.say(answered.getString(StudioServices.REFUSED));
                return;
            }
            if (turningOn) {
                this.studio.say(words(UnificationTexts.TURNED_ON));
            }
            this.answer = answered;
            this.chosen = Math.min(this.chosen, Math.max(0, this.tags().size() - 1));
        });
    }

    private static String recipes(final int count) {
        return count == 1 ? words(UnificationTexts.ONE_RECIPE) : GameText.resolve(UnificationTexts.RECIPES.with(
                count));
    }

    private static List<String> strings(final ListTag list) {
        final List<String> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    private static String first(final ListTag list) {
        return list.isEmpty() ? "" : list.getString(0);
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
