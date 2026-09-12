/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.asm.lua;

import dev.jstech.computers.cannon.asm.AsmType;
import dev.jstech.computers.cannon.asm.IOperand;
import dev.jstech.computers.cannon.asm.Instruction;
import dev.jstech.computers.cannon.asm.Opcode;
import dev.jstech.computers.cannon.run.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a program of the Assembly into Lua, so a program written here runs on a ComputerCraft computer.
 *
 * <p>Nothing of this is written by hand: the text comes out of the program itself, a line at a time, the
 * way a compiler writes any other output. A class becomes a table, a method becomes a function that runs
 * the same stack machine the runtime here runs (a stack, the slots, and a number saying which line is
 * next), and a branch is that number changing. It is not the shortest Lua anyone could write for the same
 * program; it is the Lua that is certainly the same program, which is what matters when the thing being
 * translated is somebody's reactor controller.
 *
 * <p>What the program asks of its machine ({@code Console}, {@code File}, the network) becomes what the
 * other side calls those things, through {@link LuaIntrinsics}.
 */
public final class AsmToLua {

    /** What the stack, the slots and the line counter are called in every method that comes out. */
    private static final String STACK = "S";
    private static final String HEIGHT = "n";
    private static final String SLOTS = "V";
    private static final String AT = "pc";

    /** The table every translated program keeps its types in. */
    static final String PROGRAM = "P";

    /** How deep the writing is indented at each level. */
    private static final String STEP = "  ";

    private final Loaded program;
    private final StringBuilder out = new StringBuilder();

    private AsmToLua(final Loaded program) {
        this.program = program;
    }

    /** The whole program as Lua text, ready to be put on a ComputerCraft computer. */
    public static String of(final Loaded program) {
        final AsmToLua writing = new AsmToLua(program);
        writing.write();
        return writing.out.toString();
    }

    private void write() {
        this.line(0, "local " + PROGRAM + " = {}");
        /*
         * What the program was started with. It has to be taken here, at the top of the file: the words
         * after a program's name reach a chunk as its own varargs and are out of reach anywhere else.
         */
        this.line(0, "local _given = { ... }");
        LuaPrimitives.write(this.out, this.needs());
        for (final Loaded.Type type : this.program.types()) {
            this.type(type);
        }
        this.entry();
    }

    /*
     * What this program asks of the machine it will be standing on, which decides how much of the
     * prelude has to go with it. A program that never touches a file carries no file reading at all.
     */
    private LuaPrimitives.Needs needs() {
        boolean files = false;
        boolean shell = false;
        boolean serve = false;
        for (final Loaded.Type type : this.program.types()) {
            for (final Loaded.Method method : type.methods().values()) {
                for (final Instruction instruction : method.code()) {
                    if (!(instruction.operand() instanceof IOperand.Method named)) {
                        continue;
                    }
                    files = files || "File".equals(named.owner());
                    shell = shell || "Program".equals(named.owner())
                            && ("Shell".equals(named.name()) || "Start".equals(named.name()));
                    serve = serve || "Gateway".equals(named.owner()) && "Serve".equals(named.name());
                }
            }
        }
        return new LuaPrimitives.Needs(files, shell, serve);
    }

    // types

    private void type(final Loaded.Type type) {
        final String name = quoted(type.name());
        this.line(0, "");
        this.line(0, "-- " + type.name());
        final String base = this.program.baseOf(type.name());
        this.line(0, PROGRAM + "[" + name + "] = { name = " + name
                + ", base = " + (base == null ? "nil" : quoted(base))
                + ", bases = { " + String.join(", ", type.bases().stream().map(AsmToLua::quoted).toList()) + " }"
                + ", byValue = " + type.kind().byValue()
                + ", statics = {}, methods = {}, values = {} }");
        for (final AsmType.Field field : type.fields()) {
            if (field.isStatic()) {
                this.line(0, PROGRAM + "[" + name + "].statics[" + quoted(field.name()) + "] = "
                        + LuaPrimitives.startingValue(field.type()));
            }
        }
        for (final java.util.Map.Entry<String, Integer> value : type.values().entrySet()) {
            this.line(0, PROGRAM + "[" + name + "].values[" + quoted(value.getKey()) + "] = " + value.getValue());
        }
        this.line(0, PROGRAM + "[" + name + "].new = function()");
        this.line(1, "return { type = " + name + (fieldsOf(type).isEmpty() ? "" : ", ") + fieldsOf(type) + " }");
        this.line(0, "end");
        for (final Loaded.Method method : type.methods().values()) {
            this.method(type, method);
        }
        if (type.setUp() != null) {
            this.method(type, type.setUp());
        }
    }

