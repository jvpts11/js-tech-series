/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import java.util.ArrayList;
import java.util.List;

/**
 * The types of the language's library and of the machine a program runs on, declared once: what the compiler lets a
 * program write and what an editor offers.
 *
 * <p>Only declarations live here, with nothing of the runtime or the world in them, so the compiler and the editors
 * read them with no machine anywhere; what answers each call is bound to it elsewhere. The language's own core (text,
 * the two collections, the delegates and the entry point) is declared by the compiler itself.
 */
public final class SystemApi {

    private static final String IO = "System.IO";
    private static final String UTILS = "System.Utils";
    private static final String EXECUTION = "System.Execution";
    private static final String THREADING = "System.Threading";
    private static final String MACHINE = "System.Machine";
    private static final String NETWORK = "System.Network";
    private static final String OPERATIONS = "System.Operations";
    private static final String UI = "System.UI";

    private static final String VOID = "void";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BOOL = "bool";
    private static final String STRING = "string";
    private static final String OBJECT = "object";
    private static final String STRINGS = "List<string>";
    private static final String ROWS = "List<Map<string, object>>";
    private static final String WIDGET = "Widget";

    private static final List<TypeSpec> TYPES = List.of(math(), convert(), console(), program(), process(),
            processMessage(), thread(), time(), random(), file(), cpuInfo(), diskInfo(), osInfo(), processInfo(),
            computer(), holdingInfo(), serverInfo(), network(), stockEvent(), subscription(), remoteComputer(),
            iqlResult(), iql(), workStat(), mainframe(), askResult(), operationInfo(), operations(), ccComputer(),
            ccPeripheral(), gatewayMessage(), gateway(), widget(), window(), box("Row"), box("Column"), label(),
            button(), textBox(), checkBox(), progressBar(), listBox(), canvas(), messageBox());

    private SystemApi() {
    }

    /** Every type, in the order it was declared. */
    public static List<TypeSpec> types() {
        return TYPES;
    }

