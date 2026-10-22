/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class StepReaderTest {

    /* A millimetre, in the game's pixels. */
    private static final float MILLIMETRE = 16.0F / 1000.0F;
    private static final double SHARE = 0.05;

    @Test
    void cube_readsItsSixFacesInMillimetres() {
        final StepReader.Counted read = read(cube(false));
        assertEquals(6, read.laid());
        assertEquals(0, read.dropped());
        assertEquals(12, triangles(read.model()), "two triangles a square face");
        assertEquals(10 * MILLIMETRE, size(read.model()).x, 1e-5, "10 mm wide");
    }

    @Test
    void cube_facesOutward() {
        assertOutward(read(cube(false)).model());
    }

    @Test
    void cube_takesTheColourItsStyleGives() {
        final ModelData cube = read(cube(true)).model();
        assertTrue(cube.textures().getFirst().name().contains("ffff0000"), cube.textures().getFirst().name());
    }

    @Test
    void cylinder_withASeam_isLaidRoundAndOutward() {
        final StepReader.Counted read = read(cylinder(true));
        assertEquals(0, read.dropped());
        final double radius = 5 * MILLIMETRE;
        final double height = 10 * MILLIMETRE;
        assertEquals(2 * Math.PI * radius * height + 2 * Math.PI * radius * radius, area(read.model()),
                SHARE * area(read.model()));
        assertOutward(read.model());
    }

    @Test
    void cylinder_boundByItsTwoCirclesAlone_isLaidAsABand() {
        final StepReader.Counted read = read(cylinder(false));
        assertEquals(0, read.dropped());
        final double radius = 5 * MILLIMETRE;
        final double height = 10 * MILLIMETRE;
        assertEquals(2 * Math.PI * radius * height + 2 * Math.PI * radius * radius, area(read.model()),
                SHARE * area(read.model()));
    }

    @Test
    void sphere_ofOneSeamFromPoleToPole_isLaidWhole() {
        final StepReader.Counted read = read(sphere());
        assertEquals(0, read.dropped());
        final double radius = 5 * MILLIMETRE;
        assertEquals(4 * Math.PI * radius * radius, area(read.model()), SHARE * 4 * Math.PI * radius * radius);
        assertOutward(read.model());
    }

    @Test
    void bSplinePatch_isLaidOnItsCurve() {
        final StepReader.Counted read = read(patch());
        assertEquals(0, read.dropped());
        assertTrue(triangles(read.model()) > 8, "a curved patch takes more than two triangles");
        final float top = size(read.model()).y;
        assertTrue(top > 1 * MILLIMETRE && top < 4 * MILLIMETRE, "it bulges as its control points say: " + top);
    }

    @Test
    void faceOnASurfaceNotRead_isLeftOutAndCounted() {
        final StepReader.Counted read = read(cube(false).replaceFirst("PLANE\\('',", "OFFSET_SURFACE('',"));
        assertEquals(1, read.dropped());
        assertEquals(5, read.laid());
    }

    @Test
    void assembly_placesItsPartWhereTheTransformSays() {
        final ModelData read = read(assembly()).model();
        assertEquals("box", read.bones().getFirst().name());
        final ModelBone part = read.bone("cube_1").orElseThrow();
        assertEquals("box", part.parent());
        assertEquals(20 * MILLIMETRE, part.translation().x(), 1e-5, "20 mm along the assembly's X");
    }

    @Test
    void read_refusesAFileThatIsNotStep() {
        assertThrows(ModelFormatException.class, () -> StepReader.read("not", "solid x".getBytes(
                StandardCharsets.US_ASCII)));
    }

    /* ---------------------------------------------------------------- the solids */

    private static String cube(final boolean red) {
        final Writer step = new Writer();
        final int[] v = new int[8];
        final double[][] corners = {{0, 0, 0}, {10, 0, 0}, {10, 10, 0}, {0, 10, 0}, {0, 0, 10}, {10, 0, 10},
            {10, 10, 10}, {0, 10, 10}};
        for (int i = 0; i < 8; i++) {
            v[i] = step.vertex(corners[i]);
        }
        final List<Integer> faces = new ArrayList<>();
        faces.add(step.planeFace(new int[] {v[0], v[3], v[2], v[1]}, corners[0], new double[] {0, 0, -1}));
        faces.add(step.planeFace(new int[] {v[4], v[5], v[6], v[7]}, corners[4], new double[] {0, 0, 1}));
        faces.add(step.planeFace(new int[] {v[0], v[1], v[5], v[4]}, corners[0], new double[] {0, -1, 0}));
        faces.add(step.planeFace(new int[] {v[2], v[3], v[7], v[6]}, corners[2], new double[] {0, 1, 0}));
        faces.add(step.planeFace(new int[] {v[1], v[2], v[6], v[5]}, corners[1], new double[] {1, 0, 0}));
        faces.add(step.planeFace(new int[] {v[3], v[0], v[4], v[7]}, corners[0], new double[] {-1, 0, 0}));
        final int solid = step.solid(faces);
        if (red) {
            final int colour = step.add("COLOUR_RGB('',1.,0.,0.)");
            final int fill = step.add("FILL_AREA_STYLE_COLOUR(''," + ref(colour) + ")");
            final int area = step.add("FILL_AREA_STYLE(''," + list(fill) + ")");
            final int surface = step.add("SURFACE_STYLE_FILL_AREA(" + ref(area) + ")");
            final int side = step.add("SURFACE_SIDE_STYLE(''," + list(surface) + ")");
            final int usage = step.add("SURFACE_STYLE_USAGE(.BOTH.," + ref(side) + ")");
            final int assignment = step.add("PRESENTATION_STYLE_ASSIGNMENT(" + list(usage) + ")");
            step.add("STYLED_ITEM('color'," + list(assignment) + "," + ref(solid) + ")");
        }
        return step.text();
    }

    private static String cylinder(final boolean seam) {
        final Writer step = new Writer();
        final int bottom = step.vertex(new double[] {5, 0, 0});
        final int top = step.vertex(new double[] {5, 0, 10});
        final int bottomCircle = step.edge(bottom, bottom, step.add("CIRCLE(''," + step.placement(new double[3],
                new double[] {0, 0, 1}) + ",5.)"));
        final int topCircle = step.edge(top, top, step.add("CIRCLE(''," + step.placement(new double[] {0, 0, 10},
                new double[] {0, 0, 1}) + ",5.)"));
        final int surface = step.add("CYLINDRICAL_SURFACE(''," + step.placement(new double[3],
                new double[] {0, 0, 1}) + ",5.)");
        final int side;
        if (seam) {
            final int line = step.line(new double[] {5, 0, 0}, new double[] {5, 0, 10});
            final int seamEdge = step.edge(bottom, top, line);
            side = step.face(List.of(step.loop(step.oriented(bottomCircle, true), step.oriented(seamEdge, true),
                    step.oriented(topCircle, false), step.oriented(seamEdge, false))), List.of(), surface);
        } else {
            side = step.face(List.of(step.loop(step.oriented(bottomCircle, true))),
                    List.of(step.loop(step.oriented(topCircle, false))), surface);
        }
        final int bottomFace = step.face(List.of(step.loop(step.oriented(bottomCircle, false))), List.of(),
                step.add("PLANE(''," + step.placement(new double[3], new double[] {0, 0, -1}) + ")"));
        final int topFace = step.face(List.of(step.loop(step.oriented(topCircle, true))), List.of(),
                step.add("PLANE(''," + step.placement(new double[] {0, 0, 10}, new double[] {0, 0, 1}) + ")"));
        step.solid(List.of(side, bottomFace, topFace));
        return step.text();
    }

    private static String sphere() {
        final Writer step = new Writer();
        final int south = step.vertex(new double[] {0, 0, -5});
        final int north = step.vertex(new double[] {0, 0, 5});
        final int meridian = step.add("CIRCLE(''," + step.placement(new double[3], new double[] {0, -1, 0},
                new double[] {1, 0, 0}) + ",5.)");
        final int seam = step.edge(south, north, meridian);
        final int surface = step.add("SPHERICAL_SURFACE(''," + step.placement(new double[3],
                new double[] {0, 0, 1}) + ",5.)");
        step.solid(List.of(step.face(List.of(step.loop(step.oriented(seam, true), step.oriented(seam, false))),
                List.of(), surface)));
        return step.text();
    }

    /* A square patch 10 mm a side, bulging 2.5 mm up in its middle, a B-spline of degree 2 bounded by four. */
    private static String patch() {
        final Writer step = new Writer();
        final double[][][] control = new double[3][3][];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                control[i][j] = new double[] {i * 5, j * 5, i == 1 && j == 1 ? 10 : 0};
            }
        }
        final int[][] points = new int[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                points[i][j] = step.point(control[i][j]);
            }
        }
        final StringBuilder rows = new StringBuilder("(");
        for (int i = 0; i < 3; i++) {
            rows.append(i > 0 ? "," : "").append(list(points[i][0], points[i][1], points[i][2]));
        }
        rows.append(")");
        final int surface = step.add("B_SPLINE_SURFACE_WITH_KNOTS('',2,2," + rows
                + ",.UNSPECIFIED.,.F.,.F.,.F.,(3,3),(3,3),(0.,1.),(0.,1.),.UNSPECIFIED.)");
        final int a = step.vertex(control[0][0]);
        final int b = step.vertex(control[2][0]);
        final int c = step.vertex(control[2][2]);
        final int d = step.vertex(control[0][2]);
        final int ab = step.edge(a, b, step.curve(points[0][0], points[1][0], points[2][0]));
        final int bc = step.edge(b, c, step.curve(points[2][0], points[2][1], points[2][2]));
        final int cd = step.edge(c, d, step.curve(points[2][2], points[1][2], points[0][2]));
        final int da = step.edge(d, a, step.curve(points[0][2], points[0][1], points[0][0]));
        step.solid(List.of(step.face(List.of(step.loop(step.oriented(ab, true), step.oriented(bc, true),
                step.oriented(cd, true), step.oriented(da, true))), List.of(), surface)));
        return step.text().replace("MANIFOLD_SOLID_BREP('',", "SHELL_BASED_SURFACE_MODEL('',(")
                .replaceFirst("CLOSED_SHELL", "OPEN_SHELL").replaceAll("(SHELL_BASED_SURFACE_MODEL\\('',\\(#\\d+)",
                        "$1)");
    }

    private static String assembly() {
        final Writer step = new Writer();
        final String cube = cube(false);
        step.raw(cube.substring(cube.indexOf("DATA;") + 5, cube.indexOf("ENDSEC;", cube.indexOf("DATA;"))));
        final int solid = step.lastSolid();
        final int context = step.add("(GEOMETRIC_REPRESENTATION_CONTEXT(3) GLOBAL_UNIT_ASSIGNED_CONTEXT(("
                + ref(step.add("(LENGTH_UNIT() NAMED_UNIT(*) SI_UNIT(.MILLI.,.METRE.))")) + "))"
                + " REPRESENTATION_CONTEXT('',''))");
        final int partAxis = step.add("AXIS2_PLACEMENT_3D(''," + ref(step.point(new double[3])) + ",$,$)");
        final int partShape = step.add("ADVANCED_BREP_SHAPE_REPRESENTATION('cube'," + list(solid, partAxis) + ","
                + ref(context) + ")");
        final int boxOrigin = step.add("AXIS2_PLACEMENT_3D(''," + ref(step.point(new double[3])) + ",$,$)");
        final int boxPlace = step.add("AXIS2_PLACEMENT_3D(''," + ref(step.point(new double[] {20, 0, 0}))
                + ",$,$)");
        final int boxShape = step.add("SHAPE_REPRESENTATION('box'," + list(boxOrigin, boxPlace) + ","
                + ref(context) + ")");
        final int cubeProduct = step.product("cube", partShape);
        final int boxProduct = step.product("box", boxShape);
        final int usage = step.add("NEXT_ASSEMBLY_USAGE_OCCURRENCE('1','cube_1',''," + ref(boxProduct) + ","
                + ref(cubeProduct) + ",$)");
        final int usageShape = step.add("PRODUCT_DEFINITION_SHAPE('',''," + ref(usage) + ")");
        final int transform = step.add("ITEM_DEFINED_TRANSFORMATION(''," + "''," + ref(partAxis) + ","
                + ref(boxPlace) + ")");
        final int relation = step.add("(REPRESENTATION_RELATIONSHIP('',''," + ref(partShape) + "," + ref(boxShape)
                + ") REPRESENTATION_RELATIONSHIP_WITH_TRANSFORMATION(" + ref(transform)
                + ") SHAPE_REPRESENTATION_RELATIONSHIP())");
        step.add("CONTEXT_DEPENDENT_SHAPE_REPRESENTATION(" + ref(relation) + "," + ref(usageShape) + ")");
        return step.text();
    }

    /* ---------------------------------------------------------------- measuring */

    private static StepReader.Counted read(final String text) {
        return StepReader.readCounted("test", text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static int triangles(final ModelData data) {
        return ModelShaping.meshTriangles(data);
    }

    /* The size of the model's box at rest, in pixels. */
    private static Vector3f size(final ModelData data) {
        final Vector3f low = new Vector3f(Float.MAX_VALUE);
        final Vector3f high = new Vector3f(-Float.MAX_VALUE);
        forEachTriangle(data, (a, b, c) -> {
            for (final Vector3f point : List.of(a, b, c)) {
                low.min(point);
                high.max(point);
            }
        });
        return high.sub(low);
    }

    private static double area(final ModelData data) {
        final double[] sum = {0};
        forEachTriangle(data, (a, b, c) -> sum[0] += new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a))
                .length() / 2);
        return sum[0];
    }

    /* Every triangle faces away from the middle of the model, as a closed solid's outside does. */
    private static void assertOutward(final ModelData data) {
        final Vector3f low = new Vector3f(Float.MAX_VALUE);
        final Vector3f high = new Vector3f(-Float.MAX_VALUE);
        forEachTriangle(data, (a, b, c) -> {
            for (final Vector3f point : List.of(a, b, c)) {
                low.min(point);
                high.max(point);
            }
        });
        final Vector3f middle = new Vector3f(low).add(high).mul(0.5F);
        final int[] inward = {0};
        final int[] all = {0};
        forEachTriangle(data, (a, b, c) -> {
            final Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
            if (normal.lengthSquared() < 1e-14) {
                return;
            }
            all[0]++;
            final Vector3f centre = new Vector3f(a).add(b).add(c).div(3).sub(middle);
            if (normal.dot(centre) < 0) {
                inward[0]++;
            }
        });
        assertEquals(0, inward[0], inward[0] + " of " + all[0] + " triangles face inward");
    }

    private static void forEachTriangle(final ModelData data, final TriangleVisitor visitor) {
        final ModelPose pose = new ModelPose(data);
        final Matrix4f[] world = new Matrix4f[data.bones().size()];
        for (int i = 0; i < world.length; i++) {
            world[i] = new Matrix4f();
        }
        pose.world(world);
        for (int bone = 0; bone < world.length; bone++) {
            for (final ModelMesh mesh : data.bones().get(bone).meshes()) {
                final float[] p = mesh.positions();
                final int[] t = mesh.triangles();
                for (int i = 0; i < t.length; i += 3) {
                    final Vector3f a = world[bone].transformPosition(new Vector3f(p[t[i] * 3], p[t[i] * 3 + 1],
                            p[t[i] * 3 + 2]));
                    final Vector3f b = world[bone].transformPosition(new Vector3f(p[t[i + 1] * 3],
                            p[t[i + 1] * 3 + 1], p[t[i + 1] * 3 + 2]));
                    final Vector3f c = world[bone].transformPosition(new Vector3f(p[t[i + 2] * 3],
                            p[t[i + 2] * 3 + 1], p[t[i + 2] * 3 + 2]));
                    visitor.visit(a, b, c);
                }
            }
        }
    }

    private static String ref(final int id) {
        return "#" + id;
    }

    private static String list(final int... ids) {
        final StringBuilder out = new StringBuilder("(");
        for (int i = 0; i < ids.length; i++) {
            out.append(i > 0 ? "," : "").append(ref(ids[i]));
        }
        return out.append(")").toString();
    }

    private static String number(final double value) {
        final String written = String.format(Locale.ROOT, "%.6f", value);
        return written.contains(".") ? written : written + ".";
    }

    private interface TriangleVisitor {

        void visit(Vector3f a, Vector3f b, Vector3f c);
    }

    /* Writes a STEP file an instance at a time, numbering them, in millimetres. */
    private static final class Writer {

        private final StringBuilder data = new StringBuilder();
        private final Map<Long, int[]> edges = new HashMap<>();
        private final Map<Integer, double[]> places = new HashMap<>();
        private int next = 1;
        private int lastSolid;

        int add(final String instance) {
            final int id = this.next++;
            this.data.append('#').append(id).append('=').append(instance).append(";\n");
            return id;
        }

        void raw(final String instances) {
            this.data.append(instances);
            int highest = 0;
            for (final String line : instances.split("\n")) {
                final String trimmed = line.strip();
                if (trimmed.startsWith("#") && trimmed.indexOf('=') > 1) {
                    highest = Math.max(highest, Integer.parseInt(trimmed.substring(1, trimmed.indexOf('='))));
                    if (trimmed.contains("MANIFOLD_SOLID_BREP")) {
                        this.lastSolid = Integer.parseInt(trimmed.substring(1, trimmed.indexOf('=')));
                    }
                }
            }
            this.next = highest + 1;
        }

        int lastSolid() {
            return this.lastSolid;
        }

        int point(final double[] xyz) {
            return add("CARTESIAN_POINT('',(" + number(xyz[0]) + "," + number(xyz[1]) + "," + number(xyz[2]) + "))");
        }

        int direction(final double[] xyz) {
            return add("DIRECTION('',(" + number(xyz[0]) + "," + number(xyz[1]) + "," + number(xyz[2]) + "))");
        }

        int vertex(final double[] xyz) {
            final int vertex = add("VERTEX_POINT(''," + ref(point(xyz)) + ")");
            this.places.put(vertex, xyz);
            return vertex;
        }

        String placement(final double[] origin, final double[] axis) {
            final double[] along = Math.abs(axis[0]) > 0.9 ? new double[] {0, 1, 0} : new double[] {1, 0, 0};
            return placement(origin, axis, along);
        }

        String placement(final double[] origin, final double[] axis, final double[] along) {
            return ref(add("AXIS2_PLACEMENT_3D(''," + ref(point(origin)) + "," + ref(direction(axis)) + ","
                    + ref(direction(along)) + ")"));
        }

        int line(final double[] from, final double[] to) {
            final double[] way = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
            final double length = Math.sqrt(way[0] * way[0] + way[1] * way[1] + way[2] * way[2]);
            final int vector = add("VECTOR(''," + ref(direction(new double[] {way[0] / length, way[1] / length,
                way[2] / length})) + "," + number(length) + ")");
            return add("LINE(''," + ref(point(from)) + "," + ref(vector) + ")");
        }

        int curve(final int a, final int b, final int c) {
            return add("B_SPLINE_CURVE_WITH_KNOTS('',2," + list(a, b, c)
                    + ",.UNSPECIFIED.,.F.,.F.,(3,3),(0.,1.),.UNSPECIFIED.)");
        }

        int edge(final int from, final int to, final int curve) {
            return add("EDGE_CURVE(''," + ref(from) + "," + ref(to) + "," + ref(curve) + ",.T.)");
        }

        int oriented(final int edge, final boolean forward) {
            return add("ORIENTED_EDGE('',*,*," + ref(edge) + "," + (forward ? ".T." : ".F.") + ")");
        }

        int loop(final int... oriented) {
            return add("EDGE_LOOP(''," + list(oriented) + ")");
        }

        int face(final List<Integer> outer, final List<Integer> inner, final int surface) {
            final List<Integer> bounds = new ArrayList<>();
            for (final int loop : outer) {
                bounds.add(add("FACE_OUTER_BOUND(''," + ref(loop) + ",.T.)"));
            }
            for (final int loop : inner) {
                bounds.add(add("FACE_BOUND(''," + ref(loop) + ",.T.)"));
            }
            return add("ADVANCED_FACE(''," + list(bounds.stream().mapToInt(Integer::intValue).toArray()) + ","
                    + ref(surface) + ",.T.)");
        }

        /* A flat face through vertices in order, counter-clockwise seen from outside, each edge made once. */
        int planeFace(final int[] vertices, final double[] origin, final double[] normal) {
            final int[] oriented = new int[vertices.length];
            for (int i = 0; i < vertices.length; i++) {
                final int from = vertices[i];
                final int to = vertices[(i + 1) % vertices.length];
                final long key = Math.min(from, to) * 100_000L + Math.max(from, to);
                int[] edge = this.edges.get(key);
                if (edge == null) {
                    edge = new int[] {edge(from, to, line(this.places.get(from), this.places.get(to))), from};
                    this.edges.put(key, edge);
                }
                oriented[i] = oriented(edge[0], edge[1] == from);
            }
            final int plane = add("PLANE(''," + placement(origin, normal) + ")");
            return face(List.of(loop(oriented)), List.of(), plane);
        }

        int solid(final List<Integer> faces) {
            final int shell = add("CLOSED_SHELL(''," + list(faces.stream().mapToInt(Integer::intValue).toArray())
                    + ")");
            this.lastSolid = add("MANIFOLD_SOLID_BREP(''," + ref(shell) + ")");
            return this.lastSolid;
        }

        int product(final String name, final int shape) {
            final int context = add("PRODUCT_CONTEXT('',$,'mechanical')");
            final int product = add("PRODUCT('" + name + "','" + name + "',''," + list(context) + ")");
            final int formation = add("PRODUCT_DEFINITION_FORMATION('',''," + ref(product) + ")");
            final int definition = add("PRODUCT_DEFINITION('design',''," + ref(formation) + ",$)");
            final int definitionShape = add("PRODUCT_DEFINITION_SHAPE('',''," + ref(definition) + ")");
            add("SHAPE_DEFINITION_REPRESENTATION(" + ref(definitionShape) + "," + ref(shape) + ")");
            return definition;
        }

        String text() {
            return "ISO-10303-21;\nHEADER;\nFILE_DESCRIPTION((''),'2;1');\nFILE_NAME('test','',(''),(''),'','','');"
                    + "\nFILE_SCHEMA(('AUTOMOTIVE_DESIGN'));\nENDSEC;\nDATA;\n#999999=(LENGTH_UNIT() NAMED_UNIT(*)"
                    + " SI_UNIT(.MILLI.,.METRE.));\n" + this.data + "ENDSEC;\nEND-ISO-10303-21;\n";
        }
    }
}
