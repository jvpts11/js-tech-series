/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Which of the sounds that want to be heard are: no more than the game can play at once, the ones that matter most
 * first, and of those the nearest.
 *
 * <p>The game plays a couple of hundred short sounds at once but only a handful read as they play, and it shares both
 * with everything else in the world. A room of machines that each want a fan would take them all; this keeps the
 * series inside a share of its own, and what it drops is what the player would have heard least.
 */
public final class VoiceBudget {

    private final int maxStatic;
    private final int maxStreaming;

    /** The order sounds are chosen in: higher priority first, then the nearest, then by id so it never flickers. */
    private static final Comparator<Candidate> ORDER = Comparator.comparingInt(Candidate::priority).reversed()
            .thenComparingDouble(Candidate::distance)
            .thenComparing(Candidate::id);

    /**
     * A budget of at most {@code maxStatic} sounds loaded whole and {@code maxStreaming} sounds read as they play.
     */
    public VoiceBudget(final int maxStatic, final int maxStreaming) {
        if (maxStatic < 0 || maxStreaming < 0) {
            throw new IllegalArgumentException("a budget cannot be less than nothing");
        }
        this.maxStatic = maxStatic;
        this.maxStreaming = maxStreaming;
    }

    /** The sounds that fit, in the order they were chosen. */
    public List<Candidate> choose(final Collection<Candidate> wanted) {
        final List<Candidate> sorted = new ArrayList<>(wanted);
        sorted.sort(ORDER);
        final List<Candidate> chosen = new ArrayList<>();
        int statics = 0;
        int streams = 0;
        for (final Candidate candidate : sorted) {
            if (candidate.streaming()) {
                if (streams < maxStreaming) {
                    streams++;
                    chosen.add(candidate);
                }
            } else if (statics < maxStatic) {
                statics++;
                chosen.add(candidate);
            }
        }
        return chosen;
    }

    /**
     * A sound that wants to be heard.
     *
     * @param id        what tells it apart from every other sound wanting to play
     * @param priority  how much it matters, higher first
     * @param distance  how far it is from the listener, in blocks
     * @param streaming whether it is read as it plays, which takes one of the few streaming channels
     */
    public record Candidate(String id, int priority, double distance, boolean streaming) {
    }
}
