/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.joint.JointKind;
import dev.jstech.core.moving.joint.StructureJoint;
import dev.jstech.core.moving.joint.StructureJoints;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Sitting aboard: a player using a seat of a moving structure ({@link BlockAboard.Seat}), not crouching, sits on it,
 * as long as no one else does ({@link StructureSeatEntity}); one sitting at a helm steers the structure, the controls
 * they hold told to it each tick they change ({@link StructureHelmPayload}).
 */
final class StructureSeating {

    /* How far from their eyes a player takes hold of a rope, in blocks. */
    private static final double ZIPLINE_REACH = 6.0;

    private StructureSeating() {
    }

    /** Hands the game's bus what sitting answers, once, as the kit starts. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, StructureSeating::onUse);
    }

    /** A player at the helm of {@code payload}'s seat told the controls they hold: the structure is told them. */
    static void helm(final ServerPlayer player, final StructureHelmPayload payload) {
        if (player.getVehicle() instanceof StructureSeatEntity seat && seat.getId() == payload.seat()) {
            helm(player, payload.keys());
        }
    }

    /**
     * {@code player}, sitting at a helm, holds the controls {@code keys}, each its {@link StructureHelmPayload#bit}:
     * the structure they sit on is told them; whether they sit at a helm.
     */
    static boolean helm(final ServerPlayer player, final int keys) {
        if (!(player.getVehicle() instanceof StructureSeatEntity seat) || !seat.helm()
                || !(seat.structure() instanceof MovingStructure structure)
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        final Map<StructureControls.ControlAxis, Float> held = new EnumMap<>(StructureControls.ControlAxis.class);
        for (final StructureControls.ControlAxis axis : StructureControls.ControlAxis.values()) {
            held.put(axis, (keys & StructureHelmPayload.bit(axis)) != 0 ? 1F : 0F);
        }
        final StructureFrame frame = structure.frame();
        final Direction way = BlockAboard.wayAt(level, seat.seat());
        final Vec3 forward = new Vec3(frame.turnX(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnY(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnZ(way.getStepX(), way.getStepY(), way.getStepZ()));
        final Vec3 up = new Vec3(frame.turnX(0, 1, 0), frame.turnY(0, 1, 0), frame.turnZ(0, 1, 0));
        StructureMounts.rootOf(structure).controls().pilot(level.getGameTime(), held, forward, up);
        return true;
    }

    /** A player takes hold of a rope at {@code payload}'s place on it, to ride it down. */
    static void zipline(final ServerPlayer player, final StructureZiplinePayload payload) {
        zipline(player, payload.joint(), payload.along());
    }

    /**
     * {@code player} takes hold of the rope {@code joint} at {@code along} of its way from its first end: within
     * their reach, not riding anything, they hang from it there, to ride it down ({@link ZiplineEntity}); whether
     * they hang from it.
     */
    static boolean zipline(final ServerPlayer player, final UUID joint, final double along) {
        if (player.isPassenger() || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        final StructureJoint rope = StructureJoints.byId(level, joint);
        if (rope == null || !(rope.kind() instanceof JointKind.Rope)) {
            return false;
        }
        final ZiplineEntity hold = ZiplineEntity.made(level, rope, Math.max(0, Math.min(1, along)));
        final Vec3 at = hold.position().add(0, ZiplineEntity.HANGS, 0);
        if (at.distanceTo(player.getEyePosition()) > ZIPLINE_REACH) {
            hold.discard();
            return false;
        }
        level.addFreshEntity(hold);
        return player.startRiding(hold, true);
    }

    /**
     * {@code player} sits on the seat at the yard's block {@code pos} of a structure, as using it does, unless they
     * ride something, or someone sits there already; whether they sit.
     */
    static boolean sit(final ServerPlayer player, final BlockPos pos) {
        if (player.isPassenger()) {
            return false;
        }
        final ServerLevel level = player.serverLevel();
        final Optional<BlockAboard.Seat> seat = BlockAboard.at(level, pos).seat();
        if (seat.isEmpty() || MovingStructures.at(level, pos) == null || taken(level, pos) != null) {
            return false;
        }
        final StructureSeatEntity sitting = StructureSeatEntity.made(level, pos, seat.get());
        level.addFreshEntity(sitting);
        return player.startRiding(sitting, true);
    }

    /* A player uses a block of a structure that is a seat: they sit on it, unless crouching, or it is taken. */
    private static void onUse(final PlayerInteractEvent.RightClickBlock event) {
        final BlockPos pos = event.getPos();
        final Player player = event.getEntity();
        if (!YardLayout.isYardBlock(pos.getX()) || player.isSecondaryUseActive() || player.isPassenger()) {
            return;
        }
        final Level level = event.getLevel();
        if (BlockAboard.at(level, pos).seat().isEmpty() || MovingStructures.at(level, pos) == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (player instanceof ServerPlayer sitter) {
            sit(sitter, pos);
        }
    }

    /* The seat entity someone sits in on the yard's block {@code pos}, or null for a seat free. */
    private static @Nullable StructureSeatEntity taken(final ServerLevel level, final BlockPos pos) {
        final IStructureBody body = MovingStructures.at(level, pos);
        if (body == null) {
            return null;
        }
        final StructureFrame frame = body.frame();
        final Vec3 at = new Vec3(frame.worldX(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                frame.worldY(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                frame.worldZ(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        for (final Entity near : level.getEntities((Entity) null, new AABB(at, at).inflate(1),
                any -> any instanceof StructureSeatEntity)) {
            if (near instanceof StructureSeatEntity seat && seat.seat().equals(pos) && !seat.getPassengers()
                    .isEmpty()) {
                return seat;
            }
        }
        return null;
    }
}
