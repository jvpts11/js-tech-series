/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import dev.jstech.core.model.StepGeometry.Surface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3d;

/**
 * Lays one face of a STEP solid with triangles on its surface: its loops taken onto the surface's two parameters, a
 * surface that comes round (a cylinder, a sphere, a torus) unrolled so each loop runs on without a jump, the second
 * pass of a seam put a turn from the first, a pole's points given the turn of their neighbours; a loop that goes once
 * round closed by the other loop round, or by the pole the face reaches; and the region between laid with triangles
 * in the surface's own lengths, with points inside where it curves ({@link UvTriangulator}). The corners on the face's
 * edges are the edges' own points, so faces either side of an edge meet. Pure.
 */
final class FaceLayout {

    private final Surface surface;
    private final double tolerance;
    private final double periodU;
    private final double periodV;

    private static final double WRAPS = 0.25;
    private static final int MOST_INSIDE = 24;
    private static final double CLEAR = 0.35;
    private static final double LEAST_SCALE = 1e-9;

    private FaceLayout(final Surface surface, final double tolerance) {
        this.surface = surface;
        this.tolerance = tolerance;
        this.periodU = surface.periodU();
        this.periodV = surface.periodV();
    }

    /**
     * Lays a face; whether it could be.
     *
     * @param loops       the points of each of its loops, in order
     * @param edges       for each loop, the edge each of its points came from, -1 for none; empty for none known
     * @param pieces      for each loop, which of its edges each point is on, counted along it; empty for none
     * @param directions  for each loop, whether it runs the way its face's bound says, as written
     * @param sameSense   whether the face faces the way its surface's normal does
     * @param into        where its triangles go, counter-clockwise seen from the side it faces
     */
    static boolean lay(final Surface surface, final List<List<Vector3d>> loops, final List<List<Integer>> edges,
                       final List<List<Integer>> pieces, final List<Boolean> directions, final double tolerance,
                       final boolean sameSense, final ISink into) {
        return new FaceLayout(surface, tolerance).run(loops, edges, pieces, directions, sameSense, into);
    }

    private boolean run(final List<List<Vector3d>> loops, final List<List<Integer>> edges,
                        final List<List<Integer>> pieces, final List<Boolean> directions, final boolean sameSense,
                        final ISink into) {
        final List<double[]> flat = new ArrayList<>();
        final List<List<Vector3d>> points = new ArrayList<>();
        final List<Unrolled> unrolled = new ArrayList<>();
        for (int i = 0; i < loops.size(); i++) {
            final Unrolled loop = poles(unroll(loops.get(i), i < edges.size() ? edges.get(i) : List.of(),
                    i < pieces.size() ? pieces.get(i) : List.of()));
            unrolled.add(loop);
            flat.add(loop.uv());
            points.add(loop.points());
        }
        final List<double[]> rings = new ArrayList<>();
        final List<List<Vector3d>> ringPoints = new ArrayList<>();
        final List<Integer> wrapsU = new ArrayList<>();
        final List<Integer> wrapsV = new ArrayList<>();
        for (int i = 0; i < flat.size(); i++) {
            final Unrolled loop = unrolled.get(i);
            if (this.periodU > 0 && wraps(loop, 0, this.periodU)) {
                wrapsU.add(i);
            } else if (this.periodV > 0 && wraps(loop, 1, this.periodV)) {
                wrapsV.add(i);
            } else {
                rings.add(loop.uv());
                ringPoints.add(loop.points());
            }
        }
        if (!closeWrapped(wrapsU, 0, flat, points, directions, sameSense, rings, ringPoints)
                || !closeWrapped(wrapsV, 1, flat, points, directions, sameSense, rings, ringPoints)) {
            return false;
        }
        if (rings.isEmpty()) {
            return false;
        }
        align(rings, 0, this.periodU);
        align(rings, 1, this.periodV);
        return triangulate(rings, ringPoints, sameSense, into);
    }

