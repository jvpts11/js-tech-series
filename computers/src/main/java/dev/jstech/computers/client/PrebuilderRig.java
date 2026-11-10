/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.google.gson.JsonObject;
import dev.jstech.computers.client.chain.ChainRigs;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Prebuilder's rig as its model's file writes it beside the model (computers/docs/design/survival.md), read into
 * its values once a file: everything in blocks from the middle of its terminal's floor, the machine facing north.
 *
 * @param arms       the left arm and the right one
 * @param trays      the left tray (the left arm's) and the right one
 * @param drawnIn    the telescope's length drawn in, one stage long
 * @param stages     how many stages the telescope has
 * @param cup        the suction cup's radius as its model draws it
 * @param cupHead    the half size of the cup's head as its model draws it
 * @param jawSpan    the two-finger head's half span as its model draws it
 * @param finger     a finger's half length as its model draws it
 * @param reach      how far a finger reaches out of the head as its model draws it
 * @param open       how far each finger stands off when the gripper is open
 * @param caseAt     the middle of the case's floor on the lift, up
 * @param caseTurn   how the case is turned on the lift, its open side to the front
 * @param openSide   where the case's open side is along the machine's depth
 * @param sink       how far the lift goes down into the base
 * @param hover      how far over a part the gripper waits before it goes down to it
 * @param gap        from the gripper's face to the face of the part it holds
 * @param clear      the height a carried part keeps above: the case, the back wall, the tower
 * @param rise       the share of the mock's loop the lift takes to come up
 * @param first      when in the mock's loop the first part is taken
 * @param each       the share of the mock's loop one part's trip takes
 * @param spacing    the share of the mock's loop between one arm's trip and the other's
 */
record PrebuilderRig(Arm[] arms, Tray[] trays, float drawnIn, int stages, float cup, float cupHead, float jawSpan,
                     float finger, float reach, float open, Vector3f caseAt, Quaternionf caseTurn, float openSide,
                     float sink, float hover, float gap, float clear, float rise, float first, float each,
                     float spacing) {

    /* The last file read, and its values. */
    private static @Nullable JsonObject readFrom;
    private static @Nullable PrebuilderRig read;

    /** The Prebuilder's rig, or none while its file is missing. */
    static @Nullable PrebuilderRig of() {
        final JsonObject file = ChainRigs.of("prebuilder");
        if (file != readFrom) {
            readFrom = file;
            read = file.has("arms") ? parse(file) : null;
        }
        return read;
    }

    private static PrebuilderRig parse(final JsonObject file) {
        final JsonObject arms = file.getAsJsonObject("arms");
        final JsonObject trays = file.getAsJsonObject("trays");
        final JsonObject telescope = file.getAsJsonObject("telescope");
        final JsonObject heads = file.getAsJsonObject("heads");
        final JsonObject theCase = file.getAsJsonObject("case");
        final JsonObject reach = file.getAsJsonObject("reach");
        final JsonObject loop = file.getAsJsonObject("loop");
        return new PrebuilderRig(new Arm[] {Arm.of(arms.getAsJsonObject("a")), Arm.of(arms.getAsJsonObject("b"))},
                new Tray[] {Tray.of(trays.getAsJsonObject("left")), Tray.of(trays.getAsJsonObject("right"))},
                ChainRigs.number(telescope, "drawn_in"), (int) ChainRigs.number(telescope, "stages"),
                ChainRigs.number(heads, "cup"), ChainRigs.number(heads, "cup_head"),
                ChainRigs.number(heads, "jaw_span"), ChainRigs.number(heads, "finger"),
                ChainRigs.number(heads, "finger_reach"), ChainRigs.number(heads, "open"),
                ChainRigs.point(theCase, "at"), ChainRigs.turn(theCase, "turn"),
                ChainRigs.number(theCase, "open_side"), ChainRigs.number(theCase, "sink"),
                ChainRigs.number(reach, "hover"), ChainRigs.number(reach, "gap"), ChainRigs.number(reach, "clear"),
                ChainRigs.number(loop, "rise"), ChainRigs.number(loop, "first"), ChainRigs.number(loop, "each"),
                ChainRigs.number(loop, "spacing"));
    }

    /**
     * One arm: where its turret stands and its shoulder's pin, its upper arm's and forearm's lengths pin to pin,
     * where its wrist rests and the height it moves at empty; and its cylinders' and hose's ends, each in the frame
     * of the piece it hangs on (the turret's, the upper arm's, the forearm's), with the hose's length at rest.
     */
    record Arm(Vector3f foot, Vector3f shoulder, float upper, float fore, Vector3f home, float transit,
               Vector3f liftA, Vector3f liftB, Vector3f foreA, Vector3f foreB, Vector3f hoseA, Vector3f hoseB,
               float hoseRest) {

        static Arm of(final JsonObject arm) {
            return new Arm(ChainRigs.point(arm, "foot"), ChainRigs.point(arm, "shoulder"),
                    ChainRigs.number(arm, "upper"), ChainRigs.number(arm, "fore"), ChainRigs.point(arm, "home"),
                    ChainRigs.number(arm, "transit"), ChainRigs.point(arm, "lift_a"), ChainRigs.point(arm, "lift_b"),
                    ChainRigs.point(arm, "fore_a"), ChainRigs.point(arm, "fore_b"), ChainRigs.point(arm, "hose_a"),
                    ChainRigs.point(arm, "hose_b"), ChainRigs.number(arm, "hose_rest"));
        }
    }

    /** A kitting tray: its top's least corner and its most, up, and how far it sinks with a finished computer. */
    record Tray(Vector3f min, Vector3f max, float drop) {

        static Tray of(final JsonObject tray) {
            return new Tray(ChainRigs.point(tray, "min"), ChainRigs.point(tray, "max"),
                    ChainRigs.number(tray, "drop"));
        }
    }
}
