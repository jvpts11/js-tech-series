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
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * The payload monitor: what the server sent the player, or everyone, tick by tick over the last hundred ticks, as
 * bars; a click on a bar shows that tick's packets by kind, each with its mod, how many, their bytes and the biggest.
 */
final class PayloadsPage extends StudioPage {

    private final Button whom;
    private final List<CompoundTag> ticks = new ArrayList<>();
    private boolean everyone;
    private boolean asking;
    private int chosen = -1;
    private int ticksShown;

    private static final int EVERY = 40;
    private static final int CHART = 88;
    private static final int ROW = 14;

    PayloadsPage(final StudioScreen studio) {
        super(studio);
        this.whom = this.panel.add(new Button(() -> words(this.everyone ? ToolTexts.TO_YOU : ToolTexts.EVERYONE),
                () -> {
                    this.everyone = !this.everyone;
                    this.chosen = -1;
                    this.ask();
                }));
    }

    @Override
    TextKey tab() {
        return StudioTool.PAYLOADS.title();
    }

    @Override
    List<String> crumbs() {
        return List.of(words(this.tab()), words(this.everyone ? ToolTexts.LAST_100_EVERYONE : ToolTexts.LAST_100_YOU));
    }

    @Override
    List<String> lines() {
        final List<String> out = new ArrayList<>();
        final CompoundTag tick = this.shownTick();
        if (tick != null) {
            final ListTag sent = tick.getList(StudioKeys.SENT, Tag.TAG_COMPOUND);
            for (int i = 0; i < sent.size(); i++) {
                out.add(sent.getCompound(i).getString(StudioKeys.KIND));
            }
        }
        return out;
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect body = laid.body();
        place(this.whom, new StudioLayout.Rect(body.right() - 82, body.y() - 1, 82, 14));
    }

    @Override
    void shown() {
        this.ask();
    }

