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
 * The Operations pages of the Guide to Operations: what an Operation is, the eight states it settles in, the queues it
 * runs in, its priority, cancelling it and the log, then the IQL statements and the engines that ask the network for
 * work. Each entry explains the idea from zero, then the rule the mod applies, and ends with what can go wrong.
 */
final class OperationsPages {

    private OperationsPages() {
    }

    /** Declares the section and the entries of the Operations pages, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("operations").titled("Operations")
                .icon(ComputingModule.MAINFRAME).in(chapter).register();
        whatOperationsAre(guide, section);
        operationsCatalogue(guide, section);
        operationStates(guide, section);
        statesAndQueues(guide, section);
        priorities(guide, section);
        cancellingAndTheLog(guide, section);
        iql(guide, section);
        engines(guide, section);
    }

    private static void whatOperationsAre(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("what_operations_are", section).titled("What an Operation is")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("In many storage mods, storage works like a service that is always there. You put a"
                        + " disk in a drive, and it serves you whatever is on it. That works, but it makes the"
                        + " whole system feel like one huge chest that you put things in and take things out of.")
                .paragraph("J's Computers does not work like that. Here the network does not only serve you: its"
                        + " computers do work to get each thing done, and every piece of that work is an"
                        + " Operation.")
                .define("Operation", "One action the network carries out, queued and then run over game ticks.")
                .paragraph("So, what is an Operation? It is an action the network is carrying out. Its names come"
                        + " from SQL (Structured Query Language), the language databases speak. When you take items"
                        + " out of storage, you do not fetch them from a disk yourself. You send the network a"
                        + " SELECT, and the network works it.")
                .paragraph("The request goes into a queue. The Mainframe picks it up, finds where the items are,"
                        + " and moves them to you as fast as its hardware allows. That is why the Mainframe's"
                        + " hardware matters so much ([](jsc:mainframes)).")
                .define("queue", "A lane on the Mainframe that moves items for one Operation at a time. More queues"
                        + " mean more Operations at once.")
                .paragraph("Most Operations go through the same stages. An Operation waits in a queue, runs over game"
                        + " ticks (20 ticks make a second), and then settles in one of eight states"
                        + " ([](jsc:operation_states)).")
                .paragraph("A timed Operation does not finish on the spot. A SELECT first takes a moment to find the"
                        + " items, and then it streams them out over ticks. By default that moment is 10 ticks on a"
                        + " hard disk, 3 on a solid-state drive and 1 on a non-volatile memory express (NVMe)"
                        + " drive.")
                .define("index", "The network's own list of what each of its servers holds, kept up to date so that"
                        + " the network knows where each item is.")
                .paragraph("Some Operations settle at once, on the spot: the index upkeep (ANALYZE, REINDEX and"
                        + " VACUUM), LOCK, UNLOCK and DROP.")
                .paragraph("Operations need a Mainframe. Every computer on the network is joined by data cable to"
                        + " one Mainframe, which runs the work and keeps the list of what is running"
                        + " ([](jsc:what_a_network_is)).")
                .ifSomethingGoesWrong("The result says \"the network has no running Mainframe\".",
                        "The Mainframe is off, or it is not on this network. Switch it on, or join it to the"
                                + " network ([](jsc:first_network)).",
                        "The Mainframe has no operating system installed, so it refuses new Operations.",
                        "Install an operating system on it ([](jsc:installing)).")
                .seeAlso("jsc:operations_catalogue", "jsc:iql")
                .register();
    }

    private static void operationsCatalogue(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("operations_catalogue", section).titled("The catalogue of Operations")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("Here is every Operation the network knows, and what it does. Most of their names come"
                        + " from the commands of databases, so if you know a little SQL, you already know many of"
                        + " them.")
                .define("IQL", "Item Query Language: the network's own language, in which you type the work that you"
                        + " want done, such as SELECT 64 iron_ingot.")
                .define("statement", "One IQL instruction: an action, a question or a maintenance command.")
                .define("verb", "The word that starts an IQL instruction, such as SELECT or QUERY.")
                .define("personal-use card", "A workshop card in the computer that asks for an UPDATE. The card"
                        + " does the work for that computer.")
                .table("Every Operation", "Operation", "What it does", "An example")
                .row(GuideCell.code("SELECT"), GuideCell.words("Takes items out of storage and brings them to the"
                        + " computer that asked."), GuideCell.code("SELECT 64 iron_ingot"))
                .row(GuideCell.code("INSERT"), GuideCell.words("Puts items that the computer holds into the"
                        + " network's storage."), GuideCell.code("INSERT 64 cobblestone"))
                .row(GuideCell.code("MOVE"), GuideCell.words("Moves items between two servers, or between a"
                        + " server and a named bus."), GuideCell.code("MOVE 64 cobblestone FROM <server> TO"
                        + " <server>"))
                .row(GuideCell.code("DELETE"), GuideCell.words("Sends items out of the network, through the bus"
                        + " named after TO. Destroys nothing."), GuideCell.code("DELETE 64 cobblestone TO"
                        + " '<bus name>'"))
                .row(GuideCell.code("DROP"), GuideCell.words("Destroys items for good."),
                        GuideCell.code("DROP 64 cobblestone"))
                .row(GuideCell.code("CRAFT"),
                        GuideCell.words("Makes items the network crafts, in machines or in steps."),
                        GuideCell.code("CRAFT 64 torch"))
                .row(GuideCell.code("UPDATE"), GuideCell.words("Changes an item the network holds, with a"
                        + " personal-use card."), GuideCell.code("UPDATE 1 iron_pickaxe SET REPAIR"))
                .row(GuideCell.code("ANALYZE"), GuideCell.words("Keeps the index up to date. It looks only at the"
                        + " servers that changed, and drops the entries of servers that left."),
                        GuideCell.code("ANALYZE"))
                .row(GuideCell.code("REINDEX"), GuideCell.words("Rebuilds the whole index from every disk."),
                        GuideCell.code("REINDEX"))
                .row(GuideCell.code("VACUUM"),
                        GuideCell.words("Removes the index entries that hold nothing, or point at a server that is no"
                                + " longer on the network."),
                        GuideCell.code("VACUUM"))
                .row(GuideCell.code("LOCK"), GuideCell.words("Reserves items, so that other Operations wait for"
                        + " them."), GuideCell.code("LOCK 64 iron_ingot"))
                .row(GuideCell.code("UNLOCK"), GuideCell.words("Releases a reservation."),
                        GuideCell.code("UNLOCK iron_ingot"))
                .paragraph("Each name in the first column is an IQL verb ([](jsc:query_and_select)). QUERY and COUNT"
                        + " only read the network, so they are not Operations. The Learning IQL book explains each"
                        + " verb in its own entry: UPDATE in [](jsc:update_cards), and LOCK in"
                        + " [](jsc:lock_unlock).")
                .paragraph("The Network program sends most of them for you when you click, so you do not have to"
                        + " type them. In IQL you can say exactly what you want ([](jsc:iql)).")
                .paragraph("UPDATE works in three stages. The item is taken out of the network with a SELECT, the"
                        + " card works on it, and the result is put back with an INSERT. The card is a workshop card"
                        + " in the computer that asked ([](jsc:workshop)).")
                .table("The cards of UPDATE", "Card", "Its UPDATE actions")
                .row(GuideCell.words("Furnace Card"), GuideCell.code("SMELT"))
                .row(GuideCell.words("Enchanting Card"), GuideCell.code("ENCHANT"))
                .row(GuideCell.words("Anvil Card"), GuideCell.code("REPAIR, COMBINE, NAME"))
                .paragraph("Whoever asked pays the card's experience price, except for smelting. A name with no"
                        + " prefix, such as iron_ingot, means the Minecraft item of that name.")
                .warning("DROP destroys items for good, and nothing brings them back.")
                .ifSomethingGoesWrong("The result says \"the network was not holding the\", followed by an item.",
                        "The network does not hold that item. Store it first ([](jsc:storage_servers)), or check"
                                + " its name.",
                        "The result says the card could not do it: nothing to change, too expensive or too few"
                                + " levels.",
                        "Check the item. A card needs something to change, and the player who asked needs enough"
                                + " levels for the work.",
                        "The result says whoever asked for it was not here to pay.",
                        "The player who asked has to be there to pay the experience. Ask again with them present.")
                .seeAlso("jsc:operation_states", "jsc:iql")
                .register();
    }

    private static void operationStates(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("operation_states", section).titled("The eight states of an Operation")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("Every Operation is in one of eight states at any time. The Processes tab of the"
                        + " [Network Manager](jsc:network_manager) shows you each one, in the words of the table"
                        + " below.")
                .paragraph("Three states mean that the Operation is still going: queued, running and waiting."
                        + " The other five are final. The Operation is then done, and it is kept in the"
                        + " log ([](jsc:cancelling_and_the_log)).")
                .define("log", "The Mainframe's list of the last 32 Operations that settled, newest first.")
                .define("hold", "A reservation on items. Other Operations that need those items wait until the"
                        + " reservation ends.")
                .table("The eight states", "State", "The Network Manager says", "What it means")
                .row(GuideCell.code("PENDING"), GuideCell.words("queued"), GuideCell.words("Ready, and waiting for"
                        + " a free queue."))
                .row(GuideCell.code("PROCESSING"), GuideCell.words("running"), GuideCell.words("Holds a queue and"
                        + " moves items."))
                .row(GuideCell.code("WAITING"), GuideCell.words("waiting"), GuideCell.words("Blocked by another"
                        + " Operation's hold. It takes no queue while it waits."))
                .row(GuideCell.code("COMPLETED"), GuideCell.words("done"), GuideCell.words("Moved all that was"
                        + " asked."))
                .row(GuideCell.code("COMPLETED_PARTIAL"), GuideCell.words("partial"), GuideCell.words("Moved some of"
                        + " it, less than was asked."))
                .row(GuideCell.code("FAILED"), GuideCell.words("failed"), GuideCell.words("Moved nothing, and was"
                        + " not done."))
                .row(GuideCell.code("RESOURCE_LOCKED"), GuideCell.words("locked"), GuideCell.words("Gave up waiting"
                        + " for a hold, after the waiting time ran out."))
                .row(GuideCell.code("DISCARDED"), GuideCell.words("discarded"), GuideCell.words("Stopped before it"
                        + " finished, on purpose or by the machine."))
                .paragraph("The list colours each row by its state: going or done, held up, or stopped. A row can"
                        + " also show a line that gives the reason, such as the ones below.")
                .table("The reason lines", "The line", "What it means")
                .row(GuideCell.code("delivered 40 of 64"), GuideCell.words("It moved some of what was asked. The two"
                        + " numbers are what moved and what was asked."))
                .row(GuideCell.code("timed out waiting on a locked resource"), GuideCell.words("A hold on the items"
                        + " lasted too long."))
                .row(GuideCell.code("failed"), GuideCell.words("Nothing was moved, and the Operation was not"
                        + " done."))
                .row(GuideCell.code("the network was not holding enough"), GuideCell.words("A SELECT asked for"
                        + " more than the network holds."))
                .row(GuideCell.code("the network had no room left for the"), GuideCell.words("An INSERT found no"
                        + " room left in the network's storage."))
                .ifSomethingGoesWrong("An Operation stays queued.",
                        "All the queues are busy. A Mainframe with more graphics cards has more queues"
                                + " ([](jsc:expansion_cards)), and a faster processor finishes each one sooner"
                                + " ([](jsc:mainframes)).",
                        "It ends locked.",
                        "The items were held for longer than the waiting time, which is 60 seconds by default."
                                + " Wait, or UNLOCK the item ([](jsc:lock_unlock)).",
                        "It ends partial.",
                        "The network held less than you asked for. Store more of the item"
                                + " ([](jsc:storage_servers)), or ask for less.",
                        "It ends failed.",
                        "The network held none of the item, or a step was refused. Check the item's name, and that"
                                + " the item is stored.")
                .seeAlso("jsc:states_and_queues", "jsc:cancelling_and_the_log")
                .register();
    }

    private static void statesAndQueues(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("states_and_queues", section).titled("Queues and graphics cards")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("A queue is one lane that moves items for one Operation at a time. The Mainframe has one"
                        + " queue for its processor, and one more for each graphics card in it. A Mainframe with a"
                        + " processor and three graphics cards works four queues at the same time.")
                .define("engine", "The program on the Mainframe that reads IQL and plans the network's work.")
                .paragraph("Subframes on the same network lend their graphics card queues too, if they run the same"
                        + " engine. A Subframe that runs another engine lends none ([](jsc:subframes)).")
                .define("Items a tick (it/t)", "How many items a queue or the Mainframe moves in one game tick.")
                .paragraph("Each queue runs at its own speed, measured in items a tick (it/t). The processor's queue"
                        + " runs at the processor's capacity. A graphics card's queue runs at the lower of the"
                        + " processor's capacity and the card's own speed. A weak card, or a card in an older slot, is"
                        + " slower.")
                .table("The speed of each queue", "Queue", "Runs at")
                .row(GuideCell.words("The processor's queue"), GuideCell.words("The processor's capacity."))
                .row(GuideCell.words("Each graphics card's queue"), GuideCell.words("The lower of the processor's"
                        + " capacity and the card's speed."))
                .paragraph("The queues do not share their capacity among themselves. A second graphics card gives"
                        + " you a second lane. It does not make the first lane faster.")
                .paragraph("The pace of the whole Mainframe is the lower of two figures. One is its orchestration"
                        + " capacity: how many items a tick the Mainframe, with its Subframes, can move. The other is"
                        + " its random-access memory (RAM) buffer, the working memory of the Mainframe in items a tick"
                        + " ([](jsc:memory)).")
                .paragraph("The Network Manager shows the queues in use, as Parallel queues, and the Orchestration"
                        + " capacity, in items a tick. A machine step of an autocraft does not take one of these queues"
                        + " ([](jsc:how_autocrafting_works)).")
                .paragraph("You can hot-swap a graphics card without losing work. The number of queues changes in"
                        + " place, and the Operations keep running.")
                .ifSomethingGoesWrong("The network feels slow.",
                        "Look at the Mainframe first. Its processor and its graphics cards set how many Operations run"
                                + " at once, and how fast ([](jsc:mainframes)).")
                .seeAlso("jsc:operation_states", "jsc:priorities")
                .register();
    }

    private static void priorities(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("priorities", section).titled("Priorities").icon(ComputingModule.MAINFRAME)
                .paragraph("Every Operation has a priority, which decides which waiting Operation gets a free queue"
                        + " first. There are five levels, from the lowest to the highest. A new Operation starts at"
                        + " MEDIUM, unless you choose another level.")
                .define("priority", "The level, from LOW to HIGH, that decides which waiting Operation gets a free"
                        + " queue first.")
                .paragraph("When two Operations have the same level, the one that came first goes first.")
                .table("The five levels", "Level", "Keyword in IQL", "Short label")
                .row(GuideCell.code("LOW"), GuideCell.code("low"), GuideCell.words("LOW"))
                .row(GuideCell.code("MEDIUM_LOW"), GuideCell.code("medium_low"), GuideCell.words("MED-"))
                .row(GuideCell.code("MEDIUM"), GuideCell.code("medium"), GuideCell.words("MED"))
                .row(GuideCell.code("MEDIUM_HIGH"), GuideCell.code("medium_high"), GuideCell.words("MED+"))
                .row(GuideCell.code("HIGH"), GuideCell.code("high"), GuideCell.words("HIGH"))
                .paragraph("Keywords are not case-sensitive, and you can write medium-low or medium-high with a hyphen."
                        + " The keyword normal means MEDIUM. The short label is what dense lists show.")
                .define("aging", "The rise of a waiting Operation's level: one step for each period it waits for a"
                        + " queue.")
                .paragraph("An Operation that is ready but gets no free queue climbs one level for every 30 seconds"
                        + " it waits. By default, that is 600 ticks. It climbs no higher than HIGH, so low work is"
                        + " delayed, but it is never left behind for good. The mod's configuration sets this period,"
                        + " and a period of 0 switches aging off.")
                .paragraph("Aging does not change the level you chose. Each tick, the Mainframe works out the level"
                        + " it uses for that tick: the chosen level, plus what aging has added.")
                .subheading("Changing a priority")
                .steps("Open the Processes tab of the Network Manager, and click the row of the Operation.",
                        "Press > to raise its level one step, or < to lower it.")
                .paragraph("In IQL, end a statement with PRIORITY and a level, such as SELECT 64 iron_ingot PRIORITY"
                        + " HIGH. In the Network program, the popup of a request has a PRIORITY setting that raises or"
                        + " lowers the level one step at a time. A CRAFT takes its level from the statement or from"
                        + " its request ([](jsc:asking_for_a_craft)).")
                .ifSomethingGoesWrong("An Operation is WAITING, and raising its level changes nothing.",
                        "A waiting Operation waits for a hold on items, not for a queue. Wait until the hold ends, or"
                                + " UNLOCK the item ([](jsc:lock_unlock)).",
                        "The result says PRIORITY is not valid on a read.",
                        "Take PRIORITY off the QUERY. It sets the level of an Operation, and a QUERY only reads"
                                + " ([](jsc:clauses_priority_if_with)).",
                        "The result says unknown priority level, and lists the five names.",
                        "Write one of the five names from the table, such as HIGH.")
                .seeAlso("jsc:states_and_queues", "jsc:iql")
                .register();
    }

    private static void cancellingAndTheLog(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cancelling_and_the_log", section).titled("Cancelling and the log")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("You can stop an Operation while it runs or waits. The Processes tab of the Network Manager"
                        + " lists the Operations in flight. Click a row, and its popup shows the type, the item, the"
                        + " amount, the timing, the parts of its work and a Cancel button.")
                .steps("Open the Network Manager, and its Processes tab.",
                        "Click the row of the Operation that you want to stop.",
                        "Press Cancel in the popup.")
                .paragraph("Cancelling hands back what the Operation held. The Operation then settles as DISCARDED on"
                        + " the next tick, and the log records it.")
                .paragraph("The Command Prompt has the same control through its interac command. The two forms in"
                        + " the next table work on the Operations in flight.")
                .table("The Command Prompt commands")
                .fixed("Lists the Operations in flight, with their identifiers (IDs)", "interac ops")
                .fixed("Cancels one, by the start of its ID", "interac cancel <id>")
                .paragraph("The Mainframe keeps a log of the Operations that settled. It holds the last 32 Operations,"
                        + " newest first. The Log tab of the Network Manager shows it under the title \"Network log\".")
                .table("What a row of the log shows", "Part of the row", "What it tells you")
                .row(GuideCell.words("Type"), GuideCell.words("The kind of Operation, such as SELECT or MOVE."))
                .row(GuideCell.words("Item"), GuideCell.words("The item that it worked on."))
                .row(GuideCell.words("Amount"), GuideCell.words("How much moved, out of how much was asked."))
                .row(GuideCell.words("Status"), GuideCell.words("Its state, such as PENDING, COMPLETED or DISCARDED."))
                .row(GuideCell.words("Reason"), GuideCell.words("The line with the reason, when there is one."))
                .row(GuideCell.words("Waited and ran"), GuideCell.words("How many ticks it waited for a queue, and how"
                        + " many it ran."))
                .paragraph("The Stats tab shows the Operations that settled in the last hour. It says \"No Operations"
                        + " settled in the last hour.\" when none did.")
                .paragraph("When the Mainframe is switched off, loses its operating system, or meets a network"
                        + " conflict, everything in flight ends. Each Operation is settled as DISCARDED and logged,"
                        + " and what it held is given back. A network conflict happens when two Mainframes claim the"
                        + " same network ([](jsc:network_identity)).")
                .paragraph("A save keeps the Operations in flight, and they resume at the next start. A save older"
                        + " than 24 hours is discarded and logged instead, by default. The mod's configuration can"
                        + " change that time.")
                .ifSomethingGoesWrong("The Operation is still in the list after I cancelled it.",
                        "The Operation is settled as DISCARDED on the next tick, not at once. Wait one tick, and the"
                                + " list refreshes.",
                        "My Operation vanished after a reload.",
                        "The save was older than 24 hours, so the Mainframe discarded it and logged it. Look for it in"
                                + " the log.",
                        "The log says \"No Operations logged yet.\"",
                        "The log stays empty until an Operation settles. Wait until one finishes.")
                .seeAlso("jsc:operation_states", "jsc:network_identity")
                .register();
    }

    private static void iql(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("iql", section).titled("IQL, the network's language").icon(ComputingModule.MAINFRAME)
                .paragraph("Clicking in the Network program is fine for taking a stack of iron. But what if you want to"
                        + " ask which items the network holds more than 100 of, or to craft 64 torches from a script?"
                        + " That is what IQL is for: asking the network for anything, in words, at a command prompt,"
                        + " in a program or in a script.")
                .paragraph("IQL reads like the language of databases. A few lines show the idea:")
                .table("A few lines of IQL")
                .fixed("Take items", "SELECT 64 iron_ingot")
                .fixed("Craft", "CRAFT 64 torch")
                .fixed("Ask", "QUERY items WHERE qty > 100")
                .fixed("Sort and cut", "QUERY items ORDER BY name LIMIT 2")
                .fixed("Look at the disks", "QUERY disks")
                .paragraph("A statement starts with a verb. An action needs a quantity and an item, and it may add"
                        + " other clauses. A question starts with QUERY or SHOW. The quantity is a number, or ALL for"
                        + " everything the network holds of that item. Percentages such as 50% are not supported.")
                .table("The shape of an action")
                .fixed("Start", "VERB [quantity] item")
                .fixed("Then, in any order", "[FROM place] [TO place] [WHERE condition] [IF condition]"
                        + " [ORDER BY field] [LIMIT n] [PRIORITY level]")
                .subheading("Running a statement")
                .steps("Open the Command Prompt on a computer that is on the network.",
                        "Type iql, a space, and the statement, such as iql SELECT 64 Cobblestone.",
                        "Press [[Enter]].")
                .paragraph("The words operation, op and sql do the same as iql. Outside the network, the command"
                        + " refuses to run, and it says that the computer is not on a network.")
                .paragraph("The IQL Server Management Studio is the full console: you query the network, trace its"
                        + " work, look after its index and locks, and run its jobs from one place. It needs the Midsoft"
                        + " IQL Server, version 2000 or newer ([](jsc:studio_overview)).")
                .paragraph("A script is statements separated by semicolons. Two dashes start a comment that runs to"
                        + " the end of the line. A script can also have parameters, written like <name, type,"
                        + " default>, and the Studio asks you for their values when you run it.")
                .warning("DROP destroys items for good. DELETE with ALL sends out everything the network holds of an"
                        + " item. The Studio"
                        + " asks before it runs either one, unless you have turned that question off.")
                .paragraph("On an engine that keeps them, you can save a query as a view or a procedure. A job runs"
                        + " statements on a timer, or when a condition is met. A job needs the Automation Engine"
                        + " installed on the Mainframe ([](jsc:automation_engine)).")
                .paragraph("The book Learning IQL explains every verb, clause and error, each in its own entry"
                        + " ([](jsc:what_iql_is)).")
                .ifSomethingGoesWrong("The network refuses a line, with a message such as unknown verb.",
                        "Check the word that the message names. The Studio points at the place where IQL stopped"
                                + " reading ([](jsc:iql_errors)).",
                        "The result says DELETE exports out of the network and needs a TO destination.",
                        "Add TO, and the name of a bus ([](jsc:buses)).",
                        "The result says MOVE is internal and needs both FROM and TO.",
                        "Name the server to take from, and the server to put into ([](jsc:storage_servers)).",
                        "The result says nothing to select.",
                        "The network holds none of that item. Check its name, or store some first"
                                + " ([](jsc:storage_servers)).",
                        "A view, a procedure or a job is refused, and the result says the network's engine keeps"
                                + " none of them.",
                        "Use an engine that keeps them ([](jsc:engines)). A job also needs the Automation Engine"
                                + " ([](jsc:automation_engine)).",
                        "The result says the IF did not hold, so the action was not run.",
                        "Check the condition. The IF reads how much of the item the network holds.")
                .seeAlso("jsc:what_iql_is", "jsc:iql_errors")
                .register();
    }

    private static void engines(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("engines", section).titled("Engines").icon(ComputingModule.MAINFRAME)
                .paragraph("What reads IQL and plans the network's work is the engine. Every new Mainframe comes with"
                        + " the Midsoft IQL Server, installed and running. Other engines can be installed on the"
                        + " Mainframe too, and each one speaks its own version of IQL with its own extras.")
                .table("The engines", "Engine", "What it offers", "Versions by era")
                .row(GuideCell.words("Midsoft IQL Server"), GuideCell.words("IQL, with views and procedures."),
                        GuideCell.words("Vintage 4.2, Legacy 2000, Transition 2008, Standard 2012, Advanced 2022"))
                .row(GuideCell.words("NextgreIQL"), GuideCell.words("Shows the plan it made and how it ran, takes"
                        + " hints that change the plan, and takes extras from other mods."),
                        GuideCell.words("Legacy 7.0, Transition 8.3, Standard 9.0, Advanced 16"))
                .row(GuideCell.words("Prophet YourIQL"), GuideCell.words("You declare the state the network keeps;"
                        + " it works out the Operations to hold it, and answers subscriptions."),
                        GuideCell.words("Legacy 3.23, Transition 5.0, Standard 5.6, Advanced 8.0"))
                .paragraph("NextgreIQL and Prophet YourIQL come from the Legacy era on. Prophet works differently from"
                        + " the others: you say what state the network must keep, and it does the work to keep it."
                        + " The book Learning IQL explains each engine ([](jsc:iql_engines_overview)).")
                .paragraph("A program names the engine it needs. The IQL Server Management Studio needs the Midsoft IQL"
                        + " Server, version 2000 or newer. The Nextgre Planner Studio needs NextgreIQL, and the Prophet"
                        + " Reactive Console needs Prophet YourIQL, version 3.23 or newer.")
                .paragraph("Replacing an engine takes at least 2 seconds, and up to 2 minutes. A network with"
                        + " more kinds of item takes longer. While the engine is replaced, the work already running"
                        + " carries on, and new plans are refused.")
                .paragraph("Without a working engine, only the transfers still work, such as moving items with SELECT,"
                        + " INSERT, MOVE and DELETE. CRAFT, QUERY and UPDATE are refused.")
                .paragraph("When a program says it needs an engine, you install that engine on the Mainframe, or start"
                        + " it if it is stopped ([](jsc:engine_services)).")
                .ifSomethingGoesWrong("A program says that no compatible engine was found on this network.",
                        "The engine is missing or stopped on this Mainframe. Install it or start it"
                                + " ([](jsc:engine_services)).",
                        "The result says Network Operations Service unavailable.",
                        "An engine is being replaced. Wait until the replacement ends. The work already running"
                                + " carries on.",
                        "A program says it needs an engine that offers a feature the network does not have.",
                        "The engine on this Mainframe lacks that extra. Use an engine that has it"
                                + " ([](jsc:engine_problems)).")
                .seeAlso("jsc:iql", "jsc:automation_engine")
                .register();
    }
}
