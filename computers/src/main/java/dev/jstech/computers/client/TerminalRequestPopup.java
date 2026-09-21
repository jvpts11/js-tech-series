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
import dev.jstech.computers.operation.payload.RequestServerBreakdownPayload;
import dev.jstech.computers.operation.payload.ServerBreakdownPayload;
import dev.jstech.computers.operation.payload.TerminalLocalUploadPayload;
import dev.jstech.computers.operation.payload.TerminalLocalWithdrawPayload;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Asking the network for a thing, and the same question asked of the machine's own store.
 *
 * <p>One panel for both because a player is doing the same thing either way: saying how many and saying
 * go. What differs is where it comes from and where it lands, and that is the difference between the two
 * shapes below: the network's question can be opened out to pick which servers it pulls from and which
 * computer it lands on, and the machine's own store has nowhere to choose between, only a direction.
 */
final class TerminalRequestPopup {

    private static final int POPUP_W = ComputerTerminalLayout.POPUP_W;
    private static final int[] STEP_AMOUNTS = {-1000, -100, -10, -1, 1, 10, 100, 1000};
    private static final String[] STEP_LABELS = {"----", "---", "--", "-", "+", "++", "+++", "++++"};
    private static final int SERVER_ROWS = 4;
    private static final int H_SIMPLE = 110;
    private static final int H_ADVANCED = 200;

    private final ComputerTerminalScreen screen;
    private final ComputerTerminalMenu menu;
    private final TerminalQuantityBox qty;

    @Nullable
    private NetworkItemEntry entry;
    private int amount = 1;
    /** Whether this is the machine's own store being asked, rather than the network. */
    private boolean local;
    private boolean advanced;
    private final Set<String> deselectedServers = new HashSet<>();
    private int destServerIndex;

    TerminalRequestPopup(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu,
                         final TerminalQuantityBox qty) {
        this.screen = screen;
        this.menu = menu;
        this.qty = qty;
    }

    boolean isOpen() {
        return this.entry != null;
    }

    /** Asking the network: the reply brings both the sources to pick from and the places to send it. */
    void openFromNetwork(final NetworkItemEntry picked) {
        this.entry = picked;
        this.local = false;
        this.advanced = false;
        this.deselectedServers.clear();
        this.destServerIndex = 0;
        menu.setServerBreakdown(List.of());  // clear stale rows; the reply repopulates
        menu.setNetworkServers(List.of());   // ditto the destination picker's computer list
        start(picked);
        PacketDistributor.sendToServer(new RequestServerBreakdownPayload(
                menu.monitorPos(), menu.hostPos(), picked.key()));
    }

    /** Asking the machine's own store: there is nothing to choose, only which way it goes. */
    void openFromStorage(final NetworkItemEntry picked) {
        this.entry = picked;
        this.local = true;
        this.advanced = false;
        start(picked);
    }

    void close() {
        this.entry = null;
        this.local = false;
        this.advanced = false;
        this.deselectedServers.clear();
    }

    /** Enter does what the big button does: the network's request, or the way out of local storage. */
    void submit() {
        if (this.local) {
            sendStorage(false);
        } else {
            send();
        }
    }

    /** Where the quantity field belongs while this question is up. */
    void placeField() {
        this.qty.moveTo(x() + 8, y() + 30);
    }

    void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        final NetworkItemEntry picked = this.entry;
        if (picked == null) {
            return;
        }
        /*
         * Items (the grid and the inventory) render at a higher z than flat fills, so the panel must sit
         * above them or they show through. Push the whole thing, the quantity field included, forward.
         */
        g.pose().pushPose();
        g.pose().translate(0, 0, 350);
        final int left = screen.left();
        final int top = screen.top();
        g.fill(left, top, left + ComputerTerminalLayout.WIDTH, top + ComputerTerminalLayout.HEIGHT, 0xE0070A0F);
        final int px = x();
        final int py = y();
        final int ph = height();
        g.fill(px - 1, py - 1, px + POPUP_W + 1, py + ph + 1, JsTechTheme.accent());
        g.fill(px, py, px + POPUP_W, py + ph, 0xFF0F151C);