    @Override
    void tick() {
        if (++this.ticksShown % EVERY == 0) {
            this.ask();
        }
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final StudioLayout.Rect body = this.regions.body();
        StudioDraw.text(g, font(), words(this.everyone ? ToolTexts.PAYLOADS_EVERYONE_HEAD : ToolTexts.PAYLOADS_HEAD),
                body.x() + 2, body.y() + 2, this.whom.x() - body.x() - StudioLayout.GAP, colours.text());
        final StudioLayout.Rect chart = this.chartArea();
        StudioDraw.panel(g, font(), chart, "");
        if (this.ticks.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NOTHING_SENT), chart.x() + StudioDraw.INSET,
                    chart.y() + StudioDraw.INSET, chart.width(), colours.dim());
        }
        // The oldest at the left, as time runs.
        final long[] bytes = new long[this.ticks.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = bytesOf(this.ticks.get(bytes.length - 1 - i));
        }
        final int lit = this.litBar();
        StudioDraw.bars(g, this.barsArea(), bytes, colours.bar(), lit, colours.outline());
        final CompoundTag tick = this.shownTick();
        if (tick != null) {
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.BYTES_CAPTION.with(GameLocale.count(tick.getLong(
                    StudioKeys.TICK)), bytesWritten(bytesOf(tick)))), chart.x() + StudioDraw.INSET,
                    chart.bottom() - 9, chart.width() - 2 * StudioDraw.INSET, colours.label());
        }
        final StudioLayout.Rect table = new StudioLayout.Rect(body.x(), chart.bottom() + StudioLayout.GAP,
                body.width(), body.bottom() - chart.bottom() - StudioLayout.GAP);
        StudioDraw.panel(g, font(), table, tick == null ? "" : GameText.resolve(ToolTexts.TICK_HEAD.with(
                GameLocale.count(tick.getLong(StudioKeys.TICK)), packetsOf(tick), bytesWritten(bytesOf(tick)))));
        if (tick != null) {
            this.kinds(g, table, tick, colours);
        }
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final StudioLayout.Rect bars = this.barsArea();
        if (!bars.contains(mouseX, mouseY) || this.ticks.isEmpty()) {
            return false;
        }
        final int bar = (int) ((mouseX - bars.x()) * this.ticks.size() / bars.width());
        this.chosen = this.ticks.size() - 1 - Math.max(0, Math.min(this.ticks.size() - 1, bar));
        return true;
    }

    private void kinds(final GuiGraphics g, final StudioLayout.Rect table, final CompoundTag tick,
                       final StudioPalette.Colours colours) {
        final int[] at = {table.x() + 12, table.x() + table.width() * 36 / 100, table.x() + table.width() * 53 / 100,
            table.x() + table.width() * 66 / 100, table.x() + table.width() * 78 / 100};
        final List<TextKey> heads = List.of(ToolTexts.KIND, ToolTexts.MOD, ToolTexts.PACKETS, ToolTexts.BYTES,
                ToolTexts.BIGGEST);
        for (int i = 0; i < heads.size(); i++) {
            StudioDraw.small(g, font(), words(heads.get(i)), at[i], table.y() + StudioDraw.HEAD + 2,
                    table.width() / 5, colours.text());
        }
        final ListTag sent = tick.getList(StudioKeys.SENT, Tag.TAG_COMPOUND);
        final long biggest = sent.isEmpty() ? 1 : Math.max(1, sent.getCompound(0).getLong(StudioKeys.BYTES));
        for (int i = 0; i < sent.size(); i++) {
            final int y = table.y() + StudioDraw.HEAD + 14 + i * ROW;
            if (y + ROW > table.bottom()) {
                break;
            }
            final CompoundTag kind = sent.getCompound(i);
            final String id = kind.getString(StudioKeys.KIND);
            StudioDraw.text(g, font(), id, at[0], y, at[1] - at[0] - 4, colours.label());
            StudioDraw.small(g, font(), modOf(id), at[1], y + 1, at[2] - at[1] - 4, colours.dim());
            StudioDraw.text(g, font(), GameLocale.count(kind.getLong(StudioKeys.PACKETS)), at[2], y,
                    at[3] - at[2] - 4, colours.label());
            StudioDraw.text(g, font(), bytesWritten(kind.getLong(StudioKeys.BYTES)), at[3], y, at[4] - at[3] - 4,
                    i == 0 ? colours.text() : colours.label());
            StudioDraw.text(g, font(), bytesWritten(kind.getLong(StudioKeys.MOST)), at[4], y, table.width() / 10,
                    colours.label());
            final int barLeft = at[4] + table.width() / 10;
            g.fill(barLeft, y + 2, barLeft + (int) ((table.right() - 8 - barLeft) * kind.getLong(StudioKeys.BYTES)
                    / biggest), y + 6, colours.bar());
        }
    }

    private @Nullable CompoundTag shownTick() {
        if (this.ticks.isEmpty()) {
            return null;
        }
        if (this.chosen >= 0 && this.chosen < this.ticks.size()) {
            return this.ticks.get(this.chosen);
        }
        CompoundTag busiest = this.ticks.getFirst();
        for (final CompoundTag tick : this.ticks) {
            if (bytesOf(tick) > bytesOf(busiest)) {
                busiest = tick;
            }
        }
        return busiest;
    }

    private int litBar() {
        final CompoundTag tick = this.shownTick();
        return tick == null ? -1 : this.ticks.size() - 1 - this.ticks.indexOf(tick);
    }

    private StudioLayout.Rect chartArea() {
        final StudioLayout.Rect body = this.regions.body();
        return new StudioLayout.Rect(body.x(), body.y() + 16, body.width(), CHART);
    }

    private StudioLayout.Rect barsArea() {
        final StudioLayout.Rect chart = this.chartArea();
        return new StudioLayout.Rect(chart.x() + StudioDraw.INSET, chart.y() + 8, chart.width() - 2 * StudioDraw.INSET,
                chart.height() - 22);
    }

    private void ask() {
        if (this.asking || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        this.asking = true;
        final CompoundTag args = new CompoundTag();
        args.putBoolean(StudioKeys.EVERYONE, this.everyone);
        StudioAnswers.ask(StudioServices.PAYLOADS, args, answered -> {
            this.asking = false;
            if (answered.contains(StudioServices.REFUSED)) {
                this.studio.say(answered.getString(StudioServices.REFUSED));
                return;
            }
            this.ticks.clear();
            final ListTag listed = answered.getList(StudioKeys.TICKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < listed.size(); i++) {
                this.ticks.add(listed.getCompound(i));
            }
            if (this.chosen >= this.ticks.size()) {
                this.chosen = -1;
            }
        });
    }

    private static long bytesOf(final CompoundTag tick) {
        long out = 0;
        final ListTag sent = tick.getList(StudioKeys.SENT, Tag.TAG_COMPOUND);
        for (int i = 0; i < sent.size(); i++) {
            out += sent.getCompound(i).getLong(StudioKeys.BYTES);
        }
        return out;
    }

    private static long packetsOf(final CompoundTag tick) {
        long out = 0;
        final ListTag sent = tick.getList(StudioKeys.SENT, Tag.TAG_COMPOUND);
        for (int i = 0; i < sent.size(); i++) {
            out += sent.getCompound(i).getLong(StudioKeys.PACKETS);
        }
        return out;
    }

    private static String bytesWritten(final long bytes) {
        return UnitFormatter.forCurrentLocale().compact(bytes, Unit.BYTES);
    }

    /* The name of the mod a payload's id names, as the mod is loaded; the namespace itself for one that is not. */
    private static String modOf(final String kind) {
        final ResourceLocation id = ResourceLocation.tryParse(kind);
        if (id == null) {
            return "";
        }
        return ModList.get().getModContainerById(id.getNamespace()).map(mod -> mod.getModInfo().getDisplayName())
                .orElse(id.getNamespace());
    }
}
