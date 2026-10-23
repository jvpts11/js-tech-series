/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a Mainframe as one cabinet from its controller block, the model facing north with the controller in the
 * middle of its bottom front row, so each facing turns it about that block onto the multiblock's own footprint. Every
 * processor, memory stick, graphics card, disk and the power supply is its own bone, shown only when that slot
 * actually holds the part, so what a player installs is what a player sees through the open service bay.
 *
 * <p>The machine states itself in hardware, never on a screen: three annunciator lamps for running,
 * network and fault. A Mainframe is read on a linked monitor like every other computer in the mod.
 */
public final class MainframeRenderer implements IKitDriver<MainframeBlockEntity> {

    /** The roof fans and the tape reels, which turn while the machine is up. */
    private static final String WORK = "work";
    private static final String WORK_CLIP = "animation.mainframe.work";
    private static final String[] CPUS = DeviceLamps.numbered("cpu_", MainframeBlockEntity.CPU_SLOTS);
    private static final String[] RAMS = DeviceLamps.numbered("ram_", MainframeBlockEntity.RAM_SLOTS);
    private static final String[] GPUS = DeviceLamps.numbered("gpu_", MainframeBlockEntity.GPU_SLOTS);
    private static final String[] DISKS = DeviceLamps.numbered("disk_", MainframeBlockEntity.DISK_SLOTS);
    private static final String[] DISK_LAMPS = DeviceLamps.numbered("disk_lamp_", MainframeBlockEntity.DISK_SLOTS);

    private MainframeRenderer() {
    }

    /** The renderer of the Mainframes; the cabinet spans twelve blocks, so it is drawn within its whole box. */
    public static BlockEntityRenderer<MainframeBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<MainframeBlockEntity>look(ComputingLooks.MAINFRAME)
                .driver(new MainframeRenderer())
                .box(MainframeBlockEntity::renderBox)
                .build();
    }

    /* Every other visual (the installed hardware, the lamps, the panel) is a bone shown or hidden, not a clip. */
    @Override
    public void drive(final MainframeBlockEntity mainframe, final ModelAnimator animator, final double seconds) {
        DeviceClips.play(animator, WORK, mainframe.visualRunning() ? WORK_CLIP : null, seconds);
    }

    @Override
    public void move(final MainframeBlockEntity mainframe, final ModelPose pose, final double seconds) {
        for (int i = 0; i < MainframeBlockEntity.CPU_SLOTS; i++) {
            DeviceLamps.show(pose, CPUS[i], mainframe.hardwareInstalled(MainframeBlockEntity.CPU_SLOTS_START + i));
        }
        for (int i = 0; i < MainframeBlockEntity.RAM_SLOTS; i++) {
            DeviceLamps.show(pose, RAMS[i], mainframe.hardwareInstalled(MainframeBlockEntity.RAM_SLOTS_START + i));
        }
        for (int i = 0; i < MainframeBlockEntity.GPU_SLOTS; i++) {
            DeviceLamps.show(pose, GPUS[i], mainframe.hardwareInstalled(MainframeBlockEntity.GPU_SLOTS_START + i));
        }
        for (int i = 0; i < MainframeBlockEntity.DISK_SLOTS; i++) {
            final boolean seated = mainframe.hardwareInstalled(MainframeBlockEntity.DISK_SLOTS_START + i);
            DeviceLamps.show(pose, DISKS[i], seated);
            DeviceLamps.show(pose, DISK_LAMPS[i], seated && mainframe.diskCarriesSystem(i));
        }
        DeviceLamps.show(pose, "psu", mainframe.hardwareInstalled(MainframeBlockEntity.PSU_SLOT));

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
}
