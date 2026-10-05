/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.client.MonitorFrame;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.motion.FadeLayer;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionStyles;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * Paints the desktop, back to front: the monitor's frame around the glass, the wallpaper, the icons, the windows and
 * the inventory laid over the one in front, the panel and what sits on it, the menus, whatever is being dragged, and
 * over the finished desktop the tooltips, the carried stack, the brightness dim and the dialogs.
 *
 * <p>Each layer draws at a depth of its own ({@link DesktopZ}), strictly increasing, so the depth buffer keeps a back
 * layer behind a front one: the batched text of a back layer (an icon's name) can never paint over a front one (an
 * open window). Flushing the text batch between layers does not do it, since a flush outside a managed draw does
 * nothing on this version of the game, which is why the icon-name-over-window fault kept coming back.
 */
final class DesktopPainter {

    private final DesktopState desktop;
    /** The inventory slot under the pointer this frame, or null. */
    @Nullable
    private Slot hovered;
    /* Where the pointer came to rest when the hover hints last started coming up, and their coming up. */
    private int tipX = FAR;
    private int tipY = FAR;
    private Motion tipIn = Motion.FINISHED;

    /** A pointer far off the glass, for a window drawn going away, which nothing in it should light up for. */
    private static final int FAR = -10000;
    /** How far the pointer may drift, in desktop pixels, before the hint under it counts as a new one. */
    private static final int TIP_SLACK = 2;
    /** How far above the carried stack the pointer is drawn. */
    private static final int POINTER_LIFT = 50;
    /** No place at all, for a motion that goes nowhere but grows or shrinks where it is. */
    private static final int[] NOWHERE = {0, 0, 0, 0};

