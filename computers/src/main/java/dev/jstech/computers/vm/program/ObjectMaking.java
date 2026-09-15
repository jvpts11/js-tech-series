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
import java.util.List;
import java.util.Map;

/**
 * Making a program's objects: an instance of one of its types, whose constructor then waits its turn, one the runtime
 * brings with it, an array, and the copy a struct makes of itself; and reaching into an array's places.
 */
final class ObjectMaking {

    private final Heap heap;
    private final Library library;
    private final ProgramImage program;
    private final CallDispatch calls;

    ObjectMaking(final Heap heap, final Library library, final ProgramImage program, final CallDispatch calls) {
        this.heap = heap;
        this.library = library;
        this.program = program;
        this.calls = calls;
    }

    /** Makes what a {@code new} line names, taking its arguments off the stack, and puts it on. */
    void newObject(final Frame frame, final ProgramImage.Creation creation, final int line) {
        final List<Object> arguments = CallDispatch.take(frame, creation.outs());
        if (creation.handled() != null) {
            frame.push(creation.handled().make(this.heap, line));
            return;
        }
        frame.push(creation.type() == null
                ? this.library.create(creation.made().owner(), arguments, line)
                : this.instance(creation.type(), creation.constructor(), arguments, line));
    }

    /**
     * A copy of a struct: a new object holding what the old one holds, counted like any other. A value
     * that is not a struct, null included, is handed back as it is, since there is nothing to copy.
     */
    Object copyOf(final Object value, final int line) {
        if (!(value instanceof Values.Obj original)) {
            return value;
        }
        final TypeImage known = this.program.type(original.type());
        if (known == null || known.kind() != AsmType.Kind.STRUCT) {
            return value;
        }
        final Values.Obj made = new Values.Obj(original.type());
        this.heap.allocate(made, known.instanceSize(), line);
        for (final Map.Entry<String, Object> field : original.all().entrySet()) {
            made.set(field.getKey(), this.copyOf(field.getValue(), line));
        }
        return made;
    }

    /**
     * An instance of the type named: the program's own, with the constructor taking that many arguments queued, or
     * one the runtime brings.
     */
    Object instance(final String type, final List<Object> arguments, final int line) {
        final TypeImage known = this.program.type(type);
        if (known == null) {
            return this.library.create(type, arguments, line);
        }
        return this.instance(known, known.constructor(arguments.size()), arguments, line);
    }

    private Values.Obj instance(final TypeImage type, final MethodImage constructor, final List<Object> arguments,
                                final int line) {
        final Values.Obj made = new Values.Obj(type.name());
        this.heap.allocate(made, type.instanceSize(), line);
        if (constructor != null) {
            this.calls.enter(constructor, made, arguments, line);
        }
        return made;
    }

    /** Makes an array of the element type, as long as the number on top of the stack says, and puts it on. */
    void newArray(final Frame frame, final IOperand.Type element, final int line) {
        final int length = Numbers.toInt(frame.pop());
        if (length < 0) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, "an array cannot have " + length + " places");
        }
        final Values.Arr made = new Values.Arr(element.name(), length);
        this.heap.allocate(made, Heap.HEADER + (long) Heap.sizeOf(element.name()) * length, line);
        frame.push(made);
    }

    /** Reads the place of an array the stack names and puts what it holds on. */
    void loadElement(final Frame frame, final int line) {
        final int index = Numbers.toInt(frame.pop());
        frame.push(this.array(frame.pop(), line).get(index, line));
    }

    /** Writes the value on top of the stack into the place of an array beneath it. */
    void storeElement(final Frame frame, final int line) {
        final Object value = frame.pop();
        final int index = Numbers.toInt(frame.pop());
        this.array(frame.pop(), line).set(index, value, line);
    }

    /** The array a value is, or a halt when it is none. */
    Values.Arr array(final Object value, final int line) {
        if (this.heap.alive(value, line) instanceof Values.Arr array) {
            return array;
        }
        throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no array here");
    }
}
