/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * What a kind of component takes, beyond what every component takes.
 *
 * <p>Every value is first held to the bounds all components share: no handlers, files, threads, windows or
 * widgets, nothing that holds itself, at most 32 KB, sixteen levels deep and 4096 parts. What reaches a validator
 * has passed those, and comes as plain values: null, a {@code Boolean}, an {@code Integer}, a {@code Long}, a
 * {@code Double}, a {@code Character}, a {@code String}, a {@code List} or a {@code Map}, a map keeping its order and
 * the keys the program used. Answering false refuses it: a program handing over data the kind refuses is stopped and
 * told, and an action a player's screen sends that the kind refuses never reaches the program.
 */
@ApiStatus.Experimental
public interface IComponentValidator {

    /** Takes anything within the shared bounds. */
    IComponentValidator ANYTHING = new IComponentValidator() {
    };

    /** Whether a program may hand a component of this kind that value to show. */
    default boolean data(final Object value) {
        return true;
    }

    /**
     * Whether a player's screen may tell the program that this happened to a component of this kind.
     *
     * @param name  what happened, as the kind's renderer names it
     * @param value what came with it
     */
    default boolean action(final String name, final Object value) {
        return true;
    }
}
