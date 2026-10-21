/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * One thing a creature does, as its data names it: wander, look at players, run from what it fears, follow food, fight.
 * Each becomes one of the game's goals for the creature; a mod adds kinds of its own.
 */
public interface ICreatureBehaviour {

    /** The kind of behaviour, which reads and writes it. */
    CreatureBehaviourType<?> type();

    /** The goal it gives {@code creature}. */
    Goal goal(CreatureEntity creature);

    /** Whether it chooses whom the creature goes for, rather than what it does. */
    default boolean targets() {
        return false;
    }
}
