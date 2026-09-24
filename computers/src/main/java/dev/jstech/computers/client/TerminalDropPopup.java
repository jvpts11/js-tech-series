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
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.NetworkServersPayload;
import dev.jstech.computers.operation.payload.TerminalDropPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * DROP DATA: the one thing this interface does that cannot be undone, so it asks first.
 *
 * <p>Three things can be dropped and they are three different sizes of mistake: everything the network
 * holds, everything one server holds, or the types picked out by hand. The popup is modal on purpose,
 * because a click that lands behind a question like this one is a click nobody meant.
 */
final class TerminalDropPopup {

    private static final int DROP_W = 206;
    private static final int DROP_H = 146;
    private static final int DROP_GRID_COLS = 9;
    private static final int DROP_GRID_ROWS = 3;
    /* The three scopes, in the order of TerminalDropPayload's SCOPE_* numbers. */
    private static final TextKey[] SCOPES =
            {TerminalUpkeepTexts.SCOPE_NETWORK, TerminalUpkeepTexts.SCOPE_SERVER, TerminalUpkeepTexts.SCOPE_TYPES};

    private final ComputerTerminalScreen screen;
    private final ComputerTerminalMenu menu;

    private boolean open;
    private int scope = TerminalDropPayload.SCOPE_NETWORK;
    private final Set<StorageKey> types = new LinkedHashSet<>();
    private int serverIndex;
    private int typeScrollRow;

    TerminalDropPopup(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        this.screen = screen;
        this.menu = menu;
    }

    boolean isOpen() {
        return this.open;
    }

    void open() {
        this.open = true;
        this.scope = TerminalDropPayload.SCOPE_NETWORK;
        this.types.clear();
        this.serverIndex = 0;
        this.typeScrollRow = 0;
    }

    void close() {
        this.open = false;
        this.types.clear();
    }

    /** The wheel walks the grid of types, which is the one thing here that can run past its box. */
    boolean scrolled(final double dy) {
        if (!this.open || dy == 0) {
            return false;
        }
        if (this.scope == TerminalDropPayload.SCOPE_TYPES) {
            this.typeScrollRow = Math.max(0, this.typeScrollRow - (int) Math.signum(dy));
        }
        return true;
    }

