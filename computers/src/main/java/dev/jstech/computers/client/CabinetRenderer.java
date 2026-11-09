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
import dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.content.GeoLook;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.COOLER_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.CPU_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.CPU_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.DISK_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.DISK_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.GPU_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.GPU_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.MOTHERBOARD_SLOT;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.PSU_SLOT;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.RAM_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.RAM_SLOTS_START;

/**
 * Draws a cabinet computer, the Mainframe or the WAN Gateway Computer, as one model from its origin block, the model
 * facing north with the origin on its bottom front row, so each facing turns it about that block onto the cabinet's
 * own footprint.
 *
 * <p>What a player installs is what a player sees through the service bay, each part drawn by a model of its own
 * ({@link PartModels}), as a small computer's are: the board stands on the back of the card bay facing the front, on
 * its seats the processors with the cooler over each, the memory and the cards; the supply lies in its niche on the
 * floor of the disk bay as a computer holds it, with no leads, as no computer has cables inside; and each disk rides in
 * its sled, centred in the sled's window on the seat for its form. A board is made, like a computer's, to face the
 * open side of a case, so on the cabinet's seat it is turned a quarter about the vertical.
 *
 * <p>The machine states itself in hardware, never on a screen: three annunciator lamps for running,
 * network and fault, and on each sled the lamp of a disk that carries a system. A cabinet is read on a linked
 * monitor like every other computer in the mod.
 *
 * @param <B> the cabinet's block entity
 */
public class CabinetRenderer<B extends AbstractCabinetComputerBlockEntity> implements IKitDriver<B>, IKitExtras<B> {

    /* The clip the cabinet's work bone plays while the machine is up: its roof fans, and the tape reels of some. */
    private final String workClip;

    /** The roof fans and the tape reels, which turn while the machine is up. */
    private static final String WORK = "work";
    private static final String MAINFRAME_WORK = "animation.mainframe.work";
    private static final String[] DISK_LAMPS = DeviceLamps.numbered("disk_lamp_", DISK_SLOTS);
    /** Where the board's model goes: it stands there turned a quarter, to face the front. */
    private static final String SEAT_BOARD = "seat_board";
    private static final float QUARTER = 90f;
    private static final String[] CPU_SEATS = DeviceLamps.numbered("seat_cpu_", CPU_SLOTS);
    private static final String[] RAM_SEATS = DeviceLamps.numbered("seat_ram_", RAM_SLOTS);
    private static final String[] CARD_SEATS = DeviceLamps.numbered("seat_card_", GPU_SLOTS);
    private static final String DISK = "disk_";

    /* Each sled's seat for each form of disk: seat_<form>_<sled>. */
    private static final Map<String, String[]> DISK_SEATS = new HashMap<>();

    protected CabinetRenderer(final String workClip) {
        this.workClip = workClip;
    }

    /** The renderer of the Mainframes; the cabinet spans twelve blocks, so it is drawn within its whole box. */
    public static BlockEntityRenderer<MainframeBlockEntity> mainframe(
            final BlockEntityRendererProvider.Context context) {
        return build(ComputingLooks.MAINFRAME, new CabinetRenderer<>(MAINFRAME_WORK));
    }

    /* The lamps and the panel are bones shown or hidden, the parts drawn by their own models, not clips. */
    @Override
    public void drive(final B cabinet, final ModelAnimator animator, final double seconds) {
        DeviceClips.play(animator, WORK, cabinet.visualRunning() ? workClip : null, seconds);
    }