        // Header: icon, name and what there is, plus the toggle that opens the question out.
        screen.drawDataIcon(g, picked.key(), -1L, px + 8, py + 5);
        g.drawString(screen.tabFont(),
                screen.tabFont().plainSubstrByWidth(picked.name().getString(), POPUP_W - 78),
                px + 28, py + 6, JsTechTheme.text(), false);
        g.drawString(screen.tabFont(), ComputerTerminalScreen.fmt(picked.total())
                        + (picked.key().isItem() ? "" : " mB") + (this.local ? " in local" : " available"),
                px + 28, py + 17, JsTechTheme.dim(), false);
        if (!this.local) {
            final boolean advHover = inRect(mouseX, mouseY, px + POPUP_W - 44, py + 5, 36, 12);
            g.fill(px + POPUP_W - 44, py + 5, px + POPUP_W - 8, py + 17,
                    this.advanced ? JsTechTheme.accent() : (advHover ? 0xFF24323C : 0xFF1A222B));
            g.drawCenteredString(screen.tabFont(), "ADV", px + POPUP_W - 26, py + 7,
                    this.advanced ? 0xFF0F151C : JsTechTheme.dim());
        }

        // Quantity: the field, drawn here so it sits on the panel, and the button that asks for all of it.
        g.fill(px + 7, py + 29, px + 127, py + 45, JsTechTheme.track());
        this.qty.render(g, mouseX, mouseY, partialTick);
        final boolean maxHover = inRect(mouseX, mouseY, px + POPUP_W - 58, py + 30, 50, 14);
        g.fill(px + POPUP_W - 58, py + 30, px + POPUP_W - 8, py + 44, maxHover ? 0xFF2BB3A4 : 0xFF1F9488);
        g.drawCenteredString(screen.tabFont(), "MAX", px + POPUP_W - 33, py + 33, 0xFFFFFFFF);

        for (int i = 0; i < STEP_LABELS.length; i++) {
            final int bx = px + 8 + i * 23;
            final boolean hover = inRect(mouseX, mouseY, bx, py + 48, 22, 14);
            g.fill(bx, py + 48, bx + 22, py + 62, hover ? 0xFF24323C : 0xFF1A222B);
            g.drawCenteredString(screen.tabFont(), STEP_LABELS[i], bx + 11, py + 51,
                    STEP_AMOUNTS[i] > 0 ? JsTechTheme.accent() : JsTechTheme.amber());
        }