    void render(final GuiGraphics g, final int mouseX, final int mouseY) {
        if (!this.open) {
            return;
        }
        final int left = screen.left();
        final int top = screen.top();
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        g.fill(left, top, left + ComputerTerminalLayout.WIDTH, top + ComputerTerminalLayout.HEIGHT, 0xE0070A0F);
        final int px = x();
        final int py = y();
        g.fill(px - 1, py - 1, px + DROP_W + 1, py + DROP_H + 1, JsTechTheme.red());
        g.fill(px, py, px + DROP_W, py + DROP_H, 0xFF0F151C);

        g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.DROP_DATA), px + 8, py + 6,
                JsTechTheme.red(), false);
        g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.IRREVERSIBLE), px + 8, py + 17,
                JsTechTheme.dim(), false);

        // Scope selector: NETWORK | SERVER | TYPES.
        final int segW = (DROP_W - 16) / 3;
        for (int i = 0; i < 3; i++) {
            final int bx = px + 8 + i * segW;
            final boolean on = this.scope == i;
            final boolean hov = inRect(mouseX, mouseY, bx, py + 30, segW - 2, 14);
            g.fill(bx, py + 30, bx + segW - 2, py + 44, on ? JsTechTheme.red() : (hov ? 0xFF24323C : 0xFF1A222B));
            g.drawCenteredString(screen.tabFont(), GameText.resolve(SCOPES[i]), bx + (segW - 2) / 2, py + 33,
                    on ? 0xFFFFFFFF : JsTechTheme.dim());
        }

        final int bodyY = py + 50;
        switch (this.scope) {
            case TerminalDropPayload.SCOPE_SERVER -> renderServer(g, mouseX, mouseY, px, bodyY);
            case TerminalDropPayload.SCOPE_TYPES -> renderTypes(g, px, bodyY);
            default -> { // SCOPE_NETWORK
                int y = bodyY;
                for (final FormattedCharSequence line : screen.tabFont().split(
                        GameText.component(TerminalUpkeepTexts.DESTROYS_ALL), DROP_W - 16)) {
                    g.drawString(screen.tabFont(), line, px + 8, y, JsTechTheme.text(), false);
                    y += 11;
                }
                g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.TYPES_OVER.with(
                                ComputerTerminalScreen.fmt(menu.indexedTypes()), menu.indexedServers())),
                        px + 8, y + 6, JsTechTheme.amber(), false);
            }
        }

        // CANCEL | CONFIRM DROP.
        final int by = py + DROP_H - 22;
        final boolean cancelHov = inRect(mouseX, mouseY, px + 8, by, 70, 16);
        g.fill(px + 8, by, px + 78, by + 16, cancelHov ? 0xFF2A3340 : 0xFF1A222B);
        g.drawCenteredString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.CANCEL), px + 43, by + 4,
                JsTechTheme.text());
        final boolean canConfirm = confirmEnabled();
        final boolean confHov = inRect(mouseX, mouseY, px + 84, by, DROP_W - 92, 16);
        g.fill(px + 84, by, px + DROP_W - 8, by + 16,
                !canConfirm ? 0xFF3A2420 : (confHov ? 0xFFB23228 : 0xFF8A241C));
        g.drawCenteredString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.CONFIRM_DROP),
                px + 84 + (DROP_W - 92) / 2, by + 4, canConfirm ? 0xFFFFFFFF : JsTechTheme.dim());
        g.pose().popPose();
    }

    /** Modal: every click is answered here while the question is up, so none of them land behind it. */
    boolean clicked(final double mouseX, final double mouseY, final int button) {
        if (button == 1) {
            close();
            return true;
        }
        if (button != 0) {
            return true;
        }
        final int px = x();
        final int py = y();
        final int segW = (DROP_W - 16) / 3;
        for (int i = 0; i < 3; i++) {
            if (inRect(mouseX, mouseY, px + 8 + i * segW, py + 30, segW - 2, 14)) {
                this.scope = i;
                return true;
            }
        }
        final int bodyY = py + 50;
        if (this.scope == TerminalDropPayload.SCOPE_SERVER && !menu.networkServers().isEmpty()) {
            if (inRect(mouseX, mouseY, px + 8, bodyY, 14, 14)) {
                this.serverIndex--;
                return true;
            }
            if (inRect(mouseX, mouseY, px + DROP_W - 22, bodyY, 14, 14)) {
                this.serverIndex++;
                return true;
            }
        }
        if (this.scope == TerminalDropPayload.SCOPE_TYPES) {
            final int idx = typeCellAt((int) mouseX, (int) mouseY, px, bodyY);
            final List<NetworkItemEntry> items = menu.networkItems();
            if (idx >= 0 && idx < items.size()) {
                final StorageKey key = items.get(idx).key();
                if (!this.types.remove(key) && this.types.size() < TerminalDropPayload.MAX_TYPES) {
                    this.types.add(key);
                }
                return true;
            }
        }
        final int by = py + DROP_H - 22;
        if (inRect(mouseX, mouseY, px + 8, by, 70, 16)) {
            close();
            return true;
        }
        if (inRect(mouseX, mouseY, px + 84, by, DROP_W - 92, 16) && confirmEnabled()) {
            send();
            close();
            return true;
        }
        if (mouseX < px || mouseX >= px + DROP_W || mouseY < py || mouseY >= py + DROP_H) {
            close();
        }
        return true;
    }

    private int x() {
        return screen.left() + (ComputerTerminalLayout.WIDTH - DROP_W) / 2;
    }

    private int y() {
        return screen.top() + (ComputerTerminalLayout.HEIGHT - DROP_H) / 2;
    }

    private boolean confirmEnabled() {
        return switch (this.scope) {
            case TerminalDropPayload.SCOPE_SERVER -> !menu.networkServers().isEmpty();
            case TerminalDropPayload.SCOPE_TYPES -> !this.types.isEmpty();
            default -> true; // SCOPE_NETWORK
        };
    }

    private void renderServer(final GuiGraphics g, final int mouseX, final int mouseY,
                              final int px, final int bodyY) {
        final List<NetworkServersPayload.ServerEntry> servers = menu.networkServers();
        if (servers.isEmpty()) {
            g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.NO_SERVERS), px + 8, bodyY,
                    JsTechTheme.amber(), false);
            return;
        }
        final NetworkServersPayload.ServerEntry target =
                servers.get(Math.floorMod(this.serverIndex, servers.size()));
        final boolean lh = inRect(mouseX, mouseY, px + 8, bodyY, 14, 14);
        final boolean rh = inRect(mouseX, mouseY, px + DROP_W - 22, bodyY, 14, 14);
        g.fill(px + 8, bodyY, px + 22, bodyY + 14, lh ? 0xFF24323C : 0xFF1A222B);
        g.drawCenteredString(screen.tabFont(), "<", px + 15, bodyY + 3, JsTechTheme.accent());
        g.fill(px + DROP_W - 22, bodyY, px + DROP_W - 8, bodyY + 14, rh ? 0xFF24323C : 0xFF1A222B);
        g.drawCenteredString(screen.tabFont(), ">", px + DROP_W - 15, bodyY + 3, JsTechTheme.accent());
        g.drawCenteredString(screen.tabFont(),
                screen.tabFont().plainSubstrByWidth(target.name(), DROP_W - 56),
                px + DROP_W / 2, bodyY + 3, JsTechTheme.text());
        g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.WIPES_SERVER), px + 8, bodyY + 20,
                JsTechTheme.text(), false);
        g.drawString(screen.tabFont(),
                GameText.resolve(TerminalUpkeepTexts.FREE_NOW.with(ComputerTerminalScreen.fmt(target.free()))),
                px + 8, bodyY + 32, JsTechTheme.dim(), false);
    }

    private void renderTypes(final GuiGraphics g, final int px, final int bodyY) {
        final List<NetworkItemEntry> items = menu.networkItems();
        if (items.isEmpty()) {
            g.drawString(screen.tabFont(), GameText.resolve(TerminalUpkeepTexts.NO_TYPES), px + 8, bodyY,
                    JsTechTheme.amber(), false);
            return;
        }
        final int gridX = px + (DROP_W - DROP_GRID_COLS * 18) / 2;
        final int rows = (items.size() + DROP_GRID_COLS - 1) / DROP_GRID_COLS;
        this.typeScrollRow = Math.max(0, Math.min(Math.max(0, rows - DROP_GRID_ROWS), this.typeScrollRow));
        final int start = this.typeScrollRow * DROP_GRID_COLS;
        for (int r = 0; r < DROP_GRID_ROWS; r++) {
            for (int col = 0; col < DROP_GRID_COLS; col++) {
                final int sx = gridX + col * 18;
                final int sy = bodyY + r * 18;
                screen.slotBg(g, sx, sy);
                final int idx = start + r * DROP_GRID_COLS + col;
                if (idx < items.size()) {
                    final NetworkItemEntry e = items.get(idx);
                    screen.drawDataIcon(g, e.key(), e.total(), sx, sy);
                    if (this.types.contains(e.key())) {
                        g.pose().pushPose();
                        g.pose().translate(0, 0, 200); // frame the slot ABOVE the rendered item
                        g.fill(sx - 1, sy - 1, sx + 17, sy, JsTechTheme.red());
                        g.fill(sx - 1, sy + 16, sx + 17, sy + 17, JsTechTheme.red());
                        g.fill(sx - 1, sy, sx, sy + 16, JsTechTheme.red());
                        g.fill(sx + 16, sy, sx + 17, sy + 16, JsTechTheme.red());
                        g.pose().popPose();
                    }
                }
            }
        }
        g.drawString(screen.tabFont(),
                GameText.resolve(TerminalUpkeepTexts.SELECTED.with(this.types.size(), items.size())),
                px + 8, bodyY + DROP_GRID_ROWS * 18 + 2, JsTechTheme.amber(), false);
    }

    private int typeCellAt(final int mx, final int my, final int px, final int bodyY) {
        final int gridX = px + (DROP_W - DROP_GRID_COLS * 18) / 2;
        final int relX = mx - gridX;
        final int relY = my - bodyY;
        if (relX < 0 || relX >= DROP_GRID_COLS * 18 || relY < 0 || relY >= DROP_GRID_ROWS * 18) {
            return -1;
        }
        return (this.typeScrollRow + relY / 18) * DROP_GRID_COLS + relX / 18;
    }

    private void send() {
        final List<StorageKey> chosen = this.scope == TerminalDropPayload.SCOPE_TYPES
                ? new ArrayList<>(this.types) : List.of();
        String serverKey = "";
        if (this.scope == TerminalDropPayload.SCOPE_SERVER) {
            final List<NetworkServersPayload.ServerEntry> servers = menu.networkServers();
            if (servers.isEmpty()) {
                return;
            }
            serverKey = servers.get(Math.floorMod(this.serverIndex, servers.size())).key();
        }
        PacketDistributor.sendToServer(new TerminalDropPayload(
                menu.monitorPos(), menu.hostPos(), this.scope, chosen, serverKey));
        screen.maintHint = TerminalTexts.LOGGED.with("DROP");
    }

    private static boolean inRect(final double mx, final double my, final int x, final int y,
                                  final int w, final int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
