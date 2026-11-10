/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.chain.machine.ComputerKit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * What the Prebuilder does with one kit (computers/docs/design/survival.md), worked out once a computer it builds, as
 * its approved mock moves it: the lift brings the empty case up with the two kitting trays; the parts go in the order
 * they go in, the board first, the two arms taking turns, each part from its arm's tray (the other arm's when it does
 * not fit there), standing with the face it is taken by turned up; each trip brings the wrist over the part, runs the
 * telescope down and takes it, carries it over the case turned as it will sit, stops with it outside the open side
 * and runs only the telescope in to set it on its seat. Then the computer sinks into the base with the trays.
 * A part with no seat to be brought to, or no model of its own, is in the case from the start.
 *
 * <p>Everything is in blocks in the machine's model, facing north; time is in the mock's loops, a job as long as its
 * trips need.
 */
final class PrebuilderJob {

    private final PrebuilderRig rig;
    private final List<Trip> trips;
    private final Trip[] bySlot;
    private final float sinkAt;
    private final float length;

    /* How long the finished computer takes to sink, and the trays, in the mock's loops. */
    private static final float SINKING = 0.14F;
    private static final float TRAYS_SINKING = 0.1F;
    private static final float PIXEL = 1F / 16F;
    private static final Vector3fc UP = new Vector3f(0, 1, 0);
    /* Two fingers close across the machine's width; a part goes into the case toward the back. */
    private static final Vector3fc ACROSS = new Vector3f(1, 0, 0);
    private static final Vector3fc INTO = new Vector3f(0, 0, 1);
    /* An empty gripper on the move: its axis level, to the back. */
    private static final Matrix3fc HOME_TOOL = new Matrix3f();
    /* A part thinner than this across the face it is taken by is taken by two fingers, a thicker one by the cup. */
    private static final float FINGERS_UNDER = 2 * PIXEL;
    /* Between the parts in a tray, and from its edges. */
    private static final float NEST_GAP = 0.5F * PIXEL;
    /* The shares of a trip at which the part is taken out of its tray and set on its seat. */
    private static final float TAKEN = 0.27F;
    private static final float SEATED = 0.76F;

    private PrebuilderJob(final PrebuilderRig rig, final List<Trip> trips, final int slots) {
        this.rig = rig;
        this.trips = List.copyOf(trips);
        this.bySlot = new Trip[slots];
        float end = rig.first();
        for (final Trip trip : trips) {
            bySlot[trip.slot()] = trip;
            end = Math.max(end, trip.t0() + rig.each());
        }
        this.sinkAt = trips.isEmpty() ? rig.first() + rig.each() : end;
        this.length = sinkAt + SINKING;
    }

    /**
     * The job of building the computer whose parts are {@code parts}, each to be brought to its seat in {@code seats}
     * (by its hardware slot's key, from the corner of the computer's block facing north).
     */
    static PrebuilderJob of(final PrebuilderRig rig, final ItemContainerContents parts,
                            final Map<String, Matrix4f> seats) {
        final Matrix4f caseCorner = new Matrix4f().translation(rig.caseAt()).rotate(rig.caseTurn())
                .translate(-0.5F, 0, -0.5F);
        final List<Integer> order = new ArrayList<>();
        for (int slot = 0; slot < parts.getSlots(); slot++) {
            if (!parts.getStackInSlot(slot).isEmpty()) {
                order.add(slot);
            }
        }
        order.sort(Comparator.comparingInt(slot -> ComputerKit.rank(parts.getStackInSlot(slot))));
        final Shelf[] shelves = {new Shelf(rig.trays()[0]), new Shelf(rig.trays()[1])};
        final float[] armFree = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        final List<Trip> trips = new ArrayList<>();
        float lastStart = rig.first() - rig.spacing();
        int preferred = 0;
        for (final int slot : order) {
            final ItemStack part = parts.getStackInSlot(slot);
            final ResourceLocation item = BuiltInRegistries.ITEM.getKey(part.getItem());
            final Matrix4f seat = seats.get(PartModels.Drawer.key(slot));
            final float[] reach = PartModels.reach(item);
            if (seat == null || reach == null) {
                continue;
            }
            // The arm is the one whose tray the part goes in; its trip starts once that arm is free.
            final Trip trip = Trip.plan(rig, slot, item, new Matrix4f(caseCorner).mul(seat), reach, shelves,
                    preferred);
            final float t0 = Math.max(lastStart + rig.spacing(), armFree[trip.arm()]);
            final Trip timed = trip.at(t0);
            trips.add(timed);
            lastStart = t0;
            armFree[timed.arm()] = t0 + rig.each();
            preferred = 1 - timed.arm();
        }
        return new PrebuilderJob(rig, trips, parts.getSlots());
    }

