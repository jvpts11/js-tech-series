/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.MoveFilePayload;
import dev.jstech.computers.operation.payload.SetIconPositionPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.layout.DesktopZ;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What the pointer carries across the wallpaper: an icon being dragged (a file, a folder or a program) onto a folder,
 * an open file manager, the trash or a free grid cell; a file dragged out of a file manager and dropped on the
 * desktop, the trash or another file manager; and the rubber band swept over bare wallpaper to pick every icon it
 * touches. A {@code .dat} is a projection of a drive's items and cannot be moved by hand, so a move of one raises the
 * locked-file dialog instead.
 */
final class DesktopDrags {

    private final DesktopState desktop;
    /** Whether the rubber band is being swept, from where, and to where the pointer has taken it. */
    private boolean bandActive;
    private double bandStartX;
    private double bandStartY;
    private double bandX;
    private double bandY;
    /** The icon slot armed for a drag, or -1, and whether the pointer has left the click's dead zone yet. */
    private int iconSlot = -1;
    private boolean iconDragging;
    private double iconX;
    private double iconY;
    private double iconStartX;
    private double iconStartY;

    /** How far the pointer must travel from the press before an icon click becomes a drag. */
    private static final double DRAG_THRESHOLD = 3.0;

    DesktopDrags(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Whether an icon is being dragged, which slot, and where the pointer has taken it. */
    boolean iconDragging() {
        return iconDragging;
    }

    int iconSlot() {
        return iconSlot;
    }

    double iconX() {
        return iconX;
    }

    double iconY() {
        return iconY;
    }

    /**
     * Arms a drag of the icon in {@code slot}, pressed at a desktop-local point. Any icon can be repositioned, a
     * program's as well as a file's (a program's only ever pins to a cell); the drag does not begin until the pointer
     * leaves a small dead zone, so a click or a double click never turns into an accidental move.
     */
    void armIcon(final int slot, final double x, final double y) {
        iconSlot = slot;
        iconDragging = false;
        iconStartX = x;
        iconStartY = y;
    }

    /** Starts the rubber band at a desktop-local point, when that point is bare wallpaper in the work area. */
    void startBand(final double x, final double y) {
        final DesktopViewport view = desktop.view();
        if (y >= view.workAreaTop() && y < view.workAreaBottom() && overWallpaper(x, y)) {
            bandActive = true;
            bandStartX = x;
            bandStartY = y;
            bandX = x;
            bandY = y;
        }
    }

    /**
     * The pointer moved to a desktop-local point with a button held: an armed icon follows it, once it has left the
     * dead zone, and the band grows and picks again what it covers. Returns whether either took the move.
     */
    boolean drag(final double x, final double y) {
        if (iconSlot >= 0) {
            iconX = x;
            iconY = y;
            if (!iconDragging && (Math.abs(x - iconStartX) > DRAG_THRESHOLD
                    || Math.abs(y - iconStartY) > DRAG_THRESHOLD)) {
                iconDragging = true;
            }
            return true;
        }
        if (bandActive) {
            bandX = x;
            bandY = y;
            desktop.iconGrid().selectWithin(bandRect());
            return true;
        }
        return false;
    }

    /** Letting go ends the sweep, whatever it covered staying picked. Returns whether a sweep was going on. */
    boolean endBand() {
        if (!bandActive) {
            return false;
        }
        bandActive = false;
        return true;
    }

    /**
     * Letting go of an armed icon: a dragged one is dropped at the desktop-local point, and the arming ends either
     * way. Returns whether an icon had been dragged, in which case the release was the drop and nothing else's.
     */
    boolean dropIcon(final double x, final double y) {
        final boolean dragged = iconDragging && iconSlot >= 0;
        if (dragged) {
            drop(x, y);
        }
        iconDragging = false;
        iconSlot = -1;
        return dragged;
    }

    /**
     * A file dragged out of the file manager in front and let go over the bare desktop, the trash or another file
     * manager: it moves into the desktop folder or into the folder that other window shows, or goes to the trash.
     * Returns whether it took the release, so the file manager's own drop inside its window does not also run; a
     * release back inside the file manager it came from is left to it.
     */
    boolean dropFromFileManager(final double dx, final double dy) {
        final DesktopWindow front = desktop.wm().front();
        if (front == null || !(front.app() instanceof FilesApp origin) || !origin.isDragging()) {
            return false;
        }
        final DiskFilesPayload.WireFile dragged = origin.draggedFile();
        if (dragged == null) {
            return false;
        }
        if (dx >= front.x() && dx <= front.x() + front.width() && dy >= front.y() && dy <= front.y() + front.height()) {
            return false;
        }
        if (overTrash(dx, dy)) {
            if (dragged.readOnly()) {
                desktop.notices().datLocked();
            } else {
                DeskTrash.delete(desktop.hostPos(), List.of(dragged.path()));
            }
            origin.cancelDrag();
            return true;
        }
        final DesktopWindow other = fileManagerAt(dx, dy);
        if (other != null && other != front && other.app() instanceof FilesApp dest) {
            moveFrom(origin, dest, dragged, dest.crossWindowDropDir(other, dx, dy));
            origin.cancelDrag();
            return true;
        }
        final DesktopViewport view = desktop.view();
        if (dy >= view.workAreaTop() && dy < view.workAreaBottom() && overWallpaper(dx, dy)) {
            moveFrom(origin, null, dragged, desktop.deskDir());
            origin.cancelDrag();
            return true;
        }
        return false;
    }

    /**
     * Draws what a drag shows: the grid cell a dragged icon would land on and its name trailing the pointer, and the
     * rubber band, a translucent fill with a solid outline, the way every desktop draws one.
     */
    void render(final GuiGraphics g, final int sw, final int tbY, final int perCol) {
        renderIcon(g, sw, tbY, perCol);
        if (!bandActive) {
            return;
        }
        final int[] r = bandRect();
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.ICONS + 1);
        final DesktopShellPalette.Colours c = DesktopShellPalette.get();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], c.bandFill());
        g.fill(r[0], r[1], r[0] + r[2], r[1] + 1, c.bandEdge());
        g.fill(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], c.bandEdge());
        g.fill(r[0], r[1], r[0] + 1, r[1] + r[3], c.bandEdge());
        g.fill(r[0] + r[2] - 1, r[1], r[0] + r[2], r[1] + r[3], c.bandEdge());
        g.pose().popPose();
    }

    /**
     * Whether a desktop-local point is on the bare wallpaper, clear of every window on show, which is where a free
     * icon drop snaps to a cell and where a file dragged across windows lands on the desktop.
     */
    boolean overWallpaper(final double mx, final double my) {
        final DesktopWindows wm = desktop.wm();
        for (final DesktopWindow w : wm.all()) {
            if (!wm.away(w) && mx >= w.x() && mx <= w.x() + w.width() && my >= w.y() && my <= w.y() + w.height()) {
                return false;
            }
        }
        return true;
    }

    /** The band's rectangle in desktop-local coordinates: {x, y, w, h}. */
    private int[] bandRect() {
        final int bx = (int) Math.min(bandStartX, bandX);
        final int by = (int) Math.min(bandStartY, bandY);
        return new int[] {bx, by, (int) Math.abs(bandX - bandStartX), (int) Math.abs(bandY - bandStartY)};
    }

    /**
     * What an icon being dragged shows: the grid cell it would snap to when it is over free wallpaper (not over a
     * folder, whose own outline wins, and not off the wallpaper, where the drop does nothing), and its name.
     */
    private void renderIcon(final GuiGraphics g, final int sw, final int tbY, final int perCol) {
        if (!iconDragging || iconSlot < 0) {
            return;
        }
        final DesktopIcons grid = desktop.iconGrid();
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.DRAG);
        if (grid.slotAt(iconX, iconY, perCol) < 0 && iconX < sw && iconY < tbY && overWallpaper(iconX, iconY)) {
            grid.drawDropCell(g, grid.cellAt(iconX, iconY, perCol));
        }
        final List<Launcher> icons = desktop.deskIcons();
        final List<DiskFilesPayload.WireFile> files = desktop.deskFiles();
        if (iconSlot < icons.size() + files.size()) {
            final String label = iconSlot < icons.size() ? icons.get(iconSlot).label()
                    : DesktopIcons.baseName(files.get(iconSlot - icons.size()).path());
            final Font font = desktop.textFont();
            final int gx = (int) iconX + 6;
            final int gy = (int) iconY + 2;
            final DesktopShellPalette.Colours c = DesktopShellPalette.get();
            g.fill(gx, gy, gx + font.width(label) + 6, gy + 12, c.ghostFill());
            Draw.text(g, font, label, gx + 3, gy + 2, c.ghostInk());
        }
        g.pose().popPose();
    }

    /**
     * Drops the dragged icon at a desktop-local point. In order: on the trash, a file is deleted; on an open file
     * manager, it moves into the folder that window shows; on a folder's icon, it moves inside; and on bare
     * wallpaper, the icon is pinned to the grid cell there, unless another icon already holds it, so two never
     * stack. A program's icon has no file behind it, so for it only the pinning applies.
     */
    private void drop(final double dx, final double dy) {
        final List<Launcher> icons = desktop.deskIcons();
        final List<DiskFilesPayload.WireFile> files = desktop.deskFiles();
        if (iconSlot >= icons.size() + files.size()) {
            return;
        }
        final DiskFilesPayload.WireFile src = iconSlot < icons.size() ? null : files.get(iconSlot - icons.size());
        if (src != null && overTrash(dx, dy)) {
            if (src.readOnly()) {
                desktop.notices().datLocked();
            } else {
                desktop.iconGrid().forget(src.path());
                DeskTrash.delete(desktop.hostPos(), List.of(src.path()));
            }
            return;
        }
        final DesktopWindow fileManager = fileManagerAt(dx, dy);
        if (fileManager != null && fileManager.app() instanceof FilesApp to && src != null) {
            final String destDir = to.crossWindowDropDir(fileManager, dx, dy);
            if (destDir != null && !sameParent(src.path(), destDir)) {
                if (src.readOnly()) {
                    desktop.notices().datLocked();
                } else {
                    PacketDistributor.sendToServer(new MoveFilePayload(desktop.hostPos(), src.path(), destDir));
                    desktop.iconGrid().forget(src.path());
                    to.refresh();
                    desktop.requestDesktop();
                }
            }
            return;
        }
        final DesktopIcons grid = desktop.iconGrid();
        final int perCol = grid.perColumn();
        final int target = grid.slotAt(dx, dy, perCol);
        if (target >= icons.size() && target != iconSlot && src != null) {
            final DiskFilesPayload.WireFile dst = files.get(target - icons.size());
            if (dst.directory()) {
                if (src.readOnly()) {
                    desktop.notices().datLocked();
                } else {
                    PacketDistributor.sendToServer(new MoveFilePayload(desktop.hostPos(), src.path(), dst.path()));
                    grid.forget(src.path());
                    desktop.requestDesktop();
                }
                return;
            }
        }
        final DesktopViewport view = desktop.view();
        if (dy >= view.workAreaTop() && dy < view.workAreaBottom() && overWallpaper(dx, dy)) {
            final int cell = grid.cellAt(dx, dy, perCol);
            if (grid.cellTaken(cell, iconSlot, perCol)) {
                return;
            }
            final String key = grid.keyOf(iconSlot);
            grid.pin(key, cell);
            PacketDistributor.sendToServer(new SetIconPositionPayload(desktop.hostPos(), key, cell));
        }
    }

    /** Whether a desktop-local point is on the trash: its icon on the wallpaper, or CDE's control for it. */
    private boolean overTrash(final double dx, final double dy) {
        final DesktopViewport view = desktop.view();
        if (desktop.panelStyle() == PanelStyle.CDE) {
            return CdeFrontPanelLayout.controlAt(dx, dy, view.width(), view.height())
                    == CdeFrontPanelLayout.Control.TRASH;
        }
        final DesktopIcons grid = desktop.iconGrid();
        return desktop.isTrashIcon(grid.slotAt(dx, dy, grid.perColumn())) && overWallpaper(dx, dy);
    }

    /**
     * Moves a file dragged out of {@code origin} into {@code destDir}, and has the file managers and the desktop
     * list their folders again. A move into the folder it is already in does nothing.
     */
    private void moveFrom(final FilesApp origin, @Nullable final FilesApp dest, final DiskFilesPayload.WireFile dragged,
                          @Nullable final String destDir) {
        if (destDir == null || sameParent(dragged.path(), destDir)) {
            return;
        }
        if (dragged.readOnly()) {
            desktop.notices().datLocked();
            return;
        }
        PacketDistributor.sendToServer(new MoveFilePayload(desktop.hostPos(), dragged.path(), destDir));
        origin.refresh();
        if (dest != null) {
            dest.refresh();
        }
        desktop.requestDesktop();
    }

    /** The topmost file manager on show whose window is under a desktop-local point, or null. */
    @Nullable
    private DesktopWindow fileManagerAt(final double mx, final double my) {
        final DesktopWindows wm = desktop.wm();
        final List<DesktopWindow> windows = wm.all();
        for (int i = windows.size() - 1; i >= 0; i--) {
            final DesktopWindow w = windows.get(i);
            if (!wm.away(w) && w.app() instanceof FilesApp
                    && mx >= w.x() && mx <= w.x() + w.width() && my >= w.y() && my <= w.y() + w.height()) {
                return w;
            }
        }
        return null;
    }

    /** Whether {@code destDir} is already the folder {@code srcPath} is in, which makes a move a no-op. */
    private static boolean sameParent(final String srcPath, final String destDir) {
        final int slash = srcPath.lastIndexOf('/');
        final String parent = slash < 0 ? "" : srcPath.substring(0, slash);
        return parent.equals(destDir);
    }
}
