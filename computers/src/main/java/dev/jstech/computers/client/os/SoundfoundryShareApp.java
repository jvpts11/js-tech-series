/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.SongSearch;
import dev.jstech.computers.client.audio.SoundfoundryShares;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.computers.gui.layout.SoundfoundryShareLayout;
import dev.jstech.computers.operation.payload.SoundfoundryShareActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryShareStatePayload;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.logic.TextEditState;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * Soundfoundry's sharing window, the one NET opens: songs searched for over the network, among the songs the other
 * computers running Soundfoundry share and the server's catalogue, sold by Voidsoft Music; the songs on their way in,
 * each at the speed of the slowest cable it has to come over; and the songs this computer shares. It wears the
 * player's skin, the same on every desktop.
 *
 * <p>Everything it lists is the machine's, asked for every second; what is the window's own is the tab it shows, what
 * is typed in the search, which rows are picked and how far each list is scrolled.
 */
public final class SoundfoundryShareApp implements IDesktopApp {

    private final BlockPos host;
    private final TextEditState query = new TextEditState(SongSearch.MAX_QUERY);
    private final List<Rect> tabs = new ArrayList<>();
    private boolean typing;
    private int tab = TAB_SEARCH;
    private int pickedFound = -1;
    private int pickedDownload = -1;
    private int foundScroll;
    private int downloadScroll;
    private int sharedScroll;
    private boolean addWhenDone = true;
    private boolean shaded;
    private boolean searched;
    private long nextLookAt;
    /** When the answer whose trouble was last said came, so each is said once. */
    private long heardAt = -1L;
    private Text notice = Text.EMPTY;
    private long noticeUntil;
    private boolean focused = true;
    private int pressedControl = DesktopWindow.BUTTON_NONE;
    private int lastRow = -1;
    private long lastClickAt;
    private int lastX;
    private int lastY;
    private int mouseX;
    private int mouseY;

    /** The key the window goes by, which is also the id its factory is registered under. */
    public static final String KEY = JsComputers.MODID + ":soundfoundry/share";
    public static final int TAB_SEARCH = 0;
    public static final int TAB_DOWNLOADS = 1;
    public static final int TAB_SHARED = 2;
    private static final int WIDTH = SoundfoundryShareLayout.WIDTH;
    private static final long LOOK_EVERY = 1000L;
    private static final long DOUBLE_CLICK = 300L;
    private static final long NOTICE_FOR = 4000L;
    private static final double MEGABYTE = 1024.0 * 1024.0;

    public SoundfoundryShareApp(final BlockPos host) {
        this.host = host;
        look();
    }

    @Override
    public String title() {
        return GameText.resolve(SoundfoundryAppTexts.SHARE_TITLE.with(ProgramClient.nameOf(Programs.SOUNDFOUNDRY)));
    }

    @Override
    public ResourceLocation iconId() {
        return Programs.SOUNDFOUNDRY;
    }

    @Override
    public int defaultWidth() {
        return WIDTH;
    }

    @Override
    public int defaultHeight() {
        return shaded ? SoundfoundryLayout.SHADE_H : SoundfoundryShareLayout.HEIGHT;
    }

    @Override
    public boolean drawsOwnFrame() {
        return true;
    }

    @Override
    public int frameControlAt(final int x, final int y) {
        if (SoundfoundryLayout.minimize(WIDTH).contains(x, y)) {
            return DesktopWindow.BUTTON_MINIMIZE;
        }
        if (SoundfoundryLayout.close(WIDTH).contains(x, y)) {
            return DesktopWindow.BUTTON_CLOSE;
        }
        return DesktopWindow.BUTTON_NONE;
    }

    @Override
    public boolean frameDragAt(final int x, final int y) {
        return y >= 0 && y < SoundfoundryLayout.SHADE_H && x >= 0 && x < WIDTH
                && !SoundfoundryLayout.minimize(WIDTH).contains(x, y)
                && !SoundfoundryLayout.shade(WIDTH).contains(x, y)
                && !SoundfoundryLayout.close(WIDTH).contains(x, y);
    }

    @Override
    public void frameState(final boolean isFocused, final int pressed) {
        this.focused = isFocused;
        this.pressedControl = pressed;
    }

    @Override
    public void onRestored() {
        look();
        if (searched) {
            search();
        }
    }

