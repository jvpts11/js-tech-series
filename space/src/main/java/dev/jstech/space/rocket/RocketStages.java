/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.RocketPart;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * A rocket's stack read through the registry of parts: its burns, from the parts that are there, its height, its
 * stages, and whether its parts fit, each part's foot as wide as the top of the one under it, as real rockets' parts of
 * one diameter fit together and an adapter joins two. Its burns come from its tanks full, as its design shows them, or
 * from what its tanks hold: an engine burns only the propellant it was made for, and lights only where its stage's
 * tanks hold some of it; a propellant its engines do not burn is carried as dead weight. Boosters come full, as solid
 * ones are cast.
 */
public final class RocketStages {

    private RocketStages() {
    }

    /** The burns of {@code stack} with its tanks full, from the bottom up; none while a part is unknown. */
    public static Optional<List<StageMath.Burn>> burns(final Map<ResourceLocation, RocketPart> parts,
                                                      final RocketStack stack) {
        return staged(parts, stack, null, Map.of(), Set.of());
    }

    /**
     * The burns of {@code stack} with its tanks holding {@code tanks}, by their places from the bottom, the propellants
     * read from {@code propellants}; none while a part is unknown.
     */
    public static Optional<List<StageMath.Burn>> burns(final Map<ResourceLocation, RocketPart> parts,
                                                      final RocketStack stack,
                                                      final Map<ResourceLocation, Propellant> propellants,
                                                      final Map<Integer, TankLoad> tanks) {
        return staged(parts, stack, Map.copyOf(propellants), Map.copyOf(tanks), Set.of());
    }

    /**
     * The burns of {@code stack} as {@link #burns(Map, RocketStack, Map, Map)} gives them, the engines of the parts at
     * {@code dead} giving no thrust: parts that lost blocks, holed engines that do not light.
     */
    public static Optional<List<StageMath.Burn>> burns(final Map<ResourceLocation, RocketPart> parts,
                                                      final RocketStack stack,
                                                      final Map<ResourceLocation, Propellant> propellants,
                                                      final Map<Integer, TankLoad> tanks, final Set<Integer> dead) {
        return staged(parts, stack, Map.copyOf(propellants), Map.copyOf(tanks), Set.copyOf(dead));
    }

    /** How tall {@code stack} stands, in blocks; 0 while a part is unknown. */
    public static int height(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        int height = 0;
        for (final RocketStack.Section section : stack.sections()) {
            final RocketPart part = parts.get(section.part().id());
            if (part == null) {
                return 0;
            }
            height += part.height() * layers(part, section.part());
        }
        return height;
    }

    /**
     * The first place in {@code stack}, from the bottom, where a part does not fit the one under it, or where a part is
     * unknown; none where every part fits.
     */
    public static Optional<Integer> misfit(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        int top = -1;
        for (int k = 0; k < stack.sections().size(); k++) {
            final RocketPart part = parts.get(stack.sections().get(k).part().id());
            if (part == null || top >= 0 && part.diameter() != top) {
                return Optional.of(k);
            }
            top = part.topDiameter();
        }
        return Optional.empty();
    }

