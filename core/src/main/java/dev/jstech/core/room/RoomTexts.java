/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;

/** What a room shows of itself in words: on the view through goggles, and on the Atmosphere Detector. */
@TextHolder
public final class RoomTexts {

    public static final TextKey ROOM = TextKey.of("jscore.room.room", "ROOM, %s m3");
    public static final TextKey FALLING = TextKey.of("jscore.room.falling", "%s kPa and falling");
    public static final TextKey RISING = TextKey.of("jscore.room.rising", "%s kPa and rising");
    public static final TextKey STEADY = TextKey.of("jscore.room.steady", "%s kPa");
    public static final TextKey THIN = TextKey.of("jscore.room.thin", "O2 %s kPa: too thin to breathe");
    public static final TextKey BREATHABLE = TextKey.of("jscore.room.breathable", "O2 %s kPa: fine to breathe");
    public static final TextKey NO_LEAKS = TextKey.of("jscore.room.no_leaks", "No leaks");
    public static final TextKey ONE_LEAK = TextKey.of("jscore.room.one_leak", "1 leak: %s");
    public static final TextKey LEAKS = TextKey.of("jscore.room.leaks", "%s leaks: %s");
    public static final TextKey AND_MORE = TextKey.of("jscore.room.and_more", "%s and %s more");
    public static final TextKey THE_BLOCK = TextKey.of("jscore.room.the_block", "the %s");
    public static final TextKey HOLE_ROOF = TextKey.of("jscore.room.hole_roof", "a hole in the roof");
    public static final TextKey HOLE_FLOOR = TextKey.of("jscore.room.hole_floor", "a hole in the floor");
    public static final TextKey HOLE_WALL = TextKey.of("jscore.room.hole_wall", "a hole in the wall");
    public static final TextKey SEPARATOR = TextKey.of("jscore.room.separator", "%s, %s");
    public static final TextKey SEEN_GOGGLES = TextKey.of("jscore.room.seen_goggles", "Seen through the goggles");
    public static final TextKey SEEN_DETECTOR = TextKey.of("jscore.room.seen_detector",
            "Shown by the Atmosphere Detector");
    /** How many leaks are named before the rest are counted. */
    public static final int NAMED = 2;
    /* A pressure is read as changing above this, in kilopascals a second. */
    private static final double STILL = 0.05;

    private RoomTexts() {
    }

    /** Its pressure, and whether it is falling or rising. */
    public static Text pressure(final double kilopascals, final double change) {
        final String shown = String.format(Locale.ROOT, "%.0f", kilopascals);
        if (change < -STILL) {
            return FALLING.with(shown);
        }
        return change > STILL ? RISING.with(shown) : STEADY.with(shown);
    }

    /** Its oxygen, and whether it is enough to breathe. */
    public static Text oxygen(final double partial, final boolean breathable) {
        final String shown = String.format(Locale.ROOT, "%.1f", partial);
        return breathable ? BREATHABLE.with(shown) : THIN.with(shown);
    }

    /** Its leaks, the first few named and the rest counted. */
    public static Text leaks(final List<RoomView.Leak> leaks) {
        if (leaks.isEmpty()) {
            return NO_LEAKS.text();
        }
        Text named = name(leaks.getFirst());
        for (int i = 1; i < Math.min(NAMED, leaks.size()); i++) {
            named = SEPARATOR.with(named, name(leaks.get(i)));
        }
        final Text listed = leaks.size() > NAMED ? AND_MORE.with(named, leaks.size() - NAMED) : named;
        return leaks.size() == 1 ? ONE_LEAK.with(listed) : LEAKS.with(leaks.size(), listed);
    }

    /** A leak by name: the block that leaks, or where a hole is. */
    public static Text name(final RoomView.Leak leak) {
        if (!leak.hole()) {
            return THE_BLOCK.with(GameText.of(BuiltInRegistries.BLOCK.get(leak.name()).getName()));
        }
        if (leak.side() == Direction.UP) {
            return HOLE_ROOF.text();
        }
        return leak.side() == Direction.DOWN ? HOLE_FLOOR.text() : HOLE_WALL.text();
    }
}