    /*
     * A loop's points on the surface's parameters, each turn put so it runs on from the one before: the second pass
     * of a seam a whole turn from the first, and a pole's points at the turn of the point before.
     */
    private Unrolled unroll(final List<Vector3d> loop, final List<Integer> edgeOf, final List<Integer> pieceOf) {
        final int count = loop.size();
        final double[] out = new double[count * 2];
        final boolean[] singular = new boolean[count];
        final Map<Integer, double[]> firstPass = new HashMap<>();
        final Map<Integer, Integer> passes = new HashMap<>();
        for (final Integer edge : edgeOf) {
            if (edge >= 0) {
                passes.merge(edge, 1, Integer::sum);
            }
        }
        double[] previous = null;
        int i = 0;
        while (i < count) {
            final int edge = i < edgeOf.size() ? edgeOf.get(i) : -1;
            final int piece = i < pieceOf.size() ? pieceOf.get(i) : 0;
            int end = i;
            while (end < count && (end < edgeOf.size() ? edgeOf.get(end) : -1) == edge
                    && (end < pieceOf.size() ? pieceOf.get(end) : 0) == piece) {
                end++;
            }
            for (int k = i; k < end; k++) {
                final double[] raw = this.surface.parametersOf(loop.get(k), previous);
                singular[k] = this.surface.singular(loop.get(k));
                if (singular[k] && previous != null) {
                    raw[0] = previous[0];
                }
                if (previous != null) {
                    raw[0] = nearest(raw[0], previous[0], this.periodU);
                    raw[1] = nearest(raw[1], previous[1], this.periodV);
                }
                out[k * 2] = raw[0];
                out[k * 2 + 1] = raw[1];
                previous = raw;
            }
            if (edge >= 0 && passes.getOrDefault(edge, 0) > 1) {
                final double[] mean = mean(out, i, end);
                final double[] first = firstPass.get(edge);
                if (first == null) {
                    firstPass.put(edge, mean);
                } else {
                    // The seam's second pass, lying where its first does: a turn either side, toward the rest.
                    shiftSeam(out, i, end, mean, first, edgeOf, passes);
                    previous = new double[] {out[(end - 1) * 2], out[(end - 1) * 2 + 1]};
                }
            }
            i = Math.max(end, i + 1);
        }
        // A pole the loop starts at takes the turn of the first point after it.
        for (int k = 0; k < count && singular[k]; k++) {
            for (int next = k + 1; next < count; next++) {
                if (!singular[next]) {
                    out[k * 2] = out[next * 2];
                    break;
                }
            }
        }
        return new Unrolled(out, new ArrayList<>(loop), singular);
    }

    /*
     * A pole reached at one turn and left at another: the pole put at both, so the loop runs along it between, as
     * the edge of the face it is in the parameters; at the loop's close too.
     */
    private Unrolled poles(final Unrolled loop) {
        final int count = loop.points().size();
        final List<double[]> uv = new ArrayList<>(count + 2);
        final List<Vector3d> points = new ArrayList<>(count + 2);
        final List<Boolean> singular = new ArrayList<>(count + 2);
        for (int k = 0; k < count; k++) {
            final double u = loop.uv()[k * 2];
            final double v = loop.uv()[k * 2 + 1];
            if (loop.singular()[k] && k > 0 && Math.abs(uv.getLast()[0] - u) > 1e-9) {
                uv.add(new double[] {uv.getLast()[0], v});
                points.add(loop.points().get(k));
                singular.add(true);
            }
            uv.add(new double[] {u, v});
            points.add(loop.points().get(k));
            singular.add(loop.singular()[k]);
        }
        if (count > 1 && loop.singular()[0] && Math.abs(uv.getLast()[0] - uv.getFirst()[0]) > 1e-9) {
            uv.add(new double[] {uv.getLast()[0], uv.getFirst()[1]});
            points.add(loop.points().getFirst());
            singular.add(true);
        }
        final double[] out = new double[uv.size() * 2];
        final boolean[] flags = new boolean[singular.size()];
        for (int k = 0; k < uv.size(); k++) {
            out[k * 2] = uv.get(k)[0];
            out[k * 2 + 1] = uv.get(k)[1];
            flags[k] = singular.get(k);
        }
        return new Unrolled(out, points, flags);
    }

