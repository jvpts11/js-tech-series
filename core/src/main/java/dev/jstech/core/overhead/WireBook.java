/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.overhead;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Every overhead wire hung in a dimension, kept with it and sent to the players in it to draw, and the number the next
 * wire hung takes.
 *
 * @param wires the wires
 * @param next  the next wire's number
 */
public record WireBook(List<HungWire> wires, long next) {

    /** A dimension with no wire hung. */
    public static final WireBook EMPTY = new WireBook(List.of(), 1L);

    public static final Codec<WireBook> CODEC = RecordCodecBuilder.create(book -> book.group(
            HungWire.CODEC.listOf().fieldOf("wires").forGetter(WireBook::wires),
            Codec.LONG.fieldOf("next").forGetter(WireBook::next)
    ).apply(book, WireBook::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WireBook> STREAM_CODEC = StreamCodec.composite(
            HungWire.STREAM_CODEC.apply(ByteBufCodecs.list()), WireBook::wires,
            ByteBufCodecs.VAR_LONG, WireBook::next,
            WireBook::new);

    public WireBook {
        wires = List.copyOf(wires);
    }

    /** The book with a wire of {@code cable} hung from {@code from} to {@code to}, fastened at {@code a} and {@code b}. */
    public WireBook with(final WireEnd from, final WireEnd to, final ResourceLocation cable, final Vec3 a,
                         final Vec3 b) {
        final Set<Integer> taken = new HashSet<>();
        for (final HungWire wire : this.wires) {
            if (wire.from().pos().equals(from.pos())) {
                taken.add(wire.slot());
            }
        }
        int slot = 0;
        while (taken.contains(slot)) {
            slot++;
        }
        final List<HungWire> more = new ArrayList<>(this.wires);
        more.add(new HungWire(this.next, from, to, cable, slot, a, b));
        return new WireBook(more, this.next + 1);
    }

    /** The book without the wire numbered {@code id}. */
    public WireBook without(final long id) {
        final List<HungWire> fewer = new ArrayList<>(this.wires);
        fewer.removeIf(wire -> wire.id() == id);
        return new WireBook(fewer, this.next);
    }

    /** The wire numbered {@code id}. */
    public Optional<HungWire> byId(final long id) {
        return this.wires.stream().filter(wire -> wire.id() == id).findFirst();
    }

    /** The wires hung from the block at {@code pos}, at either end. */
    public List<HungWire> at(final BlockPos pos) {
        return this.wires.stream().filter(wire -> wire.touches(pos)).toList();
    }

    /** The wires hung from {@code end}. */
    public List<HungWire> at(final WireEnd end) {
        return this.wires.stream().filter(wire -> wire.hangsFrom(end)).toList();
    }

    /** The wire hung from the block at {@code pos} numbered {@code slot} there. */
    public Optional<HungWire> hungFrom(final BlockPos pos, final int slot) {
        return this.wires.stream().filter(wire -> wire.from().pos().equals(pos) && wire.slot() == slot).findFirst();
    }

    /** Whether a wire hangs between {@code one} and {@code other}, whichever way it was hung. */
    public boolean joins(final WireEnd one, final WireEnd other) {
        return this.wires.stream().anyMatch(wire -> wire.hangsFrom(one) && wire.hangsFrom(other));
    }
}
