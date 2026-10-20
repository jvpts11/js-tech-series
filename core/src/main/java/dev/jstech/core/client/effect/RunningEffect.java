/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.effect.Emitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An effect playing on the player's game: its definition, where it is and which way it faces now (it may follow
 * something), where its beams and arcs go, its colour and size, whether it is held until let go or plays once, how
 * long it has played, and its emitters as they play. An emitter another emits from its particles plays only there.
 */
final class RunningEffect {

    final ClientLevel level;
    final @Nullable ResourceLocation id;
    final @Nullable Object holder;
    final Supplier<Vec3> at;
    final Supplier<Vec3> way;
    final Supplier<Optional<Vec3>> to;
    final int colour;
    final float scale;
    final long seed;
    final List<EmitterRun> runs = new ArrayList<>();
    EffectDefinition definition;
    double elapsed;
    boolean started;
    boolean stopped;
    EffectPlace place = EffectPlace.STILL;
    /* Where it was the tick before, for what it emits moving to be laid along the way it went; null at first. */
    @Nullable Vec3 lastAt;

    RunningEffect(final ClientLevel level, final @Nullable ResourceLocation id, final EffectDefinition definition,
                  final @Nullable Object holder, final Supplier<Vec3> at, final Supplier<Vec3> way,
                  final Supplier<Optional<Vec3>> to, final int colour, final float scale, final long seed) {
        this.level = level;
        this.id = id;
        this.holder = holder;
        this.at = at;
        this.way = way;
        this.to = to;
        this.colour = colour;
        this.scale = scale;
        this.seed = seed;
        this.definition = definition;
        build();
    }

    /** Whether it plays until let go. */
    boolean held() {
        return this.holder != null && !this.stopped;
    }

    /** How long it emits, in seconds: the longest of its emitters' times and bursts. */
    double end() {
        double end = 0;
        for (final Emitter emitter : this.definition.emitters()) {
            // A mark and what the screen does are laid as it starts, and last on their own.
            end = Math.max(end, switch (emitter.kind()) {
                case PARTICLES -> emitter.emission().duration();
                case BEAM, ARC -> emitter.extra().fade();
                case MARK, SCREEN -> 0;
            });
            for (final Emitter.Burst burst : emitter.emission().bursts()) {
                end = Math.max(end, burst.at());
            }
        }
        return end;
    }

    /** How many of its particles live. */
    int alive() {
        int alive = 0;
        for (final EmitterRun run : this.runs) {
            alive += aliveOf(run);
        }
        return alive;
    }

    /**
     * Plays {@code changed} from now on, as an edit does: emitters it keeps by name keep their particles and take on
     * its numbers at once.
     */
    void replace(final EffectDefinition changed) {
        final Map<String, Emitter> byName = new HashMap<>();
        changed.emitters().forEach(each -> byName.put(each.name(), each));
        final boolean same = this.definition.emitters().size() == changed.emitters().size()
                && this.definition.emitters().stream().allMatch(each -> byName.containsKey(each.name()));
        this.definition = changed;
        if (same) {
            renew(this.runs, byName);
        } else {
            this.runs.clear();
            build();
        }
    }

    private void build() {
        final Set<String> carried = new HashSet<>();
        this.definition.emitters().forEach(each -> carried.addAll(each.emission().children()));
        for (final Emitter emitter : this.definition.emitters()) {
            if (!carried.contains(emitter.name())) {
                this.runs.add(run(emitter, false, new HashSet<>()));
            }
        }
    }

    /* A run of {@code emitter} with runs of what its particles carry; an emitter carrying itself is cut there. */
    private EmitterRun run(final Emitter emitter, final boolean child, final Set<String> above) {
        final EmitterRun run = new EmitterRun(this, emitter, child);
        above.add(emitter.name());
        for (final String name : emitter.emission().children()) {
            if (!above.contains(name)) {
                this.definition.emitter(name).ifPresent(each -> run.children.add(run(each, true,
                        new HashSet<>(above))));
            }
        }
        return run;
    }

    private static void renew(final List<EmitterRun> runs, final Map<String, Emitter> byName) {
        for (final EmitterRun run : runs) {
            final Emitter now = byName.get(run.emitter.name());
            if (now != null) {
                run.emitter = now;
            }
            renew(run.children, byName);
        }
    }

    private static int aliveOf(final EmitterRun run) {
        int alive = run.alive;
        for (final EmitterRun child : run.children) {
            alive += aliveOf(child);
        }
        return alive;
    }
}
