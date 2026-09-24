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
 * Algorithm for matching a {@link MultiblockPattern} against a world (abstracted as an {@link IBlockProvider}) at a candidate controller position.
 */
public final class PatternMatcher {

    private PatternMatcher() {
        // Utility class, no instances.
    }

    public static IMatchResult match(
            MultiblockPattern pattern,
            IBlockProvider provider,
            long controllerEncodedPos
    ) {
        // Try NORTH first; remember its failure for the debug-friendly fallback.
        IMatchResult.Failure firstFailure = null;
        for (Rotation rotation : Rotation.values()) {
            IMatchResult result = matchRotation(pattern, provider, controllerEncodedPos, rotation);
            if (result instanceof IMatchResult.Success) {
                return result;
            }
            if (firstFailure == null) {
                firstFailure = (IMatchResult.Failure) result;
            }
        }
        // No rotation matched. Return the NORTH-orientation failure for clarity.
        return firstFailure;
    }

    private static IMatchResult matchRotation(
            MultiblockPattern pattern,
            IBlockProvider provider,
            long controllerEncodedPos,
            Rotation rotation
    ) {
        int[] cWorld = decodePosition(controllerEncodedPos);
        int cwX = cWorld[0], cwY = cWorld[1], cwZ = cWorld[2];

        List<Long> slavePositions = new ArrayList<>();

        for (int py = 0; py < pattern.sizeY(); py++) {
            for (int pz = 0; pz < pattern.sizeZ(); pz++) {
                for (int px = 0; px < pattern.sizeX(); px++) {
                    char c = pattern.charAt(px, py, pz);

                    // Pattern-local offset from controller.
                    int relX = px - pattern.controllerX();
                    int relY = py - pattern.controllerY();
                    int relZ = pz - pattern.controllerZ();

                    // Apply rotation in the horizontal plane (Y unaffected).
                    int[] rotated = rotation.transform(relX, relZ);
                    int worldDx = rotated[0];
                    int worldDz = rotated[1];

                    int worldX = cwX + worldDx;
                    int worldY = cwY + relY;
                    int worldZ = cwZ + worldDz;
                    long worldEncoded = encodePosition(worldX, worldY, worldZ);

                    if (c == MultiblockPattern.IGNORE_CHAR) {
                        // Ignored slot, no validation, no slave registration.
                        continue;
                    }

                    String actualBlockId = provider.blockAt(worldEncoded);
                    if (actualBlockId == null) {
                        actualBlockId = "minecraft:air";
                    }

                    if (c == MultiblockPattern.CONTROLLER_CHAR) {
                        // Controller slot, must be at the controller's own
                        continue;
                    }

                    // Regular slot, must satisfy the matcher.
                    IBlockMatcher matcher = pattern.mapping().get(c);
                    /*
                     * Builder validation guarantees mapping is present, but
                     * be defensive in case of ill-constructed patterns.
                     */
                    if (matcher == null || !matcher.matches(actualBlockId)) {
                        return new IMatchResult.Failure(
                                relX, relY, relZ,
                                c,
                                actualBlockId
                        );
                    }
                    slavePositions.add(worldEncoded);
                }
            }
        }

        return new IMatchResult.Success(rotation, slavePositions);
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
