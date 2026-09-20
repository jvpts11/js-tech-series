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
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Local heading: what this machine is made of and how much of it is in use.
 *
 * <p>The hardware on the left, what the network round it looks like on the right. Which is the point of
 * the heading: it is the one place that answers "is this machine itself all right", as opposed to every
 * other heading, which answers something about the network it is on.
 */
final class LocalTerminalTab extends AbstractTerminalTab {

    private static final int PANE_X = ComputerTerminalLayout.PANE_X;
    private static final int PANE_Y = ComputerTerminalLayout.PANE_Y;
    private static final int PANE_W = ComputerTerminalLayout.PANE_W;

    /** How wide the hardware column runs: up to the panel beside it, with a gutter. */
    private static final int COL_W = PANE_X - ComputerTerminalLayout.CONTENT_X - 10;

    LocalTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        final int tileW = (COL_W - 8) / 3;
        for (int i = 0; i < 3; i++) {
            final int tx = cx + i * (tileW + 4);
            g.fill(tx, cy + 32, tx + tileW, cy + 58, PANEL());
            g.fill(tx, cy + 32, tx + tileW, cy + 33, LINE());
        }
        inlineTrack(g, cx, cy + 76, COL_W, frac(menu.installedCpus(), menu.cpuSlots()), ACCENT2());
        inlineTrack(g, cx, cy + 88, COL_W, frac(menu.installedRam(), menu.ramSlots()), ACCENT2());
        inlineTrack(g, cx, cy + 100, COL_W, frac(menu.installedGpus(), menu.gpuSlots()), ACCENT());
        inlineTrack(g, cx, cy + 112, COL_W, frac(menu.installedDisks(), menu.diskSlots()), ACCENT());
        final long cap = menu.storageCapacity();
        final double sf = cap <= 0 ? 0 : Math.min(1.0, (double) menu.storageUsed() / cap);
        inlineTrack(g, cx, cy + 128, COL_W, sf, cap <= 0 ? DIM() : (sf > 0.9 ? RED() : GREEN()));
        screen.paneBg(g, x, y);
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final int tileW = (COL_W - 8) / 3;
        g.drawString(font(), "THIS COMPUTER", cx, cy + 20, DIM(), false);
        tile(g, cx, cy + 32, "CAPACITY", fmt(menu.capacity()), "it/t");
        tile(g, cx + tileW + 4, cy + 32, "QUEUES", String.valueOf(menu.queues()), "");
        tile(g, cx + 2 * (tileW + 4), cy + 32, "RAM BUF", fmt(menu.ramBuffer()), "it");

        g.drawString(font(), "HARDWARE", cx, cy + 64, DIM(), false);
        barLabel(g, cx, cy + 76, COL_W, "CPU", menu.installedCpus() + "/" + menu.cpuSlots());
        barLabel(g, cx, cy + 88, COL_W, "RAM", menu.installedRam() + "/" + menu.ramSlots());
        barLabel(g, cx, cy + 100, COL_W, "GPU", menu.installedGpus() + "/" + menu.gpuSlots());
        barLabel(g, cx, cy + 112, COL_W, "Disk", menu.installedDisks() + "/" + menu.diskSlots());
        final String store = menu.storageCapacity() <= 0 ? "no disk"
                : fmt(menu.storageUsed()) + "/" + fmt(menu.storageCapacity());
        barLabel(g, cx, cy + 128, COL_W, "Storage", store);

        renderNetworkPane(g);
    }

    /** What the machine can see of the network it is on, which is the other half of "is this all right". */
    private void renderNetworkPane(final GuiGraphics g) {
        final int px = PANE_X + 6;
        final int right = PANE_X + PANE_W - 6;
        g.drawString(font(), "THE NETWORK", px, PANE_Y + 6, DIM(), false);
        final int net = menu.networkLinkState();
        final String state = net == 2 ? "two orchestrators" : net == 1 ? "joined" : "not on one";
        g.drawString(font(), state, px, PANE_Y + 20, net == 2 ? RED() : net == 1 ? GREEN() : DIM(), false);
        if (net != 1) {
            g.drawString(font(), "A data cable to a Mainframe", px, PANE_Y + 36, DIM(), false);
            g.drawString(font(), "is what puts it on one.", px, PANE_Y + 46, DIM(), false);
            return;
        }
        row(g, px, right, PANE_Y + 36, "Servers", String.valueOf(menu.serverCount()));
        row(g, px, right, PANE_Y + 48, "Computers", String.valueOf(menu.pcCount()));
        row(g, px, right, PANE_Y + 60, "Subframes", String.valueOf(menu.subframeCount()));
        g.fill(px, PANE_Y + 74, right, PANE_Y + 75, LINE());
        row(g, px, right, PANE_Y + 80, "Held", fmt(menu.networkStorageUsed()));
        row(g, px, right, PANE_Y + 92, "Room", fmt(menu.networkStorageTotal()));
        row(g, px, right, PANE_Y + 104, "In flight", String.valueOf(menu.activeOps().size()));
    }

    private void row(final GuiGraphics g, final int px, final int right, final int y,
                     final String key, final String value) {
        g.drawString(font(), key, px, y, DIM(), false);
        g.drawString(font(), value, right - font().width(value), y, TEXT(), false);
    }
}
