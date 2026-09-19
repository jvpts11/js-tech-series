/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.CdeStyleLayout;
import dev.jstech.core.client.gui.component.Texts;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * CDE's Style Manager: a strip of pages, and a click on one opens it. There are two, the two that mean something
 * here: Color, which the whole desktop is drawn from, and Backdrop, which each workspace wears.
 *
 * <p>This is what a CDE desktop has where the others have their settings. Everything else a machine keeps about
 * itself is still set at its prompt with {@code config}.
 */
final class StyleManagerApp implements IDesktopApp {

    /** The page of each kind that is open, so a second click brings it forward rather than opening another. */
    @Nullable
    private CdeColorPage color;
    @Nullable
    private CdeBackdropPage backdrop;

    private OsSkin skin;
    private int left;
    private int top;

    private static final String TITLE = "Style Manager";
    private static final List<String> PAGES = List.of("Color", "Backdrop");
    private static final int COLOR = 0;

    /** The four colours the Color page's picture is made of, the way the real one showed a palette. */
    private static final int[] PAINTS = {0xFFD8332C, 0xFF3B6FD8, 0xFF3FA34D, 0xFFE2C36B};

    /** The middle of the page so named on the strip, in desktop pixels, or null when the strip has none. */
    @Nullable
    int[] pageCentre(final String page) {
        final int index = PAGES.indexOf(page);
        return index < 0 ? null : CdeStylePages.centre(CdeStyleLayout.page(index), this.left, this.top);
    }

    /** The Color page, while it is open. */
    @Nullable
    CdeColorPage colorPage() {
        return this.color;
    }

    /** The Backdrop page, while it is open. */
    @Nullable
    CdeBackdropPage backdropPage() {
        return this.backdrop;
    }

    @Override
    public String title() {
        return TITLE;
    }

    @Override
    public int defaultWidth() {
        return CdeStyleLayout.STRIP_W + CdeStyleLayout.FRAME_W;
    }

    @Override
    public int defaultHeight() {
        return CdeStyleLayout.STRIP_H + CdeStyleLayout.FRAME_H;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return defaultHeight();
    }

    @Override
    public void applySkin(final OsSkin skin) {
        this.skin = skin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        final DesktopScreen desktop = DesktopScreen.current();
        if (this.skin == null || desktop == null) {
            return;
        }
        final CdePalette p = desktop.cdePalette();
        this.skin.panel(g, x, y, CdeStyleLayout.STRIP_W, CdeStyleLayout.STRIP_H);
        for (int i = 0; i < PAGES.size(); i++) {
            final Rect page = CdeStyleLayout.page(i);
            final int px = x + page.x();
            final int py = y + page.y();
            // The page that is open stands pushed in, the way a pressed control does.
            if (open(i)) {
                MotifChrome.sunken(g, px, py, page.w(), page.h(), p.inset(), p);
            }
            final int ix = px + (page.w() - CdeStyleLayout.PAGE_ICON) / 2;
            final int iy = py + 4;
            if (i == COLOR) {
                paints(g, ix, iy);
            } else {
                MotifChrome.sunken(g, ix, iy, CdeStyleLayout.PAGE_ICON, CdeStyleLayout.PAGE_ICON, p.backdropA(), p);
                g.pose().pushPose();
                g.pose().translate(ix + 1, iy + 1, 0);
                MotifChrome.backdrop(g, CdeStyleLayout.PAGE_ICON - 2, CdeStyleLayout.PAGE_ICON - 2, p,
                        desktop.cdeStyle().backdrop(desktop.workspace()));
                g.pose().popPose();
            }
            final String name = PAGES.get(i);
            Texts.small(g, font, name, px + (page.w() - Texts.smallWidth(font, name)) / 2,
                    iy + CdeStyleLayout.PAGE_ICON + 4, this.skin.text());
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button == 0) {
            final int page = CdeStyleLayout.pageAt(mouseX - this.left, mouseY - this.top, PAGES.size());
            if (page >= 0) {
                openPage(page);
            }
        }
    }

    /** Opens a page over the strip, or brings it forward when it is already open. */
    private void openPage(final int page) {
        final DesktopScreen desktop = DesktopScreen.current();
        if (desktop == null) {
            return;
        }
        final IDesktopApp opening;
        if (page == COLOR) {
            if (this.color == null) {
                this.color = new CdeColorPage(desktop.cdeStyle().palette(), () -> this.color = null);
            }
            opening = this.color;
        } else {
            if (this.backdrop == null) {
                this.backdrop = new CdeBackdropPage(desktop.cdeStyle().backdrop(desktop.workspace()),
                        () -> this.backdrop = null);
            }
            opening = this.backdrop;
        }
        opening.applySkin(this.skin);
        DesktopScreen.openDialogFor(this, opening);
    }

    private boolean open(final int page) {
        return page == COLOR ? this.color != null : this.backdrop != null;
    }

    /** The Color page's picture: four paints in a square, one to each corner. */
    private static void paints(final GuiGraphics g, final int x, final int y) {
        final int half = CdeStyleLayout.PAGE_ICON / 2;
        for (int i = 0; i < PAINTS.length; i++) {
            final int px = x + (i % 2) * half;
            final int py = y + (i / 2) * half;
            g.fill(px, py, px + half, py + half, PAINTS[i]);
        }
    }
}
