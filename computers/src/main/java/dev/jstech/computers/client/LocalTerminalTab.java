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
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

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
        g.drawString(font(), GameText.resolve(TerminalGridTexts.THIS_COMPUTER), cx, cy + 20, DIM(), false);
        tile(g, cx, cy + 32, GameText.resolve(AssemblyTexts.CAPACITY), fmt(menu.capacity()),
                GameText.resolve(AssemblyTexts.ITEMS_PER_TICK));
        tile(g, cx + tileW + 4, cy + 32, GameText.resolve(AssemblyTexts.QUEUES), String.valueOf(menu.queues()), "");
        tile(g, cx + 2 * (tileW + 4), cy + 32, GameText.resolve(AssemblyTexts.RAM_BUFFER_SHORT),
                fmt(menu.ramBuffer()), GameText.resolve(AssemblyTexts.ITEMS));

        g.drawString(font(), GameText.resolve(TerminalGridTexts.HARDWARE), cx, cy + 64, DIM(), false);
        barLabel(g, cx, cy + 76, COL_W, GameText.resolve(AssemblyTexts.CPU),
                menu.installedCpus() + "/" + menu.cpuSlots());
        barLabel(g, cx, cy + 88, COL_W, GameText.resolve(AssemblyTexts.RAM),
                menu.installedRam() + "/" + menu.ramSlots());
        barLabel(g, cx, cy + 100, COL_W, GameText.resolve(AssemblyTexts.GPU),
                menu.installedGpus() + "/" + menu.gpuSlots());
        barLabel(g, cx, cy + 112, COL_W, GameText.resolve(TerminalGridTexts.DISK),
                menu.installedDisks() + "/" + menu.diskSlots());
        final String store = menu.storageCapacity() <= 0 ? GameText.resolve(TerminalGridTexts.NO_DISK)
                : fmt(menu.storageUsed()) + "/" + fmt(menu.storageCapacity());
        barLabel(g, cx, cy + 128, COL_W, GameText.resolve(TerminalGridTexts.STORAGE), store);

        renderNetworkPane(g);
    }

    /** What the machine can see of the network it is on, which is the other half of "is this all right". */
    private void renderNetworkPane(final GuiGraphics g) {
        final int px = PANE_X + 6;
        final int right = PANE_X + PANE_W - 6;
        g.drawString(font(), GameText.resolve(TerminalGridTexts.THE_NETWORK), px, PANE_Y + 6, DIM(), false);
        final int net = menu.networkLinkState();
        final TextKey state = net == 2 ? TerminalGridTexts.TWO_ORCHESTRATORS
                : net == 1 ? TerminalGridTexts.JOINED : TerminalGridTexts.NOT_ON_ONE;
        g.drawString(font(), GameText.resolve(state), px, PANE_Y + 20,
                net == 2 ? RED() : net == 1 ? GREEN() : DIM(), false);
        if (net != 1) {
            int y = PANE_Y + 36;
            for (final FormattedCharSequence line
                    : font().split(GameText.component(TerminalGridTexts.CABLE_HINT), PANE_W - 12)) {
                g.drawString(font(), line, px, y, DIM(), false);
                y += 10;
            }
            return;
        }
        row(g, px, right, PANE_Y + 36, TerminalGridTexts.SERVERS, String.valueOf(menu.serverCount()));
        row(g, px, right, PANE_Y + 48, TerminalGridTexts.COMPUTERS, String.valueOf(menu.pcCount()));
        row(g, px, right, PANE_Y + 60, TerminalGridTexts.SUBFRAMES, String.valueOf(menu.subframeCount()));
        g.fill(px, PANE_Y + 74, right, PANE_Y + 75, LINE());
        row(g, px, right, PANE_Y + 80, TerminalGridTexts.HELD, fmt(menu.networkStorageUsed()));
        row(g, px, right, PANE_Y + 92, TerminalGridTexts.ROOM, fmt(menu.networkStorageTotal()));
        row(g, px, right, PANE_Y + 104, TerminalGridTexts.IN_FLIGHT, String.valueOf(menu.activeOps().size()));
    }

    private void row(final GuiGraphics g, final int px, final int right, final int y,
                     final TextKey key, final String value) {
        g.drawString(font(), GameText.resolve(key), px, y, DIM(), false);
        g.drawString(font(), value, right - font().width(value), y, TEXT(), false);
    }
}