    /*
     * Puts a seam's second pass a turn from its first, whichever way the rest of the loop lies; a pass already a turn
     * from its first in either parameter, as a torus's seams come out, is left where it is.
     */
    private void shiftSeam(final double[] out, final int from, final int to, final double[] mean,
                           final double[] first, final List<Integer> edgeOf, final Map<Integer, Integer> passes) {
        for (int axis = 0; axis < 2; axis++) {
            final double period = axis == 0 ? this.periodU : this.periodV;
            if (period > 0 && Math.abs(mean[axis] - first[axis]) > period / 2) {
                return;
            }
        }
        for (int axis = 0; axis < 2; axis++) {
            final double period = axis == 0 ? this.periodU : this.periodV;
            if (period <= 0 || Math.abs(mean[axis] - first[axis]) > period / 2) {
                continue;
            }
            double sum = 0;
            int counted = 0;
            for (int k = 0; k < out.length / 2; k++) {
                final int edge = k < edgeOf.size() ? edgeOf.get(k) : -1;
                if (edge < 0 || passes.getOrDefault(edge, 0) < 2) {
                    sum += out[k * 2 + axis];
                    counted++;
                }
            }
            final double rest = counted == 0 ? first[axis] - 1 : sum / counted;
            final double shift = first[axis] > rest ? -period : period;
            for (int k = from; k < to; k++) {
                out[k * 2 + axis] += shift;
            }
            // Only the parameter the seam lies across is moved.
            return;
        }
    }

    /*
     * Whether a loop goes once round in {@code axis}: its steps adding up to a turn, the closing one taken the short
     * way round, unless it closes at a pole, where any turn is the same point.
     */
    private static boolean wraps(final Unrolled loop, final int axis, final double period) {
        final double[] uv = loop.uv();
        final int count = uv.length / 2;
        if (count < 2) {
            return false;
        }
        double sum = 0;
        for (int k = 0; k < count; k++) {
            final double from = uv[k * 2 + axis];
            final double to = uv[((k + 1) % count) * 2 + axis];
            double step = to - from;
            if (k == count - 1 && !loop.singular()[0] && !loop.singular()[count - 1]) {
                step -= Math.rint(step / period) * period;
            }
            sum += step;
        }
        return Math.abs(Math.abs(sum) - period) < period * WRAPS;
    }

    /*
     * Closes the loops that go once round in {@code axis}: two of them into the band between; one by the pole the
     * face reaches; any more are left out.
     */
    private boolean closeWrapped(final List<Integer> wrapped, final int axis, final List<double[]> flat,
                                 final List<List<Vector3d>> loops, final List<Boolean> directions,
                                 final boolean sameSense, final List<double[]> rings,
                                 final List<List<Vector3d>> ringPoints) {
        if (wrapped.isEmpty()) {
            return true;
        }
        final double period = axis == 0 ? this.periodU : this.periodV;
        final int other = 1 - axis;
        final Cut first = cut(flat.get(wrapped.getFirst()), loops.get(wrapped.getFirst()), axis, period, Double.NaN);
        if (wrapped.size() >= 2) {
            final Cut second = cut(flat.get(wrapped.get(1)), loops.get(wrapped.get(1)), axis, period, first.start);
            final double[] ring = new double[first.uv.length + second.uv.length];
            System.arraycopy(first.uv, 0, ring, 0, first.uv.length);
            final List<Vector3d> points = new ArrayList<>(first.points);
            // The other loop backward, so the ring runs round the band and back.
            for (int k = second.uv.length / 2 - 1, at = first.uv.length; k >= 0; k--, at += 2) {
                ring[at] = second.uv[k * 2];
                ring[at + 1] = second.uv[k * 2 + 1];
                points.add(second.points.get(k));
            }
            rings.add(ring);
            ringPoints.add(points);
            return true;
        }
        final Double pole = pole(axis, flat.get(wrapped.getFirst()), directions.isEmpty() ? true
                : directions.get(wrapped.getFirst()), sameSense);
        if (pole == null) {
            return false;
        }
        final int count = first.uv.length / 2;
        final double[] ring = new double[first.uv.length + 4];
        System.arraycopy(first.uv, 0, ring, 0, first.uv.length);
        final double end = first.uv[(count - 1) * 2 + axis];
        ring[count * 2 + axis] = end;
        ring[count * 2 + other] = pole;
        ring[count * 2 + 2 + axis] = first.start;
        ring[count * 2 + 2 + other] = pole;
        final List<Vector3d> points = new ArrayList<>(first.points);
        points.add(at(axis, end, pole));
        points.add(at(axis, first.start, pole));
        rings.add(ring);
        ringPoints.add(points);
        return true;
    }

