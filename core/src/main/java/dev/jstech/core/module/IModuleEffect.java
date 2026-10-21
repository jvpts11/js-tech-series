/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

/**
 * What a module does while it is on and has its energy: for whoever wears the item it is in, making them faster or
 * tougher, letting them see in the dark; or to the tool it is in, held in the main hand, making it mine an area or a
 * vein, reach further, or work as though enchanted. A worn one is asked again each second, and a tool's each time
 * the tool is looked at; what it stops asking for goes, so a module taken out, turned off or out of energy leaves
 * nothing behind. A mod adds kinds of its own; a kind does nothing where it has no part.
 */
public interface IModuleEffect {

    /** The kind of effect, which reads and writes it. */
    ModuleEffectType<?> type();

    /** What it does this second for the wearer, told to {@code wearing}; nothing, unless the kind says. */
    default void apply(final ModuleWearing wearing) {
        // A kind with no part in what is worn does nothing.
    }

    /** What it does to the tool it is in, told to {@code tool}; nothing, unless the kind says. */
    default void shape(final ModuleTool tool) {
        // A kind with no part in tools does nothing.
    }
}
