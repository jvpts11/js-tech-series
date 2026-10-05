/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.help.HelpCommand;
import dev.jstech.computers.gui.help.HelpTarget;
import dev.jstech.computers.gui.help.HelpTexts;
import dev.jstech.computers.gui.help.HelpWindowTexts;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.guide.GuideRecipes;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A page of a help window, laid out once for its width and drawn from then on: an entry of a manual with its headings,
 * steps, tables, pictures, recipes and links, a chapter or a section with what it holds, a manual's top page and its
 * index, the machine's commands and a command's page, and the results of a search, the pages read and the favorites.
 *
 * <p>Every form draws its page this way, in its own colours and with its own size of title, so the entry reads the same
 * on Frames 95 and on CDE and only looks like each.
 */
final class HelpPageView {

    private final List<Op> ops = new ArrayList<>();
    private final Style style;
    private final Font font;
    private final int width;
    private int y = PAD;
    /** Where the title's picture ends, so the text beside it keeps clear of it until then. */
    private int iconBottom;

    /** The link Favorites shows to keep the page; it is no page, so it is told apart by what it says. */
    static final String FAVORITE = "favorite||";

    private static final int PAD = 4;
    private static final int LINE = 10;
    private static final int GAP = 3;
    private static final int TERM_INDENT = 8;
    private static final int ICON = 32;
    private static final int RECIPE_ROW = 18;

    private HelpPageView(final Font font, final int width, final Style style) {
        this.font = font;
        this.width = width;
        this.style = style;
    }

    /**
     * How a form draws a page: its ink, its title, its headings, its links, its quiet text, its warnings, the lines of
     * a table, its shading, and how large its title is.
     */
    record Style(int ink, int title, int heading, int link, int faint, int warning, int rule, int shade,
                 float titleScale) {
    }

    /** What a page is when it is not a manual's or a command's. */
    record Special(HelpSession.Special kind, String searched, List<HelpTarget> listed) {
    }

    /** A page laid out: what to draw, and how tall it is. */
    record Laid(List<Op> ops, int height) {

        /** The link under a point of the page, measured from its top left, as written into it, or null. */
        @Nullable
        String linkAt(final double px, final double py) {
            for (final Op op : this.ops) {
                if (op instanceof Text text && !text.link().isEmpty() && px >= text.x() && px < text.x() + text.w()
                        && py >= text.y() - 1 && py < text.y() + LINE) {
                    return text.link();
                }
            }
            return null;
        }

        /** Every link of the page, as written into it, in the order they stand, for a test to follow. */
        List<String> links() {
            final List<String> out = new ArrayList<>();
            for (final Op op : this.ops) {
                if (op instanceof Text text && !text.link().isEmpty()) {
                    out.add(text.link());
                }
            }
            return out;
        }

        /** Every piece of text the page shows, in order, for a test to read. */
        List<String> words() {
            final List<String> out = new ArrayList<>();
            for (final Op op : this.ops) {
                if (op instanceof Text text) {
                    out.add(text.text());
                }
            }
            return out;
        }

        /** Draws the page with its top left at that point. */
        void draw(final GuiGraphics g, final Font font, final int x, final int y, final double mouseX,
                  final double mouseY, final int linkHover) {
            for (final Op op : this.ops) {
                switch (op) {
                    case Text text -> {
                        final boolean over = !text.link().isEmpty() && mouseX >= x + text.x()
                                && mouseX < x + text.x() + text.w() && mouseY >= y + text.y() - 1
                                && mouseY < y + text.y() + LINE;
                        final int colour = over ? linkHover : text.colour();
                        if (text.scale() != 1.0f) {
                            g.pose().pushPose();
                            g.pose().translate(x + text.x(), y + text.y(), 0);
                            g.pose().scale(text.scale(), text.scale(), 1.0f);
                            Draw.text(g, font, styled(text.text(), text.bold()), 0, 0, colour);
                            g.pose().popPose();
                        } else {
                            Draw.text(g, font, styled(text.text(), text.bold()), x + text.x(), y + text.y(), colour);
                        }
                    }
                    case Fill fill -> g.fill(x + fill.x(), y + fill.y(), x + fill.x() + fill.w(),
                            y + fill.y() + fill.h(), fill.colour());
                    case Outline outline -> Draw.outline(g, x + outline.x(), y + outline.y(), outline.w(),
                            outline.h(), outline.colour());
                    case Stack stack -> {
                        g.pose().pushPose();
                        g.pose().translate(x + stack.x(), y + stack.y(), 0);
                        g.pose().scale(stack.scale(), stack.scale(), 1.0f);
                        g.renderItem(stack.stack(), 0, 0);
                        g.renderItemDecorations(font, stack.stack(), 0, 0);
                        g.pose().popPose();
                    }
                }
            }
        }
    }

