/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import dev.jstech.core.model.StepFile.Entity;
import dev.jstech.core.model.StepGeometry.Curve;
import dev.jstech.core.model.StepGeometry.Frame;
import dev.jstech.core.model.StepGeometry.Surface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Reads STEP (ISO 10303-21, the AP203, AP214 and AP242 a CAD program exports): its solids' boundaries, every face
 * laid with triangles on its own surface (planes, cylinders, cones, spheres, tori, B-spline surfaces, surfaces of
 * revolution and of extrusion) within the loops of its edges, each edge sampled once so the faces either side of it
 * meet; faceted solids and the triangles AP242 can carry as they are; the products of an assembly, each a bone of its
 * own placed as the file places it, with the parts it holds hanging from it; and the colours of solids and faces, each
 * a texture in the game's look ({@link CadLook}). Lengths are taken in the unit the file says, as a block is a metre.
 * It is turned from the CAD programs' upright Z to the game's Y and stands on its origin. A face on a surface it does
 * not know, or whose loops do not close, is left out and counted. Pure.
 *
 * <p>This is no modelling kernel: nothing is cut, joined or mended; the exporting program has worked out every edge
 * already, and what is read here is laid out as it says.
 */
public final class StepReader {

    private final String name;
    private final StepFile file;
    private final double pixels;
    private final double radians;
    private final double tolerance;
    private final Map<Integer, List<Vector3d>> edges = new HashMap<>();
    private final Map<Integer, Integer> colours = new HashMap<>();
    private final Map<Integer, Integer> colourTextures = new LinkedHashMap<>();
    private final List<ModelTexture> textures = new ArrayList<>();
    private final List<ModelBone> bones = new ArrayList<>();
    private final Set<String> boneNames = new HashSet<>();
    private final Map<Integer, List<Integer>> sameSpace = new HashMap<>();
    private final Map<Integer, Integer> productShapes = new HashMap<>();
    private final Map<Integer, List<Usage>> usages = new HashMap<>();
    private final Matrix4d upright;
    private final Matrix4d backward;
    private boolean tessellated;
    private int faces;
    private int dropped;

    private static final double MILLIMETRES_A_METRE = 1000.0;
    /*
     * The finest the faces are laid out, against the size of the whole, and the coarsest, a 64th of a block: as fine
     * as the game's pixels show, no finer.
     */
    private static final double FINEST = 1.0 / 150.0;
    private static final double COARSEST_MILLIMETRES = 1000.0 / 64.0;
    private static final int MOST_DEPTH = 32;
    private static final int COLOUR_DEPTH = 12;
    /* A spline edge is first cut in at most this many stretches, each halved at most this many times. */
    private static final int FIRST_PIECES = 8;
    private static final int MOST_HALVINGS = 10;
    private static final Map<String, Integer> NAMED_COLOURS = Map.of("red", 0xFFFF0000, "green", 0xFF00FF00,
            "blue", 0xFF0000FF, "yellow", 0xFFFFFF00, "magenta", 0xFFFF00FF, "cyan", 0xFF00FFFF,
            "black", 0xFF000000, "white", 0xFFFFFFFF);
    private static final Set<String> SOLIDS = Set.of("MANIFOLD_SOLID_BREP", "BREP_WITH_VOIDS", "FACETED_BREP",
            "SHELL_BASED_SURFACE_MODEL", "CLOSED_SHELL", "OPEN_SHELL", "TESSELLATED_SOLID", "TESSELLATED_SHELL",
            "TRIANGULATED_FACE", "COMPLEX_TRIANGULATED_FACE", "TRIANGULATED_SURFACE_SET",
            "COMPLEX_TRIANGULATED_SURFACE_SET", "ADVANCED_FACE", "FACE_SURFACE");
    private static final Set<String> TESSELLATED = Set.of("TESSELLATED_SOLID", "TESSELLATED_SHELL",
            "TRIANGULATED_FACE", "COMPLEX_TRIANGULATED_FACE", "TRIANGULATED_SURFACE_SET",
            "COMPLEX_TRIANGULATED_SURFACE_SET");

    private StepReader(final String name, final StepFile file) {
        this.name = name;
        this.file = file;
        final double millimetres = millimetres();
        this.pixels = millimetres / MILLIMETRES_A_METRE * CadLook.PIXELS_A_METRE;
        this.radians = radians();
        this.tolerance = tolerance(millimetres);
        this.upright = new Matrix4d().rotateX(-Math.PI / 2);
        this.backward = new Matrix4d(this.upright).invert();
        for (final Entity entity : file.all().values()) {
            if (TESSELLATED.contains(entity.type())) {
                this.tessellated = true;
                break;
            }
        }
    }

    /**
     * The model a STEP file holds.
     *
     * @throws ModelFormatException when it is not one, or holds no solid that can be laid out
     */
    public static ModelData read(final String name, final byte[] bytes) {
        return readCounted(name, bytes).model();
    }

    /**
     * {@link #read}, and how many of the file's faces it laid out and how many it left out.
     *
     * @throws ModelFormatException when it is not one, or holds no solid that can be laid out
     */
    public static Counted readCounted(final String name, final byte[] bytes) {
        final StepFile file = StepFile.parse(name, new String(bytes, StandardCharsets.ISO_8859_1));
        final StepReader reader = new StepReader(name, file);
        reader.gatherColours();
        reader.gatherStructure();
        reader.build();
        if (reader.bones.isEmpty()) {
            throw new ModelFormatException("the STEP file " + name + " holds no solid the Core can lay out");
        }
        return new Counted(CadLook.standing(new ModelData(name, CadLook.SIZE, CadLook.SIZE, reader.bones, Map.of(),
                reader.textures, List.of())), reader.faces - reader.dropped, reader.dropped);
    }

    /* ---------------------------------------------------------------- units, size and colours */

