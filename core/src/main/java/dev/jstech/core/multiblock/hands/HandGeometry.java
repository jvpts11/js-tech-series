/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelCube;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.model.Rotations;
import dev.jstech.core.multiblock.Orientation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Where the parts a player works by hand stand in a formed structure's model: each part's boxes, the cubes of its
 * bones, with its doors turned on their hinges as far as they are open; which part a line of sight meets first; and
 * how the model stands in the world. The same on the server, which checks a click, and on the player's game, which
 * outlines what is looked at and draws the doors turning. Pure.
 */
public final class HandGeometry {

    private final ModelData model;
    private final List<IHandPart> parts;
    /* The bones of each part, by their place in the model. */
    private final int[][] bonesOf;
    /* The door each part lies behind, by its number among the doors, or -1; and each door, and its part. */
    private final int[] behind;
    private final List<IHandPart.Door> doors = new ArrayList<>();
    private final List<Integer> doorParts = new ArrayList<>();
    /* The bones each door turns on its hinge, by name: those of it whose parent is not of it too. */
    private final List<String[]> turned = new ArrayList<>();
    /*
     * Each door's boxes shut, its doorway; and the bounds of each part that never moves, its lowest corner then its
     * highest in the model's pixels, or null for a part that moves with a door: a line that misses them misses the
     * part, read without placing a box.
     */
    private final List<List<Box>> doorways = new ArrayList<>();
    private final Vector3f[][] still;
    /* A pose and the bones' places to work in, one for each thread that asks, as the server's and the game's do. */
    private final ThreadLocal<Work> work;

    /* A model's pixels in a block, and the middle of a block. */
    private static final float PIXELS = 16;
    private static final float HALF = 0.5F;

    /**
     * The parts {@code parts} of {@code model}, each found by its bones.
     *
     * @throws IllegalArgumentException when a part has no bone in the model or nothing drawn in its bones, or a place
     *                                  lies behind a door that is not one of the parts
     */
    public HandGeometry(final ModelData model, final List<IHandPart> parts) {
        this.model = model;
        this.parts = List.copyOf(parts);
        final Map<String, Integer> doorNumbers = new HashMap<>();
        for (int i = 0; i < this.parts.size(); i++) {
            if (this.parts.get(i) instanceof IHandPart.Door door) {
                doorNumbers.put(door.bone(), this.doors.size());
                this.doors.add(door);
                this.doorParts.add(i);
            }
        }
        this.bonesOf = new int[this.parts.size()][];
        this.behind = new int[this.parts.size()];
        for (int i = 0; i < this.parts.size(); i++) {
            final IHandPart part = this.parts.get(i);
            final List<Integer> found = new ArrayList<>();
            int cubes = 0;
            for (int bone = 0; bone < model.bones().size(); bone++) {
                if (part.owns(model.bones().get(bone).name())) {
                    found.add(bone);
                    cubes += model.bones().get(bone).cubes().size();
                }
            }
            if (found.isEmpty()) {
                throw new IllegalArgumentException("the model " + model.name() + " has no bone " + part.bone());
            }
            if (cubes == 0) {
                throw new IllegalArgumentException("the bone " + part.bone() + " of the model " + model.name()
                        + " draws nothing to work by hand");
            }
            this.bonesOf[i] = found.stream().mapToInt(Integer::intValue).toArray();
            this.behind[i] = -1;
            if (part instanceof IHandPart.Place place && place.door() != null) {
                final Integer door = doorNumbers.get(place.door());
                if (door == null) {
                    throw new IllegalArgumentException("the place " + place.bone() + " lies behind " + place.door()
                            + ", which is no door of the model");
                }
                this.behind[i] = door;
            }
        }
        final boolean[] ofDoor = new boolean[model.bones().size()];
        for (final int part : this.doorParts) {
            final List<String> top = new ArrayList<>();
            for (final int bone : this.bonesOf[part]) {
                ofDoor[bone] = true;
                final String parent = model.bones().get(bone).parent();
                if (parent == null || !this.parts.get(part).owns(parent)) {
                    top.add(model.bones().get(bone).name());
                }
            }
            this.turned.add(top.toArray(String[]::new));
        }
        this.work = ThreadLocal.withInitial(() -> new Work(model));
        final Work rest = new Work(model);
        rest.pose(this, new float[this.doors.size()]);
        for (final int part : this.doorParts) {
            this.doorways.add(List.copyOf(boxesIn(rest.world, part)));
        }
        this.still = new Vector3f[this.parts.size()][];
        for (int part = 0; part < this.parts.size(); part++) {
            if (!(this.parts.get(part) instanceof IHandPart.Door) && !movesWithDoor(part, ofDoor)) {
                final Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
                final Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
                for (final Box box : boxesIn(rest.world, part)) {
                    for (final Vector3f corner : box.corners()) {
                        min.min(corner);
                        max.max(corner);
                    }
                }
                this.still[part] = new Vector3f[] {min, max};
            }
        }
    }

