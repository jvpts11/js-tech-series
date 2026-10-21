/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

/**
 * What a module does for whoever wears it while it is on and has its energy: makes them faster or tougher, lets them
 * see in the dark. Each second it works it is asked again; what it stops asking for goes, so a module taken out, turned
 * off or out of energy leaves nothing behind. A mod adds kinds of its own.
 */
public interface IModuleEffect {

    /** The kind of effect, which reads and writes it. */
    ModuleEffectType<?> type();

    /** What it does this second, told to {@code wearing}. */
    void apply(ModuleWearing wearing);
}
