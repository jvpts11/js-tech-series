/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;

import java.util.ArrayList;
import java.util.List;

/**
 * Says, in one line, what the recipe a player picked does differently from another recipe for the same item:
 * the ingredients it swaps, the stages it adds or saves, the time it costs or saves for the amount asked, and
 * what it is short of. Only what differs is named; two recipes that differ in nothing say so.
 */
@TextHolder
public final class RecipeDifferences {

    private static final String SEP = " · ";

    private static final TextKey AGAINST = TextKey.of("jsc.crafting.differences.against", "%s against %s: %s");
    private static final TextKey THE_SAME = TextKey.of("jsc.crafting.differences.the_same",
            "%s against %s: the same ingredients, stages and time");
    private static final TextKey TAKES_INSTEAD =
            TextKey.of("jsc.crafting.differences.takes_instead", "takes %s instead of %s");
    private static final TextKey ALSO_TAKES = TextKey.of("jsc.crafting.differences.also_takes", "also takes %s");
    private static final TextKey DOES_WITHOUT = TextKey.of("jsc.crafting.differences.does_without", "does without %s");
    private static final TextKey ONE_MORE_STAGE =
            TextKey.of("jsc.crafting.differences.one_more_stage", "one more stage");
    private static final TextKey ONE_STAGE_FEWER =
            TextKey.of("jsc.crafting.differences.one_stage_fewer", "one stage fewer");
    private static final TextKey MORE_STAGES = TextKey.of("jsc.crafting.differences.more_stages", "%s more stages");
    private static final TextKey STAGES_FEWER = TextKey.of("jsc.crafting.differences.stages_fewer", "%s stages fewer");
    private static final TextKey SLOWER = TextKey.of("jsc.crafting.differences.slower", "%s s slower for %s");
    private static final TextKey FASTER = TextKey.of("jsc.crafting.differences.faster", "%s s faster for %s");
    private static final TextKey ALL_IN_STOCK_UNLIKE =
            TextKey.of("jsc.crafting.differences.all_in_stock_unlike", "everything in stock, unlike %s");
    private static final TextKey SHORT_CRAFTED_FIRST = TextKey.of("jsc.crafting.differences.short_crafted_first",
            "%s short, which the network would craft first");
    private static final TextKey SHORT_UNMAKEABLE = TextKey.of("jsc.crafting.differences.short_unmakeable",
            "%s short, and nothing on the network makes it");

    private RecipeDifferences() {
    }

    /** The strip's text for {@code chosen} against {@code other}, for {@code quantity} of the result. */
    public static Text describe(final RecipeChoice chosen, final RecipeChoice other, final long quantity) {
        final List<Text> parts = new ArrayList<>();
        final Text ingredients = ingredients(chosen, other);
        if (!ingredients.isEmpty()) {
            parts.add(ingredients);
        }
        final Text stages = stages(chosen.stages(), other.stages());
        if (!stages.isEmpty()) {
            parts.add(stages);
        }
        final Text time = time(chosen.estimateTicks(), other.estimateTicks(), quantity);
        if (!time.isEmpty()) {
            parts.add(time);
        }
        final Text stock = stock(chosen, other);
        if (!stock.isEmpty()) {
            parts.add(stock);
        }
        if (parts.isEmpty()) {
            return THE_SAME.with(chosen.label(), other.label());
        }
        return AGAINST.with(chosen.label(), other.label(), TextLists.join(SEP, parts));
    }

    /** "takes A instead of X", "also takes A", or "does without X"; nothing when both take the same things. */
    private static Text ingredients(final RecipeChoice chosen, final RecipeChoice other) {
        final List<Text> onlyChosen = namesOnlyIn(chosen, other);
        final List<Text> onlyOther = namesOnlyIn(other, chosen);
        if (onlyChosen.isEmpty() && onlyOther.isEmpty()) {
            return Text.EMPTY;
        }
        if (!onlyChosen.isEmpty() && !onlyOther.isEmpty()) {
            return TAKES_INSTEAD.with(join(onlyChosen), join(onlyOther));
        }
        if (!onlyChosen.isEmpty()) {
            return ALSO_TAKES.with(join(onlyChosen));
        }
        return DOES_WITHOUT.with(join(onlyOther));
    }

    private static List<Text> namesOnlyIn(final RecipeChoice a, final RecipeChoice b) {
        final List<Text> out = new ArrayList<>();
        for (final RecipeChoice.Input in : a.inputs()) {
            if (!has(b, in.name()) && !out.contains(in.name())) {
                out.add(in.name());
            }
        }
        return out;
    }

    private static boolean has(final RecipeChoice choice, final Text name) {
        for (final RecipeChoice.Input in : choice.inputs()) {
            if (in.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Text join(final List<Text> names) {
        return TextLists.join(", ", names);
    }

    private static Text stages(final int chosen, final int other) {
        final int delta = chosen - other;
        if (delta == 0) {
            return Text.EMPTY;
        }
        if (delta == 1) {
            return ONE_MORE_STAGE.text();
        }
        if (delta == -1) {
            return ONE_STAGE_FEWER.text();
        }
        return delta > 0 ? MORE_STAGES.with(delta) : STAGES_FEWER.with(-delta);
    }

    private static Text time(final int chosen, final int other, final long quantity) {
        if (chosen <= 0 || other <= 0) {
            return Text.EMPTY;
        }
        final long a = RecipeChoice.seconds(chosen);
        final long b = RecipeChoice.seconds(other);
        if (a == b) {
            return Text.EMPTY;
        }
        return (a > b ? SLOWER : FASTER).with(Math.abs(a - b), quantity);
    }

    /** What the chosen recipe is short of, or that it has everything where the other does not. */
    private static Text stock(final RecipeChoice chosen, final RecipeChoice other) {
        final List<RecipeChoice.Input> missing = chosen.shortInputs();
        if (missing.isEmpty()) {
            return other.allInStock() ? Text.EMPTY : ALL_IN_STOCK_UNLIKE.with(other.label());
        }
        return (chosen.shortIsCraftable() ? SHORT_CRAFTED_FIRST : SHORT_UNMAKEABLE)
                .with(RecipeChoice.shortfalls(missing));
    }
}
