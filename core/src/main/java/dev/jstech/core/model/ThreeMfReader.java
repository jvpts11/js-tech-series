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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reads 3MF, the package CAD programs and 3D printers share: its meshes, in the unit the file says; the parts of an
 * assembly, each object placed by a component or by the build a bone of its own, its pivot at its origin, with the
 * place the file gives it; the colour of each triangle, from base materials or colour groups, each colour a texture in
 * the game's look ({@link CadLook}); textures laid by the file's own picture coordinates; and the parts the production
 * extension keeps in files of their own. It is turned from the CAD programs' upright Z to the game's Y and stands on
 * its origin. Pure.
 */
public final class ThreeMfReader {

    private final String name;
    private final PackageFiles files;
    private final Map<String, Part> parts = new HashMap<>();
    private final List<ModelBone> bones = new ArrayList<>();
    private final Set<String> boneNames = new HashSet<>();
    private final List<ModelTexture> textures = new ArrayList<>();
    private final Map<Integer, Integer> colourTextures = new LinkedHashMap<>();
    private final Map<String, Integer> pictureTextures = new LinkedHashMap<>();
    private final Matrix4f upright = CadLook.upright();
    private final Matrix4f backward = CadLook.upright().invert();

    private static final String MODEL_RELATION = "/3dmodel";
    private static final String DEFAULT_MODEL = "3D/3dmodel.model";
    private static final String RELATIONS = "_rels/.rels";
    private static final int MOST_DEPTH = 32;
    private static final int TRANSFORM_NUMBERS = 12;
    private static final int HEX = 16;
    private static final Map<String, Float> MILLIMETRES = Map.of("micron", 0.001F, "millimeter", 1.0F,
            "centimeter", 10.0F, "inch", 25.4F, "foot", 304.8F, "meter", 1000.0F);

    private ThreeMfReader(final String name, final PackageFiles files) {
        this.name = name;
        this.files = files;
    }

    /**
     * The model a 3MF package holds.
     *
     * @throws ModelFormatException when it is not one
     */
    public static ModelData read(final String name, final byte[] bytes) {
        final ThreeMfReader reader = new ThreeMfReader(name, PackageFiles.of(name, bytes));
        final String root = reader.rootModel();
        final Part part = reader.part(root);
        int index = 0;
        for (final Item item : part.build) {
            final String path = item.path == null ? root : PackageFiles.resolve(root, item.path);
            reader.place(path, item.object, item.transform, null, "part_" + index++, 0);
        }
        if (reader.bones.isEmpty()) {
            throw new ModelFormatException("the 3MF file " + name + " builds nothing");
        }
        return CadLook.standing(new ModelData(name, CadLook.SIZE, CadLook.SIZE, reader.bones, Map.of(),
                reader.textures, List.of()));
    }

    /* The model part the package's relationships name as its 3D model, or where the format puts it. */
    private String rootModel() {
        final String[] found = {null};
        this.files.file(RELATIONS).ifPresent(bytes -> XmlElements.walk(RELATIONS, bytes, element -> {
            if (element.name().equals("Relationship") && element.attribute("Type", "").endsWith(MODEL_RELATION)
                    && found[0] == null) {
                found[0] = element.attribute("Target");
            }
        }, closed -> { }));
        final String path = found[0] == null ? DEFAULT_MODEL : PackageFiles.resolve("", found[0]);
        if (this.files.file(path).isEmpty()) {
            throw new ModelFormatException("the 3MF file " + this.name + " has no model part at " + path);
        }
        return path;
    }

    /* A model part of the package, read the first time it is asked for. */
    private Part part(final String path) {
        final Part known = this.parts.get(path.toLowerCase(Locale.ROOT));
        if (known != null) {
            return known;
        }
        final byte[] bytes = this.files.file(path).orElseThrow(() -> new ModelFormatException("the 3MF file "
                + this.name + " names a part " + path + " it does not hold"));
        final Part part = new Part(path);
        try {
            XmlElements.walk(path, bytes, part::open, part::close);
        } catch (final NumberFormatException broken) {
            throw new ModelFormatException("the 3MF file " + this.name + " has a number that does not read in "
                    + path + ": " + broken.getMessage());
        }
        this.parts.put(path.toLowerCase(Locale.ROOT), part);
        return part;
    }

