/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3d;

/**
 * Holds bodies apart where they meet, by sequential impulses: each contact pushes its two bodies apart along its way
 * as much as it takes for them to stop closing there, never pulling, and holds them against sliding as much as its
 * grip lets; all the contacts in turn, again and again, until they agree. A contact a little into the other body pushes
 * it out a little a step; one still apart, or less than a hair into it, lets the bodies close up to resting a hair into
 * each other, and no further, so a fast body stops where it meets, never through, and every point of a face rests as
 * deep; a blow bounces a little as the bodies touch, as fast as they came. Each contact
 * starts from the whole push it gave the step before, so a body resting on many points rests still. A body into
 * another is moved out of it, never sped out: what pushes it out moves it over the step and is forgotten, so however a
 * stack came to sink into itself, it is pushed apart without its top being thrown.
 */
public final class ContactSolver {

    private final List<Contact> live = new ArrayList<>();
    private final List<Contact> spare = new ArrayList<>();
    /* The pushes each contact gave last step, by its key, for it to start from this step. */
    private Map<Long, double[]> before = new HashMap<>();
    private Map<Long, double[]> after = new HashMap<>();
    /* The keys of this step's contacts started from a push of the step before, each once. */
    private final Set<Long> started = new HashSet<>();
    private final Vector3d relative = new Vector3d();
    private final Vector3d scratch = new Vector3d();
    private final Vector3d push = new Vector3d();
    /* The group the next contact added alone is put in, each its own: below every group a caller names. */
    private long lone = -1;

    /** How many times every contact is worked in a step. */
    public static final int ITERATIONS = 10;
    /* How much of its depth a contact into the other body moves it out a step, and the depth it lets be. */
    private static final double BAUMGARTE = 0.2;
    private static final double SLOP = 0.005;
    /* The fastest a contact moves a body out of another, in metres a second, so a deep one comes out over steps. */
    private static final double MOST_PUSH = 3.0;
    /*
     * How much of last step's push a contact starts from: all of it, so a body resting on many points, a great hull on
     * the ground, holds still from the first pass instead of sinking a little each step and righting itself.
     */
    private static final double WARM = 1.0;
    /*
     * What is kept of a contact from step to step: its three pushes, how fast it came while apart, and whether its
     * bodies were still apart there.
     */
    private static final int KEPT = 5;
    private static final int COMING = 3;
    private static final int WAS_APART = 4;
    /* Below this a way is taken for none. */
    private static final double TINY = 1.0e-9;
    /* A way leaning towards south or north more than this, the sine of 45 degrees, lies nearest that axis. */
    private static final double NEAR_Z = Math.sqrt(0.5);
    /* A contact's point is known again to an eighth of a block. */
    private static final double GRID = 8;
    /* The steps of a key's mixing, the golden ratio's and two of good spread, so near points get far keys. */
    private static final long GOLDEN = -7_046_029_254_386_353_131L;
    private static final long SPREAD_FIRST = -4_658_895_280_553_007_687L;
    private static final long SPREAD_SECOND = -7_723_592_293_110_705_685L;
    private static final int SHIFT_FIRST = 30;
    private static final int SHIFT_SECOND = 27;
    private static final int SHIFT_LAST = 31;
    /* A blow closing slower than this, in metres a second, does not bounce, so a body at rest settles. */
    private static final double SETTLES = 1.0;
    /* How many times longer a step with a blow is worked. */
    private static final int BLOW_TIMES = 3;

    /**
     * A contact between body {@code a} and body {@code b} at the world's {@code point}, its way {@code normal} from
     * {@code b} to {@code a}, as far apart there as {@code apart} (less than nought when into each other), holding
     * against sliding by {@code grip}; {@code key} names it from step to step. The vectors are copied.
     */
    public Contact add(final RigidBody a, final RigidBody b, final Vector3d point, final Vector3d normal,
                       final double apart, final double grip, final long key) {
        return add(a, b, point, normal, apart, grip, key, lone--);
    }

