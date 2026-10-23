/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.List;
import org.joml.Vector3d;

/**
 * The curves and surfaces of a STEP solid, as the exporting program left them, worked out where they are: a curve at
 * a parameter and the parameter of a point on it; a surface at two parameters and the two of a point on it, as its
 * definition in ISO 10303-42 has them, each turned so its normal is the one the standard gives it. Lines, circles,
 * ellipses, polylines and B-spline curves, rational or not; planes, cylinders, cones, spheres, tori, B-spline surfaces,
 * rational or not, surfaces of revolution and of linear extrusion. Pure.
 */
final class StepGeometry {

    private static final double TURN = 2 * Math.PI;
    private static final int SEARCH = 24;
    private static final int NEWTON = 12;
    private static final double STEP = 1e-6;

    private StepGeometry() {
    }

    /**
     * A place and its axes, as AXIS2_PLACEMENT_3D writes them: its X and Z axes as given, square to each other, and
     * Y across them.
     */
    record Frame(Vector3d origin, Vector3d x, Vector3d y, Vector3d z) {

        /** The frame of the world itself. */
        static Frame world() {
            return new Frame(new Vector3d(), new Vector3d(1, 0, 0), new Vector3d(0, 1, 0), new Vector3d(0, 0, 1));
        }

        /** A frame at {@code origin} with {@code z} up and {@code x} along, both perhaps not square. */
        static Frame of(final Vector3d origin, final Vector3d z, final Vector3d xWanted) {
            final Vector3d up = new Vector3d(z).normalize();
            Vector3d along = new Vector3d(xWanted).sub(new Vector3d(up).mul(up.dot(xWanted)));
            if (along.lengthSquared() < 1e-20) {
                along = Math.abs(up.x) < 0.9 ? new Vector3d(1, 0, 0) : new Vector3d(0, 1, 0);
                along.sub(new Vector3d(up).mul(up.dot(along)));
            }
            along.normalize();
            return new Frame(new Vector3d(origin), along, new Vector3d(up).cross(along), up);
        }

        /** Where a point given in the frame is in the world. */
        Vector3d at(final double a, final double b, final double c) {
            return new Vector3d(this.origin).fma(a, this.x).fma(b, this.y).fma(c, this.z);
        }
    }

    /** A curve, at a parameter, and the parameter of a point on it. */
    sealed interface Curve permits Line, Circle, Ellipse, Polyline, BSplineCurve {

        Vector3d at(double t);

        /** The parameter of the point on it nearest {@code point}. */
        double parameterOf(Vector3d point);

        /** How long a turn of its parameter is, for one that comes round; 0 for one that does not. */
        default double period() {
            return 0;
        }

        /** How many pieces it is drawn in from {@code from} to {@code to}, as fine as {@code tolerance} asks. */
        int pieces(double from, double to, double tolerance);
    }

    /** A surface at two parameters, and the two of a point on it. */
    sealed interface Surface permits Plane, Cylinder, Cone, Sphere, Torus, BSplineSurface, Revolution, Extrusion {

        Vector3d at(double u, double v);

        /** The parameters of the point on it nearest {@code point}, starting from {@code near} when there is one. */
        double[] parametersOf(Vector3d point, double[] near);

        /** How long a turn of U is, for a surface that comes round in U; 0 for one that does not. */
        default double periodU() {
            return 0;
        }

        /** How long a turn of V is. */
        default double periodV() {
            return 0;
        }

        /** Whether it is flat, so a face on it needs no points inside. */
        default boolean flat() {
            return false;
        }

        /** How far across its faces are laid with points inside, U then V, in its parameters; 0 for no need. */
        double[] spacing(double tolerance);

        /** Whether {@code point} is where one of its parameters says nothing, as a sphere's pole. */
        default boolean singular(final Vector3d point) {
            return false;
        }
    }

    record Line(Vector3d origin, Vector3d direction) implements Curve {

        @Override
        public Vector3d at(final double t) {
            return new Vector3d(this.origin).fma(t, this.direction);
        }

        @Override
        public double parameterOf(final Vector3d point) {
            return new Vector3d(point).sub(this.origin).dot(this.direction) / this.direction.lengthSquared();
        }

        @Override
        public int pieces(final double from, final double to, final double tolerance) {
            return 1;
        }
    }

    record Circle(Frame frame, double radius) implements Curve {

