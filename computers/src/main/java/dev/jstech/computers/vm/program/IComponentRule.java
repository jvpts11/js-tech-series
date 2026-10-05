/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * What the runtime needs to know of one kind of generic component: whether it reaches outside the game, and what it
 * takes beyond what every component takes. The kinds themselves are added by mods, outside the runtime, which is told
 * how to look them up through {@link ComponentRules}.
 */
public interface IComponentRule {

    /** The kind's name, as a program names it. */
    String id();

    /** Whether what it draws or does reaches outside the game, which is off unless the server turns it on. */
    boolean reachesOutside();

    /** Whether a program may hand a component of this kind that value, given as plain values. */
    boolean takesData(Object value);

    /** Whether a player's screen may say this happened to a component of this kind. */
    boolean takesAction(String name, Object value);
}