    /** How far into the job's loop it is when {@code done} of the computer is done. */
    float at(final float done) {
        return done * length;
    }

    /** Where the lift is, in blocks under its top (0 up, the rig's sink down), at loop time {@code a}. */
    float lift(final float a) {
        if (a < rig.rise()) {
            return -rig.sink() * (1 - ease(a / rig.rise()));
        }
        return a < sinkAt ? 0 : -rig.sink() * ease((a - sinkAt) / SINKING);
    }

    /** How far the trays are down, from 0 up to 1 down, at loop time {@code a}. */
    float trays(final float a) {
        if (a < rig.rise()) {
            return 1 - ease(a / rig.rise());
        }
        return a < sinkAt ? 0 : ease((a - sinkAt) / TRAYS_SINKING);
    }

    /** Whether the part in hardware slot {@code slot} is on its seat at loop time {@code a}. */
    boolean seated(final int slot, final float a) {
        final Trip trip = slot < bySlot.length ? bySlot[slot] : null;
        return trip == null || a >= trip.t0() + SEATED * rig.each();
    }

    /** The parts it brings, in the order it takes them. */
    List<Trip> trips() {
        return trips;
    }

    /** Whether {@code trip}'s part is still in its tray at loop time {@code a}. */
    boolean inTray(final Trip trip, final float a) {
        return a < trip.t0() + TAKEN * rig.each();
    }

    /** Where {@code trip}'s part stands in its tray, the tray {@code down} of the way down. */
    Matrix4f inTrayAt(final Trip trip, final float down) {
        final float drop = down * rig.trays()[trip.arm()].drop();
        return new Matrix4f().set(trip.trayTurn()).setTranslation(trip.trayAt().x(), trip.trayAt().y() - drop,
                trip.trayAt().z()).mul(trip.own());
    }

    /**
     * Arm {@code arm} at loop time {@code a}, the case {@code down} blocks down: its wrist, its tool's frame, the
     * telescope's length, how far its gripper stands open, the part it holds and where, and what it is tooled for.
     */
    Arm arm(final int arm, final float a, final float down) {
        final PrebuilderRig.Arm rigged = rig.arms()[arm];
        final Trip tooled = tooledFor(arm, a);
        for (final Trip trip : trips) {
            final float s = (a - trip.t0()) / rig.each();
            if (trip.arm() == arm && s >= 0 && s < 1) {
                return during(rigged, trip, s, down);
            }
        }
        return new Arm(new Vector3f(rigged.home()), new Matrix3f(HOME_TOOL), rig.drawnIn(), 1, null, null, tooled);
    }

    /** {@code x} eased in and out, as every move of the mock is: 0 below 0, 1 above 1. */
    static float ease(final float x) {
        final float clamped = Math.clamp(x, 0, 1);
        return (float) (0.5 - 0.5 * Math.cos(Math.PI * clamped));
    }

