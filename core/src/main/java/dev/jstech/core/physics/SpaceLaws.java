/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.physics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Space that does not join up as usual, on the server, for every entity and player: an area that wraps round (going
 * out through one face comes back in through the opposite), one whose way in leads to the same place of another box
 * (a shortcut, or a door bigger inside, its box small and the one it leads to large, in this dimension or another),
 * and one whose way out leads to just outside another box (the way back out). An entity just carried across is let be
 * a moment, so it never bounces back.
 */
final class SpaceLaws {

    private static final Map<Entity, Vec3> LAST = new WeakHashMap<>();
    private static final Map<Entity, Long> CROSSED = new WeakHashMap<>();
    private static final long SETTLE = 10;

    private SpaceLaws() {
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(EntityTickEvent.Post.class, SpaceLaws::afterTick);
    }

    /* Whether the entity crossed an edge of an area of its level with a law of space, and if so carries it across. */
    private static void afterTick(final EntityTickEvent.Post event) {
        final Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        final List<PhysicsArea> areas = Physics.areas(level);
        if (areas.isEmpty()) {
            LAST.remove(entity);
            return;
        }
        final Vec3 now = entity.position();
        final Vec3 was = LAST.put(entity, now);
        final Long crossed = CROSSED.get(entity);
        if (was == null || crossed != null && level.getGameTime() - crossed < SETTLE) {
            return;
        }
        for (final PhysicsArea area : areas) {
            final Laws.Space space = area.laws().space().orElse(null);
            if (space == null) {
                continue;
            }
            final boolean inside = area.contains(now);
            final boolean before = area.contains(was);
            final Laws.Crossing kind = space.kind();
            if (kind == Laws.Crossing.WRAP && before && !inside) {
                final AABB bounds = area.bounds();
                carry(entity, level, new Vec3(PhysicsMath.wrap(now.x, bounds.minX, bounds.maxX),
                        PhysicsMath.wrap(now.y, bounds.minY, bounds.maxY),
                        PhysicsMath.wrap(now.z, bounds.minZ, bounds.maxZ)));
                return;
            }
            if (kind == Laws.Crossing.ENTER && !before && inside || kind == Laws.Crossing.LEAVE && before && !inside) {
                lead(entity, level, area, space, now);
                return;
            }
        }
    }

    /* Carries the entity from {@code at} of the area to the same place of the box it leads to, scaled to its size. */
    private static void lead(final Entity entity, final ServerLevel level, final PhysicsArea area,
                             final Laws.Space space, final Vec3 at) {
        if (space.target().isEmpty()) {
            return;
        }
        final ServerLevel there = space.dimension().map(dimension -> level.getServer().getLevel(dimension))
                .orElse(level);
        if (there == null) {
            return;
        }
        final AABB from = area.bounds();
        final AABB to = new PhysicsArea(area.id(), space.target().get(), Laws.NONE, 0).bounds();
        carry(entity, there, new Vec3(PhysicsMath.map(at.x, from.minX, from.getXsize(), to.minX, to.getXsize()),
                PhysicsMath.map(at.y, from.minY, from.getYsize(), to.minY, to.getYsize()),
                PhysicsMath.map(at.z, from.minZ, from.getZsize(), to.minZ, to.getZsize())));
    }

    /* Puts the entity at {@code to} of {@code level}, moving as it moved, and lets it be a moment. */
    private static void carry(final Entity entity, final ServerLevel level, final Vec3 to) {
        final Vec3 moving = entity.getDeltaMovement();
        if (entity.teleportTo(level, to.x, to.y, to.z, Set.of(), entity.getYRot(), entity.getXRot())) {
            entity.setDeltaMovement(moving);
            LAST.put(entity, to);
            CROSSED.put(entity, level.getGameTime());
        }
    }
}
