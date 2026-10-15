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
import dev.jstech.core.grid.GridPlace;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * An overhead wire hung between two insulators: which, of which line, and where it is fastened at each end, so it is
 * drawn, touched and counted where it hangs. It is known in its grid by the block it is hung from and a number of its
 * own there ({@link #place()}).
 *
 * @param id    its number in its dimension
 * @param from  the end it was hung from
 * @param to    the end it was hung to
 * @param cable the line's cable
 * @param slot  its number among the wires hung from {@code from}'s block
 * @param a     where it is fastened at {@code from}
 * @param b     where it is fastened at {@code to}
 */
public record HungWire(long id, WireEnd from, WireEnd to, ResourceLocation cable, int slot, Vec3 a, Vec3 b) {

    public static final Codec<HungWire> CODEC = RecordCodecBuilder.create(wire -> wire.group(
            Codec.LONG.fieldOf("id").forGetter(HungWire::id),
            WireEnd.CODEC.fieldOf("from").forGetter(HungWire::from),
            WireEnd.CODEC.fieldOf("to").forGetter(HungWire::to),
            ResourceLocation.CODEC.fieldOf("cable").forGetter(HungWire::cable),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("slot").forGetter(HungWire::slot),
            Vec3.CODEC.fieldOf("a").forGetter(HungWire::a),
            Vec3.CODEC.fieldOf("b").forGetter(HungWire::b)
    ).apply(wire, HungWire::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HungWire> STREAM_CODEC = StreamCodec.of(
            (buf, wire) -> {
                buf.writeVarLong(wire.id());
                WireEnd.STREAM_CODEC.encode(buf, wire.from());
                WireEnd.STREAM_CODEC.encode(buf, wire.to());
                buf.writeResourceLocation(wire.cable());
                buf.writeVarInt(wire.slot());
                buf.writeVec3(wire.a());
                buf.writeVec3(wire.b());
            },
            buf -> new HungWire(buf.readVarLong(), WireEnd.STREAM_CODEC.decode(buf), WireEnd.STREAM_CODEC.decode(buf),
                    buf.readResourceLocation(), ByteBufCodecs.VAR_INT.decode(buf), buf.readVec3(), buf.readVec3()));

    public HungWire {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(cable, "cable");
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
    }

    /** Its place in its grid. */
    public GridPlace place() {
        return GridPlace.hung(this.from.pos().asLong(), this.slot);
    }

    /** Its length, from insulator to insulator, in blocks. */
    public double length() {
        return this.a.distanceTo(this.b);
    }

    /** How many blocks of wire it is: its length, a part of a block counted whole. */
    public int blocks() {
        return (int) Math.ceil(length());
    }

    /** Whether it hangs from the block at {@code pos}, at either end. */
    public boolean touches(final BlockPos pos) {
        return this.from.pos().equals(pos) || this.to.pos().equals(pos);
    }

    /** Whether it hangs from {@code end}, at either end. */
    public boolean hangsFrom(final WireEnd end) {
        return this.from.equals(end) || this.to.equals(end);
    }

    /** Where it is fastened at {@code a}, as a point of its curve. */
    public Catenary.Point start() {
        return new Catenary.Point(this.a.x, this.a.y, this.a.z);
    }

    /** Where it is fastened at {@code b}, as a point of its curve. */
    public Catenary.Point end() {
        return new Catenary.Point(this.b.x, this.b.y, this.b.z);
    }
}
