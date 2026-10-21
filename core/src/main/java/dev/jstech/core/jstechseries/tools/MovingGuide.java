/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.vehicle.Vehicles;
import net.minecraft.world.item.Items;

/**
 * The pages of what moves in the Core's chapter of the manual, in a section of their own: the vehicles of every mod
 * and data pack, with their parts, and the physics tool that moves moving structures by hand.
 */
public final class MovingGuide {

    private MovingGuide() {
    }

    /** Declares the section and its pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        final ModGuide.SectionRef moving = CoreItems.CONTENT.guide().section("moving").titled("Things That Move")
                .icon(() -> Items.MINECART).register();
        CoreItems.CONTENT.guide().page("vehicles", moving).titled("Vehicles").icon(() -> Items.MINECART)
                .covers(Vehicles.ITEM::get).covers(Vehicles.PART_ITEM::get)
                .whatItIs("Every vehicle of the mods and data packs you have: each placed from a Vehicle item that "
                        + "names its kind, and made of parts, each a Vehicle Part item while it is out of one.")
                .whatItIsFor("Travelling and hauling: its seats carry players and robots, and whoever sits in a "
                        + "driving seat drives it.")
                .howToGetIt("As the mod or the data pack that brings the vehicle says. In creative, the Tools & "
                        + "Utilities tab holds every kind and every part.")
                .howToUseIt("Use the item on the ground to place the vehicle there, facing the way you look: it is "
                                + "yours, and its key is cut for you when its kind has keys.",
                        "Use the vehicle to get in; in a driving seat, the walking keys drive it, as it burns its "
                                + "fuel or draws on its battery.",
                        "Use a part on the vehicle near its empty place of that kind to fit it; sneak with a wrench "
                                + "to take one out, or hold its material in the other hand to mend a worn one.",
                        "Use a dye on it to paint it, and its key with the sneak key held to lock or unlock it.",
                        "Sneak with an empty hand near its hitch to hitch a trailer to it, or to let it go.",
                        "Hit it to break it back into its item, which keeps its parts, its fuel and its load, and "
                                + "places it again as it was.")
                .whatCanGoWrong("It does not move.",
                        "Its fuel or its battery is empty, or a part it needs is missing or worn out.",
                        "It cannot be changed or broken.",
                        "It is someone else's: only its owner and whom they trust may set it up.")
                .register();
        CoreItems.CONTENT.guide().page("physics_tool", moving).titled("Physics Tool")
                .icon(PhysicsTool.ITEM::get).covers(PhysicsTool.ITEM::get)
                .whatItIs("A gunmetal tool with three claws round its emitter, which moves moving structures by "
                        + "hand.")
                .whatItIsFor("Setting a moving structure where you want it: lifting it, pushing it, or holding it "
                        + "still in the air.")
                .table("Its modes")
                .property("Grab", "Holds the point aimed at before you as you move and look, as a stiff spring "
                        + "that bears its weight")
                .property("Push", "Gives it a blow where it is aimed")
                .property("Freeze", "Holds it where it stands, in the air too, until it is let go again")
                .howToGetIt("Craft it of a lodestone in the middle, an iron ingot on each of its sides, and an "
                        + "amethyst shard and a redstone dust at two opposite corners. A server may turn the recipe "
                        + "off.")
                .howToUseIt("Aim at a moving structure and hold the use button to act on it in the tool's mode.",
                        "Turn the mouse wheel to change the mode, and with Shift its strength, from a tenth to the "
                                + "whole: how hard it holds and pushes.")
                .whatCanGoWrong("Nothing moves.",
                        "The tool moves only moving structures. A heavy one needs more strength: turn it up.")
                .register();
    }
}
