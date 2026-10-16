/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.effect.Emitter;
import java.util.ArrayList;
import java.util.List;

/**
 * One emitter of an effect as it plays: what it has built up to emit at its rate, the next of its bursts, the emitters
 * its particles carry, and how many of its particles live.
 */
final class EmitterRun {

    final RunningEffect effect;
    final List<EmitterRun> children = new ArrayList<>();
    final boolean child;
    Emitter emitter;
    double carry;
    int nextBurst;
    int alive;

    EmitterRun(final RunningEffect effect, final Emitter emitter, final boolean child) {
        this.effect = effect;
        this.emitter = emitter;
        this.child = child;
    }

    /** The emitter it plays. */
    Emitter emitter() {
        return this.emitter;
    }
}
