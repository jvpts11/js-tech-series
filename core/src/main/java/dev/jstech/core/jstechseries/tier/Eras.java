/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.jstech.core.JsCore;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The eras of hardware, in their order, as a registry rather than a closed list: the series' seven are declared by the
 * Core, and an add-on declares its own after the last or right after any other, while the game loads. Every rule that
 * asks for "this era or later" follows the order ({@link #atLeast}). An era is saved by its id, so an era an add-on
 * puts between two moves no other.
 *
 * <pre>{@code
 * public static final Era QUANTUM = Eras.declareAfter(new Era(
 *         ResourceLocation.fromNamespaceAndPath("myaddon", "quantum"), MyTexts.QUANTUM.text(), 0xFF00FF),
 *         Eras.idOf(HardwareEra.ADVANCED));      // between Advanced and Exa
 * }</pre>
 */
public final class Eras {

    /* Each era, by id, with the era it was declared after, or none for one at the end; in the order declared. */
    private static final Map<ResourceLocation, Era> DECLARED = new LinkedHashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> AFTER = new HashMap<>();
    /** How an era is saved: by its id, which must name a declared era. */
    public static final Codec<Era> CODEC = ResourceLocation.CODEC.comapFlatMap(
            id -> byId(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "no era " + id)), Era::id);
    private static @Nullable List<Era> ordered;

    static {
        // The series' seven, in their order.
        for (final HardwareEra era : HardwareEra.values()) {
            declare(new Era(idOf(era), era.text(), era.screenColor()));
        }
    }

    private Eras() {
    }

    /** The id the series' {@code era} has in the registry: {@code jscore:legacy}. */
    public static ResourceLocation idOf(final HardwareEra era) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, era.serializedName());
    }

    /** Declares {@code era} after every era declared so far, while the game loads. */
    public static synchronized Era declare(final Era era) {
        return put(era, null);
    }

    /** Declares {@code era} right after {@code after}, before any era that came after it, while the game loads. */
    public static synchronized Era declareAfter(final Era era, final ResourceLocation after) {
        Objects.requireNonNull(after, "after");
        if (!DECLARED.containsKey(after)) {
            throw DeclarationError.of(era.id().getNamespace(), "the era " + era.id(),
                    "is declared after " + after + ", which is no era yet",
                    "declare it after an era declared before it, such as one of the series' seven");
        }
        return put(era, after);
    }

    /** The era of {@code id}, if one is declared. */
    public static synchronized Optional<Era> byId(final ResourceLocation id) {
        return Optional.ofNullable(DECLARED.get(id));
    }

    /** The registry's era for the series' {@code era}. */
    public static Era of(final HardwareEra era) {
        return byId(idOf(era)).orElseThrow();
    }

    /** Every era, earliest first. */
    public static synchronized List<Era> ordered() {
        if (ordered == null) {
            return order();
        }
        return ordered;
    }

    /** Where {@code era} stands in the order, from 0 for the earliest; -1 for one not declared. */
    public static int position(final Era era) {
        return ordered().indexOf(era);
    }

    /** Whether {@code era} is {@code least} or later. */
    public static boolean atLeast(final Era era, final Era least) {
        return position(era) >= position(least);
    }

    /** Closes the registry once the game has loaded, settling the order; from the Core. */
    @ApiStatus.Internal
    public static synchronized void freeze() {
        ordered = order();
    }

    private static Era put(final Era era, final @Nullable ResourceLocation after) {
        Objects.requireNonNull(era, "era");
        if (ordered != null) {
            throw DeclarationError.of(era.id().getNamespace(), "the era " + era.id(),
                    "is declared after the game has loaded",
                    "declare it from the mod's constructor, or from a class the constructor loads");
        }
        if (DECLARED.putIfAbsent(era.id(), era) != null) {
            throw DeclarationError.of(era.id().getNamespace(), "the era " + era.id(), "is declared twice",
                    "give one of the two another id");
        }
        if (after != null) {
            AFTER.put(era.id(), after);
        }
        return era;
    }

    /*
     * The order: the eras declared at the end, in turn, each followed by the eras declared right after it, in the
     * order they were declared, each in turn followed by its own.
     */
    private static List<Era> order() {
        final List<Era> out = new ArrayList<>();
        for (final Era era : DECLARED.values()) {
            if (!AFTER.containsKey(era.id())) {
                place(era, out);
            }
        }
        return List.copyOf(out);
    }

    private static void place(final Era era, final List<Era> out) {
        out.add(era);
        for (final Era next : DECLARED.values()) {
            if (era.id().equals(AFTER.get(next.id()))) {
                place(next, out);
            }
        }
    }
}
