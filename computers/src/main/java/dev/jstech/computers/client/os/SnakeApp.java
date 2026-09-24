/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.program.SnakeGame;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Snake as a desktop app.
 *
 * <p>The window chrome follows the installed skin and the arena does not, the same bargain the other game
 * makes. Every rule lives in the pure {@link SnakeGame}; this class draws it, feeds it the arrow keys, and
 * steps it on the clock.
 *
 * <p>The snake moves on wall time rather than on game ticks. A desktop app is drawn from the render pass and
 * has no tick of its own to hang off, and reading the clock keeps it moving at the same speed whatever the
 * frame rate is doing.
 */
@PaletteHolder
public final class SnakeApp implements IDesktopApp {

    private static final int CELL = 7;
    private static final int MARGIN = 4;
    private static final int TOOLBAR_H = 15;
    private static final int STATUS_H = 11;
    /**
     * The arena's own colours, which the installed skin does not touch: {@code jsc:game/snake}. The band the
     * starting hint sits on is there so it reads over whatever the arena is showing.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "game/snake",
            new Colours(0xFF12160F, 0xFF1A2015, 0xFF2E3826, 0xFF6FBF3F, 0xFFB7F07A, 0xFFE0553F, 0xFFF0B23A,
                    0xC0000000, 0xFFFFFFFF, 0xFFB8C2CE, 0xD012160F, 0xFFDCEFE1));
    /** A game tick in milliseconds, which is what the engine's speeds are counted in. */
    private static final long TICK_MS = 50L;

    private OsSkin skin = OsSkin.fallback();
    private SnakeGame.Speed speed = SnakeGame.Speed.NORMAL;
    private boolean walls = true;
    private SnakeGame game = new SnakeGame(System.nanoTime(), true);
    private int best;

    private long lastStepMs = System.currentTimeMillis();
    /** Set on the first key, so a fresh game waits for the player instead of walking into a wall alone. */
    private boolean started;

    /** The speeds in the order they are offered, so the buttons and the clicks read the same list. */
    private static final List<SnakeGame.Speed> SPEEDS = List.of(SnakeGame.Speed.values());

    private final Panel root = new Panel();
    private final Button restart;
    private final Button[] speedButtons = new Button[SPEEDS.size()];
    private final Button wallsButton;

    public SnakeApp() {
        restart = root.add(new Button(GameText.resolve(SnakeTexts.NEW), this::newGame));
        int at = 0;
        for (final SnakeGame.Speed s : SPEEDS) {
            speedButtons[at++] = root.add(new Button(label(s), () -> setSpeed(s)));
        }
        wallsButton = root.add(new Button(this::wallsLabel, this::toggleWalls));
    }

    private static String label(final SnakeGame.Speed s) {
        return GameText.resolve(switch (s) {
            case SLOW -> SnakeTexts.SLOW;
            case NORMAL -> SnakeTexts.NORMAL;
            case FAST -> SnakeTexts.FAST;
        });
    }

    /**
     * The walls button says one word and shows its state by being lit, the way the speeds do.
     *
     * <p>It used to read "No walls" when they were off, which is four letters wider, and the toolbar has
     * no room for that: the button was pushed left onto the last speed and the two drew over each other.
     */
    private String wallsLabel() {
        return GameText.resolve(SnakeTexts.WALLS);
    }

    private void setSpeed(final SnakeGame.Speed s) {
        this.speed = s;
    }

    private void toggleWalls() {
        this.walls = !walls;
        newGame();
    }

    private void newGame() {
        this.game = new SnakeGame(System.nanoTime(), walls);
        this.started = false;
        this.lastStepMs = System.currentTimeMillis();
    }

    @Override
    public String title() {
        return "Snake";
    }

    /**
     * Wide enough for the toolbar rather than only for the arena.
     *
     * <p>The arena wants a hundred and forty eight; the five buttons above it want rather more, and a
     * window sized to the arena alone had them drawn over one another.
     */
    private static final int TOOLBAR_ROOM = 190;

    @Override
    public int defaultWidth() {
        return Math.max(TOOLBAR_ROOM, SnakeGame.COLS * CELL + MARGIN * 2);
    }

