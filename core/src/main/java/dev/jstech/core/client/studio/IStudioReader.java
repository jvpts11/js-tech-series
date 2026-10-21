/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

/**
 * Reads one part of what a thing is made of, and what is wrong with it across its parts, into the thing's sheet. The
 * Core's readers read the model, the screen, the effects, the sounds, the names, the recipes, the manual page, the
 * tags, the drops, the settings and the GameTests; a mod adds one for a part of its own ({@link StudioReaders}).
 */
@FunctionalInterface
public interface IStudioReader {

    /** Writes into {@code sheet} what it reads of {@code thing}, with what the whole studio read once in context. */
    void read(StudioThing thing, StudioContext context, StudioSheet sheet);
}
