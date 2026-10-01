/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.grid.GridMember;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.DyeColor;

/**
 * One wire running through a cable block: a cable of a kind, in a colour or in none. The colour tells apart runs of
 * one line side by side: two wires of one line and generation join when their colours agree, the same colour or either
 * in none, so an uncoloured wire joins every colour.
 *
 * @param type   the cable
 * @param colour its colour, or none
 */
public record Wire(CableType type, Optional<DyeColor> colour) {

    public static final Codec<Wire> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CoreCables.REGISTRY.byNameCodec().fieldOf("cable").forGetter(Wire::type),
            DyeColor.CODEC.optionalFieldOf("colour").forGetter(Wire::colour)
    ).apply(instance, Wire::new));

    public Wire {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(colour, "colour");
    }

    /** A wire of {@code type} in no colour. */
    public static Wire of(final CableType type) {
        return new Wire(type, Optional.empty());
    }

    /** The same wire in {@code dye}. */
    public Wire dyed(final DyeColor dye) {
        return new Wire(this.type, Optional.of(dye));
    }

    /** What the wire stands for in its grid. */
    public GridMember member() {
        return this.type.member(this.colour);
    }

    /** Whether this and {@code other}, touching, join: the same cable, and colours that agree. */
    public boolean joins(final Wire other) {
        return this.type == other.type && member().joins(other.member());
    }

    /** The slot the wire takes in a block: its lane, or the middle for a cable that never shares a block. */
    public Lane slot() {
        return this.type.lane().orElse(Lane.MIDDLE);
    }
}
