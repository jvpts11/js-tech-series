/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.ModelBuilder;
import dev.jstech.core.moving.BlockAboard;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketPart;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The formed models of the skeleton's rocket parts, round as the approved parts are: each block of a part's kit is a
 * bone of its own, named {@code c_<x>_<layer>_<z>} by its place in the kit, so a block missing is a bone hidden. A part
 * is drawn as one model, but for a few that change with what stands round them: a tank is a model a layer, its foot,
 * its lowest, its middle ones, its highest, or the one layer it is alone; a separation ring raises its interstage skirt
 * round a vacuum engine standing on it; the instrument ring of five closes its adapter's four panels round a lunar
 * lander standing on it; the landing legs and the lander's legs are folded on the way up and spread to land.
 */
public final class RocketPartModels {

    /** Every model, by its name. */
    private static final Map<String, DeclaredModel> MODELS = new LinkedHashMap<>();
    private static final String[] NAMES = {"instrument_ring_3", "instrument_ring_5", "instrument_ring_5_adapter",
        "separation_ring_3", "separation_ring_5", "separation_ring_3_skirt", "separation_ring_5_skirt",
        "sea_level_engine_3", "vacuum_engine_3", "sea_level_engine_5", "vacuum_engine_5", "tank_3_bottom",
        "tank_3_middle", "tank_3_top", "tank_3_alone", "tank_3_foot", "tank_5_bottom", "tank_5_middle", "tank_5_top",
        "tank_5_alone", "tank_5_foot", "landing_legs_3", "landing_legs_3_stowed", "landing_legs_5",
        "landing_legs_5_stowed", "lunar_lander_descent", "lunar_lander_descent_stowed", "lunar_lander_ascent",
        "capsule_3", "capsule_5"};
    /* A vacuum engine's impulse at sea level is well under its own in a vacuum: its large nozzle does badly there. */
    private static final double VACUUM_NOZZLE = 0.7;
    /* The capsule of five, its crew's cabin inside it, has the most triangles, and every one of them is drawn. */
    private static final int MOST_TRIANGLES = 28_000;
    /*
     * A capsule's crew lie on their couches on their backs, facing its nose, their eyes a little over the couch: the
     * crew seat, which in the lander's cabin is a chair facing its Flight Computer, is a couch in a capsule.
     */
    private static final BlockAboard COUCH = new BlockAboard(Optional.of(new BlockAboard.Seat(0.1, false, true)),
            Optional.empty(), Optional.empty(), Optional.empty());

    static {
        for (final String name : NAMES) {
            final ModelBuilder model = SpaceContent.CONTENT.model("rocket/" + name)
                    .file("jsmodels/rocket/" + name + ".gltf").texture("textures/models/rocket/" + name + ".png")
                    .triangles(MOST_TRIANGLES);
            if (name.startsWith("capsule_")) {
                model.aboard(RocketKitBlocks.CREW_SEAT.getId(), COUCH);
            }
            MODELS.put(name, model.register());
        }
    }

    private RocketPartModels() {
    }

    /** Declares the models, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The model of {@code name}, if it is one of the parts' models. */
    public static Optional<DeclaredModel> get(final String name) {
        return Optional.ofNullable(MODELS.get(name));
    }

    /** Every model's name. */
    public static List<String> names() {
        return List.of(NAMES);
    }

    /** The bone of the kit's block at {@code x, layer, z}. */
    public static String bone(final int x, final int layer, final int z) {
        return "c_" + x + "_" + layer + "_" + z;
    }

    /**
     * The models {@code stack} is drawn with, its core's from the bottom up and then its boosters', each with the
     * height its foot stands at, a booster's foot level with the part it hangs at; a part whose model is not known is
     * left out, its height kept. {@code legsDown} spreads the legs, as for a landing.
     */
    public static List<Placed> of(final Map<ResourceLocation, RocketPart> parts,
                                  final Map<ResourceLocation, RocketKit> kits, final RocketStack stack,
                                  final boolean legsDown) {
        final List<Placed> placed = new ArrayList<>();
        final List<RocketPieces.Piece> pieces = RocketPieces.of(parts, stack);
        final int[] feet = new int[stack.sections().size()];
        int y = 0;
        for (int k = 0; k < pieces.size(); k++) {
            final RocketPieces.Piece piece = pieces.get(k);
            if (!piece.inBooster()) {
                feet[piece.section()] = y;
            } else if (k == 0 || !sameColumn(pieces.get(k - 1), piece)) {
                y = feet[piece.section()];
            }
            final RocketPieces.Piece over = k + 1 < pieces.size() && sameColumn(piece, pieces.get(k + 1))
                    ? pieces.get(k + 1) : null;
            y = place(parts, kits, piece, over == null ? null : parts.get(over.part().id()), legsDown, y, placed);
        }
        return List.copyOf(placed);
    }

