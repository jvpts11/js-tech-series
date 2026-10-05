/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.api;

import dev.jstech.core.language.LanguageRegistry;
import dev.jstech.core.operation.OperationTypeRegistry;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * The one moment anything may be added to what the Core keeps.
 *
 * <p>Fired once on the mod bus while the game loads, after every mod has been built and before anything
 * has run. Listening for it is how an addon adds a language or a kind of Operation; there is no other
 * way in, because after the loading is done every one of these is closed and refuses to change. That is
 * what lets a world be sure that what it saved yesterday still means the same thing today.
 *
 * <ul>
 *   <li>A <b>language</b> is one the computers of the series can be programmed in: a player writes a program in it
 *       and a machine runs it. See {@link #languages()}.</li>
 *   <li>An <b>Operation</b> is a request made of a data network, such as search the storage or craft this; a
 *       <b>kind</b> of Operation is what every request of one kind has in common. See {@link #operations()}.</li>
 * </ul>
 *
 * <p>Listen for it on your mod's own bus, the one your mod's constructor is handed:
 *
 * <pre>{@code
 * public MyAddon(IEventBus modBus) {
 *     modBus.addListener(MyAddon::register);
 * }
 *
 * private static void register(CoreRegisterEvent event) {
 *     event.operations().register(MyOperations.COUNT_ITEMS);
 *     event.languages().register(new MyLanguage());
 * }
 * }</pre>
 *
 * <p>Nothing replaces anything quietly, because which of two won would otherwise depend on the order the mods
 * happened to load in. A kind of Operation under an id already taken is refused, and stops the load. A language
 * under an id already taken replaces the one before it, so an addon can improve a language in place, and the log
 * says so; a language claiming a file extension another language already has is refused.
 */
public final class CoreRegisterEvent extends Event implements IModBusEvent {

    private final LanguageRegistry languages;
    private final OperationTypeRegistry operations;

    /** Made and fired by the Core alone; a mod listens for it and never makes one. */
    public CoreRegisterEvent(final LanguageRegistry languages, final OperationTypeRegistry operations) {
        this.languages = languages;
        this.operations = operations;
    }

    /**
     * The languages the computers of a world can be programmed in.
     *
     * <p>A language claims the file extensions it writes and the ones it runs. One already claimed is refused,
     * and so is one the machines keep for themselves, such as that of the listings they run (the assembly a
     * compiler of the series turns a program into).
     */
    public LanguageRegistry languages() {
        return this.languages;
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
