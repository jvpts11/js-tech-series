/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * How the wires of one cable block lie and how much room they take: plain geometry, in pixels of the block (0 to 16
 * on each axis), worked out from each wire's lane, thickness and links. The block's model draws it and the block's
 * shape is made of it, so what a player sees is what a player hits.
 *
 * <p>Where a wire crosses a face alone it crosses in the middle; where several cross one face, each crosses in its own
 * lane. A wire that keeps one place through the block runs straight through it or bends at its own point. Where the
 * wires change places, or would cross one another inside the block, the block becomes a junction box: a housing the
 * wires enter and leave, each in its place on each face.
 *
 * <p>Faces are numbered as the game numbers directions: down, up, north, south, west, east.
 */
public final class BundleShape {

    private final List<Piece> pieces;
    private final List<Plug> plugs;
    private final List<List<Box>> reach;
    private final boolean junction;

    /** How many faces a block has. */
    public static final int FACES = 6;
    /** The axis each face's normal runs along: 0 for x, 1 for y, 2 for z. */
    private static final int[] AXIS = {1, 1, 2, 2, 0, 0};
    /** Which way each face's normal points along its axis. */
    private static final int[] SIGN = {-1, 1, -1, 1, -1, 1};
    /* The two axes across a run along each axis, across first and up second. */
    private static final int[][] ACROSS = {{2, 1}, {0, 2}, {0, 1}};
    /* The faces by their initial (down, east, north, south, up, west): the order that picks a bend's axis. */
    private static final int[] BY_INITIAL = {0, 5, 2, 3, 1, 4};
    /* The arms in the order the ring is offered to them: the level ones by initial first, then up and down. */
    private static final int[] RING_ORDER = {5, 2, 3, 4, 0, 1};
    private static final double MIDDLE = 8.0;
    private static final double BLOCK = 16.0;
    /* The housing is never wider than the block less a pixel each side, and clears the wires by a pixel and a half. */
    private static final double HOUSING_MOST = 7.0;
    private static final double HOUSING_CLEARANCE = 1.5;
    private static final double SCREW_INSET = 1.5;
    private static final double SCREW_HALF = 0.5;
    private static final double SCREW_HEIGHT = 0.3;
    private static final double EDGE_FROM_TOP = 2.0;
    private static final double EDGE_HEIGHT = 0.4;
    private static final double EDGE_PROUD = 0.1;
    /* A ring stands proud of its jacket by a little, three pixels long, on an arm at least four long. */
    private static final double RING_PROUD = 0.35;
    private static final double RING_HALF = 1.5;
    private static final double RING_ROOM = 4.0;
    private static final double TOUCH = 0.01;

    private BundleShape(final List<Piece> pieces, final List<Plug> plugs, final List<List<Box>> reach,
                        final boolean junction) {
        this.pieces = List.copyOf(pieces);
        this.plugs = List.copyOf(plugs);
        this.reach = reach.stream().map(List::copyOf).toList();
        this.junction = junction;
    }

    /** How the {@code strands} of one block lie, in the order given; each piece and plug names its strand by index. */
    public static BundleShape of(final List<Strand> strands) {
        return new Layout(strands).build();
    }

    /** Every box drawn: the wires' jackets and rings, and the junction box's housing when there is one. */
    public List<Piece> pieces() {
        return this.pieces;
    }

    /** Where each wire meets a device, with the plug of its kind. */
    public List<Plug> plugs() {
        return this.plugs;
    }

    /** Whether the wires meet in a junction box. */
    public boolean junction() {
        return this.junction;
    }

    /**
     * What a player aims at to pick the strand at {@code strand}: its jacket, or in a junction box the way it takes
     * from the middle to each face it crosses, through the housing.
     */
    public List<Box> reach(final int strand) {
        return this.reach.get(strand);
    }

    /** The axis a face's normal runs along. */
    public static int axis(final int face) {
        return AXIS[face];
    }

