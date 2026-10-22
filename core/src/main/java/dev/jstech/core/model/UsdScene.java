/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * A USD stage made a model of the Core's: every prim that places things (Xforms, meshes, scopes) a bone, placed by its
 * transform operations in the stage's unit and with its up axis; every mesh's polygons laid as triangles with their
 * normals, their pictures' places and their colours as each is spread (over the mesh, by face, by point or by corner);
 * a mesh's look from the material it is bound to (UsdPreviewSurface's diffuse colour, or the texture that feeds it),
 * or from its display colour; a skeleton's joints bones of their own, at rest, with the meshes it bends skinned to
 * them; and what moves over time, transforms and skeletal animations, one animation. Pure.
 */
final class UsdScene {

    private final String name;
    private final UsdPrim root;
    private final Function<String, byte[]> files;
    private final double pixels;
    private final boolean zUp;
    private final double start;
    private final double perSecond;
    private final double end;
    private final List<ModelBone> bones = new ArrayList<>();
    private final Set<String> boneNames = new HashSet<>();
    private final Map<String, ModelSkin> skins = new LinkedHashMap<>();
    private final List<ModelTexture> textures = new ArrayList<>();
    private final Map<String, Integer> textureByKey = new HashMap<>();
    private final Map<String, ModelAnimation.Tracks> tracks = new LinkedHashMap<>();
    private final Map<String, Skeleton> skeletons = new HashMap<>();
    private final Map<String, PendingSkin> pendingSkins = new LinkedHashMap<>();
    private final Matrix4d upright;
    private final Matrix4d backward;

    private static final double DEFAULT_METRES = 0.01;
    private static final double DEFAULT_RATE = 24.0;
    private static final int MOST_INFLUENCES = 4;
    private static final Set<String> NOT_PLACED = Set.of("Material", "Shader", "NodeGraph", "GeomSubset",
            "SkelAnimation", "Camera", "DistantLight", "DomeLight", "SphereLight", "RectLight", "DiskLight",
            "CylinderLight", "SpatialAudio", "BlendShape");
    private static final List<String> ORDERS = List.of("XYZ", "XZY", "YXZ", "YZX", "ZXY", "ZYX");

    UsdScene(final String name, final UsdPrim root, final Function<String, byte[]> files) {
        this.name = name;
        this.root = root;
        this.files = files;
        final double metres = root.metadata.get("metersPerUnit") instanceof Double value && value > 0 ? value
                : DEFAULT_METRES;
        this.pixels = metres * CadLook.PIXELS_A_METRE;
        this.zUp = "Z".equals(root.metadata.get("upAxis"));
        this.start = root.metadata.get("startTimeCode") instanceof Double value ? value : 0;
        this.end = root.metadata.get("endTimeCode") instanceof Double value ? value : this.start;
        final Object rate = root.metadata.containsKey("timeCodesPerSecond") ? root.metadata.get("timeCodesPerSecond")
                : root.metadata.get("framesPerSecond");
        this.perSecond = rate instanceof Double value && value > 0 ? value : DEFAULT_RATE;
        this.upright = this.zUp ? new Matrix4d().rotateX(-Math.PI / 2) : new Matrix4d();
        this.backward = new Matrix4d(this.upright).invert();
    }

    /** The model the stage is. */
    ModelData model() {
        for (final UsdPrim child : this.root.children) {
            walk(child, null);
        }
        // A skin names its skeleton's joints by their bones, which the walk has made by now.
        this.pendingSkins.forEach((bone, pending) -> {
            final Skeleton skeleton = this.skeletons.get(pending.skeleton());
            if (skeleton != null && skeleton.names().size() * 16 == pending.inverseBinds().length) {
                this.skins.put(bone, new ModelSkin(skeleton.names(), pending.inverseBinds()));
            }
        });
        if (this.bones.isEmpty()) {
            throw new ModelFormatException("the USD file " + this.name + " places nothing");
        }
        final List<ModelAnimation> animations = new ArrayList<>();
        if (!this.tracks.isEmpty()) {
            animations.add(new ModelAnimation("animation", Math.max(0, (this.end - this.start) / this.perSecond),
                    ModelAnimation.Loop.LOOP, this.tracks, Map.of(), List.of()));
        }
        return new ModelData(this.name, 1, 1, this.bones, this.skins, this.textures, animations);
    }

