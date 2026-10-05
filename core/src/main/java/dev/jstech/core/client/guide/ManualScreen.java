/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.api.client.GuideBlockRenderers;
import dev.jstech.core.api.client.IGuideBlockRenderer;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideLayout;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import dev.jstech.core.palette.PaletteRoles;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * An open manual: the binder in its style, one page or two at a time, with the chapter tabs at its edge, the contents
 * and search buttons on its top edge and the arrows on its sides.
 *
 * <p>It opens at its cover, or at the page of an entry. Clicking a line of the contents, a number after "See", a line
 * of the index or a chapter's tab goes there; the arrows, the arrow keys, Page Up and Page Down and the mouse wheel
 * turn the pages; the search button turns the left page into a search of the index that filters as the player types.
 * Pointing at an item shows its name and what the game says of it.
 */
public final class ManualScreen extends Screen {

    private final GuideManual manual;
    private final GuideStyle style;
    private final String openAt;
    private final GuideRecipes recipes = new GuideRecipes();
    private final Map<String, Integer> colours = new HashMap<>();
    private final Map<String, Integer> tabColours = new HashMap<>();
    private final Map<String, CompoundTag> customData = new HashMap<>();
    private final List<String> chapters = new ArrayList<>();
    private GuideBook book;
    private ManualScreenLayout.Geometry geometry;
    private int left;
    private int top;
    /** The spread shown: -1 is the closed cover, then 0 for the first page or pair of pages. */
    private int spread = -1;
    private boolean searching;
    private EditBox searchField;
    private ItemStack hoveredItem = ItemStack.EMPTY;
    private Component hoveredWords;

    /** How far below the lines of the index its dots sit, at the size they are drawn. */
    private static final int DOT_DROP = 6;
    private static final int DOT_PITCH = 3;
    private static final int LINK_DROP = 9;
    /** How far the rings reach into each page: past the gutter's shade, short of the text. */
    private static final int RING_SPAN = 8;
    private static final int[] RINGS = {28, 96, 164};
    private static final int SLOT = 18;
    private static final int SLOT_GAP = 2;
    private static final int ICON_SCALE_PERCENT = 75;
    private static final int LABEL_PAD = 10;
    private static final int CYCLE_MILLIS = 1000;
    private static final double SHADOW = 0.6;

    public ManualScreen(final GuideManual manual, final GuideStyle style, final String openAt) {
        super(GameText.component(GuideTexts.CONTENTS));
        this.manual = manual;
        this.style = style;
        this.openAt = openAt == null ? "" : openAt;
    }

