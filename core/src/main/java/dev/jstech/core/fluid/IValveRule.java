/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fluid;

import net.minecraft.server.level.ServerLevel;

/**
 * When a valve on a line lets what flows through it pass ({@link LineValveBlockEntity}): always, as a check valve
 * does, which only ever lets it one way; while a player has opened it by hand; or while a redstone signal reaches it.
 */
@FunctionalInterface
public interface IValveRule {

    /** Always open: a check valve, which lets what flows through one way only. */
    IValveRule ALWAYS = (level, valve) -> true;
    /** Open while a player has opened it by hand. */
    IValveRule BY_HAND = (level, valve) -> valve.openByHand();
    /** Open while a redstone signal reaches it. */
    IValveRule BY_SIGNAL = (level, valve) -> level.hasNeighborSignal(valve.getBlockPos());

    /** Whether {@code valve} lets what flows through it pass now. */
    boolean open(ServerLevel level, LineValveBlockEntity valve);
}
