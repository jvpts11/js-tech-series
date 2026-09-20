/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The pure rules of Klondike solitaire, drawing one card at a time.
 *
 * <p>A stock to turn over, a waste to take from, four foundations that fill upward in a suit from the ace,
 * and seven tableau piles that fall downward in alternating colours. The deal comes from a seeded shuffle,
 * so the same number always deals the same game: that is what lets a test play a whole hand, and what lets a
 * player hand a game to somebody else by its number.
 *
 * <p>It touches no Minecraft type, so the whole of it runs under plain JUnit.
 */
public final class SolitaireGame {

    /** How many piles the tableau has, which is the seven every Klondike deal uses. */
    public static final int TABLEAU_PILES = 7;

    /** One foundation per suit, each filling from its ace upward. */
    public static final int FOUNDATIONS = 4;

    /** The highest rank, the king, which is also the only card an empty tableau pile accepts. */
    public static final int KING = 13;

    /** The lowest rank, the ace, which is the only card an empty foundation accepts. */
    public static final int ACE = 1;

    /*
     * What each move is worth, in the scoring the game has always used. Turning a face-down card over and
     * freeing a card out of the waste both pay, because both open the game up; sending a card home pays most;
     * taking one back off a foundation costs, because it undoes progress; and going round the stock again
     * costs heavily, because a player who keeps cycling is not making any.
     */
    private static final int SCORE_WASTE_TO_TABLEAU = 5;
    private static final int SCORE_TURN_OVER = 5;
    private static final int SCORE_TO_FOUNDATION = 10;
    private static final int SCORE_FROM_FOUNDATION = -15;
    private static final int SCORE_RECYCLE = -100;

    private static final int DECK_SIZE = 52;

    /** The four suits, two of them red, which is what the tableau alternates by. */
    public enum Suit {
        SPADES, HEARTS, DIAMONDS, CLUBS;

        /** Whether this suit is a red one, which is the only thing the tableau's rule asks of it. */
        public boolean red() {
            return this == HEARTS || this == DIAMONDS;
        }
    }

    /** One card: a rank from the ace at 1 to the king at 13, and a suit. */
    public record Card(int rank, Suit suit) {

        public Card {
            if (rank < ACE || rank > KING) {
                throw new IllegalArgumentException("rank must be in [1, 13], was " + rank);
            }
            if (suit == null) {
                throw new IllegalArgumentException("a card must have a suit");
            }
        }

        /** Whether this card is red, which decides what it may be laid on in the tableau. */
        public boolean red() {
            return suit.red();
        }
    }

    public enum State { PLAYING, WON }

    private final List<Card> stock = new ArrayList<>(DECK_SIZE);
    private final List<Card> waste = new ArrayList<>(DECK_SIZE);
    /* Keyed by the suit itself rather than by a number derived from it, so nothing depends on their order. */
    private final Map<Suit, List<Card>> foundations = new EnumMap<>(Suit.class);
    private final List<List<Card>> tableau = new ArrayList<>(TABLEAU_PILES);
    /** How many cards at the bottom of each tableau pile are still face down. */
    private final int[] faceDown = new int[TABLEAU_PILES];

    private final long seed;

    private int score;
    private int moves;
    private State state = State.PLAYING;

    /** Deals a game from {@code seed}; the same seed always deals the same game. */
    public SolitaireGame(final long seed) {
        this.seed = seed;
        for (final Suit suit : Suit.values()) {
            foundations.put(suit, new ArrayList<>(KING));
        }
        for (int i = 0; i < TABLEAU_PILES; i++) {
            tableau.add(new ArrayList<>(KING + TABLEAU_PILES));
        }
        deal(seed);
    }

    /** The number this game was dealt from, so a player can write it down and deal it again. */
    public long seed() {
        return seed;
    }

    public State state() {
        return state;
    }

    public boolean isWon() {
        return state == State.WON;
    }

    public int score() {
        return score;
    }

    public int moves() {
        return moves;
    }

    /** How many cards are still face down in the stock, waiting to be turned. */
    public int stockSize() {
        return stock.size();
    }

    /** How many cards have been turned into the waste and not yet played. */
    public int wasteSize() {
        return waste.size();
    }

    /** The card the waste is offering, or null when nothing has been turned over. */
    public Card wasteTop() {
        return waste.isEmpty() ? null : waste.get(waste.size() - 1);
    }

