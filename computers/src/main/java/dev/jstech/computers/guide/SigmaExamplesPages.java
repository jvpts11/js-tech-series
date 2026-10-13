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
 * The pages of the examples chapter of The Book of Sigma: one complete program for each part of the library, each
 * copied exactly as it is written, with what its lines do and what can go wrong. Every program is one the compiler
 * accepts at the version its page names.
 */
final class SigmaExamplesPages {

    private SigmaExamplesPages() {
    }

    /** Declares the sections and entries of the examples chapter of The Book of Sigma, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        intro(guide, chapter);
        network(guide, chapter);
        devices(guide, chapter);
        language(guide, chapter);
        windows(guide, chapter);
    }

    private static void intro(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("examples_intro").titled("Running the examples")
                .in(chapter).register();
        guide.page("sigma_examples_intro", section).titled("Running the examples")
                .paragraph("Every example in this chapter is a whole program in Sigma#, the full language."
                        + " Each one is a single file, and you can type it in, build it and run it on a computer"
                        + " of the mod.")
                .paragraph("An example starts with using lines, which bring the library's names into the file. Its"
                        + " class has a method called Main, and the program starts there"
                        + " ([](jsc:sigma_entry_points)).")
                .subheading("Building and running an example")
                .steps("Save the program as a file that ends in .sgs, such as Buses.sgs.",
                        "Type sgsc Buses.sgs at the prompt and press [[Enter]].",
                        "Read the answer. When it says wrote Buses.asm, the program is built.",
                        "Type sigma run Buses.asm and press [[Enter]] to start the program.")
                .paragraph("The compiler writes the built program beside the source file, with the ending .asm. After a"
                        + " good build it prints wrote Buses.asm, then a line that begins run it with: sigma run"
                        + " Buses.asm. The compiler is explained in ([](jsc:sigma_compile)).")
                .subheading("The options of the compiler")
                .table("Options of sgsc", "Option", "What it does")
                .row(GuideCell.code("-o <out.asm>"),
                        GuideCell.words("Names the built file, in place of the source's name with .asm."))
                .row(GuideCell.code("--arch <isa>"),
                        GuideCell.words("Chooses the instruction set. Without it, the default jsc:x86 is used."))
                .row(GuideCell.code("--lang <version>"),
                        GuideCell.words("Chooses the language version the program is checked against: 1 or 2."))
                .paragraph("The version decides which names a program may use. A program that uses a name from"
                        + " version 2 needs version 2. The table below gives the version each example needs.")
                .table("The examples", "Example", "Source file", "Version needed")
                .row(GuideCell.words("Network"), GuideCell.code("Network.sgs"), GuideCell.words("1"))
                .row(GuideCell.words("Buses"), GuideCell.code("Buses.sgs"), GuideCell.words("2"))
                .row(GuideCell.words("Crafting"), GuideCell.code("Crafting.sgs"), GuideCell.words("2"))
                .row(GuideCell.words("Machine"), GuideCell.code("Machine.sgs"), GuideCell.words("1"))
                .row(GuideCell.words("Redstone"), GuideCell.code("Redstone.sgs"), GuideCell.words("2"))
                .row(GuideCell.words("Sound"), GuideCell.code("Sound.sgs"), GuideCell.words("2"))
                .row(GuideCell.words("Gateway"), GuideCell.code("Gateway.sgs"), GuideCell.words("1"))
                .row(GuideCell.words("Values"), GuideCell.code("Values.sgs"), GuideCell.words("1"))
                .row(GuideCell.words("Steps"), GuideCell.code("Steps.sgs"), GuideCell.words("1"))
                .row(GuideCell.words("Programs"), GuideCell.code("Programs.sgs"), GuideCell.words("1"))
                .row(GuideCell.words("Old names"), GuideCell.code("OldNames.sgs"), GuideCell.words("2"))
                .row(GuideCell.words("Windows"), GuideCell.code("Ui.sgs"), GuideCell.words("1"))
                .row(GuideCell.words("Widgets"), GuideCell.code("Widgets.sgs"), GuideCell.words("2"))
                .define("Language version", "A numbered stage of Sigma#. A later stage adds names, and a program that"
                        + " uses one of them needs that stage.")
                .ifSomethingGoesWrong("The compiler prints an error and says nothing was written.",
                        "Fix the first error it prints, then build the file again. A file with an error gets no"
                                + " .asm file.",
                        "The message reads \"'abc' is not a version of Σ#; there are 1 to 2\", because the text after"
                                + " --lang is not a version.",
                        "Type 1 or 2 after --lang.",
                        "The compiler refuses a name that the chosen version does not have, with code S3057.",
                        "Build with the version the name needs, or take the name out of the program.",
                        "The compiler says a name is in a namespace and asks for a using line, with code S3040.",
                        "Add the using line it asks for above the namespace line, then build again.")
                .seeAlso("jsc:sigma_compile", "jsc:sigma_run", "jsc:sigma_errors")
                .register();
    }

    private static void network(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("examples_network").titled("The network")
                .in(chapter).register();
        sigmaExampleNetwork(guide, section);
        sigmaExampleBuses(guide, section);
        sigmaExampleCrafting(guide, section);
    }

    private static void sigmaExampleNetwork(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_network", section).titled("Network")
                .paragraph("This example reads the network: how much it holds, which servers keep an item, and which"
                        + " computers are on it. It also watches a stock, asks the network for jobs and starts a"
                        + " program on another computer.")
                .paragraph("The network is every computer and device joined by data cable to one Mainframe"
                        + " ([](jsc:what_a_network_is)). The example reads it through the library's Network type,"
                        + " and asks the Mainframe through its Mainframe type.")
                .subheading("The program")
                .listing("Network.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Collections.*;",
                        "using System.Network.*;",
                        "using System.Operations.*;",
                        "using System.Execution.*;",
                        "namespace Corpus;",
                        "class NetworkCorpus {",
                        "    static void Main() {",
                        "        bool online = Network.Online;",
                        "        string current = Network.Current;",
                        "        long stored = Network.Capacity + Network.Used + "
                                + "Network.Total(\"minecraft:iron_ingot\");",
                        "        List<string> types = Network.Types();",
                        "        Console.PrintLine(\"\" + online + current + stored + types.Count);",
                        "        foreach (HoldingInfo holding in Network.Find(\"minecraft:iron_ingot\")) {",
                        "            Console.PrintLine(holding.Server + holding.Quantity);",
                        "        }",
                        "        foreach (ServerInfo server in Network.Servers()) {",
                        "            Console.PrintLine(server.Name + server.Stored + server.Capacity);",
                        "        }",
                        "        Subscription any = Network.Watch(\"minecraft:iron_ingot\", Told);",
                        "        Subscription below = Network.WatchBelow(\"minecraft:iron_ingot\", 10, Told);",
                        "        Subscription above = Network.WatchAbove(\"minecraft:iron_ingot\", 100, Told);",
                        "        Console.PrintLine(\"\" + any.Id + any.Item + below.Id + above.Id);",
                        "",
                        "        RemoteComputer desk = Network.Computer(\"desk\");",
                        "        List<RemoteComputer> all = Network.Computers();",
                        "        Console.PrintLine(desk.Host + desk.Name + desk.Type + desk.Os + desk.Online + "
                                + "all.Count);",
                        "        List<string> args = new List<string>();",
                        "        Process one = desk.Start(\"C:\\\\tool.asm\");",
                        "        Process two = desk.Start(\"C:\\\\tool.asm\", args);",
                        "        Process three = desk.Start(\"C:\\\\tool.asm\", args, \"lab\");",
                        "        List<string> shell = desk.Shell(\"dir\");",
                        "        bool sent = desk.Send(one.Id, \"hi\");",
                        "        List<Process> processes = desk.Processes();",
                        "        Console.PrintLine(\"\" + two.Id + three.Id + shell.Count + sent + processes.Count);",
                        "",
                        "        IqlResult result = Iql.Run(\"QUERY items\");",
                        "        List<Map<string, object>> rows = Iql.Query(\"QUERY items\");",
                        "        IqlResult exec = Iql.Exec(\"restock\");",
                        "        IqlResult execWith = Iql.Exec(\"restock\", args);",
                        "        IqlResult file = Iql.RunFile(\"C:\\\\job.iql\");",
                        "        Console.PrintLine(\"\" + result.Ok + result.Message + result.Rows.Count + "
                                + "rows.Count);",
                        "        Console.PrintLine(\"\" + exec.Ok + execWith.Ok + file.Ok);",
                        "",
                        "        bool mainframe = Mainframe.Online;",
                        "        int peak = Mainframe.PeakToday;",
                        "        WorkStat stat = Mainframe.Stats(\"PULL\");",
                        "        List<WorkStat> work = Mainframe.Work();",
                        "        Console.PrintLine(\"\" + mainframe + peak + stat.Type + stat.Count"
                                + " + stat.AverageWait + stat.AverageRun);",
                        "        Console.PrintLine(\"\" + stat.ShortfallPercent + stat.Moved + work.Count);",
                        "",
                        "        AskResult pulled = Operations.Pull(\"minecraft:oak_log\", 64);",
                        "        AskResult pushed = Operations.Push(\"minecraft:oak_log\", 64);",
                        "        AskResult crafted = Operations.Craft(\"minecraft:stick\", 4);",
                        "        AskResult cancelled = Operations.Cancel(\"op-1\");",
                        "        AskResult raised = Operations.Reprioritise(\"op-1\", \"HIGH\");",
                        "        OperationInfo info = Operations.Get(\"op-1\");",
                        "        List<OperationInfo> list = Operations.List();",
                        "        Console.PrintLine(\"\" + pulled.Ok + pulled.Message + pushed.Ok + crafted.Ok"
                                + " + cancelled.Ok + raised.Ok);",
                        "        Console.PrintLine(info.Id + info.Type + info.Item + info.Moved + info.Requested"
                                + " + info.Status + info.Priority);",
                        "        Console.PrintLine(\"\" + list.Count);",
                        "    }",
                        "",
                        "    static void Told(StockEvent told) {",
                        "        Console.PrintLine(told.Item + told.Total + told.Previous);",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("Network.Online"), GuideCell.words("Tells whether this computer is on a network."))
                .row(GuideCell.code("Network.Capacity, Used, Total(...)"),
                        GuideCell.words("How much the network can hold, how much it holds, and how much of one item."))
                .row(GuideCell.code("Network.Types()"), GuideCell.words("The kinds of item the network holds."))
                .row(GuideCell.code("Network.Find(...)"),
                        GuideCell.words("Each server that holds the item, with the quantity it holds."))
                .row(GuideCell.code("Network.Servers()"),
                        GuideCell.words("Each server with its name, what it stores and its capacity."))
                .row(GuideCell.code("Network.Watch, WatchBelow, WatchAbove"),
                        GuideCell.words("Ask to be told when a stock changes, or goes below or above a number."))
                .row(GuideCell.code("Network.Computer(...)"),
                        GuideCell.words("Finds one computer on the network by its name."))
                .row(GuideCell.code("Network.Computers()"), GuideCell.words("Lists every computer on the network."))
                .row(GuideCell.code("desk.Start(...)"),
                        GuideCell.words("Starts a program on that computer, with or without arguments."))
                .row(GuideCell.code("desk.Shell(...)"),
                        GuideCell.words("Runs one line at that computer's prompt, and gives back what it printed."))
                .row(GuideCell.code("desk.Send(...)"), GuideCell.words("Sends a text to one of its processes."))
                .row(GuideCell.code("desk.Processes()"), GuideCell.words("Lists its processes."))
                .row(GuideCell.code("Iql.Run(...)"),
                        GuideCell.words("Runs one statement of the network's query language (IQL). It does not stop"
                                + " the program when the statement is refused."))
                .row(GuideCell.code("Iql.Query(...)"),
                        GuideCell.words("Runs a query and gives back its rows. It stops the program when the query is"
                                + " refused."))
                .row(GuideCell.code("Iql.Exec(...)"),
                        GuideCell.words("Runs a procedure by its name, with or without a list of arguments."))
                .row(GuideCell.code("Iql.RunFile(...)"), GuideCell.words("Runs each statement of a file."))
                .row(GuideCell.code("Mainframe.Online, PeakToday"),
                        GuideCell.words("Whether the Mainframe is online, and its peak for the day."))
                .row(GuideCell.code("Mainframe.Stats(...), Work()"),
                        GuideCell.words("The work done for one kind, and the work done for every kind."))
                .row(GuideCell.code("Operations.Pull, Push, Craft"),
                        GuideCell.words("Ask the network for a job of one kind. Each one answers with an Ok flag"
                                + " and a Message."))
                .row(GuideCell.code("Operations.Cancel, Reprioritise"),
                        GuideCell.words("Cancel a job, or change its priority."))
                .row(GuideCell.code("Operations.Get, List()"),
                        GuideCell.words("Read one job, or every job."))
                .paragraph("Watching is better than asking. The handler Told is called when the stock changes, so the"
                        + " program does not have to ask every tick. Told prints the item, the total and the stock"
                        + " before the change.")
                .define("Holding", "The stock of one item that one server holds, with the quantity.")
                .define("Subscription", "A request for the network to call a handler when a stock changes.")
                .define("Ask", "A request sent to the network. It answers whether it was taken, and why not, without"
                        + " stopping the program.")
                .define("IQL", "The network's query language: short statements, written as text, that read or change"
                        + " the network.")
                .ifSomethingGoesWrong("Online is false: this computer is not on a network.",
                        "Join the computer to the network with a data cable to a Mainframe"
                                + " ([](jsc:first_network)).",
                        "A query is refused, and the program stops with a message from the network's engine.",
                        "Read the message, or use Iql.Run, which does not stop on a refused statement.",
                        "Get gives null for a job that has finished.",
                        "Test the answer for null before you read its fields.")
                .seeAlso("jsc:iql", "jsc:mainframes", "jsc:sigma_library")
                .register();
    }

    private static void sigmaExampleBuses(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_buses", section).titled("Buses")
                .paragraph("A bus is a part of the network that moves items, and each bus has a name. This example"
                        + " finds two buses by their names and sets them, one call at a time.")
                .paragraph("Each setting gives back the same bus, so the calls chain: one setting is followed by the"
                        + " next on the same line. The buses themselves, with their filters and tags, are explained"
                        + " in ([](jsc:buses)).")
                .subheading("The program")
                .listing("Buses.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Network.*;",
                        "namespace Corpus;",
                        "class BusCorpus {",
                        "    static void Main() {",
                        "        Bus ore = bus(\"Ore in\");",
                        "        if (ore != null) {",
                        "            ore.On().Continuous().Only(\"iron_ore, coal\").Keep(16).Max(64).Priority(5);",
                        "            ore.AllBut(\"dirt\").Tag(\"c:ores\").Fuzzy(true).WhenStock(\"iron_ore\", 512)"
                                + ".WhenStockTag(\"c:ores\", 4096);",
                        "            ore.Between(18, 6).After(Bus.Named(\"Coal in\")).OnDemand().Off();",
                        "            BusItem iron = ore.Item(\"iron_ore\").Keep(16).Max(64);",
                        "            Console.PrintLine(ore.Name + iron.Bus + iron.Item);",
                        "        }",
                        "        Bus wall = Bus.Named(\"Chest wall\");",
                        "        if (wall != null) {",
                        "            wall.ReadWrite().ReadOnly().WriteOnly();",
                        "        }",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("bus(\"Ore in\")"),
                        GuideCell.words("Finds the bus called Ore in, or gives null when no bus has that name."))
                .row(GuideCell.code("if (ore != null)"),
                        GuideCell.words("Checks that the bus was found before any setting is used."))
                .row(GuideCell.code("On(), Continuous(), Only(...)"),
                        GuideCell.words("Powers the bus, keeps it moving, and accepts only the listed items."))
                .row(GuideCell.code("Keep(16), Max(64), Priority(5)"),
                        GuideCell.words("Sets how many to keep, the most to move, and the priority number."))
                .row(GuideCell.code("AllBut(...), Tag(...), Fuzzy(true)"),
                        GuideCell.words("Refuses the listed items, adds a tag, and turns on fuzzy matching of items."))
                .row(GuideCell.code("WhenStock(\"iron_ore\", 512)"),
                        GuideCell.words("Acts only when the stock of iron ore reaches 512."))
                .row(GuideCell.code("WhenStockTag(\"c:ores\", 4096)"),
                        GuideCell.words("Acts only when the stock of the tag reaches 4,096."))
                .row(GuideCell.code("Between(18, 6)"),
                        GuideCell.words("Sets two hours of the day, each from 0 to 23."))
                .row(GuideCell.code("After(Bus.Named(\"Coal in\"))"),
                        GuideCell.words("Runs this bus after the bus called Coal in."))
                .row(GuideCell.code("OnDemand(), Off()"),
                        GuideCell.words("Makes the bus move only when the network asks, and turns it off."))
                .row(GuideCell.code("ore.Item(\"iron_ore\")"),
                        GuideCell.words("Gives the settings of one item on the bus, so Keep and Max can be set for"
                                + " that item alone."))
                .row(GuideCell.code("Console.PrintLine(...)"),
                        GuideCell.words("Prints the bus name, then the bus and the item that these settings"
                                + " belong to."))
                .row(GuideCell.code("wall.ReadWrite(), ReadOnly(), WriteOnly()"),
                        GuideCell.words("Shows the three access settings, one after another, on the second bus."))
                .ifSomethingGoesWrong("The name gives nothing: no bus has that name, so the bus is null.",
                        "Check the spelling of the name, and test for null before you use the bus.",
                        "Between gets an hour outside 0 to 23, and the program stops with a message such as \"25 is"
                                + " not an hour of the day\".",
                        "Give hours from 0 to 23 only.",
                        "A bus cannot take some settings, and the program stops on that line with the bus's message.",
                        "Remove that line, or use a bus that takes the setting.")
                .seeAlso("jsc:buses", "jsc:sigma_library")
                .register();
    }

    private static void sigmaExampleCrafting(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_crafting", section).titled("Crafting")
                .paragraph("Crafting is the network's work of making items from patterns. This example sets up two"
                        + " crafting interfaces and two routers, then asks the network to smelt raw iron.")
                .paragraph("The network's own language, IQL, carries crafting too ([](jsc:iql)). The words of that"
                        + " language are not needed here: this program uses only the library's calls.")
                .paragraph("Patterns and jobs are explained in ([](jsc:how_autocrafting_works)). Routers are explained"
                        + " in ([](jsc:input_routers_and_receiving_buses)).")
                .subheading("The program")
                .listing("Crafting.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Network.*;",
                        "using System.Operations.*;",
                        "namespace Corpus;",
                        "class CraftingCorpus {",
                        "    static void Main() {",
                        "        CraftInterface kiln = craftInterface(\"Kiln\");",
                        "        if (kiln != null) {",
                        "            kiln.Exclusive(true).MaxJobs(4).Pause();",
                        "            kiln.Resume();",
                        "            Console.PrintLine(kiln.Name);",
                        "        }",
                        "        CraftRouter north = craftRouter(\"North\");",
                        "        if (north != null) {",
                        "            north.Only(\"gravel\").Fuzzy(false);",
                        "            north.AllBut(\"dirt\");",
                        "            north.Tag(\"c:sands\");",
                        "            north.Any();",
                        "            Console.PrintLine(north.Name);",
                        "        }",
                        "        CraftInterface mixer = CraftInterface.Named(\"Mixer\");",
                        "        CraftRouter west = CraftRouter.Named(\"West\");",
                        "        if (mixer != null && west != null) {",
                        "            mixer.Route(\"Coarse dirt\", \"gravel\", west);",
                        "            mixer.Route(\"Coarse dirt\", \"dirt\", null);",
                        "        }",
                        "        AskResult smelted = Operations.Update(\"minecraft:raw_iron\", \"SMELT\", 8);",
                        "        Console.PrintLine(smelted.Message);",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("craftInterface(\"Kiln\")"),
                        GuideCell.words("Finds the crafting interface by the name its window gives it, or gives"
                                + " null when none has that name."))
                .row(GuideCell.code("Exclusive(true), MaxJobs(4), Pause()"),
                        GuideCell.words("Keeps the interface for this program, allows four jobs at once, and stops"
                                + " the interface taking jobs."))
                .row(GuideCell.code("Resume()"), GuideCell.words("Lets the interface take jobs again."))
                .row(GuideCell.code("craftRouter(\"North\")"),
                        GuideCell.words("Finds a router by its name, in the same way."))
                .row(GuideCell.code("Only(\"gravel\"), Fuzzy(false)"),
                        GuideCell.words("Sets the router to accept only gravel, and turns fuzzy matching off."))
                .row(GuideCell.code("AllBut(\"dirt\"), Tag(\"c:sands\")"),
                        GuideCell.words("Sets the router to refuse dirt, and to accept the items of the tag c:sands."))
                .row(GuideCell.code("Any()"), GuideCell.words("Makes the router take anything."))
                .row(GuideCell.code("CraftInterface.Named, CraftRouter.Named"),
                        GuideCell.words("Find the interface Mixer and the router West by their names."))
                .row(GuideCell.code("Route(\"Coarse dirt\", \"gravel\", west)"),
                        GuideCell.words("Sends the gravel input of the Coarse dirt pattern through the router West."))
                .row(GuideCell.code("Route(\"Coarse dirt\", \"dirt\", null)"),
                        GuideCell.words("Routes the dirt input by the routers' own filters, since no router is given."))
                .row(GuideCell.code("Operations.Update(...)"),
                        GuideCell.words("Asks the network to smelt eight raw iron, and gives back an answer with Ok and"
                                + " Message."))
                .row(GuideCell.code("Console.PrintLine(...)"), GuideCell.words("Prints the message of that answer."))
                .define("Pattern", "A stored description of an item the network can make, and of the items it"
                        + " takes to make it.")
                .define("Job", "One crafting task that the network carries out, such as making one item.")
                .define("Router", "A filter that sends an input of a pattern on to a crafting cable.")
                .ifSomethingGoesWrong("A name gives nothing: no interface or router has that name, so the value is"
                        + " null.",
                        "Use the name the interface's window shows, and test for null before you use it.",
                        "Operations.Update runs on a computer with no network, and the program stops with \"this"
                                + " computer is not on a network\".",
                        "Run the program on a computer that is on a network.",
                        "A Route has no router, so the input follows the routers' filters instead of one router.",
                        "Give the Route a router when you want to choose the path yourself.",
                        "A setting is refused, and the program stops with the message of the interface or the router.",
                        "Remove that line, or use an interface or router that takes the setting.")
                .seeAlso("jsc:iql", "jsc:how_autocrafting_works", "jsc:sigma_library")
                .register();
    }

    private static void devices(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("examples_devices").titled("Devices")
                .in(chapter).register();
        sigmaExampleMachine(guide, section);
        sigmaExampleRedstone(guide, section);
        sigmaExampleSound(guide, section);
        sigmaExampleGateway(guide, section);
    }

    private static void sigmaExampleMachine(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_machine", section).titled("Machine")
                .paragraph("This example reads the machine it runs on: its name, its processor, its system, its memory,"
                        + " its disks and its processes. It also reads and writes files, with the paths the shell"
                        + " uses.")
                .paragraph("The records the machine gives are pictures, taken when the program asks. A program that"
                        + " needs the state again asks again, and keeps what it was told in the meantime. If you"
                        + " change the machine, ask again to see the new state.")
                .subheading("The program")
                .listing("Machine.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Collections.*;",
                        "using System.Machine.*;",
                        "namespace Corpus;",
                        "class MachineCorpus {",
                        "    static void Main() {",
                        "        bool there = File.Exists(\"a.txt\");",
                        "        string text = File.Read(\"a.txt\");",
                        "        if (File.TryRead(\"a.txt\", out string held)) {",
                        "            Console.PrintLine(held);",
                        "        }",
                        "        bool wrote = File.Write(\"a.txt\", \"x\") && File.Append(\"a.txt\", \"y\");",
                        "        bool tidied = File.Delete(\"a.txt\") && File.MkDir(\"d\");",
                        "        foreach (string entry in File.List(\"C:\\\\\")) {",
                        "            Console.PrintLine(entry);",
                        "        }",
                        "        Console.PrintLine(\"\" + there + text + wrote + tidied);",
                        "",
                        "        string name = Computer.Name;",
                        "        CpuInfo cpu = Computer.Cpu;",
                        "        OsInfo os = Computer.Os;",
                        "        int ram = Computer.RamMb + Computer.FreeRamMb;",
                        "        bool online = Computer.Online;",
                        "        Console.PrintLine(name + cpu.Mhz + cpu.Cores + cpu.Era + cpu.Architecture",
                        "            + os.Id + os.Name + ram + online);",
                        "        foreach (DiskInfo disk in Computer.Disks()) {",
                        "            Console.PrintLine(disk.Mount + disk.UsedMb + disk.CapacityMb);",
                        "        }",
                        "        List<string> programs = Computer.Programs();",
                        "        foreach (ProcessInfo process in Computer.Processes()) {",
                        "            Console.PrintLine(\"\" + process.Id + process.Name + process.State"
                                + " + process.HeldBytes + programs.Count);",
                        "        }",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("File.Exists(\"a.txt\")"), GuideCell.words("Tells whether the file is there."))
                .row(GuideCell.code("File.Read(\"a.txt\")"),
                        GuideCell.words("Reads the whole file as text. The program stops when the file is not there."))
                .row(GuideCell.code("File.TryRead(\"a.txt\", out held)"),
                        GuideCell.words("Reads the file and answers true when it could. The text goes to held."))
                .row(GuideCell.code("File.Write, File.Append"),
                        GuideCell.words("Write text to the file, or add text to its end. Each answers true when it"
                                + " happened."))
                .row(GuideCell.code("File.Delete, File.MkDir"),
                        GuideCell.words("Delete a file, or make a folder. Each answers true when it happened."))
                .row(GuideCell.code("File.List(...)"), GuideCell.words("Lists the entries of a folder, one at a time."))
                .row(GuideCell.code("Computer.Name, Cpu, Os"),
                        GuideCell.words("The machine's name, its processor (speed, cores, era and architecture), and"
                                + " its system."))
                .row(GuideCell.code("Computer.RamMb, FreeRamMb"),
                        GuideCell.words("The memory of the machine in megabytes, and the part of it that is free."))
                .row(GuideCell.code("Computer.Disks()"),
                        GuideCell.words("Gathers each disk, with its mount, the megabytes used and its capacity."))
                .row(GuideCell.code("Computer.Programs()"), GuideCell.words("Lists the programs on the machine."))
                .row(GuideCell.code("Computer.Processes()"),
                        GuideCell.words("Gathers each process, with its number, name, state and the bytes it holds."))
                .define("Path", "The place of a file or a folder, written with the folders that hold it.")
                .define("Block", "The unit a file is read or written in. The cost of reading or writing grows with each"
                        + " block.")
                .paragraph("The processor record names its era, which is the hardware era of the machine"
                        + " ([](jsc:eras)).")
                .ifSomethingGoesWrong("File.Read is asked for a file that is not there, and the program stops.",
                        "Use File.TryRead instead, which answers false when the file is not there.",
                        "A write does not happen, because the disk is full, and the write answers false.",
                        "Check the answer of each write, and free some space on the disk.",
                        "A read or a write of a big file costs more, block by block.",
                        "Read the file once, and keep its text in a variable.")
                .seeAlso("jsc:sigma_library", "jsc:eras")
                .register();
    }

    private static void sigmaExampleRedstone(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_redstone", section).titled("Redstone")
                .paragraph("Redstone is the power signal of the world, with a strength from 0 to 15. A Redstone"
                        + " Interface links the machine to the redstone of the world, so a program can read a signal"
                        + " or emit one ([](jsc:redstone_interfaces)).")
                .paragraph("This example finds an interface called Gate, sets it to read and to emit strength 15, and"
                        + " prints its name and its strength. You can match each line with the table below. It then"
                        + " finds an interface called Lever and prints its"
                        + " name.")
                .subheading("The program")
                .listing("Redstone.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Machine.*;",
                        "namespace Corpus;",
                        "class RedstoneCorpus {",
                        "    static void Main() {",
                        "        Redstone gate = redstone(\"Gate\");",
                        "        if (gate != null) {",
                        "            gate.In().Out(15);",
                        "            Console.PrintLine(gate.Name + Convert.ToString(gate.Level()));",
                        "        }",
                        "        Redstone lever = Redstone.Named(\"Lever\");",
                        "        if (lever != null) {",
                        "            Console.PrintLine(lever.Name);",
                        "        }",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("redstone(\"Gate\")"),
                        GuideCell.words("Finds the Redstone Interface called Gate. It gives null when none is linked"
                                + " under that name."))
                .row(GuideCell.code("gate.In()"), GuideCell.words("Makes the interface read the signal."))
                .row(GuideCell.code("gate.Out(15)"),
                        GuideCell.words("Makes the interface emit strength 15, the strongest."))
                .row(GuideCell.code("gate.Level()"),
                        GuideCell.words("The strength the interface reads or emits. It is 0 when nothing powers it."))
                .row(GuideCell.code("gate.Name"), GuideCell.words("The name of the interface."))
                .row(GuideCell.code("Redstone.Named(\"Lever\")"),
                        GuideCell.words("Finds a second interface by its name, in the same way."))
                .define("Redstone Interface", "A block that links the machine to the redstone of the world, so a"
                        + " program can read or emit a signal.")
                .define("Strength", "The power of a redstone signal, from 0 to 15: strength 0 is off, and strength 15"
                        + " is the strongest.")
                .ifSomethingGoesWrong("No interface has the name, so the value is null.",
                        "Give the interface that name, and test for null before you use it.",
                        "Out is given a number outside 0 to 15, and the program stops with \"16 is not a redstone"
                                + " strength, which runs from 0 to 15\".",
                        "Use a strength from 0 to 15.",
                        "The interface is no longer linked to the machine, and the program stops with \"no Redstone"
                                + " Interface named 'Gate' is linked to this machine\".",
                        "Link the interface to the machine again.")
                .seeAlso("jsc:redstone_interfaces", "jsc:sigma_library")
                .register();
    }

    private static void sigmaExampleSound(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_sound", section).titled("Sound")
                .paragraph("Sound makes a beep or a tune through the machine, and plays a recording from its "
                        + "disks. This"
                        + " example uses the four sound calls, and one speaker found by its name.")
                .paragraph("A beep comes out of the speaker inside the case, and each new beep cuts off the one before"
                        + " it. A tune plays through the sound card, which plays every note of a chord.")
                .paragraph("A recording from the machine's disks comes out of the monitors and speakers, as its"
                        + " system says.")
                .subheading("The program")
                .listing("Sound.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Sound.*;",
                        "namespace Corpus;",
                        "class SoundCorpus {",
                        "    static void Main() {",
                        "        Sound.Beep(880, 200);",
                        "        bool tune = Sound.Tones(\"C4:250 E4 G4 C4+E4+G4:500\");",
                        "        bool song = Sound.Play(\"Users/Public/Music/intro.ogg\");",
                        "        Sound.Stop();",
                        "        Speaker desk = Speaker.Named(\"Desk left\");",
                        "        if (desk != null) {",
                        "            bool alone = desk.Play(\"Users/Public/Music/alarm.wav\");",
                        "            Console.PrintLine(desk.Name + alone);",
                        "        }",
                        "        Console.PrintLine(\"\" + tune + song);",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("Sound.Beep(880, 200)"),
                        GuideCell.words("Plays a beep at 880 hertz for 200 milliseconds."))
                .row(GuideCell.code("Sound.Tones(...)"),
                        GuideCell.words("Plays a tune: C4 for 250 milliseconds, then E4 and G4, then the chord "
                                + "C4, E4 and"
                                + " G4 for 500 milliseconds. It answers true when the tune plays."))
                .row(GuideCell.code("Sound.Play(...)"),
                        GuideCell.words("Plays a recording from the machine's disks. It answers false when the file is"
                                + " not playable."))
                .row(GuideCell.code("Sound.Stop()"), GuideCell.words("Stops the sound of the program."))
                .row(GuideCell.code("Speaker.Named(\"Desk left\")"),
                        GuideCell.words("Finds a speaker linked to the machine by its name. It gives null when none"
                                + " has that name."))
                .row(GuideCell.code("desk.Play(...)"),
                        GuideCell.words("Plays a recording through that speaker only, and answers true when it plays."))
                .row(GuideCell.code("desk.Name"), GuideCell.words("The name of the speaker."))
                .define("Beep", "A plain tone at one pitch, for a set time.")
                .define("Pitch", "How high a sound is, measured in hertz. 440 hertz is the note A4.")
                .define("Note", "A named pitch, such as C4, that a tune can play.")
                .define("Chord", "Several notes played together, written with plus signs, such as C4+E4+G4.")
                .define("Speaker", "A block that makes sound for the machine.")
                .define("Recording", "A music file on the machine's disk, such as an .ogg or a .wav file.")
                .ifSomethingGoesWrong("Tones answers false, and no tune plays.",
                        "Check that the machine has a sound device and that its volume is above 0.",
                        "A tune holds a word that is not a note, and the program stops with a message that ends"
                                + " \"is not a note\".",
                        "Write notes such as C4, a pitch in hertz, or R for a rest.",
                        "A chord on the speaker inside the case sounds as its first note only.",
                        "Play the tune through the sound card, which plays the whole chord.",
                        "Speaker.Named gives null, because no speaker has that name.",
                        "Test for null before you use the speaker, and check its name.",
                        "Play answers false for a file that is missing or cannot be played.",
                        "Check the path, written with slashes, and the kind of the file.",
                        "A beep asks for a pitch below 20 or above 20,000 hertz.",
                        "A beep is set to the nearest limit, so give a pitch from 20 to 20,000 hertz.")
                .seeAlso("jsc:soundfoundry", "jsc:sigma_library")
                .register();
    }

    private static void sigmaExampleGateway(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_gateway", section).titled("Gateway")
                .paragraph("A Gateway links the network to ComputerCraft: the computers and peripherals of another mod,"
                        + " on the wires they use. This example reads the Gateway, calls a peripheral, switches a"
                        + " computer on and off, sends a message, and receives messages.")
                .paragraph("If you do not have ComputerCraft installed, the Gateway has no side that reaches its"
                        + " computers.")
                .paragraph("A Gateway's permissions limit what it lets through ([](jsc:gateway_permissions)).")
                .subheading("The program")
                .listing("Gateway.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Collections.*;",
                        "using System.Network.*;",
                        "namespace Corpus;",
                        "class GatewayCorpus {",
                        "    static void Main() {",
                        "        bool online = Gateway.Online;",
                        "        string current = Gateway.Current;",
                        "        List<string> names = Gateway.Names();",
                        "        bool chosen = Gateway.Select(\"east\");",
                        "        Console.PrintLine(\"\" + online + current + names.Count + chosen);",
                        "        foreach (CcComputer computer in Gateway.Computers()) {",
                        "            Console.PrintLine(\"\" + computer.Id + computer.Name + computer.Label"
                                + " + computer.Online);",
                        "        }",
                        "        foreach (CcPeripheral device in Gateway.Peripherals()) {",
                        "            Console.PrintLine(device.Name + device.Type + device.Methods.Count);",
                        "        }",
                        "",
                        "        object none = Gateway.Call(\"left\", \"getName\");",
                        "        object one = Gateway.Call(\"left\", \"setOutput\", true);",
                        "        object two = Gateway.Call(\"left\", \"setOutput\", \"top\", true);",
                        "        object three = Gateway.Call(\"left\", \"setOutput\", \"top\", true, 1);",
                        "        bool powered = Gateway.TurnOn(1) && Gateway.Shutdown(1) && Gateway.Reboot(1);",
                        "        bool sent = Gateway.Send(1, \"hello\");",
                        "        Gateway.OnMessage(Heard);",
                        "        Console.PrintLine(\"\" + none + one + two + three + powered + sent);",
                        "    }",
                        "",
                        "    static void Heard(GatewayMessage message) {",
                        "        Console.PrintLine(\"\" + message.From + message.Text + message.Tick);",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("Gateway.Online"), GuideCell.words("Tells whether the Gateway is online."))
                .row(GuideCell.code("Gateway.Current"), GuideCell.words("The name of the Gateway that is selected."))
                .row(GuideCell.code("Gateway.Names()"), GuideCell.words("The names of the Gateways on the machine."))
                .row(GuideCell.code("Gateway.Select(\"east\")"),
                        GuideCell.words("Selects the Gateway called east, and answers true when it is chosen."))
                .row(GuideCell.code("Gateway.Computers()"),
                        GuideCell.words("Lists the ComputerCraft computers, each with its number, name, label and"
                                + " whether it is online."))
                .row(GuideCell.code("Gateway.Peripherals()"),
                        GuideCell.words("Lists the peripherals, each with its name, its type and its number of"
                                + " methods."))
                .row(GuideCell.code("Gateway.Call(\"left\", \"getName\")"),
                        GuideCell.words("Calls the method getName on the peripheral left, with no argument."))
                .row(GuideCell.code("Gateway.Call(...)"),
                        GuideCell.words("Calls the same method with one argument, with two, and with three."))
                .row(GuideCell.code("Gateway.TurnOn(1), Shutdown(1), Reboot(1)"),
                        GuideCell.words("Turns on, shuts down, or restarts the ComputerCraft computer with number 1,"
                                + " and answers true or false."))
                .row(GuideCell.code("Gateway.Send(1, \"hello\")"),
                        GuideCell.words("Sends the text hello to the computer with number 1."))
                .row(GuideCell.code("Gateway.OnMessage(Heard)"),
                        GuideCell.words("Sets Heard as the method called with each message that arrives."))
                .row(GuideCell.code("Heard(GatewayMessage message)"),
                        GuideCell.words("Prints the sender's number, the text and the tick it arrived on."))
                .define("ComputerCraft", "Another mod with computers and peripherals of its own, which a Gateway"
                        + " reaches.")
                .define("Peripheral", "A device on a computer that the computer controls through its methods.")
                .define("Priority ceiling", "The highest priority of work that a Gateway lets through to the network.")
                .ifSomethingGoesWrong("The machine has no Gateway, and the program stops with \"this computer has no"
                        + " Gateway\".",
                        "Place a Gateway on the machine's network, then select it with Select.",
                        "The name given to Select is not a Gateway, and the program stops with \"this computer has no"
                                + " Gateway called east\".",
                        "Use a name from the list that Names gives.",
                        "A call comes before any Gateway is selected, and the program stops with \"select a gateway"
                                + " first\".",
                        "Call Select first, with the name of a Gateway.",
                        "ComputerCraft is not installed, so the Gateway has no side for it, and the program stops.",
                        "Install ComputerCraft, because the Gateway works only with it.",
                        "The Gateway has no Mainframe to work with, and the program stops with \"no Mainframe on this"
                                + " network\".",
                        "Put a Mainframe on the network.")
                .seeAlso("jsc:network_gateway", "jsc:gateway_permissions", "jsc:sigma_library")
                .register();
    }

    private static void language(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("examples_language").titled("The language")
                .in(chapter).register();
        sigmaExampleValues(guide, section);
        sigmaExampleSteps(guide, section);
        sigmaExamplePrograms(guide, section);
        sigmaExampleOldNames(guide, section);
    }

    private static void sigmaExampleValues(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_values", section).titled("Values")
                .paragraph("A struct is a value. When you store one, or hand it to a method, you get a copy of it. This"
                        + " example shows that with a struct called Vec, and it puts the struct into a field, a static"
                        + " field, an array and a list.")
                .paragraph("A class is a reference type: two names can point at one object, and both see every change"
                        + " made to it. The class Held in this example holds a struct in a field, and its static field"
                        + " is shared by the whole program.")
                .subheading("The program")
                .listing("Values.sgs",
                        "using System.*;",
                        "using System.Collections.*;",
                        "namespace Corpus;",
                        "struct Vec {",
                        "    public int X;",
                        "    public int Y;",
                        "}",
                        "class Held {",
                        "    public Vec Spot;",
                        "    public static Vec Origin;",
                        "}",
                        "class ValuesCorpus {",
                        "    static Vec Kept;",
                        "",
                        "    static void Main() {",
                        "        Vec one = new Vec();",
                        "        one.X = 1;",
                        "        one.Y = 2;",
                        "",
                        "        Vec copy = one;",
                        "        copy.X = 9;",
                        "        Console.PrintLine(\"\" + one.X + copy.X);",
                        "",
                        "        Kept = one;",
                        "        Held held = new Held();",
                        "        held.Spot = one;",
                        "        Held.Origin = one;",
                        "        Console.PrintLine(\"\" + Kept.X + held.Spot.Y + Held.Origin.X);",
                        "",
                        "        Vec given = Same(one);",
                        "        Console.PrintLine(\"\" + given.X + given.Y);",
                        "",
                        "        Vec[] spots = new Vec[3];",
                        "        spots[0] = one;",
                        "        spots[1] = copy;",
                        "        Console.PrintLine(\"\" + spots[0].X + spots[1].X);",
                        "",
                        "        List<Vec> many = new List<Vec>();",
                        "        many.Add(one);",
                        "        many.Add(copy);",
                        "        Console.PrintLine(\"\" + many.Count);",
                        "",
                        "        int total = 0;",
                        "        foreach (Vec spot in spots) {",
                        "            total = total + spot.X;",
                        "        }",
                        "        foreach (Vec spot in many) {",
                        "            total = total + spot.Y;",
                        "        }",
                        "        Console.PrintLine(\"\" + total);",
                        "",
                        "        int[] numbers = new int[4];",
                        "        numbers[0] = 5;",
                        "        numbers[3] = 7;",
                        "        int sum = 0;",
                        "        foreach (int number in numbers) {",
                        "            sum = sum + number;",
                        "        }",
                        "        Console.PrintLine(\"\" + sum);",
                        "",
                        "        string[] words = new string[2];",
                        "        words[0] = \"a\";",
                        "        words[1] = \"b\";",
                        "        string joined = \"\";",
                        "        foreach (string word in words) {",
                        "            joined = joined + word;",
                        "        }",
                        "        Console.PrintLine(joined);",
                        "",
                        "        List<string> names = new List<string>();",
                        "        names.Add(\"x\");",
                        "        foreach (string name in names) {",
                        "            Console.PrintLine(name);",
                        "        }",
                        "    }",
                        "",
                        "    static Vec Same(Vec spot) {",
                        "        Vec back = spot;",
                        "        return back;",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("struct Vec"),
                        GuideCell.words("Declares Vec, a struct with two fields, X and Y, both of type int."))
                .row(GuideCell.code("class Held"),
                        GuideCell.words("Declares a class with a struct field called Spot and a static field called"
                                + " Origin."))
                .row(GuideCell.code("Vec copy = one; copy.X = 9;"),
                        GuideCell.words("Stores a copy of one in copy. Changing the copy leaves one as it was."))
                .row(GuideCell.code("Kept = one; held.Spot = one; Held.Origin = one;"),
                        GuideCell.words("Each store makes its own copy, so the three places hold separate values."))
                .row(GuideCell.code("Same(one)"),
                        GuideCell.words("Hands one to a method, which gives back a copy of it."))
                .row(GuideCell.code("spots[0] = one;"), GuideCell.words("Stores a copy in an element of the array."))
                .row(GuideCell.code("many.Add(one);"), GuideCell.words("Adds a copy to the list."))
                .row(GuideCell.code("foreach (...)"),
                        GuideCell.words("Loops over the array and the list, and reads each struct in turn."))
                .define("Value type", "A type whose values are copied when they are stored or handed over, such as a"
                        + " struct.")
                .define("Reference type", "A type whose values are one shared object: every name for it sees the same"
                        + " object, as with a class.")
                .define("Static", "Belongs to the type, not to one object: every part of the program sees the same"
                        + " value.")
                .define("Array", "A row of values of one type with a fixed length, made with new and the "
                        + "length, such as"
                        + " new Vec[3].")
                .paragraph("A field you did not give a value starts as an empty struct, so a struct field is never"
                        + " nothing. An array of structs works the same way: new Vec[3] makes three empty ones.")
                .ifSomethingGoesWrong("A struct declares a base class, and the compiler refuses it with code S3041.",
                        "A struct can implement interfaces, but it cannot have a base class. Remove the base class"
                                + " from the struct.")
                .seeAlso("jsc:sigma_classes", "jsc:sigma_library")
                .register();
    }

    private static void sigmaExampleSteps(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_steps", section).titled("Steps")
                .paragraph("A step changes a variable by one, or by an amount you give. An increment adds one, a"
                        + " decrement takes one, and a compound assignment does an operation on the variable and stores"
                        + " the result.")
                .paragraph("You write ++ or -- before or after a name. Before the name, you get the new value. "
                        + "After the"
                        + " name, you get the old value. The example shows both for each kind of place you can change.")
                .subheading("The program")
                .listing("Steps.sgs",
                        "using System.*;",
                        "using System.Collections.*;",
                        "namespace Corpus;",
                        "class Counter {",
                        "    public int Value;",
                        "    public long Big;",
                        "    public float Small;",
                        "    public double Wide;",
                        "    public static int Shared;",
                        "}",
                        "class StepsCorpus {",
                        "    static int Total;",
                        "",
                        "    static void Main() {",
                        "        int local = 0;",
                        "        local++;",
                        "        local--;",
                        "        ++local;",
                        "        --local;",
                        "        int before = local++;",
                        "        int after = ++local;",
                        "        int down = local--;",
                        "        int up = --local;",
                        "        Console.PrintLine(\"\" + before + after + down + up);",
                        "",
                        "        long big = 0;",
                        "        float small = 0;",
                        "        double wide = 0;",
                        "        big++;",
                        "        small++;",
                        "        wide--;",
                        "        Console.PrintLine(\"\" + big + small + wide);",
                        "",
                        "        Total++;",
                        "        --Total;",
                        "        int shared = Total++;",
                        "        Counter.Shared++;",
                        "        int other = ++Counter.Shared;",
                        "        Console.PrintLine(\"\" + shared + other);",
                        "",
                        "        Counter counter = new Counter();",
                        "        counter.Value++;",
                        "        --counter.Value;",
                        "        int held = counter.Value++;",
                        "        int heldUp = ++counter.Value;",
                        "        counter.Big++;",
                        "        counter.Small--;",
                        "        counter.Wide++;",
                        "        Console.PrintLine(\"\" + held + heldUp + counter.Value + counter.Big);",
                        "",
                        "        int[] numbers = new int[4];",
                        "        numbers[0]++;",
                        "        --numbers[1];",
                        "        int inside = numbers[2]++;",
                        "        int insideUp = ++numbers[3];",
                        "        Console.PrintLine(\"\" + inside + insideUp + numbers[0]);",
                        "",
                        "        List<int> list = new List<int>();",
                        "        list.Add(1);",
                        "        list[0]++;",
                        "        int fromList = list[0]--;",
                        "        Console.PrintLine(\"\" + fromList + list[0]);",
                        "",
                        "        Map<string, int> map = new Map<string, int>();",
                        "        map.Put(\"a\", 1);",
                        "        map[\"a\"]++;",
                        "        int fromMap = ++map[\"a\"];",
                        "        Console.PrintLine(\"\" + fromMap);",
                        "",
                        "        int kept = 0;",
                        "        Action count = () => {",
                        "            kept++;",
                        "            int was = kept++;",
                        "            int now = ++kept;",
                        "            kept--;",
                        "            Console.PrintLine(\"\" + was + now);",
                        "        };",
                        "        count();",
                        "        Console.PrintLine(\"\" + kept);",
                        "",
                        "        int combined = 1;",
                        "        combined += 2;",
                        "        combined -= 1;",
                        "        combined *= 3;",
                        "        combined /= 2;",
                        "        combined %= 4;",
                        "        combined &= 6;",
                        "        combined |= 1;",
                        "        combined ^= 3;",
                        "        combined <<= 2;",
                        "        combined >>= 1;",
                        "        Console.PrintLine(\"\" + combined);",
                        "",
                        "        string text = \"a\";",
                        "        text += \"b\";",
                        "        text += combined;",
                        "        Console.PrintLine(text);",
                        "",
                        "        counter.Value += 5;",
                        "        Counter.Shared += 2;",
                        "        numbers[0] += 3;",
                        "        list[0] += 4;",
                        "        int answered = counter.Value += 1;",
                        "        int alsoAnswered = numbers[0] += 1;",
                        "        int plain = local = 7;",
                        "        Console.PrintLine(\"\" + answered + alsoAnswered + plain);",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("local++; local--; ++local; --local;"),
                        GuideCell.words("Change a local variable by one, up and down, with the operator before "
                                + "and after"
                                + " the name."))
                .row(GuideCell.code("int before = local++;"),
                        GuideCell.words("Stores the old value in before, and then adds one to local."))
                .row(GuideCell.code("int after = ++local;"),
                        GuideCell.words("Adds one to local first, and then stores the new value in after."))
                .row(GuideCell.code("big++; small++; wide--;"),
                        GuideCell.words("Change a long, a float and a double by one."))
                .row(GuideCell.code("Total++; Counter.Shared++;"),
                        GuideCell.words("Change a static field of the program and a static field of a class."))
                .row(GuideCell.code("counter.Value++;"), GuideCell.words("Changes a field of an object."))
                .row(GuideCell.code("numbers[0]++;"), GuideCell.words("Changes an element of an array."))
                .row(GuideCell.code("list[0]++;"), GuideCell.words("Changes an element of a list."))
                .row(GuideCell.code("map[\"a\"]++;"), GuideCell.words("Changes the value stored under the key a "
                        + "in a map."))
                .row(GuideCell.code("kept++; kept--;"),
                        GuideCell.words("Change a variable that the lambda keeps from the lines around it."))
                .row(GuideCell.code("combined += 2; ... combined >>= 1;"),
                        GuideCell.words("Each compound assignment does its operation on combined and stores the"
                                + " result."))
                .row(GuideCell.code("text += combined;"),
                        GuideCell.words("Adds the number to the end of the text."))
                .row(GuideCell.code("int answered = counter.Value += 1;"),
                        GuideCell.words("An assignment gives back the value it stored, so answered gets the new "
                                + "value."))
                .row(GuideCell.code("int plain = local = 7;"),
                        GuideCell.words("Sets local to 7, and stores 7 in plain as well."))
                .define("Increment", "Adding one to a value, written ++.")
                .define("Decrement", "Taking one from a value, written --.")
                .define("Prefix", "An operator written before a name, such as ++count, which gives the new value.")
                .define("Postfix", "An operator written after a name, such as count++, which gives the old value.")
                .define("Compound assignment", "An assignment that does an operation on the variable first, such as"
                        + " total += 2, which means total = total + 2.")
                .define("Captured variable", "A variable around a lambda that the lambda keeps and can change.")
                .ifSomethingGoesWrong("A value of one type is stored in a variable of another, and the compiler says"
                        + " \"cannot convert\" with code S3003.",
                        "Give the variable a value of its own type, such as a whole number for an int.",
                        "A condition is not a bool, such as a number in an if, and the compiler refuses it with code"
                                + " S3009.",
                        "Write a comparison that gives true or false, such as count > 0.",
                        "A line holds only a value, such as 5;, and the compiler refuses it with code S2006.",
                        "Put an assignment, a call or an increment on the line, or store the value in a variable.",
                        "An assignment goes to something that is not a variable, a field, a property or an "
                                + "element, with"
                                + " code S2008.",
                        "Assign only to a variable, a field, a property or an element.",
                        "A name such as List is not found, and the compiler asks for a using line, with code S3040.",
                        "Add using System.Collections.*; at the top of the file, as the example does.")
                .seeAlso("jsc:sigma_types_variables", "jsc:sigma_operators")
                .register();
    }

    private static void sigmaExamplePrograms(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_programs", section).titled("Programs")
                .paragraph("A program can start other programs, talk to them and wait for them. This example names"
                        + " itself, starts three programs, sends one of them a message, waits for two of them,"
                        + " stops the third, and runs a thread beside itself.")
                .paragraph("A process is a program that runs on the machine and is listed in its process list. Starting"
                        + " one costs more than most calls, because the machine makes a whole process"
                        + " ([](jsc:sigma_running)). If you"
                        + " start a process, you can wait for it, or leave it running.")
                .subheading("The program")
                .listing("Programs.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.Collections.*;",
                        "using System.Execution.*;",
                        "using System.Threading.*;",
                        "namespace Corpus;",
                        "class ProgramsCorpus {",
                        "    static void Main() {",
                        "        Program.SetName(\"corpus\");",
                        "        string name = Program.Name;",
                        "        List<string> args = Program.Args;",
                        "        long dropped = Program.DroppedEvents;",
                        "        Process self = Program.Current;",
                        "        Process one = Program.Start(\"C:\\\\tool.asm\");",
                        "        Process two = Program.Start(\"C:\\\\tool.asm\", args);",
                        "        Process three = Program.Start(\"C:\\\\tool.asm\", args, \"lab\");",
                        "        List<string> said = Program.Shell(\"dir\");",
                        "        Program.OnMessage(m => Console.PrintLine(m.Text + m.From + m.Tick));",
                        "        Console.PrintLine(name + dropped + self.Id + said.Count);",
                        "",
                        "        int id = one.Id;",
                        "        string oneName = one.Name;",
                        "        string host = one.Host;",
                        "        if (one.Running) {",
                        "            one.Wait();",
                        "        }",
                        "        bool done = two.Wait(20);",
                        "        int code = two.ExitCode;",
                        "        three.Kill();",
                        "        List<string> output = two.Output();",
                        "        bool sent = Process.Send(id, \"hello\");",
                        "        Console.PrintLine(oneName + host + done + code + output.Count + sent);",
                        "",
                        "        Thread worker = Thread.Start(() => {",
                        "            Thread.Sleep(5);",
                        "            Thread.Yield();",
                        "        });",
                        "        Thread current = Thread.Current;",
                        "        int threadId = worker.Id;",
                        "        if (worker.Running) {",
                        "            worker.Join();",
                        "        }",
                        "        bool joined = worker.Join(10);",
                        "        worker.Stop();",
                        "        Console.PrintLine(\"\" + current.Id + threadId + joined);",
                        "        Program.Exit(0);",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("Program.SetName(\"corpus\")"),
                        GuideCell.words("Names this program, so the process list shows the name. It costs nothing."))
                .row(GuideCell.code("Program.Name, Args, DroppedEvents, Current"),
                        GuideCell.words("Read the program's name, its arguments, the number of events lost, and its own"
                                + " process."))
                .row(GuideCell.code("Program.Start(...)"),
                        GuideCell.words("Starts a program at a path, with no arguments, with a list of "
                                + "arguments, or with"
                                + " a list and a third text."))
                .row(GuideCell.code("Program.Shell(\"dir\")"),
                        GuideCell.words("Runs one line at this machine's own prompt, to the end, and gives back what it"
                                + " printed."))
                .row(GuideCell.code("Program.OnMessage(...)"),
                        GuideCell.words("Asks to be called with each message, and the lambda prints the message."))
                .row(GuideCell.code("one.Id, Name, Host"),
                        GuideCell.words("The number, the name and the computer of another process."))
                .row(GuideCell.code("one.Running, ExitCode"),
                        GuideCell.words("Tell whether the process still runs, and the number it gave back when "
                                + "it ended."))
                .row(GuideCell.code("one.Wait(), two.Wait(20)"),
                        GuideCell.words("Wait until the process ends, or wait up to a time and answer whether "
                                + "it ended."))
                .row(GuideCell.code("three.Kill()"), GuideCell.words("Stops the process."))
                .row(GuideCell.code("two.Output()"), GuideCell.words("Gives back the lines the process printed."))
                .row(GuideCell.code("Process.Send(id, \"hello\")"),
                        GuideCell.words("Sends the text hello to the process with that number."))
                .row(GuideCell.code("Thread.Start(() => { ... })"),
                        GuideCell.words("Starts a thread: a body of code that runs beside the program, taking "
                                + "turns with"
                                + " it."))
                .row(GuideCell.code("Thread.Sleep(5), Thread.Yield()"),
                        GuideCell.words("Pause the thread for a short time, or let the other code take a turn."))
                .row(GuideCell.code("Thread.Current, worker.Id, Running"),
                        GuideCell.words("The thread that runs this code, the number of the worker, and whether "
                                + "it runs."))
                .row(GuideCell.code("worker.Join(), Join(10), Stop()"),
                        GuideCell.words("Wait for the thread to end, wait for a time, or stop the thread."))
                .row(GuideCell.code("Program.Exit(0)"),
                        GuideCell.words("Ends the program and hands 0 back to the machine as its exit code."))
                .define("Exit code", "A number a program hands back when it ends. 0 usually means success.")
                .ifSomethingGoesWrong("The program waits, and it does not go on, because the process has not ended.",
                        "Use Wait with a time, which answers false when the time is up.",
                        "The Sigma compiler refuses Thread, because these machines run one thing at a time.",
                        "Build the program in Sigma#, the full language.")
                .seeAlso("jsc:sigma_running", "jsc:sigma_library")
                .register();
    }

    private static void sigmaExampleOldNames(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_old_names", section).titled("Old names")
                .paragraph("Old names are short, familiar functions, such as printf, puts and strlen, that "
                        + "older programs"
                        + " use. Each one is a short form of a call the library already has. For example, puts gives"
                        + " the same result as Console.PrintLine.")
                .paragraph("A method or a variable of your own program with the same name wins. The program calls its"
                        + " own method, not the old name. This program needs version 2, because it uses old names that"
                        + " version 2 adds, and its Main takes arguments.")
                .subheading("The program")
                .listing("OldNames.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "namespace Corpus;",
                        "class OldNamesCorpus {",
                        "    static int Main(string[] args) {",
                        "        puts(\"How many ingots?\");",
                        "        putchar('>');",
                        "        Console.Print(' ');",
                        "        string line = gets();",
                        "        int letter = getchar();",
                        "        int count;",
                        "        int scanned = scanf(\"%d\", out count) + Console.Scan(out count) + letter;",
                        "        string built;",
                        "        strcpy(out built, line);",
                        "        strcat(ref built, \"!\");",
                        "        FILE notes = fopen(\"notes.txt\", \"a+\");",
                        "        string held;",
                        "        bool gotLine = fgets(out held, notes) && !feof(notes) && notes.ReadLine(out held)"
                                + " && notes.AtEnd;",
                        "        fputs(built, notes);",
                        "        fputc('.', notes);",
                        "        notes.Write(\"x\");",
                        "        notes.Write('y');",
                        "        int where = fgetc(notes) + notes.Read() + ftell(notes) + notes.Position"
                                + " + notes.Scan(out count);",
                        "        rewind(notes);",
                        "        fseek(notes, 2);",
                        "        notes.Seek(1);",
                        "        fprintf(notes, \"%05d\\" + "n\", where);",
                        "        int more = fscanf(notes, \"%d\", out count);",
                        "        fclose(notes);",
                        "        FILE again = File.Open(\"notes.txt\", \"r\");",
                        "        again.Close();",
                        "        bool moved = remove(\"old.txt\") || rename(\"a.txt\", \"b.txt\")"
                                + " || File.Move(\"b.txt\", \"c.txt\");",
                        "        double asked = atof(line);",
                        "        int n = abs(-3) + min(4, 5) + max(1, 2);",
                        "        double root = sqrt(pow(3, 2)) + floor(2.5) + abs(-0.5) + min(1.5, 2) + max(0.5, 1);",
                        "        srand(1987);",
                        "        int lucky = rand();",
                        "        int length = strlen(line);",
                        "        int at = strstr(line, \"ingot\");",
                        "        string said = sprintf(\"%s: %d, %f\", itoa(n), lucky + length + at, root + asked);",
                        "        printf(\"%s\\" + "n\", said);",
                        "        printf(\"%-12s|%5d|%05.2f|%#x|%e|%u\\" + "n\", said, n, root, lucky, asked, length);",
                        "        int order = strcmp(line, said) + string.Compare(said, line) + atoi(line)"
                                + " + Convert.ToInt(line, -1);",
                        "        char first = toupper('a');",
                        "        char last = tolower(char.ToUpper('z'));",
                        "        bool kinds = isdigit(first) || isalpha(last) || isspace(' ') || "
                                + "char.ToLower(first) == 'a'",
                        "                || char.IsDigit('1') || char.IsLetter('b') || char.IsWhiteSpace('\\t');",
                        "        puts(itoa(order, 16) + Convert.ToString(lucky, 2) + first + last + kinds);",
                        "        if (args.Length > 1) {",
                        "            exit(0);",
                        "        }",
                        "        return args.Length;",
                        "    }",
                        "}")
                .subheading("Input and output")
                .table("Old names for printing and reading", "Old name", "The long way it calls")
                .row(GuideCell.code("printf"), GuideCell.code("Console.Print, with a formatted text"))
                .row(GuideCell.code("puts"), GuideCell.code("Console.PrintLine"))
                .row(GuideCell.code("putchar"), GuideCell.code("Console.Print, with one character"))
                .row(GuideCell.code("gets"), GuideCell.code("Console.ReadLine"))
                .row(GuideCell.code("getchar"), GuideCell.code("Console.Read"))
                .row(GuideCell.code("scanf"), GuideCell.code("Console.Scan, with one value"))
                .row(GuideCell.code("exit"), GuideCell.code("Program.Exit"))
                .subheading("Numbers and text")
                .table("Old names for numbers and text", "Old name", "The long way it calls")
                .row(GuideCell.code("abs, min, max"), GuideCell.code("Math.Abs, Math.Min, Math.Max"))
                .row(GuideCell.code("sqrt, pow, floor"), GuideCell.code("Math.Sqrt, Math.Pow, Math.Floor"))
                .row(GuideCell.code("atoi"), GuideCell.code("Convert.ToInt, which gives 0 for text that is not "
                        + "a number"))
                .row(GuideCell.code("atof"), GuideCell.code("Convert.ToDouble"))
                .row(GuideCell.code("itoa"), GuideCell.code("Convert.ToString"))
                .row(GuideCell.code("sprintf"), GuideCell.code("the formatted text, as a value"))
                .row(GuideCell.code("srand, rand"), GuideCell.code("Random.Seed, and Random.Next(32768)"))
                .row(GuideCell.code("strlen"), GuideCell.code("the Length of the text"))
                .row(GuideCell.code("strstr"), GuideCell.code("IndexOf of the text"))
                .row(GuideCell.code("strcmp"), GuideCell.code("string.Compare"))
                .row(GuideCell.code("strcpy"), GuideCell.code("the out place takes the text"))
                .row(GuideCell.code("strcat"), GuideCell.code("the ref place gets the text added on"))
                .row(GuideCell.code("toupper, tolower"), GuideCell.code("Char.ToUpper, Char.ToLower"))
                .row(GuideCell.code("isdigit, isalpha, isspace"),
                        GuideCell.code("Char.IsDigit, Char.IsLetter, Char.IsWhiteSpace"))
                .subheading("Files")
                .table("Old names for files", "Old name", "The long way it calls")
                .row(GuideCell.code("fopen"), GuideCell.code("File.Open, which gives a FILE"))
                .row(GuideCell.code("fclose"), GuideCell.code("Close of the file"))
                .row(GuideCell.code("fgets"), GuideCell.code("ReadLine of the file"))
                .row(GuideCell.code("fputs, fprintf"), GuideCell.code("Write of the text"))
                .row(GuideCell.code("fscanf"), GuideCell.code("Scan of the file"))
                .row(GuideCell.code("fgetc"), GuideCell.code("Read of the file"))
                .row(GuideCell.code("fputc"), GuideCell.code("Write of one character"))
                .row(GuideCell.code("feof"), GuideCell.code("AtEnd of the file"))
                .row(GuideCell.code("rewind"), GuideCell.code("Seek to 0"))
                .row(GuideCell.code("fseek"), GuideCell.code("Seek to the given position"))
                .row(GuideCell.code("ftell"), GuideCell.code("Position of the file"))
                .row(GuideCell.code("remove"), GuideCell.code("File.Delete"))
                .row(GuideCell.code("rename"), GuideCell.code("File.Move"))
                .paragraph("The text of a printf format holds holes, and a value fills each hole. A hole is a "
                        + "percent sign"
                        + " and a letter, and a number written into the hole sets its width.")
                .table("The holes of a format", "Hole", "Takes")
                .row(GuideCell.code("%d, %i, %u, %x, %X, %o"), GuideCell.words("A whole number."))
                .row(GuideCell.code("%f, %e, %E"), GuideCell.words("A number of type double."))
                .row(GuideCell.code("%s"), GuideCell.words("Text."))
                .row(GuideCell.code("%c"), GuideCell.words("A character."))
                .row(GuideCell.code("%%"), GuideCell.words("A percent sign, written as itself."))
                .define("Bare function", "A function written with no type or object in front of its name, such as"
                        + " puts(...).")
                .define("Format string", "A text with holes, such as %d, that the values of a call fill in order.")
                .define("Hole", "A place in a format string that one value fills.")
                .define("FILE", "A file that the program has open, from fopen.")
                .define("Out", "A variable that a method writes, and that the call hands to the method.")
                .define("Ref", "A variable that a method reads and writes, and that the call hands to the method.")
                .ifSomethingGoesWrong("The program is built at version 1, and a name that needs version 2 is refused"
                        + " with code S3057. The message names the name, the language and the version it needs.",
                        "Build with --lang 2, or take the name out of the program. See"
                                + " ([](jsc:sigma_examples_intro)).",
                        "The format of printf is kept in a variable, and the compiler refuses it with code S3053.",
                        "Write the format in quotes inside the call, where it is used.",
                        "The format has more holes than the call has values, and the compiler refuses it with "
                                + "code S3055.",
                        "Give one value for each hole, in order.",
                        "A value does not match its hole, such as text for a %d, and the compiler refuses it with code"
                                + " S3056.",
                        "Change the value, or the letter of the hole, so that they match.",
                        "scanf is written with two holes, or without out, and the compiler refuses it.",
                        "Write scanf with one hole and nothing around it, and the variable with out, as"
                                + " scanf(\"%d\", out n) does.",
                        "Your method leaves an out argument empty on one way out, and the compiler refuses it"
                                + " with code S3037.",
                        "Give the out argument a value before every return.")
                .seeAlso("jsc:sigma_printf_old_names", "jsc:sigma_methods_out_ref", "jsc:sigma_library")
                .register();
    }

    private static void windows(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("examples_interface").titled("Windows")
                .in(chapter).register();
        sigmaExampleWindows(guide, section);
        sigmaExampleWidgets(guide, section);
    }

    private static void sigmaExampleWindows(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_windows", section).titled("Windows")
                .paragraph("A window is a box on the desktop, with a title. This example builds a window, puts "
                        + "a row and"
                        + " a column of widgets in it, and shows it. A widget is a piece of a window, such as a "
                                + "label, a"
                        + " button or a box to type in. You make a window with a title, a width and a height.")
                .paragraph("The machine's own system draws the widgets, so the same program looks like the system it"
                        + " runs under. The example closes its window near the end of Main,"
                        + " and then shows a message box.")
                .subheading("The program")
                .listing("Ui.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.UI.*;",
                        "namespace Corpus;",
                        "class UiCorpus {",
                        "    static void Main() {",
                        "        Window window = new Window(\"Corpus\", 240, 150);",
                        "        window.Title = \"Corpus\";",
                        "        Row row = new Row();",
                        "        Column column = new Column();",
                        "        row.Spacing = 2;",
                        "        column.Spacing = 2;",
                        "",
                        "        Label label = new Label();",
                        "        Label named = new Label(\"name\");",
                        "        named.Text = \"name\";",
                        "        named.Visible = true;",
                        "        named.Enabled = true;",
                        "        named.Width = 40;",
                        "        named.Height = 10;",
                        "",
                        "        Button button = new Button();",
                        "        Button pressed = new Button(\"press\");",
                        "        pressed.Text = \"press\";",
                        "        pressed.OnClick += Clicked;",
                        "",
                        "        TextBox box = new TextBox();",
                        "        TextBox typed = new TextBox(\"x\");",
                        "        typed.Text = \"y\";",
                        "        typed.OnChange += Clicked;",
                        "        typed.OnSubmit += Clicked;",
                        "",
                        "        CheckBox check = new CheckBox();",
                        "        CheckBox labelled = new CheckBox(\"on\");",
                        "        CheckBox ticked = new CheckBox(\"on\", true);",
                        "        ticked.Text = \"on\";",
                        "        ticked.Checked = false;",
                        "        ticked.OnToggle += Clicked;",
                        "",
                        "        ProgressBar bar = new ProgressBar();",
                        "        ProgressBar ranged = new ProgressBar(0, 10);",
                        "        ranged.Value = 5;",
                        "        ranged.Least = 0;",
                        "        ranged.Most = 10;",
                        "",
                        "        ListBox listBox = new ListBox();",
                        "        listBox.Add(\"one\");",
                        "        listBox.Add(\"two\", \"second\");",
                        "        listBox.Selected = 0;",
                        "        listBox.OnSelect += Clicked;",
                        "",
                        "        Canvas canvas = new Canvas();",
                        "        Canvas sized = new Canvas(100, 50);",
                        "        sized.Clear(0);",
                        "        sized.FillRect(0, 0, 10, 10, 1);",
                        "        sized.DrawLine(0, 0, 10, 10, 2);",
                        "        sized.DrawText(\"hi\", 1, 1, 3);",
                        "        sized.SetPixel(2, 2, 4);",
                        "        sized.OnClick += Clicked;",
                        "",
                        "        row.Add(label);",
                        "        row.Add(named, 1);",
                        "        column.Add(row);",
                        "        column.Add(sized, 1);",
                        "        column.Add(box);",
                        "        column.Add(check);",
                        "        column.Add(labelled);",
                        "        column.Add(bar);",
                        "        column.Add(canvas);",
                        "        window.Content = column;",
                        "        window.Add(button, 0, 0, 20, 10);",
                        "        window.OnClose += Clicked;",
                        "        window.Show();",
                        "        Console.PrintLine(\"\" + window.Open + listBox.Count + sized.ClickX + sized.ClickY);",
                        "        row.Clear();",
                        "        column.Clear();",
                        "        listBox.Clear();",
                        "        window.Close();",
                        "        MessageBox.Show(\"title\", \"text\");",
                        "    }",
                        "",
                        "    static void Clicked() {",
                        "    }",
                        "}")
                .subheading("What the lines do")
                .table("Lines of the program", "Line", "What it does")
                .row(GuideCell.code("new Window(\"Corpus\", 240, 150)"),
                        GuideCell.words("Makes a window with the title Corpus, a width of 240 and a height of 150."))
                .row(GuideCell.code("new Row(), new Column(), Spacing"),
                        GuideCell.words("A row lines its widgets up across, and a column lines them up down. "
                                + "Spacing sets"
                                + " the room between them."))
                .row(GuideCell.code("new Label(\"name\"), Text, Width, Height"),
                        GuideCell.words("A label shows a text. Its size is set by Width and Height."))
                .row(GuideCell.code("new Button(\"press\"), OnClick"),
                        GuideCell.words("A button with a text on it. OnClick names the method called when the player"
                                + " clicks it."))
                .row(GuideCell.code("new TextBox(\"x\"), OnChange, OnSubmit"),
                        GuideCell.words("A box to type in. OnChange runs when its text changes, and OnSubmit when the"
                                + " player submits it."))
                .row(GuideCell.code("new CheckBox(\"on\", true), Checked"),
                        GuideCell.words("A tick box with a text. Checked says whether it is ticked."))
                .row(GuideCell.code("new ProgressBar(0, 10), Value, Least, Most"),
                        GuideCell.words("A bar that shows a value between a least and a most."))
                .row(GuideCell.code("ListBox: Add, Selected, OnSelect"),
                        GuideCell.words("A list of lines. Selected is the chosen line, and OnSelect runs when one is"
                                + " picked."))
                .row(GuideCell.code("new Canvas(100, 50), Clear, FillRect, DrawLine"),
                        GuideCell.words("A blank area to draw on, 100 wide and 50 high. Each drawing call paints one"
                                + " shape or text in a colour from 0 to 15."))
                .row(GuideCell.code("DrawText, SetPixel, ClickX, ClickY"),
                        GuideCell.words("Draw a text or one point. ClickX and ClickY give where the player last "
                                + "clicked."))
                .row(GuideCell.code("row.Add(named, 1)"),
                        GuideCell.words("Adds the label with a weight of 1, which takes a share of the room "
                                + "left over."))
                .row(GuideCell.code("window.Content = column"),
                        GuideCell.words("Makes the column the main content of the window."))
                .row(GuideCell.code("window.Add(button, 0, 0, 20, 10)"),
                        GuideCell.words("Places the button at position 0, 0, with a width of 20 and a height of 10."))
                .row(GuideCell.code("window.OnClose"), GuideCell.words("Names the method called when the window "
                        + "closes."))
                .row(GuideCell.code("window.Show()"),
                        GuideCell.words("Opens the window on the desktop and its taskbar. The window stays open after"
                                + " the tick that opened it."))
                .row(GuideCell.code("window.Open, window.Close()"),
                        GuideCell.words("Open says whether the window is open. Close closes it."))
                .row(GuideCell.code("MessageBox.Show(\"title\", \"text\")"),
                        GuideCell.words("Shows a small box with a title and a line of text."))
                .define("Window", "A box on the desktop, with a title and widgets inside it.")
                .define("Row", "A box that lines up its widgets across, one after another.")
                .define("Column", "A box that lines up its widgets down, one under another.")
                .define("Weight", "A number that gives a widget a share of the room left over. Only widgets "
                        + "with a weight"
                        + " share that room.")
                .define("Canvas", "A blank area that a program draws on, with shapes, lines and text.")
                .define("Handler", "A method that the program names, and that is called when something happens.")
                .ifSomethingGoesWrong("A window holds more than 256 widgets, counting the ones inside rows and columns,"
                        + " and the program stops with \"a window holds at most 256 widgets, counting the ones inside"
                        + " rows and columns\".",
                        "Put fewer widgets in the window. Widgets inside rows and columns count too, so"
                                + " remove some of those as well.",
                        "A row or a column holds more than 256 widgets, and the program stops with \"a row or a column"
                                + " holds at most 256 widgets\".",
                        "Split the widgets between two rows or two columns.",
                        "A widget is put inside itself, and the program stops with \"a widget cannot hold itself or a"
                                + " widget it is inside\".",
                        "Put each widget in one place only, and never inside itself.",
                        "A canvas gets more than 4,096 strokes, and the program stops with \"a canvas holds at "
                                + "most 4096"
                                + " strokes\".",
                        "Draw fewer shapes, or use a second canvas for the rest.")
                .seeAlso("jsc:sigma_example_widgets", "jsc:sigma_library")
                .register();
    }

    private static void sigmaExampleWidgets(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sigma_example_widgets", section).titled("Widgets")
                .paragraph("This example shows the rest of the widgets: text areas and number boxes, choices, "
                        + "tabs, tables,"
                        + " trees, menus, status bars, pictures, charts, logs, file dialogs, item slots, operations and"
                        + " components that a mod adds.")
                .paragraph("Some widgets draw pictures, charts, items or components. A machine with only its terminal"
                        + " cannot draw them, so making one there stops the program, and the message says so.")
                .paragraph("The handler is a method that you write and name on the widget.")
                .paragraph("Widgets have events such as OnChange, OnSelect and OnClick. Each one names the method"
                        + " called when the player acts.")
                .subheading("The program")
                .listing("Widgets.sgs",
                        "using System.*;",
                        "using System.IO.*;",
                        "using System.UI.*;",
                        "using System.Operations.*;",
                        "namespace Corpus;",
                        "class WidgetsCorpus {",
                        "    static void Main() {",
                        "        Window window = new Window(\"Widgets\", 320, 200);",
                        "        Column column = new Column();",
                        "",
                        "        TextArea area = new TextArea();",
                        "        TextArea notes = new TextArea(\"notes\");",
                        "        notes.Text = \"more\";",
                        "        notes.OnChange += Heard;",
                        "",
                        "        NumberBox numbers = new NumberBox();",
                        "        NumberBox copies = new NumberBox(1, 9);",
                        "        copies.Value = 3;",
                        "        copies.Least = 1;",
                        "        copies.Most = 9;",
                        "        copies.Step = 1;",
                        "        copies.OnChange += Heard;",
                        "",
                        "        Slider slider = new Slider();",
                        "        Slider speed = new Slider(0, 100);",
                        "        speed.Value = 50;",
                        "        speed.Least = 0;",
                        "        speed.Most = 100;",
                        "        speed.Step = 5;",
                        "        speed.OnChange += Heard;",
                        "",
                        "        RadioGroup radios = new RadioGroup();",
                        "        radios.Add(\"Fast\");",
                        "        radios.Add(\"Slow\");",
                        "        radios.Selected = 1;",
                        "        radios.Across = true;",
                        "        radios.OnSelect += Heard;",
                        "",
                        "        ComboBox combo = new ComboBox();",
                        "        combo.Add(\"Legacy\");",
                        "        combo.Selected = 1;",
                        "        combo.OnSelect += Heard;",
                        "",
                        "        GroupBox group = new GroupBox();",
                        "        GroupBox backup = new GroupBox(\"Backup\");",
                        "        backup.Text = \"Backup\";",
                        "        backup.Content = notes;",
                        "        TabView tabs = new TabView();",
                        "        tabs.Add(\"Controls\", backup);",
                        "        tabs.Selected = 1;",
                        "        tabs.OnSelect += Heard;",
                        "",
                        "        ScrollView scroll = new ScrollView();",
                        "        scroll.Content = area;",
                        "",
                        "        Table table = new Table();",
                        "        table.AddColumn(\"Name\");",
                        "        List<string> cells = new List<string>();",
                        "        cells.Add(\"a\");",
                        "        table.AddRow(cells);",
                        "        table.AddRow(\"a\");",
                        "        table.AddRow(\"a\", \"b\");",
                        "        table.AddRow(\"a\", \"b\", \"c\");",
                        "        table.AddRow(\"a\", \"b\", \"c\", \"d\");",
                        "        table.Selected = 1;",
                        "        table.OnSelect += Heard;",
                        "        string cell = table.Cell(1, 1);",
                        "",
                        "        TreeView tree = new TreeView();",
                        "        int top = tree.Add(\"This PC\");",
                        "        int under = tree.Add(\"C:\", top);",
                        "        string node = tree.NodeText(under);",
                        "        tree.Selected = under;",
                        "        tree.OnSelect += Heard;",
                        "",
                        "        MenuBar bar = new MenuBar();",
                        "        bar.Add(\"File\", \"Open\");",
                        "        bar.OnPick += Heard;",
                        "        ContextMenu menu = new ContextMenu(table);",
                        "        menu.Add(\"Rename\");",
                        "        menu.OnPick += Heard;",
                        "",
                        "        StatusBar status = new StatusBar();",
                        "        StatusBar ready = new StatusBar(\"Ready\");",
                        "        ready.Text = \"Ready\";",
                        "        ready.Add(\"12:30\");",
                        "",
                        "        Image image = new Image();",
                        "        Image photo = new Image(\"/photo.pix\");",
                        "        photo.Path = \"/photo.pix\";",
                        "",
                        "        Chart chart = new Chart();",
                        "        Chart cpu = new Chart(0, 100);",
                        "        cpu.Text = \"CPU\";",
                        "        cpu.Least = 0;",
                        "        cpu.Most = 100;",
                        "        cpu.Add(61.0);",
                        "",
                        "        LogView log = new LogView();",
                        "        log.Add(\"10:14 INFO started\");",
                        "",
                        "        OpenFileDialog open = new OpenFileDialog();",
                        "        OpenFileDialog opener = new OpenFileDialog(\"Open\");",
                        "        opener.Title = \"Open\";",
                        "        opener.Path = \"/\";",
                        "        opener.Filter = \"*.txt\";",
                        "        opener.OnChoose += Heard;",
                        "        opener.OnCancel += Heard;",
                        "        SaveFileDialog save = new SaveFileDialog();",
                        "        SaveFileDialog saver = new SaveFileDialog(\"Save\");",
                        "        saver.Title = \"Save\";",
                        "        saver.Path = \"/\";",
                        "        saver.Filter = \"*.txt\";",
                        "        saver.Name = \"a.txt\";",
                        "        saver.OnChoose += Heard;",
                        "        saver.OnCancel += Heard;",
                        "",
                        "        ItemSlot slot = new ItemSlot();",
                        "        ItemSlot diamond = new ItemSlot(\"minecraft:diamond\");",
                        "        ItemSlot stack = new ItemSlot(\"minecraft:diamond\", 64);",
                        "        stack.Item = \"minecraft:diamond\";",
                        "        stack.Amount = 64;",
                        "        stack.OnClick += Heard;",
                        "",
                        "        ItemPicker picker = new ItemPicker();",
                        "        picker.Add(\"minecraft:diamond\");",
                        "        picker.Selected = 1;",
                        "        picker.OnSelect += Heard;",
                        "",
                        "        OperationView view = new OperationView();",
                        "        view.Text = \"SELECT 64 diamond\";",
                        "        view.State = \"PROCESSING\";",
                        "        view.Done = 3;",
                        "        view.Total = 5;",
                        "        view.Computer = \"Mainframe\";",
                        "        view.Show(Operations.Get(\"op\"));",
                        "",
                        "        GenericComponent dial = new GenericComponent(\"corpus:dial\");",
                        "        dial.Data = 5;",
                        "        dial.OnAction(Acted);",
                        "",
                        "        column.Add(bar);",
                        "        column.Add(tabs, 1);",
                        "        column.Add(scroll);",
                        "        column.Add(copies);",
                        "        column.Add(speed);",
                        "        column.Add(radios);",
                        "        column.Add(combo);",
                        "        column.Add(table);",
                        "        column.Add(tree);",
                        "        column.Add(photo);",
                        "        column.Add(cpu);",
                        "        column.Add(log);",
                        "        column.Add(stack);",
                        "        column.Add(picker);",
                        "        column.Add(view);",
                        "        column.Add(dial);",
                        "        column.Add(ready);",
                        "        window.Content = column;",
                        "        window.Show();",
                        "        opener.Show(window);",
                        "        saver.Show(window);",
                        "        Console.PrintLine(cell + node + radios.Count + combo.Count + tabs.Count + table.Count"
                                + " + tree.Count",
                        "            + cpu.Count + log.Count + picker.Count + picker.Picked + bar.Picked"
                                + " + bar.PickedMenu + menu.Picked",
                        "            + dial.Kind + dial.Data + view.Done + numbers.Value + slider.Value + group.Text"
                                + " + status.Text",
                        "            + image.Path + chart.Count + open.Path + save.Name + slot.Amount);",
                        "        radios.Clear();",
                        "        combo.Clear();",
                        "        tabs.Clear();",
                        "        table.Clear();",
                        "        tree.Clear();",
                        "        bar.Clear();",
                        "        menu.Clear();",
                        "        ready.Clear();",
                        "        cpu.Clear();",
                        "        log.Clear();",
                        "        picker.Clear();",
                        "    }",
                        "",
                        "    static void Heard() {",
                        "    }",
                        "",
                        "    static void Acted(ComponentAction action) {",
                        "        Console.PrintLine(action.Name + action.Value);",
                        "    }",
                        "}")
                .subheading("The widgets")
                .table("Widgets and what they do", "Widget", "What it does")
                .row(GuideCell.code("TextArea"),
                        GuideCell.words("A box for several lines of text. OnChange runs when its text changes."))
                .row(GuideCell.code("NumberBox, Slider"),
                        GuideCell.words("A whole number between a least and a most, moved a step at a time."))
                .row(GuideCell.code("RadioGroup"),
                        GuideCell.words("A set of choices, one of which is picked. Selected is the picked choice,"
                                + " counted from 1,"
                                + " and 0 for none."))
                .row(GuideCell.code("ComboBox"),
                        GuideCell.words("A box with a list of choices. Selected is the picked choice."))
                .row(GuideCell.code("GroupBox"), GuideCell.words("A box with a caption, and a widget inside it."))
                .row(GuideCell.code("TabView"),
                        GuideCell.words("Tabs, each with a title and a widget. The first one added shows first."))
                .row(GuideCell.code("ScrollView"),
                        GuideCell.words("Shows a widget that is taller than its room, and the player scrolls it."))
                .row(GuideCell.code("Table"),
                        GuideCell.words("Rows and columns of text. Cell gives the text in one cell. A table "
                                + "holds at most"
                                + " 8 columns."))
                .row(GuideCell.code("TreeView"),
                        GuideCell.words("A tree of text. Add gives back the number of the new node, and "
                                + "NodeText gives its"
                                + " text."))
                .row(GuideCell.code("MenuBar"),
                        GuideCell.words("A bar of menus with entries. Picked and PickedMenu tell what the "
                                + "player picked."))
                .row(GuideCell.code("ContextMenu"),
                        GuideCell.words("A menu that opens on its widget when the player clicks it with the "
                                + "other button."))
                .row(GuideCell.code("StatusBar"),
                        GuideCell.words("A line of text, with a few sections at its right."))
                .row(GuideCell.code("Image"),
                        GuideCell.words("A picture read from the machine's disk, as the paint program saves it."))
                .row(GuideCell.code("Chart"),
                        GuideCell.words("A line drawn through the numbers added to it, between a least and a most."))
                .row(GuideCell.code("LogView"),
                        GuideCell.words("Lines one under another, the newest at the bottom. A line with WARN or "
                                + "ERROR is"
                                + " picked out."))
                .row(GuideCell.code("OpenFileDialog, SaveFileDialog"),
                        GuideCell.words("A dialog in which the player chooses a file. Show opens it on a window."))
                .row(GuideCell.code("ItemSlot"),
                        GuideCell.words("One slot that holds an item, shown by its item id and how many."))
                .row(GuideCell.code("ItemPicker"),
                        GuideCell.words("A grid of items with a search line above it. The player picks one."))
                .row(GuideCell.code("OperationView"),
                        GuideCell.words("Shows an Operation: its text, its state, how much is done, and its computer."))
                .row(GuideCell.code("GenericComponent"),
                        GuideCell.words("A widget that a mod adds, named by an id such as corpus:dial. OnAction "
                                + "runs when"
                                + " it acts."))
                .define("Event", "Something that happens, such as a click or a change, which a method can answer.")
                .define("Generic component", "A widget that a mod adds, identified by its own id, such as mod:name.")
                .define("Item id", "The name of an item in the game, such as minecraft:diamond.")
                .ifSomethingGoesWrong("A picture, a chart, an item slot or a component is made on a machine with only"
                        + " its terminal, and the program stops.",
                        "Run the program on a machine that draws its windows, not only its terminal.",
                        "A window holds more than 256 widgets, and the program stops with \"a window holds at most 256"
                                + " widgets, counting the ones inside rows and columns\".",
                        "Put fewer widgets in the window, and count the ones inside rows and columns.",
                        "A table gets more than 8 columns, and the program stops with \"a table has at most 8"
                                + " columns\".",
                        "Use at most 8 columns in a table.",
                        "A menu gets more than 64 entries, and the program stops with \"a menu holds at most 64"
                                + " entries\".",
                        "Keep each menu to 64 entries or fewer.",
                        "A list gets more than 1024 rows, and the program stops with \"a list holds at most 1024"
                                + " rows\".",
                        "Keep each list to 1024 rows or fewer.")
                .seeAlso("jsc:sigma_example_windows", "jsc:operations", "jsc:soundfoundry", "jsc:sigma_library")
                .register();
    }
}
