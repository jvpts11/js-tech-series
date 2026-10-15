/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.registry.CoreItems;

/**
 * The pages of the series' shared tools in the Core's chapter of the manual: the Basic Wrench, the Network Probe and
 * the Signal Probe.
 */
public final class ToolsGuide {

    /* The Core's chapter's section on cables, where the wrench's page goes. */
    private static final ModGuide.SectionRef CABLES = new ModGuide.SectionRef("cables");

    private ToolsGuide() {
    }

    /** Declares the pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        CoreItems.CONTENT.guide().page("basic_wrench", CABLES).titled("Basic Wrench")
                .icon(SharedTools.WRENCH::get).covers(SharedTools.WRENCH::get)
                .whatItIs("An open-ended spanner of iron, the series' shared wrench.")
                .whatItIsFor("Setting how each end of a pipe or a line of items meets the block it plugs into.")
                .table("The modes of an end")
                .property("Normal", "As the block says: a machine's output gives, its input takes, a tank both")
                .property("Push", "The line only fills the block, and never draws from it")
                .property("Pull", "The line only draws from the block, and never fills it")
                .property("Off", "The end plugs into nothing, and joins no cable beside it")
                .howToGetIt("Craft it of four iron ingots: two at the top corners, two down the middle.")
                .howToUseIt("Use it on the end of a wire, near the block it meets, to go round the modes.",
                        "Sneak to open the end's screen, on a pipe or a line of items: there the mode, the priority, "
                                + "the rate, the channel and the filter are set.",
                        "On any other line, sneak to go round the modes the other way.",
                        "The line says which end it set and how, above the hotbar.",
                        "While it is in your hand, what flows along the wire you look at, which way and how much, "
                                + "shows beside the crosshair.")
                .whatCanGoWrong("Nothing changes.",
                        "Point at the wire itself, near the end you want; a part on the face closes it.")
                .register();
        CoreItems.CONTENT.guide().page("network_probe", CABLES).titled("Network Probe")
                .icon(NetworkProbe.ITEM::get).covers(NetworkProbe.ITEM::get)
                .whatItIs("A handheld instrument in an orange holster that reads a whole line at once, of any kind: "
                        + "energy, liquids, gases or items.")
                .whatItIsFor("Finding what a line joins, what flows along it, where it is too narrow and what it "
                        + "loses on the way.")
                .table("Its pages")
                .property("Summary", "The flow of the last minute against the narrowest cable, what the consumers "
                        + "want, the losses, and how many sources, consumers and stores it joins")
                .property("Members", "Every block it plugs into, by what it is to the line and by kind, with what "
                        + "each gives or takes")
                .property("Flow", "Each second of the last minute, with the most, the least and the mean")
                .property("Losses", "What the cables lost, against what the sources sent")
                .howToGetIt("Take it from the creative tab J's Tech.")
                .howToUseIt("Use it on a wire of a line to link it to that line.",
                        "Use it in the air to hold it up and read its display; the keys under the display turn its "
                                + "pages.",
                        "On the members' page, choose a row with the pad and press OK: those blocks are outlined in "
                                + "the world, through walls, for ten seconds.",
                        "HOLD keeps the reading still, SHOW puts it beside the crosshair while you hold it, LIGHT "
                                + "lights the display and POWER switches it off.")
                .whatCanGoWrong("The display says there is nothing to read.",
                        "The wire it was linked to was taken out, or its chunk is far away; link it again.")
                .register();
        CoreItems.CONTENT.guide().page("signal_probe", CABLES).titled("Signal Probe")
                .icon(SignalProbe.ITEM::get).covers(SignalProbe.ITEM::get)
                .whatItIs("A handheld instrument of the network probe's family, for redstone and for signals sent "
                        + "without wire by frequency.")
                .whatItIsFor("Reading the redstone on a wire, and finding which frequencies are heard where you "
                        + "stand: whose they are, where they come from, how well they arrive and what they carry.")
                .table("Its pages")
                .property("Here", "The strength on the wire it reads, out of 15, with its peak since you linked it, "
                        + "and the frequencies heard where you stand")
                .property("Radio", "The frequencies again, one chosen and told in full: its owner, where it comes "
                        + "from, its bars and its value")
                .table("What a frequency shows")
                .property("Bars", "How well it arrives, from one at the edge of its reach to four close by; the "
                        + "value arrives whole with any bar")
                .property("Clash", "Two transmitters of one owner on one frequency: its receivers give nothing "
                        + "until one is taken off")
                .property("Lost", "A frequency of your team that is not heard where you stand")
                .howToGetIt("Take it from the creative tab J's Tech.")
                .howToUseIt("Use it on a block, a redstone wire or any other, to read the redstone on it.",
                        "Use it in the air to hold it up and read its display; the keys under the display turn its "
                                + "pages.",
                        "Walk with it to find where a receiver or a relay will hear a frequency well: each solid "
                                + "block in the way shortens a transmitter's reach.",
                        "On the radio's page, choose a frequency with the pad and press OK: where it comes from is "
                                + "outlined in the world, through walls, for ten seconds.",
                        "HOLD keeps the reading still, SHOW puts it beside the crosshair while you hold it, LIGHT "
                                + "lights the display and POWER switches it off.")
                .whatCanGoWrong("A frequency you expect is not listed.",
                        "Its transmitter is too far, behind too much wall, or its chunk is not loaded; frequencies of "
                                + "your team show as lost, others are not listed at all.")
                .register();
    }
}
