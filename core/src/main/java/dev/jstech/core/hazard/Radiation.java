/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.registry.CoreAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * The radiation family at work: every kind any mod declares, the blocks that give it off, and how much of it reaches a
 * place. From each source within its kind's reach it comes weaker with the square of the distance, and each block on
 * the way lets through only its share, by its density: lead stops nearly all of it, concrete and stone much, water
 * some, air hardly any, so a wall is real protection. What is everywhere (the sky's, what lies on a chunk) is added,
 * and so is what a body carries. What reaches a body builds up its kind's hazard, cut by what it wears.
 *
 * <pre>{@code
 * Radiation.emit(level, pos, MyRadiation.GLOW, 40);   // as the block loads or fills; 0 as it empties or goes
 * double here = Radiation.rateAt(level, player.getEyePosition(), MyRadiation.GLOW);
 * }</pre>
 */
public final class Radiation {

    private static final Map<ResourceLocation, RadiationKind> DECLARED = new LinkedHashMap<>();
    /* A source's own block, and the block the radiation reaches, are not counted as shielding between them. */
    private static final double CLOSEST = 1;
    /* Past so much weakening nothing worth counting gets through, and the rest of the way is not walked. */
    private static final double OPAQUE = 20;

    private Radiation() {
    }

    /** Declares {@code kind}, from a mod's constructor, and has what reaches a body build up its hazard. */
    public static RadiationKind declare(final RadiationKind kind) {
        synchronized (DECLARED) {
            if (DECLARED.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException("the radiation " + kind.id() + " is declared twice");
            }
        }
        kind.dose().addSource((body, level) -> kind.dose().perSecond(rateOn(body, level, kind)));
        return kind;
    }

    /** The kind {@code id}, if one is declared. */
    public static Optional<RadiationKind> find(final ResourceLocation id) {
        synchronized (DECLARED) {
            return Optional.ofNullable(DECLARED.get(id));
        }
    }

    /** Every kind declared, in the order they were. */
    public static Collection<RadiationKind> kinds() {
        synchronized (DECLARED) {
            return List.copyOf(DECLARED.values());
        }
    }

    /** Hands the game's bus what forgets the sources of a chunk as it unloads, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Unload.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.hasData(CoreAttachments.RADIATION)) {
                level.getData(CoreAttachments.RADIATION).forget(event.getChunk().getPos());
            }
        });
    }

    /**
     * Has the block at {@code pos} give off {@code strength} of {@code kind}, as much as reaches a block from it, in
     * the unit its hazard's rate is shown in; nothing or less stops it. A block says it as it loads and as its
     * contents change, and stops as it empties or goes.
     */
    public static void emit(final ServerLevel level, final BlockPos pos, final RadiationKind kind,
                            final double strength) {
        if (strength <= 0 && !level.hasData(CoreAttachments.RADIATION)) {
            return;
        }
        level.getData(CoreAttachments.RADIATION).set(pos, kind, strength);
    }

    /** How much of {@code kind} the block at {@code pos} gives off. */
    public static double strength(final ServerLevel level, final BlockPos pos, final RadiationKind kind) {
        return level.hasData(CoreAttachments.RADIATION) ? level.getData(CoreAttachments.RADIATION).strength(pos, kind)
                : 0;
    }

    /**
     * How much of {@code kind} reaches {@code at} of {@code level}, in the unit its hazard's rate is shown in: what is
     * everywhere there, and what each source in reach gives off, weaker with the square of the distance and shielded
     * by the blocks between.
     */
    public static double rateAt(final ServerLevel level, final Vec3 at, final RadiationKind kind) {
        double rate = 0;
        for (final IAmbientRadiation ambient : kind.ambients()) {
            rate += Math.max(0, ambient.at(level, at));
        }
        if (!level.hasData(CoreAttachments.RADIATION)) {
            return rate;
        }
        final int reach = kind.reach();
        final int chunkX = SectionPos.blockToSectionCoord(at.x);
        final int chunkZ = SectionPos.blockToSectionCoord(at.z);
        final int chunks = SectionPos.blockToSectionCoord(reach) + 1;
        for (final LevelRadiation.Source source : level.getData(CoreAttachments.RADIATION).in(chunkX - chunks,
                chunkZ - chunks, chunkX + chunks, chunkZ + chunks, kind)) {
            final Vec3 from = Vec3.atCenterOf(source.pos());
            final double distance = from.distanceTo(at);
            if (distance > reach) {
                continue;
            }
            rate += source.strength() / Math.max(CLOSEST, distance * distance)
                    * transmission(level, from, at, kind);
        }
        return rate;
    }

    /** How much of {@code kind} reaches {@code body}: where its eyes are, and from what it carries. */
    public static double rateOn(final LivingEntity body, final ServerLevel level, final RadiationKind kind) {
        double rate = rateAt(level, body.getEyePosition(), kind);
        for (final IBodyRadiation carried : kind.carried()) {
            rate += Math.max(0, carried.of(body));
        }
        return rate;
    }

    /**
     * The share of {@code kind} given off at {@code from} that gets through the blocks on the straight way to
     * {@code to}: each block crossed lets through its share by its density, the two ends' own blocks left out. A
     * chunk the game does not have whole counts as air.
     */
    public static double transmission(final ServerLevel level, final Vec3 from, final Vec3 to,
                                      final RadiationKind kind) {
        final double dx = to.x - from.x;
        final double dy = to.y - from.y;
        final double dz = to.z - from.z;
        int x = (int) Math.floor(from.x);
        int y = (int) Math.floor(from.y);
        int z = (int) Math.floor(from.z);
        final int endX = (int) Math.floor(to.x);
        final int endY = (int) Math.floor(to.y);
        final int endZ = (int) Math.floor(to.z);
        final int stepX = (int) Math.signum(dx);
        final int stepY = (int) Math.signum(dy);
        final int stepZ = (int) Math.signum(dz);
        // How far along the way, from 0 to 1, the next face of each axis is crossed, and how far apart its faces are.
        double nextX = dx == 0 ? Double.MAX_VALUE : (stepX > 0 ? x + 1 - from.x : from.x - x) / Math.abs(dx);
        double nextY = dy == 0 ? Double.MAX_VALUE : (stepY > 0 ? y + 1 - from.y : from.y - y) / Math.abs(dy);
        double nextZ = dz == 0 ? Double.MAX_VALUE : (stepZ > 0 ? z + 1 - from.z : from.z - z) / Math.abs(dz);
        final double eachX = dx == 0 ? Double.MAX_VALUE : 1 / Math.abs(dx);
        final double eachY = dy == 0 ? Double.MAX_VALUE : 1 / Math.abs(dy);
        final double eachZ = dz == 0 ? Double.MAX_VALUE : 1 / Math.abs(dz);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        LevelChunk chunk = null;
        double weakening = 0;
        while (true) {
            if (nextX <= nextY && nextX <= nextZ) {
                if (nextX > 1) {
                    break;
                }
                x += stepX;
                nextX += eachX;
            } else if (nextY <= nextZ) {
                if (nextY > 1) {
                    break;
                }
                y += stepY;
                nextY += eachY;
            } else {
                if (nextZ > 1) {
                    break;
                }
                z += stepZ;
                nextZ += eachZ;
            }
            if (x == endX && y == endY && z == endZ) {
                break;
            }
            if (chunk == null || chunk.getPos().x != (x >> 4) || chunk.getPos().z != (z >> 4)) {
                chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            }
            if (chunk == null) {
                continue;
            }
            pos.set(x, y, z);
            weakening += kind.attenuation() * BlockDensity.of(level, pos, chunk.getBlockState(pos));
            if (weakening > OPAQUE) {
                return 0;
            }
        }
        return Math.exp(-weakening);
    }
}