    /* ---------------------------------------------------------------- prims as bones */

    private void walk(final UsdPrim prim, @Nullable final String parent) {
        if (!prim.specifier.equals("def") || NOT_PLACED.contains(prim.type)) {
            return;
        }
        final String bone = unique(prim.name);
        final Matrix4d rest = local(prim, Double.NaN);
        final List<ModelMesh> meshes = prim.type.equals("Mesh") ? meshes(prim, bone) : List.of();
        addBone(bone, parent, rest, meshes);
        animate(prim, bone, rest);
        if (prim.type.equals("Skeleton")) {
            skeleton(prim, bone);
        }
        for (final UsdPrim child : prim.children) {
            walk(child, bone);
        }
    }

    private String unique(final String wanted) {
        String bone = wanted.isEmpty() ? "prim" : wanted;
        for (int n = 2; !this.boneNames.add(bone); n++) {
            bone = wanted + "_" + n;
        }
        return bone;
    }

    /* A bone at {@code place}, in the stage's unit, made the game's pixels and stood up. */
    private void addBone(final String bone, @Nullable final String parent, final Matrix4d place,
                         final List<ModelMesh> meshes) {
        final Matrix4f turned = game(place);
        final Vector3f translation = turned.getTranslation(new Vector3f());
        final Vector3f scale = turned.getScale(new Vector3f());
        if (turned.determinant3x3() < 0) {
            turned.scale(-1, 1, 1);
            scale.x = -scale.x;
        }
        this.bones.add(new ModelBone(bone, parent, new Vector3f(), turned.getNormalizedRotation(new Quaternionf()),
                new Vector3f(), translation, scale, List.of(), meshes, List.of(), false));
    }

    /* A place in the stage made the game's: its move in pixels, and turned so the stage's up is the game's. */
    private Matrix4f game(final Matrix4d place) {
        final Matrix4d scaled = new Matrix4d(place);
        scaled.m30(scaled.m30() * this.pixels).m31(scaled.m31() * this.pixels).m32(scaled.m32() * this.pixels);
        return new Matrix4f(new Matrix4d(this.upright).mul(scaled).mul(this.backward));
    }

    /* A point of the stage made the game's. */
    private Vector3f point(final double[] xyz) {
        final Vector3d point = new Vector3d(xyz[0], xyz[1], xyz[2]).mul(this.pixels);
        this.upright.transformPosition(point);
        return new Vector3f((float) point.x, (float) point.y, (float) point.z);
    }

    private Vector3f direction(final double[] xyz) {
        final Vector3d way = new Vector3d(xyz[0], xyz[1], xyz[2]);
        this.upright.transformDirection(way);
        return new Vector3f((float) way.x, (float) way.y, (float) way.z);
    }

    /*
     * A prim's transform at {@code time} (its rest for none): its operations in the order it lists them, each the
     * matrix of a move, a turn, a size or a whole matrix, undone where it says so.
     */
    private Matrix4d local(final UsdPrim prim, final double time) {
        final Matrix4d out = new Matrix4d();
        if (!(prim.value("xformOpOrder") instanceof List<?> order)) {
            return out;
        }
        for (final Object written : order) {
            String op = String.valueOf(written);
            final boolean undo = op.startsWith("!invert!");
            if (undo) {
                op = op.substring("!invert!".length());
            }
            final UsdPrim.Property property = prim.properties.get(op);
            final Object value = property == null ? null : at(property, time);
            final Matrix4d step = operation(op, value);
            if (undo) {
                step.invert();
            }
            out.mul(step);
        }
        return out;
    }