    /** What an object of that type holds when it is made: every field of its own, at its starting value. */
    private static String fieldsOf(final Loaded.Type type) {
        final List<String> written = new ArrayList<>();
        for (final AsmType.Field field : type.fields()) {
            if (!field.isStatic()) {
                written.add("[" + quoted(field.name()) + "] = " + LuaPrimitives.startingValue(field.type()));
            }
        }
        return String.join(", ", written);
    }

    // methods

    private void method(final Loaded.Type type, final Loaded.Method method) {
        final String key = signature(method.name(), method.parameters());
        final List<String> taken = new ArrayList<>();
        if (!method.isStatic()) {
            taken.add("self");
        }
        for (int i = 0; i < method.parameters().size(); i++) {
            taken.add("a" + i);
        }
        this.line(0, PROGRAM + "[" + quoted(type.name()) + "].methods[" + quoted(key) + "] = function("
                + String.join(", ", taken) + ")");
        this.body(method);
        this.line(0, "end");
    }

    /*
     * The method itself: the stack, the slots, and a line counter that a branch moves. Every line of the
     * assembly becomes one arm of the same choice, so a jump is nothing more than a number changing, and
     * the shape of the method needs no goto, which the other side does not have either.
     */
    private void body(final Loaded.Method method) {
        this.line(1, "local " + STACK + ", " + HEIGHT + ", " + SLOTS + " = {}, 0, {}");
        for (int i = 0; i < method.parameters().size(); i++) {
            this.line(1, SLOTS + "[" + i + "] = a" + i);
        }
        this.line(1, "local " + AT + " = 0");
        this.line(1, "while true do");
        for (int i = 0; i < method.code().size(); i++) {
            this.line(2, (i == 0 ? "if " : "elseif ") + AT + " == " + i + " then");
            this.line(3, AT + " = " + (i + 1));
            this.instruction(method, method.code().get(i), i);
        }
        if (!method.code().isEmpty()) {
            this.line(2, "else");
        }
        this.line(3, "return");
        if (!method.code().isEmpty()) {
            this.line(2, "end");
        }
        this.line(1, "end");
    }

