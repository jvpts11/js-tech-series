/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.index.IndexHealth;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The Maintenance tab (Mainframe-only): index stats and the ANALYZE / VACUUM / REINDEX / DROP actions.
 */
@PaletteHolder
final class MaintenanceTerminalTab extends AbstractTerminalTab {

    // Layout constants, mirroring ComputerTerminalScreen; update together if layout changes.
    private static final int MNT_TILE_ROW1_Y = 32;
    private static final int MNT_TILE_ROW2_Y = 56;
    private static final int MNT_TILE_H = 22;
    private static final int MNT_ACTIONS_Y = 82;
    /*
     * The health strip takes the ACTIONS caption's line when the index needs attention: the state of
     * the index is worth more than a decorative label, and the layout below stays where it was.
     */
    private static final int MNT_HEALTH_H = 10;
    private static final int MNT_BTN_ROW1_Y = 94;
    private static final int MNT_BTN_REINDEX_Y = 112;
    private static final int MNT_BTN_DROP_Y = 130;
    private static final int MNT_BTN_H = 15;

    /** The tab's own colours, {@code jsc:terminal/maintenance}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "terminal/maintenance",
            new Colours(0xFF7A3A14, 0xFF6E5A16, 0x55FFFFFF, 0xFFFFFFFF, 0xFFFFE0A0,
                    0xFF1C6F86, 0xFF2A93AE, 0xFF7A5A1E, 0xFFA8801F, 0xFF7A241C, 0xFFB23228, 0x33FFFFFF));

    MaintenanceTerminalTab(final ComputerTerminalScreen screen, final ComputerTerminalMenu menu) {
        super(screen, menu);
    }

    @Override
    public void renderTabBg(final GuiGraphics g, final int x, final int y,
                            final int cx, final int cy, final int cw,
                            final int mouseX, final int mouseY, final float partialTick) {
        final int tileW = (cw - 4) / 2;
        for (int r = 0; r < 2; r++) {
            final int ty = cy + (r == 0 ? MNT_TILE_ROW1_Y : MNT_TILE_ROW2_Y);
            for (int col = 0; col < 2; col++) {
                final int tx = cx + col * (tileW + 4);
                g.fill(tx, ty, tx + tileW, ty + MNT_TILE_H, PANEL());
                g.fill(tx, ty, tx + tileW, ty + 1, LINE());
            }
        }
        final Colours c = PALETTE.get();
        final var health = menu.indexHealth();
        if (health != IndexHealth.State.OK) {
            final int stripY = cy + MNT_ACTIONS_Y - 1;
            final int base = health == IndexHealth.State.FRAGMENTED ? c.fragmented() : c.stale();
            g.fill(cx, stripY, cx + cw, stripY + MNT_HEALTH_H, base);
            g.fill(cx, stripY, cx + cw, stripY + 1, c.stripLight());
        }
        final int halfW = (cw - 4) / 2;
        maintBtnBg(g, mouseX, mouseY, cx, cy + MNT_BTN_ROW1_Y, halfW, c.tidy(), c.tidyHover());
        maintBtnBg(g, mouseX, mouseY, cx + halfW + 4, cy + MNT_BTN_ROW1_Y, halfW, c.tidy(), c.tidyHover());
        maintBtnBg(g, mouseX, mouseY, cx, cy + MNT_BTN_REINDEX_Y, cw, c.rebuild(), c.rebuildHover());
        maintBtnBg(g, mouseX, mouseY, cx, cy + MNT_BTN_DROP_Y, cw, c.drop(), c.dropHover());
    }

    @Override
    public void renderTabLabels(final GuiGraphics g, final int cx, final int cy, final int cw) {
        g.drawString(font(), GameText.resolve(TerminalUpkeepTexts.STORAGE_INDEX), cx, cy + 20, DIM(), false);
        final int tileW = (cw - 4) / 2;
        tile(g, cx, cy + MNT_TILE_ROW1_Y, GameText.resolve(TerminalUpkeepTexts.TYPES), fmt(menu.indexedTypes()), "");
        tile(g, cx + tileW + 4, cy + MNT_TILE_ROW1_Y, GameText.resolve(TerminalUpkeepTexts.SERVERS),
                String.valueOf(menu.indexedServers()), "");
        tile(g, cx, cy + MNT_TILE_ROW2_Y, GameText.resolve(TerminalUpkeepTexts.LOCKS),
                String.valueOf(menu.activeLocks()), "");
        final long used = menu.networkStorageUsed();
        final long total = menu.networkStorageTotal();
        tile(g, cx + tileW + 4, cy + MNT_TILE_ROW2_Y, GameText.resolve(TerminalUpkeepTexts.STORAGE),
                total <= 0 ? "0" : fmt(used) + "/" + fmt(total), "");
        final var health = menu.indexHealth();
        if (health == IndexHealth.State.OK) {
            g.drawString(font(), GameText.resolve(TerminalUpkeepTexts.ACTIONS), cx, cy + MNT_ACTIONS_Y, DIM(), false);
        } else {
            // Name the state, how many item types are in doubt, and the run that settles it.
            final int types = menu.indexHealthTypes();
            final boolean fragmented = health == IndexHealth.State.FRAGMENTED;
            final String action = fragmented ? "VACUUM" : "REINDEX";
            final TextKey state = fragmented ? TerminalUpkeepTexts.FRAGMENTED : TerminalUpkeepTexts.STALE;
            final String affected = GameText.resolve(
                    (types == 1 ? TerminalUpkeepTexts.ONE_AFFECTED : TerminalUpkeepTexts.AFFECTED).with(state, types));
            g.drawString(font(), affected, cx + 2, cy + MNT_ACTIONS_Y, PALETTE.get().ink(), false);
            final String hint = GameText.resolve(TerminalUpkeepTexts.RUN.with(action));
            g.drawString(font(), hint, cx + cw - 2 - font().width(hint), cy + MNT_ACTIONS_Y,
                    PALETTE.get().hint(), false);
        }
        final int ink = PALETTE.get().ink();
        final int halfW = (cw - 4) / 2;
        g.drawCenteredString(font(), "ANALYZE", cx + halfW / 2, cy + MNT_BTN_ROW1_Y + 4, ink);
        g.drawCenteredString(font(), "VACUUM", cx + halfW + 4 + halfW / 2, cy + MNT_BTN_ROW1_Y + 4, ink);
        g.drawCenteredString(font(), "REINDEX", cx + cw / 2, cy + MNT_BTN_REINDEX_Y + 4, ink);
        g.drawCenteredString(font(), GameText.resolve(TerminalUpkeepTexts.DROP_DATA_BUTTON), cx + cw / 2,
                cy + MNT_BTN_DROP_Y + 4, ink);
        if (!screen.maintHint.isEmpty()) {
            g.drawString(font(), GameText.resolve(screen.maintHint), cx, cy + MNT_BTN_DROP_Y + MNT_BTN_H + 2,
                    ACCENT(), false);
        }
    }

    private void maintBtnBg(final GuiGraphics g, final int mx, final int my,
                            final int bx, final int by, final int w,
                            final int base, final int hover) {
        final boolean hov = inRect(mx, my, bx, by, w, MNT_BTN_H);
        g.fill(bx, by, bx + w, by + MNT_BTN_H, hov ? hover : base);
        g.fill(bx, by, bx + w, by + 1, PALETTE.get().buttonLight());
    }

    /**
     * The tab's colours: the strip of an index that is fragmented or stale and the light along its top, the words
     * on it and the run it asks for, the buttons that tidy, rebuild and drop the index with their hovers, and the
     * light along every button's top.
     */
    private record Colours(int fragmented, int stale, int stripLight, int ink, int hint, int tidy, int tidyHover,
                           int rebuild, int rebuildHover, int drop, int dropHover, int buttonLight) {
    }
}