    /** The type of that name, or null when the system declares none. */
    public static TypeSpec type(final String name) {
        for (final TypeSpec type : TYPES) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    private static TypeSpec math() {
        final Members math = new Members("Math");
        math.pure(INT, "Abs", INT);
        math.pure(DOUBLE, "Abs", DOUBLE);
        math.pure(INT, "Min", INT, INT);
        math.pure(DOUBLE, "Min", DOUBLE, DOUBLE);
        math.pure(INT, "Max", INT, INT);
        math.pure(DOUBLE, "Max", DOUBLE, DOUBLE);
        math.pure(INT, "Clamp", INT, INT, INT);
        math.pure(DOUBLE, "Clamp", DOUBLE, DOUBLE, DOUBLE);
        math.pure(DOUBLE, "Floor", DOUBLE);
        math.pure(DOUBLE, "Ceil", DOUBLE);
        math.pure(DOUBLE, "Round", DOUBLE);
        math.pure(DOUBLE, "Sqrt", DOUBLE);
        math.pure(DOUBLE, "Pow", DOUBLE, DOUBLE);
        return new TypeSpec(UTILS, "Math", math.members);
    }

    private static TypeSpec convert() {
        final Members convert = new Members("Convert");
        convert.pure(INT, "ToInt", STRING);
        convert.pure(LONG, "ToLong", STRING);
        convert.pure(FLOAT, "ToFloat", STRING);
        convert.pure(DOUBLE, "ToDouble", STRING);
        convert.pure(BOOL, "ToBool", STRING);
        convert.pure(STRING, "ToString", OBJECT);
        /*
         * The Try forms answer whether the text was a value and hand the value out sideways, for a program that would
         * rather ask again than stop on a line somebody mistyped.
         */
        convert.pure(BOOL, "TryInt", STRING, "out " + INT);
        convert.pure(BOOL, "TryLong", STRING, "out " + LONG);
        convert.pure(BOOL, "TryDouble", STRING, "out " + DOUBLE);
        convert.pure(BOOL, "TryBool", STRING, "out " + BOOL);
        return new TypeSpec(UTILS, "Convert", convert.members);
    }

    private static TypeSpec console() {
        final Members console = new Members("Console");
        console.onType(VOID, "Print", MemberKind.PROCESS, CallCost.FREE, STRING);
        console.onType(VOID, "PrintLine", MemberKind.PROCESS, CallCost.FREE, STRING);
        console.onType(VOID, "Clear", MemberKind.PROCESS, CallCost.FREE);
        /*
         * Reading waits: a program that asks for a line stops until one is typed at the terminal it is in front of.
         * HasLine asks without waiting, for a program that has other things to do meanwhile.
         */
        console.onType(STRING, "ReadLine", MemberKind.PROCESS, CallCost.FREE);
        console.onType(BOOL, "HasLine", MemberKind.PROCESS, CallCost.FREE);
        /*
         * The same wait, with the line read as a value: a program asking for a number gets one, and a line that is
         * not one stops the program with the text it could not read, as Convert would.
         */
        console.onType(INT, "ReadInt", MemberKind.PROCESS, CallCost.FREE);
        console.onType(LONG, "ReadLong", MemberKind.PROCESS, CallCost.FREE);
        console.onType(DOUBLE, "ReadDouble", MemberKind.PROCESS, CallCost.FREE);
        console.onType(BOOL, "ReadBool", MemberKind.PROCESS, CallCost.FREE);
        return new TypeSpec(IO, "Console", console.members);
    }

    /**
     * The program itself, as a thing it can speak about. A program that says what it is called is listed by that name
     * on the machine's process list; one that does not is listed by the runtime. What a program says about itself
     * costs nothing; starting another one is dear, since a whole process is made.
     */
    private static TypeSpec program() {
        final Members program = new Members("Program");
        program.onType(VOID, "SetName", MemberKind.PROCESS, CallCost.FREE, STRING);
        program.valueOnType(STRING, "Name", MemberKind.PROCESS, CallCost.FREE);
        program.valueOnType(STRINGS, "Args", MemberKind.PROCESS, CallCost.FREE);
        program.onType(VOID, "Exit", MemberKind.PROCESS, CallCost.FREE, INT);
        program.valueOnType(LONG, "DroppedEvents", MemberKind.PROCESS, CallCost.FREE);
        program.onType("Process", "Start", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING);
        program.onType("Process", "Start", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING, STRINGS);
        program.onType("Process", "Start", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING, STRINGS,
                STRING);
        program.valueOnType("Process", "Current", MemberKind.PROCESS, CallCost.FREE);
        /*
         * A line at this machine's own prompt, run to the end, and what it printed. It is the same thing a remote
         * computer is asked for, asked of the machine the program is standing on instead, and priced as a program
         * started and waited for.
         */
        program.onType(STRINGS, "Shell", MemberKind.WORLD, CallCost.perRow(SigmaCosts.SUBMIT), STRING);
        /*
         * A program handed over as text rather than named on a disk: it is read, run to its end, and says how it
         * went, the same as one started by name. That is a start and a read together.
         */
        program.onType("Process", "RunSource", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT + SigmaCosts.READ),
                STRING, STRINGS, STRING);
        program.onType(VOID, "OnMessage", MemberKind.PROCESS, CallCost.FREE, "Action<ProcessMessage>");
        return new TypeSpec(EXECUTION, "Program", program.members);
    }

