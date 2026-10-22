/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * Who may do what at the place the player looks at, as every mod asking the Core's protections hears it: a table of
 * the actions, each answered for the player, someone of their team, a player they pick and that player's machines;
 * and beside it what each protection answered, the claim the place lies in, and the server's rule on hurting others.
 */
final class AccessPage extends StudioPage {

    private final Button again;
    private final TextField who = this.panel.add(new TextField(LONGEST_NAME));
    private @Nullable CompoundTag answer;

    /* How long a player's name may be. */
    private static final int LONGEST_NAME = 16;
    private static final int ROW = 16;
    private static final int LINE = 10;
    private static final Map<String, TextKey> ACTIONS = Map.of("break", AccessTexts.BREAK, "place",
            AccessTexts.PLACE, "use", AccessTexts.USE, "explode", AccessTexts.EXPLODE, "burn", AccessTexts.BURN,
            "flow", AccessTexts.FLOW, "move", AccessTexts.MOVE, "shoot", AccessTexts.SHOOT);
    private static final String ALLOW = "ALLOW";
    private static final String DENY = "DENY";

    AccessPage(final StudioScreen studio) {
        super(studio);
        this.again = this.panel.add(new Button(() -> words(AccessTexts.LOOK_AGAIN), this::ask).setPrimary(true));
        this.who.setOnCommit(name -> this.ask());
    }

    @Override
    TextKey tab() {
        return StudioTool.ACCESS.title();
    }

    @Override
    List<String> crumbs() {
        return List.of(words(this.tab()), this.studio.lookedAt().map(at -> GameText.resolve(ToolTexts.PLACE.with(
                at.getX(), at.getY(), at.getZ()))).orElse(words(ToolTexts.NO_BLOCK)));
    }

    /* Each action with a yes or a no for each column, the columns' names first: what a test reads. */
    @Override
    List<String> lines() {
        if (this.answer == null) {
            return List.of();
        }
        final ListTag columns = this.answer.getList(StudioKeys.COLUMNS, Tag.TAG_COMPOUND);
        final List<String> out = new ArrayList<>();
        final StringBuilder heads = new StringBuilder("who");
        for (int c = 0; c < columns.size(); c++) {
            heads.append(' ').append(columns.getCompound(c).getString(StudioKeys.KIND));
        }
        out.add(heads.toString());
        final int actions = columns.isEmpty() ? 0 : columns.getCompound(0).getList(StudioKeys.ENTRIES,
                Tag.TAG_COMPOUND).size();
        for (int a = 0; a < actions; a++) {
            final StringBuilder row = new StringBuilder(columns.getCompound(0).getList(StudioKeys.ENTRIES,
                    Tag.TAG_COMPOUND).getCompound(a).getString(StudioKeys.ACTION));
            for (int c = 0; c < columns.size(); c++) {
                row.append(' ').append(columns.getCompound(c).getList(StudioKeys.ENTRIES, Tag.TAG_COMPOUND)
                        .getCompound(a).getBoolean(StudioKeys.ALLOWED) ? "yes" : "no");
            }
            out.add(row.toString());
        }
        return out;
    }

