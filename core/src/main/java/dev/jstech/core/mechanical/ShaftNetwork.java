/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mechanical;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Parts joined by shafts and gears so that they turn as one: each at its ratio to the network's speed, the speed of
 * its first part. The network adds up, seen from that first part, what each part weighs in turning (its inertia times
 * its ratio squared), what each holds back by friction (its friction times its ratio) and what each drives or asks,
 * and each step turns faster or slower by the net torque over the inertia, never past the speed where the torque
 * balances. A network whose ratios disagree, gears that would have to turn two ways at once, is jammed and does not
 * turn. Pure.
 */
public final class ShaftNetwork {

    private final List<Member> members;
    private final List<Member> working = new ArrayList<>();
    private final double inertia;
    private final double friction;
    private final boolean jammed;
    private double speed;

    /**
     * A network of {@code members}, starting at {@code rpm} at its first part; {@code jammed} when their ratios
     * disagree.
     */
    public ShaftNetwork(final List<Member> members, final double rpm, final boolean jammed) {
        this.members = List.copyOf(members);
        double held = 0.0;
        double drag = 0.0;
        for (final Member member : this.members) {
            held += member.inertia() * member.ratio() * member.ratio();
            drag += member.friction() * Math.abs(member.ratio());
            if (member.torque() != null) {
                this.working.add(member);
            }
        }
        this.inertia = held;
        this.friction = drag;
        this.jammed = jammed;
        this.speed = jammed ? 0.0 : RotationMath.radians(rpm);
    }

    /** Its parts. */
    public List<Member> members() {
        return this.members;
    }

    /** Whether its ratios disagree, so that it cannot turn. */
    public boolean jammed() {
        return this.jammed;
    }

    /** How fast its first part turns, in revolutions a minute. */
    public double rpm() {
        return RotationMath.rpm(this.speed);
    }

    /** How fast {@code member} turns, in revolutions a minute. */
    public double rpm(final Member member) {
        return RotationMath.rpm(this.speed) * member.ratio();
    }

    /** What it weighs in turning, seen from its first part, in kilogram square metres. */
    public double inertia() {
        return this.inertia;
    }

    /** What its friction holds back, seen from its first part, in newton-metres. */
    public double friction() {
        return this.friction;
    }

    /**
     * The torque it feels at {@code radiansPerSecond}, seen from its first part: what its parts drive, less what they
     * ask, less its friction against the way it turns.
     */
    public double torque(final double radiansPerSecond) {
        double sum = 0.0;
        final double rpm = RotationMath.rpm(radiansPerSecond);
        for (final Member member : this.working) {
            sum += member.ratio() * member.torque().torque(rpm * member.ratio());
        }
        return sum - Math.signum(radiansPerSecond) * this.friction;
    }

    /** Turns for {@code seconds}: faster or slower by its net torque, towards the balance; a jammed one stays put. */
    public void step(final double seconds) {
        if (!this.jammed) {
            this.speed = RotationMath.step(this.speed, this.inertia, this::torque, seconds);
        }
    }

    /** The power its parts drive in now, in watts: what the sources give, before what the rest takes. */
    public double power() {
        double sum = 0.0;
        final double rpm = rpm();
        for (final Member member : this.working) {
            final double own = rpm * member.ratio();
            sum += Math.max(0.0, RotationMath.power(member.torque().torque(own), own));
        }
        return sum;
    }

    /**
     * One part of a network as the network meets it.
     *
     * @param key      what the part is to whoever built the network: its place
     * @param ratio    how fast it turns for each turn of the first part; negative the other way
     * @param inertia  what it weighs in turning, in kilogram square metres
     * @param friction what it holds back by friction, in newton-metres
     * @param torque   what it drives or asks at each of its own speeds; none for a part that only turns
     */
    public record Member(long key, double ratio, double inertia, double friction, @Nullable ITorque torque) {

        public Member {
            if (!Double.isFinite(ratio) || ratio == 0.0 || !(inertia >= 0.0) || !(friction >= 0.0)) {
                throw new IllegalArgumentException("a part turns at a ratio other than none, with an inertia and a "
                        + "friction no less than none");
            }
        }

        /** A part that only turns. */
        public static Member turning(final long key, final double ratio, final double inertia,
                                     final double friction) {
            return new Member(key, ratio, inertia, friction, null);
        }
    }
}
