/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * A job's trigger on a step of the series' progress that the network's team reaches:
 * {@code CREATE JOB retool AS EXEC tooling WHEN ERA standard REACHED}, or {@code WHEN TIER t3 REACHED} for an
 * industrial tier. The step is named by its id or its id's path, as a world event's kind is. Pure: the job agent hears
 * each step as the team reaches it.
 *
 * @param axis the axis the step is on
 * @param step the step, as written, in lower case
 */
public record IqlReachedTrigger(Axis axis, String step) {

    private static final String REACHED = "REACHED";

    /** The axes a job can wait on, by the word that names them and the id of the axis in J's Core. */
    public enum Axis {
        /** The eras of hardware. */
        ERA("jscore:hardware_era"),
        /** The tiers of industry. */
        TIER("jscore:industrial_tier");

        private final String id;

        Axis(final String id) {
            this.id = id;
        }

        /** The id of the axis this word names. */
        public String id() {
            return id;
        }
    }

    /** The trigger {@code spec}, what follows a job's WHEN, says; null when it is no trigger on a step reached. */
    @Nullable
    public static IqlReachedTrigger parse(final String spec) {
        final String[] words = spec.strip().split("\\s+");
        if (words.length != 3 || !words[2].equalsIgnoreCase(REACHED)) {
            return null;
        }
        for (final Axis axis : Axis.values()) {
            if (axis.name().equalsIgnoreCase(words[0])) {
                return new IqlReachedTrigger(axis, words[1].toLowerCase(Locale.ROOT));
            }
        }
        return null;
    }

    /**
     * Whether the step {@code stepId} of the axis {@code axisId}, as the progress saves them, is the one this trigger
     * names: by the step's whole id, or by its path when the step's id has a namespace.
     */
    public boolean names(final String axisId, final String stepId) {
        if (!axis.id().equals(axisId)) {
            return false;
        }
        final String reached = stepId.toLowerCase(Locale.ROOT);
        return step.contains(":") ? step.equals(reached) : reached.equals(step) || reached.endsWith(":" + step);
    }
}
