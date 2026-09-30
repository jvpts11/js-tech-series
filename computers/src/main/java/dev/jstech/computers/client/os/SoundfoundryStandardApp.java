/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.audio.SongSearch;
import dev.jstech.computers.audio.SoundfoundryPlaylists;
import dev.jstech.computers.client.audio.MusicImporter;
import dev.jstech.computers.client.audio.SoundfoundryPages;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.computers.gui.layout.SoundfoundryStandardLayout;
import dev.jstech.computers.operation.payload.SoundfoundryActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryBrowsePayload;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload.Album;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload.Row;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.logic.TextEditState;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Soundfoundry on the Standard desktops: Voidsoft's player grown into a streaming service. A sidebar with its pages,
 * the playlists and the Soundfoundry Server it streams from; a page of the catalogue's albums, the network's songs and
 * those downloaded, an album's page, a playlist's, a search and the machine's own files; and a bar along the foot with
 * the song playing and its controls. It wears the same skin on every desktop and can fill the whole desktop.
 *
 * <p>What it lists is the machine's, asked for when a page opens, after anything the player does and every few seconds;
 * what is the window's own is the page it is on, what is typed in the search, the row picked and how far it scrolled.
 */
public final class SoundfoundryStandardApp implements IDesktopApp {

    private final BlockPos host;
    private final ContextMenu menu = new ContextMenu(MENU_W, MENU_ROW_H);
    private final TextEditState query = new TextEditState(SongSearch.MAX_QUERY);
    private OsSkin skin = OsSkin.fallback();
    private int page = SoundfoundryPagePayload.HOME;
    private String arg = "";
    private int scroll;
    private int picked = -1;
    private int pickedSection = SoundfoundryPagePayload.TRACKS;
    private boolean typing;
    private boolean focused = true;
    private int pressedControl = DesktopWindow.BUTTON_NONE;
    private int dragging = DRAG_NONE;
    private double dragValue;
    private long nextLookAt;
    private long nextBrowseAt;
    private long lastClickAt;
    private int lastRow = -1;
    private Text notice = Text.EMPTY;
    private long noticeUntil;
    /** When the state whose trouble was last said came, so each is said once. */
    private long heardAt = -1L;
    @Nullable
    private Text importing;
    private int lastX;
    private int lastY;
    private int width = SoundfoundryStandardLayout.DEFAULT_W;
    private int height = SoundfoundryStandardLayout.DEFAULT_H;
    private int mouseX;
    private int mouseY;

    private static final int MENU_W = 170;
    private static final int MENU_ROW_H = 12;
    private static final int DRAG_NONE = 0;
    private static final int DRAG_POSITION = 1;
    private static final int DRAG_VOLUME = 2;
    private static final long LOOK_EVERY = 1000L;
    private static final long BROWSE_EVERY = 4000L;
    private static final long DOUBLE_CLICK = 300L;
    private static final long NOTICE_FOR = 4000L;
    private static final int PAD = SoundfoundryStandardLayout.PAD;
    private static final String STATE = "S";

    public SoundfoundryStandardApp(final BlockPos host) {
        this.host = host;
        look();
        browse();
    }

    /**
     * Whether the desktop up now is one of the Standard era's, where Soundfoundry is a streaming player rather than the
     * one of the Legacy desktops: Frames 11 and the later Linux desktops, not their period forms.
     */
    static boolean onStandardDesktop() {
        final DesktopState desktop = DesktopScreen.current();
        if (desktop == null) {
            return false;
        }
        final OsSkin.Form form = desktop.prefs().skin().form();
        if (form == OsSkin.Form.KDE2 || form == OsSkin.Form.GNOME1) {
            return false;
        }
        final PanelStyle style = desktop.panelStyle();
        return style == PanelStyle.FRAMES_11 || style == PanelStyle.KDE || style == PanelStyle.GNOME
                || style == PanelStyle.CINNAMON;
    }

    @Override
    public String title() {
        return ProgramClient.nameOf(Programs.SOUNDFOUNDRY);
    }

    @Override
    public ResourceLocation iconId() {
        return Programs.SOUNDFOUNDRY;
    }

    @Override
    public int defaultWidth() {
        return SoundfoundryStandardLayout.DEFAULT_W;
    }

    @Override
    public int defaultHeight() {
        return SoundfoundryStandardLayout.DEFAULT_H;
    }

    @Override
    public int minWidth() {
        return SoundfoundryStandardLayout.MIN_W;
    }

    @Override
    public int minHeight() {
        return SoundfoundryStandardLayout.MIN_H;
    }

    @Override
    public boolean drawsOwnFrame() {
        return true;
    }

    @Override
    public boolean ownFrameFills() {
        return true;
    }

    @Override
    public int frameControlAt(final int x, final int y) {
        if (SoundfoundryStandardLayout.minimize(width).contains(x, y)) {
            return DesktopWindow.BUTTON_MINIMIZE;
        }
        if (SoundfoundryStandardLayout.maximize(width).contains(x, y)) {
            return DesktopWindow.BUTTON_MAXIMIZE;
        }
        if (SoundfoundryStandardLayout.close(width).contains(x, y)) {
            return DesktopWindow.BUTTON_CLOSE;
        }
        return DesktopWindow.BUTTON_NONE;
    }

    @Override
    public boolean frameDragAt(final int x, final int y) {
        return SoundfoundryStandardLayout.titleBar(width).contains(x, y)
                && frameControlAt(x, y) == DesktopWindow.BUTTON_NONE;
    }

    @Override
    public void frameState(final boolean isFocused, final int pressed) {
        this.focused = isFocused;
        this.pressedControl = pressed;
    }

    @Override
    public void applySkin(final OsSkin value) {
        this.skin = value;
    }

    @Override
    public void onRestored() {
        look();
        browse();
    }

    @Override
    public String saveState() {
        return STATE + page + ":" + scroll + ":" + arg;
    }

    @Override
    public void restoreState(final String state) {
        if (!state.startsWith(STATE)) {
            return;
        }
        final String[] parts = state.substring(STATE.length()).split(":", 3);
        if (parts.length < 3) {
            return;
        }
        try {
            page = Math.clamp(Integer.parseInt(parts[0]), SoundfoundryPagePayload.HOME,
                    SoundfoundryPagePayload.CATALOG);
            scroll = Math.max(0, Integer.parseInt(parts[1]));
        } catch (final NumberFormatException malformed) {
            page = SoundfoundryPagePayload.HOME;
            scroll = 0;
        }
        arg = parts[2];
        if (page == SoundfoundryPagePayload.SEARCH) {
            query.sync(arg);
        }
        browse();
    }

    @Override
    public boolean wantsEscape() {
        return typing || menu.isOpen();
    }