    /** Which way a face's normal points along its axis, -1 or 1. */
    public static int sign(final int face) {
        return SIGN[face];
    }

    /** The face opposite {@code face}. */
    public static int opposite(final int face) {
        return face ^ 1;
    }

    /**
     * One wire as the shape needs it.
     *
     * @param lane      its lane, used when it shares a face with another
     * @param thickness how thick it is, in pixels
     * @param links     the faces it crosses, a bit for each
     * @param plugs     the faces among those where it meets a device, a bit for each
     * @param ringed    whether a ring of its colour marks it
     */
    public record Strand(Lane lane, int thickness, int links, int plugs, boolean ringed) {

        public Strand {
            Objects.requireNonNull(lane, "lane");
            if (thickness <= 0 || thickness >= BLOCK) {
                throw new IllegalArgumentException("a wire is between 1 and 15 pixels thick, not " + thickness);
            }
            if ((plugs & ~links) != 0) {
                throw new IllegalArgumentException("a wire meets devices only on faces it crosses");
            }
        }

        /** Whether it crosses {@code face}. */
        public boolean crosses(final int face) {
            return (this.links & 1 << face) != 0;
        }

        /** How many faces it crosses. */
        public int linkCount() {
            return Integer.bitCount(this.links);
        }
    }

    /** What a piece is made of. */
    public enum Kind {
        /** A length of a wire's jacket. */
        JACKET,
        /** The ring of a wire's colour. */
        RING,
        /** The junction box's housing. */
        HOUSING,
        /** The seam round the housing below its lid. */
        HOUSING_EDGE,
        /** One of the four screws on the housing's lid. */
        SCREW
    }

    /**
     * One box drawn.
     *
     * @param kind   what it is made of
     * @param strand the wire it belongs to, or -1 for the housing's pieces
     * @param box    where it lies
     * @param axis   the axis a jacket runs along, which lays its texture; -1 for anything else
     * @param hidden the faces left out, a bit for each, which a neighbour, the housing or a plug covers
     */
    public record Piece(Kind kind, int strand, Box box, int axis, int hidden) {

        /** Whether the face is left out. */
        public boolean hides(final int face) {
            return (this.hidden & 1 << face) != 0;
        }
    }

    /**
     * A plug where a wire meets a device.
     *
     * @param strand the wire
     * @param face   the face the device is beyond
     * @param across how far the plug sits from the middle of the face across, in pixels
     * @param up     how far it sits from the middle of the face up, in pixels
     */
    public record Plug(int strand, int face, double across, double up) {
    }

    /**
     * An axis-aligned box in pixels of the block.
     */
    public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

        static Box of(final double[] from, final double[] to) {
            return new Box(from[0], from[1], from[2], to[0], to[1], to[2]);
        }

        /** Its lower end along {@code axis}. */
        public double min(final int axis) {
            return switch (axis) {
                case 0 -> this.minX;
                case 1 -> this.minY;
                default -> this.minZ;
            };
        }

        /** Its upper end along {@code axis}. */
        public double max(final int axis) {
            return switch (axis) {
                case 0 -> this.maxX;
                case 1 -> this.maxY;
                default -> this.maxZ;
            };
        }

        /** Whether it and {@code other} share more than a touching face. */
        public boolean overlaps(final Box other) {
            for (int axis = 0; axis < 3; axis++) {
                if (min(axis) >= other.max(axis) - TOUCH || other.min(axis) >= max(axis) - TOUCH) {
                    return false;
                }
            }
            return true;
        }

