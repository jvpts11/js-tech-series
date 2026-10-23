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
import dev.jstech.core.client.light.KitGlows;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.GeometryBaker;
import dev.jstech.core.model.MeshDeformer;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelLocator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.model.Rotations;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a model of the Core's in the pose its thing was last worked out to: every bone's faces where the bone is,
 * painted with their textures, see-through or not as the model's definition says, glowing parts at full light;
 * meshes that bend or morph worked out for the frame; and what the model holds at its locators, drawn there. The
 * model's pixels are a sixteenth of a block, its origin where the pose stack stands.
 *
 * <p>A texture that is animated shows the frame of the tick ({@link KitTextures}); one with a glowing texture beside
 * it has that drawn over it at full light; and the layers laid over the model ({@link KitLayers}) draw after it.
 * Drawn in the world, it is drawn with every other thing of its model at once, where it can be
 * ({@link KitInstancing}).
 */
public final class KitRenderer {

    private static final float PIXEL = 1.0F / 16.0F;
    private static final int WHITE = 0xFFFFFFFF;
    /* Where a model drawn in the world stands from the camera, worked out on the render thread. */
    private static final Vector3f SEEN = new Vector3f();

    private KitRenderer() {
    }

    /**
     * Draws {@code instance}'s model, as {@code variant} hides or shows its bones and paints it.
     *
     * @param held  what it holds at each locator, or none
     * @param level the world it is in, for what it holds; none in a screen
     * @param world whether it is drawn in the world, the pose stack standing where the camera is, so its glowing
     *              parts light the world around them as its definition says; not in a hand or a screen
     */
    public static <T> void draw(final KitInstance instance, final ModelDefinition.Variant variant, final T thing,
                                final PoseStack poses, final MultiBufferSource buffers, final int light,
                                final int overlay, @Nullable final IHeldThings<T> held, @Nullable final Level level,
                                final boolean world) {
        // In the world, a model far enough away is drawn as its model of fewer triangles, with the same bones.
        final KitModel model = world ? instance.model().at(poses.last().pose().getTranslation(SEEN).length())
                : instance.model();
        draw(instance, model, variant, thing, poses, buffers, light, overlay, held, level, world);
    }

