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
 * One way the network can make an item, as the craft dialog offers it when there is more than one: what the
 * recipe is called, what kind it is, the machines it runs through, how many stages it has, how long the amount
 * asked would take, and its direct inputs against the stock. Text and numbers only, so the dialog's comparison of
 * two choices can be worked out without the game, and read in the language of whoever opened the dialog.
 *
 * @param label         the recipe's name as its author gave it, or its result's name
 * @param kind          one of {@link #KIND_BENCH}, {@link #KIND_PROCESSING}, {@link #KIND_MULTI_STAGE}
 * @param machines      the machines the recipe runs through, in order; empty for a bench recipe
 * @param stages        how many stages the recipe has (one for a bench or processing recipe)
 * @param estimateTicks how long the amount asked would take, or zero when there is no estimate
 * @param inputs        the recipe's direct inputs for the amount asked, with what the network holds of each
 * @param feasible      whether the network can deliver the amount asked through this recipe, crafting what is
 *                      short where a pattern makes it
 */
@TextHolder
public record RecipeChoice(Text label, String kind, List<Text> machines, int stages, int estimateTicks,
                           List<Input> inputs, boolean feasible) {

    public static final String KIND_BENCH = "bench";
    public static final String KIND_PROCESSING = "processing";
    public static final String KIND_MULTI_STAGE = "multi-stage";

    /** A bench, where a stage of a pipeline is crafted by hand rather than by a machine. */
    public static final TextKey BENCH = TextKey.of("jsc.crafting.choice.bench", "Bench");
    private static final TextKey KIND_BENCH_NAME = TextKey.of("jsc.crafting.choice.kind_bench", "bench");
    private static final TextKey KIND_PROCESSING_NAME =
            TextKey.of("jsc.crafting.choice.kind_processing", "processing");
    private static final TextKey KIND_MULTI_STAGE_NAME =
            TextKey.of("jsc.crafting.choice.kind_multi_stage", "multi-stage");
    private static final TextKey ONE_STAGE = TextKey.of("jsc.crafting.choice.one_stage", "%s stage");
    private static final TextKey STAGES = TextKey.of("jsc.crafting.choice.stages", "%s stages");
    private static final TextKey ABOUT_SECONDS = TextKey.of("jsc.crafting.choice.about_seconds", "~%s s");
    private static final TextKey ALL_IN_STOCK = TextKey.of("jsc.crafting.choice.all_in_stock", "everything in stock");
    private static final TextKey MISSING_CRAFTABLE =
            TextKey.of("jsc.crafting.choice.missing_craftable", "missing %s · can be crafted");
    private static final TextKey MISSING_UNMAKEABLE =
            TextKey.of("jsc.crafting.choice.missing_unmakeable", "missing %s · nothing makes it");
    /* How much of a thing, then the thing: "4 Coal". */
    private static final TextKey AMOUNT_OF = TextKey.of("jsc.crafting.choice.amount_of", "%s %s");

    /**
     * One direct input of a recipe for the amount asked.
     *
     * @param name      the input's display name
     * @param need      how much the amount asked consumes
     * @param have      how much the network holds
     * @param craftable whether another pattern on the network makes the input, so a shortfall can be covered
     */
    public record Input(Text name, long need, long have, boolean craftable) {

        public boolean satisfied() {
            return have >= need;
        }

        public long shortfall() {
            return Math.max(0L, need - have);
        }
    }

    public RecipeChoice {
        machines = List.copyOf(machines);
        inputs = List.copyOf(inputs);
    }

    /** The inputs the network is short of, in the recipe's order. */
    public List<Input> shortInputs() {
        final List<Input> out = new ArrayList<>();
        for (final Input in : inputs) {
            if (!in.satisfied()) {
                out.add(in);
            }
        }
        return out;
    }

    public boolean allInStock() {
        return shortInputs().isEmpty();
    }

    /** Whether every input that is short can be crafted by some other pattern on the network. */
    public boolean shortIsCraftable() {
        for (final Input in : shortInputs()) {
            if (!in.craftable()) {
                return false;
            }
        }
        return true;
    }

    /** The machines in order, joined with arrows; "Bench" for a bench recipe. */
    public Text machinesLine() {
        return machines.isEmpty() ? BENCH.text() : TextLists.join(" -> ", machines);
    }

    /** The one-line description under the name: kind, machines, stages and time. */
    public Text kindLine() {
        final List<Text> parts = new ArrayList<>();
        parts.add(kindText());
        parts.add(machinesLine());
        parts.add((stages == 1 ? ONE_STAGE : STAGES).with(stages));
        if (estimateTicks > 0) {
            parts.add(ABOUT_SECONDS.with(seconds(estimateTicks)));
        }
        return TextLists.join(" · ", parts);
    }

    /** The note under the inputs: everything in stock, or what is missing and whether it can be crafted. */
    public Text stockNote() {
        final List<Input> missing = shortInputs();
        if (missing.isEmpty()) {
            return ALL_IN_STOCK.text();
        }
        return (shortIsCraftable() ? MISSING_CRAFTABLE : MISSING_UNMAKEABLE).with(shortfalls(missing));
    }

    /** What kind of recipe it is, as a player reads it: bench, processing or multi-stage. */
    public Text kindText() {
        return switch (kind) {
            case KIND_BENCH -> KIND_BENCH_NAME.text();
            case KIND_PROCESSING -> KIND_PROCESSING_NAME.text();
            case KIND_MULTI_STAGE -> KIND_MULTI_STAGE_NAME.text();
            default -> Text.literal(kind);
        };
    }

    /** Whole seconds for a tick count, never less than one for a positive count. */
    public static long seconds(final int ticks) {
        return ticks <= 0 ? 0L : Math.max(1L, Math.round(ticks / 20.0));
    }

    /** The inputs that are short, each as how much is missing of it: "4 Coal, 2 Iron Ingot". */
    static Text shortfalls(final List<Input> missing) {
        final List<Text> parts = new ArrayList<>(missing.size());
        for (final Input in : missing) {
            parts.add(AMOUNT_OF.with(in.shortfall(), in.name()));
        }
        return TextLists.join(", ", parts);
    }
}