    /** Something drawn on a page. */
    sealed interface Op permits Text, Fill, Outline, Stack {
    }

    /** Words, perhaps bold, perhaps larger, perhaps a link. */
    record Text(int x, int y, int w, String text, int colour, boolean bold, float scale, String link) implements Op {
    }

    /** A block of colour. */
    record Fill(int x, int y, int w, int h, int colour) implements Op {
    }

    /** A box's edge. */
    record Outline(int x, int y, int w, int h, int colour) implements Op {
    }

    /** An item, drawn at a scale. */
    record Stack(int x, int y, ItemStack stack, float scale) implements Op {
    }

    /** Lays out what a session shows, for a page that many pixels wide. */
    static Laid lay(final Font font, final int width, final Style style, final HelpTarget target,
                    @Nullable final Special special, final HelpSource source) {
        final HelpPageView page = new HelpPageView(font, Math.max(60, width), style);
        if (special != null && special.kind() != HelpSession.Special.NONE) {
            page.special(special, source);
        } else {
            page.target(target, source);
        }
        return new Laid(List.copyOf(page.ops), page.y + PAD);
    }

    /** How a link names where it goes: an entry's number and title, a manual's title, a command's name. */
    static String label(final HelpTarget target) {
        return switch (target.kind()) {
            case NODE -> HelpBooks.manual(target.manual()).map(reader -> reader.label(target.id())).orElse(target.id());
            case CONTENTS -> HelpBooks.manual(target.manual()).map(ManualReader::title).orElse(target.manual());
            case INDEX -> GameText.resolve(HelpWindowTexts.INDEX) + ": "
                    + HelpBooks.manual(target.manual()).map(ManualReader::title).orElse(target.manual());
            case COMMAND -> target.id().isEmpty() ? GameText.resolve(HelpTexts.COMMANDS_PAGES) : target.id();
        };
    }

    private static Component styled(final String text, final boolean bold) {
        return bold ? bold(text) : Component.literal(text);
    }

    private void target(final HelpTarget target, final HelpSource source) {
        switch (target.kind()) {
            case NODE -> {
                final Optional<ManualReader> reader = HelpBooks.manual(target.manual());
                if (reader.isEmpty()) {
                    return;
                }
                final Optional<ManualReader.Article> article = reader.get().article(target.id());
                if (article.isPresent()) {
                    this.article(reader.get(), article.get(), source);
                } else {
                    this.title(reader.get().label(target.id()), "");
                    for (final ManualReader.Node child : reader.get().children(target.id())) {
                        this.link(0, child.number() + " " + child.title(),
                                HelpTarget.node(reader.get().manualId(), child.id()));
                    }
                }
            }
            case CONTENTS -> HelpBooks.manual(target.manual()).ifPresent(this::top);
            case INDEX -> HelpBooks.manual(target.manual()).ifPresent(reader -> {
                this.title(label(target), "");
                for (final ManualReader.IndexLine line : reader.index()) {
                    this.link(line.term() ? TERM_INDENT : 0, line.text(), HelpTarget.node(reader.manualId(),
                            line.target()));
                }
            });
            case COMMAND -> {
                if (target.id().isEmpty()) {
                    this.commands(source.commands());
                } else {
                    this.command(target.id(), source.commandPage(target.id()));
                }
            }
        }
    }

    /** A manual's top page: its title and every chapter, then the machine's commands. */
    private void top(final ManualReader reader) {
        this.title(reader.title(), "");
        for (final String chapter : reader.chapters()) {
            reader.node(chapter).ifPresent(node -> this.link(0, node.number() + " " + node.title(),
                    HelpTarget.node(reader.manualId(), node.id())));
        }
        this.y += GAP;
        this.link(0, GameText.resolve(HelpTexts.COMMANDS_PAGES), HelpTarget.command(""));
    }

