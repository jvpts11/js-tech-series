/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.client.multiblock.PlanArmsRenderer;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import dev.jstech.space.client.ship.ShipHoles;
import dev.jstech.space.client.ship.ShipPartsRenderer;
import dev.jstech.space.rocket.CabinReadings;
import dev.jstech.space.ship.GearMotion;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.shipyard.ShipyardBlocks;
import dev.jstech.space.shipyard.ShipyardDoors;
import dev.jstech.space.shipyard.ShipyardStructure;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * Draws a formed Small Shipyard whole from its terminal: the dock's frame, as its approved drawing has it, and with it
 * its crane, its floor and its cradle, and the control booth's glass, which shapes none of its blocks. Nothing of the
 * dock is drawn while it is not formed: its blocks show. Formed, its robot arms at work on their rails. As it opens
 * (space/docs/design/starships.md), its crane rolls back along its rails, its hook drawn up, then its floor rises to
 * the ship's gear, the cradle still holding the ship, and only then does the cradle let go and fold flat into the
 * floor, the rams sinking and the jaws swinging out; closing, the other way, the cradle holding the ship before the
 * floor goes down; all drawn as far on as the dock has gone, so they move with the blocks that move, and the ship is
 * never drawn in the air. And the ship its plan builds, each part formed
 * into its model where the plan lays it, painted as the plan says, whether or not the dock stands whole, a part just
 * formed glowing round its box for a moment; its gear stowed in its cradle, coming down as the floor rises to meet
 * it and folding as the floor goes down.
 */
public final class ShipyardRenderer implements BlockEntityRenderer<ShipyardTerminalBlockEntity> {

    private final KitBlockEntityRenderer<ShipyardTerminalBlockEntity> dock;

    /* How far it is drawn from, in blocks: as far as a dock is seen across a spaceport. */
    private static final int SEEN_FROM = 192;
    /* How far from its terminal the dock reaches, and the ship it builds, in blocks. */
    private static final int DOCK_REACH = 28;
    /* How far the crane rolls back along its rails, and the floor rises, opening, in blocks. */
    private static final double CRANE_TRAVEL = ShipyardStructure.CRANE_PARKED - ShipyardStructure.CRANE_AT;
    private static final double FLOOR_RISE = ShipyardStructure.DECK_UP - ShipyardStructure.DECK_DOWN;
    /* How far under the trolley the hook hangs over the bay, and how far it is drawn up, in blocks, as modelled. */
    private static final double HOOK_HANG = 4.45;
    private static final double HOOK_UP = 3.85;
    /* The floor's top closed, from the ground's; the top of a cradle's ram standing; its jaws' pivots' height. */
    private static final double FLOOR_DOWN = ShipyardStructure.DECK_DOWN + 1.0;
    private static final double RAM_TOP = 1.75;
    private static final double PIVOT = -0.6;
    /* How far a folded ram's saddle sinks under the floor's top, and how far a jaw's pivot stands over it. */
    private static final double SUNK = 0.18;
    private static final double LIFTED = 0.02;
    /* A jaw's lean standing, a little in toward the hull, and folded, flat in its slot pointing out; in radians. */
    private static final double JAW_UP = Math.atan2(2.4, -0.3);
    private static final double JAW_OUT = Math.atan2(0.06, 1.0);
    private static final double PIXELS = 16.0;
    /* The share of the floor's way its gear takes to come down or fold: its own time, of the floor's. */
    private static final double GEAR_SHARE = GearMotion.CYCLE / ShipyardDoors.FLOOR_SECONDS;
    /* The cradle's stations, along the pit, as its model's bones are numbered. */
    private static final int STATIONS = 4;
    private static final int RAMS = 2;
    private static final CraneDriver CRANE = new CraneDriver();
    private static final CradleDriver CRADLE = new CradleDriver();