    /* The trip's moments as the mock's loop has them, each a share of the trip. */
    private Arm during(final PrebuilderRig.Arm arm, final Trip trip, final float s, final float down) {
        final float drawnIn = rig.drawnIn();
        final Vector3f front = new Vector3f(trip.front()).add(0, down, 0);
        final Vector3f above = new Vector3f(trip.hover().x(), arm.transit(), trip.hover().z());
        final Vector3f home = new Vector3f(arm.home());
        if (s < 0.16F) {
            // Over to the part at the transit height, the gripper turned down over it, then down to it.
            if (s < 0.09F) {
                return empty(swing(arm, home, above, ease(s / 0.09F), arm.transit()), HOME_TOOL, drawnIn, trip);
            }
            if (s < 0.12F) {
                return empty(above, between(HOME_TOOL, trip.toolTray(), ease((s - 0.09F) / 0.03F)), drawnIn, trip);
            }
            return empty(lerp(above, trip.hover(), ease((s - 0.12F) / 0.04F)), trip.toolTray(), drawnIn, trip);
        }
        if (s < TAKEN) {
            // The telescope down onto it, the gripper closing on it.
            final float ext = drawnIn + rig.hover() * ease((s - 0.16F) / 0.08F);
            return new Arm(new Vector3f(trip.hover()), new Matrix3f(trip.toolTray()), ext,
                    1 - ease((s - 0.24F) / 0.03F), null, null, trip);
        }
        if (s < 0.60F) {
            float ext = drawnIn;
            Matrix3f turn = new Matrix3f(trip.seatTurn());
            final Vector3f up = new Vector3f(trip.hover().x(), trip.high(), trip.hover().z());
            final Vector3f over = new Vector3f(front.x(), trip.high(), front.z());
            final Vector3f wrist;
            if (s < 0.33F) {
                wrist = new Vector3f(trip.hover());
                turn = new Matrix3f(trip.trayTurn());
                ext = drawnIn + rig.hover() * (1 - ease((s - TAKEN) / 0.06F));
            } else if (s < 0.37F) {
                wrist = lerp(trip.hover(), up, ease((s - 0.33F) / 0.04F));
                turn = new Matrix3f(trip.trayTurn());
            } else if (s < 0.42F) {
                // At the carrying height, the part turned as it will sit: tipped back until the tool is level, then
                // rolled about the tool's axis, so a long part never swings down.
                wrist = up;
                turn = carryTurn(trip, (s - 0.37F) / 0.05F);
            } else if (s < 0.54F) {
                wrist = swing(arm, up, over, ease((s - 0.42F) / 0.12F), trip.high());
            } else {
                wrist = lerp(over, front, ease((s - 0.54F) / 0.06F));
            }
            final Matrix3f tool = turn.mul(trip.grip().frame());
            return holding(trip, wrist, tool, ext, 0);
        }
        if (s < 0.79F) {
            // Only the telescope runs in, straight, and sets the part on its seat; the gripper lets go.
            final float ext = drawnIn + trip.stroke() * ease((s - 0.60F) / 0.16F);
            final float opened = ease((s - SEATED) / 0.03F);
            return s < SEATED ? holding(trip, front, trip.toolSeat(), ext, opened)
                    : new Arm(front, new Matrix3f(trip.toolSeat()), ext, opened, null, null, trip);
        }
        if (s < 0.86F) {
            final float ext = drawnIn + trip.stroke() * (1 - ease((s - 0.79F) / 0.07F));
            return new Arm(front, new Matrix3f(trip.toolSeat()), ext, 1, null, null, trip);
        }
        final Vector3f rise = new Vector3f(front.x(), arm.transit(), front.z());
        if (s < 0.90F) {
            return empty(lerp(front, rise, ease((s - 0.86F) / 0.04F)), trip.toolSeat(), drawnIn, trip);
        }
        final float x = ease((s - 0.90F) / 0.10F);
        return empty(swing(arm, rise, home, x, arm.transit()), between(trip.toolSeat(), HOME_TOOL, x), drawnIn,
                trip);
    }

    /* An arm holding nothing, its gripper open. */
    private static Arm empty(final Vector3f wrist, final Matrix3fc tool, final float ext, final Trip tooled) {
        return new Arm(wrist, new Matrix3f(tool), ext, 1, null, null, tooled);
    }

    /* The arm holding {@code trip}'s part: its near face the rig's gap beyond the gripper's face. */
    private Arm holding(final Trip trip, final Vector3f wrist, final Matrix3fc tool, final float ext,
                        final float opened) {
        final Matrix3f turn = new Matrix3f(tool).mul(new Matrix3f(trip.grip().frame()).transpose());
        final Vector3f near = tool.getColumn(2, new Vector3f()).mul(ext + rig.gap()).add(wrist);
        final Vector3f origin = near.sub(turn.transform(new Vector3f(trip.grip().near())));
        final Matrix4f at = new Matrix4f().set(turn).setTranslation(origin).mul(trip.own());
        return new Arm(new Vector3f(wrist), new Matrix3f(tool), ext, opened, trip, at, trip);
    }

    /* The trip this arm is tooled for: the one whose middle is nearest, or none for an arm with no trip. */
    private @Nullable Trip tooledFor(final int arm, final float a) {
        Trip best = null;
        for (final Trip trip : trips) {
            final boolean nearer = best == null || Math.abs(a - trip.middle(rig)) < Math.abs(a - best.middle(rig));
            if (trip.arm() == arm && nearer) {
                best = trip;
            }
        }
        return best;
    }

