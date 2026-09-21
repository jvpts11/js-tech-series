/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.client.os.OsSkin;
import dev.jstech.computers.client.os.ShellView;
import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.core.client.gui.component.UiContext;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Console heading: the machine's own prompt, inside the space rather than in a window over it.
 *
 * <p>It is a view of the machine's console and not a console of its own, which is what lets an editor
 * opened here have the glass the way one does at a bare prompt: everything the prompt can do on a machine
 * with no interface at all, it can do here.
 *
 * <p>Clicking this heading used to throw a separate Command Prompt window over the screen. A screen that
 * is the whole machine has no business opening a window onto itself, and the window was never approved.
 */
final class ConsoleTerminalTab extends AbstractTerminalTab {

    /** Where the console's glass sits: everything under the status bar and right of the rail. */
    private static final int LEFT = ComputerTerminalLayout.RAIL_X + ComputerTerminalLayout.RAIL_W + 2;
    private static final int TOP = ComputerTerminalLayout.BAR_H + 2;
    private static final int RIGHT_PAD = 2;
    private static final int BOTTOM = ComputerTerminalLayout.INV_LINE_Y - 2;

    /** The line this system uses to say where to start, which is its own word and not the DOS family's. */
    private static final String START_HINT = "showcommands lists what this machine can run.";

    @Nullable
    private ShellView view;

    /** Where the pointer was last seen over the glass, which is where a turn of the wheel happens. */
    private double pointerX;
    private double pointerY;

    ConsoleTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        final ShellView shell = shell();
        shell.setGround(SCREEN_COL());
        shell.setBounds(x + LEFT, y + TOP,
                ComputerTerminalLayout.WIDTH - LEFT - RIGHT_PAD, BOTTOM - TOP);
        this.pointerX = mouseX;
        this.pointerY = mouseY;
        shell.render(g, new UiContext(OsSkin.fallback(), font(), mouseX, mouseY, partialTick));
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        // The console draws its own glass in renderTabBg; nothing belongs over the top of it.
    }

    @Override
    public boolean onMouseClicked(final double mouseX, final double mouseY, final int button) {
        return over(mouseX, mouseY) && shell().mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseDragged(final double mouseX, final double mouseY, final int button) {
        return shell().mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseReleased(final double mouseX, final double mouseY, final int button) {
        return shell().mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScrolled(final double mouseX, final double mouseY, final double dy) {
        return over(mouseX, mouseY) && shell().mouseScrolled(this.pointerX, this.pointerY, dy);
    }

    /**
     * Every key belongs to the prompt while the prompt is what is showing, Escape excepted.
     *
     * <p>A terminal that let a key it had no use for fall through to the screen would be a terminal
     * nobody could type an 'e' at: the inventory key would shut the machine's whole interface mid-line.
     * Escape is the one key the screen keeps, so it still closes; an editor holding the glass takes even
     * that, which is what {@link #wantsEscape()} says.
     */
    @Override
    public boolean onKeyPressed(final int key, final int scanCode, final int modifiers) {
        return shell().keyPressed(key, scanCode, modifiers) || key != GLFW.GLFW_KEY_ESCAPE;
    }

    @Override
    public boolean onKeyReleased(final int key, final int scanCode, final int modifiers) {
        return shell().keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(final char c, final int modifiers) {
        return shell().charTyped(c);
    }

    @Override
    public boolean wantsEscape() {
        return shell().editing();
    }

    /** Everything the prompt has printed here, one line after another, for a test to read. */
    String glassText() {
        return this.view == null ? "" : this.view.scrollbackText();
    }

    @Override
    public void onRemoved() {
        if (this.view != null) {
            this.view.release();
            this.view = null;
        }
    }

    /**
     * The view of this machine's console, made the first time the heading is looked at.
     *
     * <p>Made late so a machine nobody opens the prompt on never talks to its console at all, and kept
     * afterwards so walking away from the heading and coming back finds what was said there.
     */
    private ShellView shell() {
        if (this.view == null) {
            this.view = new ShellView(menu.hostPos(), false, screen.systemName(), START_HINT);
        }
        return this.view;
    }

    private boolean over(final double mouseX, final double mouseY) {
        final int x = screen.left() + LEFT;
        final int y = screen.top() + TOP;
        return mouseX >= x && mouseX < x + ComputerTerminalLayout.WIDTH - LEFT - RIGHT_PAD
                && mouseY >= y && mouseY < y + BOTTOM - TOP;
    }
}
