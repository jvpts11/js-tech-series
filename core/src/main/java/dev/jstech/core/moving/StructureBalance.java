/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.physics.ForceSum;
import java.util.EnumSet;
import java.util.Set;

/**
 * What the server's owner sets of the moving structures, from the series' balance file: how many blocks one structure
 * may be made of, how much of a tick the physics of a dimension's structures may take, whether a broken one parts,
 * which of the forces of the physics act, whether the wind blows on them, whether their blows break blocks, and
 * whether the physics tool is made by its recipe. Read from many threads, set from the server's.
 */
public final class StructureBalance {

    /** How many blocks a structure is made of at most, when nothing is set. */
    public static final int DEFAULT_MOST_BLOCKS = 65_536;
    /** How much of a tick a dimension's structures' physics takes at most, in milliseconds, when nothing is set. */
    public static final double DEFAULT_PHYSICS_MILLIS = 10.0;
    private static volatile int mostBlocks = DEFAULT_MOST_BLOCKS;
    private static volatile double physicsMillis = DEFAULT_PHYSICS_MILLIS;
    private static volatile boolean split = true;
    /* The forces of the physics turned off, copied whole as one changes: what is read is never half-changed. */
    private static volatile Set<ForceSum.ForceKind> off = EnumSet.noneOf(ForceSum.ForceKind.class);
    private static volatile boolean wind = true;
    private static volatile boolean impacts = true;
    private static volatile boolean physicsToolRecipe = true;

    private StructureBalance() {
    }

    /**
     * Whether the force {@code kind} acts in the physics of moving structures: each can be turned off, gravity, drag,
     * lift, thrust, buoyancy (and the balloons with it), levitation and magnetism; the wheels always act.
     */
    public static boolean force(final ForceSum.ForceKind kind) {
        return !off.contains(kind == ForceSum.ForceKind.BALLOONS ? ForceSum.ForceKind.BUOYANCY : kind);
    }

    /** Turns the force {@code kind} of the physics on or off; from the balance file. */
    public static synchronized void force(final ForceSum.ForceKind kind, final boolean on) {
        final Set<ForceSum.ForceKind> now = EnumSet.noneOf(ForceSum.ForceKind.class);
        now.addAll(off);
        if (on) {
            now.remove(kind);
        } else {
            now.add(kind);
        }
        off = now;
    }

    /** Sets whether gravity acts on moving structures; from the balance file. */
    public static void gravity(final boolean on) {
        force(ForceSum.ForceKind.GRAVITY, on);
    }

    /** Sets whether their faces drag; from the balance file. */
    public static void drag(final boolean on) {
        force(ForceSum.ForceKind.DRAG, on);
    }

    /** Sets whether their wings lift; from the balance file. */
    public static void lift(final boolean on) {
        force(ForceSum.ForceKind.LIFT, on);
    }

    /** Sets whether their engines push; from the balance file. */
    public static void thrust(final boolean on) {
        force(ForceSum.ForceKind.THRUST, on);
    }

    /** Sets whether liquids, the air their hulls hold and their balloons hold them up; from the balance file. */
    public static void buoyancy(final boolean on) {
        force(ForceSum.ForceKind.BUOYANCY, on);
    }

    /** Sets whether their levitators hold them up; from the balance file. */
    public static void levitation(final boolean on) {
        force(ForceSum.ForceKind.LEVITATION, on);
    }

    /** Sets whether their magnets pull; from the balance file. */
    public static void magnetism(final boolean on) {
        force(ForceSum.ForceKind.MAGNETISM, on);
    }

    /** Whether the wind blows on moving structures: off, the air they meet is still. */
    public static boolean wind() {
        return wind;
    }

    /** Sets whether the wind blows on moving structures; from the balance file. */
    public static void wind(final boolean blows) {
        wind = blows;
    }

    /** Whether a blow harder than a block holds breaks it, of the world or of a structure. */
    public static boolean impacts() {
        return impacts;
    }

    /** Sets whether blows break blocks; from the balance file. */
    public static void impacts(final boolean breaks) {
        impacts = breaks;
    }

    /** Whether the physics tool is made by its recipe. */
    public static boolean physicsToolRecipe() {
        return physicsToolRecipe;
    }

    /** Sets whether the physics tool is made by its recipe; from the balance file. */
    public static void physicsToolRecipe(final boolean made) {
        physicsToolRecipe = made;
    }

    /** How many blocks a structure is made of at most. */
    public static int mostBlocks() {
        return mostBlocks;
    }

    /** Sets how many blocks a structure is made of at most; from the balance file. */
    public static void mostBlocks(final int most) {
        mostBlocks = Math.max(1, most);
    }

    /** How much of a tick the physics of a dimension's structures takes at most, in milliseconds. */
    public static double physicsMillis() {
        return physicsMillis;
    }

    /**
     * Sets how much of a tick the physics of a dimension's structures takes at most; from the balance file. None at
     * all, which no setting gives, has every structure wait every other tick, as a test of their waiting.
     */
    public static void physicsMillis(final double millis) {
        physicsMillis = Math.max(0, millis);
    }

    /** Whether a structure whose blocks are no longer all joined becomes as many structures as it now is. */
    public static boolean split() {
        return split;
    }

    /** Sets whether a structure no longer all joined parts; from the balance file. */
    public static void split(final boolean parts) {
        split = parts;
    }
}
