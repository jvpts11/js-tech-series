/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.material.MaterialForm;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.core.material.ModMaterial;
import dev.jstech.core.registry.CoreItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The Core's chapter: the little of the library a player handles, the materials every mod shares, the energy they
 * run on, the cable block they lay their cables in, and the manuals themselves.
 */
final class CoreChapter {

    private static final ModGuide GUIDE = CoreItems.CONTENT.guide();

    private CoreChapter() {
    }

    static void declare() {
        GUIDE.chapter().titled("J's Core").order(10).tab("#FF7A8AA8")
                .about("The library every mod of the series stands on. A player handles little of it: the"
                        + " materials the mods share, the energy they run on, the block their cables are laid in,"
                        + " and this manual.")
                .register();
        final ModGuide.SectionRef materials = GUIDE.section("materials").titled("Materials")
                .icon(() -> MaterialItems.get(ModMaterial.IRON, MaterialForm.PLATE).get()).register();
        final ModGuide.SectionRef energy = GUIDE.section("energy").titled("Energy").icon(() -> Items.REDSTONE)
                .register();
        final ModGuide.SectionRef cables = GUIDE.section("cables").titled("Cables").icon(() -> Items.STRING)
                .register();
        final ModGuide.SectionRef manuals = GUIDE.section("manuals").titled("Manuals")
                .icon(() -> CoreGuide.MANUAL.get()).register();
        GUIDE.page("shared_materials", materials).titled("Materials")
                .icon(() -> MaterialItems.get(ModMaterial.IRON, MaterialForm.PLATE).get())
                .coversAll(CoreChapter::materialItems)
                .whatItIs("The metals the mods of the series share, in the forms machines make them into: dust, a"
                        + " metal ground into powder, and plates, a metal pressed flat.")
                .whatItIsFor("Every mod uses the same ones, so the iron plate one mod makes is the iron plate"
                        + " another asks for. Later machines and parts are made from them.")
                .figure(() -> MaterialItems.get(ModMaterial.IRON, MaterialForm.PLATE).get(), "Iron Plate")
                .table("The materials made today")
                .property("Iron Dust", "From iron ore, raw iron or an ingot, in a Macerator")
                .property("Iron Plate", "From an iron ingot, in a Compressor")
                .property("Copper Plate", "From a copper ingot, in a Compressor")
                .howToGetIt("Make them with the machines of J's Industrial, or take them from its creative tab,"
                        + " where they are shown.")
                .howToUseIt("Grind ore in a Macerator: one ore gives two dusts.",
                        "Smelt each dust in a furnace or an Electric Furnace: one dust gives one ingot.",
                        "Press ingots in a Compressor: one ingot gives one plate.")
                .whatCanGoWrong("A dust will not go into a machine.",
                        "Only the recipes the machine knows take it. Dust is smelted, not pressed.")
                .register();
        GUIDE.page("fe_energy", energy).titled("Energy (FE)").icon(() -> Items.REDSTONE)
                .whatItIs("Energy is what machines and computers run on. It is counted in FE, the same unit most"
                        + " machine mods use, so a generator of another mod can run these machines and the other"
                        + " way round.")
                .whatItIsFor("A generator makes it, a cable carries it, a machine spends it while it works.")
                .define("Tick", "The game's beat: 20 ticks make a second. 40 FE a tick is 800 FE a second.")
                .define("Buffer", "The energy a machine can store, like a small battery inside it. It keeps a"
                        + " machine working for a while after its power stops.")
                .howToGetIt("Make it with a generator, such as J's Industrial's Coal Generator.")
                .howToUseIt("Place a generator and give it fuel.",
                        "Put machines against it, or lay energy cable from it to them.",
                        "Watch a machine's energy bar: it fills while energy comes in.")
                .whatCanGoWrong("A machine works in bursts.",
                        "It spends more a tick than its generators make. Add a generator.",
                        "The generator burns fuel but nothing gets energy.",
                        "Nothing touches it and no cable runs from it. Join a machine to it.")
                .register();
        GUIDE.page("cable_block", cables).titled("The cable block").icon(() -> Items.STRING)
                .whatItIs("One block that every mod of the series lays its cables in. Each kind of cable runs in a"
                        + " lane of its own, so cables of different mods can share a block without touching.")
                .whatItIsFor("Running energy, data and the computers' peripheral cables through the same space,"
                        + " each kept to its own line.")
                .howToGetIt("Take a cable from a mod's creative tab: J's Industrial's Energy Cable, J's Computers'"
                        + " data and peripheral cables.")
                .howToUseIt("Place a cable like a block.",
                        "Place a cable of another kind in the same block: it takes its own lane.",
                        "Break the block to take its cables back, each one as its own item.")
                .whatCanGoWrong("Two cables side by side do not join.",
                        "Only cables of the same line join. An energy cable never joins a data cable, even in"
                                + " one block.")
                .register();
        GUIDE.page("this_manual", manuals).titled("This manual").icon(() -> CoreGuide.MANUAL.get())
                .covers(() -> CoreGuide.MANUAL.get())
                .whatItIs("The Technical Reference: the binder that holds a chapter for every mod of the series"
                        + " you have, each with its tab at the edge.")
                .whatItIsFor("Finding out what a thing is and how to use it, without leaving the game.")
                .howToGetIt("You are given one the first time you join a world. Another is in the creative tab"
                        + " J's Core.")
                .howToUseIt("Use it to open it at its cover; click to open it.",
                        "Click a line of the contents, a tab, or a number after See to go there.",
                        "Turn pages with the arrows, the arrow keys or the mouse wheel.",
                        "Use the magnifier on the top edge to search the index by name.",
                        "Hold the manual key over any item, in an inventory or in your hand, to open its page.")
                .whatCanGoWrong("An item opens no page.",
                        "No manual has an entry for it yet.",
                        "A chapter is missing.",
                        "Its mod is not installed: the binder holds the mods you have.")
                .register();
    }

    /** The material items the Core registers, in the order of the materials. */
    private static List<ItemLike> materialItems() {
        final List<ItemLike> items = new ArrayList<>();
        for (final ModMaterial material : ModMaterial.values()) {
            for (final MaterialForm form : material.activeModForms()) {
                items.add(MaterialItems.get(material, form).get());
            }
        }
        return items;
    }
}
