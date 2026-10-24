/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client;

import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.industrial.crc.CrcBlockEntity;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.crc.CrcPart;
import dev.jstech.industrial.crc.CrcStructure;
import java.util.List;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a formed Computational Research Centre as one cabinet of three frames from its terminal: in each place of its
 * gates the part installed there, seen through the glazed doors; in each cell of its rear the entry standing there, or
 * the panel; and its register lamps blinking while it works. Nothing is drawn while it is not formed: its blocks show.
 */
public final class CrcRenderer implements IKitDriver<CrcBlockEntity> {

    /* The bones of a rear cell, by what the terminal says stands there: the panel, then each entry. */
    private static final List<String> CELLS = List.of("panel", "power", "link", "cold", "hot", "heat");
    private static final int LAMPS = 12;
    /* The lamps' pace, in changes a second, and the stride that makes their pattern read as a register's. */
    private static final double BLINKS = 6.0;
    private static final int STRIDE = 7;
    private static final int LIT_OF = 3;

    private CrcRenderer() {
    }

    /** The renderer of the Computational Research Centre's terminal. */
    public static BlockEntityRenderer<CrcBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<CrcBlockEntity>formedOnly(CrcContent.MODEL.id())
                .driver(new CrcRenderer())
                .build();
    }

    @Override
    public void drive(final CrcBlockEntity crc, final ModelAnimator animator, final double seconds) {
        // Every visual is a bone shown or hidden: the parts, the entries and the lamps.
    }

    @Override
    public void move(final CrcBlockEntity crc, final ModelPose pose, final double seconds) {
        final List<Integer> shown = crc.shown();
        for (int place = 0; place < CrcBlockEntity.PLACES; place++) {
            final int code = place < shown.size() ? shown.get(place) : 0;
            for (final CrcPart part : CrcPart.values()) {
                show(pose, "place_" + place + "_" + part.bone(), code == part.id());
            }
        }
        for (int cell = 0; cell < CrcStructure.REAR_COLUMNS * CrcStructure.REAR_ROWS; cell++) {
            final int at = CrcBlockEntity.PLACES + cell;
            final int code = at < shown.size() ? shown.get(at) : CrcBlockEntity.CELL_PANEL;
            final String prefix = "cell_rear_" + cell % CrcStructure.REAR_COLUMNS + cell / CrcStructure.REAR_COLUMNS
                    + "_";
            for (int kind = 0; kind < CELLS.size(); kind++) {
                show(pose, prefix + CELLS.get(kind), code == kind);
            }
        }
        final boolean working = crc.running();
        final long beat = (long) Math.floor(seconds * BLINKS);
        for (int lamp = 0; lamp < LAMPS; lamp++) {
            show(pose, "lamp_" + lamp, working && Math.floorMod(beat + (long) lamp * STRIDE, LIT_OF) != 0);
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    private static void show(final ModelPose pose, final String bone, final boolean visible) {
        final int index = pose.index(bone);
        if (index >= 0) {
            pose.setHidden(index, !visible);
        }
    }
}
