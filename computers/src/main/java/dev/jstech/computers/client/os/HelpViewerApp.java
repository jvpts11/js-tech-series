/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.help.HelpForm;
import dev.jstech.computers.gui.help.HelpTarget;
import dev.jstech.computers.gui.help.HelpTexts;
import dev.jstech.computers.gui.help.HelpTree;
import dev.jstech.computers.gui.help.HelpWindowTexts;
import dev.jstech.computers.gui.layout.HelpViewerLayout;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.MenuBar;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Help, in the form each desktop had it: Frames 95's Help Topics and its topic window, XP's Help and Support Center,
 * 7's Help and Support, the Get Help of 10 and 11, KDE's Help Center, GNOME's and Cinnamon's Help, and CDE's Help
 * Viewer.
 *
 * <p>Every form holds the same things. The manuals of the player's game, the series' Technical Reference first, are
 * books in a tree of chapters, sections and entries, each entry the page the binder prints; beside them the machine's
 * commands are a book of their own, each command's page the very one {@code man} prints at a terminal. A search finds
 * both, and a page's links lead on to others with Back to come home.
 */
public final class HelpViewerApp implements IDesktopApp {

    private final BlockPos host;
    private final HelpForm form;
    private HelpSession session;
    private OsSkin skin = OsSkin.fallback();
    private int left;
    private int top;
    private int width;
    private int height;
    private int mouseX;
    private int mouseY;
    private int treeScroll;
    private int pageScroll;
    @Nullable
    private HelpPageView.Laid laid;
    private String laidFor = "";
    /** What is typed in the search field, and whether the field has the keyboard. */
    private final StringBuilder typed = new StringBuilder();
    private boolean typing;
    /** Frames 95's tab on show (Contents, Index or Find) and the row picked in it, which Display opens. */
    private int tab;
    private String picked = "";
    private String lastClick = "";
    private long lastClickAt;
    /** GNOME's search, which its header shows only while it is open. */
    private boolean yelpSearch;
    /** The row last brought into view, so a page newly shown is brought in once and the wheel is left alone. */
    private String revealed = "";
    private final MenuBar menuBar = new MenuBar(MENU_ITEM_W, MENU_ITEM_H);

    /** The key Frames 95's topic window is opened and remembered under, which its window factory is registered at. */
    public static final String TOPIC_KEY = JsComputers.MODID + ":help_viewer/topic";
    /** The key the help window itself is opened under. */
    public static final String KEY = JsComputers.MODID + ":help_viewer";

    private static final int CONTENTS_TAB = 0;
    private static final int INDEX_TAB = 1;
    private static final int FIND_TAB = 2;
    private static final int DOUBLE_CLICK_MS = 400;
    private static final int MENU_ITEM_W = 110;
    private static final int MENU_ITEM_H = 11;
    private static final int LINE = 10;
    private static final int WHEEL = 30;
    private static final int ICON_W = 8;

    public HelpViewerApp(final BlockPos host, final HelpForm form) {
        this.host = host;
        this.form = form;
        this.session = HelpSession.open(host);
        if (form == HelpForm.CDE) {
            this.menuBar.add(GameText.resolve(HelpWindowTexts.CDE_FILE), this::fileMenu)
                    .add(GameText.resolve(HelpWindowTexts.CDE_EDIT), this::editMenu)
                    .add(GameText.resolve(HelpWindowTexts.CDE_SEARCH), this::searchMenu)
                    .add(GameText.resolve(HelpWindowTexts.CDE_NAVIGATE), this::navigateMenu)
                    .add(GameText.resolve(HelpWindowTexts.CDE_HELP), this::helpMenu);
        }
    }

    /** The form help takes on the desktop of that id. */
    public static HelpForm formOf(@Nullable final ResourceLocation desktopId) {
        final DesktopEnvironmentDef desktop = desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        if (desktop == null) {
            return HelpForm.GET_HELP;
        }
        return switch (desktop.panelStyle()) {
            case FRAMES_95 -> HelpForm.FRAMES_95;
            case FRAMES_XP -> HelpForm.FRAMES_XP;
            case FRAMES_7 -> HelpForm.FRAMES_7;
            case FRAMES_10, FRAMES_11 -> HelpForm.GET_HELP;
            case KDE -> HelpForm.KDE;
            case GNOME, CINNAMON -> HelpForm.YELP;
            case CDE -> HelpForm.CDE;
        };
    }

    /* What a test reads and clicks */

    /** The form this window has. */
    public HelpForm form() {
        return this.form;
    }

    /** The page shown, as the link to it is written. */
    public String shownPage() {
        return this.session.shown().written();
    }

    /** Every piece of text the page shows, in order. */
    public List<String> pageWords() {
        return this.laid == null ? List.of() : this.laid.words();
    }

    /** Every link of the page, as written into it. */
    public List<String> pageLinks() {
        return this.laid == null ? List.of() : this.laid.links();
    }

    /** The rows of the tree as they read, in order: Frames 95's index or found topics on those tabs. */
    public List<String> treeRows() {
        final List<String> out = new ArrayList<>();
        for (final HelpTree.Row row : this.shownRows()) {
            out.add(row.label());
        }
        return out;
    }