    /*
     * Adds the models of the part {@code piece}, its foot at {@code foot} and {@code above} over it in its column, to
     * {@code placed}; gives the height its top stands at.
     */
    private static int place(final Map<ResourceLocation, RocketPart> parts,
                             final Map<ResourceLocation, RocketKit> kits, final RocketPieces.Piece piece,
                             @Nullable final RocketPart above, final boolean legsDown, final int foot,
                             final List<Placed> placed) {
        final RocketStack.Part here = piece.part();
        final RocketPart part = parts.get(here.id());
        if (part == null) {
            return foot;
        }
        final RocketPart below = piece.below() == null ? null : parts.get(piece.below().id());
        final String name = here.id().getPath();
        int y = foot;
        if (part.stretches()) {
            final RocketKit kit = kits.get(here.id());
            final int count = Math.max(1, here.layers());
            final boolean onEngine = below != null && below.kind() == RocketPart.Kind.ENGINE;
            final boolean withFoot = kit != null && kit.foot().isPresent() && onEngine && count > 1;
            final int body = count - (withFoot ? 1 : 0);
            if (withFoot) {
                placed.add(new Placed(id(name + "_foot"), y++, piece));
            }
            for (int layer = 0; layer < body; layer++) {
                final String which = body == 1 ? "_alone" : layer == 0 ? "_bottom" : layer == body - 1 ? "_top"
                        : "_middle";
                placed.add(new Placed(id(name + which), y++, piece));
            }
            return y;
        }
        String model = name;
        if (part.kind() == RocketPart.Kind.SEPARATION_RING && vacuumEngine(above)) {
            model = name + "_skirt";
        } else if (part.kind() == RocketPart.Kind.INSTRUMENT_RING && above != null
                && above.kind() == RocketPart.Kind.LANDER_DESCENT) {
            model = name + "_adapter";
        } else if ((part.kind() == RocketPart.Kind.LANDING_LEGS || part.kind() == RocketPart.Kind.LANDER_DESCENT)
                && !legsDown) {
            model = name + "_stowed";
        }
        if (MODELS.containsKey(model)) {
            placed.add(new Placed(id(model), y, piece));
        }
        return y + part.height();
    }

    /* Whether {@code next} stands on {@code piece} in one column: the core's, or one booster's. */
    private static boolean sameColumn(final RocketPieces.Piece piece, final RocketPieces.Piece next) {
        return piece.inBooster() == next.inBooster() && (!piece.inBooster()
                || piece.section() == next.section() && piece.booster() == next.booster());
    }

    private static boolean vacuumEngine(final RocketPart part) {
        return part != null && part.kind() == RocketPart.Kind.ENGINE && part.engine().isPresent()
                && part.engine().get().seaLevelIsp() < VACUUM_NOZZLE * part.engine().get().vacuumIsp();
    }

    private static ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "rocket/" + name);
    }

    /**
     * A model of a rocket's part, where it stands in the rocket.
     *
     * @param model the model's id
     * @param y     the height its foot stands at, in blocks from the rocket's foot
     * @param piece the part it draws, as the rocket's plan numbers it
     */
    public record Placed(ResourceLocation model, int y, RocketPieces.Piece piece) {

        /** The number of its part in the rocket's plan. */
        public int part() {
            return this.piece.number();
        }

        /** The place in the rocket's stack of its part, or of the part its booster hangs at. */
        public int section() {
            return this.piece.section();
        }

        /** The quarter turns its part was turned about the rocket's axis, clockwise seen from above. */
        public int turns() {
            return this.piece.part().turns();
        }

        /** How far its middle stands east of the rocket's axis, in blocks: a booster's, beside its part. */
        public int x() {
            return this.piece.x();
        }

        /** How far its middle stands south of the rocket's axis, in blocks. */
        public int z() {
            return this.piece.z();
        }

        /** Whether it draws a segment of a booster. */
        public boolean booster() {
            return this.piece.inBooster();
        }
    }
}
