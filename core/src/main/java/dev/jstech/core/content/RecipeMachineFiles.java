/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

/**
 * The data files that say which blocks run which recipe types, so a mod that plans work can place a recipe on a
 * machine without knowing the mod that made the machine.
 *
 * <p>Each file lives in {@code data/<namespace>/recipe_machines/} of any mod or pack and is one JSON object: a
 * recipe type's id to the ids of the blocks that run it, the preferred first. A mod's declared machines are written
 * there from {@link BlockBuilder#machineFor}.
 */
public final class RecipeMachineFiles {

    /** The data folder the files are read from. */
    public static final String FOLDER = "recipe_machines";

    private RecipeMachineFiles() {
    }
}
