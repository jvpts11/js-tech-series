/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.launcher.LauncherLayout;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The launch pad's rules, as real pads keep them. Under the launcher's exhaust opening, the flame trench: a Flame
 * Deflector below the ground, open up to the opening, spanning the trench from wall to wall, its walls lined with
 * Trench Lining. By the pad, a Pad Service Port holding a whole deluge. At ignition the deluge is poured; a pad with
 * neither trench nor deluge is wrecked, as Starship's first flight tore up its pad, unless the setting spares it. And
 * in a thunderstorm the launch panel holds the countdown, as it has since lightning struck Apollo 12.
 */
public final class LaunchPads {

    /** How far below the ground a trench's deflector may be, and how far from the rocket's axis its port, in blocks. */
    public static final int DEEPEST_TRENCH = 8;
    public static final int PORT_REACH = 16;
    /* How high round the ground a port is looked for; how wide a trench may be; how hard a wrecked pad blows apart. */
    private static final int PORT_HIGH = 3;
    private static final int WIDEST_TRENCH = 16;
    /* How far from a rocket's axis its flight computer may be, and how high over its foot. */
    private static final int AXIS_ROUND = 4;
    private static final int TALLEST_ROCKET = 96;
    private static final float WRECK = 6.0F;

    private LaunchPads() {
    }

    /**
     * What protects the pad under a launcher whose rocket's axis meets its deck's top at {@code axis}, the deck's
     * length along {@code side}: its flame trench, and a deluge held ready.
     */
    public static Protection protection(final ServerLevel level, final BlockPos axis, final Direction side) {
        return new Protection(trench(level, axis, side), port(level, axis).map(PadServicePortBlockEntity::ready)
                .orElse(false));
    }

    /**
     * The pad at ignition: the deluge poured where a port holds one; a pad with neither trench nor deluge wrecked, the
     * setting on. What protected it.
     */
    public static Protection ignite(final ServerLevel level, final BlockPos axis, final Direction side) {
        return ignite(level, axis, side, true);
    }

    /** The pad at ignition as {@link #ignite(ServerLevel, BlockPos, Direction)}, its deluge poured only if it may. */
    public static Protection ignite(final ServerLevel level, final BlockPos axis, final Direction side,
                                    final boolean pour) {
        final boolean trench = trench(level, axis, side);
        final boolean deluge = pour && port(level, axis).map(PadServicePortBlockEntity::pour).orElse(false);
        if (!trench && !deluge && SpaceServerConfig.FILE.get(SpaceServerConfig.LAUNCH_DAMAGE)) {
            final BlockPos under = axis.below(LauncherLayout.RAISE);
            level.explode(null, under.getX() + 0.5, under.getY() + 0.5, under.getZ() + 0.5, WRECK,
                    Level.ExplosionInteraction.BLOCK);
        }
        return new Protection(trench, deluge);
    }

    /**
     * The launcher standing by {@code near}, its measuring hold-down arm within {@code reach} round it and from
     * {@code below} under it to {@code above} over it: where its rocket's axis meets its deck, the way its deck runs,
     * and its rocket's flight computer up that axis, if one keeps a plan; none where no launcher stands there.
     */
    public static Optional<Stand> standBy(final ServerLevel level, final BlockPos near, final int reach,
                                          final int below, final int above) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dy = -below; dy <= above; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    at.set(near.getX() + dx, near.getY() + dy, near.getZ() + dz);
                    if (level.getBlockEntity(at) instanceof LauncherDeckBlockEntity arm && arm.deck().isPresent()) {
                        final BlockPos axis = arm.getBlockPos().offset(arm.deck().get().axis());
                        return Optional.of(new Stand(axis, arm.deck().get().side(), computerOver(level, axis)));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Whether the launch panel holds the countdown now: a thunderstorm over {@code level}, the setting on. */
    public static boolean stormHolds(final Level level) {
        return level.isThundering() && SpaceServerConfig.FILE.get(SpaceServerConfig.NO_LAUNCH_IN_THUNDERSTORMS);
    }

    /* Whether the flame trench lies under the opening: its deflector below the ground, open up to it, lined. */
    private static boolean trench(final ServerLevel level, final BlockPos axis, final Direction side) {
        final int ground = axis.getY() - LauncherLayout.RAISE;
        BlockPos at = axis.below();
        while (level.getBlockState(at).isAir() && at.getY() >= ground - DEEPEST_TRENCH) {
            at = at.below();
        }
        if (at.getY() >= ground || at.getY() < ground - DEEPEST_TRENCH
                || !level.getBlockState(at).is(PadBlocks.FLAME_DEFLECTOR.get())) {
            return false;
        }
        final Direction across = side.getClockWise();
        return lined(level, at, across) && lined(level, at, across.getOpposite());
    }

    /* Whether the deflector at {@code at} reaches {@code way} to a wall of Trench Lining. */
    private static boolean lined(final ServerLevel level, final BlockPos at, final Direction way) {
        BlockPos next = at.relative(way);
        for (int k = 0; k < WIDEST_TRENCH; k++) {
            final BlockState there = level.getBlockState(next);
            if (!there.is(PadBlocks.FLAME_DEFLECTOR.get())) {
                return there.is(PadBlocks.TRENCH_LINING.get());
            }
            next = next.relative(way);
        }
        return false;
    }

    /* The flight computer keeping a rocket's plan round the axis rising from {@code axis}. */
    private static Optional<BlockPos> computerOver(final ServerLevel level, final BlockPos axis) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= TALLEST_ROCKET; dy++) {
            for (int dx = -AXIS_ROUND; dx <= AXIS_ROUND; dx++) {
                for (int dz = -AXIS_ROUND; dz <= AXIS_ROUND; dz++) {
                    at.set(axis.getX() + dx, axis.getY() + dy, axis.getZ() + dz);
                    if (level.getBlockEntity(at) instanceof FlightComputerBlockEntity rocket
                            && rocket.rocket().isPresent()) {
                        return Optional.of(at.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    /* The nearest Pad Service Port round the pad under {@code axis}, on the ground about it. */
    private static Optional<PadServicePortBlockEntity> port(final ServerLevel level, final BlockPos axis) {
        final int ground = axis.getY() - LauncherLayout.RAISE;
        PadServicePortBlockEntity best = null;
        double nearest = Double.MAX_VALUE;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -PORT_REACH; dx <= PORT_REACH; dx++) {
            for (int dz = -PORT_REACH; dz <= PORT_REACH; dz++) {
                for (int y = ground - PORT_HIGH; y <= ground + PORT_HIGH; y++) {
                    at.set(axis.getX() + dx, y, axis.getZ() + dz);
                    if (level.getBlockState(at).is(PadBlocks.PAD_SERVICE_PORT.get())
                            && level.getBlockEntity(at) instanceof PadServicePortBlockEntity port
                            && at.distSqr(axis) < nearest) {
                        nearest = at.distSqr(axis);
                        best = port;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * A launcher standing on a pad.
     *
     * @param axis     where its rocket's axis meets its deck's top
     * @param side     the way its deck runs, toward its tower
     * @param computer its rocket's flight computer, if one keeps the rocket's plan
     */
    public record Stand(BlockPos axis, Direction side, Optional<BlockPos> computer) {
    }

    /**
     * What protects a pad.
     *
     * @param trench whether its flame trench lies under the opening
     * @param deluge whether its deluge is held ready, or was poured
     */
    public record Protection(boolean trench, boolean deluge) {

        /** Whether the pad comes through the launch: a trench or a deluge. */
        public boolean safe() {
            return this.trench || this.deluge;
        }
    }
}
