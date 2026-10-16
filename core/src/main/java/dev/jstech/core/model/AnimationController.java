/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Plays one animation at a time on a model, as one part of it: the doors of a machine have one controller and its
 * fans another, each going its own way. A new animation starts from where the last one left the bones and blends to
 * its own over a transition; the controller plays faster or slower by its speed, round again, held at the end or done
 * as its animation's loop says, and passes on the events whose times go by. Pure, and kept by one thread.
 */
public final class AnimationController {

    private final String name;
    @Nullable
    private ModelAnimation animation;
    private double startedAt;
    private double speed = 1;
    private double played;
    private double lastElapsed;
    private double transition;
    private double switchedAt;
    private boolean blending;
    private List<Bound> bound = List.of();
    @Nullable
    private ModelAnimation boundFor;
    @Nullable
    private PoseContribution current;
    @Nullable
    private PoseContribution from;
    @Nullable
    private PoseContribution mixed;
    /* What it last added to a pose, its animation's or a blend, from which a new animation blends. */
    @Nullable
    private PoseContribution applied;
    private final float[] values = new float[4];

    private static final double BEFORE_START = -1.0e-9;

    public AnimationController(final String name) {
        this.name = name;
    }

    /** Its name, by which the code and the server choose it. */
    public String name() {
        return this.name;
    }

    /** The animation it plays, or none. */
    @Nullable
    public ModelAnimation playing() {
        return this.animation;
    }

    /**
     * Plays {@code next}, blending to it from where the bones are over {@code transition} seconds; an animation
     * already playing goes on as it is, unless it is over.
     *
     * @param now the model's clock, in seconds
     */
    public void play(@Nullable final ModelAnimation next, final double now, final double transition) {
        if (next != null && next == this.animation) {
            return;
        }
        replay(next, now, transition);
    }

    /** Plays {@code next} from its start, whether it plays already or not. */
    public void replay(@Nullable final ModelAnimation next, final double now, final double transition) {
        if (this.applied != null && transition > 0) {
            this.from.copyFrom(this.applied);
            this.blending = true;
        } else {
            this.blending = false;
        }
        this.transition = transition;
        this.switchedAt = now;
        this.animation = next;
        this.startedAt = now;
        this.played = 0;
        this.lastElapsed = BEFORE_START;
    }

    /** Stops, blending back to rest over {@code transition} seconds. */
    public void stop(final double now, final double transition) {
        replay(null, now, transition);
    }

    /** Plays faster or slower from now on, 1 as its animation is written; 0 holds it where it is. */
    public void speed(final double factor, final double now) {
        this.played = elapsed(now);
        this.startedAt = now;
        this.speed = Math.max(0, factor);
    }

    /** How fast it plays. */
    public double speed() {
        return this.speed;
    }

    /** Whether it plays nothing, or an animation that plays once and is over, and blends from nothing either. */
    public boolean idle(final double now) {
        final boolean still = this.animation == null || this.animation.over(elapsed(now));
        return still && !(this.blending && now - this.switchedAt < this.transition);
    }

    /**
     * Adds what its animation does at {@code now} to {@code pose}, telling {@code listener} of the events whose times
     * have gone by since it last did.
     */
    public void apply(final ModelPose pose, final double now, final ExpressionContext context,
                      final IAnimationListener listener) {
        if (this.current == null) {
            this.current = new PoseContribution(pose.bones());
            this.from = new PoseContribution(pose.bones());
            this.mixed = new PoseContribution(pose.bones());
        }
        this.current.clear();
        if (this.animation != null) {
            final double elapsed = elapsed(now);
            events(this.animation, this.lastElapsed, elapsed, listener);
            this.lastElapsed = elapsed;
            if (this.animation.over(elapsed)) {
                // An animation that plays once is done, and the bones go back to where they were.
                this.animation = null;
            } else {
                contribute(this.animation, pose, this.animation.timeAt(elapsed), context);
            }
        }
        if (this.blending && now - this.switchedAt < this.transition) {
            final float t = (float) ((now - this.switchedAt) / this.transition);
            this.mixed.blend(this.from, this.current, t, pose.model());
            this.applied = this.mixed;
        } else {
            this.blending = false;
            this.applied = this.current;
        }
        this.applied.applyTo(pose);
    }

    /** How far into its animation it is at {@code now}, in seconds of the animation, before its loop folds it. */
    public double elapsed(final double now) {
        return this.played + (now - this.startedAt) * this.speed;
    }

    /**
     * Puts it {@code elapsed} seconds into its animation at {@code now}, as a timeline dragged does; the events
     * between are not told of.
     */
    public void seek(final double elapsed, final double now) {
        this.played = Math.max(0, elapsed);
        this.startedAt = now;
        this.lastElapsed = this.played;
        this.blending = false;
    }

    /* The values of each bone and morph target the animation moves, at its time {@code time}. */
    private void contribute(final ModelAnimation playing, final ModelPose pose, final double time,
                            final ExpressionContext context) {
        if (this.boundFor != playing) {
            this.bound = bind(playing, pose);
            this.boundFor = playing;
        }
        context.animTime(time);
        for (final Bound each : this.bound) {
            final ModelAnimation.Tracks tracks = each.tracks;
            if (tracks.rotation() != null) {
                tracks.rotation().evaluate(time, context, this.values);
                if (tracks.rotation().kind() == AnimationChannel.Kind.QUATERNION) {
                    this.current.turn(each.bone, this.values);
                } else {
                    this.current.euler(each.bone, this.values);
                }
            }
            if (tracks.position() != null) {
                tracks.position().evaluate(time, context, this.values);
                this.current.move(each.bone, this.values);
            }
            if (tracks.scale() != null) {
                tracks.scale().evaluate(time, context, this.values);
                this.current.size(each.bone, this.values);
            }
        }
        for (final Map.Entry<String, AnimationChannel> weight : playing.weights().entrySet()) {
            weight.getValue().evaluate(time, context, this.values);
            this.current.weight(weight.getKey(), this.values[0]);
        }
    }

    /* The bones an animation moves, by their places in the model, worked out once for each animation played. */
    private static List<Bound> bind(final ModelAnimation playing, final ModelPose pose) {
        final List<Bound> out = new ArrayList<>();
        for (final Map.Entry<String, ModelAnimation.Tracks> entry : playing.bones().entrySet()) {
            final int bone = pose.index(entry.getKey());
            if (bone >= 0) {
                out.add(new Bound(bone, entry.getValue()));
            }
        }
        return List.copyOf(out);
    }

    /* Tells of the events whose times lie after {@code after} and up to {@code upTo}, round every loop between. */
    private void events(final ModelAnimation playing, final double after, final double upTo,
                        final IAnimationListener listener) {
        if (playing.events().isEmpty() || upTo <= after) {
            return;
        }
        final double length = playing.length();
        if (playing.loop() != ModelAnimation.Loop.LOOP || length <= 0) {
            for (final ModelAnimation.Event event : playing.events()) {
                if (event.time() > after && event.time() <= upTo) {
                    listener.event(this.name, playing, event);
                }
            }
            return;
        }
        final long firstRound = (long) Math.floor(Math.max(0, after) / length);
        final long lastRound = (long) Math.floor(upTo / length);
        for (long round = firstRound; round <= lastRound; round++) {
            for (final ModelAnimation.Event event : playing.events()) {
                final double at = round * length + event.time();
                if (at > after && at <= upTo) {
                    listener.event(this.name, playing, event);
                }
            }
        }
    }

    private record Bound(int bone, ModelAnimation.Tracks tracks) {
    }
}
