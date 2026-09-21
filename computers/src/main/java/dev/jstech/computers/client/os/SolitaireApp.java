/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.program.SolitaireGame;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Locale;

/**
 * Klondike solitaire as a desktop app.
 *
 * <p>The window chrome follows the installed skin and the felt does not, the way the real one kept its green
 * whatever the window around it looked like. Every rule lives in the pure {@link SolitaireGame}; this class
 * lays the table out, draws it, and turns dragging into moves.
 */
public final class SolitaireApp implements IDesktopApp {

    private static final int W = PlayingCards.WIDTH;
    private static final int H = PlayingCards.HEIGHT;
    private static final int GAP = 3;
    private static final int COLUMNS = SolitaireGame.TABLEAU_PILES;
    private static final int MARGIN = 4;
    private static final int TOOLBAR_H = 15;
    private static final int STATUS_H = 11;
    /** How much of a covered card shows: enough for its rank to be read, and no more. */
    private static final int STEP_UP = PlayingCards.MIN_READABLE_STRIP;
    /** Face-down cards show less, because there is nothing on them worth the room. */
    private static final int STEP_DOWN = 3;
    private static final int FELT = 0xFF1F6B3A;
    private static final int FELT_EDGE = 0xFF175430;
    private static final int STATUS_INK = 0xFFDCEFE1;
    /** How long two clicks may be apart and still be one gesture. */
    private static final long DOUBLE_CLICK_MS = 260L;

    /** Where a card was picked up from, which is the only thing a drop needs to know about it. */
    private enum Source { NONE, WASTE, FOUNDATION, TABLEAU }

    private OsSkin skin = OsSkin.fallback();
    private SolitaireGame game = new SolitaireGame(System.nanoTime());

    private final Panel root = new Panel();
    private final Button newGame;
    private final Button finish;

    /**
     * Whether the table is playing itself home.
     *
     * <p>Offered only once every card is face up, because from there the game is decided and what is left
     * is forty clicks in the one order they can be made. One card a frame rather than all at once, so it
     * is something to watch rather than a table that empties between two frames.
     */
    private boolean finishing;

    /* The table as it was last drawn, so a click reads the same numbers the drawing did. */
    private int tableX;
    private int tableY;
    private int tableW;
    private int stepUp = STEP_UP;
    private int stepDown = STEP_DOWN;

    /**
     * The foundations, left to right, as one list that both the drawing and the clicking read.
     *
     * <p>Held as a list rather than reached for by a number worked out from a suit: the order they sit in
     * on the table is this list's business and nothing else's.
     */
    private static final List<SolitaireGame.Suit> SUITS = List.of(SolitaireGame.Suit.values());

    /* What is being carried, if anything. */
    private Source dragSource = Source.NONE;
    /** Which tableau pile is being carried from; meaningless for the other sources. */
    private int dragPile;
    /** Which foundation is being carried from, or null when it is not a foundation. */
    private SolitaireGame.Suit dragSuit;
    private int dragCount;
    private int dragX;
    private int dragY;
    private int grabOffsetX;
    private int grabOffsetY;

    private long lastClickMs;
    private int lastClickPile = -1;
    private Source lastClickSource = Source.NONE;

    private long startedMs = System.currentTimeMillis();

    public SolitaireApp() {
        newGame = root.add(new Button("New", this::deal));
        finish = root.add(new Button("Finish", () -> this.finishing = true));
    }

    private void deal() {
        this.game = new SolitaireGame(System.nanoTime());
        this.dragSource = Source.NONE;
        this.finishing = false;
        this.startedMs = System.currentTimeMillis();
    }

    @Override
    public String title() {
        return "Solitaire";
    }

    @Override
    public int defaultWidth() {
        return COLUMNS * (W + GAP) - GAP + MARGIN * 2;
    }

