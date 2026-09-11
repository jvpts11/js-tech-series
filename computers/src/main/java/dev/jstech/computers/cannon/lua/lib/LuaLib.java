/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The standard library, as a table of names.
 *
 * <p>A name with a dot in it lives in the table the first part names ({@code string.rep} in
 * {@code string}); one without lives among the globals. A few names are hidden: the iterators and
 * steps the library hands back as bound functions, which a program reaches through what it was
 * given and not by name. The functions the runtime answers itself (coroutines, reading a line) are
 * listed here too, so they are installed with the rest; the runtime takes them before the table is
 * asked.
 */
public final class LuaLib {

    private static final Map<String, ILuaFunction> FUNCTIONS = new LinkedHashMap<>();
    private static final Map<String, ILuaContinuation> CONTINUATIONS = new LinkedHashMap<>();
    private static final Set<String> HIDDEN = new LinkedHashSet<>();
    private static final Map<String, Object> CONSTANTS = new LinkedHashMap<>();
    private static final Set<String> TABLES = new LinkedHashSet<>();

    static {
        LuaBase.register();
        LuaStrings.register();
        LuaTables.register();
        LuaMath.register();
        LuaOs.register();
        LuaBit32.register();
        // What a ComputerCraft computer adds to the language.
        LuaColours.register();
        LuaTerm.register();
        LuaFs.register();
        LuaTextutils.register();
        LuaCc.register();
    }

    private LuaLib() {
    }

    /** The function of that name, or null. */
    public static ILuaFunction function(final String name) {
        return FUNCTIONS.get(name);
    }

    /** The continuation of that name, or null. */
    public static ILuaContinuation continuation(final String name) {
        return CONTINUATIONS.get(name);
    }

    static void define(final String name, final ILuaFunction function) {
        FUNCTIONS.put(name, function);
    }

    /** A function reached only through a value the library handed back, never by name. */
    static void hidden(final String name, final ILuaFunction function) {
        FUNCTIONS.put(name, function);
        HIDDEN.add(name);
    }

    /** A name the runtime answers itself; what is put here is only reached if it does not. */
    static void intrinsic(final String name) {
        FUNCTIONS.put(name, (context, target, arguments, line) -> {
            throw context.error("'" + name + "' is answered by the runtime, not the library", line);
        });
    }

    static void continueWith(final String name, final ILuaContinuation continuation) {
        CONTINUATIONS.put(name, continuation);
    }

    static void constant(final String name, final Object value) {
        CONSTANTS.put(name, value);
    }

    /** A name that is given a fresh, empty table of its own in every program. */
    static void table(final String name) {
        TABLES.add(name);
    }

    /** Puts every function and constant into the table of globals, making the library tables on the way. */
    public static void install(final ILuaContext context, final Values.Table globals, final int line) {
        for (final String name : FUNCTIONS.keySet()) {
            if (!HIDDEN.contains(name)) {
                put(context, globals, name, context.function(name, null, line), line);
            }
        }
        for (final Map.Entry<String, Object> constant : CONSTANTS.entrySet()) {
            final Object value = constant.getValue() instanceof String text ? context.text(text, line)
                    : constant.getValue();
            put(context, globals, constant.getKey(), value, line);
        }
        for (final String name : TABLES) {
            put(context, globals, name, context.table(line), line);
        }
        globals.put(context.text("_G", line), globals);
        context.resized(globals, line);
        LuaCc.installed(context, globals, line);
    }

    private static void put(final ILuaContext context, final Values.Table globals, final String name,
                            final Object value, final int line) {
        final int dot = name.indexOf('.');
        if (dot < 0) {
            globals.put(context.text(name, line), value);
            return;
        }
        final String library = name.substring(0, dot);
        Values.Table into = globals.get(library) instanceof Values.Table existing ? existing : null;
        if (into == null) {
            into = context.table(line);
            globals.put(context.text(library, line), into);
        }
        into.put(context.text(name.substring(dot + 1), line), value);
        context.resized(into, line);
    }
}
