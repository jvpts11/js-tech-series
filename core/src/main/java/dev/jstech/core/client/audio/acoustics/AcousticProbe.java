/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.audio.acoustics.AcousticSpace;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Listens to the place round the listener as a bat would: rays out in every direction to the first wall each meets,
 * how far it is and what it is made of, a few rays a tick so it never weighs on the game. A whole round gives the
 * place's {@link AcousticSpace}, kept by where it was heard from, so coming back to a spot needs no new round at once.
 */
final class AcousticProbe {

    /** How many rays a round casts, and how many a tick. */
    static final int RAYS = 32;
    static final int RAYS_A_TICK = 8;
    /** How far a ray goes before it counts as reaching the open air, in blocks. */
    static final double REACH = 32;
    /* How far a ray steps between the blocks it looks at, in blocks: under one, so no block is stepped over. */
    private static final double STEP = 0.5;
    /* How many places' rounds are kept, and for how long, in ticks. */
    private static final int KEPT = 32;
    private static final int KEPT_TICKS = 200;
    private static final Vec3[] DIRECTIONS = directions(RAYS);
    private static final Map<BlockPos, Heard> HEARD = new LinkedHashMap<>(KEPT, 0.75F, true);

    private static AcousticSpace space = AcousticSpace.OPEN;
    private static List<Ray> rays = List.of();
    private static BlockPos origin = BlockPos.ZERO;
    private static int next;
    private static int hits;
    private static double distances;
    private static double drunk;
    private static List<Ray> casting = new ArrayList<>();

    private AcousticProbe() {
    }

    /** Casts this tick's rays from {@code eye}; a round ended gives the place's space. */
    static synchronized void tick(final Level level, final Vec3 eye, final long now) {
        final BlockPos at = BlockPos.containing(eye);
        if (!at.equals(origin)) {
            origin = at;
            restart();
            final Heard kept = HEARD.get(at);
            if (kept != null && now - kept.when() < KEPT_TICKS) {
                space = kept.space();
            }
        }
        for (int i = 0; i < RAYS_A_TICK && next < RAYS; i++, next++) {
            cast(level, eye, DIRECTIONS[next]);
        }
        if (next >= RAYS) {
            space = new AcousticSpace(hits == 0 ? 0 : distances / hits, (double) hits / RAYS,
                    hits == 0 ? 0 : drunk / hits, 0, 0);
            rays = List.copyOf(casting);
            HEARD.put(at, new Heard(space, now));
            if (HEARD.size() > KEPT) {
                HEARD.remove(HEARD.keySet().iterator().next());
            }
            restart();
        }
    }

    /** The place round the listener, as the last round heard it. */
    static synchronized AcousticSpace space() {
        return space;
    }

    /** The last round's rays, for the debug overlay. */
    static synchronized List<Ray> rays() {
        return rays;
    }

    /** Forgets every place heard, as the player leaves a world. */
    static synchronized void forget() {
        HEARD.clear();
        space = AcousticSpace.OPEN;
        rays = List.of();
        origin = BlockPos.ZERO;
        restart();
    }

    private static void restart() {
        next = 0;
        hits = 0;
        distances = 0;
        drunk = 0;
        casting = new ArrayList<>(RAYS);
    }

    /* One ray, from the eye to the first solid block, or to its reach. */
    private static void cast(final Level level, final Vec3 eye, final Vec3 direction) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        long last = BlockPos.containing(eye).asLong();
        for (double travelled = STEP; travelled <= REACH; travelled += STEP) {
            final Vec3 point = eye.add(direction.scale(travelled));
            at.set(point.x, point.y, point.z);
            if (at.asLong() == last) {
                continue;
            }
            last = at.asLong();
            if (!level.isLoaded(at)) {
                break;
            }
            final BlockState state = level.getBlockState(at);
            if (state.isSolidRender(level, at)) {
                hits++;
                distances += travelled;
                drunk += BlockAcoustics.absorption(state);
                casting.add(new Ray(eye, point, true));
                return;
            }
        }
        casting.add(new Ray(eye, eye.add(direction.scale(REACH)), false));
    }

    /* Directions spread evenly over a sphere, by the golden angle. */
    private static Vec3[] directions(final int count) {
        final Vec3[] out = new Vec3[count];
        final double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < count; i++) {
            final double y = 1 - 2 * (i + 0.5) / count;
            final double radius = Math.sqrt(1 - y * y);
            final double angle = golden * i;
            out[i] = new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }
        return out;
    }

    /**
     * One ray of a round.
     *
     * @param from where it was cast from
     * @param to   where it ended
     * @param hit  whether it met a wall there, rather than reaching the open air
     */
    record Ray(Vec3 from, Vec3 to, boolean hit) {
    }

    /* A place's round, and when it was heard. */
    private record Heard(AcousticSpace space, long when) {
    }
}
