/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.client;

import org.jetbrains.annotations.ApiStatus;

/** How a component's renderer tells the program what a player did to it. */
@ApiStatus.Experimental
@FunctionalInterface
public interface IComponentActions {

    /**
     * Tells the program, through its {@code OnAction} handler, that this happened.
     *
     * <p>The value is plain: null, a {@code Boolean}, an {@code Integer}, a {@code Long}, a {@code Double}, a
     * {@code Character}, a {@code String}, a {@code List} or a {@code Map} of them. The server holds it to the bounds
     * every component's value is held to and to the kind's own validator before the program hears anything, so
     * something it refuses simply never arrives.
     *
     * @param name what happened, a few letters, at most 64
     * @param value what came with it
     */
    void send(String name, Object value);
}