    /** The parts, in the order they were declared. */
    public List<IHandPart> parts() {
        return this.parts;
    }

    /** How many doors it has, numbered in the order they were declared. */
    public int doors() {
        return this.doors.size();
    }

    /** The door of number {@code door}. */
    public IHandPart.Door door(final int door) {
        return this.doors.get(door);
    }

    /** The number of the door of bone {@code bone}, or -1 when it is no door. */
    public int doorNumber(final String bone) {
        for (int i = 0; i < this.doors.size(); i++) {
            if (this.doors.get(i).bone().equals(bone)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The boxes of part {@code part}, in the model's pixels, each door as far open as {@code open} says by its number,
     * from 0 shut to 1 open.
     */
    public List<Box> boxes(final int part, final float[] open) {
        final Work at = this.work.get();
        at.pose(this, open);
        return boxesIn(at.world, part);
    }

    /**
     * The one box round part {@code part}, to outline it by, each door as far open as {@code open} says: the bounds of
     * its boxes in their own frame when they all stand in one, as the cubes of one bone or of bones at rest do, or else
     * in the model's. A place drawn by a bone for each thing it may hold is outlined once, round them all.
     */
    public Box outline(final int part, final float[] open) {
        final List<Box> boxes = boxes(part, open);
        final Matrix4f frame = boxes.getFirst().toModel();
        boolean shared = true;
        for (final Box box : boxes) {
            shared &= box.toModel().equals(frame);
        }
        final Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        final Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (final Box box : boxes) {
            if (shared) {
                min.min(box.min());
                max.max(box.max());
            } else {
                for (final Vector3f corner : box.corners()) {
                    min.min(corner);
                    max.max(corner);
                }
            }
        }
        return new Box(shared ? new Matrix4f(frame) : new Matrix4f(), min, max);
    }

    /**
     * The part the line from {@code from} to {@code to}, in the model's pixels, meets first, and how far along the
     * line, each door as far open as {@code open} says; a place behind a door is reached only through its doorway,
     * the door wholly open.
     */
    public Optional<Aim> aim(final Vector3fc from, final Vector3fc to, final float[] open) {
        final Work at = this.work.get();
        at.pose(this, open);
        Aim best = null;
        for (int part = 0; part < this.parts.size(); part++) {
            if (!reached(part, open) || !mayMeet(part, from, to, 0)) {
                continue;
            }
            final float along = along(boxesIn(at.world, part), from, to, 0);
            if (along >= 0 && (best == null || along < best.along()) && throughDoorway(part, from, to, along, 0)) {
                best = new Aim(part, along);
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Whether the line from {@code from} to {@code to} meets part {@code part}, its boxes grown by {@code slack}
     * pixels on every side, and the part is reached, through its doorway for a place behind a door, each door as far
     * open as {@code open} says.
     */
    public boolean meets(final int part, final Vector3fc from, final Vector3fc to, final float[] open,
                         final float slack) {
        if (!reached(part, open) || !mayMeet(part, from, to, slack)) {
            return false;
        }
        final Work at = this.work.get();
        at.pose(this, open);
        final float along = along(boxesIn(at.world, part), from, to, slack);
        return along >= 0 && throughDoorway(part, from, to, along, slack);
    }

    /** Whether part {@code part} can be reached, the door it lies behind, if any, wholly open. */
    public boolean reached(final int part, final float[] open) {
        return this.behind[part] < 0 || open[this.behind[part]] >= 1;
    }

    /** The door part {@code part} lies behind, by its number, or -1. */
    public int behind(final int part) {
        return this.behind[part];
    }

    /**
     * Turns each door of {@code pose}, a pose of this model or of another with the same bones, on its hinge as far as
     * {@code open} says by its number, from 0 shut to 1 open; a door the pose's model lacks is passed over.
     */
    public void poseDoors(final ModelPose pose, final float[] open) {
        final Quaternionf turn = new Quaternionf();
        for (int i = 0; i < this.doors.size(); i++) {
            final Hinge hinge = this.doors.get(i).hinge();
            turn.rotationY((float) Math.toRadians(hinge.degrees() * Math.max(0, Math.min(1, open[i]))));
            for (final String name : this.turned.get(i)) {
                final int bone = pose.index(name);
                if (bone < 0) {
                    continue;
                }
                // The bone turns about its own pivot; moved on by as much again, it turns about its hinge instead.
                final ModelBone rest = pose.model().bones().get(bone);
                final Vector3f apart = new Vector3f(hinge.x() - rest.pivot().x() - rest.translation().x(),
                        -rest.pivot().y() - rest.translation().y(),
                        hinge.z() - rest.pivot().z() - rest.translation().z());
                final Vector3f moved = turn.transform(new Vector3f(apart));
                pose.setTurn(bone, turn);
                pose.addMove(bone, apart.x - moved.x, apart.y - moved.y, apart.z - moved.z);
            }
        }
    }

    /**
     * Writes into {@code into} where a model of a structure's terminal stands round the terminal's block: its pixels,
     * {@code scale} times as big, to places in blocks from the block's lowest corner, the model made as the structure
     * stands facing north on its terminal's floor and turned, laid down or mirrored about the terminal's middle as
     * {@code orientation} says. Measured from the terminal, not from the world's origin, so the floats keep their
     * precision however far out the structure stands.
     */
    public static Matrix4f placed(final Orientation orientation, final float scale, final Matrix4f into) {
        final Matrix4f turn = new Matrix4f().zero();
        turn.set(orientation.xFrom(), 0, orientation.xSign());
        turn.set(orientation.yFrom(), 1, orientation.ySign());
        turn.set(orientation.zFrom(), 2, orientation.zSign());
        turn.m33(1);
        final float size = scale / PIXELS;
        return into.translation(HALF, HALF, HALF).mul(turn).translate(0, -HALF, 0).scale(size, size, size);
    }

    /*
     * Whether the line passes through where the door part {@code part} lies behind stands shut, before it reaches the
     * part {@code along} the line: a place is reached through its doorway, never through the structure's walls. A part
     * behind no door is always reached.
     */
    private boolean throughDoorway(final int part, final Vector3fc from, final Vector3fc to, final float along,
                                   final float slack) {
        final int door = this.behind[part];
        if (door < 0) {
            return true;
        }
        final float doorway = along(this.doorways.get(door), from, to, slack);
        return doorway >= 0 && doorway <= along;
    }

    /* Whether the line may meet part {@code part}: always for one that moves, else when it meets its bounds. */
    private boolean mayMeet(final int part, final Vector3fc from, final Vector3fc to, final float slack) {
        final Vector3f[] bounds = this.still[part];
        return bounds == null || slab(from, new Vector3f(to).sub(from), bounds[0], bounds[1], slack) >= 0;
    }

    /* The boxes of part {@code part}, its bones placed by {@code world}. */
    private List<Box> boxesIn(final Matrix4f[] world, final int part) {
        final List<Box> out = new ArrayList<>();
        for (final int bone : this.bonesOf[part]) {
            for (final ModelCube cube : this.model.bones().get(bone).cubes()) {
                out.add(box(world[bone], cube));
            }
        }
        return out;
    }

    /* Whether a bone of part {@code part}, or one it hangs from, is a door's, of those {@code ofDoor} marks. */
    private boolean movesWithDoor(final int part, final boolean[] ofDoor) {
        for (final int bone : this.bonesOf[part]) {
            String name = this.model.bones().get(bone).name();
            while (name != null) {
                final int at = indexOf(name);
                if (at < 0) {
                    break;
                }
                if (ofDoor[at]) {
                    return true;
                }
                name = this.model.bones().get(at).parent();
            }
        }
        return false;
    }

    private int indexOf(final String bone) {
        for (int i = 0; i < this.model.bones().size(); i++) {
            if (this.model.bones().get(i).name().equals(bone)) {
                return i;
            }
        }
        return -1;
    }

    /* The box of {@code cube} of a bone placed by {@code bone}: turned about its own pivot when it is turned. */
    private static Box box(final Matrix4f bone, final ModelCube cube) {
        final Matrix4f placed = new Matrix4f(bone);
        final Vector3fc turn = cube.rotation();
        if (turn.x() != 0 || turn.y() != 0 || turn.z() != 0) {
            final Vector3fc pivot = cube.pivot();
            placed.translate(pivot.x(), pivot.y(), pivot.z()).rotate(Rotations.ofEuler(turn))
                    .translate(-pivot.x(), -pivot.y(), -pivot.z());
        }
        final float grow = cube.inflate();
        final Vector3f min = new Vector3f(cube.from()).sub(grow, grow, grow);
        final Vector3f max = new Vector3f(cube.from()).add(cube.size()).add(grow, grow, grow);
        return new Box(placed, min, max);
    }

    /*
     * How far along the line from {@code start}, running {@code way}, from 0 to 1, it first meets the box from
     * {@code low} to {@code high} grown by {@code slack} on every side, all in the box's own frame; 0 from inside it,
     * and -1 when it misses.
     */
    private static float slab(final Vector3fc start, final Vector3fc way, final Vector3fc low, final Vector3fc high,
                              final float slack) {
        float near = 0;
        float far = 1;
        for (int axis = 0; axis < 3; axis++) {
            final float least = low.get(axis) - slack;
            final float most = high.get(axis) + slack;
            final float s = start.get(axis);
            final float d = way.get(axis);
            if (Math.abs(d) < 1.0e-9F) {
                if (s < least || s > most) {
                    return -1;
                }
                continue;
            }
            float enter = (least - s) / d;
            float leave = (most - s) / d;
            if (enter > leave) {
                final float swap = enter;
                enter = leave;
                leave = swap;
            }
            near = Math.max(near, enter);
            far = Math.min(far, leave);
            if (near > far) {
                return -1;
            }
        }
        return near;
    }

    /* How far along the line the first of {@code boxes}, each grown by {@code slack}, meets it; -1 when none does. */
    private static float along(final List<Box> boxes, final Vector3fc from, final Vector3fc to, final float slack) {
        float best = -1;
        for (final Box box : boxes) {
            final float at = box.along(from, to, slack);
            if (at >= 0 && (best < 0 || at < best)) {
                best = at;
            }
        }
        return best;
    }

    /**
     * A box of a part: the cube from {@code min} to {@code max} in the model's pixels at rest, placed by
     * {@code toModel} where the part stands now.
     *
     * @param toModel from the cube's place at rest to where it stands now, in the model's pixels
     * @param min     its lowest corner at rest
     * @param max     its highest corner at rest
     */
    public record Box(Matrix4f toModel, Vector3f min, Vector3f max) {

        /** Its eight corners where it stands now, the first four at its lowest Y, in the model's pixels. */
        public List<Vector3f> corners() {
            final List<Vector3f> out = new ArrayList<>(8);
            for (final float y : new float[] {this.min.y, this.max.y}) {
                out.add(this.toModel.transformPosition(new Vector3f(this.min.x, y, this.min.z)));
                out.add(this.toModel.transformPosition(new Vector3f(this.max.x, y, this.min.z)));
                out.add(this.toModel.transformPosition(new Vector3f(this.max.x, y, this.max.z)));
                out.add(this.toModel.transformPosition(new Vector3f(this.min.x, y, this.max.z)));
            }
            return out;
        }

        /**
         * How far along the line from {@code from} to {@code to}, from 0 to 1, it first meets the box grown by
         * {@code slack} pixels on every side; 0 from inside it, and -1 when it misses.
         */
        public float along(final Vector3fc from, final Vector3fc to, final float slack) {
            final Matrix4f back = new Matrix4f(this.toModel).invert();
            final Vector3f start = back.transformPosition(new Vector3f(from));
            final Vector3f way = back.transformPosition(new Vector3f(to)).sub(start);
            return slab(start, way, this.min, this.max, slack);
        }
    }

    /**
     * What a line of sight met: the part, and how far along the line, from 0 to 1.
     *
     * @param part  the part, by its place among the parts
     * @param along how far along the line
     */
    public record Aim(int part, float along) {
    }

    /* A pose of the model and its bones' places, worked again for each question. */
    private static final class Work {

        private final ModelPose pose;
        private final Matrix4f[] world;

        private Work(final ModelData model) {
            this.pose = new ModelPose(model);
            this.world = new Matrix4f[model.bones().size()];
            for (int i = 0; i < this.world.length; i++) {
                this.world[i] = new Matrix4f();
            }
        }

        private void pose(final HandGeometry geometry, final float[] open) {
            this.pose.reset();
            geometry.poseDoors(this.pose, open);
            this.pose.world(this.world);
        }
    }
}
