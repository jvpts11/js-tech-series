/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.explosion;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * The server's settings for blasts: what each mod's explosions do to the world, how many blocks the blasts of a level
 * look at and break each tick, and whether the game's own explosions show the Core's fireball, smoke and marks.
 */
public final class BlastBalance {

    /** How many blocks the blasts of a level look at in a tick, unless the server says otherwise. */
    public static final int DEFAULT_LOOKED_AT = 32_768;
    /** How many blocks the blasts of a level break in a tick, unless the server says otherwise. */
    public static final int DEFAULT_BROKEN = 1_024;
    private static volatile Map<String, BlastConsequence> consequences = Map.of();
    private static volatile int lookedAt = DEFAULT_LOOKED_AT;
    private static volatile int broken = DEFAULT_BROKEN;
    private static volatile boolean gameExplosionsShown = true;

    private BlastBalance() {
    }

    /**
     * What the explosions of {@code explosive} do: the setting given for its own id, else the one for its mod, else
     * {@link BlastConsequence#FULL}.
     */
    public static BlastConsequence consequenceOf(final ResourceLocation explosive) {
        final Map<String, BlastConsequence> set = consequences;
        final BlastConsequence own = set.get(explosive.toString());
        if (own != null) {
            return own;
        }
        return set.getOrDefault(explosive.getNamespace(), BlastConsequence.FULL);
    }

    public static void setConsequences(final Map<String, BlastConsequence> value) {
        consequences = Map.copyOf(value);
    }

    /** How many blocks the blasts of a level look at in a tick. */
    public static int lookedAt() {
        return lookedAt;
    }

    public static void setLookedAt(final int value) {
        lookedAt = value;
    }

    /** How many blocks the blasts of a level break in a tick. */
    public static int broken() {
        return broken;
    }

    public static void setBroken(final int value) {
        broken = value;
    }

    /** Whether the game's own explosions show the Core's fireball, smoke, debris and scorch marks. */
    public static boolean gameExplosionsShown() {
        return gameExplosionsShown;
    }

    public static void setGameExplosionsShown(final boolean value) {
        gameExplosionsShown = value;
    }
}
