/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A line a player advances along, step by step: the eras of hardware, the tiers of industry, or one a mod adds. A
 * mod gates what it offers on how far a player has come along an axis, through a {@link ProgressionGate}, and moves
 * a player along it with {@link PlayerProgress#reach}.
 *
 * @param <S> the axis's steps
 */
public final class ProgressionAxis<S extends IAxisStep> {

    private final ResourceLocation id;
    private final TextKey name;
    private final List<S> steps;

    private ProgressionAxis(final ResourceLocation id, final TextKey name, final List<S> steps) {
        this.id = id;
        this.name = name;
        this.steps = steps;
    }

    /**
     * An axis of these steps, in the order of their levels.
     *
     * @throws IllegalArgumentException when there are no steps, or their levels are not 0, 1, 2 and so on, or two
     *                                  share a name
     */
    public static <S extends IAxisStep> ProgressionAxis<S> of(final ResourceLocation id, final TextKey name,
                                                             final List<S> steps) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("the axis " + id + " has no steps");
        }
        for (int i = 0; i < steps.size(); i++) {
            final S step = steps.get(i);
            if (step.level() != i) {
                throw new IllegalArgumentException("the axis " + id + " has " + step.serializedName()
                        + " at level " + step.level() + " where level " + i + " belongs");
            }
            for (int j = 0; j < i; j++) {
                if (steps.get(j).serializedName().equals(step.serializedName())) {
                    throw new IllegalArgumentException("the axis " + id + " names two steps "
                            + step.serializedName());
                }
            }
        }
        return new ProgressionAxis<>(id, name, List.copyOf(steps));
    }

    public ResourceLocation id() {
        return id;
    }

    /** The axis's name as a player reads it. */
    public Text text() {
        return name.text();
    }

    /** The key of the axis's name, which the language generator writes. */
    public TextKey name() {
        return name;
    }

    /** The steps, lowest first. */
    public List<S> steps() {
        return steps;
    }

    /** The first step, where every player starts. */
    public S first() {
        return steps.getFirst();
    }

    /** The step at {@code level}, held to the axis's ends. */
    public S at(final int level) {
        return steps.get(Math.max(0, Math.min(steps.size() - 1, level)));
    }

    /** The step saved as {@code serializedName}, or null when the axis has none of that name. */
    public @Nullable S byName(final String serializedName) {
        for (final S step : steps) {
            if (step.serializedName().equals(serializedName)) {
                return step;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ProgressionAxis[" + id + ", " + steps.size() + " steps]";
    }
}