    /**
     * Tall enough for a king down to an ace without the run being squeezed out of readability.
     *
     * <p>Thirteen cards is the longest run the game can build, and each of the twelve under the last one
     * has to show enough of itself to be read. A shorter window makes the step shrink instead, which is
     * what {@link #measureStack} is for, but it should not be the ordinary case.
     */
    @Override
    public int defaultHeight() {
        return TOOLBAR_H + H + 6 + H + STEP_UP * 12 + STATUS_H + MARGIN;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return TOOLBAR_H + H + 6 + H + STEP_UP * 4 + STATUS_H + MARGIN;
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
        newGame.setBounds(x + MARGIN, y + 2, 34, 12);
        final int finishW = font.width("Finish") + 8;
        finish.setBounds(x + MARGIN + 36, y + 2, finishW, 12);
        playItselfHome();
        finish.setVisible(!finishing && !game.isWon() && game.canFinishAutomatically());
        root.render(g, ctx);
        drawSeed(g, font, x, y, width);

        final int feltTop = y + TOOLBAR_H;
        final int feltBottom = y + height - STATUS_H;
        g.fill(x, feltTop, x + width, feltBottom, FELT);
        g.fill(x, feltTop, x + width, feltTop + 1, FELT_EDGE);

        this.tableW = COLUMNS * (W + GAP) - GAP;
        this.tableX = x + Math.max(MARGIN, (width - tableW) / 2);
        this.tableY = feltTop + 4;
        measureStack(feltBottom);

        drawStockAndWaste(g, font);
        drawFoundations(g, font);
        drawTableau(g, font, feltBottom);
        drawStatus(g, font, x, feltBottom, width, height);
        drawCarried(g, font);
    }

    /** Sends one card home a frame while the table is finishing itself, and stops when none will go. */
    private void playItselfHome() {
        if (finishing && !game.playOneHome()) {
            this.finishing = false;
        }
    }

    /** Picks a stacking step that keeps the longest pile inside the felt, however tall the window is. */
    private void measureStack(final int feltBottom) {
        int longest = 0;
        for (int pile = 0; pile < COLUMNS; pile++) {
            longest = Math.max(longest, game.tableauSize(pile));
        }
        final int room = feltBottom - (tableY + H + 6) - H - 2;
        if (longest <= 1) {
            this.stepUp = STEP_UP;
            this.stepDown = STEP_DOWN;
            return;
        }
        /*
         * The cards have to fit whatever the deal did, so the step shrinks rather than the pile running off
         * the felt. It never grows past the size a rank and a suit need to stay readable.
         */
        final int wanted = Math.max(2, room / (longest - 1));
        this.stepUp = Math.min(STEP_UP, wanted);
        this.stepDown = Math.min(STEP_DOWN, Math.max(2, stepUp - 2));
    }

    private void drawSeed(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        final String seed = "#" + Long.toString(Math.abs(game.seed()) % 1_000_000L);
        g.drawString(font, seed, x + width - MARGIN - font.width(seed), y + 4, skin.dim(), false);
    }

    private void drawStockAndWaste(final GuiGraphics g, final Font font) {
        final int sx = columnX(0);
        if (game.stockSize() > 0) {
            PlayingCards.back(g, sx, tableY, H);
            final String left = String.valueOf(game.stockSize());
            g.drawString(font, left, sx + (W - font.width(left)) / 2, tableY + H - 10, 0xFFC9D6F2, false);
        } else {
            PlayingCards.emptyStock(g, sx, tableY);
        }
        final int wx = columnX(1);
        final SolitaireGame.Card top = wasteTopVisible();
        if (top == null) {
            PlayingCards.empty(g, wx, tableY);
        } else {
            PlayingCards.face(g, font, wx, tableY, top);
        }
    }

    private void drawFoundations(final GuiGraphics g, final Font font) {
        for (int i = 0; i < SUITS.size(); i++) {
            final SolitaireGame.Suit suit = SUITS.get(i);
            final int fx = columnX(firstFoundationColumn() + i);
            final SolitaireGame.Card top = foundationTopVisible(suit);
            if (top == null) {
                PlayingCards.emptyFoundation(g, fx, tableY, suit);
            } else {
                PlayingCards.face(g, font, fx, tableY, top);
            }
        }
    }

    /** The column the foundations start in, which is as far right as they fit. */
    private static int firstFoundationColumn() {
        return COLUMNS - SUITS.size();
    }

