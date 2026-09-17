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
 * <p>Two of the same thing is always refused rather than one quietly replacing the other, because which
 * of the two won would otherwise depend on the order the mods happened to load in.
 */
public final class CoreRegisterEvent extends Event implements IModBusEvent {

    private final LanguageRegistry languages;
    private final OperationTypeRegistry operations;

    public CoreRegisterEvent(final LanguageRegistry languages, final OperationTypeRegistry operations) {
        this.languages = languages;
        this.operations = operations;
    }

    /**
     * The languages the computers of a world can be programmed in.
     *
     * <p>A language claims the extensions it writes and the ones it runs. One already claimed is refused,
     * and so is one the machines keep for themselves, such as the listings they run.
     */
    public LanguageRegistry languages() {
        return this.languages;
    }

    /** The kinds of Operation the network can be asked to carry out. */
    public OperationTypeRegistry operations() {
        return this.operations;
    }
}
