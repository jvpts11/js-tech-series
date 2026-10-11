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
 * The pages of the engines chapter of Learning IQL: what an engine is, how the Services tab starts, stops and
 * replaces one, the three engines and their versions, the EXPLAIN words and hints of NextgreIQL, the KEEP, WATCH and
 * FORGET words of Prophet YourIQL, and every refusal the engines can give.
 */
final class IqlEnginesPages {

    private IqlEnginesPages() {
    }

    /** Declares the section and entries of the engines chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("engine_choice").titled("Engines")
                .icon(ComputingModule.MAINFRAME).in(chapter).register();
        overview(guide, section);
        engineServices(guide, section);
        midsoftIqlServer(guide, section);
        nextgreIql(guide, section);
        nextgreHints(guide, section);
        prophetYourIql(guide, section);
        prophetWatch(guide, section);
        engineProblems(guide, section);
    }

    private static void overview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql_engines_overview", section).titled("What an engine is")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("When you type an IQL statement, something has to read it and decide how the network does"
                        + " the work. On the network, that something is an engine. It runs as a program on the"
                        + " Mainframe ([](jsc:mainframes)), reads the statement, plans the steps, and hands the plan"
                        + " to the part of the network that carries the work out.")
                .define("Engine", "A program on the Mainframe that reads IQL, plans how the network does the work,"
                        + " and hands the plan on. Only one installed program of this kind plans at a time.")
                .define("Mainframe", "The one computer of a network that runs the network's own software, such as"
                        + " the engine that plans its work.")
                .paragraph("A new Mainframe comes with the Midsoft IQL Server installed and running, in the version of"
                        + " its age. Two more engines exist: NextgreIQL and Prophet YourIQL. Each speaks its own"
                        + " dialect of the language, and each offers extras beyond the core.")
                .define("Dialect", "The flavour of IQL an engine speaks. Every dialect accepts the core language, so"
                        + " the same core statements work on every engine.")
                .table("The engines", "Engine", "Dialect", "Ages", "Extras")
                .row(GuideCell.words("Midsoft IQL Server"), GuideCell.words("IQL"),
                        GuideCell.words("Vintage to Advanced"), GuideCell.words("procedures and views"))
                .row(GuideCell.words("NextgreIQL"), GuideCell.words("NextgreIQL"),
                        GuideCell.words("Legacy to Advanced"), GuideCell.words("explain, planner hints, extensions"))
                .row(GuideCell.words("Prophet YourIQL"), GuideCell.words("YourIQL"),
                        GuideCell.words("Legacy to Advanced"), GuideCell.words("declared states, subscriptions"))
                .paragraph("You manage an engine in the Services tab of the Network Manager ([](jsc:network_manager)),"
                        + " and a program that needs a particular engine says so when you open it. Every engine"
                        + " answers the same core: transfers, crafts and the statements of the core language.")
                .paragraph("Only one engine plans at a time, the active engine. When you stop or replace it, the"
                        + " Operations already running keep going on the plans they started with. You can still"
                        + " browse, deposit and withdraw items with no engine running.")
                .paragraph("With no engine running, transfers keep working, but crafts and IQL queries are refused."
                        + " You start an engine to plan again.")
                .ifSomethingGoesWrong("A craft or a query says the Network Operations Service is unavailable.",
                        "No engine runs. Start one in the Services tab ([](jsc:engine_services)).")
                .seeAlso("jsc:engines", "jsc:engine_services")
                .register();
    }

    private static void engineServices(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("engine_services", section).titled("Starting, stopping and replacing an engine")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("You start, stop and replace engines in the Services tab of the Network Manager. The"
                        + " tab's header card shows the state of the engine in one word: Running, Stopped or"
                        + " Replacing. The buttons under it depend on that word.")
                .subheading("The buttons")
                .table("The buttons of the Services tab", "State", "Buttons", "What you can press")
                .row(GuideCell.words("Running"), GuideCell.words("Configure..., Stop, Replace..."),
                        GuideCell.words("Stop and Replace are available; Configure... is always greyed out."))
                .row(GuideCell.words("Replacing"), GuideCell.words("Configure..., Stop, Replace..."),
                        GuideCell.words("Nothing: all three are greyed out."))
                .row(GuideCell.words("Stopped, or none chosen"), GuideCell.words("Start..."),
                        GuideCell.words("Start..., when any engine is installed."))
                .paragraph("A button does what it says or nothing at all. If the server refuses a press, nothing"
                        + " happens and the tab shows the true state again.")
                .subheading("Stopping an engine")
                .paragraph("Stop asks nothing. It stops the active engine, which stays installed and chosen. Until you"
                        + " start an engine again, the network has no engine running.")
                .paragraph("The tab then says \"No Network Operations Engine is running\". Storage stays, and the"
                        + " Operations in flight carry on. Transfers still work, but crafts and IQL queries are"
                        + " refused until an engine runs.")
                .paragraph("An engine runs only while its Mainframe is on. When the Mainframe is turned off, the engine"
                        + " stops with it.")
                .subheading("Starting an engine")
                .steps("Press Start... in the Services tab.",
                        "Choose the engine in the list titled \"Start a Network Operations Engine\".",
                        "Press Start to confirm.")
                .paragraph("The list starts with the first stopped engine selected. Its note says \"The engine starts"
                        + " at once and plans every new request.\"")
                .paragraph("If you choose an installed engine that is not the active one, the network switches to it"
                        + " at once. The timed steps of a replacement do not run then.")
                .subheading("Replacing an engine")
                .paragraph("A replacement switches the network from the running engine to another installed one."
                        + " It takes time, and the tab shows Replacing until it ends. The new engine starts at the"
                        + " end.")
                .define("Replacement", "The timed switch from one installed engine to another, during which no"
                        + " engine plans.")
                .steps("Press Replace... in the Services tab.",
                        "Choose the engine in the list titled \"Replace the Network Operations Engine\".",
                        "Read what the dialog says, then press Replace to confirm.")
                .paragraph("In the dialog, the Replace button is greyed out when the engine you chose is the one"
                        + " already active.")
                .table("What the Replace dialog says", "Line of the dialog", "What it tells you")
                .row(GuideCell.words("New requests go to <new engine>."),
                        GuideCell.words("Every request from now on is planned by the new engine."))
                .row(GuideCell.words("The <number> Operations in flight finish on the plans they started with."),
                        GuideCell.words("Work already running is not planned again."))
                .row(GuideCell.words("Scripts written in <old engine>'s own statements may stop working;"
                        + " the IQL core works on every engine."),
                        GuideCell.words("Statements that use the old engine's extras may fail, but core IQL"
                                + " still works."))
                .row(GuideCell.words("<Subframe> will take no work until it runs <new engine> too."),
                        GuideCell.words("A Subframe takes work only when it runs the same engine as the Mainframe."))
                .row(GuideCell.words("No other engine is installed on this Mainframe. Install one from its disc"
                        + " to replace this one."),
                        GuideCell.words("There is nothing to switch to yet."))
                .define("Subframe", "A second computer of the network that takes work, when it runs the same engine"
                        + " as the Mainframe.")
                .subheading("How long a replacement takes")
                .paragraph("A replacement takes 40 ticks, plus one tick for every four item types the network stores,"
                        + " up to 2,400 ticks. A tick is the game's basic time step: 20 ticks make a second.")
                .define("Tick", "One twentieth of a second in the game.")
                .define("Item type", "One kind of item the network stores, such as an iron ingot.")
                .table("How long a replacement takes", "Item types stored", "Time")
                .row(GuideCell.words("0"), GuideCell.words("40 ticks, 2 seconds"))
                .row(GuideCell.words("400"), GuideCell.words("140 ticks, 7 seconds"))
                .row(GuideCell.words("9,440 or more"), GuideCell.words("2,400 ticks, 2 minutes"))
                .paragraph("The progress window moves through the steps of the replacement in turn. N in the Keep"
                        + " step is the number of Operations still running.")
                .table("The steps of a replacement", "Step on the progress window", "Share of the time")
                .row(GuideCell.words("Stop taking new plans"), GuideCell.words("2%"))
                .row(GuideCell.words("Keep N Operations in flight on their plans"), GuideCell.words("2%"))
                .row(GuideCell.words("Stop <old engine>, or Nothing to stop when the network had none"),
                        GuideCell.words("2%"))
                .row(GuideCell.words("Start <new engine>"), GuideCell.words("2%"))
                .row(GuideCell.words("Discover storage and machines"), GuideCell.words("22%"))
                .row(GuideCell.words("Build indexes"), GuideCell.words("60%"))
                .row(GuideCell.words("Publish capabilities"), GuideCell.words("10%"))
                .paragraph("Building indexes takes the most time, because the network indexes each item type it"
                        + " stores. The window then says Ready.")
                .paragraph("While a replacement runs, no engine plans. The Operations already in flight carry on, but"
                        + " a new request that needs planning is refused until the new engine is ready. When the time"
                        + " ends, the new engine becomes the active one and starts.")
                .subheading("Installing an engine")
                .paragraph("An engine comes from its disc, like every program. You put the disc in a drive linked to"
                        + " the Mainframe, then install the program as you install any other ([](jsc:programs)).")
                .paragraph("The install puts the engine in the Mainframe's list, at the version for the Mainframe's"
                        + " age. If no engine is chosen yet, the new one becomes the active engine and starts at once.")
                .paragraph("An install is refused when the engine is already installed, or when it was not made for"
                        + " the Mainframe's age.")
                .warning("Formatting the disk of the Mainframe removes every engine installed on it.")
                .paragraph("Removing the engine that runs leaves the network with no engine, as a stop does.")
                .subheading("The iqlengine command")
                .paragraph("The Midsoft IQL Server also has a command of its own, typed at a prompt: iqlengine. It"
                        + " takes one of four verbs.")
                .table("The iqlengine verbs", "Verb", "What it does")
                .row(GuideCell.code("install"), GuideCell.words("Puts the Midsoft IQL Server on the Mainframe."))
                .row(GuideCell.code("start"), GuideCell.words("Starts the Midsoft IQL Server, and switches the"
                        + " network to it at once if another engine is active."))
                .row(GuideCell.code("stop"), GuideCell.words("Stops the Midsoft IQL Server, but only while it is the"
                        + " active engine and running."))
                .row(GuideCell.code("status"), GuideCell.words("Shows whether the Midsoft IQL Server is not installed,"
                        + " running or stopped."))
                .paragraph("On a network that runs NextgreIQL, iqlengine stop answers that the Midsoft IQL Server is"
                        + " already stopped, because the Midsoft is not the active engine.")
                .ifSomethingGoesWrong("You press Replace, and nothing happens.",
                        "Replace is greyed out while a replacement runs, when the chosen engine already runs, or when"
                                + " the Mainframe is off. Read the state word first.",
                        "The Replace dialog says no other engine is installed.",
                        "Install another engine from its disc in a drive linked to the Mainframe.",
                        "The Services tab says no engine is installed.",
                        "Put the engine's disc in a drive linked to the Mainframe, then install it.",
                        "A replacement takes longer than you expected.",
                        "The time grows with the item types the network stores, up to 2 minutes.")
                .seeAlso("jsc:engine_problems", "jsc:midsoft_iql_server")
                .register();
    }

    private static void midsoftIqlServer(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("midsoft_iql_server", section).titled("Midsoft IQL Server").icon(ComputingModule.MAINFRAME)
                .paragraph("The Midsoft IQL Server is the engine every Mainframe starts with. It speaks IQL, the"
                        + " language of the network, and it is the only one of the three engines that keeps views and"
                        + " procedures. You save a query under a name as a view, or as a procedure that you run by"
                        + " name with EXEC ([](jsc:iql_views), [](jsc:iql_procedures)).")
                .table("The Midsoft IQL Server")
                .property("Dialect", "IQL")
                .property("Extras", "procedures and views")
                .amount("Memory", 24, "MB")
                .property("Ages", "Vintage to Advanced")
                .paragraph("The version depends on the age of the Mainframe it is installed on. The Services tab shows"
                        + " the version of the installed engine.")
                .table("Versions by the age of the Mainframe", "Age of the Mainframe", "Midsoft IQL Server version")
                .row(GuideCell.words("Vintage"), GuideCell.words("4.2"))
                .row(GuideCell.words("Legacy"), GuideCell.words("2000"))
                .row(GuideCell.words("Transition"), GuideCell.words("2008"))
                .row(GuideCell.words("Standard"), GuideCell.words("2012"))
                .row(GuideCell.words("Advanced"), GuideCell.words("2022"))
                .paragraph("Versions compare by their numbers, so version 4.2 is older than version 2000. Your"
                        + " Mainframe can only"
                        + " run the version made for its own age.")
                .paragraph("On a Legacy or newer Mainframe, the IQL Server Management Studio (ISMS) works with the"
                        + " Midsoft ([](jsc:studio_overview)). The ISMS needs version 2000 or newer. A Vintage"
                        + " Mainframe has no studio, so you work with the Midsoft from the prompt.")
                .paragraph("The Midsoft has no EXPLAIN, KEEP or WATCH. Those words belong to NextgreIQL and Prophet"
                        + " YourIQL, and the Midsoft refuses them with a syntax message.")
                .paragraph("The Midsoft does not keep jobs either. A job needs the Automation Engine installed on the"
                        + " Mainframe ([](jsc:automation_engine)).")
                .ifSomethingGoesWrong("You type CREATE JOB, and the network says jobs are kept by the Automation"
                        + " Engine.",
                        "Install the Automation Engine on the Mainframe first.",
                        "You type EXPLAIN, KEEP or WATCH, and the answer starts with \"syntax:\".",
                        "Those words belong to NextgreIQL or Prophet YourIQL. Use the engine that offers them.")
                .seeAlso("jsc:iql_procedures", "jsc:engine_services")
                .register();
    }

    private static void nextgreIql(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("nextgre_iql", section).titled("NextgreIQL and EXPLAIN").icon(ComputingModule.MAINFRAME)
                .paragraph("NextgreIQL is the engine that shows you how it plans. Before any work is done, you can ask"
                        + " for the plan of a CRAFT or a SELECT, and read which steps it would take and where each"
                        + " step runs.")
                .define("Query plan", "The steps the engine chose to answer a statement, with where each step runs"
                        + " and what it costs in ticks.")
                .paragraph("A craft is made in steps, and each step runs at a bench or on a machine. A bench is a"
                        + " Crafting Computer, where hand-made recipes run. A machine is a processing machine, and it"
                        + " runs its own recipes.")
                .define("Bench", "A Crafting Computer, where hand-made crafting recipes run.")
                .define("Machine", "A processing machine, which runs its own recipes.")
                .paragraph("You can also change a plan with hints ([](jsc:nextgre_hints)), which are words written"
                        + " after a CRAFT.")
                .define("Hint", "A setting written after a CRAFT that changes the plan it makes, such as"
                        + " PREFER MACHINE.")
                .table("NextgreIQL versions by age", "Age of the Mainframe", "NextgreIQL version")
                .row(GuideCell.words("Legacy"), GuideCell.words("7.0"))
                .row(GuideCell.words("Transition"), GuideCell.words("8.3"))
                .row(GuideCell.words("Standard"), GuideCell.words("9.0"))
                .row(GuideCell.words("Advanced"), GuideCell.words("16"))
                .subheading("Showing a plan with EXPLAIN")
                .paragraph("EXPLAIN shows the plan without starting it. Write EXPLAIN in front of a CRAFT or a SELECT,"
                        + " and the engine answers with the plan instead of doing the work. Only NextgreIQL offers"
                        + " EXPLAIN; the Midsoft refuses the word with a syntax message.")
                .steps("Type EXPLAIN CRAFT, the quantity and the item name.",
                        "Press [[Enter]].")
                .listingIn("iql", "explain_craft.iql", "EXPLAIN CRAFT 64 iron_ingot")
                .paragraph("The answer is a table with one row for each step of the plan. Its columns are these.")
                .table("The columns of an explained plan", "Column", "What it shows")
                .row(GuideCell.words("step"), GuideCell.words("The step's label and its quantity, as the steps table"
                        + " below shows."))
                .row(GuideCell.words("where"),
                        GuideCell.words("Whether a bench or a machine runs the step, and which one."))
                .row(GuideCell.words("estimate"),
                        GuideCell.words("The cost in ticks, worked out before anything runs."))
                .row(GuideCell.words("actual"), GuideCell.words("Blank until the plan runs. Then it shows the time the"
                        + " step took."))
                .row(GuideCell.words("hints"), GuideCell.words("The hints that shaped the step."))
                .table("The steps of a plan", "Step label", "What it means")
                .row(GuideCell.code("Craft <item> x<n>"), GuideCell.words("Makes the item, in the quantity shown."))
                .row(GuideCell.code("Process <item> x<n>"), GuideCell.words("Processes the item in a machine."))
                .row(GuideCell.code("Pull <item> x<n>"), GuideCell.words("Takes items from one storage server. There is"
                        + " one row for each server."))
                .row(GuideCell.code("Index Seek <item> x<n>"),
                        GuideCell.words("One read for the whole quantity, from the servers named after from."))
                .row(GuideCell.code("Missing <item> x<n>"),
                        GuideCell.words("The network holds none of it and makes none of it."))
                .paragraph("Steps that run side by side are grouped. The group says how many steps it has, and how many"
                        + " of them run at once.")
                .subheading("Running a plan with EXPLAIN ANALYZE")
                .paragraph("EXPLAIN ANALYZE CRAFT does the craft as well. The engine starts it, and the plan fills in"
                        + " the actual time of each step as the step runs.")
                .paragraph("The answer names the plan the engine chose, how many plans it weighed, and the first eight"
                        + " characters of the Operation. The Nextgre Planner Studio shows the times as they come"
                        + " ([](jsc:nextgre_planner_studio)).")
                .subheading("Where the items come from")
                .paragraph("EXPLAIN SELECT shows where the items would come from, without moving anything. The answer"
                        + " gives the quantity, the item, how many servers would hand it over, and the cost in ticks.")
                .subheading("ANALYZE on its own")
                .paragraph("A plain ANALYZE, with no statement after it, does the maintenance of the network's index"
                        + " ([](jsc:operations_catalogue)). NextgreIQL then gathers statistics for the Studio, and the"
                        + " answer gives the item types, servers and recipes it measured.")
                .paragraph("The Studio shows these statistics: item types indexed, servers, recipes timed, crafts"
                        + " measured, and the time of the last ANALYZE.")
                .subheading("How the engine chooses a plan")
                .paragraph("The engine does not guess a plan. It gives every candidate plan a cost in ticks, one for"
                        + " each way it found to make the item, and it chooses the cheapest.")
                .paragraph("The Studio's Rules tab lists the rules that shape the weighing: Weigh machines against"
                        + " benches, Weigh every recipe of the result, Use measured times, Count steps that run at"
                        + " once, and Weigh the fastest servers. You can switch these rules in the Studio.")
                .paragraph("Every plan is kept on the Mainframe, and the Studio's History tab lists them. Other mods"
                        + " can add rules, hints and statistics to NextgreIQL.")
                .ifSomethingGoesWrong("The answer is \"EXPLAIN shows the plans of CRAFT and SELECT\".",
                        "Put EXPLAIN only before a CRAFT or a SELECT.",
                        "The answer is \"EXPLAIN needs a statement to explain\".",
                        "Write the CRAFT or the SELECT after EXPLAIN.",
                        "The answer is \"no item is called\" and the name you typed.",
                        "Check the spelling, and use the short name, such as iron_ingot.",
                        "The answer says that nothing on the network knows how to make the item.",
                        "No recipe makes it. Choose another item.",
                        "The answer says every plan for the item was set aside.",
                        "The planner's rules rejected every plan. Look at the Rules tab of the Nextgre Planner Studio.",
                        "The answer says the network could not start the craft.",
                        "Check the network's storage and machines.")
                .seeAlso("jsc:nextgre_hints", "jsc:nextgre_planner_studio")
                .register();
    }

    private static void nextgreHints(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("nextgre_hints", section).titled("Hints: changing NextgreIQL's plan")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("A hint changes how NextgreIQL plans a CRAFT. When you add one, you choose where the raw"
                        + " materials come from, how many steps run at once, and whether a step is made on a machine"
                        + " or at a bench.")
                .paragraph("Write the hints after the CRAFT, one after another. The engine reads them at the end of the"
                        + " statement.")
                .define("Source", "A storage server that holds raw materials for a craft.")
                .table("The hints", "Hint", "Takes", "What it does")
                .row(GuideCell.code("PREFER SOURCE 'server'"), GuideCell.words("The name of a server"),
                        GuideCell.words("Takes the raw materials from the named server first."))
                .row(GuideCell.code("AVOID SOURCE 'server'"), GuideCell.words("The name of a server"),
                        GuideCell.words("Takes nothing from the named server while another server has them."))
                .row(GuideCell.code("MAX PARALLEL n"), GuideCell.words("A whole number"),
                        GuideCell.words("Runs at most that many steps of the craft at once."))
                .row(GuideCell.code("PREFER MACHINE"), GuideCell.words("Nothing"),
                        GuideCell.words("Makes on a machine whatever a machine and a bench both make."))
                .row(GuideCell.code("PREFER BENCH"), GuideCell.words("Nothing"),
                        GuideCell.words("Makes at a bench whatever a machine and a bench both make."))
                .paragraph("A craft with two hints looks like this. The server named Storage B must be on the network.")
                .listingIn("iql", "hints_example.iql",
                        "CRAFT 64 iron_ingot PREFER SOURCE 'Storage B'" + " MAX PARALLEL 2")
                .paragraph("Capitals do not matter.")
                .paragraph("MAX PARALLEL takes a whole number above zero. Hints go after a CRAFT only, because"
                        + " the other statements refuse them.")
                .paragraph("Other mods can add hints of their own.")
                .ifSomethingGoesWrong("The answer is \"no server on the network is called\" and the name you typed.",
                        "Write the server's name as the network shows it.",
                        "The answer is \"MAX PARALLEL takes a whole number above nought\".",
                        "Write a whole number of 1 or more after MAX PARALLEL.",
                        "The answer is \"hints change the plan of a CRAFT\".",
                        "Remove the hint, or make the statement a CRAFT.")
                .seeAlso("jsc:nextgre_iql", "jsc:engine_problems")
                .register();
    }

    private static void prophetYourIql(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("prophet_youriql", section).titled("Prophet YourIQL: keeping a level")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("Prophet YourIQL is the engine that keeps a level for you. Instead of asking for a craft"
                        + " each time, you write KEEP steel_ingot >= 10000 once. The engine then works out what it must"
                        + " make to keep that many steel ingots on the network.")
                .define("State", "A level the engine is told to keep, written as one KEEP statement.")
                .define("Band", "The range between a lower and an upper level, which a state keeps.")
                .define("Watch", "A statement that waits for a condition, then runs one action when the condition"
                        + " comes true.")
                .table("Prophet YourIQL versions by age", "Age of the Mainframe", "Prophet YourIQL version")
                .row(GuideCell.words("Legacy"), GuideCell.words("3.23"))
                .row(GuideCell.words("Transition"), GuideCell.words("5.0"))
                .row(GuideCell.words("Standard"), GuideCell.words("5.6"))
                .row(GuideCell.words("Advanced"), GuideCell.words("8.0"))
                .paragraph("Its own tool is the Prophet Reactive Console ([](jsc:prophet_console)), which needs"
                        + " YourIQL 3.23 or newer.")
                .subheading("The statements")
                .table("The statements of Prophet YourIQL", "Statement", "What it does")
                .row(GuideCell.code("KEEP item >= n"), GuideCell.words("Keeps at least n of the item."))
                .row(GuideCell.code("KEEP item BETWEEN a AND b"), GuideCell.words("Keeps the item within a band, from a"
                        + " to b."))
                .row(GuideCell.code("WATCH"), GuideCell.words("Runs an action once when a condition comes true."))
                .row(GuideCell.code("FORGET item"), GuideCell.words("Stops keeping the item."))
                .row(GuideCell.code("FORGET WATCH n"), GuideCell.words("Removes the watch with that number."))
                .row(GuideCell.code("SHOW STATES"),
                        GuideCell.words("Lists the kept states and the watches in a table."))
                .paragraph("Any other statement goes to the core language unchanged.")
                .subheading("Keeping a level")
                .steps("Type KEEP, the item's short name, >= and a number.",
                        "Read the answer, which names the item and its level.")
                .listingIn("iql", "keep_steel.iql", "KEEP steel_ingot >= 10000")
                .paragraph("KEEP with >= takes exactly four words, and >= must stand as a word of its own, with"
                        + " spaces around it. KEEP with BETWEEN takes exactly six words, and its upper number must"
                        + " not be below its lower number.")
                .paragraph("Numbers are whole numbers written in digits, with no minus sign. The answer says that the"
                        + " item is \"kept at\" a level \"or more\", or \"kept between\" two levels.")
                .paragraph("Keeping the same item again replaces its band.")
                .subheading("The band and Over")
                .paragraph("KEEP BETWEEN makes the engine craft when the level falls below its band. The upper number"
                        + " only shows where the level"
                        + " stops being right. Nothing is thrown away to bring a level down, so going over the band is"
                        + " never corrected by removing items.")
                .subheading("How the engine keeps a level")
                .paragraph("Each time it looks, the engine counts what the network holds and what is already on its"
                        + " way. Work on its way is an Operation that has started and not yet finished.")
                .paragraph("When the level is below its band, the engine asks the network to craft what is missing. It"
                        + " asks for at most one batch per craft, and the batch size is a setting. The request goes"
                        + " through the network's crafting door, under the name Prophet YourIQL"
                        + " ([](jsc:how_autocrafting_works)).")
                .paragraph("If nothing on the network can make the item, the state shows Cannot hold. The engine tries"
                        + " again every 600 ticks, which is 30 seconds.")
                .subheading("The settings")
                .paragraph("The Prophet Reactive Console sets three things for this engine.")
                .table("The settings of Prophet YourIQL", "Setting", "Choices", "Default")
                .row(GuideCell.words("How often it looks"), GuideCell.words("Every 1, 2, 5 or 10 seconds"),
                        GuideCell.words("1 second"))
                .row(GuideCell.words("Largest batch per craft"), GuideCell.words("64, 256, 1,024 or 4,096 items"),
                        GuideCell.words("1,024 items"))
                .row(GuideCell.words("Reacting"), GuideCell.words("On or off"), GuideCell.words("On"))
                .paragraph("A value that is not on the list falls back to its default. The engine reacts only while it"
                        + " runs, so a stopped or replaced engine stops reacting.")
                .paragraph("The states and the watches are saved with the Mainframe. The engine keeps 48 level samples"
                        + " for each state's graph, the 32 latest reactions, and up to 4 Operations for each state in"
                        + " the console.")
                .ifSomethingGoesWrong("The answer is \"syntax: KEEP item >= n, or KEEP item BETWEEN a AND b\".",
                        "Write KEEP with >= and a number, or with BETWEEN and its two numbers.",
                        "The answer is \"no item is called\" and the name you typed.",
                        "Check the spelling of the item's short name.",
                        "A state shows Cannot hold.",
                        "Nothing on the network can make the item. Look at the state in SHOW STATES, and check that the"
                                + " network can make it.",
                        "A state shows Over.",
                        "Nothing is wrong. The engine does not remove items to bring a level down.")
                .seeAlso("jsc:prophet_watch", "jsc:prophet_console")
                .register();
    }

    private static void prophetWatch(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("prophet_watch", section).titled("Prophet YourIQL: watches, forgetting and SHOW STATES")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("A watch runs an action when a condition comes true. You write the condition as an item, a"
                        + " comparison and a number. After DO you write the action, as you would write any statement.")
                .steps("Type WATCH, the item, the comparison and the number.", "Type DO, then the action.")
                .listingIn("iql", "watch_redstone.iql", "WATCH redstone < 500 DO CRAFT redstone TO 1000")
                .table("The comparisons", "Comparison", "Means")
                .row(GuideCell.code("<"), GuideCell.words("below"))
                .row(GuideCell.code("<="), GuideCell.words("at most"))
                .row(GuideCell.code(">"), GuideCell.words("above"))
                .row(GuideCell.code(">="), GuideCell.words("at least"))
                .paragraph("The first watch you set is number 1, and the next ones count up from it. The answer says"
                        + " watch 1 set, and then writes the watch back in full, so you can check it.")
                .paragraph("A watch fires once, when its condition comes true. It is armed again only after the"
                        + " condition has stopped being true.")
                .subheading("What a watch does")
                .table("The actions of a watch", "Action", "What happens")
                .row(GuideCell.code("CRAFT item TO n"), GuideCell.words("Makes up to n of the item, counting what the"
                        + " network holds, batch by batch."))
                .row(GuideCell.words("Any other statement"), GuideCell.words("Runs as a statement, and its answer is"
                        + " shown, up to 64 rows."))
                .subheading("Forgetting")
                .table("Forgetting an item or a watch", "Statement", "What happens")
                .row(GuideCell.code("FORGET item"), GuideCell.words("The item is no longer kept."))
                .row(GuideCell.code("FORGET WATCH n"), GuideCell.words("The watch with that number is let go."))
                .paragraph("FORGET takes the name you wrote for the item. Forgetting an item also clears its record of"
                        + " recent Operations.")
                .subheading("SHOW STATES")
                .paragraph("SHOW STATES lists what the engine keeps and watches. Type it as two words, in any capitals."
                        + " The answer counts the states and the watches, and then shows a table.")
                .table("The table of SHOW STATES", "Column", "What it shows")
                .row(GuideCell.words("state"), GuideCell.words("The state as you wrote it, or the WATCH statement for"
                        + " a watch."))
                .row(GuideCell.words("status"), GuideCell.words("The status word from the table below."))
                .row(GuideCell.words("held"), GuideCell.words("What the network holds of the item."))
                .row(GuideCell.words("coming"), GuideCell.words("The Operations on their way for the item."))
                .row(GuideCell.words("last"), GuideCell.words("The last thing the engine did for it; left empty on a"
                        + " watch row."))
                .subheading("The status words")
                .table("The status words", "Status", "What it means")
                .row(GuideCell.words("New"), GuideCell.words("Not looked at yet."))
                .row(GuideCell.words("Holding"), GuideCell.words("In its band, with nothing on its way."))
                .row(GuideCell.words("Working"), GuideCell.words("Work is on its way to bring it into its band or"
                        + " keep it there."))
                .row(GuideCell.words("Cannot hold"), GuideCell.words("Below its band, and nothing could be set going."))
                .row(GuideCell.words("Over"),
                        GuideCell.words("Above its band; nothing is thrown away to bring it down."))
                .row(GuideCell.words("Armed"), GuideCell.words("A watch waiting for its condition."))
                .row(GuideCell.words("Fired"), GuideCell.words("A watch whose condition came true."))
                .paragraph("Holding is the good state. Cannot hold and Over need your attention. The others wait,"
                        + " or are in progress.")
                .paragraph("The engine does not react to watches while it is stopped, so no watch fires then.")
                .ifSomethingGoesWrong("The answer is \"syntax: WATCH item < n DO statement (the comparison one of <,"
                        + " <=, >, >=)\".",
                        "Write WATCH, the item, one comparison, a number, DO, and the action.",
                        "The answer says the watch fired, and was refused, with a reason after the colon.",
                        "Read the reason, fix the statement, let the watch go with FORGET WATCH, and set it again.",
                        "The answer says the watch fired with nothing to make.",
                        "The network already holds the target, so nothing is wrong.",
                        "FORGET says the item is not kept.",
                        "Check the item's name in SHOW STATES.",
                        "FORGET WATCH says no watch is numbered that number.",
                        "Check the number in SHOW STATES.")
                .seeAlso("jsc:prophet_youriql", "jsc:prophet_console", "jsc:engine_problems")
                .register();
    }

    private static void engineProblems(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("engine_problems", section).titled("When an engine refuses").icon(ComputingModule.MAINFRAME)
                .paragraph("An engine answers a request it cannot do with a short message, and the Services tab shows"
                        + " its own messages. Find your message in the tables below, check when you see it, and"
                        + " follow what to do.")
                .table("Messages of the engines and the Services tab", "Message", "When you see it", "What to do")
                .row(GuideCell.code("Network Operations Service unavailable"),
                        GuideCell.words("A craft, query or update is asked while no engine runs."),
                        GuideCell.words("Start an engine in the Services tab, or type iqlengine start."))
                .row(GuideCell.code("No Network Operations Engine is running"),
                        GuideCell.words("The Services tab, when no engine runs."),
                        GuideCell.words("Press Start... and choose an engine."))
                .row(GuideCell.code("No engine is installed on this Mainframe. Install one from its disc to plan the"
                        + " network's work."),
                        GuideCell.words("The Services tab, when nothing is installed."),
                        GuideCell.words("Install an engine from its disc in a drive linked to the Mainframe."))
                .row(GuideCell.code("No compatible <engine name> was found on this network."),
                        GuideCell.words("A program needs an engine that the network does not run."),
                        GuideCell.words("Install that engine if it is missing, then start it so the network runs it."))
                .row(GuideCell.code("<program> needs an engine that offers <capability>."),
                        GuideCell.words("A program needs a capability that the running engine does not offer."),
                        GuideCell.words("Run an engine that offers it. The capability names are EXPLAIN and"
                                + " PLANNER_HINTS, among others."))
                .row(GuideCell.code("No other engine is installed on this Mainframe. Install one from its disc to"
                        + " replace this one."),
                        GuideCell.words("The Replace dialog, when only one engine is installed."),
                        GuideCell.words("Install another engine from its disc."))
                .row(GuideCell.code("the Midsoft IQL Server is already installed"),
                        GuideCell.words("The iqlengine command, when its verb would change nothing, such as"
                                + " starting an engine that already runs."),
                        GuideCell.words("Read the result of iqlengine status first."))
                .row(GuideCell.code("the network has no running Mainframe to host the Midsoft IQL Server"),
                        GuideCell.words("iqlengine, when the network has no Mainframe."),
                        GuideCell.words("Build a Mainframe on the network and turn it on."))
                .row(GuideCell.code("iqlengine needs its install disc in a linked drive"),
                        GuideCell.words("An install is asked, and no matching disc is in a linked drive."),
                        GuideCell.words("Put the disc in a drive linked to the Mainframe."))
                .paragraph("Some refusals show no message at all. A press on a Services button that the server does not"
                        + " accept does nothing, and the tab keeps showing the true state. Check the state the tab"
                        + " shows before you press again.")
                .table("Messages of NextgreIQL and Prophet YourIQL", "Message", "When you see it", "What to do")
                .row(GuideCell.code("EXPLAIN needs a statement to explain"),
                        GuideCell.words("EXPLAIN has nothing after it."),
                        GuideCell.words("Write the CRAFT or the SELECT after EXPLAIN."))
                .row(GuideCell.code("EXPLAIN shows the plans of CRAFT and SELECT"),
                        GuideCell.words("EXPLAIN comes before another statement."),
                        GuideCell.words("Put EXPLAIN only before a CRAFT or a SELECT."))
                .row(GuideCell.code("hints change the plan of a CRAFT"),
                        GuideCell.words("A hint follows another statement."),
                        GuideCell.words("Remove the hint, or make the statement a CRAFT."))
                .row(GuideCell.code("MAX PARALLEL takes a whole number above nought"),
                        GuideCell.words("MAX PARALLEL has no number, or 0."),
                        GuideCell.words("Write a whole number of 1 or more."))
                .row(GuideCell.code("no server on the network is called <name>"),
                        GuideCell.words("A PREFER or AVOID SOURCE names no server on the network."),
                        GuideCell.words("Write the server's name as the network shows it."))
                .row(GuideCell.code("no item is called <name>"),
                        GuideCell.words("An item name is not known."),
                        GuideCell.words("Check the spelling, such as iron_ingot."))
                .row(GuideCell.code("the network could not start the craft of <item>"),
                        GuideCell.words("The planned craft cannot start."),
                        GuideCell.words("Check the network's storage and machines."))
                .row(GuideCell.code("nothing on the network knows how to make <item>"),
                        GuideCell.words("No recipe makes the item."),
                        GuideCell.words("Choose an item that some recipe makes."))
                .row(GuideCell.code("every plan for <item> was set aside"),
                        GuideCell.words("The planner's rules rejected every plan."),
                        GuideCell.words("Look at the Rules tab of the Nextgre Planner Studio."))
                .row(GuideCell.code("the network's engine keeps no views, procedures or jobs"),
                        GuideCell.words("CREATE, DROP or EXEC on NextgreIQL or Prophet YourIQL."),
                        GuideCell.words("Use the Midsoft IQL Server for views and procedures."))
                .row(GuideCell.code("jobs are kept by the Automation Engine; install it on the Mainframe first"),
                        GuideCell.words("CREATE JOB, when the Automation Engine is not installed."),
                        GuideCell.words("Install the Automation Engine on the Mainframe."))
                .row(GuideCell.code("syntax: <the parser's message>"),
                        GuideCell.words("A statement cannot be read; the message names the word it stopped at."),
                        GuideCell.words("Fix the word the message names."))
                .row(GuideCell.code("syntax: KEEP item >= n, or KEEP item BETWEEN a AND b"),
                        GuideCell.words("A KEEP is malformed."),
                        GuideCell.words("See Prophet YourIQL: keeping a level."))
                .row(GuideCell.code("syntax: WATCH item < n DO statement (the comparison one of <, <=, >, >=)"),
                        GuideCell.words("A WATCH is malformed."),
                        GuideCell.words("See Prophet YourIQL: watches, forgetting and SHOW STATES."))
                .row(GuideCell.code("syntax: FORGET item, or FORGET WATCH n"),
                        GuideCell.words("A FORGET is malformed."),
                        GuideCell.words("See Prophet YourIQL: watches, forgetting and SHOW STATES."))
                .row(GuideCell.code("<name> is not kept, or no watch is numbered <n>"),
                        GuideCell.words("FORGET names an item or a watch that is not there."),
                        GuideCell.words("Check the names and numbers with SHOW STATES."))
                .seeAlso("jsc:engine_services", "jsc:nextgre_hints", "jsc:prophet_youriql")
                .register();
    }
}
