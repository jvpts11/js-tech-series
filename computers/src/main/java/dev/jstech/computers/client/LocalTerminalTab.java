/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;

/**
 * The Local heading: what this machine is made of, how much of it is in use, and what it draws.
 *
 * <p>The hardware on the left, what the network round it looks like on the right. Which is the point of
 * the heading: it is the one place that answers "is this machine itself all right", as opposed to every
 * other heading, which answers something about the network it is on. Its id stands across from its title, which
 * tells it apart from machines of the same name.
 */
final class LocalTerminalTab extends AbstractTerminalTab {

    private static final int PANE_X = ComputerTerminalLayout.PANE_X;
    private static final int PANE_Y = ComputerTerminalLayout.PANE_Y;
    private static final int PANE_W = ComputerTerminalLayout.PANE_W;
    private static final int COL_W = ComputerTerminalLayout.LOCAL_COL_W;
    /* A joule a tick is twenty watts: what the line gives, in FE a tick, is its watts over twenty. */
    private static final double WATTS_A_FE_TICK = 20.0;

    LocalTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        for (int column = 0; column < 3; column++) {
            tileBg(g, cx, cy, 0, column);
        }
        // What the parts draw, and what the line gives them unless the computers run free.
        tileBg(g, cx, cy, 1, 0);
        if (ComputersServerConfig.drawEnergy()) {
            tileBg(g, cx, cy, 1, 1);
        }
        inlineTrack(g, cx, cy + ComputerTerminalLayout.localBarDy(0), COL_W,
                frac(menu.installedCpus(), menu.cpuSlots()), ACCENT2());
        inlineTrack(g, cx, cy + ComputerTerminalLayout.localBarDy(1), COL_W,
                frac(menu.installedRam(), menu.ramSlots()), ACCENT2());
        inlineTrack(g, cx, cy + ComputerTerminalLayout.localBarDy(2), COL_W,
                frac(menu.installedGpus(), menu.gpuSlots()), ACCENT());
        inlineTrack(g, cx, cy + ComputerTerminalLayout.localBarDy(3), COL_W,
                frac(menu.installedDisks(), menu.diskSlots()), ACCENT());
        final long cap = menu.storageCapacity();
        final double sf = cap <= 0 ? 0 : Math.min(1.0, (double) menu.storageUsed() / cap);
        inlineTrack(g, cx, cy + ComputerTerminalLayout.LOCAL_STORAGE_DY, COL_W, sf,
                cap <= 0 ? DIM() : (sf > 0.9 ? RED() : GREEN()));
        screen.paneBg(g, x, y);
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        final int titleY = cy + ComputerTerminalLayout.LOCAL_TITLE_DY;
        Draw.text(g, font(), GameText.resolve(TerminalGridTexts.THIS_COMPUTER), cx, titleY, DIM());
        final String id = menu.machineId();
        Draw.text(g, font(), id, cx + COL_W - font().width(id), titleY, TEXT());
        tile(g, cx + ComputerTerminalLayout.localTileDx(0), cy + ComputerTerminalLayout.localTileDy(0),
                GameText.resolve(AssemblyTexts.CAPACITY), fmt(menu.capacity()),
                GameText.resolve(AssemblyTexts.ITEMS_PER_TICK));
        tile(g, cx + ComputerTerminalLayout.localTileDx(1), cy + ComputerTerminalLayout.localTileDy(0),
                GameText.resolve(AssemblyTexts.QUEUES), String.valueOf(menu.queues()), "");
        tile(g, cx + ComputerTerminalLayout.localTileDx(2), cy + ComputerTerminalLayout.localTileDy(0),
                GameText.resolve(AssemblyTexts.RAM_BUFFER_SHORT), fmt(menu.ramBuffer()),
                GameText.resolve(AssemblyTexts.ITEMS));
        tile(g, cx + ComputerTerminalLayout.localTileDx(0), cy + ComputerTerminalLayout.localTileDy(1),
                GameText.resolve(AssemblyTexts.DRAW), fmt(menu.partsWatts()),
                GameText.resolve(TerminalGridTexts.WATTS));
        if (ComputersServerConfig.drawEnergy()) {
            tile(g, cx + ComputerTerminalLayout.localTileDx(1), cy + ComputerTerminalLayout.localTileDy(1),
                    GameText.resolve(TerminalGridTexts.LINE), fmt(Math.round(menu.lineWatts() / WATTS_A_FE_TICK)),
                    GameText.resolve(TerminalGridTexts.FE_PER_TICK));
        }

