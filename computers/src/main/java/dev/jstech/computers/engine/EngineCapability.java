/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.engine;

import dev.jstech.core.id.IStableName;
import org.jetbrains.annotations.ApiStatus;

/**
 * What a Network Operations Engine offers beyond what every engine answers.
 *
 * <p>Every engine pulls, pushes, moves, exports, fills, crafts, previews a craft's plan and runs the core of the
 * network's language. These are the extras, the part of the contract an engine may or may not have. A program that
 * needs one asks the network for the capability, never for an engine by name, so it runs on any engine that offers
 * it, and a program that needs none runs on every engine.
 */
@ApiStatus.Experimental
public enum EngineCapability implements IStableName {
    /** Saved views and procedures, kept on the Mainframe and run by name. */
    PROCEDURES_AND_VIEWS("procedures_and_views"),
    /** A state the network is told to keep, which the engine works towards and then holds. */
    DECLARATIVE_STATE("declarative_state"),
    /** Subscriptions a program holds to changes in the network, answered as they happen. */
    SUBSCRIPTIONS("subscriptions"),
    /** The plan shown as it was made, and afterwards as it ran. */
    EXPLAIN("explain"),
    /** Hints that change the plan the engine makes. */
    PLANNER_HINTS("planner_hints"),
    /** Operators, rules and statistics that other mods register into the engine's planner. */
    EXTENSIONS("extensions");

    private final String serializedName;

    EngineCapability(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
