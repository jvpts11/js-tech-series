/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PlacedModel;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Where a cable block's wires end in a plug: each wire's type, the face it meets a device on, and how far along that
 * face its lane lies. Which plug it is depends on the device beyond, a video plug at a screen, so it is chosen where
 * the block is drawn, from the world it is drawn in: the world's mesh in a world, a manual's scene in a scene, which
 * the block entity's own world is not.
 *
 * @param sockets each plug still to choose
 */
public record CablePlugs(List<Socket> sockets) {

    /** Where a cable block's model data carries the plugs still to choose. */
    public static final ModelProperty<CablePlugs> PROPERTY = new ModelProperty<>();
    /** A block whose wires meet no device. */
    public static final CablePlugs NONE = new CablePlugs(List.of());

    public CablePlugs {
        sockets = List.copyOf(sockets);
    }

    /**
     * The model data with each plug chosen for the device beyond it in that world and added to the block's layout,
     * after which none is left to choose.
     */
    public static ModelData placed(final ModelData data, final BlockGetter level, final BlockPos pos) {
        final CablePlugs plugs = data.get(PROPERTY);
        if (plugs == null || plugs.sockets.isEmpty()) {
            return data;
        }
        final ModelLayout layout = data.get(ModelLayout.PROPERTY);
        final List<PlacedModel> placed = new ArrayList<>(layout == null ? List.of() : layout.models());
        for (final Socket socket : plugs.sockets) {
            placed.add(socket.placed(level.getBlockEntity(pos.relative(socket.face()))));
        }
        return data.derive().with(ModelLayout.PROPERTY, new ModelLayout(placed)).with(PROPERTY, NONE).build();
    }

    /**
     * A plug still to choose.
     *
     * @param type the wire's type, which has a plug for each kind of port
     * @param face the face the wire meets a device on
     * @param dx   how far the plug is moved east, in pixels, to its wire's lane
     * @param dy   how far it is moved up, in pixels
     * @param dz   how far it is moved south, in pixels
     */
    public record Socket(CableType type, Direction face, float dx, float dy, float dz) {

        /** The plug of the port it enters, a video plug at a screen; the wire's own before anything else. */
        PlacedModel placed(@Nullable final BlockEntity beyond) {
            final ResourceLocation model = beyond instanceof IPeripheralEndpoint peripheral
                    ? this.type.plug(peripheral.portKind()) : this.type.plug();
            return new PlacedModel(model, this.face, this.dx, this.dy, this.dz);
        }
    }
}