    @Override
    public void move(final B cabinet, final ModelPose pose, final double seconds) {
        for (int i = 0; i < DISK_SLOTS; i++) {
            DeviceLamps.show(pose, DISK_LAMPS[i], cabinet.hardwareInstalled(DISK_SLOTS_START + i)
                    && cabinet.diskCarriesSystem(i));
        }

        final boolean running = cabinet.visualRunning();
        final boolean valid = cabinet.visualBuildValid();
        DeviceLamps.show(pose, "lamp_run", running && valid);
        DeviceLamps.show(pose, "lamp_net", running && cabinet.visualNetworked());
        // Amber-and-red for "powered but the build will not come up": parts are in and it still fails.
        DeviceLamps.show(pose, "lamp_fault", running && !valid);
        DeviceLamps.show(pose, "lightbar_plinth", running && valid);
        DeviceLamps.show(pose, "lightbar_cornice", running && valid);
        // The Transition cabinet's blue slit down the mullion, the Advanced one's white lines up its sides.
        DeviceLamps.show(pose, "lightbar_slit", running && valid);
        DeviceLamps.show(pose, "lightbar_edges", running && valid);
        DeviceLamps.show(pose, "service_panel", !cabinet.servicePanelOff());
        moveOwn(cabinet, pose);
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    @Override
    public void draw(final B cabinet, final KitInstance model, final PoseStack poses, final MultiBufferSource buffers,
                     final int light, final int overlay, final float partialTick) {
        final PartModels.Drawer drawer = new PartModels.Drawer(cabinet, cabinet.visualRunning(), poses, buffers,
                light, overlay, partialTick);
        board(cabinet, model, drawer, poses);
        drawer.supply(PSU_SLOT, cabinet.installedPart(PSU_SLOT), model, PartModels.Drawer.NO_LEADS);
        for (int k = 0; k < DISK_SLOTS; k++) {
            final int at = DISK_SLOTS_START + k;
            final PartModels.Part disk = PartModels.part(cabinet.installedPart(at));
            if (disk != null && disk.kind() != null && disk.kind().startsWith(DISK)) {
                drawer.draw(PartModels.Drawer.key(at), disk, model, diskSeat(disk.kind().substring(DISK.length()), k),
                        false, null, null);
            }
        }
    }

    /** The renderer {@code drawn} makes of the cabinets {@code look} names, drawn within each one's whole box. */
    protected static <B extends AbstractCabinetComputerBlockEntity> BlockEntityRenderer<B> build(
            final GeoLook<B> look, final CabinetRenderer<B> drawn) {
        return KitBlockEntityRenderer.<B>look(look)
                .driver(drawn)
                .extras(drawn)
                .box(AbstractCabinetComputerBlockEntity::renderBox)
                .build();
    }

    /** What a kind of cabinet shows or hides of its own, after the lamps and the panel every cabinet has. */
    protected void moveOwn(final B cabinet, final ModelPose pose) {
    }

    /*
     * The board on its seat, turned to face the front, and on its own seats what stands on it: a processor and its
     * cooler on each socket, the memory and the cards.
     */
    private static void board(final AbstractCabinetComputerBlockEntity cabinet, final KitInstance model,
                              final PartModels.Drawer drawer, final PoseStack poses) {
        final PartModels.Part board = PartModels.part(cabinet.installedPart(MOTHERBOARD_SLOT));
        if (board == null) {
            return;
        }
        poses.pushPose();
        if (KitRenderer.toBone(model, SEAT_BOARD, poses)) {
            poses.mulPose(Axis.YP.rotationDegrees(QUARTER));
            final KitInstance on = drawer.draw(PartModels.Drawer.key(MOTHERBOARD_SLOT), board, null, null, false,
                    null, null);
            for (int k = 0; on != null && k < CPU_SLOTS; k++) {
                final int cpu = CPU_SLOTS_START + k;
                final int cooler = COOLER_SLOTS_START + k;
                drawer.processor(cpu, cabinet.installedPart(cpu), cooler, cabinet.installedPart(cooler), on,
                        CPU_SEATS[k], null, null);
            }
            for (int k = 0; on != null && k < RAM_SLOTS; k++) {
                final int at = RAM_SLOTS_START + k;
                drawer.slot(at, cabinet.installedPart(at), on, RAM_SEATS[k]);
            }
            for (int k = 0; on != null && k < GPU_SLOTS; k++) {
                final int at = GPU_SLOTS_START + k;
                drawer.slot(at, cabinet.installedPart(at), on, CARD_SEATS[k]);
            }
        }
        poses.popPose();
    }

    private static String diskSeat(final String form, final int sled) {
        return DISK_SEATS.computeIfAbsent(form, named -> DeviceLamps.numbered("seat_" + named + "_", DISK_SLOTS))[sled];
    }
}