    private static Matrix4d operation(final String op, @Nullable final Object value) {
        final String kind = op.startsWith("xformOp:") ? op.substring("xformOp:".length()).split(":")[0] : op;
        final double[] numbers = value instanceof double[] array ? array : value instanceof Double one
                ? new double[] {one} : new double[0];
        final Matrix4d out = new Matrix4d();
        if (numbers.length == 0) {
            return out;
        }
        switch (kind) {
            case "translate" -> out.translation(numbers[0], numbers[1], numbers[2]);
            case "scale" -> out.scaling(numbers[0], numbers.length > 1 ? numbers[1] : numbers[0],
                    numbers.length > 2 ? numbers[2] : numbers[0]);
            case "rotateX" -> out.rotationX(Math.toRadians(numbers[0]));
            case "rotateY" -> out.rotationY(Math.toRadians(numbers[0]));
            case "rotateZ" -> out.rotationZ(Math.toRadians(numbers[0]));
            case "orient" -> out.rotation(new Quaterniond(numbers[0], numbers[1], numbers[2], numbers[3]).normalize());
            case "transform" -> out.set(numbers);
            default -> {
                if (kind.startsWith("rotate") && ORDERS.contains(kind.substring("rotate".length()))) {
                    // Turned about the first axis first: as matrices, the last of the three is applied last.
                    final String axes = kind.substring("rotate".length());
                    for (int i = axes.length() - 1; i >= 0; i--) {
                        final double angle = Math.toRadians(numbers["XYZ".indexOf(axes.charAt(i))]);
                        switch (axes.charAt(i)) {
                            case 'X' -> out.rotateX(angle);
                            case 'Y' -> out.rotateY(angle);
                            default -> out.rotateZ(angle);
                        }
                    }
                }
            }
        }
        return out;
    }

    /* A property's value at {@code time}, between its samples, or at rest for none. */
    @Nullable
    private static Object at(final UsdPrim.Property property, final double time) {
        if (Double.isNaN(time) || property.samples.isEmpty()) {
            return property.value != null ? property.value : property.samples.isEmpty() ? null
                    : property.samples.firstEntry().getValue();
        }
        final Map.Entry<Double, Object> before = property.samples.floorEntry(time);
        final Map.Entry<Double, Object> after = property.samples.ceilingEntry(time);
        if (before == null) {
            return after.getValue();
        }
        if (after == null || after.getKey().equals(before.getKey())) {
            return before.getValue();
        }
        final double share = (time - before.getKey()) / (after.getKey() - before.getKey());
        if (before.getValue() instanceof double[] a && after.getValue() instanceof double[] b && a.length == b.length) {
            final double[] out = new double[a.length];
            for (int i = 0; i < a.length; i++) {
                out[i] = a[i] + (b[i] - a[i]) * share;
            }
            return out;
        }
        if (before.getValue() instanceof Double a && after.getValue() instanceof Double b) {
            return a + (b - a) * share;
        }
        return before.getValue();
    }

    /* The moves of a prim over time, as channels from its rest: its turn as it is, its move and size beside rest. */
    private void animate(final UsdPrim prim, final String bone, final Matrix4d rest) {
        if (!(prim.value("xformOpOrder") instanceof List<?> order)) {
            return;
        }
        final TreeSet<Double> times = new TreeSet<>();
        for (final Object op : order) {
            final UsdPrim.Property property = prim.properties.get(String.valueOf(op).replace("!invert!", ""));
            if (property != null) {
                times.addAll(property.samples.keySet());
            }
        }
        if (times.size() < 2) {
            return;
        }
        final List<Matrix4d> places = new ArrayList<>();
        for (final double time : times) {
            places.add(local(prim, time));
        }
        this.tracks.put(bone, channels(new ArrayList<>(times), places, rest));
    }

    /* Channels of keyframes from places at times, each beside {@code rest} as the Core's channels move a bone. */
    private ModelAnimation.Tracks channels(final List<Double> times, final List<Matrix4d> places,
                                           final Matrix4d rest) {
        final Matrix4f restGame = game(rest);
        final Vector3f restMove = restGame.getTranslation(new Vector3f());
        final Vector3f restSize = restGame.getScale(new Vector3f());
        final List<Keyframe> turns = new ArrayList<>();
        final List<Keyframe> moves = new ArrayList<>();
        final List<Keyframe> sizes = new ArrayList<>();
        for (int k = 0; k < times.size(); k++) {
            final double seconds = (times.get(k) - this.start) / this.perSecond;
            final Matrix4f place = game(places.get(k));
            final Quaternionf turn = place.getNormalizedRotation(new Quaternionf());
            final Vector3f move = place.getTranslation(new Vector3f()).sub(restMove);
            final Vector3f size = place.getScale(new Vector3f());
            turns.add(keyframe(seconds, turn.x, turn.y, turn.z, turn.w));
            moves.add(keyframe(seconds, move.x, move.y, move.z));
            sizes.add(keyframe(seconds, ratio(size.x, restSize.x), ratio(size.y, restSize.y),
                    ratio(size.z, restSize.z)));
        }
        return new ModelAnimation.Tracks(new AnimationChannel(AnimationChannel.Kind.QUATERNION, turns),
                new AnimationChannel(AnimationChannel.Kind.POSITION, moves),
                new AnimationChannel(AnimationChannel.Kind.SCALE, sizes));
    }

