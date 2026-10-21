/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The moving structures of other mods the kit's meet, each kind told by an adapter behind its own mod's guard: Create's
 * contraptions, the sub-levels of Sable that Create Aeronautics flies. The kit's own structures meet theirs as
 * obstacles that move, never take the blocks they keep, and ask the world where a place of theirs is. With no adapter
 * declared, every query here answers nothing, and the kit is as it is alone.
 */
public final class ForeignMovers {

    /* Below this squared speed, in metres or radians a second, a structure stands still. */
    private static final double STILL = 1.0e-8;
    private static final List<IForeignKind> KINDS = new CopyOnWriteArrayList<>();

    private ForeignMovers() {
    }

    /** Declares {@code kind}, from its adapter, once its mod is found and of a version the adapter covers. */
    public static void declare(final IForeignKind kind) {
        KINDS.add(Objects.requireNonNull(kind, "kind"));
    }

    /** Whether any kind is declared: when none is, the kit has nothing of other mods to meet. */
    public static boolean any() {
        return !KINDS.isEmpty();
    }

    /** The moving structures of other mods in {@code level} whose blocks may be in {@code box}. */
    public static List<ForeignMover> near(final Level level, final AABB box) {
        if (KINDS.isEmpty()) {
            return List.of();
        }
        final List<ForeignMover> out = new ArrayList<>();
        for (final IForeignKind kind : KINDS) {
            kind.near(level, box, out::add);
        }
        return out;
    }

    /**
     * Whether a moving structure of another mod whose blocks may be in {@code box} of {@code level} goes or turns:
     * one standing still can meet nothing that is not already where it meets it.
     */
    public static boolean goingIn(final Level level, final AABB box) {
        if (KINDS.isEmpty()) {
            return false;
        }
        for (final ForeignMover mover : near(level, box)) {
            if (mover.speed().lengthSqr() > STILL || mover.spin().lengthSqr() > STILL) {
                return true;
            }
        }
        return false;
    }

    /** The kind whose region holds {@code pos}, whose blocks the kit takes into none of its structures, or null. */
    public static @Nullable IForeignKind holding(final Level level, final BlockPos pos) {
        for (final IForeignKind kind : KINDS) {
            if (kind.holds(level, pos)) {
                return kind;
            }
        }
        return null;
    }

    /** Whether {@code entity} is a moving structure of another mod, which the kit's structures never push aside. */
    public static boolean isMover(final Entity entity) {
        for (final IForeignKind kind : KINDS) {
            if (kind.isMover(entity)) {
                return true;
            }
        }
        return false;
    }

    /** Where {@code place} is in the world now: a place of another mod's region where its structure has it. */
    public static Vec3 inWorld(final Level level, final Vec3 place) {
        for (final IForeignKind kind : KINDS) {
            final Vec3 there = kind.inWorld(level, place);
            if (there != place) {
                return there;
            }
        }
        return place;
    }
}
