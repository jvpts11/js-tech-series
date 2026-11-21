/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What is heard of the Prebuilder's work (computers/docs/design/survival.md), each sound at its moment of the job's
 * loop as its drawn work reaches it: the press as the lift brings the empty case up, and again as the built computer
 * sinks into the base; an arm as it sets off for a part, and again as it carries it over; and the part clicking into
 * its seat. Pure, so it is tested without the game.
 *
 * @param cues the sounds of the job, in the order they are met
 */
public record PrebuilderCues(List<Cue> cues) {

    /** The moment of the loop the press is heard at as the lift brings the case up: the job's start. */
    public static final float START = 0;
    /* A loop's moment before any job's start, where a job not yet heard stands. */
    private static final float BEFORE = -1;

    public PrebuilderCues {
        cues = cues.stream().sorted(Comparator.comparingDouble(Cue::at)).toList();
    }

    /**
     * The sounds of a job whose trips start at the moments {@code starts} of its loop, each {@code each} long, its part
     * taken at the share {@code taken} of a trip and set in its seat at the share {@code seated}, the built computer
     * sinking at {@code sinkAt}.
     */
    public static PrebuilderCues of(final float[] starts, final float each, final float taken, final float seated,
                                    final float sinkAt) {
        final List<Cue> cues = new ArrayList<>();
        cues.add(new Cue(START, Sound.PRESS));
        for (final float start : starts) {
            cues.add(new Cue(start, Sound.ARM));
            cues.add(new Cue(start + taken * each, Sound.ARM));
            cues.add(new Cue(start + seated * each, Sound.PART));
        }
        cues.add(new Cue(sinkAt, Sound.PRESS));
        return new PrebuilderCues(cues);
    }

    /**
     * The sounds met going from the moment {@code from} of the loop to {@code to}, {@code from} itself left out: a
     * moment behind {@code from} is a new job, heard from its start, and {@link #notYet()} is before any job's start.
     */
    public List<Sound> crossed(final float from, final float to) {
        final float since = to < from ? BEFORE : from;
        return cues.stream().filter(cue -> cue.at() > since && cue.at() <= to).map(Cue::sound).toList();
    }

    /** The moment a Prebuilder not yet heard at work stands at: before any job's start. */
    public static float notYet() {
        return BEFORE;
    }

    /** What is heard. */
    public enum Sound {
        /** The lift's press, bringing the case up or letting the computer down. */
        PRESS,
        /** An arm on the move. */
        ARM,
        /** A part going into its seat. */
        PART
    }

    /**
     * A sound at its moment.
     *
     * @param at    its moment of the job's loop
     * @param sound what is heard
     */
    public record Cue(float at, Sound sound) {
    }
}
