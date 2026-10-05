/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.ComponentKind;
import dev.jstech.computers.vm.program.ComponentRules;
import dev.jstech.computers.vm.program.IComponentRule;
import dev.jstech.core.JsCore;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The kinds of generic component the mods have added, by name.
 *
 * <p>A kind is added while the game loads, in the namespace of the mod adding it, and the list closes once loading
 * is done, so the kinds a world's programs name do not change under somebody playing. Two mods naming the same kind
 * is a mistake in one of them and stops the load; adding one after it is refused with a word in the log. A program
 * may still name a kind nobody added, which is what a world carried to a game without the mod does: the program
 * keeps its value, and every screen shows a placeholder in its place.
 */
public final class ComponentKinds {

    /** The namespaces no mod names a kind in: the game's and the series' own. */
    private static final Set<String> RESERVED = Set.of("minecraft", JsComputers.MODID, JsCore.MODID);

    private static final Map<String, ComponentKind> KINDS = new ConcurrentHashMap<>();
    private static volatile boolean frozen;

    private ComponentKinds() {
    }

    /** Tells the programs' runtime how to find these kinds, as the game loads. */
    public static void install() {
        ComponentRules.install(id -> {
            final ComponentKind kind = find(id);
            return kind == null ? null : new Rule(kind);
        });
    }

    /** Adds a kind; refused once loading is done, and a name already taken or outside its mod's namespace stops it. */
    public static void register(final ComponentKind kind) {
        if (kind == null) {
            return;
        }
        if (frozen) {
            JsComputers.LOGGER.warn("The component kind {} was not registered: kinds are only added while the game "
                    + "loads", kind.id());
            return;
        }
        if (RESERVED.contains(kind.id().getNamespace())) {
            throw new IllegalStateException("The component kind " + kind.id() + " cannot be added: a kind is named "
                    + "in the namespace of the mod that adds it, and that one is the game's or the series' own");
        }
        if (KINDS.putIfAbsent(kind.id().toString(), kind) != null) {
            throw new IllegalStateException("A component kind is already registered as " + kind.id());
        }
    }

    /** Closes the list, as the mod does once every mod has loaded. */
    public static void freeze() {
        frozen = true;
    }

    /** The kind of that name, or null when no mod in this game added it. */
    public static ComponentKind find(final String id) {
        return id == null ? null : KINDS.get(id);
    }

    /** A kind as the runtime asks about it. */
    private record Rule(ComponentKind kind) implements IComponentRule {

        @Override
        public String id() {
            return this.kind.id().toString();
        }

        @Override
        public boolean reachesOutside() {
            return this.kind.reachesOutside();
        }

        @Override
        public boolean takesData(final Object value) {
            return this.kind.validator().data(value);
        }

        @Override
        public boolean takesAction(final String name, final Object value) {
            return this.kind.validator().action(name, value);
        }
    }
}
