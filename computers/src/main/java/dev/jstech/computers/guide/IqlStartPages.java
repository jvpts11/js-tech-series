/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * The getting-started pages of the Learning IQL book: what IQL and Operations are, the five places a statement is
 * typed, and the first queries a player runs step by step on a network. The pages assume nothing about the mod.
 */
final class IqlStartPages {

    private IqlStartPages() {
    }

    /** Declares the section and the entries of the getting-started pages, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("iql_start").titled("Getting started").in(chapter)
                .register();
        whatIqlIs(guide, section);
        whereToTypeIql(guide, section);
        firstQueries(guide, section);
    }

    private static void whatIqlIs(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("what_iql_is", section).titled("What IQL and Operations are")
                .paragraph("Your items are not kept in one chest. They sit on the disks of the network: a set of"
                        + " computers and devices joined by data cable to one Mainframe. To get an item out, you do"
                        + " not open a box. You ask the network for it, and the network fetches it for you.")
                .define("network", "Every computer and device joined by data cable to one Mainframe.")
                .define("Mainframe", "The one computer of a network that runs its work and keeps track of it.")
                .paragraph("You ask in IQL, the Item Query Language. IQL is the network's own language, and it looks"
                        + " like the languages of databases: a word for the action, then the item and how many."
                        + " Its words come from SQL (Structured Query Language), the language that databases speak.")
                .define("IQL", "The network's own query language: the words you type to ask it for items and to tell"
                        + " it what work to do.")
                .paragraph("Each line you type is a statement. A statement either asks a question, or it asks for"
                        + " work. QUERY asks a question, and SHOW means the same. A question gets its answer as rows,"
                        + " and it changes nothing.")
                .define("statement", "One complete IQL instruction, typed on its own line.")
                .define("query", "A question put to the network, which it answers with rows and without changing"
                        + " anything.")
                .define("row", "One line of an answer, such as one disk or one item, with its details.")
                .paragraph("When a statement asks for work, the network turns it into an Operation. An Operation is"
                        + " one action the network carries out. It waits its turn, the network works on it over game"
                        + " ticks, and then it ends in one of eight states. Its action words, such as SELECT and"
                        + " DELETE, are SQL-style words.")
                .define("Operation", "One action the network carries out, from the moment it is asked until it ends.")
                .define("tick", "One beat of the game. 20 ticks make a second.")
                .paragraph("Take SELECT 64 iron_ingot. It asks the network to bring 64 iron ingots to you. That is"
                        + " one Operation. The reply only says that the Operation is queued. The items reach you as"
                        + " the network works it.")
                .table("The parts of a statement", "Part", "What it is", "Example")
                .row(GuideCell.words("Action"), GuideCell.words("The word that says what to do, such as SELECT or"
                        + " QUERY"), GuideCell.code("SELECT"))
                .row(GuideCell.words("Quantity"), GuideCell.words("How many. You may leave it out."),
                        GuideCell.code("64"))
                .row(GuideCell.words("Item"), GuideCell.words("The item's name, as one word."),
                        GuideCell.code("iron_ingot"))
                .row(GuideCell.words("Extra words"), GuideCell.words("Words such as WHERE, which narrow the answer."),
                        GuideCell.code("WHERE qty > 100"))
                .paragraph("The quantity is optional. Leave it out with SELECT, and the network brings all it holds of"
                        + " that item. The word ALL means the same. A quantity is a whole number: 50% is refused.")
                .paragraph("Write an item's name as one word, with an underscore for each space. iron_ingot means"
                        + " minecraft:iron_ingot, the iron ingot of Minecraft. A name with a colon, such as mod:item,"
                        + " is taken as the full name.")
                .paragraph("Each Operation also has a priority. It is MEDIUM unless you say otherwise. Write PRIORITY"
                        + " and a level such as HIGH at the end of a statement to set it ([](jsc:priorities)).")
                .paragraph("An Operation ends in one of eight states, and the network tells you which one"
                        + " ([](jsc:operation_states)).")
                .ifSomethingGoesWrong("The reply says \"the network has no running Mainframe\".",
                        "The Mainframe is off, or it is not on this network. Turn it on, or join the computer to its"
                                + " network ([](jsc:first_network)).",
                        "The Operation stays PENDING.",
                        "The Mainframe is busy, off, or has no processor time left for it. Wait a little, or give the"
                                + " Mainframe more processors or graphics cards ([](jsc:mainframes)).",
                        "The Operation ends RESOURCE_LOCKED.",
                        "Another Operation, or a LOCK, holds an item it needs. Wait for the other Operation to finish,"
                                + " or read about locks ([](jsc:lock_unlock)).",
                        "The Operation ends COMPLETED_PARTIAL.",
                        "The network had less than you asked for. Ask for a smaller quantity. QUERY items shows how"
                                + " much of each item the network holds.")
                .seeAlso("jsc:operations_catalogue", "jsc:where_to_type_iql", "jsc:query_and_select")
                .register();
    }

    private static void whereToTypeIql(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("where_to_type_iql", section).titled("Where you type IQL")
                .paragraph("You can type IQL in five places. Each one needs the network to be ready first, and each"
                        + " one reads its statements a little differently. The list below names the five places, and"
                        + " the parts after it show how to use each.")
                .define("script", "A file of IQL statements that the network runs one after another.")
                .table("The five places", "Place", "How you reach it", "What it is for")
                .row(GuideCell.words("The iql command"), GuideCell.words("Type iql at the prompt, then a statement"),
                        GuideCell.words("One statement at a time"))
                .row(GuideCell.words("IQL Server Management Studio (ISMS)"),
                        GuideCell.words("Open the program"), GuideCell.words("Writing and running whole scripts"))
                .row(GuideCell.code("run"), GuideCell.words("Type run and a file name, on MC-NET"),
                        GuideCell.words("Running a file of statements"))
                .row(GuideCell.words("The Iql class of Sigma"),
                        GuideCell.words("Write a Sigma program that calls it"),
                        GuideCell.words("Programs that ask the network"))
                .row(GuideCell.words("Automation Manager"), GuideCell.words("Open the program and fill in its form"),
                        GuideCell.words("Work the network repeats for you"))
                .subheading("The iql command at the prompt")
                .paragraph("Type iql, a space, then one statement, and press [[Enter]]. The command also answers to"
                        + " the names operation, op and sql, so op SELECT 64 iron_ingot does the same. It needs a"
                        + " data network under the computer. A read on a computer with no network answers \"not on a"
                        + " network\".")
                .paragraph("A read answers with rows, at most 64 of them. A write answers with one line, such as"
                        + " \"SELECT queued: 64 Iron Ingot -> local storage\". The item is named as the game shows it,"
                        + " such as Iron Ingot, not by its internal name.")
                .define("local storage", "This computer's own storage, where the items you take out of the network"
                        + " go.")
                .subheading("IQL Server Management Studio (ISMS)")
                .paragraph("The IQL Server Management Studio (ISMS) is the program for writing scripts. A script is"
                        + " several statements in one file, with room to edit them. ISMS opens and saves files that"
                        + " end in .iql.")
                .paragraph("In ISMS, each statement ends with a semicolon. A comment starts with two dashes and runs to"
                        + " the end of the line. Press the keys below to run what you wrote.")
                .table("The keys of ISMS", "Key", "The command, as the menu writes it")
                .row(GuideCell.code("F5"), GuideCell.words("Execute"))
                .row(GuideCell.code("Ctrl+E"), GuideCell.words("Execute Selection"))
                .row(GuideCell.code("Alt+Break"), GuideCell.words("Cancel Executing Query"))
                .row(GuideCell.code("F1"), GuideCell.words("IQL Reference"))
                .paragraph("A script can ask for values first. Its parameters are written in angle brackets with a type"
                        + " and a default, and the dialog \"Specify Values for Template Parameters\" lists each one as"
                        + " name (type).")
                .subheading("run, for a file")
                .paragraph("The run command runs a file of IQL on a computer that runs MC-NET ([](jsc:mc_net)). Type"
                        + " run, a space and the file's name. The computer needs a disk, and run takes only files that"
                        + " end in .iql.")
                .paragraph("run is stricter than ISMS. It reads one statement a line, skips every line that starts with"
                        + " two dashes, and has no semicolons. It asks you for no values. It refuses the reads, QUERY"
                        + " and COUNT. It checks the whole file before the first line runs, so a bad line moves"
                        + " nothing.")
                .subheading("The Iql class in Sigma")
                .paragraph("Sigma, one of the programming languages of the computers, can ask the network too."
                        + " Its class Iql, in the namespace System.Network, has four members.")
                .table("The members of the Iql class", "Member", "What it gives back")
                .row(GuideCell.code("Run"), GuideCell.words("An IqlResult, with Ok, Message and Rows"))
                .row(GuideCell.code("Query"), GuideCell.words("The rows of a read"))
                .row(GuideCell.code("Exec"), GuideCell.words("An IqlResult. It also takes a list of strings."))
                .row(GuideCell.code("RunFile"), GuideCell.words("An IqlResult, from a file"))
                .paragraph("The exact way to call it is in [Iql](jsc:iql_library), in the Book of Sigma.")
                .subheading("Automation Manager")
                .paragraph("The Automation Manager ([](jsc:automation_manager)) does not take typed IQL. Its form makes"
                        + " work that repeats: Restock below, Batch Craft, Move, or IQL. For the IQL kind, you pick a"
                        + " .iql file from the Mainframe's system disk, and you set how often it runs in the field"
                        + " Every (30s).")
                .paragraph("Typing IQL needs the Midsoft IQL Server running. You start and stop it with the command"
                        + " iqlengine ([](jsc:engine_services)).")
                .ifSomethingGoesWrong("You type iql alone, and the reply shows a usage line with an example.",
                        "Type a statement after iql.",
                        "The reply says \"not on a network\".",
                        "The computer has no data network under it. Join it with a data cable"
                                + " ([](jsc:data_cables)).",
                        "run says \"only .iql files can be run\".",
                        "Rename the file so that it ends in .iql, or write the statements in a new .iql file.",
                        "run says \"QUERY/COUNT are not supported by 'run'\".",
                        "A run cannot read. Type the read at the prompt, with iql or operation.",
                        "run says \"syntax error in\", followed by a line.",
                        "The whole file is checked first, so no line ran. Fix the line that the message names.",
                        "ISMS says \"No compatible Midsoft IQL Server was found on this network.\"",
                        "Make sure the Mainframe is on, and that the Midsoft IQL Server is installed and running"
                                + " ([](jsc:engine_services)).",
                        "ISMS says the Midsoft IQL Server is not installed.",
                        "Install it from the Network Manager, in its Network Services, as the message says"
                                + " ([](jsc:network_manager)).",
                        "A statement answers \"Network Operations Service unavailable\".",
                        "The Midsoft IQL Server is stopped. Start it with iqlengine start.",
                        "The Automation Manager says \"No engine - install the Automation Engine on the Mainframe\".",
                        "Install the Automation Engine on the Mainframe, then try again.",
                        "The reply says \"jobs are kept by the Automation Engine\".",
                        "Install the Automation Engine on the Mainframe first.")
                .seeAlso("jsc:isms_studio", "jsc:automation_manager", "jsc:iql_engines_overview")
                .register();
    }

    private static void firstQueries(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("first_queries", section).titled("Your first queries, step by step")
                .paragraph("This entry takes you from a network with nothing typed yet to your first answers and your"
                        + " first work. Each step shows what you type and what the network says back. If your reply"
                        + " differs, read the problems at the end.")
                .subheading("Before you start")
                .steps("Join the computer to the network with a data cable ([](jsc:first_network)).",
                        "Turn on the Mainframe ([](jsc:mainframes)).",
                        "Type iqlengine status and press [[Enter]].")
                .paragraph("The reply says \"Midsoft IQL Server: running\" when the server is ready to answer. If it"
                        + " says stopped, start it with iqlengine start ([](jsc:engine_services)).")
                .define("server", "A computer on the network that holds items on its disks.")
                .paragraph("Type net to see a summary of the whole network. It has headings for the network, the"
                        + " Mainframe, the servers, the computers, the subframes and the types it indexes.")
                .subheading("Asking questions")
                .table("Your first reads", "You type", "The reply")
                .row(GuideCell.code("iql QUERY disks"), GuideCell.words("A row for each disk, or \"no rows\""))
                .row(GuideCell.code("iql QUERY servers"), GuideCell.words("A row for each server"))
                .row(GuideCell.code("iql QUERY items WHERE qty > 100"),
                        GuideCell.words("The items held in amounts over 100"))
                .row(GuideCell.code("iql QUERY items ORDER BY qty DESC LIMIT 2"),
                        GuideCell.words("The 2 rows with the largest quantity"))
                .paragraph("Each row is one line. Its name and details are on the left, and its quantity is on the"
                        + " right. The command line shows at most 64 rows, so a longer answer is cut short.")
                .define("table", "A list that a QUERY reads, such as disks, servers or items.")
                .paragraph("The three tables of these first reads have these fields. Each field is one piece of"
                        + " information about a row.")
                .table("The fields of three tables", "Table", "Fields")
                .row(GuideCell.words("disks"), GuideCell.code("disk, server, tier, size, used"))
                .row(GuideCell.words("servers"), GuideCell.code("name, used, free, era"))
                .row(GuideCell.words("items"), GuideCell.code("item, qty, server, tag, enchant"))
                .paragraph("ORDER BY sorts the rows by one field before they are shown. DESC reverses"
                        + " the order. LIMIT 2 stops the answer after 2 rows. The other tables are listed in"
                        + " [QUERY and SELECT](jsc:query_and_select).")
                .subheading("Taking items out")
                .steps("Type iql SELECT 64 iron_ingot and press [[Enter]].",
                        "Read the reply: it says the SELECT is queued, and where the items go.",
                        "Type iql CRAFT 64 torch and press [[Enter]].",
                        "Read the reply: it says the CRAFT is queued.")
                .table("Your first work", "You type", "The reply")
                .row(GuideCell.code("iql SELECT 64 iron_ingot"),
                        GuideCell.code("SELECT queued: 64 Iron Ingot -> local storage"))
                .row(GuideCell.code("iql CRAFT 64 torch"), GuideCell.code("CRAFT queued: 64 Torch"))
                .paragraph("SELECT with no FROM takes the items from anywhere on the network. To take them from one"
                        + " server, write FROM and the server's name, as QUERY servers lists it. The reply names the"
                        + " item by its display name, and the items reach local storage: this computer's own storage.")
                .paragraph("CRAFT makes items, and it works only for items the network has a pattern for"
                        + " ([](jsc:how_autocrafting_works)).")
                .subheading("Watching the work")
                .paragraph("Type interac ops at the prompt to list the work that is still running. The Network Manager"
                        + " program also lists the Operations in flight ([](jsc:network_manager)).")
                .ifSomethingGoesWrong("The reply says \"syntax: expected an item\".",
                        "Write the item's name after the action, such as iron_ingot.",
                        "The reply says \"syntax: percentage quantities are not supported yet\".",
                        "Write a number of items instead, such as 50 items.",
                        "The reply says \"syntax: SELECT has no TO destination\".",
                        "SELECT brings items to you. Leave out TO. To send items between two places, use MOVE.",
                        "The reply says \"syntax: DELETE exports out of the network and needs a TO destination\".",
                        "Add TO, and the place the items leave for.",
                        "The reply says \"syntax: MOVE is internal and needs both FROM and TO\".",
                        "Give both places, FROM where the items come from and TO where they go.",
                        "The reply says \"syntax: FROM is not valid on a read\".",
                        "A query only reads. Leave out FROM, or use an action that moves items.",
                        "The reply says \"syntax: unknown priority level\".",
                        "Use one of the levels the reply lists: LOW, MEDIUM_LOW, MEDIUM, MEDIUM_HIGH or HIGH.",
                        "The reply says \"syntax: unknown verb\", followed by a word.",
                        "Check the spelling of the action word. The words that exist are in"
                                + " [the catalogue](jsc:operations_catalogue).",
                        "The reply says \"unknown item\", followed by the name.",
                        "The network does not know that name. Check the spelling, or write the full name, such as"
                                + " minecraft:iron_ingot.",
                        "The reply says \"no server named\", followed by the name.",
                        "No server has that name. Use QUERY servers to see the names.",
                        "The reply says \"syntax: expected an object to query\".",
                        "Name what to query after QUERY, such as disks or items.",
                        "The reply says \"no pattern crafts\", followed by the item's name.",
                        "The network has no pattern for that item ([](jsc:how_autocrafting_works)).",
                        "The reply says \"Network Operations Service unavailable\".",
                        "The Midsoft IQL Server is stopped. Start it with iqlengine start.",
                        "The reply says \"the network has no running Mainframe\".",
                        "Turn the Mainframe on.")
                .seeAlso("jsc:query_and_select", "jsc:operations_catalogue", "jsc:iql_errors")
                .register();
    }
}
