/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reads FBX, the format Maya, 3ds Max, Cinema 4D and Blender all write: its models as bones, with their pivots and
 * the turns before and after their own; their meshes, split by material, with normals and pictures; their skins and
 * blend shapes; and every animation stack as an animation. A file says which way is up and how long its unit is; the
 * reader hangs everything from one root bone that turns the file's axes into the game's and its unit into pixels, so
 * the file's own numbers stay as they are. Pure.
 */
public final class FbxReader {

    private final String name;
    private final Map<Long, FbxNode> objects = new LinkedHashMap<>();
    private final List<Link> links = new ArrayList<>();
    private final Map<Long, String> boneNames = new LinkedHashMap<>();
    private final List<ModelTexture> textures = new ArrayList<>();
    private final Map<Long, Integer> textureIndex = new HashMap<>();
    private final Map<Long, Pose> poses = new HashMap<>();

    /** The name of the bone everything of an FBX file hangs from. */
    public static final String AXES = "fbx_axes";

    private static final double TICKS_PER_SECOND = 46_186_158_000.0;
    private static final float PIXELS_PER_CENTIMETRE = 0.16F;
    private static final double SAMPLE_STEP = 1.0 / 30;
    private static final String[] ORDERS = {"XYZ", "XZY", "YZX", "YXZ", "ZXY", "ZYX", "XYZ"};
    private static final String SPLIT = "\u0000\u0001";
    private static final int CONSTANT = 0x2;
    private static final int LINEAR = 0x4;
    private static final int CUBIC = 0x8;
    private static final int CONSTANT_NEXT = 0x100;
    private static final int MAX_JOINTS = 4;

    private FbxReader(final String name, final FbxNode root) {
        this.name = name;
        final FbxNode objectsNode = root.child("Objects");
        if (objectsNode != null) {
            for (final FbxNode object : objectsNode.children()) {
                if (!object.properties().isEmpty() && object.properties().getFirst() instanceof Long id) {
                    this.objects.put(id, object);
                }
            }
        }
        final FbxNode connections = root.child("Connections");
        if (connections != null) {
            for (final FbxNode link : connections.all("C")) {
                this.links.add(new Link(link.text(0), link.whole(1), link.whole(2), link.text(3)));
            }
        }
    }

    /**
     * The model an FBX file holds, binary or text, of version 7 or later.
     *
     * @throws ModelFormatException when the file is not one
     */
    public static ModelData read(final String name, final byte[] bytes) {
        final FbxParser.Document document = FbxParser.parse(name, bytes);
        return new FbxReader(name, document.root()).model(document.root());
    }

    private ModelData model(final FbxNode root) {
        final Map<String, ModelSkin> skins = new LinkedHashMap<>();
        final List<ModelBone> bones = new ArrayList<>();
        bones.add(axes(root));
        final Set<String> taken = new HashSet<>(Set.of(AXES));
        for (final Map.Entry<Long, FbxNode> entry : this.objects.entrySet()) {
            if (entry.getValue().name().equals("Model")) {
                this.boneNames.put(entry.getKey(), unique(objectName(entry.getValue()), taken));
            }
        }
        for (final Map.Entry<Long, FbxNode> entry : this.objects.entrySet()) {
            if (entry.getValue().name().equals("Texture")) {
                this.textureIndex.put(entry.getKey(), this.textures.size());
                this.textures.add(texture(entry.getKey(), entry.getValue()));
            }
        }
        for (final Map.Entry<Long, String> entry : this.boneNames.entrySet()) {
            final long id = entry.getKey();
            final FbxNode node = this.objects.get(id);
            final Pose pose = pose(node);
            this.poses.put(id, pose);
            final List<ModelMesh> meshes = new ArrayList<>();
            for (final long child : childrenOf(id, "Geometry")) {
                final FbxNode geometry = this.objects.get(child);
                if (geometry.text(2).equals("Mesh")) {
                    meshes.addAll(meshes(child, geometry, id, pose, entry.getValue(), skins));
                }
            }
            final Long parent = parentModel(id);
            bones.add(new ModelBone(entry.getValue(), parent == null ? AXES : this.boneNames.get(parent), pose.pivot,
                    pose.rotation(pose.lclRotation), new Vector3f(), pose.translation, pose.scale, List.of(), meshes,
                    List.of(), property(node, "Visibility", 1) == 0));
        }
        final List<ModelAnimation> animations = new ArrayList<>();
        for (final Map.Entry<Long, FbxNode> entry : this.objects.entrySet()) {
            if (entry.getValue().name().equals("AnimationStack")) {
                animations.add(animation(entry.getKey(), entry.getValue()));
            }
        }
        return new ModelData(this.name, 1, 1, bones, skins, this.textures, animations);
    }

