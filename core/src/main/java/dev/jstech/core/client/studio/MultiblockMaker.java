/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.recipeview.MultiblockCard;
import dev.jstech.core.client.recipeview.MultiblockEntry;
import dev.jstech.core.client.recipeview.MultiblockRecipeViews;
import dev.jstech.core.client.recipeview.MultiblockViewPalette;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.MultiblockViewLayout;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Making a caught structure a multiblock, on the studio's page of structures: the block chosen as its terminal, the
 * letters the recorder gave each kind of block with how many it found, its layers as the pattern's file writes them,
 * how it may stand, and, written, the pattern's file; beside it, the page the recipe viewers will show of it.
 */
final class MultiblockMaker {

    private final StudioScreen studio;
    private final Supplier<String> name;
    private final Runnable relayout;
    private final Button another;
    private final Checkbox rotations;
    private final Checkbox mirrored;
    private final Checkbox lying;
    private final Button write;
    private final Button project;
    private @Nullable CompoundTag recorded;
    private @Nullable MultiblockPattern pattern;
    private @Nullable MultiblockCard card;
    private @Nullable MultiblockEntry entry;
    private String path = "";
    private boolean reload;
    private int tableY;
    private int resultsY;
    private float cardScale = 1;
    private int cardX;
    private int cardY;

    /* The pitch of a row of the letters and the layers, and the room a letter's own column takes. */
    private static final int ROW = 10;
    private static final int LETTER_ROOM = 10;
    private static final int BUTTON_HEIGHT = 18;
    /* The room a button leaves round its words. */
    private static final int LABEL_ROOM = 8;
    private static final int BOX_ROW = 16;
    private static final char AIR_SHOWN = '.';
    private static final StableNames<PortKind> PORTS = StableNames.of(PortKind.class);

    /* The terminal chosen, kept while the game runs, and how the pattern may stand. */
    private static @Nullable BlockPos terminal;
    private static boolean mirrors;
    private static boolean liesDown;
    private static boolean making;

    MultiblockMaker(final StudioScreen studio, final Panel panel, final Supplier<String> name,
                    final Runnable relayout, final Runnable toggleProjector, final BooleanSupplier projecting) {
        this.studio = studio;
        this.name = name;
        this.relayout = relayout;
        this.another = panel.add(new Button(() -> words(ToolTexts.CATCH_ANOTHER), () -> {
            making = false;
            relayout.run();
        }));
        this.rotations = panel.add(new Checkbox(() -> words(ToolTexts.FOUR_ROTATIONS), () -> true, () -> { })
                .setLabelScale(StudioDraw.SMALL));
        this.rotations.setEnabled(false);
        this.mirrored = panel.add(new Checkbox(() -> words(ToolTexts.MIRRORED_TOO), () -> mirrors, () -> {
            mirrors = !mirrors;
            this.ask(false);
        }).setLabelScale(StudioDraw.SMALL));
        this.lying = panel.add(new Checkbox(() -> words(ToolTexts.LYING_DOWN), () -> liesDown, () -> {
            liesDown = !liesDown;
            this.ask(false);
        }).setLabelScale(StudioDraw.SMALL));
        this.write = panel.add(new Button(() -> words(ToolTexts.WRITE_PATTERN), () -> this.ask(true))
                .setPrimary(true));
        this.project = panel.add(new Button(() -> words(projecting.getAsBoolean() ? ToolTexts.HIDE_PROJECTOR
                : ToolTexts.SHOW_PROJECTOR), toggleProjector));
    }

    /** Whether the page shows a structure being made a multiblock, rather than one being caught. */
    static boolean making() {
        return making;
    }

    /**
     * Starts making the structure between {@code first} and {@code second} a multiblock, its terminal the block the
     * studio was opened looking at; whether it could, the block looked at being between them.
     */
    boolean start(@Nullable final BlockPos first, @Nullable final BlockPos second) {
        final Optional<BlockPos> looked = this.studio.lookedAt();
        if (first == null || second == null || looked.isEmpty() || !between(looked.get(), first, second)) {
            this.studio.say(words(ToolTexts.LOOK_AT_TERMINAL));
            return false;
        }
        terminal = looked.get();
        making = true;
        this.path = "";
        this.ask(false);
        return true;
    }

