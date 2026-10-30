/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.ApiStatus;

/**
 * A machine of the network whose work waits on items: a crane waiting for the blocks of the rocket it builds. While
 * a request it was given is {@link OperationStatus#WAITING WAITING}, whatever runs the network may read what it waits
 * for and bring it, from what the network stores and, where the network can make it, made first. The machine takes
 * what it waits for through its inbox, and no more of each than it needs.
 */
@ApiStatus.Experimental
public interface IItemNeeds {

    /** What it waits for now, each item with how many more it takes, what it needs first first; none for nothing. */
    List<ItemNeed> needs();

    /** Where what it waits for goes in; none while it takes nothing. */
    Optional<IItemHandler> inbox();

    /**
     * An item a machine waits for, and how many more of it it takes.
     *
     * @param item  the item
     * @param count how many more, at least one
     */
    record ItemNeed(Item item, int count) {

        public ItemNeed {
            Objects.requireNonNull(item, "item");
            if (count < 1) {
                throw new IllegalArgumentException("an item needed is needed at least once: " + count);
            }
        }
    }
}
