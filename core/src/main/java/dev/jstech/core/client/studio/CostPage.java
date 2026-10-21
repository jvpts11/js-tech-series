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
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.SceneProjection;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The cost a tick: a measure of every machine, what each costs the server in microseconds a tick, the dearest first,
 * with where it is and whose it is; the land round the dearest, each measured block there marked as dear as it is;
 * and the heat map, which marks them so in the world.
 */
final class CostPage extends StudioPage {

    private final Button measure;
    private final Button heat;
    private final StudioScene land = new StudioScene();
    private final List<Entry> entries = new ArrayList<>();
    private @Nullable CompoundTag answer;
    private boolean measuring;
    private boolean turning;

    private static final int SECONDS = 10;
    private static final int ROW = 23;
    private static final int RADIUS = 3;
    private static final double NANOS_PER_MICRO = 1000.0;
    private static final double NANOS_PER_MILLI = 1_000_000.0;
    private static final int LEGEND_STEPS = 50;
    private static final int GLOW_ALPHA = 0x78 << 24;

    CostPage(final StudioScreen studio) {
        super(studio);
        this.measure = this.panel.add(new Button(() -> this.measuring ? words(ToolTexts.MEASURING)
                : GameText.resolve(ToolTexts.MEASURE.with(SECONDS)), this::measure));
        this.heat = this.panel.add(new Button(() -> words(StudioHeatMap.on() ? ToolTexts.HEAT_ON
                : ToolTexts.HEAT_OFF), () -> StudioHeatMap.on(!StudioHeatMap.on())));
    }

    @Override
    TextKey tab() {
        return StudioTool.COST.title();
    }

    @Override
    List<String> crumbs() {
        return this.answer == null ? List.of(words(this.tab())) : List.of(words(this.tab()),
                GameText.resolve(ToolTexts.MEASURED_FOR.with(SECONDS)));
    }

