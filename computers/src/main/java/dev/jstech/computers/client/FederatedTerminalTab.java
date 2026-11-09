/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.client.os.InteractorFederation;
import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.wan.FederationRequestPayload;
import dev.jstech.computers.operation.payload.wan.FederationStatePayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.wan.WanRight;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Federated heading on MC-NET (computers/docs/design/network.md): a network this one is federated with, in a view
 * of its own, as the Network Interactor's Federated tab shows it on a desktop. Its store is a grid; a cell picked out
 * is taken from it (a stack, or all of it with Shift) or crafted there (one, or a stack with Shift), and a stack held
 * over the grid is sent to it. Beside the grid, what this network may do there, which the far network allows or refuses
 * by its own gateway's switches.
 */
final class FederatedTerminalTab extends AbstractTerminalTab {

    /* The network shown, by its id; empty for the nearest. */
    private String shown = "";
    @Nullable
    private StorageKey picked;
    private int scroll;
    private int ticks;
    /* What the grid lists, worked out once for each answer of the server's. */
    @Nullable
    private FederationStatePayload listed;
    private List<NetworkItemEntry> entries = List.of();
    /* Where the screen stood when last drawn, which a click is read against. */
    private int lastX;
    private int lastY;

    private static final int GRID_X = ComputerTerminalLayout.GRID_X;
    private static final int GRID_Y = ComputerTerminalLayout.GRID_Y;
    private static final int COLS = ComputerTerminalLayout.GRID_COLS;
    private static final int ROWS = ComputerTerminalLayout.GRID_ROWS;
    private static final int SLOT = ComputerTerminalLayout.SLOT;
    private static final int HEADING_Y = ComputerTerminalLayout.TOOLBAR_Y + 2;
    private static final int PANE_X = ComputerTerminalLayout.PANE_X;
    private static final int PANE_Y = ComputerTerminalLayout.PANE_Y;
    private static final int PANE_W = ComputerTerminalLayout.PANE_W;
    private static final int BTN_Y = ComputerTerminalLayout.PANE_BTN_Y;
    private static final int BTN_W = ComputerTerminalLayout.PANE_BTN_W;
    private static final int BTN_H = ComputerTerminalLayout.PANE_BTN_H;
    private static final int TAKE_X = ComputerTerminalLayout.PANE_GET_X;
    private static final int CRAFT_X = ComputerTerminalLayout.PANE_CRAFT_X;
    /* The step to the next network, at the panel's top right when there are several. */
    private static final int NEXT_W = 30;
    /* How often, in ticks, the heading asks again how the federation stands. */
    private static final int ASK_TICKS = 20;
    /* What a click on Craft has crafted there with Shift held: a stack, where a plain click crafts one. */
    private static final long CRAFT_STACK = 64L;

    FederatedTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void onContainerTick() {
        if (ticks++ % ASK_TICKS == 0) {
            ask();
        }
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y, final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        lastX = x;
        lastY = y;
        final List<NetworkItemEntry> items = entries();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, (items.size() + COLS - 1) / COLS - ROWS)));
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                final int sx = x + GRID_X + col * SLOT;
                final int sy = y + GRID_Y + row * SLOT;
                slotBg(g, sx, sy);
                final int index = (scroll + row) * COLS + col;
                if (index < items.size()) {
                    final NetworkItemEntry entry = items.get(index);
                    // Something it holds none of, there only to be crafted, shows no count.
                    drawDataIcon(g, entry.key(), entry.total() > 0 ? entry.total() : -1L, sx, sy);
                    if (entry.key().equals(picked)) {
                        g.fill(sx - 1, sy - 1, sx + 17, sy, ACCENT());
                        g.fill(sx - 1, sy + 16, sx + 17, sy + 17, ACCENT());
                    }
                }
            }
        }
        screen.paneBg(g, x, y);
        if (peer() != null) {
            button(g, x + TAKE_X, y + BTN_Y, takeable());
            button(g, x + CRAFT_X, y + BTN_Y, craftable());
        }
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final FederationStatePayload.Peer peer = peer();
        if (peer == null) {
            Draw.text(g, font(), GameText.resolve(InteractorFederation.CAPTION_NONE), cx, HEADING_Y, DIM());
            int ly = PANE_Y + 6;
            for (final FormattedCharSequence line
                    : font().split(GameText.component(InteractorFederation.NONE), PANE_W - 12)) {
                Draw.text(g, font(), line, PANE_X + 6, ly, DIM());
                ly += 10;
            }
            return;
        }
        Draw.text(g, font(), GameText.resolve(InteractorFederation.CAPTION.with(
                peer.name().toUpperCase(Locale.ROOT))), cx, HEADING_Y, ACCENT());
        final String count = GameText.resolve(InteractorFederation.COUNT.with(peer.types(),
                DiskSpec.sizeLabel(peer.usedMb())));
        Draw.text(g, font(), count, cx + cw - font().width(count), HEADING_Y, DIM());
        if (entries().isEmpty()) {
            Draw.text(g, font(), font().plainSubstrByWidth(GameText.resolve((peer.allows(WanRight.SEE)
                    ? InteractorFederation.HOLDS_NOTHING : InteractorFederation.NOT_SEEN).with(peer.name())),
                    COLS * SLOT - 4), GRID_X + 2, GRID_Y + 4, DIM());
        }
        pane(g, peer);
    }

    @Override
    public boolean onMouseClicked(final double mouseX, final double mouseY, final int button) {
        final FederationStatePayload state = state();
        final FederationStatePayload.Peer peer = peer();
        if (button != 0 || state == null || peer == null) {
            return false;
        }
        final double lx = mouseX - lastX;
        final double ly = mouseY - lastY;
        final boolean onGrid = lx >= GRID_X && lx < GRID_X + COLS * SLOT && ly >= GRID_Y && ly < GRID_Y + ROWS * SLOT;
        if (onGrid && !menu.getCarried().isEmpty()) {
            // A stack held over the grid crosses to the network shown, when it lets this one send.
            if (peer.allows(WanRight.SEND)) {
                act(FederationRequestPayload.SEND, Optional.empty(), 0L);
            }
            return true;
        }
        if (onGrid) {
            final int index = (scroll + (int) ((ly - GRID_Y) / SLOT)) * COLS + (int) ((lx - GRID_X) / SLOT);
            picked = index < entries().size() ? entries().get(index).key() : null;
            return true;
        }
        if (state.peers().size() > 1 && inRect(lx, ly, PANE_X + PANE_W - 6 - NEXT_W, PANE_Y + 4, NEXT_W, 10)) {
            shown = state.nextNetwork();
            picked = null;
            scroll = 0;
            ask();
            return true;
        }
        if (inRect(lx, ly, TAKE_X, BTN_Y, BTN_W, BTN_H) && takeable()) {
            final long held = held(picked);
            act(FederationRequestPayload.TAKE, Optional.of(picked),
                    Screen.hasShiftDown() ? held : Math.min(held, picked.batch()));
            return true;
        }
        if (inRect(lx, ly, CRAFT_X, BTN_Y, BTN_W, BTN_H) && craftable()) {
            act(FederationRequestPayload.CRAFT, Optional.of(picked), Screen.hasShiftDown() ? CRAFT_STACK : 1L);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScrolled(final double mouseX, final double mouseY, final double dy) {
        scroll = Math.max(0, scroll - (int) Math.signum(dy));
        return true;
    }

    /* Beside the grid: whose network, how it is reached, what this network may do there, and the two buttons. */
    private void pane(final GuiGraphics g, final FederationStatePayload.Peer peer) {
        final FederationStatePayload state = state();
        final int px = PANE_X + 6;
        final int edge = PANE_X + PANE_W - 6;
        final boolean several = state != null && state.peers().size() > 1;
        Draw.text(g, font(), font().plainSubstrByWidth(peer.name(), PANE_W - 12 - (several ? NEXT_W : 0)), px,
                PANE_Y + 6, ACCENT());
        if (several) {
            final String next = state.shownIndex() + "/" + state.peers().size() + " >";
            Draw.text(g, font(), next, edge - font().width(next), PANE_Y + 6, ACCENT2());
        }
        Draw.text(g, font(), InteractorFederation.idOf(peer), px, PANE_Y + 17, DIM());
        Draw.text(g, font(), font().plainSubstrByWidth(InteractorFederation.reachOf(peer), PANE_W - 12), px,
                PANE_Y + 30, GREEN());
        Draw.text(g, font(), font().plainSubstrByWidth(InteractorFederation.lineOf(peer), PANE_W - 12), px,
                PANE_Y + 41, DIM());
        g.fill(px, PANE_Y + 52, edge, PANE_Y + 53, LINE());
        Draw.text(g, font(), GameText.resolve(InteractorFederation.MAY), px, PANE_Y + 56, DIM());
        int ry = PANE_Y + 67;
        for (final WanRight right : WanRight.values()) {
            final boolean may = peer.allows(right);
            Draw.text(g, font(), GameText.resolve(InteractorFederation.rightLabel(right)), px, ry, TEXT());
            final String word = GameText.resolve(may ? InteractorFederation.YES : InteractorFederation.NO);
            Draw.text(g, font(), word, edge - font().width(word), ry, may ? GREEN() : DIM());
            ry += 9;
        }
        Draw.textCentered(g, font(), GameText.resolve(TerminalGridTexts.GET), TAKE_X + BTN_W / 2, BTN_Y + 2,
                takeable() ? ACCENT() : DIM());
        Draw.textCentered(g, font(), GameText.resolve(InteractorFederation.CRAFT), CRAFT_X + BTN_W / 2, BTN_Y + 2,
                craftable() ? ACCENT() : DIM());
    }

    private void button(final GuiGraphics g, final int bx, final int by, final boolean enabled) {
        g.fill(bx, by, bx + BTN_W, by + BTN_H, enabled ? TAB_ON() : TRACK());
        g.fill(bx, by, bx + BTN_W, by + 1, enabled ? ACCENT() : LINE());
    }

    private void ask() {
        PacketDistributor.sendToServer(FederationRequestPayload.ask(menu.hostPos(), menu.monitorPos(), shown));
    }

    private void act(final byte action, final Optional<StorageKey> key, final long amount) {
        PacketDistributor.sendToServer(new FederationRequestPayload(menu.hostPos(), menu.monitorPos(), shown, action,
                key, amount));
    }

    /* Whether the cell picked can be taken: the network holds some of it and lets this one take. */
    private boolean takeable() {
        final FederationStatePayload.Peer peer = peer();
        return picked != null && peer != null && peer.allows(WanRight.TAKE) && held(picked) > 0L;
    }

    /* Whether the cell picked can be crafted there: the network makes it and lets this one craft. */
    private boolean craftable() {
        final FederationStatePayload state = state();
        final FederationStatePayload.Peer peer = peer();
        return picked != null && state != null && peer != null && peer.allows(WanRight.CRAFT)
                && state.craftable().contains(picked);
    }

    private long held(@Nullable final StorageKey key) {
        for (final NetworkItemEntry entry : entries()) {
            if (entry.key().equals(key)) {
                return entry.total();
            }
        }
        return 0L;
    }

    private List<NetworkItemEntry> entries() {
        final FederationStatePayload state = state();
        if (state != listed) {
            listed = state;
            entries = state == null ? List.of() : state.entries();
        }
        return entries;
    }

    /* The server's last answer; the network it shows becomes the one this heading asks for. */
    @Nullable
    private FederationStatePayload state() {
        final FederationStatePayload state = menu.federation();
        if (state != null && shown.isEmpty()) {
            shown = state.shown();
        }
        return state;
    }

    @Nullable
    private FederationStatePayload.Peer peer() {
        final FederationStatePayload state = state();
        return state == null ? null : state.shownPeer();
    }
}