    /**
     * Another program on the same machine, as the one that started it holds it: a number, a name, and the machine's
     * word on how it is getting on. Reading its output or its exit code is asking the machine, which is why they cost
     * what a look at the machine costs.
     */
    private static TypeSpec process() {
        final Members process = new Members("Process");
        process.valueOnObject(INT, "Id", MemberKind.PROCESS, CallCost.FREE);
        process.valueOnObject(STRING, "Name", MemberKind.PROCESS, CallCost.FREE);
        process.valueOnObject(STRING, "Host", MemberKind.PROCESS, CallCost.FREE);
        process.valueOnObject(BOOL, "Running", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        process.valueOnObject(INT, "ExitCode", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        process.onObject(VOID, "Wait", MemberKind.PROCESS, CallCost.FREE);
        process.onObject(BOOL, "Wait", MemberKind.PROCESS, CallCost.FREE, LONG);
        process.onObject(VOID, "Kill", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK));
        process.onObject(STRINGS, "Output", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ));
        process.onType(BOOL, "Send", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK), INT, STRING);
        return new TypeSpec(EXECUTION, "Process", process.members);
    }

    /** A line another program sent this one: who sent it, what it said, and when. */
    private static TypeSpec processMessage() {
        final Members message = new Members("ProcessMessage");
        message.recordValue(INT, "From");
        message.recordValue(STRING, "Text");
        message.recordValue(LONG, "Tick");
        return new TypeSpec(EXECUTION, "ProcessMessage", message.members);
    }

    /**
     * More than one thing at once inside one program.
     *
     * <p>A thread runs a body of its own beside the rest of the program, taking turns with it a few instructions at a
     * time, over the same memory. Nothing runs at the same instant, so a single expression is never torn; a run of
     * them can be, which is what {@code lock} is for.
     */
    private static TypeSpec thread() {
        final Members thread = new Members("Thread");
        thread.onType("Thread", "Start", MemberKind.PROCESS, CallCost.FREE, "Action");
        thread.valueOnType("Thread", "Current", MemberKind.PROCESS, CallCost.FREE);
        thread.onType(VOID, "Sleep", MemberKind.PROCESS, CallCost.FREE, LONG);
        thread.onType(VOID, "Yield", MemberKind.PROCESS, CallCost.FREE);
        thread.valueOnObject(INT, "Id", MemberKind.PROCESS, CallCost.FREE);
        thread.valueOnObject(BOOL, "Running", MemberKind.PROCESS, CallCost.FREE);
        thread.onObject(VOID, "Join", MemberKind.PROCESS, CallCost.FREE);
        thread.onObject(BOOL, "Join", MemberKind.PROCESS, CallCost.FREE, LONG);
        thread.onObject(VOID, "Stop", MemberKind.PROCESS, CallCost.FREE);
        return new TypeSpec(THREADING, "Thread", thread.members);
    }

    /** The world's clock as the machine reads it, and the one sum on it that needs no machine at all. */
    private static TypeSpec time() {
        final Members time = new Members("Time");
        time.valueOnType(LONG, "Tick", MemberKind.WORLD, CallCost.FREE);
        time.valueOnType(LONG, "DayTime", MemberKind.WORLD, CallCost.FREE);
        time.valueOnType(LONG, "Day", MemberKind.WORLD, CallCost.FREE);
        time.pure(LONG, "Ticks", INT);
        return new TypeSpec(UTILS, "Time", time.members);
    }

    /** Chance, drawn from the program's own sequence, which a program may start again from a number of its choosing. */
    private static TypeSpec random() {
        final Members random = new Members("Random");
        random.onType(INT, "Next", MemberKind.PROCESS, CallCost.FREE, INT);
        random.onType(DOUBLE, "NextDouble", MemberKind.PROCESS, CallCost.FREE);
        random.onType(VOID, "Seed", MemberKind.PROCESS, CallCost.FREE, LONG);
        return new TypeSpec(UTILS, "Random", random.members);
    }

    /**
     * The machine's own drives, reached with the paths the shell uses.
     *
     * <p>Writing can fail without the program being wrong: a disk fills up. So the writes answer whether they happened
     * rather than stopping the program, and reading something that is not there is asked for with the try form. Asking
     * whether something is there is cheap; reading costs a read, and writing twice that, because a write is a thing the
     * machine cannot take back.
     */
    private static TypeSpec file() {
        final Members file = new Members("File");
        file.onType(BOOL, "Exists", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK), STRING);
        file.onType(STRING, "Read", MemberKind.WORLD, CallCost.of(SigmaCosts.READ), STRING);
        file.onType(BOOL, "TryRead", MemberKind.WORLD, CallCost.of(SigmaCosts.READ), STRING, "out " + STRING);
        file.onType(BOOL, "Write", MemberKind.WORLD, CallCost.of(SigmaCosts.WRITE), STRING, STRING);
        file.onType(BOOL, "Append", MemberKind.WORLD, CallCost.of(SigmaCosts.WRITE), STRING, STRING);
        file.onType(BOOL, "Delete", MemberKind.WORLD, CallCost.of(SigmaCosts.WRITE), STRING);
        file.onType(BOOL, "MkDir", MemberKind.WORLD, CallCost.of(SigmaCosts.WRITE), STRING);
        file.onType(STRINGS, "List", MemberKind.WORLD, CallCost.of(SigmaCosts.READ), STRING);
        return new TypeSpec(IO, "File", file.members);
    }

    /*
     * The little records the machine answers with. They are pictures taken when they were asked for, not live views: a
     * program holds what it was told, and asks again when it wants to know again.
     */

    private static TypeSpec cpuInfo() {
        final Members cpu = new Members("CpuInfo");
        cpu.recordValue(INT, "Mhz");
        cpu.recordValue(INT, "Cores");
        cpu.recordValue(STRING, "Era");
        return new TypeSpec(MACHINE, "CpuInfo", cpu.members);
    }

    private static TypeSpec diskInfo() {
        final Members disk = new Members("DiskInfo");
        disk.recordValue(STRING, "Mount");
        disk.recordValue(LONG, "UsedMb");
        disk.recordValue(LONG, "CapacityMb");
        return new TypeSpec(MACHINE, "DiskInfo", disk.members);
    }

    private static TypeSpec osInfo() {
        final Members os = new Members("OsInfo");
        os.recordValue(STRING, "Id");
        os.recordValue(STRING, "Name");
        return new TypeSpec(MACHINE, "OsInfo", os.members);
    }

    private static TypeSpec processInfo() {
        final Members process = new Members("ProcessInfo");
        process.recordValue(INT, "Id");
        process.recordValue(STRING, "Name");
        process.recordValue(STRING, "State");
        process.recordValue(LONG, "HeldBytes");
        return new TypeSpec(MACHINE, "ProcessInfo", process.members);
    }

    /**
     * The machine the program is running on. What it is costs a glance; what it holds has to be gathered, because the
     * answer is a list the machine has to walk to build.
     */
    private static TypeSpec computer() {
        final Members computer = new Members("Computer");
        computer.valueOnType(STRING, "Name", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        computer.valueOnType("CpuInfo", "Cpu", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        computer.valueOnType("OsInfo", "Os", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        computer.valueOnType(INT, "RamMb", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        computer.valueOnType(INT, "FreeRamMb", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        computer.valueOnType(BOOL, "Online", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        computer.onType("List<DiskInfo>", "Disks", MemberKind.WORLD, CallCost.of(SigmaCosts.GATHER));
        computer.onType(STRINGS, "Programs", MemberKind.WORLD, CallCost.of(SigmaCosts.GATHER));
        computer.onType("List<ProcessInfo>", "Processes", MemberKind.WORLD, CallCost.of(SigmaCosts.GATHER));
        return new TypeSpec(MACHINE, "Computer", computer.members);
    }

    private static TypeSpec holdingInfo() {
        final Members holding = new Members("HoldingInfo");
        holding.recordValue(STRING, "Server");
        holding.recordValue(LONG, "Quantity");
        return new TypeSpec(NETWORK, "HoldingInfo", holding.members);
    }

    private static TypeSpec serverInfo() {
        final Members server = new Members("ServerInfo");
        server.recordValue(STRING, "Name");
        server.recordValue(LONG, "Stored");
        server.recordValue(LONG, "Capacity");
        return new TypeSpec(NETWORK, "ServerInfo", server.members);
    }

    /**
     * The data network the machine is on.
     *
     * <p>{@code Online} and {@code Current} answer on any machine, because whether there is a network is a fair
     * question anywhere. Everything else needs one, and says so if there is none. Whether there is one is a glance;
     * what is on it is a read, and a read that brings back rows is priced by how many.
     */
    private static TypeSpec network() {
        final Members network = new Members("Network");
        network.valueOnType(BOOL, "Online", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK));
        network.valueOnType(STRING, "Current", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK));
        network.valueOnType(LONG, "Capacity", MemberKind.WORLD, CallCost.of(SigmaCosts.READ));
        network.valueOnType(LONG, "Used", MemberKind.WORLD, CallCost.of(SigmaCosts.READ));
        network.onType(LONG, "Total", MemberKind.WORLD, CallCost.of(SigmaCosts.READ), STRING);
        network.onType(STRINGS, "Types", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ));
        network.onType("List<HoldingInfo>", "Find", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ), STRING);
        network.onType("List<ServerInfo>", "Servers", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ));
        /*
         * Being told beats asking. A program that wants to know when the iron runs low says so once and is called when
         * it happens, instead of asking every tick for the rest of the world's life. The program keeps what it asked to
         * be told about, and asking costs nothing, and is meant to.
         */
        network.onType("Subscription", "Watch", MemberKind.PROCESS, CallCost.FREE, STRING, "Action<StockEvent>");
        network.onType("Subscription", "WatchBelow", MemberKind.PROCESS, CallCost.FREE, STRING, LONG,
                "Action<StockEvent>");
        network.onType("Subscription", "WatchAbove", MemberKind.PROCESS, CallCost.FREE, STRING, LONG,
                "Action<StockEvent>");
        network.onType("List<RemoteComputer>", "Computers", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ));
        network.onType("RemoteComputer", "Computer", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK), STRING);
        return new TypeSpec(NETWORK, "Network", network.members);
    }

    private static TypeSpec stockEvent() {
        final Members event = new Members("StockEvent");
        event.recordValue(STRING, "Item");
        event.recordValue(LONG, "Total");
        event.recordValue(LONG, "Previous");
        return new TypeSpec(NETWORK, "StockEvent", event.members);
    }

    private static TypeSpec subscription() {
        final Members subscription = new Members("Subscription");
        subscription.recordValue(INT, "Id");
        subscription.recordValue(STRING, "Item");
        return new TypeSpec(NETWORK, "Subscription", subscription.members);
    }

    /**
     * Another computer on the network, and what a program may do on it: start a program there (a handle like a local
     * one comes back), run a line at its prompt, send a line to a program of its. The other machine says whether it
     * takes any of that. What it is costs nothing once it is in hand; starting a program there or running a line at
     * its prompt is work for that machine and priced like a submission; a line sent to it is a touch; its process list
     * is a list.
     */
    private static TypeSpec remoteComputer() {
        final Members remote = new Members("RemoteComputer");
        remote.recordValue(STRING, "Host");
        remote.recordValue(STRING, "Name");
        remote.recordValue(STRING, "Type");
        remote.recordValue(STRING, "Os");
        remote.recordValue(BOOL, "Online");
        remote.onObject("Process", "Start", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING);
        remote.onObject("Process", "Start", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING, STRINGS);
        remote.onObject("Process", "Start", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING, STRINGS,
                STRING);
        remote.onObject(STRINGS, "Shell", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING);
        remote.onObject(BOOL, "Send", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK), INT, STRING);
        remote.onObject("List<Process>", "Processes", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ));
        return new TypeSpec(NETWORK, "RemoteComputer", remote.members);
    }

    private static TypeSpec iqlResult() {
        final Members result = new Members("IqlResult");
        result.recordValue(BOOL, "Ok");
        result.recordValue(STRING, "Message");
        result.recordValue(ROWS, "Rows");
        return new TypeSpec(NETWORK, "IqlResult", result.members);
    }

    /**
     * The network's own language, from a program: a statement goes to the Mainframe's engine as it would from the
     * prompt, and what it answers comes back as rows a program can walk. Every statement is work for the Mainframe,
     * and rows are rows.
     */
    private static TypeSpec iql() {
        final Members iql = new Members("Iql");
        iql.onType("IqlResult", "Run", MemberKind.WORLD, CallCost.perRow(SigmaCosts.WRITE), STRING);
        iql.onType(ROWS, "Query", MemberKind.WORLD, CallCost.perRow(SigmaCosts.WRITE), STRING);
        iql.onType("IqlResult", "Exec", MemberKind.WORLD, CallCost.perRow(SigmaCosts.WRITE), STRING);
        iql.onType("IqlResult", "Exec", MemberKind.WORLD, CallCost.perRow(SigmaCosts.WRITE), STRING, STRINGS);
        iql.onType("IqlResult", "RunFile", MemberKind.WORLD, CallCost.perRow(SigmaCosts.WRITE), STRING);
        return new TypeSpec(NETWORK, "Iql", iql.members);
    }

    private static TypeSpec workStat() {
        final Members stat = new Members("WorkStat");
        stat.recordValue(STRING, "Type");
        stat.recordValue(INT, "Count");
        stat.recordValue(INT, "AverageWait");
        stat.recordValue(INT, "AverageRun");
        stat.recordValue(INT, "ShortfallPercent");
        stat.recordValue(LONG, "Moved");
        return new TypeSpec(NETWORK, "WorkStat", stat.members);
    }

    /**
     * The machine that orchestrates the network, and what it remembers of the work it has done.
     *
     * <p>A kind of work the network has not done reads as zeroes, so a script can add up and compare without first
     * asking whether there is anything to add up. What it has done is a list, and a list is priced by its length.
     */
    private static TypeSpec mainframe() {
        final Members mainframe = new Members("Mainframe");
        mainframe.valueOnType(BOOL, "Online", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK));
        mainframe.valueOnType(INT, "PeakToday", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE_NETWORK));
        mainframe.onType("WorkStat", "Stats", MemberKind.WORLD, CallCost.of(SigmaCosts.READ), STRING);
        mainframe.onType("List<WorkStat>", "Work", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ));
        return new TypeSpec(NETWORK, "Mainframe", mainframe.members);
    }

    private static TypeSpec askResult() {
        final Members asked = new Members("AskResult");
        asked.recordValue(BOOL, "Ok");
        asked.recordValue(STRING, "Message");
        return new TypeSpec(OPERATIONS, "AskResult", asked.members);
    }

    private static TypeSpec operationInfo() {
        final Members operation = new Members("OperationInfo");
        operation.recordValue(STRING, "Id");
        operation.recordValue(STRING, "Type");
        operation.recordValue(STRING, "Item");
        operation.recordValue(LONG, "Moved");
        operation.recordValue(LONG, "Requested");
        operation.recordValue(STRING, "Status");
        operation.recordValue(STRING, "Priority");
        return new TypeSpec(OPERATIONS, "OperationInfo", operation.members);
    }

    /**
     * Asking the network to move things.
     *
     * <p>Asking can fail without the program being wrong: there may be no Mainframe running, or nothing that crafts
     * the thing. So an ask answers whether it was taken and why not, and a script carries on and tries something else
     * rather than stopping. Asking the network to move or make something is work for the whole base.
     */
    private static TypeSpec operations() {
        final Members operations = new Members("Operations");
        operations.onType("AskResult", "Pull", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING, LONG);
        operations.onType("AskResult", "Push", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING, LONG);
        operations.onType("AskResult", "Craft", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING, LONG);
        operations.onType("AskResult", "Cancel", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING);
        /*
         * Moving one up the queue is asked for, not written into the record a program was handed: what it holds is a
         * picture of how things were, and painting over a picture changes nothing.
         */
        operations.onType("AskResult", "Reprioritise", MemberKind.WORLD, CallCost.of(SigmaCosts.SUBMIT), STRING,
                STRING);
        operations.onType("OperationInfo", "Get", MemberKind.WORLD, CallCost.of(SigmaCosts.READ), STRING);
        operations.onType("List<OperationInfo>", "List", MemberKind.WORLD, CallCost.perRow(SigmaCosts.READ));
        return new TypeSpec(OPERATIONS, "Operations", operations.members);
    }

    private static TypeSpec ccComputer() {
        final Members computer = new Members("CcComputer");
        computer.recordValue(LONG, "Id");
        computer.recordValue(STRING, "Name");
        computer.recordValue(STRING, "Label");
        computer.recordValue(BOOL, "Online");
        return new TypeSpec(NETWORK, "CcComputer", computer.members);
    }

    private static TypeSpec ccPeripheral() {
        final Members device = new Members("CcPeripheral");
        device.recordValue(STRING, "Name");
        device.recordValue(STRING, "Type");
        device.recordValue(STRINGS, "Methods");
        return new TypeSpec(NETWORK, "CcPeripheral", device.members);
    }

    private static TypeSpec gatewayMessage() {
        final Members message = new Members("GatewayMessage");
        message.recordValue(LONG, "From");
        message.recordValue(STRING, "Text");
        message.recordValue(LONG, "Tick");
        return new TypeSpec(NETWORK, "GatewayMessage", message.members);
    }

    /**
     * The Gateways this machine has, and through them the ComputerCraft computers and devices on the wire.
     *
     * <p>{@code Online} answers anywhere, because whether there is a Gateway at all is a fair question on any machine.
     * Everything else needs one, and a machine with none says so rather than pretending. Reaching across to another
     * mod's computer is the dearest thing a program can do short of asking the network for work, and the prices say
     * so.
     */
    private static TypeSpec gateway() {
        final Members gateway = new Members("Gateway");
        gateway.valueOnType(BOOL, "Online", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        gateway.valueOnType(STRING, "Current", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        gateway.onType(STRINGS, "Names", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE));
        gateway.onType(BOOL, "Select", MemberKind.WORLD, CallCost.of(SigmaCosts.GLANCE), STRING);
        gateway.onType("List<CcComputer>", "Computers", MemberKind.WORLD, CallCost.perRow(SigmaCosts.GATHER));
        gateway.onType("List<CcPeripheral>", "Peripherals", MemberKind.WORLD, CallCost.perRow(SigmaCosts.GATHER));
        /*
         * A call takes whatever that peripheral's method takes, which is a different number of things for every one of
         * them, so there is a way of writing it for each count rather than one that takes a list the program has to
         * build first. Each thing handed over adds to what the call costs.
         */
        final int call = SigmaCosts.CALL_ACROSS;
        final int each = SigmaCosts.PER_ARGUMENT_ACROSS;
        gateway.onType(OBJECT, "Call", MemberKind.WORLD, CallCost.of(call), STRING, STRING);
        gateway.onType(OBJECT, "Call", MemberKind.WORLD, CallCost.of(call + each), STRING, STRING, OBJECT);
        gateway.onType(OBJECT, "Call", MemberKind.WORLD, CallCost.of(call + 2 * each), STRING, STRING, OBJECT, OBJECT);
        gateway.onType(OBJECT, "Call", MemberKind.WORLD, CallCost.of(call + 3 * each), STRING, STRING, OBJECT, OBJECT,
                OBJECT);
        gateway.onType(BOOL, "TurnOn", MemberKind.WORLD, CallCost.of(SigmaCosts.SEND_ACROSS), LONG);
        gateway.onType(BOOL, "Shutdown", MemberKind.WORLD, CallCost.of(SigmaCosts.SEND_ACROSS), LONG);
        gateway.onType(BOOL, "Reboot", MemberKind.WORLD, CallCost.of(SigmaCosts.SEND_ACROSS), LONG);
        gateway.onType(BOOL, "Send", MemberKind.WORLD, CallCost.of(SigmaCosts.SEND_ACROSS), LONG, STRING);
        gateway.onType(VOID, "OnMessage", MemberKind.PROCESS, CallCost.FREE, "Action<GatewayMessage>");
        return new TypeSpec(NETWORK, "Gateway", gateway.members);
    }

    /*
     * The windows a program can open on the desktop of the machine it runs on. A widget asks for the size it needs and
     * the machine's own system draws it, so the same program looks like whichever system it is running under. Rows and
     * columns share out the room; a weight of one or more takes a share of what is left over, and nothing else takes
     * any.
     *
     * All of it is the program's own. Making a widget and reading one cost nothing; changing what a window shows costs
     * a draw, since the machine has to draw it again for whoever is looking. Opening a window is dearer: it goes on the
     * machine's desktop and its taskbar, and it outlives the tick that asked for it.
     */

    /** What every widget has: whether it shows, whether it answers, and a size of its own if it wants one. */
    private static TypeSpec widget() {
        final Members widget = new Members(WIDGET);
        widget.drawnValue(BOOL, "Visible");
        widget.drawnValue(BOOL, "Enabled");
        widget.drawnValue(INT, "Width");
        widget.drawnValue(INT, "Height");
        return new TypeSpec(UI, WIDGET, widget.members);
    }

    private static TypeSpec window() {
        final Members window = new Members("Window");
        window.made(STRING, INT, INT);
        window.drawnValue(STRING, "Title");
        window.drawnValue(WIDGET, "Content");
        window.valueOnObject(BOOL, "Open", MemberKind.PROCESS, CallCost.FREE);
        window.onObject(VOID, "Show", MemberKind.PROCESS, CallCost.of(SigmaCosts.WRITE));
        window.drawn("Close");
        // A widget put exactly where the program says, for one that lays itself out.
        window.drawn("Add", WIDGET, INT, INT, INT, INT);
        window.told("OnClose");
        return new TypeSpec(UI, "Window", window.members);
    }

    /** A row or a column, which shares out its room among the widgets put in it. */
    private static TypeSpec box(final String name) {
        final Members box = new Members(name);
        box.made();
        box.drawn("Add", WIDGET);
        box.drawn("Add", WIDGET, INT);
        box.drawnValue(INT, "Spacing");
        box.drawn("Clear");
        return new TypeSpec(UI, name, WIDGET, box.members);
    }

    private static TypeSpec label() {
        final Members label = new Members("Label");
        label.made();
        label.made(STRING);
        label.drawnValue(STRING, "Text");
        return new TypeSpec(UI, "Label", WIDGET, label.members);
    }

    private static TypeSpec button() {
        final Members button = new Members("Button");
        button.made();
        button.made(STRING);
        button.drawnValue(STRING, "Text");
        button.told("OnClick");
        return new TypeSpec(UI, "Button", WIDGET, button.members);
    }

    private static TypeSpec textBox() {
        final Members textBox = new Members("TextBox");
        textBox.made();
        textBox.made(STRING);
        textBox.drawnValue(STRING, "Text");
        textBox.told("OnChange");
        textBox.told("OnSubmit");
        return new TypeSpec(UI, "TextBox", WIDGET, textBox.members);
    }

    private static TypeSpec checkBox() {
        final Members checkBox = new Members("CheckBox");
        checkBox.made();
        checkBox.made(STRING);
        checkBox.made(STRING, BOOL);
        checkBox.drawnValue(STRING, "Text");
        checkBox.drawnValue(BOOL, "Checked");
        checkBox.told("OnToggle");
        return new TypeSpec(UI, "CheckBox", WIDGET, checkBox.members);
    }

    private static TypeSpec progressBar() {
        final Members progress = new Members("ProgressBar");
        progress.made();
        progress.made(INT, INT);
        progress.drawnValue(INT, "Value");
        progress.drawnValue(INT, "Least");
        progress.drawnValue(INT, "Most");
        return new TypeSpec(UI, "ProgressBar", WIDGET, progress.members);
    }

    private static TypeSpec listBox() {
        final Members listBox = new Members("ListBox");
        listBox.made();
        listBox.drawn("Add", STRING);
        listBox.drawn("Add", STRING, STRING);
        listBox.drawn("Clear");
        listBox.valueOnObject(INT, "Count", MemberKind.PROCESS, CallCost.FREE);
        listBox.drawnValue(INT, "Selected");
        listBox.told("OnSelect");
        return new TypeSpec(UI, "ListBox", WIDGET, listBox.members);
    }

    private static TypeSpec canvas() {
        final Members canvas = new Members("Canvas");
        canvas.made();
        canvas.made(INT, INT);
        canvas.drawn("Clear", INT);
        canvas.drawn("FillRect", INT, INT, INT, INT, INT);
        canvas.drawn("DrawLine", INT, INT, INT, INT, INT);
        canvas.drawn("DrawText", STRING, INT, INT, INT);
        canvas.drawn("SetPixel", INT, INT, INT);
        canvas.valueOnObject(INT, "ClickX", MemberKind.PROCESS, CallCost.FREE);
        canvas.valueOnObject(INT, "ClickY", MemberKind.PROCESS, CallCost.FREE);
        canvas.told("OnClick");
        return new TypeSpec(UI, "Canvas", WIDGET, canvas.members);
    }

    /** A window the machine makes for a program with nothing but a title and a line to show. */
    private static TypeSpec messageBox() {
        final Members messageBox = new Members("MessageBox");
        messageBox.onType(VOID, "Show", MemberKind.PROCESS, CallCost.of(SigmaCosts.WRITE), STRING, STRING);
        return new TypeSpec(UI, "MessageBox", messageBox.members);
    }

    /** Gathers the members of one type, in the order they are declared. */
    private static final class Members {

        private final String owner;
        private final List<IMemberSpec> members = new ArrayList<>();

        Members(final String owner) {
            this.owner = owner;
        }

        /** A method called on the type that the language answers with nothing but what it hands over. */
        void pure(final String returns, final String name, final String... parameters) {
            this.onType(returns, name, MemberKind.PURE, CallCost.FREE, parameters);
        }

        /** A method called on the type. */
        void onType(final String returns, final String name, final MemberKind kind, final CallCost cost,
                    final String... parameters) {
            this.members.add(new MethodSpec(this.id(name, parameters), returns, true, kind, cost));
        }

        /** A method called on an object of the type. */
        void onObject(final String returns, final String name, final MemberKind kind, final CallCost cost,
                      final String... parameters) {
            this.members.add(new MethodSpec(this.id(name, parameters), returns, false, kind, cost));
        }

        /** A value read from the type. */
        void valueOnType(final String type, final String name, final MemberKind kind, final CallCost cost) {
            this.members.add(new PropertySpec(this.id(name), type, true, false, kind, cost, CallCost.FREE));
        }

        /** A value read from an object of the type. */
        void valueOnObject(final String type, final String name, final MemberKind kind, final CallCost cost) {
            this.members.add(new PropertySpec(this.id(name), type, false, false, kind, cost, CallCost.FREE));
        }

        /** A value on a record the program was handed, which it reads from its own copy for nothing. */
        void recordValue(final String type, final String name) {
            this.valueOnObject(type, name, MemberKind.PROCESS, CallCost.FREE);
        }

        /** A value on a widget, read for nothing and written at the price of drawing the widget again. */
        void drawnValue(final String type, final String name) {
            this.members.add(new PropertySpec(this.id(name), type, false, true, MemberKind.PROCESS, CallCost.FREE,
                    CallCost.of(SigmaCosts.DRAW)));
        }

        /** A call on a widget, which changes what it shows and so costs drawing it again. */
        void drawn(final String name, final String... parameters) {
            this.onObject(VOID, name, MemberKind.PROCESS, CallCost.of(SigmaCosts.DRAW), parameters);
        }

        /** A way of making one of these, which is the program's own and costs nothing. */
        void made(final String... parameters) {
            this.members.add(new ConstructorSpec(this.id(ConstructorSpec.NAME, parameters), MemberKind.PROCESS,
                    CallCost.FREE));
        }

        /** Something a widget is told, which a program hears by joining a handler, a write on the widget. */
        void told(final String name) {
            this.members.add(new EventSpec(this.id(name), "Action", false, MemberKind.PROCESS,
                    CallCost.of(SigmaCosts.DRAW)));
        }

        private MemberId id(final String name, final String... parameters) {
            return new MemberId(this.owner, name, List.of(parameters));
        }
    }
}