    @Override
    public int defaultHeight() {
        return TOOLBAR_H + SnakeGame.ROWS * CELL + STATUS_H + MARGIN * 2;
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
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    /**
     * The best score stays with the machine.
     *
     * <p>A high score nobody can come back to is not a high score. The window's own state is kept by the
     * computer along with the window, so the number is that machine's rather than this session's.
     */
    @Override
    public String saveState() {
        return best > 0 ? String.valueOf(best) : "";
    }

    @Override
    public void restoreState(final String state) {
        if (state == null || state.isBlank()) {
            return;
        }
        try {
            this.best = Math.max(0, Integer.parseInt(state.trim()));
        } catch (final NumberFormatException notANumber) {
            // A state somebody edited into nonsense simply means no best yet.
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        advance();
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layoutToolbar(font, x, y, width);
        root.render(g, ctx);

        final int arenaW = SnakeGame.COLS * CELL;
        final int arenaH = SnakeGame.ROWS * CELL;
        final int ax = x + Math.max(MARGIN, (width - arenaW) / 2);
        final int ay = y + TOOLBAR_H + MARGIN;
        drawArena(g, ax, ay, arenaW, arenaH);
        if (game.isDead()) {
            drawOver(g, font, ax, ay, arenaW, arenaH);
        } else if (!started) {
            drawHint(g, font, ax, ay, arenaW, arenaH);
        }
        drawStatus(g, font, x, y + height - STATUS_H, width);
    }

    private void layoutToolbar(final Font font, final int x, final int y, final int width) {
        int bx = x + MARGIN;
        // At least as wide as it always was, and wider for a language whose word for it is longer.
        final int rw = Math.max(30, font.width(GameText.resolve(SnakeTexts.NEW)) + 8);
        restart.setBounds(bx, y + 2, rw, 12);
        bx += rw + 3;
        for (int i = 0; i < SPEEDS.size(); i++) {
            final int w = font.width(label(SPEEDS.get(i))) + 8;
            speedButtons[i].setBounds(bx, y + 2, w, 12);
            speedButtons[i].setPrimary(SPEEDS.get(i) == speed);
            bx += w + 2;
        }
        final int ww = font.width(wallsLabel()) + 8;
        /*
         * Pinned to the right edge, and never left of where the speeds end: a narrow window would
         * otherwise slide it back over the last of them rather than simply running out of room.
         */
        wallsButton.setBounds(Math.max(bx, x + width - MARGIN - ww), y + 2, ww, 12);
        wallsButton.setPrimary(walls);
    }

    private void drawArena(final GuiGraphics g, final int ax, final int ay,
                           final int arenaW, final int arenaH) {
        final Colours c = PALETTE.get();
        g.fill(ax - 1, ay - 1, ax + arenaW + 1, ay + arenaH + 1, c.wall());
        for (int row = 0; row < SnakeGame.ROWS; row++) {
            for (int col = 0; col < SnakeGame.COLS; col++) {
                final int cx = ax + col * CELL;
                final int cy = ay + row * CELL;
                // A faint check, so the arena reads as a grid the snake moves on rather than as a flat box.
                g.fill(cx, cy, cx + CELL, cy + CELL, (col + row) % 2 == 0 ? c.ground() : c.groundAlt());
                if (game.isFood(col, row)) {
                    g.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, c.food());
                } else if (game.isHead(col, row)) {
                    g.fill(cx, cy, cx + CELL, cy + CELL, c.head());
                } else if (game.isBody(col, row)) {
                    g.fill(cx, cy, cx + CELL, cy + CELL, c.body());
                }
            }
        }
    }

    private void drawOver(final GuiGraphics g, final Font font, final int ax, final int ay,
                          final int arenaW, final int arenaH) {
        final Colours c = PALETTE.get();
        g.fill(ax, ay, ax + arenaW, ay + arenaH, c.overShade());
        centre(g, font, GameText.resolve(SnakeTexts.GAME_OVER), ax, ay + arenaH / 2 - 10, arenaW, c.overInk());
        centre(g, font, GameText.resolve(SnakeTexts.SCORE.with(game.score())), ax, ay + arenaH / 2, arenaW,
                c.score());
        centre(g, font, GameText.resolve(SnakeTexts.AGAIN), ax, ay + arenaH / 2 + 10, arenaW, c.again());
    }

    private void drawHint(final GuiGraphics g, final Font font, final int ax, final int ay,
                          final int arenaW, final int arenaH) {
        // On a band of its own, because the snake is lying right where the words go.
        final String text = GameText.resolve(SnakeTexts.START_HINT);
        final int ty = ay + arenaH / 2 - 5;
        g.fill(ax, ty - 2, ax + arenaW, ty + 11, PALETTE.get().hintBand());
        centre(g, font, text, ax, ty + 1, arenaW, PALETTE.get().hint());
    }

    private static void centre(final GuiGraphics g, final Font font, final String text,
                               final int x, final int y, final int width, final int colour) {
        g.drawString(font, text, x + (width - font.width(text)) / 2, y, colour, false);
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        final String left = GameText.resolve(SnakeTexts.SCORE_AND_BEST.with(game.score(), best));
        g.drawString(font, left, x + MARGIN, y + 2, skin.text(), false);
        final String right = GameText.resolve(SnakeTexts.LENGTH.with(game.length()));
        g.drawString(font, right, x + width - MARGIN - font.width(right), y + 2, skin.dim(), false);
    }

    /** Steps the game as many times as the clock says have come due since the last frame. */
    private void advance() {
        if (!started || game.isDead()) {
            this.lastStepMs = System.currentTimeMillis();
            return;
        }
        final long stepMs = speed.ticksPerStep() * TICK_MS;
        final long now = System.currentTimeMillis();
        /*
         * Capped rather than caught up: a window that was behind a menu for a minute would otherwise take
         * hundreds of steps in one frame and kill a snake the player never saw move.
         */
        int budget = 4;
        while (now - lastStepMs >= stepMs && budget-- > 0 && !game.isDead()) {
            game.step();
            this.lastStepMs += stepMs;
        }
        if (now - lastStepMs >= stepMs) {
            this.lastStepMs = now;
        }
        if (game.isDead()) {
            this.best = Math.max(best, game.score());
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final SnakeGame.Direction to = switch (key) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> SnakeGame.Direction.UP;
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> SnakeGame.Direction.DOWN;
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> SnakeGame.Direction.LEFT;
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> SnakeGame.Direction.RIGHT;
            default -> null;
        };
        if (to == null) {
            return false;
        }
        if (game.isDead()) {
            newGame();
            return true;
        }
        if (!started) {
            this.started = true;
            this.lastStepMs = System.currentTimeMillis();
        }
        game.turn(to);
        return true;
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * The arena's colours: its checked ground, the wall round it, the snake's body and head, the food, the end of a
     * game (the shade over the arena, its heading, the score and the line under it), and the starting hint's band
     * and words.
     */
    private record Colours(int ground, int groundAlt, int wall, int body, int head, int food, int overInk,
                           int overShade, int score, int again, int hintBand, int hint) {
    }
}
