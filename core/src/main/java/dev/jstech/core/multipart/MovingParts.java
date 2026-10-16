/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

/**
 * The block spaces on the player's game that hold a part of a kind that moves ({@link PartType#moving()}): the only
 * ones drawn each frame besides the world's mesh, so a world of still parts costs nothing frame to frame. A space notes
 * itself as its parts reach the player's game; one that is gone, or holds no moving part any more, is dropped as the
 * spaces are drawn. Only the player's game uses it, on its main thread.
 */
public final class MovingParts {

    private static final Set<BlockPos> SPACES = new HashSet<>();

    private MovingParts() {
    }

    /** Notes whether the space at {@code pos} holds a part that moves. */
    public static void note(final BlockPos pos, final boolean moving) {
        if (moving) {
            SPACES.add(pos.immutable());
        } else {
            SPACES.remove(pos);
        }
    }

    /** Whether the space at {@code pos} is noted as holding a part that moves. */
    public static boolean noted(final BlockPos pos) {
        return SPACES.contains(pos);
    }

    /** Hands each noted space to {@code drawn}, which says whether it still holds a part that moves; the rest go. */
    public static void visit(final Predicate<BlockPos> drawn) {
        SPACES.removeIf(pos -> !drawn.test(pos));
    }

    /** Forgets every space, as the player leaves the world. */
    public static void clear() {
        SPACES.clear();
    }
}
