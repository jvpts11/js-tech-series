/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.ServerBreakdownPayload;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The Network heading: the network's whole store as a grid, and beside it everything the machine knows
 * about the one thing that is picked out.
 *
 * <p>The panel is the same answer the prompt gives for a thing, which is why it is here: a player working
 * the network from the screen and a player working it from the prompt are told the same thing, in the
 * same words, and neither surface has to be learned twice.
 */
final class NetworkTerminalTab extends AbstractTerminalTab {

    private static final int GRID_ROWS = ComputerTerminalLayout.GRID_ROWS;
    private static final int TOOLBAR_Y = ComputerTerminalLayout.TOOLBAR_Y;
    private static final int SORT_X = ComputerTerminalLayout.SORT_X;
    private static final int SORT_W = ComputerTerminalLayout.SORT_W;
    private static final int MOD_X = ComputerTerminalLayout.MOD_X;
    private static final int MOD_W = ComputerTerminalLayout.MOD_W;
    private static final int PANE_X = ComputerTerminalLayout.PANE_X;
    private static final int PANE_Y = ComputerTerminalLayout.PANE_Y;
    private static final int PANE_W = ComputerTerminalLayout.PANE_W;
    private static final int DEPOSIT_X = ComputerTerminalLayout.DEPOSIT_X;
    private static final int DEPOSIT_Y = ComputerTerminalLayout.DEPOSIT_Y;
    private static final int DEPOSIT_W = ComputerTerminalLayout.DEPOSIT_W;

    /** How many servers the panel names before it says how many more there are. */
    private static final int PANE_SERVER_ROWS = 2;

    NetworkTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        gridBg(g, x, y, 0, GRID_ROWS);
        screen.paneBg(g, x, y);
        final NetworkItemEntry picked = screen.selectedEntry();
        if (picked != null) {
            paneButton(g, x + ComputerTerminalScreen.PANE_GET_X, y + ComputerTerminalScreen.PANE_BTN_Y, true);
            paneButton(g, x + ComputerTerminalScreen.PANE_CRAFT_X, y + ComputerTerminalScreen.PANE_BTN_Y,
                    screen.craftFor(picked) != null);
        }
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final int shown = visibleItems().size();
        final String kinds = GameText.resolve(
                (shown == 1 ? TerminalGridTexts.ONE_KIND : TerminalGridTexts.KINDS).with(shown));
        g.drawString(font(), kinds, cx + cw - font().width(kinds), TOOLBAR_Y + 3, DIM(), false);
        g.drawCenteredString(font(),
                GameText.resolve(screen.sortByQuantity ? TerminalGridTexts.QUANTITY : TerminalGridTexts.NAME),
                SORT_X + SORT_W / 2, TOOLBAR_Y + 3, ACCENT());
        final String mod = screen.modFilter();
        g.drawCenteredString(font(),
                mod.isEmpty() ? GameText.resolve(TerminalGridTexts.MOD) : font().plainSubstrByWidth(mod, MOD_W - 6),
                MOD_X + MOD_W / 2, TOOLBAR_Y + 3, mod.isEmpty() ? DIM() : ACCENT());
        final boolean holding = !menu.getCarried().isEmpty();
        g.drawCenteredString(font(), GameText.resolve(TerminalGridTexts.DEPOSIT_ALL),
                DEPOSIT_X + DEPOSIT_W / 2 + 4, DEPOSIT_Y + 2, holding ? ACCENT() : DIM());
        renderPane(g);
    }

    /** Everything the machine knows about what is picked out, or an invitation to pick something out. */
    private void renderPane(final GuiGraphics g) {
        final NetworkItemEntry picked = screen.selectedEntry();
        final int px = PANE_X + 6;
        final int right = PANE_X + PANE_W - 6;
        if (picked == null) {
            g.drawString(font(), GameText.resolve(TerminalGridTexts.NOTHING_PICKED), px, PANE_Y + 6, DIM(), false);
            int y = PANE_Y + 20;
            for (final FormattedCharSequence line
                    : font().split(GameText.component(TerminalGridTexts.PICK_HINT), PANE_W - 12)) {
                g.drawString(font(), line, px, y, DIM(), false);
                y += 10;
            }
            return;
        }
        g.drawString(font(), font().plainSubstrByWidth(picked.name().getString(), PANE_W - 12),
                px, PANE_Y + 6, ACCENT(), false);
        g.drawString(font(), font().plainSubstrByWidth(picked.key().id(), PANE_W - 12),
                px, PANE_Y + 18, DIM(), false);
        rule(g, px, PANE_Y + 29, right);

        g.drawString(font(), GameText.resolve(TerminalGridTexts.HELD_BY), px, PANE_Y + 35, TEXT(), false);
        final String total = fmt(picked.total());
        g.drawString(font(), total, right - font().width(total), PANE_Y + 35, TEXT(), false);
        final List<ServerBreakdownPayload.ServerHolding> held = menu.serverBreakdown();
        if (held.isEmpty()) {
            g.drawString(font(), GameText.resolve(TerminalGridTexts.ASKING), px + 6, PANE_Y + 47, DIM(), false);
        }
        for (int i = 0; i < PANE_SERVER_ROWS && i < held.size(); i++) {
            final ServerBreakdownPayload.ServerHolding row = held.get(i);
            final int ry = PANE_Y + 47 + i * 12;
            g.drawString(font(), font().plainSubstrByWidth(row.label(), PANE_W - 60), px + 6, ry, DIM(), false);
            final String count = fmt(row.count());
            g.drawString(font(), count, right - font().width(count), ry, TEXT(), false);
        }
        if (held.size() > PANE_SERVER_ROWS) {
            g.drawString(font(), GameText.resolve(AssemblyTexts.MORE.with(held.size() - PANE_SERVER_ROWS)),
                    px + 6, PANE_Y + 47 + PANE_SERVER_ROWS * 12, DIM(), false);
        }
        rule(g, px, PANE_Y + 81, right);

        g.drawString(font(), GameText.resolve(TerminalGridTexts.MADE_FROM), px, PANE_Y + 85, TEXT(), false);
        final CraftCatalogPayload.Entry pattern = screen.craftFor(picked);
        if (pattern == null) {
            g.drawString(font(), GameText.resolve(TerminalGridTexts.NO_PATTERN), px + 6, PANE_Y + 95, DIM(), false);
        } else {
            final int dot = switch (pattern.availability()) {
                case CraftCatalogPayload.DOT_GREEN -> GREEN();
                case CraftCatalogPayload.DOT_AMBER -> AMBER();
                default -> RED();
            };
            g.drawString(font(), GameText.resolve(TerminalGridTexts.HAS_PATTERN), px + 6, PANE_Y + 95, dot, false);
        }
        g.drawCenteredString(font(), GameText.resolve(TerminalGridTexts.GET),
                ComputerTerminalScreen.PANE_GET_X + ComputerTerminalScreen.PANE_BTN_W / 2,
                ComputerTerminalScreen.PANE_BTN_Y + 2, ACCENT());
        g.drawCenteredString(font(), GameText.resolve(TerminalGridTexts.CRAFT),
                ComputerTerminalScreen.PANE_CRAFT_X + ComputerTerminalScreen.PANE_BTN_W / 2,
                ComputerTerminalScreen.PANE_BTN_Y + 2, pattern == null ? DIM() : ACCENT());
    }

    private void rule(final GuiGraphics g, final int x0, final int y, final int x1) {
        g.fill(x0, y, x1, y + 1, LINE());
    }

    private void paneButton(final GuiGraphics g, final int bx, final int by, final boolean enabled) {
        g.fill(bx, by, bx + ComputerTerminalScreen.PANE_BTN_W, by + ComputerTerminalScreen.PANE_BTN_H,
                enabled ? TAB_ON() : TRACK());
        g.fill(bx, by, bx + ComputerTerminalScreen.PANE_BTN_W, by + 1, enabled ? ACCENT() : LINE());
    }
}
