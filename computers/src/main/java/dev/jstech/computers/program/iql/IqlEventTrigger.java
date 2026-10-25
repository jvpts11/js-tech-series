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
 * A job's trigger on a world event: {@code CREATE JOB safe AS EXEC shutdown WHEN EVENT mymod:solar_storm COMING},
 * with {@code UNDER WAY} or {@code ENDED} in place of {@code COMING}. The kind is named by its id, or by its id's path.
 * Pure: the job agent asks the world what the network sees.
 *
 * @param event the kind of world event, as written, in lower case
 * @param stage the step that fires the job
 */
public record IqlEventTrigger(String event, Stage stage) {

    private static final String KEYWORD = "EVENT";
    /* The two words of the step under way, read one at a time as the language's keywords are. */
    private static final String UNDER = "UNDER";
    private static final String WAY = "WAY";

    /** The step of a world event that fires a job. */
    public enum Stage {
        /** The network sees it coming: its instrument is on the network and its warning has gone out. */
        COMING,
        /** It has begun where the network stands. */
        UNDER_WAY,
        /** It was under way where the network stands, and is no longer. */
        ENDED
    }

    /** The trigger {@code spec}, what follows a job's WHEN, says; null when it is no trigger on a world event. */
    @Nullable
    public static IqlEventTrigger parse(final String spec) {
        final String[] words = spec.strip().split("\\s+");
        if (words.length < 3 || words.length > 4 || !words[0].equalsIgnoreCase(KEYWORD)) {
            return null;
        }
        final Stage stage;
        if (words.length == 4) {
            stage = words[2].equalsIgnoreCase(UNDER) && words[3].equalsIgnoreCase(WAY) ? Stage.UNDER_WAY : null;
        } else {
            stage = switch (words[2].toUpperCase(Locale.ROOT)) {
                case "COMING" -> Stage.COMING;
                case "ENDED" -> Stage.ENDED;
                default -> null;
            };
        }
        return stage == null ? null : new IqlEventTrigger(words[1].toLowerCase(Locale.ROOT), stage);
    }

    /** Whether the kind {@code id} ({@code namespace:path}) is the one this trigger names, by its id or its path. */
    public boolean names(final String id) {
        return this.event.contains(":") ? this.event.equals(id) : id.endsWith(":" + this.event);
    }
}
