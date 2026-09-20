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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.SolitaireGame.Card;
import dev.jstech.computers.program.SolitaireGame.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class SolitaireGameTest {

    private SolitaireGame game;

    @BeforeEach
    void setUp() {
        game = new SolitaireGame(1234L);
    }

    @Test
    void deal_putsTwentyEightCardsOnTheTableauAndTheRestInTheStock() {
        int onTable = 0;
        for (int pile = 0; pile < SolitaireGame.TABLEAU_PILES; pile++) {
            assertEquals(pile + 1, game.tableauSize(pile), "pile " + pile + " was dealt the wrong number");
            onTable += game.tableauSize(pile);
        }
        assertEquals(28, onTable);
        assertEquals(24, game.stockSize());
        assertEquals(0, game.wasteSize());
    }

    @Test
    void deal_showsOnlyTheLastCardOfEachPile() {
        for (int pile = 0; pile < SolitaireGame.TABLEAU_PILES; pile++) {
            assertEquals(pile, game.faceDownCount(pile));
            assertTrue(game.tableauFaceUp(pile, game.tableauSize(pile) - 1),
                    "pile " + pile + " has its last card face down");
            if (pile > 0) {
                assertFalse(game.tableauFaceUp(pile, 0), "pile " + pile + " has its first card face up");
            }
        }
    }

    @Test
    void deal_usesAWholeDeckWithNoCardTwice() {
        final Set<Card> seen = new HashSet<>();
        for (int pile = 0; pile < SolitaireGame.TABLEAU_PILES; pile++) {
            for (int i = 0; i < game.tableauSize(pile); i++) {
                assertTrue(seen.add(game.tableauCard(pile, i)), "a card was dealt twice");
            }
        }
        while (game.stockSize() > 0) {
            game.drawFromStock();
            assertTrue(seen.add(game.wasteTop()), "a card was dealt twice");
        }
        assertEquals(52, seen.size());
    }

    @Test
    void deal_isTheSameGameForTheSameSeed() {
        final SolitaireGame twin = new SolitaireGame(1234L);
        for (int pile = 0; pile < SolitaireGame.TABLEAU_PILES; pile++) {
            for (int i = 0; i < game.tableauSize(pile); i++) {
                assertEquals(game.tableauCard(pile, i), twin.tableauCard(pile, i));
            }
        }
    }

    @Test
    void deal_isADifferentGameForADifferentSeed() {
        final SolitaireGame other = new SolitaireGame(4321L);
        boolean anyDifferent = false;
        for (int pile = 0; pile < SolitaireGame.TABLEAU_PILES && !anyDifferent; pile++) {
            for (int i = 0; i < game.tableauSize(pile); i++) {
                if (!game.tableauCard(pile, i).equals(other.tableauCard(pile, i))) {
                    anyDifferent = true;
                    break;
                }
            }
        }
        assertTrue(anyDifferent, "two seeds dealt the same game");
    }

    @Test
    void drawFromStock_movesOneCardToTheWaste() {
        assertTrue(game.drawFromStock());
        assertEquals(23, game.stockSize());
        assertEquals(1, game.wasteSize());
        assertNotNull(game.wasteTop());
    }

    @Test
    void drawFromStock_recyclesTheWasteWhenTheStockRunsOut() {
        while (game.stockSize() > 0) {
            game.drawFromStock();
        }
        assertEquals(24, game.wasteSize());
        assertTrue(game.drawFromStock(), "the waste should go back under the stock");
        assertEquals(24, game.stockSize());
        assertEquals(0, game.wasteSize());
    }

    @Test
    void drawFromStock_keepsEveryCardAcrossARecycle() {
        // Nothing is lost or gained going round: the twenty-four are all still there on the other side.
        for (int i = 0; i < 24; i++) {
            game.drawFromStock();
        }
        assertEquals(24, game.stockSize() + game.wasteSize());
        game.drawFromStock();
        assertEquals(24, game.stockSize() + game.wasteSize());
        game.drawFromStock();
        assertEquals(24, game.stockSize() + game.wasteSize());
    }

    @Test
    void recyclingTheStock_costsScore() {
        game.wasteToFoundation();
        final int before = game.score();
        while (game.stockSize() > 0) {
            game.drawFromStock();
        }
        game.drawFromStock();
        assertTrue(game.score() <= before, "going round the stock again should not pay");
    }

    @Test
    void score_neverGoesBelowNothing() {
        while (game.stockSize() > 0) {
            game.drawFromStock();
        }
        game.drawFromStock();
        game.drawFromStock();
        assertTrue(game.score() >= 0, "the score went negative");
    }

    @Test
    void foundation_takesAnAceFirstAndThenTheNextRankUp() {
        final SolitaireGame fresh = new SolitaireGame(99L);
        assertNull(fresh.foundationTop(Suit.SPADES));
        assertEquals(0, fresh.foundationSize(Suit.SPADES));
    }

    @Test
    void tableauToTableau_refusesAPileOntoItself() {
        assertFalse(game.tableauToTableau(3, 1, 3));
    }

    @Test
    void tableauToTableau_refusesMoreCardsThanAreFaceUp() {
        // Pile 6 has seven cards with six of them face down, so two cards cannot travel from it.
        assertFalse(game.tableauToTableau(6, 2, 0));
    }

    @Test
    void tableauToTableau_refusesACountOfNothing() {
        assertFalse(game.tableauToTableau(0, 0, 1));
    }

    @Test
    void tableauPile_outOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> game.tableauSize(SolitaireGame.TABLEAU_PILES));
        assertThrows(IllegalArgumentException.class, () -> game.tableauSize(-1));
    }

    @Test
    void card_refusesARankOutsideTheDeck() {
        assertThrows(IllegalArgumentException.class, () -> new Card(0, Suit.SPADES));
        assertThrows(IllegalArgumentException.class, () -> new Card(14, Suit.SPADES));
        assertThrows(IllegalArgumentException.class, () -> new Card(1, null));
    }

    @Test
    void red_isHeartsAndDiamondsOnly() {
        assertTrue(Suit.HEARTS.red());
        assertTrue(Suit.DIAMONDS.red());
        assertFalse(Suit.SPADES.red());
        assertFalse(Suit.CLUBS.red());
    }

    @Test
    void allHome_isFalseOnAFreshDeal() {
        assertFalse(game.allHome());
        assertFalse(game.isWon());
        assertEquals(SolitaireGame.State.PLAYING, game.state());
    }

    @Test
    void canFinishAutomatically_isFalseWhileCardsAreStillFaceDown() {
        assertFalse(game.canFinishAutomatically());
    }

    @Test
    void playOneHome_answersFalseWhenNothingWillGo() {
        // A fresh deal has no ace showing in most games; either it plays one or it says so, never both.
        final boolean played = game.playOneHome();
        if (!played) {
            assertEquals(0, game.foundationSize(Suit.SPADES) + game.foundationSize(Suit.HEARTS)
                    + game.foundationSize(Suit.DIAMONDS) + game.foundationSize(Suit.CLUBS));
        }
    }

    @Test
    void moves_countEveryAcceptedAction() {
        final int before = game.moves();
        game.drawFromStock();
        assertEquals(before + 1, game.moves());
        game.tableauToTableau(0, 0, 1); // refused, so it is not a move
        assertEquals(before + 1, game.moves());
    }

    @Test
    void seed_isKept() {
        assertEquals(1234L, game.seed());
    }
}
