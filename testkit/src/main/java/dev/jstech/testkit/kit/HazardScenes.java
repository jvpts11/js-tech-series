/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.core.hazard.HazardBand;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.text.TextKey;
import dev.jstech.testkit.gametest.Expect;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * A scene for hazards: a dose of a hazard given to a body, as a source would build it up; and the band it stands in
 * checked, by the band's name, with what built up said when it is another.
 *
 * <pre>{@code
 * HazardScenes.dose(player, MyHazards.SPORES, 60.0);
 * HazardScenes.expectBand(helper, player, MyHazards.SPORES, MyTexts.CHOKING);
 * }</pre>
 */
public final class HazardScenes {

    private HazardScenes() {
    }

    /** Builds {@code amount} of {@code kind} up in {@code body}, on top of what it has. */
    public static void dose(final LivingEntity body, final HazardKind kind, final double amount) {
        Hazards.add(body, kind, amount);
    }

    /** Sets what {@code body} has of {@code kind} to {@code level}. */
    public static void set(final LivingEntity body, final HazardKind kind, final double level) {
        Hazards.set(body, kind, level);
    }

    /**
     * That {@code body} stands in the band of {@code kind} named {@code band}, or in none when {@code band} is
     * null; else a failure saying the band it stands in and what built up.
     */
    public static void expectBand(final GameTestHelper helper, final LivingEntity body, final HazardKind kind,
                                  final TextKey band) {
        final Optional<HazardBand> now = Hazards.band(body, kind);
        final TextKey found = now.map(HazardBand::name).orElse(null);
        if (band == null ? found != null : !band.equals(found)) {
            throw Expect.failure(helper, at(helper, body), "the band of " + kind.id() + ": expected "
                    + nameOf(band) + ", found " + nameOf(found) + ", with " + Hazards.level(body, kind)
                    + " built up");
        }
    }

    /** Where {@code body} stands, in the arena, for a failure to mark. */
    public static BlockPos at(final GameTestHelper helper, final LivingEntity body) {
        return helper.relativePos(body.blockPosition());
    }

    private static String nameOf(final TextKey band) {
        return band == null ? "none" : band.english();
    }
}
