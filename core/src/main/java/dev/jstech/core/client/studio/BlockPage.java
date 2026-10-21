/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.developer.studio.NbtLines;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The block inspector: the block the player looked at, read every second while watching: its block entity's fields as
 * it saves them, what each face offers, and what has gone in and out of it each second over the last fifteen.
 */
final class BlockPage extends StudioPage {

    private final Button again;
    private final Button watch;
    private final List<long[]> totals = new ArrayList<>();
    private final long[][] perSecond = new long[FLOWS][SECONDS];
    private @Nullable CompoundTag answer;
    private boolean watching = true;
    private boolean asking;
    private int ticks;

    /** How many seconds the bars keep, and how many kinds of flow there are. */
    private static final int SECONDS = 15;
    private static final int FLOWS = 6;
    private static final int EVERY = 20;
    private static final int FACE_ROW = 24;
    private static final int FLOW_ROW = 44;
    private static final List<TextKey> FLOW_NAMES = List.of(ToolTexts.ENERGY_IN, ToolTexts.ENERGY_OUT,
            ToolTexts.ITEMS_IN, ToolTexts.ITEMS_OUT, ToolTexts.FLUID_IN, ToolTexts.FLUID_OUT);
    private static final List<String> FLOW_KEYS = List.of(StudioKeys.ENERGY_IN, StudioKeys.ENERGY_OUT,
            StudioKeys.ITEMS_IN, StudioKeys.ITEMS_OUT, StudioKeys.FLUID_IN, StudioKeys.FLUID_OUT);
    private static final Map<String, TextKey> SIDES = Map.of("north", ToolTexts.NORTH, "south", ToolTexts.SOUTH,
            "east", ToolTexts.EAST, "west", ToolTexts.WEST, "up", ToolTexts.UP, "down", ToolTexts.DOWN);

    BlockPage(final StudioScreen studio) {
        super(studio);
        this.again = this.panel.add(new Button(() -> words(ToolTexts.LOOK_AGAIN), this::lookAgain));
        this.watch = this.panel.add(new Button(() -> words(this.watching ? ToolTexts.WATCHING_ON
                : ToolTexts.WATCHING_OFF), () -> this.watching = !this.watching));
    }

    @Override
    TextKey tab() {
        return StudioTool.BLOCK.title();
    }

    @Override
    List<String> crumbs() {
        return List.of(words(this.tab()), this.studio.lookedAt().map(this::placeName).orElse(words(
                ToolTexts.NO_BLOCK)));
    }

    @Override
    List<String> lines() {
        if (this.answer == null) {
            return List.of();
        }
        final List<String> out = new ArrayList<>();
        NbtLines.unlisted(this.answer.getList(StudioKeys.FIELDS, Tag.TAG_COMPOUND)).forEach((path, value) ->
                out.add(path + " " + value));
        return out;
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final StudioLayout.Rect body = laid.body();
        place(this.watch, new StudioLayout.Rect(body.right() - 82, body.y() - 1, 82, 14));
        place(this.again, new StudioLayout.Rect(body.right() - 82 - StudioLayout.GAP - 78, body.y() - 1, 78, 14));
    }

    @Override
    void shown() {
        this.ask();
    }