    /*
     * The other parameter at the pole a face reaches past a loop that goes once round: a sphere's top or bottom, a
     * cone's tip, as the loop runs and the face faces; none for a surface with no pole there.
     */
    private Double pole(final int axis, final double[] uv, final boolean direction, final boolean sameSense) {
        double sum = 0;
        final int count = uv.length / 2;
        for (int k = 0; k + 1 < count; k++) {
            sum += uv[(k + 1) * 2 + axis] - uv[k * 2 + axis];
        }
        // A loop seen from the side its face faces has its face on its left: going up the turn, the face is above.
        final boolean above = sum > 0 == direction == sameSense;
        if (this.surface instanceof StepGeometry.Sphere && axis == 0) {
            return above ? Math.PI / 2 : -Math.PI / 2;
        }
        if (this.surface instanceof StepGeometry.Cone cone && axis == 0 && Math.tan(cone.angle()) != 0) {
            return -cone.radius() / Math.tan(cone.angle());
        }
        return null;
    }

    /* A point of the surface at a turn {@code along} of {@code axis} and {@code across} of the other. */
    private Vector3d at(final int axis, final double along, final double across) {
        return axis == 0 ? this.surface.at(along, across) : this.surface.at(across, along);
    }

    /*
     * A loop that goes once round, cut where its turn is {@code start} (its own first point's for none), running up
     * the turn from there a whole turn on, with a point made where it crosses the cut.
     */
    private Cut cut(final double[] uv, final List<Vector3d> points, final int axis, final double period,
                    final double start) {
        final int count = uv.length / 2;
        double sum = 0;
        for (int k = 0; k + 1 < count; k++) {
            sum += uv[(k + 1) * 2 + axis] - uv[k * 2 + axis];
        }
        // Running up the turn: a loop that runs down it is read backward.
        final double[] up = new double[uv.length];
        final List<Vector3d> upPoints = new ArrayList<>(count);
        for (int k = 0; k < count; k++) {
            final int from = sum >= 0 ? k : count - 1 - k;
            up[k * 2] = uv[from * 2];
            up[k * 2 + 1] = uv[from * 2 + 1];
            upPoints.add(points.get(from));
        }
        final double cutAt = Double.isNaN(start) ? up[axis] : start;
        // Every point a turn on or back so it lies from the cut to a turn past it.
        final List<double[]> laid = new ArrayList<>();
        final List<Vector3d> laidPoints = new ArrayList<>();
        int startIndex = 0;
        double lowest = Double.MAX_VALUE;
        for (int k = 0; k < count; k++) {
            double value = up[k * 2 + axis];
            value = cutAt + ((value - cutAt) % period + period) % period;
            if (value - cutAt < lowest) {
                lowest = value - cutAt;
                startIndex = k;
            }
        }
        for (int k = 0; k < count; k++) {
            final int index = (startIndex + k) % count;
            double value = up[index * 2 + axis];
            value = cutAt + ((value - cutAt) % period + period) % period;
            if (k > 0 && value < laid.getLast()[axis] - period / 2) {
                value += period;
            }
            final double[] point = new double[2];
            point[axis] = value;
            point[1 - axis] = up[index * 2 + 1 - axis];
            laid.add(point);
            laidPoints.add(upPoints.get(index));
        }
        // The two ends at the cut and a turn past it, the other parameter between the points either side.
        final double[] last = laid.getLast();
        final double[] firstPoint = laid.getFirst();
        final double span = firstPoint[axis] + period - last[axis];
        final double share = span <= 0 ? 0 : (cutAt + period - last[axis]) / span;
        final double across = last[1 - axis] + (firstPoint[1 - axis] - last[1 - axis]) * share;
        final List<double[]> ends = new ArrayList<>();
        final List<Vector3d> endPoints = new ArrayList<>();
        if (firstPoint[axis] - cutAt > 1e-12) {
            final double[] head = new double[2];
            head[axis] = cutAt;
            head[1 - axis] = across;
            ends.add(head);
            endPoints.add(at(axis, cutAt, across));
        }
        ends.addAll(laid);
        endPoints.addAll(laidPoints);
        final double[] tail = new double[2];
        tail[axis] = cutAt + period;
        tail[1 - axis] = across;
        ends.add(tail);
        endPoints.add(at(axis, cutAt + period, across));
        final double[] flat = new double[ends.size() * 2];
        for (int k = 0; k < ends.size(); k++) {
            flat[k * 2] = ends.get(k)[0];
            flat[k * 2 + 1] = ends.get(k)[1];
        }
        return new Cut(flat, endPoints, cutAt);
    }

