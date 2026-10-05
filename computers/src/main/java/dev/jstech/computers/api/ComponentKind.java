/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * A kind of component a mod adds for Σ# programs to put in their windows.
 *
 * <p>A program makes one with {@code new GenericComponent("yourmod:dial")}, hands it whatever value it likes and hears
 * back through {@code OnAction} what a player did to it. What the kind looks like is drawn on each player's screen
 * by the renderer registered for it on the client; a player whose game has no renderer for it, because the mod is
 * missing there, sees a placeholder naming it. The server keeps the value with the program, saved and sent like any
 * other widget's, so nothing here runs anywhere but on the screens.
 *
 * @param id             the kind's name, in the namespace of the mod that adds it
 * @param reachesOutside whether what it draws or does reaches outside the game, such as reading a web address or a
 *                       file on the player's computer; such a kind is off unless both the server and the player turn
 *                       it on
 * @param validator      what it takes beyond what every component takes
 */
@ApiStatus.Experimental
public record ComponentKind(ResourceLocation id, boolean reachesOutside, IComponentValidator validator) {

    public ComponentKind {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(validator, "validator");
    }

    /** A kind that stays inside the game and takes anything within the shared bounds. */
    public ComponentKind(final ResourceLocation id) {
        this(id, false, IComponentValidator.ANYTHING);
    }
}
