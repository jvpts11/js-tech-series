/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.StructureMotions;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import java.util.Comparator;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * A rocket's cruise between worlds through its system's space, as its flight computer flies it: from where it left
 * its low orbit to the spot of the low orbit of the world it goes to, in the time its planned transfer takes, along a
 * gentle arc, slow to leave and slow to arrive. Its crew sees the world left behind shrink and the one ahead grow. It
 * comes to rest at the spot, where its flight computer captures it into that world's orbit.
 *
 * @param fromX  where it set out
 * @param fromY  where it set out
 * @param fromZ  where it set out
 * @param toX    where it arrives
 * @param toY    where it arrives
 * @param toZ    where it arrives
 * @param start  the game's tick it set out at
 * @param travel how long the cruise takes, in ticks
 * @param to     the world it goes to, by its id among the worlds
 */
public record RocketCruise(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, long start,
                          long travel, ResourceLocation to) implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "cruise");
    public static final MapCodec<RocketCruise> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.fieldOf("from_x").forGetter(RocketCruise::fromX),
            Codec.DOUBLE.fieldOf("from_y").forGetter(RocketCruise::fromY),
            Codec.DOUBLE.fieldOf("from_z").forGetter(RocketCruise::fromZ),
            Codec.DOUBLE.fieldOf("to_x").forGetter(RocketCruise::toX),
            Codec.DOUBLE.fieldOf("to_y").forGetter(RocketCruise::toY),
            Codec.DOUBLE.fieldOf("to_z").forGetter(RocketCruise::toZ),
            Codec.LONG.fieldOf("start").forGetter(RocketCruise::start),
            Codec.LONG.fieldOf("travel").forGetter(RocketCruise::travel),
            ResourceLocation.CODEC.fieldOf("to").forGetter(RocketCruise::to)
    ).apply(instance, RocketCruise::new));
    /* How far the arc bulges out of the straight line, a share of the way. */
    private static final double BULGE = 0.12;
    /* How many ticks ahead the space on its way is kept loaded, every chunk it will pass through till then. */
    private static final int LOOK_AHEAD = 40;
    /*
     * The ticket a cruise holds the space on its way by: going by itself a little after it was last taken, and making
     * its chunk tick as the land round a player does.
     */
    private static final TicketType<ChunkPos> PASSING = TicketType.create("jsspace_cruise",
            Comparator.comparingLong(ChunkPos::toLong), 200);
    private static final int TICKET_REACH = 2;

    public RocketCruise {
        Objects.requireNonNull(to, "to");
        travel = Math.max(1, travel);
    }

    /** Declares the rule, once, from the mod's constructor, so a cruise is saved and read back with its structure. */
    public static void declare() {
        StructureMotions.register(TYPE, CODEC);
    }

    /** The cruise of a rocket standing at {@code at}, setting out at {@code now} for {@code to}'s spot {@code spot}. */
    public static RocketCruise of(final StructurePose at, final double[] spot, final long now, final long travel,
                                  final ResourceLocation to) {
        return new RocketCruise(at.x(), at.y(), at.z(), spot[0], spot[1], spot[2], now, travel, to);
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        if (!(context.world() instanceof ServerLevel level) || context.structure() == null) {
            return new Step(now, this);
        }
        final long tick = level.getGameTime();
        // It keeps the space it stands in loaded and going a while, and the space ahead loading before it gets there,
        // so it never outruns the space it flies into, fast as it goes. Like any moving structure it freezes with its
        // space when no player is in it and no chunk loader holds it, and goes on when someone comes.
        hold(level, ChunkPos.asLong(BlockPos.containing(now.x(), now.y(), now.z())));
        long last = Long.MIN_VALUE;
        for (int ahead = 0; ahead <= LOOK_AHEAD; ahead++) {
            final double[] there = along(progress(tick + ahead));
            final long chunk = ChunkPos.asLong(BlockPos.containing(there[0], there[1], there[2]));
            if (chunk != last) {
                hold(level, chunk);
                last = chunk;
            }
        }
        final double done = progress(tick);
        final double[] at = along(done);
        if (!level.isPositionEntityTicking(BlockPos.containing(at[0], at[1], at[2]))) {
            // The space ahead is not going yet: flown into, it would stop there. It waits, its cruise a tick longer.
            return new Step(now, new RocketCruise(this.fromX, this.fromY, this.fromZ, this.toX, this.toY, this.toZ,
                    this.start + 1, this.travel, this.to));
        }
        final StructurePose next = now.movedTo(at[0], at[1], at[2]);
        if (done >= 1) {
            // Arrived at the spot of the other world's orbit, resting there: its flight computer captures it.
            return new Step(next, null);
        }
        return new Step(next, this);
    }

    /** How far along its way it is at {@code tick}, from 0 as it sets out to 1 as it arrives. */
    public double progress(final long tick) {
        return Math.clamp((double) (tick - this.start + 1) / this.travel, 0, 1);
    }

    /** The same cruise arriving at {@code tick}, as when everyone aboard sleeps through it. */
    public RocketCruise skipped(final long tick) {
        return new RocketCruise(this.fromX, this.fromY, this.fromZ, this.toX, this.toY, this.toZ, tick - this.travel,
                this.travel, this.to);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public boolean readsWorld() {
        return true;
    }

    /* Keeps the chunk {@code at} of the space loaded and going a while. */
    private static void hold(final ServerLevel level, final long at) {
        final ChunkPos chunk = new ChunkPos(at);
        level.getChunkSource().addRegionTicket(PASSING, chunk, TICKET_REACH, chunk, true);
    }

    /*
     * Where it is {@code done} of the way, eased so it leaves and arrives slowly: on an arc bulging square to the
     * straight way, level, its height going straight from one end to the other.
     */
    private double[] along(final double done) {
        final double s = done * done * (3 - 2 * done);
        final double dx = this.toX - this.fromX;
        final double dz = this.toZ - this.fromZ;
        final double midX = (this.fromX + this.toX) / 2 - dz * BULGE;
        final double midZ = (this.fromZ + this.toZ) / 2 + dx * BULGE;
        final double a = (1 - s) * (1 - s);
        final double b = 2 * (1 - s) * s;
        final double c = s * s;
        return new double[] {a * this.fromX + b * midX + c * this.toX, this.fromY + (this.toY - this.fromY) * s,
                a * this.fromZ + b * midZ + c * this.toZ};
    }
}
