/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelPose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a rack as one cabinet from its origin block, the model facing north with the cabinet extending east and
 * south of the origin, so each facing turns it about the origin onto the cabinet's own footprint. The
 * cabinet bone is always there; every row has one bone per unit type and only the one matching what is seated shows,
 * so an installed server is literally in the rack in its own era's shape. The supercomputer's light bar follows the
 * bays, its chassis bands follow the seated nodes, and its livery panel hides when taken off for service.
 */
public final class RackRenderer implements IKitDriver<ServerRackBlockEntity> {

    private static final int ROWS = ServerRackBlockEntity.CAPACITY_U;
    /** The roof fans, which turn while any bay is powered. */
    private static final String FANS = "fans";
    private static final String FANS_CLIP = "animation.rack.fans";
    private static final String ROW = "row_";
    private static final String BAND = "band";
    /* The bones of each cabinet's rows, worked out once from their names: row_<row>_<unit>. */
    private static final ModelFacts<List<RowBone>> ROW_BONES = new ModelFacts<>(RackRenderer::rowBonesOf);

    private RackRenderer() {
    }

    /** The renderer of the racks; a cabinet spans twelve blocks, so it is drawn within its whole box. */
    public static BlockEntityRenderer<ServerRackBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<ServerRackBlockEntity>look(ComputingLooks.RACK)
                .driver(new RackRenderer())
                .box(ServerRackBlockEntity::renderBox)
                .build();
    }

    /* The light bar and the seated units are bones shown or hidden, not clips. */
    @Override
    public void drive(final ServerRackBlockEntity rack, final ModelAnimator animator, final double seconds) {
        DeviceClips.play(animator, FANS, rack.anyBayOn() ? FANS_CLIP : null, seconds);
    }

    /*
     * Every row bone is decided here, including units the code has no seat for yet (a bone the model carries but
     * nothing can mount stays hidden), so an empty row is really empty.
     */
    @Override
    public void move(final ServerRackBlockEntity rack, final ModelPose pose, final double seconds) {
        for (final RowBone bone : ROW_BONES.of(pose.model())) {
            pose.setHidden(bone.index(), !rowBoneVisible(rack, bone.row(), bone.unit()));
        }
        final boolean on = rack.anyBayOn();
        DeviceLamps.show(pose, "lightbar_online", on);
        DeviceLamps.show(pose, "lightbar_fault", !on && rack.anyComputerSeated());
        DeviceLamps.show(pose, "service_panel", !rack.servicePanelOff());
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /** Rows count from the top of the cabinet, like the rack's slots; a 2U unit at row r also covers r + 1. */
    private static boolean rowBoneVisible(final ServerRackBlockEntity rack, final int row, final String unit) {
        if (row < 0 || row >= ROWS) {
            return false;
        }
        final int seated = rack.unitCodeAt(row);
        if (unit.equals(BAND)) {
            // A chassis band lights for both rows a seated 2U node covers.
            return ServerRackBlockEntity.isNodeUnit(seated)
                    || (row > 0 && ServerRackBlockEntity.isNodeUnit(rack.unitCodeAt(row - 1)));
        }
        return seated != ServerRackBlockEntity.UNIT_NONE
                && unit.equals(ServerRackBlockEntity.UNIT_BONES[seated]);
    }

    /* Every bone of a cabinet's model that is a row's unit. */
    private static List<RowBone> rowBonesOf(final ModelData model) {
        final List<RowBone> rows = new ArrayList<>();
        final List<ModelBone> bones = model.bones();
        for (int i = 0; i < bones.size(); i++) {
            final String name = bones.get(i).name();
            if (!name.startsWith(ROW)) {
                continue;
            }
            final int cut = name.indexOf('_', ROW.length());
            if (cut > ROW.length()) {
                try {
                    rows.add(new RowBone(i, Integer.parseInt(name.substring(ROW.length(), cut)),
                            name.substring(cut + 1)));
                } catch (final NumberFormatException notARow) {
                    // A bone named as a row but numbered as none is no row's.
                }
            }
        }
        return List.copyOf(rows);
    }

    /**
     * A bone of a row's unit.
     *
     * @param index its place among the model's bones
     * @param row   the row it is in, from the top
     * @param unit  the unit it shows, as {@link ServerRackBlockEntity#UNIT_BONES} names it, or a chassis band
     */
    private record RowBone(int index, int row, String unit) {
    }
}
