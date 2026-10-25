/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import dev.jstech.core.model.BedrockReader;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.multiblock.Orientation;
import java.util.List;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class HandGeometryTest {

    /*
     * A door a pixel thick, sixteen long, at x -23 to -22 as the Core reads it; a place behind it, drawn by a bone for
     * each of two things it may hold; a button out in front; a lever that draws nothing; and a latch on the door's
     * outer face, hanging from it. The door's hinge is at its outer face, at its near end, and it opens a quarter turn
     * outward.
     */
    private static final String MODEL = """
            {"format_version": "1.12.0", "minecraft:geometry": [{
              "description": {"identifier": "geometry.hands", "texture_width": 16, "texture_height": 16},
              "bones": [
                {"name": "door_0", "pivot": [0, 0, 0], "cubes": [{"origin": [22, 0, 0], "size": [1, 16, 16],
                  "uv": [0, 0]}]},
                {"name": "place_0_part", "pivot": [0, 0, 0], "cubes": [{"origin": [10, 4, 4], "size": [8, 8, 8],
                  "uv": [0, 0]}]},
                {"name": "place_0_other", "pivot": [0, 0, 0], "cubes": [{"origin": [10, 4, 2], "size": [4, 4, 4],
                  "uv": [0, 0]}]},
                {"name": "button_start", "pivot": [0, 0, 0], "cubes": [{"origin": [-2, 0, -10], "size": [2, 2, 2],
                  "uv": [0, 0]}]},
                {"name": "lever", "pivot": [0, 0, 0]},
                {"name": "latch", "parent": "door_0", "pivot": [0, 0, 0], "cubes": [{"origin": [23, 6, 12],
                  "size": [1, 2, 2], "uv": [0, 0]}]}
              ]}]}
            """;
    private static final float CLOSE = 1.0e-4F;

    @Test
    void aim_meetsTheShutDoorBeforeThePlaceBehindIt() {
        final HandGeometry geometry = geometry();
        final Optional<HandGeometry.Aim> aim = geometry.aim(new Vector3f(-40, 8, 8), new Vector3f(0, 8, 8),
                new float[] {0});
        assertTrue(aim.isPresent());
        assertEquals(0, aim.get().part(), "the door is met first");
        assertEquals(17 / 40F, aim.get().along(), CLOSE, "at its outer face");
        assertFalse(geometry.reached(1, new float[] {0}), "the place behind it is out of reach while it is shut");
    }

    @Test
    void aim_reachesThePlaceOnceItsDoorHasOpenedOutOfTheWay() {
        final HandGeometry geometry = geometry();
        final Optional<HandGeometry.Aim> aim = geometry.aim(new Vector3f(-40, 8, 8), new Vector3f(0, 8, 8),
                new float[] {1});
        assertTrue(aim.isPresent());
        assertEquals(1, aim.get().part(), "the place, its door open");
        assertEquals(22 / 40F, aim.get().along(), CLOSE);
    }

    @Test
    void aim_reachesAPlaceOnlyThroughItsDoorway() {
        final HandGeometry geometry = geometry();
        assertTrue(geometry.aim(new Vector3f(40, 8, 8), new Vector3f(-40, 8, 8), new float[] {1}).isEmpty(),
                "from the far side, the place is behind the structure's wall, not its doorway");
        assertFalse(geometry.meets(1, new Vector3f(40, 8, 8), new Vector3f(-40, 8, 8), new float[] {1}, 0));
        assertTrue(geometry.meets(1, new Vector3f(-40, 8, 8), new Vector3f(0, 8, 8), new float[] {1}, 0));
    }

    @Test
    void boxes_turnTheDoorAQuarterTurnOutOnItsHinge() {
        final HandGeometry geometry = geometry();
        final Optional<HandGeometry.Aim> aim = geometry.aim(new Vector3f(-31, 8, -10), new Vector3f(-31, 8, 10),
                new float[] {1});
        assertTrue(aim.isPresent() && aim.get().part() == 0, "open, the door stands out across the line: " + aim);
        assertEquals(0.5F, aim.get().along(), CLOSE, "its inner face on the hinge's line");
        final List<HandGeometry.Box> open = geometry.boxes(0, new float[] {1});
        float least = Float.MAX_VALUE;
        for (final Vector3f corner : open.getFirst().corners()) {
            least = Math.min(least, corner.x);
        }
        assertEquals(-39, least, CLOSE, "its far end sixteen pixels out from the hinge");
        assertTrue(geometry.aim(new Vector3f(-31, 8, -10), new Vector3f(-31, 8, 10), new float[] {0}).isEmpty(),
                "shut, it stands along the side and the line misses it");
    }

    @Test
    void aim_findsAPartHangingFromADoorWhereTheDoorHasSwungIt() {
        final HandGeometry geometry = new HandGeometry(model(), List.of(
                new IHandPart.Door("door_0", Hinge.vertical(-23, 0).turns(-90)),
                new IHandPart.Control("latch", 0)));
        final Optional<HandGeometry.Aim> open = geometry.aim(new Vector3f(-36, 7, -10), new Vector3f(-36, 7, 10),
                new float[] {1});
        assertTrue(open.isPresent() && open.get().part() == 1, "the latch, swung out with its door: " + open);
        assertEquals(0.45F, open.get().along(), CLOSE, "on the door's outer face, before the door");
        final Optional<HandGeometry.Aim> shut = geometry.aim(new Vector3f(-40, 7, 13), new Vector3f(0, 7, 13),
                new float[] {0});
        assertTrue(shut.isPresent() && shut.get().part() == 1, "shut, it stands on the door's face: " + shut);
        assertEquals(0.4F, shut.get().along(), CLOSE);
    }

    @Test
    void meets_growsTheBoxesByTheSlackGiven() {
        final HandGeometry geometry = geometry();
        assertFalse(geometry.meets(2, new Vector3f(-5, 1, -9), new Vector3f(-0.5F, 1, -9), new float[] {0}, 0));
        assertTrue(geometry.meets(2, new Vector3f(-5, 1, -9), new Vector3f(-0.5F, 1, -9), new float[] {0}, 1));
    }

    @Test
    void poseDoors_turnsThePosesDoorAboutItsHinge() {
        final HandGeometry geometry = geometry();
        final ModelPose pose = new ModelPose(model());
        geometry.poseDoors(pose, new float[] {1});
        final Matrix4f[] world = new Matrix4f[pose.bones()];
        for (int i = 0; i < world.length; i++) {
            world[i] = new Matrix4f();
        }
        pose.world(world);
        final Vector3f farEnd = world[pose.index("door_0")].transformPosition(new Vector3f(-23, 0, 16));
        assertEquals(-39, farEnd.x, CLOSE, "the shut door's far end swings out sixteen pixels");
        assertEquals(0, farEnd.z, CLOSE, "to the hinge's line");
    }

    @Test
    void outline_boundsAPlacesBonesInOneBoxAndTurnsWithItsDoor() {
        final HandGeometry geometry = geometry();
        final HandGeometry.Box place = geometry.outline(1, new float[] {1});
        assertEquals(-18, place.min().x, CLOSE, "round both of the place's bones");
        assertEquals(4, place.min().y, CLOSE);
        assertEquals(2, place.min().z, CLOSE, "the second bone's near end");
        assertEquals(-10, place.max().x, CLOSE);
        assertEquals(12, place.max().y, CLOSE);
        assertEquals(12, place.max().z, CLOSE, "the first bone's far end");
        float least = Float.MAX_VALUE;
        for (final Vector3f corner : geometry.outline(0, new float[] {1}).corners()) {
            least = Math.min(least, corner.x);
        }
        assertEquals(-39, least, CLOSE, "the open door outlined where it has swung to");
    }

    @Test
    void placed_standsTheModelOnItsTerminalsFloorTurnedAsTheStructureStands() {
        final Vector3f north = HandGeometry.placed(Orientation.NORTH, 1, new Matrix4f())
                .transformPosition(new Vector3f(16, 0, 0));
        assertEquals(1.5F, north.x, CLOSE);
        assertEquals(0, north.y, CLOSE, "on the terminal's floor");
        assertEquals(0.5F, north.z, CLOSE);
        final Vector3f east = HandGeometry.placed(Orientation.EAST, 1, new Matrix4f())
                .transformPosition(new Vector3f(0, 0, 16));
        assertEquals(-0.5F, east.x, CLOSE, "facing east, the model's length runs west");
        assertEquals(0.5F, east.z, CLOSE);
        final Vector3f twice = HandGeometry.placed(Orientation.NORTH, 2, new Matrix4f())
                .transformPosition(new Vector3f(16, 0, 0));
        assertEquals(2.5F, twice.x, CLOSE, "a model twice as big reaches twice as far");
    }

    @Test
    void geometry_refusesAPartWithNoBoneOrNothingDrawnAndAPlaceBehindNoDoor() {
        assertThrows(IllegalArgumentException.class, () -> new HandGeometry(model(),
                List.of(new IHandPart.Control("button_missing", 0))));
        assertThrows(IllegalArgumentException.class, () -> new HandGeometry(model(),
                List.of(new IHandPart.Control("lever", 0))), "a bone with no cube cannot be seen to be worked");
        assertThrows(IllegalArgumentException.class, () -> new HandGeometry(model(),
                List.of(new IHandPart.Place("place_0", 0, "door_9"))));
    }

    private static HandGeometry geometry() {
        return new HandGeometry(model(), List.of(
                new IHandPart.Door("door_0", Hinge.vertical(-23, 0).turns(-90)),
                new IHandPart.Place("place_0", 0, "door_0"),
                new IHandPart.Control("button_start", 0)));
    }

    private static ModelData model() {
        return BedrockReader.geometry("hands", JsonParser.parseString(MODEL).getAsJsonObject()).getFirst();
    }
}
