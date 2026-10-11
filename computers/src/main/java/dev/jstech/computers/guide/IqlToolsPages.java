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
 * Declares the pages of the tools chapter of the Learning IQL book: the IQL Server Management Studio, the IQL Server
 * Profiler, Nextgre Planner Studio and Prophet Reactive Console, each explained from its window and its keys.
 */
final class IqlToolsPages {

    private IqlToolsPages() {
    }

    /** Declares the sections and entries of the tools chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        studioSection(guide, chapter);
        profilerSection(guide, chapter);
        engineProgramsSection(guide, chapter);
    }

    private static void studioSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("isms_studio").titled("The Management Studio")
                .in(chapter).register();
        studioOverview(guide, section);
        studioKeys(guide, section);
        studioExplorer(guide, section);
        studioQueries(guide, section);
        studioActivity(guide, section);
        studioTemplates(guide, section);
    }

    private static void studioOverview(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("studio_overview", section).titled("The IQL Server Management Studio")
                .paragraph("The IQL Server Management Studio (ISMS) is the program where you write IQL, run it,"
                        + " and read what the network answers. Use it when you want to query the network by hand.")
                .paragraph("The studio is a desktop program. Open it from its desktop icon: typing a command at the"
                        + " prompt does not open it. The console answers \"Open the ISMS from its desktop icon.\"")
                .paragraph("The studio works with the Midsoft IQL Server, the engine that runs on the"
                        + " [Mainframe](jsc:mainframes) and reads your IQL. You need a network with that engine"
                        + " before the studio can work: [the Midsoft IQL Server](jsc:midsoft_iql_server).")
                .paragraph("The studio runs from the Legacy era on, and it needs 128 MB of memory.")
                .subheading("Its look in each era")
                .paragraph("The studio's dress follows the era of your computer. Every look has the same functions."
                        + " Only the dress and a few names change.")
                .table("Look by era", "Era", "Look")
                .row(GuideCell.words("Legacy"), GuideCell.words("The 2000 look. The name in the window is"
                        + " \"IQL Query Analyzer\"."))
                .row(GuideCell.words("Transition"), GuideCell.words("The 2008 look."))
                .row(GuideCell.words("Standard"), GuideCell.words("The 2012 look."))
                .row(GuideCell.words("Advanced and every era after it"), GuideCell.words("The 2022 look."))
                .subheading("The parts of the window")
                .table("Part of the window", "Part", "What it holds")
                .row(GuideCell.words("Menu bar"), GuideCell.words("The menus File, Edit, View, Query, Tools,"
                        + " Window and Help."))
                .row(GuideCell.words("Toolbar"), GuideCell.words("The buttons Plan, Grid, Text, File and Cancel."
                        + " Cancel stops the query that is running."))
                .row(GuideCell.words("Object Explorer"), GuideCell.words("The tree of the network, described in its"
                        + " own entry."))
                .row(GuideCell.words("Query tabs"), GuideCell.words("One tab for each query. Each tab holds a"
                        + " script and its results."))
                .row(GuideCell.words("Status bar"), GuideCell.words("The engine, its version, the computer and the"
                        + " time of the last run."))
                .paragraph("The Object Explorer Details, the Activity Monitor, the Template Explorer and the IQL"
                        + " Reference open beside the queries. The Profiler opens in its own window.")
                .subheading("Settings and the start")
                .paragraph("The studio keeps its settings in the file isms.cfg at the top of the computer's disk."
                        + " When it starts, it opens an empty query tab, unless you turn off \"Open a new query when"
                        + " the studio starts\" in Tools > Options.")
                .define("Script", "The statements in one query tab. The statements are separated by semicolons and"
                        + " run one at a time.")
                .ifSomethingGoesWrong(
                        "The studio says \"No compatible Midsoft IQL Server was found on this network.\"",
                        "Put a Mainframe that runs the Midsoft IQL Server on this network"
                                + " ([](jsc:midsoft_iql_server)).",
                        "The studio says \"<computer> runs <engine>. Use the tools of that engine, or ask whoever"
                                + " runs the Mainframe to switch it in the Network Manager.\"",
                        "Use the tools of the engine that runs, or switch the engine in the Network Manager"
                                + " ([](jsc:network_manager)).",
                        "The studio says \"<computer> has no Midsoft IQL Server installed.\"",
                        "Install the engine from the Network Services of the Network Manager"
                                + " ([](jsc:network_manager)).",
                        "The studio says \"Not connected: <name>\".",
                        "Connect the studio to a network whose Mainframe runs the engine, then try again.")
                .seeAlso("jsc:what_iql_is", "jsc:first_queries")
                .register();
    }

    private static void studioKeys(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("studio_keys", section).titled("Its keys")
                .paragraph("Most actions have a key. The menu writes that key beside the item, and the studio answers"
                        + " to the same key. The first table lists the keys for files and text.")
                .paragraph("When you read a key written with a plus sign, press the keys together:"
                        + " [[Ctrl]]+[[Shift]]+[[S]] means hold Ctrl and Shift, then press S.")
                .table("Keys for files and text", "Keys", "Menu item and what it does")
                .row(GuideCell.code("Ctrl+N"), GuideCell.words("New Query. Opens a new query tab."))
                .row(GuideCell.code("Ctrl+O"), GuideCell.words("Open File. Opens a file in the studio."))
                .row(GuideCell.code("Ctrl+F4"), GuideCell.words("Close. Closes the current query tab."))
                .row(GuideCell.code("Ctrl+S"), GuideCell.words("Save. Saves the current tab."))
                .row(GuideCell.code("Ctrl+Shift+S"), GuideCell.words("Save All. Saves every tab."))
                .row(GuideCell.code("Ctrl+Z"), GuideCell.words("Undo. Takes back the last change."))
                .row(GuideCell.code("Ctrl+Y"), GuideCell.words("Redo. Makes the change again."))
                .row(GuideCell.code("Ctrl+X"), GuideCell.words("Cut."))
                .row(GuideCell.code("Ctrl+C"), GuideCell.words("Copy."))
                .row(GuideCell.code("Ctrl+V"), GuideCell.words("Paste."))
                .row(GuideCell.code("Ctrl+A"), GuideCell.words("Select All."))
                .row(GuideCell.code("Ctrl+F"), GuideCell.words("Find. Searches the tab."))
                .row(GuideCell.code("Ctrl+H"), GuideCell.words("Replace. Searches the tab and replaces the text."))
                .row(GuideCell.code("Ctrl+G"), GuideCell.words("Go To Line. Moves to a line you choose."))
                .row(GuideCell.code("Ctrl+K"), GuideCell.words("Comment / Uncomment. Turns the lines into comments,"
                        + " or back."))
                .row(GuideCell.code("Ctrl+Shift+U"), GuideCell.words("Make Uppercase."))
                .row(GuideCell.code("Ctrl+U"), GuideCell.words("Make Lowercase."))
                .row(GuideCell.code("Ctrl+J"), GuideCell.words("List Members. Offers the names the studio knows."))
                .row(GuideCell.code("Ctrl+Space"), GuideCell.words("Complete Word. Offers the names the studio knows."))
                .table("Keys for running and results", "Keys", "Menu item and what it does")
                .row(GuideCell.code("F5"), GuideCell.words("Execute. Runs the whole script."))
                .row(GuideCell.code("Ctrl+E"), GuideCell.words("Execute Selection. Runs only the selected text. With"
                        + " nothing selected, it runs the whole tab."))
                .row(GuideCell.code("Alt+Break"), GuideCell.words("Cancel Executing Query. Stops the query that is"
                        + " running."))
                .row(GuideCell.code("Ctrl+F5"), GuideCell.words("Parse. Checks the syntax without running anything."))
                .row(GuideCell.code("Ctrl+L"), GuideCell.words("Display Estimated Plan. Shows the plan of a CRAFT"
                        + " statement without running it."))
                .row(GuideCell.code("Ctrl+D"), GuideCell.words("Results to Grid."))
                .row(GuideCell.code("Ctrl+T"), GuideCell.words("Results to Text."))
                .row(GuideCell.code("Ctrl+R"), GuideCell.words("Results Pane. Shows the results pane."))
                .row(GuideCell.code("Ctrl+Shift+F"), GuideCell.words("Results to File. Saves the results as"
                        + " comma-separated values."))
                .row(GuideCell.code("Ctrl+Shift+M"), GuideCell.words("Specify Values for Template Parameters. Asks"
                        + " for the values of a statement's blanks."))
                .table("Keys for the windows and help", "Keys", "Menu item and what it does")
                .row(GuideCell.code("F8"), GuideCell.words("Object Explorer. Opens the tree of the network."))
                .row(GuideCell.code("F7"), GuideCell.words("Object Explorer Details. Opens the details of the picked"
                        + " node."))
                .row(GuideCell.code("F4"), GuideCell.words("Properties Window. Shows the facts of the picked node."))
                .row(GuideCell.code("Ctrl+Tab"), GuideCell.words("Next Tab. Moves to the next query tab."))
                .row(GuideCell.code("Ctrl+Shift+Tab"), GuideCell.words("Previous Tab. Moves to the previous query"
                        + " tab."))
                .row(GuideCell.code("F1"), GuideCell.words("IQL Reference. Shows the list of statements."))
                .paragraph("The Break key is sometimes printed as Pause, and Alt+Pause also cancels a running query."
                        + " The Keyboard Shortcuts page lists every key of the studio's menus. The Help menu opens it.")
                .register();
    }

    private static void studioExplorer(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("studio_explorer", section).titled("The Object Explorer")
                .paragraph("The Object Explorer is the tree at the side of the studio. Its top node is the network,"
                        + " and folders hang below it. When you open a folder, you see what the network holds.")
                .paragraph("Every folder holds one kind of thing. Right-click a node, and you see the menu of that"
                        + " kind of node. The next table names the folders.")
                .table("Folders of the network", "Folder", "What it holds")
                .row(GuideCell.words("Tables"), GuideCell.words("The tables of the network. Each table has its"
                        + " columns below it."))
                .row(GuideCell.words("Views"), GuideCell.words("The views you saved with CREATE VIEW."))
                .row(GuideCell.words("Stored Procedures"), GuideCell.words("The procedures you saved with CREATE"
                        + " PROCEDURE, under Programmability."))
                .row(GuideCell.words("Storage"), GuideCell.words("One server for each storage server on the"
                        + " network."))
                .row(GuideCell.words("Management"), GuideCell.words("The Index, the Locks and the Operations Log."
                        + " On the Legacy look this folder is called Maintenance."))
                .row(GuideCell.words("Automation Agent"), GuideCell.words("The Jobs folder."))
                .row(GuideCell.words("Jobs"), GuideCell.words("The jobs you made with CREATE JOB. A paused job shows"
                        + " (paused) beside its name."))
                .paragraph("The Index is the network's record of what each storage holds. Its note tells you how"
                        + " healthy it is. The next table shows each note and the statement that fixes it.")
                .table("Index notes", "Note", "What it means", "Fix")
                .row(GuideCell.words("healthy"), GuideCell.words("Consistent."), GuideCell.words("none"))
                .row(GuideCell.words("stale"), GuideCell.words("Hot events left entries the index has not re-read."),
                        GuideCell.code("REINDEX;"))
                .row(GuideCell.words("fragmented"), GuideCell.words("Mass removals left ghost entries pointing at"
                        + " storage that is gone."), GuideCell.code("VACUUM;"))
                .paragraph("A lock holds items so that other Operations cannot take them. Each entry under Locks"
                        + " shows the item and its quantity, as an item name with x and a number.")
                .define("Index", "The network's record of what each storage holds. ANALYZE, REINDEX and VACUUM keep"
                        + " it fresh.")
                .define("Lock", "A hold on items, so that other Operations cannot take them.")
                .table("The menu of each node", "Node", "Menu items", "Statement it runs")
                .row(GuideCell.words("Network (the root)"), GuideCell.words("New Query, Refresh, Start, Stop and"
                        + " Restart the engine, Properties"), GuideCell.words("none"))
                .row(GuideCell.words("Table"), GuideCell.words("Query Top 100 Rows, Count Rows, Script Table as,"
                        + " Properties, Refresh"), GuideCell.code("QUERY <table> LIMIT 100;"))
                .row(GuideCell.words("View"), GuideCell.words("Query View, Script View as, Properties"),
                        GuideCell.code("QUERY <view>;"))
                .row(GuideCell.words("Stored Procedure"), GuideCell.words("Execute Stored Procedure, Modify, Script"
                        + " Procedure as, Properties"), GuideCell.code("EXEC <name>;"))
                .row(GuideCell.words("Server"), GuideCell.words("Query What It Holds"),
                        GuideCell.code("QUERY items WHERE server = \"<name>\";"))
                .row(GuideCell.words("Index"), GuideCell.words("Analyze"), GuideCell.code("ANALYZE;"))
                .row(GuideCell.words("Index"), GuideCell.words("Rebuild"), GuideCell.code("REINDEX;"))
                .row(GuideCell.words("Index"), GuideCell.words("Vacuum"), GuideCell.code("VACUUM;"))
                .row(GuideCell.words("Lock"), GuideCell.words("Unlock"), GuideCell.words("none"))
                .row(GuideCell.words("Operations Log"), GuideCell.words("Query the Log"),
                        GuideCell.code("QUERY operations LIMIT 100;"))
                .row(GuideCell.words("Job"), GuideCell.words("Start Job, Pause Job, Delete"), GuideCell.words("none"))
                .row(GuideCell.words("Automation Agent"), GuideCell.words("New Job"), GuideCell.words("none"))
                .paragraph("Script Table as and Script View as open a submenu. Each item writes a statement into a new"
                        + " query tab, or copies it to the clipboard. Delete on a job asks first: \"Delete the job"
                        + " <name>?\"")
                .paragraph("Object Explorer Details (F7) lists the children of the picked node in columns: Name,"
                        + " Kind, Rows, Columns, What it holds, Last change, State, Trigger, Quantity and Version. It"
                        + " has a Search box and counts its items. Properties (F4) lists the facts of the picked"
                        + " node. A running job shows as running there.")
                .ifSomethingGoesWrong(
                        "The details say \"Nothing is picked in the explorer.\"",
                        "Click a node in the tree, then open the details again with F7.",
                        "Stop and Restart of the engine need a connection to the network.",
                        "Connect the studio to a network first. The studio says \"Not connected: <name>\" until"
                                + " it is.")
                .seeAlso("jsc:maintenance_analyze_vacuum_reindex", "jsc:iql_jobs")
                .register();
    }

    private static void studioQueries(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("studio_queries", section).titled("Writing and running queries")
                .paragraph("Each query tab holds one script: the statements you write, and the results of the last run."
                        + " The tabs are named Query 1, Query 2 and so on. [[Ctrl]]+[[Tab]] moves to the next tab.")
                .subheading("Your first run")
                .steps("Press [[Ctrl]]+[[N]] to open a new query tab.",
                        "Type a statement and end it with a semicolon, for example QUERY items LIMIT 10;",
                        "Press [[F5]] to run the whole script.",
                        "Read the results under the script. Press [[Ctrl]]+[[D]] to see them as a grid.")
                .paragraph("Statements run one at a time. The studio sends the next one only when the last one has"
                        + " come back.")
                .paragraph("Each Operation that a run starts is listed in the Messages tab, as \"Operation #<id>"
                        + " started - Cancel Executing Query stops it\". Press [[Alt]]+[[Break]], or the toolbar's"
                        + " Cancel, to stop the run. The studio then answers \"Query was cancelled by user.\"")
                .subheading("Statements that cannot be undone")
                .paragraph("Some statements cannot be undone: every DROP statement, and a DELETE of ALL. Before one"
                        + " runs, the studio asks \"This statement cannot be undone:\", shows the statement and asks"
                        + " \"Run it?\". Its buttons are Yes, No and a box named \"Don't ask again\". Ticking that box"
                        + " turns the question off.")
                .subheading("Options")
                .table("Options of the studio", "Option", "Default", "What it does")
                .row(GuideCell.words("Ask before a statement that cannot be undone"), GuideCell.words("on"),
                        GuideCell.words("Asks before each statement that cannot be undone. Tools > Options."))
                .row(GuideCell.words("Open a new query when the studio starts"), GuideCell.words("on"),
                        GuideCell.words("Opens an empty query tab at the start. Tools > Options."))
                .row(GuideCell.words("Show line numbers"), GuideCell.words("on"),
                        GuideCell.words("Shows a number beside each line of a script. Tools > Options."))
                .row(GuideCell.words("Stop the script at the first error"), GuideCell.words("on"),
                        GuideCell.words("Stops the run at the first failed statement. Query > Query Options."))
                .subheading("Limits")
                .paragraph("A statement may be 512 characters long. A longer one is refused, and the studio marks the"
                        + " line where it starts.")
                .subheading("Results and messages")
                .paragraph("Results appear in a grid by default. [[Ctrl]]+[[T]] shows them as text, and"
                        + " [[Ctrl]]+[[Shift]]+[[F]] saves them to a file as comma-separated values.")
                .paragraph("The Messages tab, next to Results, lists what the run did. It keeps at most 200 messages"
                        + " for each tab.")
                .subheading("Checking and planning")
                .paragraph("[[Ctrl]]+[[E]] runs only the selected text. [[Ctrl]]+[[F5]] checks the syntax without"
                        + " running anything. A clean check answers \"Commands completed successfully.\"")
                .paragraph("[[Ctrl]]+[[L]] shows the estimated plan of a CRAFT statement. For any other statement the"
                        + " studio answers \"Display Estimated Plan works on a CRAFT statement.\" The plan lists the"
                        + " steps chosen to make the craft.")
                .subheading("Saving and opening")
                .paragraph("Save writes the tab to its file. Save As... writes a new script file. Scripts are saved"
                        + " with the extension .iql, and a file typed as IQL opens in the studio.")
                .ifSomethingGoesWrong(
                        "The studio says \"There is nothing to run.\"",
                        "Type a statement in the tab first, or select the text you want to run.",
                        "The studio marks a line: \"Line <n>: the statement is <length> characters long, more than the"
                                + " 512 a statement may be.\"",
                        "Split the statement into shorter statements, each ending with a semicolon.",
                        "The studio asks \"<tab> has changes that are not saved. Close it anyway?\"",
                        "Press [[Ctrl]]+[[S]] to save the tab first, then close it.",
                        "The run answers \"Query completed with errors\".",
                        "Read the Messages tab, fix the line it names, and run the script again.",
                        "Parse answers \"Parse found an error\" beside a line.",
                        "Fix the line that the message names, then parse again with [[Ctrl]]+[[F5]].",
                        "Save answers \"There are no results to save.\"",
                        "Run a query first, then save its results.",
                        "The studio answers \"Display Estimated Plan works on a CRAFT statement.\"",
                        "Put a CRAFT statement in the tab, then press [[Ctrl]]+[[L]].")
                .seeAlso("jsc:query_and_select", "jsc:iql_limits")
                .register();
    }

    private static void studioActivity(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("studio_activity", section).titled("The Activity Monitor")
                .paragraph("The Activity Monitor shows the Operations in flight on the network: the work that the"
                        + " network is doing right now. Open it from the Tools menu, and you see the list refresh by"
                        + " itself while it stays open.")
                .paragraph("The top line reads \"Craft slots <used> / <total>\": the craft slots in use out of the"
                        + " total. On the right it reads \"<n> running\". When nothing runs, the list says \"No"
                        + " Operations in flight.\"")
                .table("The status of each Operation", "Status word", "Group")
                .row(GuideCell.words("done, running"), GuideCell.words("going"))
                .row(GuideCell.words("partial, waiting, queued"), GuideCell.words("held up"))
                .row(GuideCell.words("failed, locked, discarded"), GuideCell.words("stopped"))
                .paragraph("Each group has a colour of its own, so you can see at a glance which Operations go on and"
                        + " which are held up or stopped. The statuses are explained in"
                        + " [the Operation states](jsc:operation_states).")
                .paragraph("Click an Operation row to open a menu with one item, Cancel Operation. The item works only"
                        + " for an Operation that has an id. Cancel Operation asks the network to stop that"
                        + " Operation.")
                .ifSomethingGoesWrong(
                        "The list says \"No Operations in flight.\"",
                        "Start a query or a craft. Its Operation appears in this list.",
                        "Cancel Operation cannot be chosen on a row.",
                        "That Operation has no id yet. Wait for it to get one, or press [[Alt]]+[[Break]] in the query"
                                + " tab that started it.")
                .seeAlso("jsc:operations_catalogue", "jsc:cancelling_and_the_log")
                .register();
    }

    private static void studioTemplates(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("studio_templates", section).titled("Templates and the IQL Reference")
                .paragraph("A template is a statement with blanks, which you fill in before it runs. The Template"
                        + " Explorer, in the View menu, lists eight of them. Click one, and you open it in a new"
                        + " query.")
                .define("Template", "A saved statement with blanks in it. The blanks are filled in before the"
                        + " statement runs.")
                .table("The eight templates", "Template", "Statement it opens")
                .row(GuideCell.words("Query a table"), GuideCell.code("QUERY <table, table, items> WHERE <column,"
                        + " column, qty> < <value, number, 64> ORDER BY <column, column, qty> DESC LIMIT <rows,"
                        + " number, 100>;"))
                .row(GuideCell.words("Count an item"), GuideCell.code("COUNT <item, name, iron_ingot>;"))
                .row(GuideCell.words("Craft an item"), GuideCell.code("CRAFT <quantity, number, 64> <item, name,"
                        + " iron_ingot>;"))
                .row(GuideCell.words("Create a view"), GuideCell.code("CREATE VIEW <name, name, low_stock> AS QUERY"
                        + " items WHERE qty < <value, number, 64>;"))
                .row(GuideCell.words("Create a procedure"), GuideCell.code("CREATE PROCEDURE <name, name, restock>"
                        + " AS { CRAFT <quantity, number, 64> <item, name, torch>; };"))
                .row(GuideCell.words("Create a job"), GuideCell.code("CREATE JOB <name, name, nightly_vacuum> AS"
                        + " VACUUM EVERY <interval, interval, 20m>;"))
                .row(GuideCell.words("Hold an item"), GuideCell.code("LOCK <quantity, number, 64> <item, name,"
                        + " diamond>;"))
                .row(GuideCell.words("Update an item"), GuideCell.code("UPDATE <quantity, number, 1> <item, name,"
                        + " iron_sword> SET REPAIR;"))
                .paragraph("A blank has three parts in its angle brackets: a name, a type and a default value. For"
                        + " example, <quantity, number, 64> is a blank named quantity, of the type number, whose value"
                        + " starts as 64.")
                .paragraph("Press [[Ctrl]]+[[Shift]]+[[M]] to fill in the blanks of the current query. The dialog"
                        + " Specify Values for Template Parameters asks for each value, and shows each blank as its"
                        + " name and type.")
                .subheading("The IQL Reference")
                .paragraph("Press [[F1]] to open the IQL Reference. It lists each statement with what it does. The"
                        + " table below gives the same list, with the meaning the studio gives each line.")
                .table("The IQL Reference", "Syntax", "What it does")
                .row(GuideCell.code("QUERY table [WHERE ...] [ORDER BY ...] [LIMIT n]"),
                        GuideCell.words("Reads the rows of a table."))
                .row(GuideCell.code("COUNT item"), GuideCell.words("Counts an item the network holds."))
                .row(GuideCell.code("SELECT n item"), GuideCell.words("Brings an item to this computer."))
                .row(GuideCell.code("INSERT n item"), GuideCell.words("Stores an item from this computer."))
                .row(GuideCell.code("DELETE n item TO place"), GuideCell.words("Sends an item out of the network."))
                .row(GuideCell.code("MOVE n item FROM server TO server"),
                        GuideCell.words("Moves an item between servers."))
                .row(GuideCell.code("DROP n item"), GuideCell.words("Destroys an item, for good."))
                .row(GuideCell.code("CRAFT n item"), GuideCell.words("Makes an item from its recipes."))
                .row(GuideCell.code("UPDATE n item SET action"),
                        GuideCell.words("Changes an item with a personal-use card."))
                .row(GuideCell.code("LOCK n item / UNLOCK item"), GuideCell.words("Holds an item, or lets it go."))
                .row(GuideCell.code("ANALYZE / REINDEX / VACUUM"), GuideCell.words("Looks after the index."))
                .row(GuideCell.code("CREATE VIEW name AS query"), GuideCell.words("Saves a query under a name."))
                .row(GuideCell.code("CREATE PROCEDURE name AS { ... }"),
                        GuideCell.words("Saves statements to run together."))
                .row(GuideCell.code("CREATE JOB name AS ... EVERY time"),
                        GuideCell.words("Runs statements on a timer or a condition."))
                .row(GuideCell.code("EXEC name"), GuideCell.words("Runs a saved procedure."))
                .row(GuideCell.code("WHERE / ORDER BY / LIMIT"), GuideCell.words("WHERE keeps the rows that match,"
                        + " ORDER BY sorts them, LIMIT stops after so many."))
                .row(GuideCell.code("; --"), GuideCell.words("A semicolon ends a statement; two dashes start a"
                        + " comment."))
                .ifSomethingGoesWrong(
                        "[[Ctrl]]+[[Shift]]+[[M]] says \"This query has no template parameters.\"",
                        "Open a template from the Template Explorer, or write the statement by hand.")
                .seeAlso("jsc:first_queries", "jsc:query_and_select", "jsc:iql_count", "jsc:insert_delete_move_drop",
                        "jsc:iql_craft", "jsc:update_cards", "jsc:lock_unlock", "jsc:iql_views", "jsc:iql_procedures",
                        "jsc:iql_jobs")
                .register();
    }

    private static void profilerSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("iql_profiler_section").titled("Watching the engine")
                .in(chapter).register();
        profiler(guide, section);
    }

    private static void profiler(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("profiler", section).titled("The IQL Server Profiler")
                .paragraph("The IQL Server Profiler records the network's work as it happens, one row for each event."
                        + " When a craft or a query does something you did not expect, the Profiler shows you what"
                        + " the network did, row by row.")
                .paragraph("Open it from the studio's Tools menu, with the item IQL Server Profiler, or from the"
                        + " desktop. It opens in its own window, not as a tab of the studio.")
                .paragraph("Recording is done in a trace: one session that you start, pause, stop and save as a file."
                        + " Each new trace is numbered.")
                .define("Trace", "One recording session in the Profiler. It starts, pauses and stops, and its rows can"
                        + " be saved as a file.")
                .subheading("What it records")
                .table("The events the Profiler records", "Event", "Group", "What it records")
                .row(GuideCell.code("StatementStarting"), GuideCell.words("Statements"),
                        GuideCell.words("a statement comes through the network's door"))
                .row(GuideCell.code("StatementCompleted"), GuideCell.words("Statements"),
                        GuideCell.words("a statement came back, with the rows it read"))
                .row(GuideCell.code("OperationCreated"), GuideCell.words("Operations"),
                        GuideCell.words("the Mainframe took an Operation on"))
                .row(GuideCell.code("OperationSettled"), GuideCell.words("Operations"),
                        GuideCell.words("an Operation ended: its moves, its time, and why it failed if it did"))
                .row(GuideCell.code("LockAcquired"), GuideCell.words("Locks"),
                        GuideCell.words("an Operation took a lock, or an item was held by hand"))
                .row(GuideCell.code("PlanChosen"), GuideCell.words("Plans"),
                        GuideCell.words("the engine chose how a craft is made"))
                .row(GuideCell.code("BusMoved"), GuideCell.words("Buses"),
                        GuideCell.words("an Import Bus or an Export Bus moved items"))
                .paragraph("Each row has the columns EventClass, TextData, Requester, Computer, Items, Duration and"
                        + " StartTime.")
                .subheading("Starting, pausing and stopping")
                .table("The buttons of the Profiler", "Button", "What it does")
                .row(GuideCell.words("New Trace"), GuideCell.words("Stops the trace, empties the rows and numbers the"
                        + " new trace. [[Ctrl]]+[[N]] does the same."))
                .row(GuideCell.words("Open"), GuideCell.words("Reads a trace file back into the window."))
                .row(GuideCell.words("Save"), GuideCell.words("Writes the rows to a file."))
                .row(GuideCell.words("Start"), GuideCell.words("Begins the trace, or continues it, with the groups you"
                        + " chose."))
                .row(GuideCell.words("Pause"), GuideCell.words("Stops listening and keeps the rows. The server stops"
                        + " sending rows."))
                .row(GuideCell.words("Stop"), GuideCell.words("Ends the trace. The rows stay until you choose New"
                        + " Trace or Clear."))
                .row(GuideCell.words("Clear"), GuideCell.words("Empties the rows."))
                .row(GuideCell.words("Find"), GuideCell.words("Searches the rows as text."))
                .row(GuideCell.words("Events"), GuideCell.words("Chooses the groups to record: Statements, Operations,"
                        + " Locks, Plans and Buses."))
                .paragraph("Changing the groups while a trace runs restarts it with the new groups. A group that"
                        + " nobody watches records nothing.")
                .paragraph("The status line reads \"Trace is running.\", \"Trace is paused.\" or \"Trace is"
                        + " stopped.\" A new trace with no rows says \"No events yet. Start the trace to record what"
                        + " the network does.\"")
                .subheading("Reading the rows")
                .paragraph("Under the grid, the detail lines of the row you choose appear. An event shows who asked for"
                        + " it, as \"asked by: <requester> on <computer>\", and how long it took, as \"took <n>"
                        + " ticks (<n> s)\". Twenty ticks make one second. When nobody asked, the requester reads"
                        + " \"network\".")
                .paragraph("The Profiler keeps at most 2,000 rows. When the trace is full, the oldest rows are dropped."
                        + " The bottom bar shows \"Ln <n>, Col 1\" for the selected row, where Ln is the row number,"
                        + " and \"Rows: <n>\" for the count of rows.")
                .subheading("Finding and replaying")
                .paragraph("Find searches the rows as text. Find Next moves to the next match, and Escape closes the"
                        + " find strip. When nothing else matches, it says \"Nothing more holds <text>.\"")
                .paragraph("Replay Statement, in the Replay menu, runs a recorded statement again. It is on only when"
                        + " you have selected a StatementStarting or a StatementCompleted row. It opens the studio and"
                        + " runs the statement in a new query.")
                .subheading("Saving and opening")
                .paragraph("Save As... writes the rows to a file. The file name is the trace's name, with spaces"
                        + " replaced by underscores, and the extension .log. The file has a header line with the seven"
                        + " column names, then one line for each row, and its values are separated by tabs.")
                .paragraph("The detail lines of a row are joined with \" | \", and a line break inside a row becomes a"
                        + " space. Save is off until the trace has rows.")
                .ifSomethingGoesWrong(
                        "The window says \"No events yet. Start the trace to record what the network does.\"",
                        "Press Start. The Profiler records only what happens after the trace starts.",
                        "The trace has stopped by itself.",
                        "A trace ends when the player who started it leaves the game, and when the server stops."
                                + " Use New Trace to start a new one.",
                        "The save says \"The trace was saved to <file>, but it was too long: it was cut to <n> rows"
                                + " and <n> rows were left out.\"",
                        "Record a shorter trace, or choose fewer groups, so that fewer rows are kept.",
                        "Replay Statement is off in the Replay menu.",
                        "Select a StatementStarting or a StatementCompleted row first.",
                        "The Save button is off.",
                        "Save is off until the trace has rows. Start the trace and let some events come in.")
                .seeAlso("jsc:operations_catalogue", "jsc:buses")
                .register();
    }

    private static void engineProgramsSection(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("engine_programs").titled("The engines' own tools")
                .in(chapter).register();
        nextgrePlannerStudio(guide, section);
        prophetConsole(guide, section);
    }

    private static void nextgrePlannerStudio(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("nextgre_planner_studio", section).titled("Nextgre Planner Studio")
                .paragraph("The Nextgre Planner Studio shows how NextgreIQL plans a craft, and how long each step"
                        + " of the plan takes. Use it when a craft takes longer than you expect, or when you want to"
                        + " tune the planner.")
                .paragraph("The studio needs the NextgreIQL engine, version 7.0, running on the Mainframe."
                        + " The engine is explained in [the NextgreIQL engine](jsc:nextgre_iql).")
                .define("Planner", "The part of NextgreIQL that picks how a craft is made, by weighing the recipes it"
                        + " knows.")
                .define("Lane", "One machine that can run at the same time as other machines.")
                .define("Bench", "A recipe made by hand at a crafting bench, as opposed to one that runs in a"
                        + " machine.")
                .table("The four tabs", "Tab", "What it shows")
                .row(GuideCell.words("Explain"), GuideCell.words("The plan of a statement, box by box."))
                .row(GuideCell.words("Statistics"), GuideCell.words("What the planner has measured on this network."))
                .row(GuideCell.words("Rules"), GuideCell.words("The planner's rules, which you can switch on and off."))
                .row(GuideCell.words("History"), GuideCell.words("Every plan the planner made lately."))
                .subheading("Explain")
                .paragraph("Type a CRAFT or a SELECT, then press [[F7]] for Explain, or [[Shift]]+[[F7]] for Explain"
                        + " Analyze. Explain plans the statement without running it. Explain Analyze runs it too, and"
                        + " the boxes fill in as the craft goes.")
                .paragraph("The plan is a tree of boxes. Each box is one step, with its name and its time. Before the"
                        + " craft runs, a box reads \"est <n> s\", the estimate in seconds. After Explain Analyze, it"
                        + " reads \"est <n> s, actual <n> s\".")
                .table("The text on a box", "Text", "What the step does")
                .row(GuideCell.code("Craft <item> x<quantity>"), GuideCell.words("Makes the item from a recipe."))
                .row(GuideCell.code("Process <item> x<quantity>"), GuideCell.words("Makes the item in a machine."))
                .row(GuideCell.code("Pull <item> x<quantity>"),
                        GuideCell.words("Takes the raw items from storage."))
                .row(GuideCell.code("Missing <item> x<quantity>"),
                        GuideCell.words("Nothing on the network holds the item."))
                .row(GuideCell.code("Index Seek <item> x<quantity>"),
                        GuideCell.words("Reads the item through the index."))
                .paragraph("Under the name, a box says where its step runs: \"at a bench, <item> runs\" or \"on a"
                        + " machine, <item> runs\". It also says \"from <server>\" for the server that gives the items,"
                        + " and \"<n> steps, <n> at once\" when steps run side by side. The hints of the statement are"
                        + " marked on the boxes they changed.")
                .paragraph("The header shows \"Planning <n> ms\" and \"Execution <n>\". Beside the tree, PLANS WEIGHED"
                        + " lists each plan the planner weighed, with its cost in ticks. The chosen plan reads"
                        + " \"chosen\". A plan that was set aside reads \"set aside by\", and the reason.")
                .paragraph("A partial plan reads \"makes <n> of the <n> asked for\". A plan that cannot be made reads"
                        + " \"cannot be made: <n> <item> missing\".")
                .subheading("Statistics")
                .paragraph("The Statistics tab shows what the planner has measured on this network. Press Gather"
                        + " Statistics to measure again. The reply reads \"statistics gathered: <n> item types on <n>"
                        + " servers, <n> recipes timed\".")
                .table("The statistics", "Line", "What it counts")
                .row(GuideCell.words("Item types indexed"), GuideCell.words("The item types the index knows."))
                .row(GuideCell.words("Servers"), GuideCell.words("The storage servers the planner knows."))
                .row(GuideCell.words("Recipes timed"), GuideCell.words("The recipes that have a measured time."))
                .row(GuideCell.words("Crafts measured"), GuideCell.words("The crafts the planner has measured."))
                .row(GuideCell.words("Last ANALYZE"),
                        GuideCell.words("\"never\", or how many minutes ago ANALYZE last ran."))
                .row(GuideCell.words("<recipe> at a bench, <recipe> on a machine"),
                        GuideCell.words("The measured time of one recipe."))
                .subheading("Rules")
                .paragraph("A rule is a choice the planner makes when it weighs plans. You can switch each rule on or"
                        + " off for this Mainframe. Rules that other mods add are listed under RULES FROM OTHER MODS.")
                .table("The planner's own rules", "Rule", "What it does")
                .row(GuideCell.words("Weigh machines against benches"),
                        GuideCell.words("Also plans the craft with machine recipes before bench recipes, and keeps the"
                                + " cheaper."))
                .row(GuideCell.words("Weigh every recipe of the result"),
                        GuideCell.words("Plans the craft once with each bench recipe that makes what was asked for, and"
                                + " keeps the cheapest."))
                .row(GuideCell.words("Use measured times"),
                        GuideCell.words("Reckons each recipe at the time it was measured to take on this network,"
                                + " rather than at a guess."))
                .row(GuideCell.words("Count steps that run at once"),
                        GuideCell.words("Reckons steps that do not wait on each other as running side by side."))
                .row(GuideCell.words("Weigh the fastest servers"),
                        GuideCell.words("Also plans taking the raw materials from the servers that hand them over"
                                + " fastest."))
                .subheading("Hints")
                .paragraph("A hint goes after a CRAFT and changes its plan. The built-in hints are these. The hints are"
                        + " explained in full in [the NextgreIQL hints](jsc:nextgre_hints).")
                .table("The built-in hints", "Hint", "Takes a value", "What it does")
                .row(GuideCell.code("PREFER SOURCE <server>"), GuideCell.words("Yes, a server name"),
                        GuideCell.words("Takes the raw materials from the named server first."))
                .row(GuideCell.code("AVOID SOURCE <server>"), GuideCell.words("Yes, a server name"),
                        GuideCell.words("Takes nothing from the named server while another one has it."))
                .row(GuideCell.code("MAX PARALLEL <n>"), GuideCell.words("Yes, a whole number above 0"),
                        GuideCell.words("Runs at most that many stages of the craft at once."))
                .row(GuideCell.code("PREFER MACHINE"), GuideCell.words("No"),
                        GuideCell.words("Makes on a machine whatever both a machine and a bench make."))
                .row(GuideCell.code("PREFER BENCH"), GuideCell.words("No"),
                        GuideCell.words("Makes at a bench whatever both a machine and a bench make."))
                .subheading("History")
                .paragraph("Every plan NextgreIQL made lately is kept on the Mainframe. Click a plan to show it on the"
                        + " Explain tab. The State column reads \"planned\", \"running\", \"done\" or \"fell short\"."
                        + " Until a craft is planned, the History tab says \"No plans yet. Every craft NextgreIQL plans"
                        + " is kept here.\"")
                .ifSomethingGoesWrong(
                        "The program refuses to open and says that no compatible engine was found on this network.",
                        "Run NextgreIQL, version 7.0, on the Mainframe ([](jsc:engine_services)).",
                        "The studio says \"EXPLAIN needs a statement to explain\".",
                        "Write a CRAFT or a SELECT after EXPLAIN.",
                        "The studio says \"EXPLAIN shows the plans of CRAFT and SELECT\".",
                        "Explain a CRAFT or a SELECT. Other statements have no plan to show.",
                        "The studio says \"hints change the plan of a CRAFT\".",
                        "Put the hint after a CRAFT, not after another statement.",
                        "The studio says \"MAX PARALLEL takes a whole number above nought\".",
                        "Write a whole number above 0 after MAX PARALLEL.",
                        "The studio says \"no server on the network is called <name>\".",
                        "Check the server's name in the Object Explorer ([](jsc:studio_explorer)).",
                        "The studio says \"no item is called <name>\".",
                        "Check the spelling of the item's name.",
                        "The studio says \"nothing on the network knows how to make <name>\".",
                        "Choose an item that a recipe on the network makes.",
                        "The studio says \"every plan for <name> was set aside\".",
                        "Look at PLANS WEIGHED to see why each plan was set aside, and at the Rules tab.",
                        "The studio says \"the network could not start the craft of <name>\".",
                        "Look at the Operations in the Activity Monitor ([](jsc:studio_activity)).",
                        "A plan reads \"cannot be made: <n> <item> missing\".",
                        "The network does not hold enough of that item. Bring the missing items, then explain again.")
                .seeAlso("jsc:nextgre_iql", "jsc:nextgre_hints", "jsc:craft_planner")
                .register();
    }

    private static void prophetConsole(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("prophet_console", section).titled("Prophet Reactive Console")
                .paragraph("The Prophet Reactive Console keeps the network's stock where you want it. You tell it which"
                        + " items to keep. When a level falls, it asks the network for what is missing, on its own.")
                .paragraph("The console needs the YourIQL engine, version 3.23, running on the Mainframe. Its own"
                        + " statements are explained in [the YourIQL engine](jsc:prophet_youriql).")
                .define("State", "One item the network is told to keep. In the States list, each state is one KEEP"
                        + " line.")
                .define("Band", "The range that a state is kept inside: at least a number, or between two numbers.")
                .define("Level", "What the network counts for an item: the amount held, plus the amount on its way,"
                        + " plus the amount arriving.")
                .subheading("The prompt")
                .paragraph("Type a YourIQL statement at the prompt, then press Check or Apply. Check reads the"
                        + " statement without changing anything, and answers \"The statement reads well.\" when it is"
                        + " right. Apply runs the statement.")
                .paragraph("A statement that does not start with KEEP, WATCH, FORGET or SHOW STATES goes to the normal"
                        + " IQL engine, explained in [the engines](jsc:iql_engines_overview).")
                .subheading("The tabs")
                .table("The four tabs", "Tab", "What it shows")
                .row(GuideCell.words("States"), GuideCell.words("The states the network is held to, one line each."))
                .row(GuideCell.words("Subscriptions"), GuideCell.words("The watches, each with its number and status."))
                .row(GuideCell.words("Reactions"), GuideCell.words("What the engine did lately."))
                .row(GuideCell.words("Settings"), GuideCell.words("How often the engine looks, how much it asks for"
                        + " at once, and whether it reacts."))
                .subheading("The statements")
                .table("The YourIQL statements", "Statement", "Form", "What it does")
                .row(GuideCell.words("KEEP"), GuideCell.code("KEEP <item> >= <n>"),
                        GuideCell.words("Keeps at least <n> of the item, counting what is on its way."))
                .row(GuideCell.words("KEEP with a band"), GuideCell.code("KEEP <item> BETWEEN <a> AND <b>"),
                        GuideCell.words("Keeps the level between <a> and <b>."))
                .row(GuideCell.words("WATCH"), GuideCell.code("WATCH <item> < <n> DO <statement>"),
                        GuideCell.words("Runs the statement once, when the level crosses the line. The comparisons are"
                                + " <, <=, > and >=."))
                .row(GuideCell.words("WATCH with CRAFT"), GuideCell.code("WATCH <item> < <n> DO CRAFT <item> TO <n>"),
                        GuideCell.words("Makes the item up to level <n>, when the watch fires."))
                .row(GuideCell.words("FORGET"), GuideCell.code("FORGET <item>"),
                        GuideCell.words("Stops keeping the item."))
                .row(GuideCell.words("FORGET WATCH"), GuideCell.code("FORGET WATCH <n>"),
                        GuideCell.words("Removes watch number <n>."))
                .row(GuideCell.words("SHOW STATES"), GuideCell.code("SHOW STATES"),
                        GuideCell.words("Lists the states and the watches."))
                .paragraph("A number is a whole number, zero or more. An upper end in a band only says where the level"
                        + " stops being where it should be: nothing is thrown away to bring it down. The recipe"
                        + " [keep a stock of an item](jsc:recipe_keep_stock) shows a KEEP line at work.")
                .subheading("The States tab")
                .paragraph("The States tab lists each state, with where it stands and what is being done for it. Under"
                        + " the list, the graph of the selected state shows its level.")
                .table("The columns of the States tab", "Column", "What it shows")
                .row(GuideCell.words("DESIRED STATE"), GuideCell.words("The band, or the minimum, that you asked for."))
                .row(GuideCell.words("STATE"), GuideCell.words("Where the state stands, as one of the words in the next"
                        + " table."))
                .row(GuideCell.words("NOW"), GuideCell.words("The level, against its band."))
                .row(GuideCell.words("WORK"), GuideCell.words("The Operations on its way, as \"<n> on the way\", or"
                        + " \"no operations\"."))
                .row(GuideCell.words("LAST REACTION"), GuideCell.words("The last thing done for the state."))
                .table("The states", "Status", "Meaning")
                .row(GuideCell.words("New"), GuideCell.words("Not looked at yet."))
                .row(GuideCell.words("Holding"), GuideCell.words("In its band, with nothing on its way."))
                .row(GuideCell.words("Working"), GuideCell.words("Work is on its way to bring it into its band, or to"
                        + " keep it there."))
                .row(GuideCell.words("Cannot hold"), GuideCell.words("Below its band, and nothing could be set going."))
                .row(GuideCell.words("Over"), GuideCell.words("Above its band. Nothing is thrown away to bring it"
                        + " down."))
                .row(GuideCell.words("Armed"), GuideCell.words("A watch, waiting for its condition."))
                .row(GuideCell.words("Fired"), GuideCell.words("A watch that fired, and waits for its condition to"
                        + " stop."))
                .paragraph("When the level is below its band, the engine asks the network for the difference as a"
                        + " craft. If nothing could be set going, the state reads Cannot hold, and the engine tries"
                        + " again after 600 ticks (30 seconds).")
                .paragraph("A state above its band, with nothing on its way, reads Over. With work on its way, it reads"
                        + " Working.")
                .subheading("The Subscriptions tab")
                .paragraph("The Subscriptions tab lists the watches. Each watch shows its number, its status, Armed or"
                        + " Fired, and how many times it fired, as \"fired <n> times\". Press Forget to remove a watch."
                        + " The number is the one that FORGET WATCH uses.")
                .subheading("The Reactions tab")
                .paragraph("The Reactions tab lists what the engine did lately, with how long ago, as \"<n> s ago\" or"
                        + " \"<n> min ago\". It keeps at most 32 reactions.")
                .table("The reactions", "Reaction", "What happened")
                .row(GuideCell.words("asked for <n> <item>, Operation <id>"),
                        GuideCell.words("The engine asked the network for a craft, which is now an Operation."))
                .row(GuideCell.words("nothing on the network makes <item>"),
                        GuideCell.words("The engine wanted an item that no recipe on the network makes."))
                .row(GuideCell.words("watch <n> fired: <statement>"),
                        GuideCell.words("A watch fired, and its statement ran."))
                .row(GuideCell.words("watch <n> fired, and was refused: <reason>"),
                        GuideCell.words("A watch fired, but its statement was refused."))
                .row(GuideCell.words("watch <n> fired with nothing to make"),
                        GuideCell.words("A watch fired, and there was nothing to make."))
                .subheading("The Settings tab")
                .paragraph("The Settings tab sets how often the engine looks at the network, how much it asks for at"
                        + " once, and whether it reacts at all.")
                .table("The settings", "Setting", "Choices", "Default")
                .row(GuideCell.words("Look at the network every <n> s"),
                        GuideCell.words("1, 2, 5 or 10 seconds"), GuideCell.words("1 second (20 ticks)"))
                .row(GuideCell.words("Ask for at most, in one craft"),
                        GuideCell.words("64, 256, 1,024 or 4,096 items"), GuideCell.words("1,024 items"))
                .row(GuideCell.words("React to changes"), GuideCell.words("On or Paused"), GuideCell.words("On"))
                .paragraph("When React to changes is Paused, the engine makes no reactions at all. The states are still"
                        + " shown.")
                .ifSomethingGoesWrong(
                        "The console refuses to open, and says that no compatible engine was found on this network.",
                        "Run the YourIQL engine on the Mainframe ([](jsc:engine_services)).",
                        "The statement is refused: \"syntax: KEEP item >= n, or KEEP item BETWEEN a AND b\".",
                        "Write a KEEP line in one of these two forms.",
                        "The statement is refused: \"syntax: WATCH item < n DO statement\".",
                        "Write the comparison as one of <, <=, > or >=, and put DO before the statement.",
                        "The statement is refused: \"syntax: FORGET item, or FORGET WATCH n\".",
                        "Write FORGET with an item, or FORGET WATCH with a number.",
                        "The statement is refused: \"no item is called <name>\".",
                        "Check the spelling of the item's name.",
                        "FORGET is refused: \"<name> is not kept\".",
                        "Forget only an item that a KEEP line keeps. SHOW STATES lists them.",
                        "FORGET WATCH is refused: \"no watch is numbered <n>\".",
                        "Write SHOW STATES to see the number of each watch.",
                        "The Reactions tab says \"watch <n> fired, and was refused: <reason>\".",
                        "Forget that watch with FORGET WATCH, then set it again with a statement that works.",
                        "A state reads Cannot hold.",
                        "Wait: the engine tries again after 600 ticks (30 seconds). Check that the network holds the"
                                + " items the craft needs.",
                        "The Reactions tab stays empty, and no state reacts.",
                        "Open the Settings tab and set React to changes to On.")
                .seeAlso("jsc:prophet_youriql", "jsc:prophet_watch", "jsc:recipe_keep_stock")
                .register();
    }
}
