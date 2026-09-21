/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.RequestSheetFactsPayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.computers.operation.payload.SheetFactsPayload;
import dev.jstech.computers.program.Spreadsheet;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exceed: a sheet of cells that can ask the network what it is holding.
 *
 * <p>Besides the arithmetic a spreadsheet has always done, a cell may write {@code =QTY("iron_ingot")},
 * {@code =FREE()} or {@code =SERVERS()}, and those are the ones that make it worth having on a computer
 * rather than on paper. They are drawn apart from the cells the player typed, so nobody wonders why a
 * number moved by itself, and they are worked out from what the machine last said rather than from asking
 * the world, which is what keeps the sheet instant.
 *
 * <p>It reads and writes the comma separated files the mod already has, so a sheet is a file any other
 * program on the machine can open.
 */
public final class ExceedApp implements IDesktopApp, CodeFileReplies.IReader {

    private static final int TOOLBAR_H = 15;
    private static final int FORMULA_H = 14;
    private static final int HEAD_H = 10;
    private static final int ROW_H = 10;
    private static final int ROW_HEAD_W = 18;
    private static final int COL_W = 46;
    private static final int STATUS_H = 11;
    private static final int MARGIN = 3;
    private static final int LIVE_INK = 0xFF1C4FA8;
    private static final int ERROR_INK = 0xFFB4231F;
    /** How long the sheet waits before asking the machine again, so typing does not send a packet a key. */
    private static final long ASK_EVERY_MS = 3000L;
    /** The longest name a cell may ask about, which is what the request payload carries. */
    private static final int MAX_ITEM_NAME = 120;

    /** The one window whose asking cells the machine's answers belong to. */
    @Nullable
    private static ExceedApp open;

    private final BlockPos host;
    private final BlockPos monitorPos;
    private final FileDialog dialog;
    private final Panel root = new Panel();
    private final TextField formula = new TextField(160);
    private final Button openButton;
    private final Button saveButton;
    private final Button saveAsButton;
    private final Button refreshButton;

    private final Spreadsheet sheet = new Spreadsheet();
    private final Facts facts = new Facts();

    private OsSkin skin = OsSkin.fallback();
    private String path = "";
    private String status = "";
    private boolean dirty;
    private int cursorRow;
    private int cursorColumn;
    private int scrollRow;
    private int scrollColumn;
    private long lastAskMs;

    /* Where the grid was last drawn, so a click reads the same numbers. */
    private int gridX;
    private int gridY;
    private int shownRows;
    private int shownColumns;

    /** What the machine last said, which every asking cell is worked out from. */
    private static final class Facts implements Spreadsheet.INetworkFacts {

        private final Map<String, Long> counts = new HashMap<>();
        private long free = -1L;
        private long servers = -1L;

        @Override
        public long quantity(final String item) {
            final Long held = counts.get(item);
            return held == null ? -1L : held;
        }

        @Override
        public long free() {
            return free;
        }

        @Override
        public long servers() {
            return servers;
        }
    }

    public ExceedApp(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;
        this.dialog = new FileDialog(host, this);
        openButton = root.add(new Button("Open", this::chooseOpen));
        saveButton = root.add(new Button("Save", this::save));
        saveAsButton = root.add(new Button("Save As", this::chooseSaveAs));
        refreshButton = root.add(new Button("Refresh", this::ask));
        root.add(formula);
        sheet.setFacts(facts);
        open = this;
    }

    /** Takes the machine's answer, when this window is the one on the glass that asked for it. */
    public static void accept(final SheetFactsPayload payload) {
        final ExceedApp showing = open;
        if (showing == null) {
            return;
        }
        showing.facts.counts.clear();
        for (int i = 0; i < payload.items().size() && i < payload.counts().size(); i++) {
            showing.facts.counts.put(payload.items().get(i), payload.counts().get(i));
        }
        showing.facts.free = payload.free();
        showing.facts.servers = payload.servers();
        // The facts are changed in place, so the sheet has to be told its answers may have moved.
        showing.sheet.forget();
        showing.status = showing.sheet.liveCells() + " live cells";
    }

    /* Asking the machine */

    /**
     * Asks about the things the sheet's cells name.
     *
     * <p>The names are read out of the formulas rather than kept in a list beside them, so a cell that was
     * edited to ask about something else is asked about at once and nothing goes stale behind the sheet.
     */
    private void ask() {
        final List<String> items = namedItems();
        this.lastAskMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(new RequestSheetFactsPayload(host, monitorPos, items));
    }