    /**
     * The places in {@code stack} of each of its stages, from the bottom: a stage is the parts up to and with the one
     * that ends it, and the last is what is left above. A part unknown ends no stage.
     */
    public static List<List<Integer>> stages(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        final List<List<Integer>> stages = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        for (int k = 0; k < stack.sections().size(); k++) {
            current.add(k);
            final RocketPart part = parts.get(stack.sections().get(k).part().id());
            if (part != null && part.kind().endsStage()) {
                stages.add(List.copyOf(current));
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            stages.add(List.copyOf(current));
        }
        return List.copyOf(stages);
    }

    /** The places of the stage {@code stage}, numbered from one at the bottom; none for a stage it does not have. */
    public static List<Integer> stage(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                      final int stage) {
        final List<List<Integer>> stages = stages(parts, stack);
        return stage < 1 || stage > stages.size() ? List.of() : stages.get(stage - 1);
    }

    /**
     * Whether the stage {@code stage} can come back and land, as Falcon 9's first stage does: it has landing legs and
     * an engine to slow it down.
     */
    public static boolean canComeBack(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                      final int stage) {
        boolean legs = false;
        boolean engine = false;
        for (final int place : stage(parts, stack, stage)) {
            final RocketPart part = parts.get(stack.sections().get(place).part().id());
            if (part != null) {
                legs |= part.kind() == RocketPart.Kind.LANDING_LEGS;
                engine |= part.engine().isPresent();
            }
        }
        return legs && engine;
    }

    /** The widest of the stage {@code stage}'s parts, in blocks; none for a stage it does not have. */
    public static int diameter(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                               final int stage) {
        int widest = 0;
        for (final int place : stage(parts, stack, stage)) {
            final RocketPart part = parts.get(stack.sections().get(place).part().id());
            if (part != null) {
                widest = Math.max(widest, Math.max(part.diameter(), part.topDiameter()));
            }
        }
        return widest;
    }

    /** The stage {@code stage}'s mass empty, its boosters' with it, in kilograms. */
    public static double dryMass(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                 final int stage) {
        double mass = 0;
        for (final int place : stage(parts, stack, stage)) {
            final RocketStack.Section section = stack.sections().get(place);
            mass += piece(parts, section.part(), null, null).map(StageMath.Piece::dry).orElse(0.0);
            for (final List<RocketStack.Part> booster : section.boosters()) {
                mass += summed(parts, booster).map(StageMath.Piece::dry).orElse(0.0);
            }
        }
        return mass;
    }

    /* The burns, the tanks full where {@code propellants} is none. */
    private static Optional<List<StageMath.Burn>> staged(final Map<ResourceLocation, RocketPart> parts,
                                                        final RocketStack stack,
                                                        final Map<ResourceLocation, Propellant> propellants,
                                                        final Map<Integer, TankLoad> tanks, final Set<Integer> dead) {
        final List<StageMath.Piece> pieces = new ArrayList<>();
        for (final List<Integer> stage : stages(parts, stack)) {
            final ResourceLocation burnt = propellants == null ? null : burnt(parts, stack, stage);
            final Propellant propellant = burnt == null ? null : propellants.get(burnt);
            for (final int place : stage) {
                final RocketStack.Section section = stack.sections().get(place);
                final Optional<StageMath.Piece> core = propellants == null ? piece(parts, section.part(), null, null)
                        : loaded(parts, section.part(), tanks.get(place), burnt, propellant, lit(stage, tanks,
                        burnt, propellant));
                if (core.isEmpty()) {
                    return Optional.empty();
                }
                final List<StageMath.Piece> boosters = new ArrayList<>();
                for (final List<RocketStack.Part> booster : section.boosters()) {
                    final Optional<StageMath.Piece> summed = summed(parts, booster);
                    if (summed.isEmpty()) {
                        return Optional.empty();
                    }
                    boosters.add(summed.get());
                }
                final StageMath.Piece piece = core.get();
                // A holed part's engine does not light: no thrust, nothing gained from what it would burn.
                final boolean lights = !dead.contains(place);
                pieces.add(new StageMath.Piece(piece.dry(), piece.propellant(), lights ? piece.thrust() : 0,
                        lights ? piece.seaLevelIsp() : 0, lights ? piece.vacuumIsp() : 0, piece.endsStage(),
                        boosters));
            }
        }
        return Optional.of(StageMath.burns(pieces));
    }

    /* The propellant the first engine of a stage burns, which its other engines are taken to burn too. */
    private static ResourceLocation burnt(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                          final List<Integer> stage) {
        for (final int place : stage) {
            final RocketPart part = parts.get(stack.sections().get(place).part().id());
            if (part != null && part.engine().isPresent()) {
                return part.engine().get().propellant();
            }
        }
        return null;
    }

    /* Whether a stage's tanks hold any of what its engines burn, so they light. */
    private static boolean lit(final List<Integer> stage, final Map<Integer, TankLoad> tanks,
                               final ResourceLocation burnt, final Propellant propellant) {
        if (burnt == null || propellant == null) {
            return false;
        }
        for (final int place : stage) {
            final TankLoad load = tanks.get(place);
            if (load != null && load.usable(burnt, propellant) > 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * A part with what its tank holds: what its engines can burn is its propellant, the rest dead weight; its
     * engines' thrust only where its stage's engines light.
     */
    private static Optional<StageMath.Piece> loaded(final Map<ResourceLocation, RocketPart> parts,
                                                    final RocketStack.Part placed, final TankLoad load,
                                                    final ResourceLocation burnt, final Propellant propellant,
                                                    final boolean lit) {
        final Optional<StageMath.Piece> empty = piece(parts, placed, 0.0, lit);
        if (empty.isEmpty() || load == null) {
            return empty;
        }
        final double usable = burnt == null || propellant == null ? 0 : load.usable(burnt, propellant);
        final StageMath.Piece piece = empty.get();
        return Optional.of(new StageMath.Piece(piece.dry() + load.total() - usable, usable, piece.thrust(),
                piece.seaLevelIsp(), piece.vacuumIsp(), piece.endsStage(), List.of()));
    }

    /*
     * A part as the sums see it, with no boosters of its own: its tanks full where {@code propellant} is none, else
     * holding that; its engines lit where {@code lit} is none or true.
     */
    private static Optional<StageMath.Piece> piece(final Map<ResourceLocation, RocketPart> parts,
                                                   final RocketStack.Part placed, final Double propellant,
                                                   final Boolean lit) {
        final RocketPart part = parts.get(placed.id());
        if (part == null) {
            return Optional.empty();
        }
        final int layers = layers(part, placed);
        final boolean burns = lit == null || lit;
        final double thrust = burns ? part.engine().map(RocketPart.Engine::thrust).orElse(0.0) : 0.0;
        final double seaLevelIsp = part.engine().map(RocketPart.Engine::seaLevelIsp).orElse(0.0);
        final double vacuumIsp = part.engine().map(RocketPart.Engine::vacuumIsp).orElse(0.0);
        final double held = propellant == null ? part.propellant() * layers : propellant;
        return Optional.of(new StageMath.Piece(part.mass() * layers, held, thrust, seaLevelIsp, vacuumIsp,
                part.kind().endsStage(), List.of()));
    }

    /* A booster's parts summed into one: its mass, its propellant and its engines. */
    private static Optional<StageMath.Piece> summed(final Map<ResourceLocation, RocketPart> parts,
                                                    final List<RocketStack.Part> booster) {
        double dry = 0;
        double propellant = 0;
        double thrust = 0;
        double flow = 0;
        double seaLevel = 0;
        for (final RocketStack.Part placed : booster) {
            final Optional<StageMath.Piece> piece = piece(parts, placed, null, null);
            if (piece.isEmpty()) {
                return Optional.empty();
            }
            final StageMath.Piece p = piece.get();
            dry += p.dry();
            propellant += p.propellant();
            if (p.thrust() > 0 && p.vacuumIsp() > 0) {
                thrust += p.thrust();
                flow += p.thrust() / p.vacuumIsp();
                seaLevel += p.thrust() * p.seaLevelIsp() / p.vacuumIsp();
            }
        }
        final double vacuumIsp = flow <= 0 ? 0 : thrust / flow;
        final double seaLevelIsp = flow <= 0 ? 0 : seaLevel / flow;
        return Optional.of(new StageMath.Piece(dry, propellant, thrust, seaLevelIsp, vacuumIsp, false, List.of()));
    }

    private static int layers(final RocketPart part, final RocketStack.Part placed) {
        return part.stretches() ? placed.layers() : 1;
    }
}
