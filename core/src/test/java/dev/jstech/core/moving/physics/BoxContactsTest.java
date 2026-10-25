/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class BoxContactsTest {

    private static final double CLOSE = 1.0e-6;

    @Test
    void collide_boxesApartBeyondTheMarginMeetNowhere() {
        final List<Found> found = new ArrayList<>();
        final int count = new BoxContacts().collide(cube(0, 2, 0), cube(0, 0, 0), 0.5, sink(found));
        assertEquals(0, count);
        assertTrue(found.isEmpty());
    }

    @Test
    void collide_aCubeLyingFlatOnAnotherMeetsAtFourPoints() {
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(cube(0, 1, 0), cube(0, 0, 0), 0.1, sink(found));
        assertEquals(4, found.size());
        for (final Found each : found) {
            assertEquals(1, each.way.y, CLOSE);
            assertEquals(0, each.apart, CLOSE);
            assertEquals(0.5, each.at.y, CLOSE);
        }
    }

    @Test
    void collide_aCubeSunkIntoAnotherIsAsDeepAsItSank() {
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(cube(0.2, 0.9, 0.1), cube(0, 0, 0), 0.1, sink(found));
        assertTrue(found.size() >= 4);
        for (final Found each : found) {
            assertEquals(1, each.way.y, CLOSE);
            assertEquals(-0.1, each.apart, CLOSE);
        }
    }

    @Test
    void collide_aCubeStandingOnItsEdgeMeetsAtTwoPoints() {
        final OrientedBox edge = turned(new Vector3d(0, 0.5 + Math.sqrt(0.5), 0), new Vector3d(0, 0, 1),
                Math.PI / 4);
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(edge, ground(), 0.1, sink(found));
        assertEquals(2, found.size());
        for (final Found each : found) {
            assertEquals(1, each.way.y, CLOSE);
            assertEquals(0, each.at.x, CLOSE);
            assertEquals(0, each.apart, 1.0e-9);
        }
    }

    @Test
    void collide_aCubeTurnedAboutTheUpOnTheGroundMeetsAtItsFourCorners() {
        final OrientedBox turned = turned(new Vector3d(0, 1, 0), new Vector3d(0, 1, 0), Math.toRadians(30));
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(turned, ground(), 0.1, sink(found));
        assertEquals(4, found.size());
        for (final Found each : found) {
            assertEquals(1, each.way.y, CLOSE);
            assertEquals(0.5, each.at.y, CLOSE);
        }
    }

    @Test
    void collide_aCubeTurnedAHairOnAnotherOfItsSizeMeetsAtItsFourCorners() {
        /*
         * Turned a tenth of a degree and a hair aside: its edges cross the sides of the face below it, at its corners
         * and along them.
         */
        final OrientedBox hair = turned(new Vector3d(0.0005, 1, 0), new Vector3d(0, 1, 0), 0.002);
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(hair, cube(0, 0, 0), 0.1, sink(found));
        assertEquals(4, found.size(), "at its four corners, none cut in two or along an edge: " + found);
        for (final Found each : found) {
            assertEquals(0.5, Math.abs(each.at.x), 0.01, "at a corner: " + each.at);
            assertEquals(0.5, Math.abs(each.at.z), 0.01, "at a corner: " + each.at);
        }
    }

    @Test
    void collide_aCubeHangingOverTheEdgeMeetsAllOverTheFaceItShares() {
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(cube(0.7, 1, 0.6), cube(0, 0, 0), 0.1, sink(found));
        assertEquals(4, found.size());
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        for (final Found each : found) {
            minX = Math.min(minX, each.at.x);
            maxX = Math.max(maxX, each.at.x);
        }
        assertEquals(0.2, minX, CLOSE);
        assertEquals(0.5, maxX, CLOSE);
    }

    @Test
    void collide_twoEdgesCrossingMeetAtOnePointBetweenThem() {
        final OrientedBox upper = turned(new Vector3d(0, 0.5 + Math.sqrt(0.5) - 0.01, 0), new Vector3d(0, 0, 1),
                Math.PI / 4);
        final OrientedBox lower = turned(new Vector3d(0, -Math.sqrt(0.5) + 0.5, 0), new Vector3d(1, 0, 0),
                Math.PI / 4);
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(upper, lower, 0.1, sink(found));
        assertEquals(1, found.size());
        assertEquals(BoxContacts.EDGES, found.get(0).feature);
        assertEquals(1, found.get(0).way.y, CLOSE);
        assertEquals(0.5, found.get(0).at.y, 0.01);
    }

    @Test
    void collide_aCubeFallingNearTheGroundMeetsItAheadOfTime() {
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(cube(0, 1.3, 0), cube(0, 0, 0), 0.5, sink(found));
        assertEquals(4, found.size());
        assertEquals(0.3, found.get(0).apart, CLOSE);
    }

    @Test
    void collide_theWayPointsFromTheSecondBoxToTheFirst() {
        final List<Found> found = new ArrayList<>();
        new BoxContacts().collide(cube(-0.95, 0, 0), cube(0, 0, 0), 0.1, sink(found));
        assertTrue(!found.isEmpty());
        assertEquals(-1, found.get(0).way.x, CLOSE);
        assertEquals(-0.05, found.get(0).apart, CLOSE);
    }

    private static OrientedBox cube(final double x, final double y, final double z) {
        return new OrientedBox().square(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5);
    }

    private static OrientedBox ground() {
        return new OrientedBox().square(-2, -0.5, -2, 2, 0.5, 2);
    }

    private static OrientedBox turned(final Vector3d centre, final Vector3d about, final double angle) {
        final Quaterniond turn = new Quaterniond().fromAxisAngleRad(about.x, about.y, about.z, angle);
        final Vector3d x = turn.transform(new Vector3d(1, 0, 0));
        final Vector3d y = turn.transform(new Vector3d(0, 1, 0));
        final Vector3d z = turn.transform(new Vector3d(0, 0, 1));
        return new OrientedBox().set(centre, x, y, z, 0.5, 0.5, 0.5);
    }

    private static BoxContacts.IPointSink sink(final List<Found> into) {
        return (at, way, apart, feature) -> into.add(new Found(new Vector3d(at), new Vector3d(way), apart, feature));
    }

    private record Found(Vector3d at, Vector3d way, double apart, int feature) {
    }
}
