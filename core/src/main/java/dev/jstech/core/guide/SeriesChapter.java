/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.registry.CoreItems;
import net.minecraft.world.item.Items;

/**
 * The series' own chapter, the first of the Technical Reference: what every mod of the series shares and a player
 * meets in all of them, the eras computers come in, the tiers machines come in, and the network that joins them.
 * Its files and its sentences ship with the Core, under the series' namespace.
 */
final class SeriesChapter {

    private static final ModGuide GUIDE = CoreItems.CONTENT.guide("jstech");

    private SeriesChapter() {
    }

    static void declare() {
        GUIDE.chapter().titled("The Series").order(0).tab("#FFB8BCC2")
                .about("What every mod of the series shares: the eras computers come in, the tiers machines come"
                        + " in, and the network that joins them all.")
                .register();
        final ModGuide.SectionRef eras = GUIDE.section("eras").titled("The eras").icon(() -> Items.CLOCK)
                .register();
        final ModGuide.SectionRef tiers = GUIDE.section("tiers").titled("The tiers")
                .icon(() -> Items.IRON_INGOT).register();
        final ModGuide.SectionRef network = GUIDE.section("network").titled("The network")
                .icon(() -> Items.REDSTONE).register();
        GUIDE.page("hardware_eras", eras).titled("Eras of hardware").icon(() -> Items.CLOCK)
                .whatItIs("An era is a generation of computing hardware, from the Vintage, like the machines of the"
                        + " early 1990s, to the Advanced, like the machines of today. Every computer and every part"
                        + " belongs to one.")
                .whatItIsFor("Eras are how computers grow: a newer era is faster at everything, with more cores at"
                        + " higher clocks, more memory and faster disks, and it runs newer systems and programs.")
                .define("Word size", "How many bits a processor works with at once: 16 in the Vintage, 32 in the"
                        + " Legacy, 64 from the Transition on. On a network's disks it decides how much room an"
                        + " item takes.")
                .table("The eras that have hardware today")
                .property("Vintage", "Early 1990s, 16-bit, floppy disks")
                .property("Legacy", "Around 2000, 32-bit, CDs")
                .property("Transition", "Late 2000s, 64-bit, DVDs")
                .property("Standard", "The 2010s, 64-bit, USB sticks and DVDs")
                .property("Advanced", "Today, 64-bit, USB sticks and Blu-ray")
                .howToGetIt("Each era has its own creative tab in J's Computers, from J's Computers - Vintage to"
                        + " J's Computers - Advanced, with the same shelves in each.")
                .howToUseIt("Pick an era and build everything of it: a computer of an era takes only a motherboard"
                                + " of its own era, and the board takes only parts of its era.",
                        "Install a system of that era or an older one: a system needs hardware of its own era or"
                                + " newer.",
                        "Move up an era when you want more speed; the old machines keep working beside the new.")
                .whatCanGoWrong("A part does not go into the computer at all.",
                        "It is from another era than the motherboard. Use a part of the board's era.",
                        "The installer refuses the system.",
                        "The system is from a newer era than the computer. Use an older system, or newer"
                                + " hardware.")
                .register();
        GUIDE.page("industrial_tiers", tiers).titled("Industrial tiers").icon(() -> Items.IRON_INGOT)
                .whatItIs("A tier is a step of industry, from T0 to T9: how hard a material or a machine is to make,"
                        + " and what has to exist before it can be made.")
                .whatItIsFor("Tiers set the order things are reached in, from the first machines that run on coal"
                        + " to the last ones. They are a different scale from the eras: an era says how new a"
                        + " computer is, a tier how far industry has come.")
                .howToGetIt("There is nothing to take: tiers are reached by making things. Today the first tiers"
                        + " have content; the higher ones come with later updates.")
                .howToUseIt("Start with the machines of the first tier: the Coal Generator, the Macerator, the"
                                + " Compressor and the Electric Furnace.",
                        "Use what they make, dust and plates, to reach what needs them.")
                .whatCanGoWrong("A material shows in a list but cannot be made.",
                        "It belongs to a tier that has no machines yet. It comes with a later update.")
                .register();
        GUIDE.page("data_network", network).titled("The network").icon(() -> Items.REDSTONE)
                .whatItIs("The network is computers joined by data cables around one large computer, the"
                        + " Mainframe. Each network has exactly one Mainframe, and every machine on it shares its"
                        + " name.")
                .whatItIsFor("A network stores items as data on its disks, gives them back when asked, moves them"
                        + " between machines and crafts for you. Machines of other mods of the series join it"
                        + " through the same cables.")
                .define("Operation", "A request the network carries out: store this, give me that, craft this."
                        + " Every one waits in a queue on the Mainframe until it is done.")
                .howToGetIt("Build a Mainframe from J's Computers, and lay data cables from it to the machines"
                        + " that should join.")
                .howToUseIt("Build the Mainframe and give it its parts.",
                        "Lay data cable from it to a Server Rack with servers and disks, where the items are kept.",
                        "Join a computer to the network and open its Network program to put items in and take"
                                + " them out.")
                .whatCanGoWrong("Two Mainframes, and nothing works.",
                        "A network has one Mainframe. Split the cables, so that each Mainframe has a network of"
                                + " its own.")
                .register();
    }
}
