/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.asm.lua;

import dev.jstech.computers.cannon.asm.IOperand;
import dev.jstech.computers.cannon.run.Library;
import java.util.List;

/**
 * What the things a program asks of its machine become on the other side.
 *
 * <p>A program written here says {@code Console.PrintLine} and {@code File.Read} and
 * {@code Network.Total}; a ComputerCraft computer calls those things by other names, and what it has no
 * name for at all it reaches through the Gateway, which is our own network seen from over there. So the
 * same program, unchanged, prints with their print, reads with their fs, and asks our network the same
 * questions it would ask standing on it.
 *
 * <p>What neither side can serve stops the program with a reason, there as it would here.
 */
final class LuaIntrinsics {

    /** The owners the other side answers itself. */
    private static final List<String> THEIRS = List.of("Console", "string", "List", "Map", "Math", "Convert",
            "Time", "Random", "Delegate", "File");

    /** The one thing of ours a program holds rather than names: another computer on the network. */
    private static final String REMOTE = "RemoteComputer";

    /** What a running program answers about itself, wherever it is running. */
    private static final List<String> OURS = List.of("Exit", "OnMessage");

    private LuaIntrinsics() {
    }

    /**
     * Whether the call is made on something rather than on a name, which puts that something on the stack
     * under the call's own arguments and so first among what is written down here.
     */
    static boolean takesTarget(final String owner, final String member) {
        return Library.onSomething(owner, member) || REMOTE.equals(owner);
    }

    /** The call, as Lua text, with the arguments already written down in order. */
    static String call(final IOperand.Method named, final List<String> passed) {
        final String owner = named.owner();
        final String member = named.name();
        final String first = at(passed, 0);
        final String second = at(passed, 1);
        final String third = at(passed, 2);
        if ("Program".equals(owner) && "Shell".equals(member)) {
            return "_shell(" + first + ")";
        }
        if ("Program".equals(owner) && "Start".equals(member)) {
            // A program started over there runs to its end there and then, since nothing runs behind.
            return "_start(" + first + ")";
        }
        if ("Gateway".equals(owner) && "Serve".equals(member)) {
            return "_serve(" + AsmToLua.PROGRAM + ", " + first + ")";
        }
        if ("Program".equals(owner) && OURS.contains(member)) {
            /*
             * Stopping and being told are the running program's own business, here as there: the
             * runtime answers them itself rather than asking the machine, so the translation does too.
             */
            return ("Exit".equals(member) ? "_exit(" : "_onmessage(") + first + ")";
        }
        if (!THEIRS.contains(owner)) {
            // Everything about our world is asked of the Gateway, which is where our world is from there.
            return ask(owner, member, passed);
        }
        return switch (owner + "." + member) {
            case "Console.PrintLine" -> "print(" + text(first) + ")";
            case "Console.Print" -> "io.write(" + text(first) + ")";
            case "Console.ReadLine" -> "read()";
            case "Console.ReadInt" -> "_int(tonumber(read()) or 0)";
            case "string.Concat" -> "(" + text(first) + " .. " + text(second) + ")";
            case "string.Length" -> "#" + first;
            case "string.Substring" -> passed.size() < 2 ? "\"\""
                    : "string.sub(" + first + ", " + second + " + 1"
                    + (passed.size() > 2 ? ", " + second + " + " + third : "") + ")";
            case "string.IndexOf" -> "((string.find(" + first + ", " + second + ", 1, true) or 0) - 1)";
            case "string.Contains" -> "(string.find(" + first + ", " + second + ", 1, true) ~= nil)";
            case "string.StartsWith" -> "(string.sub(" + first + ", 1, #" + second + ") == " + second + ")";
            case "string.EndsWith" -> "(" + second + " == \"\" or string.sub(" + first + ", -#" + second
                    + ") == " + second + ")";
            case "string.ToUpper" -> "string.upper(" + first + ")";
            case "string.ToLower" -> "string.lower(" + first + ")";
            case "string.Trim" -> "(string.match(" + first + ", \"^%s*(.-)%s*$\"))";
            case "Math.Abs" -> "math.abs(" + first + ")";
            case "Math.Min" -> "math.min(" + first + ", " + second + ")";
            case "Math.Max" -> "math.max(" + first + ", " + second + ")";
            case "Math.Floor" -> "_real(math.floor(" + first + "))";
            case "Math.Ceil" -> "_real(math.ceil(" + first + "))";
            case "Math.Round" -> "_real(math.floor(" + first + " + 0.5))";
            case "Math.Sqrt" -> "math.sqrt(" + first + ")";
            case "Math.Pow" -> "(" + first + " ^ " + second + ")";
            case "Math.Clamp" -> "math.max(" + second + ", math.min(" + third + ", " + first + "))";
            case "Convert.ToInt", "Convert.ToLong" -> "_int(tonumber(" + first + ") or 0)";
            case "Convert.ToDouble", "Convert.ToFloat" -> "(tonumber(" + first + ") or 0)";
            case "Convert.ToString" -> text(first);
            case "Convert.ToBool" -> "(" + first + " == true or " + first + " == \"true\")";
            case "Time.Ticks" -> "(" + first + " * 20)";
            case "Random.Next" -> "math.random(0, math.max(0, " + first + " - 1))";
            case "Random.NextDouble" -> "math.random()";
            case "Delegate.Invoke" -> "_invoke(" + AsmToLua.PROGRAM + ", " + String.join(", ", passed) + ")";
            case "Delegate.Combine" -> "_combine(" + first + ", " + second + ")";
            case "Delegate.Remove" -> "_part(" + first + ", " + second + ")";
            case "List.New" -> "_list()";
            case "List.Add" -> "_listadd(" + first + ", " + second + ")";
            case "List.Count" -> first + ".n";
            case "List.At", "List.Get" -> "_listat(" + first + ", " + second + ")";
            case "List.Put", "List.Set" -> "_listput(" + first + ", " + second + ", " + third + ")";
            case "List.Remove", "List.RemoveAt" -> "_listremove(" + first + ", " + second + ")";
            case "Map.New" -> "_map()";
            case "Map.Put", "Map.Set" -> "_mapput(" + first + ", " + second + ", " + third + ")";
            case "Map.At", "Map.Get" -> "_mapat(" + first + ", " + second + ")";
            case "Map.Remove" -> "_maptake(" + first + ", " + second + ")";
            case "Map.Count" -> first + ".n";
            /*
             * The files of the machine the program is standing on. Over there that is that computer's
             * own disk: a program asking for its own files gets its own, wherever "its own" now is.
             */
            case "File.Exists" -> "fs.exists(" + first + ")";
            case "File.Read", "File.Text" -> "_fsread(" + first + ")";
            case "File.TryRead" -> "_fstry(" + first + ")";
            case "File.Write", "File.Put" -> "_fswrite(" + first + ", " + text(second) + ", false)";
            case "File.Append" -> "_fswrite(" + first + ", " + text(second) + ", true)";
            case "File.Delete", "File.Remove" -> "fs.delete(" + first + ")";
            case "File.MkDir", "File.MakeDir" -> "fs.makeDir(" + first + ")";
            case "File.List", "File.Entries" -> "_fslist(" + first + ")";
            case "File.Free" -> "fs.getFreeSpace(" + first + ")";
            case "File.Capacity" -> "fs.getCapacity(" + first + ")";
            default -> unknown(owner, member);
        };
    }

