/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.core.tier.HardwareEra;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Where the front of a small computer's case carries what a click works: its power button, its 5.25-inch bays and the
 * eject button of the drive in each, and its USB ports. Every edge is in sixty-fourths of the block's front, counted
 * from its top left as the front is seen, as {@link FrontArea} and {@link EjectButton} measure, and each is where the
 * case's model draws the part, so a click lands on what the player sees.
 *
 * <p>The case's front is drawn thirty texels across, set a texel in from each side of the block, and thirty-two down,
 * so a texel is two sixty-fourths. Free of Minecraft types, so where each part lies is testable without the game.
 *
 * @param power the power button
 * @param bays  each 5.25-inch bay, top first: the bezel of the part in it
 * @param ports each USB port, top first: its socket
 */
public record CaseFrontLayout(FrontArea power, List<FrontArea> bays, List<FrontArea> ports) {

    /** Sixty-fourths to a texel of the case's front. */
    public static final int UNIT = 2;
    /* How tall a 5.25-inch bay is, in texels. */
    private static final int BAY_HEIGHT = 5;
    /* How big a USB-A socket is, in texels. */
    private static final int SOCKET_WIDTH = 3;
    private static final int SOCKET_HEIGHT = 2;

    private static final CaseFrontLayout VINTAGE = front(texels(23, 15, 4, 6), 4, 26, new int[] {2}, new int[0][]);
    private static final CaseFrontLayout LEGACY = front(texels(12, 18, 6, 5), 4, 26, new int[] {2, 8}, new int[0][]);
    private static final CaseFrontLayout TRANSITION = front(texels(16, 15, 6, 6), 5, 25, new int[] {2, 8},
            new int[][] {{5, 14}, {5, 18}});
    /* The Standard Neutral and High Performance cases share one front; the Aesthetic's bay is a texel narrower. */
    private static final CaseFrontLayout STANDARD = front(texels(23, 9, 4, 4), 4, 26, new int[] {3},
            new int[][] {{3, 8}, {3, 12}});
    private static final CaseFrontLayout STANDARD_AESTHETIC = front(texels(23, 9, 4, 4), 5, 25, new int[] {3},
            new int[][] {{3, 8}, {3, 12}});
    /* The Advanced cases have no bay; the Aesthetic's glass front keeps its ports a texel further in. */
    private static final CaseFrontLayout ADVANCED = front(texels(13, 2, 4, 4), 4, 26, new int[0],
            new int[][] {{3, 2}, {3, 6}});
    private static final CaseFrontLayout ADVANCED_AESTHETIC = front(texels(13, 2, 4, 4), 4, 26, new int[0],
            new int[][] {{4, 2}, {4, 6}});

    public CaseFrontLayout {
        bays = List.copyOf(bays);
        ports = List.copyOf(ports);
    }

    /** The front of the case of {@code era} in {@code style}. */
    public static CaseFrontLayout of(final HardwareEra era, final CaseStyle style) {
        final boolean aesthetic = style == CaseStyle.AESTHETIC;
        return switch (era) {
            case VINTAGE -> VINTAGE;
            case LEGACY -> LEGACY;
            case TRANSITION -> TRANSITION;
            case STANDARD -> aesthetic ? STANDARD_AESTHETIC : STANDARD;
            case ADVANCED, EXA, SINGULARITY -> aesthetic ? ADVANCED_AESTHETIC : ADVANCED;
        };
    }

    /** The part at texel {@code x}, {@code y} of a case's front, {@code width} by {@code height} texels. */
    public static FrontArea texels(final int x, final int y, final int width, final int height) {
        // The front is drawn a texel in from the block's left side.
        return new FrontArea(UNIT * (1 + x), UNIT * y, UNIT * (1 + x + width), UNIT * (y + height));
    }

    /**
     * The eject button of a drive of {@code kind} in bay {@code bay}, where its bezel draws it near the bezel's right
     * end; null for the hot-swap bay, which has none, and for a bay the front does not have.
     */
    @Nullable
    public EjectButton ejectButton(final int bay, final BayDriveSpec.Kind kind) {
        if (bay < 0 || bay >= bays.size()) {
            return null;
        }
        final FrontArea at = bays.get(bay);
        // The bezels paint their button in from their right end, a texel high: from the end, in texels, and down.
        final int[] button = switch (kind) {
            case CD -> new int[] {5, 2, 3};
            case DVD -> new int[] {4, 2, 4};
            case BLU_RAY -> new int[] {4, 2, 3};
            case HOT_SWAP -> null;
        };
        if (button == null) {
            return null;
        }
        final int top = at.top() + UNIT * button[2];
        return new EjectButton(at.right() - UNIT * button[0], top, at.right() - UNIT * button[1], top + UNIT, 0);
    }

    /* A front from its power button, the span its bays fill across and the row of each, and each port's socket. */
    private static CaseFrontLayout front(final FrontArea power, final int bayLeft, final int bayRight,
                                         final int[] bayRows, final int[][] sockets) {
        final FrontArea[] bays = new FrontArea[bayRows.length];
        for (int bay = 0; bay < bayRows.length; bay++) {
            bays[bay] = texels(bayLeft, bayRows[bay], bayRight - bayLeft, BAY_HEIGHT);
        }
        final FrontArea[] ports = new FrontArea[sockets.length];
        for (int port = 0; port < sockets.length; port++) {
            ports[port] = texels(sockets[port][0], sockets[port][1], SOCKET_WIDTH, SOCKET_HEIGHT);
        }
        return new CaseFrontLayout(power, List.of(bays), List.of(ports));
    }
}