    /* Places object {@code id} of the part at {@code path} as a bone, hanging from {@code parent}, and its parts. */
    private void place(final String path, final int id, final Matrix4f transform, @Nullable final String parent,
                       final String fallback, final int depth) {
        if (depth > MOST_DEPTH) {
            throw new ModelFormatException("the 3MF file " + this.name + " nests its components too deep");
        }
        final Part part = part(path);
        final MfObject object = part.objects.get(id);
        if (object == null) {
            throw new ModelFormatException("the 3MF file " + this.name + " names an object " + id + " that "
                    + path + " has none of");
        }
        if ("support".equals(object.type)) {
            return;
        }
        final String bone = unique(object.name.isEmpty() ? fallback : object.name);
        final Matrix4f local = new Matrix4f(this.upright).mul(inPixels(transform, part.pixels)).mul(this.backward);
        final Vector3f translation = local.getTranslation(new Vector3f());
        final Vector3f scale = local.getScale(new Vector3f());
        if (local.determinant3x3() < 0) {
            // A mirrored part: its turn is that of its mirror's, its size turned inside out across X.
            local.scale(-1, 1, 1);
            scale.x = -scale.x;
        }
        final Quaternionf rotation = local.getNormalizedRotation(new Quaternionf());
        this.bones.add(new ModelBone(bone, parent, new Vector3f(), rotation, new Vector3f(), translation, scale,
                List.of(), meshes(part, object), List.of(), false));
        int index = 0;
        for (final Component component : object.components) {
            final String within = component.path == null ? path : PackageFiles.resolve(path, component.path);
            place(within, component.object, component.transform, bone, bone + "_" + index++, depth + 1);
        }
    }

    /* A place written in the file's unit, its move in the game's pixels. */
    private static Matrix4f inPixels(final Matrix4f transform, final float pixels) {
        final Matrix4f out = new Matrix4f(transform);
        out.m30(out.m30() * pixels).m31(out.m31() * pixels).m32(out.m32() * pixels);
        return out;
    }

    /* An object's mesh as meshes of the model, one for each colour or picture its triangles are painted with. */
    private List<ModelMesh> meshes(final Part part, final MfObject object) {
        if (object.points.size() == 0) {
            return List.of();
        }
        final Map<Integer, Painted> byLook = new LinkedHashMap<>();
        final int count = object.corners.size() / 3;
        for (int t = 0; t < count; t++) {
            final int group = object.groups.get(t) >= 0 ? object.groups.get(t) : object.group;
            final int first = object.firsts.get(t) >= 0 ? object.firsts.get(t) : object.index;
            final TextureGroup pictures = part.textureGroups.get(group);
            if (pictures != null) {
                final int texture = picture(part, pictures);
                final Painted painted = byLook.computeIfAbsent(-1 - texture, key -> new Painted(texture, true));
                final int[] picked = {first, object.seconds.get(t) >= 0 ? object.seconds.get(t) : first,
                    object.thirds.get(t) >= 0 ? object.thirds.get(t) : first};
                for (int k = 0; k < 3; k++) {
                    painted.textured(object, part.pixels, object.corners.get(t * 3 + k), pictures, picked[k]);
                }
                continue;
            }
            final int colour = colour(part, group, first);
            final int texture = colourTexture(colour);
            final Painted painted = byLook.computeIfAbsent(texture, key -> new Painted(texture, false));
            for (int k = 0; k < 3; k++) {
                painted.plain(object, part.pixels, object.corners.get(t * 3 + k));
            }
        }
        final List<ModelMesh> meshes = new ArrayList<>();
        for (final Painted painted : byLook.values()) {
            meshes.add(painted.mesh());
        }
        return meshes;
    }

    /* The colour a triangle is painted with: a base material's or a colour group's, or steel for none. */
    private static int colour(final Part part, final int group, final int index) {
        final IntList colours = part.colourGroups.get(group);
        return colours == null || index < 0 || index >= colours.size() ? CadLook.STEEL : colours.get(index);
    }

    private int colourTexture(final int colour) {
        return this.colourTextures.computeIfAbsent(colour, key -> {
            this.textures.add(CadLook.colour(key));
            return this.textures.size() - 1;
        });
    }

    /* The texture of a group of picture coordinates: its picture from the package, read the first time. */
    private int picture(final Part part, final TextureGroup group) {
        final String path = part.pictures.getOrDefault(group.picture, "");
        final String resolved = PackageFiles.resolve(part.path, path);
        return this.pictureTextures.computeIfAbsent(resolved.toLowerCase(Locale.ROOT), key -> {
            final Optional<byte[]> bytes = this.files.file(resolved);
            this.textures.add(bytes.isPresent() ? new ModelTexture(resolved, "", bytes.get(), 0, 0)
                    : ModelTexture.inFile(resolved, resolved));
            return this.textures.size() - 1;
        });
    }