        Draw.text(g, font(), GameText.resolve(TerminalGridTexts.HARDWARE), cx,
                cy + ComputerTerminalLayout.LOCAL_HARDWARE_DY, DIM());
        barLabel(g, cx, cy + ComputerTerminalLayout.localBarDy(0), COL_W, GameText.resolve(AssemblyTexts.CPU),
                menu.installedCpus() + "/" + menu.cpuSlots());
        barLabel(g, cx, cy + ComputerTerminalLayout.localBarDy(1), COL_W, GameText.resolve(AssemblyTexts.RAM),
                menu.installedRam() + "/" + menu.ramSlots());
        barLabel(g, cx, cy + ComputerTerminalLayout.localBarDy(2), COL_W, GameText.resolve(AssemblyTexts.GPU),
                menu.installedGpus() + "/" + menu.gpuSlots());
        barLabel(g, cx, cy + ComputerTerminalLayout.localBarDy(3), COL_W, GameText.resolve(TerminalGridTexts.DISK),
                menu.installedDisks() + "/" + menu.diskSlots());
        final String store = menu.storageCapacity() <= 0 ? GameText.resolve(TerminalGridTexts.NO_DISK)
                : fmt(menu.storageUsed()) + "/" + fmt(menu.storageCapacity());
        barLabel(g, cx, cy + ComputerTerminalLayout.LOCAL_STORAGE_DY, COL_W,
                GameText.resolve(TerminalGridTexts.STORAGE), store);

        renderNetworkPane(g);
    }

    /* The panel of the tile in {@code row} and {@code column}, with the line across its top. */
    private void tileBg(final GuiGraphics g, final int cx, final int cy, final int row, final int column) {
        final int tx = cx + ComputerTerminalLayout.localTileDx(column);
        final int ty = cy + ComputerTerminalLayout.localTileDy(row);
        g.fill(tx, ty, tx + ComputerTerminalLayout.localTileW(), ty + ComputerTerminalLayout.LOCAL_TILE_H, PANEL());
        g.fill(tx, ty, tx + ComputerTerminalLayout.localTileW(), ty + 1, LINE());
    }

    /** What the machine can see of the network it is on, which is the other half of "is this all right". */
    private void renderNetworkPane(final GuiGraphics g) {
        final int px = PANE_X + 6;
        final int right = PANE_X + PANE_W - 6;
        Draw.text(g, font(), GameText.resolve(TerminalGridTexts.THE_NETWORK), px, PANE_Y + 6, DIM());
        final int net = menu.networkLinkState();
        final TextKey state = net == 2 ? TerminalGridTexts.TWO_ORCHESTRATORS
                : net == 1 ? TerminalGridTexts.JOINED : TerminalGridTexts.NOT_ON_ONE;
        Draw.text(g, font(), GameText.resolve(state), px, PANE_Y + 20,
                net == 2 ? RED() : net == 1 ? GREEN() : DIM());
        if (net != 1) {
            int y = PANE_Y + 36;
            for (final FormattedCharSequence line
                    : font().split(GameText.component(TerminalGridTexts.CABLE_HINT), PANE_W - 12)) {
                Draw.text(g, font(), line, px, y, DIM());
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
        Draw.text(g, font(), GameText.resolve(key), px, y, DIM());
        Draw.text(g, font(), value, right - font().width(value), y, TEXT());
    }
}
