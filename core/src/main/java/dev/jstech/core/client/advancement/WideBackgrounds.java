/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.advancement;

import dev.jstech.core.gui.ParallaxWindow;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Advancement tabs whose background is one picture wider than the tab's window, drawn once behind the tree in place of
 * the game's 16-pixel tiles and sliding slower than the tree as it is dragged ({@link ParallaxWindow}), so it never
 * repeats and the window never reaches its edge.
 *
 * <p>An advancement names its tile as its background, as any does; a mod declares, on the player's game, the wide
 * picture that tile stands for. A tab whose tile has no wide picture is tiled as the game tiles it, and so is every tab
 * where the Core is not installed, so the tile should be a fair small piece of the wide picture.
 *
 * <pre>{@code
 * WideBackgrounds.declare("mymod:textures/gui/advancements/sky.png",
 *         "mymod:textures/gui/advancements/sky_wide.png", 400, 200);
 * }</pre>
 */
public final class WideBackgrounds {

    /** The tab's window, as the game draws it. */
    public static final int WINDOW_WIDTH = 234;
    public static final int WINDOW_HEIGHT = 113;

    private static final Map<ResourceLocation, Wide> BY_TILE = new ConcurrentHashMap<>();

    private WideBackgrounds() {
    }

    /**
     * Draws the tabs whose background is {@code tile} with the picture {@code picture}, {@code width} by {@code height}
     * pixels, both written as an advancement writes its background ({@code "mymod:textures/gui/sky.png"}).
     *
     * @throws IllegalArgumentException when the picture is smaller than the tab's window, which it has to fill
     */
    public static void declare(final String tile, final String picture, final int width, final int height) {
        if (width < WINDOW_WIDTH || height < WINDOW_HEIGHT) {
            throw new IllegalArgumentException("The wide background " + picture + " is " + width + " by " + height
                    + " pixels; an advancement tab's window is " + WINDOW_WIDTH + " by " + WINDOW_HEIGHT
                    + ", and the picture has to fill it.");
        }
        BY_TILE.put(ResourceLocation.parse(tile), new Wide(ResourceLocation.parse(picture), width, height));
    }

    /** The wide picture declared for the tile {@code tile}, or null for a tile drawn as the game draws it. */
    public static @Nullable Wide of(final ResourceLocation tile) {
        return BY_TILE.get(tile);
    }

    /**
     * A wide picture: its texture and its size.
     *
     * @param texture the picture
     * @param width   its width in pixels
     * @param height  its height in pixels
     */
    public record Wide(ResourceLocation texture, int width, int height) {

        /**
         * Draws the part of the picture the tree's scroll shows, filling the window from its corner; the tree stands
         * at {@code scrollX}, {@code scrollY}, having started at {@code startX}, {@code startY}.
         */
        public void draw(final GuiGraphics graphics, final double scrollX, final double scrollY, final double startX,
                         final double startY) {
            final int u = ParallaxWindow.offset(scrollX, startX, width, WINDOW_WIDTH);
            final int v = ParallaxWindow.offset(scrollY, startY, height, WINDOW_HEIGHT);
            graphics.blit(texture, 0, 0, u, v, WINDOW_WIDTH, WINDOW_HEIGHT, width, height);
        }
    }
}
