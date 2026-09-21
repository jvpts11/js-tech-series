/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.DesktopIconLayout;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.core.client.gui.component.Texts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The icons on the wallpaper: where each one sits, what it looks like, and which ones are picked.
 *
 * <p>The grid is the same one every desktop has used: columns filled top to bottom, then left to right,
 * each cell wide enough for a name on two lines. An icon a player has dragged somewhere keeps that spot
 * for good, and the rest flow around it into whatever cells are still free, so moving one icon never
 * shuffles the others.
 *
 * <p>One method resolves those cells and everything else asks it: the drawing, the hit-test, the drop
 * target and the rubber band. That is deliberate. When the cells were worked out separately for drawing
 * and for clicking, an icon could be drawn in one cell and clicked in another, and the player has no way
 * of knowing which of the two the desktop believes.
 *
 * <p>Names get the same care, for the same reason. A label is wrapped inside its own cell over at most
 * two lines and cut with an ellipsis past that; a name wider than its cell used to run across its
 * neighbour, which is how "Network" and "Command Prompt" came to read as one word.
 */
final class DesktopIcons {

    /**
     * Where a dragged icon stays, as a packed grid cell ({@code col << 16 | row}) under its stable id
     * ({@code app:<label>} for a launcher, {@code file:<name>} for a file or folder). The server sends
     * these and keeps them, so a desktop reopened after a reload shows every icon where it was left. An
     * icon with no entry flows into the next free cell, so a fresh desktop looks as it always did.
     */
    private final Map<String, Integer> pinned = new HashMap<>();

    /** The icons a rubber band has caught, by slot. */
    private final Set<Integer> selected = new LinkedHashSet<>();

    /*
     * The grid. The pitch is wide enough for the labels drawn under it: a narrower one was what let two
     * names read as a single word.
     */
    private static final int PITCH_X = 50;
    private static final int PITCH_Y = 44;

    /** Where the first column starts: far enough in that its cell's highlight clears the screen edge. */
    private static final int ORIGIN_X = 14;

    /** An icon's cell: the box its highlight, its drop outline and its hit-test all use. */
    private static final int CELL_W = 46;
    private static final int CELL_H = 40;

    /** How wide an icon's picture is drawn. */
    private static final int ICON_W = 24;

    /** The cell's top-left corner relative to the icon's own: the icon sits centred in the cell. */
    private static final int CELL_DX = (ICON_W - CELL_W) / 2;
    private static final int CELL_DY = -2;

    /**
     * How wide one line of a label may run before it wraps, how many lines it may take, and how tall a
     * line stands. Names are drawn in the small text a dense panel uses, which is what lets a word like
     * "Calculator" fit its cell whole without the grid spreading across the whole desktop.
     */
    private static final int LABEL_W = CELL_W - 2;
    private static final int LABEL_LINES = 2;
    private static final int LABEL_LINE_H = 8;

    private final DesktopScreen desktop;

