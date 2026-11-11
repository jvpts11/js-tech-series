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
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.hardware.RigGeometry;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a Mining Computer as its open frame (computers/docs/design/teracoin.md), every part in view: the mining board
 * lying face up on the deck with the processor and the memory on its seats, the supply and the disk beside it, and on
 * the rails the risers the player put in, each with the card seated on it standing, its bracket to the front. A riser's
 * lamp is lit while the rig runs with a card on it; the Advanced's fan bar turns while it runs, and so do the cards'
 * own fans. Every part is the part's own model, placed as the approved model places it.
 */
public final class RigRenderer implements IKitDriver<MiningComputerBlockEntity>,
        IKitExtras<MiningComputerBlockEntity> {

    private static final int MOST = MiningComputerBlockEntity.MOST_RISERS;
    private static final String[] RISERS = DeviceLamps.numbered("riser_", MOST);
    private static final String[] LAMPS_ON = DeviceLamps.numbered("lamp_on_", MOST);
    private static final String[] LAMPS_OFF = DeviceLamps.numbered("lamp_off_", MOST);
    private static final String[] RAM_SEATS = DeviceLamps.numbered("seat_ram_", MiningComputerBlockEntity.RAM_SLOTS);
    private static final String SEAT_CPU = "seat_cpu";
    private static final String PSU = "psu_";
    private static final String DISK = "disk_";
    private static final ModelFacts<PartModels.Spins> FANS = new ModelFacts<>(model -> PartModels.Spins.of(
            model.bones()));
    private static final float PIXEL = 16f;
    private static final float MIDDLE = 8f;
    private static final float QUARTER = 90f;
    private static final float HALF = 180f;
    /* A card stands this far over the rail and this far behind the front of its riser's place. */
    private static final float CARD_LIFT = 0.7f;
    private static final float CARD_BACK = 0.5f;

    private RigRenderer() {
    }

    /** The renderer of the Mining Computers, drawn within each rig's whole box. */
    public static BlockEntityRenderer<MiningComputerBlockEntity> create(
            final BlockEntityRendererProvider.Context context) {
        final RigRenderer drawn = new RigRenderer();
        return KitBlockEntityRenderer.<MiningComputerBlockEntity>look(ComputingLooks.MINING_COMPUTER)
                .driver(drawn)
                .extras(drawn)
                .box(MiningComputerBlockEntity::renderBox)
                .build();
    }

    @Override
    public void drive(final MiningComputerBlockEntity rig, final ModelAnimator animator, final double seconds) {
    }

    /* The risers put in, each lamp lit while the rig runs with a card on its riser; the fan bar turning. */
    @Override
    public void move(final MiningComputerBlockEntity rig, final ModelPose pose, final double seconds) {
        final boolean running = rig.visualRunning();
        for (int k = 0; k < MOST; k++) {
            final boolean riser = rig.hasRiser(k);
            final boolean lit = riser && running && rig.cardPart(k) != null;
            DeviceLamps.show(pose, RISERS[k], riser);
            DeviceLamps.show(pose, LAMPS_ON[k], lit);
            DeviceLamps.show(pose, LAMPS_OFF[k], riser && !lit);
        }
        if (running) {
            FANS.of(pose.model()).turn(pose, PartModels.fanTurn(seconds));
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    @Override
    public void draw(final MiningComputerBlockEntity rig, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        final PartModels.Drawer drawer = new PartModels.Drawer(rig, rig.visualRunning(), poses, buffers, light,
                overlay, partialTick);
        final Place place = Place.of(rig.rigEra());
        board(rig, drawer, poses, place);
        final PartModels.Part psu = PartModels.part(rig.installedPart(MiningComputerBlockEntity.PSU_SLOT));
        if (psu != null && psu.kind() != null && psu.kind().startsWith(PSU)) {
            poses.pushPose();
            at(poses, place.psu());
            if (place.psuOnSide()) {
                poses.mulPose(Axis.ZP.rotationDegrees(QUARTER));
            }
            drawer.draw(PartModels.Drawer.key(MiningComputerBlockEntity.PSU_SLOT), psu, null, null, false,
                    PartModels.Drawer.NO_LEADS, null);
            poses.popPose();
        }
        final PartModels.Part disk = PartModels.part(rig.installedPart(MiningComputerBlockEntity.DISK_SLOT));
        if (disk != null && disk.kind() != null && disk.kind().startsWith(DISK)) {
            poses.pushPose();
            at(poses, place.disk());
            drawer.draw(PartModels.Drawer.key(MiningComputerBlockEntity.DISK_SLOT), disk, null, null, false, null,
                    null);
            poses.popPose();
        }
        cards(rig, drawer, poses);
    }

    /*
     * The board turned face up on the deck (a quarter turn about the rig's depth), and on its own seats the processor
     * and the memory; the rig has no cooler, cooled by the air it stands in.
     */
    private static void board(final MiningComputerBlockEntity rig, final PartModels.Drawer drawer,
                              final PoseStack poses, final Place place) {
        final PartModels.Part board = PartModels.part(rig.installedPart(MiningComputerBlockEntity.MOTHERBOARD_SLOT));
        if (board == null) {
            return;
        }
        poses.pushPose();
        at(poses, place.board());
        poses.mulPose(Axis.ZP.rotationDegrees(QUARTER));
        final KitInstance on = drawer.draw(PartModels.Drawer.key(MiningComputerBlockEntity.MOTHERBOARD_SLOT), board,
                null, null, false, null, null);
        if (on != null) {
            drawer.slot(MiningComputerBlockEntity.CPU_SLOT, rig.installedPart(MiningComputerBlockEntity.CPU_SLOT), on,
                    SEAT_CPU);
            for (int k = 0; k < MiningComputerBlockEntity.RAM_SLOTS; k++) {
                final int slot = MiningComputerBlockEntity.RAM_SLOTS_START + k;
                drawer.slot(slot, rig.installedPart(slot), on, RAM_SEATS[k]);
            }
        }
        poses.popPose();
    }

    /* Each card standing on its riser, its bracket to the front, its middle over the riser's. */
    private static void cards(final MiningComputerBlockEntity rig, final PartModels.Drawer drawer,
                              final PoseStack poses) {
        final RigGeometry geometry = RigGeometry.of(rig.rigEra());
        for (int k = 0; k < rig.riserCount(); k++) {
            final ResourceLocation item = rig.cardPart(k);
            final PartModels.Part card = PartModels.part(item);
            final float[] reach = PartModels.reach(item);
            if (card == null || reach == null || !rig.hasRiser(k)) {
                continue;
            }
            poses.pushPose();
            at(poses, new float[] {(float) geometry.riserX(k) - (reach[1] + reach[4]) / 2f,
                    (float) geometry.railY() + CARD_LIFT, (float) geometry.front() + CARD_BACK});
            poses.mulPose(Axis.ZP.rotationDegrees(QUARTER));
            poses.mulPose(Axis.XP.rotationDegrees(HALF));
            drawer.draw(PartModels.Drawer.key(MiningComputerBlockEntity.CARD_SLOTS_START + k), card, null, null,
                    false, null, null);
            poses.popPose();
        }
    }

    /* Moves the drawing to a point of the rig facing north, in its pixels from its origin block's corner. */
    private static void at(final PoseStack poses, final float[] point) {
        poses.translate((point[0] - MIDDLE) / PIXEL, point[1] / PIXEL, (point[2] - MIDDLE) / PIXEL);
    }

    /**
     * Where each era's rig holds its parts, in its pixels facing north: the board's own point (its processor's seat
     * turned face up lands on the socket of the deck), the supply's and the disk's, and whether the supply stands on
     * its side, as the Transition's narrow frame holds it.
     */
    private record Place(float[] board, float[] psu, boolean psuOnSide, float[] disk) {

        private static final Place TRANSITION = new Place(new float[] {14.0f, 8.4f, 4.25f},
                new float[] {14.2f, 9.0f, 14.5f}, true, new float[] {14.5f, 1.0f, 6.0f});
        private static final Place WIDE = new Place(new float[] {18.5f, 8.4f, 4.25f},
                new float[] {26.0f, 1.0f, 9.0f}, false, new float[] {30.0f, 1.0f, 14.0f});

        static Place of(@Nullable final Era era) {
            return era == null || era.isAtMost(HardwareEra.TRANSITION) ? TRANSITION : WIDE;
        }
    }
}
