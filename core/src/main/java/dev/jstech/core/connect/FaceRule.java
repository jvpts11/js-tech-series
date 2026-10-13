/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which faces of a block something is on, named from the block's own point of view: every face, or some of its
 * {@link RelativeFace}s, which turn with the block. A rule naming relative faces never matches a block that has no
 * facing, since such a block has no front to count from.
 */
public final class FaceRule {

    private final Set<RelativeFace> faces;
    private final boolean every;

    /** Every face of the block. */
    public static final FaceRule EVERY = new FaceRule(EnumSet.allOf(RelativeFace.class), true);
    /** The block's back only: where a computer takes its network cable. */
    public static final FaceRule BACK = of(RelativeFace.BACK);
    /** The block's front only. */
    public static final FaceRule FRONT = of(RelativeFace.FRONT);

    private FaceRule(final Set<RelativeFace> faces, final boolean every) {
        this.faces = faces;
        this.every = every;
    }

    /** These faces of the block, which turn with it. */
    public static FaceRule of(final RelativeFace first, final RelativeFace... more) {
        final EnumSet<RelativeFace> faces = EnumSet.of(Objects.requireNonNull(first, "first"), more);
        return new FaceRule(faces, false);
    }

    /** Whether the world's {@code face} of a block in {@code state} is one of this rule's. */
    public boolean matches(final BlockState state, final Direction face) {
        if (this.every) {
            return true;
        }
        final Optional<Direction> facing = RelativeFace.facingOf(state);
        return facing.isPresent() && this.faces.contains(RelativeFace.of(facing.get(), face));
    }
}
