/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.workshop;

import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What one piece of a personal-use card's work costs, as the server's setting chooses ({@link UpdateCost}): the
 * joules the computer that has the card draws for it, the levels whoever asked pays, both, or nothing.
 *
 * <p>The energy is the work's own: smelting, 2 kJ an item; enchanting, 10 kJ for every level the game's table would
 * take; repairing, 2 kJ for every percent of durability given back; combining, 10 kJ for every level the game's anvil
 * would ask; naming, 5 kJ. The levels are half of what the game's own block would take, rounded up, at least one, and
 * never any lapis. One joule is one FE. Every number is a first estimate.
 *
 * @param levels the levels whoever asked pays
 * @param joules the joules the computer draws for it
 */
@TextHolder
public record WorkshopPrice(int levels, long joules) {

    /** What costs nothing. */
    public static final WorkshopPrice FREE = new WorkshopPrice(0, 0L);
    public static final TextKey NOTHING = TextKey.of("jsc.workshop.price.free", "free");
    /** Energy alone, in kilojoules. */
    public static final TextKey ENERGY = TextKey.of("jsc.workshop.price.energy", "%s kJ");
    /** Energy, in kilojoules, and levels. */
    public static final TextKey BOTH = TextKey.of("jsc.workshop.price.both", "%s kJ and %s");
    public static final TextKey LEVEL = TextKey.of("jsc.update.level", "%s level");
    public static final TextKey LEVELS = TextKey.of("jsc.update.levels", "%s levels");
    private static final long SMELT_JOULES = 2_000L;
    private static final long ENCHANT_JOULES_A_LEVEL = 10_000L;
    /* Two kilojoules for every percent, as joules for the whole durability: 2 000 J a percent, a hundred percent. */
    private static final long REPAIR_JOULES_WHOLE = 200_000L;
    private static final long COMBINE_JOULES_A_LEVEL = 10_000L;
    private static final long NAME_JOULES = 5_000L;
    private static final double JOULES_A_KILOJOULE = 1_000.0;

    public WorkshopPrice {
        levels = Math.max(0, levels);
        joules = Math.max(0L, joules);
    }

    /** One item smelted: the energy, and no levels, since a furnace asks none. */
    public static WorkshopPrice smelt(final UpdateCost cost) {
        return of(cost, 0, SMELT_JOULES);
    }

    /** Enchanting offer {@code offer} (0, 1 or 2), which the game's table takes one, two or three levels for. */
    public static WorkshopPrice enchant(final UpdateCost cost, final int offer) {
        final int tableLevels = Math.max(0, Math.min(2, offer)) + 1;
        return of(cost, tableLevels, tableLevels * ENCHANT_JOULES_A_LEVEL);
    }

    /**
     * What the anvil's {@code action} costs (a repair, a combine or a name), where the game's anvil asks
     * {@code anvilLevels}: a repair by the durability it gives back, {@code restored} of {@code maxDamage}.
     */
    public static WorkshopPrice anvil(final UpdateCost cost, final UpdateAction action, final int anvilLevels,
                                      final int restored, final int maxDamage) {
        final long joules = switch (action) {
            case REPAIR -> maxDamage <= 0 || restored <= 0 ? 0L
                    : (restored * REPAIR_JOULES_WHOLE + maxDamage - 1L) / maxDamage;
            case COMBINE -> anvilLevels * COMBINE_JOULES_A_LEVEL;
            case NAME -> NAME_JOULES;
            case SMELT, ENCHANT -> 0L;
        };
        return of(cost, anvilLevels, joules);
    }

    /** The levels a card asks where the game's own block takes {@code vanilla}: half, rounded up, at least one. */
    public static int halfLevels(final int vanilla) {
        return vanilla <= 0 ? 0 : Math.max(1, (vanilla + 1) / 2);
    }

    /** {@code count} levels, in the words for one or for more. */
    public static Text levelsText(final int count) {
        return (count == 1 ? LEVEL : LEVELS).with(count);
    }

    /** Whether it costs nothing at all. */
    public boolean free() {
        return levels == 0 && joules == 0L;
    }

    /** The price in words: the kilojoules, the levels, both, or free. */
    public Text text() {
        final String kilojoules = UnitFormatter.forCurrentLocale().decimal(joules / JOULES_A_KILOJOULE);
        if (joules > 0L) {
            return levels > 0 ? BOTH.with(kilojoules, levelsText(levels)) : ENERGY.with(kilojoules);
        }
        return levels > 0 ? levelsText(levels) : NOTHING.text();
    }

    /* What {@code cost} takes of a piece of work the game's block asks {@code vanilla} levels for, and its energy. */
    private static WorkshopPrice of(final UpdateCost cost, final int vanilla, final long joules) {
        return new WorkshopPrice(cost.levels() ? halfLevels(vanilla) : 0, cost.energy() ? joules : 0L);
    }
}