    /* How many millimetres the file's unit of length is: its SI unit with its prefix, or one it converts from. */
    private double millimetres() {
        for (final Entity entity : this.file.all().values()) {
            if (!entity.is("LENGTH_UNIT")) {
                continue;
            }
            final Entity si = entity.part("SI_UNIT");
            if (si != null) {
                return prefix(si.arg(0)) * MILLIMETRES_A_METRE;
            }
            final Entity converted = entity.part("CONVERSION_BASED_UNIT");
            if (converted != null) {
                final String unit = converted.string(0).toLowerCase(Locale.ROOT);
                if (unit.contains("inch")) {
                    return 25.4;
                }
                if (unit.contains("foot")) {
                    return 304.8;
                }
                final Entity measure = this.file.at(converted.arg(1));
                if (measure != null) {
                    final Entity base = this.file.at(measure.arg(1));
                    final double below = base != null && base.part("SI_UNIT") != null
                            ? prefix(base.part("SI_UNIT").arg(0)) * MILLIMETRES_A_METRE : 1.0;
                    return StepFile.number(measure.arg(0)) * below;
                }
            }
        }
        return 1.0;
    }

    private static double prefix(final Object written) {
        if (!(written instanceof StepFile.Enum prefix)) {
            return 1.0;
        }
        return switch (prefix.value()) {
            case "MILLI" -> 1e-3;
            case "CENTI" -> 1e-2;
            case "DECI" -> 1e-1;
            case "MICRO" -> 1e-6;
            case "KILO" -> 1e3;
            default -> 1.0;
        };
    }

    /* How many radians the file's unit of angle is: one, or a degree's for a file in degrees. */
    private double radians() {
        for (final Entity entity : this.file.all().values()) {
            if (entity.is("PLANE_ANGLE_UNIT") && entity.part("CONVERSION_BASED_UNIT") != null) {
                final Entity converted = entity.part("CONVERSION_BASED_UNIT");
                if (converted.string(0).toLowerCase(Locale.ROOT).contains("degree")) {
                    return Math.PI / 180;
                }
            }
        }
        return 1.0;
    }

    /* How near its surfaces the faces are laid, in the file's unit: finer for a small part, never too coarse. */
    private double tolerance(final double millimetres) {
        final Vector3d low = new Vector3d(Double.MAX_VALUE);
        final Vector3d high = new Vector3d(-Double.MAX_VALUE);
        for (final Entity entity : this.file.all().values()) {
            if (entity.type().equals("CARTESIAN_POINT")) {
                final Vector3d point = point(entity);
                low.min(point);
                high.max(point);
            }
        }
        final double size = low.x > high.x ? 1.0 : new Vector3d(high).sub(low).length();
        return Math.max(1e-9, Math.min(size * FINEST, COARSEST_MILLIMETRES / millimetres));
    }

    /* The colour of every solid, shell and face its styles give one, a face's over its solid's. */
    private void gatherColours() {
        for (final Entity entity : this.file.all().values()) {
            if (!entity.type().equals("STYLED_ITEM") && !entity.type().equals("OVER_RIDING_STYLED_ITEM")) {
                continue;
            }
            final Integer colour = colourIn(entity.arg(1), 0);
            if (colour != null && entity.arg(2) instanceof StepFile.Ref item) {
                this.colours.put(item.id(), colour);
            }
        }
    }

