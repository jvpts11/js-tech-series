/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.connect.Connection;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.DyeColor;

/**
 * One wire running through the middle of a block: a cable of a line, in one generation of it, and in a colour or in
 * none. Two wires of the same line, generation and colour are the same wire, so a block holds each once.
 *
 * @param kind   the line and generation
 * @param colour its colour, or none
 */
public record Wire(Connection kind, Optional<DyeColor> colour) {

    public static final Codec<Wire> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Connection.CODEC.fieldOf("kind").forGetter(Wire::kind),
            DyeColor.CODEC.optionalFieldOf("colour").forGetter(Wire::colour)
    ).apply(instance, Wire::new));

    public Wire {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(colour, "colour");
    }

    /** A wire of {@code kind} in no colour. */
    public static Wire of(final Connection kind) {
        return new Wire(kind, Optional.empty());
    }
}
