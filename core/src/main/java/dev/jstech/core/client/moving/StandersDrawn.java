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
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * Whoever walks on the floor of a moving structure in its frame ({@link DeckGravity#walksIn}) is drawn standing on
 * it, on every player's game: upright on its floor about their feet, as the structure is drawn, facing where they look
 * along it, so a player crossing a ship rolled on its side is drawn walking its floor rather than standing out of it
 * up the world. The game draws everyone upright, turned about the world's up; this turns them in the floor's frame.
 */
final class StandersDrawn {

    /* Those turned as they are drawn this frame, set straight again once drawn. */
    private static final Set<LivingEntity> TURNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private StandersDrawn() {
    }

    /** Hands the game's bus the drawing of those walking a turned floor, once. */
    static void listen() {
        // Last, so a renderer that draws an entity its own way and stops the game's draws it as it would have.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderLivingEvent.Pre.class, StandersDrawn::before);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderLivingEvent.Post.class, StandersDrawn::after);
    }

    /**
     * The turn {@code stander} is drawn with, {@code partialTick} of the way through the tick, laid before the game's
     * own turn of them about the world's up: the floor's frame, and from the way the game faces them to the way they
     * look along the floor; null when they walk on no turned floor.
     */
    static @Nullable Quaternionf turn(final LivingEntity stander, final float partialTick) {
        if (!(stander instanceof Player player) || !walks(player)
                || !(DeckGravity.fieldOf(stander) instanceof ClientStructure structure)) {
            return null;
        }
        final StructurePose drawn = structure.drawnAt(partialTick);
        final StructureFrame frame = new StructureFrame(drawn, 0, 0, 0);
        final Vec3 look = Vec3.directionFromRotation(stander.getViewXRot(partialTick),
                stander.getViewYRot(partialTick));
        final double alongX = frame.unturnX(look.x, look.y, look.z);
        final double alongZ = frame.unturnZ(look.x, look.y, look.z);
        final float facing = (float) Math.toDegrees(Math.atan2(-alongX, alongZ));
        final float body = stander.yBodyRotO + (stander.yBodyRot - stander.yBodyRotO) * partialTick;
        // The game turns them by 180 less their body's yaw about the world's up: this lays the floor's frame before
        // it, and the floor's own turn by 180 less where they face along it, the game's turn taken back.
        return new Quaternionf((float) drawn.qx(), (float) drawn.qy(), (float) drawn.qz(), (float) drawn.qw())
                .rotateY((float) Math.toRadians(body - facing));
    }

    /**
     * Whether {@code player} walks on the floor of a structure in its frame, on this game: the player whose game it is
     * as their game moves them ({@link DeckWalk}); anyone else when they stand in the field of one turned or out in
     * space, on their feet, as their own game moves them there.
     */
    static boolean walks(final Player player) {
        if (player instanceof LocalPlayer local) {
            return DeckWalk.walks(local);
        }
        if (player.isPassenger() || player.isSpectator() || player.isFallFlying()) {
            return false;
        }
        final IStructureBody body = DeckGravity.fieldOf(player);
        return body != null && DeckGravity.walksIn(body);
    }

    private static void before(final RenderLivingEvent.Pre<?, ?> event) {
        final Quaternionf turn = turn(event.getEntity(), event.getPartialTick());
        if (turn == null) {
            return;
        }
        event.getPoseStack().pushPose();
        event.getPoseStack().mulPose(turn);
        TURNED.add(event.getEntity());
    }

    private static void after(final RenderLivingEvent.Post<?, ?> event) {
        if (TURNED.remove(event.getEntity())) {
            event.getPoseStack().popPose();
        }
    }
}