    private static float ratio(final float value, final float rest) {
        return rest == 0 ? 1 : value / rest;
    }

    private static Keyframe keyframe(final double seconds, final float... numbers) {
        final ModelExpression[] values = new ModelExpression[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            values[i] = ModelExpression.constant(numbers[i]);
        }
        return new Keyframe(seconds, values, values, Easing.LINEAR, Double.NaN, new float[0], new float[0]);
    }

    /* ---------------------------------------------------------------- skeletons */

    /*
     * A skeleton's joints, each a bone hanging from its parent joint or from the skeleton, at its rest; and its
     * animation, if it has one, as channels of those bones.
     */
    private void skeleton(final UsdPrim prim, final String skeletonBone) {
        if (!(prim.value("joints") instanceof List<?> joints)) {
            return;
        }
        final double[][] rests = matrices(prim.value("restTransforms"));
        final double[][] binds = matrices(prim.value("bindTransforms"));
        final List<String> names = new ArrayList<>();
        final Map<String, String> byPath = new HashMap<>();
        final List<Matrix4d> restPlaces = new ArrayList<>();
        for (int i = 0; i < joints.size(); i++) {
            final String path = String.valueOf(joints.get(i));
            final int slash = path.lastIndexOf('/');
            final String parentPath = slash < 0 ? null : path.substring(0, slash);
            final String bone = unique(slash < 0 ? path : path.substring(slash + 1));
            final String parent = parentPath == null ? skeletonBone : byPath.getOrDefault(parentPath, skeletonBone);
            final Matrix4d rest;
            if (i < rests.length) {
                rest = new Matrix4d().set(rests[i]);
            } else if (i < binds.length) {
                // With no rest given, the bind pose is the rest: each joint beside its parent's bind.
                final int parentIndex = parentPath == null ? -1 : joints.indexOf(parentPath);
                rest = parentIndex >= 0 && parentIndex < binds.length ? new Matrix4d().set(binds[parentIndex]).invert()
                        .mul(new Matrix4d().set(binds[i])) : new Matrix4d().set(binds[i]);
            } else {
                rest = new Matrix4d();
            }
            addBone(bone, parent, rest, List.of());
            byPath.put(path, bone);
            names.add(bone);
            restPlaces.add(rest);
        }
        final Skeleton skeleton = new Skeleton(names);
        this.skeletons.put(prim.path, skeleton);
        final UsdPrim animation = target(prim, "skel:animationSource");
        if (animation != null) {
            skeletalAnimation(animation, skeleton, joints, restPlaces);
        }
    }

    private void skeletalAnimation(final UsdPrim animation, final Skeleton skeleton, final List<?> skeletonJoints,
                                   final List<Matrix4d> rests) {
        if (!(animation.value("joints") instanceof List<?> joints)) {
            return;
        }
        final UsdPrim.Property moves = animation.properties.get("translations");
        final UsdPrim.Property turns = animation.properties.get("rotations");
        final UsdPrim.Property sizes = animation.properties.get("scales");
        final TreeSet<Double> times = new TreeSet<>();
        for (final UsdPrim.Property property : new UsdPrim.Property[] {moves, turns, sizes}) {
            if (property != null) {
                times.addAll(property.samples.keySet());
            }
        }
        if (times.isEmpty()) {
            return;
        }
        for (int j = 0; j < joints.size(); j++) {
            final int index = skeletonJoints.indexOf(joints.get(j));
            if (index < 0) {
                continue;
            }
            final List<Matrix4d> places = new ArrayList<>();
            for (final double time : times) {
                final double[] move = row(moves == null ? null : at(moves, time), j, new double[] {0, 0, 0});
                final double[] turn = row(turns == null ? null : at(turns, time), j, new double[] {0, 0, 0, 1});
                final double[] size = row(sizes == null ? null : at(sizes, time), j, new double[] {1, 1, 1});
                places.add(new Matrix4d().translation(move[0], move[1], move[2]).rotate(new Quaterniond(turn[0],
                        turn[1], turn[2], turn[3]).normalize()).scale(size[0], size[1], size[2]));
            }
            this.tracks.put(skeleton.names().get(index), channels(new ArrayList<>(times), places, rests.get(index)));
        }
    }

