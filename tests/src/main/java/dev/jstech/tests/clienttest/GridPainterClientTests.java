/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.font.CellFonts;
import dev.jstech.core.client.font.GridPainter;
import dev.jstech.core.font.CoreFonts;
import dev.jstech.core.font.GridSpan;
import dev.jstech.core.font.IGridPiece;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The Core's grid painter on the real client, in the Core's terminal font: a frame of box lines, the blocks and a
 * shade, letters of the font, and a character only the game's font has, read back from the frame the player sees.
 */
public final class GridPainterClientTests {

    private static final int FRAME_WAIT = 80;
    /** What the test's screen clears to: black, which no part of the grid is drawn in. */
    private static final int BACKDROP = 0xFF000000;
    /** The colour the grid is drawn in. */
    private static final int INK = 0xFF00FF00;
    private static final int LEFT = 20;
    private static final int TOP = 20;
    private static final int PITCH = 10;
    /** A character of the basic plane the terminal font lacks and the game's font draws: the CJK ideograph "middle". */
    private static final int MIDDLE = 0x4E2D;
    private static final List<String> ROWS = List.of(
            "┌──┐ A█",
            "│▀▄│ i░",
            "└──┘ " + Character.toString(MIDDLE));

    private GridPainterClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void gridPainter_drawsTheCellFontAndFillsTheBoxLinesAndBlocks(final ClientTestContext ctx) {
        final Canvas canvas = new Canvas();
        ctx.then(0, () -> ctx.mc().setScreen(canvas))
                .thenWaitUntil(() -> canvas.frames >= 3, FRAME_WAIT, "the grid to be drawn a few times")
                .thenScreenshot(2, "grid-painter")
                .thenAssert(0, () -> CellFonts.count(CoreFonts.FIXED_6X10) > 1500,
                        "the Core's terminal font is loaded with its characters")
                .thenAssert(0, () -> CellFonts.covers(CoreFonts.FIXED_6X10, 'A')
                                && !CellFonts.covers(CoreFonts.FIXED_6X10, MIDDLE),
                        "the font has the Latin letters and not the ideographs")
                .then(0, () -> {
                    try (Frame frame = Frame.take(ctx.mc())) {
                        read(ctx, frame);
                    }
                })
                .thenAssert(0, () -> canvas.painter.layout(ctx.mc().font, List.of(new GridSpan<>(ROWS.get(2), INK)))
                                .stream().anyMatch(piece -> piece instanceof IGridPiece.Text<Integer> text
                                        && !text.cellFont()),
                        "the character the terminal font lacks is laid out in the game's font")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    private static void read(final ClientTestContext ctx, final Frame frame) {
        // The full block, cell 6 of row 0: every corner and the middle of its cell.
        ctx.assertTrue(frame.ink(LEFT + 36, TOP) && frame.ink(LEFT + 41, TOP + 9) && frame.ink(LEFT + 38, TOP + 5),
                "the full block fills its whole cell");
        ctx.assertTrue(frame.ink(LEFT + 2, TOP + 9) && frame.ink(LEFT + 2, TOP + 10) && frame.ink(LEFT + 2, TOP + 19)
                && frame.ink(LEFT + 2, TOP + 20), "the frame's left side runs on from row to row with no gap");
        ctx.assertTrue(frame.ink(LEFT + 5, TOP + 5) && frame.ink(LEFT + 6, TOP + 5) && frame.ink(LEFT + 17, TOP + 5)
                && frame.ink(LEFT + 18, TOP + 5), "the frame's top runs on from cell to cell");
        // The A of the font, cell 5 of row 0: its apex on row 1, its bar across row 5, nothing on row 0.
        ctx.assertTrue(frame.ink(LEFT + 32, TOP + 1), "the A's apex is where the font puts it");
        ctx.assertTrue(!frame.ink(LEFT + 32, TOP), "the row above the A is the empty top of its cell");
        ctx.assertTrue(frame.ink(LEFT + 30, TOP + 5) && frame.ink(LEFT + 34, TOP + 5) && !frame.ink(LEFT + 35, TOP + 5),
                "the A's bar is five pixels across its cell");
        ctx.assertTrue(frame.ink(LEFT + 9, TOP + 10) && !frame.ink(LEFT + 9, TOP + 15),
                "the upper half block fills the top of its cell only");
        final int shade = frame.green(LEFT + 38, TOP + 15);
        ctx.assertTrue(shade > 40 && shade < 90, "the light shade is a quarter of the colour, not " + shade);
        boolean ideograph = false;
        for (int y = TOP + 20; y < TOP + 30; y++) {
            for (int x = LEFT + 28; x < LEFT + 40; x++) {
                ideograph |= frame.ink(x, y);
            }
        }
        ctx.assertTrue(ideograph, "the game's font draws the character the terminal font lacks");
    }

    /** One captured frame, read at logical GUI coordinates. */
    private record Frame(NativeImage image, double scale) implements AutoCloseable {

        static Frame take(final Minecraft mc) {
            return new Frame(Screenshot.takeScreenshot(mc.getMainRenderTarget()), mc.getWindow().getGuiScale());
        }

        @Override
        public void close() {
            image.close();
        }

        /** The green of the pixel in the middle of that logical pixel. */
        int green(final int x, final int y) {
            final int abgr = image.getPixelRGBA((int) ((x + 0.5) * scale), (int) ((y + 0.5) * scale));
            return (abgr >> 8) & 0xFF;
        }

        /** Whether that logical pixel is in the grid's colour. */
        boolean ink(final int x, final int y) {
            final int abgr = image.getPixelRGBA((int) ((x + 0.5) * scale), (int) ((y + 0.5) * scale));
            return (abgr & 0xFF) < 30 && ((abgr >> 8) & 0xFF) > 220 && ((abgr >> 16) & 0xFF) < 30;
        }
    }

    /** A screen that clears to black and draws the grid onto itself, counting the frames it drew. */
    private static final class Canvas extends Screen {

        private final GridPainter<Integer> painter = new GridPainter<>(CoreFonts.FIXED_6X10);
        private int frames;

        Canvas() {
            super(Component.empty());
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, width, height, BACKDROP);
            painter.draw(g, font, ROWS, row -> List.of(new GridSpan<>(row, INK)), LEFT, TOP, PITCH,
                    Integer::intValue, BACKDROP);
            frames++;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
