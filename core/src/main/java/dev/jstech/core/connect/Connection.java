/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

/**
 * What reaches a face of a block: a line, the job a cable does (a network's access or backbone, a machine's power, a
 * crafting run), in one generation of it. Lines are open ids, so a mod brings lines of its own without the Core
 * knowing them.
 *
 * <p>A generation is how new the line is, counted from 0: each era of a line is a generation of it. A port takes its
 * own generation of a line and every earlier one, never a later one, which is how a machine takes its era's cables
 * and the earlier eras', and never a newer era's.
 *
 * @param line       the line
 * @param generation how new it is, from 0
 */
public record Connection(ResourceLocation line, int generation) {

    /** How a connection is saved: its line and, past the first, its generation. */
    public static final Codec<Connection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("line").forGetter(Connection::line),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("generation", 0).forGetter(Connection::generation)
    ).apply(instance, Connection::new));

    public Connection {
        Objects.requireNonNull(line, "line");
        if (generation < 0) {
            throw new IllegalArgumentException("a line's generation counts from 0, not " + generation);
        }
    }

    /** The first generation of {@code line}. */
    public static Connection of(final ResourceLocation line) {
        return new Connection(line, 0);
    }

    /** Whether a port of this line and generation takes {@code offered}: the same line, no newer than this. */
    public boolean takes(final Connection offered) {
        return this.line.equals(offered.line) && offered.generation <= this.generation;
    }
}