    private static double[] row(@Nullable final Object value, final int index, final double[] otherwise) {
        return value instanceof double[][] rows && index < rows.length ? rows[index] : otherwise;
    }

    private static double[][] matrices(@Nullable final Object value) {
        if (value instanceof double[][] rows) {
            return rows;
        }
        if (value instanceof double[] flat && flat.length % 16 == 0) {
            final double[][] out = new double[flat.length / 16][16];
            for (int i = 0; i < out.length; i++) {
                System.arraycopy(flat, i * 16, out[i], 0, 16);
            }
            return out;
        }
        return new double[0][];
    }

    /* Where a prim is in the stage at rest: its transform and those of every prim above it. */
    private Matrix4d world(final UsdPrim prim) {
        final List<UsdPrim> chain = new ArrayList<>();
        UsdPrim at = prim;
        while (at != null && !at.path.equals("/")) {
            chain.add(at);
            final int slash = at.path.lastIndexOf('/');
            at = slash <= 0 ? null : this.root.find(at.path.substring(0, slash));
        }
        final Matrix4d out = new Matrix4d();
        for (int i = chain.size() - 1; i >= 0; i--) {
            out.mul(local(chain.get(i), Double.NaN));
        }
        return out;
    }

    /* The prim a relationship of {@code prim} or of a prim above it names first. */
    @Nullable
    private UsdPrim target(final UsdPrim prim, final String relationship) {
        UsdPrim at = prim;
        while (at != null) {
            final UsdPrim.Property property = at.properties.get(relationship);
            if (property != null && !property.targets.isEmpty()) {
                return this.root.find(property.targets.getFirst().split("\\.")[0]);
            }
            final int slash = at.path.lastIndexOf('/');
            at = slash <= 0 ? null : this.root.find(at.path.substring(0, slash));
        }
        return null;
    }

    /* ---------------------------------------------------------------- meshes */

    /* A mesh's polygons as meshes of the model, one for each look its faces are painted with. */
    private List<ModelMesh> meshes(final UsdPrim prim, final String bone) {
        final double[][] points = tuples(prim.value("points"));
        final double[] counts = numbers(prim.value("faceVertexCounts"));
        final double[] corners = numbers(prim.value("faceVertexIndices"));
        if (points.length == 0 || counts.length == 0) {
            return List.of();
        }
        final boolean leftHanded = "leftHanded".equals(prim.value("orientation"));
        final Spread normals = spread(prim, prim.properties.containsKey("primvars:normals") ? "primvars:normals"
                : "normals", "vertex");
        final Spread pictures = spread(prim, pictureName(prim), "faceVarying");
        final Spread colours = spread(prim, "primvars:displayColor", "constant");
        final int[] lookOfFace = new int[counts.length];
        final List<Look> looks = new ArrayList<>();
        looks.add(look(material(prim), colours, 0));
        for (final UsdPrim subset : prim.children) {
            if (!subset.type.equals("GeomSubset") || !(subset.value("indices") instanceof double[] faces)) {
                continue;
            }
            final UsdPrim material = material(subset);
            if (material == null) {
                continue;
            }
            looks.add(look(material, colours, 0));
            for (final double face : faces) {
                if (face >= 0 && face < lookOfFace.length) {
                    lookOfFace[(int) face] = looks.size() - 1;
                }
            }
        }
        final Skin skin = skin(prim, bone);
        final List<Builder> builders = new ArrayList<>();
        for (final Look look : looks) {
            builders.add(new Builder(look, skin != null));
        }
        int corner = 0;
        for (int face = 0; face < counts.length; face++) {
            final int sides = (int) counts[face];
            final Builder into = builders.get(lookOfFace[face]);
            for (int k = 1; k + 1 < sides; k++) {
                final int[] order = leftHanded ? new int[] {0, k + 1, k} : new int[] {0, k, k + 1};
                for (final int which : order) {
                    final int at = corner + which;
                    final int point = (int) corners[at];
                    double[] place = points[point];
                    double[] normal = normals.at(face, point, at);
                    if (skin != null) {
                        // A skinned mesh is laid where it was bound, in the stage's space, as the skin bends it from.
                        final Vector3d bound = skin.bind().transformPosition(new Vector3d(place[0], place[1],
                                place[2]));
                        place = new double[] {bound.x, bound.y, bound.z};
                        if (normal != null && normal.length >= 3) {
                            final Vector3d way = skin.bind().transformDirection(new Vector3d(normal[0], normal[1],
                                    normal[2])).normalize();
                            normal = new double[] {way.x, way.y, way.z};
                        }
                    }
                    into.corner(point(place), normal, pictures.at(face, point, at), skin, point);
                }
            }
            corner += sides;
        }
        final List<ModelMesh> out = new ArrayList<>();
        for (final Builder builder : builders) {
            if (builder.triangles.size() > 0) {
                out.add(builder.mesh(this));
            }
        }
        return out;
    }

