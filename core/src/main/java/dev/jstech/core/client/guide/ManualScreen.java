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
import dev.jstech.core.guide.GuideBlock;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * An open manual: the binder or the folder in its style, one page or two at a time, with the chapter tabs at a
 * binder's edge, the contents and search buttons on its top edge and the arrows on its sides.
 *
 * <p>It opens at its cover, or at the page of an entry. Clicking a line of the contents, a number after "See", a line
 * of the index or a chapter's tab goes there; the arrows, the arrow keys, Page Up and Page Down and the mouse wheel
 * turn the pages; the search button turns the left page into a search of the index that filters as the player types.
 * Pointing at an item shows its name and what the game says of it.
 *
 * <p>A style of drawings draws its sheets as drawings are drawn: the grid, the frame with its zones, and the title
 * block in the corner in place of a page's head and foot.
 */
public final class ManualScreen extends Screen {

    private final GuideManual manual;
    private final GuideStyle style;
    private final String openAt;
    private final GuideRecipes recipes = new GuideRecipes();
    private final GuideFaces faces = new GuideFaces();
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
    /** The rivets down a closed binder's spine, and how wide the spine is. */
    private static final int[] RIVETS = {38, 106, 174};
    private static final int SPINE_WIDTH = 14;
    private static final int SLOT = 18;
    private static final int SLOT_GAP = 2;
    private static final int NAME_ROOM = 52;
    private static final int ICON_SCALE_PERCENT = 75;
    private static final int LABEL_PAD = 10;
    private static final int LABEL_WIDTH = 136;
    private static final int FOLDER_LABEL_WIDTH = 128;
    private static final int MARK = 32;
    private static final int CYCLE_MILLIS = 1000;
    private static final double SHADOW = 0.6;
    /** A drawing's grid, and its frame's zones: six across, four down. */
    private static final int GRID = 10;
    private static final int ZONES_ACROSS = 6;
    private static final int ZONES_DOWN = 4;
    /** The title block's rows and the widths of its bottom row's cells. */
    private static final int BLOCK_ROW = 10;
    private static final int BLOCK_LAST_ROW = 22;
    private static final int[] BLOCK_CELLS = {54, 62, 34};
    /** Every set of drawings is at its first revision. */
    private static final String REVISION = "A";
    private static final String SPACED_DASH = "  -  ";
    /** The balloons of a block's views: their radius, and where the first stands beside the top view. */
    private static final int BALLOON = 5;
    private static final int BALLOON_X = 80;
    private static final int BALLOON_STEP = 48;
    private static final int BALLOON_ROOM = 48;
    /*
     * The spread each manual was left open at, by its id, for as long as the game runs: a manual opened again from its
     * item opens where it was closed, as a book does.
     */
    private static final Map<String, Integer> LEFT_AT = new HashMap<>();

    public ManualScreen(final GuideManual manual, final GuideStyle style, final String openAt) {
        super(GameText.component(GuideTexts.CONTENTS));
        this.manual = manual;
        this.style = style;
        this.openAt = openAt == null ? "" : openAt;
        if (this.openAt.isEmpty()) {
            this.spread = LEFT_AT.getOrDefault(manual.id(), -1);
        }
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
                field.width() - 6, field.height() - 4, this.colour(GuideStyle.LABEL_INK, ""));
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

