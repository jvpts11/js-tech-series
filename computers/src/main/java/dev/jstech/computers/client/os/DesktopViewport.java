/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.os.PanelStyle;

/**
 * Where a desktop sits on the game's screen and how big it draws: the monitor's glass, centred in the game's window,
 * the scale the desktop is drawn at inside it, the arithmetic that turns a point on the screen into a point on the
 * desktop and back, and the work area its panel leaves to the windows.
 *
 * <p>Everything on a desktop is laid out in desktop pixels, over a desktop that is wider than the glass when it is
 * drawn smaller than its designed size; this is the one place that knows how the two relate.
 */
final class DesktopViewport {

    private final DesktopState desktop;
    /** How big the desktop draws everything, as a percentage of its designed size; 0 stands for the default. */
    private int scalePercent;

    /** The percentage the desktop is drawn at when the machine has not been told another: the size that reads best. */
    static final int DEFAULT_SCALE = 75;

    DesktopViewport(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** Draws the desktop at that percentage of its designed size from now on; 0 stands for the default. */
    void setScalePercent(final int percent) {
        this.scalePercent = percent;
    }

    /** The scale as a factor: three quarters unless the setting says otherwise. */
    double scale() {
        return (scalePercent <= 0 ? DEFAULT_SCALE : scalePercent) / 100.0;
    }

    /**
     * The glass's width on the screen, in screen pixels: what the frame wraps and the scissor clips. The glass is a
     * centred window rather than the whole game's, which leaves room for the monitor's frame around it and its chin.
     */
    int glassWidth() {
        return MonitorGlass.width(desktop.surface().surfaceWidth());
    }

    int glassHeight() {
        return MonitorGlass.height(desktop.surface().surfaceHeight());
    }

    /**
     * The desktop's width as the desktop sees it: the glass's, and more of it when the desktop is drawn smaller.
     * Everything laid out on the desktop uses this pair and is drawn under the scale.
     */
    int width() {
        return (int) Math.round(glassWidth() / scale());
    }

    int height() {
        return (int) Math.round(glassHeight() / scale());
    }

    /** The screen x of the glass's left edge, where the desktop's own x begins. */
    int left() {
        return (desktop.surface().surfaceWidth() - glassWidth()) / 2;
    }

    int top() {
        return (desktop.surface().surfaceHeight() - glassHeight()) / 2;
    }

    /** The screen x of a desktop-local x, for a hook that hands a test a point to click. */
    int screenX(final int local) {
        return left() + (int) Math.round(local * scale());
    }

    int screenY(final int local) {
        return top() + (int) Math.round(local * scale());
    }

    /**
     * Whether an absolute screen point is on the glass. The game hands the screen clicks from anywhere in its window,
     * the monitor's frame and chin included, and a point there is not on the desktop even when its local coordinates
     * fall where the taskbar is.
     */
    boolean onGlass(final double absX, final double absY) {
        return absX >= left() && absX < left() + glassWidth() && absY >= top() && absY < top() + glassHeight();
    }

    /** The desktop-local x of an absolute screen x, allowing for where the glass is and how it is scaled. */
    double localX(final double absX) {
        return (absX - left()) / scale();
    }

    double localY(final double absY) {
        return (absY - top()) / scale();
    }

    /**
     * An absolute screen x moved so the container's own slot test, which adds {@code leftPos} to a slot's
     * desktop-local x, lands on the right slot under a scaled desktop.
     */
    double slotX(final double absX) {
        return left() + localX(absX);
    }

    double slotY(final double absY) {
        return top() + localY(absY);
    }

    /**
     * Whether the panel sits at the top. Only the modern GNOME shell does that: the GNOME of the Legacy era put its
     * panel at the bottom, and its top bar did not exist for another decade.
     */
    boolean panelOnTop() {
        return desktop.panelStyle() == PanelStyle.GNOME && !desktop.periodPanel();
    }

    /**
     * How tall the band a panel stands in is. A taskbar is a taskbar's height on every desktop that has one; CDE's
     * Front Panel is a slab of pictures and stands taller, and windows keep out of its band the whole width of the
     * desktop although the slab itself is only as wide as what it holds.
     */
    int panelBand() {
        return desktop.panelStyle() == PanelStyle.CDE ? CdeFrontPanelLayout.BAND_H : DesktopScreen.TASKBAR_H;
    }

    /**
     * Whether a bar runs along the top: the modern GNOME's only panel, or GNOME 2's upper one, which carries its menus
     * and its notification area while its windows are listed along the foot.
     */
    boolean barOnTop() {
        return panelOnTop() || desktop.gnome2();
    }

    /** The first desktop-local row of the work area. */
    int workAreaTop() {
        return barOnTop() ? DesktopScreen.TASKBAR_H : 0;
    }

    /** One past the last desktop-local row of the work area: the bottom panel's top, or the screen's bottom. */
    int workAreaBottom() {
        return panelOnTop() ? height() : height() - panelBand();
    }

    /** The pixels a bottom panel reserves, none under a top one. */
    int panelReserve() {
        return panelOnTop() ? 0 : panelBand();
    }
}
