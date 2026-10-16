/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.GeometryBaker;
import dev.jstech.core.model.MeshDeformer;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelLocator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.model.Rotations;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws a model of the Core's in the pose its thing was last worked out to: every bone's faces where the bone is,
 * painted with their textures, see-through or not as the model's definition says, glowing parts at full light;
 * meshes that bend or morph worked out for the frame; and what the model holds at its locators, drawn there. The
 * model's pixels are a sixteenth of a block, its origin where the pose stack stands.
 */
public final class KitRenderer {

    private static final float PIXEL = 1.0F / 16.0F;
    private static final int WHITE = 0xFFFFFFFF;

    private KitRenderer() {
    }

    /**
     * Draws {@code instance}'s model, as {@code variant} hides or shows its bones and paints it.
     *
     * @param held  what it holds at each locator, or none
     * @param level the world it is in, for what it holds; none in a screen
     */
    public static <T> void draw(final KitInstance instance, final ModelDefinition.Variant variant, final T thing,
                                final PoseStack poses, final MultiBufferSource buffers, final int light,
                                final int overlay, @Nullable final IHeldThings<T> held, @Nullable final Level level) {
        final KitModel model = instance.model();
        final Matrix4f[] world = instance.world();
        final boolean[] hidden = hidden(instance, variant);
        final ResourceLocation painted = variant.texture().isEmpty() ? null : ResourceLocation.parse(
                variant.texture());
        poses.pushPose();
        poses.scale(PIXEL, PIXEL, PIXEL);
        final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
        for (int i = 0; i < bones.size(); i++) {
            final List<BakedGeometry.Part> parts = bones.get(i);
            if (parts.isEmpty() || hidden[i]) {
                continue;
            }
            poses.pushPose();
            poses.mulPose(world[i]);
            for (final BakedGeometry.Part part : parts) {
                emit(buffers.getBuffer(type(model, part.texture(), painted)), poses.last(), part.vertices(),
                        part.vertices().length, part.glow() ? LightTexture.FULL_BRIGHT : light, overlay);
            }
            poses.popPose();
        }
        for (final MeshDeformer deformer : instance.deformers()) {
            final BakedGeometry.Deformed deformed = deformer.deformed();
            if (hidden[deformed.bone()]) {
                continue;
            }
            final float[] corners = deformer.deform(instance.pose(), world);
            final GeometryBaker.Corners laid = instance.corners();
            laid.clear();
            GeometryBaker.mesh(deformed.mesh(), corners, laid);
            poses.pushPose();
            if (deformed.skin() == null) {
                // A mesh that only morphs stays in its bone's space; one a skin bends is in the model's.
                poses.mulPose(world[deformed.bone()]);
            }
            emit(buffers.getBuffer(type(model, deformed.mesh().texture(), painted)), poses.last(), laid.data(),
                    laid.size(), deformed.glow() ? LightTexture.FULL_BRIGHT : light, overlay);
            poses.popPose();
        }
        if (held != null) {
            heldThings(instance, hidden, thing, held, poses, buffers, light, overlay, level);
        }
        poses.popPose();
    }

    /**
     * Draws one bone of {@code instance}'s model and every bone hanging from it, the bone's pivot where the pose stack
     * stands and its own turn left out: for a part of a model that follows something else, as armour follows the
     * limbs of whoever wears it.
     *
     * @param through what every face is drawn into besides its own texture, as the glint of an enchanted item; none
     *                for nothing more
     */
    public static void drawBranch(final KitInstance instance, final int bone, final PoseStack poses,
                                  final MultiBufferSource buffers, final int light, final int overlay,
                                  @Nullable final VertexConsumer through) {
        final KitModel model = instance.model();
        final Matrix4f[] world = instance.world();
        final ModelPose pose = instance.pose();
        final Matrix4f undo = instance.branchScratch().set(world[bone]).invert();
        final ModelBone root = model.data().bones().get(bone);
        poses.pushPose();
        poses.scale(PIXEL, PIXEL, PIXEL);
        poses.translate(-root.pivot().x(), -root.pivot().y(), -root.pivot().z());
        final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
        for (int i = 0; i < bones.size(); i++) {
            if (!under(pose, i, bone) || pose.hidden(i) || bones.get(i).isEmpty()) {
                continue;
            }
            poses.pushPose();
            if (i != bone) {
                // Where the bone hangs from the root, the root's own place and turn taken away.
                poses.mulPose(undo);
                poses.mulPose(world[i]);
            }
            for (final BakedGeometry.Part part : bones.get(i)) {
                final int lit = part.glow() ? LightTexture.FULL_BRIGHT : light;
                emit(buffers.getBuffer(type(model, part.texture(), null)), poses.last(), part.vertices(),
                        part.vertices().length, lit, overlay);
                if (through != null) {
                    emit(through, poses.last(), part.vertices(), part.vertices().length, lit, overlay);
                }
            }
            poses.popPose();
        }
        poses.popPose();
    }

