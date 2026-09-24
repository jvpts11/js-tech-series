/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.HelpViewerLayout;
import dev.jstech.computers.operation.payload.HelpPayload;
import dev.jstech.computers.operation.payload.RequestHelpPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The Help Viewer: one manual, read in a window.
 *
 * <p>What it shows is not a second body of text. It is the very pages {@code man} prints at a terminal, asked
 * of the machine through the same filter that decides what that machine can run, so the Help never teaches
 * something that is not there and a command that gains an option gains it here with nobody writing it twice.
 *
 * <p>The list on the left is grouped by what a person would be looking for rather than by where the mod keeps
 * things, a click on a row opens its page, and a click on a name under SEE ALSO goes there, with Backtrack to
 * come back. Search is {@code apropos}: what every page is for, narrowed to the word typed.
 */
public final class HelpViewerApp implements IDesktopApp {

    /** Every open window, so a machine's answer reaches the ones showing that machine. */
    private static final List<HelpViewerApp> OPEN = new ArrayList<>();

    private final BlockPos host;

    /** The list as the machine gave it, and the page it is showing, in this player's language. */
    private List<Shown> entries = List.of();
    private String page = "";
    private List<String> lines = List.of();

    /** What has been typed in the search field, which narrows the list to what answers to it. */
    private final StringBuilder searching = new StringBuilder();

    /** The pages opened before this one, newest last, which is what Backtrack walks. */
    private final List<String> back = new ArrayList<>();

    private int scroll;
    private int listScroll;
    private OsSkin skin = OsSkin.fallback();
    private int left;
    private int top;

    /** Which of the buttons walks back, the first of them. */
    private static final int BACKTRACK = 0;

    public HelpViewerApp(final BlockPos host) {
        this.host = host;
        OPEN.add(this);
        ask("");
    }

    /** Hands a machine's answer to every open window showing that machine. */
    public static void accept(final HelpPayload payload) {
        final List<Shown> shown = new ArrayList<>(payload.entries().size());
        for (final HelpPayload.Entry entry : payload.entries()) {
            shown.add(new Shown(GameText.resolve(entry.group()), entry.name(), GameText.resolve(entry.summary())));
        }
        final List<String> page = new ArrayList<>(payload.lines().size());
        for (final WireLine line : payload.lines()) {
            page.add(line.toLine().text(GameText.LOADED));
        }
        for (final HelpViewerApp app : OPEN) {
            if (app.host.equals(payload.hostPos())) {
                app.entries = shown;
                app.page = payload.page();
                app.lines = page;
                app.scroll = 0;
            }
        }
    }

    /** What the window is showing, for a test to read. */
    public List<String> shownLines() {
        return this.lines;
    }

    /** The name of the page it is showing. */
    public String shownPage() {
        return this.page;
    }

    /** The rows of the list, groups and all, as they are drawn. */
    public List<String> shownList() {
        final List<String> out = new ArrayList<>();
        String group = "";
        for (final Shown entry : narrowed()) {
            if (!entry.group().equals(group)) {
                group = entry.group();
                out.add(group);
            }
            out.add("  " + entry.name());
        }
        return out;
    }

    @Override
    public String title() {
        final String title = GameText.resolve(HelpViewerAppTexts.TITLE);
        return this.page.isEmpty() ? title : title + " - " + this.page;
    }

    @Override
    public int defaultWidth() {
        return HelpViewerLayout.W;
    }