    private void commands(final List<HelpCommand> commands) {
        this.title(GameText.resolve(HelpTexts.COMMANDS_PAGES), "");
        if (commands.isEmpty()) {
            this.words(GameText.resolve(HelpTexts.NOTHING_YET), 0, this.style.faint(), false);
            return;
        }
        final Map<String, List<HelpCommand>> groups = new LinkedHashMap<>();
        for (final HelpCommand command : commands) {
            groups.computeIfAbsent(command.group(), group -> new ArrayList<>()).add(command);
        }
        for (final Map.Entry<String, List<HelpCommand>> group : groups.entrySet()) {
            this.heading(group.getKey(), false);
            for (final HelpCommand command : group.getValue()) {
                final int nameW = this.font.width(command.name());
                this.ops.add(new Text(TERM_INDENT, this.y, nameW, command.name(), this.style.link(), false, 1.0f,
                        HelpTarget.command(command.name()).written()));
                final String summary = this.fit(command.summary(), this.width - 2 * PAD - TERM_INDENT - nameW - 8);
                this.ops.add(new Text(TERM_INDENT + nameW + 8, this.y, this.font.width(summary), summary,
                        this.style.faint(), false, 1.0f, ""));
                this.y += LINE;
            }
        }
    }

    private void command(final String name, @Nullable final List<String> lines) {
        this.title(name.toUpperCase(Locale.ROOT), "");
        if (lines == null) {
            this.words(GameText.resolve(HelpTexts.NOTHING_YET), 0, this.style.faint(), false);
            return;
        }
        for (final String line : lines) {
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            if (line.isBlank()) {
                this.y += LINE / 2;
            } else if (indent == 0) {
                this.heading(line, false);
            } else {
                this.words(line.strip(), Math.min(4 * TERM_INDENT, indent * 2), this.style.ink(), false);
            }
        }
    }

    private void special(final Special special, final HelpSource source) {
        switch (special.kind()) {
            case RESULTS -> this.results(special.searched(), source);
            case HISTORY -> {
                this.title(GameText.resolve(HelpWindowTexts.HISTORY), "");
                if (special.listed().isEmpty()) {
                    this.words(GameText.resolve(HelpWindowTexts.NO_HISTORY), 0, this.style.faint(), false);
                }
                for (final HelpTarget target : special.listed()) {
                    this.link(0, label(target), target);
                }
            }
            case FAVORITES -> {
                this.title(GameText.resolve(HelpWindowTexts.FAVORITES), "");
                this.ops.add(new Text(PAD, this.y, this.font.width(GameText.resolve(HelpWindowTexts.ADD_FAVORITE)),
                        GameText.resolve(HelpWindowTexts.ADD_FAVORITE), this.style.link(), false, 1.0f, FAVORITE));
                this.y += LINE + GAP;
                if (special.listed().isEmpty()) {
                    this.words(GameText.resolve(HelpWindowTexts.NO_FAVORITES), 0, this.style.faint(), false);
                }
                for (final HelpTarget target : special.listed()) {
                    this.link(0, label(target), target);
                }
            }
            case NONE -> {
            }
        }
    }

    private void results(final String searched, final HelpSource source) {
        final List<HelpTarget> found = new ArrayList<>();
        for (final ManualReader reader : HelpBooks.all()) {
            for (final String entry : reader.search(searched)) {
                found.add(HelpTarget.node(reader.manualId(), entry));
            }
        }
        final String wanted = searched.toLowerCase(Locale.ROOT);
        for (final HelpCommand command : source.commands()) {
            if (command.name().toLowerCase(Locale.ROOT).contains(wanted)
                    || command.summary().toLowerCase(Locale.ROOT).contains(wanted)) {
                found.add(HelpTarget.command(command.name()));
            }
        }
        if (found.isEmpty()) {
            this.title(GameText.resolve(HelpWindowTexts.NO_RESULTS.with(searched)), "");
            return;
        }
        this.title(GameText.resolve(HelpWindowTexts.RESULTS.with(found.size(), searched)), "");
        String manual = null;
        for (final HelpTarget target : found) {
            if (target.kind() == HelpTarget.Kind.NODE && !target.manual().equals(manual)) {
                manual = target.manual();
                this.heading(label(HelpTarget.contents(manual)), false);
            } else if (target.kind() == HelpTarget.Kind.COMMAND && manual != null) {
                manual = null;
                this.heading(GameText.resolve(HelpTexts.COMMANDS_PAGES), false);
            }
            this.link(TERM_INDENT, label(target), target);
        }
    }

