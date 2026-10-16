/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.util.DeclarationError;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * The knowledge of every mod ({@link Knowledge}), declared while the game loads and closed once it has loaded. A
 * milestone is a piece of knowledge too, declared with its set ({@link Milestones}).
 */
public final class Knowledges {

    private static final Map<ResourceLocation, Knowledge> DECLARED = new LinkedHashMap<>();
    private static boolean frozen;

    private Knowledges() {
    }

    /**
     * Declares {@code knowledge}, from a mod's constructor or the class that holds it, before the game has loaded.
     *
     * @return the knowledge, to keep in a constant
     */
    public static synchronized Knowledge declare(final Knowledge knowledge) {
        Objects.requireNonNull(knowledge, "knowledge");
        if (frozen) {
            throw DeclarationError.of(knowledge.id().getNamespace(), "the knowledge " + knowledge.id(),
                    "is declared after the game has loaded",
                    "declare it from the mod's constructor, or from a class the constructor loads");
        }
        if (DECLARED.putIfAbsent(knowledge.id(), knowledge) != null) {
            throw DeclarationError.of(knowledge.id().getNamespace(), "the knowledge " + knowledge.id(),
                    "is declared twice", "give one of the two another id");
        }
        return knowledge;
    }

    /** The knowledge of {@code id}, if one is declared. */
    public static synchronized Optional<Knowledge> byId(final ResourceLocation id) {
        return Optional.ofNullable(DECLARED.get(id));
    }

    /** Every piece of knowledge, in the order the mods declared them. */
    public static synchronized List<Knowledge> all() {
        return List.copyOf(DECLARED.values());
    }

    /** Closes the list once the game has loaded; from the Core. */
    @ApiStatus.Internal
    public static synchronized void freeze() {
        frozen = true;
    }
}
