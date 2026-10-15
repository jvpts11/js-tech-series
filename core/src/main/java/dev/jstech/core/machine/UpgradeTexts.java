/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** What an upgrade's tooltip says it does: each effect it has, and how many of it fit in one machine. */
@TextHolder
public final class UpgradeTexts {

    public static final TextKey SPEED = TextKey.of("jscore.upgrade.speed", "Works %s times as fast");
    public static final TextKey ENERGY = TextKey.of("jscore.upgrade.energy", "Spends %s times the energy a tick");
    public static final TextKey PARALLEL = TextKey.of("jscore.upgrade.parallel", "Works %s more recipes at once");
    public static final TextKey YIELD = TextKey.of("jscore.upgrade.yield", "Makes %s times the items");
    public static final TextKey BYPRODUCTS = TextKey.of("jscore.upgrade.byproducts",
            "Byproducts %s times as likely");
    public static final TextKey USE = TextKey.of("jscore.upgrade.use", "Spends %s times the fluid and matter");
    public static final TextKey HEAT = TextKey.of("jscore.upgrade.heat", "Gives off %s times the heat");
    public static final TextKey SOUND = TextKey.of("jscore.upgrade.sound", "%s times as loud");
    public static final TextKey BUFFER = TextKey.of("jscore.upgrade.buffer", "Stores %s times the energy");
    public static final TextKey MOST = TextKey.of("jscore.upgrade.most", "At most %s in one machine");
    private static final double NEUTRAL = 1e-9;

    private UpgradeTexts() {
    }

    /** A line for each effect of {@code effect} that changes a machine. */
    public static List<Text> effects(final UpgradeEffect effect) {
        final List<Text> lines = new ArrayList<>();
        factor(lines, SPEED, effect.speed());
        factor(lines, ENERGY, effect.energy());
        if (effect.parallel() > 0) {
            lines.add(PARALLEL.with(effect.parallel()));
        }
        factor(lines, YIELD, effect.yield());
        factor(lines, BYPRODUCTS, effect.byproducts());
        factor(lines, USE, effect.use());
        factor(lines, HEAT, effect.heat());
        factor(lines, SOUND, effect.sound());
        factor(lines, BUFFER, effect.buffer());
        return lines;
    }

    private static void factor(final List<Text> lines, final TextKey line, final double factor) {
        if (Math.abs(factor - 1.0) > NEUTRAL) {
            lines.add(line.with(String.format(Locale.ROOT, "%.2f", factor)));
        }
    }
}
