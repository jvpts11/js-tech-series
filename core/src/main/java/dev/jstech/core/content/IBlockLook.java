/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * How a block looks in the world: which model each of its states shows, and how that model is turned. The generator
 * writes the block state file and the models from it.
 */
public sealed interface IBlockLook
        permits IBlockLook.Fixed,
        IBlockLook.Facing,
        IBlockLook.Pipe {

    /** The block's own texture on every face. */
    static IBlockLook cubeAll(final String id) {
        return fixed(IBlockModel.cubeAll(id));
    }

    /** A pillar of the block's {@code _side} and {@code _top} textures. */
    static IBlockLook column(final String id) {
        return fixed(IBlockModel.column(id));
    }

    /** A machine facing the way it was placed, from its {@code _front}, {@code _side} and {@code _top} textures. */
    static IBlockLook orientable(final String id) {
        return facing(IBlockModel.orientable(id));
    }

    /** One model, whatever the state. */
    static IBlockLook fixed(final IBlockModel model) {
        return new Fixed(model);
    }

    /** One model, turned so its front looks the way the block's horizontal facing points. */
    static Facing facing(final IBlockModel model) {
        return new Facing(model, Facing.FRONT_TOWARD_FACING, null);
    }

    /**
     * A cable: a core in the middle, and an arm toward each of the six sides it connects to, drawn from the core and
     * arm models given (which take the cable's texture as {@code #cable}).
     */
    static IBlockLook pipe(final String texture, final String coreParent, final String armParent) {
        return new Pipe(texture, coreParent, armParent);
    }

    /** Every state shows the one model. */
    record Fixed(IBlockModel model) implements IBlockLook {
    }

    /**
     * The model turned to the block's horizontal facing: {@code angleOffset} is added to the facing's own angle, 180
     * for a model whose front is its north face to look the way the block faces. A toggle swaps in another model
     * while a property of the block is on (a screen lit, a drive loaded).
     */
    record Facing(IBlockModel model, int angleOffset, @Nullable Toggle toggle) implements IBlockLook {

        static final int FRONT_TOWARD_FACING = 180;
        static final int FRONT_AGAINST_FACING = 0;

        /** The same look with its front on the side the facing points away from (a screen facing its placer). */
        public Facing frontAgainstFacing() {
            return new Facing(model, FRONT_AGAINST_FACING, toggle);
        }

        /** The same look, showing that model instead while that property is on. */
        public Facing whileOn(final BooleanProperty property, final IBlockModel on) {
            return new Facing(model, angleOffset, new Toggle(property, on));
        }
    }

    /** A cable drawn as a core with an arm toward each connected side, from the six sides of a pipe block. */
    record Pipe(String texture, String coreParent, String armParent) implements IBlockLook {
    }

    /** Another model while a property of the block is on. */
    record Toggle(BooleanProperty property, IBlockModel on) {
    }
}
