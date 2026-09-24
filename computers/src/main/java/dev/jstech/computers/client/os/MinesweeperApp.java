/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.GameWonPayload;
import dev.jstech.computers.operation.payload.desktop.GamePayloads;
import dev.jstech.computers.program.MinesweeperGame;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Minesweeper as a desktop app. The window chrome follows the installed skin, while the minefield keeps the
 * game's own classic identity (grey bevelled cells, red LED counters, the reset face, and the canonical
 * number colours). All rules live in the pure {@link MinesweeperGame}; the board component only draws it
 * and turns clicks into reveals and flags.
 */
@PaletteHolder
public final class MinesweeperApp implements IDesktopApp {

    /** The game's own colours, the grey board and the numbers it always had: {@code jsc:game/minesweeper}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "game/minesweeper",
            new Colours(0xFFC0C0C0, 0xFFFFFFFF, 0xFF808080, 0xFF9A9A9A, 0xFF200000, 0xFFFF2B2B, 0xFF101010,
                    0xFFD01818, 0xFF0000FF, 0xFF008000, 0xFFFF0000, 0xFF000080, 0xFF800000, 0xFF008080,
                    0xFF000000, 0xFF808080));
    private static final int PANEL_H = 24;

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    private MinesweeperGame.Difficulty difficulty = MinesweeperGame.Difficulty.BEGINNER;
    private MinesweeperGame game = new MinesweeperGame(difficulty, System.nanoTime());
    private boolean timing;
    private long startMs;
    private long frozenSeconds;

    private final Panel root = new Panel();
    private final Map<MinesweeperGame.Difficulty, Button> difficultyButtons =
            new EnumMap<>(MinesweeperGame.Difficulty.class);
    private final Button face;
    private final Board board;

    /** The minefield: a grid of cells sized to the window, revealed with the left button and flagged with the right. */
    private final class Board extends UiComponent {

        private int cell = 12;

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            sunken(g, x() - 2, y() - 2, width() + 4, height() + 4);
            for (int r = 0; r < game.rows(); r++) {
                for (int c = 0; c < game.cols(); c++) {
                    drawCell(g, ctx.font(), r, c);
                }
            }
        }