    private void drawTableau(final GuiGraphics g, final Font font, final int feltBottom) {
        final int top = tableY + H + 6;
        for (int pile = 0; pile < COLUMNS; pile++) {
            final int px = columnX(pile);
            final int size = visibleTableauSize(pile);
            if (size == 0) {
                PlayingCards.empty(g, px, top);
                continue;
            }
            int cy = top;
            for (int i = 0; i < size; i++) {
                final boolean up = game.tableauFaceUp(pile, i);
                final int step = up ? stepUp : stepDown;
                final boolean last = i == size - 1;
                final int visible = last ? H : step;
                if (cy + visible > feltBottom) {
                    break;
                }
                if (!up) {
                    PlayingCards.back(g, px, cy, visible);
                } else if (last) {
                    PlayingCards.face(g, font, px, cy, game.tableauCard(pile, i));
                } else {
                    PlayingCards.faceStrip(g, font, px, cy, visible, game.tableauCard(pile, i));
                }
                cy += step;
            }
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int feltBottom,
                            final int width, final int height) {
        g.fill(x, feltBottom, x + width, feltBottom + STATUS_H, skin.windowBg());
        final int ty = feltBottom + 2;
        final String left = "Score " + game.score() + "   Moves " + game.moves();
        g.drawString(font, left, x + MARGIN, ty, skin.text(), false);
        final String right = game.isWon() ? "You win" : clock();
        g.drawString(font, right, x + width - MARGIN - font.width(right), ty,
                game.isWon() ? 0xFF1C7A32 : skin.dim(), false);
    }

    /** The cards being carried, drawn last so they sit over everything they are passing across. */
    private void drawCarried(final GuiGraphics g, final Font font) {
        if (dragSource == Source.NONE) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F);
        int cy = dragY;
        for (int i = 0; i < dragCount; i++) {
            final SolitaireGame.Card card = carriedCard(i);
            if (card == null) {
                break;
            }
            PlayingCards.face(g, font, dragX, cy, card);
            cy += stepUp;
        }
        g.pose().popPose();
    }

    /** The nth card of what is being carried, counting from the one the player grabbed. */
    private SolitaireGame.Card carriedCard(final int offset) {
        return switch (dragSource) {
            case WASTE -> offset == 0 ? game.wasteTop() : null;
            case FOUNDATION -> offset == 0 && dragSuit != null ? game.foundationTop(dragSuit) : null;
            case TABLEAU -> {
                final int index = game.tableauSize(dragPile) - dragCount + offset;
                yield index >= 0 && index < game.tableauSize(dragPile)
                        ? game.tableauCard(dragPile, index) : null;
            }
            case NONE -> null;
        };
    }

    /*
     * A card being carried is off its pile as far as the eye is concerned, so the pile it came from is drawn
     * without it. The rules still hold it, because a drop that is refused has to put it back.
     */

    private SolitaireGame.Card wasteTopVisible() {
        return dragSource == Source.WASTE ? null : game.wasteTop();
    }

    private SolitaireGame.Card foundationTopVisible(final SolitaireGame.Suit suit) {
        if (dragSource == Source.FOUNDATION && dragSuit == suit) {
            final int size = game.foundationSize(suit);
            return size > 1 ? new SolitaireGame.Card(size - 1, suit) : null;
        }
        return game.foundationTop(suit);
    }

    private int visibleTableauSize(final int pile) {
        final int size = game.tableauSize(pile);
        return dragSource == Source.TABLEAU && dragPile == pile ? size - dragCount : size;
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (button != 0 || game.isWon()) {
            return;
        }
        final int mx = (int) mouseX;
        final int my = (int) mouseY;
        final int column = columnAt(mx);
        if (column < 0) {
            return;
        }
        final boolean onTopRow = my >= tableY && my < tableY + H;
        if (onTopRow && column == 0) {
            game.drawFromStock();
            return;
        }
        if (onTopRow && column == 1) {
            grabWaste(mx, my);
            return;
        }
        if (onTopRow && column >= firstFoundationColumn()) {
            grabFoundation(column, mx, my);
            return;
        }
        grabTableau(column, mx, my);
    }

    private void grabWaste(final int mx, final int my) {
        if (game.wasteTop() == null) {
            return;
        }
        if (isDoubleClick(Source.WASTE, 1)) {
            game.wasteToFoundation();
            return;
        }
        startDrag(Source.WASTE, 1, 1, columnX(1), tableY, mx, my);
    }

    private void grabFoundation(final int column, final int mx, final int my) {
        final int index = column - firstFoundationColumn();
        if (index < 0 || index >= SUITS.size()) {
            return;
        }
        final SolitaireGame.Suit suit = SUITS.get(index);
        if (game.foundationTop(suit) == null) {
            return;
        }
        this.dragSuit = suit;
        startDrag(Source.FOUNDATION, index, 1, columnX(column), tableY, mx, my);
    }