        /** How far {@code point} lies from the box, 0 when it is inside. */
        public double distanceTo(final double x, final double y, final double z) {
            final double dx = Math.max(Math.max(this.minX - x, 0.0), x - this.maxX);
            final double dy = Math.max(Math.max(this.minY - y, 0.0), y - this.maxY);
            final double dz = Math.max(Math.max(this.minZ - z, 0.0), z - this.maxZ);
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    /* The working out of one block's shape. */
    private static final class Layout {

        private final List<Strand> strands;
        private final int[] crossing = new int[FACES];
        private final List<Piece> pieces = new ArrayList<>();
        private final List<Plug> plugs = new ArrayList<>();
        private final List<List<Box>> reach = new ArrayList<>();

        Layout(final List<Strand> strands) {
            this.strands = List.copyOf(strands);
            for (final Strand strand : this.strands) {
                for (int face = 0; face < FACES; face++) {
                    if (strand.crosses(face)) {
                        this.crossing[face]++;
                    }
                }
                this.reach.add(new ArrayList<>());
            }
        }

        BundleShape build() {
            final List<double[]> points = new ArrayList<>();
            boolean moves = false;
            for (final Strand strand : this.strands) {
                final double[] point = new double[3];
                moves |= !settle(strand, point);
                points.add(point);
            }
            final boolean junction = this.strands.size() > 1 && (moves || anyCross(points));
            if (junction) {
                junction();
            } else {
                for (int i = 0; i < this.strands.size(); i++) {
                    plain(i, points.get(i));
                }
            }
            for (int i = 0; i < this.strands.size(); i++) {
                final Strand strand = this.strands.get(i);
                for (int face = 0; face < FACES; face++) {
                    if ((strand.plugs() & 1 << face) != 0) {
                        final double[] at = facePoint(strand, face);
                        this.plugs.add(new Plug(i, face, at[0], at[1]));
                    }
                }
            }
            return new BundleShape(this.pieces, this.plugs, this.reach, junction);
        }

        /* Where the wire crosses {@code face}: the middle when it crosses that face alone, its lane when it shares. */
        private double[] facePoint(final Strand strand, final int face) {
            if (this.crossing[face] < 2) {
                return new double[] {0.0, 0.0};
            }
            return new double[] {strand.lane().across() * Lane.PITCH, strand.lane().up() * Lane.PITCH};
        }

        /*
         * Fills the wire's point, how far from the block's middle it runs, from the places it crosses its faces at;
         * false when two faces put it in two places, so it has to change place inside the block. A wire that crosses
         * no face, in a block it shares, keeps to its lane as a run along x would.
         */
        private boolean settle(final Strand strand, final double[] point) {
            if (strand.links() == 0) {
                if (this.strands.size() > 1) {
                    point[ACROSS[0][0]] = strand.lane().across() * Lane.PITCH;
                    point[ACROSS[0][1]] = strand.lane().up() * Lane.PITCH;
                }
                return true;
            }
            final boolean[] known = new boolean[3];
            boolean steady = true;
            for (int face = 0; face < FACES; face++) {
                if (!strand.crosses(face)) {
                    continue;
                }
                final int[] across = ACROSS[AXIS[face]];
                final double[] at = facePoint(strand, face);
                for (int k = 0; k < 2; k++) {
                    final int axis = across[k];
                    if (!known[axis]) {
                        point[axis] = at[k];
                        known[axis] = true;
                    } else if (point[axis] != at[k]) {
                        steady = false;
                    }
                }
            }
            return steady;
        }

        /* Whether two wires' arms, or the cores of wires that cross nothing, would run into one another. */
        private boolean anyCross(final List<double[]> points) {
            final List<Box> boxes = new ArrayList<>();
            final List<Integer> owners = new ArrayList<>();
            for (int i = 0; i < this.strands.size(); i++) {
                final Strand strand = this.strands.get(i);
                if (strand.links() == 0) {
                    boxes.add(core(points.get(i), strand.thickness()));
                    owners.add(i);
                }
                for (int face = 0; face < FACES; face++) {
                    if (strand.crosses(face)) {
                        boxes.add(arm(points.get(i), face, strand.thickness(), false));
                        owners.add(i);
                    }
                }
            }
            for (int a = 0; a < boxes.size(); a++) {
                for (int b = a + 1; b < boxes.size(); b++) {
                    if (!owners.get(a).equals(owners.get(b)) && boxes.get(a).overlaps(boxes.get(b))) {
                        return true;
                    }
                }
            }
            return false;
        }

        /* The wire alone in its place: straight through, or a core at its point with an arm to each face. */
        private void plain(final int index, final double[] point) {
            final Strand strand = this.strands.get(index);
            final int t = strand.thickness();
            final int links = strand.links();
            final int straight = straightAxis(links);
            if (straight >= 0) {
                final double[] from = centred(point, t, -1);
                final double[] to = centred(point, t, 1);
                from[straight] = 0.0;
                to[straight] = BLOCK;
                final Box box = Box.of(from, to);
                jacket(index, box, straight, links);
                if (strand.ringed()) {
                    ring(index, box, straight, MIDDLE);
                }
                return;
            }
            final int coreAxis = links == 0 ? 0 : AXIS[firstByInitial(links)];
            jacket(index, core(point, t), coreAxis, links);
            boolean ringed = !strand.ringed();
            for (final int face : RING_ORDER) {
                if (!strand.crosses(face)) {
                    continue;
                }
                final int axis = AXIS[face];
                final Box arm = arm(point, face, t, true);
                jacket(index, arm, axis, 1 << face | 1 << opposite(face));
                if (!ringed && arm.max(axis) - arm.min(axis) >= RING_ROOM) {
                    ring(index, arm, axis, (arm.min(axis) + arm.max(axis)) / 2.0);
                    ringed = true;
                }
            }
        }

        /* The housing, and each wire from its face to the block's face, in its place there. */
        private void junction() {
            double extent = 0.0;
            for (final Strand strand : this.strands) {
                for (int face = 0; face < FACES; face++) {
                    if (strand.crosses(face)) {
                        final double[] at = facePoint(strand, face);
                        final double half = strand.thickness() / 2.0;
                        extent = Math.max(extent, Math.max(Math.abs(at[0]) + half, Math.abs(at[1]) + half));
                    }
                }
                if (strand.links() == 0) {
                    // A wire that crosses nothing sits in its lane inside the block, so the housing must hold it.
                    final double half = strand.thickness() / 2.0;
                    extent = Math.max(extent, half);
                    extent = Math.max(extent, Math.abs(strand.lane().across() * Lane.PITCH) + half);
                    extent = Math.max(extent, Math.abs(strand.lane().up() * Lane.PITCH) + half);
                }
            }
            final double half = Math.min(HOUSING_MOST, extent + HOUSING_CLEARANCE);
            final double top = MIDDLE + half;
            this.pieces.add(new Piece(Kind.HOUSING, -1, new Box(MIDDLE - half, MIDDLE - half, MIDDLE - half,
                    MIDDLE + half, top, MIDDLE + half), -1, 0));
            for (final int sx : new int[] {-1, 1}) {
                for (final int sz : new int[] {-1, 1}) {
                    final double x = MIDDLE + sx * (half - SCREW_INSET);
                    final double z = MIDDLE + sz * (half - SCREW_INSET);
                    this.pieces.add(new Piece(Kind.SCREW, -1, new Box(x - SCREW_HALF, top, z - SCREW_HALF,
                            x + SCREW_HALF, top + SCREW_HEIGHT, z + SCREW_HALF), -1, 0));
                }
            }
            this.pieces.add(new Piece(Kind.HOUSING_EDGE, -1, new Box(MIDDLE - half - EDGE_PROUD,
                    top - EDGE_FROM_TOP, MIDDLE - half - EDGE_PROUD, MIDDLE + half + EDGE_PROUD,
                    top - EDGE_FROM_TOP + EDGE_HEIGHT, MIDDLE + half + EDGE_PROUD), -1, 0));
            for (int i = 0; i < this.strands.size(); i++) {
                final Strand strand = this.strands.get(i);
                final int t = strand.thickness();
                for (int face = 0; face < FACES; face++) {
                    if (!strand.crosses(face)) {
                        continue;
                    }
                    final int axis = AXIS[face];
                    final double[] point = new double[3];
                    final double[] at = facePoint(strand, face);
                    point[ACROSS[axis][0]] = at[0];
                    point[ACROSS[axis][1]] = at[1];
                    final double[] from = centred(point, t, -1);
                    final double[] to = centred(point, t, 1);
                    if (SIGN[face] > 0) {
                        from[axis] = MIDDLE + half;
                        to[axis] = BLOCK;
                    } else {
                        from[axis] = 0.0;
                        to[axis] = MIDDLE - half;
                    }
                    this.pieces.add(new Piece(Kind.JACKET, i, Box.of(from, to), axis,
                            1 << face | 1 << opposite(face)));
                    this.reach.get(i).add(arm(point, face, t, false));
                }
                if (strand.links() == 0) {
                    final double[] point = new double[3];
                    point[ACROSS[0][0]] = strand.lane().across() * Lane.PITCH;
                    point[ACROSS[0][1]] = strand.lane().up() * Lane.PITCH;
                    jacket(i, core(point, t), 0, 0);
                }
            }
        }

        private void jacket(final int index, final Box box, final int axis, final int hidden) {
            this.pieces.add(new Piece(Kind.JACKET, index, box, axis, hidden));
            this.reach.get(index).add(box);
        }

        /* A ring three pixels long, a little proud of the jacket, round {@code at} along the axis. */
        private void ring(final int index, final Box jacket, final int axis, final double at) {
            final double[] from = new double[3];
            final double[] to = new double[3];
            for (int k = 0; k < 3; k++) {
                from[k] = jacket.min(k) - RING_PROUD;
                to[k] = jacket.max(k) + RING_PROUD;
            }
            from[axis] = at - RING_HALF;
            to[axis] = at + RING_HALF;
            this.pieces.add(new Piece(Kind.RING, index, Box.of(from, to), axis, 0));
        }

        /* The axis of a wire that crosses exactly two faces, opposite one another; -1 for any other wire. */
        private static int straightAxis(final int links) {
            if (Integer.bitCount(links) != 2) {
                return -1;
            }
            final int first = Integer.numberOfTrailingZeros(links);
            return (links & 1 << opposite(first)) != 0 ? AXIS[first] : -1;
        }

        private static int firstByInitial(final int links) {
            for (final int face : BY_INITIAL) {
                if ((links & 1 << face) != 0) {
                    return face;
                }
            }
            return 0;
        }

        /* A cube of the wire's thickness at its point. */
        private static Box core(final double[] point, final int thickness) {
            return Box.of(centred(point, thickness, -1), centred(point, thickness, 1));
        }

        /*
         * The wire from its point to {@code face}: from the core's side when {@code fromCore}, else from the point
         * itself.
         */
        private static Box arm(final double[] point, final int face, final int thickness, final boolean fromCore) {
            final int axis = AXIS[face];
            final double[] from = centred(point, thickness, -1);
            final double[] to = centred(point, thickness, 1);
            final double centre = MIDDLE + point[axis];
            final double start = fromCore ? centre + SIGN[face] * thickness / 2.0 : centre;
            final double end = MIDDLE + SIGN[face] * MIDDLE;
            from[axis] = Math.min(start, end);
            to[axis] = Math.max(start, end);
            return Box.of(from, to);
        }

        /* The corner of a cube of {@code thickness} round the point, the low one for -1 and the high one for 1. */
        private static double[] centred(final double[] point, final int thickness, final int side) {
            final double[] corner = new double[3];
            for (int k = 0; k < 3; k++) {
                corner[k] = MIDDLE + point[k] + side * thickness / 2.0;
            }
            return corner;
        }
    }
}
