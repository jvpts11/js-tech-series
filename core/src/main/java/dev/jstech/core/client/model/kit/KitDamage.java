/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.JsCore;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.multiblock.damage.DamageLook;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.damage.MeshDamage;
import dev.jstech.core.multiblock.damage.Strike;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Core's models drawn damaged: a model's faces with strikes laid on them ({@link MeshDamage}), cut finer round
 * each, pushed and cut as their stamps say, and the pixels of each strike's edge ({@link DamageLook}) drawn over the
 * faces round it, as pictures made for it. A model's faces far from every strike stay as they were. Worked out once
 * for each model and set of strikes, the most recently drawn kept with their pictures; a damaged model is drawn one by
 * one, never many at once, and always as itself, near or far.
 */
@PaletteHolder
public final class KitDamage {

    /** The colours the edges of strikes are painted in: torn metal, its dark speckles, soot, char, glow, scrapes. */
    public static final Palette<DamageLook.Colours> PALETTE = Palettes.declare(JsCore.MODID, "damage_edges",
            new DamageLook.Colours(0xFFC4C8CE, 0xFF5C6068, 0xFF463A32, 0xFF221A16, 0xFFFF8C28, 0xFF767A80));
    /* How many damaged models are kept, the most recently drawn. */
    private static final int KEPT = 32;
    private static final float PIXEL = 1.0F / 16.0F;
    /* How far out of its face a strike's pixels are drawn, in blocks, so they lie over it whatever is drawn first. */
    private static final float LIFT = 0.003F;
    /* A corner as the strikes are laid: its place in the model's space, in blocks, and its point on the texture. */
    private static final int LAID = 5;
    private static final int CORNERS = 4;
    private static final int AXES = 3;
    private static final int[] HALVES = {0, 1, 2, 0, 2, 3};
    private static final Map<Key, Damaged> DAMAGED = new LinkedHashMap<>(KEPT, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<Key, Damaged> eldest) {
            if (size() <= KEPT) {
                return false;
            }
            eldest.getValue().release();
            return true;
        }
    };
    private static final Set<KitModel> DRAWN = Collections.newSetFromMap(new IdentityHashMap<>());
    /* The strikes gathered for each block a formed model is drawn from, until a block's damage changes. */
    private static final Map<BlockPos, Gathered> GATHERED = new HashMap<>();
    private static final int MOST_GATHERED = 4096;
    private static int pictures;
    private static int gatheredAt = -1;

    private KitDamage() {
    }

    /**
     * The strikes laid on the formed model drawn from the block at {@code pos}, which is the block {@code place} of it:
     * gathered from its blocks the first time, and again only once a block's damage has changed.
     */
    public static List<Strike> strikes(final Level level, final BlockPos pos, final FormedPlace place) {
        if (gatheredAt != FormedDamage.changes() || GATHERED.size() > MOST_GATHERED) {
            GATHERED.clear();
            gatheredAt = FormedDamage.changes();
        }
        final Gathered known = GATHERED.get(pos);
        if (known != null && known.level() == level && known.place().equals(place)) {
            return known.strikes();
        }
        final List<Strike> strikes = FormedDamage.strikes(level, pos, place);
        GATHERED.put(pos.immutable(), new Gathered(level, place, strikes));
        return strikes;
    }

    /** {@code model} with {@code strikes} laid on it, worked out the first time it is drawn so; the model for none. */
    public static KitModel of(final KitModel model, final List<Strike> strikes) {
        if (strikes.isEmpty()) {
            return model;
        }
        final Key key = new Key(model, List.copyOf(strikes));
        final Damaged known = DAMAGED.get(key);
        if (known != null) {
            return known.model();
        }
        final Damaged made = make(model, key.strikes());
        DAMAGED.put(key, made);
        DRAWN.add(made.model());
        return made.model();
    }

    /* Whether {@code model} is a damaged one, drawn one by one. */
    static boolean damaged(final KitModel model) {
        return DRAWN.contains(model);
    }

    /* Lets every damaged model and its pictures go, as the models are read again. On the render thread. */
    static void forget() {
        DAMAGED.values().forEach(Damaged::release);
        DAMAGED.clear();
        DRAWN.clear();
        GATHERED.clear();
    }

    private static Damaged make(final KitModel model, final List<Strike> strikes) {
        final ModelPose pose = new ModelPose(model.data());
        final Matrix4f[] world = new Matrix4f[model.data().bones().size()];
        for (int i = 0; i < world.length; i++) {
            world[i] = new Matrix4f();
        }
        pose.world(world);
        final float size = PIXEL * model.scale();
        final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
        final List<int[]> parts = new ArrayList<>();
        final List<Floats> kept = new ArrayList<>();
        final Floats near = new Floats();
        final List<Integer> tags = new ArrayList<>();
        final Vector3f corner = new Vector3f();
        final float[] quad = new float[CORNERS * LAID];
        for (int bone = 0; bone < bones.size() && bone < world.length; bone++) {
            for (int p = 0; p < bones.get(bone).size(); p++) {
                final BakedGeometry.Part part = bones.get(bone).get(p);
                final int index = parts.size();
                parts.add(new int[] {bone, p});
                final Floats keep = new Floats();
                kept.add(keep);
                final float[] vertices = part.vertices();
                for (int face = 0; face < part.faces(); face++) {
                    for (int c = 0; c < CORNERS; c++) {
                        final int from = (face * CORNERS + c) * BakedGeometry.STRIDE;
                        world[bone].transformPosition(corner.set(vertices[from], vertices[from + 1],
                                vertices[from + 2])).mul(size);
                        quad[c * LAID] = corner.x;
                        quad[c * LAID + 1] = corner.y;
                        quad[c * LAID + 2] = corner.z;
                        quad[c * LAID + 3] = vertices[from + 3];
                        quad[c * LAID + 4] = vertices[from + 4];
                    }
                    if (!reaches(quad, strikes)) {
                        keep.add(vertices, face * CORNERS * BakedGeometry.STRIDE, CORNERS * BakedGeometry.STRIDE);
                        continue;
                    }
                    // A triangle is kept as a face whose last corner is twice: its second half is no face at all.
                    final boolean triangle = sameCorner(quad, 2, 3);
                    for (int h = 0; h < (triangle ? AXES : HALVES.length); h++) {
                        near.add(quad, HALVES[h] * LAID, LAID);
                    }
                    for (int h = 0; h < (triangle ? 1 : 2); h++) {
                        tags.add(index);
                    }
                }
            }
        }
        final MeshDamage.Damaged damaged = MeshDamage.apply(near.toArray(), LAID,
                tags.stream().mapToInt(Integer::intValue).toArray(), strikes);
        final Matrix4f[] inverse = new Matrix4f[world.length];
        for (int i = 0; i < world.length; i++) {
            inverse[i] = world[i].invert(new Matrix4f());
        }
        // The struck faces back into their bones' space, each part keeping its own.
        final float[][] normals = normals(damaged);
        for (int t = 0; t < damaged.triangles(); t++) {
            final int bone = parts.get(damaged.tags()[t])[0];
            emit(kept.get(damaged.tags()[t]), damaged.corners(), t, inverse[bone], size, normals[t], 0, null);
        }
        final List<ResourceLocation> textures = new ArrayList<>(model.textures());
        final List<ResourceLocation> made = new ArrayList<>();
        final List<List<BakedGeometry.Part>> out = new ArrayList<>();
        for (int bone = 0; bone < bones.size(); bone++) {
            out.add(new ArrayList<>());
        }
        for (int i = 0; i < parts.size(); i++) {
            final int[] at = parts.get(i);
            final BakedGeometry.Part part = bones.get(at[0]).get(at[1]);
            if (kept.get(i).size() > 0) {
                out.get(at[0]).add(new BakedGeometry.Part(part.texture(), part.glow(), kept.get(i).toArray()));
            }
        }
        // Each strike's edge, its pixels over the faces of the hull it struck, lifted a hair off them.
        for (int s = 0; s < Math.min(strikes.size(), Long.SIZE); s++) {
            final DamageLook look = new DamageLook(strikes.get(s), PALETTE.get());
            final ResourceLocation picture = picture(look.side(), look.pixels());
            made.add(picture);
            textures.add(picture);
            decal(out, parts, damaged, s, look, inverse, size, normals, textures.size() - 1, false);
            if (look.glows()) {
                final ResourceLocation glowing = picture(look.side(), look.glow());
                made.add(glowing);
                textures.add(glowing);
                decal(out, parts, damaged, s, look, inverse, size, normals, textures.size() - 1, true);
            }
        }
        final BakedGeometry geometry = new BakedGeometry(out, model.geometry().deformed());
        final KitModel drawn = new KitModel(model.id(), model.definition(), model.data(), geometry, model.animations(),
                textures, model.variants(), model.file(), model.bounds(), null);
        return new Damaged(drawn, made);
    }

    /* Lays the faces strike {@code s} struck, with its picture {@code texture}, into a part of each of their bones. */
    private static void decal(final List<List<BakedGeometry.Part>> out, final List<int[]> parts,
                              final MeshDamage.Damaged damaged, final int s, final DamageLook look,
                              final Matrix4f[] inverse, final float size, final float[][] normals, final int texture,
                              final boolean glow) {
        final Map<Integer, Floats> byBone = new LinkedHashMap<>();
        for (int t = 0; t < damaged.triangles(); t++) {
            if ((damaged.struck()[t] & 1L << s) == 0) {
                continue;
            }
            final int bone = parts.get(damaged.tags()[t])[0];
            emit(byBone.computeIfAbsent(bone, any -> new Floats()), damaged.corners(), t, inverse[bone], size,
                    normals[t], LIFT, uvs(damaged, t, look));
        }
        byBone.forEach((bone, faces) -> out.get(bone).add(new BakedGeometry.Part(texture, glow, faces.toArray())));
    }

    /* The points on a strike's picture of triangle {@code t}'s corners, by where they were before the strikes. */
    private static float[] uvs(final MeshDamage.Damaged damaged, final int t, final DamageLook look) {
        final float[] out = new float[AXES * 2];
        final double[] uv = new double[2];
        for (int c = 0; c < AXES; c++) {
            final int at = (t * AXES + c) * AXES;
            look.uv(damaged.rest()[at], damaged.rest()[at + 1], damaged.rest()[at + 2], uv);
            out[c * 2] = (float) uv[0];
            out[c * 2 + 1] = (float) uv[1];
        }
        return out;
    }

    /*
     * Adds triangle {@code t} to {@code faces} as a face of four corners, its last twice, in its bone's space: lifted
     * {@code lift} blocks out of itself, its texture's points {@code uvs} in place of its own when given.
     */
    private static void emit(final Floats faces, final float[] corners, final int t, final Matrix4f inverse,
                             final float size, final float[] normal, final float lift,
                             final float @Nullable [] uvs) {
        final Vector3f place = new Vector3f();
        final Vector3f way = inverse.transformDirection(new Vector3f(normal[0], normal[1], normal[2])).normalize();
        for (int c = 0; c < CORNERS; c++) {
            final int k = Math.min(c, 2);
            final int at = (t * AXES + k) * LAID;
            place.set(corners[at] + normal[0] * lift, corners[at + 1] + normal[1] * lift,
                    corners[at + 2] + normal[2] * lift).div(size);
            inverse.transformPosition(place);
            faces.add(place.x);
            faces.add(place.y);
            faces.add(place.z);
            faces.add(uvs == null ? corners[at + 3] : uvs[k * 2]);
            faces.add(uvs == null ? corners[at + 4] : uvs[k * 2 + 1]);
            faces.add(way.x);
            faces.add(way.y);
            faces.add(way.z);
        }
    }

    /* The way each triangle faces now, in the model's space; up for one of no area. */
    private static float[][] normals(final MeshDamage.Damaged damaged) {
        final float[][] out = new float[damaged.triangles()][];
        final float[] c = damaged.corners();
        for (int t = 0; t < damaged.triangles(); t++) {
            final int a = t * AXES * LAID;
            final Vector3f one = new Vector3f(c[a + LAID] - c[a], c[a + LAID + 1] - c[a + 1],
                    c[a + LAID + 2] - c[a + 2]);
            final Vector3f two = new Vector3f(c[a + 2 * LAID] - c[a], c[a + 2 * LAID + 1] - c[a + 1],
                    c[a + 2 * LAID + 2] - c[a + 2]);
            final Vector3f way = one.cross(two);
            out[t] = way.lengthSquared() > 0 ? new float[] {way.normalize().x, way.y, way.z} : new float[] {0, 1, 0};
        }
        return out;
    }

    /* Whether any strike reaches the face of four corners {@code quad}, either of its halves, anywhere across it. */
    private static boolean reaches(final float[] quad, final List<Strike> strikes) {
        final float[] second = new float[AXES * LAID];
        System.arraycopy(quad, 0, second, 0, LAID);
        System.arraycopy(quad, 2 * LAID, second, LAID, 2 * LAID);
        for (final Strike strike : strikes) {
            if (MeshDamage.reaches(strike, quad, LAID, 0) || MeshDamage.reaches(strike, second, LAID, 0)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameCorner(final float[] quad, final int first, final int second) {
        for (int n = 0; n < LAID; n++) {
            if (quad[first * LAID + n] != quad[second * LAID + n]) {
                return false;
            }
        }
        return true;
    }

    /* A picture of {@code argb} pixels, {@code side} to a side, made for a strike and kept by the game's textures. */
    private static ResourceLocation picture(final int side, final int[] argb) {
        final NativeImage image = new NativeImage(side, side, false);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(argb[y * side + x]));
            }
        }
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "damage/" + pictures++);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }

    /* A model and the strikes laid on it, the model told apart by which one it is. */
    private record Key(KitModel model, List<Strike> strikes) {

        Key {
            strikes = List.copyOf(strikes);
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Key key && key.model == this.model && key.strikes.equals(this.strikes);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this.model) * 31 + this.strikes.hashCode();
        }
    }

    /* The strikes gathered for a block a formed model is drawn from, in which world and as which block of it. */
    private record Gathered(Level level, FormedPlace place, List<Strike> strikes) {

        Gathered {
            strikes = List.copyOf(strikes);
        }
    }

    /* A damaged model and the pictures made for it, let go together. */
    private record Damaged(KitModel model, List<ResourceLocation> pictures) {

        Damaged {
            Objects.requireNonNull(model, "model");
            pictures = List.copyOf(pictures);
        }

        void release() {
            DRAWN.remove(this.model);
            this.pictures.forEach(Minecraft.getInstance().getTextureManager()::release);
        }
    }

    /* Numbers gathered one by one, in an array that grows, with no number boxed. */
    private static final class Floats {

        private float[] values = new float[64];
        private int size;

        void add(final float value) {
            if (this.size == this.values.length) {
                this.values = Arrays.copyOf(this.values, this.size * 2);
            }
            this.values[this.size++] = value;
        }

        void add(final float[] from, final int start, final int count) {
            if (this.size + count > this.values.length) {
                this.values = Arrays.copyOf(this.values, Math.max(this.values.length * 2, this.size + count));
            }
            System.arraycopy(from, start, this.values, this.size, count);
            this.size += count;
        }

        int size() {
            return this.size;
        }

        float[] toArray() {
            return Arrays.copyOf(this.values, this.size);
        }
    }
}