    @Override
    protected void init() {
        this.resolveColours();
        final GuideLibrary library = GuideLibrary.loaded();
        final GuideContents contents = library.contentsOf(this.manual);
        this.chapters.clear();
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            this.chapters.add(chapter.chapter().namespace());
            this.tabColours.put(chapter.chapter().namespace(), this.tabColour(chapter.chapter()));
        }
        final int keep = this.spread;
        this.book = GuideLayout.lay(this.manual, this.style, contents,
                new ClientGuideText(this.font, this.style, this.recipes));
        this.geometry = ManualScreenLayout.of(this.style.pageWidth(), this.style.pageHeight(), this.style.spread());
        this.left = (this.width - this.geometry.width()) / 2;
        this.top = (this.height - this.geometry.height()) / 2;
        final ManualScreenLayout.Rect field = ManualScreenLayout.searchField(this.geometry, this.style.margin());
        final String typed = this.searchField == null ? "" : this.searchField.getValue();
        this.searchField = new SearchField(this.font, this.left + field.x() + 3, this.top + field.y() + 3,
                field.width() - 6, field.height() - 4, this.colour(GuideStyle.INK, ""));
        this.searchField.setBordered(false);
        this.searchField.setValue(typed);
        this.searchField.setVisible(this.searching);
        this.addRenderableWidget(this.searchField);
        if (keep >= 0) {
            this.spread = Math.min(keep, this.lastSpread());
        } else if (!this.openAt.isEmpty()) {
            this.book.pageOf(this.openAt).ifPresent(this::goToPage);
        }
    }

    /** The spread shown now: -1 for the cover. */
    public int spread() {
        return this.spread;
    }

    /** The manual laid out as it is shown. */
    public GuideBook book() {
        return this.book;
    }

    /** Whether the search is open on the left page. */
    public boolean searching() {
        return this.searching;
    }

    /** The manual it shows. */
    public GuideManual manual() {
        return this.manual;
    }

    /** Shows the spread holding that page. */
    public void goToPage(final int page) {
        this.searching = false;
        if (this.searchField != null) {
            this.searchField.setVisible(false);
        }
        this.spread = Math.max(0, Math.min(this.lastSpread(), this.style.spread() ? page / 2 : page));
    }

    /** Shows the spread holding where that entry, section or chapter starts, when the manual holds it. */
    public boolean goTo(final String target) {
        final OptionalInt page = this.book.pageOf(target);
        page.ifPresent(this::goToPage);
        return page.isPresent();
    }

    /** Opens or closes the search on the left page. */
    public void toggleSearch() {
        this.searching = !this.searching;
        if (this.spread < 0) {
            this.spread = 0;
        }
        this.searchField.setVisible(this.searching);
        this.setFocused(this.searching ? this.searchField : null);
        this.searchField.setFocused(this.searching);
    }

    /** Types into the search, as a player would. */
    public void search(final String words) {
        if (!this.searching) {
            this.toggleSearch();
        }
        this.searchField.setValue(words);
    }

    /** The index lines the search finds for what is typed, in index order. */
    public List<GuideBook.IndexLine> found() {
        final String wanted = this.searchField == null ? "" : this.searchField.getValue().strip()
                .toLowerCase(Locale.ROOT);
        final List<GuideBook.IndexLine> found = new ArrayList<>();
        for (final GuideBook.IndexLine line : this.book.index()) {
            if (wanted.isEmpty() || line.text().toLowerCase(Locale.ROOT).contains(wanted)) {
                found.add(line);
            }
        }
        return found;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        this.hoveredItem = ItemStack.EMPTY;
        this.hoveredWords = null;
        g.pose().pushPose();
        g.pose().translate(this.left, this.top, 0.0F);
        final int mx = mouseX - this.left;
        final int my = mouseY - this.top;
        if (this.spread < 0) {
            this.drawCover(g);
        } else {
            this.drawBinder(g, mx, my);
        }
        g.pose().popPose();
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (!this.hoveredItem.isEmpty()) {
            g.renderTooltip(this.font, this.hoveredItem, mouseX, mouseY);
        } else if (this.hoveredWords != null) {
            g.renderTooltip(this.font, this.hoveredWords, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        final double mx = mouseX - this.left;
        final double my = mouseY - this.top;
        if (this.spread < 0) {
            this.spread = 0;
            return true;
        }
        if (this.geometry.contents().contains(mx, my)) {
            this.goToPage(this.book.contents());
            return true;
        }
        if (this.geometry.search().contains(mx, my)) {
            this.toggleSearch();
            return true;
        }
        if (this.geometry.previous().contains(mx, my)) {
            return this.turn(-1);
        }
        if (this.geometry.next().contains(mx, my)) {
            return this.turn(1);
        }
        for (int i = 0; i < Math.min(this.chapters.size(), this.geometry.tabs()); i++) {
            if (ManualScreenLayout.tab(this.geometry, i).contains(mx, my)) {
                return this.goTo(this.chapters.get(i));
            }
        }
        final String link = this.linkAt(mx, my);
        return !link.isEmpty() && this.goTo(link);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        return scrollY != 0.0 && this.turn(scrollY < 0.0 ? 1 : -1);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.searching && this.searchField.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            return this.searchField.keyPressed(key, scanCode, modifiers);
        }
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> this.turn(1);
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> this.turn(-1);
            case GLFW.GLFW_KEY_HOME -> {
                this.goToPage(this.book.contents());
                yield true;
            }
            default -> super.keyPressed(key, scanCode, modifiers);
        };
    }

    /** Turns that many spreads, forward or back, never past the cover or the last. */
    public boolean turn(final int by) {
        final int to = Math.max(-1, Math.min(this.lastSpread(), this.spread + by));
        if (to == this.spread) {
            return false;
        }
        this.spread = to;
        if (this.searching) {
            this.toggleSearch();
        }
        return true;
    }

    private int lastSpread() {
        final int pages = this.book.pages().size();
        return this.style.spread() ? (pages - 1) / 2 : pages - 1;
    }

    private void resolveColours() {
        this.colours.clear();
        if (!this.style.palette().isEmpty()) {
            this.colours.putAll(PaletteLookup.roles(this.style.palette()));
        }
        this.style.colours().forEach((role, written) -> {
            final Integer colour = PaletteLookup.parse(written);
            if (colour != null) {
                this.colours.put(role, colour);
            }
        });
    }

    private int tabColour(final GuideChapter chapter) {
        final Integer own = PaletteLookup.colourOf(chapter.tab(), GuideStyle.TAB);
        return own == null ? this.colours.getOrDefault(GuideStyle.TAB, GuidePalettes.BINDER.get().tab()) : own;
    }

    /** A role's colour, a chapter's tab standing for the tab role on that chapter's pages. */
    private int colour(final String role, final String chapter) {
        if (GuideStyle.TAB.equals(role) && this.tabColours.containsKey(chapter)) {
            return this.tabColours.get(chapter);
        }
        final Integer colour = this.colours.get(role);
        if (colour != null) {
            return colour;
        }
        // A style that names no colour for a role is drawn in the binder's, so nothing is ever left unpainted.
        final Integer binder = PaletteRoles.read(GuidePalettes.BINDER.get()).get(role);
        return binder == null ? GuidePalettes.BINDER.get().ink() : binder;
    }

    private void drawCover(final GuiGraphics g) {
        final ManualScreenLayout.Rect page = this.geometry.left();
        final int x = page.x() - ManualScreenLayout.COVER;
        final int width = page.width() + 2 * ManualScreenLayout.COVER;
        this.bevel(g, x, 0, width, this.geometry.height(), GuideStyle.COVER);
        final int labelWidth = page.width() - 2 * LABEL_PAD;
        final int labelX = page.x() + LABEL_PAD;
        final int labelY = page.y() + page.height() / 5;
        final List<String> lines = new ArrayList<>();
        for (final String key : this.manual.coverKeys()) {
            lines.add(this.text(key));
        }
        final int height = LABEL_PAD * 2 + lines.size() * TextSize.SMALL.lineHeight()
                + 2 * TextSize.HEADING.lineHeight() + TextSize.SMALL.lineHeight() * 2;
        g.fill(labelX, labelY, labelX + labelWidth, labelY + height, this.colour(GuideStyle.LABEL, ""));
        Draw.outline(g, labelX, labelY, labelWidth, height, this.colour(GuideStyle.RULE, ""));
        int y = labelY + LABEL_PAD;
        for (final String line : lines) {
            this.centred(g, line, TextSize.SMALL, labelX, labelWidth, y, GuideStyle.FAINT);
            y += TextSize.SMALL.lineHeight();
        }
        y += 4;
        for (final String line : GuideLayout.wrap(this.text(this.manual.titleKey()), labelWidth - 8, TextSize.HEADING,
                new ClientGuideText(this.font, this.style, this.recipes))) {
            this.centred(g, line, TextSize.HEADING, labelX, labelWidth, y, GuideStyle.LABEL_INK);
            y += TextSize.HEADING.lineHeight();
        }
        if (!this.manual.edition().isEmpty()) {
            this.centred(g, this.text(this.manual.edition()), TextSize.SMALL, labelX, labelWidth, y + 2,
                    GuideStyle.FAINT);
        }
        if (!this.manual.partNumber().isEmpty()) {
            this.centred(g, this.manual.partNumber(), TextSize.SMALL, labelX, labelWidth,
                    page.y() + page.height() - TextSize.SMALL.lineHeight() - 4, GuideStyle.LABEL);
        }
    }

    private void drawBinder(final GuiGraphics g, final int mx, final int my) {
        this.bevel(g, 0, 0, this.geometry.coverRight(), this.geometry.height(), GuideStyle.COVER);
        this.drawTabs(g, mx, my);
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        this.drawPage(g, this.geometry.left(), first, true, mx, my);
        if (this.geometry.right() != null) {
            this.drawPage(g, this.geometry.right(), this.rightPage(first), false, mx, my);
            if (this.style.rings()) {
                this.drawRings(g);
            }
        }
        this.drawButton(g, this.geometry.contents(), false, mx, my);
        this.drawButton(g, this.geometry.search(), true, mx, my);
        if (this.spread > -1) {
            this.drawArrow(g, this.geometry.previous(), true);
        }
        if (this.spread < this.lastSpread()) {
            this.drawArrow(g, this.geometry.next(), false);
        }
    }

    private void drawTabs(final GuiGraphics g, final int mx, final int my) {
        final String current = this.currentChapter();
        for (int i = 0; i < Math.min(this.chapters.size(), this.geometry.tabs()); i++) {
            final ManualScreenLayout.Rect tab = ManualScreenLayout.tab(this.geometry, i);
            final String chapter = this.chapters.get(i);
            final boolean on = chapter.equals(current);
            final int x = tab.x() - (on ? 2 : 0);
            g.fill(x, tab.y(), tab.x() + tab.width(), tab.y() + tab.height(), this.tabColours.get(chapter));
            Draw.outline(g, x, tab.y(), tab.x() + tab.width() - x, tab.height(), this.shadowOf(
                    this.tabColours.get(chapter)));
            final String number = String.valueOf(i + 1);
            final int textX = this.geometry.coverRight() + (tab.x() + tab.width() - this.geometry.coverRight()) / 2
                    - GuideFonts.width(this.font, number, TextSize.BOLD, this.style) / 2;
            GuideFonts.draw(g, this.font, number, on ? TextSize.BOLD : TextSize.BODY, this.style, textX,
                    tab.y() + (tab.height() - 8) / 2, this.colour(GuideStyle.TAB_INK, ""));
            if (tab.contains(mx, my)) {
                this.hoveredWords = Component.literal(this.text(GuideLibrary.loaded().chapter(chapter)
                        .map(GuideChapter::titleKey).orElse(chapter)));
            }
        }
    }

    private String currentChapter() {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        if (first >= 0 && first < this.book.pages().size()) {
            final String chapter = this.book.pages().get(first).chapter();
            if (!chapter.isEmpty()) {
                return chapter;
            }
        }
        if (this.style.spread() && first + 1 < this.book.pages().size()) {
            return this.book.pages().get(first + 1).chapter();
        }
        return "";
    }

    private void drawPage(final GuiGraphics g, final ManualScreenLayout.Rect page, final int index,
                          final boolean leftPage, final int mx, final int my) {
        g.fill(page.x(), page.y(), page.x() + page.width(), page.y() + page.height(), this.colour(GuideStyle.PAPER,
                ""));
        if (this.geometry.right() != null) {
            final int gutterX = leftPage ? page.x() + page.width() - 6 : page.x();
            g.fill(gutterX, page.y(), gutterX + (leftPage ? 6 : 5), page.y() + page.height(),
                    this.colour(GuideStyle.GUTTER, ""));
        }
        if (this.searching && leftPage) {
            this.drawSearch(g, page, mx, my);
            return;
        }
        if (index < 0 || index >= this.book.pages().size()) {
            return;
        }
        final GuideBook.Page shown = this.book.pages().get(index);
        this.drawRunning(g, page, shown, leftPage);
        for (final GuidePiece piece : shown.pieces()) {
            this.drawPiece(g, page, piece, shown.chapter(), mx, my);
        }
    }

    /** The head and the foot of a page: the chapter or the section, the folio and the manual's title. */
    private void drawRunning(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuideBook.Page shown,
                             final boolean leftPage) {
        final int margin = this.style.margin();
        final int contentX = page.x() + margin;
        final int contentWidth = page.width() - 2 * margin;
        final String head = shown.header().toUpperCase(Locale.ROOT);
        final int headWidth = GuideFonts.width(this.font, head, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, head, TextSize.SMALL, this.style, leftPage ? contentX
                : contentX + contentWidth - headWidth, page.y() + GuideLayout.HEADER_Y, this.colour(GuideStyle.FAINT,
                ""));
        final int rule = this.colour(GuideStyle.RULE, "");
        g.fill(contentX, page.y() + GuideLayout.HEADER_RULE_Y, contentX + contentWidth,
                page.y() + GuideLayout.HEADER_RULE_Y + 1, rule);
        final int footY = page.y() + page.height() - 16;
        g.fill(contentX, footY, contentX + contentWidth, footY + 1, rule);
        final int folioWidth = GuideFonts.width(this.font, shown.folio(), TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, shown.folio(), TextSize.SMALL, this.style, leftPage ? contentX
                : contentX + contentWidth - folioWidth, footY + 5, this.colour(GuideStyle.INK, ""));
        final String title = this.text(this.manual.titleKey());
        final int titleWidth = GuideFonts.width(this.font, title, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, title, TextSize.SMALL, this.style, contentX + (contentWidth - titleWidth) / 2,
                footY + 5, this.colour(GuideStyle.FAINT, ""));
    }

    private void drawPiece(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuidePiece piece,
                           final String chapter, final int mx, final int my) {
        final int x = page.x() + piece.x();
        final int y = page.y() + piece.y();
        switch (piece) {
            case GuidePiece.Text text -> {
                GuideFonts.draw(g, this.font, text.text(), text.size(), this.style, x, y,
                        this.colour(text.colour(), chapter));
                if (!text.link().isEmpty()) {
                    final int width = GuideFonts.width(this.font, text.text(), text.size(), this.style);
                    g.fill(x, y + LINK_DROP, x + width, y + LINK_DROP + 1, this.colour(GuideStyle.LINK, chapter));
                }
            }
            case GuidePiece.Leader leader -> this.drawLeader(g, x, y, leader, chapter, mx, my);
            case GuidePiece.Rule rule -> g.fill(x, y, x + rule.width(), y + 1, this.colour(rule.colour(), chapter));
            case GuidePiece.Box box -> {
                g.fill(x, y, x + box.width(), y + box.height(), this.colour(box.fill(), chapter));
                Draw.outline(g, x, y, box.width(), box.height(), this.colour(box.edge(), chapter));
            }
            case GuidePiece.Item item -> this.drawItem(g, this.stackOf(item.item()), x, y, item.scale() * 100, mx,
                    my);
            case GuidePiece.Recipes rows -> this.drawRecipes(g, x, y, rows, chapter, mx, my);
            case GuidePiece.Custom custom -> {
                final IGuideBlockRenderer renderer = GuideBlockRenderers.get(ResourceLocation.parse(custom.type()));
                if (renderer != null) {
                    renderer.draw(g, this.font, x, y, custom.width(), custom.height(), this.dataOf(custom.data()),
                            mx + this.left, my + this.top);
                }
            }
        }
    }

    private void drawLeader(final GuiGraphics g, final int x, final int y, final GuidePiece.Leader leader,
                            final String chapter, final int mx, final int my) {
        int textX = x;
        if (!leader.icon().isEmpty()) {
            this.drawItem(g, this.stackOf(leader.icon()), x, y - 2, ICON_SCALE_PERCENT, mx, my);
            textX += GuideLayout.ICON - 4;
        }
        final int colour = this.colour(leader.link().isEmpty() ? leader.colour() : GuideStyle.INK, chapter);
        GuideFonts.draw(g, this.font, leader.left(), leader.size(), this.style, textX, y, colour);
        final int leftEnd = textX + GuideFonts.width(this.font, leader.left(), leader.size(), this.style) + 3;
        final int rightWidth = GuideFonts.width(this.font, leader.right(), leader.size(), this.style);
        final int rightX = x + leader.width() - rightWidth;
        GuideFonts.draw(g, this.font, leader.right(), leader.size(), this.style, rightX, y, colour);
        final int dots = this.colour(GuideStyle.FAINT, chapter);
        for (int dot = leftEnd; dot < rightX - 2; dot += DOT_PITCH) {
            g.fill(dot, y + DOT_DROP, dot + 1, y + DOT_DROP + 1, dots);
        }
        if (!leader.link().isEmpty() && mx >= x && mx < x + leader.width() && my >= y - 1 && my < y + 9) {
            g.fill(x, y + LINK_DROP, x + leader.width(), y + LINK_DROP + 1, this.colour(GuideStyle.LINK, chapter));
        }
    }

    private void drawRecipes(final GuiGraphics g, final int x, final int y, final GuidePiece.Recipes rows,
                             final String chapter, final int mx, final int my) {
        final List<GuideRecipes.View> views = this.recipes.of(rows.type(), rows.output());
        for (int i = 0; i < rows.count() && rows.first() + i < views.size(); i++) {
            final GuideRecipes.View view = views.get(rows.first() + i);
            final int rowY = y + i * GuideLayout.RECIPE_ROW + 4;
            int slotX = x;
            for (final List<ItemStack> choices : view.inputs()) {
                this.drawSlot(g, slotX, rowY, this.cycle(choices), chapter, mx, my);
                slotX += SLOT + SLOT_GAP;
            }
            final int outputsWidth = view.outputs().size() * (SLOT + SLOT_GAP);
            final int arrowStart = slotX + 2;
            final int arrowEnd = x + rows.width() - outputsWidth - 4;
            final int ink = this.colour(GuideStyle.INK, chapter);
            final int arrowY = rowY + SLOT / 2 + 2;
            g.fill(arrowStart, arrowY, arrowEnd, arrowY + 1, ink);
            for (int head = 0; head < 4; head++) {
                g.fill(arrowEnd - head, arrowY - head, arrowEnd - head + 1, arrowY + head + 1, ink);
            }
            final String work = view.work().getString();
            if (!work.isEmpty()) {
                final int workWidth = GuideFonts.width(this.font, work, TextSize.SMALL, this.style);
                GuideFonts.draw(g, this.font, work, TextSize.SMALL, this.style,
                        arrowStart + (arrowEnd - arrowStart - workWidth) / 2, arrowY - 8,
                        this.colour(GuideStyle.FAINT, chapter));
            }
            int outX = x + rows.width() - outputsWidth;
            for (final ItemStack output : view.outputs()) {
                this.drawSlot(g, outX, rowY, output, chapter, mx, my);
                outX += SLOT + SLOT_GAP;
            }
        }
    }

    private void drawSlot(final GuiGraphics g, final int x, final int y, final ItemStack stack, final String chapter,
                          final int mx, final int my) {
        g.fill(x, y, x + SLOT, y + SLOT, this.colour(GuideStyle.SHADE, chapter));
        Draw.outline(g, x, y, SLOT, SLOT, this.colour(GuideStyle.RULE, chapter));
        this.drawItem(g, stack, x + 1, y + 1, 100, mx, my);
        if (!stack.isEmpty()) {
            g.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    private ItemStack cycle(final List<ItemStack> choices) {
        if (choices.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return choices.get((int) (Util.getMillis() / CYCLE_MILLIS % choices.size()));
    }

    private void drawItem(final GuiGraphics g, final ItemStack stack, final int x, final int y, final int percent,
                          final int mx, final int my) {
        if (stack.isEmpty()) {
            return;
        }
        final float scale = percent / 100.0F;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        final int size = Math.round(16 * scale);
        if (mx >= x && my >= y && mx < x + size && my < y + size) {
            this.hoveredItem = stack;
        }
    }

    private ItemStack stackOf(final String item) {
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)));
    }

    private CompoundTag dataOf(final String json) {
        return this.customData.computeIfAbsent(json, written -> {
            final Tag tag = new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(written))
                    .convert(NbtOps.INSTANCE).getValue();
            return tag instanceof CompoundTag compound ? compound : new CompoundTag();
        });
    }

    private void drawSearch(final GuiGraphics g, final ManualScreenLayout.Rect page, final int mx, final int my) {
        final ManualScreenLayout.Rect field = ManualScreenLayout.searchField(this.geometry, this.style.margin());
        g.fill(field.x(), field.y(), field.x() + field.width(), field.y() + field.height(),
                this.colour(GuideStyle.LABEL, ""));
        Draw.outline(g, field.x(), field.y(), field.width(), field.height(), this.colour(GuideStyle.RULE, ""));
        final String head = this.text(GuideTexts.INDEX.key()).toUpperCase(Locale.ROOT);
        final int margin = this.style.margin();
        GuideFonts.draw(g, this.font, head, TextSize.SMALL, this.style, page.x() + margin,
                page.y() + GuideLayout.HEADER_Y, this.colour(GuideStyle.FAINT, ""));
        final List<GuideBook.IndexLine> found = this.found();
        int y = field.y() + field.height() + 4;
        final String count = found.isEmpty() ? this.text(GuideTexts.FOUND_NONE.key())
                : this.text(GuideTexts.FOUND.key(), found.size());
        GuideFonts.draw(g, this.font, count, TextSize.SMALL, this.style, page.x() + margin, y,
                this.colour(GuideStyle.FAINT, ""));
        y += TextSize.SMALL.lineHeight() + 4;
        final int lineHeight = TextSize.BODY.lineHeight() + 4;
        for (final GuideBook.IndexLine line : found) {
            if (y + lineHeight > page.y() + page.height() - GuideLayout.FOOTER_ROOM) {
                break;
            }
            this.drawLeader(g, page.x() + margin, y, new GuidePiece.Leader(0, 0, page.width() - 2 * margin,
                    line.text(), line.folio(), line.term() ? TextSize.BODY : TextSize.BOLD, GuideStyle.INK,
                    line.target(), line.icon()), "", mx, my);
            y += lineHeight;
        }
    }

    /** The target of the link under the pointer, on either page or among the search's results, or empty. */
    private String linkAt(final double mx, final double my) {
        if (this.searching && this.geometry.left().contains(mx, my)) {
            return this.searchResultAt(mx, my);
        }
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        final String onLeft = this.linkOn(this.geometry.left(), first, mx, my);
        if (!onLeft.isEmpty() || this.geometry.right() == null) {
            return onLeft;
        }
        return this.linkOn(this.geometry.right(), this.rightPage(first), mx, my);
    }

    /**
     * The page on the right: the one after the left, or, while the search is open, the page of the index where the
     * first thing found stands, so the reader sees it among its neighbours.
     */
    private int rightPage(final int first) {
        if (!this.searching) {
            return first + 1;
        }
        final List<GuideBook.IndexLine> found = this.found();
        if (!found.isEmpty()) {
            final String wanted = found.getFirst().text();
            for (int page = this.book.indexAt(); page < this.book.pages().size(); page++) {
                for (final GuidePiece piece : this.book.pages().get(page).pieces()) {
                    if (piece instanceof GuidePiece.Leader leader && leader.left().equals(wanted)) {
                        return page;
                    }
                }
            }
        }
        return this.book.indexAt();
    }

    private String linkOn(final ManualScreenLayout.Rect page, final int index, final double mx, final double my) {
        if (!page.contains(mx, my) || index < 0 || index >= this.book.pages().size()) {
            return "";
        }
        final double px = mx - page.x();
        final double py = my - page.y();
        for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
            if (piece instanceof GuidePiece.Text text && !text.link().isEmpty() && py >= text.y() - 1
                    && py < text.y() + text.size().lineHeight() && px >= text.x()
                    && px < text.x() + GuideFonts.width(this.font, text.text(), text.size(), this.style)) {
                return text.link();
            }
            if (piece instanceof GuidePiece.Leader leader && !leader.link().isEmpty() && py >= leader.y() - 1
                    && py < leader.y() + leader.size().lineHeight() && px >= leader.x()
                    && px < leader.x() + leader.width()) {
                return leader.link();
            }
        }
        return "";
    }

    private String searchResultAt(final double mx, final double my) {
        final ManualScreenLayout.Rect field = ManualScreenLayout.searchField(this.geometry, this.style.margin());
        final int lineHeight = TextSize.BODY.lineHeight() + 4;
        final int first = field.y() + field.height() + 4 + TextSize.SMALL.lineHeight() + 4;
        if (my < first) {
            return "";
        }
        final int row = (int) ((my - first) / lineHeight);
        final List<GuideBook.IndexLine> found = this.found();
        return row >= 0 && row < found.size() ? found.get(row).target() : "";
    }

    private void bevel(final GuiGraphics g, final int x, final int y, final int width, final int height,
                       final String role) {
        final int fill = this.colour(role, "");
        g.fill(x, y, x + width, y + height, fill);
        final int light = this.colour(GuideStyle.COVER_EDGE, "");
        g.fill(x, y, x + width, y + 1, light);
        g.fill(x, y, x + 1, y + height, light);
        final int dark = this.shadowOf(fill);
        g.fill(x, y + height - 1, x + width, y + height, dark);
        g.fill(x + width - 1, y, x + width, y + height, dark);
    }

    private void drawRings(final GuiGraphics g) {
        final ManualScreenLayout.Rect left = this.geometry.left();
        final int from = left.x() + left.width() - RING_SPAN;
        final int to = this.geometry.right().x() + RING_SPAN;
        final int ring = this.colour(GuideStyle.RING, "");
        final int hole = this.colour(GuideStyle.FAINT, "");
        for (final int ringY : RINGS) {
            final int y = left.y() + ringY;
            g.fill(from + 2, y - 1, from + 5, y + 4, hole);
            g.fill(to - 5, y - 1, to - 2, y + 4, hole);
            g.fill(from, y, to, y + 3, ring);
            g.fill(from, y + 3, to, y + 4, this.shadowOf(ring));
        }
    }

    private void drawButton(final GuiGraphics g, final ManualScreenLayout.Rect button, final boolean search,
                            final int mx, final int my) {
        this.bevel(g, button.x(), button.y(), button.width(), button.height(), GuideStyle.COVER_EDGE);
        final int mark = this.colour(GuideStyle.LABEL, "");
        if (search) {
            final int x = button.x() + 4;
            final int y = button.y() + 1;
            g.fill(x + 1, y, x + 3, y + 1, mark);
            g.fill(x, y + 1, x + 1, y + 3, mark);
            g.fill(x + 3, y + 1, x + 4, y + 3, mark);
            g.fill(x + 1, y + 3, x + 3, y + 4, mark);
            g.fill(x + 4, y + 4, x + 5, y + 5, mark);
            g.fill(x + 5, y + 5, x + 6, y + 6, mark);
        } else {
            for (final int line : new int[] {2, 4, 6}) {
                g.fill(button.x() + 3, button.y() + line - 1, button.x() + 11, button.y() + line, mark);
            }
        }
        if (button.contains(mx, my)) {
            this.hoveredWords = GameText.component(search ? GuideTexts.TO_SEARCH : GuideTexts.CONTENTS);
        }
    }

    private void drawArrow(final GuiGraphics g, final ManualScreenLayout.Rect arrow, final boolean back) {
        final int colour = this.colour(GuideStyle.LABEL, "");
        for (int column = 0; column < arrow.width(); column++) {
            final int reach = back ? column : arrow.width() - 1 - column;
            g.fill(arrow.x() + column, arrow.y() + arrow.height() / 2 - reach, arrow.x() + column + 1,
                    arrow.y() + arrow.height() / 2 + reach + 1, colour);
        }
    }

    private void centred(final GuiGraphics g, final String text, final TextSize size, final int x, final int width,
                         final int y, final String role) {
        final int textWidth = GuideFonts.width(this.font, text, size, this.style);
        GuideFonts.draw(g, this.font, text, size, this.style, x + (width - textWidth) / 2, y, this.colour(role, ""));
    }

    private String text(final String key, final Object... args) {
        return new ClientGuideText(this.font, this.style, this.recipes).text(key, args);
    }

    /** The colour darkened, for the shaded edge of a bevel. */
    private int shadowOf(final int colour) {
        final int red = (int) ((colour >> 16 & 0xFF) * SHADOW);
        final int green = (int) ((colour >> 8 & 0xFF) * SHADOW);
        final int blue = (int) ((colour & 0xFF) * SHADOW);
        return colour >>> 24 << 24 | red << 16 | green << 8 | blue;
    }

    /**
     * The search's field: the game's own field for what is typed, drawn plain in the page's ink, with no shadow under
     * its letters, as every text of the series is drawn.
     */
    private static final class SearchField extends EditBox {

        private final Font font;
        private final int ink;

        private static final long BLINK_MILLIS = 300L;

        SearchField(final Font font, final int x, final int y, final int width, final int height, final int ink) {
            super(font, x, y, width, height, GameText.component(GuideTexts.SEARCH));
            this.font = font;
            this.ink = ink;
        }

        @Override
        public void renderWidget(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            final String shown = Texts.tail(this.font, this.getValue(), this.getWidth() - 2);
            Draw.text(g, this.font, shown, this.getX(), this.getY(), this.ink);
            if (this.isFocused() && Util.getMillis() / BLINK_MILLIS % 2 == 0) {
                final int cursorX = this.getX() + this.font.width(shown);
                g.fill(cursorX, this.getY() - 1, cursorX + 1, this.getY() + 9, this.ink);
            }
        }
    }
}
