/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import net.minecraft.world.entity.Entity;

/** What a mod tells an agent of an entity, a vehicle or a creature of its own; nothing of others' entities. */
@FunctionalInterface
public interface IEntityReading {

    /** Tells {@code report} what there is to tell of {@code entity}; nothing for an entity not the mod's. */
    void read(Entity entity, AgentReport report);
}
