/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.util.DeclarationError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of one thing hardware is made of, by id, open to every mod while the game loads and closed once it has
 * loaded: the memory generations, the slot generations, the form factors, the kinds of expansion card. This mod's own
 * are declared as the class that names them loads; an add-on declares its own through the register event, by the same
 * call. Plain text, so hardware stays testable without Minecraft loaded.
 *
 * @param <T> the kind
 */
public final class DeclaredKinds<T> {

    private final String what;
    private final Function<T, String> idOf;
    private final Map<String, T> byId = new LinkedHashMap<>();
    private volatile boolean closed;

    /**
     * Kinds of {@code what}, each known by the id {@code idOf} reads from it.
     *
     * @param what what one kind is, for the messages: "memory generation"
     * @param idOf the kind's id, {@code namespace:path}
     */
    public DeclaredKinds(final String what, final Function<T, String> idOf) {
        this.what = Objects.requireNonNull(what, "what");
        this.idOf = Objects.requireNonNull(idOf, "idOf");
    }

    /**
     * Declares {@code kind}, while the game loads.
     *
     * @throws DeclarationError when one of the same id is declared already, or the game has loaded
     */
    public synchronized T declare(final T kind) {
        final String id = idOf.apply(Objects.requireNonNull(kind, what));
        HardwareIds.requireNamespaced(id, "a " + what);
        final String mod = id.substring(0, id.indexOf(':'));
        if (closed) {
            throw DeclarationError.of(mod, "the " + what + " " + id, "is declared after the game has loaded",
                    "declare it from the register event, while the game loads");
        }
        if (byId.putIfAbsent(id, kind) != null) {
            throw DeclarationError.of(mod, "the " + what + " " + id, "is declared twice",
                    "give one of the two another id");
        }
        return kind;
    }

    /** The kind of {@code id}, or null when none is declared; a bare name is one of this mod's own. */
    public synchronized @Nullable T byId(final String id) {
        return byId.get(id.indexOf(':') < 0 ? HardwareIds.own(id) : id);
    }

    /** Every kind, in the order declared. */
    public synchronized List<T> all() {
        return List.copyOf(byId.values());
    }

    /** Closes the list once the game has loaded. */
    public void close() {
        closed = true;
    }
}
