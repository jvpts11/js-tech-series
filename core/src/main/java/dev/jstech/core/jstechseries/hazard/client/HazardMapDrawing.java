/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import com.mojang.math.Axis;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.hazard.HazardMapsClient;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.HazardMap;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.ChunkPos;

/**
 * A map of hazards drawn: the land round a place, each chunk the map knows coloured by the step its reading reached,
 * what was never read greyed, the chunks' grid, and the player's arrow; on the device, a sweep going round while it
 * reads. The same drawing serves the device's little screen and the whole map.
 */
@PaletteHolder
public final class HazardMapDrawing {

    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "hazard_map", new Colours(
            0xFF << 24 | 0xE01E1E, 0xFF << 24 | 0xE8501E, 0xFF << 24 | 0xF08A30, 0xFF << 24 | 0xF8B840,
            0xFF << 24 | 0xF0DC60, 0xFF << 24 | 0x5A9A3A, 0xFF << 24 | 0x808080, 0xA0 << 24 | 0x383838, 0x28 << 24,
            0xFF << 24 | 0xFFFFFF, 0x78 << 24 | 0x78FFA0));
    /* How opaque a step's colour is laid over the land, out of 255, so the land shows through. */
    private static final int STEP_ALPHA = 0x90;

    private HazardMapDrawing() {
    }

    /**
     * Draws the map in the rectangle at {@code (x, y)}, {@code w} by {@code h}, {@code cell} pixels a chunk, centred
     * on {@code (centreX, centreZ)} in chunks, with {@code kind}'s readings over {@code picture}'s land; the player's
     * arrow at {@code (playerX, playerZ)}, in chunks, turned {@code yaw} degrees as the game turns them; a sweep at
     * {@code sweep} turns round, if not negative.
     */
    public static void draw(final GuiGraphics g, final TerrainPicture picture, final int x, final int y, final int w,
                            final int h, final float cell, final double centreX, final double centreZ,
                            final Optional<HazardKind> kind, final double playerX, final double playerZ,
                            final float yaw, final double sweep) {
        final Colours c = PALETTE.get();
        final double across = w / (double) cell;
        final double down = h / (double) cell;
        final double fromX = centreX - across / 2;
        final double fromZ = centreZ - down / 2;
        Draw.pushScissor(g, x, y, x + w, y + h);
        picture.draw(g, x, y, cell, fromX, fromZ, across, down);
        for (int cz = (int) Math.floor(fromZ); cz < fromZ + down; cz++) {
            for (int cx = (int) Math.floor(fromX); cx < fromX + across; cx++) {
                final int sx = x + (int) Math.round((cx - fromX) * cell);
                final int sy = y + (int) Math.round((cz - fromZ) * cell);
                final int ex = x + (int) Math.round((cx + 1 - fromX) * cell);
                final int ey = y + (int) Math.round((cz + 1 - fromZ) * cell);
                final long chunk = ChunkPos.asLong(cx, cz);
                final int overlay = kind.map(found -> overlay(found, chunk, c)).orElse(c.unread());
                if (overlay != 0) {
                    g.fill(sx, sy, ex, ey, overlay);
                }
                g.fill(sx, sy, ex, sy + 1, c.grid());
                g.fill(sx, sy, sx + 1, ey, c.grid());
            }
        }
        final int ax = x + (int) Math.round((playerX - fromX) * cell);
        final int ay = y + (int) Math.round((playerZ - fromZ) * cell);
        if (sweep >= 0) {
            final double reach = Math.min(w, h) * 0.4;
            for (int q = 0; q < reach; q++) {
                final int px = ax + (int) Math.round(q * Math.cos(sweep));
                final int py = ay + (int) Math.round(q * Math.sin(sweep));
                g.fill(px, py, px + 1, py + 1, c.sweep());
            }
        }
        arrow(g, ax, ay, yaw, c.arrow());
        Draw.popScissor(g);
    }

    /** The colour of the step {@code step} of {@code kind}'s map, its lowest 0; for its legend. */
    public static int stepColour(final HazardKind kind, final int step) {
        final Colours c = PALETTE.get();
        final List<Integer> steps = List.of(c.hottest(), c.hot(), c.warm(), c.mild(), c.mildest());
        final int fromTop = kind.mapSteps().size() - 1 - step;
        return steps.get(Math.clamp(fromTop, 0, steps.size() - 1));
    }

    /* The colour laid over the chunk {@code chunk} for {@code kind}: its step's, none when normal, grey unread. */
    private static int overlay(final HazardKind kind, final long chunk, final Colours c) {
        final Optional<HazardMap.Reading> reading = HazardMapsClient.reading(kind.id(), chunk);
        if (reading.isEmpty()) {
            return c.unread();
        }
        final int step = kind.mapStep(reading.get().value());
        return step < 0 ? 0 : STEP_ALPHA << 24 | stepColour(kind, step) & 0xFFFFFF;
    }

    /* The player's arrow: a small white chevron pointing the way they look. */
    private static void arrow(final GuiGraphics g, final int x, final int y, final float yaw, final int colour) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(yaw + 180));
        g.fill(0, -3, 1, -2, colour);
        g.fill(-1, -2, 2, -1, colour);
        g.fill(-2, -1, 3, 0, colour);
        g.fill(-2, 0, 0, 1, colour);
        g.fill(1, 0, 3, 1, colour);
        g.pose().popPose();
    }

    /**
     * The colours of a map of hazards.
     *
     * @param hottest the highest step's colour
     * @param hot     the step below it
     * @param warm    the step below that
     * @param mild    the step below that
     * @param mildest the fifth step from the top, and any below it
     * @param normal  a reading below the first step, in the legend
     * @param never   a chunk never read, in the legend
     * @param unread  laid over a chunk never read
     * @param grid    the chunks' lines
     * @param arrow   the player's arrow
     * @param sweep   the device's sweep
     */
    public record Colours(int hottest, int hot, int warm, int mild, int mildest, int normal, int never, int unread,
                          int grid, int arrow, int sweep) {
    }
}