    /* Moves every ring a whole turn at a time so all lie in the first's turn. */
    private static void align(final List<double[]> rings, final int axis, final double period) {
        if (period <= 0 || rings.size() < 2) {
            return;
        }
        final double reference = mean(rings.getFirst(), 0, rings.getFirst().length / 2)[axis];
        for (int r = 1; r < rings.size(); r++) {
            final double[] ring = rings.get(r);
            final double centre = mean(ring, 0, ring.length / 2)[axis];
            final double shift = Math.rint((reference - centre) / period) * period;
            if (shift != 0) {
                for (int k = 0; k < ring.length / 2; k++) {
                    ring[k * 2 + axis] += shift;
                }
            }
        }
    }

    /* Lays the region the rings bound with triangles, in the surface's lengths, and hands them over. */
    private boolean triangulate(final List<double[]> rings, final List<List<Vector3d>> ringPoints,
                                final boolean sameSense, final ISink into) {
        double lowU = Double.MAX_VALUE;
        double highU = -Double.MAX_VALUE;
        double lowV = Double.MAX_VALUE;
        double highV = -Double.MAX_VALUE;
        for (final double[] ring : rings) {
            for (int k = 0; k < ring.length / 2; k++) {
                lowU = Math.min(lowU, ring[k * 2]);
                highU = Math.max(highU, ring[k * 2]);
                lowV = Math.min(lowV, ring[k * 2 + 1]);
                highV = Math.max(highV, ring[k * 2 + 1]);
            }
        }
        final double middleU = (lowU + highU) / 2;
        final double middleV = (lowV + highV) / 2;
        final double scaleU = scale(middleU, middleV, Math.max(highU - lowU, 1e-6), true);
        final double scaleV = scale(middleU, middleV, Math.max(highV - lowV, 1e-6), false);
        final List<double[]> scaled = new ArrayList<>(rings.size());
        for (final double[] ring : rings) {
            final double[] out = new double[ring.length];
            for (int k = 0; k < ring.length / 2; k++) {
                out[k * 2] = ring[k * 2] * scaleU;
                out[k * 2 + 1] = ring[k * 2 + 1] * scaleV;
            }
            scaled.add(out);
        }
        final double[] inside = inside(scaled, lowU, highU, lowV, highV, scaleU, scaleV);
        final UvTriangulator.Result laid = UvTriangulator.triangulate(scaled, inside, this.tolerance * 1e-3);
        if (laid.triangles().length == 0) {
            return false;
        }
        final List<Vector3d> sources = new ArrayList<>();
        ringPoints.forEach(sources::addAll);
        final int count = laid.points().length / 2;
        final Vector3d[] corners = new Vector3d[count];
        for (int k = 0; k < count; k++) {
            final int source = laid.sources()[k];
            corners[k] = source >= 0 && source < sources.size() ? sources.get(source)
                    : this.surface.at(laid.points()[k * 2] / scaleU, laid.points()[k * 2 + 1] / scaleV);
        }
        final int[] triangles = laid.triangles();
        for (int t = 0; t < triangles.length; t += 3) {
            final Vector3d a = corners[triangles[t]];
            final Vector3d b = corners[triangles[t + 1]];
            final Vector3d c = corners[triangles[t + 2]];
            if (sameSense) {
                into.triangle(a, b, c);
            } else {
                into.triangle(a, c, b);
            }
        }
        return true;
    }

    /* How long a step of a parameter is on the surface about the middle of the face, in the file's unit. */
    private double scale(final double u, final double v, final double range, final boolean alongU) {
        final double h = range * 1e-3;
        final Vector3d a = alongU ? this.surface.at(u - h, v) : this.surface.at(u, v - h);
        final Vector3d b = alongU ? this.surface.at(u + h, v) : this.surface.at(u, v + h);
        final double measured = a.distance(b) / (2 * h);
        if (measured > LEAST_SCALE) {
            return measured;
        }
        // At a pole a parameter's step goes nowhere; the face is measured a little way off it.
        final double offset = range / 4;
        final Vector3d c = alongU ? this.surface.at(u - h, v + offset) : this.surface.at(u + offset, v - h);
        final Vector3d d = alongU ? this.surface.at(u + h, v + offset) : this.surface.at(u + offset, v + h);
        return Math.max(LEAST_SCALE, c.distance(d) / (2 * h));
    }