    /* The name of the primvar a mesh's pictures are laid by: st, or the first of two numbers a corner. */
    private static String pictureName(final UsdPrim prim) {
        if (prim.properties.containsKey("primvars:st")) {
            return "primvars:st";
        }
        for (final Map.Entry<String, UsdPrim.Property> entry : prim.properties.entrySet()) {
            final String type = entry.getValue().typeName;
            if (entry.getKey().startsWith("primvars:") && !entry.getKey().endsWith(":indices")
                    && (type.startsWith("texCoord2") || type.equals("float2[]"))) {
                return entry.getKey();
            }
        }
        return "primvars:st";
    }

    /* A primvar as spread over a mesh: its values, the indices into them where it has them, and how it spreads. */
    private Spread spread(final UsdPrim prim, final String primvar, final String otherwise) {
        final UsdPrim.Property property = prim.properties.get(primvar);
        if (property == null) {
            return Spread.NONE;
        }
        final double[][] values = tuples(at(property, Double.NaN));
        final double[] indices = numbers(prim.value(primvar + ":indices"));
        final Object interpolation = property.metadata.get("interpolation");
        return new Spread(values, indices, interpolation instanceof String written ? written : otherwise);
    }

    /* The material a prim is bound to, if it is bound to one. */
    @Nullable
    private UsdPrim material(final UsdPrim prim) {
        final UsdPrim.Property binding = prim.properties.get("material:binding");
        if (binding == null || binding.targets.isEmpty()) {
            return null;
        }
        return this.root.find(binding.targets.getFirst());
    }

    /*
     * A look: the texture of the material's surface, when its diffuse colour is fed by one; else its diffuse colour;
     * else the mesh's display colour; else steel.
     */
    private Look look(@Nullable final UsdPrim material, final Spread colours, final int face) {
        final UsdPrim surface = material == null ? null : surface(material);
        if (surface != null) {
            final UsdPrim.Property diffuse = surface.properties.get("inputs:diffuseColor");
            if (diffuse != null && !diffuse.targets.isEmpty()) {
                final UsdPrim reader = this.root.find(diffuse.targets.getFirst().split("\\.")[0]);
                if (reader != null && reader.value("inputs:file") instanceof UsdPrim.Asset asset) {
                    return new Look(picture(asset.path()), true);
                }
            }
            if (diffuse != null && at(diffuse, Double.NaN) instanceof double[] colour && colour.length >= 3) {
                return new Look(colour(colour), false);
            }
        }
        final double[][] values = colours.values();
        if (values.length > 0 && values[Math.min(face, values.length - 1)].length >= 3) {
            return new Look(colour(values[0]), false);
        }
        return new Look(colour(CadLook.STEEL), false);
    }

    /* The shader a material's surface is connected to. */
    @Nullable
    private UsdPrim surface(final UsdPrim material) {
        for (final String output : List.of("outputs:surface", "outputs:preview:surface")) {
            final UsdPrim.Property property = material.properties.get(output);
            if (property != null && !property.targets.isEmpty()) {
                return this.root.find(property.targets.getFirst().split("\\.")[0]);
            }
        }
        return null;
    }