    /* The root bone: the file's axes turned into the game's, and its unit made pixels. */
    private ModelBone axes(final FbxNode root) {
        final FbxNode settings = root.child("GlobalSettings");
        final int up = settings == null ? 1 : (int) property(settings, "UpAxis", 1);
        final int upSign = settings == null ? 1 : (int) property(settings, "UpAxisSign", 1);
        final int front = settings == null ? 2 : (int) property(settings, "FrontAxis", 2);
        final int frontSign = settings == null ? 1 : (int) property(settings, "FrontAxisSign", 1);
        final int coord = settings == null ? 0 : (int) property(settings, "CoordAxis", 0);
        final int coordSign = settings == null ? 1 : (int) property(settings, "CoordAxisSign", 1);
        final float unit = settings == null ? 1 : (float) property(settings, "UnitScaleFactor", 1);
        final Matrix3f turn = new Matrix3f().zero();
        turn.setRowColumn(0, coord, coordSign);
        turn.setRowColumn(1, up, upSign);
        turn.setRowColumn(2, front, frontSign);
        float flip = 1;
        if (turn.determinant() < 0) {
            // A left-handed file turns round by a mirror, which no turn can be: the mirror goes into the size.
            turn.scale(-1, 1, 1);
            flip = -1;
        }
        final float size = unit * PIXELS_PER_CENTIMETRE;
        return new ModelBone(AXES, null, new Vector3f(), turn.getNormalizedRotation(new Quaternionf()),
                new Vector3f(), new Vector3f(), new Vector3f(size * flip, size, size), List.of(), List.of(),
                List.of(), false);
    }

    /* A model's place, pivot, turns and size, as its properties write them. */
    private static Pose pose(final FbxNode node) {
        final int order = (int) property(node, "RotationOrder", 0);
        final Vector3f translation = vector(node, "Lcl Translation", 0).add(vector(node, "RotationOffset", 0));
        return new Pose(translation, vector(node, "Lcl Translation", 0), vector(node, "RotationPivot", 0),
                vector(node, "Lcl Rotation", 0), euler(order, vector(node, "PreRotation", 0)),
                euler(order, vector(node, "PostRotation", 0)).invert(), order, vector(node, "Lcl Scaling", 1),
                geometric(node));
    }

    /* The model's own move, turn and size of its meshes alone, which its children do not take. */
    private static Matrix4f geometric(final FbxNode node) {
        return new Matrix4f().translate(vector(node, "GeometricTranslation", 0))
                .rotate(euler(0, vector(node, "GeometricRotation", 0)))
                .scale(vector(node, "GeometricScaling", 1));
    }

