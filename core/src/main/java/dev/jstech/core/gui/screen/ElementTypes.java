/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Every kind of component a screen's file may hold. The Core declares its own ({@link CoreElements}); a mod declares
 * more the same way, from its constructor, and registers on the client what draws them. A file names a kind by its id,
 * and a kind of the Core's by its name alone: {@code "type": "button"} is {@code "jscore:button"}.
 */
public final class ElementTypes {

    private static final Map<ResourceLocation, ElementType<?>> BY_ID = new ConcurrentHashMap<>();

    /** A kind as a file names it. */
    static final Codec<ElementType<?>> TYPE_CODEC = Codec.STRING.comapFlatMap(ElementTypes::read,
            type -> type.id().getNamespace().equals(JsCore.MODID) ? type.id().getPath() : type.id().toString());

    private ElementTypes() {
    }

    /**
     * Declares the kind {@code id}, whose own fields {@code props} reads, holding other components when
     * {@code holds}.
     *
     * @throws DeclarationError when a kind of that id was declared already
     */
    public static <P> ElementType<P> declare(final ResourceLocation id, final MapCodec<P> props, final boolean holds) {
        final ElementType<P> type = new ElementType<>(id, props, holds);
        if (BY_ID.putIfAbsent(id, type) != null) {
            throw DeclarationError.of(id.getNamespace(), "the screen component " + id, "is declared twice",
                    "declare it once, as a static final, and use that field");
        }
        return type;
    }

    /** The kind {@code id}, or none. */
    public static Optional<ElementType<?>> get(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Every kind declared. */
    public static List<ElementType<?>> all() {
        return new ArrayList<>(BY_ID.values());
    }

    private static DataResult<ElementType<?>> read(final String name) {
        final ResourceLocation id = name.indexOf(':') < 0 ? ResourceLocation.tryBuild(JsCore.MODID, name)
                : ResourceLocation.tryParse(name);
        final ElementType<?> type = id == null ? null : BY_ID.get(id);
        return type != null ? DataResult.success(type)
                : DataResult.error(() -> "\"" + name + "\" is no kind of screen component a mod installed declares");
    }
}