    private ShipyardRenderer() {
        this.dock = KitBlockEntityRenderer.<ShipyardTerminalBlockEntity>formedOnly(ShipyardBlocks.FRAME_MODEL.id())
                .extras(ShipyardRenderer::extras)
                .viewDistance(SEEN_FROM)
                .build();
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                ShipyardBlocks.TERMINAL_ENTITY.get(), context -> new ShipyardRenderer()));
    }

    @Override
    public void render(final ShipyardTerminalBlockEntity terminal, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        this.dock.render(terminal, partialTick, poses, buffers, light, overlay);
        if (terminal.formed()) {
            PlanArmsRenderer.draw(terminal, terminal.arms(), ShipyardBlocks.ARM.id(), poses, buffers, overlay,
                    partialTick);
        }
        ShipHolograms.drawFormed(terminal, poses, buffers);
        final Optional<ShipDesign> plan = terminal.plan();
        final Optional<BlockPos> at = terminal.planAt();
        final List<Integer> formed = terminal.formedParts();
        if (plan.isEmpty() || at.isEmpty() || formed.isEmpty() || terminal.getLevel() == null) {
            return;
        }
        final double progress = terminal.doorsProgress(terminal.getLevel().getGameTime() + partialTick);
        ShipPartsRenderer.drawShip(terminal, plan.get(), formed::contains, ShipHoles.of(terminal, plan.get(),
                terminal.planTurn(), terminal.holes()), GearMotion.at(gearUp(terminal.doors(), progress)),
                at.get().subtract(terminal.getBlockPos()), terminal.planTurn(), CabinReadings.AT_REST, poses, buffers,
                KitBlockEntityRenderer.lightAround(terminal.getLevel(), at.get(), light), overlay);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /*
     * The dock and the ship in it reach far from the terminal's own chunk, so they are drawn whenever their box is in
     * sight, whether or not the game's culling sees the terminal's chunk: else a player standing in the docked ship,
     * its walls hiding that chunk, sees the ship round them vanish.
     */
    @Override
    public boolean shouldRenderOffScreen(final ShipyardTerminalBlockEntity terminal) {
        return true;
    }

    /* The dock's box, and with a plan the box round the terminal the dock and its ship stand in. */
    @Override
    public AABB getRenderBoundingBox(final ShipyardTerminalBlockEntity terminal) {
        final AABB box = this.dock.getRenderBoundingBox(terminal);
        return terminal.plan().isEmpty() ? box : box.minmax(new AABB(terminal.getBlockPos()).inflate(DOCK_REACH));
    }

    /*
     * The crane, the floor, the cradle and the glass, where the dock's frame stands, the crane and the floor moved
     * and the cradle folded as far as the dock has opened.
     */
    private static void extras(final ShipyardTerminalBlockEntity terminal, final KitInstance dock,
                               final PoseStack poses, final MultiBufferSource buffers, final int light,
                               final int overlay, final float partialTick) {
        final double now = terminal.getLevel() == null ? 0 : terminal.getLevel().getGameTime() + partialTick;
        final Opening opening = Opening.of(terminal.doors(), terminal.doorsProgress(now));
        draw(terminal, ShipyardBlocks.CRANE_MODEL.id(), 0, opening.crane() * CRANE_TRAVEL, CRANE, opening, poses,
                buffers, light, overlay);
        draw(terminal, ShipyardBlocks.FLOOR_MODEL.id(), opening.floor() * FLOOR_RISE, 0, null, opening, poses,
                buffers, light, overlay);
        draw(terminal, ShipyardBlocks.CRADLE_MODEL.id(), 0, 0, CRADLE, opening, poses, buffers, light, overlay);
        // The glass last, for it is clear.
        draw(terminal, ShipyardBlocks.GLASS.id(), 0, 0, null, opening, poses, buffers, light, overlay);
    }

    /* Draws the model {@code id} moved up {@code up} and back {@code back}, its bones posed by {@code driver}. */
    private static void draw(final ShipyardTerminalBlockEntity terminal, final ResourceLocation id, final double up,
                             final double back, final @Nullable IKitDriver<Opening> driver, final Opening opening,
                             final PoseStack poses, final MultiBufferSource buffers, final int light,
                             final int overlay) {
        final Optional<KitModel> model = KitModels.get(id);
        if (model.isEmpty()) {
            return;
        }
        final KitInstance instance = KitAnimations.part(terminal, id.getPath(), model.get());
        if (driver != null) {
            instance.animate(KitClock.frame(), KitClock.now(), driver, opening, IAnimationListener.NONE);
        }
        poses.pushPose();
        poses.translate(0, up, back);
        KitRenderer.draw(instance, ModelDefinition.Variant.NONE, terminal, poses, buffers, light, overlay, null,
                terminal.getLevel(), true);
        poses.popPose();
    }

    /*
     * How far up the docked ship's gear is: stowed in its cradle, coming down as the floor rises to meet it, standing
     * on the open floor, folding as the floor goes down.
     */
    private static double gearUp(final ShipyardDoors.State doors, final double progress) {
        return switch (doors) {
            case CLOSED, CRANE_BACK, CRANE_IN -> 1;
            case FLOOR_UP -> 1 - Math.min(1, progress / GEAR_SHARE);
            case CRADLE_FOLDS, OPEN, CRADLE_HOLDS -> 0;
            case FLOOR_DOWN -> Math.min(1, progress / GEAR_SHARE);
        };
    }

    /*
     * How far the dock has opened, each from 0 to 1: its crane rolled back, its floor risen, and its cradle let go of
     * the ship and folded into the floor, only once the floor stands up under the ship's gear.
     */
    private record Opening(double crane, double floor, double cradle) {

        static Opening of(final ShipyardDoors.State doors, final double progress) {
            return switch (doors) {
                case CLOSED -> new Opening(0, 0, 0);
                case CRANE_BACK -> new Opening(progress, 0, 0);
                case FLOOR_UP -> new Opening(1, progress, 0);
                case CRADLE_FOLDS -> new Opening(1, 1, progress);
                case OPEN -> new Opening(1, 1, 1);
                case CRADLE_HOLDS -> new Opening(1, 1, 1 - progress);
                case FLOOR_DOWN -> new Opening(1, 1 - progress, 0);
                case CRANE_IN -> new Opening(1 - progress, 0, 0);
            };
        }
    }

    /* Draws the hook up under the trolley as the crane rolls back: its cables shortened, its block raised. */
    private static final class CraneDriver implements IKitDriver<Opening> {

        @Override
        public void drive(final Opening opening, final ModelAnimator animator, final double seconds) {
            // Nothing plays on the crane; its hook is moved below.
        }

        @Override
        public void move(final Opening opening, final ModelPose pose, final double seconds) {
            final int cables = pose.index("cables");
            if (cables >= 0) {
                pose.multiplySize(cables, 1, (float) (1 - opening.crane() * HOOK_UP / HOOK_HANG), 1);
            }
            final int hook = pose.index("hook");
            if (hook >= 0) {
                pose.addMove(hook, 0, (float) (opening.crane() * HOOK_UP * PIXELS), 0);
            }
        }

        @Override
        public boolean movesBones() {
            return true;
        }
    }

    /*
     * Folds the cradle flat into the floor once the floor stands up under the ship: each ram's saddle sinking to just
     * under the floor's top, each jaw's pivot rising to the floor's top as the jaw swings out and down to lie in its
     * slot; standing again to hold the ship, the other way, before the floor goes down.
     */
    private static final class CradleDriver implements IKitDriver<Opening> {

        private final Quaternionf turn = new Quaternionf();
        private final List<String> rams = new ArrayList<>();
        private final List<String> west = new ArrayList<>();
        private final List<String> east = new ArrayList<>();

        private CradleDriver() {
            for (int station = 0; station < STATIONS; station++) {
                for (int ram = 0; ram < RAMS; ram++) {
                    this.rams.add("ram_" + station + "_" + ram);
                }
                this.west.add("jaw_" + station + "_west");
                this.east.add("jaw_" + station + "_east");
            }
        }

        @Override
        public void drive(final Opening opening, final ModelAnimator animator, final double seconds) {
            // Nothing plays on the cradle; its bones are moved below.
        }

        @Override
        public void move(final Opening opening, final ModelPose pose, final double seconds) {
            final double t = opening.cradle();
            final double top = FLOOR_DOWN + opening.floor() * FLOOR_RISE;
            final float sinks = (float) (-t * (RAM_TOP - (top - SUNK)) * PIXELS);
            final float rides = (float) (t * (top + LIFTED - PIVOT) * PIXELS);
            final float swings = (float) (t * (JAW_OUT - JAW_UP));
            for (final String ram : this.rams) {
                final int bone = pose.index(ram);
                if (bone >= 0) {
                    pose.addMove(bone, 0, sinks, 0);
                }
            }
            // The east jaws swing out to the east, the west ones to the west: the same swing, mirrored.
            jaws(pose, this.east, rides, swings);
            jaws(pose, this.west, rides, -swings);
        }

        @Override
        public boolean movesBones() {
            return true;
        }

        private void jaws(final ModelPose pose, final List<String> jaws, final float rides, final float swings) {
            for (final String jaw : jaws) {
                final int bone = pose.index(jaw);
                if (bone >= 0) {
                    pose.addMove(bone, 0, rides, 0);
                    pose.setTurn(bone, this.turn.identity().rotateZ(swings));
                }
            }
        }
    }
}
