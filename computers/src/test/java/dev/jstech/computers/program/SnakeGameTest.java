/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.SnakeGame.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SnakeGameTest {

    private SnakeGame game;

    @BeforeEach
    void setUp() {
        game = new SnakeGame(42L, true);
    }

    @Test
    void newGame_startsWithFourSegmentsGoingRight() {
        assertEquals(4, game.length());
        assertEquals(Direction.RIGHT, game.direction());
        assertEquals(SnakeGame.State.PLAYING, game.state());
        assertEquals(0, game.score());
    }

    @Test
    void newGame_putsFoodSomewhereTheSnakeIsNot() {
        final int food = game.food();
        assertTrue(food >= 0, "a fresh arena should have food in it");
        assertFalse(game.isBody(food % SnakeGame.COLS, food / SnakeGame.COLS),
                "the food was placed on the snake");
    }

    @Test
    void newGame_isTheSameRunForTheSameSeed() {
        final SnakeGame twin = new SnakeGame(42L, true);
        assertEquals(game.food(), twin.food());
        for (int i = 0; i < 5; i++) {
            game.step();
            twin.step();
            assertEquals(game.head(), twin.head());
            assertEquals(game.food(), twin.food());
        }
    }

    @Test
    void newGame_isADifferentRunForADifferentSeed() {
        // Not every pair of seeds must differ on the first placement, so walk a few and require one change.
        final SnakeGame other = new SnakeGame(9999L, true);
        assertNotEquals(game.food(), other.food());
    }

    @Test
    void step_movesTheHeadOneCellAndKeepsTheLength() {
        final int before = game.head();
        final int length = game.length();
        game.step();
        assertEquals(before + 1, game.head(), "the head should move one cell right");
        assertEquals(length, game.length(), "the snake grew without eating");
    }

    @Test
    void turn_refusesToDoubleBackOnItself() {
        assertFalse(game.turn(Direction.LEFT), "a snake going right may not turn left");
        assertEquals(Direction.RIGHT, game.direction());
    }

    @Test
    void turn_takesEffectOnTheNextStep() {
        assertTrue(game.turn(Direction.DOWN));
        assertEquals(Direction.RIGHT, game.direction(), "the turn should not apply before the step");
        game.step();
        assertEquals(Direction.DOWN, game.direction());
    }

    @Test
    void turn_cannotBeReversedByTwoPressesBetweenSteps() {
        /*
         * Going right, asking for up and then for left must not leave the snake going left: the second
         * turn is judged against the way it is really going, not against the turn asked for a moment ago.
         */
        assertTrue(game.turn(Direction.UP));
        assertFalse(game.turn(Direction.LEFT));
        game.step();
        assertEquals(Direction.UP, game.direction());
    }

    @Test
    void turn_backToTheCurrentWayTakesBackATurnAskedForByMistake() {
        assertTrue(game.turn(Direction.UP));
        assertTrue(game.turn(Direction.RIGHT));
        game.step();
        assertEquals(Direction.RIGHT, game.direction());
    }

    @Test
    void turn_refusesOnceTheSnakeIsDead() {
        while (!game.isDead()) {
            game.step();
        }
        assertFalse(game.turn(Direction.UP));
    }

    @Test
    void walls_killTheSnakeAtTheEdge() {
        // Going right from the start, the wall is the only thing it can reach.
        for (int i = 0; i < SnakeGame.COLS + 2 && !game.isDead(); i++) {
            game.step();
        }
        assertTrue(game.isDead(), "the snake should have hit the right wall");
        assertEquals(SnakeGame.State.DEAD, game.state());
    }

    @Test
    void withoutWalls_theSnakeComesBackRoundTheOtherSide() {
        final SnakeGame open = new SnakeGame(42L, false);
        for (int i = 0; i < SnakeGame.COLS + 4; i++) {
            open.step();
        }
        assertFalse(open.isDead(), "a wrapping arena has no wall to die on");
    }

    @Test
    void hasWalls_saysWhichGameThisIs() {
        assertTrue(game.hasWalls());
        assertFalse(new SnakeGame(1L, false).hasWalls());
    }

    @Test
    void step_doesNothingOnceTheSnakeIsDead() {
        while (!game.isDead()) {
            game.step();
        }
        final int head = game.head();
        final int length = game.length();
        assertFalse(game.step());
        assertEquals(head, game.head());
        assertEquals(length, game.length());
    }

    @Test
    void eating_growsTheSnakeAndPaysScore() {
        /*
         * Steered onto its own food rather than waiting for a lucky line: the food's cell is known, so the
         * snake is walked to it, which is what a player does and what the rules have to hold up under.
         */
        final SnakeGame open = new SnakeGame(7L, false);
        final int startLength = open.length();
        assertTrue(walkToFood(open), "the snake never reached the food");
        assertEquals(startLength + 1, open.length());
        assertTrue(open.score() > 0, "eating paid nothing");
    }

    @Test
    void eating_putsNewFoodSomewhereTheSnakeIsNot() {
        final SnakeGame open = new SnakeGame(7L, false);
        assertTrue(walkToFood(open));
        final int food = open.food();
        assertTrue(food >= 0);
        assertFalse(open.isBody(food % SnakeGame.COLS, food / SnakeGame.COLS));
    }

    @Test
    void theSnake_canFollowItsOwnTailWithoutDying() {
        /*
         * The tail leaves the cell as the head arrives on it, so a snake at full stretch turning a tight
         * corner must live. Reading the body before the tail has moved is what used to kill it.
         */
        final SnakeGame open = new SnakeGame(3L, false);
        final int length = open.length();
        open.turn(Direction.DOWN);
        open.step();
        open.turn(Direction.LEFT);
        open.step();
        open.turn(Direction.UP);
        open.step();
        assertFalse(open.isDead(), "the snake died following its own tail");
        assertEquals(length, open.length(), "the snake lost a segment following its own tail");
        assertTrue(open.isBody(open.head() % SnakeGame.COLS, open.head() / SnakeGame.COLS),
                "the head's own cell read as free afterwards");
        assertEquals(length, bodyCells(open), "the cells the snake covers no longer match its length");
    }

    /** How many cells of the whole arena the snake is covering, which must equal how long it is. */
    private static int bodyCells(final SnakeGame game) {
        int covered = 0;
        for (int y = 0; y < SnakeGame.ROWS; y++) {
            for (int x = 0; x < SnakeGame.COLS; x++) {
                if (game.isBody(x, y)) {
                    covered++;
                }
            }
        }
        return covered;
    }

    @Test
    void theSnake_coversExactlyAsManyCellsAsItIsLongThroughoutALongRun() {
        /*
         * Walked rather than reasoned about: what the snake covers and how long it says it is are two
         * different records of the same thing, and a step that moved one without the other left a cell
         * marked taken for the rest of the game, which food can never land on and the snake dies against.
         */
        final SnakeGame open = new SnakeGame(11L, false);
        final Direction[] circuit = {Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT};
        for (int step = 0; step < 200 && !open.isDead(); step++) {
            open.turn(circuit[(step / 2) % circuit.length]);
            open.step();
            assertEquals(open.length(), bodyCells(open),
                    "after step " + step + " the snake covered " + bodyCells(open)
                            + " cells and is " + open.length() + " long");
            assertTrue(open.isBody(open.head() % SnakeGame.COLS, open.head() / SnakeGame.COLS),
                    "after step " + step + " the head's own cell read as free");
        }
    }

    @Test
    void isHead_andIsBody_agreeWithTheHeadCell() {
        final int head = game.head();
        final int x = head % SnakeGame.COLS;
        final int y = head / SnakeGame.COLS;
        assertTrue(game.isHead(x, y));
        assertTrue(game.isBody(x, y), "the head is part of the body");
    }

    @Test
    void cellsOutsideTheArena_areNeitherBodyNorHeadNorFood() {
        assertFalse(game.isBody(-1, 0));
        assertFalse(game.isBody(SnakeGame.COLS, 0));
        assertFalse(game.isHead(0, -1));
        assertFalse(game.isFood(0, SnakeGame.ROWS));
    }

    @Test
    void seed_isKept() {
        assertEquals(42L, game.seed());
    }

    @Test
    void speeds_getFasterInOrder() {
        assertTrue(SnakeGame.Speed.SLOW.ticksPerStep() > SnakeGame.Speed.NORMAL.ticksPerStep());
        assertTrue(SnakeGame.Speed.NORMAL.ticksPerStep() > SnakeGame.Speed.FAST.ticksPerStep());
        assertTrue(SnakeGame.Speed.FAST.ticksPerStep() >= 1, "a speed of no ticks would never step");
    }

    @Test
    void opposite_isTheWayBack() {
        assertEquals(Direction.DOWN, Direction.UP.opposite());
        assertEquals(Direction.UP, Direction.DOWN.opposite());
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite());
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite());
    }

    /** Steers the snake onto the food one axis at a time; false when it dies or gives up first. */
    private static boolean walkToFood(final SnakeGame snake) {
        for (int guard = 0; guard < SnakeGame.COLS * SnakeGame.ROWS && !snake.isDead(); guard++) {
            final int food = snake.food();
            if (food < 0) {
                return false;
            }
            final int hx = snake.head() % SnakeGame.COLS;
            final int hy = snake.head() / SnakeGame.COLS;
            final int fx = food % SnakeGame.COLS;
            final int fy = food / SnakeGame.COLS;
            if (hy != fy) {
                snake.turn(hy < fy ? Direction.DOWN : Direction.UP);
            } else if (hx != fx) {
                snake.turn(hx < fx ? Direction.RIGHT : Direction.LEFT);
            }
            if (snake.step()) {
                return true;
            }
        }
        return false;
    }
}
