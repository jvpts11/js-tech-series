/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.id;

/**
 * An enum constant with a name of its own for text formats: data files, item components, keywords a player types, and
 * text a saved state is written as. The name is declared with the constant, so renaming the constant in the code does
 * not change what the files and the player see.
 *
 * <p>Look constants up with a {@link StableNames} table built once per enum.
 */
public interface IStableName {

    /** The constant's name: unique within its enum, made of lowercase letters, digits and underscores. */
    String serializedName();
}