    /**
     * A contact as {@link #add(RigidBody, RigidBody, Vector3d, Vector3d, double, double, long)}, one of the points
     * where the same two shapes meet, {@code group} naming them, added one after another: the points of a group
     * bounce together, once any of them touches, so a face landing a little tilted is not set turning.
     */
    public Contact add(final RigidBody a, final RigidBody b, final Vector3d point, final Vector3d normal,
                       final double apart, final double grip, final long key, final long group) {
        final Contact contact = spare.isEmpty() ? new Contact() : spare.remove(spare.size() - 1);
        contact.set(a, b, point, normal, apart, grip, key);
        contact.group = group;
        live.add(contact);
        return contact;
    }

    /** The contacts of this step. */
    public List<Contact> contacts() {
        return live;
    }

    /**
     * The key of a contact from step to step: the two things that met, {@code pair}, and where on the first body
     * it is, {@code local}, in the body's own frame, to an eighth of a block, so the same point is known again however
     * the body moved, and its push carried over.
     */
    public static long key(final long pair, final Vector3d local) {
        long hash = mix(pair);
        hash = mix(hash + Math.round(local.x * GRID));
        hash = mix(hash + Math.round(local.y * GRID));
        return mix(hash + Math.round(local.z * GRID));
    }

    /** Forgets this step's contacts, keeping what they gave for the next step to start from. */
    public void clear() {
        spare.addAll(live);
        live.clear();
    }

    /** Forgets everything, last step's pushes too. */
    public void reset() {
        clear();
        forget();
    }

    /** Forgets the pushes its contacts gave, keeping the contacts. */
    public void forget() {
        before.clear();
        after.clear();
    }

    /**
     * Forgets the pushes its last step's contacts gave against any body but {@code kept}: for a body falling asleep,
     * which next looks about afresh against what may have moved since, rather than with pushes of another moment that
     * would kick what rests on it, while its pushes against {@code kept}, the ground, which does not move, still hold.
     */
    public void forgetBut(final RigidBody kept) {
        for (final Contact contact : live) {
            if (contact.a != kept && contact.b != kept) {
                before.remove(contact.key);
            }
        }
    }

    /**
     * Works the contacts over a step of {@code seconds}: each readied, started from its push of the step before,
     * then worked {@code iterations} times; their pushes are kept for the next step. A blow bounces when it closes
     * faster than a metre a second.
     */
    public void solve(final double seconds, final int iterations) {
        solve(seconds, iterations, SETTLES);
    }

    /**
     * Works the contacts as {@link #solve(double, int)}, a blow bouncing only when it closes faster than
     * {@code settles}, in metres a second: more than the pull gives a body resting in a step, so a body at rest under
     * a strong pull does not bounce.
     */
    public void solve(final double seconds, final int iterations, final double settles) {
        iterate(begin(seconds, iterations, settles));
        end();
    }

    /**
     * Readies the contacts for a step of {@code seconds}, each started from its push of the step before, a blow
     * bouncing only when it closes faster than {@code settles}: how many times they are to be worked, from
     * {@code iterations}, more when a blow came. What holds the same bodies by joints may be worked between.
     */
    public int begin(final double seconds, final int iterations, final double settles) {
        final int times = ready(seconds, iterations, settles);
        warm();
        return times;
    }