    /** The highest card sent home in {@code suit}, or null while that foundation is still empty. */
    public Card foundationTop(final Suit suit) {
        final List<Card> pile = foundations.get(suit);
        return pile.isEmpty() ? null : pile.get(pile.size() - 1);
    }

    /** How many cards are on a foundation, which is also the rank it has reached. */
    public int foundationSize(final Suit suit) {
        return foundations.get(suit).size();
    }

    /** How many cards a tableau pile holds, face down ones included. */
    public int tableauSize(final int pile) {
        return tableau.get(checkPile(pile)).size();
    }

    /** The card at {@code index} in a pile, counting from the bottom. */
    public Card tableauCard(final int pile, final int index) {
        return tableau.get(checkPile(pile)).get(index);
    }

    /** Whether the card at {@code index} in a pile is face up, which is what a player may act on. */
    public boolean tableauFaceUp(final int pile, final int index) {
        return index >= faceDown[checkPile(pile)];
    }

    /** How many cards at the bottom of a pile are still face down. */
    public int faceDownCount(final int pile) {
        return faceDown[checkPile(pile)];
    }

    /**
     * Turns the next card of the stock over into the waste.
     *
     * <p>With the stock empty this puts the waste back under it instead, which is the pass the scoring
     * charges for: going round again is not progress, and a player who only cycles should see that.
     */
    public boolean drawFromStock() {
        if (state != State.PLAYING) {
            return false;
        }
        if (stock.isEmpty()) {
            if (waste.isEmpty()) {
                return false;
            }
            for (int i = waste.size() - 1; i >= 0; i--) {
                stock.add(waste.get(i));
            }
            waste.clear();
            addScore(SCORE_RECYCLE);
            moves++;
            return true;
        }
        waste.add(stock.remove(stock.size() - 1));
        moves++;
        return true;
    }

    /** Sends the waste's card home, when the foundation of its suit is ready for it. */
    public boolean wasteToFoundation() {
        final Card card = wasteTop();
        if (card == null || !foundationAccepts(card)) {
            return false;
        }
        waste.remove(waste.size() - 1);
        foundations.get(card.suit()).add(card);
        addScore(SCORE_TO_FOUNDATION);
        finish();
        moves++;
        return true;
    }

    /** Lays the waste's card on a tableau pile, when that pile will take it. */
    public boolean wasteToTableau(final int pile) {
        final Card card = wasteTop();
        if (card == null || !tableauAccepts(pile, card)) {
            return false;
        }
        waste.remove(waste.size() - 1);
        tableau.get(pile).add(card);
        addScore(SCORE_WASTE_TO_TABLEAU);
        moves++;
        return true;
    }

    /** Sends the bottom card of a tableau pile home, when its foundation is ready for it. */
    public boolean tableauToFoundation(final int pile) {
        final List<Card> from = tableau.get(checkPile(pile));
        if (from.isEmpty()) {
            return false;
        }
        final Card card = from.get(from.size() - 1);
        if (!foundationAccepts(card)) {
            return false;
        }
        from.remove(from.size() - 1);
        foundations.get(card.suit()).add(card);
        addScore(SCORE_TO_FOUNDATION);
        turnOver(pile);
        finish();
        moves++;
        return true;
    }

    /**
     * Moves the last {@code count} cards of one tableau pile onto another.
     *
     * <p>Only a run already in order may travel, because that is what a player is holding when they drag one:
     * every card in it face up, each one lower than and a different colour from the one above it.
     */
    public boolean tableauToTableau(final int from, final int count, final int to) {
        checkPile(from);
        checkPile(to);
        if (from == to || count < 1) {
            return false;
        }
        final List<Card> source = tableau.get(from);
        if (count > source.size() - faceDown[from]) {
            return false;
        }
        final int start = source.size() - count;
        if (!isRun(source, start)) {
            return false;
        }
        if (!tableauAccepts(to, source.get(start))) {
            return false;
        }
        final List<Card> moving = new ArrayList<>(source.subList(start, source.size()));
        source.subList(start, source.size()).clear();
        tableau.get(to).addAll(moving);
        turnOver(from);
        moves++;
        return true;
    }