    private void instruction(final Loaded.Method method, final Instruction instruction, final int at) {
        final IOperand operand = instruction.operand();
        switch (instruction.opcode()) {
            case LDC_I4 -> this.push(String.valueOf(((IOperand.I4) operand).value()));
            case LDC_I8 -> this.push(String.valueOf(((IOperand.I8) operand).value()));
            case LDC_R4 -> this.push(real(((IOperand.R4) operand).value()));
            case LDC_R8 -> this.push(real(((IOperand.R8) operand).value()));
            case LDNULL -> this.push("nil");
            case LDSTR -> this.push(quoted(((IOperand.Text) operand).value()));
            case LDTHIS -> this.push("self");
            case LDLOC -> this.push(SLOTS + "[" + ((IOperand.Slot) operand).index() + "]");
            case STLOC -> {
                this.line(3, SLOTS + "[" + ((IOperand.Slot) operand).index() + "] = " + top());
                this.drop(1);
            }
            case POP -> this.drop(1);
            case DUP -> this.push(top());
            case COPY -> this.line(3, top() + " = _copy(" + top() + ")");
            case LDFLD -> this.line(3, top() + " = _field(" + top() + ", "
                    + quoted(((IOperand.Field) operand).name()) + ")");
            case STFLD -> {
                this.line(3, "_setfield(" + under(1) + ", " + quoted(((IOperand.Field) operand).name())
                        + ", " + top() + ")");
                this.drop(2);
            }
            case LDSFLD -> this.statics(operand, true);
            case STSFLD -> this.statics(operand, false);
            case ADD, SUB, MUL, DIV, REM, AND, OR, XOR, SHL, SHR ->
                    this.arithmetic(method, instruction.opcode(), at);
            case NEG -> this.line(3, top() + " = -" + top());
            case NOT -> this.line(3, top() + " = _not(" + top() + ")");
            case CONV_I4, CONV_I8 -> this.line(3, top() + " = _int(" + top() + ")");
            case CONV_R4, CONV_R8 -> this.line(3, top() + " = _real(" + top() + ")");
            case CEQ -> this.fold("_eq(" + PROGRAM + ", " + under(1) + ", " + top() + ")");
            case CLT -> this.fold("(" + under(1) + " < " + top() + ")");
            case CGT -> this.fold("(" + under(1) + " > " + top() + ")");
            case BR -> this.line(3, AT + " = " + target(method, instruction));
            case BRTRUE -> this.jump(method, instruction, "_truth(" + top() + ")");
            case BRFALSE -> this.jump(method, instruction, "not _truth(" + top() + ")");
            case BEQ -> this.jumpTwo(method, instruction, same());
            case BNE -> this.jumpTwo(method, instruction, "not " + same());
            case BLT -> this.jumpTwo(method, instruction, under(1) + " < " + top());
            case BLE -> this.jumpTwo(method, instruction, under(1) + " <= " + top());
            case BGT -> this.jumpTwo(method, instruction, under(1) + " > " + top());
            case BGE -> this.jumpTwo(method, instruction, under(1) + " >= " + top());
            case NEWOBJ -> this.newObject((IOperand.Constructor) operand);
            case NEWARR -> this.line(3, top() + " = _array(" + top() + ", "
                    + quoted(((IOperand.Type) operand).name()) + ")");
            case LDELEM -> this.fold("_element(" + under(1) + ", " + top() + ")");
            case STELEM -> {
                this.line(3, "_setelement(" + under(2) + ", " + under(1) + ", " + top() + ")");
                this.drop(3);
            }
            case LDLEN -> this.line(3, top() + " = " + top() + ".n");
            case CASTCLASS -> this.line(3, top() + " = _cast(" + PROGRAM + ", " + top() + ", "
                    + quoted(((IOperand.Type) operand).name()) + ")");
            case ISINST -> this.line(3, top() + " = _is(" + PROGRAM + ", " + top() + ", "
                    + quoted(((IOperand.Type) operand).name()) + ")");
            case LDFN -> this.handler((IOperand.Method) operand);
            case CALL -> this.call((IOperand.Method) operand);
            case CALLVIRT -> this.through((IOperand.Method) operand);
            case MONITOR_ENTER, MONITOR_EXIT -> this.drop(1);
            case DISPOSE -> this.drop(1);
            case SYS -> this.line(3, "error(\"this computer is not on a network\", 0)");
            case RET -> this.line(3, method.gives() ? "return " + top() : "return");
            default -> {
                // Anything the assembly grows later is left out loudly rather than translated wrongly.
                this.line(3, "error(" + quoted("this program uses " + instruction.opcode()
                        + ", which cannot be translated") + ", 0)");
            }
        }
    }

    // the pieces the instructions are written with

    /*
     * The value is put in its place before the stack grows, never after: what is being pushed is often
     * read off the stack itself, and a stack that had already grown would be read in the wrong place.
     */
    private void push(final String value) {
        this.line(3, STACK + "[" + HEIGHT + " + 1] = " + value);
        this.line(3, HEIGHT + " = " + HEIGHT + " + 1");
    }

    private void drop(final int many) {
        this.line(3, HEIGHT + " = " + HEIGHT + " - " + many);
    }

    /** What is on top of the stack, and what is under it. */
    private static String top() {
        return STACK + "[" + HEIGHT + "]";
    }

    private static String under(final int many) {
        return STACK + "[" + HEIGHT + " - " + many + "]";
    }

    /** Whether the two on top say the same thing, which is more than whether they are the same thing. */
    private static String same() {
        return "_eq(" + PROGRAM + ", " + under(1) + ", " + top() + ")";
    }

    /** Two things on the stack become one: the answer takes the place of the first. */
    private void fold(final String answer) {
        this.line(3, under(1) + " = " + answer);
        this.drop(1);
    }

    private void jump(final Loaded.Method method, final Instruction instruction, final String when) {
        this.line(3, "if " + when + " then " + AT + " = " + target(method, instruction) + " end");
        this.drop(1);
    }

    private void jumpTwo(final Loaded.Method method, final Instruction instruction, final String when) {
        this.line(3, "if " + when + " then " + AT + " = " + target(method, instruction) + " end");
        this.drop(2);
    }

    private static int target(final Loaded.Method method, final Instruction instruction) {
        return Loaded.target(method, instruction);
    }

    private void statics(final IOperand operand, final boolean reading) {
        final IOperand.Field field = (IOperand.Field) operand;
        final Loaded.Type type = this.program.type(field.owner());
        final String place = PROGRAM + "[" + quoted(field.owner()) + "]"
                + (type != null && type.kind() == AsmType.Kind.ENUM ? ".values[" : ".statics[")
                + quoted(field.name()) + "]";
        if (reading) {
            // A value the machine keeps rather than the program is asked of the machine, not of a table.
            this.push(type == null ? LuaIntrinsics.read(field.owner(), field.name()) : place);
            return;
        }
        this.line(3, place + " = " + top());
        this.drop(1);
    }