    private void grabTableau(final int column, final int mx, final int my) {
        final int index = tableauIndexAt(column, my);
        if (index < 0 || !game.tableauFaceUp(column, index)) {
            return;
        }
        final int count = game.tableauSize(column) - index;
        if (isDoubleClick(Source.TABLEAU, column) && count == 1) {
            game.tableauToFoundation(column);
            return;
        }
        startDrag(Source.TABLEAU, column, count, columnX(column), tableauCardY(column, index), mx, my);
    }

    private void startDrag(final Source source, final int pile, final int count,
                           final int cardX, final int cardY, final int mx, final int my) {
        this.dragSource = source;
        this.dragPile = pile;
        this.dragCount = count;
        this.grabOffsetX = mx - cardX;
        this.grabOffsetY = my - cardY;
        this.dragX = cardX;
        this.dragY = cardY;
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (dragSource == Source.NONE) {
            return;
        }
        this.dragX = (int) mouseX - grabOffsetX;
        this.dragY = (int) mouseY - grabOffsetY;
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        root.mouseReleased(mouseX, mouseY, button);
        if (dragSource == Source.NONE) {
            return;
        }
        /*
         * The card is dropped by where its own top left corner ended up, not by where the pointer is: a run
         * grabbed by its lower half would otherwise land a column away from where it looks like it is.
         */
        drop(dragX + W / 2, dragY + H / 2);
        this.dragSource = Source.NONE;
        this.dragCount = 0;
        this.dragSuit = null;
    }

    private void drop(final int cx, final int cy) {
        final int column = columnAt(cx);
        if (column < 0) {
            return;
        }
        final boolean onTopRow = cy < tableY + H + 3;
        if (onTopRow && column >= firstFoundationColumn()) {
            dropOnFoundation();
            return;
        }
        if (onTopRow) {
            return;
        }
        switch (dragSource) {
            case WASTE -> game.wasteToTableau(column);
            case FOUNDATION -> {
                if (dragSuit != null) {
                    game.foundationToTableau(dragSuit, column);
                }
            }
            case TABLEAU -> game.tableauToTableau(dragPile, dragCount, column);
            case NONE -> { }
        }
    }

    private void dropOnFoundation() {
        switch (dragSource) {
            case WASTE -> game.wasteToFoundation();
            // Only the single bottom card of a run can go home, which is what a one-card drag is.
            case TABLEAU -> {
                if (dragCount == 1) {
                    game.tableauToFoundation(dragPile);
                }
            }
            case FOUNDATION, NONE -> { }
        }
    }

    /** Whether this press follows one on the same place closely enough to be a double click. */
    private boolean isDoubleClick(final Source source, final int pile) {
        final long now = System.currentTimeMillis();
        final boolean same = source == lastClickSource && pile == lastClickPile
                && now - lastClickMs <= DOUBLE_CLICK_MS;
        this.lastClickMs = now;
        this.lastClickSource = source;
        this.lastClickPile = pile;
        return same;
    }

    /** Which column a pixel is in, or -1 when it is in a gap or off the table. */
    private int columnAt(final int mx) {
        final int rel = mx - tableX;
        if (rel < 0 || rel >= tableW) {
            return -1;
        }
        final int column = rel / (W + GAP);
        return rel - column * (W + GAP) < W ? column : -1;
    }

    /** Which card of a pile a pixel is on, counting from the bottom, or -1 for none. */
    private int tableauIndexAt(final int pile, final int my) {
        final int size = game.tableauSize(pile);
        if (size == 0) {
            return -1;
        }
        int cy = tableY + H + 6;
        int found = -1;
        for (int i = 0; i < size; i++) {
            final int step = game.tableauFaceUp(pile, i) ? stepUp : stepDown;
            final int visible = i == size - 1 ? H : step;
            if (my >= cy && my < cy + visible) {
                found = i;
            }
            cy += step;
        }
        return found;
    }

    private int tableauCardY(final int pile, final int index) {
        int cy = tableY + H + 6;
        for (int i = 0; i < index; i++) {
            cy += game.tableauFaceUp(pile, i) ? stepUp : stepDown;
        }
        return cy;
    }

    private int columnX(final int column) {
        return tableX + column * (W + GAP);
    }

    private String clock() {
        final long seconds = (System.currentTimeMillis() - startedMs) / 1000L;
        return String.format(Locale.ROOT, "%d:%02d", Math.min(99, seconds / 60), seconds % 60);
    }
}
