/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.core.content.DeclaredModel;
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
    private static final int MOST_TRIANGLES = 24_000;

    static {
        for (final String name : NAMES) {
            MODELS.put(name, SpaceContent.CONTENT.model("rocket/" + name).file("jsmodels/rocket/" + name + ".gltf")
                    .texture("textures/models/rocket/" + name + ".png").triangles(MOST_TRIANGLES).register());
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
     * The models {@code stack} is drawn with, from the bottom up, each with the height its foot stands at; a part
     * whose model is not known is left out, its height kept. {@code legsDown} spreads the legs, as for a landing.
     */
    public static List<Placed> of(final Map<ResourceLocation, RocketPart> parts,
                                  final Map<ResourceLocation, RocketKit> kits, final RocketStack stack,
                                  final boolean legsDown) {
        final List<Placed> placed = new ArrayList<>();
        int y = 0;
        final List<RocketStack.Section> sections = stack.sections();
        for (int k = 0; k < sections.size(); k++) {
            final RocketStack.Part here = sections.get(k).part();
            final RocketPart part = parts.get(here.id());
            if (part == null) {
                continue;
            }
            final RocketPart below = k > 0 ? parts.get(sections.get(k - 1).part().id()) : null;
            final RocketPart above = k + 1 < sections.size() ? parts.get(sections.get(k + 1).part().id()) : null;
            final String name = here.id().getPath();
            if (part.stretches()) {
                final RocketKit kit = kits.get(here.id());
                final int count = Math.max(1, here.layers());
                final boolean onEngine = below != null && below.kind() == RocketPart.Kind.ENGINE;
                final boolean foot = kit != null && kit.foot().isPresent() && onEngine && count > 1;
                final int body = count - (foot ? 1 : 0);
                if (foot) {
                    placed.add(new Placed(id(name + "_foot"), y++, k));
                }
                for (int layer = 0; layer < body; layer++) {
                    final String which = body == 1 ? "_alone" : layer == 0 ? "_bottom" : layer == body - 1 ? "_top"
                            : "_middle";
                    placed.add(new Placed(id(name + which), y++, k));
                }
                continue;
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
                placed.add(new Placed(id(model), y, k));
            }
            y += part.height();
        }
        return List.copyOf(placed);
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
     * @param model   the model's id
     * @param y       the height its foot stands at, in blocks from the rocket's foot
     * @param section the place of its part in the rocket's stack, from the bottom
     */
    public record Placed(ResourceLocation model, int y, int section) {
    }
}
