/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.computers.os.fs.FileOpeners;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextArea;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.logic.TextDocument;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The text editor every system here ships with, and the one a fresh install opens a {@code .txt} with.
 *
 * <p>It was a name field, a text area and a status line. It is now what a text editor is expected to be:
 * line numbers down the side, find and replace, go to line, a wrap that can be turned off, the line and
 * column in the bar, Open and Save As through the system's own file window, and more than one file open
 * at a time.
 *
 * <p>It stays a text editor. Syntax highlighting, completion, projects and a compiler are what tell the
 * five development environments apart, and they stay theirs.
 */
public final class EditorApp implements IDesktopApp, CodeFileReplies.IReader {

    private static final int TOOLBAR_H = 15;
    private static final int TAB_H = 13;
    private static final int STATUS_H = 10;
    private static final int GUTTER_MIN = 14;
    private static final int MARGIN = 3;
    private static final int MAX_TABS = 8;
    private static final String UNTITLED = "untitled.txt";

    /** One file open in the window: what it is called, what is in it, and whether it has been changed. */
    private static final class Tab {

        private String path;
        private String name;
        private String text = "";
        private boolean dirty;
        /** The line the view was on, so coming back to a tab finds it where it was left. */
        private int caretLine;

        Tab(final String path, final String name) {
            this.path = path;
            this.name = name;
        }
    }

    private final BlockPos host;
    private final FileDialog dialog;
    private final Panel root = new Panel();
    private final TextArea body = new TextArea();
    private final Button openButton;
    private final Button saveButton;
    private final Button saveAsButton;
    private final Button findButton;
    private final Button gotoButton;
    private final EditorFindBar find;

    private final List<Tab> tabs = new ArrayList<>();
    private int active;
    /** The page the machine is writing right now, so its answer marks the right one saved. */
    @Nullable
    private Tab saving;
    /** A page whose close was asked for while it had unsaved changes, and is waiting to be asked again. */
    @Nullable
    private Tab closing;

    private OsSkin skin = OsSkin.fallback();
    private String status = "Ctrl+S to save";
    /** Where the tab strip was last drawn, so a click on it reads the same numbers. */
    private int tabStripY;
    private int tabStripX;
    private final List<Integer> tabEnds = new ArrayList<>();

    public EditorApp(final BlockPos host) {
        this.host = host;
        this.dialog = new FileDialog(host, this);
        this.find = new EditorFindBar(body::document);
        openButton = root.add(new Button("Open", this::chooseOpen));
        saveButton = root.add(new Button("Save", this::save));
        saveAsButton = root.add(new Button("Save As", this::chooseSaveAs));
        findButton = root.add(new Button("Find", () -> toggleFind(EditorFindBar.Mode.FIND)));
        gotoButton = root.add(new Button("Go to", () -> toggleFind(EditorFindBar.Mode.GO_TO_LINE)));
        root.add(body);
        body.setOnEdit(this::edited);
        tabs.add(new Tab("", UNTITLED));
        root.focus(body);
    }

    /* What is open */

    private Tab current() {
        return tabs.get(Math.max(0, Math.min(active, tabs.size() - 1)));
    }

    private void edited() {
        current().dirty = true;
        // Typing again takes back a close that was asked for, so the next one asks afresh.
        this.closing = null;
    }

    private void switchTo(final int index) {
        if (index < 0 || index >= tabs.size() || index == active) {
            return;
        }
        stash();
        this.active = index;
        final Tab tab = current();
        body.setText(tab.text);
        body.document().setCursor(Math.min(tab.caretLine, Math.max(0, body.document().lineCount() - 1)), 0);
        this.status = tab.path.isEmpty() ? "New file" : tab.path;
        root.focus(body);
    }

    /** Keeps what is in the area against the tab it belongs to, before anything replaces it. */
    private void stash() {
        final Tab tab = current();
        tab.text = body.text();
        tab.caretLine = body.document().cursorLine();
    }

    /** Opens another page; answers false when there is no room for one, so a caller can stop. */
    private boolean newTab(final String path, final String name) {
        if (tabs.size() >= MAX_TABS) {
            this.status = "Only " + MAX_TABS + " files at once";
            return false;
        }
        stash();
        tabs.add(new Tab(path, name));
        this.active = tabs.size() - 1;
        body.setText("");
        root.focus(body);
        return true;
    }