    /** The way the faces painted with picture {@code texture} are drawn. */
    static RenderType type(final KitModel model, final int texture, @Nullable final ResourceLocation painted) {
        final ResourceLocation location = painted != null && texture == 0 ? painted : model.texture(texture);
        final ModelDefinition definition = model.definition();
        return definition.translucent() ? KitRenderTypes.translucent(location, definition.cracks())
                : KitRenderTypes.cutout(location, definition.cracks());
    }

    /*
     * Which bones are hidden this frame: as the pose has them, then as the variant hides or shows them, and every
     * bone hanging from a hidden one.
     */
    private static boolean[] hidden(final KitInstance instance, final ModelDefinition.Variant variant) {
        final ModelPose pose = instance.pose();
        final boolean[] out = instance.hiddenScratch();
        for (int i = 0; i < out.length; i++) {
            final String name = instance.model().data().bones().get(i).name();
            boolean hide = pose.hides(i);
            if (variant.hidden().contains(name)) {
                hide = true;
            } else if (variant.shown().contains(name)) {
                hide = false;
            }
            final int parent = pose.parent(i);
            out[i] = hide || parent >= 0 && out[parent];
        }
        return out;
    }

    /* Whether bone {@code bone} is {@code root} or hangs from it. */
    private static boolean under(final ModelPose pose, final int bone, final int root) {
        for (int at = bone; at >= 0; at = pose.parent(at)) {
            if (at == root) {
                return true;
            }
        }
        return false;
    }

    private static void emit(final VertexConsumer consumer, final PoseStack.Pose pose, final float[] vertices,
                             final int count, final int light, final int overlay) {
        for (int at = 0; at < count; at += BakedGeometry.STRIDE) {
            consumer.addVertex(pose, vertices[at], vertices[at + 1], vertices[at + 2])
                    .setColor(WHITE)
                    .setUv(vertices[at + 3], vertices[at + 4])
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, vertices[at + 5], vertices[at + 6], vertices[at + 7]);
        }
    }

    /* What the model holds at each locator, drawn there as an item in a frame is, the bone's turn with it. */
    private static <T> void heldThings(final KitInstance instance, final boolean[] hidden, final T thing,
                                       final IHeldThings<T> held, final PoseStack poses,
                                       final MultiBufferSource buffers, final int light, final int overlay,
                                       @Nullable final Level level) {
        final List<ModelBone> bones = instance.model().data().bones();
        for (int i = 0; i < bones.size(); i++) {
            if (bones.get(i).locators().isEmpty() || hidden[i]) {
                continue;
            }
            for (final ModelLocator locator : bones.get(i).locators()) {
                final ItemStack stack = held.at(thing, locator.name());
                if (stack.isEmpty()) {
                    continue;
                }
                poses.pushPose();
                poses.mulPose(instance.world()[i]);
                poses.translate(locator.offset().x(), locator.offset().y(), locator.offset().z());
                poses.mulPose(Rotations.ofEuler(locator.rotation()));
                // Back to blocks, which an item is drawn in.
                poses.scale(1 / PIXEL, 1 / PIXEL, 1 / PIXEL);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                        overlay, poses, buffers, level, i);
                poses.popPose();
            }
        }
    }
}
