/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a {@link MultiblockPattern} against a world (abstracted as an {@link IBlockProvider}) at a candidate
 * terminal position, in the older form a terminal of four turns reads: the pattern's own {@link
 * MultiblockPattern#check} says all a formed structure counts and gives.
 */
public final class PatternMatcher {

    private PatternMatcher() {
        // Utility class, no instances.
    }

    /**
     * The match: every place of the structure but the terminal's, its inside with them, and its ports; or the
     * wrong place of the attempt that went furthest, the four turns tried first, north first. A structure found
     * mirrored or lying down reads as north.
     */
    public static IMatchResult match(
            MultiblockPattern pattern,
            IBlockProvider provider,
            long terminalEncodedPos
    ) {
        return switch (pattern.check(provider, terminalEncodedPos)) {
            case ShapeMatch.Formed formed -> {
                final List<Long> slaves = new ArrayList<>(formed.parts());
                slaves.addAll(formed.inside());
                yield new IMatchResult.Success(formed.orientation().rotation().orElse(Rotation.NORTH), slaves,
                        formed.ports());
            }
            case ShapeMatch.Failure failure -> new IMatchResult.Failure(failure.relX(), failure.relY(),
                    failure.relZ(), failure.wanted().length() == 1 ? failure.wanted().charAt(0) : '?',
                    failure.found());
        };
    }

    // Position encoding (Phase 0 only)

    public static long encodePosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38
                | ((long) y & 0xFFFL)
                | ((long) z & 0x3FFFFFFL) << 12;
    }

    public static int[] decodePosition(long encoded) {
        // Each field is moved up to the top of the long and shifted back down arithmetically, which sign-extends it:
        // x already sits at the top (26 bits), z is the 26 bits under it and y the 12 at the bottom.
        final int x = (int) (encoded >> 38);
        final int y = (int) (encoded << 52 >> 52);
        final int z = (int) (encoded << 26 >> 38);

        return new int[] { x, y, z };
    }
}
