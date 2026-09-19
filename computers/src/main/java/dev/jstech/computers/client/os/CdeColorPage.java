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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * The Color page of CDE's Style Manager: the eight palettes under the names they had, and the colours of the
 * one picked beside the list.
 *
 * <p>The whole desktop wears a palette the moment it is picked, which is how a player sees what it does to the
 * frames, the panel and the backdrop before keeping it. OK keeps it with the machine; Cancel, or closing the page
 * any other way, puts back the palette the desktop had when the page was opened.
 */
final class CdeColorPage implements IDesktopApp {

    /** What the Style Manager is told when the page goes, so it opens a fresh one the next time. */
    private final Runnable gone;

    /** The palette the desktop wore when the page was opened, which Cancel puts back. */
    private final String kept;
    private String picked;

    /** Whether OK or Cancel has answered, so closing the window afterwards changes nothing further. */
    private boolean answered;

    private OsSkin skin;
    private int left;
    private int top;

    private static final String TITLE = "Style Manager - Color";

    /** The palettes by name, in the order the Style Manager lists them. */
    private static final List<String> NAMES = names();

    CdeColorPage(final String kept, final Runnable gone) {
        this.kept = kept;
        this.picked = kept;
        this.gone = gone;
    }

    /** The middle of the palette so named on the list, in desktop pixels, or null when the list has none. */
    @Nullable
    int[] rowCentre(final String name) {
        final int index = NAMES.indexOf(name);
        return index < 0 ? null : CdeStylePages.centre(CdeStyleLayout.row(true, index), this.left, this.top);
    }

    /** The middle of OK (0) or Cancel (1), in desktop pixels. */
    int[] buttonCentre(final int button) {
        return CdeStylePages.centre(CdeStyleLayout.button(true, button), this.left, this.top);
    }

    @Override
    public String title() {
        return TITLE;
    }

    @Override
    public int defaultWidth() {
        return CdeStyleLayout.COLOR_W + CdeStyleLayout.FRAME_W;
    }

    @Override
    public int defaultHeight() {
        return CdeStyleLayout.COLOR_H + CdeStyleLayout.FRAME_H;
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
        if (this.skin == null) {
            return;
        }
        final CdePalette shown = CdePalette.named(this.picked);
        CdeStylePages.list(g, font, this.skin, shown, x, y, true, NAMES, NAMES.indexOf(this.picked));
        final Rect name = CdeStyleLayout.colorName();
        g.drawString(font, shown.name(), x + name.x(), y + name.y(), this.skin.text(), false);
        final int[] colours = {shown.active(), shown.window(), shown.inset(), shown.light(), shown.shade(),
            shown.backdropA(), shown.backdropB(), shown.ink()};
        for (int i = 0; i < CdeStyleLayout.SWATCHES; i++) {
            final Rect s = CdeStyleLayout.swatch(i);
            MotifChrome.raised(g, x + s.x(), y + s.y(), s.w(), s.h(), colours[i], shown);
        }
        CdeStylePages.buttons(g, font, this.skin, x, y, true, "OK", "Cancel", mouseX, mouseY);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button != 0) {
            return;
        }
        final int row = CdeStyleLayout.rowAt(true, mouseX - this.left, mouseY - this.top, NAMES.size());
        if (row >= 0) {
            pick(NAMES.get(row));
            return;
        }
        final int pressed = CdeStyleLayout.buttonAt(true, mouseX - this.left, mouseY - this.top);
        if (pressed == 0) {
            keep();
        } else if (pressed == 1) {
            putBack();
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final int at = NAMES.indexOf(this.picked);
        switch (key) {
            case GLFW.GLFW_KEY_UP -> pick(NAMES.get(Math.max(0, at - 1)));
            case GLFW.GLFW_KEY_DOWN -> pick(NAMES.get(Math.min(NAMES.size() - 1, at + 1)));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> keep();
            case GLFW.GLFW_KEY_ESCAPE -> putBack();
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Escape is this page's Cancel, so the desktop must not take it as the way out of the monitor. */
    @Override
    public boolean wantsEscape() {
        return true;
    }

    @Override
    public void onClosed() {
        if (!this.answered) {
            wear(this.kept);
        }
        this.gone.run();
    }

    /** Picks a palette, and the whole desktop wears it at once. */
    private void pick(final String name) {
        this.picked = name;
        wear(name);
    }

    private void keep() {
        this.answered = true;
        final DesktopScreen desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.keepCdeStyle(desktop.cdeStyle().withPalette(this.picked));
        }
        DesktopScreen.closeDialog(this);
    }

    private void putBack() {
        this.answered = true;
        wear(this.kept);
        DesktopScreen.closeDialog(this);
    }

    /* Only the palette changes: the backdrops may have been set on the other page in the meantime. */
    private static void wear(final String palette) {
        final DesktopScreen desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.wearCdeStyle(desktop.cdeStyle().withPalette(palette));
        }
    }

    private static List<String> names() {
        final List<String> out = new ArrayList<>(CdePalette.ALL.size());
        for (final CdePalette each : CdePalette.ALL) {
            out.add(each.name());
        }
        return List.copyOf(out);
    }
}
