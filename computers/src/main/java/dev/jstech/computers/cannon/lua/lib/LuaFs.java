/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * ComputerCraft's {@code fs}, and the file side of {@code io}, on the disks of the machine the program
 * runs on.
 *
 * <p>A path is ComputerCraft's: from the machine's root, parts joined with a slash, {@code ..} going
 * up and never above the root. A file opened for writing is kept in the program until it is flushed
 * or closed, which is when the disk sees it, so a program writing a line at a time pays for the disk
 * once rather than on every line.
 */
final class LuaFs {

    private static final String PATH = "Path";
    private static final String MODE = "Mode";
    private static final String TEXT = "Text";
    private static final String POSITION = "Pos";
    private static final String CLOSED = "Closed";
    private static final int GLOB_LIMIT = 4096;

    private LuaFs() {
    }

    /** The path made plain: no empty parts, no dots, and nothing that climbs above the root. */
    static String normalise(final String path) {
        final Deque<String> parts = new ArrayDeque<>();
        for (final String part : path.replace('\\', '/').split("/")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty()) {
                    parts.removeLast();
                }
                continue;
            }
            parts.addLast(part);
        }
        return String.join("/", parts);
    }

    /** A path as a program at the shell means it: from the root when it starts with a slash, else from its folder. */
    static String resolve(final ILuaContext context, final String path) {
        if (path.startsWith("/") || path.startsWith("\\")) {
            return normalise(path);
        }
        return normalise(context.directory() + "/" + path);
    }

    static String nameOf(final String path) {
        final String plain = normalise(path);
        final int slash = plain.lastIndexOf('/');
        return plain.isEmpty() ? "root" : plain.substring(slash + 1);
    }

    static String dirOf(final String path) {
        final String plain = normalise(path);
        if (plain.isEmpty()) {
            return "..";
        }
        final int slash = plain.lastIndexOf('/');
        return slash < 0 ? "" : plain.substring(0, slash);
    }

    static void register() {
        LuaLib.define("fs.combine", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "combine", arguments, line);
            final StringBuilder joined = new StringBuilder(args.text(0));
            for (int i = 1; i < arguments.length; i++) {
                joined.append('/').append(args.text(i));
            }
            return context.text(normalise(joined.toString()), line);
        });
        LuaLib.define("fs.getName", (context, target, arguments, line) ->
                context.text(nameOf(new LuaArgs(context, "getName", arguments, line).text(0)), line));
        LuaLib.define("fs.getDir", (context, target, arguments, line) ->
                context.text(dirOf(new LuaArgs(context, "getDir", arguments, line).text(0)), line));
        LuaLib.define("fs.list", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "list", arguments, line).text(0));
            final List<ILuaFiles.Entry> entries = context.files().list(path);
            if (entries == null) {
                throw context.error("/" + path + ": Not a directory", line);
            }
            final List<String> names = new ArrayList<>();
            for (final ILuaFiles.Entry entry : entries) {
                names.add(entry.name());
            }
            names.sort(String::compareTo);
            final Values.Table out = context.table(line);
            for (int i = 0; i < names.size(); i++) {
                out.put((long) (i + 1), context.text(names.get(i), line));
            }
            context.resized(out, line);
            return out;
        });
        LuaLib.define("fs.exists", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "exists", arguments, line).text(0));
            return path.isEmpty() || context.files().stat(path) != null;
        });
        LuaLib.define("fs.isDir", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "isDir", arguments, line).text(0));
            if (path.isEmpty()) {
                return true;
            }
            final ILuaFiles.Entry entry = context.files().stat(path);
            return entry != null && entry.directory();
        });
        LuaLib.define("fs.isReadOnly", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "isReadOnly", arguments, line).text(0));
            final ILuaFiles.Entry entry = path.isEmpty() ? null : context.files().stat(path);
            return entry != null && entry.readOnly();
        });
        LuaLib.define("fs.getSize", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "getSize", arguments, line).text(0));
            final ILuaFiles.Entry entry = context.files().stat(path);
            if (entry == null) {
                throw context.error("/" + path + ": No such file", line);
            }
            return entry.directory() ? 0L : entry.size();
        });
        LuaLib.define("fs.getFreeSpace", (context, target, arguments, line) ->
                context.files().free(normalise(new LuaArgs(context, "getFreeSpace", arguments, line).text(0))));
        LuaLib.define("fs.getCapacity", (context, target, arguments, line) ->
                context.files().capacity(normalise(new LuaArgs(context, "getCapacity", arguments, line).text(0))));
        LuaLib.define("fs.getDrive", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "getDrive", arguments, line).text(0));
            return path.isEmpty() || context.files().stat(path) != null ? context.text("hdd", line) : null;
        });
        LuaLib.define("fs.isDriveRoot", (context, target, arguments, line) ->
                normalise(new LuaArgs(context, "isDriveRoot", arguments, line).text(0)).isEmpty());
        LuaLib.define("fs.makeDir", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "makeDir", arguments, line).text(0));
            makeDirs(context, path, line);
            return null;
        });
        LuaLib.define("fs.delete", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "delete", arguments, line).text(0));
            if (context.files().stat(path) != null) {
                delete(context, path, line);
            }
            return null;
        });
        LuaLib.define("fs.copy", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "copy", arguments, line);
            copy(context, normalise(args.text(0)), normalise(args.text(1)), line);
            return null;
        });
        LuaLib.define("fs.move", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "move", arguments, line);
            final String from = normalise(args.text(0));
            copy(context, from, normalise(args.text(1)), line);
            delete(context, from, line);
            return null;
        });
        LuaLib.define("fs.attributes", (context, target, arguments, line) -> {
            final String path = normalise(new LuaArgs(context, "attributes", arguments, line).text(0));
            final ILuaFiles.Entry entry = path.isEmpty() ? new ILuaFiles.Entry("", true, 0, false, 0)
                    : context.files().stat(path);
            if (entry == null) {
                throw context.error("/" + path + ": No such file", line);
            }
            final Values.Table out = context.table(line);
            out.put(context.text("size", line), entry.directory() ? 0L : entry.size());
            out.put(context.text("isDir", line), entry.directory());
            out.put(context.text("isReadOnly", line), entry.readOnly());
            out.put(context.text("created", line), entry.modified());
            out.put(context.text("modified", line), entry.modified());
            out.put(context.text("modification", line), entry.modified());
            context.resized(out, line);
            return out;
        });
        LuaLib.define("fs.find", (context, target, arguments, line) -> {
            final String pattern = normalise(new LuaArgs(context, "find", arguments, line).text(0));
            final List<String> found = new ArrayList<>();
            find(context, "", pattern.isEmpty() ? new String[0] : pattern.split("/"), 0, found);
            found.sort(String::compareTo);
            final Values.Table out = context.table(line);
            for (int i = 0; i < found.size(); i++) {
                out.put((long) (i + 1), context.text(found.get(i), line));
            }
            context.resized(out, line);
            return out;
        });
        LuaLib.define("fs.complete", (context, target, arguments, line) -> context.table(line));
        LuaLib.define("fs.open", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "open", arguments, line);
            return open(context, normalise(args.text(0)), args.text(1), false, line);
        });
        LuaLib.define("io.open", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "open", arguments, line);
            return open(context, resolve(context, args.text(0)), args.text(1, "r"), true, line);
        });
        LuaLib.define("io.lines", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "lines", arguments, line);
            final Object handle = open(context, resolve(context, args.text(0)), "r", true, line);
            if (!(handle instanceof Values.Table table)) {
                throw context.error(args.text(0) + ": No such file", line);
            }
            return table.get("lines");
        });
        registerHandle();
    }

    private static void makeDirs(final ILuaContext context, final String path, final int line) {
        if (path.isEmpty()) {
            return;
        }
        final ILuaFiles.Entry entry = context.files().stat(path);
        if (entry != null) {
            if (!entry.directory()) {
                throw context.error("/" + path + ": File exists", line);
            }
            return;
        }
        makeDirs(context, dirOf(path), line);
        final String wrong = context.files().makeDir(path);
        if (wrong != null) {
            throw context.error("/" + path + ": " + wrong, line);
        }
    }

    private static void delete(final ILuaContext context, final String path, final int line) {
        if (path.isEmpty()) {
            throw context.error("/: Access denied", line);
        }
        final ILuaFiles.Entry entry = context.files().stat(path);
        if (entry != null && entry.directory()) {
            final List<ILuaFiles.Entry> inside = context.files().list(path);
            if (inside != null) {
                for (final ILuaFiles.Entry child : inside) {
                    delete(context, path + "/" + child.name(), line);
                }
            }
        }
        final String wrong = context.files().delete(path);
        if (wrong != null) {
            throw context.error("/" + path + ": " + wrong, line);
        }
    }

    private static void copy(final ILuaContext context, final String from, final String to, final int line) {
        final ILuaFiles.Entry entry = context.files().stat(from);
        if (entry == null) {
            throw context.error("/" + from + ": No such file", line);
        }
        if (context.files().stat(to) != null) {
            throw context.error("/" + to + ": File exists", line);
        }
        if (to.equals(from) || to.startsWith(from + "/")) {
            throw context.error("/" + from + ": Can't copy a directory inside itself", line);
        }
        if (entry.directory()) {
            makeDirs(context, to, line);
            final List<ILuaFiles.Entry> inside = context.files().list(from);
            if (inside != null) {
                for (final ILuaFiles.Entry child : inside) {
                    copy(context, from + "/" + child.name(), to + "/" + child.name(), line);
                }
            }
            return;
        }
        makeDirs(context, dirOf(to), line);
        final String wrong = context.files().write(to, context.files().read(from));
        if (wrong != null) {
            throw context.error("/" + to + ": " + wrong, line);
        }
    }

    private static void find(final ILuaContext context, final String at, final String[] parts, final int index,
                             final List<String> found) {
        if (found.size() >= GLOB_LIMIT) {
            return;
        }
        if (index == parts.length) {
            found.add(at);
            return;
        }
        final String part = parts[index];
        if (part.indexOf('*') < 0 && part.indexOf('?') < 0) {
            final String next = at.isEmpty() ? part : at + "/" + part;
            if (context.files().stat(next) != null) {
                find(context, next, parts, index + 1, found);
            }
            return;
        }
        final List<ILuaFiles.Entry> inside = context.files().list(at);
        if (inside == null) {
            return;
        }
        final String regex = "\\Q" + part.replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q") + "\\E";
        for (final ILuaFiles.Entry child : inside) {
            if (child.name().matches(regex)) {
                find(context, at.isEmpty() ? child.name() : at + "/" + child.name(), parts, index + 1, found);
            }
        }
    }

    // handles

    /*
     * A handle is a table of functions bound to one state object, which is the open file: its path,
     * the mode, the text read or being written, and where reading has got to.
     */
    private static Object open(final ILuaContext context, final String path, final String mode, final boolean io,
                               final int line) {
        final char kind = mode.isEmpty() ? 'r' : mode.charAt(0);
        final Values.Obj state = context.state("fs.handle", null, line);
        state.set(PATH, context.text(path, line));
        state.set(MODE, context.text(String.valueOf(kind), line));
        state.set(POSITION, 0L);
        state.set(CLOSED, false);
        final ILuaFiles.Entry entry = context.files().stat(path);
        switch (kind) {
            case 'r' -> {
                final String text = entry == null || entry.directory() ? null : context.files().read(path);
                if (text == null) {
                    return context.values(java.util.Arrays.asList(null,
                            context.text("/" + path + ": No such file", line)), line);
                }
                state.set(TEXT, context.text(text, line));
            }
            case 'w', 'a' -> {
                if (entry != null && entry.directory()) {
                    return context.values(java.util.Arrays.asList(null,
                            context.text("/" + path + ": Cannot write to directory", line)), line);
                }
                final String before = kind == 'a' && entry != null ? context.files().read(path) : null;
                state.set(TEXT, context.text(before == null ? "" : before, line));
                // An opened file exists from that moment, as on any disk.
                final String wrong = context.files().write(path, before == null ? "" : before);
                if (wrong != null) {
                    return context.values(java.util.Arrays.asList(null, context.text("/" + path + ": " + wrong, line)),
                            line);
                }
            }
            default -> throw context.error("Unsupported mode", line);
        }
        final Values.Table handle = context.table(line);
        final String[] names = kind == 'r'
                ? new String[] {"readLine", "readAll", "read", "lines", "seek", "close"}
                : new String[] {"write", "writeLine", "flush", "seek", "close"};
        for (final String name : names) {
            handle.put(context.text(name, line), context.function("fs.handle." + name, state, line));
        }
        context.resized(handle, line);
        return handle;
    }

    /* A handle's functions take the handle itself first when called with a colon, which is skipped. */
    private static Object[] own(final Object[] arguments) {
        if (arguments.length > 0 && arguments[0] instanceof Values.Table) {
            final Object[] rest = new Object[arguments.length - 1];
            System.arraycopy(arguments, 1, rest, 0, rest.length);
            return rest;
        }
        return arguments;
    }

    private static Values.Obj open(final ILuaContext context, final Object target, final int line) {
        final Values.Obj state = (Values.Obj) target;
        if (Boolean.TRUE.equals(state.get(CLOSED))) {
            throw context.error("attempt to use a closed file", line);
        }
        return state;
    }

    private static void registerHandle() {
        LuaLib.hidden("fs.handle.readLine", (context, target, arguments, line) ->
                readLine(context, open(context, target, line), LuaValues.truth(own(arguments).length > 0
                        ? own(arguments)[0] : null), line));
        LuaLib.hidden("fs.handle.lines", (context, target, arguments, line) ->
                readLine(context, open(context, target, line), false, line));
        LuaLib.hidden("fs.handle.readAll", (context, target, arguments, line) -> {
            final Values.Obj state = open(context, target, line);
            final String text = String.valueOf(state.get(TEXT));
            final int at = (int) (long) (Long) state.get(POSITION);
            if (at >= text.length()) {
                return text.isEmpty() && at == 0 ? context.text("", line) : null;
            }
            state.set(POSITION, (long) text.length());
            return context.text(text.substring(at), line);
        });
        LuaLib.hidden("fs.handle.read", (context, target, arguments, line) -> {
            final Values.Obj state = open(context, target, line);
            final Object[] args = own(arguments);
            final Object format = args.length > 0 ? args[0] : null;
            if (format instanceof String how) {
                // io's formats: a line, the whole rest, a number.
                final String plain = how.startsWith("*") ? how.substring(1) : how;
                if (plain.startsWith("a")) {
                    final String text = String.valueOf(state.get(TEXT));
                    final int at = (int) (long) (Long) state.get(POSITION);
                    state.set(POSITION, (long) text.length());
                    return context.text(at >= text.length() ? "" : text.substring(at), line);
                }
                if (plain.startsWith("n")) {
                    final Object read = readLine(context, state, false, line);
                    return read == null ? null : LuaNumbers.toNumber(read);
                }
                return readLine(context, state, plain.startsWith("L"), line);
            }
            final long count = format == null ? 1 : Math.max(0, LuaNumbers.toInteger(format) == null ? 1
                    : LuaNumbers.toInteger(format));
            final String text = String.valueOf(state.get(TEXT));
            final int at = (int) (long) (Long) state.get(POSITION);
            if (at >= text.length()) {
                return null;
            }
            final int end = (int) Math.min(text.length(), at + count);
            state.set(POSITION, (long) end);
            return context.text(text.substring(at, end), line);
        });
        LuaLib.hidden("fs.handle.write", (context, target, arguments, line) -> {
            final Values.Obj state = open(context, target, line);
            final StringBuilder more = new StringBuilder(String.valueOf(state.get(TEXT)));
            for (final Object value : own(arguments)) {
                if (value instanceof Long whole && "w".equals(state.get(MODE)) && arguments.length == 1) {
                    more.append(LuaValues.plainString(whole));
                } else {
                    more.append(LuaValues.plainString(value));
                }
            }
            state.set(TEXT, context.text(more.toString(), line));
            return null;
        });
        LuaLib.hidden("fs.handle.writeLine", (context, target, arguments, line) -> {
            final Values.Obj state = open(context, target, line);
            final Object[] args = own(arguments);
            final String piece = args.length > 0 ? LuaValues.plainString(args[0]) : "";
            state.set(TEXT, context.text(state.get(TEXT) + piece + "\n", line));
            return null;
        });
        LuaLib.hidden("fs.handle.flush", (context, target, arguments, line) -> {
            flush(context, open(context, target, line), line);
            return null;
        });
        LuaLib.hidden("fs.handle.seek", (context, target, arguments, line) -> {
            final Values.Obj state = open(context, target, line);
            final Object[] args = own(arguments);
            final String whence = args.length > 0 && args[0] instanceof String given ? given : "cur";
            final long offset = args.length > 1 && LuaNumbers.toInteger(args[1]) != null ? LuaNumbers.toInteger(args[1]) : 0;
            final long length = String.valueOf(state.get(TEXT)).length();
            final long base = switch (whence) {
                case "set" -> 0;
                case "end" -> length;
                default -> (Long) state.get(POSITION);
            };
            final long at = Math.max(0, Math.min(length, base + offset));
            state.set(POSITION, at);
            return at;
        });
        LuaLib.hidden("fs.handle.close", (context, target, arguments, line) -> {
            final Values.Obj state = open(context, target, line);
            if (!"r".equals(state.get(MODE))) {
                flush(context, state, line);
            }
            state.set(CLOSED, true);
            return null;
        });
    }

    private static Object readLine(final ILuaContext context, final Values.Obj state, final boolean keepEnd,
                                   final int line) {
        final String text = String.valueOf(state.get(TEXT));
        final int at = (int) (long) (Long) state.get(POSITION);
        if (at >= text.length()) {
            return null;
        }
        final int newline = text.indexOf('\n', at);
        final int end = newline < 0 ? text.length() : newline;
        state.set(POSITION, (long) (newline < 0 ? text.length() : newline + 1));
        String read = text.substring(at, keepEnd && newline >= 0 ? newline + 1 : end);
        if (!keepEnd && read.endsWith("\r")) {
            read = read.substring(0, read.length() - 1);
        }
        return context.text(read, line);
    }

    private static void flush(final ILuaContext context, final Values.Obj state, final int line) {
        final String wrong = context.files().write(String.valueOf(state.get(PATH)), String.valueOf(state.get(TEXT)));
        if (wrong != null) {
            throw context.error("/" + state.get(PATH) + ": " + wrong, line);
        }
    }
}
