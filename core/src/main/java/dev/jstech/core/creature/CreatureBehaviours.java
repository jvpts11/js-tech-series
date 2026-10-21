/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Every kind of creature behaviour, the Core's own and those mods add: a mod declares its kinds from its constructor,
 * and a creature's data names each behaviour by its kind's id.
 */
public final class CreatureBehaviours {

    private static final Map<ResourceLocation, CreatureBehaviourType<?>> BY_ID = new ConcurrentHashMap<>();

    /** How any behaviour is read and written: its kind's id under {@code type}, then the kind's own fields. */
    public static final Codec<ICreatureBehaviour> CODEC = Codec.lazyInitialized(() -> ResourceLocation.CODEC
            .comapFlatMap(CreatureBehaviours::find, CreatureBehaviourType::id)
            .dispatch("type", ICreatureBehaviour::type, CreatureBehaviourType::codec));

    private CreatureBehaviours() {
    }

    /**
     * Declares a kind of behaviour.
     *
     * @throws IllegalStateException when a kind of that id is declared already
     */
    public static <B extends ICreatureBehaviour> CreatureBehaviourType<B> declare(final CreatureBehaviourType<B> type) {
        if (BY_ID.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("the creature behaviour " + type.id() + " is declared twice");
        }
        return type;
    }

    private static DataResult<CreatureBehaviourType<?>> find(final ResourceLocation id) {
        final CreatureBehaviourType<?> type = BY_ID.get(id);
        return type == null ? DataResult.error(() -> "no creature behaviour is declared as " + id)
                : DataResult.success(type);
    }
}
