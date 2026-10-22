/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelMesh;
import dev.jstech.core.model.ModelPose;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A model drawn with the Core's models, as the player's game holds it once its files are read: what it is, how its
 * definition says to draw it, its shape worked out, the animations it can play, the textures each of its pictures is
 * on, the models of its variants that are drawn from files of their own, the box it fills at rest, and the model of
 * fewer triangles it is drawn as far away, the same bones with its meshes brought down.
 *
 * @param id         its name
 * @param definition how it is drawn
 * @param data       the model as its file has it, brought to its definition's detail
 * @param geometry   its shape, worked out
 * @param animations the animations it can play, its file's and its animation files'
 * @param textures   the texture of each of its pictures, by their place in the model
 * @param variants   the models of its variants drawn from files of their own, by those files
 * @param file       the file it was read from
 * @param bounds     the box it fills at rest, in blocks, about its origin, at the size it is drawn
 * @param far        the model it is drawn as from its definition's distance away, or none for the same everywhere
 */
public record KitModel(ResourceLocation id, ModelDefinition definition, ModelData data, BakedGeometry geometry,
                       List<ModelAnimation> animations, List<ResourceLocation> textures,
                       Map<String, KitModel> variants, ResourceLocation file, AABB bounds, @Nullable KitModel far) {

    private static final float PIXEL = 1.0F / 16.0F;

    public KitModel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(geometry, "geometry");
        animations = List.copyOf(animations);
        textures = List.copyOf(textures);
        variants = Map.copyOf(variants);
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(bounds, "bounds");
    }

    /** The texture of picture {@code index}, or the first for one it has none of. */
    public ResourceLocation texture(final int index) {
        return index < this.textures.size() ? this.textures.get(index) : this.textures.getFirst();
    }

    /** How many times bigger than its file says it is drawn. */
    public float scale() {
        return (float) this.definition.detail().scale();
    }

    /** The model it is drawn as {@code blocks} away: the one of fewer triangles past its distance, or this one. */
    public KitModel at(final double blocks) {
        return this.far != null && blocks > this.definition.detail().farFrom() ? this.far : this;
    }

    /** The model a variant is drawn as: its own file's, or this one. */
    public KitModel drawnAs(final ModelDefinition.Variant variant) {
        if (variant.model().isEmpty()) {
            return this;
        }
        return this.variants.getOrDefault(variant.model(), this);
    }

    /**
     * The box a model of that shape fills at rest, in blocks, {@code scale} times its file's size; a block's own for
     * one of no faces.
     */
    static AABB restBounds(final ModelData data, final BakedGeometry geometry, final float scale) {
        final ModelPose pose = new ModelPose(data);
        final Matrix4f[] world = new Matrix4f[data.bones().size()];
        for (int i = 0; i < world.length; i++) {
            world[i] = new Matrix4f();
        }
        pose.world(world);
        final Vector3f low = new Vector3f(Float.MAX_VALUE);
        final Vector3f high = new Vector3f(-Float.MAX_VALUE);
        final Vector3f corner = new Vector3f();
        for (int bone = 0; bone < world.length; bone++) {
            for (final BakedGeometry.Part part : geometry.partsOf(bone)) {
                final float[] vertices = part.vertices();
                for (int at = 0; at < vertices.length; at += BakedGeometry.STRIDE) {
                    world[bone].transformPosition(corner.set(vertices[at], vertices[at + 1], vertices[at + 2]));
                    low.min(corner);
                    high.max(corner);
                }
            }
        }
        for (final BakedGeometry.Deformed deformed : geometry.deformed()) {
            final ModelMesh mesh = deformed.mesh();
            final Matrix4f place = deformed.skin() == null ? world[deformed.bone()] : new Matrix4f();
            for (int at = 0; at + 2 < mesh.positions().length; at += 3) {
                place.transformPosition(corner.set(mesh.positions()[at], mesh.positions()[at + 1],
                        mesh.positions()[at + 2]));
                low.min(corner);
                high.max(corner);
            }
        }
        if (low.x > high.x) {
            return new AABB(-0.5, 0, -0.5, 0.5, 1, 0.5);
        }
        final float size = PIXEL * scale;
        return new AABB(low.x * size, low.y * size, low.z * size, high.x * size, high.y * size, high.z * size);
    }
}
