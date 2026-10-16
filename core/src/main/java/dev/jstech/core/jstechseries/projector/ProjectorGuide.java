/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.registry.CoreItems;

/**
 * The Core's section on multiblocks in its chapter of the manual, and the pages in it of the tools that show and build
 * them: the Structure Projector's and the Multiblock Builder's.
 */
public final class ProjectorGuide {

    private ProjectorGuide() {
    }

    /** Declares the section and its pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        final ModGuide.SectionRef multiblocks = CoreItems.CONTENT.guide().section("multiblocks")
                .titled("Multiblocks").icon(StructureProjector.BLOCK::asItem).register();
        CoreItems.CONTENT.guide().page("structure_projector", multiblocks).titled("Structure Projector")
                .icon(StructureProjector.BLOCK::asItem).covers(StructureProjector.BLOCK::asItem)
                .whatItIs("A projector that shows any multiblock of any mod you have in the world, before you build"
                        + " it: the blocks still missing as pale ghosts, a wrong block outlined in red, and above"
                        + " the structure how much is in place.")
                .whatItIsFor("Building a multiblock right the first time, block by block, without counting layers"
                        + " from a picture.")
                .define("Ghost", "A pale, see-through block where a block of the multiblock still goes. Look at one"
                        + " to see which block and on which layer.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that build multiblocks give it"
                        + " their recipes.")
                .howToUseIt("Place it beside where the multiblock is to stand: it faces you as you place it, and"
                        + " shows the multiblock on the side it faces.",
                        "Use it to open its screen and choose the multiblock from the list.",
                        "Turn it with the knob, mirror it if it allows, and show it a layer at a time with the"
                                + " layer's wheel, or only what is still missing.",
                        "Build into the ghosts: each goes as its block is placed, and the count above follows.")
                .whatCanGoWrong("Nothing shows.",
                        "Its switch is off, no multiblock is chosen, or you are more than 64 blocks away.",
                        "A block stays outlined in red.",
                        "Another block stands there. Break it and place the one the ghost shows.")
                .register();
        CoreItems.CONTENT.guide().page("multiblock_builder", multiblocks).titled("Multiblock Builder")
                .icon(MultiblockBuilder.ITEM::get).covers(MultiblockBuilder.ITEM::get)
                .whatItIs("A handheld instrument that builds a multiblock around its controller for you, block by"
                        + " block, from your inventory: it says what it will place and how much of each you have, and"
                        + " asks what goes where a place takes several blocks.")
                .whatItIsFor("Building a large multiblock without placing every block of it by hand.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that build multiblocks give it"
                        + " their recipes.")
                .howToUseIt("Place the multiblock's controller, then use the builder on it: it is aimed at it, and its"
                        + " display shows what is missing and what you have.",
                        "On the page of choices, choose with the arrows what goes where a place takes several blocks.",
                        "Press OK: it builds, a block at a time, as you would place them. HOLD holds it and carries"
                                + " it on.",
                        "SHOW shows the multiblock's ghosts while you hold it; LIGHT lights its display.")
                .whatCanGoWrong("It says this controller forms no multiblock it knows.",
                        "The block is not a multiblock's controller. Use it on the controller itself.",
                        "It stops with a reason.",
                        "You ran out of a block, walked too far away, or a place is protected or taken. Its display"
                                + " says which; fix it and press OK again.")
                .register();
    }
}