    /** Whether the spread shown holds that page. */
    public boolean showing(final int page) {
        return page >= 0 && this.spread == (this.style.spread() ? page / 2 : page);
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

    /* The page the manual is closed at is where it opens next time from its item. */
    @Override
    public void removed() {
        LEFT_AT.put(this.manual.id(), this.spread);
        super.removed();
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
            this.drawOpen(g, mx, my);
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
        for (int i = 0; i < this.tabCount(); i++) {
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

    /** How many chapter tabs stand at the edge: a binder's, as many as fit; a folder has none. */
    private int tabCount() {
        return this.style.cover().kind() == GuideStyle.CoverKind.BINDER
                ? Math.min(this.chapters.size(), this.geometry.tabs()) : 0;
    }

    private boolean drawing() {
        return this.style.decor().titleBlock();
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

    private int colour(final String role) {
        return this.colour(role, "");
    }

    // ---------------------------------------------------------------- the closed manual

    private void drawCover(final GuiGraphics g) {
        final ManualScreenLayout.Rect cover = ManualScreenLayout.cover(this.geometry);
        if (this.style.cover().kind() == GuideStyle.CoverKind.FOLDER) {
            this.drawFolder(g, cover);
        } else {
            this.drawClosedBinder(g, cover);
        }
    }

    /** A binder closed: its vinyl, the spine with its rivets, its tabs, and its words on a label or on itself. */
    private void drawClosedBinder(final GuiGraphics g, final ManualScreenLayout.Rect cover) {
        for (int i = 0; i < Math.min(this.chapters.size(), this.geometry.tabs()); i++) {
            final ManualScreenLayout.Rect tab = ManualScreenLayout.coverTab(this.geometry, i);
            final int colour = this.tabColours.get(this.chapters.get(i));
            this.bevel(g, tab.x() - 1, tab.y(), tab.width() + 1, tab.height(), colour, this.lighter(colour),
                    this.shadowOf(colour));
            GuideFonts.draw(g, this.font, String.valueOf(i + 1), TextSize.BODY, this.style, tab.x() + 2,
                    tab.y() + (tab.height() - 8) / 2, this.colour(GuideStyle.TAB_INK));
        }
        final int fill = this.colour(GuideStyle.COVER);
        this.bevel(g, cover.x(), 0, cover.width(), cover.height(), fill, this.colour(GuideStyle.COVER_EDGE),
                this.shadowOf(fill));
        final int grain = this.mix(fill, this.colour(GuideStyle.COVER_EDGE), 0.25);
        for (int x = cover.x() + 2; x < cover.x() + cover.width() - 2; x += 3) {
            g.fill(x, 2, x + 1, cover.height() - 2, grain);
        }
        final int spine = this.colour(GuideStyle.SPINE);
        this.bevel(g, cover.x(), 0, SPINE_WIDTH, cover.height(), spine, fill, this.shadowOf(spine));
        final int ring = this.colour(GuideStyle.RING);
        for (final int rivet : RIVETS) {
            this.bevel(g, cover.x() + 2, rivet - 2, 10, 7, this.shadowOf(ring), ring,
                    this.shadowOf(this.shadowOf(ring)));
        }
        final int faceX = cover.x() + SPINE_WIDTH;
        final int faceWidth = cover.width() - SPINE_WIDTH;
        if (this.style.cover().label()) {
            this.drawLabel(g, faceX + (faceWidth - LABEL_WIDTH) / 2, 34, LABEL_WIDTH);
        } else {
            this.drawWords(g, faceX + faceWidth / 2, 36, this.colour(GuideStyle.COVER), LABEL_WIDTH - 40,
                    faceWidth - 2 * LABEL_PAD);
        }
        if (this.style.cover().band()) {
            g.fill(cover.x(), cover.height() - 18, cover.x() + cover.width(), cover.height() - 15,
                    this.colour(GuideStyle.BAND));
        }
        if (!this.manual.partNumber().isEmpty()) {
            final String part = this.text(GuideTexts.PART_NUMBER.key(), this.manual.partNumber());
            this.centred(g, part, TextSize.SMALL, faceX, faceWidth, cover.height() - 22,
                    this.mix(fill, this.colour(GuideStyle.LABEL), 0.6));
        }
    }

    /** A folder closed: the edges of its sheets at its side, its board, its label and the elastic round it. */
    private void drawFolder(final GuiGraphics g, final ManualScreenLayout.Rect cover) {
        final int width = cover.width() - 2;
        g.fill(cover.x() + width, 12, cover.x() + width + 8, cover.height() - 18, this.colour(GuideStyle.PAPER));
        g.fill(cover.x() + width - 3, 16, cover.x() + width + 5, cover.height() - 14, this.colour(GuideStyle.GRID));
        final int fill = this.colour(GuideStyle.COVER);
        this.bevel(g, cover.x(), 0, width, cover.height(), fill, this.colour(GuideStyle.COVER_EDGE),
                this.shadowOf(fill));
        final int grain = this.mix(fill, this.colour(GuideStyle.COVER_EDGE), 0.25);
        for (int y = 3; y < cover.height() - 3; y += 4) {
            g.fill(cover.x() + 2, y, cover.x() + width - 2, y + 1, grain);
        }
        this.drawLabel(g, cover.x() + (width - FOLDER_LABEL_WIDTH) / 2, 30, FOLDER_LABEL_WIDTH);
        if (this.style.cover().band()) {
            g.fill(cover.x(), cover.height() - 48, cover.x() + width, cover.height() - 45,
                    this.colour(GuideStyle.BAND));
        }
    }

    /** The cover's label: a card with the manual's mark, its lines, its title and its edition. */
    private void drawLabel(final GuiGraphics g, final int x, final int y, final int width) {
        final int label = this.colour(GuideStyle.LABEL);
        final int titleRoom = width - LABEL_PAD;
        final int height = this.wordsHeight(titleRoom) + (this.manual.icon().isEmpty() ? 20 : 14);
        this.bevel(g, x, y, width, height, label, this.lighter(label), this.shadowOf(this.lighter(label)));
        g.fill(x, y, x + width, y + 2, this.lighter(label));
        this.drawWords(g, x + width / 2, y + (this.manual.icon().isEmpty() ? 10 : 8), label, width - 2 * LABEL_PAD,
                titleRoom);
    }

    /**
     * The words of a cover from {@code y} down, centred on {@code centre}: the mark, the first line bold in its own
     * colour and the others small, a rule, the title large, a rule and the edition. Their ground is {@code ground},
     * which the rules and the small lines are drawn a shade of; the rules are {@code room} wide, and the title wraps
     * in {@code titleRoom}.
     */
    private void drawWords(final GuiGraphics g, final int centre, final int top, final int ground, final int room,
                           final int titleRoom) {
        int y = top;
        if (!this.manual.icon().isEmpty()) {
            final ResourceLocation mark = ResourceLocation.parse(this.manual.icon()).withPrefix("textures/")
                    .withSuffix(".png");
            g.blit(mark, centre - MARK / 2, y, 0, 0, MARK, MARK, MARK, MARK);
            y += MARK + 10;
        }
        final int ink = this.colour(GuideStyle.LABEL_INK);
        final int rule = this.mix(ground, ink, 0.4);
        final int faint = this.mix(ground, ink, 0.6);
        final int x = centre - room / 2;
        boolean first = true;
        for (final String key : this.manual.coverKeys()) {
            this.centred(g, this.text(key), first ? TextSize.BOLD : TextSize.SMALL, x, room, y,
                    first ? this.colour(GuideStyle.COVER_LINE) : faint);
            y += first ? 12 : 11;
            first = false;
        }
        g.fill(x + 4, y, x + room - 4, y + 1, rule);
        y += 7;
        final float scale = this.titleScale();
        for (final String line : GuideLayout.wrap(this.text(this.manual.titleKey()), (int) (titleRoom / scale),
                TextSize.BOLD, new ClientGuideText(this.font, this.style, this.recipes))) {
            final int lineWidth = (int) (GuideFonts.width(this.font, line, TextSize.BOLD, this.style) * scale);
            g.pose().pushPose();
            g.pose().translate(centre - lineWidth / 2.0F, y, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            GuideFonts.draw(g, this.font, line, TextSize.BOLD, this.style, 0, 0, ink);
            g.pose().popPose();
            y += Math.round(10 * scale) + 1;
        }
        y += 1;
        g.fill(x + 4, y, x + room - 4, y + 1, rule);
        y += 5;
        if (!this.manual.edition().isEmpty()) {
            this.centred(g, this.text(this.manual.edition(), this.sheetCount()), TextSize.SMALL, x, room, y, faint);
        }
    }

    /** How tall the cover's words stand, the title wrapping in {@code titleRoom}. */
    private int wordsHeight(final int titleRoom) {
        final int lines = GuideLayout.wrap(this.text(this.manual.titleKey()), (int) (titleRoom / this.titleScale()),
                TextSize.BOLD, new ClientGuideText(this.font, this.style, this.recipes)).size();
        final int mark = this.manual.icon().isEmpty() ? 0 : MARK + 10;
        final int coverLines = this.manual.coverKeys().isEmpty() ? 0 : 12 + 11 * (this.manual.coverKeys().size() - 1);
        return mark + coverLines + 7 + lines * (Math.round(10 * this.titleScale()) + 1) + 1 + 5
                + TextSize.SMALL.lineHeight();
    }

    /** A binder's title is set larger than a folder's label can take. */
    private float titleScale() {
        return this.style.cover().kind() == GuideStyle.CoverKind.FOLDER ? 1.25F : 1.5F;
    }

    /** How many sheets the manual has: its pages past the front matter. */
    private int sheetCount() {
        int sheets = 0;
        for (final GuideBook.Page page : this.book.pages()) {
            if (!page.chapter().isEmpty()) {
                sheets++;
            }
        }
        return sheets;
    }

    // ---------------------------------------------------------------- the open manual

    private void drawOpen(final GuiGraphics g, final int mx, final int my) {
        final int fill = this.colour(GuideStyle.COVER);
        this.bevel(g, 0, 0, this.geometry.coverRight(), this.geometry.height(), fill,
                this.colour(GuideStyle.COVER_EDGE), this.shadowOf(fill));
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
        for (int i = 0; i < this.tabCount(); i++) {
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
                    tab.y() + (tab.height() - 8) / 2, this.colour(GuideStyle.TAB_INK));
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
        g.fill(page.x(), page.y(), page.x() + page.width(), page.y() + page.height(),
                this.colour(GuideStyle.PAPER));
        if (this.geometry.right() != null) {
            final int gutterX = leftPage ? page.x() + page.width() - 6 : page.x();
            g.fill(gutterX, page.y(), gutterX + (leftPage ? 6 : 5), page.y() + page.height(),
                    this.colour(GuideStyle.GUTTER));
        }
        if (this.style.decor().grid()) {
            this.drawGrid(g, page);
        }
        if (this.style.decor().frame()) {
            this.drawFrame(g, page);
        }
        if (this.searching && leftPage) {
            this.drawSearch(g, page, mx, my);
            return;
        }
        if (index < 0 || index >= this.book.pages().size()) {
            return;
        }
        final GuideBook.Page shown = this.book.pages().get(index);
        if (this.drawing()) {
            this.drawTitleBlock(g, page, shown);
        } else {
            this.drawRunning(g, page, shown, leftPage);
        }
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
                : contentX + contentWidth - headWidth, page.y() + GuideLayout.HEADER_Y, this.colour(GuideStyle.FAINT));
        final int rule = this.colour(GuideStyle.RULE);
        g.fill(contentX, page.y() + GuideLayout.HEADER_RULE_Y, contentX + contentWidth,
                page.y() + GuideLayout.HEADER_RULE_Y + 1, rule);
        final int footY = page.y() + page.height() - 16;
        g.fill(contentX, footY, contentX + contentWidth, footY + 1, rule);
        final int folioWidth = GuideFonts.width(this.font, shown.folio(), TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, shown.folio(), TextSize.SMALL, this.style, leftPage ? contentX
                : contentX + contentWidth - folioWidth, footY + 5, this.colour(GuideStyle.INK));
        final String title = this.text(this.manual.titleKey());
        final int titleWidth = GuideFonts.width(this.font, title, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, title, TextSize.SMALL, this.style, contentX + (contentWidth - titleWidth) / 2,
                footY + 5, this.colour(GuideStyle.FAINT));
    }

    /** A drawing's faint grid over the whole sheet. */
    private void drawGrid(final GuiGraphics g, final ManualScreenLayout.Rect page) {
        final int grid = this.colour(GuideStyle.GRID);
        for (int x = page.x() + GRID; x < page.x() + page.width(); x += GRID) {
            g.fill(x, page.y(), x + 1, page.y() + page.height(), grid);
        }
        for (int y = page.y() + GRID; y < page.y() + page.height(); y += GRID) {
            g.fill(page.x(), y, page.x() + page.width(), y + 1, grid);
        }
    }

    /** A drawing's frame, its zones numbered along the top and lettered down the side, as a reader finds a part. */
    private void drawFrame(final GuiGraphics g, final ManualScreenLayout.Rect page) {
        final int x0 = page.x() + GuideLayout.FRAME;
        final int y0 = page.y() + GuideLayout.FRAME;
        final int width = page.width() - 2 * GuideLayout.FRAME;
        final int height = page.height() - 2 * GuideLayout.FRAME;
        Draw.outline(g, x0, y0, width, height, this.colour(GuideStyle.RULE));
        final int faint = this.colour(GuideStyle.FAINT);
        for (int i = 0; i < ZONES_ACROSS; i++) {
            final int zoneX = x0 + width * i / ZONES_ACROSS;
            if (i > 0) {
                g.fill(zoneX, y0 - 3, zoneX + 1, y0, faint);
            }
            final String number = String.valueOf(i + 1);
            final int numberWidth = GuideFonts.width(this.font, number, TextSize.SMALL, this.style);
            GuideFonts.draw(g, this.font, number, TextSize.SMALL, this.style,
                    x0 + width * (2 * i + 1) / (2 * ZONES_ACROSS) - numberWidth / 2, page.y() + 1, faint);
        }
        for (int i = 0; i < ZONES_DOWN; i++) {
            final String letter = String.valueOf((char) ('A' + i));
            GuideFonts.draw(g, this.font, letter, TextSize.SMALL, this.style, page.x() + 1,
                    y0 + height * (2 * i + 1) / (2 * ZONES_DOWN) - 3, faint);
        }
    }

    /**
     * A drawing's title block in the corner of its frame: the set it belongs to, what the sheet shows, the drawing's
     * number, which sheet of how many, and its revision.
     */
    private void drawTitleBlock(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuideBook.Page shown) {
        final int x = page.x() + page.width() - GuideLayout.FRAME - GuideLayout.TITLE_BLOCK_WIDTH;
        final int y = page.y() + page.height() - GuideLayout.FRAME - GuideLayout.TITLE_BLOCK_HEIGHT;
        final int width = GuideLayout.TITLE_BLOCK_WIDTH;
        final int line = this.colour(GuideStyle.RULE);
        final int faint = this.colour(GuideStyle.FAINT);
        final int ink = this.colour(GuideStyle.INK);
        g.fill(x, y, x + width, y + GuideLayout.TITLE_BLOCK_HEIGHT, this.colour(GuideStyle.PAPER));
        Draw.outline(g, x, y, width, GuideLayout.TITLE_BLOCK_HEIGHT + 1, line);
        g.fill(x, y + BLOCK_ROW, x + width, y + BLOCK_ROW + 1, line);
        g.fill(x, y + BLOCK_LAST_ROW, x + width, y + BLOCK_LAST_ROW + 1, line);
        final String set = (this.manual.coverKeys().isEmpty() ? "" : this.text(this.manual.coverKeys().getFirst())
                + SPACED_DASH) + this.text(this.manual.titleKey());
        GuideFonts.draw(g, this.font, this.fit(set.toUpperCase(Locale.ROOT), width - 6, TextSize.SMALL),
                TextSize.SMALL, this.style, x + 3, y + 2, faint);
        GuideFonts.draw(g, this.font, this.fit(shown.title().toUpperCase(Locale.ROOT), width - 6, TextSize.BOLD),
                TextSize.BOLD, this.style, x + 3, y + BLOCK_ROW + 2, this.colour(GuideStyle.HEADING));
        final String[] labels = {this.text(GuideTexts.DRAWING.key()), this.text(GuideTexts.SHEET.key()),
                this.text(GuideTexts.REVISION.key())};
        final String[] values = {shown.folio(), this.text(GuideTexts.SHEET_OF.key(), shown.sheet(), shown.sheets()),
                REVISION};
        int cellX = x;
        for (int i = 0; i < BLOCK_CELLS.length; i++) {
            if (i > 0) {
                g.fill(cellX, y + BLOCK_LAST_ROW, cellX + 1, y + GuideLayout.TITLE_BLOCK_HEIGHT, line);
            }
            final String label = labels[i].toUpperCase(Locale.ROOT);
            GuideFonts.draw(g, this.font, label, TextSize.SMALL, this.style, cellX + 3, y + BLOCK_LAST_ROW + 2, faint);
            final int valueX = cellX + 3 + GuideFonts.width(this.font, label, TextSize.SMALL, this.style) + 3;
            final int room = (i == BLOCK_CELLS.length - 1 ? x + width : cellX + BLOCK_CELLS[i]) - valueX - 2;
            GuideFonts.draw(g, this.font, this.fit(values[i].toUpperCase(Locale.ROOT), room, TextSize.SMALL),
                    TextSize.SMALL, this.style, valueX, y + BLOCK_LAST_ROW + 2, ink);
            cellX += BLOCK_CELLS[i];
        }
    }

    private void drawPiece(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuidePiece piece,
                           final String chapter, final int mx, final int my) {
        final int x = page.x() + piece.x();
        final int y = page.y() + piece.y();
        switch (piece) {
            case GuidePiece.Text text -> this.drawText(g, x, y, text, chapter, mx, my);
            case GuidePiece.Leader leader -> this.drawLeader(g, x, y, leader, chapter, mx, my);
            case GuidePiece.Rule rule -> g.fill(x, y, x + rule.width(), y + 1, this.colour(rule.colour(), chapter));
            case GuidePiece.Dots dots -> {
                final int colour = this.colour(dots.colour(), chapter);
                for (int dot = x; dot < x + dots.width(); dot += DOT_PITCH) {
                    g.fill(dot, y, dot + 1, y + 1, colour);
                }
            }
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
            case GuidePiece.Views views -> this.drawViews(g, x, y, views, mx, my);
            case GuidePiece.Plan plan -> this.drawPlan(g, x, y, plan, mx, my);
        }
    }

    /**
     * A run of text. A link is underlined on a page and turns the accent's colour when pointed at on a drawing, where
     * an underline would read as line work.
     */
    private void drawText(final GuiGraphics g, final int x, final int y, final GuidePiece.Text text,
                          final String chapter, final int mx, final int my) {
        final int width = GuideFonts.width(this.font, text.text(), text.size(), this.style);
        final boolean pointed = !text.link().isEmpty() && mx >= x && mx < x + width && my >= y - 1
                && my < y + text.size().lineHeight();
        final String role = pointed && this.drawing() ? GuideStyle.ACCENT : text.colour();
        GuideFonts.draw(g, this.font, text.text(), text.size(), this.style, x, y, this.colour(role, chapter));
        if (!text.link().isEmpty() && !this.drawing()) {
            g.fill(x, y + LINK_DROP, x + width, y + LINK_DROP + 1, this.colour(GuideStyle.LINK, chapter));
        }
    }

    private void drawLeader(final GuiGraphics g, final int x, final int y, final GuidePiece.Leader leader,
                            final String chapter, final int mx, final int my) {
        int textX = x;
        if (!leader.icon().isEmpty()) {
            this.drawItem(g, this.stackOf(leader.icon()), x, y - 2, ICON_SCALE_PERCENT, mx, my);
            textX += GuideLayout.ICON - 4;
        }
        if (!leader.lead().isEmpty()) {
            GuideFonts.draw(g, this.font, leader.lead(), TextSize.BOLD, this.style, textX, y,
                    this.colour(GuideStyle.HEADING, chapter));
            textX += GuideFonts.width(this.font, leader.lead(), TextSize.BOLD, this.style);
        }
        final int colour = this.colour(leader.link().isEmpty() ? leader.colour() : GuideStyle.INK, chapter);
        final int rightWidth = GuideFonts.width(this.font, leader.right(), leader.size(), this.style);
        final int rightX = x + leader.width() - rightWidth;
        // A title too long for its line is cut short before the page it leads to, never written over it.
        final String left = this.fit(leader.left(), rightX - textX - 4, leader.size());
        GuideFonts.draw(g, this.font, left, leader.size(), this.style, textX, y, colour);
        final int leftEnd = textX + GuideFonts.width(this.font, left, leader.size(), this.style) + 3;
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
        final TextSize nameSize = TextSize.SMALL;
        final int row = GuideLayout.recipeRow(this.style);
        for (int i = 0; i < rows.count() && rows.first() + i < views.size(); i++) {
            final GuideRecipes.View view = views.get(rows.first() + i);
            final int rowY = y + i * row + (row - SLOT) / 2;
            int slotX = x;
            for (final List<ItemStack> choices : view.inputs()) {
                this.drawSlot(g, slotX, rowY, this.cycle(choices), chapter, mx, my);
                slotX += SLOT + SLOT_GAP;
            }
            // One output is named beside its slot, as long as the arrow keeps room enough to read.
            final boolean named = view.outputs().size() == 1 && rows.width() - slotX + x > 2 * NAME_ROOM;
            final int nameRoom = named ? NAME_ROOM : 0;
            final int outputsWidth = view.outputs().size() * (SLOT + SLOT_GAP);
            final int arrowStart = slotX + 2;
            final int arrowEnd = x + rows.width() - nameRoom - outputsWidth - 4;
            final int ink = this.colour(GuideStyle.INK, chapter);
            final int arrowY = rowY + SLOT / 2 + 2;
            g.fill(arrowStart, arrowY, arrowEnd, arrowY + 1, ink);
            for (int head = 0; head < 4; head++) {
                g.fill(arrowEnd - head, arrowY - head, arrowEnd - head + 1, arrowY + head + 1, ink);
            }
            // The time stands over the arrow and the energy under it, as a recipe card is read.
            final int faint = this.colour(GuideStyle.FAINT, chapter);
            final String time = this.fit(view.time().getString(), arrowEnd - arrowStart, TextSize.SMALL);
            final int timeWidth = GuideFonts.width(this.font, time, TextSize.SMALL, this.style);
            GuideFonts.draw(g, this.font, time, TextSize.SMALL, this.style,
                    arrowStart + (arrowEnd - arrowStart - timeWidth) / 2, arrowY - 7, faint);
            final String energy = this.fit(view.energy().getString(), arrowEnd - arrowStart, TextSize.SMALL);
            final int energyWidth = GuideFonts.width(this.font, energy, TextSize.SMALL, this.style);
            GuideFonts.draw(g, this.font, energy, TextSize.SMALL, this.style,
                    arrowStart + (arrowEnd - arrowStart - energyWidth) / 2, arrowY + 3, faint);
            int outX = x + rows.width() - nameRoom - outputsWidth;
            for (final ItemStack output : view.outputs()) {
                this.drawSlot(g, outX, rowY, output, chapter, mx, my);
                outX += SLOT + SLOT_GAP;
            }
            if (named) {
                final String name = this.fit(view.outputs().getFirst().getHoverName().getString(), nameRoom - 2,
                        nameSize);
                GuideFonts.draw(g, this.font, name, nameSize, this.style, outX + 2, rowY + (SLOT - 6) / 2, ink);
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

    /**
     * A block from above, the front and the side, each face in its outline with its name under it, the width of one
     * block under the front, and the balloons beside the top view, each with its dotted leader to what it names.
     */
    private void drawViews(final GuiGraphics g, final int x, final int y, final GuidePiece.Views views,
                           final int mx, final int my) {
        final ItemStack stack = this.stackOf(views.item());
        final int pitch = GuideLayout.VIEW_PITCH;
        this.drawFace(g, stack, GuideBlock.View.TOP, x, y, views.labels().get(0), mx, my);
        this.drawFace(g, stack, GuideBlock.View.FRONT, x, y + pitch, views.labels().get(1), mx, my);
        this.drawFace(g, stack, GuideBlock.View.SIDE, x + pitch, y + pitch, views.labels().get(2), mx, my);
        this.drawDimension(g, x, x + GuideLayout.VIEW - 1, y + 2 * pitch, views.labels().get(3));
        final int count = views.callouts().size();
        for (int i = 0; i < count; i++) {
            final GuideBlock.Callout callout = views.callouts().get(i);
            final int step = count < 2 ? 0 : Math.min(BALLOON_STEP, (views.width() - BALLOON_X - 2 * BALLOON)
                    / (count - 1));
            final int bx = x + BALLOON_X + i * step + (count < 2 ? BALLOON_STEP / 3 : 0);
            final int by = y + 10 + (i + 1) * BALLOON_ROOM / (count + 1);
            // The view a balloon points at stands where its face was drawn: above, or in the row under it.
            final int originX = callout.view() == GuideBlock.View.SIDE ? x + pitch : x;
            final int originY = callout.view() == GuideBlock.View.TOP ? y : y + pitch;
            final int scale = GuideLayout.VIEW_SCALE;
            this.drawBalloon(g, bx, by, callout.number(), originX + callout.u() * scale + 1,
                    originY + callout.v() * scale + 1);
        }
    }

    /** One face of a block in its outline, traced or as it looks, its name centred under it. */
    private void drawFace(final GuiGraphics g, final ItemStack stack, final GuideBlock.View view, final int x,
                          final int y, final String label, final int mx, final int my) {
        final int scale = GuideLayout.VIEW_SCALE;
        final TextureAtlasSprite sprite = this.faces.face(stack, view);
        if (sprite != null) {
            this.faces.draw(g, sprite, x, y, scale, this.style.decor().traced(), this.colour(GuideStyle.INK),
                    this.colour(GuideStyle.RULE));
        } else {
            Draw.outline(g, x, y, GuideLayout.VIEW, GuideLayout.VIEW, this.colour(GuideStyle.RULE));
            this.drawItem(g, stack, x + 8, y + 8, 200, mx, my);
        }
        if (mx >= x && my >= y && mx < x + GuideLayout.VIEW && my < y + GuideLayout.VIEW) {
            this.hoveredItem = stack;
        }
        // A name wider than its face is cut short rather than run into the next face's.
        final String name = this.fit(label, GuideLayout.VIEW - 1, TextSize.SMALL);
        final int labelWidth = GuideFonts.width(this.font, name, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, name, TextSize.SMALL, this.style, x + 8 * scale - labelWidth / 2,
                y + GuideLayout.VIEW + 3, this.colour(GuideStyle.FAINT));
    }

    /** A dimension line with its arrowheads and extension lines, the size written over its middle. */
    private void drawDimension(final GuiGraphics g, final int x0, final int x1, final int y, final String size) {
        final int faint = this.colour(GuideStyle.FAINT);
        g.fill(x0, y - 6, x0 + 1, y + 2, faint);
        g.fill(x1, y - 6, x1 + 1, y + 2, faint);
        g.fill(x0, y, x1, y + 1, faint);
        for (int i = 0; i < 3; i++) {
            g.fill(x0 + 1 + i, y - i, x0 + 2 + i, y + 1 + i, faint);
            g.fill(x1 - 1 - i, y - i, x1 - i, y + 1 + i, faint);
        }
        final int width = GuideFonts.width(this.font, size, TextSize.SMALL, this.style);
        final int mid = (x0 + x1) / 2 - width / 2;
        g.fill(mid - 2, y - 3, mid + width + 2, y + 4, this.colour(GuideStyle.PAPER));
        GuideFonts.draw(g, this.font, size, TextSize.SMALL, this.style, mid, y - 3, faint);
    }

    /** A numbered balloon, and its dotted leader to the place it names. */
    private void drawBalloon(final GuiGraphics g, final int x, final int y, final int number, final int toX,
                             final int toY) {
        final int faint = this.colour(GuideStyle.FAINT);
        final int steps = Math.max(Math.abs(toX - x), Math.abs(toY - y));
        for (int i = BALLOON + 1; i < steps; i += 2) {
            final int px = x + (toX - x) * i / steps;
            final int py = y + (toY - y) * i / steps;
            g.fill(px, py, px + 1, py + 1, faint);
        }
        final int ink = this.colour(GuideStyle.INK);
        g.fill(x - BALLOON + 1, y - BALLOON + 1, x + BALLOON, y + BALLOON, this.colour(GuideStyle.PAPER));
        for (int angle = 0; angle < 360; angle += 12) {
            final double radians = Math.toRadians(angle);
            final int px = (int) Math.round(x + BALLOON * Math.cos(radians));
            final int py = (int) Math.round(y + BALLOON * Math.sin(radians));
            g.fill(px, py, px + 1, py + 1, ink);
        }
        final String written = String.valueOf(number);
        final int width = GuideFonts.width(this.font, written, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, written, TextSize.SMALL, this.style, x - width / 2 + 1, y - 2, ink);
    }

    /** Blocks seen from above side by side, each named under it, an optional place outlined in dots. */
    private void drawPlan(final GuiGraphics g, final int x, final int y, final GuidePiece.Plan plan, final int mx,
                          final int my) {
        final int cell = GuideLayout.VIEW;
        int at = x + 2;
        for (final GuidePiece.PlanPart part : plan.parts()) {
            if (part.optional()) {
                final int faint = this.colour(GuideStyle.FAINT);
                for (int i = 0; i < cell; i += 3) {
                    g.fill(at + i, y, at + i + 1, y + 1, faint);
                    g.fill(at + i, y + cell - 1, at + i + 1, y + cell, faint);
                    g.fill(at + cell - 1, y + i, at + cell, y + i + 1, faint);
                }
                int lineY = y + 14;
                for (final String line : GuideLayout.wrap(part.label(), cell - 6, TextSize.SMALL,
                        new ClientGuideText(this.font, this.style, this.recipes))) {
                    GuideFonts.draw(g, this.font, line, TextSize.SMALL, this.style, at + 4, lineY, faint);
                    lineY += TextSize.SMALL.lineHeight();
                }
            } else {
                this.drawFace(g, this.stackOf(part.item()), GuideBlock.View.TOP, at, y, part.label(), mx, my);
            }
            at += cell;
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
                this.colour(GuideStyle.LABEL));
        Draw.outline(g, field.x(), field.y(), field.width(), field.height(), this.colour(GuideStyle.RULE));
        final String head = this.text(GuideTexts.INDEX.key()).toUpperCase(Locale.ROOT);
        final int margin = this.style.margin();
        GuideFonts.draw(g, this.font, head, TextSize.SMALL, this.style, page.x() + margin,
                page.y() + GuideLayout.HEADER_Y, this.colour(GuideStyle.FAINT));
        final List<GuideBook.IndexLine> found = this.found();
        int y = field.y() + field.height() + 4;
        final String count = found.isEmpty() ? this.text(GuideTexts.FOUND_NONE.key())
                : this.text(GuideTexts.FOUND.key(), found.size());
        GuideFonts.draw(g, this.font, count, TextSize.SMALL, this.style, page.x() + margin, y,
                this.colour(GuideStyle.FAINT));
        y += TextSize.SMALL.lineHeight() + 4;
        final int lineHeight = TextSize.BODY.lineHeight() + 4;
        final int bottom = page.y() + GuideLayout.bottom(this.style, this.style.pages().columns() - 1);
        for (final GuideBook.IndexLine line : found) {
            if (y + lineHeight > bottom) {
                break;
            }
            this.drawLeader(g, page.x() + margin, y, new GuidePiece.Leader(0, 0, page.width() - 2 * margin, "",
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
                       final int fill, final int light, final int dark) {
        g.fill(x, y, x + width, y + height, fill);
        g.fill(x, y, x + width, y + 1, light);
        g.fill(x, y, x + 1, y + height, light);
        g.fill(x, y + height - 1, x + width, y + height, dark);
        g.fill(x + width - 1, y, x + width, y + height, dark);
    }

    private void drawRings(final GuiGraphics g) {
        final ManualScreenLayout.Rect left = this.geometry.left();
        final int from = left.x() + left.width() - RING_SPAN;
        final int to = this.geometry.right().x() + RING_SPAN;
        final int ring = this.colour(GuideStyle.RING);
        final int hole = this.colour(GuideStyle.FAINT);
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
        final int fill = this.colour(GuideStyle.COVER_EDGE);
        this.bevel(g, button.x(), button.y(), button.width(), button.height(), fill, this.lighter(fill),
                this.shadowOf(fill));
        final int mark = this.colour(GuideStyle.LABEL);
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
        final int colour = this.colour(GuideStyle.LABEL);
        for (int column = 0; column < arrow.width(); column++) {
            final int reach = back ? column : arrow.width() - 1 - column;
            g.fill(arrow.x() + column, arrow.y() + arrow.height() / 2 - reach, arrow.x() + column + 1,
                    arrow.y() + arrow.height() / 2 + reach + 1, colour);
        }
    }

    private void centred(final GuiGraphics g, final String text, final TextSize size, final int x, final int width,
                         final int y, final int colour) {
        final int textWidth = GuideFonts.width(this.font, text, size, this.style);
        GuideFonts.draw(g, this.font, text, size, this.style, x + (width - textWidth) / 2, y, colour);
    }

    /** The words as they fit the room, cut short with an ellipsis when they do not. */
    private String fit(final String words, final int room, final TextSize size) {
        if (GuideFonts.width(this.font, words, size, this.style) <= room) {
            return words;
        }
        String cut = words;
        while (!cut.isEmpty() && GuideFonts.width(this.font, cut + "...", size, this.style) > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + "...";
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

    /** The colour lightened, for the lit edge of a bevel. */
    private int lighter(final int colour) {
        final int red = colour >> 16 & 0xFF;
        final int green = colour >> 8 & 0xFF;
        final int blue = colour & 0xFF;
        return colour >>> 24 << 24 | (red + (255 - red) * 2 / 5) << 16 | (green + (255 - green) * 2 / 5) << 8
                | (blue + (255 - blue) * 2 / 5);
    }

    /** The colour {@code share} of the way from {@code from} to {@code to}. */
    private int mix(final int from, final int to, final double share) {
        final int red = (int) Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * share);
        final int green = (int) Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * share);
        final int blue = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * share);
        return from >>> 24 << 24 | red << 16 | green << 8 | blue;
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
