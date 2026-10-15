/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import net.minecraft.server.level.ServerLevel;

/**
 * What befalls a wire that something it is not made for reaches, told once for each tick it is so: a cable's own rule,
 * declared with {@link CableType.Builder#beyond}. The Core's ready ones are in {@link BreachRules}.
 */
@FunctionalInterface
public interface IBreachRule {

    /** {@code breach} reached the wire in tick {@code gameTime}. */
    void exposed(ServerLevel level, LineBreach breach, long gameTime);
}