    DesktopIcons(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** The file name part of a path: its last segment, or the whole path when it has no separator. */
    static String baseName(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    /** The pinned cells, for the desktop listing to refill when the server sends them. */
    Map<String, Integer> pinnedCells() {
        return pinned;
    }

    /** The icons a band has caught, which draw picked and which an action applies to. */
    Set<Integer> selection() {
        return selected;
    }

    /** How many icons a column holds: the work area, which is the screen minus the panel wherever it sits. */
    int perColumn() {
        return Math.max(1, (desktop.workAreaBottom() - desktop.workAreaTop() - 12) / PITCH_Y);
    }

    /** The stable id of the icon at slot {@code i}: {@code app:<label>} or {@code file:<name>}. */
    String keyOf(final int i) {
        final List<DesktopScreen.Launcher> launchers = desktop.deskIcons();
        if (i < launchers.size()) {
            return "app:" + launchers.get(i).label();
        }
        return "file:" + baseName(desktop.deskFiles().get(i - launchers.size()).path());
    }

    /**
     * The grid cell of every icon for this layout: a pinned one keeps its stored cell, clamped so it
     * always lands on a real column, and the rest flow top-down then left-to-right into the first cell
     * no pinned icon has claimed.
     */
    int[] cells(final int perCol) {
        final int total = desktop.deskIcons().size() + desktop.deskFiles().size();
        final List<String> keys = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            keys.add(keyOf(i));
        }
        return DesktopIconLayout.resolve(keys, pinned, perCol);
    }

    /**
     * Where a packed cell puts its icon's top-left corner. The columns count from the left edge, or from the
     * right one on a desktop that stands its objects there, where column nought is the one against the edge.
     */
    int xOf(final int packedCell) {
        final int across = ORIGIN_X + DesktopIconLayout.col(packedCell) * PITCH_X;
        return desktop.objectsStandRight() ? desktop.workAreaWidth() - across - ICON_W : across;
    }

    int yOf(final int packedCell) {
        return desktop.workAreaTop() + 10 + DesktopIconLayout.row(packedCell) * PITCH_Y;
    }

    /** The desktop-local middle of the picture in slot {@code slot}, where a click on it lands. */
    int[] centreOf(final int slot) {
        final int cell = cells(perColumn())[slot];
        return new int[] {xOf(cell) + ICON_W / 2, yOf(cell) + 11};
    }

    /** The icon slot under a desktop-local point, or -1 for the bare wallpaper. */
    int slotAt(final double mx, final double my, final int perCol) {
        final int[] cells = cells(perCol);
        for (int i = 0; i < cells.length; i++) {
            final int ix = xOf(cells[i]);
            final int iy = yOf(cells[i]);
            if (mx >= ix + CELL_DX && mx <= ix + CELL_DX + CELL_W
                    && my >= iy + CELL_DY && my <= iy + CELL_DY + CELL_H) {
                return i;
            }
        }
        return -1;
    }

    /** The packed grid cell under a desktop-local point, clamped to the grid. */
    int cellAt(final double mx, final double my, final int perCol) {
        // Counted from whichever edge the columns start at, so a mirrored grid is the same sum.
        final double across = desktop.objectsStandRight() ? desktop.workAreaWidth() - mx : mx;
        final int col = Math.max(0, (int) Math.floor((across - (ORIGIN_X - PITCH_X / 2.0)) / PITCH_X));
        final int row = Math.max(0, Math.min(perCol - 1,
                (int) Math.floor((my - desktop.workAreaTop() - (10 - PITCH_Y / 2.0)) / PITCH_Y)));
        return DesktopIconLayout.pack(col, row);
    }

    /** Whether another icon already holds that cell, which is what stops two stacking on one spot. */
    boolean cellTaken(final int cell, final int exceptSlot, final int perCol) {
        final int[] cells = cells(perCol);
        for (int i = 0; i < cells.length; i++) {
            if (i != exceptSlot && cells[i] == cell) {
                return true;
            }
        }
        return false;
    }

    /** Keeps an icon on the cell it was dropped on. */
    void pin(final String key, final int cell) {
        pinned.put(key, cell);
    }

    /** Forgets an icon's spot once its file has left the desktop folder. */
    void forget(final String path) {
        pinned.remove("file:" + baseName(path));
    }

    /** Picks every icon whose cell the band rectangle {x, y, w, h} touches. */
    void selectWithin(final int[] band) {
        selected.clear();
        final int perCol = perColumn();
        final int[] cells = cells(perCol);
        final int total = Math.min(cells.length, desktop.deskIcons().size() + desktop.deskFiles().size());
        for (int i = 0; i < total; i++) {
            final int ix = xOf(cells[i]);
            final int iy = yOf(cells[i]);
            // The icon's clickable cell, the same box the hover highlight uses.
            if (ix + CELL_DX < band[0] + band[2] && ix + CELL_DX + CELL_W > band[0]
                    && iy + CELL_DY < band[1] + band[3] && iy + CELL_DY + CELL_H > band[1]) {
                selected.add(i);
            }
        }
    }

    /**
     * Draws every icon: the launchers first, then the desktop folder's own files and folders. The picked
     * icon's full name is drawn after all of them, so it sits over the icon below instead of being
     * clipped by it.
     */
    void render(final GuiGraphics g, final int lmx, final int lmy) {
        final List<DesktopScreen.Launcher> launchers = desktop.deskIcons();
        final List<DiskFilesPayload.WireFile> files = desktop.deskFiles();
        final int total = launchers.size() + files.size();
        final int perCol = perColumn();
        final int[] cells = cells(perCol);
        final boolean dragging = desktop.draggingIcon();
        final int dropTarget = dragging
                ? slotAt(desktop.iconDragX(), desktop.iconDragY(), perCol)
                : -1;
        String pickedLabel = null;
        int pickedX = 0;
        int pickedY = 0;
        for (int i = 0; i < total; i++) {
            final int ix = xOf(cells[i]);
            final int iy = yOf(cells[i]);
            drawCell(g, i, ix, iy, lmx, lmy, dragging, dropTarget, launchers.size(), files);
            final String label = labelOf(i, launchers, files);
            if (i == desktop.pickedIcon()) {
                // Defer the full name to a pass after every icon so nothing overdraws it.
                pickedLabel = label;
                pickedX = ix;
                pickedY = iy;
            } else {
                drawName(g, label, ix, iy);
            }
        }
        if (pickedLabel != null) {
            drawPickedName(g, pickedLabel, pickedX, pickedY);
        }
    }

    /** The outline of the cell a dragged icon would snap to, drawn while it is over bare wallpaper. */
    void drawDropCell(final GuiGraphics g, final int cell) {
        final int gx = xOf(cell) + CELL_DX;
        final int gy = yOf(cell) + CELL_DY;
        g.fill(gx, gy, gx + CELL_W, gy + 1, 0x804C84F0);
        g.fill(gx, gy + CELL_H - 1, gx + CELL_W, gy + CELL_H, 0x804C84F0);
        g.fill(gx, gy, gx + 1, gy + CELL_H, 0x804C84F0);
        g.fill(gx + CELL_W - 1, gy, gx + CELL_W, gy + CELL_H, 0x804C84F0);
    }

    /** What an icon is called: a launcher's own name, or the file's, or what is being typed over it. */
    private String labelOf(final int i, final List<DesktopScreen.Launcher> launchers,
                           final List<DiskFilesPayload.WireFile> files) {
        if (i < launchers.size()) {
            return launchers.get(i).label();
        }
        final int di = i - launchers.size();
        return di == desktop.renamingIcon() ? desktop.renameText() + "_" : baseName(files.get(di).path());
    }

    /** One icon's cell and its picture: the highlight behind it, the drop outline, then the icon itself. */
    private void drawCell(final GuiGraphics g, final int i, final int ix, final int iy,
                          final int lmx, final int lmy, final boolean dragging, final int dropTarget,
                          final int launcherCount, final List<DiskFilesPayload.WireFile> files) {
        final int cellX = ix + CELL_DX;
        final int cellY = iy + CELL_DY;
        if (i == desktop.pickedIcon() || selected.contains(i)) {
            g.fill(cellX, cellY, cellX + CELL_W, cellY + CELL_H, 0x66000080);
        } else if (lmx >= cellX && lmx < cellX + CELL_W && lmy >= cellY && lmy < cellY + CELL_H && !dragging) {
            // Hover feedback so the player sees which icon the cursor is over.
            g.fill(cellX, cellY, cellX + CELL_W, cellY + CELL_H, 0x28FFFFFF);
        }
        /*
         * A green outline on the folder, or the trash, under the cursor while a real file or folder is being
         * dragged. A launcher has no file to move into a folder, so dragging one lights nothing.
         */
        if (dragging && desktop.draggedIconSlot() >= launcherCount
                && i == dropTarget && i != desktop.draggedIconSlot()
                && (i >= launcherCount ? files.get(i - launcherCount).directory() : desktop.isTrashIcon(i))) {
            g.fill(cellX, cellY, cellX + CELL_W, cellY + 1, 0xFF49E07A);
            g.fill(cellX, cellY + CELL_H - 1, cellX + CELL_W, cellY + CELL_H, 0xFF49E07A);
            g.fill(cellX, cellY, cellX + 1, cellY + CELL_H, 0xFF49E07A);
            g.fill(cellX + CELL_W - 1, cellY, cellX + CELL_W, cellY + CELL_H, 0xFF49E07A);
        }
        if (i < launcherCount) {
            ProgramIcons.draw(g, ix, iy, 24, 22, desktop.deskIcons().get(i).programId(), desktop.icons());
        } else {
            drawFileIcon(g, ix, iy, files.get(i - launcherCount));
        }
    }

    /** A name under its icon: centred, wrapped inside its own cell, and cut with an ellipsis past two lines. */
    private void drawName(final GuiGraphics g, final String label, final int ix, final int iy) {
        int ly = iy + 23;
        final List<String> lines = wrap(label, labelWidth());
        for (int li = 0; li < lines.size() && li < LABEL_LINES; li++) {
            final String line = li == LABEL_LINES - 1 && lines.size() > LABEL_LINES
                    ? fit(lines.get(li) + "...")
                    : fit(lines.get(li));
            drawLine(g, line, ix + 12, ly, desktop.themeColours().iconText(),
                    desktop.themeColours().textShadow());
            ly += LABEL_LINE_H;
        }
    }

    /** The picked icon reveals its whole name, wrapped, on a selection background, as a desktop always has. */
    private void drawPickedName(final GuiGraphics g, final String label, final int ix, final int iy) {
        int ly = iy + 23;
        for (final String line : wrap(label, labelWidth())) {
            final int lw = Texts.smallWidth(desktop.textFont(), line);
            final int lcx = ix + 12 - lw / 2;
            g.fill(lcx - 2, ly - 1, lcx + lw + 2, ly + LABEL_LINE_H, 0xE0000080);
            Texts.small(g, desktop.textFont(), line, lcx, ly, 0xFFFFFFFF);
            ly += LABEL_LINE_H;
        }
    }

    /** One label line, centred under the icon and drawn in the small text, with the theme's shadow. */
    private void drawLine(final GuiGraphics g, final String line, final int cx, final int y,
                          final int color, final boolean shadow) {
        final int lx = cx - Texts.smallWidth(desktop.textFont(), line) / 2;
        if (shadow) {
            Texts.small(g, desktop.textFont(), line, lx + 1, y + 1, 0xFF000000);
        }
        Texts.small(g, desktop.textFont(), line, lx, y, color);
    }

    private int labelWidth() {
        return Texts.smallFits(LABEL_W);
    }

    /**
     * One line, cut with an ellipsis when a single unbreakable word is wider than its cell. The test is the
     * width the line is actually drawn at, not the width it would have at full size, or a name that fits
     * its cell by a pixel gets cut for no reason.
     */
    private String fit(final String s) {
        if (Texts.smallWidth(desktop.textFont(), s) <= LABEL_W) {
            return s;
        }
        final int units = Math.max(1, labelWidth() - desktop.textFont().width("..."));
        return desktop.textFont().plainSubstrByWidth(s, units) + "...";
    }

    /** Breaks a name on its spaces, keeping at most three lines; the drawing takes the first two. */
    private List<String> wrap(final String s, final int maxW) {
        final List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (final String word : s.split(" ")) {
            final String cand = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() == 0 || desktop.textFont().width(cand) <= maxW) {
                cur = new StringBuilder(cand);
            } else {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        while (out.size() > 3) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    /** A folder or document icon (about 24 by 22) for a desktop file entry. */
    private static void drawFileIcon(final GuiGraphics g, final int x, final int y,
                                     final DiskFilesPayload.WireFile f) {
        if (f.directory()) {
            g.fill(x + 1, y + 1, x + 10, y + 4, 0xFFFFE9A8);   // tab
            g.fill(x + 1, y + 4, x + 23, y + 20, 0xFFF4C842);  // body
            g.fill(x + 1, y + 4, x + 23, y + 6, 0xFFFFF3C4);   // highlight
            outline(g, x + 1, y + 1, 22, 19, 0xFF9A7B16);
            return;
        }
        final int fill;
        final int edge;
        switch (f.ext().toLowerCase(Locale.ROOT)) {
            case "iql" -> { fill = 0xFFA9D4FF; edge = 0xFF3A72B0; }
            case "dat" -> { fill = 0xFFBDEEC0; edge = 0xFF4F9B53; }
            default -> { fill = 0xFFEDEFF3; edge = 0xFF8A93A6; }
        }
        g.fill(x + 4, y + 1, x + 21, y + 21, fill);        // sheet
        g.fill(x + 16, y + 1, x + 21, y + 6, 0xFFFFFFFF);  // folded corner
        outline(g, x + 4, y + 1, 17, 20, edge);
    }

    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