    /*
     * Dividing two whole numbers throws the fraction away, and the assembly says which division that is by
     * writing the conversion down after it; anything else divides as it reads.
     */
    private void arithmetic(final Loaded.Method method, final Opcode opcode, final int at) {
        final boolean whole = wholeNext(method, at);
        final String left = under(1);
        final String right = top();
        final String answer = switch (opcode) {
            case ADD -> left + " + " + right;
            case SUB -> left + " - " + right;
            case MUL -> left + " * " + right;
            case DIV -> whole ? "_quotient(" + left + ", " + right + ")" : "_divide(" + left + ", " + right + ")";
            case REM -> whole ? "_wholerest(" + left + ", " + right + ")" : "_rest(" + left + ", " + right + ")";
            case AND -> "_and(" + left + ", " + right + ")";
            case OR -> "_or(" + left + ", " + right + ")";
            case XOR -> "_xor(" + left + ", " + right + ")";
            case SHL -> "_shift(" + left + ", " + right + ", true)";
            default -> "_shift(" + left + ", " + right + ", false)";
        };
        this.fold(answer);
    }

    /** Whether the line after this one turns the answer into a whole number, which is what marks it. */
    private static boolean wholeNext(final Loaded.Method method, final int at) {
        if (at + 1 >= method.code().size()) {
            return false;
        }
        final Opcode next = method.code().get(at + 1).opcode();
        return next == Opcode.CONV_I4 || next == Opcode.CONV_I8;
    }

    private void newObject(final IOperand.Constructor made) {
        final int count = made.parameters().size();
        if (this.program.type(made.owner()) == null) {
            // Not one of the program's own: it is one of the things the language itself brings with it.
            final List<String> passed = new ArrayList<>();
            for (int i = count; i > 0; i--) {
                passed.add(under(i - 1));
            }
            this.line(3, "local made = " + LuaIntrinsics.made(made.owner(), passed));
            if (count > 0) {
                this.drop(count);
            }
            this.push("made");
            return;
        }
        final String key = signature(nameOf(made.owner()), made.parameters());
        this.line(3, "local made = " + PROGRAM + "[" + quoted(made.owner()) + "].new()");
        final List<String> passed = new ArrayList<>();
        passed.add("made");
        for (int i = count; i > 0; i--) {
            passed.add(under(i - 1));
        }
        this.line(3, "local ctor = " + PROGRAM + "[" + quoted(made.owner()) + "].methods[" + quoted(key) + "]");
        this.line(3, "if ctor ~= nil then ctor(" + String.join(", ", passed) + ") end");
        if (count > 0) {
            this.drop(count);
        }
        this.push("made");
    }

    /** A method handed over without brackets: the object it belongs to, kept with it. */
    private void handler(final IOperand.Method named) {
        final String key = signature(named.name(), named.parameters());
        this.line(3, top() + " = _handler(" + top() + ", " + quoted(named.owner()) + ", " + quoted(key) + ")");
    }

    /*
     * A call through a handler rather than on a name: what runs is whatever the handler holds, and the
     * handler itself sits on the stack under the call's arguments.
     */
    private void through(final IOperand.Method named) {
        final int count = named.parameters().size();
        final List<String> passed = new ArrayList<>();
        passed.add(under(count));
        for (int i = count; i > 0; i--) {
            passed.add(under(i - 1));
        }
        this.answered(named, "_invoke(" + PROGRAM + ", " + String.join(", ", passed) + ")", count + 1);
    }

    /*
     * A call: one of the program's own methods, or something the machine answers for. What the program
     * has is called straight; everything else goes through the intrinsics, which say what the other side
     * calls that thing.
     */
    private void call(final IOperand.Method named) {
        final int count = named.parameters().size();
        final boolean ours = this.program.type(named.owner()) != null;
        final boolean takesTarget = ours ? !this.isStatic(named)
                : LuaIntrinsics.takesTarget(named.owner(), named.name());
        final List<String> passed = new ArrayList<>();
        final int taken = count + (takesTarget ? 1 : 0);
        for (int i = taken; i > 0; i--) {
            passed.add(under(i - 1));
        }
        final String answer = ours
                ? this.own(named, takesTarget, passed) + "(" + String.join(", ", passed) + ")"
                : LuaIntrinsics.call(named, passed);
        this.answered(named, answer, taken);
    }

