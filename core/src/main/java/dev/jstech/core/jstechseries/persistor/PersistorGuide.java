/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The Core's section on the world in its chapter of the manual, and the Space Persistor's page in it. */
public final class PersistorGuide {

    private PersistorGuide() {
    }

    /**
     * Declares the section and its page, from the Core's constructor, after the Core's chapter; the section, for the
     * other pages on the world.
     */
    public static ModGuide.SectionRef declare() {
        final ModGuide.SectionRef world = CoreItems.CONTENT.guide().section("world")
                .titled("The world").icon(SpacePersistor.BLOCK::asItem).register();
        CoreItems.CONTENT.guide().page("space_persistor", world).titled("Space Persistor")
                .icon(SpacePersistor.BLOCK::asItem).covers(SpacePersistor.BLOCK::asItem)
                .whatItIs("A chunk loader: it keeps an area round it loaded and working while nobody is near, so a"
                        + " quarry far away, a station or a starship left in orbit goes on.")
                .whatItIsFor("Keeping machines working far from you, as far as you want to reach, within the number"
                        + " of chunks you may keep and the server's budget.")
                .define("Chunk", "A column of the world 16 blocks wide. The game loads the chunks near players"
                        + " and lets the rest go; a machine in a chunk let go stops.")
                .howToGetIt("In creative, take it from the J's Tech tab; the mods that need it give it their"
                        + " recipes.")
                .howToUseIt("Place it: it is yours, and keeps a square of three by three chunks round it.",
                        "Use it to open its screen. The map shows the land round it, the chunks it keeps outlined,"
                                + " another owner's hatched; zoom out with the minus.",
                        "Pick a shape with the keys (a square, a circle, a line or only what you click) and turn"
                                + " the radius' drums to reach further.",
                        "Click a chunk on the map to add it, or take it off.",
                        "When the server makes it spend energy, feed it power; its Protection tab holds a fuse and"
                                + " a battery as every machine's does.")
                .whatCanGoWrong("The FULL lamp is lit.",
                        "You keep as many chunks as you may, or the server keeps as many as its budget allows. Take"
                                + " chunks off here or elsewhere.",
                        "The KEEPING lamp is dark.",
                        "It is switched off, short of power, or has nothing to keep: feed it, switch it on, or check"
                                + " its area.")
                .register();
        return world;
    }
}
