/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * What a tab of the machine screen, or a machine's own face, draws with and answers to: the machine and its menu, the
 * screen's place, the mouse, and the screen's clock. Points on the steel face are given from its top left and turned
 * into the screen's by {@link #x} and {@link #y}.
 *
 * @param g       what it draws on; none while it answers a click
 * @param font    the game's font
 * @param menu    the machine's menu
 * @param left    the screen's left
 * @param top     the screen's top
 * @param mouseX  the mouse across, in the screen's pixels
 * @param mouseY  the mouse down
 * @param tick    the screen's clock, in ticks and parts of one, for what moves
 * @param press   presses one of the menu's buttons, which the server answers
 */
public record MachineView(GuiGraphics g, Font font, MachineMenu menu, int left, int top, int mouseX, int mouseY,
                          float tick, IntConsumer press) {

    /** The machine. */
    public MachineBlockEntity machine() {
        return menu.machine();
    }

    /** The screen's x of the face's {@code x}. */
    public int x(final int x) {
        return left + MachineScreenLayout.faceX(x);
    }

    /** The screen's y of the face's {@code y}. */
    public int y(final int y) {
        return top + MachineScreenLayout.faceY(y);
    }

    /** The mouse across, from the face's left. */
    public double faceMouseX() {
        return mouseX - left - MachineScreenLayout.FACE_X;
    }

    /** The mouse down, from the face's top. */
    public double faceMouseY() {
        return mouseY - top - MachineScreenLayout.FACE_Y;
    }

    /** Whether the machine is working now. */
    public boolean working() {
        return machine().running();
    }
}