    /**
     * Readies the contacts for a step as {@link #begin} does, none started yet from its push of the step before:
     * the contacts of several solvers on the same bodies are each readied as the bodies come, and only then started
     * ({@link #warm()}), so none reads the bodies as another's start already pushed them. How many times they are to
     * be worked.
     */
    public int ready(final double seconds, final int iterations, final double settles) {
        // The points of a group are as near as the nearest of them, so they bounce or close up together.
        int start = 0;
        while (start < live.size()) {
            final long group = live.get(start).group;
            double least = live.get(start).apart;
            int end = start + 1;
            while (end < live.size() && live.get(end).group == group) {
                least = Math.min(least, live.get(end).apart);
                end++;
            }
            for (int i = start; i < end; i++) {
                live.get(i).groupApart = least;
            }
            start = end;
        }
        // Every contact is readied as the bodies come, before any starts from its last push.
        boolean blow = false;
        for (final Contact contact : live) {
            prepare(contact, seconds, settles);
            blow |= contact.closing > settles;
        }
        // A blow has no push of the step before to start from: it is worked longer, so its many points agree.
        return blow ? iterations * BLOW_TIMES : iterations;
    }

    /** Starts each contact {@link #ready readied} from the push it gave the step before. */
    public void warm() {
        started.clear();
        for (final Contact contact : live) {
            warm(contact);
        }
    }

    /** Works every contact {@code times} times, there and back by turns. */
    public void iterate(final int times) {
        for (int i = 0; i < times; i++) {
            round(i);
        }
    }

    /**
     * Works every contact once, the {@code index}th time: there on an even time and back on an odd one, so no contact
     * is always worked first and the pushes share out evenly.
     */
    public void round(final int index) {
        if (index % 2 == 0) {
            for (final Contact contact : live) {
                work(contact);
            }
        } else {
            for (int j = live.size() - 1; j >= 0; j--) {
                work(live.get(j));
            }
        }
    }

    /** Keeps what each contact gave, for the next step to start from. */
    public void end() {
        after.clear();
        for (final Contact contact : live) {
            final double[] already = after.get(contact.key);
            if (already != null) {
                /*
                 * Two points known alike, as near as a key tells, as the corners of a face turned a hair on another
                 * are: what they gave is kept as one push, for the next step to start from once, not twice.
                 */
                already[0] += contact.normalImpulse;
                already[1] += contact.firstImpulse;
                already[2] += contact.secondImpulse;
                already[COMING] = Math.max(already[COMING], contact.coming);
                continue;
            }
            final double[] kept = before.get(contact.key);
            final double[] into = kept != null ? kept : new double[KEPT];
            into[0] = contact.normalImpulse;
            into[1] = contact.firstImpulse;
            into[2] = contact.secondImpulse;
            into[COMING] = contact.coming;
            into[WAS_APART] = contact.groupApart > SLOP ? 1 : 0;
            after.put(contact.key, into);
        }
        final Map<Long, double[]> swapped = before;
        before = after;
        after = swapped;
    }

    private void prepare(final Contact contact, final double seconds, final double settles) {
        final RigidBody a = contact.a;
        final RigidBody b = contact.b;
        contact.normalMass = mass(a, b, contact.normal, contact.point);
        contact.firstMass = mass(a, b, contact.first, contact.point);
        contact.secondMass = mass(a, b, contact.second, contact.point);
        relativeAt(contact);
        final double closing = -relative.dot(contact.normal);
        // Apart the step before, the bodies were let close only up to resting: they meet now as fast as they came.
        final double[] last = before.get(contact.key);
        final double came = last != null && closing > 0 ? Math.max(closing, last[COMING]) : closing;
        // Only a meeting bounces, new or still apart the step before: a contact held touching from step to step is
        // bodies resting, and bouncing it, as they shake by a hair, would set a stack jumping with the blows of those
        // landing on it.
        final boolean meeting = last == null || last[COMING] > 0 || last[WAS_APART] > 0;
        /*
         * The bodies close up to the depth it lets be and no further, whether apart or a hair into each other: so
         * every point of a face rests as deep, and a body left leaning a hair on another settles flat rather than
         * held leaning by its shallower points, every joint of a stack adding its lean.
         */
        double bias = contact.apart > -SLOP ? -(contact.apart + SLOP) / seconds : 0;
        // Into the other, it is moved out a little a step, never sped out: a speed given to push it out would stay
        // with it once out, and a stack sunk into itself would throw its top as high as each of its sinkings sped it.
        contact.pushOut = contact.apart > 0 ? 0
                : Math.min(MOST_PUSH, BAUMGARTE * Math.max(0, -contact.apart - SLOP) / seconds);
        contact.pushedOut = 0;
        contact.coming = 0;
        if (contact.groupApart > SLOP) {
            // Reached within this step, they close up to resting and no further; the blow comes as they touch, so
            // a fast body bounces off the wall it meets, never off the air before it. Only a blow is remembered.
            contact.closing = closing;
            if (meeting && closing > settles && closing * seconds > contact.apart) {
                contact.coming = came;
            }
        } else {
            contact.closing = came;
            if (meeting && came > settles && came * seconds > contact.apart) {
                bias = Math.max(bias, ContactMath.bounce(came) * came);
            }
        }
        contact.bias = bias;
    }

