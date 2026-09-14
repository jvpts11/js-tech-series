/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.IOperand;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reading and writing a program's fields: an object's own, and the ones a type keeps for itself.
 *
 * <p>A type's static fields live here, one holder per type, made the first time the type is touched, and are written
 * down with the program. A few fields belong to the process rather than to an object (the thread asking, the program's
 * arguments, the program itself, whether another program still runs), and those are asked of the process.
 */
final class FieldAccess {

    private final Process process;
    private final Heap heap;
    private final Library library;
    private final ProgramImage program;
    private final Map<String, Values.Obj> statics = new LinkedHashMap<>();
    private final Map<String, Values.Obj> view = Collections.unmodifiableMap(this.statics);

    FieldAccess(final Process process, final Heap heap, final Library library, final ProgramImage program) {
        this.process = process;
        this.heap = heap;
        this.library = library;
        this.program = program;
    }

    /** Reads a field of the object on top of the stack and puts what it holds there instead. */
    void load(final Frame frame, final IOperand.Field field, final int line) {
        final Object target = this.heap.alive(frame.pop(), line);
        if (target instanceof Values.Obj object) {
            if ("Process".equals(object.type())
                    && ("Running".equals(field.name()) || "ExitCode".equals(field.name()))) {
                // Whether another program still runs is the machine's to say, not a field to go stale.
                frame.push(this.library.programField(object.get("Id"), Process.processHost(object), field.name(),
                        line));
                return;
            }
            final Object held = object.get(field.name());
            // A widget's texts are its own, so the program is handed a copy that stays the program's.
            frame.push(held instanceof String said && UiWidgets.handles(object.type())
                    ? this.heap.text(said, line) : held);
            return;
        }
        frame.push(this.library.read(target, field.name(), line));
    }

    /** Writes the value on top of the stack into a field of the object beneath it. */
    void store(final Frame frame, final IOperand.Field field, final int line) {
        final Object value = frame.pop();
        final Object target = this.heap.alive(frame.pop(), line);
        if (!(target instanceof Values.Obj object)) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no object to write " + field.name() + " on");
        }
        if (UiWidgets.handles(object.type())) {
            // What a window shows is the machine's to draw again, so writing on a widget is paid for.
            this.library.uiWrite(object, field.name(), value, line);
            return;
        }
        object.set(field.name(), value);
    }

    /** Reads a field a type keeps for itself, one of an enum's values, or one the runtime answers. */
    void loadStatic(final Frame frame, final IOperand.Field field, final int line) {
        if ("Thread".equals(field.owner()) && "Current".equals(field.name())) {
            frame.push(this.process.tokenFor(this.process.current(), line));
            return;
        }
        if ("Program".equals(field.owner()) && "Args".equals(field.name())) {
            frame.push(this.process.argsList(line));
            return;
        }
        if ("Program".equals(field.owner()) && "Current".equals(field.name())) {
            frame.push(this.process.selfToken(line));
            return;
        }
        final TypeImage type = this.program.type(field.owner());
        if (type == null) {
            frame.push(this.library.readStatic(field.owner(), field.name(), line));
            return;
        }
        if (type.kind() == AsmType.Kind.ENUM) {
            frame.push(type.values().get(field.name()));
            return;
        }
        frame.push(this.statics(field.owner()).get(field.name()));
    }

    /** Writes the value on top of the stack into a field a type keeps for itself. */
    void storeStatic(final Frame frame, final IOperand.Field field, final int line) {
        this.statics(field.owner()).set(field.name(), frame.pop());
    }

    /** The holder of a type's static fields, made the first time the type is touched. */
    Values.Obj statics(final String owner) {
        return this.statics.computeIfAbsent(owner, Values.Obj::new);
    }

    /** Every holder made so far, by type, in the order they were made, for the save. */
    Map<String, Values.Obj> statics() {
        return this.view;
    }
}