    private String unique(final String wanted) {
        String bone = wanted;
        for (int n = 2; !this.boneNames.add(bone); n++) {
            bone = wanted + "_" + n;
        }
        return bone;
    }

    /* A 3MF transform, twelve numbers row by row for a point written as a row, as a place. */
    private static Matrix4f transform(@Nullable final String written) {
        if (written == null || written.isBlank()) {
            return new Matrix4f();
        }
        final String[] numbers = written.strip().split("\\s+");
        if (numbers.length != TRANSFORM_NUMBERS) {
            throw new NumberFormatException("a transform of " + numbers.length + " numbers: " + written);
        }
        final float[] m = new float[TRANSFORM_NUMBERS];
        for (int i = 0; i < m.length; i++) {
            m[i] = Float.parseFloat(numbers[i]);
        }
        // Each of its rows is the column the Core's matrices keep.
        return new Matrix4f(m[0], m[1], m[2], 0, m[3], m[4], m[5], 0, m[6], m[7], m[8], 0, m[9], m[10], m[11], 1);
    }

    /* A colour written #RRGGBB or #RRGGBBAA, as ARGB. */
    private static int colour(final String written) {
        final String hex = written.startsWith("#") ? written.substring(1) : written;
        final long value = Long.parseLong(hex, HEX);
        return hex.length() > 6 ? (int) ((value & 0xFF) << 24 | value >>> 8) : CadLook.OPAQUE | (int) value;
    }

    /* What one model part of the package holds, gathered as its elements open and close. */
    private static final class Part {

        private final String path;
        private final Map<Integer, MfObject> objects = new HashMap<>();
        private final Map<Integer, IntList> colourGroups = new HashMap<>();
        private final Map<Integer, TextureGroup> textureGroups = new HashMap<>();
        private final Map<Integer, String> pictures = new HashMap<>();
        private final List<Item> build = new ArrayList<>();
        private float pixels = CadLook.PIXELS_A_METRE / 1000.0F;
        @Nullable
        private MfObject object;
        @Nullable
        private IntList colours;
        @Nullable
        private TextureGroup coordinates;

        private Part(final String path) {
            this.path = path;
        }

        void open(final XmlElements element) {
            switch (element.name()) {
                case "model" -> {
                    final Float millimetres = MILLIMETRES.get(element.attribute("unit", "millimeter"));
                    this.pixels = (millimetres == null ? 1.0F : millimetres) * CadLook.PIXELS_A_METRE / 1000.0F;
                }
                case "object" -> {
                    this.object = new MfObject(element.attribute("name", ""), element.attribute("type", "model"),
                            element.whole("pid", -1), element.whole("pindex", 0));
                    this.objects.put(element.whole("id", -1), this.object);
                }
                case "vertex" -> {
                    if (this.object != null) {
                        this.object.points.add(element.number("x", 0));
                        this.object.points.add(element.number("y", 0));
                        this.object.points.add(element.number("z", 0));
                    }
                }
                case "triangle" -> {
                    if (this.object != null) {
                        this.object.corners.add(element.whole("v1", 0));
                        this.object.corners.add(element.whole("v2", 0));
                        this.object.corners.add(element.whole("v3", 0));
                        this.object.groups.add(element.whole("pid", -1));
                        this.object.firsts.add(element.whole("p1", -1));
                        this.object.seconds.add(element.whole("p2", -1));
                        this.object.thirds.add(element.whole("p3", -1));
                    }
                }
                case "component" -> {
                    if (this.object != null) {
                        this.object.components.add(new Component(element.whole("objectid", -1),
                                transform(element.attribute("transform")), element.attribute("path")));
                    }
                }
                case "item" -> this.build.add(new Item(element.whole("objectid", -1),
                        transform(element.attribute("transform")), element.attribute("path")));
                case "basematerials", "colorgroup" -> {
                    this.colours = new IntList();
                    this.colourGroups.put(element.whole("id", -1), this.colours);
                }
                case "base" -> {
                    if (this.colours != null) {
                        this.colours.add(colour(element.attribute("displaycolor", "#B4B9C0")));
                    }
                }
                case "color" -> {
                    if (this.colours != null) {
                        this.colours.add(colour(element.attribute("color", "#B4B9C0")));
                    }
                }
                case "texture2d" -> this.pictures.put(element.whole("id", -1), element.attribute("path", ""));
                case "texture2dgroup" -> {
                    this.coordinates = new TextureGroup(element.whole("texid", -1));
                    this.textureGroups.put(element.whole("id", -1), this.coordinates);
                }
                case "tex2coord" -> {
                    if (this.coordinates != null) {
                        this.coordinates.uvs.add(element.number("u", 0));
                        // The file counts a picture's height from its bottom; the Core, as the game, from its top.
                        this.coordinates.uvs.add(1 - element.number("v", 0));
                    }
                }
                default -> {
                    // Metadata, thumbnails and the extensions' other elements carry nothing the Core draws.
                }
            }
        }