    /*
     * Starts the contact from the push it gave the step before, if it was there and touching: one still apart then
     * only slowed the bodies coming, unevenly from point to point, and its points share the blow out evenly afresh.
     * Of two points known alike, the first starts from what both gave, and the other from nothing.
     */
    private void warm(final Contact contact) {
        final double[] last = before.get(contact.key);
        if (last != null && last[COMING] == 0 && started.add(contact.key)) {
            contact.normalImpulse = last[0] * WARM;
            contact.firstImpulse = last[1] * WARM;
            contact.secondImpulse = last[2] * WARM;
            apply(contact, contact.normal, contact.normalImpulse);
            apply(contact, contact.first, contact.firstImpulse);
            apply(contact, contact.second, contact.secondImpulse);
        }
    }

    private void work(final Contact contact) {
        // Against sliding first, as much as the push along the way now lets.
        final double most = contact.grip * contact.normalImpulse;
        relativeAt(contact);
        final double firstNow = clamp(contact.firstImpulse - relative.dot(contact.first) * contact.firstMass, most);
        apply(contact, contact.first, firstNow - contact.firstImpulse);
        contact.firstImpulse = firstNow;
        relativeAt(contact);
        final double secondNow = clamp(contact.secondImpulse - relative.dot(contact.second) * contact.secondMass,
                most);
        apply(contact, contact.second, secondNow - contact.secondImpulse);
        contact.secondImpulse = secondNow;
        relativeAt(contact);
        final double along = relative.dot(contact.normal);
        final double normalNow = Math.max(0, contact.normalImpulse + (contact.bias - along) * contact.normalMass);
        apply(contact, contact.normal, normalNow - contact.normalImpulse);
        contact.normalImpulse = normalNow;
        // Into the other, as fast out as its depth asks, on the way the bodies are pushed out alone.
        if (contact.pushOut > 0) {
            contact.a.pushedOutAt(contact.point, relative);
            contact.b.pushedOutAt(contact.point, scratch);
            final double out = Math.max(0, contact.pushedOut + (contact.pushOut - relative.sub(scratch)
                    .dot(contact.normal)) * contact.normalMass);
            contact.a.pushOut(contact.normal, out - contact.pushedOut, contact.point);
            contact.b.pushOut(contact.normal, contact.pushedOut - out, contact.point);
            contact.pushedOut = out;
        }
    }

    /* How fast a goes past b at the contact's point, into {@link #relative}. */
    private void relativeAt(final Contact contact) {
        contact.a.velocityAt(contact.point, relative);
        contact.b.velocityAt(contact.point, scratch);
        relative.sub(scratch);
    }

    /* Pushes a by {@code size} along {@code way} at the contact's point, and b back as much. */
    private void apply(final Contact contact, final Vector3d way, final double size) {
        if (size == 0) {
            return;
        }
        contact.a.kick(push.set(way), size, contact.point);
        contact.b.kick(push.set(way), -size, contact.point);
    }

