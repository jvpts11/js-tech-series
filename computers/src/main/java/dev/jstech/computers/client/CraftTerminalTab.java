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
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * The Craft heading: every pattern the network can make, and what it is making right now.
 *
 * <p>The catalogue on the left, what is running and what has just finished on the right, because a player
 * asking for a craft wants to see the queue they are adding to without changing heading to find it.
 */
final class CraftTerminalTab extends AbstractTerminalTab {

    private static final int GRID_X = ComputerTerminalLayout.GRID_X;
    private static final int GRID_Y = ComputerTerminalLayout.GRID_Y;
    private static final int CRAFT_COLS = ComputerTerminalLayout.GRID_COLS;
    /** The catalogue keeps the grid's own rows, minus two so the panel beside it has a foot to stand on. */
    private static final int CRAFT_ROWS = ComputerTerminalLayout.GRID_ROWS - 2;

    private static final int PANE_X = ComputerTerminalLayout.PANE_X;
    private static final int PANE_Y = ComputerTerminalLayout.PANE_Y;
    private static final int PANE_W = ComputerTerminalLayout.PANE_W;

    /** Where the running and the finished start inside the panel. */
    private static final int RUNNING_Y = PANE_Y + 6;
    private static final int RECENT_Y = PANE_Y + 62;
    private static final int RUNNING_ROWS = 3;
    private static final int RECENT_ROWS = 4;

    CraftTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        final var catalog = menu.craftCatalog();
        final int maxScroll = Math.max(0, (catalog.size() + CRAFT_COLS - 1) / CRAFT_COLS - CRAFT_ROWS);
        screen.craftScroll = Math.max(0, Math.min(screen.craftScroll, maxScroll));
        for (int row = 0; row < CRAFT_ROWS; row++) {
            for (int col = 0; col < CRAFT_COLS; col++) {
                final int sx = x + GRID_X + col * 18;
                final int sy = y + GRID_Y + row * 18;
                slotBg(g, sx, sy);
                final int index = (row + screen.craftScroll) * CRAFT_COLS + col;
                if (index < catalog.size()) {
                    final var entry = catalog.get(index);
                    drawDataIcon(g, StorageKey.of(entry.result()), -1L, sx, sy);
                    final int dot = switch (entry.availability()) {
                        case CraftCatalogPayload.DOT_GREEN -> GREEN();
                        case CraftCatalogPayload.DOT_AMBER -> AMBER();
                        default -> RED();
                    };
                    g.fill(sx + 13, sy + 1, sx + 17, sy + 5, dot);
                }
            }
        }
        screen.paneBg(g, x, y);
        // RUNNING rows: a strip per craft with the bar that says how far it has got.
        final var running = runningCrafts();
        for (int i = 0; i < Math.min(RUNNING_ROWS, running.size()); i++) {
            final OperationRecord op = running.get(i);
            final int ry = y + RUNNING_Y + 12 + i * 14;
            g.fill(x + PANE_X + 4, ry, x + PANE_X + PANE_W - 4, ry + 12, TAB_ON());
            final int barX = x + PANE_X + 8;
            final int barW = PANE_W - 16;
            g.fill(barX, ry + 9, barX + barW, ry + 11, TRACK());
            final int pct = op.requested() <= 0 ? 0
                    : (int) Math.min(100, op.moved() * 100 / Math.max(1, op.requested()));
            g.fill(barX, ry + 9, barX + barW * pct / 100, ry + 11, ACCENT());
        }
        final var recent = recentCrafts();
        for (int i = 0; i < Math.min(RECENT_ROWS, recent.size()); i++) {
            final int ry = y + RECENT_Y + 12 + i * 11;
            g.fill(x + PANE_X + 4, ry, x + PANE_X + PANE_W - 4, ry + 10, PANEL());
        }
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final var catalog = menu.craftCatalog();
        g.drawString(font(), GameText.resolve(TerminalTexts.CRAFTABLE), GRID_X, cy + 20, DIM(), false);
        final String count = GameText.resolve((catalog.size() == 1 ? TerminalTexts.ONE_PATTERN : TerminalTexts.PATTERNS)
                .with(catalog.size()));
        g.drawString(font(), count, GRID_X + CRAFT_COLS * 18 - font().width(count), cy + 20, TEXT(), false);
        if (catalog.isEmpty()) {
            g.drawString(font(), GameText.resolve(TerminalTexts.NO_PATTERNS), GRID_X, GRID_Y + 6, DIM(), false);
            g.drawString(font(), GameText.resolve(TerminalTexts.LOAD_UNDER_PATTERNS), GRID_X, GRID_Y + 18, DIM(),
                    false);
        }
        final int totalRows = (catalog.size() + CRAFT_COLS - 1) / CRAFT_COLS;
        if (totalRows > CRAFT_ROWS) {
            final String at = (screen.craftScroll + 1) + "/" + (totalRows - CRAFT_ROWS + 1);
            g.drawString(font(), at, GRID_X, GRID_Y + CRAFT_ROWS * 18 + 4, DIM(), false);
        }

