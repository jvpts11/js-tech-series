/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.ComputingModule;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Every USB stick is drawn whole in three dimensions in a port, in the hand and on the ground, a body and a plug; in
 * a slot, its flat icon. Drawn flat in a port, a stick stood out of its computer as a cut-out picture.
 */
public final class UsbStickClientTests {

    /* How deep a stick drawn in three dimensions is at least, and a flat icon at most, in blocks. */
    private static final float SOLID = 2.5F / 16;
    private static final float FLAT = 1.5F / 16;
    /* A quad's vertex: its x, y and z first, of eight ints. */
    private static final int STRIDE = 8;
    private static final int Z = 2;

    private UsbStickClientTests() {
    }

    @ClientTest(timeoutTicks = 200)
    public static void sticks_areDrawnInThreeDimensionsInAPortAndFlatInASlot(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            for (final ItemEntry<?> stick : List.of(ComputingModule.USB_FLASH_DRIVE_8G,
                    ComputingModule.USB_FLASH_DRIVE_16G, ComputingModule.USB_FLASH_DRIVE,
                    ComputingModule.USB_FLASH_DRIVE_64G)) {
                final ItemStack stack = new ItemStack(stick.get());
                final BakedModel model = ctx.mc().getItemRenderer().getModel(stack, ctx.mc().level, null, 0);
                final float port = depth(model.applyTransform(ItemDisplayContext.NONE, new PoseStack(), false));
                final float hand = depth(model.applyTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        new PoseStack(), false));
                final float slot = depth(model.applyTransform(ItemDisplayContext.GUI, new PoseStack(), false));
                ctx.assertTrue(port >= SOLID && hand >= SOLID, stick.getId() + " is drawn in three dimensions in a"
                        + " port and in the hand: " + port * 16 + " and " + hand * 16 + " sixteenths deep");
                ctx.assertTrue(slot <= FLAT, stick.getId() + " shows its flat icon in a slot: " + slot * 16
                        + " sixteenths deep");
            }
        });
    }

    /* How deep the quads of {@code model} reach along z, from the nearest to the farthest, in blocks. */
    private static float depth(final BakedModel model) {
        final List<BakedQuad> quads = new ArrayList<>(model.getQuads(null, null, RandomSource.create()));
        for (final Direction side : Direction.values()) {
            quads.addAll(model.getQuads(null, side, RandomSource.create()));
        }
        float near = Float.MAX_VALUE;
        float far = -Float.MAX_VALUE;
        for (final BakedQuad quad : quads) {
            final int[] vertices = quad.getVertices();
            for (int at = 0; at + STRIDE <= vertices.length; at += STRIDE) {
                final float z = Float.intBitsToFloat(vertices[at + Z]);
                near = Math.min(near, z);
                far = Math.max(far, z);
            }
        }
        return quads.isEmpty() ? 0 : far - near;
    }
}