    /**
     * Takes the top card of a foundation back onto a tableau pile.
     *
     * <p>Klondike allows this and a player sometimes needs it, because a card sent home too early can be the
     * one a run was waiting for. It costs score, so it stays a decision rather than a free undo.
     */
    public boolean foundationToTableau(final Suit suit, final int pile) {
        final Card card = foundationTop(suit);
        if (card == null || !tableauAccepts(pile, card)) {
            return false;
        }
        final List<Card> home = foundations.get(suit);
        home.remove(home.size() - 1);
        tableau.get(pile).add(card);
        addScore(SCORE_FROM_FOUNDATION);
        state = State.PLAYING;
        moves++;
        return true;
    }

    /**
     * Whether every card is home, which is the only way this game ends.
     *
     * <p>There is no lost state. A Klondike deal that cannot be finished simply cannot be finished, and the
     * player deals another; nothing in the rules can tell them so at the moment it happens.
     */
    public boolean allHome() {
        for (final List<Card> pile : foundations.values()) {
            if (pile.size() < KING) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether every card is face up, which is when the rest of the game is a formality.
     *
     * <p>A player in that position is only clicking, so an app may offer to finish it for them.
     */
    public boolean canFinishAutomatically() {
        if (!stock.isEmpty() || !waste.isEmpty()) {
            return false;
        }
        for (int pile = 0; pile < TABLEAU_PILES; pile++) {
            if (faceDown[pile] > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Plays one card home from wherever it can be found, for the finish that follows from
     * {@link #canFinishAutomatically}. Answers false once nothing else will go.
     */
    public boolean playOneHome() {
        if (wasteToFoundation()) {
            return true;
        }
        for (int pile = 0; pile < TABLEAU_PILES; pile++) {
            if (tableauToFoundation(pile)) {
                return true;
            }
        }
        return false;
    }

    private void deal(final long dealSeed) {
        final List<Card> deck = new ArrayList<>(DECK_SIZE);
        for (final Suit suit : Suit.values()) {
            for (int rank = ACE; rank <= KING; rank++) {
                deck.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(deck, new Random(dealSeed));
        int next = 0;
        for (int pile = 0; pile < TABLEAU_PILES; pile++) {
            for (int card = 0; card <= pile; card++) {
                tableau.get(pile).add(deck.get(next++));
            }
            // Every pile shows its last card and hides the rest, which is how Klondike is dealt.
            faceDown[pile] = pile;
        }
        while (next < deck.size()) {
            stock.add(deck.get(next++));
        }
    }

    /** Turns a pile's top card over once the face-up cards above it have gone, and pays for it. */
    private void turnOver(final int pile) {
        final List<Card> cards = tableau.get(pile);
        if (faceDown[pile] > 0 && faceDown[pile] >= cards.size()) {
            faceDown[pile] = cards.size() - 1;
            addScore(SCORE_TURN_OVER);
        }
    }

    /** Whether a foundation is ready for this card: its ace first, then each rank in turn. */
    private boolean foundationAccepts(final Card card) {
        return foundations.get(card.suit()).size() == card.rank() - 1;
    }

    /** Whether a tableau pile takes this card: a king on nothing, else one lower and the other colour. */
    private boolean tableauAccepts(final int pile, final Card card) {
        if (state != State.PLAYING) {
            return false;
        }
        final List<Card> cards = tableau.get(checkPile(pile));
        if (cards.isEmpty()) {
            return card.rank() == KING;
        }
        final Card onto = cards.get(cards.size() - 1);
        return onto.rank() == card.rank() + 1 && onto.red() != card.red();
    }

    /** Whether the cards from {@code start} to the end of a pile are already a run a player could hold. */
    private static boolean isRun(final List<Card> cards, final int start) {
        for (int i = start; i < cards.size() - 1; i++) {
            final Card upper = cards.get(i);
            final Card lower = cards.get(i + 1);
            if (upper.rank() != lower.rank() + 1 || upper.red() == lower.red()) {
                return false;
            }
        }
        return true;
    }

    private void finish() {
        if (allHome()) {
            state = State.WON;
        }
    }

    /** Adds to the score without ever letting it go below nothing, which no Klondike scoreboard shows. */
    private void addScore(final int delta) {
        score = Math.max(0, score + delta);
    }

    private static int checkPile(final int pile) {
        if (pile < 0 || pile >= TABLEAU_PILES) {
            throw new IllegalArgumentException("tableau pile must be in [0, 7), was " + pile);
        }
        return pile;
    }
}