    /** Its buttons and boxes shown, or hidden while the page catches a structure. */
    void show(final boolean shown) {
        this.another.setVisible(shown);
        this.rotations.setVisible(shown);
        this.mirrored.setVisible(shown);
        this.lying.setVisible(shown);
        this.write.setVisible(shown);
        this.project.setVisible(shown);
    }

    /** The pattern recorded, laid out to be shown in the world by the projector; none before it is. */
    Optional<ShapePreview> preview() {
        return this.pattern == null ? Optional.empty() : this.pattern.preview(0);
    }

    /** What it lists, a line a letter, as a test reads it. */
    List<String> lines() {
        final List<String> out = new ArrayList<>();
        if (this.recorded != null) {
            for (final Tag each : this.recorded.getList(StudioKeys.LETTERS, Tag.TAG_COMPOUND)) {
                final CompoundTag letter = (CompoundTag) each;
                out.add(letter.getString(StudioKeys.LETTER) + " " + letter.getString(StudioKeys.BLOCK) + " "
                        + letter.getInt(StudioKeys.COUNT));
            }
        }
        if (!this.path.isEmpty()) {
            out.add(this.path);
        }
        return out;
    }

    /** Lays its buttons and boxes out in the left column {@code left}. */
    void layout(final StudioLayout.Rect left) {
        final int x = left.x() + StudioDraw.INSET;
        final int wide = left.width() - 2 * StudioDraw.INSET;
        final int anotherWide = Math.min(wide / 3, studio.studioFont().width(words(ToolTexts.CATCH_ANOTHER)) + 10);
        StudioPage.place(this.another, new StudioLayout.Rect(left.right() - StudioDraw.INSET - anotherWide,
                left.y() + 2, anotherWide, 12));
        this.tableY = left.y() + 44;
        final int boxesY = this.tableY + StudioDraw.SMALL_ROW + ROW * this.tableRows() + 6;
        final int third = wide / 3;
        StudioPage.place(this.rotations, new StudioLayout.Rect(x, boxesY, third - 4, BOX_ROW - 4));
        StudioPage.place(this.mirrored, new StudioLayout.Rect(x + third, boxesY, third - 4, BOX_ROW - 4));
        StudioPage.place(this.lying, new StudioLayout.Rect(x + 2 * third, boxesY, third - 4, BOX_ROW - 4));
        final int half = wide / 2 - 2;
        final int buttonsY = boxesY + BOX_ROW + 4;
        if (this.fits(half)) {
            StudioPage.place(this.write, new StudioLayout.Rect(x, buttonsY, half, BUTTON_HEIGHT));
            StudioPage.place(this.project, new StudioLayout.Rect(x + half + 4, buttonsY, wide - half - 4,
                    BUTTON_HEIGHT));
            this.resultsY = buttonsY + BUTTON_HEIGHT + 6;
        } else {
            // Side by side their words would not fit: one under the other, each as wide as the column.
            StudioPage.place(this.write, new StudioLayout.Rect(x, buttonsY, wide, BUTTON_HEIGHT));
            StudioPage.place(this.project, new StudioLayout.Rect(x, buttonsY + BUTTON_HEIGHT + 4, wide,
                    BUTTON_HEIGHT));
            this.resultsY = buttonsY + 2 * BUTTON_HEIGHT + 10;
        }
    }

