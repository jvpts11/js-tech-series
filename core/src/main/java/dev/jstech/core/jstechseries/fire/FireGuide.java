/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The manual's section on fire: what it leaves, how to put it out by its class, and what watches for it. */
public final class FireGuide {

    private FireGuide() {
    }

    /** Declares the section and its pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        final ModGuide.SectionRef fire = CoreItems.CONTENT.guide().section("fire")
                .titled("Fire").icon(FireSafety.WATER_EXTINGUISHER::get).register();
        CoreItems.CONTENT.guide().page("extinguishers", fire).titled("Fire Extinguishers")
                .icon(FireSafety.WATER_EXTINGUISHER::get).covers(FireSafety.WATER_EXTINGUISHER::get)
                .covers(FireSafety.FOAM_EXTINGUISHER::get).covers(FireSafety.CARBON_DIOXIDE_EXTINGUISHER::get)
                .covers(FireSafety.DRY_POWDER_EXTINGUISHER::get)
                .whatItIs("Red cylinders you spray on a fire, each of one agent, its band in the colour of the European"
                        + " code: water all red, foam cream, carbon dioxide black, dry powder blue.")
                .whatItIsFor("Putting a fire out before it spreads, with the agent that suits what burns.")
                .define("Class of fire", "What burns: solids (wood, cloth), flammable liquids, electrical (something"
                        + " live with current), metals (sodium, magnesium) and cooking oils. Each takes its own agent.")
                .howToGetIt("In creative, take them from the J's Tech tab; the mods that need them give them their"
                        + " recipes.")
                .howToUseIt("Hold use and aim at the fire: it sprays a cone ahead, water farthest and carbon dioxide"
                                + " the shortest. Its bar shows what is left.",
                        "Water puts out solids; foam solids, liquids and cooking oils; carbon dioxide liquids and"
                                + " electrical fires, and leaves wood to smoulder; dry powder all but cooking oils.",
                        "Use a water extinguisher on water to fill it again.")
                .whatCanGoWrong("Spraying an electrical fire hurts you.",
                        "Water and foam carry the current: use carbon dioxide or dry powder.",
                        "The fire flares up round where you sprayed.",
                        "Water spreads a burning liquid or oil: use foam.",
                        "The fire bursts.",
                        "Water on a burning metal makes it burst: only dry powder smothers it.",
                        "The fire lights again.",
                        "Carbon dioxide leaves wood smouldering: finish it with water.")
                .register();
        CoreItems.CONTENT.guide().page("sprinkler", fire).titled("Sprinkler")
                .icon(FireSafety.SPRINKLER::asItem).covers(FireSafety.SPRINKLER::asItem)
                .whatItIs("A sprinkler head hung under a ceiling: its red glass bulb bursts at 68 °C, the heat of a"
                        + " fire below, and it showers the place under it.")
                .whatItIsFor("Putting out a fire in a room by itself, while nobody is there.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that need it give it their"
                        + " recipes.")
                .howToUseIt("Place it under a ceiling and run a water pipe to it, or put a tank beside it: it holds a"
                                + " bucket and draws more from the pipe.",
                        "It showers a hundred millibuckets a second while it is open, and shuts half a minute after"
                                + " the heat is gone.")
                .whatCanGoWrong("It opens but does nothing.",
                        "It has no water: check its pipe.",
                        "The fire it showers flares up or bursts.",
                        "Water is wrong for burning liquids, oils and metals: watch those with a detector instead.")
                .register();
        CoreItems.CONTENT.guide().page("fire_detectors", fire).titled("Fire Detectors")
                .icon(FireSafety.SMOKE_DETECTOR::asItem).covers(FireSafety.SMOKE_DETECTOR::asItem)
                .covers(FireSafety.HEAT_DETECTOR::asItem)
                .whatItIs("Detectors hung under a ceiling: a smoke detector sees the smoke that rises to it, a heat"
                        + " detector feels the air reach 57 °C. Their lamp lights while they call.")
                .whatItIsFor("Raising the alarm, or setting off anything you wire to them, when a fire starts.")
                .define("Obscuration", "How much of the light the smoke takes away, in percent. A smoke detector"
                        + " calls at ten.")
                .howToGetIt("In creative, take them from the J's Tech tab; the mods that need them give them their"
                        + " recipes.")
                .howToUseIt("Place one under a ceiling above what might burn: smoke rises, so a fire above it is not"
                                + " seen.",
                        "Each gives redstone on every side: full while it calls, or, chosen with a signal probe, the"
                                + " smoke or the temperature it reads.")
                .whatCanGoWrong("It never calls.",
                        "The fire is above it or too far to one side: hang it higher, over the fire.")
                .register();
        CoreItems.CONTENT.guide().page("charred_wood_and_ash", fire).titled("Charred Wood and Ash")
                .icon(CharredBlocks.CHARRED_LOG::asItem).covers(CharredBlocks.CHARRED_LOG::asItem)
                .covers(CharredBlocks.CHARRED_PLANKS::asItem).covers(CharredBlocks.ASH::asItem)
                .whatItIs("What fire leaves: logs and planks charred black, and a layer of ash where cloth, leaves"
                        + " and thin wood burnt away.")
                .whatItIsFor("Charred wood burns no more; it builds as wood does. Ash shows where a fire went.")
                .howToGetIt("They form by themselves where a fire burns wood and cloth away; break them to keep"
                        + " them.")
                .howToUseIt("Build with charred wood as with any wood; dig ash with a shovel.")
                .whatCanGoWrong("Burnt wood leaves nothing.",
                        "The server turned off what fire leaves, in the balance settings.")
                .register();
    }
}
