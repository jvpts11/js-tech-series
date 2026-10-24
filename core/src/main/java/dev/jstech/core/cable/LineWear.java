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
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * How long each pipe of a dimension has carried what wears it ({@code CableType.Builder.wornBy}), kept with the world
 * so a pipe that carried hydrogen for half an hour before the server stopped goes on from there. The lines count it
 * once every {@link #EVERY} ticks, a pipe that carried it in that tick counting the whole span; a pipe taken out, or
 * whose block goes, is forgotten, so one laid in its place starts new.
 */
public final class LineWear {

    /** How often the lines count the wear of their pipes, in ticks. */
    public static final int EVERY = 20;
    /** What each dimension's pipes have carried of what wears them, by pipe. */
    public static final DimensionState<Book> STATE = CoreState.builder(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "line_wear"), Book.CODEC, Book.EMPTY).dimension();

    private LineWear() {
    }

    /** Declares the state, from the Core's constructor. */
    public static void declare() {
        CoreStates.register(STATE);
    }

    /**
     * Adds a span of {@link #EVERY} ticks to each of {@code worn}, the pipes that carried what wears them in this tick,
     * each with the ticks it stands: those now worn through, which their lines then tell.
     */
    public static List<Pipe> wear(final ServerLevel level, final Map<Pipe, Integer> worn) {
        if (worn.isEmpty()) {
            return List.of();
        }
        final List<Pipe> through = new ArrayList<>();
        STATE.update(level, book -> {
            final Map<String, Integer> ticks = new HashMap<>(book.ticks());
            worn.forEach((pipe, stands) -> {
                final int now = ticks.merge(pipe.key(), EVERY, Integer::sum);
                if (now >= stands) {
                    through.add(pipe);
                }
            });
            return new Book(ticks);
        });
        return through;
    }

    /** How many ticks the pipe of {@code type} at {@code at} has carried what wears it. */
    public static int worn(final ServerLevel level, final BlockPos at, final CableType type) {
        return STATE.get(level).ticks().getOrDefault(new Pipe(at, type).key(), 0);
    }

    /** Forgets the pipe of {@code type} at {@code at}: it was taken out, or its block went. */
    public static void forget(final ServerLevel level, final BlockPos at, final CableType type) {
        final String key = new Pipe(at, type).key();
        if (STATE.get(level).ticks().containsKey(key)) {
            STATE.update(level, book -> {
                final Map<String, Integer> ticks = new HashMap<>(book.ticks());
                ticks.remove(key);
                return new Book(ticks);
            });
        }
    }

    /**
     * A pipe: where it is and its cable.
     *
     * @param at   the cable block it is in
     * @param type its cable
     */
    public record Pipe(BlockPos at, CableType type) {

        public Pipe {
            at = at.immutable();
        }

        /* How the book knows it: its place and its cable's id. */
        private String key() {
            return this.at.asLong() + "|" + this.type.id();
        }
    }

    /**
     * What a dimension's pipes have carried of what wears them.
     *
     * @param ticks the ticks each pipe has carried it, by the pipe's place and cable
     */
    public record Book(Map<String, Integer> ticks) {

        public static final Book EMPTY = new Book(Map.of());
        public static final Codec<Book> CODEC = RecordCodecBuilder.create(book -> book.group(
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("ticks").forGetter(Book::ticks)
        ).apply(book, Book::new));

        public Book {
            ticks = Map.copyOf(ticks);
        }
    }
}
