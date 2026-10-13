/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.ChemicalSprite;
import dev.jstech.computers.client.FluidSprite;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.live.LiveGraphics;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws an item inside a desktop window.
 *
 * <p>An item is a model, not a sprite: {@code renderItem} lifts it {@link DesktopZ#ITEM_LIFT} in front of
 * whatever pose it is given, and the count another {@link DesktopZ#DECORATION_LIFT}. A window that drew its
 * items with the raw calls therefore put them ~150 deep in front of the entire desktop, where they covered
 * every window in front of that one, and two open windows painted their items over each other's chrome. These
 * helpers push the model back into the window's own depth band, so an item can never leave the window that
 * drew it. Every app draws its items through here; only the carried (cursor) stack, which is meant to ride
 * above everything, keeps the model's own lift, through {@link #carried}.
 *
 * <p>Each model is laid with its middle on a boundary between the screen's pixels, which the desktop's fractional
 * scale would otherwise leave anywhere.
 */
@PaletteHolder
public final class DesktopItems {

    /** A data cell's amount badge ink, {@code jsc:app/desktop_items}. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/desktop_items",
            new Colours(0xFFFFFFFF));

    private DesktopItems() {
    }

    /** The item's model, inside the current window's depth band. */
    public static void item(final GuiGraphics g, final ItemStack stack, final int x, final int y) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, DesktopZ.itemOffset());
        onPixelGrid(g, x, y);
        g.renderItem(stack, x, y);
        g.pose().popPose();
    }

    /** The stack on the cursor with its count, which rides above everything, at the model's own lift. */
    public static void carried(final GuiGraphics g, final Font font, final ItemStack stack, final int x, final int y) {
        g.pose().pushPose();
        onPixelGrid(g, x, y);
        g.renderItem(stack, x, y);
        g.renderItemDecorations(font, stack, x, y);
        g.pose().popPose();
    }

    /** The item's count (and durability bar), just in front of the model it belongs to. */
    public static void count(final GuiGraphics g, final Font font, final ItemStack stack, final int x, final int y) {
        count(g, font, stack, x, y, null);
    }

    /** As above, with the count text spelled out (a network total is not the stack's own size). */
    public static void count(final GuiGraphics g, final Font font, final ItemStack stack, final int x, final int y,
                             final String label) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, DesktopZ.countOffset());
        g.renderItemDecorations(font, stack, x, y, label);
        g.pose().popPose();
    }

    /** Both, for the common case of an item cell that shows its total. */
    public static void itemWithCount(final GuiGraphics g, final Font font, final ItemStack stack, final int x,
                                     final int y, final String label) {
        item(g, stack, x, y);
        count(g, font, stack, x, y, label);
    }

    /**
     * Any kind of data in a cell: an item's model, a fluid's still texture or a chemical's swatch, with an
     * optional amount badge. A sprite is flat, so it sits at the window's item depth directly instead of
     * needing the lift an item model gets compensated for.
     */
    public static void data(final GuiGraphics g, final Font font, final StorageKey key, final int x, final int y,
                            @Nullable final String label) {
        if (key.isItem()) {
            item(g, key.stack(1), x, y);
            if (label != null) {
                count(g, font, key.stack(1), x, y, label);
            }
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, DesktopZ.BAND_ITEM);
        if (key.isChemical()) {
            ChemicalSprite.draw(g, key, x, y);
        } else {
            FluidSprite.draw(g, key.fluidPrototype(), x, y);
        }
        g.pose().popPose();
        if (label != null) {
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, DesktopZ.BAND_COUNT);
            Draw.text(g, font, label, x + 17 - font.width(label), y + 9, PALETTE.get().badgeInk());
            g.pose().popPose();
        }
    }

    /*
     * Moves the model so its middle falls on a boundary between pixels. The desktop is drawn at a fraction of its
     * designed size, which can put the seam between two faces of a model right through the middle of a column of
     * pixels; those pixels then read the texture just past the face, which on a chest is clear, and the window showed
     * through the chest's front corner. With the middle on a boundary, as at the game's own whole-number scales, every
     * pixel lies inside a face. A model already there is not moved.
     */
    private static void onPixelGrid(final GuiGraphics g, final int x, final int y) {
        final Matrix4f pose = g.pose().last().pose();
        final double perUnit = g instanceof LiveGraphics live ? live.pixelsPerUnit()
                : Minecraft.getInstance().getWindow().getGuiScale();
        final double acrossX = pose.m00() * perUnit;
        final double acrossY = pose.m11() * perUnit;
        if (acrossX <= 0.0 || acrossY <= 0.0) {
            return;
        }
        final Vector3f middle = pose.transformPosition(x + 8.0F, y + 8.0F, 0.0F, new Vector3f());
        final double px = middle.x * perUnit;
        final double py = middle.y * perUnit;
        g.pose().translate((float) ((Math.rint(px) - px) / acrossX), (float) ((Math.rint(py) - py) / acrossY), 0.0F);
    }

    /** A data cell's amount badge ink. */
    private record Colours(int badgeInk) {
    }
}
