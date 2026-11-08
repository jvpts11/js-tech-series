/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a Mainframe as one cabinet from its origin block, the model facing north with the origin in the
 * middle of its bottom front row, so each facing turns it about that block onto the cabinet's own footprint.
 *
 * <p>What a player installs is what a player sees through the service bay, each part drawn by a model of its own
 * ({@link PartModels}), as a small computer's are: the board stands on the back of the card bay facing the front, on
 * its seats the processors with the cooler over each, the memory and the cards; the supply lies in its niche on the
 * floor of the disk bay as a computer holds it, with no leads, as no computer has cables inside; and each disk rides in
 * its sled, centred in the sled's window on the seat for its form. A board is made, like a computer's, to face the
 * open side of a case, so on the cabinet's seat it is turned a quarter about the vertical.
 *
 * <p>The machine states itself in hardware, never on a screen: three annunciator lamps for running,
 * network and fault, and on each sled the lamp of a disk that carries a system. A Mainframe is read on a linked
 * monitor like every other computer in the mod.
 */
public final class MainframeRenderer implements IKitDriver<MainframeBlockEntity>, IKitExtras<MainframeBlockEntity> {

    /** The roof fans and the tape reels, which turn while the machine is up. */
    private static final String WORK = "work";
    private static final String WORK_CLIP = "animation.mainframe.work";
    private static final String[] DISK_LAMPS = DeviceLamps.numbered("disk_lamp_", MainframeBlockEntity.DISK_SLOTS);
    /** Where the board's model goes: it stands there turned a quarter, to face the front. */
    private static final String SEAT_BOARD = "seat_board";
    private static final float QUARTER = 90f;
    private static final String[] CPU_SEATS = DeviceLamps.numbered("seat_cpu_", MainframeBlockEntity.CPU_SLOTS);
    private static final String[] RAM_SEATS = DeviceLamps.numbered("seat_ram_", MainframeBlockEntity.RAM_SLOTS);
    private static final String[] CARD_SEATS = DeviceLamps.numbered("seat_card_", MainframeBlockEntity.GPU_SLOTS);
    private static final String DISK = "disk_";

    /* Each sled's seat for each form of disk: seat_<form>_<sled>. */
    private static final Map<String, String[]> DISK_SEATS = new HashMap<>();

    private MainframeRenderer() {
    }

    /** The renderer of the Mainframes; the cabinet spans twelve blocks, so it is drawn within its whole box. */
    public static BlockEntityRenderer<MainframeBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        final MainframeRenderer drawn = new MainframeRenderer();
        return KitBlockEntityRenderer.<MainframeBlockEntity>look(ComputingLooks.MAINFRAME)
                .driver(drawn)
                .extras(drawn)
                .box(MainframeBlockEntity::renderBox)
                .build();
    }

    /* The lamps and the panel are bones shown or hidden, the parts drawn by their own models, not clips. */
    @Override
    public void drive(final MainframeBlockEntity mainframe, final ModelAnimator animator, final double seconds) {
        DeviceClips.play(animator, WORK, mainframe.visualRunning() ? WORK_CLIP : null, seconds);
    }

    @Override
    public void move(final MainframeBlockEntity mainframe, final ModelPose pose, final double seconds) {
        for (int i = 0; i < MainframeBlockEntity.DISK_SLOTS; i++) {
            DeviceLamps.show(pose, DISK_LAMPS[i], mainframe.hardwareInstalled(MainframeBlockEntity.DISK_SLOTS_START + i)
                    && mainframe.diskCarriesSystem(i));
        }

        final boolean running = mainframe.visualRunning();
        final boolean valid = mainframe.visualBuildValid();
        DeviceLamps.show(pose, "lamp_run", running && valid);
        DeviceLamps.show(pose, "lamp_net", running && mainframe.visualNetworked());
        // Amber-and-red for "powered but the build will not come up": parts are in and it still fails.
        DeviceLamps.show(pose, "lamp_fault", running && !valid);
        DeviceLamps.show(pose, "lightbar_plinth", running && valid);
        DeviceLamps.show(pose, "lightbar_cornice", running && valid);
        // The Transition cabinet's blue slit down the mullion, the Advanced one's white lines up its sides.
        DeviceLamps.show(pose, "lightbar_slit", running && valid);
        DeviceLamps.show(pose, "lightbar_edges", running && valid);
        DeviceLamps.show(pose, "service_panel", !mainframe.servicePanelOff());
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    @Override
    public void draw(final MainframeBlockEntity mainframe, final KitInstance cabinet, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        final PartModels.Drawer drawer = new PartModels.Drawer(mainframe, mainframe.visualRunning(), poses, buffers,
                light, overlay, partialTick);
        board(mainframe, cabinet, drawer, poses);
        drawer.supply(MainframeBlockEntity.PSU_SLOT, mainframe.installedPart(MainframeBlockEntity.PSU_SLOT), cabinet,
                PartModels.Drawer.NO_LEADS);
        for (int k = 0; k < MainframeBlockEntity.DISK_SLOTS; k++) {
            final int at = MainframeBlockEntity.DISK_SLOTS_START + k;
            final PartModels.Part disk = PartModels.part(mainframe.installedPart(at));
            if (disk != null && disk.kind() != null && disk.kind().startsWith(DISK)) {
                drawer.draw(PartModels.Drawer.key(at), disk, cabinet, diskSeat(disk.kind().substring(DISK.length()), k),
                        false, null, null);
            }
        }
    }

    /*
     * The board on its seat, turned to face the front, and on its own seats what stands on it: a processor and its
     * cooler on each socket, the memory and the cards.
     */
    private static void board(final MainframeBlockEntity mainframe, final KitInstance cabinet,
                              final PartModels.Drawer drawer, final PoseStack poses) {
        final PartModels.Part board = PartModels.part(mainframe.installedPart(MainframeBlockEntity.MOTHERBOARD_SLOT));
        if (board == null) {
            return;
        }
        poses.pushPose();
        if (KitRenderer.toBone(cabinet, SEAT_BOARD, poses)) {
            poses.mulPose(Axis.YP.rotationDegrees(QUARTER));
            final KitInstance on = drawer.draw(PartModels.Drawer.key(MainframeBlockEntity.MOTHERBOARD_SLOT), board,
                    null, null, false, null, null);
            for (int k = 0; on != null && k < MainframeBlockEntity.CPU_SLOTS; k++) {
                final int cpu = MainframeBlockEntity.CPU_SLOTS_START + k;
                final int cooler = MainframeBlockEntity.COOLER_SLOTS_START + k;
                drawer.processor(cpu, mainframe.installedPart(cpu), cooler, mainframe.installedPart(cooler), on,
                        CPU_SEATS[k], null, null);
            }
            for (int k = 0; on != null && k < MainframeBlockEntity.RAM_SLOTS; k++) {
                final int at = MainframeBlockEntity.RAM_SLOTS_START + k;
                drawer.slot(at, mainframe.installedPart(at), on, RAM_SEATS[k]);
            }
            for (int k = 0; on != null && k < MainframeBlockEntity.GPU_SLOTS; k++) {
                final int at = MainframeBlockEntity.GPU_SLOTS_START + k;
                drawer.slot(at, mainframe.installedPart(at), on, CARD_SEATS[k]);
            }
        }
        poses.popPose();
    }

    private static String diskSeat(final String form, final int sled) {
        return DISK_SEATS.computeIfAbsent(form, named -> DeviceLamps.numbered("seat_" + named + "_",
                MainframeBlockEntity.DISK_SLOTS))[sled];
    }
}