    DesktopPainter(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The inventory slot under the pointer as the last frame found it, or null. */
    @Nullable
    Slot hovered() {
        return hovered;
    }

    /**
     * Paints the desktop with the pointer at a desktop-local point. Returns false when a cooperative kernel's crash
     * screen is all there is to draw, until the machine reboots.
     */
    boolean paint(final GuiGraphics g, final int lmx, final int lmy, final float partialTick) {
        final DesktopViewport view = desktop.view();
        final DesktopPrefs prefs = desktop.prefs();
        final int sw = view.width();
        final int sh = view.height();
        final int ox = view.left();
        final int oy = view.top();
        desktop.taskPopup().update(lmx, lmy, sw, sh - view.panelBand());
        // The monitor's frame wraps the glass, and the desktop draws inside it in its own coordinates and scale.
        MonitorFrame.renderBody(g, ox, oy, view.glassWidth(), view.glassHeight(), prefs.era(), desktop.textFont());
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        g.pose().scale((float) view.scale(), (float) view.scale(), 1);
        g.enableScissor(ox, oy, ox + view.glassWidth(), oy + view.glassHeight());
        renderWallpaper(g, sw, sh);
        // A cooperative system that ran out of memory shows its crash screen, then reboots to an empty session.
        final DesktopMemory memory = desktop.memory();
        if (memory.crashing()) {
            if (memory.crashOver()) {
                desktop.reboot();
            } else {
                memory.renderCrash(g, sw, sh);
                g.disableScissor();
                g.pose().popPose();
                return false;
            }
        }
        final boolean cde = desktop.panelStyle() == PanelStyle.CDE;
        final DesktopIcons grid = desktop.iconGrid();
        final int perCol = grid.perColumn();
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.ICONS);
        // KDE 4 stands its icons in a Folder View, with the cashew in the corner of its desktop.
        if (desktop.kde4()) {
            desktop.transitionPanels().renderKde4Desktop(g, sw, lmx, lmy);
        }
        grid.render(g, lmx, lmy);
        // CDE stands a window that was put away on its workspace as an icon, having no panel to list it on.
        if (cde) {
            desktop.cdeWindowIcons().render(g, desktop.wm().putAwayHere(), sw, view.workAreaTop(),
                    prefs.cdePalette());
        }
        g.pose().popPose();
        renderWindows(g, lmx, lmy, partialTick, sw, sh);
        final int tbY = sh - view.panelBand();
        renderPanelLayer(g, tbY, sw, sh, lmx, lmy);
        renderMenus(g, tbY, lmx, lmy, partialTick);
        desktop.drags().render(g, sw, tbY, perCol);
        g.disableScissor();
        renderOverlays(g, sw, sh, lmx, lmy, partialTick);
        renderVeil(g, sw, sh);
        // The player's pointer in the system's own cursors, over everything on the glass and kept to it.
        if (desktop.surface().ownPointer() && lmx >= 0 && lmy >= 0 && lmx < sw && lmy < sh) {
            g.enableScissor(ox, oy, ox + view.glassWidth(), oy + view.glassHeight());
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.CURSOR + POINTER_LIFT);
            desktop.pointers().draw(g, lmx, lmy);
            g.pose().popPose();
            g.disableScissor();
        }
        g.pose().popPose();
        return true;
    }

    /** The boot picture's colour still giving way to the desktop it came up into, over all of it. */
    private void renderVeil(final GuiGraphics g, final int sw, final int sh) {
        final SceneHandoff.Veil veil = desktop.veil();
        final int alpha = veil.alpha(DesktopMotion.now());
        if (alpha <= 0) {
            return;
        }
        g.pose().pushPose();
        // Above the dialogs and whatever they lift, since it covers the whole of the desktop.
        g.pose().translate(0, 0, DesktopZ.POPUP + DesktopZ.DECORATION_LIFT + DesktopZ.ITEM_DEPTH);
        g.fill(0, 0, sw, sh, alpha << 24 | veil.colour() & 0xFFFFFF);
        g.pose().popPose();
    }

    /**
     * The wall behind everything. CDE hangs no picture: each workspace wears a pattern of its own in the palette's
     * backdrop colours. Elsewhere a picture a player drew hangs in front of the built-in wallpapers, and falls back to
     * them the moment it cannot be found, so a deleted drawing never leaves the desktop with a blank wall.
     */
    private void renderWallpaper(final GuiGraphics g, final int sw, final int sh) {
        final DesktopPrefs prefs = desktop.prefs();
        if (desktop.panelStyle() == PanelStyle.CDE) {
            MotifChrome.backdrop(g, sw, sh, prefs.cdePalette(), prefs.cdeStyle().backdrop(desktop.wm().workspace()));
            return;
        }
        PixWallpaper.want(desktop.hostPos(), prefs.wallpaper());
        if (!PixWallpaper.paint(g, sw, sh)) {
            WallpaperPainter.paint(g, sw, sh, desktop.desktopId(), desktop.platform(), prefs.wallpaper(),
                    prefs.darkMode() && (desktop.panelStyle() == PanelStyle.FRAMES_11
                            || desktop.panelStyle() == PanelStyle.FRAMES_10));
        }
    }

    /**
     * The open windows, back to front, and the real container items of whichever one carries the player's inventory.
     * Each window draws in a depth band of its own: an item is a model standing well in front of the pose it is drawn
     * at, so windows sharing one depth painted their items over one another.
     */
    private void renderWindows(final GuiGraphics g, final int lmx, final int lmy, final float partialTick,
                               final int sw, final int sh) {
        final DesktopWindows wm = desktop.wm();
        final DesktopViewport view = desktop.view();
        final DesktopWindow front = wm.front();
        final double now = DesktopMotion.now();
        wm.settleClosing(now);
        final int count = wm.all().size();
        for (int i = 0; i < count; i++) {
            final DesktopWindow w = wm.all().get(i);
            w.follow(desktop.motion());
            final boolean moving = !w.motion().done(now);
            /*
             * A window on a workspace that is not up is not drawn at all, which is what makes them cost nothing; one
             * put away a moment ago is drawn on its way down to its button until it gets there.
             */
            if (wm.away(w) && !(moving && w.minimized() && w.on(wm.workspace()))) {
                continue;
            }
            w.setFocused(w == front);
            drawWindow(g, w, DesktopZ.windowZ(i, count), moving, now, lmx, lmy, partialTick, sw, sh);
        }
        // A window just closed is drawn going away over the rest, where it stood in front of them.
        for (final DesktopWindow w : wm.closing()) {
            drawWindow(g, w, DesktopZ.windowZ(count, count + 1), true, now, FAR, FAR, partialTick, sw, sh);
        }
        // The container's own items, over the window that already drew their slots' backgrounds, moving with it.
        final boolean frontMoving = front != null && !front.motion().done(now);
        if (frontMoving && (front.motion().is(MotionStyles.OUTLINE) || front.motion().is(MotionStyles.CAPTION))) {
            // Only its outline or its title bar is on the glass, so its items wait for the window to be back.
            hovered = null;
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.INVENTORY);
        if (frontMoving) {
            poseInMotion(g, front, now);
        }
        hovered = desktop.band().render(g, lmx, lmy);
        g.pose().popPose();
    }

    /**
     * One window at its depth, in the motion it is in: grown, shrunk or on its way to its button, or, for a motion
     * that carries only the outline, the outline alone with the window not yet back or already gone.
     */
    private void drawWindow(final GuiGraphics g, final DesktopWindow w, final int z, final boolean moving,
                            final double now, final int lmx, final int lmy, final float partialTick, final int sw,
                            final int sh) {
        final DesktopViewport view = desktop.view();
        // Where it stands this frame, before any motion is read against it.
        w.resolveGeometry(sw, sh, view.panelReserve(), view.workAreaTop());
        g.pose().pushPose();
        g.pose().translate(0, 0, z);
        if (moving && w.motion().is(MotionStyles.OUTLINE)) {
            final int[] to = desktop.taskbar().entryRect(w.groupKey());
            DesktopMotion.outline(g, w.motion(), now, w.x(), w.y(), w.width(), w.height(), to[0], to[1], to[2],
                    to[3]);
        } else if (moving && w.motion().is(MotionStyles.CAPTION)) {
            final int[] to = desktop.taskbar().entryRect(w.groupKey());
            final int[] at = DesktopMotion.between(w.motion(), now, w.x(), w.y(), w.width(), DesktopWindow.TITLE_H,
                    to[0], to[1], to[2], to[3]);
            w.renderCaption(g, desktop.textFont(), desktop.prefs().skin(), at[0], at[1], at[2], at[3]);
        } else {
            final Runnable draw = () -> {
                if (moving) {
                    poseInMotion(g, w, now);
                }
                w.render(g, desktop.textFont(), desktop.prefs().skin(), lmx, lmy, partialTick, sw, sh,
                        view.panelReserve(), view.workAreaTop());
            };
            // A window fading in or out is drawn whole off the glass first, so it fades as one picture.
            if (moving && w.motion().fades()) {
                FadeLayer.draw(g, (float) w.motion().opacity(now), draw);
            } else {
                draw.run();
            }
        }
        g.pose().popPose();
    }

    /** Moves the pose to where a window in motion is drawn this frame, its button being where it goes down to. */
    private void poseInMotion(final GuiGraphics g, final DesktopWindow w, final double now) {
        final int[] to = w.motion().is(MotionStyles.ZOOM) ? desktop.taskbar().entryRect(w.groupKey()) : NOWHERE;
        DesktopMotion.pose(g, w.motion(), now, w.x(), w.y(), w.width(), w.height(), to[0], to[1], to[2], to[3]);
    }

    /**
     * The panel this desktop wears, and the three things that sit just above it: a tray balloon, the figures behind
     * the notification area, and the popup listing one program's windows.
     */
    private void renderPanelLayer(final GuiGraphics g, final int tbY, final int sw, final int sh, final int lmx,
                                  final int lmy) {
        final PanelStyle style = desktop.panelStyle();
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.TASKBAR);
        if (style == PanelStyle.CDE) {
            // CDE has no bar at all: a slab of controls at the bottom centre, in its palette's relief.
            desktop.cdePanels().render(g, sw, sh, desktop.prefs().cdePalette());
        } else if (style == PanelStyle.FRAMES_11) {
            desktop.framesPanels().renderModern(g, tbY, sw, lmx, lmy);
        } else if (style == PanelStyle.FRAMES_7) {
            desktop.aeroSuperbar().render(g, tbY, sw, lmx, lmy);
        } else if (style == PanelStyle.FRAMES_10) {
            desktop.metroTaskbar().render(g, tbY, sw, lmx, lmy);
        } else if (desktop.kde4()) {
            desktop.transitionPanels().renderKde4(g, tbY, sw, sh, lmx, lmy);
        } else if (desktop.gnome2()) {
            desktop.transitionPanels().renderGnome2(g, tbY, sw, sh, lmx, lmy);
        } else if (desktop.periodPanel()) {
            // A Legacy-era Unix desktop's panel, drawn out of the skin's own relief.
            desktop.linuxPanels().renderPeriod(g, tbY, sw, sh, lmx, lmy);
        } else if (style == PanelStyle.GNOME) {
            desktop.linuxPanels().renderGnomeTopBar(g, sw, lmx, lmy);
        } else if (desktop.linuxDesktop()) {
            desktop.linuxPanels().renderModern(g, tbY, sw, sh, lmx, lmy);
        } else {
            desktop.framesPanels().renderClassic(g, tbY, sw, sh, lmx, lmy);
        }
        g.pose().popPose();
        // Plasma's copy notification and GNOME's copy popover sit above the panel, as the balloon does.
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.TASKBAR + 9);
        desktop.copies().renderOverlay(g, tbY, sw);
        g.pose().popPose();
        // A tray balloon sits above the panel and under the menus, so opening the launcher covers it.
        final DesktopNotices notices = desktop.notices();
        if (notices.balloonDrawn()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.TASKBAR + 10);
            notices.renderBalloon(g, tbY, sw);
            g.pose().popPose();
        }
        // The figures behind the notification area, while the pointer rests on it. CDE has no such area.
        if (!desktop.view().barOnTop() && style != PanelStyle.CDE) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.TASKBAR + 8);
            desktop.tray().drawTip(g, tbY, sw);
            g.pose().popPose();
        }
        // The windows of one program, over the panel and the windows themselves.
        final TaskPopup popup = desktop.taskPopup();
        if (popup.key() != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            popup.render(g, tbY, sw, sh, lmx, lmy);
            g.pose().popPose();
        }
    }

    /**
     * The menus that share a height above the panel: the launcher, the panel's own, the desktop's, and the volume
     * control with its menu.
     */
    private void renderMenus(final GuiGraphics g, final int tbY, final int lmx, final int lmy,
                             final float partialTick) {
        final DesktopViewport view = desktop.view();
        final DesktopPrefs prefs = desktop.prefs();
        final Font font = desktop.textFont();
        // The name of a Front Panel control rides at the menus' height, so no window can stand over it.
        if (desktop.panelStyle() == PanelStyle.CDE && !desktop.menuOrDialogOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            desktop.cdePanels().renderTip(g, view.width(), view.height(), prefs.cdePalette());
            g.pose().popPose();
        }
        final VolumePopup volume = desktop.volume();
        if (volume.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            volume.render(g, new UiContext(prefs.skin(), font, lmx, lmy, partialTick), view.width(), tbY,
                    view.barOnTop());
            g.pose().popPose();
        }
        // Frames 10's Task View over the windows, and its Action Center down the edge.
        if (desktop.taskView().isOpen() || desktop.actionCenter().isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            desktop.taskView().render(g, view.width(), tbY, lmx, lmy);
            desktop.actionCenter().render(g, tbY, view.width(), lmx, lmy);
            g.pose().popPose();
        }
        final DeskMenu deskMenu = desktop.deskMenu();
        final CdeLaunchers subpanels = desktop.cdeLaunchers();
        if (!desktop.start().isOpen() && !deskMenu.isOpen() && !desktop.panelMenu().isOpen() && !subpanels.isOpen()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.MENU);
        desktop.start().render(g, tbY);
        // A subpanel of CDE's Front Panel is no launcher that comes and goes: it stays up until its arrow says so.
        subpanels.render(g, view.width(), view.height(), prefs.cdePalette());
        desktop.panelMenu().render(g, lmx, lmy);
        if (deskMenu.isOpen()) {
            deskMenu.render(g, new UiContext(prefs.skin(), font, lmx, lmy, partialTick));
        }
        g.pose().popPose();
    }

    /**
     * Everything that sits over the finished desktop, in the order it stacks: hover tooltips, the stack on the
     * cursor, the brightness dim, a program's own modal dialog, a dialog over the whole desktop, the power dialog,
     * and a program's menu from its panel entry.
     */
    private void renderOverlays(final GuiGraphics g, final int sw, final int sh, final int lmx, final int lmy,
                                final float partialTick) {
        final Font font = desktop.textFont();
        final DesktopPrefs prefs = desktop.prefs();
        /*
         * Hover tooltips draw at the base pose because the game's tooltip drawing lifts itself by 400, landing them
         * above every window and the panel. The front window's program draws its own hover hints; the inventory
         * zone shows the real slot's item.
         */
        final DesktopWindow front = desktop.wm().front();
        if (front != null) {
            // A hint comes up again wherever the pointer comes to rest: faded in on a system whose hints fade.
            if (Math.abs(lmx - tipX) > TIP_SLACK || Math.abs(lmy - tipY) > TIP_SLACK) {
                tipX = lmx;
                tipY = lmy;
                tipIn = desktop.motion().start(MotionKinds.TOOLTIP_SHOW);
            }
            FadeLayer.draw(g, (float) tipIn.opacity(DesktopMotion.now()), () -> front.renderTooltip(g, font, lmx,
                    lmy));
        }
        if (hovered != null && desktop.carried().isEmpty() && hovered.hasItem()) {
            g.renderTooltip(font, hovered.getItem(), lmx, lmy);
        }
        // The stack on the cursor rides above the tooltip, at the pointer.
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.CURSOR);
        desktop.band().renderCarried(g, lmx, lmy);
        g.pose().popPose();
        // Brightness: a dim over the whole surface, from none at 100 to deeply dimmed at 0.
        if (prefs.brightness() < 100) {
            final int alpha = Math.min(210, (100 - prefs.brightness()) * 21 / 10);
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP - 1);
            g.fill(0, 0, sw, sh, alpha << 24);
            g.pose().popPose();
        }
        // A program's own modal dialog draws above every item and window, so its dim covers them rather than them
        // piercing through at their own depth.
        if (front != null && front.app().modalActive()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            front.app().renderModal(g, font, front.x() + 4, front.y() + DesktopWindow.TITLE_H + 4,
                    front.width() - 8, front.height() - DesktopWindow.TITLE_H - 8, lmx, lmy);
            g.pose().popPose();
        }
        desktop.notices().renderPopup(g, prefs.skin(), lmx, lmy, sw, sh);
        // The power dialog rides at the same height: it is the one choice that ends the session.
        final PowerDialog power = desktop.power();
        if (power.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            power.render(g, sw, sh, lmx, lmy);
            g.pose().popPose();
        }
        // A program's own menu, from its panel entry, sits above the windows it acts on.
        if (desktop.taskbar().menu().isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            desktop.taskbar().menu().render(g, new UiContext(prefs.skin(), font, lmx, lmy, partialTick));
            g.pose().popPose();
        }
        // So does a window's own menu on CDE, which hangs from the button at the left of its title bar.
        final CdeWindowMenu windowMenu = desktop.cdeWindowMenu();
        if (windowMenu.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            windowMenu.render(g, lmx, lmy, prefs.cdePalette());
            g.pose().popPose();
        }
    }
}
