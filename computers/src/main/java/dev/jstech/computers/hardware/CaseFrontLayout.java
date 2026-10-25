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
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;
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
    /** How far a stick in a port stands out of the front, in sixty-fourths: its body, past its plug. */
    public static final int STICK_OUT = 18;
    /** How far an open tray stands out of its bay, in sixty-fourths: all of it but the end still in the drive. */
    public static final int TRAY_OUT = 31;
    /* How far in from each end of its bezel a tray runs, and where it lies down the bezel, the disc on it taken in. */
    private static final int TRAY_INSET = 4;
    private static final int TRAY_TOP = 3;
    private static final int TRAY_BOTTOM = 8;
    /* How far a stick's body reaches past each edge of its socket. */
    private static final int STICK_MARGIN = 1;
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
    /* Each era's front, and the Aesthetic case's where it differs; an era after the Advanced has the Advanced's. */
    private static final EraTable<CaseFrontLayout> FRONTS = EraTable.ofFive(VINTAGE, LEGACY, TRANSITION, STANDARD,
            ADVANCED);
    private static final EraTable<CaseFrontLayout> AESTHETIC_FRONTS = EraTable.ofFive(VINTAGE, LEGACY, TRANSITION,
            STANDARD_AESTHETIC, ADVANCED_AESTHETIC);

    public CaseFrontLayout {
        bays = List.copyOf(bays);
        ports = List.copyOf(ports);
    }

    /** The front of the case of {@code era} in {@code style}. */
    public static CaseFrontLayout of(final Era era, final CaseStyle style) {
        return (style == CaseStyle.AESTHETIC ? AESTHETIC_FRONTS : FRONTS).get(era);
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

    /** Where a stick in port {@code port} stands out of the front: round its socket, its body a little wider. */
    public FrontArea stick(final int port) {
        final FrontArea socket = ports.get(port);
        return new FrontArea(socket.left() - STICK_MARGIN, socket.top() - STICK_MARGIN,
                socket.right() + STICK_MARGIN, socket.bottom() + STICK_MARGIN);
    }

    /** Where the open tray of the drive in bay {@code bay} stands out of the front, the disc lying on it taken in. */
    public FrontArea tray(final int bay) {
        final FrontArea bezel = bays.get(bay);
        return new FrontArea(bezel.left() + TRAY_INSET, bezel.top() + TRAY_TOP, bezel.right() - TRAY_INSET,
                bezel.top() + TRAY_BOTTOM);
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