        if (this.local) {
            renderStorageActions(g, mouseX, mouseY, px, py, ph);
        } else if (this.advanced) {
            renderAdvanced(g, mouseX, mouseY, px, py, ph);
        } else {
            renderSimple(g, mouseX, mouseY, px, py, ph);
        }
        g.pose().popPose();
    }

    /** Modal: a click outside closes it, a right click closes it, everything else is answered here. */
    boolean clicked(final double mouseX, final double mouseY, final int button) {
        final NetworkItemEntry picked = this.entry;
        if (picked == null) {
            return false;
        }
        if (button == 1) {
            close();
            return true;
        }
        if (button != 0) {
            return true;
        }
        final int px = x();
        final int py = y();
        final int ph = height();
        if (mouseX < px || mouseX >= px + POPUP_W || mouseY < py || mouseY >= py + ph) {
            close();
            return true;
        }
        if (!this.local && inRect(mouseX, mouseY, px + POPUP_W - 44, py + 5, 36, 12)) {
            this.advanced = !this.advanced;
            placeField();
            return true;
        }
        if (inRect(mouseX, mouseY, px + 7, py + 29, 120, 16)) {
            screen.giveKeyboardTo(this.qty);
            this.qty.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        if (inRect(mouseX, mouseY, px + POPUP_W - 58, py + 30, 50, 14)) {
            setAmount((int) Math.min(Integer.MAX_VALUE, picked.total()));
            return true;
        }
        for (int i = 0; i < STEP_AMOUNTS.length; i++) {
            if (inRect(mouseX, mouseY, px + 8 + i * 23, py + 48, 22, 14)) {
                setAmount(this.amount + STEP_AMOUNTS[i]);
                return true;
            }
        }
        if (this.local) {
            final int by = py + ph - 22;
            if (inRect(mouseX, mouseY, px + 8, by, 90, 16)) {
                sendStorage(false);
            } else if (inRect(mouseX, mouseY, px + 104, by, 90, 16)) {
                sendStorage(true);
            }
            return true;
        }
        if (this.advanced && clickedAdvanced(mouseX, mouseY, px, py)) {
            return true;
        }
        if (inRect(mouseX, mouseY, px + 8, py + ph - 22, POPUP_W - 16, 16)) {
            send();
            return true;
        }
        return true;
    }

    /** What the typed digits mean: never more than there is, and never nothing. */
    void typed(final long value) {
        final NetworkItemEntry picked = this.entry;
        if (picked == null) {
            return;
        }
        this.amount = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, Math.min(picked.total(), value)));
    }

    private void start(final NetworkItemEntry picked) {
        placeField();
        setAmount((int) Math.min(64L, Math.max(1L, picked.total())));
        screen.giveKeyboardTo(this.qty);
    }

    private void setAmount(final int value) {
        final NetworkItemEntry picked = this.entry;
        final int max = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, picked == null ? 1L : picked.total()));
        this.amount = Math.max(1, Math.min(max, value));
        this.qty.set(this.amount);
    }

    private int x() {
        return screen.left() + (ComputerTerminalLayout.WIDTH - POPUP_W) / 2;
    }

    /*
     * Centred on the height this question actually has, not on a fixed one, so the short shape does not
     * sit high on the glass with a gap under it. Opening the question out moves the top, so whoever flips
     * that toggle puts the quantity field back where the new top wants it.
     */
    private int y() {
        return screen.top() + (ComputerTerminalLayout.HEIGHT - height()) / 2;
    }

    private int height() {
        return !this.local && this.advanced ? H_ADVANCED : H_SIMPLE;
    }

    private boolean clickedAdvanced(final double mouseX, final double mouseY, final int px, final int py) {
        final List<ServerBreakdownPayload.ServerHolding> servers = menu.serverBreakdown();
        for (int i = 0; i < Math.min(SERVER_ROWS, servers.size()); i++) {
            if (inRect(mouseX, mouseY, px + 8, py + 78 + i * 12, POPUP_W - 16, 11)) {
                final String key = servers.get(i).key();
                if (!this.deselectedServers.remove(key)) {
                    this.deselectedServers.add(key);
                }
                return true;
            }
        }
        final int count = menu.networkServers().size();
        if (count > 0) {
            if (inRect(mouseX, mouseY, px + 8, py + 152, 14, 14)) {
                this.destServerIndex = Math.floorMod(this.destServerIndex - 1, count);
                return true;
            }
            if (inRect(mouseX, mouseY, px + POPUP_W - 22, py + 152, 14, 14)) {
                this.destServerIndex = Math.floorMod(this.destServerIndex + 1, count);
                return true;
            }
        }
        return false;
    }

    private void send() {
        final NetworkItemEntry picked = this.entry;
        if (picked == null || this.amount <= 0) {
            return;
        }
        final List<String> keys = new ArrayList<>();
        boolean anyDeselected = false;
        int kind = TerminalSelectPayload.DEST_AUTO;
        String destServer = "";
        /*
         * Sources and destination are the opened-out question only; a plain request pulls from everywhere
         * to wherever it lands by itself, which is the machine's own store, or the player's hands.
         */
        if (this.advanced) {
            for (final ServerBreakdownPayload.ServerHolding s : menu.serverBreakdown()) {
                if (this.deselectedServers.contains(s.key())) {
                    anyDeselected = true;
                } else {
                    keys.add(s.key());
                }
            }
            if (anyDeselected && keys.isEmpty() && !menu.serverBreakdown().isEmpty()) {
                close(); // every source unchecked, nothing to pull from
                return;
            }
            final List<NetworkServersPayload.ServerEntry> comp = menu.networkServers();
            if (comp.isEmpty()) {
                return; // no destination computer yet, keep the question open
            }
            kind = TerminalSelectPayload.DEST_SERVER;
            destServer = comp.get(Math.floorMod(this.destServerIndex, comp.size())).key();
        }
        PacketDistributor.sendToServer(new TerminalSelectPayload(
                menu.monitorPos(), menu.hostPos(), picked.key(), this.amount,
                anyDeselected ? keys : List.of(), kind, destServer));
        close();
    }

    private void sendStorage(final boolean toNetwork) {
        final NetworkItemEntry picked = this.entry;
        if (picked == null || this.amount <= 0) {
            return;
        }
        if (toNetwork) {
            PacketDistributor.sendToServer(new TerminalLocalUploadPayload(
                    menu.monitorPos(), menu.hostPos(), picked.key(), this.amount));
        } else {
            PacketDistributor.sendToServer(new TerminalLocalWithdrawPayload(
                    menu.monitorPos(), menu.hostPos(), picked.key(), this.amount));
        }
        close();
    }

    private void renderSimple(final GuiGraphics g, final int mouseX, final int mouseY,
                              final int px, final int py, final int ph) {
        final boolean hasStorage = menu.usableStorageSlots() > 0;
        g.drawString(screen.tabFont(),
                hasStorage ? "Lands in this computer's storage" : "Needs internal storage (no disk)",
                px + 8, py + 70, hasStorage ? JsTechTheme.dim() : JsTechTheme.amber(), false);
        actionButton(g, mouseX, mouseY, px + 8, py + ph - 22, POPUP_W - 16,
                "REQUEST " + ComputerTerminalScreen.fmt(this.amount));
    }

    private void renderStorageActions(final GuiGraphics g, final int mouseX, final int mouseY,
                                      final int px, final int py, final int ph) {
        g.drawString(screen.tabFont(), "Send local items to:", px + 8, py + 70, JsTechTheme.dim(), false);
        final int by = py + ph - 22;
        actionButton(g, mouseX, mouseY, px + 8, by, 90, "TO INVENTORY");
        actionButton(g, mouseX, mouseY, px + 104, by, 90, "TO NETWORK");
    }

    private void renderAdvanced(final GuiGraphics g, final int mouseX, final int mouseY,
                                final int px, final int py, final int ph) {
        g.drawString(screen.tabFont(), "PULL FROM", px + 8, py + 66, JsTechTheme.dim(), false);
        final List<ServerBreakdownPayload.ServerHolding> servers = menu.serverBreakdown();
        if (servers.isEmpty()) {
            g.drawString(screen.tabFont(), "all servers",
                    px + POPUP_W - 8 - screen.tabFont().width("all servers"), py + 66,
                    JsTechTheme.dim(), false);
        }
        for (int i = 0; i < Math.min(SERVER_ROWS, servers.size()); i++) {
            final ServerBreakdownPayload.ServerHolding s = servers.get(i);
            final int ry = py + 78 + i * 12;
            final boolean on = !this.deselectedServers.contains(s.key());
            g.fill(px + 8, ry, px + 18, ry + 10, on ? JsTechTheme.accent() : 0xFF2A3340);
            g.fill(px + 9, ry + 1, px + 17, ry + 9, on ? JsTechTheme.accent() : 0xFF11161D);
            g.drawString(screen.tabFont(), screen.tabFont().plainSubstrByWidth(s.label(), 120), px + 22,
                    ry + 1, on ? JsTechTheme.text() : JsTechTheme.dim(), false);
            final String c = ComputerTerminalScreen.fmt(s.count());
            g.drawString(screen.tabFont(), c, px + POPUP_W - 8 - screen.tabFont().width(c), ry + 1,
                    JsTechTheme.dim(), false);
        }
        if (servers.size() > SERVER_ROWS) {
            g.drawString(screen.tabFont(), "+" + (servers.size() - SERVER_ROWS) + " more (included)",
                    px + 22, py + 78 + SERVER_ROWS * 12, JsTechTheme.dim(), false);
        }

        // SEND TO: cycle through every computer that can receive items.
        g.drawString(screen.tabFont(), "SEND TO", px + 8, py + 140, JsTechTheme.dim(), false);
        final List<NetworkServersPayload.ServerEntry> comp = menu.networkServers();
        if (comp.isEmpty()) {
            g.drawString(screen.tabFont(), "No computers available", px + 8, py + 154,
                    JsTechTheme.amber(), false);
        } else {
            final NetworkServersPayload.ServerEntry target =
                    comp.get(Math.floorMod(this.destServerIndex, comp.size()));
            final boolean lh = inRect(mouseX, mouseY, px + 8, py + 152, 14, 14);
            final boolean rh = inRect(mouseX, mouseY, px + POPUP_W - 22, py + 152, 14, 14);
            g.fill(px + 8, py + 152, px + 22, py + 166, lh ? 0xFF24323C : 0xFF1A222B);
            g.drawCenteredString(screen.tabFont(), "<", px + 15, py + 155, JsTechTheme.accent());
            g.fill(px + POPUP_W - 22, py + 152, px + POPUP_W - 8, py + 166, rh ? 0xFF24323C : 0xFF1A222B);
            g.drawCenteredString(screen.tabFont(), ">", px + POPUP_W - 15, py + 155, JsTechTheme.accent());
            final String text = screen.tabFont().plainSubstrByWidth(
                    target.name() + "  (" + ComputerTerminalScreen.fmt(target.free()) + " free)", POPUP_W - 52);
            g.drawString(screen.tabFont(), text, px + 26, py + 155, JsTechTheme.text(), false);
        }
        actionButton(g, mouseX, mouseY, px + 8, py + ph - 22, POPUP_W - 16,
                "SEND " + ComputerTerminalScreen.fmt(this.amount));
    }

    private void actionButton(final GuiGraphics g, final int mouseX, final int mouseY,
                              final int x, final int y, final int w, final String label) {
        final boolean hover = inRect(mouseX, mouseY, x, y, w, 16);
        g.fill(x, y, x + w, y + 16, hover ? 0xFF2BB3A4 : 0xFF1F9488);
        g.drawCenteredString(screen.tabFont(), label, x + w / 2, y + 4, 0xFFFFFFFF);
    }

    private static boolean inRect(final double mx, final double my, final int x, final int y,
                                  final int w, final int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
