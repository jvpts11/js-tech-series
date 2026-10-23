/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.operation;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * The one moment a kind of Operation may be added to what the series' data networks can be asked.
 *
 * <p>Fired once on the mod bus while the game loads, after every mod has been built and before anything has run.
 * Listening for it is how a mod or an addon adds a kind of Operation; there is no other way in, because after the
 * loading is done the registry is closed and refuses to change. That is what lets a world be sure that what it saved
 * yesterday still means the same thing today. An <b>Operation</b> is a request made of a data network, such as search
 * the storage or craft this; a <b>kind</b> of Operation is what every request of one kind has in common.
 *
 * <pre>{@code
 * public MyAddon(IEventBus modBus) {
 *     modBus.addListener(RegisterOperationTypesEvent.class,
 *             event -> event.operations().register(MyOperations.COUNT_ITEMS));
 * }
 * }</pre>
 *
 * <p>Nothing replaces anything quietly, because which of two won would otherwise depend on the order the mods
 * happened to load in: a kind of Operation under an id already taken is refused, and stops the load.
 */
public final class RegisterOperationTypesEvent extends Event implements IModBusEvent {

    private final OperationTypeRegistry operations;

    /**
     * Public only because the Core builds it from another package. Only the Core fires it; a mod listens for it and
     * never makes one, since a copy over a registry of its own would run the listeners against nothing that is kept.
     */
    @ApiStatus.Internal
    public RegisterOperationTypesEvent(final OperationTypeRegistry operations) {
        this.operations = operations;
    }

    /**
     * The kinds of Operation the network can be asked to carry out.
     *
     * <p>A kind under an id another mod already registered is refused with an exception, which stops the load while
     * somebody is there to read why.
     */
    public OperationTypeRegistry operations() {
        return this.operations;
    }
}
