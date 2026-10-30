/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipLayout;
import dev.jstech.space.ship.ShipPartModels;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The plan a Small Shipyard builds a ship by, from its design: every block of every part's kit where it goes, laid out
 * from the block of the ship's nose on its centre line at its hull's floor, turned as the dock stands; each part of the
 * ship a part of the plan, the spine's from the nose first, then the hung ones; each block merging as its place in its
 * piece's model, a flipped section's mirrored. A block of the game in a kit, a workshop's furnace, keeps itself.
 */
public final class ShipyardPlans {

    /* A mirror across the ship's centre line, as a flipped section is drawn. */
    private static final Orientation MIRROR = new Orientation(0, -1, 1, 1, 2, 1);

    private ShipyardPlans() {
    }

    /** The plan of {@code design}, of the parts and kits {@code level} knows, in a dock standing as {@code dock}. */
    public static BuildPlan of(final Level level, final ShipDesign design, final Orientation dock) {
        final Map<ResourceLocation, ShipPart> parts = ShipParts.all(level);
        final Map<ResourceLocation, ShipKit> kits = ShipKits.all(level);
        final int turned = FormedShapes.ORIENTATIONS.indexOf(dock);
        final int mirrored = FormedShapes.ORIENTATIONS.indexOf(MIRROR.then(dock));
        final List<PlanSpot> spots = new ArrayList<>();
        for (final ShipLayout.Block block : ShipyardLayout.blocks(parts, kits, design)) {
            final ResourceLocation id = partOf(design, block.part());
            final ShipKit kit = kits.get(id);
            final ResourceLocation wanted = kit == null ? null : kit.key().get(String.valueOf(block.letter()));
            if (wanted == null) {
                continue;
            }
            final int[] at = dock.apply(block.cell().x(), block.cell().y(), block.cell().z());
            // A block of the game keeps itself, and has no place in its piece's model.
            final Optional<FormedPlace> place = JsSpace.MODID.equals(wanted.getNamespace())
                    ? Optional.of(new FormedPlace(ShipPartModels.shape(id, kit, block.piece()),
                            block.flipped() ? mirrored : turned, block.model().x(), block.model().y(),
                            block.model().z()))
                    : Optional.empty();
            spots.add(new PlanSpot(new BlockPos(at[0], at[1], at[2]), wanted, block.part(), place, List.of()));
        }
        return new BuildPlan(design.spine().size() + design.hung().size(), spots);
    }

    /** The part numbered {@code part} of {@code design}: the spine's from the nose first, then the hung ones. */
    public static ResourceLocation partOf(final ShipDesign design, final int part) {
        return part < design.spine().size() ? design.spine().get(part)
                : design.hung().get(part - design.spine().size());
    }
}