    @Override
    List<String> lines() {
        return this.entries.stream().map(entry -> entry.block() + " " + entry.micros()).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect body = laid.body();
        place(this.heat, new StudioLayout.Rect(body.right() - 82, body.y() - 1, 82, 14));
        place(this.measure, new StudioLayout.Rect(body.right() - 82 - StudioLayout.GAP - 78, body.y() - 1, 78, 14));
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final StudioLayout.Rect body = this.regions.body();
        StudioDraw.text(g, font(), words(ToolTexts.COST_HEAD), body.x() + 2, body.y() + 2,
                this.measure.x() - body.x() - StudioLayout.GAP, colours.text());
        final List<StudioLayout.Rect> columns = StudioLayout.columns(StudioLayout.below(body, 16), 0.66);
        final StudioLayout.Rect table = columns.get(0);
        StudioDraw.panel(g, font(), table, "");
        final int[] at = {table.x() + 12, table.x() + table.width() * 40 / 100, table.x() + table.width() * 64 / 100,
            table.x() + table.width() * 80 / 100};
        final List<TextKey> heads = List.of(ToolTexts.BLOCK, ToolTexts.WHERE, ToolTexts.COST, ToolTexts.OWNER);
        for (int i = 0; i < heads.size(); i++) {
            StudioDraw.small(g, font(), words(heads.get(i)), at[i], table.y() + 6, table.width() / 4, colours.text());
        }
        g.fill(table.x() + 4, table.y() + 15, table.right() - 4, table.y() + 16, colours.pointed());
        if (this.entries.isEmpty()) {
            StudioDraw.small(g, font(), words(this.measuring ? ToolTexts.MEASURING : ToolTexts.NOT_MEASURED),
                    at[0], table.y() + 22, table.width() - 24, colours.dim());
        }
        final double dearest = this.entries.isEmpty() ? 1.0 : Math.max(1.0, this.entries.getFirst().micros());
        for (int i = 0; i < this.entries.size(); i++) {
            final int y = table.y() + 20 + i * ROW;
            if (y + ROW > table.bottom()) {
                break;
            }
            final Entry entry = this.entries.get(i);
            final double share = entry.micros() / dearest;
            final int colour = StudioHeatMap.colourOf(share);
            g.fill(table.x() + 4, y, table.x() + 10, y + 8, colour);
            StudioDraw.text(g, font(), entry.block(), at[0], y, at[1] - at[0] - 4, colours.label());
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.PLACE.with(entry.pos().getX(), entry.pos().getY(),
                    entry.pos().getZ())), at[1], y + 1, at[2] - at[1] - 4, colours.dim());
            Draw.text(g, font(), String.format(GameLocale.locale(), "%.1f", entry.micros()), at[2], y,
                    i < 3 ? colours.text() : colours.label());
            StudioDraw.text(g, font(), entry.owner(), at[3], y, table.right() - at[3] - 4, colours.label());
            g.fill(at[0], y + 11, at[0] + (int) Math.round((table.right() - 8 - at[0]) * share), y + 13, colour);
        }
        this.world(g, columns.get(1), colours, dearest);
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.landArea().contains(mouseX, mouseY)) {
            this.turning = true;
            return true;
        }
        return false;
    }

    @Override
    boolean mouseDragged(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        if (this.turning) {
            this.land.turn(dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    void mouseReleased() {
        this.turning = false;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        if (this.landArea().contains(mouseX, mouseY)) {
            this.land.zoom(scrollY);
            return true;
        }
        return false;
    }

    @Override
    void closed() {
        this.land.close();
    }

    /* The land round the dearest machine with each measured block there marked, the legend and the summary. */
    private void world(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours,
                       final double dearest) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.IN_THE_WORLD));
        final StudioLayout.Rect view = this.landArea();
        g.fillGradient(view.x(), view.y(), view.right(), view.bottom(), colours.skyTop(), colours.skyLow());
        this.land.draw(g, view);
        if (this.land.showing() && !this.entries.isEmpty()) {
            final SceneProjection projection = this.land.projection(view);
            final BlockPos origin = this.entries.getFirst().pos().offset(-RADIUS, -1, -RADIUS);
            for (final Entry entry : this.entries) {
                final BlockPos local = entry.pos().subtract(origin);
                if (local.getX() < 0 || local.getY() < 0 || local.getZ() < 0 || local.getX() > 2 * RADIUS
                        || local.getY() > 2 * RADIUS || local.getZ() > 2 * RADIUS) {
                    continue;
                }
                final SceneProjection.Bounds box = projection.bounds(new GuideBlock.ScenePart(local.getX(),
                        local.getY(), local.getZ(), 0, 0, 0, GuideBlock.ScenePart.PIXELS, GuideBlock.ScenePart.PIXELS,
                        GuideBlock.ScenePart.PIXELS));
                g.fill(view.x() + (int) box.left(), view.y() + (int) box.top(), view.x() + (int) box.right(),
                        view.y() + (int) box.bottom(), StudioHeatMap.colourOf(entry.micros() / dearest) & 0xFFFFFF
                                | GLOW_ALPHA);
            }
        }
        Draw.outline(g, view.x(), view.y(), view.width(), view.height(), colours.frame());
        int y = view.bottom() + 6;
        StudioDraw.small(g, font(), words(ToolTexts.HEAT_LEGEND), area.x() + StudioDraw.INSET, y,
                area.width() - 2 * StudioDraw.INSET, colours.label());
        StudioDraw.small(g, font(), words(ToolTexts.HEAT_LEGEND_2), area.x() + StudioDraw.INSET, y + 9,
                area.width() - 2 * StudioDraw.INSET, colours.label());
        if (this.answer != null) {
            StudioDraw.small(g, font(), GameText.resolve(ToolTexts.MEASURED.with(this.answer.getInt(
                    StudioKeys.MEASURED), String.format(GameLocale.locale(), "%.2f", this.answer.getLong(
                    StudioKeys.NANOS) / NANOS_PER_MILLI))), area.x() + StudioDraw.INSET, y + 22,
                    area.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        y += 34;
        final int wide = area.width() - 2 * StudioDraw.INSET;
        for (int i = 0; i < LEGEND_STEPS; i++) {
            final int x = area.x() + StudioDraw.INSET + i * wide / LEGEND_STEPS;
            g.fill(x, y, area.x() + StudioDraw.INSET + (i + 1) * wide / LEGEND_STEPS, y + 8,
                    StudioHeatMap.colourOf((double) i / (LEGEND_STEPS - 1)));
        }
    }

    private StudioLayout.Rect landArea() {
        final List<StudioLayout.Rect> columns = StudioLayout.columns(StudioLayout.below(this.regions.body(), 16),
                0.66);
        final StudioLayout.Rect area = columns.get(1);
        return new StudioLayout.Rect(area.x() + 2, area.y() + StudioDraw.HEAD, area.width() - 4,
                Math.max(40, area.height() / 2));
    }

    private void measure() {
        if (this.measuring || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        this.measuring = true;
        final CompoundTag args = new CompoundTag();
        args.putInt(StudioKeys.SECONDS, SECONDS);
        StudioAnswers.ask(StudioServices.COST, args, answered -> {
            this.measuring = false;
            if (answered.contains(StudioServices.REFUSED)) {
                this.studio.say(answered.getString(StudioServices.REFUSED));
                return;
            }
            this.answer = answered;
            this.read(answered);
        });
    }

    private void read(final CompoundTag answered) {
        this.entries.clear();
        final ListTag listed = answered.getList(StudioKeys.ENTRIES, Tag.TAG_COMPOUND);
        final List<StudioHeatMap.Place> places = new ArrayList<>();
        double dearest = 1.0;
        for (int i = 0; i < listed.size(); i++) {
            final CompoundTag one = listed.getCompound(i);
            final ResourceLocation block = ResourceLocation.tryParse(one.getString(StudioKeys.BLOCK));
            final String name = block != null && BuiltInRegistries.BLOCK.containsKey(block)
                    ? BuiltInRegistries.BLOCK.get(block).getName().getString() : one.getString(StudioKeys.KIND);
            final Entry entry = new Entry(name, BlockPos.of(one.getLong(StudioKeys.POS)),
                    one.getString(StudioKeys.DIMENSION), one.getLong(StudioKeys.NANOS) / NANOS_PER_MICRO,
                    one.getString(StudioKeys.OWNER));
            this.entries.add(entry);
            dearest = Math.max(dearest, entry.micros());
        }
        for (final Entry entry : this.entries) {
            places.add(new StudioHeatMap.Place(entry.dimension(), entry.pos(), entry.micros() / dearest));
        }
        StudioHeatMap.show(places);
        final Minecraft game = Minecraft.getInstance();
        if (!this.entries.isEmpty() && game.level != null && this.entries.getFirst().dimension().equals(
                game.level.dimension().location().toString())) {
            final BlockPos middle = this.entries.getFirst().pos();
            this.land.show(StudioScene.blocksOf(game.level, middle.offset(-RADIUS, -1, -RADIUS),
                    middle.offset(RADIUS, 2 * RADIUS - 1, RADIUS)));
        }
    }

    /** A machine measured. */
    private record Entry(String block, BlockPos pos, String dimension, double micros, String owner) {
    }
}