    /* The first colour found under a style, by RGB or by name. */
    @Nullable
    private Integer colourIn(final Object value, final int depth) {
        if (depth > COLOUR_DEPTH) {
            return null;
        }
        if (value instanceof List<?> list) {
            for (final Object each : list) {
                final Integer found = colourIn(each, depth + 1);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        final Entity entity = this.file.at(value);
        if (entity == null) {
            return null;
        }
        if (entity.type().equals("COLOUR_RGB")) {
            return CadLook.OPAQUE | channel(entity.number(1)) << 16 | channel(entity.number(2)) << 8
                    | channel(entity.number(3));
        }
        if (entity.type().equals("DRAUGHTING_PRE_DEFINED_COLOUR")) {
            return NAMED_COLOURS.get(entity.string(0).toLowerCase(Locale.ROOT));
        }
        for (final Object argument : entity.args()) {
            final Integer found = colourIn(argument, depth + 1);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int channel(final double share) {
        return (int) Math.clamp(Math.round(share * 255), 0, 255);
    }

    /* ---------------------------------------------------------------- products and assemblies */

    /*
     * The structure the file gives its shapes: which representations share a space, the representation of each
     * product, and the products each assembly holds, where.
     */
    private void gatherStructure() {
        for (final Entity entity : this.file.all().values()) {
            switch (entity.type()) {
                case "SHAPE_REPRESENTATION_RELATIONSHIP" -> {
                    if (entity.parts().isEmpty() && entity.arg(2) instanceof StepFile.Ref a
                            && entity.arg(3) instanceof StepFile.Ref b) {
                        this.sameSpace.computeIfAbsent(a.id(), key -> new ArrayList<>()).add(b.id());
                        this.sameSpace.computeIfAbsent(b.id(), key -> new ArrayList<>()).add(a.id());
                    }
                }
                case "SHAPE_DEFINITION_REPRESENTATION" -> {
                    final Entity shape = this.file.at(entity.arg(0));
                    final Entity definition = shape == null ? null : this.file.at(shape.arg(2));
                    if (definition != null && definition.type().startsWith("PRODUCT_DEFINITION")
                            && entity.arg(1) instanceof StepFile.Ref representation) {
                        this.productShapes.put(definition.id(), representation.id());
                    }
                }
                default -> {
                    // The rest is read where it is reached.
                }
            }
        }
        for (final Entity entity : this.file.all().values()) {
            if (!entity.type().equals("CONTEXT_DEPENDENT_SHAPE_REPRESENTATION")) {
                continue;
            }
            final Entity relation = this.file.at(entity.arg(0));
            final Entity shape = this.file.at(entity.arg(1));
            final Entity usage = shape == null ? null : this.file.at(shape.arg(2));
            if (relation == null || usage == null || !usage.type().equals("NEXT_ASSEMBLY_USAGE_OCCURRENCE")) {
                continue;
            }
            final Entity parent = this.file.at(usage.arg(3));
            final Entity child = this.file.at(usage.arg(4));
            if (parent == null || child == null) {
                continue;
            }
            this.usages.computeIfAbsent(parent.id(), key -> new ArrayList<>()).add(new Usage(child.id(),
                    usage.string(1), placement(relation, child.id())));
        }
        // An assembly a file lists without a placement for it still holds its part, where its own space is.
        for (final Entity entity : this.file.all().values()) {
            if (entity.type().equals("NEXT_ASSEMBLY_USAGE_OCCURRENCE")) {
                final Entity parent = this.file.at(entity.arg(3));
                final Entity child = this.file.at(entity.arg(4));
                if (parent != null && child != null && this.usages.getOrDefault(parent.id(), List.of()).stream()
                        .noneMatch(use -> use.product() == child.id() && use.name().equals(entity.string(1)))) {
                    this.usages.computeIfAbsent(parent.id(), key -> new ArrayList<>()).add(new Usage(child.id(),
                            entity.string(1), new Matrix4d()));
                }
            }
        }
    }

    /*
     * Where a part is placed in its assembly, from the relation between their representations: the transform takes
     * the part's own placement onto the one in the assembly, whichever of the two the file names first.
     */
    private Matrix4d placement(final Entity relation, final int child) {
        final Entity transformed = relation.part("REPRESENTATION_RELATIONSHIP_WITH_TRANSFORMATION");
        final Entity relationship = relation.part("REPRESENTATION_RELATIONSHIP");
        if (transformed == null) {
            return new Matrix4d();
        }
        final Entity operator = this.file.at(transformed.arg(0));
        if (operator == null || !operator.type().equals("ITEM_DEFINED_TRANSFORMATION")) {
            return new Matrix4d();
        }
        final Entity first = this.file.at(operator.arg(2));
        final Entity second = this.file.at(operator.arg(3));
        if (first == null || second == null) {
            return new Matrix4d();
        }
        final Integer childShape = this.productShapes.get(child);
        boolean firstIsThePart = true;
        if (childShape != null) {
            final Set<Integer> space = space(childShape);
            if (holds(space, second.id()) && !holds(space, first.id())) {
                firstIsThePart = false;
            }
        } else if (relationship != null && relationship.arg(2) instanceof StepFile.Ref one) {
            firstIsThePart = !holds(space(one.id()), second.id());
        }
        final Entity part = firstIsThePart ? first : second;
        final Entity assembly = firstIsThePart ? second : first;
        return matrix(frame(assembly)).mul(matrix(frame(part)).invert());
    }

    /* Whether a placement is one of the items of the representations of a space. */
    private boolean holds(final Set<Integer> space, final int item) {
        for (final int representation : space) {
            final Entity entity = this.file.get(representation).orElse(null);
            if (entity != null) {
                for (final Object each : entity.list(1)) {
                    if (each instanceof StepFile.Ref ref && ref.id() == item) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* A representation and every one sharing its space with it. */
    private Set<Integer> space(final int representation) {
        final Set<Integer> out = new HashSet<>();
        final List<Integer> open = new ArrayList<>(List.of(representation));
        while (!open.isEmpty()) {
            final int next = open.removeLast();
            if (out.add(next)) {
                open.addAll(this.sameSpace.getOrDefault(next, List.of()));
            }
        }
        return out;
    }

    /* The bones: each product no assembly holds, and the parts under it; or, with no products, every solid. */
    private void build() {
        final Set<Integer> held = new HashSet<>();
        this.usages.values().forEach(list -> list.forEach(use -> held.add(use.product())));
        final List<Integer> roots = new ArrayList<>();
        for (final Integer product : this.productShapes.keySet()) {
            if (!held.contains(product)) {
                roots.add(product);
            }
        }
        for (final Integer assembly : this.usages.keySet()) {
            if (!held.contains(assembly) && !roots.contains(assembly)) {
                roots.add(assembly);
            }
        }
        Collections.sort(roots);
        for (final int root : roots) {
            product(root, productName(root), new Matrix4d(), null, 0);
        }
        if (this.bones.isEmpty()) {
            // A file of shapes alone, as some programs write: every solid a bone of its own, where it is.
            final Painted painted = new Painted();
            int count = 0;
            for (final Entity entity : this.file.all().values()) {
                if (isSolid(entity) && !partOfAnother(entity)) {
                    shape(entity, painted, CadLook.STEEL);
                    count++;
                }
            }
            if (count > 0) {
                addBone("model", null, new Matrix4d(), painted.meshes());
            }
        }
    }

    private void product(final int product, final String wanted, final Matrix4d place, @Nullable final String parent,
                         final int depth) {
        if (depth > MOST_DEPTH) {
            throw new ModelFormatException("the STEP file " + this.name + " nests its assemblies too deep");
        }
        final Painted painted = new Painted();
        final List<Instance> mapped = new ArrayList<>();
        final Integer representation = this.productShapes.get(product);
        if (representation != null) {
            for (final int each : space(representation)) {
                representation(each, painted, mapped);
            }
        }
        final String bone = addBone(wanted, parent, place, painted.meshes());
        for (final Instance instance : mapped) {
            mappedInstance(instance, bone, depth + 1);
        }
        for (final Usage use : this.usages.getOrDefault(product, List.of())) {
            product(use.product(), use.name().isEmpty() ? productName(use.product()) : use.name(), use.place(), bone,
                    depth + 1);
        }
    }

    /* The shapes of one representation into {@code painted}, and the items it maps from others. */
    private void representation(final int id, final Painted painted, final List<Instance> mapped) {
        final Entity representation = this.file.get(id).orElse(null);
        if (representation == null) {
            return;
        }
        final Integer colour = this.colours.get(id);
        for (final Object item : representation.list(1)) {
            final Entity entity = this.file.at(item);
            if (entity == null) {
                continue;
            }
            if (entity.type().equals("MAPPED_ITEM")) {
                final Entity map = this.file.at(entity.arg(1));
                final Entity target = this.file.at(entity.arg(2));
                if (map != null && map.arg(1) instanceof StepFile.Ref source) {
                    final Entity origin = this.file.at(map.arg(0));
                    final Matrix4d place = target == null ? new Matrix4d() : matrix(frame(target));
                    if (origin != null) {
                        place.mul(matrix(frame(origin)).invert());
                    }
                    mapped.add(new Instance(source.id(), entity.string(0), place));
                }
            } else if (isSolid(entity)) {
                shape(entity, painted, colour == null ? CadLook.STEEL : colour);
            }
        }
    }

    private void mappedInstance(final Instance instance, final String parent, final int depth) {
        if (depth > MOST_DEPTH) {
            return;
        }
        final Painted painted = new Painted();
        final List<Instance> inner = new ArrayList<>();
        for (final int each : space(instance.representation())) {
            representation(each, painted, inner);
        }
        final String bone = addBone(instance.name().isEmpty() ? parent + "_part" : instance.name(), parent,
                instance.place(), painted.meshes());
        for (final Instance next : inner) {
            mappedInstance(next, bone, depth + 1);
        }
    }

    /* Adds a bone placed by {@code place}, in the file's unit, its move made the game's pixels and turned upright. */
    private String addBone(final String wanted, @Nullable final String parent, final Matrix4d place,
                           final List<ModelMesh> meshes) {
        String bone = wanted.isBlank() ? "part" : wanted;
        for (int n = 2; !this.boneNames.add(bone); n++) {
            bone = wanted + "_" + n;
        }
        final Matrix4d local = new Matrix4d(place);
        local.m30(local.m30() * this.pixels).m31(local.m31() * this.pixels).m32(local.m32() * this.pixels);
        final Matrix4f turned = new Matrix4f(new Matrix4d(this.upright).mul(local).mul(this.backward));
        final Vector3f translation = turned.getTranslation(new Vector3f());
        final Vector3f scale = turned.getScale(new Vector3f());
        if (turned.determinant3x3() < 0) {
            turned.scale(-1, 1, 1);
            scale.x = -scale.x;
        }
        this.bones.add(new ModelBone(bone, parent, new Vector3f(), turned.getNormalizedRotation(new Quaternionf()),
                new Vector3f(), translation, scale, List.of(), meshes, List.of(), false));
        return bone;
    }

    private String productName(final int product) {
        final Entity definition = this.file.get(product).orElse(null);
        final Entity formation = definition == null ? null : this.file.at(definition.arg(2));
        final Entity item = formation == null ? null : this.file.at(formation.arg(2));
        if (item != null && item.type().equals("PRODUCT")) {
            return item.string(1).isBlank() ? item.string(0) : item.string(1);
        }
        return "part";
    }

    private static boolean isSolid(final Entity entity) {
        return SOLIDS.contains(entity.type());
    }

    /* Whether a shape is part of a bigger one (a shell of a solid, a face of a shell), so it is not drawn twice. */
    private boolean partOfAnother(final Entity entity) {
        return switch (entity.type()) {
            case "CLOSED_SHELL", "OPEN_SHELL", "ADVANCED_FACE", "FACE_SURFACE", "TRIANGULATED_FACE",
                 "COMPLEX_TRIANGULATED_FACE" -> true;
            default -> false;
        };
    }

    /* ---------------------------------------------------------------- shapes */

    /* A solid, a shell, a set of shells or of triangles, its faces laid into {@code painted}. */
    private void shape(final Entity entity, final Painted painted, final int inherited) {
        final Integer own = this.colours.get(entity.id());
        final int colour = own == null ? inherited : own;
        final boolean tessellatedOne = TESSELLATED.contains(entity.type());
        if (this.tessellated && !tessellatedOne) {
            // A file that carries its triangles is drawn by them; its exact solids would draw the same twice.
            return;
        }
        switch (entity.type()) {
            case "MANIFOLD_SOLID_BREP", "FACETED_BREP" -> shell(this.file.at(entity.arg(1)), painted, colour);
            case "BREP_WITH_VOIDS" -> {
                shell(this.file.at(entity.arg(1)), painted, colour);
                for (final Object hollow : entity.list(2)) {
                    shell(this.file.at(hollow), painted, colour);
                }
            }
            case "SHELL_BASED_SURFACE_MODEL" -> {
                for (final Object shell : entity.list(1)) {
                    shell(this.file.at(shell), painted, colour);
                }
            }
            case "CLOSED_SHELL", "OPEN_SHELL" -> shell(entity, painted, colour);
            case "ADVANCED_FACE", "FACE_SURFACE" -> sheet(List.of(entity), painted, colour, false);
            case "TESSELLATED_SOLID", "TESSELLATED_SHELL" -> {
                for (final Object item : entity.list(1)) {
                    final Entity inner = this.file.at(item);
                    if (inner != null) {
                        shape(inner, painted, colour);
                    }
                }
            }
            default -> triangles(entity, painted, colour);
        }
    }

    private void shell(@Nullable final Entity shell, final Painted painted, final int inherited) {
        if (shell == null) {
            return;
        }
        final Integer own = this.colours.get(shell.id());
        final int colour = own == null ? inherited : own;
        final List<Entity> faceList = new ArrayList<>();
        for (final Object face : shell.list(1)) {
            final Entity entity = this.file.at(face);
            if (entity != null) {
                faceList.add(entity);
            }
        }
        sheet(faceList, painted, colour, shell.type().equals("CLOSED_SHELL"));
    }

    /*
     * Faces laid together: each laid on its own, then every one turned to face as its neighbours across their
     * shared edges say ({@link ShellOrientation}), and only then painted.
     */
    private void sheet(final List<Entity> faceList, final Painted painted, final int colour, final boolean closed) {
        final List<List<Vector3d[]>> laid = new ArrayList<>();
        final List<Integer> colours = new ArrayList<>();
        for (final Entity face : faceList) {
            final Integer own = this.colours.get(face.id());
            final List<Vector3d[]> triangles = new ArrayList<>();
            if (face(face, triangles)) {
                laid.add(triangles);
                colours.add(own == null ? colour : own);
            }
        }
        final boolean[] flips = ShellOrientation.flips(laid, closed);
        for (int f = 0; f < laid.size(); f++) {
            final Part part = painted.part(colours.get(f));
            for (final Vector3d[] triangle : laid.get(f)) {
                if (flips[f]) {
                    part.triangle(triangle[0], triangle[2], triangle[1], this.pixels);
                } else {
                    part.triangle(triangle[0], triangle[1], triangle[2], this.pixels);
                }
            }
        }
    }

    /* A face: its loops on its surface's parameters, laid with triangles there; whether it was, else counted. */
    private boolean face(final Entity face, final List<Vector3d[]> into) {
        this.faces++;
        final FaceLayout.ISink sink = (a, b, c) -> into.add(new Vector3d[] {a, b, c});
        try {
            final List<List<Vector3d>> loops = new ArrayList<>();
            final List<List<Integer>> loopEdges = new ArrayList<>();
            final List<List<Integer>> loopPieces = new ArrayList<>();
            final List<Boolean> directions = new ArrayList<>();
            for (final Object bound : face.list(1)) {
                final Entity entity = this.file.at(bound);
                if (entity == null) {
                    continue;
                }
                final List<Integer> edgeOf = new ArrayList<>();
                final List<Integer> pieceOf = new ArrayList<>();
                final List<Vector3d> loop = loop(this.file.at(entity.arg(1)), edgeOf, pieceOf);
                if (!loop.isEmpty()) {
                    loops.add(loop);
                    loopEdges.add(edgeOf);
                    loopPieces.add(pieceOf);
                    directions.add(!(entity.arg(2) instanceof StepFile.Enum way) || way.value().equals("T"));
                }
            }
            if (loops.isEmpty()) {
                this.dropped++;
                return false;
            }
            final Entity surfaceEntity = this.file.at(face.arg(2));
            final Surface surface = surfaceEntity == null ? null : surface(surfaceEntity);
            if (surfaceEntity != null && surface == null) {
                // A face on a surface not read is left out, so the rest of its solid is still drawn.
                this.dropped++;
                return false;
            }
            final boolean sameSense = !(face.arg(3) instanceof StepFile.Enum sense) || sense.value().equals("T");
            final boolean laid = surface == null || surface.flat()
                    ? flatFace(loops, sink, surface, sameSense)
                    : FaceLayout.lay(surface, loops, loopEdges, loopPieces, directions, this.tolerance, sameSense,
                    sink);
            if (!laid) {
                this.dropped++;
            }
            return laid;
        } catch (final RuntimeException broken) {
            into.clear();
            this.dropped++;
            return false;
        }
    }

    /* A flat face: its loops laid in their plane, its own or the one its points lie in. */
    private boolean flatFace(final List<List<Vector3d>> loops, final FaceLayout.ISink sink,
                             @Nullable final Surface surface, final boolean sameSense) {
        final Frame frame;
        boolean turn = !sameSense;
        if (surface instanceof StepGeometry.Plane plane) {
            frame = plane.frame();
        } else {
            final Vector3d normal = newell(loops.getFirst());
            if (normal.lengthSquared() < 1e-30) {
                return false;
            }
            frame = Frame.of(loops.getFirst().getFirst(), normal, new Vector3d(1, 0, 0));
            turn = false;
        }
        final StepGeometry.Plane plane = new StepGeometry.Plane(frame);
        return FaceLayout.lay(plane, loops, List.of(), List.of(), List.of(), this.tolerance, !turn, sink);
    }

    /* The normal of a ring of points, as Newell works it out, its length twice the ring's area. */
    private static Vector3d newell(final List<Vector3d> ring) {
        final Vector3d normal = new Vector3d();
        for (int i = 0; i < ring.size(); i++) {
            final Vector3d a = ring.get(i);
            final Vector3d b = ring.get((i + 1) % ring.size());
            normal.add((a.y - b.y) * (a.z + b.z), (a.z - b.z) * (a.x + b.x), (a.x - b.x) * (a.y + b.y));
        }
        return normal;
    }

    /*
     * A loop's points in order: each edge's as sampled, turned as the loop turns it, the edge each came from in
     * {@code edgeOf} and which of the loop's edges in {@code pieceOf}, as one edge can be passed twice running; a
     * loop of one vertex, its point; a polygon's, its corners.
     */
    private List<Vector3d> loop(@Nullable final Entity loop, final List<Integer> edgeOf,
                                final List<Integer> pieceOf) {
        final List<Vector3d> out = new ArrayList<>();
        if (loop == null) {
            return out;
        }
        switch (loop.type()) {
            case "EDGE_LOOP" -> {
                int piece = 0;
                for (final Object each : loop.list(1)) {
                    final Entity oriented = this.file.at(each);
                    if (oriented == null) {
                        continue;
                    }
                    final Entity edge = this.file.at(oriented.arg(3));
                    if (edge == null) {
                        continue;
                    }
                    final List<Vector3d> points = new ArrayList<>(edgePoints(edge));
                    if (!oriented.flag(4)) {
                        Collections.reverse(points);
                    }
                    // Each edge ends where the next begins: its last point is the next one's first.
                    for (int i = 0; i < points.size() - 1; i++) {
                        out.add(points.get(i));
                        edgeOf.add(edge.id());
                        pieceOf.add(piece);
                    }
                    piece++;
                }
            }
            case "VERTEX_LOOP" -> {
                final Entity vertex = this.file.at(loop.arg(1));
                if (vertex != null) {
                    out.add(point(this.file.at(vertex.arg(1))));
                    edgeOf.add(-1);
                    pieceOf.add(0);
                }
            }
            case "POLY_LOOP" -> {
                for (final Object each : loop.list(1)) {
                    out.add(point(this.file.at(each)));
                    edgeOf.add(-1);
                    pieceOf.add(0);
                }
            }
            default -> {
                // A loop of a kind not read leaves the face without it.
            }
        }
        return out;
    }

    /* An edge's points from its start to its end, sampled once along its curve however many faces it bounds. */
    private List<Vector3d> edgePoints(final Entity edge) {
        final List<Vector3d> known = this.edges.get(edge.id());
        if (known != null) {
            return known;
        }
        final Vector3d start = vertex(this.file.at(edge.arg(1)));
        final Vector3d end = vertex(this.file.at(edge.arg(2)));
        final Curve curve = curve(this.file.at(edge.arg(3)), 0);
        final boolean same = !(edge.arg(4) instanceof StepFile.Enum sense) || sense.value().equals("T");
        final List<Vector3d> out = new ArrayList<>();
        out.add(start);
        if (curve != null && !(curve instanceof StepGeometry.Line)) {
            double from = curve.parameterOf(start);
            double to = curve.parameterOf(end);
            final double period = curve.period();
            if (period > 0) {
                final boolean closed = start.distance(end) <= this.tolerance * 1e-3;
                if (same) {
                    while (to <= from + 1e-12) {
                        to += period;
                    }
                    if (closed) {
                        to = from + period;
                    }
                } else {
                    while (to >= from - 1e-12) {
                        to -= period;
                    }
                    if (closed) {
                        to = from - period;
                    }
                }
            }
            if (curve instanceof StepGeometry.BSplineCurve) {
                // A spline's turns are found by halving each stretch whose middle strays from its chord.
                final int first = Math.max(2, Math.min(FIRST_PIECES, curve.pieces(from, to, this.tolerance)));
                Vector3d previous = start;
                for (int i = 1; i <= first; i++) {
                    final double a = from + (to - from) * (i - 1) / first;
                    final double b = from + (to - from) * i / first;
                    final Vector3d next = i == first ? end : curve.at(b);
                    halve(curve, a, previous, b, next, out, 0);
                    if (i < first) {
                        out.add(next);
                    }
                    previous = next;
                }
            } else {
                final int pieces = curve.pieces(from, to, this.tolerance);
                for (int i = 1; i < pieces; i++) {
                    out.add(curve.at(from + (to - from) * i / pieces));
                }
            }
        }
        out.add(end);
        this.edges.put(edge.id(), out);
        return out;
    }

    /* The points between two of a curve, put where its middle strays from the chord between them, halving again. */
    private void halve(final Curve curve, final double a, final Vector3d atA, final double b, final Vector3d atB,
                       final List<Vector3d> out, final int depth) {
        final double middle = (a + b) / 2;
        final Vector3d point = curve.at(middle);
        final Vector3d chord = new Vector3d(atB).sub(atA);
        final double length = chord.lengthSquared();
        final Vector3d off = new Vector3d(point).sub(atA);
        final double along = length == 0 ? 0 : off.dot(chord) / length;
        final double stray = new Vector3d(off).sub(new Vector3d(chord).mul(along)).length();
        if (stray <= this.tolerance || depth >= MOST_HALVINGS) {
            return;
        }
        halve(curve, a, atA, middle, point, out, depth + 1);
        out.add(point);
        halve(curve, middle, point, b, atB, out, depth + 1);
    }

    /* A curve the file writes, through what it is a curve on; none for one not read, drawn then as a line. */
    @Nullable
    private Curve curve(@Nullable final Entity entity, final int depth) {
        if (entity == null || depth > MOST_DEPTH) {
            return null;
        }
        if (entity.is("B_SPLINE_CURVE_WITH_KNOTS")) {
            return bSplineCurve(entity);
        }
        return switch (entity.type()) {
            case "LINE" -> {
                final Entity vector = this.file.at(entity.arg(2));
                final Vector3d direction = direction(this.file.at(vector == null ? null : vector.arg(1)));
                final double magnitude = vector == null ? 1 : vector.number(2);
                yield new StepGeometry.Line(point(this.file.at(entity.arg(1))), direction.mul(magnitude == 0 ? 1
                        : magnitude));
            }
            case "CIRCLE" -> new StepGeometry.Circle(frame(this.file.at(entity.arg(1))), entity.number(2));
            case "ELLIPSE" -> new StepGeometry.Ellipse(frame(this.file.at(entity.arg(1))), entity.number(2),
                    entity.number(3));
            case "POLYLINE" -> {
                final List<Vector3d> points = new ArrayList<>();
                for (final Object each : entity.list(1)) {
                    points.add(point(this.file.at(each)));
                }
                yield new StepGeometry.Polyline(points);
            }
            case "SURFACE_CURVE", "SEAM_CURVE", "INTERSECTION_CURVE", "BOUNDED_SURFACE_CURVE" ->
                    curve(this.file.at(entity.arg(1)), depth + 1);
            case "TRIMMED_CURVE" -> curve(this.file.at(entity.arg(1)), depth + 1);
            default -> null;
        };
    }

    private Curve bSplineCurve(final Entity entity) {
        final Entity spline = entity.part("B_SPLINE_CURVE") != null ? entity.part("B_SPLINE_CURVE") : entity;
        final Entity knotted = entity.part("B_SPLINE_CURVE_WITH_KNOTS");
        final Entity rational = entity.part("RATIONAL_B_SPLINE_CURVE");
        final boolean complex = !entity.parts().isEmpty();
        // A simple one writes its name first; a complex one's parts write none.
        final int offset = complex ? 0 : 1;
        final int degree = (int) spline.number(offset);
        final List<Object> points = spline.list(offset + 1);
        final Vector3d[] control = new Vector3d[points.size()];
        for (int i = 0; i < control.length; i++) {
            control[i] = point(this.file.at(points.get(i)));
        }
        final int knotsAt = complex ? 0 : offset + 5;
        final double[] knots = knots(knotted.list(knotsAt), knotted.list(knotsAt + 1));
        double[] weights = new double[0];
        if (rational != null) {
            final List<Object> written = rational.list(0);
            weights = new double[written.size()];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = StepFile.number(written.get(i));
            }
        }
        if (knots.length != control.length + degree + 1) {
            throw new IllegalArgumentException("a B-spline whose knots do not fit its points");
        }
        return new StepGeometry.BSplineCurve(degree, control, weights, knots);
    }

    /* A surface the file writes; none for one not read. */
    @Nullable
    private Surface surface(final Entity entity) {
        if (entity.is("B_SPLINE_SURFACE_WITH_KNOTS")) {
            return bSplineSurface(entity);
        }
        return switch (entity.type()) {
            case "PLANE" -> new StepGeometry.Plane(frame(this.file.at(entity.arg(1))));
            case "CYLINDRICAL_SURFACE" -> new StepGeometry.Cylinder(frame(this.file.at(entity.arg(1))),
                    entity.number(2));
            case "CONICAL_SURFACE" -> new StepGeometry.Cone(frame(this.file.at(entity.arg(1))), entity.number(2),
                    entity.number(3) * this.radians);
            case "SPHERICAL_SURFACE" -> new StepGeometry.Sphere(frame(this.file.at(entity.arg(1))),
                    entity.number(2));
            case "TOROIDAL_SURFACE", "DEGENERATE_TOROIDAL_SURFACE" -> new StepGeometry.Torus(frame(
                    this.file.at(entity.arg(1))), entity.number(2), entity.number(3));
            case "SURFACE_OF_REVOLUTION" -> {
                final Curve curve = curve(this.file.at(entity.arg(1)), 0);
                final Entity axis = this.file.at(entity.arg(2));
                if (curve == null || axis == null) {
                    yield null;
                }
                yield new StepGeometry.Revolution(curve, point(this.file.at(axis.arg(1))),
                        direction(this.file.at(axis.arg(2))));
            }
            case "SURFACE_OF_LINEAR_EXTRUSION" -> {
                final Curve curve = curve(this.file.at(entity.arg(1)), 0);
                final Entity vector = this.file.at(entity.arg(2));
                if (curve == null || vector == null) {
                    yield null;
                }
                yield new StepGeometry.Extrusion(curve, direction(this.file.at(vector.arg(1))).mul(
                        vector.number(2)));
            }
            case "RECTANGULAR_TRIMMED_SURFACE", "CURVE_BOUNDED_SURFACE" -> {
                final Entity basis = this.file.at(entity.arg(1));
                yield basis == null ? null : surface(basis);
            }
            default -> null;
        };
    }

    private Surface bSplineSurface(final Entity entity) {
        final Entity spline = entity.part("B_SPLINE_SURFACE") != null ? entity.part("B_SPLINE_SURFACE") : entity;
        final Entity knotted = entity.part("B_SPLINE_SURFACE_WITH_KNOTS");
        final Entity rational = entity.part("RATIONAL_B_SPLINE_SURFACE");
        final boolean complex = !entity.parts().isEmpty();
        final int offset = complex ? 0 : 1;
        final int degreeU = (int) spline.number(offset);
        final int degreeV = (int) spline.number(offset + 1);
        final List<Object> rows = spline.list(offset + 2);
        final Vector3d[][] control = new Vector3d[rows.size()][];
        for (int i = 0; i < control.length; i++) {
            final List<?> row = (List<?>) rows.get(i);
            control[i] = new Vector3d[row.size()];
            for (int j = 0; j < row.size(); j++) {
                control[i][j] = point(this.file.at(row.get(j)));
            }
        }
        final boolean closedU = spline.flag(offset + 4);
        final boolean closedV = spline.flag(offset + 5);
        final int knotsAt = complex ? 0 : offset + 7;
        final double[] knotsU = knots(knotted.list(knotsAt), knotted.list(knotsAt + 2));
        final double[] knotsV = knots(knotted.list(knotsAt + 1), knotted.list(knotsAt + 3));
        double[][] weights = new double[0][];
        if (rational != null) {
            final List<Object> written = rational.list(0);
            weights = new double[written.size()][];
            for (int i = 0; i < weights.length; i++) {
                final List<?> row = (List<?>) written.get(i);
                weights[i] = new double[row.size()];
                for (int j = 0; j < row.size(); j++) {
                    weights[i][j] = StepFile.number(row.get(j));
                }
            }
        }
        if (knotsU.length != control.length + degreeU + 1 || knotsV.length != control[0].length + degreeV + 1) {
            throw new IllegalArgumentException("a B-spline surface whose knots do not fit its points");
        }
        return new StepGeometry.BSplineSurface(degreeU, degreeV, control, weights, knotsU, knotsV, closedU, closedV);
    }

    /* Knots each written as often as its multiplicity says. */
    private static double[] knots(final List<Object> multiplicities, final List<Object> values) {
        final List<Double> out = new ArrayList<>();
        for (int i = 0; i < values.size() && i < multiplicities.size(); i++) {
            final int times = (int) StepFile.number(multiplicities.get(i));
            for (int k = 0; k < times; k++) {
                out.add(StepFile.number(values.get(i)));
            }
        }
        final double[] knots = new double[out.size()];
        for (int i = 0; i < knots.length; i++) {
            knots[i] = out.get(i);
        }
        return knots;
    }

    /* The triangles an AP242 file carries for a face or a set of faces, as they are. */
    private void triangles(final Entity entity, final Painted painted, final int colour) {
        final boolean face = entity.type().endsWith("_FACE");
        final Entity coordinates = this.file.at(entity.arg(1));
        if (coordinates == null) {
            return;
        }
        final List<Vector3d> points = new ArrayList<>();
        for (final Object each : coordinates.list(2)) {
            final List<?> xyz = (List<?>) each;
            points.add(new Vector3d(StepFile.number(xyz.get(0)), StepFile.number(xyz.get(1)),
                    StepFile.number(xyz.get(2))));
        }
        final int indexAt = face ? 5 : 4;
        final List<Object> written = entity.list(indexAt);
        final int[] index = new int[written.size()];
        for (int i = 0; i < index.length; i++) {
            index[i] = (int) StepFile.number(written.get(i));
        }
        final Part part = painted.part(colour);
        final boolean complex = entity.type().startsWith("COMPLEX_");
        if (!complex) {
            for (final Object each : entity.list(indexAt + 1)) {
                final List<?> triangle = (List<?>) each;
                part.triangle(at(points, index, triangle.get(0)), at(points, index, triangle.get(1)),
                        at(points, index, triangle.get(2)), this.pixels);
            }
            this.faces++;
            return;
        }
        for (final Object each : entity.list(indexAt + 1)) {
            final List<?> strip = (List<?>) each;
            for (int i = 2; i < strip.size(); i++) {
                final Vector3d a = at(points, index, strip.get(i - 2));
                final Vector3d b = at(points, index, strip.get(i - 1));
                final Vector3d c = at(points, index, strip.get(i));
                if (i % 2 == 0) {
                    part.triangle(a, b, c, this.pixels);
                } else {
                    part.triangle(b, a, c, this.pixels);
                }
            }
        }
        for (final Object each : entity.list(indexAt + 2)) {
            final List<?> fan = (List<?>) each;
            for (int i = 2; i < fan.size(); i++) {
                part.triangle(at(points, index, fan.getFirst()), at(points, index, fan.get(i - 1)),
                        at(points, index, fan.get(i)), this.pixels);
            }
        }
        this.faces++;
    }

    /* A point of a set of triangles, counted from 1, through its index list where it has one. */
    private static Vector3d at(final List<Vector3d> points, final int[] index, final Object written) {
        final int n = (int) StepFile.number(written) - 1;
        return points.get(index.length > 0 ? index[n] - 1 : n);
    }

    /* ---------------------------------------------------------------- points and frames */

    private Vector3d vertex(@Nullable final Entity vertex) {
        if (vertex == null) {
            throw new IllegalArgumentException("an edge without a vertex");
        }
        return point(this.file.at(vertex.arg(1)));
    }

    private static Vector3d point(@Nullable final Entity point) {
        if (point == null) {
            throw new IllegalArgumentException("a point that is not there");
        }
        final List<Object> coordinates = point.list(1);
        return new Vector3d(coordinates.isEmpty() ? 0 : StepFile.number(coordinates.get(0)),
                coordinates.size() < 2 ? 0 : StepFile.number(coordinates.get(1)),
                coordinates.size() < 3 ? 0 : StepFile.number(coordinates.get(2)));
    }

    private static Vector3d direction(@Nullable final Entity direction) {
        if (direction == null) {
            return new Vector3d(0, 0, 1);
        }
        final List<Object> ratios = direction.list(1);
        final Vector3d out = new Vector3d(StepFile.number(ratios.get(0)), StepFile.number(ratios.get(1)),
                ratios.size() > 2 ? StepFile.number(ratios.get(2)) : 0);
        return out.lengthSquared() == 0 ? new Vector3d(0, 0, 1) : out.normalize();
    }

    private Frame frame(@Nullable final Entity placement) {
        if (placement == null) {
            return Frame.world();
        }
        final Vector3d origin = point(this.file.at(placement.arg(1)));
        final Entity axis = this.file.at(placement.arg(2));
        final Entity along = this.file.at(placement.arg(3));
        return Frame.of(origin, axis == null ? new Vector3d(0, 0, 1) : direction(axis),
                along == null ? new Vector3d(1, 0, 0) : direction(along));
    }

    private int colourTexture(final int colour) {
        return this.colourTextures.computeIfAbsent(colour, key -> {
            this.textures.add(CadLook.colour(key));
            return this.textures.size() - 1;
        });
    }

    private static Matrix4d matrix(final Frame frame) {
        return new Matrix4d(frame.x().x, frame.x().y, frame.x().z, 0, frame.y().x, frame.y().y, frame.y().z, 0,
                frame.z().x, frame.z().y, frame.z().z, 0, frame.origin().x, frame.origin().y, frame.origin().z, 1);
    }

    /**
     * A STEP file's model, and how many of its faces were laid out and how many left out.
     *
     * @param model   the model
     * @param laid    the faces laid out
     * @param dropped the faces left out, on a surface not read or with loops that do not close
     */
    public record Counted(ModelData model, int laid, int dropped) {
    }

    /* A part an assembly holds: the product, the name of its use, and where it is placed in the assembly. */
    private record Usage(int product, String name, Matrix4d place) {
    }

    /* A representation mapped into another, where. */
    private record Instance(int representation, String name, Matrix4d place) {
    }

    /* The triangles of one product's shapes, by colour, each colour a mesh of its own. */
    private final class Painted {

        private final Map<Integer, Part> parts = new LinkedHashMap<>();

        Part part(final int colour) {
            return this.parts.computeIfAbsent(colour, key -> new Part(colourTexture(key)));
        }

        List<ModelMesh> meshes() {
            final List<ModelMesh> out = new ArrayList<>();
            for (final Part part : this.parts.values()) {
                if (part.triangles.size() > 0) {
                    out.add(part.mesh());
                }
            }
            return out;
        }
    }

    /** The triangles of one colour, in the game's pixels and upright, each place one corner. */
    static final class Part {

        private final int texture;
        private final Map<Vector3d, Integer> seen = new HashMap<>();
        private final FloatList positions = new FloatList();
        private final IntList triangles = new IntList();

        Part(final int texture) {
            this.texture = texture;
        }

        /** A triangle of points in the file's unit, counter-clockwise seen from outside. */
        void triangle(final Vector3d a, final Vector3d b, final Vector3d c, final double pixels) {
            this.triangles.add(corner(a, pixels));
            this.triangles.add(corner(b, pixels));
            this.triangles.add(corner(c, pixels));
        }

        private int corner(final Vector3d point, final double pixels) {
            final Integer known = this.seen.get(point);
            if (known != null) {
                return known;
            }
            final int index = this.positions.size() / 3;
            // Upright: the CAD programs' Z is the game's Y, their Y the game's -Z.
            this.positions.add((float) (point.x * pixels));
            this.positions.add((float) (point.z * pixels));
            this.positions.add((float) (-point.y * pixels));
            this.seen.put(new Vector3d(point), index);
            return index;
        }

        ModelMesh mesh() {
            return new ModelMesh(this.positions.toArray(), new float[0], new float[0], this.triangles.toArray(),
                    new int[0], new float[0], List.of(), this.texture);
        }
    }
}