    private List<String> namedItems() {
        final List<String> items = new ArrayList<>();
        for (int row = 0; row < Spreadsheet.ROWS; row++) {
            for (int column = 0; column < Spreadsheet.COLUMNS; column++) {
                collectNames(sheet.raw(row, column), items);
            }
        }
        return items;
    }

    /**
     * Pulls the names out of one cell's formula.
     *
     * <p>The upper-cased copy is made once and searched in step with the original, because upper-casing
     * can change a string's length and searching one while cutting the other would take the wrong letters.
     */
    private static void collectNames(final String text, final List<String> items) {
        if (text.length() != text.toUpperCase(Locale.ROOT).length()) {
            // A letter that changes length when upper-cased; the two cannot be walked together, so skip it.
            return;
        }
        final String upper = text.toUpperCase(Locale.ROOT);
        int at = upper.indexOf("QTY(\"");
        while (at >= 0 && items.size() < RequestSheetFactsPayload.MAX_ITEMS) {
            final int from = at + 5;
            final int to = text.indexOf('"', from);
            if (to < 0) {
                return;
            }
            final String name = text.substring(from, to);
            /*
             * Only names the packet can carry are asked about. Its cap throws when handed more than it
             * takes, and one absurd name in one cell must not stop the whole sheet asking anything.
             */
            if (!name.isEmpty() && name.length() <= MAX_ITEM_NAME && !items.contains(name)) {
                items.add(name);
            }
            at = upper.indexOf("QTY(\"", to);
        }
    }

    /* Files */

    private void chooseOpen() {
        dialog.openFile("Open sheet", "",
                List.of(FileDialog.Filter.of("Sheets", "csv"), FileDialog.Filter.ALL), this::openFile);
    }

    @Override
    public void openFile(final String target) {
        if (target == null || target.isEmpty()) {
            return;
        }
        CodeFileReplies.expectContent(this, target);
        PacketDistributor.sendToServer(new RequestFileContentPayload(host, target));
    }

    @Override
    public void onContent(final String target, final String content, final boolean exists) {
        sheet.fromCsv(exists ? content : "");
        this.path = target;
        this.dirty = false;
        this.status = exists ? "Opened " + leaf(target) : "New sheet";
        ask();
    }

    private void chooseSaveAs() {
        dialog.saveAs("Save sheet", "", path.isEmpty() ? "sheet.csv" : leaf(path),
                List.of(FileDialog.Filter.of("Sheets", "csv")), target -> {
                    this.path = target;
                    save();
                });
    }

    private void save() {
        // What is in the bar belongs to its cell before anything is written, or the last edit is not saved.
        commit();
        if (path.isEmpty()) {
            chooseSaveAs();
            return;
        }
        final String csv = sheet.toCsv();
        // The cap on a save throws when handed more than it takes, so a sheet past it is refused here.
        if (csv.length() > SaveFilePayload.MAX_CONTENT) {
            this.status = "Sheet too large to save";
            return;
        }
        CodeFileReplies.expectSaved(this);
        PacketDistributor.sendToServer(new SaveFilePayload(host, path, csv));
        FilesApps.diskChanged();
        this.status = "Saving";
    }

    @Override
    public void onContentTooLarge(final String target) {
        this.status = leaf(target) + " is too large to open here";
    }

    @Override
    public void onSaved(final boolean ok, final String message) {
        this.status = message;
        if (ok) {
            this.dirty = false;
        }
    }

    @Override
    public void onClosed() {
        CodeFileReplies.forget(this);
        if (open == this) {
            open = null;
        }
    }

    @Override
    public void onRestored() {
        open = this;
        ask();
    }

    private static String leaf(final String target) {
        final int slash = target.lastIndexOf('/');
        return slash >= 0 && slash < target.length() - 1 ? target.substring(slash + 1) : target;
    }

    /* The window */

    @Override
    public String title() {
        return (dirty ? "*" : "") + (path.isEmpty() ? "untitled.csv" : leaf(path)) + " - Exceed";
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
        return 140;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        /*
         * The window being drawn claims the machine's answers, so that with two sheets open the one in
         * front is the one whose asking cells are filled in rather than whichever was made last.
         */
        open = this;
        keepFresh();
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layoutToolbar(font, x, y, width);
        drawFormulaBar(g, font, x, y + TOOLBAR_H, width);
        root.render(g, ctx);
        final int top = y + TOOLBAR_H + FORMULA_H;
        final int bottom = y + height - STATUS_H;
        drawGrid(g, font, x, top, width, bottom);
        drawStatus(g, font, x, bottom, width);
    }