        @Override
        public Vector3d at(final double t) {
            return this.frame.at(this.radius * Math.cos(t), this.radius * Math.sin(t), 0);
        }

        @Override
        public double parameterOf(final Vector3d point) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            return Math.atan2(d.dot(this.frame.y()), d.dot(this.frame.x()));
        }

        @Override
        public double period() {
            return TURN;
        }

        @Override
        public int pieces(final double from, final double to, final double tolerance) {
            return arcPieces(Math.abs(to - from), this.radius, tolerance);
        }
    }

    record Ellipse(Frame frame, double first, double second) implements Curve {

        @Override
        public Vector3d at(final double t) {
            return this.frame.at(this.first * Math.cos(t), this.second * Math.sin(t), 0);
        }

        @Override
        public double parameterOf(final Vector3d point) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            return Math.atan2(d.dot(this.frame.y()) / this.second, d.dot(this.frame.x()) / this.first);
        }

        @Override
        public double period() {
            return TURN;
        }

        @Override
        public int pieces(final double from, final double to, final double tolerance) {
            return arcPieces(Math.abs(to - from), Math.max(this.first, this.second), tolerance);
        }
    }

    /** A line through points, its parameter running 0 at the first, 1 at the next, and on. */
    record Polyline(List<Vector3d> points) implements Curve {

        @Override
        public Vector3d at(final double t) {
            final int last = this.points.size() - 1;
            final int at = (int) Math.clamp(Math.floor(t), 0, Math.max(0, last - 1));
            final double share = Math.clamp(t - at, 0, 1);
            return new Vector3d(this.points.get(at)).lerp(this.points.get(Math.min(last, at + 1)), share);
        }

        @Override
        public double parameterOf(final Vector3d point) {
            double best = 0;
            double nearest = Double.MAX_VALUE;
            for (int i = 0; i < this.points.size(); i++) {
                final double away = this.points.get(i).distanceSquared(point);
                if (away < nearest) {
                    nearest = away;
                    best = i;
                }
            }
            return best;
        }

        @Override
        public int pieces(final double from, final double to, final double tolerance) {
            return Math.max(1, (int) Math.round(Math.abs(to - from)));
        }
    }

    /**
     * A B-spline curve: its degree, its control points, their weights for a rational one, and its knots each written
     * as often as its multiplicity says.
     */
    record BSplineCurve(int degree, Vector3d[] points, double[] weights, double[] knots) implements Curve {

        @Override
        public Vector3d at(final double t) {
            final double[] basis = new double[this.degree + 1];
            final int span = span(this.knots, this.degree, this.points.length - 1, t);
            basis(this.knots, this.degree, span, t, basis);
            final Vector3d out = new Vector3d();
            double weight = 0;
            for (int k = 0; k <= this.degree; k++) {
                final int index = span - this.degree + k;
                final double w = basis[k] * (this.weights.length > 0 ? this.weights[index] : 1);
                out.fma(w, this.points[index]);
                weight += w;
            }
            return weight == 0 ? out : out.div(weight);
        }

        @Override
        public double parameterOf(final Vector3d point) {
            final double low = this.knots[this.degree];
            final double high = this.knots[this.points.length];
            final int samples = Math.max(SEARCH, this.points.length * 4);
            double best = low;
            double nearest = Double.MAX_VALUE;
            for (int i = 0; i <= samples; i++) {
                final double t = low + (high - low) * i / samples;
                final double away = at(t).distanceSquared(point);
                if (away < nearest) {
                    nearest = away;
                    best = t;
                }
            }
            // Narrowed down by halves either side of the nearest sample.
            double reach = (high - low) / samples;
            for (int i = 0; i < NEWTON * 2; i++) {
                final double left = Math.max(low, best - reach / 2);
                final double right = Math.min(high, best + reach / 2);
                final double atLeft = at(left).distanceSquared(point);
                final double atRight = at(right).distanceSquared(point);
                if (atLeft < nearest) {
                    nearest = atLeft;
                    best = left;
                } else if (atRight < nearest) {
                    nearest = atRight;
                    best = right;
                } else {
                    reach /= 2;
                }
            }
            return best;
        }

        @Override
        public int pieces(final double from, final double to, final double tolerance) {
            final double share = Math.abs(to - from) / Math.max(1e-12, this.knots[this.points.length]
                    - this.knots[this.degree]);
            return Math.max(2, (int) Math.ceil(share * (this.points.length - 1) * Math.max(2, this.degree) * 2));
        }
    }

    record Plane(Frame frame) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            return this.frame.at(u, v, 0);
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            return new double[] {d.dot(this.frame.x()), d.dot(this.frame.y())};
        }

        @Override
        public boolean flat() {
            return true;
        }

        @Override
        public double[] spacing(final double tolerance) {
            return new double[] {0, 0};
        }
    }

    record Cylinder(Frame frame, double radius) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            return this.frame.at(this.radius * Math.cos(u), this.radius * Math.sin(u), v);
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            return new double[] {Math.atan2(d.dot(this.frame.y()), d.dot(this.frame.x())), d.dot(this.frame.z())};
        }

        @Override
        public double periodU() {
            return TURN;
        }

        @Override
        public double[] spacing(final double tolerance) {
            return new double[] {arcStep(this.radius, tolerance), 0};
        }
    }

    /** A cone: its radius where its frame stands, opening by {@code angle} from its axis as it goes up. */
    record Cone(Frame frame, double radius, double angle) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            final double r = this.radius + v * Math.tan(this.angle);
            return this.frame.at(r * Math.cos(u), r * Math.sin(u), v);
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            return new double[] {Math.atan2(d.dot(this.frame.y()), d.dot(this.frame.x())), d.dot(this.frame.z())};
        }

        @Override
        public double periodU() {
            return TURN;
        }

        @Override
        public double[] spacing(final double tolerance) {
            return new double[] {arcStep(Math.max(this.radius, tolerance), tolerance), 0};
        }

        @Override
        public boolean singular(final Vector3d point) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            final double across = Math.hypot(d.dot(this.frame.x()), d.dot(this.frame.y()));
            return across < 1e-9 * Math.max(1, this.radius);
        }
    }

    record Sphere(Frame frame, double radius) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            return this.frame.at(this.radius * Math.cos(v) * Math.cos(u), this.radius * Math.cos(v) * Math.sin(u),
                    this.radius * Math.sin(v));
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            final double x = d.dot(this.frame.x());
            final double y = d.dot(this.frame.y());
            return new double[] {Math.atan2(y, x), Math.atan2(d.dot(this.frame.z()), Math.hypot(x, y))};
        }

        @Override
        public double periodU() {
            return TURN;
        }

        @Override
        public double[] spacing(final double tolerance) {
            final double step = arcStep(this.radius, tolerance);
            return new double[] {step, step};
        }

        @Override
        public boolean singular(final Vector3d point) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            return Math.hypot(d.dot(this.frame.x()), d.dot(this.frame.y())) < 1e-9 * Math.max(1, this.radius);
        }
    }

    /** A torus: the radius of its ring, {@code major}, and of its tube, {@code minor}. */
    record Torus(Frame frame, double major, double minor) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            final double r = this.major + this.minor * Math.cos(v);
            return this.frame.at(r * Math.cos(u), r * Math.sin(u), this.minor * Math.sin(v));
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            final Vector3d d = new Vector3d(point).sub(this.frame.origin());
            final double x = d.dot(this.frame.x());
            final double y = d.dot(this.frame.y());
            return new double[] {Math.atan2(y, x), Math.atan2(d.dot(this.frame.z()), Math.hypot(x, y) - this.major)};
        }

        @Override
        public double periodU() {
            return TURN;
        }

        @Override
        public double periodV() {
            return TURN;
        }

        @Override
        public double[] spacing(final double tolerance) {
            return new double[] {arcStep(this.major + this.minor, tolerance), arcStep(this.minor, tolerance)};
        }
    }

    /**
     * A B-spline surface: its degrees in U and V, its control points row by row (a row a value of U), their weights
     * for a rational one, its knots each written as often as its multiplicity says, and whether it closes in U or V.
     */
    record BSplineSurface(int degreeU, int degreeV, Vector3d[][] points, double[][] weights, double[] knotsU,
                          double[] knotsV, boolean closedU, boolean closedV) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            final int lastU = this.points.length - 1;
            final int lastV = this.points[0].length - 1;
            final double[] bu = new double[this.degreeU + 1];
            final double[] bv = new double[this.degreeV + 1];
            final int spanU = span(this.knotsU, this.degreeU, lastU, u);
            final int spanV = span(this.knotsV, this.degreeV, lastV, v);
            basis(this.knotsU, this.degreeU, spanU, u, bu);
            basis(this.knotsV, this.degreeV, spanV, v, bv);
            final Vector3d out = new Vector3d();
            double weight = 0;
            for (int i = 0; i <= this.degreeU; i++) {
                final int row = spanU - this.degreeU + i;
                for (int j = 0; j <= this.degreeV; j++) {
                    final int column = spanV - this.degreeV + j;
                    final double w = bu[i] * bv[j] * (this.weights.length > 0 ? this.weights[row][column] : 1);
                    out.fma(w, this.points[row][column]);
                    weight += w;
                }
            }
            return weight == 0 ? out : out.div(weight);
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            final double lowU = this.knotsU[this.degreeU];
            final double highU = this.knotsU[this.points.length];
            final double lowV = this.knotsV[this.degreeV];
            final double highV = this.knotsV[this.points[0].length];
            double[] best = near != null && near.length == 2 ? near.clone() : null;
            double nearest = best == null ? Double.MAX_VALUE : at(best[0], best[1]).distanceSquared(point);
            for (int i = 0; i <= SEARCH; i++) {
                for (int j = 0; j <= SEARCH; j++) {
                    final double u = lowU + (highU - lowU) * i / SEARCH;
                    final double v = lowV + (highV - lowV) * j / SEARCH;
                    final double away = at(u, v).distanceSquared(point);
                    if (away < nearest) {
                        nearest = away;
                        best = new double[] {u, v};
                    }
                }
            }
            return refine(this, point, best, lowU, highU, lowV, highV);
        }

        @Override
        public double periodU() {
            return this.closedU ? this.knotsU[this.points.length] - this.knotsU[this.degreeU] : 0;
        }

        @Override
        public double periodV() {
            return this.closedV ? this.knotsV[this.points[0].length] - this.knotsV[this.degreeV] : 0;
        }

        @Override
        public double[] spacing(final double tolerance) {
            return new double[] {(this.knotsU[this.points.length] - this.knotsU[this.degreeU])
                    / Math.max(2, (this.points.length - 1) * 2),
                (this.knotsV[this.points[0].length] - this.knotsV[this.degreeV])
                    / Math.max(2, (this.points[0].length - 1) * 2)};
        }
    }

    /** A curve swept round an axis: U along the curve, V the turn about the axis. */
    record Revolution(Curve curve, Vector3d axisPoint, Vector3d axis) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            final Vector3d point = new Vector3d(this.curve.at(u)).sub(this.axisPoint);
            final Vector3d along = new Vector3d(this.axis).mul(this.axis.dot(point));
            final Vector3d across = new Vector3d(point).sub(along);
            final Vector3d side = new Vector3d(this.axis).cross(across);
            return new Vector3d(this.axisPoint).add(along).fma(Math.cos(v), across).fma(Math.sin(v), side);
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            final Vector3d reference = reference();
            final Vector3d d = new Vector3d(point).sub(this.axisPoint);
            final Vector3d across = new Vector3d(d).sub(new Vector3d(this.axis).mul(this.axis.dot(d)));
            final Vector3d side = new Vector3d(this.axis).cross(reference);
            final double v = Math.atan2(across.dot(side), across.dot(reference));
            // Turned back onto the curve's own plane, the point's place along the curve.
            final Vector3d back = new Vector3d(d).sub(across).fma(across.length(), reference).add(this.axisPoint);
            return new double[] {this.curve.parameterOf(back), v};
        }

        /* A closed curve swept round comes round in U as well, as a circle swept round is a torus. */
        @Override
        public double periodU() {
            return this.curve.period();
        }

        @Override
        public double periodV() {
            return TURN;
        }

        @Override
        public double[] spacing(final double tolerance) {
            final double reach = new Vector3d(this.curve.at(0)).sub(this.axisPoint).length();
            return new double[] {0, arcStep(Math.max(reach, tolerance), tolerance)};
        }

        /* The way out from the axis the curve's own plane lies. */
        private Vector3d reference() {
            final Vector3d point = new Vector3d(this.curve.at(this.curve.parameterOf(this.axisPoint))).sub(
                    this.axisPoint);
            Vector3d across = new Vector3d(point).sub(new Vector3d(this.axis).mul(this.axis.dot(point)));
            if (across.lengthSquared() < 1e-20) {
                across = new Vector3d(this.curve.at(1)).sub(this.axisPoint);
                across.sub(new Vector3d(this.axis).mul(this.axis.dot(across)));
            }
            return across.lengthSquared() < 1e-20 ? Frame.of(new Vector3d(), this.axis, new Vector3d(1, 0, 0)).x()
                    : across.normalize();
        }
    }

    /** A curve swept along a way: U along the curve, V along the way. */
    record Extrusion(Curve curve, Vector3d way) implements Surface {

        @Override
        public Vector3d at(final double u, final double v) {
            return new Vector3d(this.curve.at(u)).fma(v, this.way);
        }

        @Override
        public double[] parametersOf(final Vector3d point, final double[] near) {
            double v = 0;
            double u = 0;
            for (int i = 0; i < 3; i++) {
                u = this.curve.parameterOf(new Vector3d(point).fma(-v, this.way));
                v = new Vector3d(point).sub(this.curve.at(u)).dot(this.way) / this.way.lengthSquared();
            }
            return new double[] {u, v};
        }

        /* A closed curve drawn out comes round as the curve does: a circle's is a cylinder's band. */
        @Override
        public double periodU() {
            return this.curve.period();
        }

        @Override
        public double[] spacing(final double tolerance) {
            return new double[] {0, 0};
        }
    }

    /* How many pieces an arc of {@code angle} of a circle of {@code radius} takes, its chords near enough. */
    private static int arcPieces(final double angle, final double radius, final double tolerance) {
        return Math.max(1, (int) Math.ceil(angle / arcStep(radius, tolerance)));
    }

    /* The angle a chord of a circle may span and stay within {@code tolerance} of it, a sixteenth turn at most. */
    private static double arcStep(final double radius, final double tolerance) {
        final double most = TURN / 16;
        final double least = TURN / 72;
        if (radius <= tolerance) {
            return most;
        }
        return Math.clamp(2 * Math.acos(1 - tolerance / radius), least, most);
    }

    /* The parameters a point is nearest, narrowed from a first guess by halves, within the surface's bounds. */
    private static double[] refine(final Surface surface, final Vector3d point, final double[] start,
                                   final double lowU, final double highU, final double lowV, final double highV) {
        double u = start[0];
        double v = start[1];
        double reachU = (highU - lowU) / SEARCH;
        double reachV = (highV - lowV) / SEARCH;
        double nearest = surface.at(u, v).distanceSquared(point);
        for (int i = 0; i < NEWTON * 3; i++) {
            boolean moved = false;
            for (final double[] way : new double[][] {{reachU, 0}, {-reachU, 0}, {0, reachV}, {0, -reachV}}) {
                final double tu = Math.clamp(u + way[0], lowU, highU);
                final double tv = Math.clamp(v + way[1], lowV, highV);
                final double away = surface.at(tu, tv).distanceSquared(point);
                if (away < nearest) {
                    nearest = away;
                    u = tu;
                    v = tv;
                    moved = true;
                }
            }
            if (!moved) {
                reachU /= 2;
                reachV /= 2;
                if (reachU < STEP * (highU - lowU) && reachV < STEP * (highV - lowV)) {
                    break;
                }
            }
        }
        return new double[] {u, v};
    }

    /* The knot span {@code t} falls in, of a spline of {@code last + 1} control points, as The NURBS Book finds it. */
    static int span(final double[] knots, final int degree, final int last, final double t) {
        if (t >= knots[last + 1]) {
            return last;
        }
        if (t <= knots[degree]) {
            return degree;
        }
        int low = degree;
        int high = last + 1;
        int middle = (low + high) / 2;
        while (t < knots[middle] || t >= knots[middle + 1]) {
            if (t < knots[middle]) {
                high = middle;
            } else {
                low = middle;
            }
            middle = (low + high) / 2;
        }
        return middle;
    }

    /* The basis functions not zero at {@code t}, into {@code out}, as The NURBS Book works them out. */
    static void basis(final double[] knots, final int degree, final int span, final double t, final double[] out) {
        final double[] left = new double[degree + 1];
        final double[] right = new double[degree + 1];
        out[0] = 1;
        for (int j = 1; j <= degree; j++) {
            left[j] = t - knots[span + 1 - j];
            right[j] = knots[span + j] - t;
            double saved = 0;
            for (int r = 0; r < j; r++) {
                final double gap = right[r + 1] + left[j - r];
                final double temp = gap == 0 ? 0 : out[r] / gap;
                out[r] = saved + right[r + 1] * temp;
                saved = left[j - r] * temp;
            }
            out[j] = saved;
        }
    }
}
