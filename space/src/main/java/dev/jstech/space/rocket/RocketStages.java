/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.space.api.RocketPart;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A rocket's stack read through the registry of parts: its burns, from the parts that are there, its height, and
 * whether its parts fit, each part's foot as wide as the top of the one under it, as real rockets' parts of one
 * diameter fit together and an adapter joins two.
 */
public final class RocketStages {

    private RocketStages() {
    }

    /** The burns of {@code stack} with the parts of {@code parts}, from the bottom up; none while a part is unknown. */
    public static Optional<List<StageMath.Burn>> burns(final Map<ResourceLocation, RocketPart> parts,
                                                      final RocketStack stack) {
        final List<StageMath.Piece> pieces = new ArrayList<>();
        for (final RocketStack.Section section : stack.sections()) {
            final Optional<StageMath.Piece> core = piece(parts, section.part());
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
            pieces.add(new StageMath.Piece(piece.dry(), piece.propellant(), piece.thrust(), piece.seaLevelIsp(),
                    piece.vacuumIsp(), piece.endsStage(), boosters));
        }
        return Optional.of(StageMath.burns(pieces));
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

    /* A part as the sums see it, with no boosters of its own. */
    private static Optional<StageMath.Piece> piece(final Map<ResourceLocation, RocketPart> parts,
                                                   final RocketStack.Part placed) {
        final RocketPart part = parts.get(placed.id());
        if (part == null) {
            return Optional.empty();
        }
        final int layers = layers(part, placed);
        final double thrust = part.engine().map(RocketPart.Engine::thrust).orElse(0.0);
        final double seaLevelIsp = part.engine().map(RocketPart.Engine::seaLevelIsp).orElse(0.0);
        final double vacuumIsp = part.engine().map(RocketPart.Engine::vacuumIsp).orElse(0.0);
        return Optional.of(new StageMath.Piece(part.mass() * layers, part.propellant() * layers, thrust, seaLevelIsp,
                vacuumIsp, part.kind().endsStage(), List.of()));
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
            final Optional<StageMath.Piece> piece = piece(parts, placed);
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
