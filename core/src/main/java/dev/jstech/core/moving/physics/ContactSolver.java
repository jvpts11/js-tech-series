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
import java.util.List;
import java.util.Map;
import org.joml.Vector3d;

/**
 * Holds bodies apart where they meet, by sequential impulses: each contact pushes its two bodies apart along its way
 * as much as it takes for them to stop closing there, never pulling, and holds them against sliding as much as its
 * grip lets; all the contacts in turn, again and again, until they agree. A contact a little into the other body pushes
 * it out a little a step; one still apart lets the bodies close up to touching, and no further, so a fast body stops
 * where it meets, never through; a blow bounces a little. Each contact starts from the push it gave the step before,
 * so a body resting on many points rests still.
 */
public final class ContactSolver {

    private final List<Contact> live = new ArrayList<>();
    private final List<Contact> spare = new ArrayList<>();
    /* The pushes each contact gave last step, by its key, for it to start from this step. */
    private Map<Long, double[]> before = new HashMap<>();
    private Map<Long, double[]> after = new HashMap<>();
    private final Vector3d relative = new Vector3d();
    private final Vector3d scratch = new Vector3d();
    private final Vector3d push = new Vector3d();

    /** How many times every contact is worked in a step. */
    public static final int ITERATIONS = 10;
    /* How much of its depth a contact into the other body pushes out a second, and the depth it lets be. */
    private static final double BAUMGARTE = 0.2;
    private static final double SLOP = 0.005;
    /* The fastest a contact pushes a body out of another, in metres a second, so a deep one does not throw it. */
    private static final double MOST_PUSH = 3.0;
    /* How much of last step's push a contact starts from. */
    private static final double WARM = 0.85;
    /* Below this a way is taken for none. */
    private static final double TINY = 1.0e-9;
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
        final Contact contact = spare.isEmpty() ? new Contact() : spare.remove(spare.size() - 1);
        contact.set(a, b, point, normal, apart, grip, key);
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
        before.clear();
        after.clear();
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
        // Every contact is readied as the bodies come, before any starts from its last push.
        boolean blow = false;
        for (final Contact contact : live) {
            prepare(contact, seconds, settles);
            blow |= contact.closing > settles;
        }
        for (final Contact contact : live) {
            warm(contact);
        }
        // A blow has no push of the step before to start from: it is worked longer, so its many points agree.
        return blow ? iterations * BLOW_TIMES : iterations;
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
            final double[] kept = before.get(contact.key);
            final double[] into = kept != null ? kept : new double[3];
            into[0] = contact.normalImpulse;
            into[1] = contact.firstImpulse;
            into[2] = contact.secondImpulse;
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
        contact.closing = closing;
        double bias;
        if (contact.apart > 0) {
            bias = -contact.apart / seconds;
        } else {
            bias = Math.min(MOST_PUSH, BAUMGARTE * Math.max(0, -contact.apart - SLOP) / seconds);
        }
        // A blow within this step bounces a little.
        if (closing > settles && closing * seconds > contact.apart) {
            bias = Math.max(bias, ContactMath.bounce(closing) * closing);
        }
        contact.bias = bias;
    }

    /* Starts the contact from the push it gave the step before, if it was there. */
    private void warm(final Contact contact) {
        final double[] last = before.get(contact.key);
        if (last != null) {
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
            // The ways along the face, the same for the same way from step to step.
            final double x = Math.abs(normal.x);
            final double y = Math.abs(normal.y);
            final double z = Math.abs(normal.z);
            if (x <= y && x <= z) {
                this.first.set(0, normal.z, -normal.y);
            } else if (y <= z) {
                this.first.set(-normal.z, 0, normal.x);
            } else {
                this.first.set(normal.y, -normal.x, 0);
            }
            this.first.normalize();
            this.normal.cross(this.first, this.second);
        }
    }
}
