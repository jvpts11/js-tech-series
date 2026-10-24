/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import dev.jstech.core.machine.MachineMenu;
import dev.jstech.industrial.crc.CrcBlockEntity;
import dev.jstech.industrial.crc.CrcReadings;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What a tab of the Computational Research Centre's own draws with and answers to: the CRC and its menu, what the
 * server last said of it, the screen's place, the mouse and the screen's clock. Its points are the screen's, from its
 * top left, as {@code CrcLayout} gives them, and {@link #x} and {@link #y} put them where the screen is.
 *
 * @param g      what it draws on; none while it answers a click
 * @param font   the game's font
 * @param menu   the CRC's menu
 * @param title  the CRC's name, as its screen's title
 * @param read   what the server last said of it
 * @param left   the screen's left
 * @param top    the screen's top
 * @param mouseX the mouse across, in the screen's pixels
 * @param mouseY the mouse down
 * @param tick   the screen's clock, in ticks and parts of one, for what moves
 * @param press  presses one of the menu's buttons, which the server answers
 */
record CrcView(@Nullable GuiGraphics g, Font font, MachineMenu menu, Component title, CrcReadings read, int left,
               int top, int mouseX, int mouseY, float tick, IntConsumer press) {

    private static final double CELSIUS_ZERO = 273.15;
    private static final double HOT_FROM = 90.0;

    /** The CRC. */
    CrcBlockEntity crc() {
        return (CrcBlockEntity) menu.machine();
    }

    /** Where the screen's {@code x} is. */
    int x(final int x) {
        return left + x;
    }

    /** Where the screen's {@code y} is. */
    int y(final int y) {
        return top + y;
    }

    /** Its temperature, in degrees. */
    double celsius() {
        return crc().temperature() - CELSIUS_ZERO;
    }

    /** Whether it is hot: past 90 degrees, where its thermometer turns red and a part is close to burning. */
    boolean hot() {
        return celsius() >= HOT_FROM;
    }
}