    private void article(final ManualReader reader, final ManualReader.Article article, final HelpSource source) {
        this.title(article.number() + " " + article.title(), article.icon());
        for (final ManualReader.Piece piece : article.pieces()) {
            switch (piece) {
                case ManualReader.Piece.Heading heading -> this.heading(heading.text(), heading.warning());
                case ManualReader.Piece.Paragraph paragraph -> this.words(paragraph.text(), 0, switch (paragraph
                        .tone()) {
                    case PLAIN -> this.style.ink();
                    case NOTE -> this.style.link();
                    case WARNING -> this.style.warning();
                }, false);
                case ManualReader.Piece.Item item -> {
                    final int markerW = this.font.width(item.marker() + " ");
                    this.ops.add(new Text(PAD, this.y, markerW, item.marker(), this.style.ink(), true, 1.0f, ""));
                    this.words(item.text(), markerW, this.style.ink(), false);
                }
                case ManualReader.Piece.Term term -> {
                    this.words(term.term(), 0, this.style.ink(), true);
                    this.words(term.text(), TERM_INDENT, this.style.ink(), false);
                }
                case ManualReader.Piece.Table table -> this.table(table);
                case ManualReader.Piece.Picture picture -> this.picture(picture);
                case ManualReader.Piece.Recipes recipes -> this.recipes(recipes, source);
                case ManualReader.Piece.Links links -> this.links(reader, links);
            }
        }
    }

    /** A page's title, larger in the forms whose pages had one so, with the item it is about at its right. */
    private void title(final String title, final String icon) {
        final float scale = this.style.titleScale();
        final int room = this.width - 2 * PAD - (icon.isEmpty() ? 0 : ICON + GAP);
        final int iconTop = this.y;
        for (final String line : this.wrap(title, (int) (room / scale), true)) {
            this.ops.add(new Text(PAD, this.y, (int) (this.font.width(bold(line)) * scale), line,
                    this.style.title(), true, scale, ""));
            this.y += (int) (LINE * scale) + 1;
        }
        final ItemStack stack = stack(icon);
        if (!stack.isEmpty()) {
            final int x = this.width - PAD - ICON;
            this.ops.add(new Fill(x, iconTop, ICON, ICON, this.style.shade()));
            this.ops.add(new Stack(x, iconTop, stack, 2.0f));
            this.ops.add(new Outline(x, iconTop, ICON, ICON, this.style.rule()));
            this.iconBottom = iconTop + ICON + GAP;
        }
        this.y += GAP;
    }

    private void heading(final String text, final boolean warning) {
        this.y += GAP;
        this.words(text, 0, warning ? this.style.warning() : this.style.heading(), true);
    }

    /** Words wrapped to the page, set in that far. */
    private void words(final String words, final int indent, final int colour, final boolean bold) {
        final int room = this.room(indent);
        for (final String line : this.wrap(words, room, bold)) {
            this.ops.add(new Text(PAD + indent, this.y, this.font.width(bold ? bold(line) : Component.literal(line)),
                    line, colour, bold, 1.0f, ""));
            this.y += LINE;
        }
    }

    private void link(final int indent, final String text, final HelpTarget target) {
        final String shown = this.fit(text, this.room(indent));
        this.ops.add(new Text(PAD + indent, this.y, this.font.width(shown), shown, this.style.link(), false, 1.0f,
                target.written()));
        this.y += LINE;
    }

    private void table(final ManualReader.Piece.Table table) {
        this.y += GAP;
        this.words(table.caption(), 0, this.style.ink(), false);
        int widest = 0;
        for (final ManualReader.Row row : table.rows()) {
            widest = Math.max(widest, this.font.width(row.label()));
        }
        final int tableW = Math.min(this.width - 2 * PAD, Math.max(120, widest * 2 + 40));
        final int labelW = Math.min(widest + 8, tableW / 2);
        for (final ManualReader.Row row : table.rows()) {
            final String value = this.fit(row.value(), tableW - labelW - 6);
            this.ops.add(new Fill(PAD, this.y, labelW, LINE + 2, this.style.shade()));
            this.ops.add(new Outline(PAD, this.y, labelW, LINE + 2, this.style.rule()));
            this.ops.add(new Outline(PAD + labelW - 1, this.y, tableW - labelW + 1, LINE + 2, this.style.rule()));
            this.ops.add(new Text(PAD + 3, this.y + 2, this.font.width(row.label()), this.fit(row.label(),
                    labelW - 4), this.style.ink(), false, 1.0f, ""));
            this.ops.add(new Text(PAD + labelW + 3, this.y + 2, this.font.width(value), value, this.style.ink(),
                    false, 1.0f, ""));
            this.y += LINE + 1;
        }
        this.y += GAP + 1;
    }

