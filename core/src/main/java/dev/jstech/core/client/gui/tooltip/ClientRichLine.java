/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.tooltip.RichLine;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import org.jetbrains.annotations.ApiStatus;

/** A rich line of a tooltip drawn: its text in the tooltip's colour, its items and pictures in their places. */
@PaletteHolder
public final class ClientRichLine implements ClientTooltipComponent {

    private final RichLine line;

    /** The colour a rich line's text is drawn in, the game's tooltip white unless a resource pack says otherwise. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/rich_tooltip",
            new Colours(0xFFFFFFFF));
    /** How big an item's picture is in a line, and the room on each side of it. */
    private static final int ITEM = 16;
    private static final int ITEM_GAP = 1;
    private static final int TEXT_HEIGHT = 9;

    public ClientRichLine(final RichLine line) {
        this.line = line;
    }

    /** Hands the game what draws a rich line, once, from the Core's client constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientTooltipComponentFactoriesEvent.class,
                event -> event.register(RichLine.class, ClientRichLine::new));
    }

    @Override
    public int getHeight() {
        int tall = TEXT_HEIGHT;
        for (final RichLine.Piece piece : line.pieces()) {
            tall = Math.max(tall, switch (piece) {
                case RichLine.Piece.Text ignored -> TEXT_HEIGHT;
                case RichLine.Piece.Item ignored -> ITEM + 2;
                case RichLine.Piece.Image image -> image.height() + 2;
            });
        }
        return tall;
    }

    @Override
    public int getWidth(final Font font) {
        int wide = 0;
        for (final RichLine.Piece piece : line.pieces()) {
            wide += switch (piece) {
                case RichLine.Piece.Text text -> font.width(text.text());
                case RichLine.Piece.Item ignored -> ITEM + 2 * ITEM_GAP;
                case RichLine.Piece.Image image -> image.width() + 2 * ITEM_GAP;
            };
        }
        return wide;
    }

    @Override
    public void renderImage(final Font font, final int x, final int y, final GuiGraphics g) {
        final int tall = getHeight();
        int at = x;
        for (final RichLine.Piece piece : line.pieces()) {
            switch (piece) {
                case RichLine.Piece.Text text -> {
                    Draw.text(g, font, text.text(), at, y + (tall - TEXT_HEIGHT) / 2 + 1, PALETTE.get().text());
                    at += font.width(text.text());
                }
                case RichLine.Piece.Item item -> {
                    g.renderItem(item.stack(), at + ITEM_GAP, y + (tall - ITEM) / 2);
                    at += ITEM + 2 * ITEM_GAP;
                }
                case RichLine.Piece.Image image -> {
                    final int ix = at + ITEM_GAP;
                    final int iy = y + (tall - image.height()) / 2;
                    Draw.blended(() -> g.blitSprite(image.sprite(), ix, iy, image.width(), image.height()));
                    at += image.width() + 2 * ITEM_GAP;
                }
            }
        }
    }

    /**
     * The colours of a rich line.
     *
     * @param text its text's
     */
    public record Colours(int text) {
    }
}