    /* The part tipped back until the tool is level, then rolled about the tool's axis to how it will sit. */
    private static Matrix3f carryTurn(final Trip trip, final float x) {
        return new Matrix3f().rotationZ(trip.phi() * ease((x - 0.45F) / 0.55F))
                .mul(new Matrix3f().rotationX((float) Math.toRadians(-90) * ease(x / 0.55F)))
                .mul(trip.trayTurn());
    }

    /*
     * The wrist from {@code from} to {@code to} as a robot moves, joint by joint: the turret slewing the short way
     * round, the reach and the height changing together, so it goes round the turret's axis and never over it; at
     * the height {@code height} throughout.
     */
    private static Vector3f swing(final PrebuilderRig.Arm arm, final Vector3fc from, final Vector3fc to,
                                  final float x, final float height) {
        final Vector3fc shoulder = arm.shoulder();
        final float ax = from.x() - shoulder.x();
        final float az = from.z() - shoulder.z();
        final float bx = to.x() - shoulder.x();
        final float bz = to.z() - shoulder.z();
        final double heading = Math.atan2(az, ax);
        double turn = Math.atan2(bz, bx) - heading + Math.PI;
        turn = turn - 2 * Math.PI * Math.floor(turn / (2 * Math.PI)) - Math.PI;
        final double y = heading + turn * x;
        final double r = Math.hypot(ax, az) * (1 - x) + Math.hypot(bx, bz) * x;
        return new Vector3f((float) (shoulder.x() + r * Math.cos(y)), height, (float) (shoulder.z() + r * Math.sin(y)));
    }

    private static Vector3f lerp(final Vector3fc from, final Vector3fc to, final float x) {
        return new Vector3f(from).lerp(to, x);
    }

    /* A frame between two, turned the shortest way. */
    private static Matrix3f between(final Matrix3fc from, final Matrix3fc to, final float x) {
        final Quaternionf a = new Quaternionf().setFromUnnormalized(from);
        final Quaternionf b = new Quaternionf().setFromUnnormalized(to);
        return new Matrix3f().set(a.slerp(b, x));
    }

    /**
     * An arm at one moment.
     *
     * @param wrist  the wrist's pin
     * @param tool   the tool's frame: across the fingers, beside, and along the telescope, as columns
     * @param ext    how far the telescope reaches out of the wrist
     * @param opened how far the gripper stands open, from 0 shut to 1 open
     * @param held   the trip whose part it holds, or none
     * @param heldAt where the part it holds stands, or none
     * @param tooled the trip its gripper is tooled for, or none for an arm that brings nothing
     */
    record Arm(Vector3f wrist, Matrix3f tool, float ext, float opened, @Nullable Trip held,
               @Nullable Matrix4f heldAt, @Nullable Trip tooled) {
    }

    /**
     * How a part is taken, in its own frame: the tool's frame (across the fingers, beside, and into the part, as
     * columns), the point on the tool's axis at the part's near face, the part's half size across the fingers and its
     * reach beside them, its depth along the tool, and whether a cup takes it rather than two fingers.
     */
    record Grip(Matrix3f frame, Vector3f near, float halfAcross, float besideLow, float besideHigh, float depth,
                float face, boolean cup) {

        /* The grip of a part whose corners are {@code corners}, sitting turned {@code seated}. */
        static Grip of(final Matrix3fc seated, final Vector3f[] corners) {
            final Matrix3f back = new Matrix3f(seated).transpose();
            final Vector3f into = back.transform(new Vector3f(INTO));
            final Vector3f across = back.transform(new Vector3f(ACROSS));
            final Vector3f beside = new Vector3f(into).cross(across);
            final Vector3f lo = new Vector3f(Float.MAX_VALUE);
            final Vector3f hi = new Vector3f(-Float.MAX_VALUE);
            for (final Vector3f corner : corners) {
                lo.min(corner);
                hi.max(corner);
            }
            final Vector3f middle = new Vector3f(lo).add(hi).mul(0.5F);
            final float[] alongInto = span(corners, into);
            final float[] alongAcross = span(corners, across);
            final float[] alongBeside = span(corners, beside);
            final Vector3f near = new Vector3f(into).mul(alongInto[0] - middle.dot(into)).add(middle);
            final float nearBeside = near.dot(beside);
            final float wide = alongAcross[1] - alongAcross[0];
            final float tall = alongBeside[1] - alongBeside[0];
            final float face = Math.min(wide, tall);
            return new Grip(new Matrix3f(across, beside, into), near, wide / 2, alongBeside[0] - nearBeside,
                    alongBeside[1] - nearBeside, alongInto[1] - alongInto[0], face, face >= FINGERS_UNDER);
        }

        /* The least and the most of the corners along {@code way}. */
        private static float[] span(final Vector3f[] corners, final Vector3fc way) {
            float lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;
            for (final Vector3f corner : corners) {
                lo = Math.min(lo, corner.dot(way));
                hi = Math.max(hi, corner.dot(way));
            }
            return new float[] {lo, hi};
        }
    }

