/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.printer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.computers.item.PrintedPaperItem;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrinterModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;
import org.joml.Matrix4f;

/**
 * A printed picture in an item frame shows on the frame, as a map does: the picture as its printer put it on paper,
 * on a square of that paper filling the frame's face, turned in quarter turns with the frame. A sheet of text stays an
 * item in its frame.
 */
public final class PrintedPaperFrames {

    /** How much of the frame's face the sheet covers, as the frame's inner board is twelve pixels of sixteen. */
    private static final float FACE = 12F / 16F;
    /** The side of the sheet in the units it is drawn in, as a map's 128. */
    private static final float SIDE = 128F;

    private PrintedPaperFrames() {
    }

    /*
     * The frame has already turned the item by its rotation in eighths of a turn; a picture turns as a map does, by
     * quarters, so the eighths are undone and the quarters put back before it is laid flat on the face.
     */
    public static void onRenderItemInFrame(final RenderItemInFrameEvent event) {
        if (!(event.getItemStack().getItem() instanceof PrintedPaperItem)) {
            return;
        }
        final PrintedDocument document = PrintedPaperItem.document(event.getItemStack());
        if (!document.isPicture()) {
            return;
        }
        final PrintedPaperScreen.SheetColours colours = PrintedPaperScreen.colours();
        final PrinterModel model = document.printerModel();
        final boolean fanfold = model != null && model.sheet() == PrinterModel.Sheet.FANFOLD;
        final PrintedPictureTextures.Entry framed = PrintedPictureTextures.framed(document,
                fanfold ? colours.fanfold() : colours.paper(), colours.bar());
        if (framed == null) {
            return;
        }
        event.setCanceled(true);
        final ItemFrame frame = event.getItemFrameEntity();
        final PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-frame.getRotation() * 45F));
        pose.mulPose(Axis.ZP.rotationDegrees((frame.getRotation() % 4) * 90F + 180F));
        final float unit = FACE / SIDE;
        pose.scale(unit, unit, unit);
        pose.translate(-SIDE / 2F, -SIDE / 2F, -1F);
        final int light = frame.getType() == EntityType.GLOW_ITEM_FRAME ? LightTexture.FULL_BRIGHT
                : event.getPackedLight();
        final VertexConsumer vertices = event.getMultiBufferSource().getBuffer(RenderType.text(framed.texture()));
        final Matrix4f matrix = pose.last().pose();
        vertices.addVertex(matrix, 0F, SIDE, -0.01F).setColor(-1).setUv(0F, 1F).setLight(light);
        vertices.addVertex(matrix, SIDE, SIDE, -0.01F).setColor(-1).setUv(1F, 1F).setLight(light);
        vertices.addVertex(matrix, SIDE, 0F, -0.01F).setColor(-1).setUv(1F, 0F).setLight(light);
        vertices.addVertex(matrix, 0F, 0F, -0.01F).setColor(-1).setUv(0F, 0F).setLight(light);
        pose.popPose();
    }
}
