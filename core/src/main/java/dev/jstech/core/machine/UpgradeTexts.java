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

/**
 * What an upgrade's tooltip says it does: each effect it has as a share gained or lost ("+50% speed"), told apart as
 * a gain or a cost, and how many of it fit in one machine.
 */
@TextHolder
public final class UpgradeTexts {

    public static final TextKey SPEED = TextKey.of("jscore.upgrade.speed", "%s speed");
    public static final TextKey ENERGY = TextKey.of("jscore.upgrade.energy", "%s energy");
    public static final TextKey PARALLEL_ONE = TextKey.of("jscore.upgrade.parallel_one", "+1 recipe at once");
    public static final TextKey PARALLEL = TextKey.of("jscore.upgrade.parallel", "+%s recipes at once");
    public static final TextKey PARALLEL_ENERGY = TextKey.of("jscore.upgrade.parallel_energy",
            "energy in proportion");
    public static final TextKey YIELD = TextKey.of("jscore.upgrade.yield", "%s yield");
    public static final TextKey BYPRODUCTS = TextKey.of("jscore.upgrade.byproducts", "%s byproducts");
    public static final TextKey LIQUIDS = TextKey.of("jscore.upgrade.liquids", "%s liquids spent");
    public static final TextKey GASES = TextKey.of("jscore.upgrade.gases", "%s gases spent");
    public static final TextKey HEAT = TextKey.of("jscore.upgrade.heat", "%s heat");
    public static final TextKey SOUND = TextKey.of("jscore.upgrade.sound", "%s sound");
    public static final TextKey SILENT = TextKey.of("jscore.upgrade.silent", "the machine is silent");
    public static final TextKey BUFFER = TextKey.of("jscore.upgrade.buffer", "%s energy stored");
    public static final TextKey ROOM = TextKey.of("jscore.upgrade.room", "%s in each slot and tank");
    public static final TextKey MOST = TextKey.of("jscore.upgrade.most", "Up to %s in a machine");
    public static final TextKey ANY = TextKey.of("jscore.upgrade.any", "As many as the slots take");
    private static final double PERCENT = 100.0;

    private UpgradeTexts() {
    }

    /** A line for each effect of {@code effect} that changes a machine, in the order the tooltip shows them. */
    public static List<Line> effects(final UpgradeEffect effect) {
        final List<Line> lines = new ArrayList<>();
        share(lines, SPEED, effect.speed(), true);
        share(lines, ENERGY, effect.energy(), false);
        if (effect.parallel() > 0) {
            lines.add(new Line(effect.parallel() == 1 ? PARALLEL_ONE.with() : PARALLEL.with(effect.parallel()), true));
            lines.add(new Line(PARALLEL_ENERGY.with(), false));
        }
        share(lines, YIELD, effect.yield(), true);
        share(lines, BYPRODUCTS, effect.byproducts(), true);
        share(lines, LIQUIDS, effect.liquids(), false);
        share(lines, GASES, effect.gases(), false);
        share(lines, HEAT, effect.heat(), false);
        if (effect.sound() == 0.0) {
            lines.add(new Line(SILENT.with(), true));
        } else {
            share(lines, SOUND, effect.sound(), false);
        }
        share(lines, BUFFER, effect.buffer(), true);
        share(lines, ROOM, effect.room(), true);
        return lines;
    }

    /** How many of an upgrade fit in one machine, {@code most}, or {@link IUpgrade#ANY}. */
    public static Text most(final int most) {
        return most == IUpgrade.ANY ? ANY.with() : MOST.with(most);
    }

    /*
     * The line of {@code factor} as a share gained or lost, when it rounds to a whole percent: a gain when the factor
     * grows and {@code more} is what helps, or when it shrinks and less is what helps.
     */
    private static void share(final List<Line> lines, final TextKey line, final double factor, final boolean more) {
        final long percent = Math.round((factor - 1.0) * PERCENT);
        if (percent != 0) {
            lines.add(new Line(line.with((percent > 0 ? "+" : "") + percent + "%"), percent > 0 == more));
        }
    }

    /**
     * A line of an upgrade's tooltip.
     *
     * @param text what it says
     * @param gain whether it is a gain, shown in green, or a cost, shown in red
     */
    public record Line(Text text, boolean gain) {
    }
}
