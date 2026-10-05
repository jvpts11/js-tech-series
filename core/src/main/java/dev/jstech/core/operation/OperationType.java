/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.operation;

import dev.jstech.core.network.NetworkCategory;
import dev.jstech.core.tier.IndustrialTier;

import java.util.EnumSet;
import java.util.Objects;

/**
 * What one kind of Operation is: what it takes, where it can run, and what carries it out.
 *
 * <p>An Operation is a request a player or a program makes of a data network: search the storage, craft this, move
 * that. The network does not run it on the spot; it queues it, gives it to a computer that can carry it out, and
 * follows its {@link OperationStatus} from {@link OperationStatus#PENDING PENDING} to one of the outcomes. A kind of
 * Operation is what every request of that kind has in common, and this record is how a mod says it:
 *
 * <pre>{@code
 * public record CountItemsArgs(String item) implements IOperationArgs {
 * }
 *
 * public static final OperationType<CountItemsArgs> COUNT_ITEMS = new OperationType<>(
 *         "myaddon:count_items",          // saved in worlds: never change it
 *         CountItemsArgs.class,            // what a request of this kind carries
 *         OperationCategory.STORAGE,       // the family it belongs to
 *         IndustrialTier.T1,               // the lowest industrial tier that can run it
 *         EnumSet.of(NetworkCategory.C),   // who may run it: the computers
 *         args -> OperationStatus.COMPLETED);
 * }</pre>
 *
 * <p>Registered once while the game loads (see {@link OperationTypeRegistry}), and read from then on by everything
 * that dispatches one. What it needs of a network cannot change after it is declared: the set is copied on the way
 * in, so that whoever declared it cannot go on holding the same set and quietly change what the Operation requires
 * later.
 *
 * @param id                 the kind's id, {@code namespace:path} in lower case, with the mod's id as the namespace;
 *                           worlds save it and the network sends it, so it never changes once released
 * @param argsClass          the record a request of this kind carries
 * @param category           the family it belongs to, which the screens that list Operations group by
 * @param minTier            the lowest industrial tier, the axis of how far a world's industry has come, that can
 *                           run it
 * @param requiredCategories which members of a network may run it (see {@link NetworkCategory}); never empty
 * @param handler            the code that takes a request and starts or does the work
 */
public record OperationType<T extends IOperationArgs>(
        String id,
        Class<T> argsClass,
        OperationCategory category,
        IndustrialTier minTier,
        EnumSet<NetworkCategory> requiredCategories,
        IOperationHandler<T> handler
) {

    public OperationType {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(argsClass, "argsClass must not be null");
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(minTier, "minTier must not be null");
        Objects.requireNonNull(requiredCategories, "requiredCategories must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        if (requiredCategories.isEmpty()) {
            throw new IllegalArgumentException(
                    "requiredCategories must contain at least one NetworkCategory; got empty set for " + id);
        }
        if (!id.matches("[a-z0-9_]+:[a-z0-9_/]+")) {
            throw new IllegalArgumentException(
                    "id must be in 'namespace:path' form using [a-z0-9_/]; got: " + id);
        }
        requiredCategories = EnumSet.copyOf(requiredCategories);
    }

    /**
     * What this Operation needs of a network, as a set of its own.
     *
     * <p>A copy, so that reading what an Operation requires cannot become a way of changing it. The set kept
     * inside was already copied from whoever declared it, and this keeps that true on the way out as well.
     */
    @Override
    public EnumSet<NetworkCategory> requiredCategories() {
        return EnumSet.copyOf(this.requiredCategories);
    }
}
