/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.ShapeMatch;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.resources.ResourceLocation;

/**
 * A multiblock machine declared entirely as data, as a data pack writes it: its shape, the kind of recipe it works,
 * its slots, lanes and tanks, its energy, and how many recipes it runs at once. A terminal block that names it
 * ({@link DataMachineBlock}) is all it takes in the game.
 *
 * @param shape    its shape: a pattern, drawn or by id, or a shape by rule
 * @param recipes  the kind of recipe it works, by the recipe type's id
 * @param layout   its slots, lanes and tanks
 * @param energy   the most energy it holds, in FE
 * @param receive  the most it takes in a tick
 * @param perTick  what a recipe that names no energy of its own spends a tick
 * @param parallel how many recipes it runs at once
 */
public record DataMachine(IStructureShape shape, ResourceLocation recipes, ProcessingMachineBlockEntity.Layout layout,
                          int energy, int receive, long perTick, Parallel parallel) {

    public DataMachine {
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(recipes, "recipes");
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(parallel, "parallel");
        if (energy < 0 || receive < 0 || perTick < 0) {
            throw new IllegalArgumentException("a machine holds, takes and spends no less than no energy");
        }
    }

    /**
     * How many recipes a data machine runs at once, from what was built: always the same number, the count of a
     * place's blocks or of a part inside, the times its pattern's slice repeats, or a number its blocks give.
     *
     * @param by    what it is read from: {@link #FIXED}, {@link #COUNT}, {@link #REPEATS} or {@link #VALUE}
     * @param name  the count's or the number's name, for {@link #COUNT} and {@link #VALUE}
     * @param fixed the number, for {@link #FIXED}
     */
    public record Parallel(String by, String name, int fixed) {

        /** Always {@code fixed}. */
        public static final String FIXED = "fixed";
        /** The count of {@code name}. */
        public static final String COUNT = "count";
        /** The times the pattern's repeated slice stands. */
        public static final String REPEATS = "repeats";
        /** The number of {@code name}, rounded down. */
        public static final String VALUE = "value";
        /** One at a time. */
        public static final Parallel ONE = new Parallel(FIXED, "", 1);

        public Parallel {
            Objects.requireNonNull(by, "by");
            Objects.requireNonNull(name, "name");
            if (!by.equals(FIXED) && !by.equals(COUNT) && !by.equals(REPEATS) && !by.equals(VALUE)) {
                throw new IllegalArgumentException("recipes at once are read from fixed, count, repeats or value, not "
                        + by);
            }
        }

        /** The number for the structure {@code formed}, at least one. */
        public ToIntFunction<ShapeMatch.Formed> reader() {
            return switch (this.by) {
                case COUNT -> formed -> Math.max(1, formed.count(this.name));
                case REPEATS -> formed -> Math.max(1, formed.repeats());
                case VALUE -> formed -> Math.max(1, (int) Math.floor(formed.value(this.name, 1.0)));
                default -> formed -> Math.max(1, this.fixed);
            };
        }
    }
}
