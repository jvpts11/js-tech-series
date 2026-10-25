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
 * Declares the automation chapter: the jobs the Automation Engine fires, the Automation Manager that writes them,
 * redstone that programs set, and the cluster tools that run many machines at once.
 */
final class AutomationPages {

    private AutomationPages() {
    }

    /** Declares the sections and entries of the automation chapter, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        automationJobs(guide, chapter);
        clusterTools(guide, chapter);
    }

    private static void automationJobs(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("automation_jobs").titled("Jobs the network runs")
                .icon(ComputingModule.MAINFRAME).in(chapter).register();
        automationEngine(guide, section);
        automationManager(guide, section);
        redstonePrograms(guide, section);
    }

    private static void automationEngine(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("automation_engine", section).titled("The Automation Engine and jobs")
                .icon(ComputingModule.MAINFRAME)
                .paragraph("Some work should happen by itself, so you do not have to watch it: restock an item when"
                        + " the network runs short, or move items every hour. A job is a saved statement of IQL, the"
                        + " network's query language, with a trigger that says when the statement runs. The Automation"
                        + " Engine is the service that keeps the network's jobs and fires them on time, or when a"
                        + " condition holds.")
                .define("Job", "A saved IQL statement that the network runs by itself, at a set time or when a"
                        + " condition holds.")
                .define("Automation Engine", "The Mainframe's service that keeps the jobs and fires them. It has"
                        + " no window of its own.")
                .paragraph("Install the Automation Engine on the Mainframe, as you install any service"
                        + " ([](jsc:mainframe_services)). It has no on or off switch of its own. It fires jobs while"
                        + " it is installed and the Mainframe is running, with a valid build and power on.")
                .paragraph("If the Mainframe is off, the jobs stay in the list and none of them fires. Turn the"
                        + " Mainframe back on, and they fire again.")
                .subheading("Triggers")
                .paragraph("You give a trigger after EVERY, with an interval, or after WHEN, with a condition. The"
                        + " Automation Engine checks the jobs every 10 ticks, which is half a second.")
                .define("Trigger", "The part of a job that says when it fires: an interval after EVERY, or a"
                        + " condition after WHEN.")
                .paragraph("EVERY fires the job again after each interval. The first check only starts the count, so"
                        + " a new EVERY job waits one full interval before its first run.")
                .paragraph("WHEN fires the job on the rising edge of its condition: the moment the condition turns from"
                        + " false to true. It does not fire again while the condition stays true. A condition that is"
                        + " already true at the first check fires the job once, at that check.")
                .define("Rising edge", "The moment a condition turns from false to true. A WHEN job fires once for"
                        + " each such moment.")
                .paragraph("A WHEN condition tests only the network's stock of an item, with qty(<item>), against a"
                        + " number. The operators are =, ==, !=, <, >, <=, and >=. An item name without a colon means"
                        + " minecraft:, so iron_ingot is minecraft:iron_ingot. A modded item needs its full name, with"
                        + " its mod's name before the colon.")
                .paragraph("WHEN EVENT waits for a world event the network sees: WHEN EVENT mymod:solar_storm COMING"
                        + " fires as the network first sees it coming, which needs a machine on the network that is"
                        + " the event's instrument; UNDER WAY fires as it begins where the network stands; ENDED fires"
                        + " as it ends. Name the event by its id, or by the part after the colon.")
                .listingIn("iql", "storm_shelter.iql", "CREATE JOB shelter AS SET BUS 'Ore in' OFF"
                        + " WHEN EVENT mymod:solar_storm COMING")
                .table("Forms of a duration", "Form", "Meaning", "Ticks")
                .row(GuideCell.code("30s"), GuideCell.words("30 seconds"), GuideCell.words("600"))
                .row(GuideCell.code("5m"), GuideCell.words("5 minutes"), GuideCell.words("6,000"))
                .row(GuideCell.code("1h"), GuideCell.words("1 hour"), GuideCell.words("72,000"))
                .row(GuideCell.code("100t"), GuideCell.words("100 ticks"), GuideCell.words("100"))
                .row(GuideCell.code("30"), GuideCell.words("30 seconds, a bare number"), GuideCell.words("600"))
                .paragraph("The unit letters are s for seconds, m for minutes, h for hours and t for ticks. A number"
                        + " with no letter counts seconds. There are 20 ticks in a second.")
                .subheading("Writing a job")
                .steps("Install the Automation Engine on the Mainframe.",
                        "At the prompt of a computer on the network, type a CREATE JOB line, as the listing shows.",
                        "Press [[Enter]]. The answer ends with \"created\" and names the job.",
                        "Check the job on the Processes tab, or in the Automation Manager.")
                .listingIn("iql", "keep_iron.iql", "CREATE JOB keep_iron AS CRAFT 64 iron_ingot EVERY 5m")
                .paragraph("Write the name, then AS and the statement the job runs, then EVERY with a duration or WHEN"
                        + " with a condition. Names are up to 64 characters long. DROP JOB followed by the name removes"
                        + " a job. The IQL book explains the job statements in full ([](jsc:iql_jobs)).")
                .subheading("Watching the jobs")
                .paragraph("The Processes tab of the Task Manager ([](jsc:system_monitor_and_task_manager)) lists the"
                        + " network's engine first, then each job with its state.")
                .table("States of a job", "State", "What it means")
                .row(GuideCell.code("ACTIVE"),
                        GuideCell.words("The Automation Engine is running, so the job can fire."))
                .row(GuideCell.code("IDLE"),
                        GuideCell.words("The Automation Engine is not running, so the job does not fire."))
                .row(GuideCell.code("PAUSED"),
                        GuideCell.words("The job is paused, and it does not fire until it is restarted."))
                .paragraph("The detail beside a job shows its trigger, such as \"every 5m\" or \"when"
                        + " qty(iron_ingot) < 64\". On that tab, Start and Stop act on the network's engine, not on the"
                        + " jobs.")
                .subheading("Pausing and restarting")
                .paragraph("Pausing a job keeps it in the list and stops it from firing. Restarting a job resumes it,"
                        + " and an EVERY job counts its interval again from now. DROP JOB removes the job and its pause"
                        + " with it.")
                .ifSomethingGoesWrong("It says \"jobs are kept by the Automation Engine; install it on the Mainframe"
                        + " first\".",
                        "Install the Automation Engine on the Mainframe, then type the job again.",
                        "It says \"the network's engine keeps no views, procedures or jobs\".",
                        "Use an engine that keeps saved objects, such as the Midsoft IQL Server"
                                + " ([](jsc:engines)).",
                        "It says \"CREATE JOB needs a trigger: EVERY <duration> or WHEN <condition>\".",
                        "Add EVERY with a duration, or WHEN with a condition, at the end of the line.",
                        "It says \"CREATE JOB needs a body before its trigger\".",
                        "Write the statement between AS and EVERY or WHEN.",
                        "It says \"CREATE JOB trigger needs a value\".",
                        "Write the duration or the condition after EVERY or WHEN.",
                        "It says \"CREATE expects VIEW, PROCEDURE, or JOB\".",
                        "Write VIEW, PROCEDURE or JOB after CREATE.",
                        "It says that CREATE needs a name and an AS body, or that AS must follow the name.",
                        "Write CREATE JOB, the name, then AS and the statement.",
                        "It says \"a name is at most 64 characters long\".",
                        "Choose a shorter name, of no more than 64 characters.",
                        "A job made in IQL with a bad interval never fires.",
                        "IQL does not check an EVERY duration when you type the job, so a bad one never fires. Write"
                                + " a number with an optional unit: s, m, h or t, such as 30s or 100t.",
                        "A job is listed as PAUSED.",
                        "Restart it with the > control in the Automation Manager.",
                        "The job fires, but nothing moves.",
                        "The job runs as ordinary Operations. Look at the Operations it started ([](jsc:operations)),"
                                + " where each one shows its status.")
                .seeAlso("jsc:automation_manager", "jsc:engines", "jsc:operations")
                .register();
    }

    private static void automationManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("automation_manager", section).titled("The Automation Manager")
                .paragraph("The Automation Manager is the window where you write and watch the jobs, without typing"
                        + " IQL. It is a desktop program, so you open it from the desktop, like the other programs"
                        + " ([](jsc:programs)).")
                .paragraph("It runs on Frames, Linux and FreeBSD. It does not run on UNIX System V. It needs a"
                        + " Mainframe on the network: without one, the window says \"no Mainframe\".")
                .table("The parts of the window", "Part", "What it does")
                .row(GuideCell.words("The top band"),
                        GuideCell.words("Green, with the engine's name and \"online\", while the Automation"
                                + " Engine runs; otherwise amber, with \"No engine - install the Automation Engine"
                                + " on the Mainframe\"."))
                .row(GuideCell.words("The job count"),
                        GuideCell.words("How many jobs the network keeps, such as 3 jobs."))
                .row(GuideCell.words("The job list"), GuideCell.words("One row for each job, with the columns below."))
                .row(GuideCell.words("NEW JOB"), GuideCell.words("The form that writes a new job, in four types."))
                .subheading("The job list")
                .table("The columns of the job list", "Column", "What it shows")
                .row(GuideCell.words("JOB"), GuideCell.words("The job's name."))
                .row(GuideCell.words("TYPE"), GuideCell.words("The kind of job: Restock below, Batch Craft, Periodic"
                        + " Move or Custom."))
                .row(GuideCell.words("TRIGGER"), GuideCell.words("The interval, such as every 5m, or the condition."))
                .row(GuideCell.words("ACT"), GuideCell.words("= pauses the job, > resumes a paused job, and x deletes"
                        + " the job at once, with no question."))
                .paragraph("The window works out the kind of a job from its statement and trigger, so a job you type"
                        + " in IQL is listed the same way. A statement that starts with CRAFT is Restock below when its"
                        + " trigger is WHEN, and Batch Craft otherwise. A statement that starts with neither CRAFT nor"
                        + " MOVE is Custom.")
                .subheading("Making a job")
                .steps("Type a name in Name. Use one word, with no spaces.",
                        "Choose the type: Restock below, Batch Craft, Move or IQL.",
                        "Fill the fields that the type shows.",
                        "Click Create job. The fields clear, and the new job appears in the list.")
                .table("The four types", "Type", "Fields", "What the job does")
                .row(GuideCell.words("Restock below"), GuideCell.words("Name, Item id, When below"),
                        GuideCell.words("Crafts the amount of the item when the network's stock of it falls below the"
                                + " amount."))
                .row(GuideCell.words("Batch Craft"), GuideCell.words("Name, Item id, Amount, Every"),
                        GuideCell.words("Crafts the amount of the item every interval."))
                .row(GuideCell.words("Move"), GuideCell.words("Name, Item (blank=all), Amount, From, To, Every"),
                        GuideCell.words("Moves the amount of the item from one place to the other every interval; a"
                                + " blank item moves everything."))
                .row(GuideCell.words("IQL"), GuideCell.words("Name, Script (on Mainframe disk), Every"),
                        GuideCell.words("Runs one .iql file from the Mainframe's system disk every interval."))
                .paragraph("The Script field lists the .iql files on the Mainframe's system disk. If the disk has none,"
                        + " the field says \"no .iql files on the Mainframe's system disk\".")
                .paragraph("A Restock below job counts the stock that is in the network now. The window adds the note"
                        + " \"Does not count what is already on its way.\" It also shows a tip"
                        + " that starts \"Prophet YourIQL counts it: KEEP\", with the item and the amount."
                        + " Prophet is explained in"
                        + " ([](jsc:prophet_youriql)).")
                .paragraph("The amount takes digits only, and a blank amount counts as 1 item. An amount below 1 item"
                        + " becomes 1 item."
                        + " The Every field takes an interval such as 30s or 5m, and it must be more than zero. The"
                        + " forms are the ones in ([](jsc:automation_engine)).")
                .paragraph("Creating a job with the name of an existing job replaces that job with the new one.")
                .ifSomethingGoesWrong("The chat says \"jobs are kept by the Automation Engine; install it on the"
                        + " Mainframe first\".",
                        "Install the Automation Engine on the Mainframe.",
                        "The chat says \"Give the job a name.\"",
                        "Type a name in Name.",
                        "The chat says \"Restock below needs an item.\"",
                        "Type an Item id, such as iron_ingot.",
                        "The chat says \"Batch Craft needs an item and a valid interval (e.g. 30s, 5m).\"",
                        "Type an item and an interval such as 30s or 5m.",
                        "The chat says \"Periodic Move needs FROM, TO, and a valid interval (e.g. 30s).\"",
                        "Type the From and To places, and an interval such as 30s.",
                        "The chat says \"An IQL Script job needs a .iql file and a valid interval (e.g. 30s).\"",
                        "Choose a script in the list, and type an interval such as 30s.",
                        "The chat says that the script is not found on the Mainframe disk.",
                        "Save the .iql file on the Mainframe's system disk, then choose it.",
                        "The band says \"No engine\" although the engine is installed.",
                        "The band shows the engine only while the Mainframe runs. Turn the Mainframe on.")
                .seeAlso("jsc:automation_engine", "jsc:programs")
                .register();
    }

    private static void redstonePrograms(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("redstone_programs", section).titled("Redstone in programs")
                .icon(ComputingModule.STANDARD_REDSTONE_INTERFACE)
                .paragraph("A program can read a redstone signal, or make one, through a Redstone Interface"
                        + " ([](jsc:redstone_interfaces)). You place the interface, cable it to your computer,"
                        + " and let a program read it or set it.")
                .paragraph("Name the interface in its window first. A program finds the interface by that"
                        + " name. The name takes up to 32 characters, and it must be unique on its computer,"
                        + " whatever the capitals. An empty name answers to \"Redstone Interface\".")
                .paragraph("If you name a second interface with a name the first one has, the window refuses it with"
                        + " \"Another interface already has this name\".")
                .subheading("From IQL")
                .paragraph("The SET REDSTONE statement makes an interface read or emit. The IQL book explains"
                        + " it in full ([](jsc:set_redstone)).")
                .table("SET REDSTONE statements", "Statement", "What it does")
                .row(GuideCell.code("SET REDSTONE 'Gate' IN"), GuideCell.words("Makes the interface named Gate read."))
                .row(GuideCell.code("SET REDSTONE 'Gate' OUT 15"),
                        GuideCell.words("Makes the interface named Gate emit a strength of 15."))
                .row(GuideCell.code("SET REDSTONE 'Main Gate' IN"),
                        GuideCell.words("The same, for a name with a space. Quotes go round the name."))
                .paragraph("The strength runs from 0 to 15. A statement typed at a prompt marks the setting as made by"
                        + " IQL, and the window then shows \"Set by IQL\".")
                .subheading("From Sigma")
                .paragraph("A Sigma program reaches an interface through redstone(\"Gate\"). The Sigma book"
                        + " explains its library ([](jsc:redstone_library)).")
                .table("Sigma calls on an interface", "Call", "What it does")
                .row(GuideCell.code("redstone(\"Gate\")"),
                        GuideCell.words("Finds the interface named Gate on the computer the program runs on."))
                .row(GuideCell.code(".In()"), GuideCell.words("Makes the interface read."))
                .row(GuideCell.code(".Out(15)"), GuideCell.words("Makes the interface emit 15, from 0 to 15."))
                .row(GuideCell.code(".Level()"),
                        GuideCell.words("Returns the strength it reads or emits, or 0 while it has no power."))
                .listing("Lamp.sgs", "redstone(\"Gate\").Out(15);")
                .paragraph("Each call marks the setting with the name of the program that made it, so the window shows"
                        + " that program.")
                .subheading("Limits")
                .paragraph("A program reaches only the interfaces on its own computer. An interface on another computer"
                        + " is not found, even when both are on the same network.")
                .paragraph("A SET REDSTONE statement in a job runs each time the job fires, so a job can open"
                        + " a door on a timer ([](jsc:automation_engine)).")
                .ifSomethingGoesWrong("IQL says \"no Redstone Interface named Gate is linked to this machine\".",
                        "Check the spelling of the name in the interface's window. Check that its cable reaches this"
                                + " computer, and that the interface is not disabled.",
                        "Sigma stops the program and says the interface is not linked to this machine.",
                        "Check the name, and check the cable and the disabled setting, as above.",
                        "IQL says \"SET REDSTONE needs IN, or OUT and a strength from 0 to 15\".",
                        "Write IN, or OUT followed by a number from 0 to 15.",
                        "IQL says the strength is not a redstone strength, which runs from 0 to 15.",
                        "Use a number from 0 to 15.",
                        "Sigma stops the program, because the strength is not from 0 to 15.",
                        "Pass a number from 0 to 15 to Out.",
                        "The window says \"Off, no power\".",
                        "The interface is not live. Check its peripheral cable, and check that the interface"
                                + " is not disabled.",
                        "The window refuses a name with \"Another interface already has this name\".",
                        "Give this interface another name.")
                .seeAlso("jsc:redstone_interfaces", "jsc:set_redstone", "jsc:redstone_library",
                        "jsc:peripheral_cables")
                .register();
    }

    private static void clusterTools(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("cluster_tools").titled("Running many machines")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER).in(chapter).register();
        clusterManager(guide, section);
        clusterCommand(guide, section);
        clusterInstalls(guide, section);
    }

    private static void clusterManager(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cluster_manager", section).titled("The Cluster Manager")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER)
                .paragraph("The Cluster Manager is the window that lets you run many machines as one. It is a desktop"
                        + " program for a Cluster Management Computer ([](jsc:cluster_management_computers)). It needs"
                        + " a Cluster Interface Card in that computer ([](jsc:expansion_cards)).")
                .paragraph("Each machine in a cluster is a node. Which clusters you reach depends on the card's"
                        + " reach.")
                .define("Cluster", "A group of machines that the Cluster Manager runs as one unit: a supercomputer,"
                        + " or one section of a datacenter.")
                .define("Node", "One machine in a cluster, which the Cluster Manager can power, and install a system"
                        + " or a program on.")
                .define("Reach", "What a Cluster Interface Card can drive: the datacenter servers, supercomputers and"
                        + " artificial intelligence (AI) clusters, depending on the card.")
                .paragraph("The window runs on Frames, Linux, FreeBSD and UNIX System V. Without a card, it says"
                        + " \"A Cluster Interface Card is required.\"")
                .table("The parts of the window", "Part", "What it does")
                .row(GuideCell.words("Top tabs"), GuideCell.words("Supercomputers, Datacenters and AI. Each tab lists"
                        + " the clusters of its kind."))
                .row(GuideCell.words("Supercomputer tabs"), GuideCell.words("NODES, CLUSTER MAP and QUEUE."))
                .row(GuideCell.words("Datacenter tabs"), GuideCell.words("SERVERS and INVENTORY."))
                .row(GuideCell.words("Footer buttons"), GuideCell.words("SYSTEM ALL, PROGRAM ALL, ALL ON or ALL OFF,"
                        + " and CANCEL JOB, explained below."))
                .table("The footer buttons", "Button", "What it does")
                .row(GuideCell.words("SYSTEM ALL"), GuideCell.words("Installs a system on every node of the cluster,"
                        + " from a disc in a linked drive ([](jsc:cluster_installs))."))
                .row(GuideCell.words("PROGRAM ALL"), GuideCell.words("Installs a program on every node, from a program"
                        + " disc in a linked drive."))
                .row(GuideCell.words("ALL ON or ALL OFF"), GuideCell.words("Switches every node of the cluster on or"
                        + " off. It installs nothing."))
                .row(GuideCell.words("CANCEL JOB"), GuideCell.words("Stops the running install after the nodes being"
                        + " written."))
                .paragraph("Each node row has its own buttons. SYSTEM and PROGRAM install on that node alone. POWER ON"
                        + " and POWER OFF only switch the node.")
                .subheading("Datacenter sections")
                .paragraph("A datacenter section is the set of servers behind one face of a Server Router"
                        + " ([](jsc:server_router)). The BALANCE button cycles the router's load-balance mode, from"
                        + " ROUND-ROBIN to LEAST-LOADED, then MANUAL, and back again. The modes are explained in"
                        + " ([](jsc:load_balancing)).")
                .paragraph("Open INVENTORY to see what the section holds. The hint in the inventory says that you"
                        + " click an"
                        + " item to move it out, and drop a stack on the window to deposit it.")
                .subheading("Names")
                .paragraph("Press RENAME and type a name. An empty name goes back to the default. Supercomputers are"
                        + " named SC-1, SC-2 and onward, in the order the network registered them. A section takes its"
                        + " router's name and the face it sits on, such as Router · NORTH.")
                .subheading("Node states")
                .paragraph("Each node row shows its state in capitals. NO SYSTEM means the node has no system"
                        + " installed. BAY OFF means the node is switched off. INSTALLING means a system or a"
                        + " program is being written to it now.")
                .ifSomethingGoesWrong("The window says \"A Cluster Interface Card is required.\"",
                        "Install a Cluster Interface Card in the computer ([](jsc:expansion_cards)).",
                        "The list shows no supercomputer and no section.",
                        "The card may be too old to reach them. The table in ([](jsc:cluster_installs)) shows what"
                                + " each card reaches.",
                        "The window says \"that row is not a node this card reaches\".",
                        "The card does not reach that machine. Use a newer card for it.",
                        "The window says \"select a cluster first\".",
                        "Select a cluster in the window, then press the button again.")
                .seeAlso("jsc:cluster_command", "jsc:cluster_installs", "jsc:supercomputers", "jsc:datacenters",
                        "jsc:server_racks")
                .register();
    }

    private static void clusterCommand(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cluster_command", section).titled("The cluster command")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER)
                .paragraph("The cluster command does the same work as the Cluster Manager, for when you prefer the"
                        + " prompt. Type cluster and one of the words below at the prompt of a system on a Cluster"
                        + " Management Computer ([](jsc:cluster_management_computers)).")
                .define("R:U", "The address of one node: its rack number in the cluster's order, counted from 1, then"
                        + " its unit row, counted from 1. R2:U5 is unit row 5 of the second rack.")
                .paragraph("The command needs a Cluster Interface Card in the computer. Without one, the answer is"
                        + " \"no cluster interface card installed\". The usage line reads: list | nodes <name> | power"
                        + " <name> [R:U] on|off | install <name> system|program | status | cancel.")
                .table("The subcommands", "Command", "What it does")
                .row(GuideCell.code("list"), GuideCell.words("Lists the supercomputers and datacenter sections the card"
                        + " reaches; ls or no word at all does the same."))
                .row(GuideCell.code("nodes <name>"), GuideCell.words("Lists the nodes of one cluster, with their rack,"
                        + " unit, system and power."))
                .row(GuideCell.code("power <name> on|off"), GuideCell.words("Switches every node of the cluster on or"
                        + " off, without installing anything."))
                .row(GuideCell.code("power <name> R:U on|off"), GuideCell.words("Switches one node on or off."))
                .row(GuideCell.code("install <name> system|program"),
                        GuideCell.words("Starts a bulk install of a system or a program on the cluster"
                                + " ([](jsc:cluster_installs))."))
                .row(GuideCell.code("status"), GuideCell.words("Shows the install running now, or the last one."))
                .row(GuideCell.code("cancel"), GuideCell.words("Stops the running install once the nodes now being"
                        + " written are done."))
                .paragraph("Names are matched without regard to capitals. The install command answers with the job it"
                        + " started, how many nodes it writes at once, and a reminder to follow it with cluster status."
                        + " The status line lists the job, then how many nodes are done, writing, queued and skipped.")
                .table("The list columns", "Column", "What it shows")
                .row(GuideCell.code("KIND"), GuideCell.words("Supercomputer or datacenter."))
                .row(GuideCell.code("NAME"), GuideCell.words("The name the Cluster Manager shows for the cluster."))
                .row(GuideCell.code("STATE"), GuideCell.words("Online or offline for a supercomputer; online or empty"
                        + " for a section."))
                .row(GuideCell.code("LOAD"), GuideCell.words("The crafts in use of the supercomputer's parallel"
                        + " crafts, or the number of servers for a section."))
                .paragraph("A cluster the card cannot reach ends its row with \"(out of this card's reach)\".")
                .table("The nodes columns", "Column", "What it shows")
                .row(GuideCell.code("RACK"),
                        GuideCell.words("The rack's number, counted from 1 in the cluster's order."))
                .row(GuideCell.code("U"), GuideCell.words("The unit row, counted from 1."))
                .row(GuideCell.code("NODE"), GuideCell.words("The node's name."))
                .row(GuideCell.code("SYSTEM"), GuideCell.words("The system on the node, or none."))
                .row(GuideCell.code("POWER"), GuideCell.words("on or off."))
                .ifSomethingGoesWrong("It says \"this command only exists on a Cluster Management Computer\".",
                        "Run it on a Cluster Management Computer, not on another computer.",
                        "It says \"no cluster interface card installed\".",
                        "Install a Cluster Interface Card in the computer.",
                        "It says \"no cluster named\" followed by the name you typed.",
                        "Use a name from the list that the command shows.",
                        "It says \"no node at\" followed by an address.",
                        "Check the R:U address in the nodes listing.",
                        "It shows the usage for power.",
                        "Add the R:U address, then on or off.",
                        "It says \"already on\" or \"already off\".",
                        "The node is already in that state, or the card does not reach it.",
                        "It says \"no job running\".",
                        "Start a job with install first, or read status again after it starts.")
                .seeAlso("jsc:cluster_manager", "jsc:cluster_installs")
                .register();
    }

    private static void clusterInstalls(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("cluster_installs", section).titled("Installing on many machines")
                .icon(ComputingModule.CLUSTER_MANAGEMENT_COMPUTER)
                .paragraph("A bulk install writes one system or one program to every node of a cluster at once, or to"
                        + " the nodes you pick, instead of one machine at a time. You start it from the Cluster Manager"
                        + " ([](jsc:cluster_manager)) or with the cluster command ([](jsc:cluster_command)).")
                .paragraph("The disc comes from a drive linked to the Cluster Management Computer. Cable the"
                        + " drive to the computer with a peripheral cable ([](jsc:peripheral_cables)), and put"
                        + " the disc in the drive ([](jsc:drives)).")
                .steps("Install a Cluster Interface Card in the Cluster Management Computer.",
                        "Cable a drive to the computer, and put the disc in it.",
                        "Open the Cluster Manager, and press SYSTEM ALL or PROGRAM ALL. Or type cluster install,"
                                + " then the"
                                + " cluster's name, then system or program.",
                        "Watch the install in the window, or type cluster status.")
                .subheading("The cards and their reach")
                .paragraph("The card decides which machines a bulk install reaches, and how many nodes it writes at"
                        + " once.")
                .table("Cluster Interface Cards", "Card", "Era", "Reaches", "Nodes at once")
                .row(GuideCell.words("Serial Console Card"), GuideCell.words("Vintage"),
                        GuideCell.words("Datacenter servers only"), GuideCell.words("1"))
                .row(GuideCell.words("Management NIC"), GuideCell.words("Legacy"),
                        GuideCell.words("Datacenters and supercomputers"), GuideCell.words("2"))
                .row(GuideCell.words("Fabric Host Adapter"), GuideCell.words("Standard"),
                        GuideCell.words("Datacenters, supercomputers and AI clusters"), GuideCell.words("4"))
                .row(GuideCell.words("Fabric DPU"), GuideCell.words("Advanced"),
                        GuideCell.words("Datacenters, supercomputers and AI clusters"), GuideCell.words("8"))
                .define("Lane", "One node being written during a bulk install. The card decides how many lanes run"
                        + " at once.")
                .paragraph("Each node that is written at once takes one lane. A card placed in a slot older than the"
                        + " card was made for runs fewer lanes, because that slot carries data more slowly.")
                .paragraph("Writing one node takes 14 seconds on a machine with a clock speed of 2,000 megahertz (MHz)."
                        + " A faster machine takes less time, down to 3 seconds. A slower one takes more, up to 60"
                        + " seconds.")
                .subheading("Refusals when you start")
                .table("Messages when you start", "Message", "What to do")
                .row(GuideCell.words("no cluster interface card"),
                        GuideCell.words("Install a Cluster Interface Card in the computer."))
                .row(GuideCell.words("a job is already running"),
                        GuideCell.words("Wait for it to finish, or cancel it with CANCEL JOB or cluster cancel."))
                .row(GuideCell.words("this card does not reach that cluster"),
                        GuideCell.words("Use a card that reaches it, from the table above."))
                .row(GuideCell.words("no system disc in a linked reader"),
                        GuideCell.words("Put a system disc in a drive linked to the computer."))
                .row(GuideCell.words("no program disc in a linked reader"),
                        GuideCell.words("Put a program disc in a drive linked to the computer."))
                .row(GuideCell.words("that system installs by hand from its own shell"),
                        GuideCell.words("Install that system by hand on each node. A bulk install cannot write it."))
                .row(GuideCell.words("that disc carries nothing to install"),
                        GuideCell.words("Use a program disc that carries a program."))
                .row(GuideCell.words("no nodes in that cluster"),
                        GuideCell.words("Put machines in the cluster's racks first."))
                .subheading("Nodes the install skips")
                .paragraph("A node that cannot take the install is skipped, and the job goes on with the other nodes."
                        + " The node's name is followed by the reason in brackets.")
                .table("Skip reasons", "Reason", "What it means and what to do")
                .row(GuideCell.words("bay off"),
                        GuideCell.words("The node is switched off. Switch it on with POWER ON."))
                .row(GuideCell.words("already installed"),
                        GuideCell.words("The node already runs that system, or already has that program."))
                .row(GuideCell.words("unknown program"),
                        GuideCell.words("The install does not know the program on the disc."))
                .row(GuideCell.words("no system"),
                        GuideCell.words("The node has no system, or no console for the program."
                                + " Install a system with SYSTEM first."))
                .row(GuideCell.words("does not meet the program's requirements"),
                        GuideCell.words("The node lacks the clock speed, video memory or free system disk space that"
                                + " the program needs."))
                .row(GuideCell.words("too old for the program"),
                        GuideCell.words("The node is of an older era than the program needs."))
                .row(GuideCell.words("missing rack"), GuideCell.words("The rack no longer holds the node. Start the"
                        + " install again."))
                .row(GuideCell.words("changed while writing"),
                        GuideCell.words("The node changed while its lane was writing it."))
                .subheading("How the job ends")
                .paragraph("A system lane installs the system and sets the node to restart into it. A program lane"
                        + " installs the program on the node. When the job is over, it leaves one summary line.")
                .table("Summary lines", "Line", "When it appears")
                .row(GuideCell.code("done: <what> on <nodes>"), GuideCell.words("The install finished."))
                .row(GuideCell.code("cancelled: <what> on <nodes>"), GuideCell.words("You cancelled the job."))
                .row(GuideCell.code(", <n> skipped"),
                        GuideCell.words("Added to the end of the line above when some nodes were skipped."))
                .paragraph("Cancel stops the job after the nodes being written. The nodes still in the queue"
                        + " are left as they were. The ALL ON and ALL OFF buttons, and the power command, only"
                        + " switch power. They never start an install.")
                .ifSomethingGoesWrong("The install does not start, and it says \"a job is already running\".",
                        "Wait for the job to finish, or cancel it with CANCEL JOB.",
                        "Every node is skipped.",
                        "Read the reason in brackets after each node, and fix that reason. The table above lists them.",
                        "The install is gone after the world loads again.",
                        "Start the install again. A job does not survive a reload.")
                .seeAlso("jsc:cluster_manager", "jsc:cluster_command", "jsc:cluster_management_computers",
                        "jsc:server_racks")
                .register();
    }
}
