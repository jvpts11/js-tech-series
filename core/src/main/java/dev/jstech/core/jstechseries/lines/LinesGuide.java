/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.lines;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;

/** The page of the basic pipes in the Core's chapter of the manual. */
public final class LinesGuide {

    /* The Core's chapter's section on cables, where the pipes' page goes. */
    private static final ModGuide.SectionRef CABLES = new ModGuide.SectionRef("cables");

    private LinesGuide() {
    }

    /** Declares the page, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        CoreItems.CONTENT.guide().page("basic_pipes", CABLES).titled("Basic Pipes")
                .icon(SharedPipes.COPPER_PIPE::asItem).covers(SharedPipes.COPPER_PIPE::asItem)
                .covers(SharedPipes.IRON_GAS_PIPE::asItem).covers(SharedPipes.VACUUM_JACKETED_PIPE::asItem)
                .whatItIs("Three pipes, each holding its block alone, with a sight glass in each block that shows what"
                        + " flows inside at its level, moving the way it goes.")
                .whatItIsFor("Carrying liquids and gases from what makes them to what uses or keeps them.")
                .table("What each carries")
                .property("Copper Pipe", "Liquids up to 200 °C, 100 mB a tick")
                .property("Iron Gas Pipe", "Gases at low pressure, 100 mB a tick")
                .property("Vacuum-Jacketed Pipe", "Liquids from the coldest up to 200 °C, 400 mB a tick")
                .howToGetIt("Craft six of a row of glass panes between two rows of copper ingots, or of iron ingots"
                        + " for the gas pipe; four cryogenic pipes of iron, glass panes and obsidian. With J's"
                        + " Industrial installed, they are its first pipes, made with its own recipes.")
                .howToUseIt("Lay a pipe from a machine's output to another's input: the machine pushes what it"
                                + " makes along it.",
                        "Two tanks never pour into each other by themselves: set the end of the one to empty to"
                                + " pull, with the Basic Wrench.",
                        "Each end can be set apart: its mode, its priority, its rate, its channel and its filter.")
                .whatCanGoWrong("A pipe bursts or cracks and spills.",
                        "A liquid too hot or too cold for it reached it for a while. Use the pipe made for it.",
                        "Nothing flows.",
                        "The pipe's end may be off, on another channel, or its filter may let nothing through.")
                .register();
    }
}