    /**
     * One part's trip, its fixed points worked out once.
     *
     * @param slot      the hardware slot it goes into
     * @param item      the part
     * @param arm       the arm that brings it, 0 the left one
     * @param t0        when its trip starts, in the mock's loops
     * @param phi       how far it is turned about the upright in its tray, in radians
     * @param own       how the part's model stands in the part's own frame, turned over for a supply that sits so
     * @param grip      how it is taken
     * @param trayTurn  how its frame is turned in its tray
     * @param trayAt    where its frame is in its tray, the tray up
     * @param seatTurn  how its frame is turned on its seat
     * @param toolTray  the tool's frame taking it from its tray
     * @param toolSeat  the tool's frame setting it on its seat
     * @param hover     the wrist over it in its tray, before the telescope goes down
     * @param front     the wrist before the case, the part outside its open side
     * @param stroke    how far the telescope then runs in
     * @param high      the height it is carried at
     */
    record Trip(int slot, ResourceLocation item, int arm, float t0, float phi, Matrix4f own, Grip grip,
                Matrix3f trayTurn, Vector3f trayAt, Matrix3f seatTurn, Matrix3f toolTray, Matrix3f toolSeat,
                Vector3f hover, Vector3f front, float stroke, float high) {

        /*
         * The trip of a part sitting as {@code placed} has it, its model reaching {@code reach}, laid in the tray of
         * arm {@code preferred} or of the other when it does not fit there; it starts with the job until timed.
         */
        static Trip plan(final PrebuilderRig rig, final int slot, final ResourceLocation item, final Matrix4f placed,
                         final float[] reach, final Shelf[] shelves, final int preferred) {
            // The part's frame on its seat, a turn and a place; what is left (a supply turned over) is its own.
            final Matrix3f linear = placed.get3x3(new Matrix3f());
            if (linear.determinant() < 0) {
                linear.scale(1, -1, 1);
            }
            final Matrix3f seatTurn = new Matrix3f().set(new Quaternionf().setFromUnnormalized(linear));
            final Vector3f seatAt = placed.getTranslation(new Vector3f());
            final Matrix4f own = new Matrix4f().set(seatTurn).setTranslation(seatAt).invert().mul(placed);
            final Vector3f[] corners = new Vector3f[8];
            for (int c = 0; c < corners.length; c++) {
                corners[c] = own.transformPosition(new Vector3f(reach[(c & 1) == 0 ? 0 : 3],
                        reach[(c & 2) == 0 ? 1 : 4], reach[(c & 4) == 0 ? 2 : 5]).mul(PIXEL));
            }
            final Grip grip = Grip.of(seatTurn, corners);
            // In its tray the face it is taken by is turned up, then it is turned so it lies along the tray.
            final Matrix3f faceUp = new Matrix3f().rotationX((float) Math.toRadians(90)).mul(seatTurn);
            final float[] flat = extent(faceUp, corners);
            final float phi = flat[3] - flat[0] > flat[5] - flat[2] ? (float) Math.toRadians(90) : 0;
            final Matrix3f trayTurn = new Matrix3f().rotationY(phi).mul(faceUp);
            final float[] lying = extent(trayTurn, corners);
            final float wide = lying[3] - lying[0];
            final float deep = lying[5] - lying[2];
            final int arm = !shelves[preferred].fits(wide, deep) && shelves[1 - preferred].fits(wide, deep)
                    ? 1 - preferred : preferred;
            final PrebuilderRig.Tray tray = rig.trays()[arm];
            final float[] nest = shelves[arm].place(wide, deep);
            final Vector3f trayAt = new Vector3f(nest[0] - lying[0], tray.min().y() - lying[1], nest[1] - lying[2]);
            final Matrix3f toolTray = new Matrix3f(trayTurn).mul(grip.frame());
            final Matrix3f toolSeat = new Matrix3f(seatTurn).mul(grip.frame());
            final Vector3f nearTray = trayTurn.transform(new Vector3f(grip.near())).add(trayAt);
            final Vector3f nearSeat = seatTurn.transform(new Vector3f(grip.near())).add(seatAt);
            final float down = rig.gap() + rig.drawnIn();
            final Vector3f hover = toolTray.getColumn(2, new Vector3f()).mul(-(down + rig.hover())).add(nearTray);
            // The part waits with its far end a sixteenth outside the case, so the telescope alone takes it in.
            final float stroke = Math.max(0, nearSeat.z() - (rig.openSide() - PIXEL - grip.depth()));
            final Vector3f front = toolSeat.getColumn(2, new Vector3f()).mul(-(down + stroke)).add(nearSeat);
            // Carried as it will sit, its lowest point over the case's top and the tower's.
            final float[] seated = extent(seatTurn, corners);
            final float below = nearSeat.y() - (seatAt.y() + seated[1]);
            final float high = Math.max(Math.max(rig.clear() + below + 0.5F * PIXEL, rig.clear() + 1.5F * PIXEL),
                    hover.y() + PIXEL);
            return new Trip(slot, item, arm, 0, phi, own, grip, trayTurn, trayAt, seatTurn, toolTray, toolSeat,
                    hover, front, stroke, high);
        }

        /** The same trip starting at {@code start}. */
        Trip at(final float start) {
            return new Trip(slot, item, arm, start, phi, own, grip, trayTurn, trayAt, seatTurn, toolTray, toolSeat,
                    hover, front, stroke, high);
        }

        float middle(final PrebuilderRig rig) {
            return t0 + rig.each() / 2;
        }

        /* The least and the most of the corners turned by {@code turn}, x, y and z, then x, y and z. */
        private static float[] extent(final Matrix3fc turn, final Vector3f[] corners) {
            final Vector3f lo = new Vector3f(Float.MAX_VALUE);
            final Vector3f hi = new Vector3f(-Float.MAX_VALUE);
            for (final Vector3f corner : corners) {
                final Vector3f turned = turn.transform(new Vector3f(corner));
                lo.min(turned);
                hi.max(turned);
            }
            return new float[] {lo.x(), lo.y(), lo.z(), hi.x(), hi.y(), hi.z()};
        }
    }

