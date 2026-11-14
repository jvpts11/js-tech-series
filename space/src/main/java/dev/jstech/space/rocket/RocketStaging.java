/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.effect.Effects;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A rocket letting its spent stages go as it climbs, its flight computer firing each separation: once the burn lit
 * is above a stage, that stage and its separation ring part off as a body of their own with a jolt, the ring's retro
 * rockets pushing it away, each of its formed parts drawing itself from one of its blocks; the flight computer draws
 * only what is left, and the stage goes where its fate says ({@link StageFlights}). The stage that holds the flight
 * computer is never let go. Its boosters come loose before the first stage they hang at, once their burn is behind:
 * each pushed out from the core the way it stands, a body of its own falling where the first stage was set to go.
 */
public final class RocketStaging {

    /** The separation: a puff round the ring and a jolt felt nearby. */
    public static final ResourceLocation SEPARATION = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "separation");
    /* How fast the ring's retro rockets push the stage away, in metres a second; how far it is seen, in blocks. */
    private static final double KICK = 3.0;
    private static final int SEEN = 128;

    private RocketStaging() {
    }

    /**
     * Lets go every stage of the rocket {@code body} burnt out below the burn {@code ascent} has lit, those its flight
     * computer {@code computer} has not let go already.
     */
    public static void letGo(final ServerLevel level, final MovingStructure body,
                             final FlightComputerBlockEntity computer, final RocketAscent ascent) {
        final Optional<RocketDesign> design = computer.rocket();
        final Optional<BlockPos> foot = computer.foot();
        final List<StageMath.Burn> burns = RocketAscent.burns(level, body);
        if (design.isEmpty() || foot.isEmpty() || burns.isEmpty()) {
            return;
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final RocketStack stack = design.get().stack();
        final int count = RocketStages.stages(parts, stack).size();
        final int spent = ascent.burn() < burns.size() ? burns.get(ascent.burn()).stage() - 1 : count - 1;
        final BlockPos at = computer.getBlockPos().offset(foot.get());
        // The boosters come loose once their burn is behind, before the stage they hang at goes on burning.
        if (!computer.boostersGone() && burnt(burns, ascent.burn())) {
            separateBoosters(level, body, computer, parts, stack, at, false);
        }
        for (int stage = computer.separated() + 1; stage <= spent; stage++) {
            separate(level, body, computer, parts, stack, at, stage, false);
        }
    }

    /**
     * Leaves on the ground every stage of the rocket {@code body}, not let go yet, holding a part of the kind
     * {@code kind}, with those under it: as a lander's ascent stage leaves its descent stage behind as its launch
     * platform. They part off unpushed and stand where they are.
     */
    public static void leave(final ServerLevel level, final MovingStructure body,
                             final FlightComputerBlockEntity computer, final RocketPart.Kind kind) {
        final Optional<RocketDesign> design = computer.rocket();
        final Optional<BlockPos> foot = computer.foot();
        if (design.isEmpty() || foot.isEmpty()) {
            return;
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final RocketStack stack = design.get().stack();
        final int last = lastHolding(level, computer, kind);
        for (int stage = computer.separated() + 1; stage <= last; stage++) {
            separate(level, body, computer, parts, stack, computer.getBlockPos().offset(foot.get()), stage, true);
        }
    }

    /**
     * The highest stage not let go yet holding a part of the kind {@code kind}, which {@link #leave} leaves with those
     * under it; nought when none does.
     */
    public static int lastHolding(final ServerLevel level, final FlightComputerBlockEntity computer,
                                  final RocketPart.Kind kind) {
        final Optional<RocketDesign> design = computer.rocket();
        if (design.isEmpty()) {
            return 0;
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final RocketStack stack = design.get().stack();
        final int count = RocketStages.stages(parts, stack).size();
        int last = 0;
        for (int stage = computer.separated() + 1; stage <= count; stage++) {
            for (final int section : RocketStages.stage(parts, stack, stage)) {
                final RocketPart part = parts.get(stack.sections().get(section).part().id());
                if (part != null && part.kind() == kind) {
                    last = stage;
                }
            }
        }
        return last;
    }

    /*
     * Lets the stage {@code stage} of the rocket {@code body} go, its foot at {@code foot} in the yard: its blocks part
     * off, one block of each of its parts drawing the part, pushed down and away, where it goes decided now; or, when
     * {@code left} on the ground, unpushed, standing where it stands.
     */
    private static void separate(final ServerLevel level, final MovingStructure body,
                                 final FlightComputerBlockEntity computer,
                                 final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                 final BlockPos foot, final int stage, final boolean left) {
        if (stage == 1 && !computer.boostersGone()) {
            // The boosters hang at the first stage's engine: they go before it, whatever is left in them.
            separateBoosters(level, body, computer, parts, stack, foot, left);
        }
        final List<Integer> sections = RocketStages.stage(parts, stack, stage);
        final BuildPlan plan = RocketPlans.of(level, stack);
        for (final PlanSpot spot : plan.spots()) {
            if (sections.contains(spot.part()) && spot.block().equals(RocketKitBlocks.FLIGHT_COMPUTER.getId())) {
                // The stage holding the computer goes on with it.
                computer.letGo(stage, List.of());
                return;
            }
        }
        final Parting parting = parting(level, plan, foot, Set.copyOf(sections));
        computer.letGo(stage, sections);
        if (parting.places().isEmpty()) {
            return;
        }
        final Optional<StructureUuid> piece = body.partOff(level, parting.places(), Direction.DOWN,
                left ? 0 : parting.mass() * KICK);
        final Vec3 ring = MovingStructures.toWorld(body, Vec3.atBottomCenterOf(foot.above(parting.top() + 1)));
        Effects.at(level, SEPARATION, ring).reach(SEEN).send();
        final Vec3 under = MovingStructures.toWorld(body, Vec3.atBottomCenterOf(foot.above(parting.bottom())));
        if (piece.isPresent() && !left) {
            StageFlights.parting(level, body, piece.get(), stage, RocketStages.dryMass(parts, stack, stage),
                    parting.bottom(), under);
        }
    }

    /*
     * Lets every booster of the rocket {@code body} go, its foot at {@code foot} in the yard, each a body of its own:
     * pushed out from the core the way it stands from it, it falls where its first stage was set to go, never to a
     * platform; or, when {@code left} on the ground, unpushed, standing where it stands.
     */
    private static void separateBoosters(final ServerLevel level, final MovingStructure body,
                                         final FlightComputerBlockEntity computer,
                                         final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                         final BlockPos foot, final boolean left) {
        final List<Integer> numbers = RocketPieces.boosterNumbers(stack);
        computer.letGoBoosters(numbers);
        if (numbers.isEmpty()) {
            return;
        }
        final BuildPlan plan = RocketPlans.of(level, stack);
        final List<RocketPieces.Piece> pieces = RocketPieces.of(parts, stack);
        int from = 0;
        while (from < pieces.size()) {
            final RocketPieces.Piece first = pieces.get(from);
            int to = from + 1;
            while (to < pieces.size() && first.inBooster() && pieces.get(to).section() == first.section()
                    && pieces.get(to).booster() == first.booster()) {
                to++;
            }
            if (first.inBooster()) {
                final Set<Integer> mine = new HashSet<>();
                final List<RocketStack.Part> segments = new ArrayList<>();
                for (int k = from; k < to; k++) {
                    mine.add(pieces.get(k).number());
                    segments.add(pieces.get(k).part());
                }
                letGoBooster(level, body, foot, parting(level, plan, foot, mine), outward(first),
                        RocketStages.boosterDryMass(parts, segments), left);
            }
            from = to;
        }
    }

    /* Lets one booster go, its blocks {@code parting}, pushed {@code way}, {@code dry} kilograms empty. */
    private static void letGoBooster(final ServerLevel level, final MovingStructure body, final BlockPos foot,
                                     final Parting parting, final Direction way, final double dry,
                                     final boolean left) {
        if (parting.places().isEmpty()) {
            return;
        }
        final Optional<StructureUuid> piece = body.partOff(level, parting.places(), way,
                left ? 0 : parting.mass() * KICK);
        final Vec3 nose = MovingStructures.toWorld(body, Vec3.atBottomCenterOf(foot.above(parting.top() + 1)));
        Effects.at(level, SEPARATION, nose).reach(SEEN).send();
        final Vec3 under = MovingStructures.toWorld(body, Vec3.atBottomCenterOf(foot.above(parting.bottom())));
        if (piece.isPresent() && !left) {
            StageFlights.parting(level, body, piece.get(), RocketStages.BOOSTERS, dry, parting.bottom(), under);
        }
    }

    /* The way a booster standing at {@code piece}'s place is pushed off: out from the core, the side it stands on. */
    private static Direction outward(final RocketPieces.Piece piece) {
        if (Math.abs(piece.x()) >= Math.abs(piece.z())) {
            return piece.x() >= 0 ? Direction.EAST : Direction.WEST;
        }
        return piece.z() >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /* Whether the climb, lighting the burn {@code lit}, is past the boosters' burn: they are spent. */
    private static boolean burnt(final List<StageMath.Burn> burns, final int lit) {
        for (int k = 0; k < Math.min(lit, burns.size()); k++) {
            if (burns.get(k).boosters()) {
                return true;
            }
        }
        return false;
    }

    /*
     * The blocks standing at {@code foot} of the plan's parts {@code parts}, with their mass and their lowest and
     * highest layers, one block of each formed part among them now drawing the part itself.
     */
    private static Parting parting(final ServerLevel level, final BuildPlan plan, final BlockPos foot,
                                   final Set<Integer> parts) {
        final List<BlockPos> places = new ArrayList<>();
        final Set<String> drawn = new HashSet<>();
        double mass = 0;
        int bottom = Integer.MAX_VALUE;
        int top = Integer.MIN_VALUE;
        for (final PlanSpot spot : plan.spots()) {
            if (!parts.contains(spot.part())) {
                continue;
            }
            final BlockPos at = foot.offset(spot.at());
            final BlockState state = level.getBlockState(at);
            if (state.isAir()) {
                continue;
            }
            places.add(at);
            mass += BlockPhysics.massOf(level, at, FormedPartBlock.originalAt(level, at, state));
            bottom = Math.min(bottom, spot.at().getY());
            top = Math.max(top, spot.at().getY());
            if (spot.place().isPresent()) {
                final FormedPlace place = spot.place().get();
                final int[] cell = place.turn().apply(place.x(), place.y(), place.z());
                final String part = place.model() + "@" + at.offset(-cell[0], -cell[1], -cell[2]).asLong();
                if (!drawn.contains(part) && FormedParts.drawFrom(level, at)) {
                    drawn.add(part);
                }
            }
        }
        return new Parting(List.copyOf(places), mass, bottom, top);
    }

    /**
     * What parts off a rocket.
     *
     * @param places its blocks, in the yard
     * @param mass   their mass, in kilograms
     * @param bottom its lowest layer, in blocks over the rocket's foot
     * @param top    its highest layer
     */
    private record Parting(List<BlockPos> places, double mass, int bottom, int top) {
    }
}