    /*
     * Draws {@code instance} as {@code model}: its own model, or the one of fewer triangles it is drawn as far away,
     * which has the same bones, so the instance's pose fits it.
     */
    static <T> void draw(final KitInstance instance, final KitModel model, final ModelDefinition.Variant variant,
                         final T thing, final PoseStack poses, final MultiBufferSource buffers, final int light,
                         final int overlay, @Nullable final IHeldThings<T> held, @Nullable final Level level,
                         final boolean world) {
        final boolean[] hidden = hidden(instance, variant);
        final ResourceLocation painted = variant.texture().isEmpty() ? null : ResourceLocation.parse(
                variant.texture());
        final ModelDefinition definition = model.definition();
        final Level glowsIn = world && level != null && definition.glowLight() > 0 ? level : null;
        final long tick = KitClock.tick();
        final float size = PIXEL * model.scale();
        poses.pushPose();
        poses.scale(size, size, size);
        if (!world || !KitInstancing.take(instance, model, hidden, painted, poses, buffers, light, overlay,
                glowsIn)) {
            lay(instance, model, hidden, painted, poses, buffers, null, light, overlay, tick, glowsIn);
            if (glowsAnywhere(model, painted)) {
                lay(instance, model, hidden, painted, poses, buffers, KitRenderer::glowingType,
                        LightTexture.FULL_BRIGHT, overlay, tick, null);
            }
            final List<IKitLayer> layers = KitLayers.of(model.id());
            if (!layers.isEmpty()) {
                final IKitFaces faces = (type, lit, over) -> lay(instance, model, hidden, painted, poses, buffers,
                        type, lit, over, tick, null);
                for (final IKitLayer layer : layers) {
                    layer.draw(instance, faces, buffers, light, overlay);
                }
            }
        }
        if (held != null) {
            heldThings(instance, hidden, thing, held, poses, buffers, light, overlay, level, size);
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
        final long tick = KitClock.tick();
        final float size = PIXEL * model.scale();
        poses.pushPose();
        poses.scale(size, size, size);
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
                final ResourceLocation location = model.texture(part.texture());
                final TextureFrames frames = KitTextures.of(location).frames();
                emit(buffers.getBuffer(own(model, location)), poses.last(), part.vertices(), part.vertices().length,
                        lit, overlay, frames, tick);
                if (through != null) {
                    emit(through, poses.last(), part.vertices(), part.vertices().length, lit, overlay, frames,
                            tick);
                }
            }
            poses.popPose();
        }
        poses.popPose();
    }

    /**
     * Moves the pose stack, standing where {@code instance}'s model stands, in blocks, onto the pivot of its bone
     * {@code bone}, turned as the bone is this frame: where a thing held on that bone is drawn, riding with it. Nothing
     * is moved when the model has no such bone, or it is hidden this frame.
     *
     * @return whether it moved, so there is something to draw there
     */
    public static boolean toBone(final KitInstance instance, final String bone, final PoseStack poses) {
        final int index = instance.pose().index(bone);
        if (index < 0 || instance.pose().hidden(index)) {
            return false;
        }
        final float size = PIXEL * instance.model().scale();
        final ModelBone found = instance.model().data().bones().get(index);
        poses.scale(size, size, size);
        poses.mulPose(instance.world()[index]);
        poses.translate(found.pivot().x(), found.pivot().y(), found.pivot().z());
        poses.scale(1 / size, 1 / size, 1 / size);
        return true;
    }

    /** The way the faces painted with picture {@code texture} are drawn. */
    static RenderType type(final KitModel model, final int texture, @Nullable final ResourceLocation painted) {
        return own(model, location(model, texture, painted));
    }

    /*
     * Lays every face shown this frame: the bones' faces where the bones are, then the meshes that bend or morph,
     * each into its own way ({@code type} none) or into the way {@code type} gives its texture, skipping a face it
     * gives none for; telling the world of the glowing parts in {@code glowsIn}, when there is one.
     */
    private static void lay(final KitInstance instance, final KitModel model, final boolean[] hidden,
                            @Nullable final ResourceLocation painted, final PoseStack poses,
                            final MultiBufferSource buffers,
                            @Nullable final Function<ResourceLocation, @Nullable RenderType> type, final int light,
                            final int overlay, final long tick, @Nullable final Level glowsIn) {
        final Matrix4f[] matrices = instance.world();
        final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
        for (int i = 0; i < bones.size(); i++) {
            final List<BakedGeometry.Part> parts = bones.get(i);
            if (parts.isEmpty() || hidden[i]) {
                continue;
            }
            poses.pushPose();
            poses.mulPose(matrices[i]);
            for (final BakedGeometry.Part part : parts) {
                final ResourceLocation location = location(model, part.texture(), painted);
                final RenderType into = type == null ? own(model, location) : type.apply(location);
                if (into != null) {
                    emit(buffers.getBuffer(into), poses.last(), part.vertices(), part.vertices().length,
                            part.glow() ? LightTexture.FULL_BRIGHT : light, overlay,
                            KitTextures.of(location).frames(), tick);
                }
                if (glowsIn != null && part.glow()) {
                    KitGlows.report(glowsIn, middle(poses.last(), part.vertices()), model.definition().glowLight(),
                            model.definition().glowColour());
                }
            }
            poses.popPose();
        }
        for (final MeshDeformer deformer : instance.deformers()) {
            final BakedGeometry.Deformed deformed = deformer.deformed();
            if (hidden[deformed.bone()]) {
                continue;
            }
            final ResourceLocation location = location(model, deformed.mesh().texture(), painted);
            final RenderType into = type == null ? own(model, location) : type.apply(location);
            if (into == null) {
                continue;
            }
            final float[] corners = deformer.deform(instance.pose(), matrices);
            final GeometryBaker.Corners laid = instance.corners();
            laid.clear();
            GeometryBaker.mesh(deformed.mesh(), corners, laid);
            poses.pushPose();
            if (deformed.skin() == null) {
                // A mesh that only morphs stays in its bone's space; one a skin bends is in the model's.
                poses.mulPose(matrices[deformed.bone()]);
            }
            emit(buffers.getBuffer(into), poses.last(), laid.data(), laid.size(),
                    deformed.glow() ? LightTexture.FULL_BRIGHT : light, overlay, KitTextures.of(location).frames(),
                    tick);
            poses.popPose();
        }
    }

    /** The texture of picture {@code texture}: the variant's paint for the first, or the model's own. */
    static ResourceLocation location(final KitModel model, final int texture,
                                     @Nullable final ResourceLocation painted) {
        return painted != null && texture == 0 ? painted : model.texture(texture);
    }

    /* The way a model draws faces painted with {@code location}, see-through or not as its definition says. */
    private static RenderType own(final KitModel model, final ResourceLocation location) {
        final ModelDefinition definition = model.definition();
        return definition.translucent() ? KitRenderTypes.translucent(location, definition.cracks())
                : KitRenderTypes.cutout(location, definition.cracks());
    }

    /* The way a texture's glowing texture is drawn over it, at full light; none for one that has none. */
    private static @Nullable RenderType glowingType(final ResourceLocation location) {
        final ResourceLocation glowing = KitTextures.of(location).glowing();
        return glowing == null ? null : RenderType.entityTranslucentEmissive(glowing);
    }

    /* Whether any texture the model is painted with has a glowing texture beside it. */
    private static boolean glowsAnywhere(final KitModel model, @Nullable final ResourceLocation painted) {
        if (painted != null && KitTextures.of(painted).glowing() != null) {
            return true;
        }
        for (final ResourceLocation texture : model.textures()) {
            if (KitTextures.of(texture).glowing() != null) {
                return true;
            }
        }
        return false;
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

    /* Where the middle of a part's faces is in the world, from the pose that draws it, which stands at the camera. */
    private static Vec3 middle(final PoseStack.Pose pose, final float[] vertices) {
        float x = 0;
        float y = 0;
        float z = 0;
        final int corners = vertices.length / BakedGeometry.STRIDE;
        for (int at = 0; at < vertices.length; at += BakedGeometry.STRIDE) {
            x += vertices[at];
            y += vertices[at + 1];
            z += vertices[at + 2];
        }
        final Vector3f seen = pose.pose().transformPosition(x / corners, y / corners, z / corners, new Vector3f());
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().add(seen.x(), seen.y(), seen.z());
    }

    /* Emits the faces, their V moved onto the tick's frame of an animated texture. */
    private static void emit(final VertexConsumer consumer, final PoseStack.Pose pose, final float[] vertices,
                             final int count, final int light, final int overlay, final TextureFrames frames,
                             final long tick) {
        final float scale = frames.vScale();
        final float offset = frames.vOffset(tick);
        for (int at = 0; at < count; at += BakedGeometry.STRIDE) {
            consumer.addVertex(pose, vertices[at], vertices[at + 1], vertices[at + 2])
                    .setColor(WHITE)
                    .setUv(vertices[at + 3], vertices[at + 4] * scale + offset)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(pose, vertices[at + 5], vertices[at + 6], vertices[at + 7]);
        }
    }

    /* What the model holds at each locator, drawn there as an item in a frame is, the bone's turn with it. */
    private static <T> void heldThings(final KitInstance instance, final boolean[] hidden, final T thing,
                                       final IHeldThings<T> held, final PoseStack poses,
                                       final MultiBufferSource buffers, final int light, final int overlay,
                                       @Nullable final Level level, final float size) {
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
                // Back to blocks, which an item is drawn in, at its own size whatever the model's.
                poses.scale(1 / size, 1 / size, 1 / size);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                        overlay, poses, buffers, level, i);
                poses.popPose();
            }
        }
    }
}