    private int colour(final double[] rgb) {
        return colour(CadLook.OPAQUE | channel(rgb[0]) << 16 | channel(rgb[1]) << 8 | channel(rgb[2]));
    }

    private static int channel(final double linear) {
        return (int) Math.clamp(Math.round(Math.clamp(linear, 0, 1) * 255), 0, 255);
    }

    private int colour(final int argb) {
        return this.textureByKey.computeIfAbsent("colour " + argb, key -> {
            this.textures.add(CadLook.colour(argb));
            return this.textures.size() - 1;
        });
    }

    /* A picture the stage names, from the package or beside the file; named by its path for one not found. */
    private int picture(final String path) {
        return this.textureByKey.computeIfAbsent("picture " + path, key -> {
            final byte[] bytes = this.files.apply(path);
            this.textures.add(bytes == null ? ModelTexture.inFile(path, path) : new ModelTexture(path, "", bytes, 0,
                    0));
            return this.textures.size() - 1;
        });
    }

    /* How a skinned mesh is bent: the skeleton, each of its points' joints and weights, and where it was bound. */
    @Nullable
    private Skin skin(final UsdPrim mesh, final String bone) {
        final UsdPrim.Property indices = mesh.properties.get("primvars:skel:jointIndices");
        final UsdPrim.Property weights = mesh.properties.get("primvars:skel:jointWeights");
        if (indices == null || weights == null) {
            return null;
        }
        final UsdPrim skeletonPrim = target(mesh, "skel:skeleton");
        if (skeletonPrim == null || !(skeletonPrim.value("joints") instanceof List<?> skeletonJoints)) {
            return null;
        }
        final double[] joint = numbers(at(indices, Double.NaN));
        final double[] weight = numbers(at(weights, Double.NaN));
        final int size = indices.metadata.get("elementSize") instanceof Double each ? each.intValue() : 1;
        // The mesh may name its own joints, in its own order, among the skeleton's.
        int[] remap = null;
        if (mesh.value("skel:joints") instanceof List<?> own) {
            remap = new int[own.size()];
            for (int i = 0; i < remap.length; i++) {
                remap[i] = Math.max(0, skeletonJoints.indexOf(own.get(i)));
            }
        }
        final Matrix4d bind = mesh.value("primvars:skel:geomBindTransform") instanceof double[] matrix
                && matrix.length == 16 ? new Matrix4d().set(matrix) : new Matrix4d();
        final double[][] binds = matrices(skeletonPrim.value("bindTransforms"));
        final Matrix4d world = world(skeletonPrim);
        final float[] inverseBinds = new float[skeletonJoints.size() * 16];
        for (int j = 0; j < skeletonJoints.size(); j++) {
            final Matrix4d jointBind = j < binds.length ? new Matrix4d().set(binds[j]) : new Matrix4d();
            game(new Matrix4d(world).mul(jointBind)).invert().get(inverseBinds, j * 16);
        }
        this.pendingSkins.put(bone, new PendingSkin(skeletonPrim.path, inverseBinds));
        return new Skin(joint, weight, Math.max(1, size), remap, new Matrix4d(world).mul(bind));
    }

    private static double[][] tuples(@Nullable final Object value) {
        return value instanceof double[][] rows ? rows : new double[0][];
    }

    private static double[] numbers(@Nullable final Object value) {
        return value instanceof double[] array ? array : new double[0];
    }

    /* A skeleton read: its joints' bones, in its joints' order. */
    private record Skeleton(List<String> names) {
    }

    /* A mesh's binding to a skeleton: each point's joints and weights, so many a point, and its bind place. */
    private record Skin(double[] joints, double[] weights, int each, int @Nullable [] remap, Matrix4d bind) {
    }

    /* A skin waiting for its skeleton's bones: the skeleton's path, and each joint's inverse bind. */
    private record PendingSkin(String skeleton, float[] inverseBinds) {
    }

    /* The look of faces: the texture, and whether it is a picture laid by the mesh's own coordinates. */
    private record Look(int texture, boolean picture) {
    }

    /* A primvar's values, its indices, and how it spreads: constant, uniform (by face), vertex or faceVarying. */
    private record Spread(double[][] values, double[] indices, String interpolation) {

