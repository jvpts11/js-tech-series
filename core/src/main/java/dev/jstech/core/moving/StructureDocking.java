/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * Docking: two structures made one while they are docked, a shuttle to a station, a tug to its barge. Docked, the
 * visitor is held by the host where it stands ({@link StructureMounts}), moving as part of it, its mass and its
 * engines the host's; their momentum is shared, so a shuttle docking at speed sets the station going. Docking clamps
 * ({@link BlockAboard.Clamp}) dock by themselves: a clamp whose signal is on, meeting another structure's clamp face to
 * face, holds the other where it is and draws it in on itself, square to it, face on face, smoothly over a second or
 * so, however the two go; as its signal goes off, it lets go, the visitor pushed gently away.
 */
public final class StructureDocking {

    /* How fast a visitor let go by a clamp is pushed away from it, in metres a second. */
    private static final double PUSH_OFF = 0.5;
    /* How fast a clamp draws a visitor in, in metres a second, and turns it square, in degrees a second. */
    private static final double DRAW_SPEED = 1.5;
    private static final double DRAW_TURN = 90;
    /* Two clamps whose faces look this nearly against each other meet. */
    private static final double FACING = -0.9;
    private static final double HALF = 0.5;

    private StructureDocking() {
    }

    /**
     * Docks {@code visitor} to {@code host} where it stands: held by it, one with it, their momentum shared.
     * {@code clamp} is the host's block holding it. Refused, saying why, as {@link StructureMounts#mount} is.
     */
    public static StructureResult dock(final MovingStructure host, final MovingStructure visitor,
                                       final Optional<BlockPos> clamp) {
        final StructurePhysics carrier = StructureMounts.rootOf(host).physics();
        final StructurePhysics coming = visitor.physics();
        final Vector3d momentum = new Vector3d();
        double total = 0;
        if (carrier != null && carrier.active()) {
            momentum.fma(carrier.mass(), carrier.body().velocity());
            total += carrier.mass();
        }
        if (coming != null && coming.active()) {
            momentum.fma(coming.mass(), coming.body().velocity());
            total += coming.mass();
        }
        final StructureResult done = StructureMounts.mount(visitor, host, clamp);
        if (done.ok() && carrier != null && carrier.active() && total > 0) {
            carrier.body().velocity().set(momentum.div(total));
            carrier.wake();
        }
        return done;
    }

    /**
     * Lets {@code visitor} go from what it is docked to, going on at its place's speed and pushed away along
     * {@code away}, a way of the world, at half a metre a second.
     */
    public static StructureResult undock(final MovingStructure visitor, final Vec3 away) {
        final StructureResult done = StructureMounts.unmount(visitor, true);
        if (done.ok() && visitor.motion() instanceof PhysicsMotion going) {
            final Vec3 off = away.lengthSqr() > 0 ? away.normalize().scale(PUSH_OFF) : Vec3.ZERO;
            visitor.move(new PhysicsMotion(going.vx() + off.x, going.vy() + off.y, going.vz() + off.z, going.sx(),
                    going.sy(), going.sz(), going.warm()));
        }
        return done;
    }

    /*
     * A clamp of {@code structure} at {@code pos}, its signal on: it holds the clamp of another structure that meets
     * it face to face within its reach, drawing that structure in square to it. The structure of more blocks holds.
     */
    static void closeClamp(final ServerLevel level, final MovingStructure structure, final BlockPos pos,
                           final BlockState state, final BlockAboard.Clamp clamp) {
        if (heldAt(structure, pos) || structure.mount() != null && structure.mount().anchor().isPresent()) {
            return;
        }
        final Direction way = BlockPhysics.wayOf(state);
        final Vec3 face = faceOf(structure.frame(), pos, way);
        final Vec3 looks = turned(structure.frame(), way);
        final AABB around = new AABB(face, face).inflate(clamp.reach() + 1);
        for (final IStructureBody near : MovingStructures.near(level, around)) {
            if (near == structure || !near.ready() || !(near instanceof MovingStructure other)
                    || StructureMounts.rootOf(other) == StructureMounts.rootOf(structure)) {
                continue;
            }
            for (final Map.Entry<BlockPos, BlockAboard> each : other.aboard().blocks().entrySet()) {
                if (each.getValue().clamp().isEmpty()) {
                    continue;
                }
                final BlockPos theirs = each.getKey();
                final Direction theirWay = BlockPhysics.wayOf(level.getBlockState(theirs));
                final Vec3 theirFace = faceOf(other.frame(), theirs, theirWay);
                if (theirFace.distanceTo(face) > clamp.reach()
                        || turned(other.frame(), theirWay).dot(looks) > FACING) {
                    continue;
                }
                final boolean holds = structure.blocks() >= other.blocks();
                final MovingStructure host = holds ? structure : other;
                final MovingStructure visitor = holds ? other : structure;
                final BlockPos hostClamp = holds ? pos : theirs;
                final BlockPos visitorClamp = holds ? theirs : pos;
                if (visitor.mount() != null) {
                    continue;
                }
                // Held where it stands, their momentum shared at once, it is then drawn in on the host by a rule of
                // its own on it, so it comes in smoothly however the host goes.
                final StructurePose docked = docked(level, host, hostClamp, visitor, visitorClamp);
                if (dock(host, visitor, Optional.of(hostClamp)).ok()) {
                    visitor.move(new TowardMotion(host.frame().local(docked), DRAW_SPEED, DRAW_TURN));
                }
                return;
            }
        }
    }