    /*
     * What the call gives back is worked out while everything it takes is still on the stack, and only
     * then does the stack come down: reading it afterwards would read the wrong places.
     */
    private void answered(final IOperand.Method named, final String answer, final int taken) {
        if (!"void".equals(named.returns())) {
            this.line(3, "local answered = " + answer);
            if (taken > 0) {
                this.drop(taken);
            }
            this.push("answered");
            return;
        }
        this.line(3, answer);
        if (taken > 0) {
            this.drop(taken);
        }
    }

    /*
     * One of the program's own methods. A call on an object asks the object which one that is, since a
     * class further down may have its own; a call on a name is the one the name holds and nothing else.
     */
    private String own(final IOperand.Method named, final boolean takesTarget, final List<String> passed) {
        final String key = quoted(signature(named.name(), named.parameters()));
        if (takesTarget) {
            return "_method(" + PROGRAM + ", " + quoted(named.owner()) + ", " + key + ", "
                    + passed.getFirst() + ")";
        }
        return PROGRAM + "[" + quoted(named.owner()) + "].methods[" + key + "]";
    }

    private boolean isStatic(final IOperand.Method named) {
        final Loaded.Method found = this.program.method(named.owner(), named.name(), named.parameters());
        return found == null || found.isStatic();
    }

    // the start of the program

    /*
     * A program that runs at a terminal is its entry method and then it is over; one that stays up is set
     * up, told of every tick until the computer is told to stop, and told once at the end.
     */
    private void entry() {
        final String entry = this.program.entryPoint();
        if (entry == null) {
            return;
        }
        this.line(0, "");
        this.line(0, "-- what every type puts in its own fields before anything runs");
        for (final Loaded.Type type : this.program.types()) {
            if (type.setUp() != null) {
                this.line(0, PROGRAM + "[" + quoted(type.name()) + "].methods["
                        + quoted(signature(type.setUp().name(), type.setUp().parameters())) + "]()");
            }
        }
        this.line(0, "");
        this.line(0, "-- where it starts");
        final String at = PROGRAM + "[" + quoted(entry) + "]";
        if (this.program.shape() == dev.jstech.computers.cannon.Shape.SCRIPT) {
            /*
             * A program that stays up is set up, told of every tick, and told once at the end, whether
             * the end came because it said so, because the computer was told to stop, or not at all.
             */
            this.line(0, "local self = " + at + ".new()");
            this.line(0, "local ok, why = pcall(function()");
            this.line(1, at + ".methods[\"OnInit()\"](self)");
            this.line(1, "while true do");
            this.line(2, at + ".methods[\"OnTick()\"](self)");
            this.line(2, "_wait()");
            this.line(1, "end");
            this.line(0, "end)");
            this.line(0, "local stop = " + at + ".methods[\"OnDestroy()\"]");
            this.line(0, "if stop ~= nil then pcall(stop, self) end");
            this.line(0, "if not ok and why ~= _over then error(why, 0) end");
            return;
        }
        this.line(0, "local ok, why = pcall(" + at + ".methods[\"Main()\"])");
        this.line(0, "if not ok and why ~= _over then error(why, 0) end");
    }

    // writing it down

    private void line(final int depth, final String text) {
        this.out.append(STEP.repeat(depth)).append(text).append('\n');
    }

    /** How a method is known among the ones of its type: its name and what it takes. */
    static String signature(final String name, final List<String> parameters) {
        return name + "(" + String.join(", ", parameters) + ")";
    }

    private static String nameOf(final String type) {
        final int dot = type.lastIndexOf('.');
        return dot < 0 ? type : type.substring(dot + 1);
    }

    /** A piece of text as Lua writes one. */
    static String quoted(final String text) {
        final StringBuilder written = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '"' -> written.append("\\\"");
                case '\\' -> written.append("\\\\");
                case '\n' -> written.append("\\n");
                case '\r' -> written.append("\\r");
                case '\t' -> written.append("\\t");
                default -> {
                    if (c < 32 || c > 126) {
                        written.append("\\").append(String.format(Locale.ROOT, "%03d", (int) c));
                    } else {
                        written.append(c);
                    }
                }
            }
        }
        return written.append('"').toString();
    }

    /** A real number written so it reads back as one, whole or not. */
    private static String real(final double value) {
        if (Double.isNaN(value)) {
            return "(0/0)";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "math.huge" : "-math.huge";
        }
        final String written = String.valueOf(value);
        return written.contains(".") || written.contains("e") || written.contains("E") ? written : written + ".0";
    }
}