    @Override
    void tick() {
        if (this.watching && ++this.ticks % EVERY == 0) {
            this.ask();
        }
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final StudioLayout.Rect body = this.regions.body();
        StudioDraw.text(g, font(), words(ToolTexts.BLOCK_HEAD), body.x() + 2, body.y() + 2,
                this.again.x() - body.x() - StudioLayout.GAP, colours.text());
        final List<StudioLayout.Rect> columns = StudioLayout.columns(StudioLayout.below(body, 16), 1.0 / 3,
                1.0 / 3);
        StudioDraw.panel(g, font(), columns.get(0), words(ToolTexts.FIELDS_LIVE));
        StudioDraw.panel(g, font(), columns.get(1), words(ToolTexts.FACES));
        StudioDraw.panel(g, font(), columns.get(2), words(ToolTexts.IN_AND_OUT));
        if (this.studio.lookedAt().isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_BLOCK), columns.get(0).x() + StudioDraw.INSET,
                    columns.get(0).y() + StudioDraw.HEAD + 4, columns.get(0).width(), colours.dim());
            return;
        }
        if (this.answer == null) {
            StudioDraw.small(g, font(), words(ToolTexts.ASKING), columns.get(0).x() + StudioDraw.INSET,
                    columns.get(0).y() + StudioDraw.HEAD + 4, columns.get(0).width(), colours.dim());
            return;
        }
        if (this.answer.contains(StudioServices.REFUSED)) {
            StudioDraw.small(g, font(), this.answer.getString(StudioServices.REFUSED), columns.get(0).x()
                    + StudioDraw.INSET, columns.get(0).y() + StudioDraw.HEAD + 4, columns.get(0).width() * 3,
                    colours.error());
            return;
        }
        this.fields(g, columns.get(0), colours);
        this.faces(g, columns.get(1), colours);
        this.flows(g, columns.get(2), colours);
    }

    private void fields(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        final Map<String, String> fields = NbtLines.unlisted(this.answer.getList(StudioKeys.FIELDS,
                Tag.TAG_COMPOUND));
        if (fields.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_FIELDS), area.x() + StudioDraw.INSET,
                    area.y() + StudioDraw.HEAD + 4, area.width() - 2 * StudioDraw.INSET, colours.dim());
            return;
        }
        int y = area.y() + StudioDraw.HEAD + 2;
        for (final Map.Entry<String, String> field : fields.entrySet()) {
            if (y + StudioDraw.SMALL_ROW > area.bottom()) {
                break;
            }
            StudioDraw.row(g, font(), area, y, field.getKey(), field.getValue(), colours.label());
            y += StudioDraw.SMALL_ROW;
        }
    }

    private void faces(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        final ListTag faces = this.answer.getList(StudioKeys.FACES, Tag.TAG_COMPOUND);
        final int room = area.width() - 2 * StudioDraw.INSET - 18;
        for (int i = 0; i < faces.size(); i++) {
            final CompoundTag face = faces.getCompound(i);
            final int y = area.y() + StudioDraw.HEAD + 2 + i * FACE_ROW;
            StudioDraw.text(g, font(), words(SIDES.getOrDefault(face.getString(StudioKeys.SIDE), ToolTexts.NOTHING)),
                    area.x() + StudioDraw.INSET, y, room, colours.text());
            StudioDraw.small(g, font(), offered(face), area.x() + StudioDraw.INSET, y + 10, room, colours.dim());
            final ListTag items = face.getList(StudioKeys.ITEMS, Tag.TAG_COMPOUND);
            if (!items.isEmpty()) {
                final ResourceLocation id = ResourceLocation.tryParse(items.getCompound(0).getString(StudioKeys.ID));
                if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                    g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(id)), area.right() - StudioDraw.INSET
                            - 16, y + 1);
                }
            } else if (face.contains(StudioKeys.ENERGY)) {
                final int capacity = Math.max(1, face.getInt(StudioKeys.CAPACITY));
                final int left = area.right() - StudioDraw.INSET - 12;
                g.fill(left, y + 2, left + 12, y + 16, colours.track());
                final int tall = Math.round(14.0F * face.getInt(StudioKeys.ENERGY) / capacity);
                g.fill(left + 1, y + 16 - tall, left + 11, y + 16, colours.energy());
            }
        }
    }

    /* What a face offers, in words: its energy, its slots and their first items, its tanks. */
    private static String offered(final CompoundTag face) {
        final List<String> parts = new ArrayList<>();
        if (face.contains(StudioKeys.ENERGY)) {
            parts.add(GameText.resolve(ToolTexts.FACE_ENERGY.with(face.getInt(StudioKeys.ENERGY),
                    face.getInt(StudioKeys.CAPACITY))));
        }
        if (face.contains(StudioKeys.SLOTS)) {
            parts.add(GameText.resolve(ToolTexts.FACE_ITEMS.with(face.getInt(StudioKeys.SLOTS),
                    held(face.getList(StudioKeys.ITEMS, Tag.TAG_COMPOUND)))));
        }
        if (face.contains(StudioKeys.TANKS)) {
            final ListTag tanks = face.getList(StudioKeys.TANKS, Tag.TAG_COMPOUND);
            parts.add(GameText.resolve(ToolTexts.FACE_FLUID.with(tanks.size(), held(tanks))));
        }
        return parts.isEmpty() ? GameText.resolve(ToolTexts.NOTHING) : String.join("; ", parts);
    }

    private static String held(final ListTag held) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < held.size(); i++) {
            final CompoundTag one = held.getCompound(i);
            if (!one.getString(StudioKeys.ID).isEmpty()) {
                out.append(GameText.resolve(ToolTexts.HELD.with(nameOf(one.getString(StudioKeys.ID)),
                        one.getInt(StudioKeys.COUNT))));
            }
        }
        return out.toString();
    }

    private void flows(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        if (!this.answer.contains(StudioKeys.FLOW)) {
            StudioDraw.small(g, font(), words(ToolTexts.NOT_COUNTED), area.x() + StudioDraw.INSET,
                    area.y() + StudioDraw.HEAD + 4, area.width() - 2 * StudioDraw.INSET, colours.dim());
            return;
        }
        for (int kind = 0; kind < FLOWS; kind++) {
            final int y = area.y() + StudioDraw.HEAD + 2 + kind * FLOW_ROW;
            if (y + FLOW_ROW > area.bottom() + 4) {
                break;
            }
            final long now = this.perSecond[kind][SECONDS - 1];
            final TextKey unit = kind < 2 ? ToolTexts.FE_PER_SECOND : kind < 4 ? ToolTexts.PER_SECOND
                    : ToolTexts.MB_PER_SECOND;
            StudioDraw.text(g, font(), words(FLOW_NAMES.get(kind)), area.x() + StudioDraw.INSET, y,
                    area.width() / 2, colours.text());
            final String value = GameText.resolve(unit.with(now));
            StudioDraw.small(g, font(), value, area.right() - StudioDraw.INSET - Math.round(font().width(value)
                    * StudioDraw.SMALL), y + 1, area.width() / 2, colours.label());
            final StudioLayout.Rect chart = new StudioLayout.Rect(area.x() + StudioDraw.INSET, y + 11,
                    area.width() - 2 * StudioDraw.INSET, FLOW_ROW - 16);
            g.fill(chart.x(), chart.y(), chart.right(), chart.bottom(), colours.track());
            StudioDraw.bars(g, new StudioLayout.Rect(chart.x() + 2, chart.y() + 2, chart.width() - 4,
                    chart.height() - 3), this.perSecond[kind], kind < 2 ? colours.energy() : colours.items(), -1, 0);
        }
    }

    private void ask() {
        final Optional<BlockPos> at = this.studio.lookedAt();
        if (at.isEmpty() || this.asking || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        this.asking = true;
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.POS, at.get().asLong());
        StudioAnswers.ask(StudioServices.BLOCK, args, answered -> {
            this.asking = false;
            this.answer = answered;
            this.count(answered);
        });
    }

    /* Each second's flow from the totals read a second apart, the newest at the right of the bars. */
    private void count(final CompoundTag answered) {
        if (!answered.contains(StudioKeys.FLOW)) {
            return;
        }
        final CompoundTag flow = answered.getCompound(StudioKeys.FLOW);
        final long[] now = new long[FLOWS];
        for (int kind = 0; kind < FLOWS; kind++) {
            now[kind] = flow.getLong(FLOW_KEYS.get(kind));
        }
        if (!this.totals.isEmpty()) {
            final long[] before = this.totals.getLast();
            for (int kind = 0; kind < FLOWS; kind++) {
                System.arraycopy(this.perSecond[kind], 1, this.perSecond[kind], 0, SECONDS - 1);
                this.perSecond[kind][SECONDS - 1] = Math.max(0, now[kind] - before[kind]);
            }
        }
        this.totals.clear();
        this.totals.add(now);
    }

    private void lookAgain() {
        this.studio.say(words(ToolTexts.LOOK_AGAIN_SAID));
        Minecraft.getInstance().setScreen(null);
    }

    private String placeName(final BlockPos at) {
        final Minecraft game = Minecraft.getInstance();
        final String block = game.level == null ? "" : game.level.getBlockState(at).getBlock().getName().getString();
        return GameText.resolve(ToolTexts.AT.with(block, GameText.resolve(ToolTexts.PLACE.with(at.getX(), at.getY(),
                at.getZ()))));
    }

    /** The name of the item or the fluid of {@code id}, as the player reads it. */
    static String nameOf(final String id) {
        final ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return id;
        }
        if (BuiltInRegistries.ITEM.containsKey(key)) {
            return BuiltInRegistries.ITEM.get(key).getDescription().getString();
        }
        if (BuiltInRegistries.FLUID.containsKey(key)) {
            return BuiltInRegistries.FLUID.get(key).getFluidType().getDescription().getString();
        }
        return id;
    }
}
