/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Declares the recipes chapter of the Learning IQL book: five ready queries and statements for what a network is
 * asked to do most often, each with the era, the program and the engine it needs.
 */
final class IqlRecipesPages {

    private IqlRecipesPages() {
    }

    /** Declares the section and entries of the recipes chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("iql_recipe_list").titled("Recipes")
                .icon(ComputingModule.MAINFRAME).in(chapter).register();
        keepStock(guide, section);
        storeByType(guide, section);
        craftOnAnInterval(guide, section);
        redstoneDoor(guide, section);
        savedReport(guide, section);
    }

    private static void keepStock(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("recipe_keep_stock", section).titled("Keep a stock of an item")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("A stock is how many of an item the network holds. Often you want a number to stay where"
                        + " it is: 512 iron ingots ready for the next build, and never fewer. IQL has three ways to"
                        + " ask for that, and they work differently. Choose the one that fits what you need.")
                .define("Stock", "How many of an item the whole network holds, counted over all its servers.")
                .table("The ways to keep a stock", "Way", "Era", "What it needs")
                .row(GuideCell.words("KEEP, a level that Prophet YourIQL holds"),
                        GuideCell.words("Legacy and newer"),
                        GuideCell.words("Prophet YourIQL running on the Mainframe"))
                .row(GuideCell.words("A job that restocks below a line"),
                        GuideCell.words("Legacy and newer"),
                        GuideCell.words("The Midsoft IQL Server running, and the Automation Engine installed"))
                .row(GuideCell.words("An IF on one action, once"),
                        GuideCell.words("Vintage and newer"),
                        GuideCell.words("An engine that runs actions, such as the Midsoft IQL Server"))
                .paragraph("KEEP belongs to Prophet YourIQL ([](jsc:prophet_youriql)). You name the item and the"
                        + " level, and the engine works out the crafts that reach the level and keep it there.")
                .subheading("KEEP: name the level, and let the engine hold it")
                .paragraph("The engine counts what is already on its way: the items being crafted or moved toward"
                        + " the stock. So it orders only what is still missing.")
                .steps("Install Prophet YourIQL on the Mainframe, and make sure the Mainframe is on.",
                        "Open the Prophet Reactive Console on a computer of the network.",
                        "Type the KEEP line in the console and press [[Enter]].",
                        "Type SHOW STATES to see the item and its status.")
                .table("Lines for KEEP", "Line", "What it asks for")
                .row(GuideCell.code("KEEP iron_ingot >= 512"),
                        GuideCell.words("At least 512 iron ingots, crafted as needed."))
                .row(GuideCell.code("KEEP iron_ingot BETWEEN 256 AND 512"),
                        GuideCell.words("Between 256 and 512 iron ingots."))
                .paragraph("The spaces around >= are required, and so are the words BETWEEN and AND. A name with no"
                        + " colon means minecraft:, so iron_ingot is the vanilla iron ingot. A mod's item needs its"
                        + " full name, with the mod's name before the colon.")
                .paragraph("When the level is kept, the console confirms it with a line that ends in \"kept at 512 or"
                        + " more\".")
                .paragraph("The engine looks at the network in ticks. One tick is one step of the game, and 20 ticks"
                        + " make one second.")
                .define("Tick", "One step of the game: 20 of them make one second.")
                .table("Settings of the engine", "Setting", "Choices", "Default")
                .row(GuideCell.words("Look at the network every"),
                        GuideCell.words("20, 40, 100 or 200 ticks: 1, 2, 5 or 10 seconds"),
                        GuideCell.words("20 ticks, 1 second"))
                .row(GuideCell.words("Ask for at most, in one craft"),
                        GuideCell.words("64, 256, 1,024 or 4,096 items"),
                        GuideCell.words("1,024 items"))
                .paragraph("Two settings change how Prophet works: the look interval and the batch per craft. Prophet"
                        + " reacts only while the Mainframe is on, and only when reacting is turned on.")
                .paragraph("A watch, written WATCH item < n DO statement, runs that statement once when a level"
                        + " crosses the line you set. Inside a watch, CRAFT item TO n makes the item up to a level.")
                .paragraph("Type SHOW STATES to see each state's status. Cannot hold means the network cannot reach"
                        + " the level right now.")
                .subheading("A job: restock when the stock falls below a line")
                .paragraph("A job is a saved statement that the network runs by itself ([](jsc:iql_jobs)). The"
                        + " Automation Engine keeps the jobs and fires them on time, and it does so only while the"
                        + " Mainframe is on.")
                .define("Automation Engine", "The Mainframe's service that keeps the jobs and fires them on time.")
                .paragraph("A job with a WHEN trigger fires on the rising edge of its condition: the moment the"
                        + " condition turns from false to true. It fires once for that moment, and not again while"
                        + " the condition stays true.")
                .define("Rising edge", "The moment a condition turns from false to true.")
                .paragraph("The job crafts an item from a pattern, the recipe the network holds for that item.")
                .define("Pattern", "The recipe the network holds for an item, which tells it what to craft that item"
                        + " from.")
                .steps("Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).",
                        "Check that the Mainframe is on and that the Midsoft IQL Server is running.",
                        "Make sure the network holds a pattern for the item ([](jsc:patterns)).",
                        "Type the job at the prompt, with iql in front ([](jsc:where_to_type_iql)).")
                .table("A line for a job").fixed("Restock 64 when below 512",
                        "CREATE JOB restock_iron AS CRAFT 64 iron_ingot WHEN qty(iron_ingot) < 512")
                .paragraph("The reply reads \"job restock_iron created\". The job checks the stock about every half"
                        + " second. If the stock is already below 512 at its first check, the job fires at once.")
                .paragraph("The job looks only at the stock on the network, not at what is on its way."
                        + " It fires again only after the stock has reached 512 or more and fallen below it once more.")
                .paragraph("The Automation Manager writes the same job for you, without any IQL. Its form is called"
                        + " Restock below: you give it an item and an amount, and it creates a job that crafts that"
                        + " amount when the stock falls below the same amount ([](jsc:automation_manager)).")
                .subheading("An IF guard: one craft, when the stock is low")
                .paragraph("An IF clause on one action runs that action once, and only if its condition holds. It"
                        + " does not come back by itself, so use it for one order, not for a standing rule.")
                .table("A line with an IF").fixed("Craft 64 only when the stock is below 100",
                        "CRAFT 64 iron_ingot IF qty < 100")
                .paragraph("The IF reads the network's whole stock of the item the action names. When the condition"
                        + " does not hold, the reply reads \"the IF did not hold, so the CRAFT was not run\". That is"
                        + " not an error: the line went through and did nothing.")
                .ifSomethingGoesWrong("The reply reads \"syntax: KEEP item >= n, or KEEP item BETWEEN a AND b\".",
                        "Write the spaces around >= and the words BETWEEN and AND, and give a level with each.",
                        "The reply reads \"no item is called steel_ingt\".",
                        "Check the item's id. A mod's item needs its mod's name before the colon.",
                        "The reply reads \"jobs are kept by the Automation Engine; install it on the"
                                + " Mainframe first\".",
                        "Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).",
                        "The reply reads \"the network's engine keeps no views, procedures or jobs\".",
                        "Prophet YourIQL and NextgreIQL keep no jobs. Use the Midsoft IQL Server for a job"
                                + " ([](jsc:engines)).",
                        "SHOW STATES says Cannot hold.",
                        "The network cannot reach the level. Give the network a pattern for the item and the"
                                + " materials that pattern uses.",
                        "Nothing fires while the Mainframe is off.",
                        "Turn the Mainframe on. Jobs and Prophet YourIQL wait for it.")
                .seeAlso("jsc:iql_jobs", "jsc:prophet_youriql", "jsc:automation_manager", "jsc:engines")
                .register();
    }

    private static void storeByType(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("recipe_store_by_type", section).titled("Store items by type")
                .icon(ComputingModule.IMPORT_BUS_ITEM)
                .paragraph("Sometimes a network should take in only some kinds of item, such as iron ore and coal and"
                        + " nothing else. You set that with an Import Bus, which brings items into the network from"
                        + " the inventory it faces ([](jsc:buses)).")
                .define("Import Bus", "A part on a data cable that moves items from the inventory it faces into the"
                        + " network.")
                .paragraph("A bus moves only the items on its filter. You set the"
                        + " filter with a SET BUS line ([](jsc:set_bus)). Write the bus's name in single quotes when"
                        + " it has a space in it.")
                .define("Filter", "The list of items a bus may move.")
                .table("What the recipe needs", "Need", "What it takes")
                .row(GuideCell.words("Era"),
                        GuideCell.words("Legacy and newer for a filter; Advanced for tags"))
                .row(GuideCell.words("Program"),
                        GuideCell.words("None: you type the line at the prompt"))
                .row(GuideCell.words("Engine"),
                        GuideCell.words("The Mainframe's engine running; every Mainframe has the Midsoft IQL Server"))
                .steps("Place an Import Bus on a data cable, facing the inventory it takes items from.",
                        "Give the bus a name, as the buses page explains ([](jsc:buses)).",
                        "Type iql SET BUS 'Ore in' FILTER ONLY iron_ore, coal at the prompt.",
                        "Open the bus's window to check it ([](jsc:bus_window)).")
                .table("Lines for a bus named Ore in", "Line", "What the bus does")
                .row(GuideCell.code("SET BUS 'Ore in' FILTER ONLY iron_ore, coal"),
                        GuideCell.words("Moves only iron ore and coal."))
                .row(GuideCell.code("SET BUS 'Ore in' FILTER ALL BUT dirt"),
                        GuideCell.words("Moves everything except dirt."))
                .row(GuideCell.code("SET BUS 'Ore in' FILTER NONE"),
                        GuideCell.words("Empties the filter, so the bus moves nothing."))
                .row(GuideCell.code("SET BUS 'Ore in' FILTER TAG c:ores, c:logs"),
                        GuideCell.words("Moves the items that carry these tags."))
                .row(GuideCell.code("SET BUS 'Ore in' WHEN STOCK iron_ore < 512"),
                        GuideCell.words("Waits while the network holds 512 or more iron ore."))
                .paragraph("Advanced buses can filter by tag. The c: prefix marks the tags that many mods share.")
                .define("Tag", "A group of items that share a label, such as c:ores for ores.")
                .table("What each era of Import Bus can set", "Era", "Filter", "Tags", "Conditions")
                .row(GuideCell.words("Vintage"), GuideCell.words("No"), GuideCell.words("No"),
                        GuideCell.words("No"))
                .row(GuideCell.words("Legacy"), GuideCell.words("Yes"), GuideCell.words("No"),
                        GuideCell.words("No"))
                .row(GuideCell.words("Transition"), GuideCell.words("Yes"), GuideCell.words("No"),
                        GuideCell.words("No"))
                .row(GuideCell.words("Standard"), GuideCell.words("Yes"), GuideCell.words("No"),
                        GuideCell.words("Yes"))
                .row(GuideCell.words("Advanced"), GuideCell.words("Yes"), GuideCell.words("Yes"),
                        GuideCell.words("Yes"))
                .paragraph("A filter holds 5 items, and a bus holds 4 tags. Conditions are the WHEN lines: they keep a"
                        + " bus waiting until the network meets them ([](jsc:bus_conditions)).")
                .paragraph("A filter decides what a bus moves, not where the items go. The network decides which server"
                        + " keeps them.")
                .ifSomethingGoesWrong(
                        "The reply reads \"the bus Ore in cannot be set so: its era or its kind has no such setting\".",
                        "The bus is a Vintage bus, which has no filter, or you asked for tags on a bus below Advanced."
                                + " A filter of more than 5 items is refused as well, so split the list.",
                        "The reply reads \"no item or fluid named steel_ingt\".",
                        "Check the spelling of the item's id, such as iron_ore.",
                        "The reply reads \"no bus named Ore in on this network\".",
                        "Check the bus's name and its spelling. The bus must be on this network.",
                        "The bus moves nothing.",
                        "Check that the bus is on (SET BUS 'Ore in' ON), that its filter names items the chest holds,"
                                + " and that its WHEN condition holds.")
                .seeAlso("jsc:buses", "jsc:bus_filters_and_tags", "jsc:bus_conditions", "jsc:set_bus")
                .register();
    }

    private static void craftOnAnInterval(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("recipe_nightly_craft", section).titled("Craft on an interval")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("This recipe has the network craft an item again and again, with a gap between each craft."
                        + " You set the gap as an interval, written as a duration after EVERY, such as 30s or 1h"
                        + " ([](jsc:iql_jobs)).")
                .paragraph("The interval counts game ticks, so it measures game time, not the time of day. A job fires"
                        + " only while the Automation Engine is installed and the Mainframe is on.")
                .paragraph("A craft cannot be set to a time of day. The only time window in IQL belongs to a bus"
                        + " ([](jsc:bus_conditions)).")
                .table("What the recipe needs", "Need", "What it takes")
                .row(GuideCell.words("Era"), GuideCell.words("Legacy and newer, with the Automation Engine"))
                .row(GuideCell.words("Program"),
                        GuideCell.words("The Automation Engine installed on the Mainframe, and the Mainframe on"))
                .row(GuideCell.words("Engine"),
                        GuideCell.words("The Midsoft IQL Server running: the other engines keep no jobs"))
                .row(GuideCell.words("Pattern"), GuideCell.words("A pattern for the item on the network"))
                .steps("Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).",
                        "Make sure the network holds a pattern for the item ([](jsc:patterns)).",
                        "Type the job at the prompt, with iql in front and its number and its interval.",
                        "Wait for one interval. The first run comes one interval after the job's first check.")
                .table("A line for a job").fixed("Craft 64 torches every hour",
                        "CREATE JOB torch_hourly AS CRAFT 64 torch EVERY 1h")
                .paragraph("The Automation Manager has the same feature as a form, called Batch Craft. Its field is"
                        + " labelled Every (30s), and it takes a duration such as 30s or 5m.")
                .table("Forms of a duration", "Written", "Ticks", "Seconds")
                .row(GuideCell.code("100t"), GuideCell.words("100"), GuideCell.words("5"))
                .row(GuideCell.code("30s"), GuideCell.words("600"), GuideCell.words("30"))
                .row(GuideCell.code("5m"), GuideCell.words("6,000"), GuideCell.words("300"))
                .row(GuideCell.code("1h"), GuideCell.words("72,000"), GuideCell.words("3,600"))
                .paragraph("A bare number counts seconds. A letter after the number changes the unit: s for seconds,"
                        + " m for minutes, h for hours and t for ticks. There are 20 ticks in a second.")
                .paragraph("Always write the number. A CRAFT with no number asks the network for as much as it can"
                        + " make, every time the job runs.")
                .ifSomethingGoesWrong("The reply reads \"no pattern crafts torch\".",
                        "The network holds no pattern for the item. Give it one first ([](jsc:patterns)).",
                        "The reply reads \"unknown item: torches\".",
                        "Use the item's id, such as torch, and not its name.",
                        "The reply reads \"the network has no running Mainframe\".",
                        "Turn the Mainframe on. The job's craft cannot start without it.",
                        "The reply reads \"jobs are kept by the Automation Engine; install it on the"
                                + " Mainframe first\".",
                        "Install the Automation Engine on the Mainframe ([](jsc:automation_engine)).",
                        "The reply reads \"the network's engine keeps no views, procedures or jobs\".",
                        "The Automation Manager says \"Batch Craft needs an item and a valid interval"
                                + " (e.g. 30s, 5m).\"",
                        "Fill in the item, and an interval above zero such as 30s or 5m.",
                        "Use the Midsoft IQL Server for the job ([](jsc:engines)).")
                .seeAlso("jsc:iql_jobs", "jsc:automation_manager", "jsc:how_autocrafting_works",
                        "jsc:asking_for_a_craft")
                .register();
    }

    private static void redstoneDoor(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("recipe_redstone_door", section).titled("A redstone door")
                .icon(ComputingModule.STANDARD_REDSTONE_INTERFACE)
                .paragraph("A Redstone Interface lets a computer read a redstone signal, or send one. You place it,"
                        + " link it to the computer with a peripheral cable, and it reads the redstone beside it or"
                        + " emits a signal there ([](jsc:redstone_interfaces)).")
                .define("Redstone signal", "The on-or-off power of redstone, with a strength from 0 to 15. Strength"
                        + " 15 is the strongest.")
                .paragraph("A SET REDSTONE line makes the interface emit a strength. A door that reacts to redstone"
                        + " next to the interface receives that signal. A strength of 15 is the strongest, and 0"
                        + " turns the signal off.")
                .table("What the recipe needs", "Need", "What it takes")
                .row(GuideCell.words("Era"),
                        GuideCell.words("Every era from Vintage to Advanced has a Redstone Interface"))
                .row(GuideCell.words("Program"),
                        GuideCell.words("None: you type the line at the prompt ([](jsc:where_to_type_iql))"))
                .row(GuideCell.words("Link"),
                        GuideCell.words("A peripheral cable from the interface to the computer"))
                .steps("Place a Redstone Interface beside the door.",
                        "Link the interface to the computer with a peripheral cable ([](jsc:peripheral_cables)).",
                        "Name the interface in its own window. The default name is Redstone Interface, so give it a"
                                + " name you can type, such as Gate ([](jsc:redstone_interfaces)).",
                        "Type iql SET REDSTONE 'Gate' OUT 15 at the prompt, to send the strongest signal.",
                        "Type iql SET REDSTONE 'Gate' OUT 0 at the prompt, to turn the signal off.")
                .table("Lines of SET REDSTONE", "Line", "What it does")
                .row(GuideCell.code("SET REDSTONE 'Gate' IN"),
                        GuideCell.words("Reads the signal beside the interface."))
                .row(GuideCell.code("SET REDSTONE 'Gate' OUT 15"),
                        GuideCell.words("Emits the strongest signal, 15."))
                .row(GuideCell.code("SET REDSTONE 'Gate' OUT 0"),
                        GuideCell.words("Emits no signal."))
                .paragraph("The reply names the interface and what it did, such as \"Redstone Interface Gate emits"
                        + " 15\". Quote the name in single quotes when it has a space.")
                .ifSomethingGoesWrong("The reply reads \"no Redstone Interface named Gate is linked to this machine\".",
                        "The name is spelled differently, or the interface is not linked to this computer. Check the"
                                + " name in its window, and the cable.",
                        "The reply reads \"SET REDSTONE needs IN, or OUT and a strength from 0 to 15\".",
                        "You wrote OUT without a strength. Add a number from 0 to 15.",
                        "The reply reads \"not a redstone strength, which runs from 0 to 15: 16\".",
                        "Use a strength from 0 to 15.",
                        "The reply reads \"SET REDSTONE needs the interface's name, in quotes when it has spaces\".",
                        "Write the name right after SET REDSTONE, in single quotes when it has a space.",
                        "The interface reads and emits nothing.",
                        "Its cable has no computer at its other end. Link the interface to a computer.")
                .seeAlso("jsc:redstone_interfaces", "jsc:peripheral_cables", "jsc:set_redstone",
                        "jsc:where_to_type_iql")
                .register();
    }

    private static void savedReport(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("recipe_report", section).titled("A saved report")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("A report is a query you save under a name, so you can read the same numbers again later."
                        + " In IQL a saved query is a view ([](jsc:iql_views)). Each time you run it, the view reads"
                        + " the network again, so the numbers are current.")
                .paragraph("Only the Midsoft IQL Server keeps views. On a network whose engine is Prophet YourIQL or"
                        + " NextgreIQL, CREATE VIEW is refused.")
                .define("Row", "One line of a result. In the items table, one line is one kind of item.")
                .table("What the recipe needs", "Need", "What it takes")
                .row(GuideCell.words("Era"),
                        GuideCell.words("Vintage at the prompt; Legacy and newer for the studio"))
                .row(GuideCell.words("Program"),
                        GuideCell.words("None at the prompt; the IQL Server Management Studio for a file"))
                .row(GuideCell.words("Engine"),
                        GuideCell.words("The Midsoft IQL Server running, which every Mainframe has"))
                .steps("Type the CREATE VIEW line at the prompt, with iql in front. The reply reads \"view stock_report"
                                + " created\".",
                        "Type iql QUERY stock_report to read the report.",
                        "To keep the results in a file, open the IQL Server Management Studio, run the query, and"
                                + " choose Results to File.")
                .table("Lines for a report").fixed("Save it",
                        "CREATE VIEW stock_report AS QUERY items WHERE qty > 0 ORDER BY qty DESC LIMIT 20")
                .fixed("Read it", "QUERY stock_report")
                .fixed("Drop it", "DROP VIEW stock_report")
                .paragraph("The view's name can be at most 64 characters long. Do not name a view after a table: QUERY"
                        + " items would then run your view, not the items table.")
                .paragraph("Results to File writes the grid of results as a comma-separated values (CSV) file, a plain"
                        + " text table that spreadsheets open.")
                .table("The tables you can read", "Table", "Columns")
                .row(GuideCell.code("items"), GuideCell.code("item, qty, server, tag, enchant"))
                .row(GuideCell.code("servers"), GuideCell.code("name, used, free, era"))
                .row(GuideCell.code("disks"), GuideCell.code("disk, server, tier, size, used"))
                .row(GuideCell.code("operations"),
                        GuideCell.code("id, verb, state, item, moved, requested, priority"))
                .row(GuideCell.code("recipes"), GuideCell.code("recipe, where, machine, chance"))
                .row(GuideCell.code("computers"), GuideCell.code("name, kind, era, state, os"))
                .paragraph("The items table also reads name, count, amount, damaged and durability in a WHERE or an"
                        + " ORDER BY, though they do not show as columns. The name is the item's name as the game"
                        + " shows it.")
                .paragraph("The tag column shows one tag: the first c: tag of the item, in alphabetical order. An item"
                        + " with two c: tags shows only one, so a WHERE on tag can miss the other. An item with no c:"
                        + " tag shows its first tag.")
                .paragraph("A job can run a query too, but it throws the rows away, so a job gives you no report. Run"
                        + " the view yourself. A script file cannot hold a report either, because run refuses QUERY.")
                .ifSomethingGoesWrong("The reply reads \"no view named stock_report\".",
                        "The view was not saved, or it has another name. Type the CREATE VIEW line again.",
                        "The reply reads \"IQL recursion too deep (a procedure or view referencing itself?)\".",
                        "The view calls itself. Remove the reference to the view's own name.",
                        "The reply reads \"IQL stopped after 4096 statements for one request\".",
                        "The view makes too many statements for one request. Make it shorter, or take out a procedure.",
                        "The reply reads \"a name is at most 64 characters long\".",
                        "Shorten the view's name.",
                        "The studio reads \"There are no results to save.\"",
                        "Run the query first, so there are results to save.",
                        "The reply reads \"QUERY/COUNT are not supported by 'run', use 'operation' instead\".",
                        "Type the view's name at the prompt, or open the studio.")
                .seeAlso("jsc:iql_views", "jsc:studio_overview", "jsc:iql_limits", "jsc:items_as_data")
                .register();
    }
}
