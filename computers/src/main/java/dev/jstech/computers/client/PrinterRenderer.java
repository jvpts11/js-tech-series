/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.core.client.geo.LookGeoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws a printer as its era's machine. It says what it is doing with its lamps and its page: the power lamp lit while
 * a computer is on the other end of its cable, the busy lamp blinking while a page comes out and held lit while a job
 * waits (for paper, for room in the output, or paused), and the sheet sliding out only while it prints.
 */
public final class PrinterRenderer extends GeoBlockRenderer<PrinterBlockEntity> {

    public PrinterRenderer(final BlockEntityRendererProvider.Context context) {
        super(new LookGeoModel<>(ComputingLooks.PRINTER));
    }

    @Override
    public void preRender(final PoseStack poseStack, final PrinterBlockEntity printer, final BakedGeoModel model,
                          final MultiBufferSource bufferSource, final VertexConsumer buffer, final boolean isReRender,
                          final float partialTick, final int packedLight, final int packedOverlay, final int colour) {
        super.preRender(poseStack, printer, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        DeviceLamps.show(model, ComputingLooks.PRINTER_POWER_LAMP, printer.ownerPos() != null);
        DeviceLamps.show(model, ComputingLooks.PRINTER_BUSY_LAMP, printer.printing()
                ? DeviceLamps.blinkLit(printer.getLevel()) : printer.waiting() != PrinterBlockEntity.Wait.NONE);
        DeviceLamps.show(model, ComputingLooks.PRINTER_PAPER, printer.printing());
    }
}