        final int px = PANE_X + 6;
        final int right = PANE_X + PANE_W - 6;
        g.drawString(font(), GameText.resolve(TerminalTexts.RUNNING), px, RUNNING_Y, DIM(), false);
        final var running = runningCrafts();
        if (running.isEmpty()) {
            g.drawString(font(), GameText.resolve(TerminalTexts.NOTHING_BEING_MADE), px, RUNNING_Y + 14, DIM(), false);
        }
        for (int i = 0; i < Math.min(RUNNING_ROWS, running.size()); i++) {
            final OperationRecord op = running.get(i);
            final int ry = RUNNING_Y + 14 + i * 14;
            g.drawString(font(), font().plainSubstrByWidth(op.name().getString(), PANE_W - 70), px + 2, ry,
                    TEXT(), false);
            final String made = fmt(op.moved()) + "/" + fmt(op.requested());
            g.drawString(font(), made, right - font().width(made) - 2, ry, DIM(), false);
        }

        g.drawString(font(), GameText.resolve(TerminalTexts.JUST_MADE), px, RECENT_Y, DIM(), false);
        final var recent = recentCrafts();
        if (recent.isEmpty()) {
            g.drawString(font(), GameText.resolve(TerminalTexts.NOTHING_YET), px, RECENT_Y + 14, DIM(), false);
        }
        for (int i = 0; i < Math.min(RECENT_ROWS, recent.size()); i++) {
            final OperationRecord op = recent.get(i);
            final int ry = RECENT_Y + 14 + i * 11;
            final String st = GameText.resolve(switch (op.status()) {
                case OperationRecord.STATUS_COMPLETED -> TerminalTexts.DONE;
                case OperationRecord.STATUS_PARTIAL -> TerminalTexts.PARTIAL;
                case OperationRecord.STATUS_RESOURCE_LOCKED -> TerminalTexts.LOCKED;
                case OperationRecord.STATUS_DISCARDED -> TerminalTexts.DROPPED;
                default -> TerminalTexts.FAILED;
            });
            final int color = switch (op.status()) {
                case OperationRecord.STATUS_COMPLETED -> GREEN();
                case OperationRecord.STATUS_PARTIAL -> AMBER();
                case OperationRecord.STATUS_DISCARDED -> DIM();
                default -> RED();
            };
            final String name = font().plainSubstrByWidth(
                    GameText.resolve(TerminalTexts.MADE.with(op.name().getString(), fmt(op.moved()))),
                    PANE_W - 26 - font().width(st));
            g.drawString(font(), name, px + 2, ry, TEXT(), false);
            g.drawString(font(), st, right - font().width(st) - 2, ry, color, false);
        }
    }

    private List<OperationRecord> runningCrafts() {
        final List<OperationRecord> out = new ArrayList<>();
        for (final OperationRecord op : menu.activeOps()) {
            if (op.type() == OperationRecord.TYPE_CRAFT) {
                out.add(op);
            }
        }
        return out;
    }

    private List<OperationRecord> recentCrafts() {
        final List<OperationRecord> out = new ArrayList<>();
        for (final OperationRecord op : menu.operationsLog()) {
            if (op.type() == OperationRecord.TYPE_CRAFT) {
                out.add(op);
            }
        }
        return out;
    }
}
