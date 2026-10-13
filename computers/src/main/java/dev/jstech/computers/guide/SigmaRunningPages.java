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
 * Declares the pages of the Book of Sigma's chapter on programs running on a computer: processes and threads,
 * the credits a computer gives its programs each tick, what each call costs, the limits a running program keeps,
 * how programs are saved with the world, and how programs talk to each other and share folders.
 */
final class SigmaRunningPages {

    private SigmaRunningPages() {
    }

    /** Declares the sections and entries of the chapter on programs running, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        running(guide, chapter);
        credits(guide, chapter);
        limits(guide, chapter);
        saving(guide, chapter);
        talking(guide, chapter);
    }

    private static void running(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("running").titled("Running").in(chapter).register();
        guide.page("running_programs", section).titled("Programs and processes")
                .paragraph("A program is a file that the computer runs for you. You write it in Sigma or"
                        + " Sigma#, and the compiler turns it into a listing (a file that ends in .asm)"
                        + " that the computer runs. See [Compiling with sgsc and scc](jsc:sigma_compile).")
                .paragraph("While it runs, the program is a process: one entry in the computer's list of"
                        + " running programs, with its own number, its own name and the memory it was given."
                        + " The list lives on the computer, not on its disks. A program you have installed"
                        + " sits on a disk and does nothing until it starts.")
                .paragraph("A process leaves the list when it ends. Switching the computer off stops all of"
                        + " them. What happens to them after a save is in"
                        + " [Programs and the world's save](jsc:saved_with_world).")
                .subheading("The number and the name")
                .paragraph("Each process has a number, and the first is number 1. The computer counts up from 1 and"
                        + " skips every number that a running process still holds. Past the largest number it can"
                        + " hold, it starts again at number 1. A program can give itself a name with Program.SetName."
                        + " Until it does, its name is sigma.")
                .subheading("Starting a program at the prompt")
                .paragraph("Once the runtime is installed"
                        + " ([Installing the compilers and the runtime](jsc:sigma_installing)),"
                        + " the prompt knows the sigma command. Its commands are these:")
                .table("Commands at the prompt", "Command", "What it does")
                .row(GuideCell.code("sigma run <file>"),
                        GuideCell.words("Starts the program from its file, and passes the words after the"
                                + " file's name to it as arguments."))
                .row(GuideCell.code("sigma run <file> --heap 16M"),
                        GuideCell.words("Sets the memory the program is given, in megabytes: write 16M or 16."))
                .row(GuideCell.code("sigma ps"),
                        GuideCell.words("Lists the running programs, the same as sigma list or a bare sigma."))
                .row(GuideCell.code("sigma stop <id>"),
                        GuideCell.words("Stops the program with that number, the same as sigma kill."))
                .steps("Type sigma run, the file's name and any arguments, then press [[Enter]].",
                        "Type sigma ps to see the program's number and what it is doing.")
                .paragraph("A program that stays up in the background is a service. The prompt does not wait"
                        + " for it, and says which number it started as. Any other program keeps the prompt"
                        + " until it returns, and whatever you type goes to that program.")
                .paragraph("The list has one row per process. With nothing running, the prompt says that no"
                        + " programs are running.")
                .table("The columns of sigma ps", "Column", "What it shows")
                .row(GuideCell.words("id"), GuideCell.words("The program's number, which sigma stop takes."))
                .row(GuideCell.words("name"),
                        GuideCell.words("The program's name, cut to 20 characters, with a tilde (~) at the end"
                                + " when it is cut."))
                .row(GuideCell.words("state"),
                        GuideCell.words("What the process is doing, as the table below explains."))
                .row(GuideCell.words("memory"),
                        GuideCell.words("The memory it holds, out of the memory it was given: bytes below one"
                                + " kilobyte (KB), kilobytes above."))
                .table("The states of a process", "State", "What it means")
                .row(GuideCell.code("input"),
                        GuideCell.words("It stopped on a read and waits for a line you type."))
                .row(GuideCell.code("running"), GuideCell.words("It has instructions left to run."))
                .row(GuideCell.code("parked"),
                        GuideCell.words("It waits for something outside itself, and uses no turns until"
                                + " that is done."))
                .row(GuideCell.code("finished"), GuideCell.words("It ran to the end."))
                .row(GuideCell.code("halted"),
                        GuideCell.words("It stopped because of a mistake, and its message is written to its"
                                + " console."))
                .paragraph("A halt is a stop caused by a mistake in the program's own steps. The reasons for each"
                        + " one are in [The stops while a program runs](jsc:sigma_runtime_stops).")
                .subheading("Ending a program")
                .paragraph("A program ends in one of three ways: it returns from the method where it starts,"
                        + " it calls Program.Exit with a number, or you stop it. The number it ends with is its"
                        + " exit code. A program that returns on its own ends with exit code 0, unless it set"
                        + " another code. A halted program reports exit code 1.")
                .paragraph("When you stop a program, it gets a goodbye: it may run its own closing steps before"
                        + " it is removed. Its parents are told that it ended. The goodbye has a budget,"
                        + " described in [How much a machine runs each tick](jsc:credits_per_tick).")
                .paragraph("A finished process leaves the list once nothing waits for it. A process that"
                        + " another program started keeps its output and its exit code until that parent"
                        + " is gone.")
                .subheading("Console and script")
                .paragraph("A program has one of two shapes, console or script. A console program starts at its"
                        + " entry method, keeps the prompt while it runs, writes to the terminal that started it,"
                        + " and is gone when it returns.")
                .paragraph("Any other shape is a script. A script is set up once, is called again on every tick"
                        + " while the computer is on, and is told when it is stopped. After the world loads again,"
                        + " it comes back.")
                .subheading("Threads")
                .paragraph("A process starts with one thread, the main thread, which always has the number 1."
                        + " A program makes more threads with Thread.Start, and each new thread gets the next"
                        + " number: thread 2, then thread 3, and so on. The threads of a process share its memory, but"
                        + " each"
                        + " one keeps its own place in the calls it is making.")
                .paragraph("The computer gives the process its turns, and the process shares them among its"
                        + " threads that are ready to run. Each turn is a slice of at most 64 instructions. A"
                        + " thread that waits for something gets no turn until it can go on.")
                .paragraph("Starting a thread costs 49 instructions on top of the call, because each thread"
                        + " keeps a stack of calls of its own.")
                .table("Thread calls", "Call", "What it does")
                .row(GuideCell.code("Thread.Start(action)"),
                        GuideCell.words("Starts another thread, which runs the action you give it."))
                .row(GuideCell.code("Thread.Sleep(ticks)"),
                        GuideCell.words("Waits that many ticks, then goes on."))
                .row(GuideCell.code("Thread.Yield()"), GuideCell.words("Gives up the rest of its turn."))
                .row(GuideCell.code("Thread.Join()"), GuideCell.words("Waits until that thread ends."))
                .row(GuideCell.code("Thread.Join(ticks)"),
                        GuideCell.words("Waits up to that many ticks, and gives false if the thread has not"
                                + " ended by then."))
                .row(GuideCell.code("Thread.Stop()"), GuideCell.words("Ends that thread."))
                .row(GuideCell.code("Thread.Id"),
                        GuideCell.words("The thread's number, which is 1 for the main thread."))
                .paragraph("Each object can have a lock. Releasing a lock that you do not hold halts the"
                        + " program. [Threads and locks](jsc:threads_and_locks) explains how to take and"
                        + " release one.")
                .define("Process", "A program that is running on a computer, with its own number, name and memory.")
                .define("Tick", "One beat of the game's clock. Twenty ticks make a second.")
                .define("Instruction", "One simple step of a program's work. Each step a program takes costs"
                        + " one instruction.")
                .ifSomethingGoesWrong(
                        "The prompt answers with the file's name and extension, in the form \"name: nothing"
                                + " installed runs a .ext\".",
                        "No installed language runs files of that kind. Compile a source file with sgsc or scc"
                                + " first, or install the package that runs that kind of file"
                                + " ([the programs page](jsc:programs)).",
                        "The prompt answers with the file's name and \"does not compile\", and gives no reason.",
                        "The prompt gave no reason. Run the compiler on the file yourself to see its messages."
                                + " [Reading an error line](jsc:sigma_reading_an_error) shows how to read them.",
                        "The prompt says \"<file> is not a <language> program\".",
                        "sigma run starts only files that end in .asm, .sgs or .sg. Start one of those instead.",
                        "The prompt answers \"sigma: nothing is running as <id>\".",
                        "The number is wrong, or that program has already ended. Type sigma ps to see the"
                                + " numbers that are running.")
                .seeAlso("jsc:sigma_run", "jsc:sigma_entry_points")
                .register();
    }

    private static void credits(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("credits_and_costs").titled("Credits").in(chapter)
                .register();
        guide.page("credits_per_tick", section).titled("How much a machine runs each tick")
                .paragraph("The central processing units (CPU) of a computer give its programs their work."
                        + " Each tick, the computer turns its processors into credits: the number of"
                        + " instructions its programs may spend in that tick. More cores and a faster clock"
                        + " mean more credits.")
                .paragraph("The computer adds up, for every processor in its build, the number of cores times"
                        + " the clock speed in megahertz (MHz). Cores are the parts of a processor that work at"
                        + " the same time. See [Processors](jsc:processors). If your programs run slowly, check"
                        + " the processors first.")
                .paragraph("Then it divides that sum by eight, dropping any fraction. A computer with a processor"
                        + " always gets at least 32 credits. A computer with no processor in its build gets no"
                        + " credits, so its programs run nothing. There is no upper limit: a faster processor gets more"
                        + " credits.")
                .table("Credits for some processors", "Cores times MHz, summed", "Credits in one tick")
                .row(GuideCell.words("0, with no processor in the build"), GuideCell.words("0"))
                .row(GuideCell.words("128 (1 core at 128 MHz)"), GuideCell.words("32, the least a computer gets"))
                .row(GuideCell.words("256 (2 cores at 128 MHz)"), GuideCell.words("32"))
                .row(GuideCell.words("2,048 (4 cores at 512 MHz)"), GuideCell.words("256"))
                .row(GuideCell.words("12,000 (6 cores at 2,000 MHz)"), GuideCell.words("1,500"))
                .paragraph("A processor's tooltip also shows a capacity in items a tick (it/t). That number"
                        + " measures the network's work, not the speed of a program. A program's speed is the"
                        + " credits above. See [Processors](jsc:processors).")
                .define("Credit", "The number of instructions a computer's processors give its programs in one tick.")
                .subheading("Each tick")
                .paragraph("On every tick the computer checks whether it runs. A computer that is switched on,"
                        + " with a valid build, runs its programs. A computer that is switched off, or whose"
                        + " parts no longer make a valid build, stops all of them. Each one is given its goodbye,"
                        + " within a budget.")
                .subheading("Turns within a tick")
                .paragraph("The credits of a tick are shared among the programs in rounds. In each round, a"
                        + " program takes up to 64 credits, or an even share of what is left when that is less."
                        + " A program that finishes or waits for something gives up its place for the rest of"
                        + " the tick.")
                .paragraph("Every program has a priority. Only the low priority changes anything: a low program"
                        + " sits out every other round. Give the priority as the third argument of"
                        + " Program.Start, after the file and the arguments.")
                .table("Priorities", "Priority", "What it does")
                .row(GuideCell.code("low"), GuideCell.words("Sits out every other round of the tick."))
                .row(GuideCell.code("medium"), GuideCell.words("The default, which runs in every round."))
                .row(GuideCell.code("high"), GuideCell.words("Runs in every round, as medium does."))
                .subheading("Work for other computers")
                .paragraph("A computer that does work for a [ComputerCraft](jsc:computercraft_side) computer,"
                        + " through a [Network Gateway](jsc:network_gateway), pays for that work out of its own"
                        + " credits. Its programs get fewer credits in the next ticks until the debt is paid.")
                .paragraph("A debt larger than one tick's credits carries over to the tick after. A computer with"
                        + " no programs still pays its debts, one tick at a time.")
                .subheading("The goodbye budget")
                .paragraph("A stopped program runs its goodbye out of a budget of 4,096 instructions per tick."
                        + " All the programs stopped in that tick share the budget. A program that runs past its"
                        + " share stops before it can run its farewell code.")
                .paragraph("When a computer is switched off, or its build is no longer valid, the budget is split"
                        + " evenly among the programs that are stopped.")
                .ifSomethingGoesWrong(
                        "The programs run nothing at all.",
                        "The computer has no processor in its build, or its build is not valid. Put a processor"
                                + " in its build and make the build valid.",
                        "A program is slow on an old computer.",
                        "Its processors have few cores or a slow clock, so it gets fewer credits. Install"
                                + " processors with more cores or a faster clock ([Processors](jsc:processors)).",
                        "Some programs stop without their goodbye.",
                        "Many programs stopped in the same tick share the budget. Stop fewer programs at a time.")
                .seeAlso("jsc:call_costs", "jsc:saved_with_world")
                .register();
        guide.page("call_costs", section).titled("What each call costs")
                .paragraph("A program reaches the computer, the network and other computers through calls. Each"
                        + " call you make costs its one instruction, plus a price on top. The price depends on how much"
                        + " work the call does, so a call that reads a whole file costs more than one that reads"
                        + " a name.")
                .paragraph("The price has three parts. The fixed part is always paid. A row is one entry of a"
                        + " list that the call brings back, such as one file name, and each row adds the second"
                        + " part. A block is 4,096 bytes, or 4 kilobytes (KB), of memory that the call reads or"
                        + " writes. Each block adds the third part, and a part of a block counts as a whole"
                        + " block.")
                .paragraph("A call that only touches the program itself is free, so only its instruction is"
                        + " paid. Printing to the console and reading typed lines are free, and so are the calls"
                        + " that look at the program's own name and arguments.")
                .subheading("The price levels")
                .paragraph("Every call is priced at one of these levels. The same levels apply on every"
                        + " computer.")
                .table("The price levels", "Level", "Price in instructions", "What it is for")
                .row(GuideCell.words("Glance"), GuideCell.words("5"),
                        GuideCell.words("a look at something the computer already knows about itself"))
                .row(GuideCell.words("Glance at the network"), GuideCell.words("10"),
                        GuideCell.words("a look at something the network already knows"))
                .row(GuideCell.words("Gather"), GuideCell.words("30"),
                        GuideCell.words("gathering what the computer holds: its disks, its programs and what"
                                + " it runs"))
                .row(GuideCell.words("Read"), GuideCell.words("50"),
                        GuideCell.words("reading from a disk or the network"))
                .row(GuideCell.words("Read, per 4 KB"), GuideCell.words("1"),
                        GuideCell.words("added to a read for every block read"))
                .row(GuideCell.words("Write"), GuideCell.words("100"),
                        GuideCell.words("writing, which cannot be taken back"))
                .row(GuideCell.words("Write, per 4 KB"), GuideCell.words("2"),
                        GuideCell.words("added to a write for every block written"))
                .row(GuideCell.words("Draw"), GuideCell.words("50"),
                        GuideCell.words("changing what a window shows, which the computer then draws again"))
                .row(GuideCell.words("Draw, per KB"), GuideCell.words("10"),
                        GuideCell.words("added for every kilobyte of a value given to a window component"))
                .row(GuideCell.words("Submit"), GuideCell.words("200"),
                        GuideCell.words("asking the network to do work for the whole network"))
                .row(GuideCell.words("Start a thread"), GuideCell.words("49"),
                        GuideCell.words("starting a thread, on top of the call"))
                .row(GuideCell.words("Call across a Gateway"), GuideCell.words("100"),
                        GuideCell.words("calling a computer of another mod through a Gateway"))
                .row(GuideCell.words("Each extra argument across a Gateway"), GuideCell.words("5"),
                        GuideCell.words("for each thing handed across a Gateway"))
                .row(GuideCell.words("Send across a Gateway"), GuideCell.words("20"),
                        GuideCell.words("saying something to a computer across a Gateway, or turning one on"
                                + " or off"))
                .paragraph("File calls use the same levels. Reading a file costs the read price, plus the price"
                        + " for each block it reads.")
                .subheading("Calls for programs and processes")
                .table("Program and process calls", "Call", "Price in instructions")
                .row(GuideCell.code("Program.SetName(name)"), GuideCell.words("free"))
                .row(GuideCell.code("Program.Name, Program.Args, Program.Current"), GuideCell.words("free"))
                .row(GuideCell.code("Program.Exit(code)"), GuideCell.words("free"))
                .row(GuideCell.code("Program.OnMessage(handler)"), GuideCell.words("free"))
                .row(GuideCell.code("Program.Start(path)"),
                        GuideCell.words("200, and the longer forms, with arguments and a priority, cost the same."))
                .row(GuideCell.code("Program.Shell(line)"), GuideCell.words("200, plus 1 per row"))
                .row(GuideCell.code("Process.Id, Process.Name, Process.Host"), GuideCell.words("free"))
                .row(GuideCell.code("Process.Running"), GuideCell.words("5"))
                .row(GuideCell.code("Process.ExitCode"), GuideCell.words("5"))
                .row(GuideCell.code("Process.Wait()"), GuideCell.words("free, and so is the form with ticks."))
                .row(GuideCell.code("Process.Kill()"), GuideCell.words("10"))
                .row(GuideCell.code("Process.Output()"), GuideCell.words("50, plus 1 per row"))
                .row(GuideCell.code("Process.Send(number, text)"), GuideCell.words("10"))
                .row(GuideCell.code("Thread.Start(action)"), GuideCell.words("49"))
                .row(GuideCell.code("Thread.Sleep, Thread.Yield, Thread.Join, Thread.Stop"),
                        GuideCell.words("free"))
                .row(GuideCell.code("Thread.Id, Thread.Current"), GuideCell.words("free"))
                .subheading("Calls for the computer and the network")
                .table("Computer, network and remote calls", "Call", "Price in instructions")
                .row(GuideCell.code("Computer.Name, Cpu, Os, RamMb, FreeRamMb, Online"), GuideCell.words("5"))
                .row(GuideCell.code("Computer.Disks, Programs, Processes"), GuideCell.words("30"))
                .row(GuideCell.code("Network.Online, Network.Current, Network.Computer(name)"),
                        GuideCell.words("10"))
                .row(GuideCell.code("Network.Capacity, Network.Used, Network.Total(name)"), GuideCell.words("50"))
                .row(GuideCell.code("Network.Computers, Network.Find(name), Network.Servers"),
                        GuideCell.words("50, plus 1 per row"))
                .row(GuideCell.code("RemoteComputer.Start(path)"), GuideCell.words("200"))
                .row(GuideCell.code("RemoteComputer.Shell(line)"), GuideCell.words("200"))
                .row(GuideCell.code("RemoteComputer.Send(number, text)"), GuideCell.words("10"))
                .row(GuideCell.code("RemoteComputer.Processes()"), GuideCell.words("50, plus 1 per row"))
                .row(GuideCell.code("Gateway.Computers"), GuideCell.words("30, plus 1 per row"))
                .row(GuideCell.code("Gateway.Send(number, text)"), GuideCell.words("20"))
                .row(GuideCell.code("Gateway.OnMessage(handler)"), GuideCell.words("free"))
                .ifSomethingGoesWrong(
                        "A program slows down when it reads a long list.",
                        "A call that brings back a list costs more as the list grows, one row at a time. Ask for"
                                + " less, or make the call once and keep its answer.",
                        "A call that reads a big file costs more than expected.",
                        "Reading a file costs 50 instructions, plus 1 for each 4 KB block. A 40 KB file is ten"
                                + " blocks, so reading it with File.Read costs 60 instructions.")
                .seeAlso("jsc:credits_per_tick", "jsc:what_operations_are")
                .register();
    }

    private static void limits(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("running_limits").titled("Limits").in(chapter).register();
        guide.page("sigma_limits", section).titled("The limits of a running program")
                .paragraph("A running program has limits. They stop one program from taking all of a computer's"
                        + " memory or all of its time. Most limits are fixed. Two of the time limits can be"
                        + " changed in the config file jstech-balance.toml.")
                .paragraph("Memory is the first limit. Each program has a heap: the memory it holds for the"
                        + " objects it makes, such as text and lists. Every object counts against the memory the"
                        + " program was given. Without --heap, a program gets 1 MB. The most you can give it is"
                        + " 64 MB.")
                .table("Memory", "Limit", "Value", "What happens past it")
                .row(GuideCell.words("Memory given to a program with --heap"),
                        GuideCell.words("1 MB by default, 64 MB at most"),
                        GuideCell.words("A larger number is cut down to 64 MB."))
                .row(GuideCell.words("Memory free on the computer"),
                        GuideCell.words("The computer's memory, less what is in use"),
                        GuideCell.words("The program does not start, and the prompt says it will not fit."))
                .row(GuideCell.words("Text in the heap"),
                        GuideCell.words("16 bytes, plus 2 bytes for each character"),
                        GuideCell.words("It counts against the heap."))
                .row(GuideCell.words("Each object in the heap"),
                        GuideCell.words("16 bytes, plus 8 bytes for each link to another object"),
                        GuideCell.words("It counts against the heap."))
                .row(GuideCell.words("A message or a click"), GuideCell.words("40 bytes, plus its text"),
                        GuideCell.words("It counts against the heap of the program that receives it."))
                .paragraph("The config file jstech-balance.toml holds two keys: balance.program_machine_micros,"
                        + " for one computer, and balance.program_server_micros, for all the computers together."
                        + " Both are times in microseconds.")
                .table("Time and turns", "Limit", "Value", "What happens past it")
                .row(GuideCell.words("Instructions in one turn of a thread"), GuideCell.words("64"),
                        GuideCell.words("The turn passes to the next thread, which is not a failure."))
                .row(GuideCell.words("Credits in one round of a tick"), GuideCell.words("64"),
                        GuideCell.words("The round passes to the next program, which is not a failure."))
                .row(GuideCell.words("Work between two checks of the clock"), GuideCell.words("512 units"),
                        GuideCell.words("The tick ends early when its time runs out."))
                .row(GuideCell.words("Time per tick, for one computer"),
                        GuideCell.words("1,000 microseconds (1 millisecond) by default; from 50 to 1,000,000"
                                + " microseconds in the config"),
                        GuideCell.words("The computer stops its programs for the rest of that tick."))
                .row(GuideCell.words("Time per tick, for all computers together"),
                        GuideCell.words("8,000 microseconds (8 milliseconds) by default; from 100 to"
                                + " 10,000,000 microseconds in the config"),
                        GuideCell.words("No computer runs its programs in that tick, and the computers go first"
                                + " on the next tick."))
                .row(GuideCell.words("Farewell budget per tick"), GuideCell.words("4,096 instructions, shared"),
                        GuideCell.words("A program over its share stops before it can run its farewell code."))
                .row(GuideCell.words("Calls one inside another"), GuideCell.words("1,024"),
                        GuideCell.words("The program halts, and the message says that a method may be calling"
                                + " itself without end."))
                .table("Calls and consoles", "Limit", "Value", "What happens past it")
                .row(GuideCell.words("Calls waiting for a program"), GuideCell.words("256 calls"),
                        GuideCell.words("The new call or message is refused."))
                .row(GuideCell.words("Text of the calls waiting for a program"),
                        GuideCell.words("64 KB (65,536 bytes)"),
                        GuideCell.words("The new call or message is refused."))
                .row(GuideCell.words("Lines typed ahead for a program"), GuideCell.words("16 lines"),
                        GuideCell.words("The typed line is not taken."))
                .row(GuideCell.words("Lines kept on a console"), GuideCell.words("200 lines"),
                        GuideCell.words("The oldest lines are dropped."))
                .row(GuideCell.words("Characters kept on a console"), GuideCell.words("64,000 characters"),
                        GuideCell.words("The oldest lines are dropped."))
                .row(GuideCell.words("Length of one console line"), GuideCell.words("4,096 characters"),
                        GuideCell.words("The line is cut short, and the cut is marked with dots."))
                .row(GuideCell.words("Windows a program may have open"), GuideCell.words("8"),
                        GuideCell.words("The program halts, saying that it may have 8 windows open at once."))
                .row(GuideCell.words("Folders a computer shares"), GuideCell.words("16 folders"),
                        GuideCell.words("The prompt says that the computer already shares that many folders."))
                .table("Windows", "Limit", "Most allowed", "What happens past it")
                .row(GuideCell.words("Widgets in a window"), GuideCell.words("256"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Rows in a table widget"), GuideCell.words("1,024"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Columns in a table widget"), GuideCell.words("8"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Tabs in a tab view"), GuideCell.words("64"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Sections in a widget"), GuideCell.words("4"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Points in a drawing"), GuideCell.words("120"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Lines in a log view"), GuideCell.words("256"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Strokes in a canvas"), GuideCell.words("4,096"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Area of a canvas"), GuideCell.words("4,096"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Entries in a menu"), GuideCell.words("64"),
                        GuideCell.words("The window refuses the change."))
                .row(GuideCell.words("Size of a window"),
                        GuideCell.words("640 wide and 360 tall at most; each side at least 60"),
                        GuideCell.words("The window refuses the change."))
                .define("Heap", "The memory a running program holds for the objects it makes, such as text and"
                        + " lists. Its size is the memory the program was given.")
                .ifSomethingGoesWrong(
                        "The prompt says that the program will not fit in the free memory.",
                        "Close a program or a window, add memory to the computer ([Memory](jsc:memory)), or start"
                                + " the program with a smaller --heap.",
                        "The program halts with a message that begins \"out of memory:\" and names the lines that"
                                + " hold the most memory.",
                        "The program holds too much at once. Make it hold less, or give it more with --heap, up to"
                                + " 64 MB.",
                        "A console loses its oldest lines.",
                        "A console keeps 200 lines or 64,000 characters. Print less, or read the output sooner.",
                        "The program halts, saying that it may have 8 windows open at once.",
                        "Close one of the windows before you open another.",
                        "A program stops for some ticks on a busy server.",
                        "The time allowed per tick is used up. Raise the balance keys in jstech-balance.toml, if"
                                + " the server can spare the time.")
                .seeAlso("jsc:memory", "jsc:credits_per_tick", "jsc:sigma_runtime_stops")
                .register();
    }

    private static void saving(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("saving").titled("Saving").in(chapter).register();
        guide.page("saved_with_world", section).titled("Programs and the world's save")
                .paragraph("Your programs are kept when the world is saved. The computer writes its running"
                        + " programs into its own save data, which is the part of the world's save that belongs"
                        + " to that computer. Its disks and its settings are saved there too.")
                .paragraph("Each program is written down in full, so that it can carry on. When the world comes"
                        + " back, a program carries on from where it stopped. It does not start again from the"
                        + " beginning.")
                .table("What a save keeps for each program", "Part", "What it means")
                .row(GuideCell.words("Memory"),
                        GuideCell.words("Every object the program holds, and the links between them."))
                .row(GuideCell.words("Threads"), GuideCell.words("Each thread, and what it was doing."))
                .row(GuideCell.words("Console"),
                        GuideCell.words("The lines on the console, and the lines typed ahead."))
                .row(GuideCell.words("Waiting calls"), GuideCell.words("The calls that wait to run in the program."))
                .row(GuideCell.words("Windows"), GuideCell.words("The windows the program has open."))
                .row(GuideCell.words("Listing"), GuideCell.words("The program's listing, identified by its checksum."))
                .row(GuideCell.words("Parent"),
                        GuideCell.words("The program that started it, if another program did."))
                .paragraph("The next free number is saved too, so no number is given out twice.")
                .subheading("Switching the computer off")
                .paragraph("Switching a computer off is different. Its programs are stopped, each with its"
                        + " goodbye, and they are not kept for when it comes back on. When you turn it on again,"
                        + " you start the programs again.")
                .subheading("Reading the save back")
                .paragraph("When the save is read back, the computer checks it. If the save came from another"
                        + " version of the mod, or a listing changed since its program started, the computer"
                        + " leaves out every program of that computer.")
                .paragraph("A program whose language is no longer installed is left out alone. So is a program"
                        + " whose language does not take what it wrote. The other programs are not affected.")
                .paragraph("A program started from another computer keeps its link to the program that started"
                        + " it. After a reload, that parent is still told when the child ends.")
                .define("Checksum", "A number worked out from a text. The same text always gives the same number,"
                        + " and a changed text gives another one.")
                .ifSomethingGoesWrong(
                        "The prompt says \"the programs that were running could not be brought back from the save\".",
                        "The save came from another version of the mod, or a listing changed since it started."
                                + " Start the programs again.",
                        "A program in a language that is no longer installed is gone after a reload.",
                        "Its language is no longer installed, so the program is left out. Install that language.",
                        "A program is gone after the computer was switched off.",
                        "It was stopped, not kept. Start it again.")
                .seeAlso("jsc:sigma_run", "jsc:running_programs")
                .register();
    }

    private static void talking(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("messages_and_sharing").titled("Talking").in(chapter)
                .register();
        guide.page("program_messages", section).titled("Programs talking to each other")
                .paragraph("Programs talk to each other with messages. A message is a line of text that one"
                        + " program sends to another program's number. The other program can run on the same"
                        + " computer, on another computer of the network, or be a"
                        + " [ComputerCraft](jsc:computercraft_side) computer reached through a"
                        + " [Network Gateway](jsc:network_gateway).")
                .subheading("On one computer")
                .paragraph("A program sends a message with Process.Send, giving the number of the other program"
                        + " and the text. To receive messages, a program sets a handler with Program.OnMessage:"
                        + " the steps that run for each message. The handler gets the number of the sender (From),"
                        + " the text (Text) and the tick it arrived on (Tick).")
                .table("Messages", "Call", "Price in instructions", "What it does")
                .row(GuideCell.code("Process.Send(number, text)"), GuideCell.words("10"),
                        GuideCell.words("Sends the text to the program with that number on this computer."))
                .row(GuideCell.code("Program.OnMessage(handler)"), GuideCell.words("free"),
                        GuideCell.words("Runs the handler for each message that arrives."))
                .row(GuideCell.code("Gateway.Send(number, text)"), GuideCell.words("20"),
                        GuideCell.words("Sends the text to the ComputerCraft computer with that number."))
                .row(GuideCell.code("Gateway.OnMessage(handler)"), GuideCell.words("free"),
                        GuideCell.words("Runs the handler for each message that comes through a Gateway."))
                .paragraph("If the program you send to has no handler, it takes the message and throws it away,"
                        + " so Process.Send still gives true. Process.Send gives false when no running program has"
                        + " that number.")
                .paragraph("A message is refused when the program it is sent to has finished or halted, or when"
                        + " its waiting calls are full: 256 calls, or 64 KB of text. Then Process.Send gives"
                        + " false.")
                .paragraph("A message to a Gateway reaches every program on the computer that is listening for"
                        + " it. A computer where no program listens drops the message.")
                .subheading("Other computers on the network")
                .paragraph("Network.Computers lists the computers on the network. Network.Computer(name) gives one"
                        + " of them by its name. Each one has a host name, a name, a type, an operating system and"
                        + " an online state.")
                .paragraph("A program on another computer is reached with the remote calls below. Before any of"
                        + " them works, the other computer must be on the network and switched on. It must also"
                        + " take programs from other computers: the remote setting decides that, and it is on by"
                        + " default. Set it to on or off. A remote Start also needs a computer that runs programs.")
                .table("Calls on another computer", "Call", "Price in instructions", "What it does")
                .row(GuideCell.code("RemoteComputer.Start(path)"), GuideCell.words("200"),
                        GuideCell.words("Starts a program there, the way Program.Start does here."))
                .row(GuideCell.code("RemoteComputer.Shell(line)"), GuideCell.words("200"),
                        GuideCell.words("Runs one line at that computer's prompt."))
                .row(GuideCell.code("RemoteComputer.Send(number, text)"), GuideCell.words("10"),
                        GuideCell.words("Sends the text to the program with that number there."))
                .row(GuideCell.code("RemoteComputer.Processes()"), GuideCell.words("50, plus 1 per row"),
                        GuideCell.words("Lists the programs running there."))
                .paragraph("A program started on another computer records the caller as its parent. When it ends,"
                        + " the caller is told. The caller reads its output and its exit code with Process.Output"
                        + " and Process.ExitCode, while that program is still there to be read.")
                .paragraph("A remote program's Process value carries the other computer's host name in"
                        + " Process.Host, and calls on that value go to that computer. If the host is unknown or"
                        + " switched off, Process.Running gives false and Process.Kill does nothing.")
                .subheading("Shared folders")
                .paragraph("A computer shares a folder with the network from its prompt, with the config command."
                        + " The share takes the folder's own name. It is read only, unless you add write.")
                .table("Sharing commands", "Command", "What it does")
                .row(GuideCell.code("config share <folder> [read|write]"),
                        GuideCell.words("Opens the folder to the network, which can only read it unless write"
                                + " is given."))
                .row(GuideCell.code("config unshare <name>"), GuideCell.words("Closes the share with that name."))
                .paragraph("The prompt answers with the address of the share, such as \\\\host\\name, and says"
                        + " whether it is read only or read and write. A path on another computer starts with two"
                        + " backslashes, then the host name, then the share and the rest of the path:"
                        + " \\\\host\\share\\folder\\file. The other computer answers for its own disks.")
                .paragraph("A copy may use a shared folder at either end. The file is read where it is and written"
                        + " where it goes. The shares of a computer are also covered in"
                        + " [Shared folders](jsc:shared_folders).")
                .ifSomethingGoesWrong(
                        "Process.Send gives false.",
                        "The program has finished or halted, or its waiting calls are full. Try again later, or"
                                + " send less text.",
                        "The prompt says that a computer is powered off.",
                        "Switch that computer on, then try again.",
                        "The prompt says that a computer does not take programs from other computers.",
                        "Its remote setting is off. Turn that setting on at that computer. It is on by default.",
                        "The prompt says that a computer cannot run programs.",
                        "That computer does not run programs, so no program starts there. Start the program on a"
                                + " computer that runs programs.",
                        "The prompt says that no such computer is on this network.",
                        "The name is wrong, or the computer is not on this network. Network.Computers lists the"
                                + " computers on it.",
                        "The prompt says that a host name matches more than one machine.",
                        "Use the host name of one machine, or its node id, which is its"
                                + " [network identity](jsc:network_identity).",
                        "The prompt says that a folder is shared read-only.",
                        "The folder was shared without write. Its owner runs config unshare on it, then config"
                                + " share with write.",
                        "The prompt says that this computer already shares 16 folders.",
                        "Unshare a folder with config unshare to make room for another share.",
                        "The prompt says that another folder is already shared under that name.",
                        "A share takes the folder's name, so only one folder can have it. Unshare the other folder"
                                + " first.",
                        "The prompt says that a host is not found on this network.",
                        "Check the host name. Network.Computers lists the computers on the network.",
                        "The prompt says that the machine of a path is powered off.",
                        "Switch that computer on.",
                        "The prompt says that there is no such share on that host.",
                        "That computer shares no folder with that name. Ask its owner for the name of the share.",
                        "The prompt says that a share has to be named.",
                        "Write the path in the form host, then share, then the rest of the path:"
                                + " \\\\host\\share\\folder.")
                .seeAlso("jsc:network_gateway", "jsc:shared_folders", "jsc:remote_access")
                .register();
    }
}
