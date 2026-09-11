/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * What a ComputerCraft computer gives a program on top of the language: its events and timers,
 * {@code parallel}, the shell, {@code require}, settings, and the devices it could have.
 *
 * <p>Everything that waits does it the way ComputerCraft's own Lua does, by pulling events through
 * the program's {@code os.pullEvent}, so a program that swaps that function for the raw one (to keep
 * Ctrl+T from ending it) has its sleep and its reads behave the same way. {@code parallel} runs its
 * functions as coroutines and hands each the events it asked for, which is exactly how a coroutine's
 * {@code os.pullEvent} is answered.
 *
 * <p>The devices are those of a computer with nothing attached: no peripherals, no modem. What this
 * computer does not have at all ({@code window}, tabs) says so when called rather than being missing,
 * so a program learns why it stopped.
 */
final class LuaCc {

    /** What {@code _HOST} says this is. */
    static final String HOST = "J's Computers lrt 5.2 (Minecraft 1.21.1)";

    /** The version of the computer's system that ComputerCraft programs compare against. */
    private static final String SYSTEM_VERSION = "CraftOS 1.9";

    /** The folders {@code require} looks in, from the folder of the program asking. */
    private static final String PACKAGE_PATH = "?;?.lua;?/init.lua";

    /** The libraries {@code package.loaded} starts with, as the language has them. */
    private static final List<String> BUILT_IN = List.of("_G", "bit32", "coroutine", "io", "math", "os",
            "package", "string", "table");

    private static final String SETTINGS_FILE = ".settings";

    private static final String TIMER = "Timer";
    private static final String ROUTINES = "Routines";
    private static final String FILTERS = "Filters";
    private static final String EVENT = "Event";
    private static final String AT = "At";
    private static final String COUNT = "Count";
    private static final String LIVING = "Living";
    private static final String LIMIT = "Limit";
    private static final String NAME = "Name";
    private static final String PROTOCOL = "Protocol";
    private static final String SETTINGS = "Settings";
    private static final String DEFINED = "SettingsDefined";
    private static final String SETTINGS_READ = "SettingsRead";
    private static final String LOADING = "RequireLoading";

    private LuaCc() {
    }

    static void register() {
        LuaLib.intrinsic("os.pullEvent");
        LuaLib.intrinsic("os.pullEventRaw");
        LuaLib.constant("_HOST", HOST);
        LuaLib.constant("_CC_DEFAULT_SETTINGS", "");
        registerOs();
        registerParallel();
        registerShell();
        registerRequire();
        registerSettings();
        registerDevices();
    }

    /** Fills in what can only be made once the globals are there: the libraries already loaded. */
    static void installed(final ILuaContext context, final Values.Table globals, final int line) {
        if (!(globals.get("package") instanceof Values.Table pkg) || !(pkg.get("loaded") instanceof Values.Table loaded)) {
            return;
        }
        for (final String name : BUILT_IN) {
            final Object library = globals.get(name);
            if (library != null) {
                loaded.put(context.text(name, line), library);
            }
        }
        context.resized(loaded, line);
    }

    // os

