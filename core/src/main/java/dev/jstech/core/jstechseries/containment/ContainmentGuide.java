/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.containment;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The pages of the containment of exotic matter and the Antimatter Trap, in the Core's chapter of the manual. */
public final class ContainmentGuide {

    /* The Core's chapter's section on materials, where these pages go. */
    private static final ModGuide.SectionRef MATERIALS = new ModGuide.SectionRef("materials");

    private ContainmentGuide() {
    }

    /** Declares the pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        CoreItems.CONTENT.guide().page("exotic_matter_containment", MATERIALS).titled("Exotic Matter Containment")
                .icon(SharedContainment.EXOTIC::asItem).covers(SharedContainment.EXOTIC::asItem)
                .whatItIs("A heavy vessel whose field holds one exotic form of matter: neutronium, strange matter,"
                        + " dark matter and the rest, every one but antimatter.")
                .whatItIsFor("Keeping the exotic matter the series' mods make or gather, which nothing else holds.")
                .table("What it holds and spends")
                .property("Holds", "1,000 mB of one material")
                .property("Spends", "1 kW while it holds something")
                .property("Energy", "J's Energy at low voltage, or FE while J's Energy is off")
                .howToGetIt("The mods that keep exotic matter in it, J's Industrial and J's Space, give it their"
                        + " recipes; in creative, take it from the J's Tech tab.")
                .howToUseIt("Lay a line to it and keep the energy coming: it spends it for as long as it holds"
                        + " something.",
                        "Fill and empty it by a line of exotic matter, from any side.",
                        "Broken, it keeps what it holds in the item it drops; placed again, it holds it again.")
                .whatCanGoWrong("It takes nothing and gives nothing.",
                        "It has no energy: sealed, it lets nothing out, and nothing is lost. Bring the energy back.")
                .register();
        CoreItems.CONTENT.guide().page("antimatter_trap", MATERIALS).titled("Antimatter Trap")
                .icon(SharedContainment.ANTIMATTER_TRAP::asItem).covers(SharedContainment.ANTIMATTER_TRAP::asItem)
                .whatItIs("A Penning trap: a magnet's field and electrodes in a cold vacuum that hold antimatter away"
                        + " from every wall, as CERN keeps its antihydrogen.")
                .whatItIsFor("Keeping antimatter, which annihilates with any matter it touches and so fits in no"
                        + " other vessel.")
                .table("What it holds and spends")
                .property("Holds", "1,000 mB of one antimatter")
                .property("Spends", "2 kW while it holds something")
                .property("Energy", "J's Energy at low voltage, or FE while J's Energy is off")
                .howToGetIt("The mods that keep antimatter in it, J's Industrial and J's Space, give it their"
                        + " recipes; in creative, take it from the J's Tech tab.")
                .howToUseIt("Lay a line to it and keep the energy coming: it spends it for as long as it holds"
                        + " antimatter.",
                        "Fill and empty it by a line of exotic matter, from any side; it takes antimatter alone.",
                        "Broken, it keeps what it holds in the item it drops; placed again, it holds it again.")
                .whatCanGoWrong("It takes nothing and gives nothing.",
                        "It has no energy: sealed, it lets nothing out, and nothing annihilates. Bring the energy"
                                + " back.")
                .register();
    }
}
