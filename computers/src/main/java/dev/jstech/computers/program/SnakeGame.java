/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * The pure rules of Snake: a grid, a snake that grows by what it eats, and food that lands where the seed
 * says it does.
 *
 * <p>The seed is what makes a run reproducible, so a test can play a whole game and a player can be given
 * back the same one. Walls can be turned off, and then the grid wraps: the snake leaves one side and comes
 * back on the other, which takes away the easiest way to die and leaves only running into itself.
 *
 * <p>It touches no Minecraft type, so the whole of it runs under plain JUnit.
 */
public final class SnakeGame {

    /** How wide and tall the arena is, which is the same on every machine that runs this. */
    public static final int COLS = 20;
    public static final int ROWS = 12;

    /** What one piece of food is worth, before the bonus for how long the snake already is. */
    private static final int FOOD_SCORE = 10;

    /** The snake starts lying still with this many segments, in the middle of the arena. */
    private static final int START_LENGTH = 4;

    /** How fast the snake moves, in ticks between one step and the next. */
    public enum Speed {
        SLOW(6), NORMAL(4), FAST(2);

        private final int ticksPerStep;

        Speed(final int ticksPerStep) {
            this.ticksPerStep = ticksPerStep;
        }

        /** Game ticks between steps; fewer is faster. */
        public int ticksPerStep() {
            return ticksPerStep;
        }
    }

    /** Which way the snake is going. A snake may not turn straight back on itself. */
    public enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(final int dx, final int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public int dx() {
            return dx;
        }

        public int dy() {
            return dy;
        }

        /** The way back, which is the one turn a snake is never allowed to make. */
        public Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
            };
        }
    }

    public enum State { PLAYING, DEAD }

    /*
     * The body is a deque of packed cells: the head is at the front, the tail at the back, so a step is one
     * push and at most one pop whatever the snake's length. The occupancy set beside it is what makes the
     * collision test a constant-time array read instead of a walk down the whole body every step.
     */
    private final Deque<Integer> body = new ArrayDeque<>(COLS * ROWS);
    private final boolean[] occupied = new boolean[COLS * ROWS];

    private final Random random;
    private final long seed;
    private final boolean walls;

    private Direction direction = Direction.RIGHT;
    /** The turn taken this step, held so two turns in one step cannot fold the snake onto itself. */
    private Direction pending = Direction.RIGHT;
    private int food;
    private int score;
    private State state = State.PLAYING;

    /**
     * Starts a game.
     *
     * @param seed  the number the food's places come from; the same seed plays out the same way
     * @param walls whether the edges kill, or the grid wraps round instead
     */
    public SnakeGame(final long seed, final boolean walls) {
        this.seed = seed;
        this.walls = walls;
        this.random = new Random(seed);
        final int row = ROWS / 2;
        for (int i = 0; i < START_LENGTH; i++) {
            // Laid out left to right, so the head is the rightmost segment and the snake is already moving.
            final int cell = cell(1 + i, row);
            body.addFirst(cell);
            occupied[cell] = true;
        }
        this.food = placeFood();
    }

    /** The number this game was seeded with, so the same run can be played again. */
    public long seed() {
        return seed;
    }

    /** Whether the edges of the arena kill, rather than wrapping round to the other side. */
    public boolean hasWalls() {
        return walls;
    }

    public State state() {
        return state;
    }

    public boolean isDead() {
        return state == State.DEAD;
    }

    public int score() {
        return score;
    }

    /** How many cells the snake is long, which is what it has eaten plus what it started with. */
    public int length() {
        return body.size();
    }

    public Direction direction() {
        return direction;
    }

    /** Where the head is, packed as {@code x + y * COLS}. */
    public int head() {
        final Integer first = body.peekFirst();
        return first == null ? 0 : first;
    }

    /** Where the food is, packed as {@code x + y * COLS}, or -1 when the arena is full. */
    public int food() {
        return food;
    }

    /** Whether the snake covers a cell, which is a single array read. */
    public boolean isBody(final int x, final int y) {
        return inside(x, y) && occupied[cell(x, y)];
    }

    /** Whether the head is on a cell. */
    public boolean isHead(final int x, final int y) {
        return inside(x, y) && cell(x, y) == head();
    }

    /** Whether the food is on a cell. */
    public boolean isFood(final int x, final int y) {
        return food >= 0 && inside(x, y) && cell(x, y) == food;
    }

    /**
     * Asks the snake to turn, taking effect on its next step.
     *
     * <p>Only the turn straight back on itself is refused, and it is judged against the way the snake is
     * really going rather than against a turn asked for since: two presses between one step and the next
     * would otherwise reverse it through the corner. Asking for the way it is already going is allowed, and
     * takes back a turn asked for a moment ago, which is what a player expects of a key they pressed by
     * mistake.
     */
    public boolean turn(final Direction to) {
        if (state != State.PLAYING || to == null || to == direction.opposite()) {
            return false;
        }
        this.pending = to;
        return true;
    }

    /**
     * Moves the snake one cell.
     *
     * <p>Answers whether it ate this step, which is what a caller makes a noise or a flash out of. A dead
     * snake does nothing at all, so calling on regardless is safe.
     */
    public boolean step() {
        if (state != State.PLAYING) {
            return false;
        }
        direction = pending;
        final int headCell = head();
        int x = headCell % COLS + direction.dx();
        int y = headCell / COLS + direction.dy();
        if (!inside(x, y)) {
            if (walls) {
                state = State.DEAD;
                return false;
            }
            x = Math.floorMod(x, COLS);
            y = Math.floorMod(y, ROWS);
        }
        final int next = cell(x, y);
        final boolean eating = next == food;
        /*
         * The tail leaves as the head arrives, so the cell it is vacating is not a collision. Without this a
         * snake at full stretch dies every time it follows its own tail, which is ordinary play.
         */
        final Integer tail = body.peekLast();
        if (!eating && tail != null && next == tail) {
            body.pollLast();
            occupied[tail] = false;
        }
        if (occupied[next]) {
            state = State.DEAD;
            return false;
        }
        body.addFirst(next);
        occupied[next] = true;
        if (eating) {
            // Longer snakes are worth more, which is what makes a long run worth keeping rather than restarting.
            score += FOOD_SCORE + body.size() / 2;
            food = placeFood();
        } else if (tail != null && occupied[tail]) {
            body.pollLast();
            occupied[tail] = false;
        }
        return eating;
    }

    /** Puts food on a free cell picked from the seed, or -1 when the snake covers the whole arena. */
    private int placeFood() {
        final int free = COLS * ROWS - body.size();
        if (free <= 0) {
            return -1;
        }
        /*
         * Counted rather than guessed: picking at random and retrying is unbounded once the arena is nearly
         * full, and that is exactly when a long game would stall. Walking to the nth free cell is bounded by
         * the size of the arena and gives the same distribution.
         */
        int nth = random.nextInt(free);
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i] && nth-- == 0) {
                return i;
            }
        }
        return -1;
    }

    private static boolean inside(final int x, final int y) {
        return x >= 0 && x < COLS && y >= 0 && y < ROWS;
    }

    private static int cell(final int x, final int y) {
        return x + y * COLS;
    }
}
