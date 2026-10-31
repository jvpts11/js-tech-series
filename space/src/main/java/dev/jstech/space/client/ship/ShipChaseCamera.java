/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.camera.ThirdPersonView;
import dev.jstech.core.client.camera.ThirdPersonCameras;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * The camera behind a ship, in the third person, for whoever sits aboard it: round the ship's middle and as far back as
 * the ship is long and some, so the ship is seen whole from behind as the approved flight shows it, not from inside its
 * hull, where the game's own camera would stand.
 */
public final class ShipChaseCamera {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_chase");
    private static final int PRIORITY = 20;
    /* How far back from the ship's middle, as a share of its longest side, and at least, in blocks. */
    private static final double BACK = 1.2;
    private static final double LEAST = 8;
    private static final double HALF = 0.5;

    private ShipChaseCamera() {
    }

    /** Registers the camera, once, from the mod's player-side entry point. */
    public static void listen() {
        ThirdPersonCameras.register(ID, PRIORITY, ShipChaseCamera::view);
    }

    private static @Nullable ThirdPersonView view(final Entity viewer, final boolean front, final float partialTick) {
        if (!(viewer.getVehicle() instanceof StructureSeatEntity seat)) {
            return null;
        }
        final int plot = YardLayout.plotOfBlock(seat.seat().getX(), seat.seat().getZ());
        final ClientStructure structure = ClientMovingStructures.byPlot(plot);
        if (ShipConsoles.of(plot) == null || structure == null || !structure.ready()) {
            return null;
        }
        // The ship's middle where it is drawn this frame, between its last tick's place and this one's.
        final Box bounds = structure.bounds();
        final double x = (bounds.minX() + bounds.maxX() + 1) * HALF;
        final double y = (bounds.minY() + bounds.maxY() + 1) * HALF;
        final double z = (bounds.minZ() + bounds.maxZ() + 1) * HALF;
        final StructureFrame drawn = new StructureFrame(structure.drawnAt(partialTick), structure.pivotX(),
                structure.pivotY(), structure.pivotZ());
        final int dx = bounds.maxX() - bounds.minX() + 1;
        final int dy = bounds.maxY() - bounds.minY() + 1;
        final int dz = bounds.maxZ() - bounds.minZ() + 1;
        final double back = Math.max(LEAST, Math.max(dx, Math.max(dy, dz)) * BACK);
        // Turned round a point on the line of sight just clear of the ship, the same view as round its middle, so the
        // camera's way back never starts inside its hull, which would hold it there.
        final double clear = Math.sqrt(dx * dx + dy * dy + dz * dz) * HALF + 1;
        final Vec3 look = viewer.getViewVector(partialTick).scale(front ? -1 : 1);
        final double from = Math.min(clear, back - 1);
        return ThirdPersonView.behind(back - from).around(new Vector3d(drawn.worldX(x, y, z) - look.x * from,
                drawn.worldY(x, y, z) - look.y * from, drawn.worldZ(x, y, z) - look.z * from));
    }
}
