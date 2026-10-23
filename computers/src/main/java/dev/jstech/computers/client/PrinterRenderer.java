/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws a printer as its era's machine. It says what it is doing with its lamps and its page: the power lamp lit while
 * a computer is on the other end of its cable, the busy lamp blinking while a page comes out and held lit while a job
 * waits (for paper, for room in the output, or paused), and the sheet sliding out only while it prints.
 */
public final class PrinterRenderer implements IKitDriver<PrinterBlockEntity> {

    /** The page sliding out of the printer, looping while it prints. */
    private static final String PAGE = "printer";

    private PrinterRenderer() {
    }

    /** The renderer of the printers. */
    public static BlockEntityRenderer<PrinterBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        return KitBlockEntityRenderer.<PrinterBlockEntity>look(ComputingLooks.PRINTER)
                .driver(new PrinterRenderer())
                .build();
    }

    @Override
    public void drive(final PrinterBlockEntity printer, final ModelAnimator animator, final double seconds) {
        DeviceClips.play(animator, PAGE, printer.printing() ? PrinterBlockEntity.clip(printer.model()) : null,
                seconds);
    }

    @Override
    public void move(final PrinterBlockEntity printer, final ModelPose pose, final double seconds) {
        DeviceLamps.show(pose, ComputingLooks.PRINTER_POWER_LAMP, printer.ownerPos() != null);
        DeviceLamps.show(pose, ComputingLooks.PRINTER_BUSY_LAMP, printer.printing()
                ? DeviceLamps.blinkLit(printer.getLevel()) : printer.waiting() != PrinterBlockEntity.Wait.NONE);
        DeviceLamps.show(pose, ComputingLooks.PRINTER_PAPER, printer.printing());
    }

    @Override
    public boolean movesBones() {
        return true;
    }
}