    /* One over how much the two bodies give, together, against a push along {@code way} at {@code at}. */
    private static double mass(final RigidBody a, final RigidBody b, final Vector3d way, final Vector3d at) {
        final double give = a.give(way, at) + b.give(way, at);
        return give > TINY ? 1.0 / give : 0;
    }

    private static double clamp(final double value, final double most) {
        return Math.max(-most, Math.min(most, value));
    }

    /* Spreads the bits of {@code value} over the whole long, the last steps of a well known mixer. */
    private static long mix(final long value) {
        long z = value + GOLDEN;
        z = (z ^ (z >>> SHIFT_FIRST)) * SPREAD_FIRST;
        z = (z ^ (z >>> SHIFT_SECOND)) * SPREAD_SECOND;
        return z ^ (z >>> SHIFT_LAST);
    }

    /** Where two bodies meet, and what was done there. */
    public static final class Contact {

        private final Vector3d point = new Vector3d();
        private final Vector3d normal = new Vector3d();
        private final Vector3d first = new Vector3d();
        private final Vector3d second = new Vector3d();
        private RigidBody a;
        private RigidBody b;
        private double apart;
        private double grip;
        private long key;
        private double normalImpulse;
        private double firstImpulse;
        private double secondImpulse;
        private double normalMass;
        private double firstMass;
        private double secondMass;
        private double bias;
        private double closing;
        /* How fast it moves the bodies out of one another this step, as deep as they are, and the push it gave so. */
        private double pushOut;
        private double pushedOut;
        /* How fast the bodies came at it while it was apart and reached this step, to bounce as fast when they meet. */
        private double coming;
        /* The shapes whose meeting it is one point of, and how near the nearest point of them is. */
        private long group;
        private double groupApart;

        /** Where it is, in the world. */
        public Vector3d point() {
            return point;
        }

        /** Its way, from its second body to its first. */
        public Vector3d normal() {
            return normal;
        }

        /** How far apart the bodies were there, less than nought when into each other. */
        public double apart() {
            return apart;
        }

        /** How well it holds against sliding. */
        public double grip() {
            return grip;
        }

        /** Its name from step to step. */
        public long key() {
            return key;
        }

        /** The shapes whose meeting it is one point of. */
        public long group() {
            return group;
        }

        /** How hard it pushed the bodies apart this step, in newton seconds. */
        public double normalImpulse() {
            return normalImpulse;
        }

        /** How fast the bodies closed there before it worked, in metres a second; less than nought when parting. */
        public double closing() {
            return closing;
        }

        /** The energy of the blow it took, in joules: half its push by the speed they closed at. */
        public double blow() {
            return closing > 0 ? 0.5 * normalImpulse * closing : 0;
        }

        /** Its first body. */
        public RigidBody a() {
            return a;
        }

        /** Its second body. */
        public RigidBody b() {
            return b;
        }


        private void set(final RigidBody first, final RigidBody second, final Vector3d at, final Vector3d way,
                         final double distance, final double holding, final long name) {
            this.a = first;
            this.b = second;
            this.point.set(at);
            this.normal.set(way).normalize();
            this.apart = distance;
            this.grip = Math.max(0, holding);
            this.key = name;
            this.normalImpulse = 0;
            this.firstImpulse = 0;
            this.secondImpulse = 0;
            this.closing = 0;
            this.coming = 0;
            // The ways along the face, chosen by the axis the way lies nearest, so a way a hair off upright keeps the
            // same ways from step to step: chosen by its smallest parts instead, they swap at a hair's turn, and the
            // grip carried over from the step before pushes the wrong way, setting a stack swaying.
            if (Math.abs(normal.z) > NEAR_Z) {
                this.first.set(0, -normal.z, normal.y);
            } else {
                this.first.set(-normal.y, normal.x, 0);
            }
            this.first.normalize();
            this.normal.cross(this.first, this.second);
        }
    }
}