    @Override
    public String saveState() {
        return tab + (addWhenDone ? "1" : "0") + (shaded ? "1" : "0") + (searched ? "1" : "0") + ":" + query.value();
    }

    @Override
    public void restoreState(final String state) {
        if (state.length() < 5 || state.charAt(4) != ':') {
            return;
        }
        tab = Math.clamp(state.charAt(0) - '0', TAB_SEARCH, TAB_SHARED);
        addWhenDone = state.charAt(1) == '1';
        shaded = state.charAt(2) == '1';
        searched = state.charAt(3) == '1';
        query.sync(state.substring(5));
        if (searched) {
            search();
        }
    }

    @Override
    public boolean wantsEscape() {
        return typing;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mx, final int my, final float partialTick) {
        this.lastX = x;
        this.lastY = y;
        this.mouseX = mx;
        this.mouseY = my;
        if (Util.getMillis() >= nextLookAt) {
            look();
        }
        final SoundfoundryShares.Known known = SoundfoundryShares.of(host);
        hear(known);
        SoundfoundrySkin.plate(g, x, y, WIDTH, defaultHeight());
        SoundfoundrySkin.bar(g, font, x, y, WIDTH, words(SoundfoundryAppTexts.SHARE_BAR), focused, pressedIndex());
        if (shaded) {
            return;
        }
        renderTabs(g, font, x, y, known);
        switch (tab) {
            case TAB_DOWNLOADS -> renderDownloads(g, font, x, y, known);
            case TAB_SHARED -> renderShared(g, font, x, y, known);
            default -> renderSearch(g, font, x, y, known);
        }
        renderStatus(g, font, x, y, known);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mx, final double my, final int button) {
        final double lx = mx - lastX;
        final double ly = my - lastY;
        if (SoundfoundryLayout.shade(WIDTH).contains(lx, ly)) {
            shaded = !shaded;
            return;
        }
        if (shaded || ly < SoundfoundryLayout.SHADE_H) {
            return;
        }
        typing = tab == TAB_SEARCH && SoundfoundryShareLayout.QUERY.contains(lx, ly);
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).contains(lx, ly)) {
                tab = i;
                look();
                return;
            }
        }
        switch (tab) {
            case TAB_DOWNLOADS -> clickedDownloads(lx, ly);
            case TAB_SHARED -> {
                // A list to read; nothing on it does anything.
            }
            default -> clickedSearch(lx, ly);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final int step = -(int) Math.signum(delta) * 3;
        final SoundfoundryShares.Known known = SoundfoundryShares.of(host);
        switch (tab) {
            case TAB_DOWNLOADS -> downloadScroll = clampScroll(downloadScroll + step / 3,
                    known == null ? 0 : known.state().downloads().size(), SoundfoundryShareLayout.LIST_DOWNLOADS);
            case TAB_SHARED -> sharedScroll = clampScroll(sharedScroll + step,
                    known == null ? 0 : known.shared().size(), SoundfoundryShareLayout.LIST_ROWS);
            default -> {
                if (!SoundfoundryShareLayout.RESULTS.contains(mouseX - lastX, mouseY - lastY)) {
                    return false;
                }
                foundScroll = clampScroll(foundScroll + step, known == null ? 0 : known.found().size(),
                        SoundfoundryShareLayout.ROWS);
            }
        }
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
        if (typing) {
            return edit(key, modifiers);
        }
        final SoundfoundryShares.Known known = SoundfoundryShares.of(host);
        if (tab == TAB_SEARCH && known != null) {
            final int found = known.found().size();
            switch (key) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    if (pickedFound >= 0 && pickedFound < found) {
                        download(known.found().get(pickedFound));
                    }
                }
                case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                    if (found > 0) {
                        pickedFound = Math.clamp(pickedFound + (key == GLFW.GLFW_KEY_UP ? -1 : 1), 0, found - 1);
                        foundScroll = reveal(pickedFound, foundScroll, SoundfoundryShareLayout.ROWS);
                    }
                }
                default -> {
                    return false;
                }
            }
            return true;
        }
        if (tab == TAB_DOWNLOADS && key == GLFW.GLFW_KEY_DELETE) {
            cancelPicked();
            return true;
        }
        return false;
    }

    /* For the tests: where things are, and what the window holds. */

    /** The middle of a part of the window, by its rectangle in the layout, in desktop pixels. */
    public int[] point(final Rect part) {
        return new int[] {lastX + part.x() + part.w() / 2, lastY + part.y() + part.h() / 2};
    }

    /** The middle of a visible row of what a search found. */
    public int[] foundPoint(final int row) {
        return new int[] {lastX + SoundfoundryShareLayout.NAME_X + 40,
                lastY + SoundfoundryShareLayout.ROW_TOP + row * SoundfoundryShareLayout.ROW_H + 4};
    }

    /** The middle of a tab, as it was last drawn, or null before it was. */
    @Nullable
    public int[] tabPoint(final int index) {
        return index < tabs.size() ? point(tabs.get(index)) : null;
    }

    /** The tab it shows. */
    public int tab() {
        return tab;
    }

    /** Whether the search takes what is typed. */
    public boolean typing() {
        return typing;
    }

    /** The found song picked, or -1. */
    public int pickedFound() {
        return pickedFound;
    }

    private void clickedSearch(final double lx, final double ly) {
        final SoundfoundryShares.Known known = SoundfoundryShares.of(host);
        final List<SoundfoundryShareStatePayload.Found> found = known == null ? List.of() : known.found();
        if (SoundfoundryShareLayout.SEARCH.contains(lx, ly)) {
            search();
            return;
        }
        final Rect box = SoundfoundryShareLayout.ADD_WHEN_DONE;
        if (lx >= box.x() && lx < SoundfoundryShareLayout.RESULTS.right() && ly >= box.y() && ly < box.bottom()) {
            addWhenDone = !addWhenDone;
            return;
        }
        if (SoundfoundryShareLayout.DOWNLOAD.contains(lx, ly)) {
            if (pickedFound >= 0 && pickedFound < found.size()) {
                download(found.get(pickedFound));
            }
            return;
        }
        final int row = SoundfoundryShareLayout.resultAt(lx, ly);
        if (row < 0) {
            return;
        }
        final int index = foundScroll + row;
        if (index >= found.size()) {
            pickedFound = -1;
            return;
        }
        final long now = Util.getMillis();
        final boolean twice = index == lastRow && now - lastClickAt < DOUBLE_CLICK;
        lastRow = index;
        lastClickAt = now;
        pickedFound = index;
        if (twice) {
            download(found.get(index));
        }
    }

    private void clickedDownloads(final double lx, final double ly) {
        final SoundfoundryShares.Known known = SoundfoundryShares.of(host);
        final int count = known == null ? 0 : known.state().downloads().size();
        if (SoundfoundryShareLayout.CANCEL.contains(lx, ly)) {
            cancelPicked();
            return;
        }
        if (SoundfoundryShareLayout.CLEAR.contains(lx, ly)) {
            send(SoundfoundryShareActionPayload.of(host, SoundfoundryShareActionPayload.CLEAR, 0));
            pickedDownload = -1;
            return;
        }
        final int row = SoundfoundryShareLayout.listDownloadAt(lx, ly);
        if (row >= 0) {
            final int index = downloadScroll + row;
            pickedDownload = index < count ? index : -1;
        }
    }

    private void cancelPicked() {
        if (pickedDownload >= 0) {
            send(new SoundfoundryShareActionPayload(host, SoundfoundryShareActionPayload.REMOVE, 0, 0L, "",
                    List.of(pickedDownload)));
            pickedDownload = -1;
        }
    }

    /* The keys of the search while it takes what is typed: the ones every field on every desktop answers to. */
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
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> search();
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

    private void search() {
        typing = false;
        query.commit();
        searched = true;
        foundScroll = 0;
        pickedFound = -1;
        send(new SoundfoundryShareActionPayload(host, SoundfoundryShareActionPayload.SEARCH, 0, 0L, query.value(),
                List.of()));
    }

    private void download(final SoundfoundryShareStatePayload.Found found) {
        send(new SoundfoundryShareActionPayload(host, SoundfoundryShareActionPayload.DOWNLOAD, addWhenDone ? 1 : 0,
                found.source(), found.path(), List.of()));
    }

    private void look() {
        nextLookAt = Util.getMillis() + LOOK_EVERY;
        send(SoundfoundryShareActionPayload.of(host, SoundfoundryShareActionPayload.LOOK, tab == TAB_SHARED ? 1 : 0));
    }

    private void send(final SoundfoundryShareActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    /* Says what the machine answered about the last thing done, once. */
    private void hear(@Nullable final SoundfoundryShares.Known known) {
        if (known == null || known.receivedAt() == heardAt) {
            return;
        }
        heardAt = known.receivedAt();
        if (!known.state().trouble().isEmpty()) {
            notice = known.state().trouble();
            noticeUntil = Util.getMillis() + NOTICE_FOR;
        }
    }

    private int pressedIndex() {
        return pressedControl == DesktopWindow.BUTTON_MINIMIZE ? 0
                : pressedControl == DesktopWindow.BUTTON_CLOSE ? 2 : -1;
    }

    private void renderTabs(final GuiGraphics g, final Font font, final int x, final int y,
                            @Nullable final SoundfoundryShares.Known known) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final int downloads = known == null ? 0 : known.state().downloads().size();
        final int sharing = known == null ? 0 : known.state().sharing();
        final List<String> labels = List.of(words(SoundfoundryAppTexts.TAB_SEARCH),
                GameText.resolve(SoundfoundryAppTexts.TAB_DOWNLOADS.with(downloads)),
                GameText.resolve(SoundfoundryAppTexts.TAB_SHARED.with(sharing)));
        tabs.clear();
        tabs.addAll(SoundfoundryShareLayout.tabs(labels, font::width));
        for (int i = 0; i < tabs.size(); i++) {
            final Rect r = tabs.get(i);
            final boolean on = i == tab;
            SoundfoundrySkin.button(g, x + r.x(), y + r.y(), r.w(), r.h(), on, on);
            final String label = labels.get(i);
            Draw.text(g, font, label, x + r.x() + (r.w() - font.width(label)) / 2, y + r.y() + 2,
                    on ? c.amber() : c.ink(), on ? c.steelPressed() : c.steel());
        }
    }

    private void renderSearch(final GuiGraphics g, final Font font, final int x, final int y,
                              @Nullable final SoundfoundryShares.Known known) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        renderQuery(g, font, x, y);
        final Rect search = SoundfoundryShareLayout.SEARCH;
        SoundfoundrySkin.labelButton(g, font, x + search.x(), y + search.y(), search.w(), search.h(),
                words(SoundfoundryAppTexts.TAB_SEARCH), false, false);
        final Rect results = SoundfoundryShareLayout.RESULTS;
        SoundfoundrySkin.lcd(g, x + results.x(), y + results.y(), results.w(), results.h());
        final Rect header = SoundfoundryShareLayout.HEADER;
        SoundfoundrySkin.fill(g, x + header.x(), y + header.y(), header.w(), header.h(), c.header());
        headerWord(g, font, x + SoundfoundryShareLayout.NAME_X, y, SoundfoundryAppTexts.COLUMN_NAME);
        headerWord(g, font, x + SoundfoundryShareLayout.SIZE_X, y, SoundfoundryAppTexts.COLUMN_SIZE);
        headerWord(g, font, x + SoundfoundryShareLayout.FROM_X, y, SoundfoundryAppTexts.COLUMN_FROM);
        headerWord(g, font, x + SoundfoundryShareLayout.LINK_X, y, SoundfoundryAppTexts.COLUMN_LINK);
        final List<SoundfoundryShareStatePayload.Found> found = known == null ? List.of() : known.found();
        foundScroll = clampScroll(foundScroll, found.size(), SoundfoundryShareLayout.ROWS);
        final DataTier own = known == null ? null : SoundfoundryShareStatePayload.tierOf(known.state().ownLink());
        for (int row = 0; row < SoundfoundryShareLayout.ROWS; row++) {
            final int index = foundScroll + row;
            if (index >= found.size()) {
                break;
            }
            renderFound(g, font, x, y + SoundfoundryShareLayout.ROW_TOP + row * SoundfoundryShareLayout.ROW_H,
                    found.get(index), index == pickedFound, own);
        }
        if (found.isEmpty()) {
            final String line = words(searched ? SoundfoundryAppTexts.NOTHING_FOUND : SoundfoundryAppTexts.HINT);
            SoundfoundrySkin.lit(g, font, line, x + results.x() + (results.w() - font.width(line)) / 2,
                    y + results.y() + results.h() / 2 + 2, c.inkDim());
        }
        final Rect download = SoundfoundryShareLayout.DOWNLOAD;
        SoundfoundrySkin.labelButton(g, font, x + download.x(), y + download.y(), download.w(), download.h(),
                words(SoundfoundryAppTexts.DOWNLOAD), pickedFound >= 0, pickedFound < 0);
        final Rect box = SoundfoundryShareLayout.ADD_WHEN_DONE;
        SoundfoundrySkin.checkbox(g, x + box.x(), y + box.y(), addWhenDone);
        SoundfoundrySkin.engraved(g, font, words(SoundfoundryAppTexts.ADD_WHEN_DONE),
                x + SoundfoundryShareLayout.ADD_WHEN_DONE_TEXT_X, y + box.y() + 1, c.inkDim());
        SoundfoundrySkin.engraved(g, font, words(SoundfoundryAppTexts.DOWNLOADS), x + 8,
                y + SoundfoundryShareLayout.DOWNLOADS_LABEL_Y, c.ink());
        final Rect rule = SoundfoundryShareLayout.RULE;
        SoundfoundrySkin.fill(g, x + rule.x(), y + rule.y(), rule.w(), rule.h(), c.rule());
        final List<SoundfoundryShareStatePayload.Fetch> shown = latest(known);
        for (int i = 0; i < shown.size(); i++) {
            renderFetch(g, font, x, y + SoundfoundryShareLayout.DOWNLOAD_TOP + i * SoundfoundryShareLayout.DOWNLOAD_H,
                    shown.get(i), c.iron());
        }
    }

    /* The search, as a display that shows what is typed with its caret while it takes it. */
    private void renderQuery(final GuiGraphics g, final Font font, final int x, final int y) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final Rect box = SoundfoundryShareLayout.QUERY;
        SoundfoundrySkin.lcd(g, x + box.x(), y + box.y(), box.w(), box.h());
        final String text = typing ? query.edit() : query.value();
        final int room = box.w() - 8;
        final int caret = typing ? Math.min(query.caret(), text.length()) : text.length();
        int start = 0;
        while (start < caret && font.width(text.substring(start, caret)) > room) {
            start++;
        }
        final String shown = Texts.clip(font, text.substring(start), room);
        final int left = x + box.x() + 4;
        final int top = y + box.y() + 3;
        SoundfoundrySkin.lit(g, font, shown, left, top, c.amber());
        if (typing && Util.getMillis() / 500L % 2L == 0L) {
            SoundfoundrySkin.fill(g, left + font.width(text.substring(start, caret)), top, 1, 8, c.amber());
        }
    }

    private void renderFound(final GuiGraphics g, final Font font, final int x, final int ry,
                             final SoundfoundryShareStatePayload.Found found, final boolean picked,
                             @Nullable final DataTier own) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final int ground = picked ? c.rowPicked() : c.lcd();
        if (picked) {
            SoundfoundrySkin.fill(g, x + 7, ry - 1, SoundfoundryShareLayout.RESULTS.w() - 2, 10, c.rowPicked());
        }
        final boolean store = found.source() == SongDownload.FROM_CATALOG;
        final String name = found.artist().isEmpty() ? found.title()
                : GameText.resolve(SoundfoundryAppTexts.BY.with(found.artist(), found.title()));
        Draw.text(g, font, Texts.clip(font, name, SoundfoundryShareLayout.NAME_ROOM),
                x + SoundfoundryShareLayout.NAME_X, ry, picked ? c.amber() : c.listInk(), ground);
        Draw.text(g, font, megabytes(found.bytes()), x + SoundfoundryShareLayout.SIZE_X, ry, c.listInk(), ground);
        final int fromRoom = SoundfoundryShareLayout.LINK_X - SoundfoundryShareLayout.FROM_X - 4;
        if (store) {
            SoundfoundrySkin.smallAnvil(g, x + SoundfoundryShareLayout.FROM_X, ry + 1);
            Draw.text(g, font, Texts.clip(font, words(SoundfoundryAppTexts.STORE), fromRoom - 10),
                    x + SoundfoundryShareLayout.FROM_X + 10, ry, c.amberMid(), ground);
        } else {
            Draw.text(g, font, Texts.clip(font, found.from().toUpperCase(Locale.ROOT), fromRoom),
                    x + SoundfoundryShareLayout.FROM_X, ry, c.listInk(), ground);
        }
        final DataTier link = store ? own : SoundfoundryShareStatePayload.tierOf(found.link());
        final String linkName = store ? words(SoundfoundryAppTexts.LINK_CATALOG) : linkName(link);
        final int linkRoom = SoundfoundryShareLayout.SIGNAL_X - SoundfoundryShareLayout.LINK_X - 4;
        Draw.text(g, font, Texts.clip(font, linkName, linkRoom), x + SoundfoundryShareLayout.LINK_X, ry,
                store ? c.amberMid() : c.listInk(), ground);
        SoundfoundrySkin.signal(g, x + SoundfoundryShareLayout.SIGNAL_X, ry, SoundfoundryShareLayout.signalOf(link));
    }

    /* One song on its way, or one that came or never will: its name, how far it has got, and where it comes from. */
    private void renderFetch(final GuiGraphics g, final Font font, final int x, final int top,
                             final SoundfoundryShareStatePayload.Fetch fetch, final int ground) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final SongDownload.Status status = fetch.statusValue();
        final boolean finished = status.finished();
        final String state = switch (status) {
            case DONE -> words(SoundfoundryAppTexts.DONE);
            case FAILED -> words(SoundfoundryAppTexts.FAILED);
            case WAITING -> words(SoundfoundryAppTexts.WAITING);
            case RUNNING -> fetch.millisLeft() < 0 ? ""
                    : GameText.resolve(SoundfoundryAppTexts.LEFT.with(clockText(fetch.millisLeft())));
        };
        final int stateInk = switch (status) {
            case DONE -> c.inkDim();
            case FAILED, WAITING -> c.amberMid();
            case RUNNING -> c.amber();
        };
        final int right = x + SoundfoundryShareLayout.RIGHT;
        Draw.text(g, font, state, right - font.width(state), top, stateInk, ground);
        final int nameRoom = SoundfoundryShareLayout.RIGHT - SoundfoundryShareLayout.BAR_X - font.width(state) - 8;
        Draw.text(g, font, Texts.clip(font, fetch.name(), nameRoom), x + SoundfoundryShareLayout.BAR_X, top,
                finished ? c.inkDim() : c.listInk(), ground);
        SoundfoundrySkin.progress(g, x + SoundfoundryShareLayout.BAR_X, top + 12, SoundfoundryShareLayout.BAR_W,
                SoundfoundryShareLayout.BAR_H, fetch.bytes() <= 0 ? 1.0 : fetch.done() / (double) fetch.bytes(),
                finished);
        final String amount = GameText.resolve(SoundfoundryAppTexts.AMOUNT.with(
                String.format(Locale.ROOT, "%.1f", fetch.done() / MEGABYTE),
                String.format(Locale.ROOT, "%.1f", fetch.bytes() / MEGABYTE)));
        Draw.text(g, font, amount, x + SoundfoundryShareLayout.AMOUNT_X, top + 12, c.inkDim(), ground);
        final String via;
        if (status == SongDownload.Status.FAILED && !fetch.trouble().isEmpty()) {
            via = GameText.resolve(fetch.trouble());
        } else if (fetch.from().isEmpty()) {
            via = words(SoundfoundryAppTexts.FROM_STORE);
        } else {
            final DataTier link = SoundfoundryShareStatePayload.tierOf(fetch.link());
            final String from = fetch.from().toUpperCase(Locale.ROOT);
            via = GameText.resolve(link == null ? SoundfoundryAppTexts.FROM_HOST.with(from)
                    : SoundfoundryAppTexts.FROM_VIA.with(from, linkName(link)));
        }
        final int viaRoom = SoundfoundryShareLayout.RIGHT - SoundfoundryShareLayout.AMOUNT_X - font.width(amount) - 8;
        final String shownVia = Texts.clip(font, via, viaRoom);
        Draw.text(g, font, shownVia, right - font.width(shownVia), top + 12,
                status == SongDownload.Status.FAILED ? c.amberMid() : c.inkDim(), ground);
    }

    private void renderDownloads(final GuiGraphics g, final Font font, final int x, final int y,
                                 @Nullable final SoundfoundryShares.Known known) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final List<SoundfoundryShareStatePayload.Fetch> fetches = known == null ? List.of()
                : known.state().downloads();
        downloadScroll = clampScroll(downloadScroll, fetches.size(), SoundfoundryShareLayout.LIST_DOWNLOADS);
        if (pickedDownload >= fetches.size()) {
            pickedDownload = -1;
        }
        final Rect list = SoundfoundryShareLayout.LIST;
        for (int row = 0; row < SoundfoundryShareLayout.LIST_DOWNLOADS; row++) {
            final int index = downloadScroll + row;
            if (index >= fetches.size()) {
                break;
            }
            final int top = y + SoundfoundryShareLayout.listDownloadTop(row);
            final boolean picked = index == pickedDownload;
            if (picked) {
                SoundfoundrySkin.fill(g, x + list.x(), top - 2, list.w(), SoundfoundryShareLayout.DOWNLOAD_H - 1,
                        c.rowPicked());
            }
            renderFetch(g, font, x, top, fetches.get(index), picked ? c.rowPicked() : c.iron());
        }
        if (fetches.isEmpty()) {
            final String line = words(SoundfoundryAppTexts.NO_DOWNLOADS);
            SoundfoundrySkin.engraved(g, font, line, x + list.x() + (list.w() - font.width(line)) / 2,
                    y + list.y() + list.h() / 2, c.inkDim());
        }
        final boolean anyFinished = fetches.stream().anyMatch(fetch -> fetch.statusValue().finished());
        final Rect cancel = SoundfoundryShareLayout.CANCEL;
        SoundfoundrySkin.labelButton(g, font, x + cancel.x(), y + cancel.y(), cancel.w(), cancel.h(),
                words(SoundfoundryAppTexts.CANCEL), false, pickedDownload < 0);
        final Rect clear = SoundfoundryShareLayout.CLEAR;
        SoundfoundrySkin.labelButton(g, font, x + clear.x(), y + clear.y(), clear.w(), clear.h(),
                words(SoundfoundryAppTexts.CLEAR_DONE), false, !anyFinished);
    }

    private void renderShared(final GuiGraphics g, final Font font, final int x, final int y,
                              @Nullable final SoundfoundryShares.Known known) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final Rect list = SoundfoundryShareLayout.LIST;
        SoundfoundrySkin.lcd(g, x + list.x(), y + list.y(), list.w(), list.h());
        final List<String> shared = known == null ? List.of() : known.shared();
        sharedScroll = clampScroll(sharedScroll, shared.size(), SoundfoundryShareLayout.LIST_ROWS);
        for (int row = 0; row < SoundfoundryShareLayout.LIST_ROWS; row++) {
            final int index = sharedScroll + row;
            if (index >= shared.size()) {
                break;
            }
            SoundfoundrySkin.lit(g, font, Texts.clip(font, shared.get(index), list.w() - 8), x + list.x() + 4,
                    y + list.y() + 4 + row * SoundfoundryShareLayout.ROW_H, c.listInk());
        }
        final String folder = known == null ? "" : known.state().sharedFolder();
        if (shared.isEmpty() && known != null) {
            final String line = Texts.clip(font, GameText.resolve(SoundfoundryAppTexts.NOTHING_SHARED.with(folder)),
                    list.w() - 8);
            SoundfoundrySkin.lit(g, font, line, x + list.x() + (list.w() - font.width(line)) / 2,
                    y + list.y() + list.h() / 2, c.inkDim());
        }
        if (known != null) {
            SoundfoundrySkin.engraved(g, font, Texts.clip(font,
                            GameText.resolve(SoundfoundryAppTexts.SHARED_FROM.with(folder)), list.w() - 4), x + 8,
                    y + SoundfoundryShareLayout.FOLDER_TEXT_Y, c.inkDim());
        }
    }

    /* The bar along the foot: how many songs it shares, what it reaches, and how many songs are on their way. */
    private void renderStatus(final GuiGraphics g, final Font font, final int x, final int y,
                              @Nullable final SoundfoundryShares.Known known) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final Rect bar = SoundfoundryShareLayout.STATUS;
        SoundfoundrySkin.fill(g, x + bar.x(), y + bar.y(), bar.w(), bar.h(), c.statusBar());
        if (known == null) {
            return;
        }
        final SoundfoundryShareStatePayload state = known.state();
        final int textY = y + SoundfoundryShareLayout.STATUS_TEXT_Y;
        final String sharing = GameText.resolve(state.sharing() == 1 ? SoundfoundryAppTexts.SHARING_ONE.text()
                : SoundfoundryAppTexts.SHARING.with(state.sharing()));
        Draw.text(g, font, sharing, x + bar.x() + 4, textY, c.inkDim(), c.statusBar());
        final long coming = state.downloads().stream().filter(fetch -> !fetch.statusValue().finished()).count();
        final String downloading = coming == 0 ? ""
                : GameText.resolve(SoundfoundryAppTexts.DOWNLOADING.with(coming));
        final int right = x + SoundfoundryShareLayout.RIGHT;
        Draw.text(g, font, downloading, right - font.width(downloading), textY, c.amber(), c.statusBar());
        final boolean telling = Util.getMillis() < noticeUntil;
        final String middle = telling ? GameText.resolve(notice) : reaches(state);
        final int room = bar.w() - font.width(sharing) - font.width(downloading) - 24;
        final String shown = Texts.clip(font, middle, room);
        Draw.text(g, font, shown, x + bar.x() + (bar.w() - font.width(shown)) / 2, textY,
                telling ? c.amber() : c.inkDim(), c.statusBar());
    }

    private static String reaches(final SoundfoundryShareStatePayload state) {
        final int peers = state.computers();
        if (state.store()) {
            return GameText.resolve(peers == 1 ? SoundfoundryAppTexts.REACHES_ONE.text()
                    : SoundfoundryAppTexts.REACHES.with(peers));
        }
        return GameText.resolve(peers == 1 ? SoundfoundryAppTexts.REACHES_PEER.text()
                : SoundfoundryAppTexts.REACHES_PEERS.with(peers));
    }

    /* The downloads shown under the search: those still to come first, then the latest to finish. */
    private static List<SoundfoundryShareStatePayload.Fetch> latest(@Nullable final SoundfoundryShares.Known known) {
        final List<SoundfoundryShareStatePayload.Fetch> shown = new ArrayList<>();
        if (known == null) {
            return shown;
        }
        final List<SoundfoundryShareStatePayload.Fetch> all = known.state().downloads();
        for (final SoundfoundryShareStatePayload.Fetch fetch : all) {
            if (shown.size() < SoundfoundryShareLayout.DOWNLOADS_SHOWN && !fetch.statusValue().finished()) {
                shown.add(fetch);
            }
        }
        for (int i = all.size() - 1; i >= 0 && shown.size() < SoundfoundryShareLayout.DOWNLOADS_SHOWN; i--) {
            if (all.get(i).statusValue().finished()) {
                shown.add(all.get(i));
            }
        }
        return shown;
    }

    private static void headerWord(final GuiGraphics g, final Font font, final int x, final int y, final TextKey word) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        Draw.text(g, font, words(word), x, y + SoundfoundryShareLayout.HEADER.y() + 2, c.ink(), c.header());
    }

    private static String linkName(@Nullable final DataTier tier) {
        if (tier == null) {
            return "";
        }
        return words(switch (tier) {
            case T1_ETHERNET -> SoundfoundryAppTexts.LINK_ETHERNET;
            case T2_HBW -> SoundfoundryAppTexts.LINK_HBW;
            case T3_FIBER -> SoundfoundryAppTexts.LINK_FIBER;
            case T4_VLDC -> SoundfoundryAppTexts.LINK_VLDC;
            case T6_QUANTUM -> SoundfoundryAppTexts.LINK_QUANTUM;
            case HPC -> SoundfoundryAppTexts.LINK_HPC;
            case CRAFTING -> SoundfoundryAppTexts.LINK_CRAFTING;
        });
    }

    private static String megabytes(final long bytes) {
        return GameText.resolve(SoundfoundryAppTexts.MEGABYTES.with(
                String.format(Locale.ROOT, "%.1f", Math.max(0.1, bytes / MEGABYTE))));
    }

    private static String clockText(final long millis) {
        final long seconds = (Math.max(0L, millis) + 999L) / 1000L;
        return seconds / 60L + ":" + String.format(Locale.ROOT, "%02d", seconds % 60L);
    }

    private static int clampScroll(final int scroll, final int count, final int rows) {
        return Math.max(0, Math.min(scroll, count - rows));
    }

    private static int reveal(final int index, final int scroll, final int rows) {
        if (index < scroll) {
            return index;
        }
        return index >= scroll + rows ? index - rows + 1 : scroll;
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }
}