    private void closeTab(final int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        /*
         * A page with unsaved work on it asks once before it goes. One misplaced click on a cross is not a
         * reason to lose everything typed since the last save, and there is nothing to undo it with.
         */
        final Tab asked = tabs.get(index);
        if (asked.dirty && closing != asked) {
            this.closing = asked;
            this.status = "Unsaved changes in " + asked.name + " - close again to discard";
            return;
        }
        this.closing = null;
        if (tabs.size() == 1) {
            // The last one is emptied rather than closed, so the window is never left with no page at all.
            final Tab only = tabs.get(0);
            only.path = "";
            only.name = UNTITLED;
            only.dirty = false;
            only.text = "";
            body.setText("");
            return;
        }
        /*
         * What is in the area belongs to the tab that is showing, and closing another one must not throw it
         * away: without this, closing a background tab put the showing tab back to its last saved text and
         * everything typed since was gone.
         */
        final boolean closingTheShownOne = index == active;
        if (!closingTheShownOne) {
            stash();
        }
        tabs.remove(index);
        this.active = Math.max(0, Math.min(active > index ? active - 1 : active, tabs.size() - 1));
        if (closingTheShownOne) {
            final Tab tab = current();
            body.setText(tab.text);
            body.document().setCursor(
                    Math.min(tab.caretLine, Math.max(0, body.document().lineCount() - 1)), 0);
        }
    }

    /* Opening and saving */

    private void chooseOpen() {
        dialog.openFile("Open", "", FileDialog.Filter.sources(), this::openFile);
    }

