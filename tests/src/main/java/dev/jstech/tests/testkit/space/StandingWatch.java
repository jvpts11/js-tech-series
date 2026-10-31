/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.space;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Where the player is drawn on a moving structure, every frame the player's game draws: their place as drawn, between
 * their last two ticks, in the yard of the structure as drawn then. What a video of the game shows frame by frame,
 * as numbers a client test asserts on.
 */
public final class StandingWatch {

    private final int plot;
    private final ClientTestContext ctx;
    private final List<Vec3> onIt = new ArrayList<>();
    /* When each frame was drawn: the player's tick and how far into the next. */
    private final List<String> when = new ArrayList<>();
    /* The block the player aimed at in each frame, none aimed at none. */
    private final List<BlockPos> aims = new ArrayList<>();
    private final Consumer<RenderFrameEvent.Post> listener = this::frame;
    /* Below this many frames a watch saw too little to tell anything. */
    private static final int FEWEST = 3;

    private StandingWatch(final int plotOf, final ClientTestContext test) {
        this.plot = plotOf;
        this.ctx = test;
    }

    /** Starts watching the player on {@code structure}, every frame. */
    public static StandingWatch of(final MovingStructure structure, final ClientTestContext test) {
        final StandingWatch watch = new StandingWatch(structure.plot(), test);
        NeoForge.EVENT_BUS.addListener(watch.listener);
        return watch;
    }

    /** Stops watching. */
    public void stop() {
        NeoForge.EVENT_BUS.unregister(listener);
    }

    /** Asserts the player kept to one place on the structure, as drawn, within {@code most} blocks. */
    public void assertSteady(final double most) {
        ctx.assertTrue(onIt.size() > FEWEST, "the player was drawn in " + onIt.size() + " frames");
        final Vec3 first = onIt.getFirst();
        double furthest = 0;
        for (final Vec3 each : onIt) {
            furthest = Math.max(furthest, each.distanceTo(first));
        }
        ctx.assertTrue(furthest < most, "standing still on the deck in flight, the player is drawn steady on it,"
                + " frame after frame: they wandered " + furthest + " blocks on it in " + onIt.size() + " frames");
    }

    /**
     * Asserts the player went from where they were drawn first to where they were drawn last in one step, as from a
     * seat to where they got up: every frame at one place or the other, within {@code most} blocks, none between.
     */
    public void assertNoFrameBetween(final double most) {
        ctx.assertTrue(onIt.size() > FEWEST, "the player was drawn in " + onIt.size() + " frames");
        final Vec3 first = onIt.getFirst();
        final Vec3 last = onIt.getLast();
        boolean arrived = false;
        final List<String> between = new ArrayList<>();
        for (int k = 0; k < onIt.size(); k++) {
            final Vec3 each = onIt.get(k);
            arrived |= each.distanceTo(last) < most;
            final boolean placed = arrived ? each.distanceTo(last) < most : each.distanceTo(first) < most;
            if (!placed) {
                between.add(k + " " + each.subtract(last) + " at " + when.get(k) + "; before " + (k > 0
                        ? onIt.get(k - 1).subtract(last) + " at " + when.get(k - 1) : "none") + "; after "
                        + (k + 1 < onIt.size() ? onIt.get(k + 1).subtract(last) + " at " + when.get(k + 1) : "none"));
            }
        }
        ctx.assertTrue(between.isEmpty(), "the player is drawn where they were or where they went, never between: "
                + between.size() + " frames of " + onIt.size() + " between, off where they went by " + between
                + "; from " + first + " to " + last);
    }

    /**
     * Asserts the player's aim, standing still on the structure, stayed on one block of it frame after frame, as the
     * outline round it is drawn: never on its neighbour, never off it.
     */
    public void assertAimHeld() {
        ctx.assertTrue(aims.size() > FEWEST, "the player aimed in " + aims.size() + " frames");
        final BlockPos first = aims.getFirst();
        ctx.assertTrue(first != null && YardLayout.isYardBlock(first.getX()), "the player aims at a block of the"
                + " structure: " + first);
        int moved = 0;
        for (final BlockPos each : aims) {
            moved += first.equals(each) ? 0 : 1;
        }
        ctx.assertTrue(moved == 0, "standing still on the deck, the player's aim stays on one block of it, its"
                + " outline steady: it left " + first + " in " + moved + " frames of " + aims.size() + ": " + aims);
    }

    private void frame(final RenderFrameEvent.Post event) {
        final ClientStructure seen = ClientMovingStructures.byPlot(plot);
        if (seen == null || ctx.player() == null) {
            return;
        }
        final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        final StructureFrame frame = new StructureFrame(seen.drawnAt(partial), seen.pivotX(), seen.pivotY(),
                seen.pivotZ());
        final Vec3 at = ctx.player().getPosition(partial);
        onIt.add(new Vec3(frame.yardX(at.x, at.y, at.z), frame.yardY(at.x, at.y, at.z),
                frame.yardZ(at.x, at.y, at.z)));
        aims.add(ctx.mc().hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos() : null);
        when.add("tick " + ctx.player().tickCount + " partial " + String.format(Locale.ROOT, "%.2f", partial)
                + " ship z " + String.format(Locale.ROOT, "%.2f>%.2f", seen.previousFrame().pose().z(),
                seen.frame().pose().z()) + " tipped " + String.format(Locale.ROOT, "%.1f", Math.toDegrees(Math.acos(
                Math.min(1, seen.frame().turnY(0, 1, 0))))) + " yaw " + String.format(Locale.ROOT, "%.1f",
                seen.frame().pose().yawDegrees()) + " player z " + String.format(Locale.ROOT, "%.2f>%.2f",
                ctx.player().zo, ctx.player().getZ()));
    }
}
