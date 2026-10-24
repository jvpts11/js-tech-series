/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transit;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The capsules on their way through the tubes of each dimension, kept with the world, so a capsule sent before the
 * world was closed arrives after it opens. A capsule that arrives unloads into its station; one whose station cannot
 * take it yet, full or in a chunk not loaded, waits a second and tries again; one whose station is gone falls out of
 * the tube's end, its items on the ground there.
 */
public final class CapsuleTransit {

    /** How long a capsule whose station cannot take it waits before it tries again, in ticks. */
    public static final int WAITS = 20;
    /** The capsules on their way in each dimension, the soonest to arrive first. */
    public static final DimensionState<Book> STATE = CoreState.builder(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "capsules"), Book.CODEC, Book.EMPTY).dimension();

    private CapsuleTransit() {
    }

    /** Declares the state, from the Core's constructor. */
    public static void declare() {
        CoreStates.register(STATE);
    }

    /** Has the capsules arrive, every tick of every dimension, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
    }

    /** Sends {@code capsule} on its way. */
    public static void send(final ServerLevel level, final Capsule capsule) {
        STATE.update(level, book -> book.with(List.of(capsule)));
    }

    /** The capsules on their way in {@code level}, the soonest to arrive first. */
    public static List<Capsule> inFlight(final ServerLevel level) {
        return STATE.get(level).capsules();
    }

    /* Has every capsule due by now arrive. */
    private static void tick(final ServerLevel level) {
        final Book book = STATE.get(level);
        final long now = level.getGameTime();
        if (book.capsules().isEmpty() || book.capsules().getFirst().arrives() > now) {
            return;
        }
        final List<Capsule> waiting = new ArrayList<>();
        int due = 0;
        for (final Capsule capsule : book.capsules()) {
            if (capsule.arrives() > now) {
                break;
            }
            due++;
            if (!arrive(level, capsule)) {
                waiting.add(capsule.arrivingAt(now + WAITS));
            }
        }
        STATE.set(level, new Book(book.capsules().subList(due, book.capsules().size())).with(waiting));
    }

    /* Whether {@code capsule} is done: unloaded into its station, or fallen where the station was. */
    private static boolean arrive(final ServerLevel level, final Capsule capsule) {
        if (!Loaded.at(level, capsule.to())) {
            return false;
        }
        if (level.getBlockEntity(capsule.to()) instanceof CapsuleStationBlockEntity station) {
            return station.receive(capsule.items());
        }
        for (final ItemStack stack : capsule.items()) {
            Block.popResource(level, capsule.to(), stack.copy());
        }
        return true;
    }

    /**
     * What is on its way in a dimension.
     *
     * @param capsules the capsules, the soonest to arrive first
     */
    public record Book(List<Capsule> capsules) {

        /** None. */
        public static final Book EMPTY = new Book(List.of());
        /** How the capsules are saved with their world. */
        public static final Codec<Book> CODEC = Capsule.CODEC.listOf().xmap(Book::new, Book::capsules);

        public Book {
            capsules = List.copyOf(capsules);
        }

        /** These capsules and {@code more}, the soonest to arrive first. */
        public Book with(final List<Capsule> more) {
            if (more.isEmpty()) {
                return this;
            }
            final List<Capsule> all = new ArrayList<>(this.capsules);
            all.addAll(more);
            all.sort(Comparator.comparingLong(Capsule::arrives));
            return new Book(all);
        }
    }
}