    private List<ModelMesh> meshes(final long geometryId, final FbxNode geometry, final long modelId, final Pose pose,
                                   final String boneName, final Map<String, ModelSkin> skins) {
        final double[] points = geometry.doublesOf("Vertices");
        final long[] polygons = geometry.longsOf("PolygonVertexIndex");
        final Layer normals = Layer.of(geometry.child("LayerElementNormal"), "Normals", "NormalsIndex");
        final Layer uvs = Layer.of(geometry.child("LayerElementUV"), "UV", "UVIndex");
        final Layer materials = Layer.of(geometry.child("LayerElementMaterial"), "Materials", "");
        final List<Long> modelMaterials = childrenOf(modelId, "Material");
        final Weights weights = skin(geometryId, boneName, skins, points.length / 3);
        final List<Morph> morphs = morphs(geometryId, points.length / 3);
        final Map<Integer, MeshBuilder> byMaterial = new LinkedHashMap<>();
        final Matrix4f geometric = pose.geometric;
        final Vector3f corner = new Vector3f();
        int polygonStart = 0;
        int polygon = 0;
        for (int i = 0; i < polygons.length; i++) {
            if (polygons[i] >= 0) {
                continue;
            }
            final int size = i - polygonStart + 1;
            final int material = (int) materials.index(polygonStart, 0, polygon, 0);
            final MeshBuilder builder = byMaterial.computeIfAbsent(material, key -> new MeshBuilder(morphs.size()));
            final int first = builder.corners();
            for (int k = 0; k < size; k++) {
                final int pv = polygonStart + k;
                final long written = polygons[pv];
                final int point = (int) (written < 0 ? ~written : written);
                geometric.transformPosition(corner.set(points[point * 3], points[point * 3 + 1],
                        points[point * 3 + 2]));
                builder.positions.add(corner.x, corner.y, corner.z);
                if (normals.present()) {
                    final int at = (int) normals.index(pv, point, polygon, 3);
                    geometric.transformDirection(corner.set(normals.number(at * 3), normals.number(at * 3 + 1),
                            normals.number(at * 3 + 2))).normalize();
                    builder.normals.add(corner.x, corner.y, corner.z);
                }
                if (uvs.present()) {
                    final int at = (int) uvs.index(pv, point, polygon, 2);
                    // FBX counts a picture's height from its bottom; the Core, as the game, from its top.
                    builder.uvs.add((float) uvs.number(at * 2), 1 - (float) uvs.number(at * 2 + 1));
                } else {
                    builder.uvs.add(0, 0);
                }
                weights.add(point, builder);
                for (int m = 0; m < morphs.size(); m++) {
                    final float[] move = morphs.get(m).moves;
                    builder.morphs[m].add(move[point * 3], move[point * 3 + 1], move[point * 3 + 2]);
                }
            }
            for (int k = 1; k + 1 < size; k++) {
                builder.triangles.add(first, first + k, first + k + 1);
            }
            polygonStart = i + 1;
            polygon++;
        }
        final List<ModelMesh> out = new ArrayList<>();
        byMaterial.forEach((material, builder) -> out.add(builder.build(weights.skinned(), morphs,
                material < modelMaterials.size() ? materialTexture(modelMaterials.get(material)) : 0)));
        return out;
    }