    private static void registerOs() {
        LuaLib.define("os.queueEvent", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "queueEvent", arguments, line);
            final List<Object> values = new ArrayList<>(Math.max(1, arguments.length));
            values.add(context.text(args.text(0), line));
            for (int i = 1; i < arguments.length; i++) {
                values.add(arguments[i]);
            }
            context.queueEvent(values);
            return null;
        });
        LuaLib.define("os.startTimer", (context, target, arguments, line) ->
                context.startTimer(new LuaArgs(context, "startTimer", arguments, line).real(0)));
        LuaLib.define("os.cancelTimer", (context, target, arguments, line) -> {
            context.cancelTimer(new LuaArgs(context, "cancelTimer", arguments, line).integer(0));
            return null;
        });
        LuaLib.define("os.setAlarm", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "setAlarm", arguments, line);
            final double hour = args.real(0);
            if (hour < 0 || hour >= 24) {
                throw args.bad(0, "Number out of range");
            }
            return context.setAlarm(hour);
        });
        LuaLib.define("os.cancelAlarm", (context, target, arguments, line) -> {
            context.cancelAlarm(new LuaArgs(context, "cancelAlarm", arguments, line).integer(0));
            return null;
        });
        final ILuaFunction sleep = (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "sleep", arguments, line);
            final Values.Obj state = context.state("sleep.event", null, line);
            state.set(TIMER, context.startTimer(args.has(0) ? args.real(0) : 0));
            return pull(context, state, "timer", line);
        };
        LuaLib.define("sleep", sleep);
        LuaLib.define("os.sleep", sleep);
        LuaLib.continueWith("sleep.event", (context, state, result, line) -> {
            final List<Object> event = context.expand(result);
            if (event.size() > 1 && LuaValues.rawEqual(event.get(1), state.get(TIMER))) {
                return context.values(List.of(), line);
            }
            return pull(context, state, "timer", line);
        });
        final ILuaFunction id = (context, target, arguments, line) -> context.computerId();
        LuaLib.define("os.getComputerID", id);
        LuaLib.define("os.computerID", id);
        final ILuaFunction label = (context, target, arguments, line) -> {
            final String name = context.computerName();
            return name.isEmpty() ? null : context.text(name, line);
        };
        LuaLib.define("os.getComputerLabel", label);
        LuaLib.define("os.computerLabel", label);
        LuaLib.define("os.setComputerLabel", (context, target, arguments, line) -> {
            throw context.error("this computer's name is given by its owner, not by a program", line);
        });
        LuaLib.define("os.version", (context, target, arguments, line) -> context.text(SYSTEM_VERSION, line));
        final ILuaFunction off = (context, target, arguments, line) -> {
            context.exit();
            return null;
        };
        LuaLib.define("os.shutdown", off);
        LuaLib.define("os.reboot", off);
        LuaLib.define("os.run", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "run", arguments, line);
            final Values.Table environment = args.table(0);
            return runFile(context, environment, LuaFs.normalise(args.text(1)), args.from(2), line);
        });
        LuaLib.continueWith("run.done", (context, state, result, line) -> true);
        LuaLib.continueWith("run.failed", (context, state, raised, line) -> {
            final String message = raised == null ? "" : LuaValues.plainString(raised);
            if (!message.isEmpty()) {
                printError(context, message, line);
            }
            return false;
        });
    }

    /** Waits for the next event through the program's own {@code os.pullEvent}, as ComputerCraft does. */
    private static LuaCall pull(final ILuaContext context, final Values.Obj state, final String filter, final int line) {
        return new LuaCall(os(context, "pullEvent", line),
                filter == null ? new Object[0] : new Object[] {context.text(filter, line)}, state);
    }

    /** One of the program's os functions, or the library's own when the program has taken it away. */
    private static Object os(final ILuaContext context, final String name, final int line) {
        if (context.global("os") instanceof Values.Table os && os.get(name) != null) {
            return os.get(name);
        }
        return context.function("os." + name, null, line);
    }

    /**
     * Runs a file in an environment of its own, as {@code os.run} and {@code shell.run} do: true when it
     * finished, false when it could not start or raised, with what it raised printed in red.
     */
    private static Object runFile(final ILuaContext context, final Values.Table environment, final String path,
                                  final Object[] arguments, final int line) {
        final String text = context.files().read(path);
        if (text == null) {
            printError(context, "File not found", line);
            return false;
        }
        if (environment.metatable() == null) {
            final Values.Table meta = context.table(line);
            meta.put(context.text("__index", line), context.global("_G"));
            context.resized(meta, line);
            environment.setMetatable(meta);
        }
        final List<Object> loaded = context.expand(context.loadChunk(text, "@/" + path, environment, line));
        if (loaded.isEmpty() || !(loaded.getFirst() instanceof Values.DelegateValue function)) {
            printError(context, loaded.size() > 1 ? LuaValues.plainString(loaded.get(1)) : "cannot load " + path, line);
            return false;
        }
        return new LuaCall(function, arguments, context.state("run.done", "run.failed", line));
    }

    private static void printError(final ILuaContext context, final String message, final int line) {
        LuaLib.function("printError").call(context, null, new Object[] {context.text(message, line)}, line);
    }

    // parallel

    /*
     * The functions run as coroutines, resumed in turn with each event, each only with the kind of event
     * it last asked for (terminate always goes through). It ends when enough of them have finished: one
     * for waitForAny, all of them for waitForAll. An error in any of them is an error of the whole.
     */
    private static void registerParallel() {
        LuaLib.define("parallel.waitForAny", (context, target, arguments, line) ->
                startParallel(context, arguments, "waitForAny", true, line));
        LuaLib.define("parallel.waitForAll", (context, target, arguments, line) ->
                startParallel(context, arguments, "waitForAll", false, line));
        LuaLib.continueWith("parallel.resumed", LuaCc::parallelResumed);
        LuaLib.continueWith("parallel.event", (context, state, result, line) -> {
            state.set(EVENT, context.values(context.expand(result), line));
            state.set(AT, 1L);
            return parallelStep(context, state, line);
        });
    }

    private static Object startParallel(final ILuaContext context, final Object[] arguments, final String name,
                                        final boolean any, final int line) {
        final LuaArgs args = new LuaArgs(context, name, arguments, line);
        final Values.Table routines = context.table(line);
        for (int i = 0; i < arguments.length; i++) {
            routines.put((long) (i + 1), context.createCoroutine(args.function(i), line));
        }
        context.resized(routines, line);
        if (arguments.length == 0) {
            return null;
        }
        final Values.Obj state = context.state("parallel.resumed", null, line);
        state.set(ROUTINES, routines);
        state.set(FILTERS, context.table(line));
        state.set(EVENT, context.values(List.of(), line));
        state.set(AT, 1L);
        state.set(COUNT, (long) arguments.length);
        state.set(LIVING, (long) arguments.length);
        state.set(LIMIT, any ? (long) arguments.length - 1 : 0L);
        return parallelStep(context, state, line);
    }

    /* Resumes the next coroutine the event is for; when none is left, waits for the next event. */
    private static Object parallelStep(final ILuaContext context, final Values.Obj state, final int line) {
        final Values.Table routines = (Values.Table) state.get(ROUTINES);
        final Values.Table filters = (Values.Table) state.get(FILTERS);
        final List<Object> event = context.expand(state.get(EVENT));
        final Object kind = event.isEmpty() ? null : event.getFirst();
        final long count = (Long) state.get(COUNT);
        for (long n = (Long) state.get(AT); n <= count; n++) {
            final Object routine = routines.get(n);
            if (routine == null) {
                continue;
            }
            final Object filter = filters.get(n);
            if (filter == null || LuaValues.rawEqual(filter, kind) || "terminate".equals(kind)) {
                state.set(AT, n);
                state.set(LuaCall.NEXT, context.text("parallel.resumed", line));
                final Object[] passed = new Object[event.size() + 1];
                passed[0] = routine;
                for (int i = 0; i < event.size(); i++) {
                    passed[i + 1] = event.get(i);
                }
                return new LuaCall(context.function("coroutine.resume", null, line), passed, state);
            }
        }
        // Anything that finished without being resumed this round (it was already dead) is counted too.
        for (long n = 1; n <= count; n++) {
            final Object routine = routines.get(n);
            if (routine != null && context.coroutineDead(routine)) {
                final Object done = finished(context, state, n, line);
                if (done != null) {
                    return done;
                }
            }
        }
        state.set(LuaCall.NEXT, context.text("parallel.event", line));
        return new LuaCall(os(context, "pullEventRaw", line), new Object[0], state);
    }

    private static Object parallelResumed(final ILuaContext context, final Values.Obj state, final Object result,
                                          final int line) {
        final List<Object> answer = context.expand(result);
        final long n = (Long) state.get(AT);
        if (answer.isEmpty() || !Boolean.TRUE.equals(answer.getFirst())) {
            throw context.raise(answer.size() > 1 ? answer.get(1) : null, line);
        }
        final Values.Table filters = (Values.Table) state.get(FILTERS);
        filters.put(n, answer.size() > 1 && answer.get(1) instanceof String wanted ? wanted : null);
        final Object routine = ((Values.Table) state.get(ROUTINES)).get(n);
        if (routine != null && context.coroutineDead(routine)) {
            final Object done = finished(context, state, n, line);
            if (done != null) {
                return done;
            }
        }
        state.set(AT, n + 1);
        return parallelStep(context, state, line);
    }

    /* Takes a finished coroutine off the list; what to answer when that was enough, else null. */
    private static Object finished(final ILuaContext context, final Values.Obj state, final long n, final int line) {
        final Values.Table routines = (Values.Table) state.get(ROUTINES);
        routines.put(n, null);
        final long living = (Long) state.get(LIVING) - 1;
        state.set(LIVING, living);
        if (living <= (Long) state.get(LIMIT)) {
            return context.values(List.of(n), line);
        }
        return null;
    }

    // shell

    private static void registerShell() {
        LuaLib.define("shell.dir", (context, target, arguments, line) -> context.text(context.directory(), line));
        LuaLib.define("shell.setDir", (context, target, arguments, line) -> {
            final String path = LuaFs.resolve(context, new LuaArgs(context, "setDir", arguments, line).text(0));
            final ILuaFiles.Entry entry = path.isEmpty() ? null : context.files().stat(path);
            if (!path.isEmpty() && (entry == null || !entry.directory())) {
                throw context.error("Not a directory", line);
            }
            context.setDirectory(path);
            return null;
        });
        LuaLib.define("shell.resolve", (context, target, arguments, line) ->
                context.text(LuaFs.resolve(context, new LuaArgs(context, "resolve", arguments, line).text(0)), line));
        LuaLib.define("shell.resolveProgram", (context, target, arguments, line) -> {
            final String found = resolveProgram(context, new LuaArgs(context, "resolveProgram", arguments, line).text(0));
            return found == null ? null : context.text(found, line);
        });
        LuaLib.define("shell.getRunningProgram", (context, target, arguments, line) ->
                context.text(context.programName(), line));
        LuaLib.define("shell.run", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "run", arguments, line);
            final StringBuilder command = new StringBuilder();
            for (int i = 0; i < arguments.length; i++) {
                command.append(i == 0 ? "" : " ").append(args.text(i));
            }
            final List<String> words = words(command.toString());
            if (words.isEmpty()) {
                return false;
            }
            final Object[] passed = new Object[words.size() - 1];
            for (int i = 1; i < words.size(); i++) {
                passed[i - 1] = context.text(words.get(i), line);
            }
            return runProgram(context, words.getFirst(), passed, line);
        });
        LuaLib.define("shell.execute", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "execute", arguments, line);
            return runProgram(context, args.text(0), args.from(1), line);
        });
        LuaLib.define("shell.exit", (context, target, arguments, line) -> {
            context.exit();
            return null;
        });
        LuaLib.define("shell.path", (context, target, arguments, line) -> context.text(".", line));
        LuaLib.define("shell.setPath", (context, target, arguments, line) -> null);
        LuaLib.define("shell.aliases", (context, target, arguments, line) -> context.table(line));
        LuaLib.define("shell.setAlias", (context, target, arguments, line) -> null);
        LuaLib.define("shell.clearAlias", (context, target, arguments, line) -> null);
        LuaLib.define("shell.programs", (context, target, arguments, line) -> {
            final Values.Table out = context.table(line);
            final List<ILuaFiles.Entry> entries = context.files().list(context.directory());
            final TreeSet<String> names = new TreeSet<>();
            if (entries != null) {
                for (final ILuaFiles.Entry entry : entries) {
                    if (!entry.directory() && entry.name().toLowerCase(java.util.Locale.ROOT).endsWith(".lua")) {
                        names.add(entry.name().substring(0, entry.name().length() - 4));
                    }
                }
            }
            long i = 1;
            for (final String name : names) {
                out.put(i++, context.text(name, line));
            }
            context.resized(out, line);
            return out;
        });
        LuaLib.define("shell.complete", (context, target, arguments, line) -> null);
        LuaLib.define("shell.completeProgram", (context, target, arguments, line) -> context.table(line));
        LuaLib.define("shell.setCompletionFunction", (context, target, arguments, line) -> null);
        LuaLib.define("shell.getCompletionInfo", (context, target, arguments, line) -> context.table(line));
        final ILuaFunction tabs = (context, target, arguments, line) -> {
            throw context.error("this computer has no tabs", line);
        };
        LuaLib.define("shell.openTab", tabs);
        LuaLib.define("shell.switchTab", tabs);
    }

    /** The file a program name means, as the shell finds it: the name itself, or with .lua after it. */
    private static String resolveProgram(final ILuaContext context, final String name) {
        for (final String candidate : List.of(name, name + ".lua")) {
            final String path = LuaFs.resolve(context, candidate);
            final ILuaFiles.Entry entry = path.isEmpty() ? null : context.files().stat(path);
            if (entry != null && !entry.directory()) {
                return path;
            }
        }
        return null;
    }

    private static Object runProgram(final ILuaContext context, final String name, final Object[] arguments,
                                     final int line) {
        final String path = resolveProgram(context, name);
        if (path == null) {
            printError(context, "No such program", line);
            return false;
        }
        final Values.Table environment = context.table(line);
        final Values.Table arg = context.table(line);
        arg.put(0L, context.text(path, line));
        for (int i = 0; i < arguments.length; i++) {
            arg.put((long) (i + 1), arguments[i]);
        }
        context.resized(arg, line);
        environment.put(context.text("arg", line), arg);
        context.resized(environment, line);
        return runFile(context, environment, path, arguments, line);
    }

    /* A command line in words, a quoted run of them counting as one. */
    private static List<String> words(final String command) {
        final List<String> out = new ArrayList<>();
        final StringBuilder word = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        for (int i = 0; i < command.length(); i++) {
            final char c = command.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                any = true;
            } else if (Character.isWhitespace(c) && !quoted) {
                if (any) {
                    out.add(word.toString());
                    word.setLength(0);
                    any = false;
                }
            } else {
                word.append(c);
                any = true;
            }
        }
        if (any) {
            out.add(word.toString());
        }
        return out;
    }

    // require, dofile, loadfile

    private static void registerRequire() {
        LuaLib.table("package.loaded");
        LuaLib.table("package.preload");
        LuaLib.constant("package.path", PACKAGE_PATH);
        LuaLib.constant("package.config", "/\n;\n?\n!\n-");
        LuaLib.define("require", LuaCc::require);
        LuaLib.continueWith("require.done", (context, state, result, line) -> {
            final List<Object> values = context.expand(result);
            final Object value = values.isEmpty() || values.getFirst() == null ? Boolean.TRUE : values.getFirst();
            final String name = String.valueOf(state.get(NAME));
            if (context.global("package") instanceof Values.Table pkg && pkg.get("loaded") instanceof Values.Table loaded) {
                loaded.put(context.text(name, line), value);
                context.resized(loaded, line);
            }
            context.store(LOADING, line).put(name, null);
            return value;
        });
        LuaLib.define("package.searchpath", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "searchpath", arguments, line);
            final List<String> tried = new ArrayList<>();
            final String found = search(context, args.text(0), args.text(1), tried);
            if (found != null) {
                return context.text(found, line);
            }
            return context.values(List.of(context.text(notFound(tried), line)), line);
        });
        LuaLib.define("dofile", (context, target, arguments, line) -> {
            final String path = LuaFs.normalise(new LuaArgs(context, "dofile", arguments, line).text(0));
            final String text = context.files().read(path);
            if (text == null) {
                throw context.error("File not found", line);
            }
            final List<Object> loaded = context.expand(context.loadChunk(text, "@/" + path, null, line));
            if (loaded.isEmpty() || !(loaded.getFirst() instanceof Values.DelegateValue function)) {
                throw context.raise(loaded.size() > 1 ? loaded.get(1) : null, line);
            }
            return new LuaCall(function, new Object[0], context.state("dofile.done", null, line));
        });
        LuaLib.continueWith("dofile.done", (context, state, result, line) -> result);
        LuaLib.define("loadfile", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "loadfile", arguments, line);
            final String path = LuaFs.normalise(args.text(0));
            final String text = context.files().read(path);
            if (text == null) {
                final List<Object> failed = new ArrayList<>(2);
                failed.add(null);
                failed.add(context.text("File not found", line));
                return context.values(failed, line);
            }
            return context.loadChunk(text, "@/" + path, args.at(2) instanceof Values.Table given ? given : null, line);
        });
    }

    private static Object require(final ILuaContext context, final Object target, final Object[] arguments,
                                  final int line) {
        final String name = new LuaArgs(context, "require", arguments, line).text(0);
        if (!(context.global("package") instanceof Values.Table pkg)) {
            throw context.error("package is not a table", line);
        }
        if (pkg.get("loaded") instanceof Values.Table loaded && loaded.get(name) != null) {
            return loaded.get(name);
        }
        final Values.Table loading = context.store(LOADING, line);
        if (loading.get(name) != null) {
            throw context.error("loop or previous error loading module '" + name + "'", line);
        }
        final Values.Obj state = context.state("require.done", null, line);
        state.set(NAME, context.text(name, line));
        if (pkg.get("preload") instanceof Values.Table preload
                && preload.get(name) instanceof Values.DelegateValue loader) {
            loading.put(context.text(name, line), Boolean.TRUE);
            context.resized(loading, line);
            return new LuaCall(loader, new Object[] {context.text(name, line), context.text(":preload:", line)}, state);
        }
        final String template = pkg.get("path") instanceof String given ? given : PACKAGE_PATH;
        final List<String> tried = new ArrayList<>();
        tried.add("no field package.preload['" + name + "']");
        final String path = search(context, name, template, tried);
        if (path == null) {
            throw context.error("module '" + name + "' not found:\n  " + String.join("\n  ", tried), line);
        }
        final String text = context.files().read(path);
        if (text == null) {
            throw context.error("module '" + name + "' is at '" + path + "' but cannot be read", line);
        }
        final List<Object> chunk = context.expand(context.loadChunk(text, "@/" + path, null, line));
        if (chunk.isEmpty() || !(chunk.getFirst() instanceof Values.DelegateValue function)) {
            throw context.error("error loading module '" + name + "' from file '" + path + "':\n\t"
                    + (chunk.size() > 1 ? LuaValues.plainString(chunk.get(1)) : ""), line);
        }
        loading.put(context.text(name, line), Boolean.TRUE);
        context.resized(loading, line);
        return new LuaCall(function, new Object[] {context.text(name, line), context.text(path, line)}, state);
    }

    /*
     * The first file the path's patterns name that is there. A pattern that does not start at the root
     * is taken from the folder of the program asking, which is where ComputerCraft looks.
     */
    private static String search(final ILuaContext context, final String name, final String template,
                                 final List<String> tried) {
        final String program = context.programName();
        final String base = program.isEmpty() ? context.directory() : LuaFs.dirOf(program);
        for (final String pattern : template.split(";")) {
            if (pattern.isEmpty()) {
                continue;
            }
            final String filled = pattern.replace("?", name.replace('.', '/'));
            final String path = filled.startsWith("/") ? LuaFs.normalise(filled)
                    : LuaFs.normalise((base.equals("..") ? "" : base) + "/" + filled);
            final ILuaFiles.Entry entry = path.isEmpty() ? null : context.files().stat(path);
            if (entry != null && !entry.directory()) {
                return path;
            }
            tried.add("no file '" + path + "'");
        }
        return null;
    }

    private static String notFound(final List<String> tried) {
        return String.join("\n  ", tried);
    }

    // settings

    private static void registerSettings() {
        LuaLib.define("settings.define", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "define", arguments, line);
            final String name = args.text(0);
            final Values.Table options = args.has(1) ? args.table(1) : context.table(line);
            final Values.Table defined = context.store(DEFINED, line);
            defined.put(context.text(name, line), options);
            context.resized(defined, line);
            return null;
        });
        LuaLib.define("settings.undefine", (context, target, arguments, line) -> {
            context.store(DEFINED, line).put(new LuaArgs(context, "undefine", arguments, line).text(0), null);
            return null;
        });
        LuaLib.define("settings.set", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "set", arguments, line);
            final String name = args.text(0);
            final Object value = args.at(1);
            if (value instanceof Values.DelegateValue) {
                throw args.bad(1, "cannot store a function");
            }
            final String type = typeOf(context, name);
            if (type != null && value != null && !type.equals(LuaValues.typeName(value))) {
                throw args.bad(1, type + " expected, got " + LuaValues.typeName(value));
            }
            final Values.Table settings = settings(context, line);
            settings.put(context.text(name, line), value);
            context.resized(settings, line);
            return null;
        });
        LuaLib.define("settings.get", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "get", arguments, line);
            final String name = args.text(0);
            final Object value = settings(context, line).get(name);
            if (value != null) {
                return value;
            }
            if (args.count() > 1) {
                return args.at(1);
            }
            return context.store(DEFINED, line).get(name) instanceof Values.Table options ? options.get("default") : null;
        });
        LuaLib.define("settings.unset", (context, target, arguments, line) -> {
            settings(context, line).put(new LuaArgs(context, "unset", arguments, line).text(0), null);
            return null;
        });
        LuaLib.define("settings.clear", (context, target, arguments, line) -> {
            final Values.Table settings = settings(context, line);
            final List<Object> keys = new ArrayList<>();
            for (Object key = settings.nextKey(null); key != null; key = settings.nextKey(key)) {
                keys.add(key);
            }
            for (final Object key : keys) {
                settings.put(key, null);
            }
            return null;
        });
        LuaLib.define("settings.getNames", (context, target, arguments, line) -> {
            final TreeSet<String> names = new TreeSet<>();
            for (final Values.Table from : List.of(settings(context, line), context.store(DEFINED, line))) {
                for (Object key = from.nextKey(null); key != null; key = from.nextKey(key)) {
                    if (key instanceof String name) {
                        names.add(name);
                    }
                }
            }
            final Values.Table out = context.table(line);
            long i = 1;
            for (final String name : names) {
                out.put(i++, context.text(name, line));
            }
            context.resized(out, line);
            return out;
        });
        LuaLib.define("settings.getDetails", (context, target, arguments, line) -> {
            final String name = new LuaArgs(context, "getDetails", arguments, line).text(0);
            final Values.Table out = context.table(line);
            if (context.store(DEFINED, line).get(name) instanceof Values.Table options) {
                for (final String field : List.of("description", "default", "type")) {
                    if (options.get(field) != null) {
                        out.put(context.text(field, line), options.get(field));
                    }
                }
            }
            final Object value = settings(context, line).get(name);
            out.put(context.text("value", line), value);
            out.put(context.text("changed", line), value != null);
            context.resized(out, line);
            return out;
        });
        LuaLib.define("settings.save", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "save", arguments, line);
            final Object text = LuaLib.function("textutils.serialize").call(context, null,
                    new Object[] {settings(context, line)}, line);
            return context.files().write(LuaFs.normalise(args.text(0, SETTINGS_FILE)), LuaValues.plainString(text)) == null;
        });
        LuaLib.define("settings.load", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "load", arguments, line);
            return readSettings(context, settings(context, line), LuaFs.normalise(args.text(0, SETTINGS_FILE)), line);
        });
    }

    /* The program's settings, read from the computer's settings file the first time they are asked for. */
    private static Values.Table settings(final ILuaContext context, final int line) {
        final Values.Table settings = context.store(SETTINGS, line);
        final Values.Table read = context.store(SETTINGS_READ, line);
        if (read.get(1L) == null) {
            read.put(1L, Boolean.TRUE);
            context.resized(read, line);
            readSettings(context, settings, SETTINGS_FILE, line);
        }
        return settings;
    }

    private static boolean readSettings(final ILuaContext context, final Values.Table into, final String path,
                                        final int line) {
        final String text = context.files().read(path);
        if (text == null) {
            return false;
        }
        final Object read = LuaLib.function("textutils.unserialize").call(context, null,
                new Object[] {context.text(text, line)}, line);
        if (!(read instanceof Values.Table values)) {
            return false;
        }
        for (Object key = values.nextKey(null); key != null; key = values.nextKey(key)) {
            into.put(key, values.get(key));
        }
        context.resized(into, line);
        return true;
    }

    private static String typeOf(final ILuaContext context, final String name) {
        return context.store(DEFINED, 0).get(name) instanceof Values.Table options && options.get("type") instanceof String type
                ? type : null;
    }

    // devices

    private static void registerDevices() {
        LuaLib.define("peripheral.getNames", (context, target, arguments, line) -> context.table(line));
        LuaLib.define("peripheral.isPresent", (context, target, arguments, line) -> false);
        LuaLib.define("peripheral.getType", (context, target, arguments, line) -> null);
        LuaLib.define("peripheral.hasType", (context, target, arguments, line) -> null);
        LuaLib.define("peripheral.getMethods", (context, target, arguments, line) -> null);
        LuaLib.define("peripheral.wrap", (context, target, arguments, line) -> null);
        LuaLib.define("peripheral.find", (context, target, arguments, line) -> context.values(List.of(), line));
        LuaLib.define("peripheral.call", (context, target, arguments, line) -> {
            throw context.error("No peripheral attached", line);
        });
        LuaLib.define("rednet.open", (context, target, arguments, line) -> {
            throw context.error("No such modem: " + new LuaArgs(context, "open", arguments, line).text(0), line);
        });
        LuaLib.define("rednet.close", (context, target, arguments, line) -> null);
        LuaLib.define("rednet.isOpen", (context, target, arguments, line) -> false);
        LuaLib.define("rednet.send", (context, target, arguments, line) -> false);
        LuaLib.define("rednet.broadcast", (context, target, arguments, line) -> null);
        LuaLib.define("rednet.lookup", (context, target, arguments, line) -> null);
        final ILuaFunction noModem = (context, target, arguments, line) -> {
            throw context.error("No modem is open", line);
        };
        LuaLib.define("rednet.host", noModem);
        LuaLib.define("rednet.unhost", noModem);
        LuaLib.define("rednet.receive", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "receive", arguments, line);
            final Values.Obj state = context.state("rednet.event", null, line);
            int timeoutAt = 1;
            if (args.at(0) instanceof String protocol) {
                state.set(PROTOCOL, context.text(protocol, line));
            } else {
                timeoutAt = 0;
            }
            if (args.has(timeoutAt)) {
                state.set(TIMER, context.startTimer(args.real(timeoutAt)));
            }
            return pull(context, state, null, line);
        });
        LuaLib.continueWith("rednet.event", (context, state, result, line) -> {
            final List<Object> event = context.expand(result);
            final Object kind = event.isEmpty() ? null : event.getFirst();
            if ("rednet_message".equals(kind)) {
                final Object protocol = event.size() > 3 ? event.get(3) : null;
                if (state.get(PROTOCOL) == null || LuaValues.rawEqual(state.get(PROTOCOL), protocol)) {
                    return context.values(event.subList(1, Math.min(event.size(), 4)), line);
                }
            } else if ("timer".equals(kind) && state.get(TIMER) != null && event.size() > 1
                    && LuaValues.rawEqual(event.get(1), state.get(TIMER))) {
                return null;
            }
            return pull(context, state, null, line);
        });
        LuaLib.define("gps.locate", (context, target, arguments, line) -> null);
        LuaLib.define("window.create", (context, target, arguments, line) -> {
            throw context.error("windows are not available on this computer", line);
        });
        final ILuaFunction screen = (context, target, arguments, line) -> context.global("term");
        LuaLib.define("term.current", screen);
        LuaLib.define("term.native", screen);
        LuaLib.define("term.redirect", (context, target, arguments, line) -> {
            final Object term = context.global("term");
            if (arguments.length > 0 && arguments[0] != term) {
                throw context.error("this computer has only its own screen to draw on", line);
            }
            return term;
        });
    }
}