        private void drawCell(final GuiGraphics g, final Font font, final int r, final int c) {
            final int cx = x() + c * cell;
            final int cy = y() + r * cell;
            final boolean lost = game.state() == MinesweeperGame.State.LOST;
            final boolean showMine = lost && game.isMine(r, c);
            if (game.isRevealed(r, c) || showMine) {
                g.fill(cx, cy, cx + cell, cy + cell, colours().face());
                g.fill(cx, cy, cx + cell, cy + 1, colours().grid());
                g.fill(cx, cy, cx + 1, cy + cell, colours().grid());
                if (showMine) {
                    if (game.isRevealed(r, c)) {
                        g.fill(cx + 1, cy + 1, cx + cell, cy + cell, colours().flag()); // the mine that was triggered
                    }
                    final int m = Math.max(2, cell / 3);
                    g.fill(cx + (cell - m) / 2, cy + (cell - m) / 2, cx + (cell + m) / 2, cy + (cell + m) / 2,
                            colours().mine());
                } else {
                    final int n = game.adjacent(r, c);
                    if (n > 0) {
                        final String s = String.valueOf(n);
                        g.drawString(font, s, cx + (cell - font.width(s)) / 2 + 1, cy + (cell - 8) / 2 + 1,
                                colours().number(n), false);
                    }
                }
                return;
            }
            // Unrevealed raised cell.
            g.fill(cx, cy, cx + cell, cy + cell, colours().face());
            g.fill(cx, cy, cx + cell, cy + 1, colours().bevelLight());
            g.fill(cx, cy, cx + 1, cy + cell, colours().bevelLight());
            g.fill(cx, cy + cell - 1, cx + cell, cy + cell, colours().bevelDark());
            g.fill(cx + cell - 1, cy, cx + cell, cy + cell, colours().bevelDark());
            if (game.isFlagged(r, c)) {
                final int fx = cx + cell / 2;
                g.fill(fx, cy + 2, fx + 1, cy + cell - 3, colours().mine());              // pole
                g.fill(fx - cell / 4, cy + 2, fx, cy + cell / 2, colours().flag());   // flag
                g.fill(cx + cell / 2 - 3, cy + cell - 3, cx + cell / 2 + 3, cy + cell - 2, colours().mine()); // base
            }
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (game.isFinished()) {
                return true;
            }
            final int c = (int) ((mx - x()) / cell);
            final int r = (int) ((my - y()) / cell);
            if (r < 0 || r >= game.rows() || c < 0 || c >= game.cols()) {
                return true;
            }
            if (button == 0) {
                if (!timing) {
                    timing = true;
                    startMs = System.currentTimeMillis();
                }
                game.reveal(r, c);
            } else if (button == 1) {
                game.toggleFlag(r, c);
            }
            if (game.isFinished()) {
                frozenSeconds = timing ? Math.min(999, (System.currentTimeMillis() - startMs) / 1000) : 0;
                if (game.state() == MinesweeperGame.State.WON) {
                    PacketDistributor.sendToServer(new GameWonPayload(host, GamePayloads.MINESWEEPER,
                            difficulty.name().toLowerCase(Locale.ROOT)));
                }
            }
            return true;
        }
    }

    public MinesweeperApp(final BlockPos host) {
        this.host = host;
        for (final MinesweeperGame.Difficulty d : MinesweeperGame.Difficulty.values()) {
            difficultyButtons.put(d, root.add(new Button(shortLabel(d), () -> newGame(d))));
        }
        face = root.add(new Button(this::faceGlyph, () -> newGame(difficulty)));
        board = root.add(new Board());
    }

    private static String shortLabel(final MinesweeperGame.Difficulty d) {
        return GameText.resolve(switch (d) {
            case BEGINNER -> MinesweeperTexts.BEGINNER;
            case INTERMEDIATE -> MinesweeperTexts.INTERMEDIATE;
            case EXPERT -> MinesweeperTexts.EXPERT;
        });
    }

    private String faceGlyph() {
        return switch (game.state()) {
            case LOST -> ":(";
            case WON -> "B)";
            default -> ":)";
        };
    }

    private void newGame(final MinesweeperGame.Difficulty d) {
        this.difficulty = d;
        this.game = new MinesweeperGame(d, System.nanoTime());
        this.timing = false;
        this.frozenSeconds = 0;
    }

    @Override
    public String title() {
        return ProgramClient.nameOf("minesweeper");
    }

    @Override
    public int defaultWidth() {
        return 190;
    }

    @Override
    public int defaultHeight() {
        return 232;
    }

    @Override
    public int minWidth() {
        return 150;
    }

    @Override
    public int minHeight() {
        return 180;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());

        // Difficulty selector row.
        int dx = x + 4;
        for (final MinesweeperGame.Difficulty d : MinesweeperGame.Difficulty.values()) {
            final int dw = font.width(shortLabel(d)) + 10;
            final Button button = difficultyButtons.get(d);
            button.setBounds(dx, y + 3, dw, 13);
            button.setPrimary(d == difficulty);
            dx += dw + 3;
        }

        // Status panel: mine counter, reset face, timer, in a sunken frame.
        final int panelY = y + 19;
        sunken(g, x + 4, panelY, width - 8, PANEL_H);
        led(g, font, x + 8, panelY + 4, game.minesRemaining());
        led(g, font, x + width - 8 - 26, panelY + 4, elapsedSeconds());
        face.setBounds(x + width / 2 - 9, panelY + 3, 18, 18);

        // Board.
        final int boardTop = panelY + PANEL_H + 4;
        final int boardBottom = y + height - 4;
        final int rows = game.rows();
        final int cols = game.cols();
        board.cell = Math.max(7, Math.min(18, Math.min((width - 8) / cols, (boardBottom - boardTop) / rows)));
        final int bw = board.cell * cols;
        board.setBounds(x + (width - bw) / 2, boardTop, bw, board.cell * rows);
        root.render(g, ctx);
    }

    /** A 3-glyph red LED readout for a value, clamped to what fits (negative values keep a leading minus). */
    private static void led(final GuiGraphics g, final Font font, final int x, final int y, final int value) {
        g.fill(x, y, x + 26, y + 15, colours().ledGround());
        final String s;
        if (value < 0) {
            s = "-" + String.format(Locale.ROOT, "%02d", Math.min(99, -value));
        } else {
            s = String.format(Locale.ROOT, "%03d", Math.min(999, value));
        }
        g.drawString(font, s, x + 3, y + 4, colours().ledOn(), false);
    }

    private static void sunken(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, colours().face());
        g.fill(x, y, x + w, y + 1, colours().bevelDark());
        g.fill(x, y, x + 1, y + h, colours().bevelDark());
        g.fill(x, y + h - 1, x + w, y + h, colours().bevelLight());
        g.fill(x + w - 1, y, x + w, y + h, colours().bevelLight());
    }

    private int elapsedSeconds() {
        if (game.isFinished()) {
            return (int) frozenSeconds;
        }
        if (!timing) {
            return 0;
        }
        return (int) Math.min(999, (System.currentTimeMillis() - startMs) / 1000);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    private static Colours colours() {
        return PALETTE.get();
    }

    /**
     * The board's colours: its grey face, the two edges of a bevel, the grid, the counters' ground and digits, a
     * mine, a flag, and the colour of each count from one to eight.
     */
    private record Colours(int face, int bevelLight, int bevelDark, int grid, int ledGround, int ledOn, int mine,
                           int flag, int one, int two, int three, int four, int five, int six, int seven,
                           int eight) {

        /** The colour a count of that many neighbouring mines is written in; nought is never written. */
        int number(final int n) {
            return switch (n) {
                case 1 -> one;
                case 2 -> two;
                case 3 -> three;
                case 4 -> four;
                case 5 -> five;
                case 6 -> six;
                case 7 -> seven;
                case 8 -> eight;
                default -> 0;
            };
        }
    }
}
