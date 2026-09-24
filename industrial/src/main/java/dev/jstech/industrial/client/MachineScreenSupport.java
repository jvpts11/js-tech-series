/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared placeholder drawing for the Industrial machine screens, and their colours: the palette
 * {@code jsindustrial:machine/screen}, which a resource pack can recolour.
 */
@PaletteHolder
final class MachineScreenSupport {

    private static final Palette<Colours> PALETTE = Palettes.declare(JsIndustrial.MODID, "machine/screen",
            new Colours(0xFFC6C6C6, 0xFFFFFFFF, 0xFF555555, 0xFF373737, 0xFF8B8B8B, 0xFF200000, 0xFFFF3030,
                    0xFF555555, 0xFFFF9020, 0xFF4488FF, 0xFFFF8000, 0xFF3DCC3D));

    private MachineScreenSupport() {
    }

    /** The machine screens' colours as the loaded palette has them now. */
    static Colours colours() {
        return PALETTE.get();
    }

    static void drawPanel(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final Colours c = colours();
        g.fill(x, y, x + w, y + h, c.panel());
        g.fill(x, y, x + w, y + 1, c.bevelLight());
        g.fill(x, y, x + 1, y + h, c.bevelLight());
        g.fill(x, y + h - 1, x + w, y + h, c.bevelDark());
        g.fill(x + w - 1, y, x + w, y + h, c.bevelDark());
    }

    static void drawSlot(final GuiGraphics g, final int x, final int y) {
        final Colours c = colours();
        g.fill(x - 1, y - 1, x + 17, y + 17, c.slotBorder());
        g.fill(x, y, x + 16, y + 16, c.slotFill());
    }

    static void drawPlayerInventory(final GuiGraphics g, final int left, final int top) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(g, left + 8 + col * 18, top + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(g, left + 8 + col * 18, top + 142);
        }
    }

    static void drawEnergyBar(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final int energy, final int maxEnergy) {
        final Colours c = colours();
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c.slotBorder());
        g.fill(x, y, x + w, y + h, c.energyEmpty());
        if (maxEnergy > 0 && energy > 0) {
            final int filled = Math.min(h, energy * h / maxEnergy);
            g.fill(x, y + h - filled, x + w, y + h, c.energyFull());
        }
    }

    static void drawProgressBar(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final int progress, final int maxProgress, final int fillColor) {
        g.fill(x, y, x + w, y + h, colours().slotFill());
        if (maxProgress > 0 && progress > 0) {
            final int filled = Math.min(w, progress * w / maxProgress);
            g.fill(x, y, x + filled, y + h, fillColor);
        }
    }

    /**
     * The machine screens' colours: the panel and its bevel, a slot's border and fill, the energy bar empty and
     * full, the generator's flame unlit and lit, and each processing machine's progress.
     */
    record Colours(int panel, int bevelLight, int bevelDark, int slotBorder, int slotFill, int energyEmpty,
                   int energyFull, int flameEmpty, int flameFull, int compressorProgress, int furnaceProgress,
                   int maceratorProgress) {
    }
}