    /** Where a row of the tree is drawn, on the desktop, or null when it is out of sight. */
    @Nullable
    public int[] rowCentre(final String label) {
        final HelpViewerLayout.Box box = this.listed() ? this.listBox() : this.treeBox();
        if (box == null) {
            return null;
        }
        final List<HelpTree.Row> rows = this.shownRows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).label().equals(label)) {
                final int y = box.y() + 2 + (i - this.treeScroll) * HelpViewerLayout.ROW_H;
                if (y < box.y() || y + HelpViewerLayout.ROW_H > box.y() + box.h()) {
                    return null;
                }
                return new int[] {this.left + box.x() + 20, this.top + y + HelpViewerLayout.ROW_H / 2};
            }
        }
        return null;
    }

    /** Where a link of the page is drawn, on the desktop, or null when it is out of sight. */
    @Nullable
    public int[] linkCentre(final String link) {
        if (this.laid == null) {
            return null;
        }
        final HelpViewerLayout.Box page = this.pageBox();
        for (final HelpPageView.Op op : this.laid.ops()) {
            if (op instanceof HelpPageView.Text text && text.link().equals(link)) {
                final int y = page.y() + text.y() - this.pageScroll;
                if (y < page.y() || y + LINE > page.y() + page.h()) {
                    return null;
                }
                return new int[] {this.left + page.x() + text.x() + Math.max(1, text.w() / 2), this.top + y + 4};
            }
        }
        return null;
    }

    /** Where one of the window's buttons is, on the desktop. */
    public int[] buttonCentre(final int index) {
        final HelpViewerLayout.Box box = this.button(index).box();
        return new int[] {this.left + box.x() + box.w() / 2, this.top + box.y() + box.h() / 2};
    }

    /** Where one of Frames 95's tabs is, on the desktop. */
    public int[] tabCentre(final int index) {
        final HelpViewerLayout.Box box = this.frame().tabs().get(index).box();
        return new int[] {this.left + box.x() + box.w() / 2, this.top + box.y() + box.h() / 2};
    }

    /** Where the search field is, on the desktop, or null for a form showing none. */
    @Nullable
    public int[] searchCentre() {
        final HelpViewerLayout.Box box = this.searchBox();
        return box == null ? null : new int[] {this.left + box.x() + box.w() / 2, this.top + box.y() + box.h() / 2};
    }

    /* The window */

    @Override
    public String title() {
        return GameText.resolve(switch (this.form) {
            case FRAMES_95 -> HelpWindowTexts.TOPICS_TITLE.text();
            case FRAMES_95_TOPIC -> HelpWindowTexts.TOPIC_TITLE.text();
            case FRAMES_XP -> HelpWindowTexts.XP_TITLE.text();
            case FRAMES_7 -> HelpWindowTexts.SEVEN_TITLE.text();
            case GET_HELP -> HelpWindowTexts.GET_HELP_TITLE.text();
            case KDE -> HelpWindowTexts.KDE_PAGE_TITLE.with(HelpPageView.label(this.session.shown()));
            case YELP -> HelpWindowTexts.YELP_TITLE.text();
            case CDE -> HelpWindowTexts.CDE_TITLE.text();
        });
    }

    @Override
    public int defaultWidth() {
        return HelpViewerLayout.of(this.form).width();
    }

    @Override
    public int defaultHeight() {
        return HelpViewerLayout.of(this.form).height();
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onRestored() {
        this.session.source().refresh();
    }

    @Override
    public void onClosed() {
        HelpSession.closed(this.host);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mx, final int my, final float partialTick) {
        this.left = x;
        this.top = y;
        this.width = w;
        this.height = h;
        this.mouseX = mx;
        this.mouseY = my;
        final HelpViewerPalette.Colours colours = HelpViewerPalette.of(this.form);
        final boolean skinned = this.form == HelpForm.FRAMES_95 || this.form == HelpForm.FRAMES_95_TOPIC
                || this.form == HelpForm.CDE;
        g.fill(x, y, x + w, y + h, skinned ? this.skin.windowBg() : colours.ground());
        switch (this.form) {
            case FRAMES_95 -> this.renderTopics(g, font, colours);
            case FRAMES_95_TOPIC -> this.renderTopicWindow(g, font, colours);
            case FRAMES_XP -> this.renderXp(g, font, colours);
            case FRAMES_7 -> this.renderSeven(g, font, colours);
            case GET_HELP -> this.renderGetHelp(g, font, colours);
            case KDE -> this.renderKde(g, font, colours);
            case YELP -> this.renderYelp(g, font, colours);
            case CDE -> this.renderCde(g, font, colours, mx, my, partialTick);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mx, final double my, final int button) {
        final double rx = mx - this.left;
        final double ry = my - this.top;
        if (this.form == HelpForm.CDE && (this.menuBar.isOpen() || this.menuBar.titleAt(mx, my) >= 0)) {
            this.menuBar.mouseClicked(mx, my, button);
            return;
        }
        if (button != 0) {
            return;
        }
        final HelpViewerLayout.Box search = this.searchBox();
        this.typing = search != null && search.holds(rx, ry);
        if (this.typing) {
            return;
        }
        final List<HelpViewerLayout.Button> buttons = this.frame().buttons();
        for (int i = 0; i < buttons.size(); i++) {
            if (this.button(i).box().holds(rx, ry)) {
                this.press(i);
                return;
            }
        }
        final List<HelpViewerLayout.Button> tabs = this.frame().tabs();
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).box().holds(rx, ry)) {
                this.tab = i;
                this.typed.setLength(0);
                this.picked = "";
                this.treeScroll = 0;
                return;
            }
        }
        final HelpViewerLayout.Box tree = this.listed() ? this.listBox() : this.treeBox();
        if (tree != null && tree.holds(rx, ry)) {
            this.clickTree((int) ry - tree.y() - 2);
            return;
        }
        if (this.form == HelpForm.YELP && this.trailClicked(rx, ry)) {
            return;
        }
        final HelpViewerLayout.Box page = this.pageBox();
        if (this.form != HelpForm.FRAMES_95 && page.holds(rx, ry) && this.laid != null) {
            final String link = this.laid.linkAt(rx - page.x(), ry - page.y() + this.pageScroll);
            if (HelpPageView.FAVORITE.equals(link)) {
                this.session.addFavorite();
            } else if (link != null) {
                HelpTarget.read(link).ifPresent(this::open);
            }
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.form == HelpForm.CDE && this.menuBar.isOpen()) {
            return this.menuBar.keyPressed(key, scanCode, modifiers);
        }
        final boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        if (alt && key == GLFW.GLFW_KEY_LEFT) {
            this.back();
            return true;
        }
        if (alt && key == GLFW.GLFW_KEY_RIGHT) {
            this.session.forward();
            this.pageScroll = 0;
            return true;
        }
        if (this.typing) {
            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (!this.typed.isEmpty()) {
                        this.typed.setLength(this.typed.length() - 1);
                    }
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> this.runSearch();
                case GLFW.GLFW_KEY_ESCAPE -> this.typing = false;
                default -> {
                    return false;
                }
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> this.pageScroll += this.pageBox().h() - LINE;
            case GLFW.GLFW_KEY_PAGE_UP -> this.pageScroll = Math.max(0, this.pageScroll - this.pageBox().h() + LINE);
            case GLFW.GLFW_KEY_BACKSPACE -> this.back();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (this.form == HelpForm.FRAMES_95) {
                    this.display();
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        if (!this.typing || c < ' ' || c == 127) {
            return false;
        }
        this.typed.append(c);
        if (this.form == HelpForm.FRAMES_95) {
            this.treeScroll = 0;
            this.picked = "";
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final HelpViewerLayout.Box tree = this.listed() ? this.listBox() : this.treeBox();
        final int steps = (int) Math.signum(delta);
        if (tree != null && tree.holds(this.mouseX - this.left, this.mouseY - this.top)) {
            this.treeScroll = Math.max(0, this.treeScroll - steps * 3);
        } else {
            this.pageScroll = Math.max(0, this.pageScroll - steps * WHEEL);
        }
        return true;
    }

    /** Follows a link or a row of the tree: the page shown, the window kept where the tree shows it. */
    void open(final HelpTarget target) {
        this.session.go(target);
        this.pageScroll = 0;
    }

    /* The forms */

    /** Frames 95's Help Topics: three tabs over a sheet, the tree or a list on it, and the buttons under it. */
    private void renderTopics(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours) {
        final HelpViewerLayout.Frame frame = this.frame();
        final int dh = this.height - frame.height();
        this.skin.panel(g, this.left + 4, this.top + 16, this.width - 8, this.height - 16 - 26);
        for (int i = 0; i < frame.tabs().size(); i++) {
            final HelpViewerLayout.Box box = frame.tabs().get(i).box();
            this.skin.tab(g, font, this.left + box.x(), this.top + box.y(), box.w(), box.h(),
                    GameText.resolve(frame.tabs().get(i).label()), i == this.tab);
        }
        final TextKey hint = this.tab == CONTENTS_TAB ? HelpWindowTexts.HINT_CONTENTS
                : this.tab == INDEX_TAB ? HelpWindowTexts.HINT_INDEX : HelpWindowTexts.HINT_FIND;
        // The hint takes its lines over the tree, the last of them cut short when a language needs more.
        final List<String> hintLines = this.wrapped(font, GameText.resolve(hint), this.width - 20);
        for (int i = 0; i < Math.min(HelpViewerLayout.HINT_LINES, hintLines.size()); i++) {
            final boolean more = i == HelpViewerLayout.HINT_LINES - 1 && hintLines.size() > HelpViewerLayout.HINT_LINES;
            Draw.text(g, font, more ? this.fit(font, hintLines.get(i) + " " + hintLines.get(i + 1), this.width - 20)
                    : hintLines.get(i), this.left + 10, this.top + HelpViewerLayout.HINT_Y + i
                    * HelpViewerLayout.HINT_PITCH, this.skin.text());
        }
        if (this.tab == CONTENTS_TAB) {
            final HelpViewerLayout.Box tree = this.treeBox();
            this.skin.field(g, this.left + tree.x(), this.top + tree.y(), tree.w(), tree.h(), false);
            this.drawRows(g, font, colours, tree, this.rows(), this.picked);
        } else {
            final HelpViewerLayout.Box field = this.searchBox();
            this.field(g, font, field, "", colours);
            final HelpViewerLayout.Box list = this.listBox();
            this.skin.field(g, this.left + list.x(), this.top + list.y(), list.w(), list.h(), false);
            this.drawRows(g, font, colours, list, this.listRows(), this.picked);
        }
        final HelpTree.Row row = this.pickedRow();
        for (int i = 0; i < frame.buttons().size(); i++) {
            final HelpViewerLayout.Box box = frame.buttons().get(i).box();
            TextKey label = frame.buttons().get(i).label();
            if (i == 0 && row != null && row.book()) {
                label = this.session.isOpen(row.key()) ? HelpWindowTexts.CLOSE : HelpWindowTexts.OPEN;
            }
            this.pushButton(g, font, box.x(), box.y() + dh, box.w(), box.h(), label, i != 1, i == 0);
        }
    }

    /** Frames 95's topic window: Help Topics, Back and Options over the topic on its cream page. */
    private void renderTopicWindow(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours) {
        final HelpViewerLayout.Frame frame = this.frame();
        for (int i = 0; i < frame.buttons().size(); i++) {
            final HelpViewerLayout.Box box = frame.buttons().get(i).box();
            this.pushButton(g, font, box.x(), box.y(), box.w(), box.h(), frame.buttons().get(i).label(),
                    i == 0 || (i == 1 && this.session.canGoBack()), false);
        }
        this.drawPage(g, font, colours, this.pageBox(), 1.0f, true);
    }

    /** XP's Help and Support Center: the blue band, the row of buttons, the lilac pane and the page. */
    private void renderXp(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours) {
        final HelpViewerLayout.Frame frame = this.frame();
        g.fill(this.left, this.top, this.left + this.width, this.top + frame.band().h(), colours.band());
        this.scaled(g, font, GameText.resolve(HelpWindowTexts.XP_TITLE), 8, 6, 1.25f, colours.bandInk(), true);
        this.field(g, font, this.searchBox(), GameText.resolve(HelpWindowTexts.SEARCH), colours);
        g.fill(this.left, this.top + 35, this.left + this.width, this.top + 36, colours.rule());
        final boolean[] on = {this.session.canGoBack(), this.session.canGoForward(), true, true, true, true, false};
        for (int i = 0; i < frame.buttons().size(); i++) {
            final HelpViewerLayout.Box box = frame.buttons().get(i).box();
            final int ink = on[i] ? colours.ink() : colours.faint();
            if (i == 0) {
                this.arrow(g, box.x() + 2, box.y() + 3, true, ink);
                Draw.text(g, font, GameText.resolve(HelpWindowTexts.BACK), this.left + box.x() + 10,
                        this.top + box.y() + 2, ink);
            } else if (i == 1) {
                this.arrow(g, box.x() + 3, box.y() + 3, false, ink);
            } else {
                Draw.text(g, font, GameText.resolve(frame.buttons().get(i).label()), this.left + box.x() + 2,
                        this.top + box.y() + 2, ink);
            }
        }
        final HelpViewerLayout.Box tree = this.treeBox();
        g.fill(this.left, this.top + 36, this.left + tree.w(), this.top + this.height, colours.pane());
        final String pane = this.fit(font, GameText.resolve(HelpWindowTexts.XP_PANE), tree.w() - 16);
        Draw.text(g, font, Component.literal(pane).withStyle(ChatFormatting.BOLD), this.left + 6, this.top + 40,
                colours.title());
        this.drawRows(g, font, colours, tree, this.rows(), this.session.shown().written());
        final HelpViewerLayout.Box page = this.pageBox();
        g.fill(this.left + page.x() - 1, this.top + page.y(), this.left + page.x(), this.top + this.height,
                colours.rule());
        this.drawPage(g, font, colours, page, 1.25f, false);
    }

    /** 7's Help and Support: the arrows and the search over a bordered tree and the page. */
    private void renderSeven(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours) {
        final HelpViewerLayout.Frame frame = this.frame();
        g.fill(this.left, this.top, this.left + this.width, this.top + frame.band().h(), colours.band());
        g.fill(this.left, this.top + frame.band().h() - 1, this.left + this.width, this.top + frame.band().h(),
                colours.rule());
        this.arrow(g, this.button(0).box().x() + 3, this.button(0).box().y() + 3, true,
                this.session.canGoBack() ? colours.title() : colours.faint());
        this.arrow(g, this.button(1).box().x() + 3, this.button(1).box().y() + 3, false,
                this.session.canGoForward() ? colours.title() : colours.faint());
        this.field(g, font, this.searchBox(), GameText.resolve(HelpWindowTexts.SEARCH_HELP), colours);
        final HelpViewerLayout.Box browse = this.button(2).box();
        Draw.text(g, font, GameText.resolve(HelpWindowTexts.BROWSE_HELP), this.left + browse.x() + 2,
                this.top + browse.y() + 2, colours.ink());
        final HelpViewerLayout.Box tree = this.treeBox();
        g.fill(this.left + tree.x(), this.top + tree.y(), this.left + tree.x() + tree.w(),
                this.top + tree.y() + tree.h(), colours.pane());
        Draw.outline(g, this.left + tree.x(), this.top + tree.y(), tree.w(), tree.h(), colours.rule());
        this.drawRows(g, font, colours, tree, this.rows(), this.session.shown().written());
        this.drawPage(g, font, colours, this.pageBox(), 1.25f, true);
    }

    /** 10's and 11's Get Help: one search across the top, the tree, and the page on a card. */
    private void renderGetHelp(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours) {
        final HelpViewerLayout.Box search = this.searchBox();
        this.field(g, font, search, GameText.resolve(HelpWindowTexts.SEARCH_MANUALS), colours);
        g.fill(this.left + search.x(), this.top + search.y() + search.h() - 1, this.left + search.x() + search.w(),
                this.top + search.y() + search.h(), colours.link());
        this.drawRows(g, font, colours, this.treeBox(), this.rows(), this.session.shown().written());
        this.drawPage(g, font, colours, this.pageBox(), 1.5f, true);
    }

    /** KDE's Help Center: the tree in its grey pane and the page beside it. */
    private void renderKde(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours) {
        final HelpViewerLayout.Box tree = this.treeBox();
        g.fill(this.left, this.top, this.left + tree.w(), this.top + this.height, colours.pane());
        this.drawRows(g, font, colours, tree, this.rows(), this.session.shown().written());
        final HelpViewerLayout.Box page = this.pageBox();
        g.fill(this.left + page.x() - 1, this.top, this.left + page.x(), this.top + this.height, colours.rule());
        this.drawPage(g, font, colours, page, 1.5f, false);
    }

    /** GNOME's Help: Back and the search in the header, the trail of where the page is, and the page. */
    private void renderYelp(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours) {
        final HelpViewerLayout.Frame frame = this.frame();
        g.fill(this.left, this.top, this.left + this.width, this.top + frame.band().h(), colours.band());
        g.fill(this.left, this.top + frame.band().h() - 1, this.left + this.width, this.top + frame.band().h(),
                colours.rule());
        this.arrow(g, this.button(0).box().x() + 3, this.button(0).box().y() + 2, true,
                this.session.canGoBack() ? colours.ink() : colours.faint());
        final HelpViewerLayout.Box lens = this.button(1).box();
        Draw.outline(g, this.left + lens.x() + 2, this.top + lens.y() + 1, 6, 6, colours.ink());
        g.fill(this.left + lens.x() + 7, this.top + lens.y() + 7, this.left + lens.x() + 10,
                this.top + lens.y() + 10, colours.ink());
        if (this.yelpSearch) {
            this.field(g, font, this.searchBox(), GameText.resolve(HelpWindowTexts.SEARCH), colours);
        }
        final HelpViewerLayout.Box trail = this.frame().trail();
        int x = this.left + trail.x();
        for (final Crumb crumb : this.trail()) {
            if (x > this.left + trail.x()) {
                Draw.text(g, font, " › ", x, this.top + trail.y(), colours.faint());
                x += font.width(" › ");
            }
            Draw.text(g, font, crumb.label(), x, this.top + trail.y(), colours.link());
            x += font.width(crumb.label());
        }
        this.drawPage(g, font, colours, this.pageBox(), 1.5f, false);
    }

    /** CDE's Help Viewer: the menu bar, the topic hierarchy, the page and the four buttons. */
    private void renderCde(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours,
                           final int mx, final int my, final float partialTick) {
        final HelpViewerLayout.Frame frame = this.frame();
        final int dh = this.height - frame.height();
        final HelpViewerLayout.Box tree = this.treeBox();
        this.skin.field(g, this.left + tree.x(), this.top + tree.y(), tree.w(), tree.h(), false);
        this.drawRows(g, font, colours, tree, this.rows(), this.session.shown().written());
        final HelpViewerLayout.Box page = this.pageBox();
        this.skin.field(g, this.left + page.x(), this.top + page.y(), page.w(), page.h(), false);
        this.drawPage(g, font, colours, page, 1.25f, false);
        for (int i = 0; i < frame.buttons().size(); i++) {
            final HelpViewerLayout.Box box = frame.buttons().get(i).box();
            this.pushButton(g, font, box.x(), box.y() + dh, box.w(), box.h(), frame.buttons().get(i).label(),
                    i != 0 || this.session.canGoBack(), false);
        }
        this.menuBar.setBounds(this.left, this.top, this.width, MenuBar.HEIGHT);
        this.menuBar.setWindow(this.left, this.top, this.width, this.height);
        this.menuBar.render(g, new UiContext(this.skin, font, mx, my, partialTick));
    }

    /* Drawing */

    /** The rows of a tree or a list in a well, scrolled, the row picked lit in the form's colours. */
    private void drawRows(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours,
                          final HelpViewerLayout.Box box, final List<HelpTree.Row> rows, final String lit) {
        final int fit = Math.max(1, (box.h() - 4) / HelpViewerLayout.ROW_H);
        if (!lit.equals(this.revealed)) {
            // A page newly shown brings its row into view, as a help window's tree followed what it showed.
            this.revealed = lit;
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).key().equals(lit) && (i < this.treeScroll || i >= this.treeScroll + fit)) {
                    this.treeScroll = Math.max(0, i - fit / 2);
                }
            }
        }
        this.treeScroll = Math.max(0, Math.min(this.treeScroll, Math.max(0, rows.size() - fit)));
        Draw.pushScissor(g, this.left + box.x(), this.top + box.y(), this.left + box.x() + box.w(),
                this.top + box.y() + box.h());
        for (int i = 0; i < fit + 1 && this.treeScroll + i < rows.size(); i++) {
            final HelpTree.Row row = rows.get(this.treeScroll + i);
            final int x = this.left + box.x() + 3 + row.depth() * HelpViewerLayout.INDENT;
            final int y = this.top + box.y() + 2 + i * HelpViewerLayout.ROW_H;
            this.icon(g, x, y + 1, row.icon(), colours);
            final String label = this.fit(font, row.label(), this.left + box.x() + box.w() - x - ICON_W - 6);
            final boolean chosen = row.key().equals(lit);
            final int labelX = x + ICON_W + 3;
            if (chosen) {
                g.fill(labelX - 1, y - 1, labelX + font.width(label) + 1, y + LINE - 1, colours.select());
            }
            Draw.text(g, font, label, labelX, y, chosen ? colours.selectInk() : colours.ink());
        }
        Draw.popScissor(g);
    }

    /** A book shut or open, or a page: the help of those years drew them so, purple and white. */
    private void icon(final GuiGraphics g, final int x, final int y, final HelpTree.Icon icon,
                      final HelpViewerPalette.Colours colours) {
        switch (icon) {
            case BOOK -> {
                g.fill(x, y, x + ICON_W, y + 7, colours.book());
                g.fill(x, y, x + 1, y + 7, colours.ink());
            }
            case OPEN_BOOK -> {
                g.fill(x, y, x + ICON_W, y + 7, colours.bookOpen());
                g.fill(x + ICON_W / 2, y, x + ICON_W / 2 + 1, y + 7, colours.book());
            }
            case PAGE -> {
                g.fill(x + 1, y, x + ICON_W - 1, y + 7, colours.page());
                Draw.outline(g, x + 1, y, ICON_W - 2, 7, colours.rule());
            }
        }
    }

    /** The page, laid out again only when what it shows or its width changed, then scrolled into its well. */
    private void drawPage(final GuiGraphics g, final Font font, final HelpViewerPalette.Colours colours,
                          final HelpViewerLayout.Box page, final float titleScale, final boolean framed) {
        g.fill(this.left + page.x(), this.top + page.y(), this.left + page.x() + page.w(),
                this.top + page.y() + page.h(), colours.page());
        if (framed) {
            Draw.outline(g, this.left + page.x(), this.top + page.y(), page.w(), page.h(), colours.rule());
        }
        final HelpSession.Special special = this.session.special();
        final List<HelpTarget> listed = special == HelpSession.Special.HISTORY ? this.session.history()
                : this.session.favorites();
        final String key = this.session.shown().written() + "|" + special + "|" + this.session.searched() + "|"
                + this.session.answers() + "|" + page.w() + "|" + listed.size() + "|" + HelpBooks.all().hashCode();
        if (this.laid == null || !key.equals(this.laidFor)) {
            final HelpPageView.Style style = new HelpPageView.Style(colours.ink(), colours.title(), colours.heading(),
                    colours.link(), colours.faint(), colours.warning(), colours.rule(), colours.shade(), titleScale);
            this.laid = HelpPageView.lay(font, page.w() - 8, style, this.session.shown(),
                    new HelpPageView.Special(special, this.session.searched(), List.copyOf(listed)),
                    this.session.source());
            this.laidFor = key;
        }
        this.pageScroll = Math.max(0, Math.min(this.pageScroll, Math.max(0, this.laid.height() - page.h())));
        Draw.pushScissor(g, this.left + page.x() + 1, this.top + page.y() + 1, this.left + page.x() + page.w() - 1,
                this.top + page.y() + page.h() - 1);
        this.laid.draw(g, font, this.left + page.x() + 2, this.top + page.y() + 2 - this.pageScroll, this.mouseX,
                this.mouseY, colours.linkHover());
        Draw.popScissor(g);
        if (this.laid.height() > page.h()) {
            final int track = page.h() - 4;
            final int thumb = Math.max(12, track * page.h() / this.laid.height());
            final int at = (track - thumb) * this.pageScroll / Math.max(1, this.laid.height() - page.h());
            g.fill(this.left + page.x() + page.w() - 4, this.top + page.y() + 2 + at,
                    this.left + page.x() + page.w() - 2, this.top + page.y() + 2 + at + thumb, colours.rule());
        }
    }

    private void field(final GuiGraphics g, final Font font, final HelpViewerLayout.Box box, final String placeholder,
                       final HelpViewerPalette.Colours colours) {
        this.skin.field(g, this.left + box.x(), this.top + box.y(), box.w(), box.h(), this.typing);
        final boolean empty = this.typed.isEmpty();
        final String shown = empty && !this.typing ? placeholder : this.typed + (this.typing ? "_" : "");
        Draw.text(g, font, this.fit(font, shown, box.w() - 6), this.left + box.x() + 3,
                this.top + box.y() + (box.h() - 8) / 2, empty && !this.typing ? colours.faint() : colours.ink());
    }

    /** A push button of the desktop's own look, its words greyed when it does nothing yet. */
    private void pushButton(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                            final TextKey label, final boolean enabled, final boolean primary) {
        final String words = this.fit(font, GameText.resolve(label), w - 4);
        final boolean hovered = enabled && this.mouseX >= this.left + x && this.mouseX < this.left + x + w
                && this.mouseY >= this.top + y && this.mouseY < this.top + y + h;
        if (enabled) {
            this.skin.button(g, font, this.left + x, this.top + y, w, h, words, hovered, false, primary);
        } else {
            this.skin.button(g, font, this.left + x, this.top + y, w, h, "", false, false, false);
            Draw.text(g, font, words, this.left + x + (w - font.width(words)) / 2, this.top + y + (h - 7) / 2,
                    this.skin.dim());
        }
    }

    /** A small arrow pointing back or forward, for the forms whose buttons were arrows. */
    private void arrow(final GuiGraphics g, final int x, final int y, final boolean back, final int colour) {
        for (int i = 0; i < 4; i++) {
            final int column = back ? this.left + x + i : this.left + x + 3 - i;
            g.fill(column, this.top + y + 3 - i, column + 1, this.top + y + 4 + i, colour);
        }
    }

    private void scaled(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                        final float scale, final int colour, final boolean bold) {
        g.pose().pushPose();
        g.pose().translate(this.left + x, this.top + y, 0);
        g.pose().scale(scale, scale, 1.0f);
        Draw.text(g, font, bold ? Component.literal(text).withStyle(ChatFormatting.BOLD) : Component.literal(text), 0,
                0, colour);
        g.pose().popPose();
    }

    private String fit(final Font font, final String text, final int room) {
        if (font.width(text) <= room) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, room - font.width("..."))) + "...";
    }

    private List<String> wrapped(final Font font, final String words, final int room) {
        final List<String> out = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : words.split(" ")) {
            final String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && font.width(next) > room) {
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

    /* What the parts are, where they are */

    private HelpViewerLayout.Frame frame() {
        return HelpViewerLayout.of(this.form);
    }

    /** The tree, grown with the window. */
    @Nullable
    private HelpViewerLayout.Box treeBox() {
        final HelpViewerLayout.Frame frame = this.frame();
        if (frame.tree() == null) {
            return null;
        }
        final HelpViewerLayout.Box tree = frame.tree();
        final int dw = this.form == HelpForm.CDE || this.form == HelpForm.FRAMES_95 ? this.width - frame.width() : 0;
        final int dh = this.form == HelpForm.CDE ? 0 : this.height - frame.height();
        return new HelpViewerLayout.Box(tree.x(), tree.y(), tree.w() + dw, tree.h() + dh);
    }

    /** Frames 95's list under the field of its Index and Find tabs, grown with the window. */
    private HelpViewerLayout.Box listBox() {
        final HelpViewerLayout.Box list = this.frame().page();
        return new HelpViewerLayout.Box(list.x(), list.y(), list.w() + this.width - this.frame().width(),
                list.h() + this.height - this.frame().height());
    }

    /** The page, grown with the window. */
    private HelpViewerLayout.Box pageBox() {
        final HelpViewerLayout.Frame frame = this.frame();
        final HelpViewerLayout.Box page = frame.page();
        return new HelpViewerLayout.Box(page.x(), page.y(), page.w() + this.width - frame.width(),
                page.h() + this.height - frame.height());
    }

    /** The search field, where the form shows one now. */
    @Nullable
    private HelpViewerLayout.Box searchBox() {
        final HelpViewerLayout.Frame frame = this.frame();
        if (frame.search() == null || (this.form == HelpForm.FRAMES_95 && this.tab == CONTENTS_TAB)
                || (this.form == HelpForm.YELP && !this.yelpSearch)) {
            return null;
        }
        final HelpViewerLayout.Box search = frame.search();
        final int dw = this.width - frame.width();
        return switch (this.form) {
            case FRAMES_XP -> new HelpViewerLayout.Box(search.x() + dw, search.y(), search.w(), search.h());
            case GET_HELP, FRAMES_95, FRAMES_7 -> new HelpViewerLayout.Box(search.x(), search.y(),
                    search.w() + (this.form == HelpForm.FRAMES_7 ? 0 : dw), search.h());
            default -> search;
        };
    }

    /** A button, moved with the window's edge it keeps to. */
    private HelpViewerLayout.Button button(final int index) {
        final HelpViewerLayout.Frame frame = this.frame();
        final HelpViewerLayout.Button button = frame.buttons().get(index);
        final HelpViewerLayout.Box box = button.box();
        final int dw = this.width - frame.width();
        final int dh = this.height - frame.height();
        final boolean bottom = this.form == HelpForm.FRAMES_95 || this.form == HelpForm.CDE;
        final boolean right = (this.form == HelpForm.FRAMES_7 && index == 2) || (this.form == HelpForm.YELP
                && index == 1);
        return new HelpViewerLayout.Button(new HelpViewerLayout.Box(box.x() + (right ? dw : 0),
                box.y() + (bottom ? dh : 0), box.w(), box.h()), button.label());
    }

    /* What the parts do */

    private List<HelpTree.Row> rows() {
        return HelpTree.rows(HelpBooks.all(), this.session.source().commands(),
                GameText.resolve(HelpTexts.COMMANDS_PAGES), this.session.openBooks());
    }

    /** Frames 95's Index and Find lists: the index of the first manual from what was typed, or what answers to it. */
    private List<HelpTree.Row> listRows() {
        final List<HelpTree.Row> rows = new ArrayList<>();
        final Optional<ManualReader> first = HelpBooks.first();
        if (first.isEmpty()) {
            return rows;
        }
        final String wanted = this.typed.toString().toLowerCase(Locale.ROOT);
        if (this.tab == INDEX_TAB) {
            for (final ManualReader.IndexLine line : first.get().index()) {
                if (line.text().toLowerCase(Locale.ROOT).startsWith(wanted)) {
                    final HelpTarget target = HelpTarget.node(first.get().manualId(), line.target());
                    rows.add(new HelpTree.Row(target.written() + "#" + line.text(), line.term() ? 1 : 0, line.text(),
                            HelpTree.Icon.PAGE, target));
                }
            }
            return rows;
        }
        for (final ManualReader reader : HelpBooks.all()) {
            for (final String entry : reader.search(this.typed.toString())) {
                final HelpTarget target = HelpTarget.node(reader.manualId(), entry);
                rows.add(new HelpTree.Row(target.written(), 0, reader.label(entry), HelpTree.Icon.PAGE, target));
            }
        }
        return rows;
    }

    /** Whether Frames 95 shows a list under a field (its Index or Find tab) where the tree would be. */
    private boolean listed() {
        return this.form == HelpForm.FRAMES_95 && this.tab != CONTENTS_TAB;
    }

    /** The rows in view: the tree, or Frames 95's list on its Index and Find tabs. */
    private List<HelpTree.Row> shownRows() {
        return this.listed() ? this.listRows() : this.rows();
    }

    @Nullable
    private HelpTree.Row pickedRow() {
        for (final HelpTree.Row row : this.shownRows()) {
            if (row.key().equals(this.picked)) {
                return row;
            }
        }
        return null;
    }

    /** A click in the tree: a row picked (Frames 95), or opened. */
    private void clickTree(final int y) {
        if (y < 0) {
            return;
        }
        final List<HelpTree.Row> rows = this.shownRows();
        final int index = this.treeScroll + y / HelpViewerLayout.ROW_H;
        if (index >= rows.size()) {
            return;
        }
        final HelpTree.Row row = rows.get(index);
        if (this.form == HelpForm.FRAMES_95) {
            final long now = System.currentTimeMillis();
            final boolean twice = row.key().equals(this.lastClick) && now - this.lastClickAt < DOUBLE_CLICK_MS;
            this.lastClick = row.key();
            this.lastClickAt = now;
            this.picked = row.key();
            if (twice) {
                this.display();
            }
            return;
        }
        if (row.book()) {
            this.session.toggle(row.key());
        }
        if (row.target() != null) {
            this.open(row.target());
        }
    }

    /** Frames 95's Display: a book opened or shut, a page shown in the topic window. */
    private void display() {
        final HelpTree.Row row = this.pickedRow();
        if (row == null) {
            return;
        }
        if (row.book() && this.tab == CONTENTS_TAB) {
            this.session.toggle(row.key());
            return;
        }
        if (row.target() != null) {
            this.open(row.target());
            ActiveDesktop.openOrFocus(TOPIC_KEY);
        }
    }

    /** One of the window's buttons, by the form's order of them. */
    private void press(final int index) {
        switch (this.form) {
            case FRAMES_95 -> {
                if (index == 0) {
                    this.display();
                } else if (index == 2) {
                    ActiveDesktop.closeWindowFor(this);
                }
            }
            case FRAMES_95_TOPIC -> {
                if (index == 0) {
                    ActiveDesktop.openOrFocus(KEY);
                } else if (index == 1) {
                    this.back();
                }
            }
            case FRAMES_XP -> {
                switch (index) {
                    case 0 -> this.back();
                    case 1 -> this.session.forward();
                    case 2 -> this.session.home();
                    case 3 -> this.open(HelpTarget.index(this.manualShown()));
                    case 4 -> this.session.showSpecial(HelpSession.Special.FAVORITES);
                    case 5 -> this.session.showSpecial(HelpSession.Special.HISTORY);
                    default -> {
                    }
                }
            }
            case FRAMES_7 -> {
                switch (index) {
                    case 0 -> this.back();
                    case 1 -> this.session.forward();
                    default -> this.session.home();
                }
            }
            case YELP -> {
                if (index == 0) {
                    this.back();
                } else {
                    this.yelpSearch = !this.yelpSearch;
                    this.typing = this.yelpSearch;
                }
            }
            case CDE -> {
                switch (index) {
                    case 0 -> this.back();
                    case 1 -> this.session.showSpecial(HelpSession.Special.HISTORY);
                    case 2 -> this.open(HelpTarget.index(this.manualShown()));
                    default -> this.session.home();
                }
            }
            case GET_HELP, KDE -> {
            }
        }
        this.pageScroll = 0;
    }

    private void back() {
        this.session.back();
        this.pageScroll = 0;
    }

    private void runSearch() {
        if (this.form == HelpForm.FRAMES_95) {
            this.display();
            return;
        }
        this.session.search(this.typed.toString());
        this.pageScroll = 0;
    }

    /** The manual being read: the page's, or the first for a command's. */
    private String manualShown() {
        final HelpTarget shown = this.session.shown();
        if (!shown.manual().isEmpty()) {
            return shown.manual();
        }
        return HelpBooks.first().map(ManualReader::manualId).orElse("");
    }

    /** GNOME's trail: the manual, then each book the page sits in. */
    private List<Crumb> trail() {
        final List<Crumb> crumbs = new ArrayList<>();
        final HelpTarget shown = this.session.shown();
        final Optional<ManualReader> reader = HelpBooks.manual(this.manualShown());
        if (reader.isEmpty()) {
            return crumbs;
        }
        crumbs.add(new Crumb(reader.get().title(), HelpTarget.contents(reader.get().manualId())));
        if (shown.kind() == HelpTarget.Kind.NODE) {
            final List<Crumb> above = new ArrayList<>();
            String at = shown.id();
            while (reader.get().parent(at).isPresent()) {
                at = reader.get().parent(at).get();
                final String id = at;
                reader.get().node(id).ifPresent(node -> above.addFirst(new Crumb(node.title(),
                        HelpTarget.node(reader.get().manualId(), id))));
            }
            crumbs.addAll(above);
        }
        return crumbs;
    }

    private boolean trailClicked(final double rx, final double ry) {
        final HelpViewerLayout.Box trail = this.frame().trail();
        if (ry < trail.y() || ry >= trail.y() + LINE) {
            return false;
        }
        final Font font = Minecraft.getInstance().font;
        int x = trail.x();
        for (final Crumb crumb : this.trail()) {
            if (x > trail.x()) {
                x += font.width(" › ");
            }
            final int w = font.width(crumb.label());
            if (rx >= x && rx < x + w) {
                this.open(crumb.target());
                return true;
            }
            x += w;
        }
        return false;
    }

    /* CDE's menus */

    private List<ContextMenu.Item> fileMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(HelpWindowTexts.CLOSE), true,
                () -> ActiveDesktop.closeWindowFor(this)));
    }

    private List<ContextMenu.Item> editMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(HelpWindowTexts.CDE_COPY), false, () -> { }));
    }

    private List<ContextMenu.Item> searchMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(HelpWindowTexts.INDEX_DOTS), true,
                () -> this.open(HelpTarget.index(this.manualShown()))));
    }

    private List<ContextMenu.Item> navigateMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(HelpWindowTexts.BACKTRACK), this.session.canGoBack(),
                        this::back),
                new ContextMenu.Item(GameText.resolve(HelpWindowTexts.CDE_HOME_TOPIC), true, this.session::home),
                new ContextMenu.Item(GameText.resolve(HelpWindowTexts.HISTORY_DOTS), true,
                        () -> this.session.showSpecial(HelpSession.Special.HISTORY)),
                new ContextMenu.Item(GameText.resolve(HelpWindowTexts.TOP_LEVEL), true, this.session::home));
    }

    private List<ContextMenu.Item> helpMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(HelpWindowTexts.CDE_ABOUT), false, () -> { }));
    }

    /** A step of GNOME's trail: what it says and where it goes. */
    private record Crumb(String label, HelpTarget target) {
    }
}