    /** Draws the left column {@code left} and, in {@code view}, the page the recipe viewers will show. */
    void render(final GuiGraphics g, final StudioLayout.Rect left, final StudioLayout.Rect right,
                final StudioLayout.Rect view, final int mouseX, final int mouseY) {
        final Font font = this.studio.studioFont();
        final StudioPalette.Colours colours = StudioPalette.get();
        StudioDraw.panel(g, font, left, words(ToolTexts.MAKE_MULTIBLOCK), this.another.width() + 4);
        if (this.recorded != null) {
            final int[] size = this.recorded.getIntArray(StudioKeys.SIZE);
            if (size.length == 3) {
                StudioDraw.row(g, font, left, left.y() + 19, words(ToolTexts.CAUGHT), GameText.resolve(
                        ToolTexts.CAUGHT_VALUE.with(size[0], size[1], size[2],
                                this.recorded.getInt(StudioKeys.BLOCKS))), colours.label());
            }
        }
        final BlockPos at = terminal;
        final Minecraft game = Minecraft.getInstance();
        if (at != null && game.level != null) {
            StudioDraw.row(g, font, left, left.y() + 30, words(ToolTexts.TERMINAL), GameText.resolve(
                    ToolTexts.TERMINAL_VALUE.with(game.level.getBlockState(at).getBlock().getName().getString(),
                            at.getX(), at.getY(), at.getZ())), colours.label());
        }
        this.drawLetters(g, font, left, colours);
        this.drawResults(g, font, left, colours);
        final boolean ready = this.pattern != null;
        this.write.setEnabled(ready);
        this.project.setEnabled(ready);
        StudioDraw.panel(g, font, right, words(ToolTexts.AS_VIEWER));
        g.fill(view.x(), view.y(), view.right(), view.bottom(), colours.viewer());
        this.drawCard(g, view, mouseX, mouseY);
    }