    /* A clamp of {@code structure} at {@code pos}, its signal gone off: what it holds is let go, pushed away. */
    static void openClamp(final MovingStructure structure, final BlockPos pos, final BlockState state) {
        final Vec3 away = turned(structure.frame(), BlockPhysics.wayOf(state));
        for (final MovingStructure child : StructureMounts.children(structure)) {
            final StructureMount mount = child.mount();
            if (mount != null && mount.anchor().isPresent() && mount.anchor().get().equals(pos)) {
                undock(child, away);
            }
        }
        // A visitor whose own clamp lets go is let go too.
        final StructureMount mount = structure.mount();
        if (mount != null && mount.anchor().isPresent()) {
            final MovingStructure parent = StructureMounts.parentOf(structure);
            if (parent != null && parent.aboard().blocks().containsKey(mount.anchor().get())) {
                undock(structure, away.scale(-1));
            }
        }
    }

    /* Whether {@code structure}'s clamp at {@code pos} holds something already. */
    private static boolean heldAt(final MovingStructure structure, final BlockPos pos) {
        for (final MovingStructure child : StructureMounts.children(structure)) {
            final StructureMount mount = child.mount();
            if (mount != null && mount.anchor().isPresent() && mount.anchor().get().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Where {@code visitor} stands drawn in to {@code host}, in the world: turned so its clamp looks straight against
     * the host's, square to the host by the quarter turn nearest how it is turned, its clamp's face on the host's.
     */
    private static StructurePose docked(final ServerLevel level, final MovingStructure host, final BlockPos hostClamp,
                                        final MovingStructure visitor, final BlockPos visitorClamp) {
        final Direction hostWay = BlockPhysics.wayOf(level.getBlockState(hostClamp));
        final Direction visitorWay = BlockPhysics.wayOf(level.getBlockState(visitorClamp));
        final StructureFrame hostFrame = host.frame();
        final StructureFrame visitorFrame = visitor.frame();
        final Vector3d hostLooks = vector(turned(hostFrame, hostWay));
        final Direction hostSide = sideOf(hostWay);
        final Vector3d hostUp = vector(turned(hostFrame, hostSide));
        final Vector3d hostAcross = new Vector3d(hostLooks).cross(hostUp);
        // The visitor's own side, as it is turned now, goes to whichever of the host's four sides is nearest it.
        final Direction visitorSide = sideOf(visitorWay);
        final Vector3d visitorUpNow = vector(turned(visitorFrame, visitorSide));
        final Vector3d[] choices = {new Vector3d(hostUp), new Vector3d(hostUp).negate(), new Vector3d(hostAcross),
                new Vector3d(hostAcross).negate()};
        Vector3d up = choices[0];
        for (final Vector3d choice : choices) {
            if (choice.dot(visitorUpNow) > up.dot(visitorUpNow)) {
                up = choice;
            }
        }
        final Vector3d looks = new Vector3d(hostLooks).negate();
        final Vector3d across = new Vector3d(looks).cross(up);
        // The turn taking the visitor's yard ways (its clamp's way, its side, across) onto those of the world.
        final Vector3d ownLooks = vector(visitorWay);
        final Vector3d ownUp = vector(visitorSide);
        final Vector3d ownAcross = new Vector3d(ownLooks).cross(ownUp);
        final Matrix3d world = new Matrix3d().setColumn(0, looks).setColumn(1, up).setColumn(2, across);
        final Matrix3d own = new Matrix3d().setColumn(0, ownLooks).setColumn(1, ownUp).setColumn(2, ownAcross);
        final Matrix3d turn = world.mul(own.transpose());
        final Quaterniond rotation = new Quaterniond().setFromNormalized(turn);
        final StructurePose turned = new StructurePose(0, 0, 0, rotation.x, rotation.y, rotation.z, rotation.w);
        final StructureFrame onlyTurned = new StructureFrame(turned, visitor.pivotX(), visitor.pivotY(),
                visitor.pivotZ());
        final Vec3 hostFace = faceOf(hostFrame, hostClamp, hostWay);
        final Vec3 ownFace = faceOf(onlyTurned, visitorClamp, visitorWay);
        return turned.movedTo(hostFace.x - ownFace.x, hostFace.y - ownFace.y, hostFace.z - ownFace.z);
    }

    /* A side of a block looking {@code way}, at right angles to it: up for a block looking level, north otherwise. */
    private static Direction sideOf(final Direction way) {
        return way.getAxis().isHorizontal() ? Direction.UP : Direction.NORTH;
    }

    /* The world's place of the middle of the face of the yard's block {@code pos} looking {@code way}. */
    private static Vec3 faceOf(final StructureFrame frame, final BlockPos pos, final Direction way) {
        final double x = pos.getX() + HALF + HALF * way.getStepX();
        final double y = pos.getY() + HALF + HALF * way.getStepY();
        final double z = pos.getZ() + HALF + HALF * way.getStepZ();
        return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /* The world's way of the yard's way {@code way}, as {@code frame} is turned. */
    private static Vec3 turned(final StructureFrame frame, final Direction way) {
        return new Vec3(frame.turnX(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnY(way.getStepX(), way.getStepY(), way.getStepZ()),
                frame.turnZ(way.getStepX(), way.getStepY(), way.getStepZ()));
    }

    private static Vector3d vector(final Vec3 way) {
        return new Vector3d(way.x, way.y, way.z);
    }

    private static Vector3d vector(final Direction way) {
        return new Vector3d(way.getStepX(), way.getStepY(), way.getStepZ());
    }
}