    /** Asks the machine again now and then, so a sheet left open does not go stale behind the player. */
    private void keepFresh() {
        if (System.currentTimeMillis() - lastAskMs >= ASK_EVERY_MS && sheet.liveCells() > 0) {
            ask();
        }
    }

    private void layoutToolbar(final Font font, final int x, final int y, final int width) {
        int bx = x + MARGIN;
        bx = place(openButton, font, "Open", bx, y);
        bx = place(saveButton, font, "Save", bx, y);
        bx = place(saveAsButton, font, "Save As", bx, y);
        final int w = font.width("Refresh") + 8;
        // Pinned right, never back over the button before it; a narrow window clips rather than overlaps.
        refreshButton.setBounds(Math.max(bx, x + width - MARGIN - w), y + 1, w, 12);
    }

    private static int place(final Button button, final Font font, final String label,
                             final int bx, final int y) {
        final int w = font.width(label) + 8;
        button.setBounds(bx, y + 1, w, 12);
        return bx + w + 2;
    }

    private void drawFormulaBar(final GuiGraphics g, final Font font, final int x, final int y,
                                final int width) {
        final String name = Spreadsheet.cellName(cursorRow, cursorColumn);
        g.fill(x + MARGIN, y + 1, x + MARGIN + 28, y + FORMULA_H - 1, skin.fieldBg());
        g.drawString(font, name, x + MARGIN + (28 - font.width(name)) / 2, y + 3, skin.text(), false);
        g.drawString(font, "fx", x + MARGIN + 31, y + 3, skin.dim(), false);
        formula.setBounds(x + MARGIN + 43, y + 1, Math.max(20, width - MARGIN * 2 - 43), FORMULA_H - 2);
    }

    private void drawGrid(final GuiGraphics g, final Font font, final int x, final int top,
                          final int width, final int bottom) {
        this.gridX = x + MARGIN;
        this.gridY = top;
        final int room = width - MARGIN * 2 - ROW_HEAD_W;
        this.shownColumns = Math.max(1, room / COL_W);
        this.shownRows = Math.max(1, (bottom - top - HEAD_H) / ROW_H);
        clampScroll();

        // Column letters along the top, and the row numbers down the side.
        g.fill(gridX, gridY, gridX + ROW_HEAD_W + shownColumns * COL_W, gridY + HEAD_H, skin.listHover());
        for (int c = 0; c < shownColumns; c++) {
            final int cx = gridX + ROW_HEAD_W + c * COL_W;
            final String label = Spreadsheet.columnName(scrollColumn + c);
            g.drawString(font, label, cx + (COL_W - font.width(label)) / 2, gridY + 1, skin.text(), false);
        }
        for (int r = 0; r < shownRows; r++) {
            final int ry = gridY + HEAD_H + r * ROW_H;
            g.fill(gridX, ry, gridX + ROW_HEAD_W, ry + ROW_H, skin.listHover());
            final String label = String.valueOf(scrollRow + r + 1);
            g.drawString(font, label, gridX + ROW_HEAD_W - 2 - font.width(label), ry + 1,
                    skin.text(), false);
        }

        for (int r = 0; r < shownRows; r++) {
            for (int c = 0; c < shownColumns; c++) {
                drawCell(g, font, r, c);
            }
        }
    }

