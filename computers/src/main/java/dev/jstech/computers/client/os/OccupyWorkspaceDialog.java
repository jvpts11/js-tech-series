/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.core.text.GameText;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * CDE's Occupy Workspace: the workspaces a window is on, each with a box to tick, which is how a window moves
 * from one workspace to another or comes to be on several at once.
 *
 * <p>Nothing changes until OK is pressed, and the last box that is ticked cannot be cleared, because a window on
 * no workspace could never be found again.
 */
final class OccupyWorkspaceDialog implements IDesktopApp {

    private final String program;
    private final IntConsumer onChosen;
    private OsSkin skin;
    private int chosen;

    /* Where the content was last drawn, which is what a click is measured against. */
    private int left;
    private int top;
    private int width;
    private int height;

    /* The window as a whole: the content below needs 106 pixels, and the title bar and the frame take 22. */
    private static final int W = 140;
    private static final int H = 128;
    private static final int PAD = 8;
    private static final int ROWS_Y = 20;
    private static final int ROW_H = 13;
    private static final int BOX = 9;
    private static final int BUTTON_W = 48;
    private static final int BUTTON_H = 14;

    /**
     * @param program  the name of the window being asked about
     * @param occupied the workspaces it is on now, as a {@link WorkspaceSet}
     * @param onChosen told the new set when OK is pressed
     */
    OccupyWorkspaceDialog(final String program, final int occupied, final IntConsumer onChosen) {
        this.program = program;
        this.chosen = WorkspaceSet.normalised(occupied);
        this.onChosen = onChosen;
    }

    /** The workspaces ticked at the moment, for a test to read. */
    int chosen() {
        return this.chosen;
    }

    /** The middle of the box of workspace {@code index}, in desktop pixels, where a test clicks it. */
    int[] boxCentre(final int index) {
        return new int[] {this.left + PAD + BOX / 2, rowY(index) + BOX / 2};
    }

    /** The middle of the OK button, in desktop pixels. */
    int[] okCentre() {
        return new int[] {okX() + BUTTON_W / 2, buttonsY() + BUTTON_H / 2};
    }

    @Override
    public String title() {
        return "Occupy Workspace";
    }

    @Override
    public int defaultWidth() {
        return W;
    }

    @Override
    public int defaultHeight() {
        return H;
    }

    @Override
    public int minWidth() {
        return W;
    }

    @Override
    public int minHeight() {
        return H;
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
        this.width = w;
        this.height = h;
        if (this.skin == null) {
            return;
        }
        // The program's name gives way to the words around it, which are as wide as the language makes them.
        final int around = font.width(GameText.resolve(OccupyWorkspaceTexts.IS_ON.with("")));
        final String name = font.plainSubstrByWidth(this.program, w - PAD * 2 - around);
        g.drawString(font, GameText.resolve(OccupyWorkspaceTexts.IS_ON.with(name)), x + PAD, y + 6,
                this.skin.text(), false);
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            final int rowY = rowY(i);
            this.skin.panel(g, x + PAD, rowY, BOX, BOX);
            if (WorkspaceSet.holds(this.chosen, i)) {
                g.fill(x + PAD + 2, rowY + 2, x + PAD + BOX - 2, rowY + BOX - 2, this.skin.accent());
            }
            g.drawString(font, CdePanels.workspaceName(i), x + PAD + BOX + 6, rowY + 1, this.skin.text(), false);
        }
        final int by = buttonsY();
        this.skin.button(g, font, okX(), by, BUTTON_W, BUTTON_H, GameText.resolve(OccupyWorkspaceTexts.OK),
                over(mouseX, mouseY, okX(), by), false, true);
        this.skin.button(g, font, cancelX(), by, BUTTON_W, BUTTON_H, GameText.resolve(OccupyWorkspaceTexts.CANCEL),
                over(mouseX, mouseY, cancelX(), by), false, false);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button != 0) {
            return;
        }
        if (over(mouseX, mouseY, okX(), buttonsY())) {
            accept();
            return;
        }
        if (over(mouseX, mouseY, cancelX(), buttonsY())) {
            DesktopScreen.closeDialog(this);
            return;
        }
        // The whole row answers, the name as well as the box, as a tick box on any desktop does.
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            if (mouseX >= this.left + PAD && mouseX < this.left + this.width - PAD
                    && mouseY >= rowY(i) - 2 && mouseY < rowY(i) + ROW_H - 2) {
                this.chosen = WorkspaceSet.toggled(this.chosen, i);
                return;
            }
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            accept();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            DesktopScreen.closeDialog(this);
            return true;
        }
        return false;
    }

    /** Escape is this dialog's Cancel, so the desktop must not take it as the way out of the monitor. */
    @Override
    public boolean wantsEscape() {
        return true;
    }

    private void accept() {
        DesktopScreen.closeDialog(this);
        this.onChosen.accept(this.chosen);
    }

    private int rowY(final int index) {
        return this.top + ROWS_Y + index * ROW_H;
    }

    private int buttonsY() {
        return this.top + this.height - PAD - BUTTON_H;
    }

    private int okX() {
        return this.left + this.width / 2 - BUTTON_W - 4;
    }

    private int cancelX() {
        return this.left + this.width / 2 + 4;
    }

    private static boolean over(final double mx, final double my, final int x, final int y) {
        return mx >= x && mx < x + BUTTON_W && my >= y && my < y + BUTTON_H;
    }
}