    /* The skin of a geometry: for each of its points, up to four bones that move it and how much. */
    private Weights skin(final long geometryId, final String boneName, final Map<String, ModelSkin> skins,
                         final int points) {
        final List<String> joints = new ArrayList<>();
        final List<float[]> binds = new ArrayList<>();
        final List<List<float[]>> byPoint = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            byPoint.add(new ArrayList<>());
        }
        for (final long skin : childrenOf(geometryId, "Deformer")) {
            if (!this.objects.get(skin).text(2).equals("Skin")) {
                continue;
            }
            for (final long cluster : childrenOf(skin, "Deformer")) {
                final FbxNode node = this.objects.get(cluster);
                final List<Long> bone = childrenOf(cluster, "Model");
                if (bone.isEmpty()) {
                    continue;
                }
                final int joint = joints.size();
                joints.add(this.boneNames.get(bone.getFirst()));
                final Matrix4f link = matrix(node.doublesOf("TransformLink"));
                final Matrix4f mesh = matrix(node.doublesOf("Transform"));
                final float[] bind = new float[16];
                link.invert().mul(mesh).get(bind);
                binds.add(bind);
                final long[] indexes = node.longsOf("Indexes");
                final double[] amounts = node.doublesOf("Weights");
                for (int i = 0; i < Math.min(indexes.length, amounts.length); i++) {
                    if (indexes[i] >= 0 && indexes[i] < points) {
                        byPoint.get((int) indexes[i]).add(new float[] {joint, (float) amounts[i]});
                    }
                }
            }
        }
        if (joints.isEmpty()) {
            return new Weights(null);
        }
        final float[] flat = new float[binds.size() * 16];
        for (int i = 0; i < binds.size(); i++) {
            System.arraycopy(binds.get(i), 0, flat, i * 16, 16);
        }
        skins.put(boneName, new ModelSkin(joints, flat));
        final int[][] strongest = new int[points][MAX_JOINTS];
        final float[][] shares = new float[points][MAX_JOINTS];
        for (int p = 0; p < points; p++) {
            final List<float[]> pairs = byPoint.get(p);
            pairs.sort((a, b) -> Float.compare(b[1], a[1]));
            float total = 0;
            for (int k = 0; k < Math.min(MAX_JOINTS, pairs.size()); k++) {
                total += pairs.get(k)[1];
            }
            for (int k = 0; k < Math.min(MAX_JOINTS, pairs.size()); k++) {
                strongest[p][k] = (int) pairs.get(k)[0];
                shares[p][k] = total > 0 ? pairs.get(k)[1] / total : 0;
            }
        }
        return new Weights(new Skinning(strongest, shares));
    }

    /* The blend shapes of a geometry: how far each of its points moves at each channel's full weight. */
    private List<Morph> morphs(final long geometryId, final int points) {
        final List<Morph> out = new ArrayList<>();
        for (final long blend : childrenOf(geometryId, "Deformer")) {
            if (!this.objects.get(blend).text(2).equals("BlendShape")) {
                continue;
            }
            for (final long channel : childrenOf(blend, "Deformer")) {
                final float[] moves = new float[points * 3];
                for (final long shape : childrenOf(channel, "Geometry")) {
                    final FbxNode node = this.objects.get(shape);
                    final long[] indexes = node.longsOf("Indexes");
                    final double[] offsets = node.doublesOf("Vertices");
                    for (int i = 0; i < indexes.length && i * 3 + 2 < offsets.length; i++) {
                        final int point = (int) indexes[i];
                        if (point >= 0 && point < points) {
                            moves[point * 3] += (float) offsets[i * 3];
                            moves[point * 3 + 1] += (float) offsets[i * 3 + 1];
                            moves[point * 3 + 2] += (float) offsets[i * 3 + 2];
                        }
                    }
                }
                out.add(new Morph(objectName(this.objects.get(channel)), moves));
            }
        }
        return out;
    }

    /* The texture of a material's diffuse colour, or its first texture, by its place among the model's. */
    private int materialTexture(final long material) {
        Long chosen = null;
        for (final Link link : this.links) {
            if (link.parent == material && this.textureIndex.containsKey(link.child)
                    && (chosen == null || link.property.equals("DiffuseColor"))) {
                chosen = link.child;
            }
        }
        return chosen == null ? 0 : this.textureIndex.get(chosen);
    }

    private ModelTexture texture(final long id, final FbxNode node) {
        String file = node.textOf("RelativeFilename");
        if (file.isEmpty()) {
            file = node.textOf("FileName");
        }
        file = file.replace('\\', '/');
        for (final long video : childrenOf(id, "Video")) {
            final FbxNode content = this.objects.get(video).child("Content");
            if (content != null && !content.properties().isEmpty()) {
                final Object data = content.properties().getFirst();
                final byte[] bytes = data instanceof byte[] raw ? raw : data instanceof String text && !text.isEmpty()
                        ? Base64.getDecoder().decode(text.getBytes(StandardCharsets.US_ASCII)) : new byte[0];
                if (bytes.length > 0) {
                    return new ModelTexture(objectName(node), file, bytes, 0, 0);
                }
            }
        }
        return ModelTexture.inFile(objectName(node), file);
    }

    private ModelAnimation animation(final long stackId, final FbxNode stack) {
        final double start = property(stack, "LocalStart", 0) / TICKS_PER_SECOND;
        final double stop = property(stack, "LocalStop", 0) / TICKS_PER_SECOND;
        final Map<String, ModelAnimation.Tracks> bones = new LinkedHashMap<>();
        final Map<String, AnimationChannel> weights = new LinkedHashMap<>();
        double length = Math.max(0, stop - start);
        final List<Long> layers = childrenOf(stackId, "AnimationLayer");
        // The first layer is the animation; layers over it blend in an editor, and an export bakes them into it.
        for (final long node : layers.isEmpty() ? List.<Long>of() : childrenOf(layers.getFirst(),
                "AnimationCurveNode")) {
            final Curves curves = curves(node, start);
            length = Math.max(length, curves.last());
            for (final Link link : this.links) {
                if (link.child != node || !this.objects.containsKey(link.parent)) {
                    continue;
                }
                final String target = this.objects.get(link.parent).name();
                if (target.equals("Model") && this.boneNames.containsKey(link.parent)) {
                    final String bone = this.boneNames.get(link.parent);
                    final ModelAnimation.Tracks was = bones.getOrDefault(bone, new ModelAnimation.Tracks(null, null,
                            null));
                    final Pose pose = this.poses.get(link.parent);
                    bones.put(bone, switch (link.property) {
                        case "Lcl Rotation" -> new ModelAnimation.Tracks(rotation(curves, pose), was.position(),
                                was.scale());
                        case "Lcl Translation" -> new ModelAnimation.Tracks(was.rotation(), vectorChannel(
                                AnimationChannel.Kind.POSITION, curves, pose.lclTranslation, false), was.scale());
                        case "Lcl Scaling" -> new ModelAnimation.Tracks(was.rotation(), was.position(),
                                vectorChannel(AnimationChannel.Kind.SCALE, curves, pose.scale, true));
                        default -> was;
                    });
                } else if (link.property.equals("DeformPercent")) {
                    weights.put(objectName(this.objects.get(link.parent)), weightChannel(curves));
                }
            }
        }
        bones.values().removeIf(tracks -> tracks.rotation() == null && tracks.position() == null
                && tracks.scale() == null);
        return new ModelAnimation(objectName(stack), length, ModelAnimation.Loop.LOOP, bones, weights, List.of());
    }

    /* A curve node's curves, by the number each moves, and its values where it has no curve. */
    private Curves curves(final long node, final double start) {
        final FbxNode curveNode = this.objects.get(node);
        final Curves out = new Curves(new Curve[3], new double[3]);
        final String[] axes = {"d|X", "d|Y", "d|Z"};
        for (int i = 0; i < 3; i++) {
            out.defaults[i] = property(curveNode, axes[i], property(curveNode, "d|DeformPercent", 0));
        }
        for (final Link link : this.links) {
            if (link.parent != node || !this.objects.containsKey(link.child)
                    || !this.objects.get(link.child).name().equals("AnimationCurve")) {
                continue;
            }
            final int axis = switch (link.property) {
                case "d|X", "d|DeformPercent" -> 0;
                case "d|Y" -> 1;
                case "d|Z" -> 2;
                default -> -1;
            };
            if (axis >= 0) {
                out.curves[axis] = Curve.of(this.objects.get(link.child), start);
            }
        }
        return out;
    }

    private static AnimationChannel rotation(final Curves curves, final Pose pose) {
        final List<Keyframe> keyframes = new ArrayList<>();
        final Vector3f angles = new Vector3f();
        for (final double time : curves.times()) {
            curves.at(time, angles);
            final Quaternionf turn = pose.rotation(angles);
            keyframes.add(Keyframe.of(time, turn.x, turn.y, turn.z, turn.w));
        }
        return new AnimationChannel(AnimationChannel.Kind.QUATERNION, keyframes);
    }

    /* A channel of places, moved from the rest place, or of sizes, by which the rest size is multiplied. */
    private static AnimationChannel vectorChannel(final AnimationChannel.Kind kind, final Curves curves,
                                                  final Vector3f rest, final boolean ratio) {
        final List<Keyframe> keyframes = new ArrayList<>();
        final Vector3f value = new Vector3f();
        for (final double time : curves.times()) {
            curves.at(time, value);
            if (ratio) {
                keyframes.add(Keyframe.of(time, share(value.x, rest.x), share(value.y, rest.y),
                        share(value.z, rest.z)));
            } else {
                keyframes.add(Keyframe.of(time, value.x - rest.x, value.y - rest.y, value.z - rest.z));
            }
        }
        return new AnimationChannel(kind, keyframes);
    }

    private static AnimationChannel weightChannel(final Curves curves) {
        final List<Keyframe> keyframes = new ArrayList<>();
        final Vector3f value = new Vector3f();
        for (final double time : curves.times()) {
            curves.at(time, value);
            // A blend shape's weight is a percentage in FBX.
            keyframes.add(Keyframe.of(time, value.x / 100));
        }
        return new AnimationChannel(AnimationChannel.Kind.WEIGHT, keyframes);
    }

    /* The ids of the objects of record name {@code kind} connected below {@code parent}, in their order. */
    private List<Long> childrenOf(final long parent, final String kind) {
        final List<Long> out = new ArrayList<>();
        for (final Link link : this.links) {
            if (link.parent == parent && this.objects.containsKey(link.child)
                    && this.objects.get(link.child).name().equals(kind)) {
                out.add(link.child);
            }
        }
        return out;
    }

    @Nullable
    private Long parentModel(final long model) {
        for (final Link link : this.links) {
            if (link.child == model && link.type.equals("OO") && this.boneNames.containsKey(link.parent)) {
                return link.parent;
            }
        }
        return null;
    }

    private static String objectName(final FbxNode object) {
        final String written = object.text(1);
        final int split = written.indexOf(SPLIT);
        return split < 0 ? written : written.substring(0, split);
    }

    private static String unique(final String wanted, final Set<String> taken) {
        String out = wanted.isEmpty() ? "model" : wanted;
        int n = 1;
        while (!taken.add(out)) {
            out = wanted + "_" + n++;
        }
        return out;
    }

    /* A number of an object's own properties, the {@code P} records of its {@code Properties70}. */
    private static double property(final FbxNode object, final String key, final double otherwise) {
        final FbxNode found = p(object, key);
        return found == null ? otherwise : found.number(4, otherwise);
    }

    private static Vector3f vector(final FbxNode object, final String key, final float otherwise) {
        final FbxNode found = p(object, key);
        if (found == null) {
            return new Vector3f(otherwise);
        }
        return new Vector3f((float) found.number(4, otherwise), (float) found.number(5, otherwise),
                (float) found.number(6, otherwise));
    }

    @Nullable
    private static FbxNode p(final FbxNode object, final String key) {
        final FbxNode properties = object.child("Properties70");
        if (properties == null) {
            return null;
        }
        for (final FbxNode p : properties.all("P")) {
            if (p.text(0).equals(key)) {
                return p;
            }
        }
        return null;
    }

    /* A turn of angles in degrees about X, Y and Z, taken in the order an FBX model names. */
    private static Quaternionf euler(final int order, final Vector3f degrees) {
        final String axes = ORDERS[order >= 0 && order < ORDERS.length ? order : 0];
        final Quaternionf out = new Quaternionf();
        for (int i = 2; i >= 0; i--) {
            switch (axes.charAt(i)) {
                case 'X' -> out.rotateX((float) Math.toRadians(degrees.x));
                case 'Y' -> out.rotateY((float) Math.toRadians(degrees.y));
                default -> out.rotateZ((float) Math.toRadians(degrees.z));
            }
        }
        return out;
    }

    private static Matrix4f matrix(final double[] values) {
        final Matrix4f out = new Matrix4f();
        if (values.length >= 16) {
            final float[] numbers = new float[16];
            for (int i = 0; i < 16; i++) {
                numbers[i] = (float) values[i];
            }
            out.set(numbers);
        }
        return out;
    }

    private static float share(final float value, final float rest) {
        return rest == 0 ? value : value / rest;
    }

    /*
     * How a layer of a geometry is laid over it: a value for each corner of each polygon, for each point, for each
     * polygon, or one for all, and either straight or through a list of indexes.
     */
    private record Layer(String mapping, boolean indexed, double[] values, long[] indexes) {

        static Layer of(@Nullable final FbxNode layer, final String valuesName, final String indexName) {
            if (layer == null) {
                return new Layer("", false, new double[0], new long[0]);
            }
            final String reference = layer.textOf("ReferenceInformationType");
            final boolean indexed = !indexName.isEmpty() && !reference.equals("Direct");
            return new Layer(layer.textOf("MappingInformationType"), indexed, layer.doublesOf(valuesName),
                    indexName.isEmpty() ? new long[0] : layer.longsOf(indexName));
        }

        boolean present() {
            return this.values.length > 0;
        }

        /* Which value the corner {@code corner} of a polygon takes, its point {@code point}, the polygon's index. */
        long index(final int corner, final int point, final int polygon, final int width) {
            final int at = switch (this.mapping) {
                case "ByPolygonVertex" -> corner;
                case "ByVertice", "ByVertex", "ByControlPoint" -> point;
                case "ByPolygon" -> polygon;
                default -> 0;
            };
            if (this.indexed && at < this.indexes.length) {
                return this.indexes[at];
            }
            if (width == 0) {
                // A material layer writes the material of each polygon as its values.
                return at < this.values.length ? (long) this.values[at] : this.values.length > 0
                        ? (long) this.values[0] : 0;
            }
            return at;
        }

        double number(final int at) {
            return at >= 0 && at < this.values.length ? this.values[at] : 0;
        }
    }

    /* A model's pose: the parts the Core's bones hold, and the turns before and after its own an FBX model has. */
    private record Pose(Vector3f translation, Vector3f lclTranslation, Vector3f pivot, Vector3f lclRotation,
                        Quaternionf pre, Quaternionf postInverse, int order, Vector3f scale, Matrix4f geometric) {

        /* The model's whole turn for its own turn of {@code angles}: the one before, its own, the one after undone. */
        Quaternionf rotation(final Vector3f angles) {
            return new Quaternionf(this.pre).mul(euler(this.order, angles)).mul(this.postInverse);
        }
    }

    private record Link(String type, long child, long parent, String property) {
    }

    private record Morph(String name, float[] moves) {
    }

    private record Skinning(int[][] joints, float[][] weights) {
    }

    /* The skin a geometry's corners take their bones and weights from, or none. */
    private record Weights(@Nullable Skinning skinning) {

        boolean skinned() {
            return this.skinning != null;
        }

        void add(final int point, final MeshBuilder builder) {
            if (this.skinning == null) {
                return;
            }
            for (int k = 0; k < MAX_JOINTS; k++) {
                builder.joints.add(this.skinning.joints[point][k]);
                builder.weights.add(this.skinning.weights[point][k]);
            }
        }
    }

    /* A curve node's three curves, any of them none, and the values it holds where it has none. */
    private record Curves(Curve[] curves, double[] defaults) {

        /* The times to sample at: every key, and between the keys of curves that bend, a thirtieth of a second. */
        double[] times() {
            final TreeSet<Double> out = new TreeSet<>();
            for (final Curve curve : this.curves) {
                if (curve != null) {
                    curve.times(out);
                }
            }
            if (out.isEmpty()) {
                out.add(0.0);
            }
            final double[] times = new double[out.size()];
            int i = 0;
            for (final double time : out) {
                times[i++] = time;
            }
            return times;
        }

        double last() {
            double out = 0;
            for (final Curve curve : this.curves) {
                if (curve != null && curve.times.length > 0) {
                    out = Math.max(out, curve.times[curve.times.length - 1]);
                }
            }
            return out;
        }

        void at(final double time, final Vector3f out) {
            out.set(value(0, time), value(1, time), value(2, time));
        }

        private float value(final int axis, final double time) {
            return this.curves[axis] == null ? (float) this.defaults[axis] : (float) this.curves[axis].at(time);
        }
    }

    /* One animation curve: its keys' times in seconds, their values, and how each goes on to the next. */
    private record Curve(double[] times, double[] values, int[] flags, double[] rightSlopes, double[] leftSlopes) {

        static Curve of(final FbxNode curve, final double start) {
            final long[] ticks = curve.longsOf("KeyTime");
            final double[] values = curve.doublesOf("KeyValueFloat");
            final long[] attrFlags = curve.longsOf("KeyAttrFlags");
            final double[] attrData = curve.doublesOf("KeyAttrDataFloat");
            final long[] refCounts = curve.longsOf("KeyAttrRefCount");
            final int keys = Math.min(ticks.length, values.length);
            final double[] times = new double[keys];
            final int[] flags = new int[keys];
            final double[] right = new double[keys];
            final double[] left = new double[keys];
            int attr = 0;
            int used = 0;
            for (int k = 0; k < keys; k++) {
                times[k] = ticks[k] / TICKS_PER_SECOND - start;
                // The keys share their ways of going on: each way is written once, with how many keys in a row take it.
                while (attr < refCounts.length && used >= refCounts[attr]) {
                    attr++;
                    used = 0;
                }
                final int a = Math.min(attr, Math.max(0, attrFlags.length - 1));
                flags[k] = attrFlags.length == 0 ? LINEAR : (int) attrFlags[a];
                right[k] = a * 4 < attrData.length ? attrData[a * 4] : 0;
                left[k] = a * 4 + 1 < attrData.length ? attrData[a * 4 + 1] : 0;
                used++;
            }
            return new Curve(times, Arrays.copyOf(values, keys), flags, right, left);
        }

        void times(final TreeSet<Double> out) {
            for (int k = 0; k < this.times.length; k++) {
                out.add(this.times[k]);
                if (k + 1 < this.times.length && (this.flags[k] & CUBIC) != 0) {
                    for (double t = this.times[k] + SAMPLE_STEP; t < this.times[k + 1] - SAMPLE_STEP / 2;
                         t += SAMPLE_STEP) {
                        out.add(t);
                    }
                }
            }
        }

        double at(final double time) {
            if (this.times.length == 0) {
                return 0;
            }
            if (time <= this.times[0]) {
                return this.values[0];
            }
            final int last = this.times.length - 1;
            if (time >= this.times[last]) {
                return this.values[last];
            }
            int k = 0;
            while (k + 1 < last && this.times[k + 1] <= time) {
                k++;
            }
            final double span = this.times[k + 1] - this.times[k];
            final double t = span <= 0 ? 1 : (time - this.times[k]) / span;
            final double from = this.values[k];
            final double to = this.values[k + 1];
            if ((this.flags[k] & CONSTANT) != 0) {
                return (this.flags[k] & CONSTANT_NEXT) != 0 ? to : from;
            }
            if ((this.flags[k] & CUBIC) != 0) {
                final double t2 = t * t;
                final double t3 = t2 * t;
                return (2 * t3 - 3 * t2 + 1) * from + (t3 - 2 * t2 + t) * span * this.rightSlopes[k]
                        + (-2 * t3 + 3 * t2) * to + (t3 - t2) * span * this.leftSlopes[k];
            }
            return from + (to - from) * t;
        }
    }

    /* The corners, triangles, skins and morphs of the part of a geometry painted with one material. */
    private static final class MeshBuilder {

        private final Floats positions = new Floats();
        private final Floats normals = new Floats();
        private final Floats uvs = new Floats();
        private final Ints triangles = new Ints();
        private final Ints joints = new Ints();
        private final Floats weights = new Floats();
        private final Floats[] morphs;

        MeshBuilder(final int morphCount) {
            this.morphs = new Floats[morphCount];
            for (int i = 0; i < morphCount; i++) {
                this.morphs[i] = new Floats();
            }
        }

        int corners() {
            return this.positions.size / 3;
        }

        ModelMesh build(final boolean skinned, final List<Morph> named, final int texture) {
            final List<ModelMesh.MorphTarget> targets = new ArrayList<>();
            for (int i = 0; i < this.morphs.length; i++) {
                targets.add(new ModelMesh.MorphTarget(named.get(i).name, this.morphs[i].done()));
            }
            final float[] normalsDone = this.normals.size == this.positions.size ? this.normals.done() : new float[0];
            return new ModelMesh(this.positions.done(), normalsDone, this.uvs.done(), this.triangles.done(),
                    skinned ? this.joints.done() : new int[0], skinned ? this.weights.done() : new float[0], targets,
                    texture);
        }
    }

    private static final class Floats {

        private float[] data = new float[96];
        private int size;

        void add(final float... values) {
            if (this.size + values.length > this.data.length) {
                this.data = Arrays.copyOf(this.data, Math.max(this.data.length * 2, this.size + values.length));
            }
            System.arraycopy(values, 0, this.data, this.size, values.length);
            this.size += values.length;
        }

        float[] done() {
            return Arrays.copyOf(this.data, this.size);
        }
    }

    private static final class Ints {

        private int[] data = new int[96];
        private int size;

        void add(final int... values) {
            if (this.size + values.length > this.data.length) {
                this.data = Arrays.copyOf(this.data, Math.max(this.data.length * 2, this.size + values.length));
            }
            System.arraycopy(values, 0, this.data, this.size, values.length);
            this.size += values.length;
        }

        int[] done() {
            return Arrays.copyOf(this.data, this.size);
        }
    }
}