    @Override
    void shown() {
        this.ask();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect body = laid.body();
        place(this.again, new StudioLayout.Rect(body.right() - 110, body.y() - 1, 110, 14));
        final StudioLayout.Rect table = this.table();
        place(this.who, new StudioLayout.Rect(table.x() + 100, table.bottom() - 20, Math.min(140,
                table.width() - 110), 14));
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final StudioLayout.Rect body = this.regions.body();
        StudioDraw.text(g, font(), words(AccessTexts.HEAD), body.x() + 2, body.y() + 2,
                this.again.x() - body.x() - StudioLayout.GAP, colours.text());
        final StudioLayout.Rect table = this.table();
        final StudioLayout.Rect side = this.side();
        StudioDraw.panel(g, font(), table, words(AccessTexts.QUESTION));
        StudioDraw.panel(g, font(), side, words(AccessTexts.ANSWERED));
        StudioDraw.small(g, font(), words(AccessTexts.PICK), table.x() + 6, table.bottom() - 16, 92,
                colours.dim());
        if (this.studio.lookedAt().isEmpty() || this.answer == null) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_BLOCK), table.x() + 6, table.y() + 22,
                    table.width() - 12, colours.dim());
            return;
        }
        if (this.answer.contains(StudioServices.REFUSED)) {
            StudioDraw.small(g, font(), this.answer.getString(StudioServices.REFUSED), table.x() + 6,
                    table.y() + 22, table.width() - 12, colours.error());
            return;
        }
        this.drawTable(g, table, colours);
        this.drawAnswers(g, side, colours);
    }

    /* The actions down the left, a column for each one weighed, a tick or a cross in each cell. */
    private void drawTable(final GuiGraphics g, final StudioLayout.Rect table, final StudioPalette.Colours colours) {
        final ListTag columns = this.answer.getList(StudioKeys.COLUMNS, Tag.TAG_COMPOUND);
        final int first = table.x() + table.width() * 38 / 100;
        final int each = columns.isEmpty() ? 0 : (table.right() - 8 - first) / columns.size();
        for (int c = 0; c < columns.size(); c++) {
            StudioDraw.small(g, font(), heading(columns.getCompound(c)), first + c * each, table.y() + 18, each - 4,
                    colours.label());
        }
        final ListTag rows = columns.isEmpty() ? new ListTag() : columns.getCompound(0).getList(StudioKeys.ENTRIES,
                Tag.TAG_COMPOUND);
        for (int a = 0; a < rows.size(); a++) {
            final int y = table.y() + 32 + a * ROW;
            final TextKey action = ACTIONS.get(rows.getCompound(a).getString(StudioKeys.ACTION));
            StudioDraw.text(g, font(), action == null ? rows.getCompound(a).getString(StudioKeys.ACTION)
                    : words(action), table.x() + 6, y, first - table.x() - 10, colours.label());
            for (int c = 0; c < columns.size(); c++) {
                final boolean allowed = columns.getCompound(c).getList(StudioKeys.ENTRIES, Tag.TAG_COMPOUND)
                        .getCompound(a).getBoolean(StudioKeys.ALLOWED);
                StudioMarks.draw(g, font(), first + c * each + 4, y, allowed ? StudioSheet.Mark.FINE
                        : StudioSheet.Mark.ERROR);
            }
        }
        final Optional<String> machines = this.machinesOf(columns);
        StudioDraw.small(g, font(), machines.map(name -> GameText.resolve(AccessTexts.WEIGHED_AS.with(name)))
                .orElseGet(() -> words(AccessTexts.NOBODY)), table.x() + 6, table.bottom() - 30, table.width() - 12,
                colours.dim());
    }

    /* Each protection with its mark and what it said, the claim, and the rule on hurting others. */
    private void drawAnswers(final GuiGraphics g, final StudioLayout.Rect side, final StudioPalette.Colours colours) {
        final ListTag answers = this.answer.getList(StudioKeys.ANSWERS, Tag.TAG_COMPOUND);
        final String weighed = this.weighedName();
        int y = side.y() + 20;
        if (answers.isEmpty()) {
            y = this.answerLine(g, side, y, StudioSheet.Mark.NONE, words(AccessTexts.NONE_REGISTERED), "", colours);
        }
        for (int i = 0; i < answers.size(); i++) {
            final CompoundTag one = answers.getCompound(i);
            final String verdict = one.getString(StudioKeys.VERDICT);
            final StudioSheet.Mark mark = ALLOW.equals(verdict) ? StudioSheet.Mark.FINE
                    : DENY.equals(verdict) ? StudioSheet.Mark.WARNING : StudioSheet.Mark.NONE;
            final String said = ALLOW.equals(verdict) ? GameText.resolve(AccessTexts.ALLOWS.with(weighed))
                    : DENY.equals(verdict) ? GameText.resolve(AccessTexts.STOPS.with(weighed))
                    : words(AccessTexts.SAYS_NOTHING);
            y = this.answerLine(g, side, y, mark, one.getString(StudioKeys.NAME), said, colours);
        }
        final String claim = this.answer.contains(StudioKeys.CLAIM) ? GameText.resolve(AccessTexts.CLAIMED_BY.with(
                this.answer.getString(StudioKeys.CLAIM))) : words(AccessTexts.NO_CLAIM);
        y = this.answerLine(g, side, y, this.answer.contains(StudioKeys.CLAIM) ? StudioSheet.Mark.WARNING
                : StudioSheet.Mark.FINE, words(AccessTexts.CLAIM), claim, colours);
        final boolean pvp = this.answer.getBoolean(StudioKeys.PVP);
        final String rule = !pvp ? words(AccessTexts.PVP_OFF) : !this.answer.contains(StudioKeys.MAY_HURT)
                ? words(AccessTexts.PVP_ON) : this.answer.getBoolean(StudioKeys.MAY_HURT)
                ? GameText.resolve(AccessTexts.PVP_HURTS.with(weighed))
                : GameText.resolve(AccessTexts.PVP_SPARES.with(weighed));
        this.answerLine(g, side, y, pvp ? StudioSheet.Mark.FINE : StudioSheet.Mark.NONE, words(AccessTexts.PVP), rule,
                colours);
    }

    /* A mark, a name and what it said under it; where the next one starts. */
    private int answerLine(final GuiGraphics g, final StudioLayout.Rect side, final int y,
                           final StudioSheet.Mark mark, final String name, final String said,
                           final StudioPalette.Colours colours) {
        StudioMarks.draw(g, font(), side.x() + 6, y, mark);
        StudioDraw.text(g, font(), name, side.x() + 16, y, side.width() - 22, colours.label());
        if (said.isEmpty()) {
            return y + LINE + 6;
        }
        final int lines = StudioDraw.smallWrapped(g, font(), said, side.x() + 16, y + LINE, side.width() - 22,
                colours.dim());
        return y + LINE + lines * (LINE - 3) + 6;
    }

    private String heading(final CompoundTag column) {
        return switch (column.getString(StudioKeys.KIND)) {
            case StudioKeys.YOU -> words(AccessTexts.YOU);
            case StudioKeys.TEAM -> words(AccessTexts.YOUR_TEAM);
            case StudioKeys.MACHINES -> GameText.resolve(AccessTexts.MACHINES.with(column.getString(StudioKeys.NAME)));
            default -> column.getString(StudioKeys.NAME);
        };
    }

    private Optional<String> machinesOf(final ListTag columns) {
        for (int c = 0; c < columns.size(); c++) {
            if (StudioKeys.MACHINES.equals(columns.getCompound(c).getString(StudioKeys.KIND))) {
                return Optional.of(columns.getCompound(c).getString(StudioKeys.NAME));
            }
        }
        return Optional.empty();
    }

    /* The name of the last one weighed: whom the protections' answers are about. */
    private String weighedName() {
        final ListTag columns = this.answer.getList(StudioKeys.COLUMNS, Tag.TAG_COMPOUND);
        return columns.isEmpty() ? "" : columns.getCompound(columns.size() - 1).getString(StudioKeys.NAME);
    }

    private StudioLayout.Rect table() {
        final StudioLayout.Rect below = StudioLayout.below(this.regions.body(), 16);
        return new StudioLayout.Rect(below.x(), below.y(), below.width() * 62 / 100, below.height());
    }

    private StudioLayout.Rect side() {
        final StudioLayout.Rect below = StudioLayout.below(this.regions.body(), 16);
        final StudioLayout.Rect table = this.table();
        return new StudioLayout.Rect(table.right() + StudioLayout.GAP, below.y(), below.right() - table.right()
                - StudioLayout.GAP, below.height());
    }

    private void ask() {
        final Optional<BlockPos> at = this.studio.lookedAt();
        if (at.isEmpty() || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.POS, at.get().asLong());
        args.putString(StudioKeys.WHO, this.who.value());
        StudioAnswers.ask(StudioServices.ACCESS, args, answered -> this.answer = answered);
    }
}
