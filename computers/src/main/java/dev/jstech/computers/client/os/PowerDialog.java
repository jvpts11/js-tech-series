/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.CdeExitLayout;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.motion.GreyFilter;
import dev.jstech.core.motion.Motion;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionSpec;
import dev.jstech.core.motion.MotionStyles;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;

/**
 * The dialog that decides the fate of the whole machine: shut it down, restart it, or log off. Each of the three
 * reaches the machine; shutting down used to close the window and leave the machine running on the network with
 * everything still open. CDE asks its own way instead: how many programs are open, then Shut Down, Restart or Cancel.
 */
final class PowerDialog {

    private final DesktopState desktop;
    private boolean open;
    /** What happens to the desktop behind the dialog since it opened: it greys on Frames XP, at once elsewhere. */
    private Motion dimming = Motion.FINISHED;
    /* The desktop surface the dialog was centred on, so a click lands where it was drawn. */
    private int surfaceW;
    private int surfaceH;
    /* Whether a frame has drawn the dialog since it opened; until then a click has nowhere to land. */
    private boolean drawn;

    private static final int POWER_W = 190;
    private static final int ROW_H = 20;
    /** Each power choice with the line under it that says what it does, in the order the dialog lists them. */
    private static final TextKey[][] CHOICES = {
            {DesktopTexts.SHUT_DOWN, DesktopTexts.SHUT_DOWN_HINT},
            {DesktopTexts.RESTART, DesktopTexts.RESTART_HINT},
            {DesktopTexts.LOG_OFF, DesktopTexts.LOG_OFF_HINT},
    };

    PowerDialog(final DesktopState desktop) {
        this.desktop = desktop;
    }

    void open() {
        this.open = true;
        this.drawn = false;
        this.dimming = desktop.motion().start(MotionKinds.DIM);
    }

    void close() {
        this.open = false;
    }

    boolean isOpen() {
        return open;
    }

    /** Whether the dialog is up and a frame has drawn it, so its buttons stand where a click looks for them. */
    boolean shown() {
        return open && drawn;
    }

    /** How grey the desktop behind the dialog has gone, from 0 to 1, on a system that greys it; 0 on any other. */
    float greyed() {
        final MotionSpec system = desktop.motion().profile().spec(MotionKinds.DIM);
        if (!open || !MotionStyles.GREY.equals(system.style())) {
            return 0.0F;
        }
        return (float) (system.param("amount", 1.0) * dimming.progress(DesktopMotion.now()));
    }

    /** Draws the dialog over a desktop {@code surfaceW} by {@code surfaceH}, when it is up. */
    void render(final GuiGraphics g, final int surfaceW, final int surfaceH, final int mouseX, final int mouseY) {
        if (!open) {
            return;
        }
        this.surfaceW = surfaceW;
        this.surfaceH = surfaceH;
        this.drawn = true;
        final Font font = desktop.textFont();
        if (desktop.panelStyle() == PanelStyle.CDE) {
            CdeExitDialog.render(g, font, surfaceW, surfaceH, desktop.wm().openPrograms(),
                    desktop.prefs().cdePalette());
            return;
        }
        final OsSkin skin = desktop.prefs().skin();
        final int x = x();
        final int y = y();
        shade(g, surfaceW, surfaceH);
        skin.windowShadow(g, x, y, POWER_W, height());
        skin.windowFrame(g, x, y, POWER_W, height());
        skin.titleBar(g, x, y, POWER_W, 14);
        Draw.text(g, font, GameText.resolve(DesktopTexts.POWER), x + 6, y + 3, skin.titleText());
        for (int i = 0; i < CHOICES.length; i++) {
            final int rowY = y + 18 + i * ROW_H;
            final boolean hovered = mouseX >= x + 4 && mouseX < x + POWER_W - 4
                    && mouseY >= rowY && mouseY < rowY + ROW_H - 2;
            if (hovered) {
                g.fill(x + 4, rowY, x + POWER_W - 4, rowY + ROW_H - 2, skin.listHover());
            }
            Draw.text(g, font, GameText.resolve(CHOICES[i][0]), x + 12, rowY + 2, skin.text());
            Draw.text(g, font, GameText.resolve(CHOICES[i][1]), x + 12, rowY + 11, skin.dim());
        }
    }

    /**
     * Answers a click at a desktop-local point while the dialog is up; returns whether it took the click, which it
     * always does while open. A question with a Cancel of its own stays up until one of its buttons answers it; the
     * list closes on a click anywhere else, so nothing is shut down by accident.
     */
    boolean click(final double mouseX, final double mouseY) {
        if (!open) {
            return false;
        }
        if (desktop.panelStyle() == PanelStyle.CDE) {
            final int pressed = CdeExitLayout.buttonAt(mouseX, mouseY, surfaceW, surfaceH);
            if (pressed == CdeExitLayout.SHUT_DOWN || pressed == CdeExitLayout.RESTART) {
                choose(pressed == CdeExitLayout.SHUT_DOWN ? MachinePowerPayload.ACTION_SHUTDOWN
                        : MachinePowerPayload.ACTION_RESTART);
            } else if (pressed == CdeExitLayout.CANCEL) {
                open = false;
            }
            return true;
        }
        final int x = x();
        final int y = y();
        for (int i = 0; i < CHOICES.length; i++) {
            final int rowY = y + 18 + i * ROW_H;
            if (mouseX >= x + 4 && mouseX < x + POWER_W - 4 && mouseY >= rowY && mouseY < rowY + ROW_H - 2) {
                choose(i);
                return true;
            }
        }
        open = false;
        return true;
    }

    /**
     * Answers a key while the dialog is up, which keeps the keyboard as it keeps the mouse: Escape thinks again, and
     * on CDE Enter takes the button that wears the ring, Shut Down. Returns whether it took the key.
     */
    boolean keyPressed(final int key) {
        if (!open) {
            return false;
        }
        if (key == 256) {
            open = false;
        } else if (desktop.panelStyle() == PanelStyle.CDE && (key == 257 || key == 335)) {
            choose(MachinePowerPayload.ACTION_SHUTDOWN);
        }
        return true;
    }

    /** Tells the machine what was chosen, and puts the dialog away. */
    private void choose(final int action) {
        desktop.cyclePower(action);
        open = false;
    }

    private int height() {
        return 22 + CHOICES.length * ROW_H;
    }

    private int x() {
        return (surfaceW - POWER_W) / 2;
    }

    private int y() {
        return (surfaceH - height()) / 2;
    }

    /*
     * What lies behind the dialog. Frames XP drains the desktop to grey over a second and a half; where the graphics
     * card cannot grey it, a veil deepens over the same time instead. Every other system lays its veil at once.
     */
    private void shade(final GuiGraphics g, final int surfaceW, final int surfaceH) {
        final int veil = DesktopShellPalette.get().powerShade();
        final MotionSpec system = desktop.motion().profile().spec(MotionKinds.DIM);
        if (!MotionStyles.GREY.equals(system.style())) {
            g.fill(0, 0, surfaceW, surfaceH, veil);
            return;
        }
        if (GreyFilter.ready()) {
            // The filter works in the screen's units, so the glass is carried there through the pose.
            final Matrix4f at = g.pose().last().pose();
            GreyFilter.filterScreen(g, at.m30(), at.m31(), surfaceW * at.m00(), surfaceH * at.m11(), greyed());
            return;
        }
        final int alpha = (int) ((veil >>> 24) * dimming.progress(DesktopMotion.now()));
        g.fill(0, 0, surfaceW, surfaceH, alpha << 24 | veil & 0xFFFFFF);
    }
}
