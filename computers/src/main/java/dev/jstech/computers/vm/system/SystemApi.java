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

    private static final String VOID = "void";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BOOL = "bool";
    private static final String STRING = "string";
    private static final String OBJECT = "object";
    private static final String STRINGS = "List<string>";

    private static final List<TypeSpec> TYPES = List.of(math(), convert(), console(), program(), process(),
            processMessage(), thread(), time(), random());

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
        message.valueOnObject(INT, "From", MemberKind.PROCESS, CallCost.FREE);
        message.valueOnObject(STRING, "Text", MemberKind.PROCESS, CallCost.FREE);
        message.valueOnObject(LONG, "Tick", MemberKind.PROCESS, CallCost.FREE);
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
            this.members.add(new PropertySpec(this.id(name), type, true, false, kind, cost));
        }

        /** A value read from an object of the type. */
        void valueOnObject(final String type, final String name, final MemberKind kind, final CallCost cost) {
            this.members.add(new PropertySpec(this.id(name), type, false, false, kind, cost));
        }

        private MemberId id(final String name, final String... parameters) {
            return new MemberId(this.owner, name, List.of(parameters));
        }
    }
}