    private void picture(final ManualReader.Piece.Picture picture) {
        final ItemStack stack = stack(picture.item());
        if (stack.isEmpty()) {
            return;
        }
        this.y += GAP;
        this.ops.add(new Fill(PAD, this.y, ICON + 8, ICON + 8, this.style.shade()));
        this.ops.add(new Outline(PAD, this.y, ICON + 8, ICON + 8, this.style.rule()));
        this.ops.add(new Stack(PAD + 4, this.y + 4, stack, 2.0f));
        this.y += ICON + 8 + 2;
        if (!picture.caption().isEmpty()) {
            this.words(picture.caption(), 0, this.style.faint(), false);
        }
        this.y += GAP;
    }

    private void recipes(final ManualReader.Piece.Recipes recipes, final HelpSource source) {
        final List<GuideRecipes.View> views = source.recipeViews(recipes.type(), recipes.output());
        if (views.isEmpty()) {
            this.words(GameText.resolve(HelpTexts.NO_RECIPES), 0, this.style.faint(), false);
            return;
        }
        for (final GuideRecipes.View view : views) {
            int x = PAD;
            for (final List<ItemStack> choices : view.inputs()) {
                if (!choices.isEmpty()) {
                    this.ops.add(new Outline(x, this.y, 18, 18, this.style.rule()));
                    this.ops.add(new Stack(x + 1, this.y + 1, choices.getFirst(), 1.0f));
                    x += 19;
                }
            }
            this.ops.add(new Text(x + 2, this.y + 5, this.font.width("->"), "->", this.style.ink(), false, 1.0f,
                    ""));
            x += this.font.width("->") + 6;
            for (final ItemStack output : view.outputs()) {
                this.ops.add(new Outline(x, this.y, 18, 18, this.style.rule()));
                this.ops.add(new Stack(x + 1, this.y + 1, output, 1.0f));
                x += 19;
            }
            final List<String> cost = new ArrayList<>();
            if (!view.time().getString().isEmpty()) {
                cost.add(view.time().getString());
            }
            if (!view.energy().getString().isEmpty()) {
                cost.add(view.energy().getString());
            }
            if (!cost.isEmpty()) {
                final String said = this.fit(String.join(", ", cost), this.width - PAD - x - 4);
                this.ops.add(new Text(x + 4, this.y + 5, this.font.width(said), said, this.style.faint(), false,
                        1.0f, ""));
            }
            this.y += RECIPE_ROW + 1;
        }
        this.y += GAP;
    }

    private void links(final ManualReader reader, final ManualReader.Piece.Links links) {
        this.y += GAP;
        int x = PAD;
        final String lead = links.lead() + " ";
        this.ops.add(new Text(x, this.y, this.font.width(lead), lead, this.style.ink(), false, 1.0f, ""));
        x += this.font.width(lead);
        for (int i = 0; i < links.links().size(); i++) {
            final ManualReader.Link link = links.links().get(i);
            final String text = link.text() + (i + 1 < links.links().size() ? "," : "");
            final int w = this.font.width(text);
            if (x + w > this.width - PAD && x > PAD + this.font.width(lead)) {
                this.y += LINE;
                x = PAD;
            }
            this.ops.add(new Text(x, this.y, w, text, this.style.link(), false, 1.0f,
                    HelpTarget.node(reader.manualId(), link.target()).written()));
            x += w + this.font.width(" ");
        }
        this.y += LINE + GAP;
    }

    /** How wide a line set in that far may be, clear of the title's picture while beside it. */
    private int room(final int indent) {
        final int beside = this.y < this.iconBottom ? ICON + GAP : 0;
        return Math.max(20, this.width - 2 * PAD - indent - beside);
    }

    private List<String> wrap(final String words, final int room, final boolean bold) {
        final List<String> out = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : words.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            final String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && this.font.width(bold ? bold(next) : Component.literal(next)) > room) {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(next);
            }
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    private String fit(final String text, final int room) {
        if (this.font.width(text) <= room) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(0, room - this.font.width("..."))) + "...";
    }

    private static Component bold(final String text) {
        return Component.literal(text).withStyle(ChatFormatting.BOLD);
    }

    private static ItemStack stack(final String item) {
        if (item == null || item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ResourceLocation id = ResourceLocation.tryParse(item);
        return id == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.get(id).getDefaultInstance();
    }
}