    private void drawCell(final GuiGraphics g, final Font font, final int r, final int c) {
        final int row = scrollRow + r;
        final int column = scrollColumn + c;
        final int cx = gridX + ROW_HEAD_W + c * COL_W;
        final int cy = gridY + HEAD_H + r * ROW_H;
        g.fill(cx, cy, cx + COL_W, cy + ROW_H, skin.fieldBg());
        g.fill(cx + COL_W - 1, cy, cx + COL_W, cy + ROW_H, skin.edge());
        g.fill(cx, cy + ROW_H - 1, cx + COL_W, cy + ROW_H, skin.edge());
        final String shown = sheet.display(row, column);
        if (!shown.isEmpty()) {
            final boolean live = sheet.isLive(row, column);
            final boolean bad = shown.startsWith("#");
            final int ink = bad ? ERROR_INK : live ? LIVE_INK : skin.text();
            /*
             * Numbers are read against each other down a column, so they are set to the right; words are
             * read one at a time and start where the eye does, which is the left.
             */
            final String clipped = font.plainSubstrByWidth(shown, COL_W - 4);
            final boolean numeric = !shown.isEmpty()
                    && (Character.isDigit(shown.charAt(0)) || shown.charAt(0) == '-' || bad);
            final int tx = numeric ? cx + COL_W - 3 - font.width(clipped) : cx + 2;
            g.drawString(font, clipped, tx, cy + 1, ink, false);
        }
        if (row == cursorRow && column == cursorColumn) {
            final int accent = skin.accent();
            g.fill(cx, cy, cx + COL_W, cy + 1, accent);
            g.fill(cx, cy + ROW_H - 1, cx + COL_W, cy + ROW_H, accent);
            g.fill(cx, cy, cx + 1, cy + ROW_H, accent);
            g.fill(cx + COL_W - 1, cy, cx + COL_W, cy + ROW_H, accent);
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        final int live = sheet.liveCells();
        final String left = live == 0 ? "Ready" : live + (live == 1 ? " live cell" : " live cells");
        g.drawString(font, left, x + MARGIN, y + 2, skin.text(), false);
        if (!status.isEmpty()) {
            final String clipped = font.plainSubstrByWidth(status, width - MARGIN * 2 - font.width(left) - 8);
            g.drawString(font, clipped, x + width - MARGIN - font.width(clipped), y + 2, skin.dim(), false);
        }
    }

    private void clampScroll() {
        this.scrollRow = Math.max(0, Math.min(scrollRow, Spreadsheet.ROWS - shownRows));
        this.scrollColumn = Math.max(0, Math.min(scrollColumn, Spreadsheet.COLUMNS - shownColumns));
    }

    /** Keeps the cell being edited on the screen, which is what the arrow keys need. */
    private void follow() {
        if (cursorRow < scrollRow) {
            this.scrollRow = cursorRow;
        } else if (cursorRow >= scrollRow + shownRows) {
            this.scrollRow = cursorRow - shownRows + 1;
        }
        if (cursorColumn < scrollColumn) {
            this.scrollColumn = cursorColumn;
        } else if (cursorColumn >= scrollColumn + shownColumns) {
            this.scrollColumn = cursorColumn - shownColumns + 1;
        }
        clampScroll();
    }

    /* Input */

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (button != 0) {
            return;
        }
        final int c = (int) ((mouseX - gridX - ROW_HEAD_W) / COL_W);
        final int r = (int) ((mouseY - gridY - HEAD_H) / ROW_H);
        if (mouseX < gridX + ROW_HEAD_W || mouseY < gridY + HEAD_H
                || c < 0 || c >= shownColumns || r < 0 || r >= shownRows) {
            return;
        }
        commit();
        this.cursorRow = scrollRow + r;
        this.cursorColumn = scrollColumn + c;
        formula.set(sheet.raw(cursorRow, cursorColumn));
        root.focus(formula);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scrollRow = Math.max(0, scrollRow - (int) Math.signum(delta) * 3);
        clampScroll();
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    /** Writes what is in the bar into the cell it belongs to, which is what leaving that cell does. */
    private void commit() {
        final String typed = formula.edit();
        if (!typed.equals(sheet.raw(cursorRow, cursorColumn))) {
            sheet.set(cursorRow, cursorColumn, typed);
            this.dirty = true;
            // A formula that names a new thing has to be asked about before it can say anything.
            if (typed.toUpperCase(Locale.ROOT).contains("QTY(\"")) {
                ask();
            }
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }
        final int dr = switch (key) {
            case GLFW.GLFW_KEY_UP -> -1;
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 1;
            default -> 0;
        };
        final int dc = switch (key) {
            case GLFW.GLFW_KEY_TAB -> 1;
            default -> 0;
        };
        if (dr != 0 || dc != 0) {
            commit();
            this.cursorRow = Math.max(0, Math.min(Spreadsheet.ROWS - 1, cursorRow + dr));
            this.cursorColumn = Math.max(0, Math.min(Spreadsheet.COLUMNS - 1, cursorColumn + dc));
            formula.set(sheet.raw(cursorRow, cursorColumn));
            follow();
            return true;
        }
        return root.keyPressed(key, scanCode, modifiers);
    }
}
