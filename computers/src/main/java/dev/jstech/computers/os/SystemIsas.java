/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;

/**
 * The instruction sets a system is built for, and the translators it carries for the programs of others
 * (computers/docs/design/quince.md). A system is a program of the processors it was built for: a machine starts it
 * when its processor runs one of them, and a system built for no instruction set in particular is built for its first
 * era's processors, which is what every system was before Quince brought its own.
 *
 * @param builtFor    the ids of the instruction sets it is built for, empty for its first era's processors'
 * @param translators the ids of the translators it carries, which run on its machines the programs built for
 *                    another instruction set, as {@code IsaTranslators} declares them
 */
@ApiStatus.Experimental
public record SystemIsas(List<String> builtFor, List<String> translators) {

    /** A system that names no instruction set and carries no translator. */
    public static final SystemIsas UNNAMED = new SystemIsas(List.of(), List.of());

    /** Its fields as a system's own, side by side with the rest of what the system says about itself. */
    public static final MapCodec<SystemIsas> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Codec.STRING.listOf().optionalFieldOf("built_for", List.of()).forGetter(SystemIsas::builtFor),
            Codec.STRING.listOf().optionalFieldOf("translators", List.of()).forGetter(SystemIsas::translators)
    ).apply(inst, SystemIsas::new));

    public SystemIsas {
        builtFor = List.copyOf(Objects.requireNonNull(builtFor,
                "a system's instruction sets may be empty, not missing"));
        translators = List.copyOf(Objects.requireNonNull(translators,
                "a system's translators may be empty, not missing"));
    }
}
