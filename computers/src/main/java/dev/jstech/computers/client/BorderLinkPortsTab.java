/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.client.os.BorderLinkTexts;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.wan.BorderLinkRequestPayload;
import dev.jstech.computers.operation.payload.wan.BorderLinkStatePayload;
import dev.jstech.computers.wan.PortSettings;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * BorderLink's Ports heading on MC-NET, the first of its operating space (computers/docs/design/network.md): the
 * gateway's four ports with the line in each, the far end, the mode and the state; under them, in a panel, the port
 * picked, what the far side may do here, whether the port relays for other networks and whether it is on. A switch
 * changes on the gateway the moment it is ticked, as everything on this system does; the prompt's
 * {@code borderlink} command does the same from the Console.
 */
final class BorderLinkPortsTab extends AbstractTerminalTab {

    /* Which port is picked, 1 to 4. */
    private int selected = 1;
    /* Where the content stood when last drawn, which a click is read against. */
    private int lastCx;
    private int lastCy;
    private int lastCw;

    private static final int PORTS = 4;
    private static final float SMALL = 0.75f;
    /* The heading line, the list's head row and its rows. */
    private static final int HEADING_Y = 19;
    private static final int HEAD_Y = 29;
    private static final int HEAD_H = 11;
    private static final int ROWS_Y = 43;
    private static final int ROW = 11;
    private static final int[] COLUMNS = {4, 30, 125, 210, 280};
    /* The panel of the port picked, and the hint under it. */
    private static final int PANEL_Y = 93;
    private static final int PANEL_H = 109;
    private static final int NAME_Y = PANEL_Y + 5;
    private static final int INFO_Y = PANEL_Y + 16;
    private static final int RIGHTS_Y = PANEL_Y + 29;
    private static final int CHECKS_Y = PANEL_Y + 41;
    private static final int CHECK_ROW = 12;
    private static final int SWITCHES_Y = PANEL_Y + 71;
    private static final int CHECK_X = 5;
    private static final int CHECK_COLUMN = 150;
    private static final int BOX = 8;
    private static final int HINT_Y = PANEL_Y + PANEL_H + 8;

    BorderLinkPortsTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y, final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        lastCx = cx;
        lastCy = cy;
        lastCw = cw;
        g.fill(cx, cy + HEAD_Y, cx + cw, cy + HEAD_Y + HEAD_H, PANEL());
        g.fill(cx, cy + HEAD_Y, cx + cw, cy + HEAD_Y + 1, LINE());
        final int rowY = cy + ROWS_Y + (selected - 1) * ROW;
        g.fill(cx, rowY - 2, cx + cw, rowY + ROW - 2, TAB_ON());
        g.fill(cx, rowY - 2, cx + 2, rowY + ROW - 2, ACCENT());
        g.fill(cx, cy + PANEL_Y, cx + cw, cy + PANEL_Y + PANEL_H, PANEL());
        g.fill(cx, cy + PANEL_Y, cx + cw, cy + PANEL_Y + 1, LINE());
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final BorderLinkStatePayload state = menu.borderLink();
        Draw.text(g, font(), GameText.resolve(BorderLinkTexts.PORTS), cx, cy + HEADING_Y, DIM());
        final TextKey[] headings = {BorderLinkTexts.COLUMN_PORT_WORD, BorderLinkTexts.COLUMN_LINE,
                BorderLinkTexts.COLUMN_FAR, BorderLinkTexts.COLUMN_MODE, BorderLinkTexts.COLUMN_STATE};
        for (int c = 0; c < headings.length; c++) {
            small(g, GameText.resolve(headings[c]), cx + COLUMNS[c], cy + HEAD_Y + 3);
        }
        if (state == null || state.ports().size() != PORTS) {
            Draw.text(g, font(), GameText.resolve(BorderLinkTexts.ASKING), cx + COLUMNS[0], cy + ROWS_Y, DIM());
            return;
        }
        final String slots = GameText.resolve(state.cards() == 1 ? BorderLinkTexts.SLOTS_ONE_CARD.with(PORTS)
                : BorderLinkTexts.SLOTS_CARDS.with(PORTS, state.cards()));
        Draw.text(g, font(), slots, cx + cw - font().width(slots), cy + HEADING_Y, DIM());
        for (int i = 0; i < PORTS; i++) {
            row(g, cx, cy + ROWS_Y + i * ROW, i + 1, state.ports().get(i));
        }
        detail(g, cx, cy, state.ports().get(selected - 1));
    }

    @Override
    public boolean onMouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button != 0) {
            return false;
        }
        for (int i = 0; i < PORTS; i++) {
            if (inRect(mouseX, mouseY, lastCx, lastCy + ROWS_Y + i * ROW - 2, lastCw, ROW)) {
                selected = i + 1;
                return true;
            }
        }
        final BorderLinkStatePayload state = menu.borderLink();
        if (state == null || state.ports().size() != PORTS || !state.ports().get(selected - 1).hasCard()) {
            return false;
        }
        final PortSettings now = state.ports().get(selected - 1).settings();
        // The four rights in two rows of two, in the order they are declared.
        int k = 0;
        for (final WanRight right : WanRight.values()) {
            if (onCheck(mouseX, mouseY, CHECK_X + k % 2 * CHECK_COLUMN, CHECKS_Y + k / 2 * CHECK_ROW)) {
                set(now.with(right, !now.allows(right)));
                return true;
            }
            k++;
        }
        if (onCheck(mouseX, mouseY, CHECK_X, SWITCHES_Y)) {
            set(now.withRelay(!now.relay()));
            return true;
        }
        if (onCheck(mouseX, mouseY, CHECK_X + CHECK_COLUMN, SWITCHES_Y)) {
            set(now.withOn(!now.on()));
            return true;
        }
        return false;
    }

    /* One port's row: its number, line, far end, mode and state. */
    private void row(final GuiGraphics g, final int cx, final int y, final int port,
                     final BorderLinkStatePayload.Port shown) {
        Draw.text(g, font(), String.valueOf(port), cx + COLUMNS[0] + 4, y, TEXT());
        if (!shown.hasCard()) {
            Draw.text(g, font(), GameText.resolve(BorderLinkTexts.NO_CARD), cx + COLUMNS[1], y, DIM());
            return;
        }
        cell(g, BorderLinkTexts.lineName(shown), cx, 1, y);
        cell(g, shown.far(), cx, 2, y);
        cell(g, BorderLinkTexts.modeName(shown), cx, 3, y);
        Draw.text(g, font(), GameText.resolve(shown.up() ? BorderLinkTexts.UP : BorderLinkTexts.DOWN),
                cx + COLUMNS[4], y, shown.up() ? GREEN() : RED());
    }

    /* The panel of the port picked; its switches vanish for a port with no card, which has none to set. */
    private void detail(final GuiGraphics g, final int cx, final int cy, final BorderLinkStatePayload.Port shown) {
        final String heading = GameText.resolve(BorderLinkTexts.PORT_HEADING.with(selected));
        Draw.text(g, font(), heading, cx + 8, cy + NAME_Y, DIM());
        final int nameX = cx + 8 + font().width(heading) + 7;
        if (!shown.hasCard()) {
            Draw.text(g, font(), GameText.resolve(BorderLinkTexts.NO_CARD), nameX, cy + NAME_Y, DIM());
            small(g, GameText.resolve(BorderLinkTexts.PUT_A_CARD), cx + 8, cy + INFO_Y);
            return;
        }
        final boolean far = !shown.far().isEmpty();
        Draw.text(g, font(), far ? shown.far() : GameText.resolve(BorderLinkTexts.NOTHING_THERE), nameX,
                cy + NAME_Y, far ? TEXT() : DIM());
        small(g, GameText.resolve(BorderLinkTexts.INFO.with(BorderLinkTexts.lineName(shown), shown.speed(),
                BorderLinkTexts.where(shown))), cx + 8, cy + INFO_Y);
        final String whose = GameText.resolve(BorderLinkTexts.WHAT_MAY_HEADING.with(far
                ? shown.far().toUpperCase(Locale.ROOT) : GameText.resolve(BorderLinkTexts.THE_FAR_SIDE_HEADING)));
        Draw.text(g, font(), whose, cx + 8, cy + RIGHTS_Y, DIM());
        final PortSettings settings = shown.settings();
        int k = 0;
        for (final WanRight right : WanRight.values()) {
            check(g, cx + CHECK_X + k % 2 * CHECK_COLUMN, cy + CHECKS_Y + k / 2 * CHECK_ROW, settings.allows(right),
                    BorderLinkTexts.rightLabel(right));
            k++;
        }
        check(g, cx + CHECK_X, cy + SWITCHES_Y, settings.relay(), BorderLinkTexts.RELAY);
        check(g, cx + CHECK_X + CHECK_COLUMN, cy + SWITCHES_Y, settings.on(), BorderLinkTexts.PORT_ON);
        // What the prompt would type to flip the relay of this port, which is the command's own syntax.
        Draw.text(g, font(), GameText.resolve(BorderLinkTexts.HINT.with(selected,
                Text.literal(settings.relay() ? "relay off" : "relay on"))), cx, cy + HINT_Y, DIM(), SCREEN_COL(),
                SMALL);
    }

    /* Words at three quarters of the font, dim on a panel: the list's head row and the panel's small print. */
    private void small(final GuiGraphics g, final String words, final int x, final int y) {
        Draw.text(g, font(), words, x, y, DIM(), PANEL(), SMALL);
    }

    /* A switch: a dark box lit in the accent when on, and its word, bright when on and dim when off. */
    private void check(final GuiGraphics g, final int x, final int y, final boolean on, final TextKey label) {
        g.fill(x, y, x + BOX, y + BOX, LINE());
        g.fill(x + 1, y + 1, x + BOX - 1, y + BOX - 1, SLOT_BG());
        if (on) {
            g.fill(x + 2, y + 2, x + BOX - 2, y + BOX - 2, ACCENT());
        }
        Draw.text(g, font(), GameText.resolve(label), x + BOX + 4, y, on ? TEXT() : DIM());
    }

    /* Whether a click lands on the switch at {@code (x, y)} of the content, box or word. */
    private boolean onCheck(final double mouseX, final double mouseY, final int x, final int y) {
        return inRect(mouseX, mouseY, lastCx + x, lastCy + y - 1, CHECK_COLUMN - 10, BOX + 2);
    }

    /* A cell of the list, cut to its column. */
    private void cell(final GuiGraphics g, final String words, final int cx, final int column, final int y) {
        final int width = COLUMNS[column + 1] - COLUMNS[column] - 5;
        final String fits = font().width(words) <= width ? words
                : font().plainSubstrByWidth(words, Math.max(0, width - 6)) + "..";
        Draw.text(g, font(), fits, cx + COLUMNS[column], y, TEXT());
    }

    /* Sets the port picked's switches on the gateway, which answers with how its lines stand. */
    private void set(final PortSettings settings) {
        PacketDistributor.sendToServer(new BorderLinkRequestPayload(menu.hostPos(), selected, settings.bits()));
    }
}
