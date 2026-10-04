/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.gui.layout.PowerStripLayout;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The power strip of an opened monitor screen: a narrow strip outside the frame on its left with a Power button,
 * which shuts the computer down, over a Restart button, each saying what it does under the pointer. Both go to the
 * machine as its own power menu would.
 *
 * <p>Also answers what tube the monitor a screen is shown on has, which decides what its glass shows of the colours.
 */
@TextHolder
@PaletteHolder
public final class PowerStrip {

    static final TextKey POWER = TextKey.of("jsc.monitor.strip.power", "Shut down the PC");
    static final TextKey RESTART = TextKey.of("jsc.monitor.strip.restart", "Restart the PC");

    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "monitor/power_strip",
            new Colours(0xFF1F2226, 0xFF0C0D0F, 0xFF626262, 0xFF7A7A7A, 0xFFA0A0A0, 0xFF282828, 0xFFE65046,
                    0xFFF0C850));
    /* The two signs, nine by nine, a pixel a cell: the power sign, and the arrow going round. */
    private static final String[] POWER_SIGN = {
        "....#....",
        "..#.#.#..",
        ".#..#..#.",
        "#...#...#",
        "#.......#",
        "#.......#",
        ".#.....#.",
        "..#...#..",
        "...###..."
    };
    private static final String[] RESTART_SIGN = {
        "...###.#.",
        ".##...##.",
        ".#...###.",
        "#........",
        "#.......#",
        "#.......#",
        ".#.....#.",
        "..#...#..",
        "...###..."
    };

    private PowerStrip() {
    }

    /** The tube of the monitor at {@code monitor} as this client sees it; a colour tube when there is none there. */
    public static Tube tubeAt(@Nullable final BlockPos monitor) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (monitor == null || minecraft.level == null
                || !(minecraft.level.getBlockState(monitor).getBlock() instanceof MonitorBlock block)) {
            return Tube.COLOUR;
        }
        return block.kind().tube();
    }

    /**
     * Draws the strip beside a frame whose top left is {@code (frameX, frameY)}, the button under the pointer lit.
     * Returns what that button says, or null when the pointer is on neither.
     */
    @Nullable
    public static TextKey render(final GuiGraphics g, final int frameX, final int frameY, final int mouseX,
                                 final int mouseY) {
        final Colours c = COLOURS.get();
        final int x = PowerStripLayout.stripX(frameX);
        final int y = PowerStripLayout.stripY(frameY);
        g.fill(x, y, x + PowerStripLayout.W, y + PowerStripLayout.H, c.outline());
        g.fill(x + 1, y + 1, x + PowerStripLayout.W - 1, y + PowerStripLayout.H - 1, c.ground());
        final boolean overPower = over(mouseX, mouseY, x, y + PowerStripLayout.POWER_Y);
        final boolean overRestart = over(mouseX, mouseY, x, y + PowerStripLayout.RESTART_Y);
        button(g, x, y + PowerStripLayout.POWER_Y, overPower, POWER_SIGN, c.power(), c);
        button(g, x, y + PowerStripLayout.RESTART_Y, overRestart, RESTART_SIGN, c.restart(), c);
        return overPower ? POWER : overRestart ? RESTART : null;
    }

    /**
     * Presses the button under the pointer, if one is, for the machine at {@code host} shown on the monitor at
     * {@code monitor}; true when one was pressed.
     */
    public static boolean click(final double mouseX, final double mouseY, final int frameX, final int frameY,
                                final BlockPos host, final BlockPos monitor) {
        final int x = PowerStripLayout.stripX(frameX);
        final int y = PowerStripLayout.stripY(frameY);
        if (over(mouseX, mouseY, x, y + PowerStripLayout.POWER_Y)) {
            PacketDistributor.sendToServer(new MachinePowerPayload(host, monitor, MachinePowerPayload.ACTION_SHUTDOWN));
            return true;
        }
        if (over(mouseX, mouseY, x, y + PowerStripLayout.RESTART_Y)) {
            PacketDistributor.sendToServer(new MachinePowerPayload(host, monitor, MachinePowerPayload.ACTION_RESTART));
            return true;
        }
        return false;
    }

    private static boolean over(final double mouseX, final double mouseY, final int stripX, final int buttonY) {
        final int bx = stripX + PowerStripLayout.buttonX();
        return mouseX >= bx && mouseX < bx + PowerStripLayout.BUTTON && mouseY >= buttonY
                && mouseY < buttonY + PowerStripLayout.BUTTON;
    }

    /* A square button raised out of the strip, its sign in the middle. */
    private static void button(final GuiGraphics g, final int stripX, final int y, final boolean lit,
                               final String[] sign, final int ink, final Colours c) {
        final int x = stripX + PowerStripLayout.buttonX();
        final int size = PowerStripLayout.BUTTON;
        g.fill(x, y, x + size, y + size, lit ? c.faceLit() : c.face());
        g.fill(x, y, x + size, y + 1, c.highlight());
        g.fill(x, y, x + 1, y + size, c.highlight());
        g.fill(x, y + size - 1, x + size, y + size, c.shadow());
        g.fill(x + size - 1, y, x + size, y + size, c.shadow());
        final int ox = x + (size - sign[0].length()) / 2;
        final int oy = y + (size - sign.length) / 2;
        for (int row = 0; row < sign.length; row++) {
            for (int col = 0; col < sign[row].length(); col++) {
                if (sign[row].charAt(col) == '#') {
                    g.fill(ox + col, oy + row, ox + col + 1, oy + row + 1, ink);
                }
            }
        }
    }

    private record Colours(int ground, int outline, int face, int faceLit, int highlight, int shadow, int power,
                           int restart) {
    }
}