    @Override
    public void openFile(final String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).path.equals(path)) {
                switchTo(i);
                return;
            }
        }
        CodeFileReplies.expectContent(this, path);
        PacketDistributor.sendToServer(new RequestFileContentPayload(host, path));
    }

    @Override
    public void onContent(final String path, final String content, final boolean exists) {
        final Tab blank = current();
        if (blank.path.isEmpty() && !blank.dirty && body.text().isEmpty()) {
            // An untouched new page takes the file rather than opening a second one beside it.
            blank.path = path;
            blank.name = leaf(path);
            blank.dirty = false;
            body.setText(content);
        } else if (newTab(path, leaf(path))) {
            body.setText(content);
            current().dirty = false;
        } else {
            /*
             * There was no room for another page, and the file is not opened at all. Writing it into the
             * area anyway put the file somebody asked for over the one they were writing, kept that page's
             * own name on it, and marked it saved, so the next save wrote it away.
             */
            return;
        }
        this.status = exists ? "Opened " + leaf(path) : "New file " + leaf(path);
        root.focus(body);
    }

    private void chooseSaveAs() {
        final Tab tab = current();
        dialog.saveAs("Save As", "", tab.name.isEmpty() ? UNTITLED : tab.name,
                FileDialog.Filter.sources(), this::saveAs);
    }

    private void saveAs(final String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        final Tab tab = current();
        tab.path = path;
        tab.name = leaf(path);
        save();
    }

    private void save() {
        final Tab tab = current();
        final String target = tab.path.isEmpty() ? tab.name.trim() : tab.path;
        if (target.isEmpty()) {
            this.status = "Save As first";
            return;
        }
        // A .dat is a read-only projection of stored items; it can never be created or written by hand.
        if (FileType.of(FileOpeners.extensionOf(target)) == FileType.DAT) {
            this.status = "Cannot save a .dat file";
            DesktopScreen.showDatLockedError();
            return;
        }
        final String text = body.text();
        /*
         * Refused here rather than thrown at the network: the cap on what a save may carry throws when it
         * is handed more, which would take the packet down instead of telling the player anything.
         */
        if (text.length() > SaveFilePayload.MAX_CONTENT) {
            this.status = "Too long to save: " + text.length() + " of "
                    + SaveFilePayload.MAX_CONTENT + " characters";
            return;
        }
        this.status = "Saving...";
        /*
         * Which page is being saved is held, not looked up again when the answer comes back: a player who
         * moves to another tab while the machine is writing would otherwise have that one marked saved and
         * the one that really was written left showing unsaved changes.
         */
        this.saving = tab;
        CodeFileReplies.expectSaved(this);
        PacketDistributor.sendToServer(new SaveFilePayload(host, target, text));
        FilesApps.diskChanged();
    }

    /**
     * A file the machine could not hand over is not opened at all.
     *
     * <p>Opening it as an empty page would be the dangerous thing: the page looks like the file, and
     * saving it would write the real one away.
     */
    @Override
    public void onContentTooLarge(final String path) {
        this.status = leaf(path) + " is too large to open here";
    }

    @Override
    public void onSaved(final boolean ok, final String message) {
        this.status = message;
        if (ok && saving != null && tabs.contains(saving)) {
            saving.dirty = false;
        }
        this.saving = null;
    }

    @Override
    public void onClosed() {
        CodeFileReplies.forget(this);
    }

    private void toggleFind(final EditorFindBar.Mode mode) {
        if (find.isOpen() && find.mode() == mode) {
            find.close();
            root.focus(body);
        } else {
            find.open(mode);
        }
    }

    /* The window */

    @Override
    public String title() {
        final Tab tab = current();
        return (tab.dirty ? "*" : "") + tab.name + " - Editor";
    }

    /** The text in the buffer, which is what the player is reading or writing. */
    public String text() {
        return body.text();
    }

    /** The file the buffer holds, or empty for one not saved yet. */
    public String openFile() {
        return current().path;
    }

    @Override
    public int defaultWidth() {
        return 300;
    }

    @Override
    public int defaultHeight() {
        return 200;
    }

    @Override
    public int minWidth() {
        return 220;
    }

    @Override
    public int minHeight() {
        return 130;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layoutToolbar(font, x, y, width);
        drawTabs(g, font, x, y + TOOLBAR_H, width);

        final int gutter = gutterWidth(font);
        final int bodyTop = y + TOOLBAR_H + TAB_H;
        final int barH = find.isOpen() ? EditorFindBar.HEIGHT : 0;
        final int bodyBottom = y + height - STATUS_H - barH;
        body.setBounds(x + gutter, bodyTop, width - gutter, Math.max(20, bodyBottom - bodyTop));
        root.render(g, ctx);
        drawGutter(g, font, x, bodyTop, gutter, Math.max(20, bodyBottom - bodyTop));
        find.render(g, ctx, font, x, bodyBottom, width);
        drawStatus(g, font, x, y + height - STATUS_H, width);
    }

    private void layoutToolbar(final Font font, final int x, final int y, final int width) {
        int bx = x + MARGIN;
        bx = place(openButton, font, "Open", bx, y);
        bx = place(saveButton, font, "Save", bx, y);
        bx = place(saveAsButton, font, "Save As", bx, y);
        bx = place(findButton, font, "Find", bx, y);
        final int gotoW = font.width("Go to") + 8;
        // Pinned right, never back over the button before it; a narrow window clips rather than overlaps.
        gotoButton.setBounds(Math.max(bx, x + width - MARGIN - gotoW), y + 1, gotoW, 12);
        findButton.setPrimary(find.isOpen() && find.mode() == EditorFindBar.Mode.FIND);
        gotoButton.setPrimary(find.isOpen() && find.mode() == EditorFindBar.Mode.GO_TO_LINE);
    }

    private static int place(final Button button, final Font font, final String label,
                             final int bx, final int y) {
        final int w = font.width(label) + 8;
        button.setBounds(bx, y + 1, w, 12);
        return bx + w + 2;
    }

    private void drawTabs(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        this.tabStripY = y;
        this.tabStripX = x + MARGIN;
        tabEnds.clear();
        g.fill(x, y + TAB_H - 1, x + width, y + TAB_H, skin.edge());
        int bx = x + MARGIN;
        for (int i = 0; i < tabs.size(); i++) {
            final Tab tab = tabs.get(i);
            final String label = (tab.dirty ? "*" : "") + tab.name;
            final int w = Math.min(90, font.width(label) + 14);
            if (bx + w > x + width - MARGIN) {
                break;
            }
            final boolean on = i == active;
            g.fill(bx, y + 1, bx + w, y + TAB_H - (on ? 0 : 1), on ? skin.fieldBg() : skin.listHover());
            g.fill(bx, y + 1, bx + w, y + 2, on ? skin.accent() : skin.edge());
            g.drawString(font, font.plainSubstrByWidth(label, w - 14), bx + 4, y + 3,
                    on ? skin.text() : skin.dim(), false);
            // The cross that closes it, drawn small so it is not mistaken for part of the name.
            g.drawString(font, "x", bx + w - 7, y + 3, skin.dim(), false);
            bx += w + 2;
            tabEnds.add(bx);
        }
    }

    /**
     * The column of line numbers.
     *
     * <p>Drawn from the area's own first visible line and its own row pitch, so the two cannot drift apart:
     * a gutter that counts for itself is wrong by a whole screen the moment the text is scrolled.
     */
    private void drawGutter(final GuiGraphics g, final Font font, final int x, final int y,
                            final int gutter, final int heightOfBody) {
        g.fill(x, y, x + gutter, y + heightOfBody, skin.listHover());
        g.fill(x + gutter - 1, y, x + gutter, y + heightOfBody, skin.edge());
        final TextDocument doc = body.document();
        final int first = body.firstVisibleLine();
        final int pitch = body.lineHeight();
        final int caret = doc.cursorLine();
        for (int i = 0; i < body.visibleLines() && first + i < doc.lineCount(); i++) {
            final String number = String.valueOf(first + i + 1);
            g.drawString(font, number, x + gutter - 3 - font.width(number),
                    body.textTop() + i * pitch + 1, first + i == caret ? skin.text() : skin.dim(), false);
        }
    }

    private int gutterWidth(final Font font) {
        final int widest = font.width(String.valueOf(Math.max(99, body.document().lineCount())));
        return Math.max(GUTTER_MIN, widest + 6);
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        final TextDocument doc = body.document();
        final String left = "Ln " + (doc.cursorLine() + 1) + ", Col " + (doc.cursorCol() + 1);
        g.drawString(font, left, x + MARGIN, y + 1, skin.text(), false);
        final String right = doc.lineCount() + (doc.lineCount() == 1 ? " line   " : " lines   ")
                + bytes(body.text());
        g.drawString(font, right, x + width - MARGIN - font.width(right), y + 1, skin.dim(), false);
        final int room = width - MARGIN * 2 - font.width(left) - font.width(right) - 12;
        if (room > 20) {
            g.drawString(font, font.plainSubstrByWidth(status, room),
                    x + MARGIN + font.width(left) + 6, y + 1, skin.dim(), false);
        }
    }

    private static String bytes(final String text) {
        final int size = text.getBytes(StandardCharsets.UTF_8).length;
        return size < 1024 ? size + " B" : String.format(Locale.ROOT, "%.1f KB", size / 1024.0);
    }

    private static String leaf(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    /* Input */

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (find.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (mouseY >= tabStripY && mouseY < tabStripY + TAB_H && clickedTab(mouseX)) {
            return;
        }
        root.mouseClicked(mouseX, mouseY, button);
    }

    /** A click on the strip picks a tab, or closes it when it landed on the cross at its right end. */
    private boolean clickedTab(final double mouseX) {
        int bx = tabStripX;
        for (int i = 0; i < tabEnds.size(); i++) {
            final int end = tabEnds.get(i);
            if (mouseX >= bx && mouseX < end - 2) {
                if (mouseX >= end - 11) {
                    closeTab(i);
                } else {
                    switchTo(i);
                }
                return true;
            }
            bx = end;
        }
        return false;
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        if (find.mouseReleased(mouseX, mouseY, button)) {
            return;
        }
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        return body.mouseScrolled(body.x(), body.y(), delta);
    }

    @Override
    public boolean charTyped(final char c) {
        if (find.hasFocus()) {
            return find.charTyped(c);
        }
        return root.charTyped(c);
    }

    @Override
    public boolean wantsEscape() {
        return find.isOpen();
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && find.isOpen()) {
            find.close();
            root.focus(body);
            return true;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && shortcut(key)) {
            return true;
        }
        if (find.hasFocus()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                find.go();
                return true;
            }
            return find.keyPressed(key, scanCode, modifiers);
        }
        return root.keyPressed(key, scanCode, modifiers);
    }

    /** The keys held with control, which are the ones a text editor is expected to answer. */
    private boolean shortcut(final int key) {
        switch (key) {
            case GLFW.GLFW_KEY_S -> save();
            case GLFW.GLFW_KEY_O -> chooseOpen();
            case GLFW.GLFW_KEY_F -> toggleFind(EditorFindBar.Mode.FIND);
            case GLFW.GLFW_KEY_G -> toggleFind(EditorFindBar.Mode.GO_TO_LINE);
            case GLFW.GLFW_KEY_N -> newTab("", UNTITLED);
            case GLFW.GLFW_KEY_W -> closeTab(active);
            case GLFW.GLFW_KEY_TAB -> switchTo((active + 1) % Math.max(1, tabs.size()));
            default -> {
                return false;
            }
        }
        return true;
    }
}
