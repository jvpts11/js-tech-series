/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The tags of a build's parts laid out on the screen, as the approved drawings lay them on a screen 540 of their pixels
 * high: each lifted over the point of its part, or hung under it, at the first of its heights where it meets no tag
 * laid before it nor the line across the top of the screen and stays on the screen; failing all of them, at the
 * nearest height over or under its point that does. A tag with no room anywhere on the screen is not laid: tags never
 * stand over one another. Pure, so a unit test checks it.
 */
final class PlanTags {

    /*
     * The heights a tag is tried at over its part, the nearest first, then under it, and the other way round for a
     * part in the screen's lower half; the room kept round it from other tags, and the gap kept from the screen's
     * edges, in the screen's scaled pixels.
     */
    private static final int[] UPS = {150, 185, 220, 255, 115, 80, 45};
    private static final int[] DOWNS = {-110, -145, -180, -75, -40};
    private static final float SCREEN = 540F;
    private static final float UPPER = 280F;
    private static final int ROOM = 3;
    private static final int EDGE = 2;
    /* The least gap between a tag laid at the nearest free height and its part's point, kept for the leader's dot. */
    private static final int OFF_THE_POINT = 2;

    private PlanTags() {
    }

    /**
     * Where each of {@code wanted} is laid on a screen {@code width} by {@code height} of its scaled pixels, in their
     * order, the line across its top standing in {@code banner} if one is shown: each tag's box, or none where it has
     * no room.
     */
    static List<Optional<Box>> lay(final List<Wanted> wanted, final int width, final int height,
                                   final Optional<Box> banner) {
        final float scale = height / SCREEN;
        final List<Box> laid = new ArrayList<>();
        banner.ifPresent(laid::add);
        final List<Optional<Box>> out = new ArrayList<>();
        for (final Wanted tag : wanted) {
            // Centred over its point, moved in from a side of the screen it would stand off, its leader still on it.
            final int x0 = Math.clamp(tag.x() - tag.wide() / 2, EDGE + ROOM, Math.max(EDGE + ROOM,
                    width - EDGE - ROOM - tag.wide()));
            final Optional<Box> box = listed(tag, x0, scale, laid, width, height)
                    .or(() -> nearest(tag, x0, scale, laid, width, height));
            box.ifPresent(laid::add);
            out.add(box);
        }
        return out;
    }

    /* At the first of the heights the approved drawings try {@code tag} at where it has room. */
    private static Optional<Box> listed(final Wanted tag, final int x0, final float scale, final List<Box> laid,
                                        final int width, final int height) {
        final boolean upper = tag.y() < UPPER * scale;
        for (final int[] heights : upper ? new int[][] {UPS, DOWNS} : new int[][] {DOWNS, UPS}) {
            for (final int each : heights) {
                final int lift = Math.round(each * scale);
                final Box box = new Box(x0, lift >= 0 ? tag.y() - lift - tag.high() : tag.y() - lift, tag.wide(),
                        tag.high());
                if (clear(box, laid, width, height)) {
                    return Optional.of(box);
                }
            }
        }
        return Optional.empty();
    }

    /* At the nearest height over or under the point of {@code tag} where it has room, its own half first. */
    private static Optional<Box> nearest(final Wanted tag, final int x0, final float scale, final List<Box> laid,
                                         final int width, final int height) {
        final boolean upper = tag.y() < UPPER * scale;
        for (int gap = OFF_THE_POINT; gap <= height; gap++) {
            final Box over = new Box(x0, tag.y() - gap - tag.high(), tag.wide(), tag.high());
            final Box under = new Box(x0, tag.y() + gap, tag.wide(), tag.high());
            for (final Box box : upper ? List.of(over, under) : List.of(under, over)) {
                if (clear(box, laid, width, height)) {
                    return Optional.of(box);
                }
            }
        }
        return Optional.empty();
    }

    /* Whether {@code box}, with its room round it, stays on the screen and meets nothing laid, nor its room. */
    private static boolean clear(final Box box, final List<Box> laid, final int width, final int height) {
        if (box.x() - ROOM < EDGE || box.x() + box.wide() + ROOM > width - EDGE || box.y() - ROOM < EDGE
                || box.y() + box.high() + ROOM > height - EDGE) {
            return false;
        }
        return laid.stream().noneMatch(other -> box.meets(other, ROOM * 2));
    }

    /**
     * A tag to lay: where the top of its part's box falls on the screen, and the tag's size, in the screen's scaled
     * pixels.
     */
    record Wanted(int x, int y, int wide, int high) {
    }

    /** A box on the screen: its top left corner and its size, in the screen's scaled pixels. */
    record Box(int x, int y, int wide, int high) {

        /** Whether this box, {@code room} wider each way, meets {@code other}. */
        boolean meets(final Box other, final int room) {
            return x - room < other.x + other.wide && other.x < x + wide + room && y - room < other.y + other.high
                    && other.y < y + high + room;
        }
    }
}
