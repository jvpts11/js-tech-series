/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.moving.SeatLook;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

/**
 * Whoever sits on a seat of a moving structure that leans or pitches is drawn sitting in it, on every player's game:
 * leaning with it about where they sit, as the structure is drawn, so a pilot pitched up lies back in the seat with
 * the cockpit rather than standing up out of it. The seat already sits them along its up ({@code StructureSeatEntity});
 * the game draws everyone upright, and this turns them in it. One lying on a seat is drawn by {@link SleepersDrawn}.
 */
final class SittersDrawn {

    /* Those turned as they are drawn this frame, set straight again once drawn. */
    private static final Set<LivingEntity> TURNED = Collections.newSetFromMap(new IdentityHashMap<>());

    private SittersDrawn() {
    }

    /** Hands the game's bus the drawing of sitters, once. */
    static void listen() {
        // Last, so a renderer that draws an entity its own way and stops the game's draws it as it would have.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderLivingEvent.Pre.class, SittersDrawn::before);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, RenderLivingEvent.Post.class, SittersDrawn::after);
    }

    private static void before(final RenderLivingEvent.Pre<?, ?> event) {
        final LivingEntity sitter = event.getEntity();
        if (!(sitter.getVehicle() instanceof StructureSeatEntity seat) || seat.lyingHead() != null
                || !(seat.structure() instanceof ClientStructure structure)) {
            return;
        }
        final StructurePose drawn = structure.drawnAt(event.getPartialTick());
        final StructureFrame frame = new StructureFrame(drawn, 0, 0, 0);
        if (!SeatLook.tilted(frame)) {
            return;
        }
        // Where they sit, from where they are drawn: their eyes stand along the seat's up from it, as the seat has it.
        final Vec3 attached = sitter.getVehicleAttachmentPoint(seat);
        final double eyes = sitter.getEyeHeight() - attached.y;
        final float x = (float) (attached.x - frame.turnX(0, eyes, 0));
        final float y = (float) (sitter.getEyeHeight() - frame.turnY(0, eyes, 0));
        final float z = (float) (attached.z - frame.turnZ(0, eyes, 0));
        // How the structure leans apart from its heading, which the game's own turn of the sitter already gives.
        final Quaternionf lean = new Quaternionf((float) drawn.qx(), (float) drawn.qy(), (float) drawn.qz(),
                (float) drawn.qw()).mul(new Quaternionf().rotationY((float) Math.toRadians(drawn.yawDegrees())));
        event.getPoseStack().pushPose();
        event.getPoseStack().translate(x, y, z);
        event.getPoseStack().mulPose(lean);
        event.getPoseStack().translate(-attached.x, -attached.y, -attached.z);
        TURNED.add(sitter);
    }

    private static void after(final RenderLivingEvent.Post<?, ?> event) {
        if (TURNED.remove(event.getEntity())) {
            event.getPoseStack().popPose();
        }
    }
}