    /*
     * A kitting tray filling up as the mock lays it: rows along its length, from its front, each row's parts side by
     * side from its left as one stands before it.
     */
    static final class Shelf {

        private final PrebuilderRig.Tray tray;
        private float rowZ;
        private float rowDepth;
        private float x;

        Shelf(final PrebuilderRig.Tray tray) {
            this.tray = tray;
            this.rowZ = tray.min().z() + NEST_GAP;
            this.x = tray.max().x() - NEST_GAP;
        }

        /* Whether a part {@code wide} across and {@code deep} along the tray still goes in it. */
        boolean fits(final float wide, final float deep) {
            return wide <= tray.max().x() - tray.min().x() - 2 * NEST_GAP
                    && (newRow(wide) ? rowZ + rowDepth + NEST_GAP : rowZ) + deep <= tray.max().z() - NEST_GAP;
        }

        /* Lays a part {@code wide} by {@code deep} in the tray: its least x and z; in a new row past a full one. */
        float[] place(final float wide, final float deep) {
            if (newRow(wide)) {
                rowZ += rowDepth + NEST_GAP;
                rowDepth = 0;
                x = tray.max().x() - NEST_GAP;
            }
            final float[] at = {x - wide, rowZ};
            x -= wide + NEST_GAP;
            rowDepth = Math.max(rowDepth, deep);
            return at;
        }

        /* Whether a part {@code wide} across starts a new row, the row it would go in being full. */
        private boolean newRow(final float wide) {
            return rowDepth > 0 && x - wide < tray.min().x() + NEST_GAP;
        }
    }
}
