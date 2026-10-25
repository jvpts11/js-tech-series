/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.jstechseries.network.NetworkCategory;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.text.TextKey;

import java.util.EnumSet;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * What one kind of Operation is: what it takes, where it can run, and what carries it out.
 *
 * <p>An Operation is a request a player or a program makes of a data network: search the storage, craft this, move
 * that, run the accelerator. The network does not run it on the spot; it queues it, gives it to a member that can
 * carry it out, and follows its {@link OperationStatus} from {@link OperationStatus#PENDING PENDING} to one of the
 * outcomes. A kind of Operation is what every request of that kind has in common, and this record is how a mod says
 * it:
 *
 * <pre>{@code
 * public record CoolArgs(int kelvin) implements IOperationArgs {
 *     public static final MapCodec<CoolArgs> TEXT = RecordCodecBuilder.mapCodec(cool -> cool.group(
 *             Codec.INT.fieldOf("kelvin").forGetter(CoolArgs::kelvin)).apply(cool, CoolArgs::new));
 * }
 *
 * public static final OperationType<CoolArgs> COOL = new OperationType<>(
 *         "myaddon:cool",                  // saved in worlds: never change it
 *         MyTexts.COOL,                    // what the logs and screens call it, a sentence of a @TextHolder
 *         CoolArgs.class,                  // what a request of this kind carries
 *         OperationCategory.INDUSTRIAL,    // the family it belongs to
 *         IndustrialTier.T1,               // the lowest industrial tier that can run it
 *         EnumSet.of(NetworkCategory.C),   // who may run it: the members given work
 *         CoolArgs.TEXT,                   // how a request written as text is read: RUN myaddon:cool WITH kelvin = 300
 *         request -> ((ReactorBlockEntity) request.machine().orElseThrow()).cool(request));
 * }</pre>
 *
 * <p>The handler is handed an {@link IOperationRequest}: what it carries, the machine of the network given it, and
 * the way that machine says how the work goes until it is over, or hears that it was called off.
 *
 * <p>Registered once while the game loads (see {@link OperationTypeRegistry}), and read from then on by everything
 * that dispatches one. What it needs of a network cannot change after it is declared: the set is copied on the way
 * in, so that whoever declared it cannot go on holding the same set and quietly change what the Operation requires
 * later.
 *
 * @param id                 the kind's id, {@code namespace:path} in lower case, with the mod's id as the namespace;
 *                           worlds save it and the network sends it, so it never changes once released
 * @param name               what the kind is called where a player reads it: the log, the list of what runs, the
 *                           statistics
 * @param argsClass          the record a request of this kind carries
 * @param category           the family it belongs to, which the screens that list Operations group by
 * @param minTier            the lowest industrial tier, the axis of how far a world's industry has come, that can
 *                           run it
 * @param requiredCategories which members of a network may run it (see {@link NetworkCategory}); never empty
 * @param text               how a request of this kind written as text is read, by its fields' names, so a player
 *                           or a program can ask for one in the network's language; null for a kind only code asks
 *                           for, such as a button on the machine's own screen
 * @param handler            the code that takes a request and starts or does the work
 */
public record OperationType<T extends IOperationArgs>(
        String id,
        TextKey name,
        Class<T> argsClass,
        OperationCategory category,
        IndustrialTier minTier,
        EnumSet<NetworkCategory> requiredCategories,
        @Nullable MapCodec<T> text,
        IOperationHandler<T> handler
) {

    /** A kind only code asks for: no request of it is ever read from text. */
    public OperationType(final String id, final TextKey name, final Class<T> argsClass,
                         final OperationCategory category, final IndustrialTier minTier,
                         final EnumSet<NetworkCategory> requiredCategories, final IOperationHandler<T> handler) {
        this(id, name, argsClass, category, minTier, requiredCategories, null, handler);
    }

    public OperationType {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
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

    /** Whether a request of this kind can be read from text, as the network's language writes one. */
    public boolean readsText() {
        return this.text != null;
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