    /**
     * One of the things the language brings with it, made on the other side.
     *
     * <p>A list and a map it has of its own; a window belongs to the machine that draws it, and the
     * machine drawing this program is not ours, so a program that asks for one is told where it is.
     */
    static String made(final String type, final List<String> passed) {
        final String bare = type.contains("<") ? type.substring(0, type.indexOf('<')) : type;
        return switch (bare) {
            case "Map" -> "_map()";
            case "List" -> "_list()";
            default -> "error(" + AsmToLua.quoted("a " + bare + " cannot be made on this computer") + ", 0)";
        };
    }

    /** A value the machine keeps rather than the program: the world's clock, or something of ours. */
    static String read(final String owner, final String name) {
        return switch (owner + "." + name) {
            /* Whether there is a Gateway within reach is a fair question anywhere, and never an error. */
            case "Gateway.Online" -> "_hasgateway()";
            case "Time.Tick" -> "_int(os.epoch(\"ingame\") / 50)";
            case "Time.DayTime" -> "_int(os.time() * 1000)";
            case "Time.Day" -> "os.day()";
            case "Program.Name" -> "(shell ~= nil and shell.getRunningProgram() or \"\")";
            default -> THEIRS.contains(owner) ? unknown(owner, name) : ask(owner, name, List.of());
        };
    }

    /**
     * Anything about our own world, asked of the Gateway.
     *
     * <p>The Gateway is a peripheral over there, so asking it something is calling a method on it, which
     * is how a ComputerCraft program asks our network anything. It is asked the same question a program
     * standing on one of our machines asks its own machine, by the name of the thing and the name of the
     * member, so the answer, the price and the refusal are all the same ones. A computer with no Gateway
     * within reach cannot answer, and the program stops with that, which is the truth of where it is.
     */
    private static String ask(final String owner, final String member, final List<String> passed) {
        final List<String> all = new java.util.ArrayList<>();
        all.add(AsmToLua.quoted(owner));
        all.add(AsmToLua.quoted(member));
        all.addAll(passed);
        return "_ask(" + String.join(", ", all) + ")";
    }

    private static String unknown(final String owner, final String member) {
        return "error(" + AsmToLua.quoted(owner + "." + member + " cannot be done on this computer") + ", 0)";
    }

    private static String text(final String value) {
        return "tostring(" + value + ")";
    }

    private static String at(final List<String> passed, final int index) {
        return index < passed.size() ? passed.get(index) : "nil";
    }
}