        void close(final String name) {
            switch (name) {
                case "object" -> this.object = null;
                case "basematerials", "colorgroup" -> this.colours = null;
                case "texture2dgroup" -> this.coordinates = null;
                default -> {
                    // Nothing else is held open.
                }
            }
        }
    }

    /* An object of a part: its mesh, as the file writes it, or its components, and the look it is painted with. */
    private static final class MfObject {

        private final String name;
        private final String type;
        private final int group;
        private final int index;
        private final FloatList points = new FloatList();
        private final IntList corners = new IntList();
        private final IntList groups = new IntList();
        private final IntList firsts = new IntList();
        private final IntList seconds = new IntList();
        private final IntList thirds = new IntList();
        private final List<Component> components = new ArrayList<>();

        private MfObject(final String name, final String type, final int group, final int index) {
            this.name = name;
            this.type = type;
            this.group = group;
            this.index = index;
        }
    }

    /* The corners of an object's triangles of one colour or one picture, as a mesh of the model. */
    private final class Painted {

        private final int texture;
        private final boolean textured;
        private final Map<Integer, Integer> seen = new HashMap<>();
        private final FloatList positions = new FloatList();
        private final FloatList uvs = new FloatList();
        private final IntList triangles = new IntList();
        private final Vector3f point = new Vector3f();

        private Painted(final int texture, final boolean textured) {
            this.texture = texture;
            this.textured = textured;
        }

        /* A corner painted with a colour: one corner for each of the object's points. */
        void plain(final MfObject object, final float pixels, final int corner) {
            Integer index = this.seen.get(corner);
            if (index == null) {
                index = this.positions.size() / 3;
                this.seen.put(corner, index);
                add(object, pixels, corner);
            }
            this.triangles.add(index);
        }

        /* A corner painted with a picture: a corner of its own, at its place on the picture. */
        void textured(final MfObject object, final float pixels, final int corner, final TextureGroup group,
                      final int coordinate) {
            this.triangles.add(this.positions.size() / 3);
            add(object, pixels, corner);
            final boolean known = coordinate >= 0 && coordinate * 2 + 1 < group.uvs.size();
            this.uvs.add(known ? group.uvs.get(coordinate * 2) : 0);
            this.uvs.add(known ? group.uvs.get(coordinate * 2 + 1) : 0);
        }

        ModelMesh mesh() {
            return new ModelMesh(this.positions.toArray(), new float[0], this.textured ? this.uvs.toArray()
                    : new float[0], this.triangles.toArray(), new int[0], new float[0], List.of(), this.texture);
        }

        private void add(final MfObject object, final float pixels, final int corner) {
            if (corner < 0 || corner * 3 + 2 >= object.points.size()) {
                throw new ModelFormatException("the 3MF file " + ThreeMfReader.this.name + " has a triangle with "
                        + "a corner it has no point for: " + corner);
            }
            this.point.set(object.points.get(corner * 3), object.points.get(corner * 3 + 1),
                    object.points.get(corner * 3 + 2)).mul(pixels);
            CadLook.upright(this.point);
            this.positions.add(this.point.x);
            this.positions.add(this.point.y);
            this.positions.add(this.point.z);
        }
    }

    /* A group of picture coordinates, and the picture they are on. */
    private static final class TextureGroup {

        private final int picture;
        private final FloatList uvs = new FloatList();

        private TextureGroup(final int picture) {
            this.picture = picture;
        }
    }

    /* An object placed within another, perhaps from another part of the package. */
    private record Component(int object, Matrix4f transform, @Nullable String path) {
    }

    /* An object the file builds, where it builds it. */
    private record Item(int object, Matrix4f transform, @Nullable String path) {
    }
}