    /** A click at the page; whether the page of the recipe viewers took it. */
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return this.card != null && this.card.mouseClicked((mouseX - this.cardX) / this.cardScale,
                (mouseY - this.cardY) / this.cardScale, button);
    }

    /** A drag at the page; whether it turned the page's view. */
    boolean mouseDragged(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        return this.card != null && this.card.mouseDragged((mouseX - this.cardX) / this.cardScale,
                (mouseY - this.cardY) / this.cardScale, dragX / this.cardScale, dragY / this.cardScale);
    }

    /* The letters it found beside the layers, as many layers as there is room for. */
    private void drawLetters(final GuiGraphics g, final Font font, final StudioLayout.Rect left,
                             final StudioPalette.Colours colours) {
        final int x = left.x() + StudioDraw.INSET;
        StudioDraw.small(g, font, words(ToolTexts.LETTERS_FOUND), x, this.tableY, left.width() / 2,
                colours.text());
        final List<String[]> rows = this.tableLines();
        final int layersX = left.x() + Math.round(left.width() * 0.46F);
        final int countRight = layersX - 8;
        for (int i = 0; i < rows.size(); i++) {
            final int y = this.tableY + StudioDraw.SMALL_ROW + i * ROW;
            final String[] row = rows.get(i);
            Draw.textScaled(g, font, row[0], x, y, colours.selected(), StudioDraw.SMALL);
            final int countWide = Math.round(font.width(row[2]) * StudioDraw.SMALL);
            StudioDraw.small(g, font, row[1], x + LETTER_ROOM, y, countRight - x - LETTER_ROOM - countWide - 4,
                    colours.label());
            Draw.textScaled(g, font, row[2], countRight - countWide, y, colours.dim(), StudioDraw.SMALL);
        }
        final MultiblockPattern shown = this.pattern;
        if (shown == null) {
            return;
        }
        final int pitch = Math.max(1, Math.round(font.width("M ") * StudioDraw.SMALL));
        final int layerWide = shown.sizeX() * pitch + pitch;
        final int fit = Math.max(1, Math.min(shown.sizeY(), (left.right() - StudioDraw.INSET - layersX)
                / Math.max(1, layerWide)));
        for (int layer = 0; layer < fit; layer++) {
            final int lx = layersX + layer * layerWide;
            StudioDraw.small(g, font, GameText.resolve(ToolTexts.LAYER.with(layer + 1)), lx, this.tableY,
                    layerWide, colours.dim());
            final String[] lines = shown.rows(layer);
            for (int z = 0; z < lines.length; z++) {
                final StringBuilder spaced = new StringBuilder();
                for (final char each : lines[z].toCharArray()) {
                    spaced.append(each == MultiblockPattern.IGNORE_CHAR ? AIR_SHOWN : each).append(' ');
                }
                Draw.textScaled(g, font, spaced.toString().trim(), lx, this.tableY + StudioDraw.SMALL_ROW + z * ROW,
                        colours.label(), StudioDraw.SMALL);
            }
        }
    }

    /* What was written, each with its tick. */
    private void drawResults(final GuiGraphics g, final Font font, final StudioLayout.Rect left,
                             final StudioPalette.Colours colours) {
        if (this.path.isEmpty()) {
            return;
        }
        final int x = left.x() + StudioDraw.INSET;
        final List<String> done = List.of(this.path, words(ToolTexts.ITS_VIEWER_PAGE), words(ToolTexts.ITS_PREVIEW));
        for (int i = 0; i < done.size(); i++) {
            final int y = this.resultsY + i * StudioDraw.SMALL_ROW;
            StudioMarks.draw(g, font, x, y - 1, StudioSheet.Mark.FINE);
            StudioDraw.small(g, font, done.get(i), x + StudioMarks.WIDTH + 4, y,
                    left.width() - 3 * StudioDraw.INSET - StudioMarks.WIDTH, colours.dim());
        }
        if (this.reload) {
            StudioDraw.small(g, font, words(ToolTexts.AFTER_RELOAD), x, this.resultsY + done.size()
                    * StudioDraw.SMALL_ROW, left.width() - 2 * StudioDraw.INSET, colours.dim());
        }
    }

    /* The page the recipe viewers will show, as large as {@code view} holds it, its slots with what they take. */
    private void drawCard(final GuiGraphics g, final StudioLayout.Rect view, final int mouseX, final int mouseY) {
        final MultiblockCard shown = this.card;
        final MultiblockEntry of = this.entry;
        if (shown == null || of == null) {
            return;
        }
        this.cardScale = Math.max(0.5F, Math.min((view.width() - 8) / (float) MultiblockViewLayout.WIDTH,
                (view.height() - 8) / (float) MultiblockViewLayout.HEIGHT));
        this.cardX = view.x() + (view.width() - Math.round(MultiblockViewLayout.WIDTH * this.cardScale)) / 2;
        this.cardY = view.y() + 4;
        g.pose().pushPose();
        g.pose().translate(this.cardX, this.cardY, 0);
        g.pose().scale(this.cardScale, this.cardScale, 1);
        final Font font = Minecraft.getInstance().font;
        final List<MultiblockEntry.Material> materials = of.materials();
        final MultiblockViewPalette.Colours slots = MultiblockViewPalette.get();
        final int side = MultiblockViewLayout.SLOT;
        for (int index = 0; index < materials.size(); index++) {
            final MultiblockViewLayout.Point at = MultiblockViewLayout.material(index);
            // A slot as the viewers draw one: a shadow above and left, a light below and right.
            g.fill(at.x(), at.y(), at.x() + side, at.y() + side, slots.shade());
            g.fill(at.x() + 1, at.y() + 1, at.x() + side, at.y() + side, slots.light());
            g.fill(at.x() + 1, at.y() + 1, at.x() + side - 1, at.y() + side - 1, slots.ground());
            final ItemStack stack = materials.get(index).stacks().getFirst();
            g.renderItem(stack, at.x() + 1, at.y() + 1);
            g.renderItemDecorations(font, stack, at.x() + 1, at.y() + 1);
        }
        shown.draw(g, (mouseX - this.cardX) / this.cardScale, (mouseY - this.cardY) / this.cardScale);
        g.pose().popPose();
    }

    /* Whether the words of both buttons, either way the projector stands, fit a button {@code width} wide. */
    private boolean fits(final int width) {
        final Font font = this.studio.studioFont();
        for (final TextKey label : List.of(ToolTexts.WRITE_PATTERN, ToolTexts.SHOW_PROJECTOR,
                ToolTexts.HIDE_PROJECTOR)) {
            if (font.width(words(label)) + LABEL_ROOM > width) {
                return false;
            }
        }
        return true;
    }

    /* Each row of the letters: the letter, what it stands for, and how many; the terminal and air last. */
    private List<String[]> tableLines() {
        final List<String[]> out = new ArrayList<>();
        if (this.recorded == null) {
            return out;
        }
        for (final Tag each : this.recorded.getList(StudioKeys.LETTERS, Tag.TAG_COMPOUND)) {
            final CompoundTag letter = (CompoundTag) each;
            final String port = letter.getString(StudioKeys.PORT);
            final Optional<PortKind> kind = port.isEmpty() ? Optional.empty()
                    : Optional.ofNullable(PORTS.find(port));
            out.add(new String[] {letter.getString(StudioKeys.LETTER), kind.map(found -> GameText.resolve(
                    ToolTexts.A_PORT.with(found.text()))).orElse(letter.getString(StudioKeys.BLOCK)),
                    String.valueOf(letter.getInt(StudioKeys.COUNT))});
        }
        out.add(new String[] {String.valueOf(MultiblockPattern.TERMINAL_CHAR), words(ToolTexts.THE_TERMINAL),
                "1"});
        out.add(new String[] {String.valueOf(AIR_SHOWN), words(ToolTexts.AIR_ANY), "-"});
        return out;
    }

    /* How many rows the letters and the layers take, the taller of the two. */
    private int tableRows() {
        final int layers = this.pattern == null ? 0 : this.pattern.sizeZ();
        return Math.max(this.tableLines().size(), layers);
    }

    /* Asks the server to record the structure, and to write its pattern when {@code saving}. */
    private void ask(final boolean saving) {
        final BlockPos first = StructuresPage.first();
        final BlockPos second = StructuresPage.second();
        final BlockPos chosen = terminal;
        if (first == null || second == null || chosen == null || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.FROM, first.asLong());
        args.putLong(StudioKeys.TO, second.asLong());
        args.putLong(StudioKeys.TERMINAL, chosen.asLong());
        args.putString(StudioKeys.NAME, this.name.get().trim());
        args.putBoolean(StudioKeys.MIRRORS, mirrors);
        args.putBoolean(StudioKeys.LIES_DOWN, liesDown);
        args.putBoolean(StudioKeys.SAVE, saving);
        StudioAnswers.ask(StudioServices.MULTIBLOCK, args, answered -> {
            if (answered.contains(StudioServices.REFUSED)) {
                this.studio.say(answered.getString(StudioServices.REFUSED));
                return;
            }
            this.recorded = answered;
            this.read(answered.getString(StudioKeys.PATTERN));
            if (saving) {
                this.path = answered.contains(StudioKeys.PATH) ? answered.getString(StudioKeys.PATH)
                        : words(ToolTexts.NOT_WRITTEN);
                this.reload = answered.getBoolean(StudioKeys.RELOAD);
            }
            this.relayout.run();
        });
    }

    /* The pattern the server recorded, read from its file's words, and the page the viewers will show of it. */
    private void read(final String json) {
        final ResourceLocation id = ResourceLocation.tryParse(this.name.get().trim());
        this.pattern = MultiblockPatterns.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result()
                .orElse(null);
        this.entry = this.pattern == null || id == null ? null
                : MultiblockRecipeViews.of(id, this.pattern).orElse(null);
        this.card = this.entry == null ? null : new MultiblockCard(this.entry, true);
    }

    private static boolean between(final BlockPos at, final BlockPos first, final BlockPos second) {
        return within(at.getX(), first.getX(), second.getX()) && within(at.getY(), first.getY(), second.getY())
                && within(at.getZ(), first.getZ(), second.getZ());
    }

    private static boolean within(final int value, final int one, final int other) {
        return value >= Math.min(one, other) && value <= Math.max(one, other);
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
