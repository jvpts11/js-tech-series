/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A round trip through the save: the block the player looked at, saved and read back into a new block entity as a
 * world closed and opened again does, field by field, a field that came back other than it went marked.
 */
final class SavePage extends StudioPage {

    private final Button roundTrip;
    private @Nullable CompoundTag answer;
    private int scroll;

    private static final int ROW = 18;

    SavePage(final StudioScreen studio) {
        super(studio);
        this.roundTrip = this.panel.add(new Button(() -> words(ToolTexts.SAVE_AND_READ), this::ask).setPrimary(true));
    }

    @Override
    TextKey tab() {
        return StudioTool.SAVE.title();
    }

    @Override
    List<String> crumbs() {
        return List.of(words(this.tab()), this.studio.lookedAt().map(at -> GameText.resolve(ToolTexts.PLACE.with(
                at.getX(), at.getY(), at.getZ()))).orElse(words(ToolTexts.NO_BLOCK)));
    }

    @Override
    List<String> lines() {
        if (this.answer == null) {
            return List.of();
        }
        final ListTag changes = this.answer.getList(StudioKeys.CHANGES, Tag.TAG_COMPOUND);
        // A field that came back changed ends in a star.
        return changes.stream().map(change -> ((CompoundTag) change).getString(StudioKeys.FIELD)
                + (((CompoundTag) change).getBoolean(StudioKeys.CHANGED) ? "*" : "")).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect body = laid.body();
        place(this.roundTrip, new StudioLayout.Rect(body.right() - 134, body.y() - 1, 134, 14));
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final StudioLayout.Rect body = this.regions.body();
        StudioDraw.text(g, font(), words(ToolTexts.SAVE_HEAD), body.x() + 2, body.y() + 2,
                this.roundTrip.x() - body.x() - StudioLayout.GAP, colours.text());
        final StudioLayout.Rect table = StudioLayout.below(body, 16);
        StudioDraw.panel(g, font(), table, "");
        final int[] at = {table.x() + 18, table.x() + table.width() * 35 / 100, table.x() + table.width() * 64 / 100};
        final List<TextKey> heads = List.of(ToolTexts.FIELD, ToolTexts.BEFORE, ToolTexts.READ_BACK);
        for (int i = 0; i < heads.size(); i++) {
            StudioDraw.small(g, font(), words(heads.get(i)), at[i], table.y() + 6, table.width() / 3, colours.text());
        }
        if (this.studio.lookedAt().isEmpty() || this.answer == null) {
            StudioDraw.small(g, font(), words(this.studio.lookedAt().isEmpty() ? ToolTexts.NO_BLOCK
                    : ToolTexts.PRESS_SAVE), at[0], table.y() + 22, table.width() - 24, colours.dim());
            return;
        }
        if (this.answer.contains(StudioServices.REFUSED)) {
            StudioDraw.small(g, font(), this.answer.getString(StudioServices.REFUSED), at[0], table.y() + 22,
                    table.width() - 24, colours.error());
            return;
        }
        if (!this.answer.contains(StudioKeys.CHANGES)) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_FIELDS), at[0], table.y() + 22, table.width() - 24,
                    colours.dim());
            return;
        }
        final ListTag changes = this.answer.getList(StudioKeys.CHANGES, Tag.TAG_COMPOUND);
        int changed = 0;
        for (int i = 0; i < changes.size(); i++) {
            changed += changes.getCompound(i).getBoolean(StudioKeys.CHANGED) ? 1 : 0;
        }
        final int shown = this.rowsShown();
        for (int i = this.scroll; i < Math.min(changes.size(), this.scroll + shown); i++) {
            final CompoundTag change = changes.getCompound(i);
            final int y = table.y() + 22 + (i - this.scroll) * ROW;
            final boolean differs = change.getBoolean(StudioKeys.CHANGED);
            if (differs) {
                g.fill(table.x() + 4, y - 3, table.right() - 4, y + 12, colours.changed());
                StudioMarks.draw(g, font(), table.right() - 4 - StudioDraw.INSET - StudioMarks.WIDTH, y - 1,
                        StudioSheet.Mark.ERROR);
            }
            StudioDraw.text(g, font(), change.getString(StudioKeys.FIELD), at[0], y, at[1] - at[0] - 6,
                    colours.label());
            StudioDraw.text(g, font(), change.getString(StudioKeys.BEFORE), at[1], y, at[2] - at[1] - 6,
                    colours.label());
            StudioDraw.text(g, font(), change.getString(StudioKeys.AFTER), at[2], y, table.right() - at[2] - 24,
                    differs ? colours.error() : colours.label());
        }
        StudioDraw.small(g, font(), changed == 0 ? words(ToolTexts.SAME_SUMMARY)
                : GameText.resolve(ToolTexts.CHANGED_SUMMARY.with(changed)), at[0], table.bottom() - 12,
                table.width() - 24, colours.dim());
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        if (this.answer == null || !this.regions.body().contains(mouseX, mouseY)) {
            return false;
        }
        this.scroll = ThingsPage.scrolled(this.scroll, scrollY, this.answer.getList(StudioKeys.CHANGES,
                Tag.TAG_COMPOUND).size(), this.rowsShown());
        return true;
    }

    private int rowsShown() {
        return Math.max(1, (this.regions.body().height() - 16 - 40) / ROW);
    }

    private void ask() {
        final Optional<BlockPos> at = this.studio.lookedAt();
        if (at.isEmpty() || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.POS, at.get().asLong());
        StudioAnswers.ask(StudioServices.SAVE, args, answered -> {
            this.answer = answered;
            this.scroll = 0;
        });
    }
}
