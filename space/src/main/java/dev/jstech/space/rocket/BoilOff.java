/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.effect.Effects;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The vapour a rocket's loaded tanks give off while it stands on the ground: their cold propellant boiling away, it
 * rolls off the sides of each tank that holds any, white, and sinks as it spreads, as it pours off a real rocket on
 * its pad.
 */
public final class BoilOff {

    /** The vapour off one side of a tank. */
    public static final ResourceLocation VAPOUR = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "boil_off");
    /* How far it is seen, in blocks; the sides of a tank it rolls off. */
    private static final int SEEN = 128;
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST);

    private BoilOff() {
    }

    /** The vapour off the loaded tanks of the rocket whose plan {@code computer} keeps, as they hold now. */
    public static void rise(final ServerLevel level, final FlightComputerBlockEntity computer) {
        final Map<Integer, TankLoad> tanks = computer.tanks();
        final Optional<RocketDesign> design = computer.rocket();
        final Optional<BlockPos> foot = computer.foot();
        if (tanks.isEmpty() || design.isEmpty() || foot.isEmpty()) {
            return;
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final Vec3 base = Vec3.atBottomCenterOf(computer.getBlockPos().offset(foot.get()));
        final List<RocketStack.Section> sections = design.get().stack().sections();
        double bottom = 0;
        for (int k = 0; k < sections.size(); k++) {
            final RocketStack.Part each = sections.get(k).part();
            final RocketPart part = parts.get(each.id());
            if (part == null) {
                continue;
            }
            final double tall = part.height() * (part.stretches() ? each.layers() : 1);
            final TankLoad load = tanks.get(k);
            if (load != null && load.total() > 0) {
                for (final Direction side : SIDES) {
                    final Vec3 out = Vec3.atLowerCornerOf(side.getNormal());
                    Effects.at(level, VAPOUR, base.add(out.scale(part.diameter() / 2.0)).add(0, bottom + tall / 2, 0))
                            .facing(out).reach(SEEN).send();
                }
            }
            bottom += tall;
        }
    }
}