    @Override
    public boolean modalActive() {
        return menu.isOpen();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mx, final int my, final float partialTick) {
        this.lastX = x;
        this.lastY = y;
        this.width = w;
        this.height = h;
        this.mouseX = mx;
        this.mouseY = my;
        final long now = Util.getMillis();
        if (now >= nextLookAt) {
            look();
        }
        if (now >= nextBrowseAt) {
            browse();
        }
        final SoundfoundryStates.Known state = SoundfoundryStates.of(host);
        hear(state);
        final SoundfoundryPages.Known shown = SoundfoundryPages.of(host);
        final SoundfoundryPagePayload known = shown != null && shown.is(page, arg) ? shown.page() : null;
        final SoundfoundryPagePayload.Sidebar sidebar = shown == null ? SoundfoundryPagePayload.Sidebar.NONE
                : shown.page().sidebar();
        final SoundfoundryStandardSkin.Colours c = SoundfoundryStandardSkin.c();
        SoundfoundryStandardSkin.fill(g, x, y, w, h, c.base());
        renderTitleBar(g, font, x, y, c);
        renderSidebar(g, font, x, y, sidebar, c);
        final Rect area = SoundfoundryStandardLayout.page(w, h);
        Draw.pushScissor(g, x + area.x(), y + area.y(), x + area.right(), y + area.bottom());
        renderPage(g, font, x + area.x(), y + area.y(), area.w(), area.h(), known, sidebar, state, c);
        Draw.popScissor(g);
        renderFoot(g, font, x, y + h - SoundfoundryStandardLayout.FOOT_H, state, c);
        outline(g, x, y, w, h, c.frame());
        if (menu.isOpen()) {
            menu.render(g, new UiContext(skin, font, mx, my, partialTick));
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mx, final double my, final int button) {
        if (menu.isOpen()) {
            menu.mouseClicked(mx, my, button);
            return;
        }
        final double lx = mx - lastX;
        final double ly = my - lastY;
        final boolean wasTyping = typing;
        typing = false;
        if (ly < SoundfoundryStandardLayout.TITLE_H) {
            return;
        }
        if (ly >= height - SoundfoundryStandardLayout.FOOT_H) {
            clickedFoot(lx, ly - (height - SoundfoundryStandardLayout.FOOT_H));
        } else if (lx < SoundfoundryStandardLayout.SIDE_W) {
            clickedSidebar(lx, ly, button);
        } else {
            clickedPage(lx - SoundfoundryStandardLayout.SIDE_W, ly - SoundfoundryStandardLayout.TITLE_H, button,
                    wasTyping);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mx, final double my, final int button) {
        final double lx = mx - lastX;
        if (dragging == DRAG_POSITION) {
            dragValue = along(SoundfoundryStandardLayout.position(width), lx);
        } else if (dragging == DRAG_VOLUME) {
            dragValue = along(SoundfoundryStandardLayout.volume(width), lx);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mx, final double my, final int button) {
        final SoundfoundryStates.Known state = SoundfoundryStates.of(host);
        if (dragging == DRAG_POSITION && state != null && state.state().playing().millis() > 0) {
            send(SoundfoundryActionPayload.SEEK, 0, Math.round(dragValue * state.state().playing().millis()),
                    List.of(), List.of());
        } else if (dragging == DRAG_VOLUME) {
            send(SoundfoundryActionPayload.VOLUME, (int) Math.round(dragValue * SoundfoundryState.MAX_VOLUME));
        }
        dragging = DRAG_NONE;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final Rect area = SoundfoundryStandardLayout.page(width, height);
        if (!area.contains(mouseX - lastX, mouseY - lastY)) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * 3));
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        if (!typing) {
            return false;
        }
        if (c >= ' ' && c != 127) {
            query.type(c);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (menu.isOpen()) {
            return menu.keyPressed(key, scanCode, modifiers);
        }
        if (typing) {
            return edit(key, modifiers);
        }
        if (key == GLFW.GLFW_KEY_SPACE) {
            playOrPause(SoundfoundryStates.of(host));
            return true;
        }
        return false;
    }

    /** The page the window is on, as {@link SoundfoundryPagePayload} numbers them. */
    public int page() {
        return page;
    }

    /** What the page is of: an album, a playlist or a search. */
    public String pageArg() {
        return arg;
    }

    /** Opens that page, as a click in the window would. */
    public void open(final int value, final String of) {
        page = value;
        arg = of;
        scroll = 0;
        picked = -1;
        typing = false;
        browse();
    }

    /** A point of the window, measured from its corner, in desktop pixels, the way a click lands on it. */
    public int[] point(final int x, final int y) {
        return new int[] {lastX + x, lastY + y};
    }

    /** The middle of a part of the window, measured from its corner, the same way. */
    public int[] point(final Rect part) {
        return point(part.x() + part.w() / 2, part.y() + part.h() / 2);
    }

    /** The size the window was last drawn at. */
    public int[] size() {
        return new int[] {width, height};
    }

    /** Whether a menu of the window is open. */
    public boolean menuOpen() {
        return menu.isOpen();
    }

    /** The middle of the open menu's entry so called, in desktop pixels, or null. */
    @Nullable
    public int[] menuPoint(final String label) {
        final List<ContextMenu.Item> items = menu.items();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).label().equals(label)) {
                return menu.itemCenter(i);
            }
        }
        return null;
    }

    /* ---------- drawing ---------- */

    private void renderTitleBar(final GuiGraphics g, final Font font, final int x, final int y,
                                final SoundfoundryStandardSkin.Colours c) {
        final int w = width;
        SoundfoundryStandardSkin.fill(g, x, y, w, SoundfoundryStandardLayout.TITLE_H, c.titleBar());
        SoundfoundryStandardSkin.anvil(g, x + 6, y + 4, 0.75F, c.orange());
        SoundfoundryStandardSkin.text(g, font, title(), x + 22, y + 5, focused ? c.ink() : c.inkDim(),
                c.titleBar());
        final int minus = pressedControl == DesktopWindow.BUTTON_MINIMIZE ? c.ink() : c.inkDim();
        final int square = pressedControl == DesktopWindow.BUTTON_MAXIMIZE ? c.ink() : c.inkDim();
        final int cross = pressedControl == DesktopWindow.BUTTON_CLOSE ? c.ink() : c.inkDim();
        SoundfoundryStandardSkin.fill(g, x + w - 50, y + 9, 8, 1, minus);
        outline(g, x + w - 32, y + 5, 7, 7, square);
        for (int k = 0; k < 7; k++) {
            SoundfoundryStandardSkin.fill(g, x + w - 14 + k, y + 5 + k, 1, 1, cross);
            SoundfoundryStandardSkin.fill(g, x + w - 8 - k, y + 5 + k, 1, 1, cross);
        }
    }

    private void renderSidebar(final GuiGraphics g, final Font font, final int x, final int y,
                               final SoundfoundryPagePayload.Sidebar sidebar,
                               final SoundfoundryStandardSkin.Colours c) {
        final Rect side = SoundfoundryStandardLayout.sidebar(height);
        SoundfoundryStandardSkin.fill(g, x + side.x(), y + side.y(), side.w(), side.h(), c.side());
        final SoundfoundryStandardSkin.Glyph[] glyphs = {SoundfoundryStandardSkin.Glyph.HOME,
                SoundfoundryStandardSkin.Glyph.SEARCH, SoundfoundryStandardSkin.Glyph.LIBRARY};
        final TextKey[] labels = {SoundfoundryStandardTexts.HOME, SoundfoundryStandardTexts.SEARCH,
                SoundfoundryStandardTexts.LIBRARY};
        final int[] pages = {SoundfoundryPagePayload.HOME, SoundfoundryPagePayload.SEARCH,
                SoundfoundryPagePayload.LIBRARY};
        for (int i = 0; i < 3; i++) {
            final int ny = y + SoundfoundryStandardLayout.NAV_Y + i * SoundfoundryStandardLayout.NAV_STEP;
            final boolean on = page == pages[i];
            if (on) {
                SoundfoundryStandardSkin.fill(g, x, ny - 3, 2, 14, c.orange());
            }
            final int ink = on ? c.ink() : c.inkDim();
            SoundfoundryStandardSkin.glyph(g, glyphs[i], x + SoundfoundryStandardLayout.NAV_GLYPH_X, ny, ink);
            SoundfoundryStandardSkin.text(g, font, words(labels[i]), x + SoundfoundryStandardLayout.NAV_TEXT_X, ny,
                    ink, c.side());
        }
        final Rect rule = SoundfoundryStandardLayout.SIDE_RULE;
        SoundfoundryStandardSkin.fill(g, x + rule.x(), y + rule.y(), rule.w(), rule.h(), c.line());
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.PLAYLISTS), x + 12,
                y + SoundfoundryStandardLayout.PLAYLISTS_Y, c.inkDim(), c.side());
        SoundfoundryStandardSkin.right(g, font, "+", x + 138, y + SoundfoundryStandardLayout.PLAYLISTS_Y,
                c.inkDim(), c.side());
        final List<String> playlists = playlistsOf(sidebar);
        final int shownLists = Math.min(playlists.size(), SoundfoundryStandardLayout.playlistsShown(height));
        for (int i = 0; i < shownLists; i++) {
            final String name = playlists.get(i);
            final int py = y + SoundfoundryStandardLayout.LIST_Y + i * SoundfoundryStandardLayout.LIST_STEP;
            final boolean liked = name.equals(SoundfoundryPlaylists.LIKED);
            SoundfoundryStandardSkin.fill(g, x + SoundfoundryStandardLayout.SWATCH_X, py - 1, 10, 10,
                    liked ? c.orange() : c.swatch());
            final boolean on = page == SoundfoundryPagePayload.PLAYLIST && arg.equals(name);
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, shownName(name), 110),
                    x + SoundfoundryStandardLayout.LIST_TEXT_X, py, on ? c.orange() : c.ink(), c.side());
        }
        final Rect box = SoundfoundryStandardLayout.serverBox(height);
        SoundfoundryStandardSkin.fill(g, x + box.x(), y + box.y(), box.w(), box.h(), c.box());
        SoundfoundryStandardSkin.fill(g, x + box.x(), y + box.y(), box.w(), 1, c.line());
        final SoundfoundryPagePayload.Server server = chosen(sidebar);
        if (server != null) {
            SoundfoundryStandardSkin.fill(g, x + box.x() + 6, y + box.y() + 8, 5, 5, c.online());
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, server.name(), box.w() - 22),
                    x + box.x() + 16, y + box.y() + 6, c.ink(), c.box());
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(
                            SoundfoundryStandardTexts.ONLINE.with(server.songs())), box.w() - 10),
                    x + box.x() + 6, y + box.y() + 20, c.inkDim(), c.box());
        } else {
            SoundfoundryStandardSkin.fill(g, x + box.x() + 6, y + box.y() + 8, 5, 5, c.offline());
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.NO_SERVER_BOX),
                    x + box.x() + 16, y + box.y() + 6, c.inkDim(), c.box());
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.NO_SERVER_BOX_2),
                    x + box.x() + 6, y + box.y() + 20, c.inkDim(), c.box());
        }
    }

    private void renderPage(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                            final int ph, @Nullable final SoundfoundryPagePayload known,
                            final SoundfoundryPagePayload.Sidebar sidebar,
                            @Nullable final SoundfoundryStates.Known state, final SoundfoundryStandardSkin.Colours c) {
        switch (page) {
            case SoundfoundryPagePayload.ALBUM -> renderAlbum(g, font, px, py, pw, ph, known, state, c);
            case SoundfoundryPagePayload.PLAYLIST -> renderPlaylist(g, font, px, py, pw, ph, known, state, c);
            case SoundfoundryPagePayload.SEARCH -> renderSearch(g, font, px, py, pw, ph, known, state, c);
            case SoundfoundryPagePayload.LIBRARY -> renderLibrary(g, font, px, py, pw, ph, known, sidebar, state, c);
            case SoundfoundryPagePayload.CATALOG -> renderCatalog(g, font, px, py, pw, ph, known, c);
            default -> renderHome(g, font, px, py, pw, ph, known, state, c);
        }
    }

    private void renderHome(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                            final int ph, @Nullable final SoundfoundryPagePayload known,
                            @Nullable final SoundfoundryStates.Known state, final SoundfoundryStandardSkin.Colours c) {
        g.fillGradient(px, py, px + pw, py + 122, c.glowHome(), c.base());
        SoundfoundryStandardSkin.big(g, font, words(greeting()), px + PAD, py + SoundfoundryStandardLayout.GREETING_Y,
                2, c.ink(), c.glowHome());
        if (known == null) {
            return;
        }
        final int sy = py + SoundfoundryStandardLayout.SECTION_Y;
        if (!known.albums().isEmpty()) {
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.FROM_CATALOG), px + PAD, sy,
                    c.ink(), c.base());
            SoundfoundryStandardSkin.right(g, font, words(SoundfoundryStandardTexts.SHOW_ALL), px + pw - PAD, sy,
                    c.inkDim(), c.base());
            final int across = Math.min(known.albums().size(), SoundfoundryStandardLayout.cardsAcross(pw));
            for (int i = 0; i < across; i++) {
                card(g, font, px + PAD + i * SoundfoundryStandardLayout.CARD_STEP,
                        py + SoundfoundryStandardLayout.CARD_Y, known.albums().get(i), c);
            }
        }
        final int ly = py + SoundfoundryStandardLayout.LISTS_Y;
        final int second = px + SoundfoundryStandardLayout.secondColumn(pw);
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.ON_NETWORK), px + PAD, ly, c.ink(),
                c.base());
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.DOWNLOADED), second, ly, c.ink(),
                c.base());
        final int rows = SoundfoundryStandardLayout.homeRows(ph);
        final String playing = playingRef(state);
        final List<Row> network = known.section(SoundfoundryPagePayload.NETWORK);
        for (int i = 0; i < Math.min(rows, network.size()); i++) {
            final Row row = network.get(i);
            homeRow(g, font, px + PAD, py + SoundfoundryStandardLayout.LIST_ROW_Y
                            + i * SoundfoundryStandardLayout.LIST_ROW_STEP, second - px - PAD - 32, row,
                    GameText.resolve(row.artist().isEmpty() ? SoundfoundryStandardTexts.FROM_ALONE.with(row.from())
                            : SoundfoundryStandardTexts.FROM.with(row.artist(), row.from())),
                    row.ref().equals(playing), c);
        }
        final List<Row> downloaded = known.section(SoundfoundryPagePayload.DOWNLOADED);
        for (int i = 0; i < Math.min(rows, downloaded.size()); i++) {
            final Row row = downloaded.get(i);
            homeRow(g, font, second, py + SoundfoundryStandardLayout.LIST_ROW_Y
                            + i * SoundfoundryStandardLayout.LIST_ROW_STEP, px + pw - PAD - second - 32, row,
                    row.artist(), row.ref().equals(playing), c);
        }
    }

    private void card(final GuiGraphics g, final Font font, final int cx, final int cy, final Album album,
                      final SoundfoundryStandardSkin.Colours c) {
        final int size = SoundfoundryStandardLayout.CARD;
        SoundfoundryStandardSkin.cover(g, font, host, cx, cy, size, album.title(), album.cover());
        final int room = SoundfoundryStandardLayout.CARD_STEP - 6;
        SoundfoundryStandardSkin.text(g, font, Texts.clip(font, album.title(), room), cx,
                cy + SoundfoundryStandardLayout.CARD_TITLE_Y - SoundfoundryStandardLayout.CARD_Y, c.ink(), c.base());
        SoundfoundryStandardSkin.text(g, font, Texts.clip(font, album.artist(), room), cx,
                cy + SoundfoundryStandardLayout.CARD_ARTIST_Y - SoundfoundryStandardLayout.CARD_Y, c.inkDim(),
                c.base());
    }

    private void homeRow(final GuiGraphics g, final Font font, final int rx, final int ry, final int room,
                         final Row row, final String second, final boolean playing,
                         final SoundfoundryStandardSkin.Colours c) {
        SoundfoundryStandardSkin.cover(g, font, host, rx, ry, SoundfoundryStandardLayout.ROW_COVER, coverName(row),
                row.cover());
        final String title = Texts.clip(font, row.title(), room);
        SoundfoundryStandardSkin.text(g, font, title, rx + 28, ry + 2, playing ? c.orange() : c.ink(), c.base());
        arrowFor(g, rx + 28 + font.width(title) + 5, ry + 2, row.state(), c);
        SoundfoundryStandardSkin.text(g, font, Texts.clip(font, second, room + 24), rx + 28, ry + 12, c.inkDim(),
                c.base());
    }

    private void renderAlbum(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                             final int ph, @Nullable final SoundfoundryPagePayload known,
                             @Nullable final SoundfoundryStates.Known state,
                             final SoundfoundryStandardSkin.Colours c) {
        g.fillGradient(px, py, px + pw, py + 152, c.glowAlbum(), c.base());
        final Album album = known == null || known.albums().isEmpty() ? null : known.albums().getFirst();
        if (album == null) {
            return;
        }
        final Rect cover = SoundfoundryStandardLayout.ALBUM_COVER;
        SoundfoundryStandardSkin.cover(g, font, host, px + cover.x(), py + cover.y(), cover.w(), album.title(),
                album.cover());
        final int tx = px + SoundfoundryStandardLayout.HEAD_TEXT_X;
        final int room = pw - SoundfoundryStandardLayout.HEAD_TEXT_X - PAD;
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.ALBUM), tx,
                py + SoundfoundryStandardLayout.KIND_Y, c.ink(), c.glowAlbum());
        SoundfoundryStandardSkin.big(g, font, Texts.clip(font, album.title(), room / 3), tx,
                py + SoundfoundryStandardLayout.NAME_Y, 3, c.ink(), c.glowAlbum());
        final long minutes = Math.round(album.millis() / 60_000.0);
        final Text meta = album.year().isEmpty()
                ? SoundfoundryStandardTexts.ALBUM_META_UNDATED.with(album.artist(), album.songs(), minutes)
                : SoundfoundryStandardTexts.ALBUM_META.with(album.artist(), album.year(), album.songs(), minutes);
        SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(meta), room), tx,
                py + SoundfoundryStandardLayout.META_Y, c.ink(), c.glowAlbum());
        SoundfoundryStandardSkin.anvil(g, tx, py + SoundfoundryStandardLayout.SOURCE_Y, 0.5F, c.orange());
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.CATALOG), tx + 12,
                py + SoundfoundryStandardLayout.SOURCE_Y, c.orange(), c.glowAlbum());
        renderHeadButtons(g, font, px, py, state, c, true);
        SoundfoundryStandardSkin.text(g, font, GameText.resolve(SoundfoundryStandardTexts.DOWNLOADED_OF.with(
                        album.downloaded(), album.songs())), px + SoundfoundryStandardLayout.DOWNLOADED_X,
                py + SoundfoundryStandardLayout.DOWNLOAD_ALBUM.y() + 5, c.inkDim(), c.base());
        renderTable(g, font, px, py, pw, ph, known.section(SoundfoundryPagePayload.TRACKS),
                SoundfoundryStandardLayout.TRACKS_HEAD_Y, SoundfoundryStandardLayout.TRACKS_Y,
                SoundfoundryStandardLayout.TRACK_H, false, state, c);
    }

    private void renderPlaylist(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                                final int ph, @Nullable final SoundfoundryPagePayload known,
                                @Nullable final SoundfoundryStates.Known state,
                                final SoundfoundryStandardSkin.Colours c) {
        g.fillGradient(px, py, px + pw, py + 152, c.glowHome(), c.base());
        final Rect cover = SoundfoundryStandardLayout.ALBUM_COVER;
        if (arg.equals(SoundfoundryPlaylists.LIKED)) {
            SoundfoundryStandardSkin.fill(g, px + cover.x(), py + cover.y(), cover.w(), cover.h(), c.orange());
        } else {
            SoundfoundryStandardSkin.cover(g, font, px + cover.x(), py + cover.y(), cover.w(), arg);
        }
        final int tx = px + SoundfoundryStandardLayout.HEAD_TEXT_X;
        final int room = pw - SoundfoundryStandardLayout.HEAD_TEXT_X - PAD;
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.PLAYLIST), tx,
                py + SoundfoundryStandardLayout.KIND_Y, c.ink(), c.glowHome());
        SoundfoundryStandardSkin.big(g, font, Texts.clip(font, shownName(arg), room / 3), tx,
                py + SoundfoundryStandardLayout.NAME_Y, 3, c.ink(), c.glowHome());
        final List<Row> rows = known == null ? List.of() : known.section(SoundfoundryPagePayload.TRACKS);
        SoundfoundryStandardSkin.text(g, font, GameText.resolve(SoundfoundryStandardTexts.SONGS.with(rows.size())),
                tx, py + SoundfoundryStandardLayout.META_Y, c.ink(), c.glowHome());
        renderHeadButtons(g, font, px, py, state, c, false);
        if (known != null && rows.isEmpty()) {
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.EMPTY_PLAYLIST), px + PAD,
                    py + SoundfoundryStandardLayout.TRACKS_Y, c.inkDim(), c.base());
            return;
        }
        renderTable(g, font, px, py, pw, ph, rows, SoundfoundryStandardLayout.TRACKS_HEAD_Y,
                SoundfoundryStandardLayout.TRACKS_Y, SoundfoundryStandardLayout.TRACK_H, true, state, c);
    }

    /* The big play button, the shuffle switch and, for an album, the button that downloads it all. */
    private void renderHeadButtons(final GuiGraphics g, final Font font, final int px, final int py,
                                   @Nullable final SoundfoundryStates.Known state,
                                   final SoundfoundryStandardSkin.Colours c, final boolean album) {
        final Rect play = SoundfoundryStandardLayout.PLAY;
        SoundfoundryStandardSkin.fill(g, px + play.x(), py + play.y(), play.w(), play.h(), c.orange());
        SoundfoundryStandardSkin.triangle(g, px + play.x() + 9, py + play.y() + 6, 12, c.knob());
        final boolean shuffling = state != null && state.state().shuffle();
        final Rect shuffle = SoundfoundryStandardLayout.SHUFFLE_ALBUM;
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.SHUFFLE, px + shuffle.x() + 2,
                py + shuffle.y() + 2, shuffling ? c.orange() : c.inkDim());
        if (!album) {
            return;
        }
        final Rect download = SoundfoundryStandardLayout.DOWNLOAD_ALBUM;
        SoundfoundryStandardSkin.fill(g, px + download.x(), py + download.y(), download.w(), download.h(), c.swatch());
        SoundfoundryStandardSkin.arrow(g, px + download.x() + 6, py + download.y() + 5, c.ink());
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.DOWNLOAD_ALBUM),
                px + download.x() + 18, py + download.y() + 5, c.ink(), c.swatch());
    }

    private void renderSearch(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                              final int ph, @Nullable final SoundfoundryPagePayload known,
                              @Nullable final SoundfoundryStates.Known state,
                              final SoundfoundryStandardSkin.Colours c) {
        SoundfoundryStandardSkin.big(g, font, words(SoundfoundryStandardTexts.SEARCH), px + PAD,
                py + SoundfoundryStandardLayout.GREETING_Y, 2, c.ink(), c.base());
        final Rect box = SoundfoundryStandardLayout.SEARCH_BOX;
        SoundfoundryStandardSkin.fill(g, px + box.x(), py + box.y(), box.w(), box.h(), c.ink());
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.SEARCH, px + box.x() + 5,
                py + box.y() + 4, c.knob());
        final String text = typing ? query.edit() : query.value();
        final int room = box.w() - 26;
        final int left = px + box.x() + 18;
        final int top = py + box.y() + 4;
        if (text.isEmpty() && !typing) {
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, words(SoundfoundryStandardTexts.SEARCH_HINT),
                    room), left, top, c.inkOff(), c.ink());
        } else {
            final int caret = typing ? Math.min(query.caret(), text.length()) : text.length();
            int start = 0;
            while (start < caret && font.width(text.substring(start, caret)) > room) {
                start++;
            }
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, text.substring(start), room), left, top,
                    c.knob(), c.ink());
            if (typing && Util.getMillis() / 500L % 2L == 0L) {
                SoundfoundryStandardSkin.fill(g, left + font.width(text.substring(start, caret)), top - 1, 1, 9,
                        c.knob());
            }
        }
        if (known == null || arg.isBlank()) {
            return;
        }
        final List<Row> rows = known.section(SoundfoundryPagePayload.TRACKS);
        if (rows.isEmpty()) {
            SoundfoundryStandardSkin.text(g, font, GameText.resolve(SoundfoundryStandardTexts.NOTHING_FOUND.with(arg)),
                    px + PAD, py + SoundfoundryStandardLayout.SEARCH_ROWS_Y, c.inkDim(), c.base());
            return;
        }
        renderTable(g, font, px, py, pw, ph, rows, SoundfoundryStandardLayout.SEARCH_ROWS_Y - 16,
                SoundfoundryStandardLayout.SEARCH_ROWS_Y, SoundfoundryStandardLayout.TRACK_H, true, state, c);
    }

    private void renderLibrary(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                               final int ph, @Nullable final SoundfoundryPagePayload known,
                               final SoundfoundryPagePayload.Sidebar sidebar,
                               @Nullable final SoundfoundryStates.Known state,
                               final SoundfoundryStandardSkin.Colours c) {
        SoundfoundryStandardSkin.big(g, font, words(SoundfoundryStandardTexts.LOCAL), px + PAD,
                py + SoundfoundryStandardLayout.GREETING_Y, 2, c.ink(), c.base());
        final boolean server = !sidebar.servers().isEmpty();
        final int lift = server ? SoundfoundryStandardLayout.NO_BANNER : 0;
        if (!server) {
            final int by = py + SoundfoundryStandardLayout.BANNER_Y;
            SoundfoundryStandardSkin.fill(g, px + PAD, by, pw - 2 * PAD, SoundfoundryStandardLayout.BANNER_H,
                    c.banner());
            SoundfoundryStandardSkin.fill(g, px + PAD, by, 2, SoundfoundryStandardLayout.BANNER_H, c.orange());
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.NO_SERVER), px + PAD + 10, by + 6,
                    c.ink(), c.banner());
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, words(SoundfoundryStandardTexts.NO_SERVER_HOW),
                    pw - 2 * PAD - 14), px + PAD + 10, by + 18, c.inkDim(), c.banner());
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.NO_SERVER_HOW_2), px + PAD + 10,
                    by + 28, c.inkDim(), c.banner());
        }
        final int buttons = py + SoundfoundryStandardLayout.BUTTONS_Y - lift;
        SoundfoundryStandardSkin.fill(g, px + PAD, buttons, SoundfoundryStandardLayout.IMPORT_W,
                SoundfoundryStandardLayout.BUTTON_H, c.orange());
        SoundfoundryStandardSkin.centred(g, font, words(SoundfoundryStandardTexts.IMPORT),
                px + PAD + SoundfoundryStandardLayout.IMPORT_W / 2, buttons + 5, c.knob(), c.orange());
        final int ux = px + PAD + SoundfoundryStandardLayout.IMPORT_W + 6;
        final boolean canUpload = server && picked >= 0;
        SoundfoundryStandardSkin.fill(g, ux, buttons, SoundfoundryStandardLayout.UPLOAD_W,
                SoundfoundryStandardLayout.BUTTON_H, canUpload ? c.swatch() : c.buttonOff());
        SoundfoundryStandardSkin.centred(g, font, words(SoundfoundryStandardTexts.UPLOAD),
                ux + SoundfoundryStandardLayout.UPLOAD_W / 2, buttons + 5, canUpload ? c.ink() : c.inkOff(),
                canUpload ? c.swatch() : c.buttonOff());
        if (importing != null) {
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(importing),
                            pw - PAD - (ux - px) - SoundfoundryStandardLayout.UPLOAD_W - 12),
                    ux + SoundfoundryStandardLayout.UPLOAD_W + 8, buttons + 5, c.inkDim(), c.base());
        }
        final List<Row> rows = known == null ? List.of() : known.section(SoundfoundryPagePayload.TRACKS);
        if (known != null && rows.isEmpty()) {
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.NO_LOCAL), px + PAD,
                    py + SoundfoundryStandardLayout.LIST_HEAD_Y - lift + 16, c.inkDim(), c.base());
            return;
        }
        renderTable(g, font, px, py, pw, ph, rows, SoundfoundryStandardLayout.LIST_HEAD_Y - lift,
                SoundfoundryStandardLayout.LIST_HEAD_Y - lift + 16, SoundfoundryStandardLayout.LIST_H, true, state,
                c);
    }

    private void renderCatalog(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                               final int ph, @Nullable final SoundfoundryPagePayload known,
                               final SoundfoundryStandardSkin.Colours c) {
        SoundfoundryStandardSkin.big(g, font, words(SoundfoundryStandardTexts.FROM_CATALOG), px + PAD,
                py + SoundfoundryStandardLayout.GREETING_Y, 2, c.ink(), c.base());
        if (known == null) {
            return;
        }
        final int across = SoundfoundryStandardLayout.cardsAcross(pw);
        final int rowH = SoundfoundryStandardLayout.CARD_ARTIST_Y - SoundfoundryStandardLayout.CARD_Y + 20;
        for (int i = scroll * across; i < known.albums().size(); i++) {
            final int row = i / across - scroll;
            final int cy = py + SoundfoundryStandardLayout.SECTION_Y + row * rowH;
            if (cy > py + ph) {
                break;
            }
            card(g, font, px + PAD + (i % across) * SoundfoundryStandardLayout.CARD_STEP, cy, known.albums().get(i),
                    c);
        }
    }

    /* A table of songs: its header, then a row for each song from where it is scrolled to, the one playing lit. */
    private void renderTable(final GuiGraphics g, final Font font, final int px, final int py, final int pw,
                             final int ph, final List<Row> rows, final int headY, final int top, final int rowH,
                             final boolean artist, @Nullable final SoundfoundryStates.Known state,
                             final SoundfoundryStandardSkin.Colours c) {
        final int right = px + pw - SoundfoundryStandardLayout.TIME_RIGHT;
        SoundfoundryStandardSkin.text(g, font, "#", px + 18, py + headY, c.inkDim(), c.base());
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.COLUMN_TITLE),
                px + SoundfoundryStandardLayout.TITLE_X, py + headY, c.inkDim(), c.base());
        final int artistX = px + Math.max(SoundfoundryStandardLayout.ARTIST_X, (pw - 60) * 5 / 12);
        if (artist) {
            SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.COLUMN_ARTIST), artistX,
                    py + headY, c.inkDim(), c.base());
        }
        SoundfoundryStandardSkin.right(g, font, words(SoundfoundryStandardTexts.COLUMN_TIME), right, py + headY,
                c.inkDim(), c.base());
        SoundfoundryStandardSkin.fill(g, px + PAD, py + headY + 10, pw - 2 * PAD, 1, c.line());
        final String playing = playingRef(state);
        final int shown = SoundfoundryStandardLayout.rowsFrom(ph, top, rowH);
        final int from = Math.min(scroll, Math.max(0, rows.size() - shown));
        for (int i = 0; i < shown && from + i < rows.size(); i++) {
            final int index = from + i;
            final Row row = rows.get(index);
            final int ry = py + top + i * rowH;
            final boolean on = row.ref().equals(playing);
            final boolean isPicked = index == picked && pickedSection == SoundfoundryPagePayload.TRACKS;
            if (on || isPicked) {
                SoundfoundryStandardSkin.fill(g, px + 10, ry - 2, pw - 20, rowH - 1, on ? c.playing() : c.picked());
            }
            final int ground = on ? c.playing() : isPicked ? c.picked() : c.base();
            if (on) {
                SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.SPEAKER, px + 16, ry, c.orange());
            } else {
                SoundfoundryStandardSkin.right(g, font, String.valueOf(index + 1),
                        px + SoundfoundryStandardLayout.NUMBER_X, ry, c.inkDim(), ground);
            }
            final int titleRoom = (artist ? artistX - 8 : right - 30) - (px + SoundfoundryStandardLayout.TITLE_X);
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, row.title(), titleRoom),
                    px + SoundfoundryStandardLayout.TITLE_X, ry, on ? c.orange() : c.ink(), ground);
            if (artist) {
                SoundfoundryStandardSkin.text(g, font, Texts.clip(font, row.artist(), right - 34 - artistX),
                        artistX, ry, c.inkDim(), ground);
            }
            SoundfoundryStandardSkin.right(g, font, clock(row.millis()), right, ry, c.inkDim(), ground);
            arrowFor(g, px + pw - SoundfoundryStandardLayout.ARROW_RIGHT, ry, row.state(), c);
        }
    }

    private void renderFoot(final GuiGraphics g, final Font font, final int x, final int fy,
                            @Nullable final SoundfoundryStates.Known state, final SoundfoundryStandardSkin.Colours c) {
        final int w = width;
        SoundfoundryStandardSkin.fill(g, x, fy, w, SoundfoundryStandardLayout.FOOT_H, c.foot());
        SoundfoundryStandardSkin.fill(g, x, fy, w, 1, c.line());
        final SoundfoundryStatePayload.Song song = current(state);
        final Rect cover = SoundfoundryStandardLayout.FOOT_COVER;
        if (song != null) {
            SoundfoundryStandardSkin.cover(g, font, host, x + cover.x(), fy + cover.y(), cover.w(),
                    song.album().isEmpty() ? song.title() : song.album(), song.cover());
            final int room = SoundfoundryStandardLayout.footTitleRoom(w);
            final String title = Texts.clip(font, song.title(), room - 12);
            final int tx = x + SoundfoundryStandardLayout.FOOT_TEXT_X;
            SoundfoundryStandardSkin.text(g, font, title, tx, fy + SoundfoundryStandardLayout.FOOT_TITLE_Y, c.ink(),
                    c.foot());
            if (state.state().status() != SoundfoundryStatePayload.STOPPED && !state.state().stream()) {
                SoundfoundryStandardSkin.arrow(g, tx + font.width(title) + 5,
                        fy + SoundfoundryStandardLayout.FOOT_TITLE_Y, c.orange());
            }
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, song.artist(), room), tx,
                    fy + SoundfoundryStandardLayout.FOOT_ARTIST_Y, c.inkDim(), c.foot());
        }
        final boolean shuffle = state != null && state.state().shuffle();
        final boolean repeat = state != null && state.state().repeat();
        final Rect shuffleButton = SoundfoundryStandardLayout.shuffleButton(w);
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.SHUFFLE, x + shuffleButton.x() + 2,
                fy + SoundfoundryStandardLayout.GLYPH_Y, shuffle ? c.orange() : c.inkDim());
        final Rect previous = SoundfoundryStandardLayout.previousButton(w);
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.PREVIOUS, x + previous.x() + 2,
                fy + SoundfoundryStandardLayout.GLYPH_Y, c.ink());
        final Rect play = SoundfoundryStandardLayout.playButton(w);
        SoundfoundryStandardSkin.fill(g, x + play.x(), fy + play.y(), play.w(), play.h(), c.ink());
        final boolean playing = state != null && state.state().status() == SoundfoundryStatePayload.PLAYING;
        if (playing) {
            SoundfoundryStandardSkin.fill(g, x + play.x() + 7, fy + play.y() + 4, 2, 10, c.knob());
            SoundfoundryStandardSkin.fill(g, x + play.x() + 11, fy + play.y() + 4, 2, 10, c.knob());
        } else {
            SoundfoundryStandardSkin.triangle(g, x + play.x() + 8, fy + play.y() + 4, 10, c.knob());
        }
        final Rect next = SoundfoundryStandardLayout.nextButton(w);
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.NEXT, x + next.x() + 3,
                fy + SoundfoundryStandardLayout.GLYPH_Y, c.ink());
        final Rect repeatButton = SoundfoundryStandardLayout.repeatButton(w);
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.REPEAT, x + repeatButton.x() + 2,
                fy + SoundfoundryStandardLayout.GLYPH_Y, repeat ? c.orange() : c.inkDim());
        final Rect position = SoundfoundryStandardLayout.position(w);
        final long total = state == null ? 0L : state.state().playing().millis();
        final double at = dragging == DRAG_POSITION ? dragValue
                : total <= 0 || state == null ? 0.0 : Math.min(1.0, state.position() / (double) total);
        final int bx = x + position.x();
        final int by = fy + SoundfoundryStandardLayout.POSITION_Y;
        SoundfoundryStandardSkin.right(g, font, clock(Math.round(at * total)), bx - 6,
                fy + SoundfoundryStandardLayout.TIME_Y, c.inkDim(), c.foot());
        SoundfoundryStandardSkin.fill(g, bx, by, position.w(), 3, c.track());
        final int filled = (int) Math.round(position.w() * at);
        SoundfoundryStandardSkin.fill(g, bx, by, filled, 3, c.orange());
        if (total > 0) {
            SoundfoundryStandardSkin.fill(g, bx + filled - 2, by - 2, 5, 7, c.ink());
        }
        SoundfoundryStandardSkin.text(g, font, clock(total), bx + position.w() + 6,
                fy + SoundfoundryStandardLayout.TIME_Y, c.inkDim(), c.foot());
        final int output = state == null ? SoundfoundryStatePayload.OUT_NONE : state.state().output();
        final Rect out = SoundfoundryStandardLayout.output(w);
        final boolean monitor = output == SoundfoundryStatePayload.OUT_MONITOR
                || output == SoundfoundryStatePayload.OUT_BOTH;
        final boolean speakers = output == SoundfoundryStatePayload.OUT_SPEAKERS
                || output == SoundfoundryStatePayload.OUT_BOTH;
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.MONITOR, x + out.x(), fy + out.y() + 2,
                monitor ? c.ink() : c.inkDim());
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.SPEAKER, x + out.x() + 12, fy + out.y() + 2,
                speakers ? c.ink() : c.inkDim());
        SoundfoundryStandardSkin.text(g, font, words(SoundfoundryStandardTexts.OUT), x + out.x() + 24,
                fy + out.y() + 2, c.inkDim(), c.foot());
        final Rect volume = SoundfoundryStandardLayout.volume(w);
        SoundfoundryStandardSkin.glyph(g, SoundfoundryStandardSkin.Glyph.SPEAKER, x + volume.x() - 12,
                fy + volume.y(), c.inkDim());
        final double level = dragging == DRAG_VOLUME ? dragValue
                : state == null ? SoundfoundryState.DEFAULT_VOLUME / (double) SoundfoundryState.MAX_VOLUME
                : state.state().volume() / (double) SoundfoundryState.MAX_VOLUME;
        SoundfoundryStandardSkin.fill(g, x + volume.x(), fy + volume.y() + 3, volume.w(), 3, c.track());
        SoundfoundryStandardSkin.fill(g, x + volume.x(), fy + volume.y() + 3, (int) Math.round(volume.w() * level),
                3, c.ink());
        if (Util.getMillis() < noticeUntil && song == null) {
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(notice), position.x() - 20),
                    x + SoundfoundryStandardLayout.FOOT_TEXT_X, fy + SoundfoundryStandardLayout.FOOT_TITLE_Y,
                    c.orange(), c.foot());
        } else if (Util.getMillis() < noticeUntil) {
            SoundfoundryStandardSkin.text(g, font, Texts.clip(font, GameText.resolve(notice),
                            SoundfoundryStandardLayout.footTitleRoom(w)), x + SoundfoundryStandardLayout.FOOT_TEXT_X,
                    fy + SoundfoundryStandardLayout.FOOT_ARTIST_Y + 12, c.orange(), c.foot());
        }
    }

    private static void arrowFor(final GuiGraphics g, final int ax, final int ay, final int state,
                                 final SoundfoundryStandardSkin.Colours c) {
        switch (state) {
            case SoundfoundryPagePayload.ON_DISK -> SoundfoundryStandardSkin.arrow(g, ax, ay, c.orange());
            case SoundfoundryPagePayload.STREAM -> SoundfoundryStandardSkin.arrow(g, ax, ay, c.arrowOff());
            case SoundfoundryPagePayload.COMING, SoundfoundryPagePayload.SENDING -> {
                // Something on its way blinks, the way a download in the store's list did.
                if (Util.getMillis() / 400L % 2L == 0L) {
                    SoundfoundryStandardSkin.arrow(g, ax, ay, c.orange());
                }
            }
            default -> {
                // One of the machine's own files needs no arrow.
            }
        }
    }

    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final int colour) {
        SoundfoundryStandardSkin.fill(g, x, y, w, 1, colour);
        SoundfoundryStandardSkin.fill(g, x, y + h - 1, w, 1, colour);
        SoundfoundryStandardSkin.fill(g, x, y, 1, h, colour);
        SoundfoundryStandardSkin.fill(g, x + w - 1, y, 1, h, colour);
    }

    /* ---------- clicks ---------- */

    private void clickedSidebar(final double lx, final double ly, final int button) {
        final int[] pages = {SoundfoundryPagePayload.HOME, SoundfoundryPagePayload.SEARCH,
                SoundfoundryPagePayload.LIBRARY};
        for (int i = 0; i < 3; i++) {
            if (SoundfoundryStandardLayout.nav(i).contains(lx, ly)) {
                open(pages[i], pages[i] == SoundfoundryPagePayload.SEARCH ? query.value() : "");
                typing = pages[i] == SoundfoundryPagePayload.SEARCH;
                return;
            }
        }
        final SoundfoundryPages.Known shown = SoundfoundryPages.of(host);
        final SoundfoundryPagePayload.Sidebar sidebar = shown == null ? SoundfoundryPagePayload.Sidebar.NONE
                : shown.page().sidebar();
        if (SoundfoundryStandardLayout.NEW_PLAYLIST.contains(lx, ly)) {
            send(SoundfoundryActionPayload.PLAYLIST_NEW, 0, 0L, List.of(newPlaylistName(sidebar)), List.of());
            return;
        }
        final List<String> playlists = playlistsOf(sidebar);
        for (int i = 0; i < Math.min(playlists.size(), SoundfoundryStandardLayout.playlistsShown(height)); i++) {
            if (SoundfoundryStandardLayout.playlist(i).contains(lx, ly)) {
                final String name = playlists.get(i);
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !name.equals(SoundfoundryPlaylists.LIKED)) {
                    openMenu(List.of(item(SoundfoundryStandardTexts.DELETE_PLAYLIST, () -> {
                        send(SoundfoundryActionPayload.PLAYLIST_DELETE, 0, 0L, List.of(name), List.of());
                        if (page == SoundfoundryPagePayload.PLAYLIST && arg.equals(name)) {
                            open(SoundfoundryPagePayload.HOME, "");
                        }
                    })));
                } else {
                    open(SoundfoundryPagePayload.PLAYLIST, name);
                }
                return;
            }
        }
        if (SoundfoundryStandardLayout.serverBox(height).contains(lx, ly) && sidebar.servers().size() > 1) {
            final List<ContextMenu.Item> items = new ArrayList<>();
            for (final SoundfoundryPagePayload.Server server : sidebar.servers()) {
                items.add(new ContextMenu.Item(GameText.resolve(SoundfoundryStandardTexts.SERVER_CHOICE.with(
                        server.name(), server.songs())), !server.id().equals(sidebar.chosen()),
                        () -> send(SoundfoundryActionPayload.PICK_SERVER, 0, 0L, List.of(server.id()), List.of())));
            }
            openMenu(items);
        }
    }

    private void clickedPage(final double lx, final double ly, final int button, final boolean wasTyping) {
        final SoundfoundryPages.Known shown = SoundfoundryPages.of(host);
        final SoundfoundryPagePayload known = shown != null && shown.is(page, arg) ? shown.page() : null;
        final Rect area = SoundfoundryStandardLayout.page(width, height);
        switch (page) {
            case SoundfoundryPagePayload.HOME -> clickedHome(lx, ly, area, known, button);
            case SoundfoundryPagePayload.ALBUM, SoundfoundryPagePayload.PLAYLIST -> {
                if (SoundfoundryStandardLayout.PLAY.contains(lx, ly)) {
                    playPage(SoundfoundryPagePayload.TRACKS, 0);
                } else if (SoundfoundryStandardLayout.SHUFFLE_ALBUM.contains(lx, ly)) {
                    final SoundfoundryStates.Known state = SoundfoundryStates.of(host);
                    send(SoundfoundryActionPayload.SHUFFLE, state != null && state.state().shuffle() ? 0 : 1);
                } else if (page == SoundfoundryPagePayload.ALBUM
                        && SoundfoundryStandardLayout.DOWNLOAD_ALBUM.contains(lx, ly)) {
                    send(SoundfoundryActionPayload.DOWNLOAD_ALBUM, 0, 0L, List.of(arg), List.of());
                } else {
                    clickedTable(lx, ly, area, known, SoundfoundryStandardLayout.TRACKS_Y,
                            SoundfoundryStandardLayout.TRACK_H, button);
                }
            }
            case SoundfoundryPagePayload.SEARCH -> {
                if (SoundfoundryStandardLayout.SEARCH_BOX.contains(lx, ly)) {
                    typing = true;
                } else {
                    typing = false;
                    if (wasTyping) {
                        query.commit();
                    }
                    clickedTable(lx, ly, area, known, SoundfoundryStandardLayout.SEARCH_ROWS_Y,
                            SoundfoundryStandardLayout.TRACK_H, button);
                }
            }
            case SoundfoundryPagePayload.LIBRARY -> clickedLibrary(lx, ly, area, known, button);
            case SoundfoundryPagePayload.CATALOG -> {
                if (known == null) {
                    return;
                }
                final int across = SoundfoundryStandardLayout.cardsAcross(area.w());
                final int rowH = SoundfoundryStandardLayout.CARD_ARTIST_Y - SoundfoundryStandardLayout.CARD_Y + 20;
                final int col = (int) ((lx - PAD) / SoundfoundryStandardLayout.CARD_STEP);
                final int row = (int) ((ly - SoundfoundryStandardLayout.SECTION_Y) / rowH);
                final double inX = lx - PAD - col * SoundfoundryStandardLayout.CARD_STEP;
                final int index = (row + scroll) * across + col;
                if (lx >= PAD && ly >= SoundfoundryStandardLayout.SECTION_Y && col < across
                        && inX < SoundfoundryStandardLayout.CARD && index < known.albums().size()) {
                    open(SoundfoundryPagePayload.ALBUM, known.albums().get(index).id());
                }
            }
            default -> {
                // Every page is handled above.
            }
        }
    }

    private void clickedHome(final double lx, final double ly, final Rect area,
                             @Nullable final SoundfoundryPagePayload known, final int button) {
        if (known == null) {
            return;
        }
        final int sy = SoundfoundryStandardLayout.SECTION_Y;
        if (!known.albums().isEmpty() && ly >= sy - 2 && ly < sy + 10 && lx >= area.w() - PAD - 60) {
            open(SoundfoundryPagePayload.CATALOG, "");
            return;
        }
        final int cy = SoundfoundryStandardLayout.CARD_Y;
        if (ly >= cy && ly < SoundfoundryStandardLayout.CARD_ARTIST_Y + 10) {
            final int col = (int) ((lx - PAD) / SoundfoundryStandardLayout.CARD_STEP);
            final double inX = lx - PAD - col * SoundfoundryStandardLayout.CARD_STEP;
            final int across = Math.min(known.albums().size(), SoundfoundryStandardLayout.cardsAcross(area.w()));
            if (lx >= PAD && col < across && inX < SoundfoundryStandardLayout.CARD) {
                open(SoundfoundryPagePayload.ALBUM, known.albums().get(col).id());
            }
            return;
        }
        final int rows = SoundfoundryStandardLayout.homeRows(area.h());
        final int row = SoundfoundryStandardLayout.rowAt(ly, SoundfoundryStandardLayout.LIST_ROW_Y + 2,
                SoundfoundryStandardLayout.LIST_ROW_STEP, rows);
        if (row < 0) {
            return;
        }
        final int second = SoundfoundryStandardLayout.secondColumn(area.w());
        final int section = lx >= second ? SoundfoundryPagePayload.DOWNLOADED : SoundfoundryPagePayload.NETWORK;
        final List<Row> list = known.section(section);
        if (row < list.size()) {
            rowClicked(section, row, list.get(row), button);
        }
    }

    private void clickedLibrary(final double lx, final double ly, final Rect area,
                                @Nullable final SoundfoundryPagePayload known, final int button) {
        final SoundfoundryPages.Known shown = SoundfoundryPages.of(host);
        final boolean server = shown != null && !shown.page().sidebar().servers().isEmpty();
        final int lift = server ? SoundfoundryStandardLayout.NO_BANNER : 0;
        final int buttons = SoundfoundryStandardLayout.BUTTONS_Y - lift;
        if (ly >= buttons && ly < buttons + SoundfoundryStandardLayout.BUTTON_H) {
            if (lx >= PAD && lx < PAD + SoundfoundryStandardLayout.IMPORT_W) {
                importSongs();
            } else if (server && picked >= 0 && known != null && picked < known.rows().size()
                    && lx >= PAD + SoundfoundryStandardLayout.IMPORT_W + 6
                    && lx < PAD + SoundfoundryStandardLayout.IMPORT_W + 6 + SoundfoundryStandardLayout.UPLOAD_W) {
                send(SoundfoundryActionPayload.UPLOAD, 0, 0L, List.of(known.rows().get(picked).ref()), List.of());
            }
            return;
        }
        clickedTable(lx, ly, area, known, SoundfoundryStandardLayout.LIST_HEAD_Y - lift + 16,
                SoundfoundryStandardLayout.LIST_H, button);
    }

    private void clickedTable(final double lx, final double ly, final Rect area,
                              @Nullable final SoundfoundryPagePayload known, final int top, final int rowH,
                              final int button) {
        if (known == null) {
            return;
        }
        final List<Row> rows = known.section(SoundfoundryPagePayload.TRACKS);
        final int shown = SoundfoundryStandardLayout.rowsFrom(area.h(), top, rowH);
        final int row = SoundfoundryStandardLayout.rowAt(ly, top, rowH, shown);
        final int from = Math.min(scroll, Math.max(0, rows.size() - shown));
        if (row < 0 || from + row >= rows.size()) {
            return;
        }
        final int index = from + row;
        final Row clicked = rows.get(index);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && lx >= area.w() - SoundfoundryStandardLayout.ARROW_RIGHT - 2
                && clicked.state() == SoundfoundryPagePayload.STREAM) {
            send(SoundfoundryActionPayload.DOWNLOAD, 0, 0L, List.of(clicked.ref()), List.of());
            return;
        }
        rowClicked(SoundfoundryPagePayload.TRACKS, index, clicked, button);
    }

    /* A row picked; picked again at once, played from; asked about, a menu of what can be done with it. */
    private void rowClicked(final int section, final int index, final Row row, final int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            picked = index;
            pickedSection = section;
            openMenu(rowItems(index, row));
            return;
        }
        final long now = Util.getMillis();
        final boolean again = index == lastRow && section == pickedSection && now - lastClickAt <= DOUBLE_CLICK;
        picked = index;
        pickedSection = section;
        lastRow = index;
        lastClickAt = now;
        if (again || section != SoundfoundryPagePayload.TRACKS) {
            playPage(section, index);
        }
    }

    private List<ContextMenu.Item> rowItems(final int index, final Row row) {
        final List<ContextMenu.Item> items = new ArrayList<>();
        items.add(item(row.liked() ? SoundfoundryStandardTexts.UNLIKE : SoundfoundryStandardTexts.LIKE,
                () -> send(SoundfoundryActionPayload.LIKE, 0, 0L, List.of(row.ref()), List.of())));
        final SoundfoundryPages.Known shown = SoundfoundryPages.of(host);
        final SoundfoundryPagePayload.Sidebar sidebar = shown == null ? SoundfoundryPagePayload.Sidebar.NONE
                : shown.page().sidebar();
        for (final String name : sidebar.playlists()) {
            if (!name.equals(SoundfoundryPlaylists.LIKED)) {
                items.add(new ContextMenu.Item(GameText.resolve(SoundfoundryStandardTexts.ADD_TO.with(name)), true,
                        () -> send(SoundfoundryActionPayload.PLAYLIST_ADD, 0, 0L, List.of(name, row.ref()),
                                List.of())));
            }
        }
        if (row.state() == SoundfoundryPagePayload.STREAM) {
            items.add(item(SoundfoundryStandardTexts.DOWNLOAD,
                    () -> send(SoundfoundryActionPayload.DOWNLOAD, 0, 0L, List.of(row.ref()), List.of())));
        }
        if (row.state() == SoundfoundryPagePayload.LOCAL && !sidebar.servers().isEmpty()) {
            items.add(item(SoundfoundryStandardTexts.UPLOAD,
                    () -> send(SoundfoundryActionPayload.UPLOAD, 0, 0L, List.of(row.ref()), List.of())));
        }
        if (page == SoundfoundryPagePayload.PLAYLIST) {
            items.add(item(SoundfoundryStandardTexts.REMOVE_FROM,
                    () -> send(SoundfoundryActionPayload.PLAYLIST_REMOVE, index, 0L, List.of(arg), List.of())));
        }
        return items;
    }

    private void clickedFoot(final double lx, final double ly) {
        final int w = width;
        final SoundfoundryStates.Known state = SoundfoundryStates.of(host);
        if (SoundfoundryStandardLayout.playButton(w).contains(lx, ly)) {
            playOrPause(state);
        } else if (SoundfoundryStandardLayout.previousButton(w).contains(lx, ly)) {
            send(SoundfoundryActionPayload.PREVIOUS, 0);
        } else if (SoundfoundryStandardLayout.nextButton(w).contains(lx, ly)) {
            send(SoundfoundryActionPayload.NEXT, 0);
        } else if (SoundfoundryStandardLayout.shuffleButton(w).contains(lx, ly)) {
            send(SoundfoundryActionPayload.SHUFFLE, state != null && state.state().shuffle() ? 0 : 1);
        } else if (SoundfoundryStandardLayout.repeatButton(w).contains(lx, ly)) {
            send(SoundfoundryActionPayload.REPEAT, state != null && state.state().repeat() ? 0 : 1);
        } else if (SoundfoundryStandardLayout.position(w).contains(lx, ly)) {
            dragging = DRAG_POSITION;
            dragValue = along(SoundfoundryStandardLayout.position(w), lx);
        } else if (SoundfoundryStandardLayout.volume(w).contains(lx, ly)) {
            dragging = DRAG_VOLUME;
            dragValue = along(SoundfoundryStandardLayout.volume(w), lx);
        }
    }

    /* ---------- what the machine is asked ---------- */

    private void playOrPause(@Nullable final SoundfoundryStates.Known state) {
        final int status = state == null ? SoundfoundryStatePayload.STOPPED : state.state().status();
        if (status == SoundfoundryStatePayload.STOPPED) {
            send(SoundfoundryActionPayload.PLAY, -1);
        } else {
            send(SoundfoundryActionPayload.PAUSE, 0);
        }
    }

    private void playPage(final int section, final int index) {
        send(SoundfoundryActionPayload.PLAY_PAGE, page, index, List.of(arg), List.of(section));
    }

    /* Songs from the player's own computer, picked in their own system's dialog, into the music folder. */
    private void importSongs() {
        MusicImporter.pick(host, "", false, new MusicImporter.IListener() {
            @Override
            public void progress(final int song, final int songs, final long sent, final long total) {
                importing = SoundfoundryStandardTexts.IMPORTING.with(song, songs,
                        total <= 0 ? 100 : Math.round(sent * 100.0 / total));
            }

            @Override
            public void finished(final Path file, final boolean ok, final Text message) {
                if (!ok) {
                    say(message);
                }
            }

            @Override
            public void done() {
                importing = null;
                browse();
            }
        });
    }

    private boolean edit(final int key, final int modifiers) {
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            switch (key) {
                case GLFW.GLFW_KEY_A -> query.selectAll();
                case GLFW.GLFW_KEY_C -> Minecraft.getInstance().keyboardHandler.setClipboard(
                        query.hasSelection() ? query.selectedText() : query.edit());
                case GLFW.GLFW_KEY_V -> {
                    for (final char c : Minecraft.getInstance().keyboardHandler.getClipboard().toCharArray()) {
                        if (c >= ' ' && c != 127) {
                            query.type(c);
                        }
                    }
                }
                default -> {
                    // Nothing else is bound with Control here.
                }
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                typing = false;
                query.commit();
                open(SoundfoundryPagePayload.SEARCH, query.value());
            }
            case GLFW.GLFW_KEY_ESCAPE -> typing = false;
            case GLFW.GLFW_KEY_BACKSPACE -> query.backspace();
            case GLFW.GLFW_KEY_DELETE -> query.delete();
            case GLFW.GLFW_KEY_LEFT -> query.left(shift);
            case GLFW.GLFW_KEY_RIGHT -> query.right(shift);
            case GLFW.GLFW_KEY_HOME -> query.home(shift);
            case GLFW.GLFW_KEY_END -> query.end(shift);
            default -> {
                // The search eats every other key while it takes what is typed.
            }
        }
        return true;
    }

    private void look() {
        nextLookAt = Util.getMillis() + LOOK_EVERY;
        send(SoundfoundryActionPayload.LOOK, SoundfoundryStates.revisionOf(host));
    }

    private void browse() {
        nextBrowseAt = Util.getMillis() + BROWSE_EVERY;
        PacketDistributor.sendToServer(new SoundfoundryBrowsePayload(host, page, arg));
    }

    private void send(final int action, final int index) {
        send(action, index, 0L, List.of(), List.of());
    }

    /* An action, and the page asked for again at once, since most of them change what it lists. */
    private void send(final int action, final int index, final long value, final List<String> paths,
                      final List<Integer> indexes) {
        PacketDistributor.sendToServer(new SoundfoundryActionPayload(host, action, index, value, paths, indexes));
        if (action != SoundfoundryActionPayload.LOOK) {
            browse();
        }
    }

    /* Says what the machine answered went wrong, once for each answer. */
    private void hear(@Nullable final SoundfoundryStates.Known state) {
        if (state == null || state.receivedAt() == heardAt) {
            return;
        }
        heardAt = state.receivedAt();
        if (!state.state().trouble().isEmpty()) {
            say(state.state().trouble());
        }
    }

    private void say(final Text message) {
        notice = message;
        noticeUntil = Util.getMillis() + NOTICE_FOR;
    }

    private void openMenu(final List<ContextMenu.Item> items) {
        menu.open(items, mouseX, mouseY, lastX, lastY, width, height);
    }

    /* ---------- small things ---------- */

    private int maxScroll() {
        final SoundfoundryPages.Known shown = SoundfoundryPages.of(host);
        if (shown == null || !shown.is(page, arg)) {
            return 0;
        }
        final Rect area = SoundfoundryStandardLayout.page(width, height);
        if (page == SoundfoundryPagePayload.CATALOG) {
            final int across = SoundfoundryStandardLayout.cardsAcross(area.w());
            return Math.max(0, (shown.page().albums().size() + across - 1) / across - 1);
        }
        return Math.max(0, shown.page().rows().size() - 1);
    }

    private static double along(final Rect bar, final double lx) {
        return Math.clamp((lx - bar.x()) / Math.max(1.0, bar.w()), 0.0, 1.0);
    }

    @Nullable
    private static SoundfoundryStatePayload.Song current(@Nullable final SoundfoundryStates.Known state) {
        if (state == null || state.state().current() < 0 || state.state().current() >= state.songs().size()) {
            return null;
        }
        return state.songs().get(state.state().current());
    }

    /* The song the machine is on, as its list names it, while it plays or is paused; empty otherwise. */
    private static String playingRef(@Nullable final SoundfoundryStates.Known state) {
        final SoundfoundryStatePayload.Song song = current(state);
        return song == null || state.state().status() == SoundfoundryStatePayload.STOPPED ? "" : song.path();
    }

    @Nullable
    private static SoundfoundryPagePayload.Server chosen(final SoundfoundryPagePayload.Sidebar sidebar) {
        for (final SoundfoundryPagePayload.Server server : sidebar.servers()) {
            if (server.id().equals(sidebar.chosen())) {
                return server;
            }
        }
        return null;
    }

    private static List<String> playlistsOf(final SoundfoundryPagePayload.Sidebar sidebar) {
        return sidebar.playlists().isEmpty() ? List.of(SoundfoundryPlaylists.LIKED) : sidebar.playlists();
    }

    /* A playlist's name as the sidebar shows it: the liked songs in the player's own language. */
    private static String shownName(final String name) {
        return name.equals(SoundfoundryPlaylists.LIKED) ? words(SoundfoundryStandardTexts.LIKED) : name;
    }

    /* The first of "My Playlist #n" no playlist is called yet. */
    private static String newPlaylistName(final SoundfoundryPagePayload.Sidebar sidebar) {
        for (int n = 1; ; n++) {
            final String name = GameText.resolve(SoundfoundryStandardTexts.NEW_PLAYLIST.with(n));
            if (sidebar.playlists().stream().noneMatch(taken -> taken.equalsIgnoreCase(name))) {
                return name;
            }
        }
    }

    private static String coverName(final Row row) {
        return row.album().isEmpty() ? row.title() : row.album();
    }

    /* The greeting of the home page, by the time of day of the world the machine is in. */
    private static TextKey greeting() {
        final Minecraft mc = Minecraft.getInstance();
        final long time = mc.level == null ? 0L : Math.floorMod(mc.level.getDayTime(), 24_000L);
        // Day zero of the game's clock is six in the morning, so a quarter of the day is noon.
        if (time < 6_000L) {
            return SoundfoundryStandardTexts.MORNING;
        }
        return time < 12_000L ? SoundfoundryStandardTexts.AFTERNOON : SoundfoundryStandardTexts.EVENING;
    }

    private static String clock(final long millis) {
        final long seconds = Math.max(0L, millis) / 1000L;
        return seconds / 60L + ":" + (seconds % 60L < 10L ? "0" : "") + seconds % 60L;
    }

    private static ContextMenu.Item item(final TextKey label, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), true, action);
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