    @Override
    public int defaultHeight() {
        return HelpViewerLayout.H;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onRestored() {
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
        ask(this.page);
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        final int ink = this.skin.text();
        final int dim = this.skin.dim();
        g.fill(x, y, x + w, y + h, this.skin.windowBg());

        // The three buttons that viewer always had, the first greyed until there is somewhere to go back to.
        int bx = x + HelpViewerLayout.PAD;
        for (int i = 0; i < HelpViewerLayout.BUTTONS.size(); i++) {
            final String button = HelpViewerLayout.BUTTONS.get(i);
            final boolean on = i != BACKTRACK || !this.back.isEmpty();
            this.skin.button(g, font, bx, y + HelpViewerLayout.BUTTON_Y, HelpViewerLayout.BUTTON_W,
                    HelpViewerLayout.BUTTON_H, "", false, false, false);
            Draw.text(g, font, button, bx + 4, y + HelpViewerLayout.BUTTON_Y + 3, on ? ink : dim,
                    this.skin.windowBg());
            bx += HelpViewerLayout.BUTTON_W + HelpViewerLayout.PAD;
        }

        // The search field: what is typed in it narrows the list, which is what apropos does at a prompt.
        Draw.text(g, font, GameText.resolve(HelpViewerTexts.SEARCH), x + HelpViewerLayout.PAD,
                y + HelpViewerLayout.SEARCH_Y + 2, dim, this.skin.windowBg());
        this.skin.field(g, x + HelpViewerLayout.SEARCH_X, y + HelpViewerLayout.SEARCH_Y,
                HelpViewerLayout.SEARCH_W, HelpViewerLayout.ROW_H, true);
        Draw.text(g, font, this.searching + "_", x + HelpViewerLayout.SEARCH_X + 3,
                y + HelpViewerLayout.SEARCH_Y + 2, ink, this.skin.windowBg());

        // The list on the left, and the page on the right, each in a well of its own.
        final int bodyY = y + HelpViewerLayout.BODY_Y;
        final int bodyH = h - HelpViewerLayout.BODY_Y - HelpViewerLayout.PAD;
        this.skin.field(g, x + HelpViewerLayout.PAD, bodyY, HelpViewerLayout.LIST_W, bodyH, false);
        this.skin.field(g, x + HelpViewerLayout.DOC_X, bodyY,
                w - HelpViewerLayout.DOC_X - HelpViewerLayout.PAD, bodyH, false);

        final List<String> rows = shownList();
        final int fit = Math.max(1, bodyH / HelpViewerLayout.ROW_H);
        this.listScroll = Math.max(0, Math.min(this.listScroll, Math.max(0, rows.size() - fit)));
        for (int i = 0; i < fit && this.listScroll + i < rows.size(); i++) {
            final String row = rows.get(this.listScroll + i);
            final boolean isGroup = !row.startsWith("  ");
            final int ry = bodyY + 2 + i * HelpViewerLayout.ROW_H;
            if (!isGroup && row.trim().equalsIgnoreCase(this.page)) {
                g.fill(x + HelpViewerLayout.PAD + 1, ry - 1, x + HelpViewerLayout.PAD + HelpViewerLayout.LIST_W - 1,
                        ry + HelpViewerLayout.ROW_H - 1, this.skin.accent());
            }
            Draw.text(g, font, row, x + HelpViewerLayout.PAD + 3, ry, isGroup ? dim : ink,
                    this.skin.windowBg());
        }

        final List<String> page = wrapped(font, w - HelpViewerLayout.DOC_X - HelpViewerLayout.PAD - 6);
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, page.size() - fit)));
        for (int i = 0; i < fit && this.scroll + i < page.size(); i++) {
            Draw.text(g, font, page.get(this.scroll + i), x + HelpViewerLayout.DOC_X + 3,
                    bodyY + 2 + i * HelpViewerLayout.ROW_H, ink, this.skin.windowBg());
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        final int x = (int) mouseX - this.left;
        final int y = (int) mouseY - this.top;
        if (y >= HelpViewerLayout.BUTTON_Y && y < HelpViewerLayout.BUTTON_Y + HelpViewerLayout.BUTTON_H) {
            pressed((x - HelpViewerLayout.PAD) / (HelpViewerLayout.BUTTON_W + HelpViewerLayout.PAD));
            return;
        }
        if (y < HelpViewerLayout.BODY_Y || x > HelpViewerLayout.LIST_W + HelpViewerLayout.PAD) {
            return;
        }
        final int row = this.listScroll + (y - HelpViewerLayout.BODY_Y - 2) / HelpViewerLayout.ROW_H;
        final List<String> rows = shownList();
        if (row >= 0 && row < rows.size() && rows.get(row).startsWith("  ")) {
            open(rows.get(row).trim());
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && !this.searching.isEmpty()) {
            this.searching.setLength(this.searching.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.scroll += 10;
            return true;
        }
        if (key == GLFW.GLFW_KEY_PAGE_UP) {
            this.scroll = Math.max(0, this.scroll - 10);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(final char c) {
        if (c >= 32 && c != 127) {
            this.searching.append(c);
            this.listScroll = 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(delta) * 3);
        return true;
    }

    /** One of the three buttons: back to where we were, the pages we have read, or the whole list. */
    private void pressed(final int which) {
        if (which == BACKTRACK && !this.back.isEmpty()) {
            final String was = this.back.remove(this.back.size() - 1);
            ask(was);
            return;
        }
        if (which == 2) {
            this.searching.setLength(0);
            this.listScroll = 0;
        }
    }

    /** Opens a page, remembering where we were so Backtrack has somewhere to go. */
    private void open(final String name) {
        if (!this.page.isEmpty() && !this.page.equalsIgnoreCase(name)) {
            this.back.add(this.page);
        }
        ask(name);
    }

    /**
     * The page's lines cut to the width of the well, each a paragraph that wraps, its rows after the first set in as
     * far as the line is.
     */
    private List<String> wrapped(final Font font, final int width) {
        final List<String> out = new ArrayList<>();
        for (final String line : this.lines) {
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            final String margin = " ".repeat(indent);
            final StringBuilder row = new StringBuilder(line.substring(0, indent));
            boolean empty = true;
            for (final String word : line.substring(indent).split(" ")) {
                final String next = empty ? row + word : row + " " + word;
                if (!empty && font.width(next) > width) {
                    out.add(row.toString());
                    row.setLength(0);
                    row.append(margin).append(word);
                } else {
                    row.setLength(0);
                    row.append(next);
                }
                empty = false;
            }
            out.add(row.toString());
        }
        return out;
    }

    /** The rows that answer to what is being searched for, which is what apropos answers. */
    private List<Shown> narrowed() {
        final String wanted = this.searching.toString().toLowerCase(Locale.ROOT);
        final List<Shown> kept = new ArrayList<>();
        final Set<String> groups = new LinkedHashSet<>();
        for (final Shown entry : this.entries) {
            groups.add(entry.group());
        }
        for (final String group : groups) {
            for (final Shown entry : this.entries) {
                if (!entry.group().equals(group)) {
                    continue;
                }
                if (wanted.isEmpty() || entry.name().toLowerCase(Locale.ROOT).contains(wanted)
                        || entry.summary().toLowerCase(Locale.ROOT).contains(wanted)) {
                    kept.add(entry);
                }
            }
        }
        return kept;
    }

    private void ask(final String name) {
        PacketDistributor.sendToServer(new RequestHelpPayload(this.host, name));
    }

    /** One command of the list as this player reads it: its heading, its name and its one line. */
    private record Shown(String group, String name, String summary) {
    }
}
