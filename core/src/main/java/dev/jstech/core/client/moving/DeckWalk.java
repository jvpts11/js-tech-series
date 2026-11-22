/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.DeckGravity;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFrame;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Walking in a moving structure whose floor pulls towards itself ({@link DeckGravity}), on the player's own game:
 * turned, or out where its dimension pulls nothing, the structure's floor is down.
 *
 * <p>The game's own step is taken where the structure's blocks stand upright, in its plot of the yard, with the
 * player's place, speed and look turned into its frame; the step taken, they are turned back and the player set in
 * the world where the structure stands now. So walking, running, jumping, sneaking at an edge, stepping up a stair and
 * climbing a ladder are the game's own, on the deck, and the deck carries the player as it goes. The floor pulls as
 * hard as {@link DeckGravity#strength} says, where the world pulls otherwise or not at all; and the player's speed is
 * kept in the structure's frame, so a deck that turns turns it with them.
 */
public final class DeckWalk {

    /* The game's usual pull on a player, in blocks a tick each tick, and what the air leaves of a speed each tick. */
    private static final double USUAL_PULL = 0.08;
    private static final double AIR_DRAG = 0.98;
    /* How near a speed must be to the one last given back to count as unchanged since, squared. */
    private static final double SAME = 1.0e-12;
    /* How far under the feet the block a player stands on is looked for, in blocks. */
    private static final double UNDERFOOT = 0.2;
    /* How much faster up the world a speed set since the last step must be to be the game's jump, in blocks a tick. */
    private static final double LEAP = 0.2;
    private static boolean stepping;
    /* The structure the player last walked in, their speed in its frame, and that speed as the world was given it. */
    private static @Nullable IStructureBody walkedIn;
    private static Vec3 deckSpeed = Vec3.ZERO;
    private static @Nullable Vec3 givenBack;

    private DeckWalk() {
    }

    /** Whether {@code player} walks in the frame of a structure whose floor pulls them, rather than the world's. */
    public static boolean walks(final LocalPlayer player) {
        return body(player) != null;
    }

    /**
     * Takes the game's step {@code travel} for {@code player} in the frame of the structure whose floor pulls them,
     * if one does; whether it did.
     */
    public static boolean travel(final LocalPlayer player, final Runnable travel) {
        if (stepping) {
            return false;
        }
        final IStructureBody body = body(player);
        if (body == null) {
            walkedIn = null;
            givenBack = null;
            return false;
        }
        final StructureFrame frame = body.frame();
        final StructureFrame before = body.previousFrame();
        // Carried by the deck already this tick, the player stands where the structure stands now; else where it stood.
        final StructureFrame from = StructureFooting.standingOn(player) == body ? frame : before;
        final Vec3 at = player.position();
        final Vec3 speed = player.getDeltaMovement();
        final Vec3 deck;
        if (body != walkedIn || givenBack == null) {
            // Coming into the field, the player's own speed is the world's: the deck's is taken off it.
            deck = unturn(frame, speed.subtract(StructureFooting.speedAt(body, at)));
        } else {
            final Vec3 change = speed.subtract(givenBack);
            if (change.lengthSqr() < SAME) {
                deck = deckSpeed;
            } else if (change.y > LEAP) {
                // The game's jump sets the speed up the world, before its step: in the deck it leaps off the floor.
                final Vec3 sideways = unturn(frame, new Vec3(change.x, 0, change.z));
                deck = new Vec3(deckSpeed.x + sideways.x, speed.y, deckSpeed.z + sideways.z);
            } else {
                // Pushed since its last step, by a blow or a blast: the push is turned into the deck's frame.
                deck = deckSpeed.add(unturn(frame, change));
            }
        }
        final float yaw = player.getYRot();
        final float pitch = player.getXRot();
        final double xo = player.xo;
        final double yo = player.yo;
        final double zo = player.zo;
        final Vec3 look = unturn(frame, Vec3.directionFromRotation(pitch, yaw));
        player.setPos(from.yardX(at.x, at.y, at.z), from.yardY(at.x, at.y, at.z), from.yardZ(at.x, at.y, at.z));
        // Where it stood a tick ago, on the deck as the deck stood then: how far it walked on it, for its stride.
        player.xo = before.yardX(xo, yo, zo);
        player.yo = before.yardY(xo, yo, zo);
        player.zo = before.yardZ(xo, yo, zo);
        player.setDeltaMovement(deck);
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setXRot((float) Math.toDegrees(Math.asin(-Math.max(-1, Math.min(1, look.y)))));
        stepping = true;
        try {
            travel.run();
        } finally {
            stepping = false;
        }
        Vec3 stepped = player.getDeltaMovement();
        if (!player.onClimbable() && !player.isInWater() && !player.isInLava()) {
            stepped = stepped.add(0, -(DeckGravity.strength(player.level()) * USUAL_PULL - player.getGravity())
                    * AIR_DRAG, 0);
        }
        final Vec3 yard = player.position();
        final boolean grounded = player.onGround();
        player.setPos(frame.worldX(yard.x, yard.y, yard.z), frame.worldY(yard.x, yard.y, yard.z),
                frame.worldZ(yard.x, yard.y, yard.z));
        player.xo = xo;
        player.yo = yo;
        player.zo = zo;
        player.setYRot(yaw);
        player.setXRot(pitch);
        walkedIn = body;
        deckSpeed = stepped;
        givenBack = turn(frame, stepped);
        player.setDeltaMovement(givenBack);
        // On its floor, or in its field off it, the deck carries them, and the server hears where on it they are.
        if (grounded || StructureFooting.standingOn(player) != body) {
            StructureFooting.stood(player, body, BlockPos.containing(yard.x, yard.y - UNDERFOOT, yard.z));
        }
        return true;
    }

    /* The structure whose floor {@code player} walks on in its frame, or null where the world's walking holds. */
    private static @Nullable IStructureBody body(final LocalPlayer player) {
        if (player.isPassenger() || player.isSpectator() || player.noPhysics || player.getAbilities().flying
                || player.isFallFlying()) {
            return null;
        }
        final IStructureBody body = DeckGravity.fieldOf(player);
        return body != null && DeckGravity.walksIn(body) ? body : null;
    }

    /* {@code v} of the world turned into {@code frame}'s own directions. */
    private static Vec3 unturn(final StructureFrame frame, final Vec3 v) {
        return new Vec3(frame.unturnX(v.x, v.y, v.z), frame.unturnY(v.x, v.y, v.z), frame.unturnZ(v.x, v.y, v.z));
    }

    /* {@code v} of {@code frame}'s own directions turned into the world's. */
    private static Vec3 turn(final StructureFrame frame, final Vec3 v) {
        return new Vec3(frame.turnX(v.x, v.y, v.z), frame.turnY(v.x, v.y, v.z), frame.turnZ(v.x, v.y, v.z));
    }
}
