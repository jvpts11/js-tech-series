/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.advancement.JscEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * The network and the work it does: machines joining a Mainframe, Operations, IQL, autocrafting, and the ways an
 * Operation or a network goes wrong, which stay hidden until they happen to somebody.
 */
public final class NetworkAdvancements extends AdvancementTab {

    private static final String COMPUTERCRAFT = "computercraft";

    public NetworkAdvancements() {
        super("networks", ResourceLocation.withDefaultNamespace("textures/block/polished_deepslate.png"));
        this.root(ComputingModule.MAINFRAME_ITEM.get(), "Networks & Operations", "Bring a Mainframe onto a network",
                on(JscEvents.MAINFRAME_NETWORK));

        this.task("series_of_tubes", "root", ComputingModule.ETHERNET_CABLE_ITEM.get(), "Series of Tubes",
                "Join a computer to a Mainframe's network", on(JscEvents.COMPUTER_JOINED));
        this.task("select_from_chest", "series_of_tubes", Items.CHEST, "SELECT * FROM chest",
                "Take items out of network storage with an Operation", on(JscEvents.SELECT_DONE));
        this.task("drop_database", "select_from_chest", Items.WRITABLE_BOOK, "DROP DATABASE production_database",
                "Run an IQL statement", on(JscEvents.IQL_QUERY));
        this.goal("set_it_and_forget_it", "drop_database", Items.REPEATER, "Set It and Forget It",
                "Have an IQL JOB run on its own", on(JscEvents.IQL_JOB));
        this.secret("two_out_of_three", "select_from_chest", Items.BUNDLE, "Two Out of Three Ain't Bad",
                "Have an Operation finish only partly done", on(JscEvents.OPERATION_PARTIAL));
        this.secret("deadlock", "select_from_chest", Items.CHAIN, "Deadlock",
                "Have an Operation give up waiting on a locked resource", on(JscEvents.OPERATION_LOCKED));

        this.task("recipe_for_success", "series_of_tubes", ComputingModule.PATTERN_ENCODER_ITEM.get(),
                "Recipe for Success", "Encode a crafting pattern", on(JscEvents.PATTERN_ENCODED));
        this.task("the_factory_must_grow", "recipe_for_success", ComputingModule.CRAFTING_COMPUTER_ITEM.get(),
                "The Factory Must Grow", "Finish an autocraft", on(JscEvents.AUTOCRAFT_DONE));
        this.goal("machine_learning", "the_factory_must_grow", Items.FURNACE, "Machine Learning",
                "Finish an autocraft through a machine", on(JscEvents.MACHINE_AUTOCRAFT));
        this.challenge("supply_chain_issues", "the_factory_must_grow", Items.HOPPER, "Supply Chain Issues",
                "Finish an autocraft of ten stages or more", on(JscEvents.LONG_AUTOCRAFT));
        this.task("just_in_time", "series_of_tubes", ComputingModule.IMPORT_BUS_ITEM.get(), "Just-in-Time",
                "Put an Import Bus and an Export Bus on one network", on(JscEvents.BUSES_PAIRED));
        this.secret("who_unplugged_the_server", "series_of_tubes", Items.SHEARS, "Who Unplugged the Server?",
                "Cut the last cable between a running computer and its Mainframe", on(JscEvents.MAINFRAME_CUT));

        this.secret("there_can_be_only_one", "root", ComputingModule.MAINFRAME_ITEM.get(), "There Can Be Only One",
                "Put two Mainframes on one network", on(JscEvents.MAINFRAME_CONFLICT));
        this.goalWith(COMPUTERCRAFT, "talking_to_the_neighbours", "root",
                ComputingModule.NETWORK_GATEWAY_ITEM.get(), "Talking to the Neighbours",
                "Hear a ComputerCraft computer through a Network Gateway", on(JscEvents.COMPUTERCRAFT_MESSAGE));
    }
}
