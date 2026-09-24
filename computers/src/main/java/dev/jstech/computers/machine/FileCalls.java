/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import java.util.Map;

/**
 * The calls a program makes on the machine's drives, answered by its {@link FileService}.
 *
 * <p>Nothing here is free: a disk is slower than adding two numbers, and what each call costs is declared beside it. A
 * read or a write also says how many bytes it moved, since a large file costs more to move than a small one.
 */
final class FileCalls {

    private static final String STRING = "string";

    private FileCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        file(bindings, "Exists", (files, call, target, arguments, line) -> files.exists(path(arguments)), STRING);
        file(bindings, "Read", (files, call, target, arguments, line) -> {
            final ICliComputer.FsResult read = files.read(path(arguments));
            // A program reads in the machine's language: the file's own words, or why there were none.
            if (!read.ok()) {
                throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, read.message());
            }
            final String found = read.message().english();
            call.moved(FileService.bytesOf(found));
            return found;
        }, STRING);
        file(bindings, "TryRead", (files, call, target, arguments, line) -> {
            // What was found is filled in beside the answer, and is empty when nothing was.
            final ICliComputer.FsResult read = files.read(path(arguments));
            final String found = read.ok() ? read.message().english() : "";
            call.moved(FileService.bytesOf(found));
            arguments[1] = found;
            return read.ok();
        }, STRING, "out " + STRING);
        file(bindings, "Write", (files, call, target, arguments, line) -> {
            final String text = text(arguments);
            call.moved(FileService.bytesOf(text));
            return files.write(path(arguments), text);
        }, STRING, STRING);
        file(bindings, "Append", (files, call, target, arguments, line) -> {
            // Only what is added is moved: the file is added to where it ends, not read and written again.
            final String text = text(arguments);
            call.moved(FileService.bytesOf(text));
            return files.append(path(arguments), text);
        }, STRING, STRING);
        file(bindings, "Delete", (files, call, target, arguments, line) -> files.delete(path(arguments)), STRING);
        file(bindings, "MkDir", (files, call, target, arguments, line) -> files.makeFolder(path(arguments)), STRING);
        file(bindings, "List", (files, call, target, arguments, line) -> {
            final Values.ListValue names = new Values.ListValue();
            names.items().addAll(files.list(path(arguments)));
            return names;
        }, STRING);
    }

    private static void file(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                             final MachineCalls.IServiceFunction<FileService> function, final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::files, "File", name, function, parameters);
    }

    /** The first argument, which is the path the call is about. */
    private static String path(final Object[] arguments) {
        return arguments.length == 0 ? "" : String.valueOf(arguments[0]);
    }

    /** The second argument of a write, which is the text to put there. */
    private static String text(final Object[] arguments) {
        return arguments.length < 2 ? "" : String.valueOf(arguments[1]);
    }
}
