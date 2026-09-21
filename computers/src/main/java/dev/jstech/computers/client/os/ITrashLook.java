/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * How a trash window looks and answers the pointer on one family of desktops. The window keeps what is in the trash
 * and what is selected; a look draws that and turns clicks into the window's actions.
 */
interface ITrashLook {

    /** Draws the content rectangle, in desktop pixels. */
    void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY);

    /** A press inside the content rectangle. */
    void click(double mouseX, double mouseY, int button);

    /** How many rows the view moves for a turn of the wheel, and how far down it can go. */
    int scrollLimit();

    /** Whether a menu of the look is up, which takes the next click wherever it lands. */
    boolean menuOpen();

    /** What the menu that is up offers, top to bottom, for a test to read. */
    List<String> menuLabels();

    /** Where a test clicks the menu entry so labelled, or null when the menu that is up has none. */
    @Nullable
    int[] menuPoint(String label);

    /** The middle of item {@code index} as it is shown now, or null when it is scrolled out of view. */
    @Nullable
    int[] itemCentre(int index);

    /**
     * The middle of the control that says {@code label}: a task, a button, a place or a menu title, where a test
     * clicks it; null when this look has none of that name.
     */
    @Nullable
    int[] controlPoint(String label);
}
