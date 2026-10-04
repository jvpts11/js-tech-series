/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where everything sits in KDE's Info Center: the row of its pages along the top, and under it the page that is up,
 * either "About this System" (laid out by {@link ThisPcLayout.KdeAbout}) or "Devices by port": a heading per graphics
 * card, the audio and the ports, each port beside what is plugged into it, and the button that disables or enables
 * the selected device along the foot.
 *
 * <p>The pages are a row across the top rather than a list down the side: beside the About page a list would make
 * the window wider than a desktop drawn at its full size holds.
 */
public final class InfoCenterLayout {

    /** The row of pages: how tall, how far its words stand in from each side of a page's tab. */
    public static final int TABS_H = 14;
    public static final int TAB_PAD = 6;
    public static final int TAB_X = 4;

    /** The whole window: the About page's width, and its height under the row of pages. */
    public static final int W = ThisPcLayout.KdeAbout.W;
    public static final int H = TABS_H + ThisPcLayout.KdeAbout.H;

    /** The devices page under the row of pages: its margins, its rows and its two columns. */
    public static final int PAGE_PAD = 6;
    public static final int ROW_H = 11;
    public static final int ICON = 8;
    public static final int PORT_X = PAGE_PAD + 10;
    public static final int DEVICE_X = 132;
    public static final int TEXT_H = 7;

    /** The button along the foot of the devices page. */
    public static final int BUTTON_W = 150;
    public static final int BUTTON_H = 14;
    public static final int BUTTON_Y = H - PAGE_PAD - BUTTON_H;

    /** The room the rows have between the row of pages and the button. */
    public static final int LIST_Y = TABS_H + 4;
    public static final int LIST_H = BUTTON_Y - 4 - LIST_Y;

    private InfoCenterLayout() {
    }

    /** How many rows the devices page shows at once. */
    public static int rowsShown() {
        return Math.max(1, LIST_H / ROW_H);
    }

    /** The button's left edge, at the page's right end. */
    public static int buttonX() {
        return W - PAGE_PAD - BUTTON_W;
    }

    /** The window as solids, with the longest words of the row of pages and of a devices row in either language. */
    public static GuiLayout layout() {
        final GuiLayout l = new GuiLayout(W, H);
        l.box("tabs", 0, 0, W, TABS_H - 1);
        l.box("list", PAGE_PAD, LIST_Y, W - 2 * PAGE_PAD, LIST_H);
        l.box("button", buttonX(), BUTTON_Y, BUTTON_W, BUTTON_H);
        // "Sobre este sistema" and "Dispositivos por porta", the two pages' names, side by side.
        l.text("tab-words", TAB_X + TAB_PAD, 4, 18 + 22 + 6, 0.75f);
        // "Desativar o dispositivo escolhido", the longest the button says.
        l.text("button-words", buttonX() + 4, BUTTON_Y + 4, 33, 0.75f);
        // As much of a port's name as its column holds before the device's, and the longest device beside it.
        l.text("port", PORT_X + ICON + 2, LIST_Y + 2, 23, 0.75f);
        l.text("device", DEVICE_X + ICON + 2, LIST_Y + 2, 40, 0.75f);
        return l;
    }
}
