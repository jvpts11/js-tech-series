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
 * down with the program. A field of the program's own is read and written on its object with nothing looked up, which
 * was settled when the program loaded. A value the process or the language's core answers (the thread asking, the
 * program's arguments, the length of a text, what a widget shows) is read and written through what the program loaded
 * with, and what the machine keeps is asked of it.
 */
final class FieldAccess {

    private final Process process;
    private final Heap heap;
    private final ProgramImage program;
    private final Map<String, Values.Obj> statics = new LinkedHashMap<>();
    private final Map<String, Values.Obj> view = Collections.unmodifiableMap(this.statics);

    FieldAccess(final Process process, final Heap heap, final ProgramImage program) {
        this.process = process;
        this.heap = heap;
        this.program = program;
    }

    /** Reads a field of the object on top of the stack and puts what it holds there instead. */
    void load(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        final Object target = this.heap.alive(frame.pop(), line);
        final String name = site.field().name();
        if (site.handled() != null) {
            frame.push(site.handled().read().read(this.process, target, line));
            return;
        }
        if (site.world() >= 0 && this.process.worldCalls().binds(site.world())) {
            // Whether another program still runs is the machine's to say, not a field to go stale.
            final Object answer = this.process.worldCalls().answer(site.world(), target, WorldCalls.NOTHING, line);
            frame.push(this.heap.adopt(answer, line));
            return;
        }
        if (!(target instanceof Values.Obj object)) {
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "there is no " + name + " to read here");
        }
        if (site.own()) {
            frame.push(object.get(name));
            return;
        }
        if (site.world() >= 0 && "Process".equals(object.type())) {
            // Off any machine, the one program a program knows is going is itself, and none has ended with a code.
            final boolean itself = Integer.valueOf(this.process.machineId()).equals(object.get("Id"));
            frame.push("Running".equals(name) ? itself : 0);
            return;
        }
        frame.push(object.get(name));
    }

    /** Writes the value on top of the stack into a field of the object beneath it. */
    void store(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        final String name = site.field().name();
        final Object value = frame.pop();
        final Object target = this.heap.alive(frame.pop(), line);
        if (site.handled() != null && site.handled().write() != null) {
            site.handled().write().write(this.process, target, value, line);
            return;
        }
        if (!(target instanceof Values.Obj object)) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no object to write " + name + " on");
        }
        object.set(name, value);
    }

    /** Reads a field a type keeps for itself, one of an enum's values, or one the runtime answers. */
    void loadStatic(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        final IOperand.Field field = site.field();
        if (site.handled() != null) {
            frame.push(site.handled().read().read(this.process, null, line));
            return;
        }
        if (site.world() >= 0 && this.process.worldCalls().binds(site.world())) {
            final Object answer = this.process.worldCalls().answer(site.world(), null, WorldCalls.NOTHING, line);
            frame.push(this.heap.adopt(answer, line));
            return;
        }
        final TypeImage type = this.program.type(field.owner());
        if (type == null) {
            // A value of the world that the machine the program runs on does not answer.
            throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, field.owner() + " has no " + field.name());
        }
        if (type.kind() == AsmType.Kind.ENUM) {
            frame.push(type.values().get(field.name()));
            return;
        }
        frame.push(this.statics(field.owner()).get(field.name()));
    }

    /** Writes the value on top of the stack into a field a type keeps for itself. */
    void storeStatic(final Frame frame, final ProgramImage.ValueSite site, final int line) {
        this.statics(site.field().owner()).set(site.field().name(), frame.pop());
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