        static final Spread NONE = new Spread(new double[0][], new double[0], "constant");

        /* Its value at a face's corner, which is that point of the mesh and that corner of all; none for none. */
        double @Nullable [] at(final int face, final int point, final int corner) {
            if (this.values.length == 0) {
                return null;
            }
            int index = switch (this.interpolation) {
                case "constant" -> 0;
                case "uniform" -> face;
                case "vertex", "varying" -> point;
                default -> corner;
            };
            if (this.indices.length > 0) {
                index = index < this.indices.length ? (int) this.indices[index] : 0;
            }
            return index >= 0 && index < this.values.length ? this.values[index] : null;
        }
    }

    /* The corners of a mesh's faces of one look, each corner its own. */
    private static final class Builder {

        private final Look look;
        private final boolean skinned;
        private final FloatList positions = new FloatList();
        private final FloatList normals = new FloatList();
        private final FloatList uvs = new FloatList();
        private final IntList joints = new IntList();
        private final FloatList weights = new FloatList();
        private final IntList triangles = new IntList();
        private boolean everyNormal = true;

        private Builder(final Look look, final boolean skinned) {
            this.look = look;
            this.skinned = skinned;
        }

        void corner(final Vector3f point, final double @Nullable [] normal, final double @Nullable [] uv,
                    @Nullable final Skin skin, final int index) {
            this.triangles.add(this.positions.size() / 3);
            this.positions.add(point.x);
            this.positions.add(point.y);
            this.positions.add(point.z);
            if (normal != null && normal.length >= 3) {
                this.normals.add((float) normal[0]);
                this.normals.add((float) normal[1]);
                this.normals.add((float) normal[2]);
            } else {
                this.everyNormal = false;
            }
            if (this.look.picture()) {
                // USD counts a picture's height from its bottom; the Core, as the game, from its top.
                this.uvs.add(uv == null ? 0 : (float) uv[0]);
                this.uvs.add(uv == null ? 0 : 1 - (float) uv[1]);
            }
            if (this.skinned && skin != null) {
                influences(skin, index);
            }
        }

        /* A point's four strongest joints and their weights, made to add up to one. */
        private void influences(final Skin skin, final int point) {
            final int[] best = new int[MOST_INFLUENCES];
            final float[] most = new float[MOST_INFLUENCES];
            for (int k = 0; k < skin.each(); k++) {
                final int at = point * skin.each() + k;
                if (at >= skin.weights().length) {
                    break;
                }
                final float weight = (float) skin.weights()[at];
                int joint = (int) skin.joints()[at];
                if (skin.remap() != null) {
                    joint = joint >= 0 && joint < skin.remap().length ? skin.remap()[joint] : 0;
                }
                for (int slot = 0; slot < MOST_INFLUENCES; slot++) {
                    if (weight > most[slot]) {
                        System.arraycopy(most, slot, most, slot + 1, MOST_INFLUENCES - slot - 1);
                        System.arraycopy(best, slot, best, slot + 1, MOST_INFLUENCES - slot - 1);
                        most[slot] = weight;
                        best[slot] = joint;
                        break;
                    }
                }
            }
            float sum = 0;
            for (final float weight : most) {
                sum += weight;
            }
            for (int slot = 0; slot < MOST_INFLUENCES; slot++) {
                this.joints.add(best[slot]);
                this.weights.add(sum == 0 ? slot == 0 ? 1 : 0 : most[slot] / sum);
            }
        }

        ModelMesh mesh(final UsdScene scene) {
            float[] corners = this.positions.toArray();
            float[] ways = this.everyNormal ? this.normals.toArray() : new float[0];
            if (ways.length > 0) {
                for (int i = 0; i + 2 < ways.length; i += 3) {
                    final Vector3f turned = scene.direction(new double[] {ways[i], ways[i + 1], ways[i + 2]});
                    ways[i] = turned.x;
                    ways[i + 1] = turned.y;
                    ways[i + 2] = turned.z;
                }
            }
            return new ModelMesh(corners, ways, this.uvs.toArray(), this.triangles.toArray(),
                    this.skinned ? this.joints.toArray() : new int[0], this.skinned ? this.weights.toArray()
                    : new float[0], List.of(), this.look.texture());
        }
    }
}
