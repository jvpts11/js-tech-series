/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The pictures a desktop draws in its own style: a program's icon, a file's, a system's emblem. Each is a file at
 * {@code textures/gui/<set>/<name>/<skin>.png}, one per look, so a picture is changed by replacing a file and an
 * addon adds one by shipping a file at that path, with no code.
 *
 * <p>Not every look has every picture, and none has to: a desktop's period look falls back to its modern one, then
 * to the oldest Frames edition's, which every picture has, then to the set's stand-in in the same order. Nothing is
 * ever drawn as the missing-texture checker.
 */
public final class SkinSprites {

    /** The look every picture has, which is where a missing one falls back to. */
    public static final String BASE_SKIN = "frames_95";

    /** Marks the look a desktop wore on Legacy-era hardware. */
    public static final String PERIOD_SUFFIX = "_legacy";

    private static final String NS = "jsc";

    /*
     * Whether a file is there, asked once per path: the fallback walks several paths a frame, and the answer
     * never changes while the game runs.
     */
    private static final Map<ResourceLocation, Boolean> PRESENT = new HashMap<>();

    private SkinSprites() {
    }

    /**
     * The picture to draw for {@code name} in {@code set} under a look: its own, or the nearest one there is.
     *
     * @param fallback the set's stand-in, used when {@code name} has no picture at all
     * @param skin     the look, a Frames edition or a desktop's id, empty for the base one
     */
    public static ResourceLocation find(final String set, final String name, final String fallback,
                                        final String skin) {
        final String look = skin == null || skin.isEmpty() ? BASE_SKIN : skin;
        final ResourceLocation own = firstOf(set, name, look);
        if (own != null) {
            return own;
        }
        final ResourceLocation standIn = firstOf(set, fallback, look);
        return standIn != null ? standIn : texture(set, fallback, BASE_SKIN);
    }

    /**
     * Draws a picture authored {@code size} pixels square, centred in the box and never wider or taller than it.
     * It is drawn 1:1 when the box has room, and shrunk to the box only when it has not.
     */
    public static void draw(final GuiGraphics g, final ResourceLocation texture, final int x, final int y,
                            final int w, final int h, final int size) {
        final int side = Math.min(size, Math.min(w, h));
        final int ox = x + (w - side) / 2;
        final int oy = y + (h - side) / 2;
        if (side == size) {
            g.blit(texture, ox, oy, 0.0F, 0.0F, size, size, size, size);
        } else {
            g.blit(texture, ox, oy, side, side, 0.0F, 0.0F, size, size, size, size);
        }
    }

    /** Whether a picture is there, asked of the game's resources once per path. */
    static boolean exists(final ResourceLocation tex) {
        return PRESENT.computeIfAbsent(tex,
                t -> Minecraft.getInstance().getResourceManager().getResource(t).isPresent());
    }

    /** The first of the look's own picture, its modern look's for a period one, and the base look's. */
    private static ResourceLocation firstOf(final String set, final String name, final String look) {
        ResourceLocation tex = texture(set, name, look);
        if (exists(tex)) {
            return tex;
        }
        if (look.endsWith(PERIOD_SUFFIX)) {
            tex = texture(set, name, look.substring(0, look.length() - PERIOD_SUFFIX.length()));
            if (exists(tex)) {
                return tex;
            }
        }
        tex = texture(set, name, BASE_SKIN);
        return exists(tex) ? tex : null;
    }

    private static ResourceLocation texture(final String set, final String name, final String look) {
        return ResourceLocation.fromNamespaceAndPath(NS, "textures/gui/" + set + "/" + name + "/" + look + ".png");
    }
}
