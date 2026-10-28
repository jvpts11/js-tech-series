/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.advancement;

import dev.jstech.core.advancement.Advancements;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The events J's Space's advancements are earned by, and the one way the code reports one. Each is reported by what
 * does it, as that is built: the launch by the rocket, the landing by the descent. An advancement earned by a first of
 * the team is earned by the team's milestone instead ({@code SpaceMilestones}).
 *
 * <p>Each id is stable: an advancement names it in its data, so renaming one silently unhooks the advancement. The
 * data generation reads these same constants, which is what keeps the two from drifting apart.
 */
public final class SpaceEvents {

    /** A rocket the player is on, or launched from its panel, has lifted off. */
    public static final String ROCKET_LAUNCHED = "rocket_launched";
    /** The player saw the Earth rise over the Moon's horizon. */
    public static final String EARTHRISE = "earthrise";
    /** The player landed on a world; its detail is the world's id. */
    public static final String LANDED = "landed";

    private SpaceEvents() {
    }

    /** Reports that {@code player} caused {@code event}, as {@code jsspace:<event>}. */
    public static void award(@Nullable final Player player, final String event) {
        award(player, event, "");
    }

    /** The same, with the detail that tells apart where or what: the world landed on. */
    public static void award(@Nullable final Player player, final String event, @Nullable final String detail) {
        Advancements.award(player, ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, event), detail);
    }
}
