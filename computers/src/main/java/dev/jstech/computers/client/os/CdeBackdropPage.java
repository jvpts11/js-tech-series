/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdeBackdrop;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.CdeStyleLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * The Backdrop page of CDE's Style Manager: the patterns a workspace can wear, and a preview of the one picked
 * in the palette's own backdrop colours.
 *
 * <p>A backdrop is set for the workspace that is up, which is how the four come to look different. Picking one
 * only changes the preview; Apply puts it on the workspace and has the machine keep it, and the page stays open
 * so the next workspace can be given one of its own.
 */
final class CdeBackdropPage implements IDesktopApp {

    /** What the Style Manager is told when the page goes, so it opens a fresh one the next time. */
    private final Runnable gone;
    private CdeBackdrop picked;

    private OsSkin skin;
    private int left;
    private int top;

    private static final String TITLE = "Style Manager - Backdrop";

    /** The patterns by name, in the order the Style Manager lists them. */
    private static final List<String> LABELS = labels();

    /** The pattern the preview inside the frame is inset by, so the frame's relief stays in sight. */
    private static final int INSET = 2;

    CdeBackdropPage(final CdeBackdrop wearing, final Runnable gone) {
        this.picked = wearing;
        this.gone = gone;
    }

    /** The middle of the pattern so named on the list, in desktop pixels, or null when the list has none. */
    @Nullable
    int[] rowCentre(final String label) {
        final int index = LABELS.indexOf(label);
        return index < 0 ? null : CdeStylePages.centre(CdeStyleLayout.row(false, index), this.left, this.top);
    }

    /** The middle of Apply (0) or Close (1), in desktop pixels. */
    int[] buttonCentre(final int button) {
        return CdeStylePages.centre(CdeStyleLayout.button(false, button), this.left, this.top);
    }

    @Override
    public String title() {
        return TITLE;
    }

    @Override
    public int defaultWidth() {
        return CdeStyleLayout.BACKDROP_W + CdeStyleLayout.FRAME_W;
    }

    @Override
    public int defaultHeight() {
        return CdeStyleLayout.BACKDROP_H + CdeStyleLayout.FRAME_H;
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
        CdeStylePages.list(g, font, this.skin, p, x, y, false, LABELS, this.picked.place());
        final Rect preview = CdeStyleLayout.preview();
        this.skin.panel(g, x + preview.x(), y + preview.y(), preview.w(), preview.h());
        g.pose().pushPose();
        g.pose().translate(x + preview.x() + INSET, y + preview.y() + INSET, 0);
        MotifChrome.backdrop(g, preview.w() - INSET * 2, preview.h() - INSET * 2, p, this.picked);
        g.pose().popPose();
        final Rect label = CdeStyleLayout.forWorkspace();
        g.drawString(font, GameText.resolve(CdeTexts.FOR_WORKSPACE.with(CdePanels.workspaceName(desktop.workspace()))),
                x + label.x(), y + label.y(), this.skin.text(), false);
        CdeStylePages.buttons(g, font, this.skin, x, y, false, CdeTexts.APPLY, CdeTexts.CLOSE, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button != 0) {
            return;
        }
        final int row = CdeStyleLayout.rowAt(false, mouseX - this.left, mouseY - this.top, LABELS.size());
        if (row >= 0) {
            this.picked = byPlace(row);
            return;
        }
        final int pressed = CdeStyleLayout.buttonAt(false, mouseX - this.left, mouseY - this.top);
        if (pressed == 0) {
            apply();
        } else if (pressed == 1) {
            DesktopScreen.closeDialog(this);
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_UP -> this.picked = byPlace(Math.max(0, this.picked.place() - 1));
            case GLFW.GLFW_KEY_DOWN -> this.picked = byPlace(Math.min(LABELS.size() - 1, this.picked.place() + 1));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> apply();
            case GLFW.GLFW_KEY_ESCAPE -> DesktopScreen.closeDialog(this);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Escape is this page's Close, so the desktop must not take it as the way out of the monitor. */
    @Override
    public boolean wantsEscape() {
        return true;
    }

    @Override
    public void onClosed() {
        this.gone.run();
    }

    private void apply() {
        final DesktopScreen desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.keepCdeStyle(desktop.cdeStyle().withBackdrop(desktop.workspace(), this.picked));
        }
    }

    private static CdeBackdrop byPlace(final int place) {
        for (final CdeBackdrop each : CdeBackdrop.values()) {
            if (each.place() == place) {
                return each;
            }
        }
        return CdeBackdrop.HATCH;
    }

    private static List<String> labels() {
        final List<String> out = new ArrayList<>(CdeBackdrop.values().length);
        for (int place = 0; place < CdeBackdrop.values().length; place++) {
            out.add(byPlace(place).label());
        }
        return List.copyOf(out);
    }
}