    /* Points inside a curved face, a grid as fine as its curve asks, clear of its edges. */
    private double[] inside(final List<double[]> scaled, final double lowU, final double highU, final double lowV,
                            final double highV, final double scaleU, final double scaleV) {
        if (this.surface.flat()) {
            return new double[0];
        }
        final double[] spacing = this.surface.spacing(this.tolerance);
        double stepU = spacing[0] * scaleU;
        double stepV = spacing[1] * scaleV;
        if (stepU <= 0 && stepV <= 0) {
            return new double[0];
        }
        if (stepU <= 0) {
            stepU = stepV;
        }
        if (stepV <= 0) {
            stepV = stepU;
        }
        final double widthU = (highU - lowU) * scaleU;
        final double widthV = (highV - lowV) * scaleV;
        final int countU = (int) Math.min(MOST_INSIDE, Math.ceil(widthU / stepU));
        final int countV = (int) Math.min(MOST_INSIDE, Math.ceil(widthV / stepV));
        if (countU < 2 && countV < 2) {
            return new double[0];
        }
        final double clear = CLEAR * Math.min(widthU / Math.max(1, countU), widthV / Math.max(1, countV));
        final FloatList out = new FloatList();
        for (int i = 1; i < countU; i++) {
            for (int j = 1; j < countV; j++) {
                final double x = lowU * scaleU + widthU * i / countU;
                final double y = lowV * scaleV + widthV * j / countV;
                if (clearOf(scaled, x, y, clear)) {
                    out.add((float) x);
                    out.add((float) y);
                }
            }
        }
        final float[] points = out.toArray();
        final double[] doubles = new double[points.length];
        for (int k = 0; k < points.length; k++) {
            doubles[k] = points[k];
        }
        return doubles;
    }

    /* Whether a point is at least {@code clear} from every edge of every ring. */
    private static boolean clearOf(final List<double[]> rings, final double x, final double y, final double clear) {
        final double least = clear * clear;
        for (final double[] ring : rings) {
            final int count = ring.length / 2;
            for (int k = 0; k < count; k++) {
                final double ax = ring[k * 2];
                final double ay = ring[k * 2 + 1];
                final double bx = ring[((k + 1) % count) * 2];
                final double by = ring[((k + 1) % count) * 2 + 1];
                final double dx = bx - ax;
                final double dy = by - ay;
                final double length = dx * dx + dy * dy;
                final double share = length == 0 ? 0 : Math.clamp(((x - ax) * dx + (y - ay) * dy) / length, 0, 1);
                final double nx = ax + dx * share - x;
                final double ny = ay + dy * share - y;
                if (nx * nx + ny * ny < least) {
                    return false;
                }
            }
        }
        return true;
    }

    /* The value of a parameter a whole turn at a time nearest {@code near}; as it is for one that does not turn. */
    private static double nearest(final double value, final double near, final double period) {
        if (period <= 0) {
            return value;
        }
        return value + Math.rint((near - value) / period) * period;
    }

    private static double[] mean(final double[] uv, final int from, final int to) {
        double u = 0;
        double v = 0;
        for (int k = from; k < to; k++) {
            u += uv[k * 2];
            v += uv[k * 2 + 1];
        }
        final int count = Math.max(1, to - from);
        return new double[] {u / count, v / count};
    }

    /* A loop that goes once round, laid from its cut a turn on, its points, and where the cut is. */
    private record Cut(double[] uv, List<Vector3d> points, double start) {
    }

    /* A loop on the surface's parameters: its points there, in the world, and which are at a pole. */
    private record Unrolled(double[] uv, List<Vector3d> points, boolean[] singular) {
    }

    /** Where a face's triangles go. */
    interface ISink {

        /** A triangle, its corners in the file's unit, counter-clockwise seen from the side it faces. */
        void triangle(Vector3d a, Vector3d b, Vector3d c);
    }
}
