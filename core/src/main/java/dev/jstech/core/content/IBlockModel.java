/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import org.jetbrains.annotations.Nullable;

/**
 * One model file a block's look is made of, described rather than built: a declaration says what a block looks like
 * without touching the game's client classes, which a dedicated server does not have, and the generator builds the
 * file from the description.
 *
 * <p>A texture is named the way a model file names it: {@code "block/monitor_side"} for one of the declaring mod's
 * own, {@code "minecraft:block/glass"} for another's.
 */
public sealed interface IBlockModel
        permits IBlockModel.CubeAll,
        IBlockModel.Column,
        IBlockModel.BottomTop,
        IBlockModel.Orientable,
        IBlockModel.SixFaces,
        IBlockModel.ParticleOnly,
        IBlockModel.Handmade {

    /** The model's name, which is also its file: {@code models/block/<name>.json}. */
    String name();

    /** The block's own texture on all six faces. */
    static IBlockModel cubeAll(final String name) {
        return new CubeAll(name, "block/" + name);
    }

    /** A pillar: {@code <name>_side} around, {@code <name>_top} on both ends. */
    static IBlockModel column(final String name) {
        return new Column(name, "block/" + name + "_side", "block/" + name + "_top");
    }

    /** A front, the same side on the other faces, and a top: {@code <name>_front}, {@code _side}, {@code _top}. */
    static IBlockModel orientable(final String name) {
        return orientable(name, "block/" + name + "_side", "block/" + name + "_front", "block/" + name + "_top");
    }

    static IBlockModel orientable(final String name, final String side, final String front, final String top) {
        return new Orientable(name, side, front, top);
    }

    /** The same texture on all six faces. */
    record CubeAll(String name, String texture) implements IBlockModel {
    }

    /** Sides around and ends on top and bottom, as a pillar. */
    record Column(String name, String side, String end) implements IBlockModel {
    }

    /**
     * Sides, a bottom and a top, with the render type a block needs when it can be seen through (a tank's glass);
     * {@code null} keeps the default.
     */
    record BottomTop(String name, String side, String bottom, String top, @Nullable String renderType)
            implements IBlockModel {
    }

    /** A front, the same texture on the other sides, and a top: a machine that faces the way it was placed. */
    record Orientable(String name, String side, String front, String top) implements IBlockModel {
    }

    /** Each face its own texture, and the texture a break scatters. */
    record SixFaces(String name, String down, String up, String north, String south, String east, String west,
                    String particle) implements IBlockModel {
    }

    /**
     * Nothing drawn, only the texture a break scatters: the model of a block whose body a block entity renders.
     * Several blocks may share one.
     */
    record ParticleOnly(String name, String particle) implements IBlockModel {
    }

    /** A model written by hand in the mod's resources, used as it is. */
    record Handmade(String name) implements IBlockModel {
    }
}
